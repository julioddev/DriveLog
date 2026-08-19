package com.example.drivelog;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;

public class TrackHistoryFragment extends Fragment implements KmAdapter.OnKmClickListener {

    private RecyclerView recyclerHistory;
    private KmAdapter adapter;
    private TextView textEmpty;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_track_history, container, false);

        recyclerHistory = view.findViewById(R.id.recyclerTrackHistory);
        textEmpty = view.findViewById(R.id.textEmptyTracks);
        
        recyclerHistory.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new KmAdapter(new ArrayList<>(), this);
        recyclerHistory.setAdapter(adapter);

        View btnOptions = view.findViewById(R.id.btnTrackOptions);
        if (btnOptions != null) {
            btnOptions.setVisibility(View.GONE); // Escondido por padrão
            checkDevVisibility(btnOptions);
            btnOptions.setOnClickListener(v -> showTrackOptionsMenu(v));
        }

        setupSwipeToDelete();
        observeTracks();

        return view;
    }

    private void checkDevVisibility(View view) {
        com.google.firebase.auth.FirebaseUser user = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        if (user != null && user.getEmail() != null) {
            FirebaseHelper.checkDeveloperAccess(user.getEmail(), isDev -> {
                if (isDev && getActivity() != null) {
                    getActivity().runOnUiThread(() -> view.setVisibility(View.VISIBLE));
                }
            });
        }
    }

    private void setupSwipeToDelete() {
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getBindingAdapterPosition();
                DailyKm dailyKm = adapter.getKmAt(position);

                View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_modern_confirm, null);
                TextView title = dialogView.findViewById(R.id.textModernTitle);
                TextView message = dialogView.findViewById(R.id.textModernMessage);
                com.google.android.material.button.MaterialButton btnCancel = dialogView.findViewById(R.id.btnModernNegative);
                com.google.android.material.button.MaterialButton btnConfirm = dialogView.findViewById(R.id.btnModernPositive);

                title.setText("Excluir Gravação");
                message.setText("Deseja realmente apagar permanentemente este trajeto GPS?");
                btnConfirm.setText("EXCLUIR");

                androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                        .setView(dialogView)
                        .create();

                if (dialog.getWindow() != null) {
                    dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
                }

                btnCancel.setOnClickListener(v -> {
                    dialog.dismiss();
                    adapter.notifyItemChanged(position);
                });

                btnConfirm.setOnClickListener(v -> {
                    dialog.dismiss();
                    new Thread(() -> {
                        AppDao dao = AppDatabase.getInstance(requireContext()).appDao();
                        dao.deleteRoutePointsForKm(dailyKm.id);
                        dao.deleteDailyKm(dailyKm);
                        CloudSyncHelper.syncNow(requireContext());
                    }).start();
                    Toast.makeText(getContext(), "Gravação excluída", Toast.LENGTH_SHORT).show();
                });

                dialog.show();
            }
        }).attachToRecyclerView(recyclerHistory);
    }

    private void observeTracks() {
        AppDatabase.getInstance(requireContext()).appDao().getAllAutomaticRoutesLive().observe(getViewLifecycleOwner(), tracks -> {
            if (tracks == null || tracks.isEmpty()) {
                textEmpty.setVisibility(View.VISIBLE);
                recyclerHistory.setVisibility(View.GONE);
            } else {
                textEmpty.setVisibility(View.GONE);
                recyclerHistory.setVisibility(View.VISIBLE);
                adapter.setKmList(tracks);
            }
        });
    }

    @Override
    public void onKmClick(DailyKm dailyKm) {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).showRouteOnMap(dailyKm.id);
        }
        
        // Força a mudança imediata para a aba de Rastreamento (onde está o mapa)
        if (getParentFragment() instanceof KmParentFragment) {
            ((KmParentFragment) getParentFragment()).switchToTracking();
        }
    }

    @Override
    public void onKmLongClick(DailyKm dailyKm, View anchor) {
        com.google.firebase.auth.FirebaseUser user = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        if (user != null && user.getEmail() != null) {
            FirebaseHelper.checkDeveloperAccess(user.getEmail(), isDev -> {
                if (isDev && getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        androidx.appcompat.widget.PopupMenu popup = new androidx.appcompat.widget.PopupMenu(requireContext(), anchor);
                        popup.getMenu().add(1, 99, 1, "🚀 Dev: Compartilhar Gravação");
                        popup.setOnMenuItemClickListener(item -> {
                            if (item.getItemId() == 99) {
                                shareRecording(dailyKm);
                            }
                            return true;
                        });
                        popup.show();
                    });
                }
            });
        }
    }

    private void shareRecording(DailyKm dailyKm) {
        new Thread(() -> {
            AppDao dao = AppDatabase.getInstance(requireContext()).appDao();
            java.util.List<RoutePoint> points = dao.getRoutePointsForKm(dailyKm.id);
            com.google.firebase.auth.FirebaseUser user = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
            
            if (user != null && !points.isEmpty()) {
                getActivity().runOnUiThread(() -> Toast.makeText(getContext(), "Compartilhando gravação...", Toast.LENGTH_SHORT).show());
                FirebaseHelper.shareRecordingWithDevelopers(user.getEmail(), user.getDisplayName(), dailyKm, points, new FirebaseHelper.GlobalUploadCallback() {
                    @Override public void onSuccess() {
                        if (getActivity() != null) getActivity().runOnUiThread(() -> Toast.makeText(getContext(), "Gravação compartilhada!", Toast.LENGTH_LONG).show());
                    }
                    @Override public void onFailure(String msg) {
                        if (getActivity() != null) getActivity().runOnUiThread(() -> Toast.makeText(getContext(), "Erro: " + msg, Toast.LENGTH_SHORT).show());
                    }
                });
            }
        }).start();
    }

    private void showTrackOptionsMenu(View anchor) {
        com.google.firebase.auth.FirebaseUser user = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        if (user != null && user.getEmail() != null) {
            FirebaseHelper.checkDeveloperAccess(user.getEmail(), isDev -> {
                if (isDev && getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        androidx.appcompat.widget.PopupMenu popup = new androidx.appcompat.widget.PopupMenu(requireContext(), anchor);
                        
                        android.content.SharedPreferences prefs = requireContext().getSharedPreferences("AppConfig", android.content.Context.MODE_PRIVATE);
                        boolean autoShare = prefs.getBoolean("dev_auto_share_recordings", false);

                        popup.getMenu().add(1, 101, 1, "🔄 Dev: Compartilhamento Automático")
                                .setCheckable(true)
                                .setChecked(autoShare);
                                
                        popup.getMenu().add(1, 100, 2, "📥 Dev: Baixar Gravações");
                        
                        popup.setOnMenuItemClickListener(item -> {
                            if (item.getItemId() == 100) {
                                showSharedRecordings();
                            } else if (item.getItemId() == 101) {
                                boolean newVal = !item.isChecked();
                                item.setChecked(newVal);
                                prefs.edit().putBoolean("dev_auto_share_recordings", newVal).apply();
                                Toast.makeText(getContext(), newVal ? "Auto-compartilhamento ligado" : "Auto-compartilhamento desligado", Toast.LENGTH_SHORT).show();
                            }
                            return true;
                        });
                        popup.show();
                    });
                }
            });
        }
    }

    private void showSharedRecordings() {
        FirebaseHelper.fetchSharedDeveloperRecordings(new FirebaseHelper.SharedRoutesCallback() {
            @Override
            public void onResult(java.util.List<java.util.Map<String, Object>> recordings) {
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    String[] names = new String[recordings.size()];
                    for (int i = 0; i < recordings.size(); i++) {
                        java.util.Map<String, Object> r = recordings.get(i);
                        names[i] = r.get("name") + "\n(De: " + r.get("sharedByName") + ")";
                    }
                    new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                            .setTitle("Gravações Compartilhadas")
                            .setItems(names, (dialog, which) -> importSharedRecording(recordings.get(which)))
                            .show();
                });
            }
            @Override public void onError(String msg) {
                if (getActivity() != null) getActivity().runOnUiThread(() -> Toast.makeText(getContext(), "Erro: " + msg, Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void importSharedRecording(java.util.Map<String, Object> data) {
        new Thread(() -> {
            try {
                long date = (long) data.get("date");
                double dist = (double) data.get("gpsDistance");
                java.util.List<java.util.Map<String, Object>> pointsData = (java.util.List<java.util.Map<String, Object>>) data.get("points");

                AppDao dao = AppDatabase.getInstance(requireContext()).appDao();
                DailyKm km = new DailyKm();
                km.date = date;
                km.gpsDistance = dist;
                km.totalKm = dist;
                km.isAutomatic = true;
                km.isCompleted = true;
                long newId = dao.insertDailyKm(km);

                java.util.List<RoutePoint> points = new java.util.ArrayList<>();
                for (java.util.Map<String, Object> p : pointsData) {
                    RoutePoint pt = new RoutePoint();
                    pt.dailyKmId = (int) newId;
                    pt.latitude = (double) p.get("lat");
                    pt.longitude = (double) p.get("lon");
                    pt.timestamp = (long) p.get("ts");
                    points.add(pt);
                }
                dao.insertRoutePoints(points);

                if (getActivity() != null) getActivity().runOnUiThread(() -> Toast.makeText(getContext(), "Gravação importada!", Toast.LENGTH_SHORT).show());
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }

    @Override
    public void onSaveEdit(DailyKm dailyKm, int position) {
    }
}
