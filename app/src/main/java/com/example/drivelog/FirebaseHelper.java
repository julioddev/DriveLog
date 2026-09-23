package com.example.drivelog;

import android.util.Log;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.auth.FirebaseAuth;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;

public class FirebaseHelper {

    private static final String COLLECTION_GLOBAL_FIX = "global_corrected_addresses";
    private static final String COLLECTION_GLOBAL_QUADRAS = "global_quadras";
    private static final String COLLECTION_DELETION_REQUESTS = "deletion_requests";
    private static final String COLLECTION_USERS = "users";
    private static final String COLLECTION_USERNAMES = "usernames";
    private static final String COLLECTION_FRIEND_REQUESTS = "friend_requests";
    private static final String COLLECTION_DEV_ROUTES = "developer_shared_routes";
    private static final String COLLECTION_DEV_RECORDINGS = "developer_shared_recordings";
    private static final String COLLECTION_DEV_ALERTS = "developer_scanner_alerts";

    private static final String COLLECTION_HAZARDS = "community_hazards";

    public interface UserMetadataCallback {
        void onSuccess(long installDate, int subType);
        void onError(String msg);
    }

    public static void syncUserMetadata(String userId, long localInstallDate, int localSubType, UserMetadataCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String sanitizedEmail = userId.trim().toLowerCase();
        DocumentReference userRef = db.collection(COLLECTION_USERS).document(sanitizedEmail);

        userRef.get().addOnSuccessListener(doc -> {
            Map<String, Object> data = new HashMap<>();
            data.put("email", sanitizedEmail);
            data.put("displayName", FirebaseAuth.getInstance().getCurrentUser() != null ? 
                    FirebaseAuth.getInstance().getCurrentUser().getDisplayName() : "Entregador");
            data.put("lastSeen", FieldValue.serverTimestamp());

            if (doc.exists()) {
                // Usuário já existe, recuperamos dados e atualizamos e-mail/nome para busca
                Long cloudDate = doc.getLong("installDate");
                Long cloudSub = doc.getLong("subType");
                
                userRef.set(data, SetOptions.merge());

                if (callback != null) {
                    callback.onSuccess(
                        cloudDate != null ? cloudDate : localInstallDate,
                        cloudSub != null ? cloudSub.intValue() : localSubType
                    );
                }
            } else {
                // Usuário novo, inicializa tudo
                data.put("installDate", localInstallDate);
                data.put("subType", localSubType);
                data.put("comboioMode", 2); // 🔥 PADRÃO: Invisível ao instalar
                userRef.set(data);
                if (callback != null) callback.onSuccess(localInstallDate, localSubType);
            }
        }).addOnFailureListener(e -> {
            if (callback != null) callback.onError(e.getMessage());
        });
    }

    public interface GlobalCorrectionCallback {
        void onResult(double lat, double lon, int likes, int dislikes, String creatorId, String publicNote, int commentCount, String creatorName, long updateDate);
        void onError(String msg);
    }

    public interface GlobalUploadCallback {
        void onSuccess();
        void onFailure(String msg);
    }

