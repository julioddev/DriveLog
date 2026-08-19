package com.example.drivelog;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.text.Editable;
import android.text.TextWatcher;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class CommunityAddressesFragment extends Fragment {

    private RecyclerView recyclerView;
    private CommunityAdapter adapter;
    private TextView textEmpty;
    private SwipeRefreshLayout swipeRefresh;
    private EditText editSearch;
    private SharedPreferences prefs;
    private com.google.firebase.firestore.ListenerRegistration communityListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_community_addresses, container, false);
        
        recyclerView = view.findViewById(R.id.recyclerCommunity);
        textEmpty = view.findViewById(R.id.textEmptyCommunity);
        swipeRefresh = view.findViewById(R.id.swipeRefreshCommunity);
        editSearch = view.findViewById(R.id.editSearchCommunity);
        
        prefs = requireContext().getSharedPreferences("AppConfig", Context.MODE_PRIVATE);
        
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new CommunityAdapter(new ArrayList<>(), this::toggleFavoriteCity, this::onLikeClicked, this::onDislikeClicked, this::showCommentsDialog, this::onDownloadClicked);
        recyclerView.setAdapter(adapter);
        
        swipeRefresh.setOnRefreshListener(this::loadCommunityData);
        loadCommunityData();

        editSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.filter(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });
        
        return view;
    }

    private void loadCommunityData() {
        if (communityListener != null) communityListener.remove();
        
        swipeRefresh.setRefreshing(true);
        communityListener = FirebaseHelper.listenCommunityAddresses(new FirebaseHelper.CommunityFetchCallback() {
            @Override
            public void onSuccess(List<CorrectedAddress> list) {
                if (isAdded()) {
                    getActivity().runOnUiThread(() -> {
                        swipeRefresh.setRefreshing(false);
                        if (list.isEmpty()) {
                            Log.d("Community", "Lista da comunidade veio vazia do Firebase.");
                            textEmpty.setVisibility(View.VISIBLE);
                            textEmpty.setText("Nenhuma correção global encontrada no momento.");
                            recyclerView.setVisibility(View.GONE);
                        } else {
                            Log.d("Community", "Exibindo " + list.size() + " endereços da comunidade.");
                            textEmpty.setVisibility(View.GONE);
                            recyclerView.setVisibility(View.VISIBLE);
                            adapter.setData(list);
                        }
                    });
                }
            }

            @Override
            public void onError(String msg) {
                if (isAdded()) {
                    getActivity().runOnUiThread(() -> {
                        swipeRefresh.setRefreshing(false);
                        Toast.makeText(getContext(), "Erro: " + msg, Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (communityListener != null) {
            communityListener.remove();
        }
    }

    private void toggleFavoriteCity(String city) {
        Set<String> favorites = new HashSet<>(prefs.getStringSet("favorite_cities", new HashSet<>()));
        if (favorites.contains(city)) {
            favorites.remove(city);
        } else {
            favorites.add(city);
        }
        prefs.edit().putStringSet("favorite_cities", favorites).apply();
        adapter.updateFavorites(favorites);
    }

    private void onLikeClicked(CorrectedAddress item) {
        String userName = prefs.getString("profile_name", "Entregador");
        String userId = prefs.getString("current_user_id", "anon");
        FirebaseHelper.addFeedback(item.address, true, null, userName, userId);
        Toast.makeText(getContext(), "Voto processado!", Toast.LENGTH_SHORT).show();
    }

    private void onDislikeClicked(CorrectedAddress item) {
        String userName = prefs.getString("profile_name", "Entregador");
        String userId = prefs.getString("current_user_id", "anon");
        FirebaseHelper.addFeedback(item.address, false, null, userName, userId);
        Toast.makeText(getContext(), "Voto processado!", Toast.LENGTH_SHORT).show();
    }

    private void onDownloadClicked(CorrectedAddress item) {
        new Thread(() -> {
            AppDatabase.getInstance(requireContext()).appDao().insertCorrectedAddress(item);
            if (getActivity() != null) getActivity().runOnUiThread(() -> 
                Toast.makeText(getContext(), "Endereço salvo nos seus registros!", Toast.LENGTH_SHORT).show());
        }).start();
    }

    private void showCommentsDialog(CorrectedAddress item) {
        android.widget.LinearLayout layout = new android.widget.LinearLayout(requireContext());
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(40, 20, 40, 20);

        TextView textTitle = new TextView(requireContext());
        textTitle.setText("Comentários para este endereço:");
        textTitle.setPadding(0, 0, 0, 20);
        layout.addView(textTitle);

        final android.widget.LinearLayout listContainer = new android.widget.LinearLayout(requireContext());
        listContainer.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.addView(listContainer);

        TextView textLoading = new TextView(requireContext());
        textLoading.setText("Carregando comentários...");
        listContainer.addView(textLoading);

        FirebaseHelper.fetchComments(item.address, new FirebaseHelper.CommentsFetchCallback() {
            @Override
            public void onSuccess(List<FirebaseHelper.CommentsFetchCallback.CommentModel> comments) {
                if (isAdded()) {
                    getActivity().runOnUiThread(() -> {
                        listContainer.removeAllViews();
                        if (comments.isEmpty()) {
                            TextView empty = new TextView(requireContext());
                            empty.setText("Nenhum comentário ainda.");
                            listContainer.addView(empty);
                        } else {
                            for (FirebaseHelper.CommentsFetchCallback.CommentModel c : comments) {
                                TextView tv = new TextView(requireContext());
                                tv.setText(c.user + ": " + c.text);
                                tv.setPadding(0, 10, 0, 10);
                                listContainer.addView(tv);
                            }
                        }
                    });
                }
            }

            @Override
            public void onError(String msg) {
                if (isAdded()) {
                    getActivity().runOnUiThread(() -> {
                        listContainer.removeAllViews();
                        TextView error = new TextView(requireContext());
                        error.setText("Erro ao carregar: " + msg);
                        listContainer.addView(error);
                    });
                }
            }
        });

        final android.widget.EditText input = new android.widget.EditText(requireContext());
        input.setHint("Escreva um comentário...");
        layout.addView(input);

        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Comunidade: " + item.address)
                .setView(layout)
                .setPositiveButton("Comentar", (d, w) -> {
                    String comment = input.getText().toString().trim();
                    if (!comment.isEmpty()) {
                        String userName = prefs.getString("profile_name", "Entregador");
                        String userId = prefs.getString("current_user_id", "anon");
                        FirebaseHelper.addFeedback(item.address, null, comment, userName, userId);
                        Toast.makeText(getContext(), "Comentário enviado!", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Fechar", null)
                .show();
    }

    private static class CommunityAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private static final int TYPE_CITY = 0;
        private static final int TYPE_NEIGHBORHOOD = 1;
        private static final int TYPE_ITEM = 2;

        private final List<Object> displayList = new ArrayList<>();
        private final OnFavoriteListener favoriteListener;
        private final OnItemInteractionListener interactionListener;
        private final OnItemInteractionListener dislikeListener;
        private final OnItemInteractionListener commentListener;
        private final OnItemInteractionListener downloadListener;

        private Set<String> favorites = new HashSet<>();
        private List<CorrectedAddress> originalList = new ArrayList<>();
        private String currentFilter = "";

        interface OnFavoriteListener { void onToggle(String city); }
        interface OnItemInteractionListener { void onAction(CorrectedAddress item); }

        CommunityAdapter(List<CorrectedAddress> list, 
                         OnFavoriteListener favoriteListener, 
                         OnItemInteractionListener likeListener,
                         OnItemInteractionListener dislikeListener,
                         OnItemInteractionListener commentListener,
                         OnItemInteractionListener downloadListener) {
            this.favoriteListener = favoriteListener;
            this.interactionListener = likeListener;
            this.dislikeListener = dislikeListener;
            this.commentListener = commentListener;
            this.downloadListener = downloadListener;
        }

        void setData(List<CorrectedAddress> list) {
            this.originalList = list;
            rebuildDisplayList();
        }

        void updateFavorites(Set<String> newFavorites) {
            this.favorites = newFavorites;
            rebuildDisplayList();
        }

        void filter(String query) {
            this.currentFilter = query.toLowerCase().trim();
            rebuildDisplayList();
        }

        private void rebuildDisplayList() {
            displayList.clear();
            
            // Map<City, Map<Neighborhood, List<Address>>>
            Map<String, Map<String, List<CorrectedAddress>>> hierarchy = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

            for (CorrectedAddress addr : originalList) {
                if (!currentFilter.isEmpty()) {
                    boolean matches = (addr.address != null && addr.address.toLowerCase().contains(currentFilter)) ||
                            (addr.neighborhood != null && addr.neighborhood.toLowerCase().contains(currentFilter)) ||
                            (addr.city != null && addr.city.toLowerCase().contains(currentFilter));
                    if (!matches) continue;
                }

                String cityKey = (addr.city != null && !addr.city.isEmpty()) ? addr.city : "Outras Cidades";
                String neighborhoodKey = (addr.neighborhood != null && !addr.neighborhood.isEmpty()) ? addr.neighborhood : "Sem Bairro";
                
                if (!hierarchy.containsKey(cityKey)) hierarchy.put(cityKey, new TreeMap<>(String.CASE_INSENSITIVE_ORDER));
                Map<String, List<CorrectedAddress>> cityMap = hierarchy.get(cityKey);
                
                if (!cityMap.containsKey(neighborhoodKey)) cityMap.put(neighborhoodKey, new ArrayList<>());
                cityMap.get(neighborhoodKey).add(addr);
            }

            // Ordena cidades: Favoritas primeiro
            List<String> sortedCities = new ArrayList<>(hierarchy.keySet());
            Collections.sort(sortedCities, (a, b) -> {
                boolean favA = favorites.contains(a);
                boolean favB = favorites.contains(b);
                if (favA && !favB) return -1;
                if (!favA && favB) return 1;
                return a.compareToIgnoreCase(b);
            });

            for (String city : sortedCities) {
                displayList.add(new CityHeader(city, favorites.contains(city)));
                Map<String, List<CorrectedAddress>> neighborhoods = hierarchy.get(city);
                for (Map.Entry<String, List<CorrectedAddress>> entry : neighborhoods.entrySet()) {
                    displayList.add(new NeighborhoodHeader(entry.getKey()));
                    displayList.addAll(entry.getValue());
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
                return new CityViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_community_city, parent, false));
            } else if (viewType == TYPE_NEIGHBORHOOD) {
                return new NeighborhoodViewHolder(LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_1, parent, false));
            } else {
                return new ItemViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_community_address, parent, false));
            }
        }

        @Override public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            Object obj = displayList.get(position);
            if (holder instanceof CityViewHolder) {
                CityHeader header = (CityHeader) obj;
                ((CityViewHolder) holder).text.setText(header.name);
                ((CityViewHolder) holder).imgFav.setImageResource(header.isFav ? android.R.drawable.btn_star_big_on : android.R.drawable.btn_star_big_off);
                ((CityViewHolder) holder).imgFav.setOnClickListener(v -> favoriteListener.onToggle(header.name));
            } else if (holder instanceof NeighborhoodViewHolder) {
                ((NeighborhoodViewHolder) holder).text.setText(" bairro: " + ((NeighborhoodHeader) obj).name);
            } else if (holder instanceof ItemViewHolder) {
                CorrectedAddress item = (CorrectedAddress) obj;
                ItemViewHolder h = (ItemViewHolder) holder;
                h.textAddress.setText(item.address);
                h.textBairro.setText(item.neighborhood != null ? item.neighborhood : "Sem bairro");
                h.textLikes.setText(String.valueOf(item.likes));
                h.textDislikes.setText(String.valueOf(item.dislikes));
                h.textComments.setText(String.valueOf(item.commentCount));

                // Configuração definitiva dos cliques usando botões reais
                h.btnLike.setOnClickListener(v -> interactionListener.onAction(item));
                h.btnDislike.setOnClickListener(v -> dislikeListener.onAction(item));
                h.btnComments.setOnClickListener(v -> commentListener.onAction(item));
                h.btnDownload.setOnClickListener(v -> downloadListener.onAction(item));
                
                // Permitir clique no item para ver detalhes/coordenadas
                h.itemView.setOnClickListener(v -> {
                    String coordsInfo = String.format(java.util.Locale.US, "📍 Lat: %.6f\n📍 Lon: %.6f", item.latitude, item.longitude);
                    String[] options = {"Abrir no Google Maps", "👍 Like", "👎 Dislike", "📥 Baixar Correção", "💬 Comentar"};
                    
                    new androidx.appcompat.app.AlertDialog.Builder(v.getContext())
                            .setTitle(item.address)
                            .setMessage(coordsInfo)
                            .setItems(options, (dialog, which) -> {
                                if (which == 0) {
                                    try {
                                        Uri gmmIntentUri = Uri.parse("geo:" + item.latitude + "," + item.longitude + "?q=" + item.latitude + "," + item.longitude + "(" + Uri.encode(item.address) + ")");
                                        android.content.Intent mapIntent = new android.content.Intent(android.content.Intent.ACTION_VIEW, gmmIntentUri);
                                        mapIntent.setPackage("com.google.android.apps.maps");
                                        v.getContext().startActivity(mapIntent);
                                    } catch (Exception e) {
                                        Toast.makeText(v.getContext(), "Google Maps não encontrado", Toast.LENGTH_SHORT).show();
                                    }
                                } else if (which == 1) interactionListener.onAction(item);
                                else if (which == 2) dislikeListener.onAction(item);
                                else if (which == 3) downloadListener.onAction(item);
                                else if (which == 4) commentListener.onAction(item);
                            })
                            .setNegativeButton("Fechar", null)
                            .show();
                });
            }
        }

        @Override public int getItemCount() { return displayList.size(); }

        static class CityHeader { String name; boolean isFav; CityHeader(String name, boolean isFav) { this.name = name; this.isFav = isFav; } }
        static class NeighborhoodHeader { String name; NeighborhoodHeader(String name) { this.name = name; } }

        static class CityViewHolder extends RecyclerView.ViewHolder {
            TextView text; ImageView imgFav;
            CityViewHolder(View v) { super(v); text = v.findViewById(R.id.textCityName); imgFav = v.findViewById(R.id.imgCityFav); }
        }
        static class NeighborhoodViewHolder extends RecyclerView.ViewHolder {
            TextView text;
            NeighborhoodViewHolder(View v) { super(v); text = v.findViewById(android.R.id.text1); text.setTextSize(14); text.setTextColor(0xFF888888); }
        }
        static class ItemViewHolder extends RecyclerView.ViewHolder {
            TextView textAddress, textBairro, textComments, textLikes, textDislikes;
            View btnComments, btnLike, btnDislike, btnDownload;

            ItemViewHolder(View v) { 
                super(v); 
                textAddress = v.findViewById(R.id.textCommunityAddress);
                textBairro = v.findViewById(R.id.textCommunityBairro);
                textComments = v.findViewById(R.id.textCommunityComments);
                textLikes = v.findViewById(R.id.textCommunityLikes);
                textDislikes = v.findViewById(R.id.textCommunityDislikes);
                btnLike = v.findViewById(R.id.btnCommunityLike);
                btnDislike = v.findViewById(R.id.btnCommunityDislike);
                btnComments = v.findViewById(R.id.btnCommunityComments);
                btnDownload = v.findViewById(R.id.btnCommunityDownload);
            }
        }
    }
}
