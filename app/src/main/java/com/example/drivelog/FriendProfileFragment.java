package com.example.drivelog;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;

import java.util.Locale;

public class FriendProfileFragment extends Fragment {

    private static final String ARG_EMAIL = "friend_email";
    private String friendEmail;

    private TextView textName, textEmail, textLikes, textFixes, textRoutes, textUsername;
    private ImageView imgAvatar;
    private View cardLiveShare, layoutShareControls, layoutSharingActive;
    private com.google.android.material.tabs.TabLayout tabDuration;
    private TextView textShareStatus;
    private MaterialButton btnStart, btnStop;

    public static FriendProfileFragment newInstance(String email) {
        FriendProfileFragment fragment = new FriendProfileFragment();
        Bundle args = new Bundle();
        args.putString(ARG_EMAIL, email);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            friendEmail = getArguments().getString(ARG_EMAIL);
        }
    }

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
        
        cardLiveShare = view.findViewById(R.id.cardLiveShare);
        layoutShareControls = view.findViewById(R.id.layoutShareControls);
        layoutSharingActive = view.findViewById(R.id.layoutSharingActive);
        tabDuration = view.findViewById(R.id.tabLayoutShareDuration);
        textShareStatus = view.findViewById(R.id.textShareStatus);
        btnStart = view.findViewById(R.id.btnStartSharing);
        btnStop = view.findViewById(R.id.btnStopSharing);

        // Esconde elementos exclusivos do "Meu Perfil"
        view.findViewById(R.id.cardProfileAvatar).setClickable(false);
        view.findViewById(R.id.textProfilePhotoHint).setVisibility(View.GONE);
        view.findViewById(R.id.textProfileFooter).setVisibility(View.GONE);
        view.findViewById(R.id.btnEditUsername).setClickable(false);

        setupLiveShare();
        loadFriendData();

        return view;
    }

    private void setupLiveShare() {
        if (getContext() == null) return;
        android.content.SharedPreferences prefs = requireContext().getSharedPreferences("AppConfig", android.content.Context.MODE_PRIVATE);
        
        com.google.firebase.auth.FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || user.getEmail() == null) {
            cardLiveShare.setVisibility(View.GONE);
            return;
        }

        String myEmail = user.getEmail();

        // 🔥 BLOQUEIO TOTAL: Só DEVs podem compartilhar individualmente
        FirebaseHelper.checkDeveloperAccess(myEmail, isDev -> {
            if (!isAdded()) return;
            requireActivity().runOnUiThread(() -> {
                if (!isDev) {
                    cardLiveShare.setVisibility(View.GONE);
                    return;
                }

                // Se for DEV, segue a lógica original de privacidade
                int globalMode = prefs.getInt("comboio_global_mode", 2); 
                if (globalMode == 2) {
                    cardLiveShare.setVisibility(View.GONE);
                    return;
                }

                cardLiveShare.setVisibility(View.VISIBLE);
                
                FirebaseHelper.checkShareStatus(myEmail, friendEmail, (isSharing, expiry) -> {
                    if (!isAdded()) return;
                    requireActivity().runOnUiThread(() -> {
                        if (isSharing) {
                            layoutShareControls.setVisibility(View.GONE);
                            layoutSharingActive.setVisibility(View.VISIBLE);
                            if (expiry == -1) {
                                textShareStatus.setText("Compartilhando tempo ilimitado");
                            } else {
                                String time = new java.text.SimpleDateFormat("HH:mm", Locale.getDefault()).format(new java.util.Date(expiry));
                                textShareStatus.setText("Compartilhando até as " + time);
                            }
                        } else {
                            layoutShareControls.setVisibility(View.VISIBLE);
                            layoutSharingActive.setVisibility(View.GONE);
                        }
                    });
                });
            });
        });

        btnStart.setOnClickListener(v -> {
            int pos = tabDuration.getSelectedTabPosition();
            int hours = 1;
            if (pos == 1) hours = 4;
            else if (pos == 2) hours = 8;
            else if (pos == 3) hours = -1; // Sempre

            FirebaseHelper.startSharingWithFriend(myEmail, friendEmail, hours, new FirebaseHelper.GlobalUploadCallback() {
                @Override public void onSuccess() { 
                    if (isAdded()) requireActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "Compartilhamento iniciado!", Toast.LENGTH_SHORT).show();
                        setupLiveShare(); 
                    });
                }
                @Override public void onFailure(String msg) {
                    if (isAdded()) requireActivity().runOnUiThread(() -> Toast.makeText(getContext(), "Erro: " + msg, Toast.LENGTH_SHORT).show());
                }
            });
        });

        btnStop.setOnClickListener(v -> {
            FirebaseHelper.stopSharingWithFriend(myEmail, friendEmail, new FirebaseHelper.GlobalUploadCallback() {
                @Override public void onSuccess() {
                    if (isAdded()) requireActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "Compartilhamento encerrado.", Toast.LENGTH_SHORT).show();
                        setupLiveShare();
                    });
                }
                @Override public void onFailure(String msg) {}
            });
        });
    }

    private void loadFriendData() {
        FirebaseHelper.fetchUserProfile(friendEmail, new FirebaseHelper.FriendProfileCallback() {
            @Override
            public void onResult(String name, String email, String username, String avatarBase64, int likes, int fixes, int routes, boolean isFixed) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    textName.setText(name);
                    textEmail.setText(email);
                    
                    if (username != null && !username.isEmpty()) {
                        textUsername.setText("@" + username);
                    } else {
                        textUsername.setText("Sem @usuario");
                    }

                    textLikes.setText(String.valueOf(likes));
                    textFixes.setText(String.valueOf(fixes));
                    textRoutes.setText(String.valueOf(routes));

                    if (avatarBase64 != null && !avatarBase64.isEmpty()) {
                        try {
                            byte[] decoded = Base64.decode(avatarBase64, Base64.DEFAULT);
                            Bitmap bitmap = BitmapFactory.decodeByteArray(decoded, 0, decoded.length);
                            imgAvatar.setImageBitmap(bitmap);
                            imgAvatar.setColorFilter(null);
                        } catch (Exception ignored) {}
                    }
                });
            }

            @Override
            public void onError(String msg) {
                if (isAdded()) requireActivity().runOnUiThread(() -> Toast.makeText(getContext(), "Erro ao carregar perfil: " + msg, Toast.LENGTH_SHORT).show());
            }
        });
    }
}
