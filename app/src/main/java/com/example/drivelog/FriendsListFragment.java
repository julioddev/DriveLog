package com.example.drivelog;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
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

public class FriendsListFragment extends Fragment {

    private EditText editEmail;
    private MaterialButton btnAdd;
    private RecyclerView recycler;
    private FriendsAdapter adapter;
    private ListenerRegistration listenerRegistration;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_friend_list, container, false);

        editEmail = view.findViewById(R.id.editFriendEmail);
        btnAdd = view.findViewById(R.id.btnAddFriend);
        recycler = view.findViewById(R.id.recyclerFriends);

        recycler.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new FriendsAdapter();
        recycler.setAdapter(adapter);

        btnAdd.setOnClickListener(v -> searchAndAddFriend());

        startListening();

        return view;
    }

    private void startListening() {
        String myEmail = FirebaseAuth.getInstance().getCurrentUser() != null ? FirebaseAuth.getInstance().getCurrentUser().getEmail() : null;
        if (myEmail == null) return;

        listenerRegistration = FirebaseHelper.listenFriendRequests(myEmail, new FirebaseHelper.FriendRequestCallback() {
            @Override
            public void onResult(List<Map<String, Object>> requests) {
                if (getActivity() == null) return;
                
                // Filtra apenas amigos aceitos
                List<Map<String, Object>> friendsOnly = new ArrayList<>();
                for (Map<String, Object> req : requests) {
                    if ("accepted".equals(req.get("status"))) {
                        friendsOnly.add(req);
                    }
                }
                getActivity().runOnUiThread(() -> adapter.setList(friendsOnly, myEmail));
            }

            @Override
            public void onError(String msg) {
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> Toast.makeText(getContext(), "Erro ao carregar amigos", Toast.LENGTH_SHORT).show());
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (listenerRegistration != null) listenerRegistration.remove();
    }

    private void searchAndAddFriend() {
        String query = editEmail.getText().toString().trim().toLowerCase();
        if (query.isEmpty()) {
            Toast.makeText(getContext(), "Digite um e-mail ou @usuario", Toast.LENGTH_SHORT).show();
            return;
        }

        String myEmail = FirebaseAuth.getInstance().getCurrentUser() != null ? FirebaseAuth.getInstance().getCurrentUser().getEmail() : "";
        if (query.equalsIgnoreCase(myEmail)) {
            Toast.makeText(getContext(), "Você não pode adicionar a si mesmo", Toast.LENGTH_SHORT).show();
            return;
        }

        if (query.startsWith("@")) {
            // Busca por Username
            FirebaseHelper.findCourierByUsername(query, new FirebaseHelper.FriendSearchCallback() {
                @Override
                public void onFound(String name, String foundEmail, String uid) {
                    if (getActivity() == null) return;
                    getActivity().runOnUiThread(() -> sendRequest(foundEmail, name));
                }

                @Override
                public void onNotFound() {
                    if (getActivity() == null) return;
                    getActivity().runOnUiThread(() -> Toast.makeText(getContext(), "Nome de usuário não encontrado", Toast.LENGTH_LONG).show());
                }

                @Override
                public void onError(String msg) {
                    if (getActivity() == null) return;
                    getActivity().runOnUiThread(() -> Toast.makeText(getContext(), "Erro: " + msg, Toast.LENGTH_SHORT).show());
                }
            });
        } else {
            // Busca por E-mail (Lógica anterior mantida)
            FirebaseHelper.findCourierByEmail(query, new FirebaseHelper.FriendSearchCallback() {
                @Override
                public void onFound(String name, String foundEmail, String uid) {
                    if (getActivity() == null) return;
                    getActivity().runOnUiThread(() -> sendRequest(foundEmail, name));
                }

                @Override
                public void onNotFound() {
                    if (getActivity() == null) return;
                    getActivity().runOnUiThread(() -> Toast.makeText(getContext(), "E-mail não encontrado no DriveLog", Toast.LENGTH_LONG).show());
                }

                @Override
                public void onError(String msg) {
                    if (getActivity() == null) return;
                    getActivity().runOnUiThread(() -> Toast.makeText(getContext(), "Erro: " + msg, Toast.LENGTH_SHORT).show());
                }
            });
        }
    }

    private void sendRequest(String targetEmail, String name) {
        String myEmail = FirebaseAuth.getInstance().getCurrentUser().getEmail();
        String myName = FirebaseAuth.getInstance().getCurrentUser().getDisplayName();

        FirebaseHelper.sendFriendRequest(myEmail, myName, targetEmail, new FirebaseHelper.GlobalUploadCallback() {
            @Override
            public void onSuccess() {
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "Convite enviado para " + name, Toast.LENGTH_SHORT).show();
                    editEmail.setText("");
                });
            }

            @Override
            public void onFailure(String msg) {
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> Toast.makeText(getContext(), "Falha ao enviar: " + msg, Toast.LENGTH_SHORT).show());
            }
        });
    }

    private static class FriendsAdapter extends RecyclerView.Adapter<FriendsAdapter.ViewHolder> {
        private final List<Map<String, Object>> list = new ArrayList<>();
        private String myEmail;

        void setList(List<Map<String, Object>> newList, String myEmail) {
            this.myEmail = myEmail.toLowerCase();
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
            String fromId = (String) item.get("fromId");
            String toId = (String) item.get("toId");
            String fromName = (String) item.get("fromName");

            boolean isIncoming = toId != null && toId.equalsIgnoreCase(myEmail);
            String displayName = isIncoming ? fromName : toId;
            String targetEmail = isIncoming ? fromId : toId;
            
            holder.textName.setText(displayName);
            holder.textStatus.setText("Amigo Parceiro");

            // 🔥 Carrega a miniatura da foto de perfil e o nome real na lista
            holder.imgAvatar.setImageResource(R.drawable.bg_circle_primary);
            holder.imgAvatar.setColorFilter(android.graphics.Color.parseColor("#2196F3"));

            final int currentPos = holder.getBindingAdapterPosition();
            FirebaseHelper.fetchUserProfile(targetEmail, new FirebaseHelper.FriendProfileCallback() {
                @Override
                public void onResult(String name, String email, String username, String avatarBase64, int likes, int fixes, int routes, boolean isFixed) {
                    if (holder.getBindingAdapterPosition() == currentPos) {
                        holder.textName.post(() -> {
                            String finalDisplayName = (name != null) ? name : displayName;
                            if (username != null && !username.isEmpty()) {
                                holder.textName.setText(finalDisplayName + " (@" + username + ")");
                            } else {
                                holder.textName.setText(finalDisplayName);
                            }
                        });

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

            // 🔥 Abre o perfil do amigo ao clicar
            holder.itemView.setOnClickListener(v -> {
                android.content.Context context = v.getContext();
                while (context instanceof android.content.ContextWrapper) {
                    if (context instanceof MainActivity) {
                        ((MainActivity) context).openFragmentInSettings(
                                FriendProfileFragment.newInstance(targetEmail), 
                                "Perfil de " + displayName
                        );
                        break;
                    }
                    context = ((android.content.ContextWrapper) context).getBaseContext();
                }
            });

            holder.btnDelete.setOnClickListener(v -> {
                FirebaseHelper.rejectFriendRequest(fromId, toId, new FirebaseHelper.GlobalUploadCallback() {
                    @Override public void onSuccess() { }
                    @Override public void onFailure(String msg) { }
                });
            });
        }

        @Override
        public int getItemCount() { return list.size(); }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView textName, textStatus;
            ImageButton btnDelete;
            ImageView imgAvatar;
            ViewHolder(View v) {
                super(v);
                textName = v.findViewById(R.id.textFriendName);
                textStatus = v.findViewById(R.id.textFriendStatus);
                btnDelete = v.findViewById(R.id.btnDeleteFriend);
                imgAvatar = v.findViewById(R.id.imgFriendAvatar);
                v.findViewById(R.id.btnAcceptFriend).setVisibility(View.GONE);
            }
        }
    }
}
