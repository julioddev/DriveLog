package com.example.drivelog;

import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.util.Log;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ScannerService extends AccessibilityService {

    public static final String ACTION_ROUTE_DETECTED = "com.example.drivelog.ROUTE_DETECTED";
    private long lastAlertTime = 0;
    private long lastNoOfferAlertTime = 0;
    private long lastNextDayClickTime = 0;
    private long lastLoopRestartTime = 0;
    private String lastSwipedTab = "";
    private boolean isWaitingForLoop = false;
    private boolean isPausedByUser = false;
    private boolean isDeveloper = false;
    private boolean needsToReturnToHoje = false;
    private long lastSuccessfulInteractionTime = System.currentTimeMillis();
    private long cycleStartTime = System.currentTimeMillis();
    private android.os.Handler loopHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private com.google.firebase.firestore.ListenerRegistration globalAlertListener;
    private String lastDetectedPkg = "";
    private String lastWatchdogPkg = "";

    // Variáveis para consolidar o resultado do ciclo
    private boolean[] cycleResults = new boolean[4]; // true se encontrou rota
    private boolean[] cycleChecked = new boolean[4]; // true se o dia foi verificado
    private String[] cycleDayNames = new String[4];
    private boolean[] lastCycleResults = new boolean[4]; // Estado do ciclo anterior
    
    private final Runnable watchdogRunnable = new Runnable() {
        @Override
        public void run() {
            if (isPausedByUser) {
                loopHandler.postDelayed(this, 2000);
                return;
            }

            long now = System.currentTimeMillis();
            String targetPkg = "com.mercadoenvios.crowdsourcing";
            
            // 🔥 VIGIA RADICAL: Não importa o que aconteça, se o app estiver aberto há mais de 30s, mata.
            // Para ser 100% infalível, vamos verificar se o serviço AINDA consegue ler o pacote.
            AccessibilityNodeInfo root = getRootInActiveWindow();
            String currentPkg = (root != null && root.getPackageName() != null) ? root.getPackageName().toString() : "";
            if (root != null) root.recycle();

            // Se o app alvo está na tela
            if (currentPkg.equals(targetPkg)) {
                if (!lastWatchdogPkg.equals(targetPkg)) {
                    cycleStartTime = now;
                    broadcastDebugLog("[Vigia] App focado. Monitorando travamento...");
                }
                lastWatchdogPkg = targetPkg;
                
                // 🔥 ANTI-TRAVA PERSONALIZÁVEL
                android.content.SharedPreferences prefs = getSharedPreferences("AppConfig", MODE_PRIVATE);
                int timeoutSeconds = prefs.getInt("scanner_antitrava_timeout", 30);
                if (timeoutSeconds < 10) timeoutSeconds = 10; // Segurança mínima

                // Se excedeu o tempo configurado, REINÍCIO FORÇADO ABSOLUTO
                if (now - cycleStartTime > (timeoutSeconds * 1000L) && !isWaitingForLoop) {
                    broadcastDebugLog("🚨 ANTI-TRAVA (" + timeoutSeconds + "s): Ciclo travado. EXECUTANDO RESET!");
                    executeEmergencyReset(targetPkg);
                }
            } else if (!currentPkg.isEmpty() && !currentPkg.contains("com.example.drivelog")) {
                // Se saiu do app ou trocou de tela, reseta a contagem para quando ele voltar
                lastWatchdogPkg = ""; 
            }

            loopHandler.postDelayed(this, 1000); 
        }
    };

    private void executeEmergencyReset(String pkg) {
        cycleStartTime = System.currentTimeMillis();
        lastSuccessfulInteractionTime = cycleStartTime;
        isWaitingForLoop = true;
        needsToReturnToHoje = true;
        
        // Tenta fechar de forma agressiva
        restartTargetApp(pkg);
    }

    private final BroadcastReceiver pauseReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("com.example.drivelog.SCANNER_PAUSE_TOGGLE".equals(intent.getAction())) {
                isPausedByUser = intent.getBooleanExtra("paused", false);
                Log.d("ScannerService", "Scanner pausado pelo usuário: " + isPausedByUser);
            }
        }
    };

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        Log.d("ScannerService", "Serviço de Acessibilidade Conectado!");
        
        // Carrega estado inicial
        isPausedByUser = getSharedPreferences("AppConfig", MODE_PRIVATE)
                .getBoolean("scanner_paused_by_user", false);

        IntentFilter filter = new IntentFilter("com.example.drivelog.SCANNER_PAUSE_TOGGLE");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(pauseReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            registerReceiver(pauseReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(pauseReceiver, filter);
        }

        // Configuração dinâmica para escutar mudanças em qualquer janela
        android.accessibilityservice.AccessibilityServiceInfo info = new android.accessibilityservice.AccessibilityServiceInfo();
        info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED | 
                         AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED |
                         AccessibilityEvent.TYPE_WINDOWS_CHANGED;
        info.feedbackType = android.accessibilityservice.AccessibilityServiceInfo.FEEDBACK_GENERIC;
        info.flags = android.accessibilityservice.AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS | 
                     android.accessibilityservice.AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS |
                     android.accessibilityservice.AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS;
        info.notificationTimeout = 10; // Reduzido drasticamente para reagir instantaneamente
        info.flags |= android.accessibilityservice.AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS | 
                     android.accessibilityservice.AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS |
                     android.accessibilityservice.AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS;

        setServiceInfo(info);

        checkDevAccess();
        
        // Inicia o Watchdog ticker
        loopHandler.post(watchdogRunnable);
    }

    private void checkDevAccess() {
        android.content.SharedPreferences prefs = getSharedPreferences("AppConfig", MODE_PRIVATE);
        String email = prefs.getString("profile_email", null);
        
        if (email == null || email.isEmpty()) {
            com.google.firebase.auth.FirebaseUser user = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
            if (user != null) email = user.getEmail();
        }
        
        if (email != null && !email.isEmpty()) {
            final String finalEmail = email;
            FirebaseHelper.checkDeveloperAccess(email, result -> {
                if (result && !isDeveloper) {
                    isDeveloper = true;
                    broadcastDebugLog("✅ Acesso DEV Identificado: " + finalEmail);
                    startGlobalAlertListener();
                } else if (!result && isDeveloper) {
                    isDeveloper = false; // Caso tenha perdido acesso
                }
            });
        }
    }

    private void startGlobalAlertListener() {
        // Removido daqui. Agora a MainActivity gerencia a escuta global 
        // para não depender do serviço de acessibilidade estar ativo.
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (isPausedByUser) return;

        // Se ainda não sabemos se é dev, tenta checar agora que temos um evento (e possivelmente um email)
        if (!isDeveloper) {
            checkDevAccess();
        }

        // 🔥 Otimização: Se for mudança de conteúdo e for do nosso próprio app, ignora rápido
        CharSequence eventPkg = event.getPackageName();
        if (eventPkg != null) {
            String pkgStr = eventPkg.toString();
            if (pkgStr.equals(getPackageName()) || pkgStr.contains("com.example.drivelog")) {
                return;
            }
            lastDetectedPkg = pkgStr;
            // 🔥 Grava o app na lista de "Detectados"
            recordDetectedApp(pkgStr);
        }

        // Ignora eventos que não sejam de mudança de janela ou conteúdo
        int eventType = event.getEventType();
        if (eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED && 
            eventType != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED &&
            eventType != AccessibilityEvent.TYPE_WINDOWS_CHANGED) {
            return;
        }

        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if (rootNode == null) rootNode = event.getSource();
        if (rootNode == null) return;

        processScreen(rootNode, lastDetectedPkg);

        if (rootNode != event.getSource()) {
            rootNode.recycle();
        }
    }

    private void processScreen(AccessibilityNodeInfo rootNode, String currentPkg) {
        if (isPausedByUser) return;

        // 3. Verificação tripla de segurança no nó raiz
        CharSequence rootPkg = rootNode.getPackageName();
        if (rootPkg != null) {
            String rootPkgStr = rootPkg.toString();
            if (rootPkgStr.equals(getPackageName()) || rootPkgStr.contains("com.example.drivelog")) {
                return;
            }
        }

        android.content.SharedPreferences prefs = getSharedPreferences("AppConfig", MODE_PRIVATE);
        boolean scanAllApps = prefs.getBoolean("scanner_scan_all_apps", false);
        Set<String> whitelist = prefs.getStringSet("scanner_whitelist_apps", new HashSet<>());
        Set<String> blacklist = prefs.getStringSet("scanner_blacklist_apps", new HashSet<>());
        int filterMode = prefs.getInt("scanner_filter_mode", 0); // 0: None, 1: Blacklist, 2: Whitelist

        // 4. Lógica de Filtragem (Mutuamente Exclusiva)
        boolean isAllowed = false;
        
        if (filterMode == 2) {
            isAllowed = whitelist.contains(currentPkg);
        } else if (filterMode == 1) {
            isAllowed = !blacklist.contains(currentPkg);
        } else {
            isAllowed = scanAllApps || currentPkg.equals("com.mercadoenvios.crowdsourcing");
        }

        if (!isAllowed || currentPkg.contains("com.example.drivelog") || currentPkg.equals(getPackageName())) {
            return;
        }

        // 5. Verificação de Tela e Navegação Automática
        // Baseado no seu print, as frases exatas são:
        // "Ainda não há ofertas disponíveis para este dia"
        // "Verifique em alguns instantes se há novas ofertas."
        boolean hasNoOffersMessage = searchRecursive(rootNode, "Ainda não há ofertas") || 
                                     searchRecursive(rootNode, "disponíveis para este dia") ||
                                     searchRecursive(rootNode, "Verifique em alguns instantes");
        
        boolean isExtraDriver = currentPkg.equals("com.mercadoenvios.crowdsourcing");
        
        long now = System.currentTimeMillis();

        if (isExtraDriver) {
            boolean autoClickEnabled = prefs.getBoolean("scanner_auto_click_next_day", true);
            if (isWaitingForLoop) return;

            AccessibilityNodeInfo hojeNodeCheck = findNodeByExactText(rootNode, "Hoje");
            if (hojeNodeCheck == null) {
                if (autoClickEnabled) {
                    navigateToDisponiveisTab(rootNode);
                }
                return;
            } else {
                if (needsToReturnToHoje && autoClickEnabled) {
                    boolean isSelected = hojeNodeCheck.isSelected() || isAnyParentSelected(hojeNodeCheck);
                    if (!isSelected) {
                        broadcastDebugLog("[Loop] Retornando para aba Hoje...");
                        performClick(hojeNodeCheck);
                        lastNextDayClickTime = now;
                        hojeNodeCheck.recycle();
                        return;
                    } else {
                        needsToReturnToHoje = false;
                        lastSuccessfulInteractionTime = now; 
                        cycleStartTime = now; // ✅ Início de um novo ciclo saudável
                        // Resetar resultados ao voltar para o Hoje (novo ciclo)
                        for (int i = 0; i < 4; i++) {
                            cycleChecked[i] = false;
                            cycleResults[i] = false;
                            cycleDayNames[i] = (i == 0) ? "Hoje" : getDayNameRelative(i).split("-")[0];
                        }
                    }
                }
                hojeNodeCheck.recycle();
            }
        }

        // 6. Lógica de Alertas e Pular Dias
        int currentDayIndex = getCurrentDayIndex(rootNode);
        
        if (hasNoOffersMessage) {
            // Marcar que este dia não tem ofertas
            if (currentDayIndex != -1) {
                cycleChecked[currentDayIndex] = true;
                cycleResults[currentDayIndex] = false;
                lastSuccessfulInteractionTime = now; // ✅ Detectou mensagem válida
            }

            if (isExtraDriver && prefs.getBoolean("scanner_auto_click_next_day", true)) {
                clickNextDayIfNoOffers(rootNode);
            }
        } else {
            // Caso 2: A frase NÃO está na tela (POSSÍVEL ROTA!)
            // Verificação baseada no print: Procura por "vaga", "disponível" ou o símbolo "R$"
            boolean isRouteConfirmed = searchRecursive(rootNode, "vaga") || 
                                     searchRecursive(rootNode, "disponível") ||
                                     searchRecursive(rootNode, "R$");

            if (isRouteConfirmed) {
                lastSuccessfulInteractionTime = now; // ✅ Detectou rota
                // Marcar que este dia TEM ofertas
                if (currentDayIndex != -1) {
                    cycleChecked[currentDayIndex] = true;
                    cycleResults[currentDayIndex] = true;
                }

                if (now - lastAlertTime > 10000) {
                    lastAlertTime = now;
                    triggerAlert();
                    broadcastDebugLog("🔥 ROTA DETECTADA! (Confirmado por vaga/R$)");
                }
                
                // Se encontramos rota, podemos querer pular para o próximo dia para ver se tem mais
                if (isExtraDriver && prefs.getBoolean("scanner_auto_click_next_day", true)) {
                    clickNextDayIfNoOffers(rootNode);
                }
            }
        }
    }

    private int getCurrentDayIndex(AccessibilityNodeInfo root) {
        if (isTabSelected(root, "Hoje")) return 0;
        
        for (int i = 1; i <= 3; i++) {
            String dayFull = getDayNameRelative(i);
            String dayShort = dayFull.contains("-") ? dayFull.split("-")[0] : dayFull;
            if (isTabSelected(root, dayFull) || isTabSelected(root, dayShort)) return i;
        }
        return -1;
    }

    private boolean searchRecursive(AccessibilityNodeInfo node, String textToFind) {
        if (node == null) return false;
        
        // 🔥 Proteção Adicional: Verifica se o próprio nó pertence ao nosso app
        CharSequence nodePkg = node.getPackageName();
        if (nodePkg != null && (nodePkg.toString().contains("com.example.drivelog") || nodePkg.toString().equals(getPackageName()))) {
            return false;
        }

        boolean found = false;

        // 1. Tenta extrair Texto principal
        if (node.getText() != null) {
            String nodeText = node.getText().toString();
            if (!nodeText.trim().isEmpty()) {
                // broadcastDebugLog(nodeText); // Opcional: reduzir spam de logs
                if (nodeText.toLowerCase().contains(textToFind.toLowerCase())) {
                    found = true;
                }
            }
        }
        
        // 2. Tenta extrair Descrição (usada em botões e imagens)
        if (node.getContentDescription() != null) {
            String desc = node.getContentDescription().toString();
            if (!desc.trim().isEmpty()) {
                broadcastDebugLog("[Desc] " + desc);
                if (desc.toLowerCase().contains(textToFind.toLowerCase())) {
                    Log.d("ScannerService", "Descrição encontrada: " + desc);
                    found = true;
                }
            }
        }

        // 3. Tenta extrair "Hint" ou outros textos secundários (API 26+)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            if (node.getHintText() != null) {
                String hint = node.getHintText().toString();
                broadcastDebugLog("[Hint] " + hint);
                if (hint.toLowerCase().contains(textToFind.toLowerCase())) found = true;
            }
        }

        // 4. Continua a busca nos filhos (mesmo que já tenha achado no pai, para popular os logs)
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                if (searchRecursive(child, textToFind)) found = true;
                child.recycle(); // Libera memória do nó filho após processar
            }
        }
        
        return found;
    }

    private void clickNextDayIfNoOffers(AccessibilityNodeInfo root) {
        android.content.SharedPreferences prefs = getSharedPreferences("AppConfig", MODE_PRIVATE);
        int actionDelay = prefs.getInt("scanner_action_delay", 2000); 

        long now = System.currentTimeMillis();
        if (now - lastNextDayClickTime < actionDelay) return; 

        // 🔥 Lógica de Navegação Direta Sequencial
        int currentIdx = getCurrentDayIndex(root);
        
        if (currentIdx >= 0 && currentIdx < 3) {
            int targetIdx = currentIdx + 1;
            String targetName = getDayNameRelative(targetIdx).split("-")[0];
            broadcastDebugLog("[Scanner] Dia +" + currentIdx + " sem rotas. Indo para " + targetName);
            clickSpecificDay(root, targetIdx);
        } else if (currentIdx == 3) {
            if (prefs.getBoolean("scanner_loop_enabled", true)) {
                // Finalizar o ciclo: Gerar e Enviar Notificação Consolidada
                sendConsolidatedNotification();
                
                broadcastDebugLog("Verificação completa (4 dias). Reiniciando App...");
                isWaitingForLoop = true;
                needsToReturnToHoje = true; 
                lastLoopRestartTime = System.currentTimeMillis();
                restartTargetApp("com.mercadoenvios.crowdsourcing");
            }
        } else {
            Log.d("ScannerService", "Seleção de aba não detectada claramente.");
        }
    }

    private void sendConsolidatedNotification() {
        android.content.SharedPreferences prefs = getSharedPreferences("AppConfig", MODE_PRIVATE);
        
        // 🔥 Removido o bloqueio de "Estado inalterado" para garantir que a coleção Firebase 
        // seja atualizada a cada ciclo completo, mesmo que os dados sejam os mesmos.
        // Isso resolve o problema de parar de enviar após o primeiro sucesso.

        // Salva o estado atual para o próximo comparativo (mantido para logs)
        System.arraycopy(cycleResults, 0, lastCycleResults, 0, 4);

        StringBuilder sb = new StringBuilder();
        boolean hasAnyRoute = false;
        java.util.List<String> foundDays = new java.util.ArrayList<>();
        java.util.List<String> emptyDays = new java.util.ArrayList<>();

        for (int i = 0; i < 4; i++) {
            if (cycleChecked[i]) {
                if (cycleResults[i]) {
                    hasAnyRoute = true;
                    foundDays.add(cycleDayNames[i]);
                } else {
                    emptyDays.add(cycleDayNames[i]);
                }
            }
        }

        if (hasAnyRoute) {
            sb.append("Rota encontrada para ");
            if (foundDays.size() == 4) {
                sb.append("os 4 dias");
            } else {
                for (int i = 0; i < foundDays.size(); i++) {
                    sb.append(foundDays.get(i));
                    if (i < foundDays.size() - 2) sb.append(", ");
                    else if (i == foundDays.size() - 2) sb.append(" e ");
                }
            }
        } else {
            sb.append("Nenhuma rota detectada para ");
            for (int i = 0; i < emptyDays.size(); i++) {
                sb.append(emptyDays.get(i));
                if (i < emptyDays.size() - 2) sb.append(", ");
                else if (i == emptyDays.size() - 2) sb.append(" e ");
            }
        }

        String finalMessage = sb.toString();

        // Notificação Local (Apenas se configurado)
        if (hasAnyRoute && prefs.getBoolean("notif_offer_local", true)) {
            sendSystemNotification("Scanner DriveLog", finalMessage, 1001);
            broadcastLocalAlert("🔥 ROTA!", finalMessage);
        } else if (!hasAnyRoute && prefs.getBoolean("notif_no_offer_local", false)) {
            sendSystemNotification("Scanner DriveLog", finalMessage, 1002);
            broadcastLocalAlert("💤 Sem Ofertas", finalMessage);
        }

        // Notificação Global para Desenvolvedores
        if (isDeveloper) {
            boolean shouldSendGlobal = (hasAnyRoute && prefs.getBoolean("notif_offer_global", true)) ||
                                     (!hasAnyRoute && prefs.getBoolean("notif_no_offer_global", false));
            
            broadcastDebugLog("DEBUG: shouldSendGlobal=" + shouldSendGlobal + " | hasAnyRoute=" + hasAnyRoute);

            if (shouldSendGlobal) {
                String email = prefs.getString("profile_email", "dev@example.com");
                String name = prefs.getString("profile_name", "Dev");
                
                broadcastDebugLog("DEBUG: Tentando enviar para o Firebase...");
                
                // 🔥 Adicionamos prefixo para o receptor saber se é Rota ou Sem Oferta sem precisar de boolean extra no Firebase (economia)
                String firebaseMsg = (hasAnyRoute ? "🔥 ROTA! - " : "💤 Sem Ofertas - ") + finalMessage;
                
                FirebaseHelper.broadcastScannerAlert(email, name, firebaseMsg, hasAnyRoute, (success, error) -> {
                    if (success) {
                        broadcastDebugLog("☁️ [NUVEM] Alerta enviado com sucesso!");
                    } else {
                        broadcastDebugLog("❌ [ERRO NUVEM] " + error);
                    }
                });
            }
        } else {
            broadcastDebugLog("DEBUG: Usuário NÃO identificado como DEV no ScannerService.");
        }
    }

    private void broadcastLocalAlert(String title, String message) {
        ScannerAlertManager.addAlert(this, title, message, System.currentTimeMillis());
        Intent intent = new Intent(ACTION_ROUTE_DETECTED);
        intent.putExtra("title", title);
        intent.putExtra("message", message);
        intent.putExtra("time", System.currentTimeMillis());
        sendBroadcast(intent);
    }

    private void restartTargetApp(String packageName) {
        lastSwipedTab = ""; // Reseta estado
        
        broadcastDebugLog("[Vigia] Matando processo travado...");

        // 🔥 TÁTICA DE "TERRA ARRASADA" PARA FECHAR O APP
        // 1. Abre as Configurações do Android diretamente na tela do app Envios Extra
        try {
            Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:" + packageName));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NO_HISTORY);
            startActivity(intent);
            
            // 2. Aguarda um momento e simula o botão BACK para sair das configurações
            // Isso tira o foco do app e interrompe qualquer thread de UI travada.
            loopHandler.postDelayed(() -> {
                performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK);
                performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME);
            }, 1500);

        } catch (Exception e) {
            // Fallback se falhar ao abrir settings
            performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME);
        }

        // 3. Agenda a reabertura FORÇADA
        int waitSecs = getSharedPreferences("AppConfig", MODE_PRIVATE).getInt("scanner_loop_wait_time", 30);
        if (waitSecs < 10) waitSecs = 10; // Tempo maior para garantir limpeza

        loopHandler.postDelayed(() -> {
            isWaitingForLoop = false;
            broadcastDebugLog("[Vigia] Reabrindo com instância limpa...");
            
            Intent launchIntent = getPackageManager().getLaunchIntentForPackage(packageName);
            if (launchIntent != null) {
                // FLAGS CRÍTICAS: Faz o Android descartar qualquer coisa velha e começar do zero absoluto
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | 
                                    Intent.FLAG_ACTIVITY_CLEAR_TOP | 
                                    Intent.FLAG_ACTIVITY_CLEAR_TASK |
                                    Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
                startActivity(launchIntent);
            }
        }, waitSecs * 1000L);
    }

    private boolean isTabSelected(AccessibilityNodeInfo root, String text) {
        if (root == null || text == null || text.isEmpty()) return false;
        
        // Tenta encontrar o nó pelo texto
        List<AccessibilityNodeInfo> nodes = root.findAccessibilityNodeInfosByText(text);
        if (nodes != null) {
            for (AccessibilityNodeInfo node : nodes) {
                if (node.getText() != null && node.getText().toString().equalsIgnoreCase(text)) {
                    boolean selected = node.isSelected() || isAnyParentSelected(node);
                    // Importante: No Envios Extra, às vezes a aba é um container que fica 'Selected'
                    // mas o texto dentro não. O isAnyParentSelected já ajuda nisso.
                    node.recycle();
                    if (selected) {
                        // Limpa o resto da lista para evitar leaks
                        for (AccessibilityNodeInfo n : nodes) { if (n != node) n.recycle(); }
                        return true;
                    }
                } else {
                    node.recycle();
                }
            }
        }
        return false;
    }

    private boolean isAnyParentSelected(AccessibilityNodeInfo node) {
        AccessibilityNodeInfo parent = node.getParent();
        if (parent == null) return false;
        if (parent.isSelected()) {
            parent.recycle();
            return true;
        }
        boolean res = isAnyParentSelected(parent);
        parent.recycle();
        return res;
    }

    private void clickSpecificDay(AccessibilityNodeInfo root, int daysFromToday) {
        String dayName = getDayNameRelative(daysFromToday);
        AccessibilityNodeInfo targetNode = findNodeByExactText(root, dayName);
        
        if (targetNode == null && dayName.contains("-")) {
            targetNode = findNodeByExactText(root, dayName.split("-")[0]);
        }

        if (targetNode != null) {
            boolean success = performClick(targetNode);
            if (success) {
                lastNextDayClickTime = System.currentTimeMillis();
                Log.d("ScannerService", "Auto-clique para dia +" + daysFromToday + ": " + dayName);
                broadcastDebugLog("[Auto-Clique] Pulando para " + dayName + "...");
            }
            targetNode.recycle();
        }
    }

    private void navigateToDisponiveisTab(AccessibilityNodeInfo root) {
        // Tenta encontrar por texto exato primeiro
        AccessibilityNodeInfo disponiveisTab = findNodeByExactText(root, "Disponíveis");
        
        // Se não achar por texto, tenta por descrição de conteúdo (comum em ícones de menu inferior)
        if (disponiveisTab == null) {
            disponiveisTab = findNodeByContentDescription(root, "Disponíveis");
        }

        if (disponiveisTab != null) {
            boolean success = performClick(disponiveisTab);
            if (success) {
                lastNextDayClickTime = System.currentTimeMillis();
                Log.d("ScannerService", "Auto-clique realizado na aba principal 'Disponíveis'");
                broadcastDebugLog("[Auto-Clique] Clicando em 'Disponíveis' para abrir agenda.");
            } else {
                Log.d("ScannerService", "Falha ao executar clique em 'Disponíveis'");
                broadcastDebugLog("[Erro] Encontrei 'Disponíveis' mas o clique falhou.");
            }
            disponiveisTab.recycle();
        } else {
            Log.d("ScannerService", "Aba 'Disponíveis' não encontrada na tela atual.");
        }
    }

    private AccessibilityNodeInfo findNodeByContentDescription(AccessibilityNodeInfo root, String desc) {
        if (root == null) return null;
        if (root.getContentDescription() != null && root.getContentDescription().toString().equalsIgnoreCase(desc)) {
            return root;
        }
        for (int i = 0; i < root.getChildCount(); i++) {
            AccessibilityNodeInfo child = root.getChild(i);
            AccessibilityNodeInfo result = findNodeByContentDescription(child, desc);
            if (result != null) return result;
            if (child != null) child.recycle();
        }
        return null;
    }

    private String getDayNameRelative(int daysFromToday) {
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        calendar.add(java.util.Calendar.DAY_OF_YEAR, daysFromToday);
        
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("EEEE", new java.util.Locale("pt", "BR"));
        String dayName = sdf.format(calendar.getTime());
        
        if (!dayName.isEmpty()) {
            return dayName.substring(0, 1).toUpperCase() + dayName.substring(1);
        }
        return "";
    }

    private AccessibilityNodeInfo findNodeByExactText(AccessibilityNodeInfo root, String text) {
        if (root == null) return null;
        List<AccessibilityNodeInfo> nodes = root.findAccessibilityNodeInfosByText(text);
        if (nodes != null) {
            for (AccessibilityNodeInfo node : nodes) {
                if (node.getText() != null && node.getText().toString().equalsIgnoreCase(text)) {
                    return node;
                }
                node.recycle();
            }
        }
        return null;
    }

    private boolean performClick(AccessibilityNodeInfo node) {
        if (node == null) return false;
        
        boolean success = false;
        if (node.isClickable()) {
            success = node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
        }
        
        if (!success) {
            AccessibilityNodeInfo parent = node.getParent();
            if (parent != null) {
                success = performClick(parent);
                parent.recycle();
            }
        }
        
        // 🔥 Fallback: Se o clique via Acessibilidade falhou, tenta via Gesto (Coordenadas)
        if (!success && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            success = performGestureClick(node);
        }

        if (success) {
            lastSuccessfulInteractionTime = System.currentTimeMillis();
        }
        return success;
    }

    private boolean performGestureClick(AccessibilityNodeInfo node) {
        if (node == null || android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.N) return false;
        
        android.graphics.Rect bounds = new android.graphics.Rect();
        node.getBoundsInScreen(bounds);
        
        int x = bounds.centerX();
        int y = bounds.centerY();
        
        // Verifica se as coordenadas são válidas (dentro da tela)
        if (x <= 0 || y <= 0) return false;

        android.accessibilityservice.GestureDescription.Builder builder = new android.accessibilityservice.GestureDescription.Builder();
        android.graphics.Path path = new android.graphics.Path();
        path.moveTo(x, y);
        
        builder.addStroke(new android.accessibilityservice.GestureDescription.StrokeDescription(path, 0, 100));
        
        broadcastDebugLog("👉 Clique via Gesto em: " + x + "," + y);
        return dispatchGesture(builder.build(), null, null);
    }

    private AccessibilityNodeInfo findNodeByPartialText(AccessibilityNodeInfo root, String text) {
        if (root == null) return null;
        List<AccessibilityNodeInfo> nodes = root.findAccessibilityNodeInfosByText(text);
        if (nodes != null && !nodes.isEmpty()) {
            return nodes.get(0);
        }
        return null;
    }

    private void performSwipeDown(int startX, int startY) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            android.util.DisplayMetrics metrics = getResources().getDisplayMetrics();
            int width = metrics.widthPixels;
            int height = metrics.heightPixels;

            android.accessibilityservice.GestureDescription.Builder builder = new android.accessibilityservice.GestureDescription.Builder();
            android.graphics.Path path = new android.graphics.Path();
            
            // Coordenadas
            int x = (startX != -1) ? startX : width / 2;
            int yStart = (startY != -1) ? startY : height / 3;
            int yEnd = yStart + (height / 4); // Arrasta um pouco para baixo
            
            if (yEnd > height) yEnd = height - 50;
            
            path.moveTo(x, yStart);
            path.lineTo(x, yEnd);
            
            builder.addStroke(new android.accessibilityservice.GestureDescription.StrokeDescription(path, 0, 500));
            
            dispatchGesture(builder.build(), null, null);
            Log.d("ScannerService", "Gesto de Swipe iniciado em: " + x + ", " + yStart);
        }
    }

    private void broadcastDebugLog(String text) {
        if (isPausedByUser) return;
        
        Intent intent = new Intent("com.example.drivelog.SCANNER_DEBUG_LOG");
        intent.putExtra("text", text);
        intent.putExtra("time", System.currentTimeMillis());
        sendBroadcast(intent);
    }

    private void recordDetectedApp(String pkg) {
        android.content.SharedPreferences prefs = getSharedPreferences("AppConfig", MODE_PRIVATE);
        Set<String> detected = new HashSet<>(prefs.getStringSet("scanner_detected_apps", new HashSet<>()));
        if (!detected.contains(pkg)) {
            detected.add(pkg);
            prefs.edit().putStringSet("scanner_detected_apps", detected).apply();
            Log.d("ScannerService", "Novo app detectado: " + pkg);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (loopHandler != null) {
            loopHandler.removeCallbacksAndMessages(null);
        }
        if (globalAlertListener != null) {
            globalAlertListener.remove();
            globalAlertListener = null;
        }
        try {
            unregisterReceiver(pauseReceiver);
        } catch (Exception e) {
            // Ignora se não registrado
        }
    }

    private boolean findText(AccessibilityNodeInfo node, String text) {
        return searchRecursive(node, text);
    }

    private void triggerAlert() {
        android.content.SharedPreferences prefs = getSharedPreferences("AppConfig", MODE_PRIVATE);
        
        // 🔥 Garante que o status de DEV esteja atualizado antes de tentar o Firebase
        checkDevAccess();
        
        // 1. Toca Alarme
        try {
            Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);
            r.play();
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 2. Notificação do Sistema (Se habilitado)
        if (prefs.getBoolean("notif_offer_local", true)) {
            sendSystemNotification(getString(R.string.scanner_alert_title), getString(R.string.scanner_alert_content), 1001);
        }

        // 3. Notificação Global (Apenas para Desenvolvedores e se habilitado)
        if (isDeveloper && prefs.getBoolean("notif_offer_global", true)) {
            String email = prefs.getString("profile_email", "dev@example.com");
            String name = prefs.getString("profile_name", "Dev");
            FirebaseHelper.broadcastScannerAlert(email, name, getString(R.string.scanner_alert_content), true, null);
        }

        // 4. Avisa a Aba de Detecção
        broadcastLocalAlert(getString(R.string.det_route_screen), getString(R.string.scanner_alert_message));
    }

    private void sendSystemNotification(String title, String content, int id) {
        android.app.NotificationManager nm = (android.app.NotificationManager) getSystemService(android.content.Context.NOTIFICATION_SERVICE);
        
        // 🔥 Canais Dedicados para Personalização no Android
        // id 1001 = Rota (Usa canal de ALTA prioridade/som de alerta)
        // id 1002 = Sem Oferta (Usa canal de prioridade MÉDIA/som padrão)
        String channelId = (id == 1001) ? "ScannerRouteChannel" : "ScannerStatusChannel";
        String channelName = (id == 1001) ? "Scanner: Rota Detectada" : "Scanner: Status da Verificação";
        int importance = (id == 1001) ? android.app.NotificationManager.IMPORTANCE_HIGH : android.app.NotificationManager.IMPORTANCE_DEFAULT;

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            android.app.NotificationChannel channel = new android.app.NotificationChannel(channelId, channelName, importance);
            if (id == 1001) {
                channel.setDescription("Notificações disparadas quando uma nova vaga de entrega é encontrada.");
                channel.enableVibration(true);
                channel.setBypassDnd(true); // Opcional: permite furar o Não Perturbe se o usuário quiser
            } else {
                channel.setDescription("Notificações de resumo quando o ciclo termina sem ofertas.");
            }
            nm.createNotificationChannel(channel);
        }

        androidx.core.app.NotificationCompat.Builder builder = new androidx.core.app.NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.ic_map)
                .setContentTitle(title)
                .setContentText(content)
                .setPriority((id == 1001) ? androidx.core.app.NotificationCompat.PRIORITY_HIGH : androidx.core.app.NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);

        nm.notify(id, builder.build());
    }

    @Override
    public void onInterrupt() {}
}
