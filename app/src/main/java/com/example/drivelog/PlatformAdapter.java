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
import java.util.Locale;

public class PlatformAdapter extends RecyclerView.Adapter<PlatformAdapter.ViewHolder> {

    private List<Platform> platforms;
    private final AppDao dao;
    private final Context context;

    public PlatformAdapter(Context context, List<Platform> platforms, AppDao dao) {
        this.context = context;
        this.platforms = platforms;
        this.dao = dao;
    }

    public void setPlatforms(List<Platform> platforms) {
        this.platforms = platforms;
        notifyDataSetChanged();
    }

    public void onItemMove(int fromPosition, int toPosition) {
        java.util.Collections.swap(platforms, fromPosition, toPosition);
        notifyItemMoved(fromPosition, toPosition);
        
        // Update all indexes in DB
        new Thread(() -> {
            for (int i = 0; i < platforms.size(); i++) {
                Platform p = platforms.get(i);
                p.orderIndex = i;
                dao.updatePlatform(p);
            }
            CloudSyncHelper.syncNow(context);
        }).start();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_platform_config, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Platform platform = platforms.get(position);
        holder.textName.setText(platform.name);
        holder.textValue.setText(String.format(Locale.getDefault(), "Base: R$ %.2f", platform.defaultValue));
        holder.switchEnabled.setChecked(platform.isEnabled);

        holder.switchEnabled.setOnCheckedChangeListener((buttonView, isChecked) -> {
            platform.isEnabled = isChecked;
            new Thread(() -> {
                dao.updatePlatform(platform);
                CloudSyncHelper.syncNow(holder.itemView.getContext());
            }).start();
        });

        holder.btnEdit.setOnClickListener(v -> showEditDialog(holder.itemView.getContext(), platform));

        holder.btnDelete.setOnClickListener(v -> {
            View dv = LayoutInflater.from(context).inflate(R.layout.dialog_modern_confirm, null);
            TextView tt = dv.findViewById(R.id.textModernTitle);
            TextView tm = dv.findViewById(R.id.textModernMessage);
            com.google.android.material.button.MaterialButton bn = dv.findViewById(R.id.btnModernNegative);
            com.google.android.material.button.MaterialButton bp = dv.findViewById(R.id.btnModernPositive);

            tt.setText("Excluir Plataforma");
            tm.setText("Deseja excluir " + platform.name + "?");
            bp.setText("EXCLUIR");

            AlertDialog dialog = new AlertDialog.Builder(context).setView(dv).create();
            if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            bn.setOnClickListener(v1 -> dialog.dismiss());
            bp.setOnClickListener(v1 -> {
                dialog.dismiss();
                new Thread(() -> {
                    dao.deletePlatform(platform);
                    CloudSyncHelper.syncNow(context, "Plataforma Excluída");
                }).start();
            });
            dialog.show();
        });
    }

    private void showEditDialog(Context context, Platform platform) {
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_add_platform, null);
        EditText editName = dialogView.findViewById(R.id.editNewPlatformName);
        EditText editValue = dialogView.findViewById(R.id.editNewPlatformValue);
        
        editName.setText(platform.name);
        editValue.setText(String.format(Locale.getDefault(), "%.2f", platform.defaultValue));

        new AlertDialog.Builder(context)
                .setTitle("Editar Plataforma")
                .setView(dialogView)
                .setPositiveButton("Salvar", (dialog, which) -> {
                    platform.name = editName.getText().toString();
                    String valStr = editValue.getText().toString();
                    platform.defaultValue = valStr.isEmpty() ? 0 : Double.parseDouble(valStr.replace(",", "."));
                    new Thread(() -> {
                        dao.updatePlatform(platform);
                        CloudSyncHelper.syncNow(context);
                    }).start();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    @Override
    public int getItemCount() {
        return platforms.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textName, textValue;
        MaterialSwitch switchEnabled;
        ImageButton btnEdit, btnDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textName = itemView.findViewById(R.id.textPlatformName);
            textValue = itemView.findViewById(R.id.textPlatformValue);
            switchEnabled = itemView.findViewById(R.id.switchPlatformEnabled);
            btnEdit = itemView.findViewById(R.id.btnEditPlatform);
            btnDelete = itemView.findViewById(R.id.btnDeletePlatform);
        }
    }
}