    public static void uploadCorrection(String userId, String userName, CorrectedAddress addr, GlobalUploadCallback callback) {
        if (addr == null || addr.address == null) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String docId = sanitizeAddressId(addr.address);

        Map<String, Object> data = new HashMap<>();
        data.put("address", addr.address);
        data.put("latitude", addr.latitude);
        data.put("longitude", addr.longitude);
        data.put("neighborhood", addr.neighborhood);
        data.put("city", addr.city != null ? addr.city : "Cidade não informada");
        data.put("lastUpdate", FieldValue.serverTimestamp());
        data.put("creatorId", userId);
        data.put("creatorName", userName);
        
        // Gerencia a nota pública: envia ou remove
        if (addr.isNotePublic && addr.notes != null && !addr.notes.trim().isEmpty()) {
            data.put("publicNote", addr.notes);
            data.put("publicNoteDate", FieldValue.serverTimestamp());
        } else {
            data.put("publicNote", FieldValue.delete());
            data.put("publicNoteDate", FieldValue.delete());
        }

        // Inicializa contadores apenas se o documento for novo
        db.collection(COLLECTION_GLOBAL_FIX).document(docId).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        data.put("likes", 0);
                        data.put("dislikes", 0);
                        data.put("commentCount", 0);
                    }
                    db.collection(COLLECTION_GLOBAL_FIX).document(docId)
                            .set(data, SetOptions.merge())
                            .addOnSuccessListener(aVoid -> {
                                Log.d("FirebaseHelper", "Endereço enviado com sucesso: " + docId);
                                if (callback != null) callback.onSuccess();
                            })
                            .addOnFailureListener(e -> {
                                Log.e("FirebaseHelper", "Erro no envio: " + e.getMessage());
                                if (callback != null) callback.onFailure(e.getMessage());
                            });
                });
    }

    public static void searchGlobal(String address, GlobalCorrectionCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String docId = sanitizeAddressId(address);

        db.collection(COLLECTION_GLOBAL_FIX).document(docId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        Double lat = doc.getDouble("latitude");
                        Double lon = doc.getDouble("longitude");
                        Long likes = doc.getLong("likes");
                        Long dislikes = doc.getLong("dislikes");
                        String creatorId = doc.getString("creatorId");
                        String creatorName = doc.getString("creatorName");
                        String publicNote = doc.getString("publicNote");
                        Long commentCount = doc.getLong("commentCount");
                        Timestamp ts = doc.getTimestamp("publicNoteDate");
                        if (ts == null) ts = doc.getTimestamp("lastUpdate");
                        long updateDate = ts != null ? ts.toDate().getTime() : 0;

                        if (lat != null && lon != null) {
                            callback.onResult(lat, lon, 
                                (likes != null ? likes.intValue() : 0), 
                                (dislikes != null ? dislikes.intValue() : 0),
                                creatorId, publicNote,
                                (commentCount != null ? commentCount.intValue() : 0),
                                (creatorName != null ? creatorName : "Entregador"),
                                updateDate);
                        } else {
                            callback.onError("Dados incompletos");
                        }
                    } else {
                        callback.onError("Não encontrado");
                    }
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public static void addFeedback(String address, Boolean isLike, String comment, String userName, String userId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String docId = sanitizeAddressId(address);
        DocumentReference docRef = db.collection(COLLECTION_GLOBAL_FIX).document(docId);
        DocumentReference userVoteRef = docRef.collection("user_votes").document(userId);

        if (isLike != null) {
            db.runTransaction(transaction -> {
                DocumentSnapshot addressDoc = transaction.get(docRef);
                DocumentSnapshot voteDoc = transaction.get(userVoteRef);
                
                long currentLikes = 0;
                if (addressDoc.exists() && addressDoc.get("likes") != null) {
                    currentLikes = addressDoc.getLong("likes");
                }
                long currentDislikes = 0;
                if (addressDoc.exists() && addressDoc.get("dislikes") != null) {
                    currentDislikes = addressDoc.getLong("dislikes");
                }

                int likeDelta = 0;
                int dislikeDelta = 0;

                if (voteDoc.exists()) {
                    Boolean oldIsLike = voteDoc.getBoolean("isLike");
                    if (oldIsLike != null) {
                        if (oldIsLike.equals(isLike)) {
                            // Desfazer o mesmo voto
                            if (isLike) likeDelta = -1; else dislikeDelta = -1;
                            transaction.delete(userVoteRef);
                        } else {
                            // Inverter o voto
                            if (isLike) { likeDelta = 1; dislikeDelta = -1; }
                            else { likeDelta = -1; dislikeDelta = 1; }
                            Map<String, Object> vData = new HashMap<>();
                            vData.put("isLike", isLike);
                            vData.put("timestamp", FieldValue.serverTimestamp());
                            transaction.set(userVoteRef, vData);
                        }
                    }
                } else {
                    // Novo voto
                    if (isLike) likeDelta = 1; else dislikeDelta = 1;
                    Map<String, Object> vData = new HashMap<>();
                    vData.put("isLike", isLike);
                    vData.put("timestamp", FieldValue.serverTimestamp());
                    transaction.set(userVoteRef, vData);
                }

                Map<String, Object> addrData = new HashMap<>();
                if (!addressDoc.exists()) {
                    addrData.put("address", address);
                    addrData.put("city", "Cidade não informada");
                    addrData.put("commentCount", 0);
                }
                
                long newLikes = Math.max(0, currentLikes + likeDelta);
                long newDislikes = Math.max(0, currentDislikes + dislikeDelta);
                
                addrData.put("likes", newLikes);
                addrData.put("dislikes", newDislikes);
                
                transaction.set(docRef, addrData, SetOptions.merge());
                return null;
            }).addOnFailureListener(e -> Log.e("FirebaseHelper", "Voto falhou: " + e.getMessage()));
        }

        if (comment != null && !comment.trim().isEmpty()) {
            Map<String, Object> commentData = new HashMap<>();
            commentData.put("text", comment);
            commentData.put("user", userName);
            commentData.put("userId", userId);
            commentData.put("date", FieldValue.serverTimestamp());
            docRef.collection("comments").add(commentData)
                .addOnSuccessListener(ref -> docRef.update("commentCount", FieldValue.increment(1)));
        }
    }

    private static String sanitizeAddressId(String address) {
        if (address == null) return "unknown";
        return address.trim().toLowerCase()
                .replace(" ", "_")
                .replaceAll("[^a-z0-9_]", "");
    }

    public interface CommunityFetchCallback {
        void onSuccess(List<CorrectedAddress> list);
        void onError(String msg);
    }

    public interface CommentsFetchCallback {
        class CommentModel {
            public String id, user, userId, text;
            public long date;
            public CommentModel(String id, String user, String userId, String text, long date) {
                this.id = id; this.user = user; this.userId = userId; this.text = text; this.date = date;
            }
        }
        void onSuccess(List<CommentModel> comments);
        void onError(String msg);
    }

    public static void fetchComments(String address, CommentsFetchCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String docId = sanitizeAddressId(address);
        db.collection(COLLECTION_GLOBAL_FIX).document(docId).collection("comments")
                .orderBy("date", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<CommentsFetchCallback.CommentModel> list = new ArrayList<>();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        String id = doc.getId();
                        String user = doc.getString("user");
                        String userId = doc.getString("userId");
                        String text = doc.getString("text");
                        Timestamp ts = doc.getTimestamp("date");
                        long date = ts != null ? ts.toDate().getTime() : 0;
                        if (user != null && text != null) {
                            list.add(new CommentsFetchCallback.CommentModel(id, user, userId, text, date));
                        }
                    }
                    callback.onSuccess(list);
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public static void updateComment(String address, String commentId, String newText, GlobalUploadCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String docId = sanitizeAddressId(address);
        db.collection(COLLECTION_GLOBAL_FIX).document(docId).collection("comments").document(commentId)
                .update("text", newText)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    public static void deleteComment(String address, String commentId, GlobalUploadCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String docId = sanitizeAddressId(address);
        DocumentReference addrRef = db.collection(COLLECTION_GLOBAL_FIX).document(docId);
        addrRef.collection("comments").document(commentId).delete()
                .addOnSuccessListener(aVoid -> {
                    addrRef.update("commentCount", FieldValue.increment(-1));
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    public static ListenerRegistration listenCommunityAddresses(CommunityFetchCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        return db.collection(COLLECTION_GLOBAL_FIX)
                .addSnapshotListener((queryDocumentSnapshots, e) -> {
                    if (e != null) {
                        callback.onError(e.getMessage());
                        return;
                    }
                    if (queryDocumentSnapshots != null) {
                        Log.d("FirebaseHelper", "Recebidos " + queryDocumentSnapshots.size() + " endereços da comunidade.");
                        List<CorrectedAddress> list = new ArrayList<>();
                        for (DocumentSnapshot doc : queryDocumentSnapshots) {
                            CorrectedAddress ca = new CorrectedAddress();
                            ca.address = doc.getString("address");
                            ca.neighborhood = doc.getString("neighborhood");
                            ca.city = doc.getString("city");
                            ca.latitude = doc.getDouble("latitude") != null ? doc.getDouble("latitude") : 0;
                            ca.longitude = doc.getDouble("longitude") != null ? doc.getDouble("longitude") : 0;
                            ca.creatorId = doc.getString("creatorId");
                            
                            Long l = doc.getLong("likes");
                            ca.likes = l != null ? l.intValue() : 0;
                            Long dl = doc.getLong("dislikes");
                            ca.dislikes = dl != null ? dl.intValue() : 0;
                            Long cc = doc.getLong("commentCount");
                            ca.commentCount = cc != null ? cc.intValue() : 0;

                            list.add(ca);
                        }
                        callback.onSuccess(list);
                    }
                });
    }

    public static void fetchCommunityAddresses(CommunityFetchCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection(COLLECTION_GLOBAL_FIX)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<CorrectedAddress> list = new ArrayList<>();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        CorrectedAddress ca = new CorrectedAddress();
                        ca.address = doc.getString("address");
                        ca.neighborhood = doc.getString("neighborhood");
                        ca.city = doc.getString("city");
                        ca.latitude = doc.getDouble("latitude") != null ? doc.getDouble("latitude") : 0;
                        ca.longitude = doc.getDouble("longitude") != null ? doc.getDouble("longitude") : 0;
                        ca.creatorId = doc.getString("creatorId");
                        
                        Long l = doc.getLong("likes");
                        ca.likes = l != null ? l.intValue() : 0;
                        Long dl = doc.getLong("dislikes");
                        ca.dislikes = dl != null ? dl.intValue() : 0;
                        Long cc = doc.getLong("commentCount");
                        ca.commentCount = cc != null ? cc.intValue() : 0;

                        list.add(ca);
                    }
                    callback.onSuccess(list);
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public static void deletePublicNote(String address, GlobalUploadCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String docId = sanitizeAddressId(address);
        
        Map<String, Object> updates = new HashMap<>();
        updates.put("publicNote", FieldValue.delete());
        
        db.collection(COLLECTION_GLOBAL_FIX).document(docId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    if (callback != null) callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onFailure(e.getMessage());
                });
    }

    public static void requestAddressDeletion(CorrectedAddress addr, String userId, String reason, GlobalUploadCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String docId = sanitizeAddressId(addr.address);
        
        Map<String, Object> requestData = new HashMap<>();
        requestData.put("address", addr.address);
        requestData.put("neighborhood", addr.neighborhood);
        requestData.put("city", addr.city);
        requestData.put("latitude", addr.latitude);
        requestData.put("longitude", addr.longitude);
        requestData.put("creatorId", addr.creatorId);
        requestData.put("requesterId", userId);
        requestData.put("reason", reason);
        requestData.put("requestDate", FieldValue.serverTimestamp());
        requestData.put("originalDocId", docId);

        // 1. Salva na tabela de moderação
        db.collection(COLLECTION_DELETION_REQUESTS).add(requestData)
                .addOnSuccessListener(ref -> {
                    // 2. Remove da tabela pública (opcional: você pode apenas marcar como oculto, mas aqui vamos mover)
                    db.collection(COLLECTION_GLOBAL_FIX).document(docId).delete()
                            .addOnSuccessListener(aVoid -> {
                                if (callback != null) callback.onSuccess();
                            })
                            .addOnFailureListener(e -> {
                                if (callback != null) callback.onFailure("Movido para análise, mas erro ao ocultar: " + e.getMessage());
                            });
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onFailure(e.getMessage());
                });
    }

    public static void deleteAddressFromCommunity(String address, GlobalUploadCallback callback) {
        // Mantendo o método para compatibilidade, mas agora ele é direto (admin/fallback)
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String docId = sanitizeAddressId(address);
        db.collection(COLLECTION_GLOBAL_FIX).document(docId).delete()
                .addOnSuccessListener(aVoid -> {
                    if (callback != null) callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onFailure(e.getMessage());
                });
    }

    /**
     * Verifica se o e-mail atual tem permissões de desenvolvedor
     */
    public static void checkDeveloperAccess(String email, DeveloperAccessCallback callback) {
        if (email == null) { callback.onResult(false); return; }
        
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("admin_config").document("developers").get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        List<String> allowedEmails = (List<String>) doc.get("allowedEmails");
                        boolean isAllowed = allowedEmails != null && allowedEmails.contains(email.trim().toLowerCase());
                        callback.onResult(isAllowed);
                    } else {
                        callback.onResult(false);
                    }
                })
                .addOnFailureListener(e -> callback.onResult(false));
    }

    public static void fetchDeveloperList(DeveloperListCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("admin_config").document("developers").get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        List<String> emails = (List<String>) doc.get("allowedEmails");
                        
                        Map<String, Boolean> fixedMap = new HashMap<>();
                        try {
                            Object fixedObj = doc.get("fixed");
                            if (fixedObj instanceof Map) {
                                Map<?, ?> rawMap = (Map<?, ?>) fixedObj;
                                for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
                                    if (entry.getKey() instanceof String && entry.getValue() instanceof Boolean) {
                                        fixedMap.put((String) entry.getKey(), (Boolean) entry.getValue());
                                    }
                                }
                            }
                        } catch (Exception ignored) {}

                        if (emails == null) emails = new ArrayList<>();
                        callback.onResult(emails, fixedMap);
                    } else {
                        callback.onResult(new ArrayList<>(), new HashMap<>());
                    }
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public interface DeveloperListCallback {
        void onResult(List<String> emails, Map<String, Boolean> fixedMap);
        void onError(String msg);
    }

    public interface DeveloperAccessCallback {
        void onResult(boolean isDeveloper);
    }

    // --- SISTEMA DE DESENVOLVEDOR: COMPARTILHAMENTO DE ROTAS ---

    public static void shareRouteWithDevelopers(String devEmail, String devName, RouteHeader header, List<RouteStop> stops, GlobalUploadCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, Object> routeData = new HashMap<>();
        routeData.put("name", header.name);
        routeData.put("date", header.date);
        routeData.put("sharedBy", devEmail);
        routeData.put("sharedByName", devName);
        routeData.put("timestamp", FieldValue.serverTimestamp());

        List<Map<String, Object>> stopsList = new ArrayList<>();
        for (RouteStop s : stops) {
            Map<String, Object> sm = new HashMap<>();
            sm.put("address", s.address);
            sm.put("lat", s.latitude);
            sm.put("lon", s.longitude);
            sm.put("neighborhood", s.neighborhood);
            sm.put("sequence", s.sequence);
            sm.put("packageCount", s.packageCount);
            sm.put("sortOrder", s.sortOrder);
            stopsList.add(sm);
        }
        routeData.put("stops", stopsList);

        db.collection(COLLECTION_DEV_ROUTES).add(routeData)
                .addOnSuccessListener(ref -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    public interface SharedRoutesCallback {
        void onResult(List<Map<String, Object>> routes);
        void onError(String msg);
    }

    public static void fetchSharedDeveloperRoutes(SharedRoutesCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection(COLLECTION_DEV_ROUTES)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Map<String, Object>> list = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot) {
                        Map<String, Object> data = doc.getData();
                        if (data != null) {
                            data.put("id", doc.getId());
                            list.add(data);
                        }
                    }
                    callback.onResult(list);
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public static void shareRecordingWithDevelopers(String devEmail, String devName, DailyKm km, List<RoutePoint> points, GlobalUploadCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, Object> data = new HashMap<>();
        data.put("date", km.date);
        data.put("totalKm", km.totalKm);
        data.put("gpsDistance", km.gpsDistance);
        data.put("sharedBy", devEmail);
        data.put("sharedByName", devName);
        data.put("timestamp", FieldValue.serverTimestamp());

        List<Map<String, Object>> pointsList = new ArrayList<>();
        for (RoutePoint p : points) {
            Map<String, Object> pm = new HashMap<>();
            pm.put("lat", p.latitude);
            pm.put("lon", p.longitude);
            pm.put("ts", p.timestamp);
            pointsList.add(pm);
        }
        data.put("points", pointsList);

        db.collection(COLLECTION_DEV_RECORDINGS).add(data)
                .addOnSuccessListener(ref -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    public static void fetchSharedDeveloperRecordings(SharedRoutesCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection(COLLECTION_DEV_RECORDINGS)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Map<String, Object>> list = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot) {
                        Map<String, Object> data = doc.getData();
                        if (data != null) {
                            data.put("id", doc.getId());
                            // Reaproveitando o campo 'name' para data/km para exibir na lista
                            long dateLong = data.get("date") != null ? (long) data.get("date") : 0;
                            String dateStr = new SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault()).format(new Date(dateLong));
                            double dist = data.get("gpsDistance") != null ? (double) data.get("gpsDistance") : 0;
                            data.put("name", "Grav: " + dateStr + " (" + String.format("%.1f", dist) + " KM)");
                            list.add(data);
                        }
                    }
                    callback.onResult(list);
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public static void broadcastScannerAlert(String devEmail, String devName, String message, boolean isOfferDetected, final ScannerBroadcastCallback cb) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, Object> alert = new HashMap<>();
        alert.put("email", devEmail);
        alert.put("name", devName);
        alert.put("message", message);
        alert.put("isOfferDetected", isOfferDetected);
        alert.put("timestamp", FieldValue.serverTimestamp());

        db.collection(COLLECTION_DEV_ALERTS).add(alert)
            .addOnSuccessListener(ref -> {
                Log.d("FirebaseHelper", "Alerta gravado com sucesso: " + ref.getId());
                if (cb != null) cb.onResult(true, null);
            })
            .addOnFailureListener(e -> {
                Log.e("FirebaseHelper", "Erro ao gravar alerta: " + e.getMessage());
                if (cb != null) cb.onResult(false, e.getMessage());
            });
    }

    public interface ScannerBroadcastCallback {
        void onResult(boolean success, String error);
    }

    public interface ScannerAlertCallback {
        void onNewAlert(String name, String message, boolean isOfferDetected, String email);
    }

    public static ListenerRegistration listenToDeveloperAlerts(ScannerAlertCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        long now = System.currentTimeMillis();
        // Aumentamos o tempo de escuta para os últimos 60 minutos para garantir captura inicial
        return db.collection(COLLECTION_DEV_ALERTS)
                .whereGreaterThan("timestamp", new Timestamp(new Date(now - 3600000)))
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null) {
                        Log.e("FirebaseHelper", "Erro no listener global: " + e.getMessage());
                        return;
                    }
                    if (snapshot == null) return;
                    
                    for (DocumentChange dc : snapshot.getDocumentChanges()) {
                        if (dc.getType() == DocumentChange.Type.ADDED) {
                            DocumentSnapshot doc = dc.getDocument();
                            String name = doc.getString("name");
                            String msg = doc.getString("message");
                            Boolean isOffer = doc.getBoolean("isOfferDetected");
                            String email = doc.getString("email");
                            
                            // 🔥 OTIMIZAÇÃO: Verifica se o timestamp existe. Se não existir (novo doc sendo escrito), 
                            // o snapshot dispara duas vezes. Tratamos apenas o que tem tempo confirmado pelo servidor.
                            Timestamp ts = doc.getTimestamp("timestamp");
                            if (ts != null) {
                                long alertTime = ts.toDate().getTime();
                                // Só processamos alertas que aconteceram nos últimos 2 minutos 
                                // (para não disparar notificações de coisas velhas ao abrir o app)
                                if (alertTime > now - 120000) {
                                    if (name != null && msg != null && isOffer != null) {
                                        callback.onNewAlert(name, msg, isOffer, email);
                                    }
                                }
                            }
                        }
                    }
                });
    }

    // --- SISTEMA DE AMIZADE ---

    public interface FriendSearchCallback {
        void onFound(String name, String email, String uid);
        void onNotFound();
        void onError(String msg);
    }

    /**
     * Procura um usuário pelo @username exato
     */
    public static void findCourierByUsername(String username, FriendSearchCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String cleanUsername = username.trim().toLowerCase().replace("@", "");
        
        // Busca na tabela de exclusividade para pegar o e-mail do dono
        db.collection(COLLECTION_USERNAMES).document(cleanUsername).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String email = doc.getString("email");
                        if (email != null) {
                            findCourierByEmail(email, callback);
                        } else {
                            callback.onNotFound();
                        }
                    } else {
                        callback.onNotFound();
                    }
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public interface UsernameCheckCallback {
        void onResult(boolean isAvailable);
        void onError(String msg);
    }

    /**
     * Verifica se um @username já está em uso
     */
    public static void checkUsernameAvailability(String username, UsernameCheckCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String cleanUsername = username.trim().toLowerCase().replace("@", "");
        
        db.collection(COLLECTION_USERNAMES).document(cleanUsername).get()
                .addOnSuccessListener(doc -> {
                    callback.onResult(!doc.exists());
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    /**
     * Tenta reservar um @username de forma exclusiva
     */
    public static void claimUsername(String oldUsername, String newUsername, String email, GlobalUploadCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String cleanNew = newUsername.trim().toLowerCase().replace("@", "");
        String cleanOld = oldUsername != null ? oldUsername.trim().toLowerCase().replace("@", "") : null;
        String sanitizedEmail = email.trim().toLowerCase();

        db.runTransaction(transaction -> {
            DocumentReference newRef = db.collection(COLLECTION_USERNAMES).document(cleanNew);
            DocumentReference userRef = db.collection(COLLECTION_USERS).document(sanitizedEmail);

            // 1. Verifica se o novo nome já está ocupado
            DocumentSnapshot newDoc = transaction.get(newRef);
            if (newDoc.exists()) {
                String ownerEmail = newDoc.getString("email");
                if (ownerEmail != null && !ownerEmail.equalsIgnoreCase(sanitizedEmail)) {
                    throw new FirebaseFirestoreException(
                            "Username already taken", 
                            FirebaseFirestoreException.Code.ALREADY_EXISTS);
                }
            }

            // 2. Reserva o novo nome
            Map<String, Object> nameData = new HashMap<>();
            nameData.put("email", sanitizedEmail);
            transaction.set(newRef, nameData);

            // 3. Atualiza o perfil do usuário
            transaction.update(userRef, "username", cleanNew);

            // 4. Se tinha um nome antigo diferente, libera ele
            if (cleanOld != null && !cleanOld.isEmpty() && !cleanOld.equals(cleanNew)) {
                DocumentReference oldRef = db.collection(COLLECTION_USERNAMES).document(cleanOld);
                transaction.delete(oldRef);
            }

            return null;
        }).addOnSuccessListener(aVoid -> callback.onSuccess())
          .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    /**
     * Procura um usuário pelo e-mail exato
     */
    public static void findCourierByEmail(String email, FriendSearchCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String sanitizedEmail = email.trim().toLowerCase();

        // Busca direta pelo ID do documento (mais rápido e confiável)
        db.collection(COLLECTION_USERS).document(sanitizedEmail).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        callback.onFound(
                                doc.getString("displayName") != null ? doc.getString("displayName") : "Entregador",
                                sanitizedEmail,
                                doc.getId()
                        );
                    } else {
                        callback.onNotFound();
                    }
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    /**
     * Envia um convite de amizade
     */
    public static void sendFriendRequest(String fromId, String fromName, String toId, GlobalUploadCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, Object> request = new HashMap<>();
        request.put("fromId", fromId);
        request.put("fromName", fromName);
        request.put("toId", toId);
        request.put("status", "pending"); // pending, accepted, rejected
        request.put("timestamp", FieldValue.serverTimestamp());
        request.put("participants", Arrays.asList(fromId.toLowerCase(), toId.toLowerCase()));

        db.collection(COLLECTION_FRIEND_REQUESTS)
                .document(fromId.replace(".", "_") + "_" + toId.replace(".", "_")) 
                .set(request)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    public interface FriendRequestCallback {
        void onResult(List<Map<String, Object>> requests);
        void onError(String msg);
    }

    /**
     * Aceita um convite de amizade
     */
    public static void acceptFriendRequest(String fromId, String toId, GlobalUploadCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String docId = fromId.replace(".", "_") + "_" + toId.replace(".", "_");
        db.collection(COLLECTION_FRIEND_REQUESTS)
                .document(docId)
                .update("status", "accepted")
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    /**
     * Recusa um convite de amizade
     */
    public static void rejectFriendRequest(String fromId, String toId, GlobalUploadCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String docId = fromId.replace(".", "_") + "_" + toId.replace(".", "_");
        db.collection(COLLECTION_FRIEND_REQUESTS)
                .document(docId)
                .delete()
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    /**
     * Escuta convites pendentes e amigos em tempo real
     */
    public static ListenerRegistration listenFriendRequests(String myEmail, FriendRequestCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        // Escuta tanto convites que eu enviei quanto recebi
        return db.collection(COLLECTION_FRIEND_REQUESTS)
                .whereArrayContainsAny("participants", Arrays.asList(myEmail.toLowerCase()))
                .addSnapshotListener((querySnapshot, e) -> {
                    if (e != null) { callback.onError(e.getMessage()); return; }
                    if (querySnapshot != null) {
                        List<Map<String, Object>> list = new ArrayList<>();
                        for (DocumentSnapshot doc : querySnapshot) {
                            Map<String, Object> data = doc.getData();
                            if (data != null) {
                                data.put("id", doc.getId());
                                list.add(data);
                            }
                        }
                        callback.onResult(list);
                    }
                });
    }

    public interface NotificationCallback {
        void onResult(List<Map<String, Object>> notifications);
        void onError(String msg);
    }

    /**
     * Escuta tanto Pedidos de Amizade quanto Novos Compartilhamentos de Localização
     */
    public static ListenerRegistration listenAllNotifications(String myEmail, NotificationCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String me = myEmail.toLowerCase();
        
        // Listener 1: Pedidos de Amizade
        return db.collection(COLLECTION_FRIEND_REQUESTS)
                .whereEqualTo("toId", me)
                .whereEqualTo("status", "pending")
                .addSnapshotListener((requests, e1) -> {
                    if (e1 != null) { callback.onError(e1.getMessage()); return; }
                    
                    // Listener 2: Compartilhamentos de Localização (active_shares)
                    db.collection("active_shares")
                        .whereEqualTo("to", me)
                        .addSnapshotListener((shares, e2) -> {
                            if (e2 != null) { callback.onError(e2.getMessage()); return; }
                            
                            List<Map<String, Object>> all = new ArrayList<>();
                            
                            // Adiciona pedidos de amizade
                            if (requests != null) {
                                for (DocumentSnapshot doc : requests) {
                                    Map<String, Object> data = doc.getData();
                                    if (data != null) {
                                        data.put("type", "friend_request");
                                        all.add(data);
                                    }
                                }
                            }
                            
                            // Adiciona compartilhamentos ativos
                            if (shares != null) {
                                long now = System.currentTimeMillis();
                                for (DocumentSnapshot doc : shares) {
                                    Map<String, Object> data = doc.getData();
                                    if (data != null) {
                                        Long expiry = doc.getLong("expiry");
                                        if (expiry == null || expiry == -1 || expiry > now) {
                                            data.put("type", "location_share");
                                            // Normaliza campos para o adapter
                                            data.put("fromId", doc.get("from"));
                                            all.add(data);
                                        }
                                    }
                                }
                            }
                            
                            callback.onResult(all);
                        });
                });
    }

    public interface FriendProfileCallback {
        void onResult(String name, String email, String username, String avatarBase64, int likes, int fixes, int routes, boolean isFixed);
        void onError(String msg);
    }

    /**
     * Busca dados completos de um perfil para visualização de amigos
     */
    public static void fetchUserProfile(String email, FriendProfileCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection(COLLECTION_USERS).document(email.trim().toLowerCase()).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String name = doc.getString("displayName");
                        String mail = doc.getString("email");
                        String username = doc.getString("username");
                        String avatar = doc.getString("avatarBase64");
                        Long likes = doc.getLong("likes");
                        Long fixes = doc.getLong("fixes");
                        Long routes = doc.getLong("routes");
                        Boolean fixo = doc.getBoolean("fixo");

                        callback.onResult(
                                name != null ? name : "Entregador",
                                mail != null ? mail : email,
                                username != null ? username : "",
                                avatar != null ? avatar : "",
                                likes != null ? likes.intValue() : 0,
                                fixes != null ? fixes.intValue() : 0,
                                routes != null ? routes.intValue() : 0,
                                fixo != null ? fixo : false
                        );
                    } else {
                        callback.onError("Perfil não encontrado");
                    }
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    /**
     * Atualiza a localização em tempo real para o Modo Comboio
     */
    public static void updateLiveLocation(String email, double lat, double lon) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, Object> updates = new HashMap<>();
        Map<String, Object> loc = new HashMap<>();
        loc.put("lat", lat);
        loc.put("lon", lon);
        loc.put("ts", FieldValue.serverTimestamp());
        
        updates.put("liveLocation", loc);
        updates.put("isOnline", true);

        db.collection(COL_USERS).document(email.trim().toLowerCase())
                .set(updates, SetOptions.merge());
    }

    /**
     * Remove a localização em tempo real (fica Offline)
     */
    public static void goOffline(String email) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, Object> updates = new HashMap<>();
        updates.put("liveLocation", FieldValue.delete());
        updates.put("isOnline", false);

        db.collection(COL_USERS).document(email.trim().toLowerCase())
                .set(updates, SetOptions.merge());
    }

    public interface FriendsLocationCallback {
        void onUpdate(List<FriendLocation> locations);
    }

    public static class FriendLocation {
        public String email, name, username, avatar;
        public double lat, lon;
        public long timestamp;
    }

    /**
     * Inicia o compartilhamento de localização com um amigo específico
     * @param durationHours -1 para tempo indeterminado
     */
    public static void startSharingWithFriend(String myEmail, String friendEmail, int durationHours, GlobalUploadCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String shareId = myEmail.replace(".", "_") + "_to_" + friendEmail.replace(".", "_");
        
        Map<String, Object> data = new HashMap<>();
        data.put("from", myEmail.toLowerCase());
        data.put("to", friendEmail.toLowerCase());
        data.put("timestamp", FieldValue.serverTimestamp());
        
        if (durationHours > 0) {
            long expiry = System.currentTimeMillis() + (durationHours * 3600000L);
            data.put("expiry", expiry);
        } else {
            data.put("expiry", -1L); // Ilimitado
        }

        db.collection("active_shares").document(shareId).set(data)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    public static void stopSharingWithFriend(String myEmail, String friendEmail, GlobalUploadCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String shareId = myEmail.replace(".", "_") + "_to_" + friendEmail.replace(".", "_");
        db.collection("active_shares").document(shareId).delete()
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    public interface ShareStatusCallback {
        void onResult(boolean isSharing, long expiry);
    }

    public static void checkShareStatus(String myEmail, String friendEmail, ShareStatusCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String shareId = myEmail.replace(".", "_") + "_to_" + friendEmail.replace(".", "_");
        db.collection("active_shares").document(shareId).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                Long expiry = doc.getLong("expiry");
                if (expiry != null && expiry > 0 && expiry < System.currentTimeMillis()) {
                    doc.getReference().delete();
                    callback.onResult(false, 0);
                } else {
                    callback.onResult(true, expiry != null ? expiry : -1);
                }
            } else {
                callback.onResult(false, 0);
            }
        });
    }

    public static void updateComboioPreference(String email, int mode) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection(COL_USERS).document(email.trim().toLowerCase())
                .update("comboioMode", mode);
    }

    public interface HazardCallback {
        void onUpdate(List<HazardReport> list);
    }

    public static class HazardReport {
        public String id, type, description, creatorId;
        public double lat, lon;
        public long timestamp;
        public long expiryTimestamp; 
        public int likes = 0;
        public int dislikes = 0;
        public HazardReport() {}
        public HazardReport(String t, String d, double la, double lo, String c, int durationMinutes) {
            type = t; description = d; lat = la; lon = lo; creatorId = c; 
            timestamp = System.currentTimeMillis();
            expiryTimestamp = timestamp + (durationMinutes * 60 * 1000L);
        }
    }

    public static void reportHazard(HazardReport h, GlobalUploadCallback cb) {
        FirebaseFirestore.getInstance().collection(COLLECTION_HAZARDS).add(h)
                .addOnSuccessListener(doc -> cb.onSuccess())
                .addOnFailureListener(e -> cb.onFailure(e.getMessage()));
    }

    public static ListenerRegistration listenHazards(HazardCallback cb) {
        long now = System.currentTimeMillis();
        return FirebaseFirestore.getInstance().collection(COLLECTION_HAZARDS)
                .whereGreaterThan("expiryTimestamp", now)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null || snapshot == null) return;
                    List<HazardReport> list = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        HazardReport h = doc.toObject(HazardReport.class);
                        if (h != null) { h.id = doc.getId(); list.add(h); }
                    }
                    cb.onUpdate(list);
                });
    }

    public static void deleteHazard(String id) {
        FirebaseFirestore.getInstance().collection(COLLECTION_HAZARDS).document(id).delete();
    }

    public static void addHazardFeedback(String hazardId, boolean isLike) {
        DocumentReference doc = FirebaseFirestore.getInstance().collection(COLLECTION_HAZARDS).document(hazardId);
        String field = isLike ? "likes" : "dislikes";
        doc.update(field, FieldValue.increment(1));
    }

    /**
     * Escuta a localização dos amigos baseada na privacidade de cada um (Tempo Real)
     */
    public static ListenerRegistration listenFriendsLocations(String myEmail, FriendsLocationCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String me = myEmail.toLowerCase();
        
        // Listener VIVO que detecta movimento de QUALQUER pessoa que possa estar compartilhando comigo
        // Para simplificar e garantir funcionamento real-time, ouvimos a coleção de usuários
        // que tenham o modo comboio ativado ou localizações recentes.
        return db.collection(COL_USERS)
            .whereGreaterThan("liveLocation.ts", new Timestamp(new Date(System.currentTimeMillis() - 3600000))) // Ultima hora
            .addSnapshotListener((users, e) -> {
                if (e != null || users == null) return;

                // Agora validamos permissões de forma rápida
                db.collection("active_shares").whereEqualTo("to", me).get().addOnSuccessListener(shares -> {
                    List<String> targeted = new ArrayList<>();
                    long now = System.currentTimeMillis();
                    for (DocumentSnapshot s : shares) {
                        Long exp = s.getLong("expiry");
                        if (exp == null || exp == -1 || exp > now) targeted.add(s.getString("from").toLowerCase());
                    }

                    db.collection(COLLECTION_FRIEND_REQUESTS).whereArrayContains("participants", me).whereEqualTo("status", "accepted").get().addOnSuccessListener(friends -> {
                        List<String> friendList = new ArrayList<>();
                        for (DocumentSnapshot f : friends) {
                            List<String> p = (List<String>) f.get("participants");
                            if (p != null) for (String mail : p) if (!mail.equalsIgnoreCase(me)) friendList.add(mail.toLowerCase());
                        }

                        List<FriendLocation> result = new ArrayList<>();
                        for (DocumentSnapshot doc : users) {
                            String email = doc.getString("email");
                            if (email == null) continue;
                            String lowEmail = email.toLowerCase();
                            
                            Long mode = doc.getLong("comboioMode");
                            boolean isPublic = (mode != null && mode == 1);
                            
                            if (targeted.contains(lowEmail) || (friendList.contains(lowEmail) && isPublic)) {
                                Map<String, Object> loc = (Map<String, Object>) doc.get("liveLocation");
                                if (loc != null) {
                                    FriendLocation fl = new FriendLocation();
                                    fl.email = email;
                                    fl.name = doc.getString("displayName");
                                    fl.username = doc.getString("username");
                                    fl.avatar = doc.getString("avatarBase64");
                                    fl.lat = parseDoubleSafe(loc.get("lat"));
                                    fl.lon = parseDoubleSafe(loc.get("lon"));
                                    Object tsObj = loc.get("ts");
                                    if (tsObj instanceof Timestamp) fl.timestamp = ((Timestamp) tsObj).toDate().getTime();
                                    result.add(fl);
                                }
                            }
                        }
                        callback.onUpdate(result);
                    });
                });
            });
    }

    private static double parseDoubleSafe(Object o) {
        if (o instanceof Double) return (Double) o;
        if (o instanceof Long) return ((Long) o).doubleValue();
        if (o instanceof String) try { return Double.parseDouble((String) o); } catch (Exception e) {}
        return 0;
    }

    public static void updateDeveloperEmails(List<String> emails, GlobalUploadCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, Object> data = new HashMap<>();
        data.put("allowedEmails", emails);
        db.collection("admin_config").document("developers")
                .set(data, SetOptions.merge())
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    public static void updateRemoteMenuConfig(String id, boolean isSub0, boolean isSub1, boolean isSub2, GlobalUploadCallback cb) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        
        // 1. Sub 0 (Público Geral)
        DocumentReference ref0 = db.collection("menu_sub0").document(id);
        if (isSub0) ref0.set(new HashMap<>()); else ref0.delete();

        // 2. Sub 1 (Premium)
        DocumentReference ref1 = db.collection("menu_sub1").document(id);
        if (isSub1) ref1.set(new HashMap<>()); else ref1.delete();

        // 3. Sub 2 (Developer)
        DocumentReference ref2 = db.collection("menu_sub2").document(id);
        if (isSub2) ref2.set(new HashMap<>()); else ref2.delete();

        if (cb != null) cb.onSuccess();
    }

    public interface RemoteMenuConfigCallback {
        void onUpdate(List<DocumentSnapshot> sub0, List<DocumentSnapshot> sub1, List<DocumentSnapshot> sub2);
    }

    public static ListenerRegistration listenRemoteMenuConfigAll(RemoteMenuConfigCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        final List<DocumentSnapshot> d0 = new ArrayList<>();
        final List<DocumentSnapshot> d1 = new ArrayList<>();
        final List<DocumentSnapshot> d2 = new ArrayList<>();

        ListenerRegistration l0 = db.collection("menu_sub0").addSnapshotListener((snapshot, e) -> {
            if (e != null || snapshot == null) return;
            d0.clear(); d0.addAll(snapshot.getDocuments());
            callback.onUpdate(d0, d1, d2);
        });

        ListenerRegistration l1 = db.collection("menu_sub1").addSnapshotListener((snapshot, e) -> {
            if (e != null || snapshot == null) return;
            d1.clear(); d1.addAll(snapshot.getDocuments());
            callback.onUpdate(d0, d1, d2);
        });

        ListenerRegistration l2 = db.collection("menu_sub2").addSnapshotListener((snapshot, e) -> {
            if (e != null || snapshot == null) return;
            d2.clear(); d2.addAll(snapshot.getDocuments());
            callback.onUpdate(d0, d1, d2);
        });

        return new ListenerRegistration() {
            @Override public void remove() { l0.remove(); l1.remove(); l2.remove(); }
        };
    }

    private static final String COL_USERS = "users";

    public interface RemoteMenuCallback {
        void onUpdate(List<String> allowedIds);
    }

    public interface UsersListCallback {
        void onResult(List<Map<String, Object>> users);
        void onError(String msg);
    }

    public static void fetchAllUsers(UsersListCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection(COL_USERS)
                .orderBy("lastSeen", Query.Direction.DESCENDING)
                .limit(200)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Map<String, Object>> list = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot) {
                        Map<String, Object> data = doc.getData();
                        if (data != null) {
                            data.put("id", doc.getId());
                            list.add(data);
                        }
                    }
                    callback.onResult(list);
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public static void updateUserPermissions(String email, long installDate, int subType, GlobalUploadCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, Object> updates = new HashMap<>();
        updates.put("installDate", installDate);
        updates.put("subType", subType);

        db.collection(COL_USERS).document(email.trim().toLowerCase())
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    if (callback != null) callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onFailure(e.getMessage());
                });
    }

    /**
     * Escuta a configuração remota de menus/abas baseada no nível de assinatura do usuário
     */
    public static ListenerRegistration listenRemoteMenus(int subType, RemoteMenuCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        
        final List<String> m0 = new ArrayList<>();
        final List<String> m1 = new ArrayList<>();
        final List<String> m2 = new ArrayList<>();

        // Sempre ouvimos a base (Sub 0)
        ListenerRegistration l0 = db.collection("menu_sub0").addSnapshotListener((snapshot, e) -> {
            if (e != null || snapshot == null) return;
            m0.clear();
            for (DocumentSnapshot doc : snapshot.getDocuments()) m0.add(doc.getId());
            combineAndNotify(m0, m1, m2, subType, callback);
        });

        // Se for Premium (1) ou Dev (2), ouve a Premium
        ListenerRegistration l1 = null;
        if (subType >= 1) {
            l1 = db.collection("menu_sub1").addSnapshotListener((snapshot, e) -> {
                if (e != null || snapshot == null) return;
                m1.clear();
                for (DocumentSnapshot doc : snapshot.getDocuments()) m1.add(doc.getId());
                combineAndNotify(m0, m1, m2, subType, callback);
            });
        }

        // Se for Dev (2), ouve a Dev
        ListenerRegistration l2 = null;
        if (subType >= 2) {
            l2 = db.collection("menu_sub2").addSnapshotListener((snapshot, e) -> {
                if (e != null || snapshot == null) return;
                m2.clear();
                for (DocumentSnapshot doc : snapshot.getDocuments()) m2.add(doc.getId());
                combineAndNotify(m0, m1, m2, subType, callback);
            });
        }

        final ListenerRegistration fl1 = l1;
        final ListenerRegistration fl2 = l2;

        return new ListenerRegistration() {
            @Override public void remove() {
                l0.remove();
                if (fl1 != null) fl1.remove();
                if (fl2 != null) fl2.remove();
            }
        };
    }

    private static void combineAndNotify(List<String> d0, List<String> d1, List<String> d2, int subType, RemoteMenuCallback cb) {
        List<String> all = new ArrayList<>(d0);
        if (subType >= 1) {
            for (String id : d1) if (!all.contains(id)) all.add(id);
        }
        if (subType >= 2) {
            for (String id : d2) if (!all.contains(id)) all.add(id);
        }
        cb.onUpdate(all);
    }

    // --- SISTEMA DE QUADRAS DA COMUNIDADE ---

    public interface GlobalQuadrasCallback {
        void onResult(List<CorrectedQuadra> quadras);
        void onError(String message);
    }

    public static void uploadQuadra(String creatorId, String creatorName, CorrectedQuadra quadra, GlobalUploadCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String namePart = quadra.name != null ? quadra.name : "quadra";
        String neighPart = quadra.neighborhood != null ? quadra.neighborhood : "";
        String docId = sanitizeAddressId(namePart + "_" + neighPart);

        Map<String, Object> data = new HashMap<>();
        data.put("name", quadra.name);
        data.put("neighborhood", quadra.neighborhood != null ? quadra.neighborhood : "");
        data.put("city", quadra.city != null ? quadra.city : "");
        data.put("latitude", quadra.latitude);
        data.put("longitude", quadra.longitude);
        data.put("notes", quadra.notes != null ? quadra.notes : "");
        data.put("creatorId", creatorId != null ? creatorId : "anon");
        data.put("creatorName", creatorName != null ? creatorName : "Entregador");
        data.put("type", quadra.type != null ? quadra.type : "QUADRA");
        data.put("updatedAt", FieldValue.serverTimestamp());

        db.collection(COLLECTION_GLOBAL_QUADRAS).document(docId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        data.put("lastUpdate", FieldValue.serverTimestamp());
                        db.collection(COLLECTION_GLOBAL_QUADRAS).document(docId).update(data)
                                .addOnSuccessListener(aVoid -> { if (callback != null) callback.onSuccess(); })
                                .addOnFailureListener(e -> { if (callback != null) callback.onFailure(e.getMessage()); });
                    } else {
                        data.put("likes", 0);
                        data.put("dislikes", 0);
                        data.put("lastUpdate", FieldValue.serverTimestamp());
                        db.collection(COLLECTION_GLOBAL_QUADRAS).document(docId).set(data)
                                .addOnSuccessListener(aVoid -> { if (callback != null) callback.onSuccess(); })
                                .addOnFailureListener(e -> { if (callback != null) callback.onFailure(e.getMessage()); });
                    }
                })
                .addOnFailureListener(e -> { if (callback != null) callback.onFailure(e.getMessage()); });
    }

    public static void getAllGlobalQuadras(GlobalQuadrasCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection(COLLECTION_GLOBAL_QUADRAS)
                .orderBy("name", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<CorrectedQuadra> result = new ArrayList<>();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        String name = doc.getString("name");
                        Double lat = doc.getDouble("latitude");
                        Double lon = doc.getDouble("longitude");

                        if (name != null && lat != null && lon != null) {
                            CorrectedQuadra q = new CorrectedQuadra(
                                    name,
                                    doc.getString("neighborhood"),
                                    doc.getString("city"),
                                    lat,
                                    lon
                            );
                            q.notes = doc.getString("notes");
                            q.creatorId = doc.getString("creatorId");
                            q.creatorName = doc.getString("creatorName");
                            q.type = doc.getString("type");
                            q.docId = doc.getId();
                            Long likes = doc.getLong("likes");
                            Long dislikes = doc.getLong("dislikes");
                            q.likes = likes != null ? likes.intValue() : 0;
                            q.dislikes = dislikes != null ? dislikes.intValue() : 0;
                            result.add(q);
                        }
                    }
                    if (callback != null) callback.onResult(result);
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onError(e.getMessage());
                });
    }

    public static void deleteGlobalQuadra(String docId, GlobalUploadCallback callback) {
        if (docId == null || docId.isEmpty()) return;
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection(COLLECTION_GLOBAL_QUADRAS).document(docId).delete()
                .addOnSuccessListener(aVoid -> { if (callback != null) callback.onSuccess(); })
                .addOnFailureListener(e -> { if (callback != null) callback.onFailure(e.getMessage()); });
    }

    public static void addQuadraFeedback(String name, String neighborhood, String city, double latitude, double longitude, Boolean isLike, String comment, String userName, String userId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String namePart = name != null ? name : "quadra";
        String neighPart = neighborhood != null ? neighborhood : "";
        String docId = sanitizeAddressId(namePart + "_" + neighPart);

        DocumentReference docRef = db.collection(COLLECTION_GLOBAL_QUADRAS).document(docId);
        DocumentReference userVoteRef = docRef.collection("user_votes").document(userId);

        if (isLike != null) {
            db.runTransaction(transaction -> {
                DocumentSnapshot quadraDoc = transaction.get(docRef);
                DocumentSnapshot voteDoc = transaction.get(userVoteRef);

                long currentLikes = 0;
                if (quadraDoc.exists() && quadraDoc.get("likes") != null) {
                    Long l = quadraDoc.getLong("likes");
                    if (l != null) currentLikes = l;
                }
                long currentDislikes = 0;
                if (quadraDoc.exists() && quadraDoc.get("dislikes") != null) {
                    Long d = quadraDoc.getLong("dislikes");
                    if (d != null) currentDislikes = d;
                }

                int likeDelta = 0;
                int dislikeDelta = 0;

                if (voteDoc.exists()) {
                    Boolean oldIsLike = voteDoc.getBoolean("isLike");
                    if (oldIsLike != null) {
                        if (oldIsLike.equals(isLike)) {
                            if (isLike) likeDelta = -1; else dislikeDelta = -1;
                            transaction.delete(userVoteRef);
                        } else {
                            if (isLike) { likeDelta = 1; dislikeDelta = -1; }
                            else { likeDelta = -1; dislikeDelta = 1; }
                            Map<String, Object> vData = new HashMap<>();
                            vData.put("isLike", isLike);
                            vData.put("timestamp", FieldValue.serverTimestamp());
                            transaction.set(userVoteRef, vData);
                        }
                    }
                } else {
                    if (isLike) likeDelta = 1; else dislikeDelta = 1;
                    Map<String, Object> vData = new HashMap<>();
                    vData.put("isLike", isLike);
                    vData.put("timestamp", FieldValue.serverTimestamp());
                    transaction.set(userVoteRef, vData);
                }

                Map<String, Object> qData = new HashMap<>();
                long newLikes = Math.max(0, currentLikes + likeDelta);
                long newDislikes = Math.max(0, currentDislikes + dislikeDelta);

                qData.put("likes", newLikes);
                qData.put("dislikes", newDislikes);
                if (name != null) qData.put("name", name);
                if (neighborhood != null) qData.put("neighborhood", neighborhood);
                if (city != null) qData.put("city", city);
                if (latitude != 0) qData.put("latitude", latitude);
                if (longitude != 0) qData.put("longitude", longitude);

                transaction.set(docRef, qData, SetOptions.merge());
                return null;
            }).addOnSuccessListener(aVoid -> Log.d("FirebaseHelper", "Voto na quadra " + docId + " gravado com sucesso!"))
              .addOnFailureListener(e -> Log.e("FirebaseHelper", "Voto na quadra falhou: " + e.getMessage()));
        }

        if (comment != null && !comment.trim().isEmpty()) {
            Map<String, Object> commentData = new HashMap<>();
            commentData.put("text", comment);
            commentData.put("user", userName);
            commentData.put("userId", userId);
            commentData.put("date", FieldValue.serverTimestamp());
            docRef.collection("comments").add(commentData)
                .addOnSuccessListener(ref -> docRef.update("commentCount", FieldValue.increment(1)));
        }

        // Notifica o criador da Quadra/Bloco em segundo plano
        docRef.get().addOnSuccessListener(doc -> {
            if (doc != null && doc.exists()) {
                String creatorId = doc.getString("creatorId");
                Double lat = doc.getDouble("latitude");
                Double lon = doc.getDouble("longitude");
                String qType = doc.getString("type");
                double quadLat = lat != null ? lat : 0;
                double quadLon = lon != null ? lon : 0;

                if (isLike != null) {
                    String action = isLike ? "LIKE" : "DISLIKE";
                    sendCommunityNotification(creatorId, userId, userName, name, neighborhood, action, null, quadLat, quadLon, qType);
                }
                if (comment != null && !comment.trim().isEmpty()) {
                    sendCommunityNotification(creatorId, userId, userName, name, neighborhood, "COMMENT", comment.trim(), quadLat, quadLon, qType);
                }
            }
        });
    }

    public static void sendCommunityNotification(String targetUserId, String senderId, String senderName, String quadraName, String quadraNeighborhood, String actionType, String commentText, double lat, double lon, String quadraType) {
        if (targetUserId == null || targetUserId.trim().isEmpty() || targetUserId.equals("anon")) {
            return;
        }
        String target = targetUserId.trim().toLowerCase(Locale.ROOT);
        String sender = senderId != null ? senderId.trim().toLowerCase(Locale.ROOT) : "anon";

        String formattedName = senderName;
        if (formattedName == null || formattedName.trim().isEmpty() || formattedName.equalsIgnoreCase("Entregador") || formattedName.equalsIgnoreCase("Um entregador")) {
            if (sender.contains("@")) {
                formattedName = sender.split("@")[0];
            } else {
                formattedName = "Um entregador";
            }
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, Object> notif = new HashMap<>();
        notif.put("targetUserId", target);
        notif.put("senderId", sender);
        notif.put("senderName", formattedName);
        notif.put("quadraName", quadraName != null ? quadraName : "Quadra");
        notif.put("quadraNeighborhood", quadraNeighborhood != null ? quadraNeighborhood : "");
        notif.put("actionType", actionType); // "LIKE", "DISLIKE", "COMMENT"
        notif.put("commentText", commentText != null ? commentText : "");
        notif.put("latitude", lat);
        notif.put("longitude", lon);
        notif.put("type", quadraType != null ? quadraType : "QUADRA");
        notif.put("timestamp", FieldValue.serverTimestamp());
        notif.put("isRead", false);

        db.collection("community_notifications").add(notif)
                .addOnSuccessListener(ref -> Log.d("FirebaseHelper", "Notificação gravada no Firestore para " + target))
                .addOnFailureListener(e -> Log.e("FirebaseHelper", "Falha ao gravar notificação: " + e.getMessage()));
    }

    public interface CommunityNotificationCallback {
        void onResult(List<Map<String, Object>> list, int unreadCount);
    }

    public static ListenerRegistration listenCommunityNotifications(String userId, CommunityNotificationCallback callback) {
        if (userId == null || userId.trim().isEmpty() || userId.equals("anon")) {
            if (callback != null) callback.onResult(new ArrayList<>(), 0);
            return null;
        }
        String target = userId.trim().toLowerCase(Locale.ROOT);
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        return db.collection("community_notifications")
                .whereEqualTo("targetUserId", target)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null) {
                        Log.e("FirebaseHelper", "Erro ao escutar notificações de " + target + ": " + e.getMessage());
                        if (callback != null) callback.onResult(new ArrayList<>(), 0);
                        return;
                    }
                    if (snapshot == null) {
                        if (callback != null) callback.onResult(new ArrayList<>(), 0);
                        return;
                    }
                    List<Map<String, Object>> list = new ArrayList<>();
                    int unread = 0;
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        Map<String, Object> data = doc.getData();
                        if (data != null) {
                            data.put("docId", doc.getId());
                            Boolean isRead = doc.getBoolean("isRead");
                            if (Boolean.FALSE.equals(isRead) || isRead == null) {
                                unread++;
                            }
                            list.add(data);
                        }
                    }

                    // Ordena em memória por timestamp decrescente (evita erro de índice composto no Firestore)
                    list.sort((a, b) -> {
                        Timestamp t1 = (Timestamp) a.get("timestamp");
                        Timestamp t2 = (Timestamp) b.get("timestamp");
                        if (t1 == null && t2 == null) return 0;
                        if (t1 == null) return 1;
                        if (t2 == null) return -1;
                        return t2.compareTo(t1);
                    });

                    Log.d("FirebaseHelper", "Notificações recebidas para " + target + ": total=" + list.size() + ", unread=" + unread);
                    if (callback != null) callback.onResult(list, unread);
                });
    }

    public static void markCommunityNotificationsAsRead(List<String> docIds) {
        if (docIds == null || docIds.isEmpty()) return;
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        WriteBatch batch = db.batch();
        for (String id : docIds) {
            batch.update(db.collection("community_notifications").document(id), "isRead", true);
        }
        batch.commit();
    }

    public static void clearCommunityNotifications(String userId) {
        if (userId == null || userId.isEmpty()) return;
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("community_notifications")
                .whereEqualTo("targetUserId", userId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot != null) {
                        WriteBatch batch = db.batch();
                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                            batch.delete(doc.getReference());
                        }
                        batch.commit();
                    }
                });
    }

    public interface SingleQuadraCallback {
        void onResult(int likes, int dislikes);
    }

    public static void getQuadraDetails(String name, String neighborhood, SingleQuadraCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String namePart = name != null ? name : "quadra";
        String neighPart = neighborhood != null ? neighborhood : "";
        String docId = sanitizeAddressId(namePart + "_" + neighPart);

        db.collection(COLLECTION_GLOBAL_QUADRAS).document(docId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc != null && doc.exists()) {
                        Long l = doc.getLong("likes");
                        Long d = doc.getLong("dislikes");
                        int likes = l != null ? l.intValue() : 0;
                        int dislikes = d != null ? d.intValue() : 0;
                        if (callback != null) callback.onResult(likes, dislikes);
                    }
                });
    }

    public interface UserVoteCallback {
        void onVote(Boolean isLike);
    }

    public static void getUserQuadraVote(String name, String neighborhood, String userId, UserVoteCallback callback) {
        if (userId == null || userId.isEmpty()) {
            if (callback != null) callback.onVote(null);
            return;
        }
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String namePart = name != null ? name : "quadra";
        String neighPart = neighborhood != null ? neighborhood : "";
        String docId = sanitizeAddressId(namePart + "_" + neighPart);

        db.collection(COLLECTION_GLOBAL_QUADRAS).document(docId)
                .collection("user_votes").document(userId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        Boolean isLike = doc.getBoolean("isLike");
                        if (callback != null) callback.onVote(isLike);
                    } else {
                        if (callback != null) callback.onVote(null);
                    }
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onVote(null);
                });
    }

    public interface QuadraCommentsCallback {
        void onCommentsUpdated(List<Map<String, Object>> comments);
    }

    public static ListenerRegistration listenQuadraComments(String name, String neighborhood, QuadraCommentsCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String namePart = name != null ? name : "quadra";
        String neighPart = neighborhood != null ? neighborhood : "";
        String docId = sanitizeAddressId(namePart + "_" + neighPart);

        return db.collection(COLLECTION_GLOBAL_QUADRAS).document(docId)
                .collection("comments")
                .orderBy("date", Query.Direction.ASCENDING)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null || snapshot == null) return;
                    List<Map<String, Object>> list = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        Map<String, Object> data = doc.getData();
                        if (data != null) {
                            data.put("docId", doc.getId());
                            list.add(data);
                        }
                    }
                    if (callback != null) callback.onCommentsUpdated(list);
                });
    }

    public static void deleteQuadraComment(String name, String neighborhood, String commentDocId, GlobalUploadCallback callback) {
        if (commentDocId == null || commentDocId.isEmpty()) return;
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String namePart = name != null ? name : "quadra";
        String neighPart = neighborhood != null ? neighborhood : "";
        String docId = sanitizeAddressId(namePart + "_" + neighPart);

        DocumentReference docRef = db.collection(COLLECTION_GLOBAL_QUADRAS).document(docId);
        docRef.collection("comments").document(commentDocId).delete()
                .addOnSuccessListener(aVoid -> {
                    docRef.update("commentCount", FieldValue.increment(-1));
                    if (callback != null) callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onFailure(e.getMessage());
                });
    }
}
