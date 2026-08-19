package com.example.drivelog;

import android.os.Bundle;
import android.text.InputType;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class RouteHistoryFragment extends Fragment {

    private RecyclerView recyclerView;
    private HistoryAdapter adapter;
    private TextView textEmpty;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_route_history, container, false);
        
        recyclerView = view.findViewById(R.id.recyclerRouteHistory);
        textEmpty = view.findViewById(R.id.textEmptyHistory);
        
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new HistoryAdapter(new ArrayList<>(), 
                this::onRouteSelected, 
                this::onEditNameClicked,
                this::onDeleteClicked);
        recyclerView.setAdapter(adapter);
        
        loadHistory();
        
        return view;
    }

    private void loadHistory() {
        AppDatabase.getInstance(requireContext()).appDao().getAllRoutesLive().observe(getViewLifecycleOwner(), list -> {
            if (list == null || list.isEmpty()) {
                textEmpty.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.GONE);
            } else {
                textEmpty.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
                adapter.setList(list);
            }
        });
    }

    private void onRouteSelected(RouteHeader route) {
        requireContext().getSharedPreferences("AppConfig", android.content.Context.MODE_PRIVATE)
                .edit().putInt("last_opened_route_id", route.id).apply();
        
        Toast.makeText(getContext(), "Rota Ativa: " + route.name, Toast.LENGTH_SHORT).show();
        
        if (getParentFragment() instanceof MapParentFragment) {
            ((MapParentFragment) getParentFragment()).switchToMap();
        }
    }

    private void onEditNameClicked(RouteHeader route) {
        EditText input = new EditText(getContext());
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setText(route.name);
        input.setSelection(route.name.length());

        new AlertDialog.Builder(requireContext())
                .setTitle("Editar Nome da Rota")
                .setView(input)
                .setPositiveButton("Salvar", (dialog, which) -> {
                    String newName = input.getText().toString().trim();
                    if (!newName.isEmpty()) {
                        route.name = newName;
                        new Thread(() -> {
                            AppDatabase.getInstance(requireContext()).appDao().updateRouteHeader(route);
                            if (getActivity() != null) getActivity().runOnUiThread(() -> CloudSyncHelper.syncNow(requireContext()));
                        }).start();
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void onDeleteClicked(RouteHeader route) {
        View v = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_modern_confirm, null);
        TextView tt = v.findViewById(R.id.textModernTitle);
        TextView tm = v.findViewById(R.id.textModernMessage);
        com.google.android.material.button.MaterialButton bn = v.findViewById(R.id.btnModernNegative);
        com.google.android.material.button.MaterialButton bp = v.findViewById(R.id.btnModernPositive);

        tt.setText("Excluir Rota");
        tm.setText("Deseja excluir permanentemente a rota \"" + route.name + "\"?");
        bp.setText("EXCLUIR");

        AlertDialog d = new AlertDialog.Builder(requireContext()).setView(v).create();
        if (d.getWindow() != null) d.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        bn.setOnClickListener(v1 -> d.dismiss());
        bp.setOnClickListener(v1 -> {
            d.dismiss();
            new Thread(() -> {
                AppDatabase.getInstance(requireContext()).appDao().deleteRouteHeader(route);
                if (getActivity() != null) getActivity().runOnUiThread(() -> CloudSyncHelper.syncNow(requireContext()));
            }).start();
        });
        d.show();
    }

    private static class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {
        private final List<RouteHeader> list;
        private final OnRouteSelectedListener selectListener;
        private final OnActionClickListener editListener;
        private final OnActionClickListener deleteListener;
        private final SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

        interface OnRouteSelectedListener { void onSelected(RouteHeader item); }
        interface OnActionClickListener { void onClick(RouteHeader item); }

        HistoryAdapter(List<RouteHeader> list, 
                       OnRouteSelectedListener selectListener, 
                       OnActionClickListener editListener,
                       OnActionClickListener deleteListener) {
            this.list = list;
            this.selectListener = selectListener;
            this.editListener = editListener;
            this.deleteListener = deleteListener;
        }

        void setList(List<RouteHeader> newList) {
            list.clear();
            list.addAll(newList);
            notifyDataSetChanged();
        }

        @NonNull @Override public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_route_history, parent, false);
            return new ViewHolder(v);
        }

        @Override public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            RouteHeader item = list.get(position);
            holder.textName.setText(item.name);
            holder.textDate.setText(sdf.format(new java.util.Date(item.date)));
            
            if (item.isCompleted) {
                holder.textStatus.setVisibility(View.VISIBLE);
                if (item.failedCount > 0) {
                    holder.textStatus.setText("Concluída (" + item.failedCount + " falhas)");
                    holder.textStatus.setTextColor(Color.parseColor("#F44336")); // Red
                } else {
                    holder.textStatus.setText("Concluída");
                    holder.textStatus.setTextColor(Color.parseColor("#4CAF50")); // Green
                }
            } else {
                holder.textStatus.setVisibility(View.GONE);
            }
            
            holder.itemView.setOnClickListener(v -> selectListener.onSelected(item));
            holder.btnEdit.setOnClickListener(v -> editListener.onClick(item));
            holder.btnDelete.setOnClickListener(v -> deleteListener.onClick(item));
        }

        @Override public int getItemCount() { return list.size(); }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView textName, textDate, textStatus;
            ImageButton btnEdit, btnDelete;
            ViewHolder(View v) {
                super(v);
                textName = v.findViewById(R.id.textRouteName);
                textDate = v.findViewById(R.id.textRouteDate);
                textStatus = v.findViewById(R.id.textRouteStatus);
                btnEdit = v.findViewById(R.id.btnEditRouteName);
                btnDelete = v.findViewById(R.id.btnDeleteRoute);
            }
        }
    }
}
