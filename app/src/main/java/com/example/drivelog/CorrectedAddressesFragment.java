package com.example.drivelog;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class CorrectedAddressesFragment extends Fragment {

    private RecyclerView recyclerView;
    private CorrectedAdapter adapter;
    private TextView textEmpty;
    private android.widget.EditText editSearch;
    private List<CorrectedAddress> fullList = new ArrayList<>();
    private String currentUserId;

    private final ActivityResultLauncher<Intent> importLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) processImport(uri);
                }
            }
    );

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_corrected_addresses, container, false);
        
        recyclerView = view.findViewById(R.id.recyclerCorrected);
        textEmpty = view.findViewById(R.id.textEmpty);
        editSearch = view.findViewById(R.id.editSearchCorrected);
        
        currentUserId = requireContext().getSharedPreferences("AppConfig", android.content.Context.MODE_PRIVATE).getString("current_user_id", "anon");
        
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new CorrectedAdapter(new ArrayList<>(), currentUserId, this::onDeleteClicked);
        recyclerView.setAdapter(adapter);
        
        loadCorrected();

        if (editSearch != null) {
            editSearch.addTextChangedListener(new android.text.TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) { filter(s.toString()); }
                @Override public void afterTextChanged(android.text.Editable s) {}
            });
        }

        view.findViewById(R.id.btnExportCorrected).setOnClickListener(v -> showExportOptions());
        view.findViewById(R.id.btnImportCorrected).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            importLauncher.launch(intent);
        });
        
        return view;
    }

    private void loadCorrected() {
        AppDatabase.getInstance(requireContext()).appDao().getAllCorrectedAddressesLive().observe(getViewLifecycleOwner(), list -> {
            this.fullList = list;
            if (list == null || list.isEmpty()) {
                textEmpty.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.GONE);
            } else {
                textEmpty.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
                if (editSearch != null && !editSearch.getText().toString().isEmpty()) {
                    filter(editSearch.getText().toString());
                } else {
                    adapter.setList(list);
                }
            }
        });
    }

    private void filter(String query) {
        if (adapter != null) {
            adapter.filter(query);
            if (adapter.getItemCount() == 0 && !query.isEmpty()) {
                textEmpty.setVisibility(View.VISIBLE);
                textEmpty.setText("Nenhum endereço encontrado para '" + query + "'");
                recyclerView.setVisibility(View.GONE);
            } else if (fullList.isEmpty()) {
                textEmpty.setVisibility(View.VISIBLE);
                textEmpty.setText("Nenhum endereço fixado ainda.");
                recyclerView.setVisibility(View.GONE);
            } else {
                textEmpty.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
            }
        }
    }

    private void showExportOptions() {
        if (fullList.isEmpty()) {
            Toast.makeText(getContext(), "Nenhum endereço para exportar", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] options = {"Exportar Tudo", "Por Bairro/Pasta", "Selecionar Específico"};
        new AlertDialog.Builder(requireContext())
                .setTitle("Exportar Endereços")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) exportAddresses(fullList, "todos_enderecos");
                    else if (which == 1) showNeighborhoodExportPicker();
                    else showSpecificExportPicker();
                })
                .show();
    }

    private void showNeighborhoodExportPicker() {
        Map<String, List<CorrectedAddress>> grouped = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (CorrectedAddress a : fullList) {
            String key = a.neighborhood != null ? a.neighborhood : "Sem Bairro";
            if (!grouped.containsKey(key)) grouped.put(key, new ArrayList<>());
            grouped.get(key).add(a);
        }
        
        String[] names = grouped.keySet().toArray(new String[0]);
        new AlertDialog.Builder(requireContext())
                .setTitle("Escolha o Bairro")
                .setItems(names, (dialog, which) -> {
                    String selected = names[which];
                    exportAddresses(grouped.get(selected), "bairro_" + selected.replaceAll("[^a-zA-Z0-9]", "_"));
                })
                .show();
    }

    private void showSpecificExportPicker() {
        String[] addresses = new String[fullList.size()];
        boolean[] checked = new boolean[fullList.size()];
        for (int i = 0; i < fullList.size(); i++) addresses[i] = fullList.get(i).address;

        List<CorrectedAddress> selected = new ArrayList<>();
        new AlertDialog.Builder(requireContext())
                .setTitle("Selecione os Endereços")
                .setMultiChoiceItems(addresses, checked, (dialog, which, isChecked) -> {
                    if (isChecked) selected.add(fullList.get(which));
                    else selected.remove(fullList.get(which));
                })
                .setPositiveButton("Exportar", (dialog, which) -> {
                    if (!selected.isEmpty()) exportAddresses(selected, "enderecos_selecionados");
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void exportAddresses(List<CorrectedAddress> list, String filename) {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("address;neighborhood;city;latitude;longitude;updatedAt\n");
            for (CorrectedAddress a : list) {
                sb.append(a.address).append(";")
                  .append(a.neighborhood != null ? a.neighborhood : "").append(";")
                  .append(a.city != null ? a.city : "").append(";")
                  .append(a.latitude).append(";")
                  .append(a.longitude).append(";")
                  .append(a.updatedAt).append("\n");
            }

            File cacheFile = new File(requireContext().getCacheDir(), filename + ".dlf");
            try (FileOutputStream out = new FileOutputStream(cacheFile)) {
                out.write(sb.toString().getBytes());
            }

            Uri contentUri = FileProvider.getUriForFile(requireContext(), requireContext().getPackageName() + ".fileprovider", cacheFile);
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("application/octet-stream");
            intent.putExtra(Intent.EXTRA_STREAM, contentUri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(intent, "Compartilhar Endereços"));

        } catch (Exception e) {
            Toast.makeText(getContext(), "Erro ao exportar: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void processImport(Uri uri) {
        try {
            List<CorrectedAddress> imported = new ArrayList<>();
            InputStream in = requireContext().getContentResolver().openInputStream(uri);
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            String line;
            boolean first = true;
            while ((line = reader.readLine()) != null) {
                if (first) { first = false; continue; }
                String[] p = line.split(";");
                if (p.length >= 5) {
                    CorrectedAddress ca = new CorrectedAddress();
                    ca.address = p[0];
                    ca.neighborhood = p[1].isEmpty() ? null : p[1];
                    ca.city = p[2].isEmpty() ? null : p[2];
                    ca.latitude = Double.parseDouble(p[3]);
                    ca.longitude = Double.parseDouble(p[4]);
                    ca.updatedAt = p.length > 5 ? Long.parseLong(p[5]) : System.currentTimeMillis();
                    imported.add(ca);
                } else if (p.length == 4) { // Retrocompatibilidade
                    CorrectedAddress ca = new CorrectedAddress();
                    ca.address = p[0];
                    ca.neighborhood = p[1].isEmpty() ? null : p[1];
                    ca.latitude = Double.parseDouble(p[2]);
                    ca.longitude = Double.parseDouble(p[3]);
                    ca.updatedAt = System.currentTimeMillis();
                    imported.add(ca);
                }
            }
            reader.close();

            if (imported.isEmpty()) {
                Toast.makeText(getContext(), "Arquivo inválido ou vazio", Toast.LENGTH_SHORT).show();
                return;
            }

            showImportMixDialog(imported);

        } catch (Exception e) {
            Toast.makeText(getContext(), "Erro ao ler arquivo", Toast.LENGTH_SHORT).show();
        }
    }

    private void showImportMixDialog(List<CorrectedAddress> imported) {
        String[] display = new String[imported.size()];
        boolean[] checked = new boolean[imported.size()];
        for (int i = 0; i < imported.size(); i++) {
            CorrectedAddress a = imported.get(i);
            display[i] = (a.neighborhood != null ? "[" + a.neighborhood + "] " : "") + a.address;
            checked[i] = true;
        }

        Set<CorrectedAddress> selected = new HashSet<>(imported);

        new AlertDialog.Builder(requireContext())
                .setTitle("Escolha o que importar")
                .setMultiChoiceItems(display, checked, (dialog, which, isChecked) -> {
                    if (isChecked) selected.add(imported.get(which));
                    else selected.remove(imported.get(which));
                })
                .setPositiveButton("Importar e Mixar", (dialog, which) -> {
                    new Thread(() -> {
                        AppDao dao = AppDatabase.getInstance(requireContext()).appDao();
                        for (CorrectedAddress ca : selected) {
                            dao.insertCorrectedAddress(ca);
                        }
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                Toast.makeText(getContext(), "Endereços mixados com sucesso!", Toast.LENGTH_SHORT).show();
                                CloudSyncHelper.syncNow(requireContext());
                            });
                        }
                    }).start();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void onDeleteClicked(CorrectedAddress corrected) {
        boolean isAlreadyShared = corrected.creatorId != null && corrected.creatorId.equals(currentUserId);
        String coordsInfo = String.format(java.util.Locale.US, "📍 Lat: %.6f\n📍 Lon: %.6f", corrected.latitude, corrected.longitude);
        
        List<String> optionsList = new ArrayList<>();
        optionsList.add(coordsInfo);
        optionsList.add("Mover para Bairro/Pasta");
        optionsList.add("Remover Fixação");
        if (!isAlreadyShared) {
            optionsList.add("📤 Compartilhar com a Comunidade");
        }

        String[] options = optionsList.toArray(new String[0]);
        
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle(corrected.address)
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        try {
                            Uri gmmIntentUri = Uri.parse("geo:" + corrected.latitude + "," + corrected.longitude + "?q=" + corrected.latitude + "," + corrected.longitude + "(" + Uri.encode(corrected.address) + ")");
                            Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                            mapIntent.setPackage("com.google.android.apps.maps");
                            startActivity(mapIntent);
                        } catch (Exception e) {
                            Toast.makeText(getContext(), "Google Maps não encontrado", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        String selected = options[which];
                        if (selected.equals("Mover para Bairro/Pasta")) {
                            showMoveToNeighborhoodDialog(corrected);
                        } else if (selected.equals("Remover Fixação")) {
                            confirmDelete(corrected);
                        } else if (selected.contains("Compartilhar")) {
                            shareWithCommunity(corrected, currentUserId);
                        }
                    }
                })
                .setNegativeButton("Fechar", null)
                .show();
    }

    private void shareWithCommunity(CorrectedAddress corrected, String currentUserId) {
        String uName = requireContext().getSharedPreferences("AppConfig", android.content.Context.MODE_PRIVATE).getString("profile_name", "Entregador");
        FirebaseHelper.uploadCorrection(currentUserId, uName, corrected, new FirebaseHelper.GlobalUploadCallback() {
            @Override
            public void onSuccess() {
                new Thread(() -> {
                    corrected.creatorId = currentUserId;
                    AppDatabase.getInstance(requireContext()).appDao().updateCorrectedAddress(corrected);
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> Toast.makeText(getContext(), "Compartilhado com sucesso!", Toast.LENGTH_SHORT).show());
                    }
                }).start();
            }

            @Override
            public void onFailure(String msg) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> Toast.makeText(getContext(), "Erro ao compartilhar: " + msg, Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    private void showMoveToNeighborhoodDialog(CorrectedAddress corrected) {
        new Thread(() -> {
            List<CorrectedAddress> all = AppDatabase.getInstance(requireContext()).appDao().getAllCorrectedAddresses();
            java.util.Set<String> neighborhoods = new java.util.TreeSet<>(String.CASE_INSENSITIVE_ORDER);
            for (CorrectedAddress addr : all) {
                if (addr.neighborhood != null && !addr.neighborhood.isEmpty()) {
                    neighborhoods.add(addr.neighborhood);
                }
            }
            List<String> neighborhoodList = new ArrayList<>(neighborhoods);

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    android.widget.LinearLayout layout = new android.widget.LinearLayout(requireContext());
                    layout.setOrientation(android.widget.LinearLayout.VERTICAL);
                    layout.setPadding(50, 40, 50, 10);

                    final android.widget.AutoCompleteTextView input = new android.widget.AutoCompleteTextView(requireContext());
                    input.setHint("Nome do Bairro ou Pasta");
                    input.setText(corrected.neighborhood != null ? corrected.neighborhood : "");
                    
                    android.widget.ArrayAdapter<String> suggestAdapter = new android.widget.ArrayAdapter<>(requireContext(),
                            android.R.layout.simple_dropdown_item_1line, neighborhoodList);
                    input.setAdapter(suggestAdapter);
                    input.setThreshold(1); // Sugere ao digitar 1 letra

                    layout.addView(input);

                    new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                            .setTitle("Organizar em Pasta")
                            .setMessage(neighborhoodList.isEmpty() ? 
                                    "Digite o nome da pasta para agrupar." : 
                                    "Escolha uma pasta existente ou digite uma nova.")
                            .setView(layout)
                            .setPositiveButton("Mover", (d, w) -> {
                                String neighborhood = input.getText().toString().trim();
                                new Thread(() -> {
                                    corrected.neighborhood = neighborhood.isEmpty() ? null : neighborhood;
                                    AppDatabase.getInstance(requireContext()).appDao().updateCorrectedAddress(corrected);
                                    if (getActivity() != null) {
                                        getActivity().runOnUiThread(() -> CloudSyncHelper.syncNow(requireContext()));
                                    }
                                }).start();
                            })
                            .setNegativeButton("Cancelar", null)
                            .show();
                });
            }
        }).start();
    }

    private void confirmDelete(CorrectedAddress corrected) {
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Remover Fixação")
                .setMessage("Deseja remover a correção deste endereço?")
                .setPositiveButton("Remover", (d, w) -> {
                    new Thread(() -> {
                        AppDatabase.getInstance(requireContext()).appDao().deleteCorrectedAddress(corrected);
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> CloudSyncHelper.syncNow(requireContext()));
                        }
                    }).start();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private static class CorrectedAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private static final int TYPE_CITY = 0;
        private static final int TYPE_NEIGHBORHOOD = 1;
        private static final int TYPE_ITEM = 2;

        private final List<Object> displayList = new ArrayList<>();
        private final OnDeleteListener listener;
        private final String currentUserId;
        private List<CorrectedAddress> originalList = new ArrayList<>();
        private String currentFilter = "";

        interface OnDeleteListener { void onDelete(CorrectedAddress item); }

        CorrectedAdapter(List<CorrectedAddress> list, String currentUserId, OnDeleteListener listener) {
            this.listener = listener;
            this.currentUserId = currentUserId;
            setList(list);
        }

        void setList(List<CorrectedAddress> newList) {
            this.originalList = newList;
            rebuildDisplayList();
        }

        void filter(String query) {
            this.currentFilter = query.toLowerCase().trim();
            rebuildDisplayList();
        }

        private void rebuildDisplayList() {
            displayList.clear();
            
            // Map<City, Map<Neighborhood, List<CorrectedAddress>>>
            Map<String, Map<String, List<CorrectedAddress>>> hierarchy = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

            for (CorrectedAddress addr : originalList) {
                if (!currentFilter.isEmpty()) {
                    boolean matches = (addr.address != null && addr.address.toLowerCase().contains(currentFilter)) ||
                            (addr.neighborhood != null && addr.neighborhood.toLowerCase().contains(currentFilter)) ||
                            (addr.city != null && addr.city.toLowerCase().contains(currentFilter));
                    if (!matches) continue;
                }

                String cityKey = (addr.city != null && !addr.city.isEmpty()) ? addr.city : "Minha Cidade";
                String neighborhoodKey = (addr.neighborhood != null && !addr.neighborhood.isEmpty()) ? addr.neighborhood : "Sem Bairro / Diversos";
                
                if (!hierarchy.containsKey(cityKey)) hierarchy.put(cityKey, new TreeMap<>(String.CASE_INSENSITIVE_ORDER));
                Map<String, List<CorrectedAddress>> cityMap = hierarchy.get(cityKey);
                
                if (!cityMap.containsKey(neighborhoodKey)) cityMap.put(neighborhoodKey, new ArrayList<>());
                cityMap.get(neighborhoodKey).add(addr);
            }

            for (Map.Entry<String, Map<String, List<CorrectedAddress>>> cityEntry : hierarchy.entrySet()) {
                displayList.add(new CityHeader(cityEntry.getKey()));
                for (Map.Entry<String, List<CorrectedAddress>> nbEntry : cityEntry.getValue().entrySet()) {
                    displayList.add(new NeighborhoodHeader(nbEntry.getKey()));
                    displayList.addAll(nbEntry.getValue());
                }
            }
            notifyDataSetChanged();
        }

        @Override public int getItemViewType(int position) {
            Object obj = displayList.get(position);
            if (obj instanceof CityHeader) return TYPE_CITY;
            if (obj instanceof NeighborhoodHeader) return TYPE_NEIGHBORHOOD;
            return TYPE_ITEM;
        }

        @NonNull @Override public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            if (viewType == TYPE_CITY) {
                View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_community_city, parent, false);
                return new CityViewHolder(v);
            } else if (viewType == TYPE_NEIGHBORHOOD) {
                View v = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_1, parent, false);
                v.setPadding(32, 0, 0, 0); // Indentação para bairro
                v.setBackgroundColor(0xFFF0F0F0);
                return new NeighborhoodViewHolder(v);
            } else {
                View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_my_address, parent, false);
                return new ItemViewHolder(v);
            }
        }

        @Override public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            Object obj = displayList.get(position);
            if (holder instanceof CityViewHolder) {
                CityHeader header = (CityHeader) obj;
                CityViewHolder h = (CityViewHolder) holder;
                h.text.setText(header.name);
                h.imgFav.setVisibility(View.GONE); // Não usamos favoritos na aba 'Meus'
            } else if (holder instanceof NeighborhoodViewHolder) {
                NeighborhoodHeader header = (NeighborhoodHeader) obj;
                NeighborhoodViewHolder h = (NeighborhoodViewHolder) holder;
                h.text.setText(" bairro: " + header.name);
                h.text.setTextSize(13);
                h.text.setTextColor(0xFF888888);
            } else if (holder instanceof ItemViewHolder) {
                CorrectedAddress item = (CorrectedAddress) obj;
                ItemViewHolder h = (ItemViewHolder) holder;
                h.textAddress.setText(item.address);
                
                String statusText = "";
                int color = 0xFF888888;
                if (item.creatorId == null) {
                    statusText = "Local (Não enviado)";
                    color = 0xFF4CAF50; // Verde
                } else if (item.creatorId.equals(currentUserId)) {
                    statusText = "Comunidade (Sincronizado)";
                    color = 0xFF2196F3; // Azul
                } else {
                    statusText = "Comunidade (Baixado)";
                    color = 0xFFFF9800; // Laranja
                }
                
                h.textStatus.setText(statusText);
                h.textStatus.setTextColor(color);
                
                String coords = "Lat: " + String.format("%.6f", item.latitude) + " | Lon: " + String.format("%.6f", item.longitude);
                h.textCoords.setText(coords);

                h.imgMenu.setOnClickListener(v -> listener.onDelete(item));
                h.itemView.setOnClickListener(v -> listener.onDelete(item));
            }
        }

        @Override public int getItemCount() { return displayList.size(); }

        static class CityHeader { String name; CityHeader(String name) { this.name = name; } }
        static class NeighborhoodHeader { String name; NeighborhoodHeader(String name) { this.name = name; } }

        static class CityViewHolder extends RecyclerView.ViewHolder {
            TextView text; ImageView imgFav;
            CityViewHolder(View v) { super(v); text = v.findViewById(R.id.textCityName); imgFav = v.findViewById(R.id.imgCityFav); }
        }

        static class NeighborhoodViewHolder extends RecyclerView.ViewHolder {
            TextView text;
            NeighborhoodViewHolder(View v) { super(v); text = v.findViewById(android.R.id.text1); }
        }

        static class ItemViewHolder extends RecyclerView.ViewHolder {
            TextView textAddress, textStatus, textCoords;
            ImageView imgMenu;
            ItemViewHolder(View v) {
                super(v);
                textAddress = v.findViewById(R.id.textMyAddress);
                textStatus = v.findViewById(R.id.textMyStatus);
                textCoords = v.findViewById(R.id.textMyCoords);
                imgMenu = v.findViewById(R.id.imgActionMenu);
            }
        }
    }
}
