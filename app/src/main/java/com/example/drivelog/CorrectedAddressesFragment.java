package com.example.drivelog;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public class CorrectedAddressesFragment extends Fragment {

    private RecyclerView recyclerView;
    private CorrectedAdapter adapter;
    private TextView textEmpty;
    private EditText editSearch;
    private List<CorrectedAddress> fullList = new ArrayList<>();
    private String currentUserId;
    private boolean isDev = false;

    private void checkDevAccess() {
        FirebaseUser fUser = FirebaseAuth.getInstance().getCurrentUser();
        String myEmail = fUser != null ? fUser.getEmail() : "";
        FirebaseHelper.checkDeveloperAccess(myEmail, result -> {
            if (isAdded() && getActivity() != null) {
                getActivity().runOnUiThread(() -> this.isDev = result);
            }
        });
    }

    private final ActivityResultLauncher<Intent> importLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) processImport(uri);
                }
            }
    );

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_corrected_addresses, container, false);
        
        recyclerView = view.findViewById(R.id.recyclerCorrected);
        textEmpty = view.findViewById(R.id.textEmpty);
        editSearch = view.findViewById(R.id.editSearchCorrected);
        
        currentUserId = requireContext().getSharedPreferences("AppConfig", Context.MODE_PRIVATE).getString("current_user_id", "anon");
        
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new CorrectedAdapter(new ArrayList<>(), currentUserId, this::onDeleteClicked);
        recyclerView.setAdapter(adapter);
        
        loadCorrected();
        checkDevAccess();

        if (editSearch != null) {
            editSearch.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) { filter(s.toString()); }
                @Override public void afterTextChanged(Editable s) {}
            });
        }

        view.findViewById(R.id.btnExportCorrected).setOnClickListener(v -> showExportOptions());
        view.findViewById(R.id.btnImportCorrected).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            importLauncher.launch(intent);
        });

        View btnDeleteDownloaded = view.findViewById(R.id.btnDeleteDownloaded);
        if (btnDeleteDownloaded != null) {
            btnDeleteDownloaded.setOnClickListener(v -> confirmDeleteAllDownloaded());
        }
        
        return view;
    }

    private void confirmDeleteAllDownloaded() {
        if (fullList == null || fullList.isEmpty()) {
            Toast.makeText(getContext(), "Nenhum endereço para excluir.", Toast.LENGTH_SHORT).show();
            return;
        }

        List<CorrectedAddress> downloadedList = new ArrayList<>();
        for (CorrectedAddress addr : fullList) {
            if (addr.creatorId != null && !addr.creatorId.equals(currentUserId)) {
                downloadedList.add(addr);
            }
        }

        if (downloadedList.isEmpty()) {
            Toast.makeText(getContext(), "Nenhum endereço baixado da comunidade para excluir.", Toast.LENGTH_SHORT).show();
            return;
        }

        int count = downloadedList.size();
        new AlertDialog.Builder(requireContext())
                .setTitle("Excluir Correções Baixadas")
                .setMessage("Deseja excluir todas as " + count + " correções baixadas da comunidade?\n\nSuas correções locais e enviadas serão preservadas, e as paradas das rotas voltarão para a posição original da planilha.")
                .setPositiveButton("Excluir " + count + " Correção(ões)", (dialog, which) -> {
                    new Thread(() -> {
                        AppDao dao = AppDatabase.getInstance(requireContext()).appDao();
                        for (CorrectedAddress addr : downloadedList) {
                            dao.deleteCorrectedAddress(addr);
                            restoreStopsForDeletedAddress(dao, addr.address);
                        }
                        Activity activity = getActivity();
                        if (activity != null) {
                            activity.runOnUiThread(() -> {
                                Toast.makeText(getContext(), count + " correções baixadas excluídas e coordenadas originais restauradas!", Toast.LENGTH_SHORT).show();
                                CloudSyncHelper.syncNow(requireContext(), "Limpeza de Baixados");
                            });
                        }
                    }).start();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void loadCorrected() {
        AppDatabase.getInstance(requireContext()).appDao().getAllCorrectedAddressesLive().observe(getViewLifecycleOwner(), list -> {
            this.fullList = list;
            if (list == null || list.isEmpty()) {
                textEmpty.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.GONE);
            } else {
                textEmpty.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
                if (editSearch != null && !editSearch.getText().toString().isEmpty()) {
                    filter(editSearch.getText().toString());
                } else {
                    adapter.setList(list);
                }
            }
        });
    }

    private void filter(String query) {
        if (adapter != null) {
            adapter.filter(query);
            if (adapter.getItemCount() == 0 && !query.isEmpty()) {
                textEmpty.setVisibility(View.VISIBLE);
                textEmpty.setText("Nenhum endereço encontrado para '" + query + "'");
                recyclerView.setVisibility(View.GONE);
            } else if (fullList.isEmpty()) {
                textEmpty.setVisibility(View.VISIBLE);
                textEmpty.setText("Nenhum endereço fixado ainda.");
                recyclerView.setVisibility(View.GONE);
            } else {
                textEmpty.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
            }
        }
    }

    private void showExportOptions() {
        if (fullList.isEmpty()) {
            Toast.makeText(getContext(), "Nenhum endereço para exportar", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] options = {"Exportar Tudo", "Por Bairro/Pasta", "Selecionar Específico"};
        new AlertDialog.Builder(requireContext())
                .setTitle("Exportar Endereços")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) exportAddresses(fullList, "todos_enderecos");
                    else if (which == 1) showNeighborhoodExportPicker();
                    else showSpecificExportPicker();
                })
                .show();
    }

    private void showNeighborhoodExportPicker() {
        Map<String, List<CorrectedAddress>> grouped = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (CorrectedAddress a : fullList) {
            String key = a.neighborhood != null ? a.neighborhood : "Sem Bairro";
            if (!grouped.containsKey(key)) grouped.put(key, new ArrayList<>());
            grouped.get(key).add(a);
        }
        
        String[] names = grouped.keySet().toArray(new String[0]);
        new AlertDialog.Builder(requireContext())
                .setTitle("Escolha o Bairro")
                .setItems(names, (dialog, which) -> {
                    String selected = names[which];
                    exportAddresses(grouped.get(selected), "bairro_" + selected.replaceAll("[^a-zA-Z0-9]", "_"));
                })
                .show();
    }

    private void showSpecificExportPicker() {
        String[] addresses = new String[fullList.size()];
        boolean[] checked = new boolean[fullList.size()];
        for (int i = 0; i < fullList.size(); i++) addresses[i] = fullList.get(i).address;

        List<CorrectedAddress> selected = new ArrayList<>();
        new AlertDialog.Builder(requireContext())
                .setTitle("Selecione os Endereços")
                .setMultiChoiceItems(addresses, checked, (dialog, which, isChecked) -> {
                    if (isChecked) selected.add(fullList.get(which));
                    else selected.remove(fullList.get(which));
                })
                .setPositiveButton("Exportar", (dialog, which) -> {
                    if (!selected.isEmpty()) exportAddresses(selected, "enderecos_selecionados");
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void exportAddresses(List<CorrectedAddress> list, String filename) {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("address;neighborhood;city;latitude;longitude;updatedAt\n");
            for (CorrectedAddress a : list) {
                sb.append(a.address).append(";")
                  .append(a.neighborhood != null ? a.neighborhood : "").append(";")
                  .append(a.city != null ? a.city : "").append(";")
                  .append(a.latitude).append(";")
                  .append(a.longitude).append(";")
                  .append(a.updatedAt).append("\n");
            }

            File cacheFile = new File(requireContext().getCacheDir(), filename + ".dlf");
            try (FileOutputStream out = new FileOutputStream(cacheFile)) {
                out.write(sb.toString().getBytes());
            }

            Uri contentUri = FileProvider.getUriForFile(requireContext(), requireContext().getPackageName() + ".fileprovider", cacheFile);
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("application/octet-stream");
            intent.putExtra(Intent.EXTRA_STREAM, contentUri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(intent, "Compartilhar Endereços"));

        } catch (Exception e) {
            Toast.makeText(getContext(), "Erro ao exportar: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void processImport(Uri uri) {
        try {
            List<CorrectedAddress> imported = new ArrayList<>();
            InputStream in = requireContext().getContentResolver().openInputStream(uri);
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            String line;
            boolean first = true;
            while ((line = reader.readLine()) != null) {
                if (first) { first = false; continue; }
                String[] p = line.split(";");
                if (p.length >= 5) {
                    CorrectedAddress ca = new CorrectedAddress();
                    ca.address = p[0];
                    ca.neighborhood = p[1].isEmpty() ? null : p[1];
                    ca.city = p[2].isEmpty() ? null : p[2];
                    ca.latitude = Double.parseDouble(p[3]);
                    ca.longitude = Double.parseDouble(p[4]);
                    ca.updatedAt = p.length > 5 ? Long.parseLong(p[5]) : System.currentTimeMillis();
                    imported.add(ca);
                } else if (p.length == 4) { // Retrocompatibilidade
                    CorrectedAddress ca = new CorrectedAddress();
                    ca.address = p[0];
                    ca.neighborhood = p[1].isEmpty() ? null : p[1];
                    ca.latitude = Double.parseDouble(p[2]);
                    ca.longitude = Double.parseDouble(p[3]);
                    ca.updatedAt = System.currentTimeMillis();
                    imported.add(ca);
                }
            }
            reader.close();

            if (imported.isEmpty()) {
                Toast.makeText(getContext(), "Arquivo inválido ou vazio", Toast.LENGTH_SHORT).show();
                return;
            }

            showImportMixDialog(imported);

        } catch (Exception e) {
            Toast.makeText(getContext(), "Erro ao ler arquivo", Toast.LENGTH_SHORT).show();
        }
    }

    private void showImportMixDialog(List<CorrectedAddress> imported) {
        String[] display = new String[imported.size()];
        boolean[] checked = new boolean[imported.size()];
        for (int i = 0; i < imported.size(); i++) {
            CorrectedAddress a = imported.get(i);
            display[i] = (a.neighborhood != null ? "[" + a.neighborhood + "] " : "") + a.address;
            checked[i] = true;
        }

        Set<CorrectedAddress> selected = new HashSet<>(imported);

        new AlertDialog.Builder(requireContext())
                .setTitle("Escolha o que importar")
                .setMultiChoiceItems(display, checked, (dialog, which, isChecked) -> {
                    if (isChecked) selected.add(imported.get(which));
                    else selected.remove(imported.get(which));
                })
                .setPositiveButton("Importar e Mixar", (dialog, which) -> {
                    new Thread(() -> {
                        AppDao dao = AppDatabase.getInstance(requireContext()).appDao();
                        for (CorrectedAddress ca : selected) {
                            dao.insertCorrectedAddress(ca);
                        }
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                Toast.makeText(getContext(), "Endereços mixados com sucesso!", Toast.LENGTH_SHORT).show();
                                CloudSyncHelper.syncNow(requireContext(), "Endereços Importados");
                            });
                        }
                    }).start();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void onDeleteClicked(CorrectedAddress corrected) {
        boolean isAlreadyShared = corrected.creatorId != null && corrected.creatorId.equals(currentUserId);
        String coordsInfo = String.format(Locale.US, "📍 Lat: %.6f\n📍 Lon: %.6f", corrected.latitude, corrected.longitude);
        
        List<String> optionsList = new ArrayList<>();
        optionsList.add(coordsInfo);
        optionsList.add("Mover para Bairro/Pasta");
        optionsList.add("Remover Fixação");
        if (!isAlreadyShared) {
            optionsList.add("📤 Compartilhar com a Comunidade");
        }
        if (isDev || isAlreadyShared) {
            if (isAlreadyShared && !isDev) {
                optionsList.add("🗑️ Excluir Minha Correção da Comunidade");
            } else if (isDev && !isAlreadyShared) {
                optionsList.add("🗑️ Excluir da Comunidade (Dev)");
            } else {
                optionsList.add("🗑️ Excluir da Comunidade");
            }
        }

        String[] options = optionsList.toArray(new String[0]);
        
        new AlertDialog.Builder(requireContext())
                .setTitle(corrected.address)
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        try {
                            Uri gmmIntentUri = Uri.parse("geo:" + corrected.latitude + "," + corrected.longitude + "?q=" + corrected.latitude + "," + corrected.longitude + "(" + Uri.encode(corrected.address) + ")");
                            Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                            mapIntent.setPackage("com.google.android.apps.maps");
                            startActivity(mapIntent);
                        } catch (Exception e) {
                            Toast.makeText(getContext(), "Google Maps não encontrado", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        String selected = options[which];
                        if (selected.equals("Mover para Bairro/Pasta")) {
                            showMoveToNeighborhoodDialog(corrected);
                        } else if (selected.equals("Remover Fixação")) {
                            confirmDelete(corrected);
                        } else if (selected.contains("Compartilhar")) {
                            shareWithCommunity(corrected, currentUserId);
                        } else if (selected.contains("Excluir da Comunidade")) {
                            confirmDeleteFromCommunity(corrected);
                        }
                    }
                })
                .setNegativeButton("Fechar", null)
                .show();
    }

    private void confirmDeleteFromCommunity(CorrectedAddress corrected) {
        boolean isMine = corrected.creatorId != null && corrected.creatorId.equals(currentUserId);
        String message = (isDev && !isMine) 
                ? "Você é desenvolvedor. Deseja excluir permanentemente o endereço \"" + corrected.address + "\" da comunidade?"
                : "Deseja excluir permanentemente a sua correção do endereço \"" + corrected.address + "\" da comunidade?";

        new AlertDialog.Builder(requireContext())
                .setTitle("Excluir da Comunidade")
                .setMessage(message)
                .setPositiveButton("Excluir", (dialog, which) -> {
                    FirebaseHelper.deleteAddressFromCommunity(corrected.address, new FirebaseHelper.GlobalUploadCallback() {
                        @Override
                        public void onSuccess() {
                            new Thread(() -> {
                                AppDao dao = AppDatabase.getInstance(requireContext()).appDao();
                                CorrectedAddress local = dao.getCorrectedAddress(corrected.address);
                                if (local != null) dao.deleteCorrectedAddress(local);
                                restoreStopsForDeletedAddress(dao, corrected.address);
                                if (getActivity() != null) {
                                    getActivity().runOnUiThread(() -> 
                                        Toast.makeText(getContext(), "Endereço excluído da comunidade e posição original restaurada!", Toast.LENGTH_SHORT).show()
                                    );
                                }
                            }).start();
                        }

                        @Override
                        public void onFailure(String msg) {
                            if (getActivity() != null) {
                                getActivity().runOnUiThread(() -> 
                                    Toast.makeText(getContext(), "Erro ao excluir da comunidade: " + msg, Toast.LENGTH_SHORT).show()
                                );
                            }
                        }
                    });
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void shareWithCommunity(CorrectedAddress corrected, String currentUserId) {
        String uName = requireContext().getSharedPreferences("AppConfig", Context.MODE_PRIVATE).getString("profile_name", "Entregador");
        FirebaseHelper.uploadCorrection(currentUserId, uName, corrected, new FirebaseHelper.GlobalUploadCallback() {
            @Override
            public void onSuccess() {
                new Thread(() -> {
                    corrected.creatorId = currentUserId;
                    AppDatabase.getInstance(requireContext()).appDao().updateCorrectedAddress(corrected);
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> Toast.makeText(getContext(), "Compartilhado com sucesso!", Toast.LENGTH_SHORT).show());
                    }
                }).start();
            }

            @Override
            public void onFailure(String msg) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (msg != null && msg.startsWith("ALREADY_CORRECTED_BY_OTHER")) {
                            String[] parts = msg.split(":");
                            String creatorName = (parts.length > 1 && !parts[1].isEmpty()) ? parts[1] : "outro entregador";
                            promptSubstitutionRequestDialog(corrected, creatorName, currentUserId, uName);
                        } else {
                            Toast.makeText(getContext(), "Erro ao compartilhar: " + msg, Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        });
    }

    private void promptSubstitutionRequestDialog(CorrectedAddress newAddr, String creatorName, String userId, String userName) {
        if (getContext() == null || newAddr == null) return;

        View v = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_modern_confirm, null);
        TextView title = v.findViewById(R.id.textModernTitle);
        TextView message = v.findViewById(R.id.textModernMessage);
        MaterialButton btnCancel = v.findViewById(R.id.btnModernNegative);
        MaterialButton btnConfirm = v.findViewById(R.id.btnModernPositive);

        if (title != null) title.setText("Endereço Já Corrigido");
        if (message != null) {
            message.setText("O endereço \"" + newAddr.address + "\" já possui uma correção na comunidade enviada por " 
                    + (creatorName != null && !creatorName.isEmpty() ? creatorName : "outro entregador") 
                    + ".\n\nPara evitar divergências na comunidade, você não pode sobrescrevê-lo diretamente. Deseja enviar um pedido de substituição para análise de um desenvolvedor?");
        }

        if (btnCancel != null) btnCancel.setText("CANCELAR");
        if (btnConfirm != null) btnConfirm.setText("SOLICITAR SUBSTITUIÇÃO");

        AlertDialog dialog = new AlertDialog.Builder(requireContext()).setView(v).create();
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        if (btnCancel != null) btnCancel.setOnClickListener(v2 -> dialog.dismiss());
        if (btnConfirm != null) {
            btnConfirm.setOnClickListener(v2 -> {
                dialog.dismiss();
                promptSubstitutionReasonDialog(newAddr, userId, userName);
            });
        }

        dialog.show();
    }

    private void promptSubstitutionReasonDialog(CorrectedAddress newAddr, String userId, String userName) {
        View v = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_community_comments, null);
        AlertDialog dialog = new AlertDialog.Builder(requireContext()).setView(v).create();
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        EditText editReason = v.findViewById(R.id.editCommentInput);
        View btnSend = v.findViewById(R.id.btnSendComment);
        View btnCancel = v.findViewById(R.id.btnCancelComments);

        if (editReason != null) {
            editReason.setHint("Explique o motivo da alteração (ex: portão correto fica na rua de trás)...");
        }

        if (btnSend != null) {
            btnSend.setOnClickListener(v2 -> {
                String reason = editReason != null ? editReason.getText().toString().trim() : "";
                if (reason.isEmpty()) {
                    reason = "Solicitação de substituição de localização";
                }
                FirebaseHelper.requestAddressSubstitution(newAddr, userId, userName, reason, new FirebaseHelper.GlobalUploadCallback() {
                    @Override
                    public void onSuccess() {
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                Toast.makeText(getContext(), "Solicitação de substituição enviada para análise!", Toast.LENGTH_LONG).show();
                                dialog.dismiss();
                            });
                        }
                    }

                    @Override
                    public void onFailure(String msg) {
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> Toast.makeText(getContext(), "Erro ao solicitar: " + msg, Toast.LENGTH_SHORT).show());
                        }
                    }
                });
            });
        }

        if (btnCancel != null) btnCancel.setOnClickListener(v2 -> dialog.dismiss());
        dialog.show();
    }

    private void showMoveToNeighborhoodDialog(CorrectedAddress corrected) {
        new Thread(() -> {
            List<CorrectedAddress> all = AppDatabase.getInstance(requireContext()).appDao().getAllCorrectedAddresses();
            Set<String> neighborhoods = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
            for (CorrectedAddress addr : all) {
                if (addr.neighborhood != null && !addr.neighborhood.isEmpty()) {
                    neighborhoods.add(addr.neighborhood);
                }
            }
            List<String> neighborhoodList = new ArrayList<>(neighborhoods);

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    LinearLayout layout = new LinearLayout(requireContext());
                    layout.setOrientation(LinearLayout.VERTICAL);
                    layout.setPadding(50, 40, 50, 10);

                    final AutoCompleteTextView input = new AutoCompleteTextView(requireContext());
                    input.setHint("Nome do Bairro ou Pasta");
                    input.setText(corrected.neighborhood != null ? corrected.neighborhood : "");
                    
                    ArrayAdapter<String> suggestAdapter = new ArrayAdapter<>(requireContext(),
                            android.R.layout.simple_dropdown_item_1line, neighborhoodList);
                    input.setAdapter(suggestAdapter);
                    input.setThreshold(1); // Sugere ao digitar 1 letra

                    layout.addView(input);

                    new AlertDialog.Builder(requireContext())
                            .setTitle("Organizar em Pasta")
                            .setMessage(neighborhoodList.isEmpty() ? 
                                    "Digite o nome da pasta para agrupar." : 
                                    "Escolha uma pasta existente ou digite uma nova.")
                            .setView(layout)
                            .setPositiveButton("Mover", (d, w) -> {
                                String neighborhood = input.getText().toString().trim();
                                new Thread(() -> {
                                    corrected.neighborhood = neighborhood.isEmpty() ? null : neighborhood;
                                    AppDatabase.getInstance(requireContext()).appDao().updateCorrectedAddress(corrected);
                                    if (getActivity() != null) {
                                        getActivity().runOnUiThread(() -> CloudSyncHelper.syncNow(requireContext()));
                                    }
                                }).start();
                            })
                            .setNegativeButton("Cancelar", null)
                            .show();
                });
            }
        }).start();
    }

    private void confirmDelete(CorrectedAddress corrected) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Remover Fixação")
                .setMessage("Deseja remover a correção deste endereço e voltar para a localização original da planilha?")
                .setPositiveButton("Remover", (d, w) -> {
                    new Thread(() -> {
                        AppDao dao = AppDatabase.getInstance(requireContext()).appDao();
                        dao.deleteCorrectedAddress(corrected);
                        restoreStopsForDeletedAddress(dao, corrected.address);
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                Toast.makeText(getContext(), "Correção removida e localização original restaurada!", Toast.LENGTH_SHORT).show();
                                CloudSyncHelper.syncNow(requireContext(), "Fixação Removida");
                            });
                        }
                    }).start();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    public static void restoreStopsForDeletedAddress(AppDao dao, String addressText) {
        if (dao == null || addressText == null || addressText.isEmpty()) return;
        List<RouteStop> allStops = dao.getAllRouteStops();
        if (allStops == null || allStops.isEmpty()) return;

        String normTarget = normalizeAddress(addressText);

        for (RouteStop stop : allStops) {
            if (stop.originalLatitude == 0.0 && stop.originalLongitude == 0.0) continue;

            String normStopAddr = stop.address != null ? normalizeAddress(stop.address) : "";
            String normAllAddr = stop.allAddresses != null ? normalizeAddress(stop.allAddresses) : "";

            boolean matches = (normStopAddr.contains(normTarget) || normTarget.contains(normStopAddr)
                    || normAllAddr.contains(normTarget) || normTarget.contains(normAllAddr));

            if (matches) {
                CorrectedAddress remainingCorr = dao.getCorrectedAddress(stop.address);
                if (remainingCorr != null) {
                    stop.latitude = remainingCorr.latitude;
                    stop.longitude = remainingCorr.longitude;
                } else {
                    stop.latitude = stop.originalLatitude;
                    stop.longitude = stop.originalLongitude;
                }
                dao.updateRouteStop(stop);
            }
        }
    }

    private static String normalizeAddress(String input) {
        if (input == null) return "";
        return Normalizer.normalize(input.toUpperCase(), Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .replaceAll("[.,\\-]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static class CorrectedAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private static final int TYPE_CITY = 0;
        private static final int TYPE_NEIGHBORHOOD = 1;
        private static final int TYPE_ITEM = 2;

        private final List<Object> displayList = new ArrayList<>();
        private final OnDeleteListener listener;
        private final String currentUserId;
        private List<CorrectedAddress> originalList = new ArrayList<>();
        private String currentFilter = "";

        interface OnDeleteListener { void onDelete(CorrectedAddress item); }

        CorrectedAdapter(List<CorrectedAddress> list, String currentUserId, OnDeleteListener listener) {
            this.listener = listener;
            this.currentUserId = currentUserId;
            setList(list);
        }

        void setList(List<CorrectedAddress> newList) {
            this.originalList = newList;
            rebuildDisplayList();
        }

        void filter(String query) {
            this.currentFilter = query.toLowerCase().trim();
            rebuildDisplayList();
        }

        private void rebuildDisplayList() {
            displayList.clear();
            
            // Map<City, Map<Neighborhood, List<CorrectedAddress>>>
            Map<String, Map<String, List<CorrectedAddress>>> hierarchy = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

            for (CorrectedAddress addr : originalList) {
                if (!currentFilter.isEmpty()) {
                    boolean matches = (addr.address != null && addr.address.toLowerCase().contains(currentFilter)) ||
                            (addr.neighborhood != null && addr.neighborhood.toLowerCase().contains(currentFilter)) ||
                            (addr.city != null && addr.city.toLowerCase().contains(currentFilter));
                    if (!matches) continue;
                }

                String cityKey = (addr.city != null && !addr.city.isEmpty()) ? addr.city : "Minha Cidade";
                String neighborhoodKey = (addr.neighborhood != null && !addr.neighborhood.isEmpty()) ? addr.neighborhood : "Sem Bairro / Diversos";
                
                if (!hierarchy.containsKey(cityKey)) hierarchy.put(cityKey, new TreeMap<>(String.CASE_INSENSITIVE_ORDER));
                Map<String, List<CorrectedAddress>> cityMap = hierarchy.get(cityKey);
                
                if (!cityMap.containsKey(neighborhoodKey)) cityMap.put(neighborhoodKey, new ArrayList<>());
                cityMap.get(neighborhoodKey).add(addr);
            }

            for (Map.Entry<String, Map<String, List<CorrectedAddress>>> cityEntry : hierarchy.entrySet()) {
                displayList.add(new CityHeader(cityEntry.getKey()));
                for (Map.Entry<String, List<CorrectedAddress>> nbEntry : cityEntry.getValue().entrySet()) {
                    displayList.add(new NeighborhoodHeader(nbEntry.getKey()));
                    displayList.addAll(nbEntry.getValue());
                }
            }
            notifyDataSetChanged();
        }

        @Override public int getItemViewType(int position) {
            Object obj = displayList.get(position);
            if (obj instanceof CityHeader) return TYPE_CITY;
            if (obj instanceof NeighborhoodHeader) return TYPE_NEIGHBORHOOD;
            return TYPE_ITEM;
        }

        @NonNull @Override public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            if (viewType == TYPE_CITY) {
                View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_community_city, parent, false);
                return new CityViewHolder(v);
            } else if (viewType == TYPE_NEIGHBORHOOD) {
                View v = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_1, parent, false);
                v.setPadding(32, 0, 0, 0); // Indentação para bairro
                v.setBackgroundColor(0xFFF0F0F0);
                return new NeighborhoodViewHolder(v);
            } else {
                View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_my_address, parent, false);
                return new ItemViewHolder(v);
            }
        }

        @Override public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            Object obj = displayList.get(position);
            if (holder instanceof CityViewHolder) {
                CityHeader header = (CityHeader) obj;
                CityViewHolder h = (CityViewHolder) holder;
                h.text.setText(header.name);
                h.imgFav.setVisibility(View.GONE); // Não usamos favoritos na aba 'Meus'
            } else if (holder instanceof NeighborhoodViewHolder) {
                NeighborhoodHeader header = (NeighborhoodHeader) obj;
                NeighborhoodViewHolder h = (NeighborhoodViewHolder) holder;
                h.text.setText(" bairro: " + header.name);
                h.text.setTextSize(13);
                h.text.setTextColor(0xFF888888);
            } else if (holder instanceof ItemViewHolder) {
                CorrectedAddress item = (CorrectedAddress) obj;
                ItemViewHolder h = (ItemViewHolder) holder;
                h.textAddress.setText(item.address);
                
                String statusText = "";
                int color = 0xFF888888;
                if (item.creatorId == null) {
                    statusText = "Local (Não enviado)";
                    color = 0xFF4CAF50; // Verde
                } else if (item.creatorId.equals(currentUserId)) {
                    statusText = "Comunidade (Sincronizado)";
                    color = 0xFF2196F3; // Azul
                } else {
                    statusText = "Comunidade (Baixado)";
                    color = 0xFFFF9800; // Laranja
                }
                
                h.textStatus.setText(statusText);
                h.textStatus.setTextColor(color);
                
                String coords = "Lat: " + String.format("%.6f", item.latitude) + " | Lon: " + String.format("%.6f", item.longitude);
                h.textCoords.setText(coords);

                h.imgMenu.setOnClickListener(v -> listener.onDelete(item));
                h.itemView.setOnClickListener(v -> listener.onDelete(item));
            }
        }

        @Override public int getItemCount() { return displayList.size(); }

        static class CityHeader { String name; CityHeader(String name) { this.name = name; } }
        static class NeighborhoodHeader { String name; NeighborhoodHeader(String name) { this.name = name; } }

        static class CityViewHolder extends RecyclerView.ViewHolder {
            TextView text; ImageView imgFav;
            CityViewHolder(View v) { super(v); text = v.findViewById(R.id.textCityName); imgFav = v.findViewById(R.id.imgCityFav); }
        }

        static class NeighborhoodViewHolder extends RecyclerView.ViewHolder {
            TextView text;
            NeighborhoodViewHolder(View v) { super(v); text = v.findViewById(android.R.id.text1); }
        }

        static class ItemViewHolder extends RecyclerView.ViewHolder {
            TextView textAddress, textStatus, textCoords;
            ImageView imgMenu;
            ItemViewHolder(View v) {
                super(v);
                textAddress = v.findViewById(R.id.textMyAddress);
                textStatus = v.findViewById(R.id.textMyStatus);
                textCoords = v.findViewById(R.id.textMyCoords);
                imgMenu = v.findViewById(R.id.imgActionMenu);
            }
        }
    }
}
