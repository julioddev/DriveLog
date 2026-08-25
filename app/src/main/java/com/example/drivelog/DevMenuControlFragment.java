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
        menuList.add(new MenuItemModel("km", "Geral: Rastreamento (KM) e Controles no Mapa"));
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
        menuList.add(new MenuItemModel("settings_users", "Ajustes: DEV (Usuários Dev)"));
        menuList.add(new MenuItemModel("premium_features", "Ajustes: Seções Avançadas (Assinatura/Modo App/Amigos)"));
        menuList.add(new MenuItemModel("btn_check_updates", "Ajustes: Botão Verificar Atualizações"));
    }

    private void fetchCurrentConfig() {
        FirebaseHelper.listenRemoteMenuConfigAll((d0, d1, d2) -> {
            if (!isAdded()) return;
            
            for (MenuItemModel item : menuList) {
                item.isSub0 = false;
                item.isSub1 = false;
                item.isSub2 = false;

                for (DocumentSnapshot doc : d0) if (doc.getId().equals(item.id)) { item.isSub0 = true; break; }
                for (DocumentSnapshot doc : d1) if (doc.getId().equals(item.id)) { item.isSub1 = true; break; }
                for (DocumentSnapshot doc : d2) if (doc.getId().equals(item.id)) { item.isSub2 = true; break; }
            }

            // Regra fixa para abas críticas de desenvolvedor (Sempre Sub 2)
            for (MenuItemModel item : menuList) {
                if (item.id.equals("settings_dev") || item.id.equals("settings_menu") || item.id.equals("settings_emails") || item.id.equals("settings_users")) {
                    item.isSub0 = false; item.isSub1 = false; item.isSub2 = true;
                }
            }
            if (adapter != null) adapter.notifyDataSetChanged();
        });
    }

    private void saveConfig() {
        if (getContext() == null) return;
        Toast.makeText(getContext(), "Salvando configurações...", Toast.LENGTH_SHORT).show();
        
        for (MenuItemModel item : menuList) {
            // Regra de segurança
            if (item.id.equals("settings_dev") || item.id.equals("settings_menu") || 
                item.id.equals("settings_emails") || item.id.equals("settings_users")) {
                item.isSub0 = false; item.isSub1 = false; item.isSub2 = true;
            }
            FirebaseHelper.updateRemoteMenuConfig(item.id, item.isSub0, item.isSub1, item.isSub2, null);
        }
        
        Toast.makeText(getContext(), "Configurações salvas!", Toast.LENGTH_SHORT).show();
    }

    private static class MenuItemModel {
        String id, description; boolean isSub0, isSub1, isSub2;
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
            
            boolean isLockedDev = item.id.equals("settings_dev") || item.id.equals("settings_menu") || item.id.equals("settings_emails") || item.id.equals("settings_users");
            
            holder.checkSub0.setOnCheckedChangeListener(null);
            holder.checkSub1.setOnCheckedChangeListener(null);
            holder.checkSub2.setOnCheckedChangeListener(null);

            holder.checkSub0.setChecked(item.isSub0);
            holder.checkSub1.setChecked(item.isSub1);
            holder.checkSub2.setChecked(item.isSub2);

            if (isLockedDev) {
                holder.checkSub0.setVisibility(View.GONE);
                holder.checkSub1.setVisibility(View.GONE);
                holder.checkSub2.setEnabled(false);
            } else {
                holder.checkSub0.setVisibility(View.VISIBLE);
                holder.checkSub1.setVisibility(View.VISIBLE);
                holder.checkSub2.setEnabled(true);

                holder.checkSub0.setOnCheckedChangeListener((v, c) -> item.isSub0 = c);
                holder.checkSub1.setOnCheckedChangeListener((v, c) -> item.isSub1 = c);
                holder.checkSub2.setOnCheckedChangeListener((v, c) -> item.isSub2 = c);
            }
        }
        @Override public int getItemCount() { return items.size(); }
        class ViewHolder extends RecyclerView.ViewHolder {
            TextView textId, textDesc; CheckBox checkSub0, checkSub1, checkSub2;
            ViewHolder(View v) { 
                super(v); 
                textId = v.findViewById(R.id.textMenuId); 
                textDesc = v.findViewById(R.id.textMenuDescription); 
                checkSub0 = v.findViewById(R.id.checkSub0); 
                checkSub1 = v.findViewById(R.id.checkSub1); 
                checkSub2 = v.findViewById(R.id.checkSub2); 
            }
        }
    }
}
