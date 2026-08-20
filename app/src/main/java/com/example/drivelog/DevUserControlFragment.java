package com.example.drivelog;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DevUserControlFragment extends Fragment {

    private RecyclerView recyclerUsers;
    private SwipeRefreshLayout swipeRefresh;
    private UserAdapter adapter;
    private final List<Map<String, Object>> userList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dev_user_control, container, false);

        recyclerUsers = view.findViewById(R.id.recyclerDevUsers);
        swipeRefresh = view.findViewById(R.id.swipeRefreshUsers);

        recyclerUsers.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new UserAdapter(userList);
        recyclerUsers.setAdapter(adapter);

        swipeRefresh.setOnRefreshListener(this::fetchUsers);

        fetchUsers();

        return view;
    }

    private void fetchUsers() {
        swipeRefresh.setRefreshing(true);
        FirebaseHelper.fetchAllUsers(new FirebaseHelper.UsersListCallback() {
            @Override
            public void onResult(List<Map<String, Object>> users) {
                if (!isAdded()) return;
                userList.clear();
                userList.addAll(users);
                adapter.notifyDataSetChanged();
                swipeRefresh.setRefreshing(false);
            }

            @Override
            public void onError(String msg) {
                if (!isAdded()) return;
                Toast.makeText(getContext(), "Erro: " + msg, Toast.LENGTH_SHORT).show();
                swipeRefresh.setRefreshing(false);
            }
        });
    }

    private class UserAdapter extends RecyclerView.Adapter<UserAdapter.ViewHolder> {
        private final List<Map<String, Object>> items;
        private final SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault());

        UserAdapter(List<Map<String, Object>> items) { this.items = items; }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_dev_user, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Map<String, Object> user = items.get(position);
            String email = (String) user.get("id");
            String name = (String) user.get("displayName");
            
            // Tratamento seguro para subType
            Object subTypeObj = user.get("subType");
            int subType = 0;
            if (subTypeObj instanceof Long) subType = ((Long) subTypeObj).intValue();
            else if (subTypeObj instanceof Integer) subType = (Integer) subTypeObj;
            
            Object lastSeenObj = user.get("lastSeen");
            String lastSeenStr = "Nunca";
            if (lastSeenObj instanceof com.google.firebase.Timestamp) {
                lastSeenStr = sdf.format(((com.google.firebase.Timestamp) lastSeenObj).toDate());
            }

            holder.txtEmail.setText(email);
            holder.txtName.setText(name != null ? name : "Sem Nome");
            holder.txtStatus.setText("Visto: " + lastSeenStr + " | Sub: " + subType);

            if (subType == 1) holder.txtStatus.setTextColor(Color.parseColor("#4CAF50")); // Premium (Verde)
            else if (subType == 2) holder.txtStatus.setTextColor(Color.parseColor("#F44336")); // Dev (Vermelho)
            else holder.txtStatus.setTextColor(Color.GRAY); // Free

            Object installDateObj = user.get("installDate");
            long currentInstallDate = 0;
            if (installDateObj instanceof Long) currentInstallDate = (Long) installDateObj;

            holder.btnTrial.setOnClickListener(v -> updatePermissions(email, System.currentTimeMillis(), 0));
            holder.btnFull.setOnClickListener(v -> {
                // Ao liberar acesso total, mantemos a data de instalação original ou usamos a atual se não existir
                long dateToSave = (installDateObj != null) ? (long) installDateObj : System.currentTimeMillis();
                updatePermissions(email, dateToSave, 1);
            });
        }

        private void updatePermissions(String email, long installDate, int subType) {
            FirebaseHelper.updateUserPermissions(email, installDate, subType, new FirebaseHelper.GlobalUploadCallback() {
                @Override
                public void onSuccess() {
                    if (isAdded()) {
                        Toast.makeText(getContext(), "Permissões de " + email + " atualizadas!", Toast.LENGTH_SHORT).show();
                        fetchUsers();
                    }
                }

                @Override
                public void onFailure(String msg) {
                    if (isAdded()) Toast.makeText(getContext(), "Erro: " + msg, Toast.LENGTH_SHORT).show();
                }
            });
        }

        @Override
        public int getItemCount() { return items.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView txtName, txtEmail, txtStatus;
            View btnTrial, btnFull;
            ViewHolder(View v) {
                super(v);
                txtName = v.findViewById(R.id.txtUserName);
                txtEmail = v.findViewById(R.id.txtUserEmail);
                txtStatus = v.findViewById(R.id.txtUserStatus);
                btnTrial = v.findViewById(R.id.btnGiveTrial);
                btnFull = v.findViewById(R.id.btnGiveFull);
            }
        }
    }
}
