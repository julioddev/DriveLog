package com.example.drivelog;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.materialswitch.MaterialSwitch;
import java.util.List;

public class GasStationAdapter extends RecyclerView.Adapter<GasStationAdapter.ViewHolder> {

    private List<GasStation> stations;
    private final AppDao dao;
    private final Context context;

    public GasStationAdapter(Context context, List<GasStation> stations, AppDao dao) {
        this.context = context;
        this.stations = stations;
        this.dao = dao;
    }

    public void setStations(List<GasStation> stations) {
        this.stations = stations;
        notifyDataSetChanged();
    }

    public void onItemMove(int fromPosition, int toPosition) {
        java.util.Collections.swap(stations, fromPosition, toPosition);
        notifyItemMoved(fromPosition, toPosition);

        new Thread(() -> {
            for (int i = 0; i < stations.size(); i++) {
                GasStation s = stations.get(i);
                s.orderIndex = i;
                dao.updateGasStation(s);
            }
            CloudSyncHelper.syncNow(context);
        }).start();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_gas_station_config, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        GasStation station = stations.get(position);
        holder.textName.setText(station.name);
        holder.textDefaultStatus.setVisibility(station.isDefault ? View.VISIBLE : View.GONE);
        holder.switchEnabled.setChecked(station.isEnabled);
        
        holder.btnSetDefault.setImageResource(station.isDefault ? android.R.drawable.btn_star_big_on : android.R.drawable.btn_star_big_off);
        if (station.isDefault) holder.btnSetDefault.setColorFilter(holder.itemView.getContext().getResources().getColor(android.R.color.holo_orange_dark));
        else holder.btnSetDefault.clearColorFilter();

        holder.switchEnabled.setOnCheckedChangeListener((buttonView, isChecked) -> {
            station.isEnabled = isChecked;
            new Thread(() -> {
                dao.updateGasStation(station);
                CloudSyncHelper.syncNow(holder.itemView.getContext());
            }).start();
        });

        holder.btnEdit.setOnClickListener(v -> showEditDialog(holder.itemView.getContext(), station));

        holder.btnSetDefault.setOnClickListener(v -> {
            new Thread(() -> {
                for (GasStation s : dao.getAllGasStations()) {
                    s.isDefault = (s.id == station.id);
                    dao.updateGasStation(s);
                }
                CloudSyncHelper.syncNow(holder.itemView.getContext());
            }).start();
        });

        holder.btnDelete.setOnClickListener(v -> {
            View dv = LayoutInflater.from(context).inflate(R.layout.dialog_modern_confirm, null);
            TextView tt = dv.findViewById(R.id.textModernTitle);
            TextView tm = dv.findViewById(R.id.textModernMessage);
            com.google.android.material.button.MaterialButton bn = dv.findViewById(R.id.btnModernNegative);
            com.google.android.material.button.MaterialButton bp = dv.findViewById(R.id.btnModernPositive);

            tt.setText("Excluir Posto");
            tm.setText("Deseja realmente excluir o posto " + station.name + "?");
            bp.setText("EXCLUIR");

            AlertDialog dialog = new AlertDialog.Builder(context).setView(dv).create();
            if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            bn.setOnClickListener(v1 -> dialog.dismiss());
            bp.setOnClickListener(v1 -> {
                dialog.dismiss();
                new Thread(() -> {
                    dao.deleteGasStation(station);
                    CloudSyncHelper.syncNow(context);
                }).start();
            });
            dialog.show();
        });
    }

    private void showEditDialog(Context context, GasStation station) {
        EditText input = new EditText(context);
        input.setText(station.name);
        new AlertDialog.Builder(context)
                .setTitle("Editar Posto")
                .setView(input)
                .setPositiveButton("Salvar", (dialog, which) -> {
                    station.name = input.getText().toString();
                    new Thread(() -> {
                        dao.updateGasStation(station);
                        CloudSyncHelper.syncNow(context);
                    }).start();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    @Override
    public int getItemCount() {
        return stations.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textName, textDefaultStatus;
        MaterialSwitch switchEnabled;
        ImageButton btnEdit, btnDelete, btnSetDefault;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textName = itemView.findViewById(R.id.textStationName);
            textDefaultStatus = itemView.findViewById(R.id.textDefaultStatus);
            switchEnabled = itemView.findViewById(R.id.switchStationEnabled);
            btnEdit = itemView.findViewById(R.id.btnEditStation);
            btnDelete = itemView.findViewById(R.id.btnDeleteStation);
            btnSetDefault = itemView.findViewById(R.id.btnSetDefaultStation);
        }
    }
}
