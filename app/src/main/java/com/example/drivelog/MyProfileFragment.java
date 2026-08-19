package com.example.drivelog;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public class MyProfileFragment extends Fragment {

    private TextView textName, textEmail, textLikes, textFixes, textRoutes, textUsername;
    private ImageView imgAvatar;
    private View cardAvatar, btnEditUsername;

    // 🔥 NOVO: Usando o Photo Picker moderno (Não precisa de permissões no manifesto!)
    private final ActivityResultLauncher<androidx.activity.result.PickVisualMediaRequest> pickMedia =
            registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
                if (uri != null) {
                    handleImagePick(uri);
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_my_profile, container, false);

        textName = view.findViewById(R.id.textProfileName);
        textEmail = view.findViewById(R.id.textProfileEmail);
        textLikes = view.findViewById(R.id.textProfileLikes);
        textFixes = view.findViewById(R.id.textProfileFixes);
        textRoutes = view.findViewById(R.id.textProfileRoutes);
        textUsername = view.findViewById(R.id.textProfileUsername);
        imgAvatar = view.findViewById(R.id.imgProfileAvatar);
        cardAvatar = view.findViewById(R.id.cardProfileAvatar);
        btnEditUsername = view.findViewById(R.id.btnEditUsername);

        cardAvatar.setOnClickListener(v -> {
            // Abre o seletor de fotos do sistema de forma segura e moderna
            pickMedia.launch(new androidx.activity.result.PickVisualMediaRequest.Builder()
                    .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                    .build());
        });

        btnEditUsername.setOnClickListener(v -> showEditUsernameDialog());

        loadProfileData();
        loadLocalAvatar();

        return view;
    }

    private void showEditUsernameDialog() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_username, null);
        com.google.android.material.textfield.TextInputEditText editInput = dialogView.findViewById(R.id.editUsernameInput);
        
        // Apenas letras, números e underline
        editInput.setFilters(new android.text.InputFilter[]{
            new android.text.InputFilter.LengthFilter(20),
            (source, start, end, dest, dstart, dend) -> {
                for (int i = start; i < end; i++) {
                    if (!Character.isLetterOrDigit(source.charAt(i)) && source.charAt(i) != '_') {
                        return "";
                    }
                }
                return null;
            }
        });

        String currentUsername = textUsername.getText().toString().replace("@", "");
        if (!currentUsername.equals("Definir @usuario")) {
            editInput.setText(currentUsername);
        }

        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        dialogView.findViewById(R.id.btnCancelUsername).setOnClickListener(v -> dialog.dismiss());
        dialogView.findViewById(R.id.btnSaveUsername).setOnClickListener(v -> {
            String newUsername = editInput.getText().toString().trim().toLowerCase().replace("@", "");
            if (newUsername.length() < 3) {
                Toast.makeText(getContext(), "Mínimo 3 caracteres", Toast.LENGTH_SHORT).show();
                return;
            }
            dialog.dismiss();
            saveUsernameExclusive(currentUsername, newUsername);
        });

        dialog.show();
    }

    private void saveUsernameExclusive(String oldName, String newName) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || user.getEmail() == null) return;

        FirebaseHelper.claimUsername(oldName, newName, user.getEmail(), new FirebaseHelper.GlobalUploadCallback() {
            @Override
            public void onSuccess() {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        textUsername.setText("@" + newName);
                        Toast.makeText(getContext(), "Nome de usuário reservado!", Toast.LENGTH_SHORT).show();
                    });
                }
            }

            @Override
            public void onFailure(String msg) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (msg.contains("ALREADY_EXISTS") || msg.contains("taken")) {
                            Toast.makeText(getContext(), "Este @usuario já está ocupado", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(getContext(), "Erro: " + msg, Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        });
    }

    private void loadProfileData() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            textName.setText(user.getDisplayName());
            textEmail.setText(user.getEmail());
            
            // Carrega username do Firestore
            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("users").document(user.getEmail().toLowerCase()).get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists() && doc.contains("username")) {
                            textUsername.setText("@" + doc.getString("username"));
                        } else {
                            textUsername.setText("Definir @usuario");
                        }
                    });
        }

        new Thread(() -> {
            Context context = getContext();
            if (context == null) return;
            
            AppDao dao = AppDatabase.getInstance(context).appDao();
            int totalFixes = dao.getAllCorrectedAddresses().size();
            
            // 🔥 Busca número de rotas concluídas
            int totalRoutes = 0;
            for (RouteHeader header : dao.getAllRoutes()) {
                if (header.isCompleted) totalRoutes++;
            }
            
            final int fFixes = totalFixes;
            final int fRoutes = totalRoutes;

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    textFixes.setText(String.valueOf(fFixes));
                    textRoutes.setText(String.valueOf(fRoutes));
                    // Sincroniza dados com o Firestore para que amigos vejam
                    syncStatsWithFirestore(fFixes, fRoutes);
                });
            }
        }).start();
    }

    private void syncStatsWithFirestore(int fixes, int routes) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        String encodedAvatar = requireContext().getSharedPreferences("AppConfig", Context.MODE_PRIVATE)
                .getString("profile_avatar_base64", "");

        java.util.Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("fixes", fixes);
        stats.put("routes", routes);
        if (!encodedAvatar.isEmpty()) stats.put("avatarBase64", encodedAvatar);

        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("users").document(user.getEmail().toLowerCase())
                .set(stats, com.google.firebase.firestore.SetOptions.merge());
    }

    private void handleImagePick(Uri uri) {
        try {
            InputStream is = requireContext().getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(is);
            
            // Redimensiona para não sobrecarregar o banco (max 300x300)
            Bitmap resized = Bitmap.createScaledBitmap(bitmap, 300, 300, true);
            imgAvatar.setImageBitmap(resized);
            imgAvatar.setColorFilter(null); // Remove o tint do placeholder

            // Salva em Base64 no SharedPreferences para persistência local rápida
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            resized.compress(Bitmap.CompressFormat.JPEG, 70, baos);
            byte[] bytes = baos.toByteArray();
            String encoded = Base64.encodeToString(bytes, Base64.DEFAULT);
            
            requireContext().getSharedPreferences("AppConfig", Context.MODE_PRIVATE)
                    .edit().putString("profile_avatar_base64", encoded).apply();
            
            // 🔥 Sincroniza foto com Firestore imediatamente
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null) {
                java.util.Map<String, Object> update = new java.util.HashMap<>();
                update.put("avatarBase64", encoded);
                com.google.firebase.firestore.FirebaseFirestore.getInstance()
                        .collection("users").document(user.getEmail().toLowerCase())
                        .set(update, com.google.firebase.firestore.SetOptions.merge());
            }

            Toast.makeText(getContext(), "Foto de perfil atualizada!", Toast.LENGTH_SHORT).show();
            
        } catch (Exception e) {
            Toast.makeText(getContext(), "Erro ao processar imagem", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadLocalAvatar() {
        String encoded = requireContext().getSharedPreferences("AppConfig", Context.MODE_PRIVATE)
                .getString("profile_avatar_base64", "");
        if (!encoded.isEmpty()) {
            byte[] decoded = Base64.decode(encoded, Base64.DEFAULT);
            Bitmap bitmap = BitmapFactory.decodeByteArray(decoded, 0, decoded.length);
            imgAvatar.setImageBitmap(bitmap);
            imgAvatar.setColorFilter(null);
        }
    }
}
