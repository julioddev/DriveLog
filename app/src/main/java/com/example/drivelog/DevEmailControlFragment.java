package com.example.drivelog;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DevEmailControlFragment extends Fragment {

    private RecyclerView recyclerEmails;
    private EmailAdapter emailAdapter;
    private final List<String> devEmails = new ArrayList<>();
    private final Map<String, Boolean> fixedEmailsMap = new HashMap<>();
    private EditText editNewEmail;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dev_email_control, container, false);

        recyclerEmails = view.findViewById(R.id.recyclerDevEmails);
        recyclerEmails.setLayoutManager(new LinearLayoutManager(getContext()));
        emailAdapter = new EmailAdapter(devEmails);
        recyclerEmails.setAdapter(emailAdapter);

        editNewEmail = view.findViewById(R.id.editAddDevEmail);
        view.findViewById(R.id.btnAddDevEmail).setOnClickListener(v -> addEmail());
        view.findViewById(R.id.btnSaveDevEmails).setOnClickListener(v -> saveEmails());

        fetchDevEmails();

        return view;
    }

    private void fetchDevEmails() {
        FirebaseHelper.fetchDeveloperList(new FirebaseHelper.DeveloperListCallback() {
            @Override public void onResult(List<String> emails, Map<String, Boolean> fixedMap) {
                if (!isAdded()) return;
                devEmails.clear(); devEmails.addAll(emails);
                fixedEmailsMap.clear(); fixedEmailsMap.putAll(fixedMap);
                if (emailAdapter != null) emailAdapter.notifyDataSetChanged();
            }
            @Override public void onError(String msg) { 
                if (isAdded()) Toast.makeText(getContext(), "Erro ao carregar devs: " + msg, Toast.LENGTH_SHORT).show(); 
            }
        });
    }

    private void addEmail() {
        if (editNewEmail == null) return;
        String email = editNewEmail.getText().toString().trim().toLowerCase();
        if (!email.isEmpty() && !devEmails.contains(email)) {
            devEmails.add(email);
            if (emailAdapter != null) emailAdapter.notifyDataSetChanged();
            editNewEmail.setText("");
        }
    }

    private void saveEmails() {
        if (getContext() == null) return;
        Toast.makeText(getContext(), "Salvando lista de emails...", Toast.LENGTH_SHORT).show();
        
        FirebaseHelper.updateDeveloperEmails(devEmails, new FirebaseHelper.GlobalUploadCallback() {
            @Override public void onSuccess() {
                if (isAdded()) Toast.makeText(getContext(), "Lista de e-mails salva!", Toast.LENGTH_SHORT).show();
            }
            @Override public void onFailure(String msg) {
                if (isAdded()) Toast.makeText(getContext(), "Erro ao salvar: " + msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private class EmailAdapter extends RecyclerView.Adapter<EmailAdapter.ViewHolder> {
        private final List<String> items;
        EmailAdapter(List<String> items) { this.items = items; }
        @NonNull @Override public ViewHolder onCreateViewHolder(@NonNull ViewGroup p, int vt) {
            LinearLayout container = new LinearLayout(p.getContext());
            container.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            container.setOrientation(LinearLayout.HORIZONTAL);
            container.setGravity(android.view.Gravity.CENTER_VERTICAL);
            int padding = (int) (12 * p.getResources().getDisplayMetrics().density);
            container.setPadding(padding, padding / 2, padding, padding / 2);
            
            TextView tv = new TextView(p.getContext());
            tv.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
            tv.setTextSize(14);
            tv.setTextColor(Color.BLACK);
            container.addView(tv);
            
            ImageButton btn = new ImageButton(p.getContext());
            btn.setImageResource(android.R.drawable.ic_menu_delete);
            btn.setBackground(null);
            btn.setPadding(padding, padding, padding, padding);
            container.addView(btn);
            
            return new ViewHolder(container, tv, btn);
        }
        @Override public void onBindViewHolder(@NonNull ViewHolder h, int pos) {
            String email = items.get(pos); 
            h.text.setText(email);
            
            boolean isFixedByAdmin = fixedEmailsMap.containsKey(email) && Boolean.TRUE.equals(fixedEmailsMap.get(email));
            
            if (isFixedByAdmin) {
                h.btn.setVisibility(View.GONE);
                h.text.setTextColor(Color.GRAY);
            } else {
                h.btn.setVisibility(View.VISIBLE);
                h.text.setTextColor(Color.BLACK);
                h.btn.setOnClickListener(v -> { 
                    items.remove(pos); 
                    notifyDataSetChanged(); 
                });

                FirebaseHelper.fetchUserProfile(email, new FirebaseHelper.FriendProfileCallback() {
                    @Override public void onResult(String n, String e, String u, String a, int l, int f, int r, boolean isFixed) {
                        if (isFixed && h.getAdapterPosition() == pos) {
                            h.btn.setVisibility(View.GONE);
                            h.text.setTextColor(Color.GRAY);
                        }
                    }
                    @Override public void onError(String msg) {}
                });
            }
        }
        @Override public int getItemCount() { return items.size(); }
        class ViewHolder extends RecyclerView.ViewHolder {
            TextView text; ImageButton btn;
            ViewHolder(View v, TextView t, ImageButton b) { super(v); this.text = t; this.btn = b; }
        }
    }
}
