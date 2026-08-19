package com.example.drivelog;

import android.app.AlertDialog;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class KmHistoryFragment extends Fragment implements KmAdapter.OnKmClickListener {

    private RecyclerView recyclerHistory;
    private KmAdapter adapter;
    private int restoredEditId = -1;
    private DailyKm restoredDraft = null;

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (adapter != null && adapter.getEditingKmId() != -1) {
            outState.putInt("editing_km_id", adapter.getEditingKmId());
            outState.putSerializable("draft_km_item", adapter.getEditingItem());
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_km_history, container, false);

        recyclerHistory = view.findViewById(R.id.recyclerKmHistory);
        recyclerHistory.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new KmAdapter(new ArrayList<>(), this);
        recyclerHistory.setAdapter(adapter);

        if (savedInstanceState != null) {
            restoredEditId = savedInstanceState.getInt("editing_km_id", -1);
            restoredDraft = (DailyKm) savedInstanceState.getSerializable("draft_km_item");
            if (restoredEditId != -1) {
                adapter.setEditingKmId(restoredEditId);
            }
        }

        recyclerHistory.addOnItemTouchListener(new RecyclerView.OnItemTouchListener() {
            @Override
            public boolean onInterceptTouchEvent(@NonNull RecyclerView rv, @NonNull android.view.MotionEvent e) {
                if (e.getAction() == android.view.MotionEvent.ACTION_DOWN) {
                    rv.getParent().requestDisallowInterceptTouchEvent(true);
                }
                return false;
            }
            @Override public void onTouchEvent(@NonNull RecyclerView rv, @NonNull android.view.MotionEvent e) {}
            @Override public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {}
        });

        setupSwipeActions();
        updateHistory();

        return view;
    }

    private void setupSwipeActions() {
        ItemTouchHelper.SimpleCallback swipeCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getBindingAdapterPosition();
                DailyKm dailyKm = adapter.getKmAt(position);

                if (direction == ItemTouchHelper.LEFT) {
                    adapter.setEditingPosition(position);
                } else if (direction == ItemTouchHelper.RIGHT) {
                    showDeleteConfirmation(dailyKm, position);
                }
            }

            @Override
            public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                    View itemView = viewHolder.itemView;
                    Paint paint = new Paint();
                    Drawable icon;
                    int iconMargin, iconTop, iconBottom, iconLeft, iconRight;

                    if (dX > 0) { // Swiping Right (Delete)
                        paint.setColor(Color.RED);
                        c.drawRect((float) itemView.getLeft(), (float) itemView.getTop(), dX, (float) itemView.getBottom(), paint);

                        icon = ContextCompat.getDrawable(getContext(), R.drawable.ic_delete);
                        if (icon != null) {
                            iconMargin = (itemView.getHeight() - icon.getIntrinsicHeight()) / 2;
                            iconTop = itemView.getTop() + iconMargin;
                            iconBottom = iconTop + icon.getIntrinsicHeight();
                            iconLeft = itemView.getLeft() + iconMargin;
                            iconRight = iconLeft + icon.getIntrinsicWidth();
                            icon.setBounds(iconLeft, iconTop, iconRight, iconBottom);
                            icon.draw(c);
                        }
                    } else if (dX < 0) { // Swiping Left (Edit)
                        paint.setColor(Color.parseColor("#FFC107")); // Material Amber
                        c.drawRect((float) itemView.getRight() + dX, (float) itemView.getTop(), (float) itemView.getRight(), (float) itemView.getBottom(), paint);

                        icon = ContextCompat.getDrawable(getContext(), R.drawable.ic_edit);
                        if (icon != null) {
                            iconMargin = (itemView.getHeight() - icon.getIntrinsicHeight()) / 2;
                            iconTop = itemView.getTop() + iconMargin;
                            iconBottom = iconTop + icon.getIntrinsicHeight();
                            iconRight = itemView.getRight() - iconMargin;
                            iconLeft = iconRight - icon.getIntrinsicWidth();
                            icon.setBounds(iconLeft, iconTop, iconRight, iconBottom);
                            icon.draw(c);
                        }
                    }
                }
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            }
        };
        new ItemTouchHelper(swipeCallback).attachToRecyclerView(recyclerHistory);
    }

    private void showDeleteConfirmation(DailyKm dailyKm, int position) {
        UiHelper.showBottomSheetConfirm(
                requireContext(),
                "Excluir KM?",
                "Deseja realmente excluir este registro de KM?",
                "EXCLUIR",
                () -> {
                    new Thread(() -> {
                        AppDatabase.getInstance(getContext()).appDao().deleteDailyKm(dailyKm);
                        updateHistory();
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> Toast.makeText(getContext(), "Registro excluído", Toast.LENGTH_SHORT).show());
                        }
                    }).start();
                }
        );
    }

    private void updateHistory() {
        int currentEditId = (adapter != null) ? adapter.getEditingKmId() : restoredEditId;
        DailyKm draft = (adapter != null && adapter.getEditingKmId() != -1) ? adapter.getEditingItem() : restoredDraft;

        AppDatabase.getInstance(requireContext()).appDao().getAllDailyKmLive()
                .observe(getViewLifecycleOwner(), list -> {
                    List<DailyKm> finalList = new ArrayList<>(list);
                    if (currentEditId != -1 && draft != null) {
                        for (int i = 0; i < finalList.size(); i++) {
                            if (finalList.get(i).id == currentEditId) {
                                finalList.set(i, draft);
                                break;
                            }
                        }
                    }
                    adapter.setKmList(finalList);
                    if (currentEditId != -1) adapter.setEditingKmId(currentEditId);
                });
    }

    @Override
    public void onKmClick(DailyKm dailyKm) {
        if (dailyKm.isCompleted && getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).showRouteOnMap(dailyKm.id);
            Toast.makeText(getContext(), "Carregando rota no mapa...", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onKmLongClick(DailyKm dailyKm, View anchor) {
        // Opções de desenvolvedor podem ser adicionadas aqui se necessário para registros manuais
    }

    @Override
    public void onSaveEdit(DailyKm dailyKm, int position) {
        if (dailyKm.isCompleted) {
            Fuel lastFuel = AppDatabase.getInstance(getContext()).appDao().getLastCompletedFuel();
            if (lastFuel != null && lastFuel.liters > 0 && lastFuel.kmDriven > 0) {
                double consumption = lastFuel.kmDriven / lastFuel.liters;
                dailyKm.consumptionUsed = consumption;
                dailyKm.estimatedFuelCost = (dailyKm.totalKm / consumption) * lastFuel.pricePerLiter;
            } else {
                // Fallback para valores padrão das configurações
                android.content.SharedPreferences prefs = requireActivity().getSharedPreferences("AppConfig", android.content.Context.MODE_PRIVATE);
                float defConsumption = prefs.getFloat("default_consumption", 10.0f);
                float defPrice = prefs.getFloat("default_fuel_price", 5.50f);
                
                dailyKm.consumptionUsed = defConsumption;
                dailyKm.estimatedFuelCost = (dailyKm.totalKm / defConsumption) * defPrice;
            }
        }
        AppDatabase.getInstance(getContext()).appDao().updateDailyKm(dailyKm);
        Toast.makeText(getContext(), "Registro atualizado!", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onResume() {
        super.onResume();
        updateHistory();
    }
}
