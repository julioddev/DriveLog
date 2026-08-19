package com.example.drivelog;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.ListenerRegistration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FriendRequestsFragment extends Fragment {

    private RecyclerView recycler;
    private RequestsAdapter adapter;
    private TextView textEmpty;
    private ListenerRegistration listenerRegistration;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_friend_requests, container, false);

        recycler = view.findViewById(R.id.recyclerRequests);
        textEmpty = view.findViewById(R.id.textNoRequests);

        recycler.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new RequestsAdapter();
        recycler.setAdapter(adapter);

        startListening();

        return view;
    }

    private void startListening() {
        com.google.firebase.auth.FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || user.getEmail() == null) return;
        String myEmail = user.getEmail();

        listenerRegistration = FirebaseHelper.listenAllNotifications(myEmail, new FirebaseHelper.NotificationCallback() {
            @Override
            public void onResult(List<Map<String, Object>> notifications) {
                if (getActivity() == null) return;
                FirebaseHelper.checkDeveloperAccess(myEmail, isDev -> {
                    if (getActivity() == null) return;
                    getActivity().runOnUiThread(() -> {
                        List<Map<String, Object>> filtered = new ArrayList<>();
                        for (Map<String, Object> n : notifications) {
                            if (!"friend_request".equals(n.get("type")) && !isDev) continue;
                            filtered.add(n);
                        }
                        adapter.setList(filtered);
                        textEmpty.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
                    });
                });
            }

            @Override public void onError(String msg) { }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (listenerRegistration != null) listenerRegistration.remove();
    }

    private static class RequestsAdapter extends RecyclerView.Adapter<RequestsAdapter.ViewHolder> {
        private final List<Map<String, Object>> list = new ArrayList<>();

        void setList(List<Map<String, Object>> newList) {
            list.clear();
            list.addAll(newList);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_friend, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Map<String, Object> item = list.get(position);
            String type = (String) item.get("type");
            String fromId = (String) item.get("fromId");
            String fromName = (String) item.get("fromName");

            // Configuração base
            holder.textName.setText(fromName != null ? fromName : fromId);
            holder.imgAvatar.setImageResource(R.drawable.bg_circle_primary);
            holder.imgAvatar.setColorFilter(android.graphics.Color.parseColor("#2196F3"));

            if ("friend_request".equals(type)) {
                holder.textStatus.setText("Enviou um convite de amizade");
                holder.btnAccept.setVisibility(View.VISIBLE);
                holder.btnAccept.setText("ACEITAR");
                holder.btnDelete.setVisibility(View.VISIBLE);
                
                holder.btnAccept.setOnClickListener(v -> {
                    FirebaseHelper.acceptFriendRequest(fromId, FirebaseAuth.getInstance().getCurrentUser().getEmail(), new FirebaseHelper.GlobalUploadCallback() {
                        @Override public void onSuccess() { }
                        @Override public void onFailure(String msg) { }
                    });
                });
                
                holder.btnDelete.setOnClickListener(v -> {
                    FirebaseHelper.rejectFriendRequest(fromId, FirebaseAuth.getInstance().getCurrentUser().getEmail(), new FirebaseHelper.GlobalUploadCallback() {
                        @Override public void onSuccess() { }
                        @Override public void onFailure(String msg) { }
                    });
                });
            } else {
                // Modo Compartilhamento (location_share)
                holder.itemView.setVisibility(View.VISIBLE);
                holder.itemView.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                holder.textStatus.setText("Começou a compartilhar a localização com você!");
                holder.btnAccept.setVisibility(View.VISIBLE);
                holder.btnAccept.setText("VER NO MAPA");
                holder.btnDelete.setVisibility(View.GONE);

                holder.btnAccept.setOnClickListener(v -> {
                    // Busca a localização atual do usuário no Firebase
                    com.google.firebase.firestore.FirebaseFirestore.getInstance()
                        .collection("users").document(fromId.toLowerCase()).get()
                        .addOnSuccessListener(doc -> {
                            double lat = 0, lon = 0;
                            Map<String, Object> loc = (Map<String, Object>) doc.get("liveLocation");
                            if (loc != null) {
                                lat = parseDoubleSafe(loc.get("lat"));
                                lon = parseDoubleSafe(loc.get("lon"));
                            }

                            android.content.Context context = v.getContext();
                            while (context instanceof android.content.ContextWrapper) {
                                if (context instanceof MainActivity) {
                                    ((MainActivity) context).returnToMainMap(lat, lon);
                                    return;
                                }
                                context = ((android.content.ContextWrapper) context).getBaseContext();
                            }
                        });
                });
            }

            // 🔥 Carrega a foto de quem gerou a notificação
            final int currentPos = holder.getBindingAdapterPosition();
            FirebaseHelper.fetchUserProfile(fromId, new FirebaseHelper.FriendProfileCallback() {
                @Override
                public void onResult(String name, String email, String username, String avatarBase64, int likes, int fixes, int routes, boolean isFixed) {
                    if (holder.getBindingAdapterPosition() == currentPos) {
                        if (fromName == null) holder.textName.setText(name != null ? name : fromId);
                        
                        if (avatarBase64 != null && !avatarBase64.isEmpty()) {
                            try {
                                byte[] decoded = android.util.Base64.decode(avatarBase64, android.util.Base64.DEFAULT);
                                android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeByteArray(decoded, 0, decoded.length);
                                holder.imgAvatar.post(() -> {
                                    holder.imgAvatar.setImageBitmap(bitmap);
                                    holder.imgAvatar.clearColorFilter();
                                });
                            } catch (Exception ignored) {}
                        }
                    }
                }
                @Override public void onError(String msg) {}
            });
        }

        @Override
        public int getItemCount() { return list.size(); }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView textName, textStatus;
            MaterialButton btnAccept;
            ImageButton btnDelete;
            ImageView imgAvatar;
            ViewHolder(View v) {
                super(v);
                textName = v.findViewById(R.id.textFriendName);
                textStatus = v.findViewById(R.id.textFriendStatus);
                btnAccept = v.findViewById(R.id.btnAcceptFriend);
                btnDelete = v.findViewById(R.id.btnDeleteFriend);
                imgAvatar = v.findViewById(R.id.imgFriendAvatar);
            }
        }

        private static double parseDoubleSafe(Object o) {
            if (o instanceof Double) return (Double) o;
            if (o instanceof Long) return ((Long) o).doubleValue();
            if (o instanceof String) try { return Double.parseDouble((String) o); } catch (Exception e) {}
            return 0;
        }
    }
}
