package com.example.drivelog;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class QuadrasFragment extends Fragment implements QuadraAdapter.OnQuadraClickListener {

    private RecyclerView recyclerQuadras;
    private QuadraAdapter adapter;
    private ProgressBar progressBar;
    private TextView textEmpty;
    private EditText editSearch;
    private List<CorrectedQuadra> fullQuadraList = new ArrayList<>();
    private String currentUserId = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_quadras, container, false);

        recyclerQuadras = view.findViewById(R.id.recyclerQuadras);
        progressBar = view.findViewById(R.id.progressQuadras);
        textEmpty = view.findViewById(R.id.textEmptyQuadras);
        editSearch = view.findViewById(R.id.editSearchQuadra);

        currentUserId = requireContext().getSharedPreferences("AppConfig", Context.MODE_PRIVATE)
                .getString("current_user_id", "anon");

        recyclerQuadras.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new QuadraAdapter(this, currentUserId);
        recyclerQuadras.setAdapter(adapter);

        editSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                filterQuadras(s.toString());
            }
        });

        loadQuadras();

        return view;
    }

    private void loadQuadras() {
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);

        // 1. Carrega Quadras Locais do Room
        new Thread(() -> {
            AppDao dao = AppDatabase.getInstance(requireContext()).appDao();
            List<CorrectedQuadra> localQuadras = dao.getAllCorrectedQuadras();

            // 2. Carrega Quadras Globais do Firebase
            FirebaseHelper.getAllGlobalQuadras(new FirebaseHelper.GlobalQuadrasCallback() {
                @Override
                public void onResult(List<CorrectedQuadra> globalQuadras) {
                    List<CorrectedQuadra> combined = new ArrayList<>(localQuadras);

                    if (globalQuadras != null) {
                        for (CorrectedQuadra gq : globalQuadras) {
                            boolean exists = false;
                            for (CorrectedQuadra lq : localQuadras) {
                                if (lq.name != null && lq.name.equalsIgnoreCase(gq.name) &&
                                    (lq.neighborhood == null || gq.neighborhood == null || lq.neighborhood.equalsIgnoreCase(gq.neighborhood))) {
                                    lq.likes = gq.likes;
                                    lq.dislikes = gq.dislikes;
                                    if (gq.docId != null) lq.docId = gq.docId;
                                    if (gq.notes != null && !gq.notes.isEmpty()) lq.notes = gq.notes;
                                    if (gq.creatorId != null) lq.creatorId = gq.creatorId;
                                    if (gq.creatorName != null) lq.creatorName = gq.creatorName;
                                    exists = true;
                                    break;
                                }
                            }
                            if (!exists) combined.add(gq);
                        }
                    }

                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            if (progressBar != null) progressBar.setVisibility(View.GONE);
                            fullQuadraList = combined;
                            filterQuadras(editSearch != null ? editSearch.getText().toString() : "");
                        });
                    }
                }

                @Override
                public void onError(String message) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            if (progressBar != null) progressBar.setVisibility(View.GONE);
                            fullQuadraList = localQuadras;
                            filterQuadras(editSearch != null ? editSearch.getText().toString() : "");
                        });
                    }
                }
            });
        }).start();
    }

    private void filterQuadras(String query) {
        if (query == null || query.trim().isEmpty()) {
            adapter.setQuadrasList(fullQuadraList);
            if (textEmpty != null) textEmpty.setVisibility(fullQuadraList.isEmpty() ? View.VISIBLE : View.GONE);
            return;
        }

        String q = query.toLowerCase(Locale.ROOT).trim();
        List<CorrectedQuadra> filtered = new ArrayList<>();
        for (CorrectedQuadra item : fullQuadraList) {
            boolean nameMatch = item.name != null && item.name.toLowerCase(Locale.ROOT).contains(q);
            boolean neighMatch = item.neighborhood != null && item.neighborhood.toLowerCase(Locale.ROOT).contains(q);
            boolean cityMatch = item.city != null && item.city.toLowerCase(Locale.ROOT).contains(q);

            if (nameMatch || neighMatch || cityMatch) {
                filtered.add(item);
            }
        }

        adapter.setQuadrasList(filtered);
        if (textEmpty != null) textEmpty.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onShowOnMap(CorrectedQuadra quadra) {
        if (getActivity() instanceof MainActivity) {
            MainActivity main = (MainActivity) getActivity();
            main.getSupportFragmentManager().popBackStack(); // Volta para o mapa
            main.showQuadraOnMap(quadra.latitude, quadra.longitude, quadra.name);
        }
    }

    @Override
    public void onDelete(CorrectedQuadra quadra) {
        if (getContext() == null) return;
        View view = getLayoutInflater().inflate(R.layout.dialog_modern_confirm, null);
        TextView txtTitle = view.findViewById(R.id.textModernTitle);
        TextView txtMessage = view.findViewById(R.id.textModernMessage);
        com.google.android.material.button.MaterialButton btnNegative = view.findViewById(R.id.btnModernNegative);
        com.google.android.material.button.MaterialButton btnPositive = view.findViewById(R.id.btnModernPositive);

        if (txtTitle != null) txtTitle.setText("Excluir Quadra");
        if (txtMessage != null) txtMessage.setText("Deseja realmente apagar esta Quadra (" + quadra.name + ")?");
        if (btnPositive != null) btnPositive.setText("EXCLUIR");

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(view)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        if (btnNegative != null) {
            btnNegative.setOnClickListener(v -> dialog.dismiss());
        }

        if (btnPositive != null) {
            btnPositive.setOnClickListener(v -> {
                dialog.dismiss();
                new Thread(() -> {
                    AppDao dao = AppDatabase.getInstance(requireContext()).appDao();
                    dao.deleteCorrectedQuadraByNameAndNeighborhood(quadra.name, quadra.neighborhood);

                    String docIdToDel = quadra.docId;
                    if (docIdToDel == null || docIdToDel.isEmpty()) {
                        String namePart = quadra.name != null ? quadra.name : "quadra";
                        String neighPart = quadra.neighborhood != null ? quadra.neighborhood : "";
                        docIdToDel = namePart.trim().toLowerCase().replace(" ", "_").replaceAll("[^a-z0-9_]", "") + "_" +
                                     neighPart.trim().toLowerCase().replace(" ", "_").replaceAll("[^a-z0-9_]", "");
                    }
                    FirebaseHelper.deleteGlobalQuadra(docIdToDel, null);

                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            Toast.makeText(getContext(), "Quadra excluída", Toast.LENGTH_SHORT).show();
                            loadQuadras();
                        });
                    }
                }).start();
            });
        }

        dialog.show();
    }
}
