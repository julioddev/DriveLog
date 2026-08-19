package com.example.drivelog;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class DevMenuControlFragment extends Fragment {

    private RecyclerView recyclerView;
    private MenuAdapter adapter;
    private final List<MenuItemModel> menuList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dev_menu_control, container, false);

        recyclerView = view.findViewById(R.id.recyclerDevMenuControl);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        
        setupMenuList();
        adapter = new MenuAdapter(menuList);
        recyclerView.setAdapter(adapter);

        view.findViewById(R.id.btnSaveMenuConfig).setOnClickListener(v -> saveConfig());

        fetchCurrentConfig();

        return view;
    }

    private void setupMenuList() {
        menuList.clear();
        menuList.add(new MenuItemModel("maps", "Aba Principal: Mapa"));
        menuList.add(new MenuItemModel("earnings", "Aba Principal: Ganhos"));
        menuList.add(new MenuItemModel("km", "Aba Principal: Rastreamento (KM)"));
        menuList.add(new MenuItemModel("fuel", "Aba Principal: Abastecimento"));
        menuList.add(new MenuItemModel("maintenance", "Aba Principal: Manutenção"));
        menuList.add(new MenuItemModel("reports", "Aba Principal: Relatórios"));
        menuList.add(new MenuItemModel("friends", "Aba Principal: Amigos"));
        menuList.add(new MenuItemModel("corrected_addresses", "Gaveta: Endereços Corrigidos"));
        menuList.add(new MenuItemModel("settings", "Aba Principal: Ajustes"));
        menuList.add(new MenuItemModel("settings_general", "Ajustes: Geral"));
        menuList.add(new MenuItemModel("settings_map", "Ajustes: Mapa"));
        menuList.add(new MenuItemModel("settings_tracking", "Ajustes: Rastreamento"));
        menuList.add(new MenuItemModel("settings_platforms", "Ajustes: Plataformas"));
        menuList.add(new MenuItemModel("settings_features", "Ajustes: Recursos"));
        menuList.add(new MenuItemModel("settings_dev", "Ajustes: DEV (Scanner/Alertas)"));
        menuList.add(new MenuItemModel("settings_menu", "Ajustes: DEV (Menu Remoto)"));
        menuList.add(new MenuItemModel("settings_emails", "Ajustes: DEV (Emails Dev)"));
        menuList.add(new MenuItemModel("btn_check_updates", "Ajustes: Botão Verificar Atualizações"));
    }

    private void fetchCurrentConfig() {
        FirebaseHelper.listenRemoteMenuConfigAll((userDocs, devDocs) -> {
            if (!isAdded()) return;
            
            // Reseta antes de aplicar
            for (MenuItemModel item : menuList) {
                item.isPublic = false;
                item.isDevOnly = false;
            }

            for (DocumentSnapshot doc : userDocs) {
                for (MenuItemModel item : menuList) {
                    if (item.id.equals(doc.getId())) {
                        item.isPublic = true;
                        break;
                    }
                }
            }

            for (DocumentSnapshot doc : devDocs) {
                for (MenuItemModel item : menuList) {
                    if (item.id.equals(doc.getId())) {
                        item.isDevOnly = true;
                        break;
                    }
                }
            }

            // Regra fixa para abas críticas de desenvolvedor
            for (MenuItemModel item : menuList) {
                if (item.id.equals("settings_dev") || item.id.equals("settings_menu") || item.id.equals("settings_emails")) {
                    item.isPublic = false;
                    item.isDevOnly = true;
                }
            }
            if (adapter != null) adapter.notifyDataSetChanged();
        });
    }

    private void saveConfig() {
        if (getContext() == null) return;
        Toast.makeText(getContext(), "Salvando configurações de menu...", Toast.LENGTH_SHORT).show();
        
        for (MenuItemModel item : menuList) {
            FirebaseHelper.updateRemoteMenuConfig(item.id, item.isPublic, item.isDevOnly, null);
        }
        
        Toast.makeText(getContext(), "Configurações de menu salvas!", Toast.LENGTH_SHORT).show();
    }

    private static class MenuItemModel {
        String id, description; boolean isPublic, isDevOnly;
        MenuItemModel(String id, String desc) { this.id = id; this.description = desc; }
    }

    private class MenuAdapter extends RecyclerView.Adapter<MenuAdapter.ViewHolder> {
        private final List<MenuItemModel> items;
        MenuAdapter(List<MenuItemModel> items) { this.items = items; }
        @NonNull @Override public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) { 
            return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_dev_menu_config, parent, false)); 
        }
        @Override public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            MenuItemModel item = items.get(position);
            holder.textId.setText(item.id); holder.textDesc.setText(item.description);
            
            boolean isLockedDev = item.id.equals("settings_dev") || item.id.equals("settings_menu") || item.id.equals("settings_emails");
            
            if (isLockedDev) {
                item.isPublic = false; item.isDevOnly = true;
                holder.checkPublic.setVisibility(View.GONE); 
                holder.checkDevOnly.setChecked(true); 
                holder.checkDevOnly.setEnabled(false);
            } else {
                holder.checkPublic.setVisibility(View.VISIBLE); 
                holder.checkDevOnly.setEnabled(true);
                holder.checkPublic.setOnCheckedChangeListener(null); 
                holder.checkPublic.setChecked(item.isPublic);
                holder.checkPublic.setOnCheckedChangeListener((v, checked) -> item.isPublic = checked);
                holder.checkDevOnly.setOnCheckedChangeListener(null); 
                holder.checkDevOnly.setChecked(item.isDevOnly);
                holder.checkDevOnly.setOnCheckedChangeListener((v, checked) -> item.isDevOnly = checked);
            }
        }
        @Override public int getItemCount() { return items.size(); }
        class ViewHolder extends RecyclerView.ViewHolder {
            TextView textId, textDesc; CheckBox checkPublic, checkDevOnly;
            ViewHolder(View v) { 
                super(v); 
                textId = v.findViewById(R.id.textMenuId); 
                textDesc = v.findViewById(R.id.textMenuDescription); 
                checkPublic = v.findViewById(R.id.checkPublic); 
                checkDevOnly = v.findViewById(R.id.checkDevOnly); 
            }
        }
    }
}
