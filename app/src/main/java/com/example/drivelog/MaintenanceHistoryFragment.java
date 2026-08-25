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

public class MaintenanceHistoryFragment extends Fragment implements MaintenanceAdapter.OnMaintClickListener {

    private RecyclerView recyclerHistory;
    private MaintenanceAdapter adapter;
    private int restoredEditId = -1;
    private Maintenance restoredDraft = null;

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (adapter != null && adapter.getEditingId() != -1) {
            outState.putInt("editing_maint_id", adapter.getEditingId());
            outState.putSerializable("draft_maint_item", adapter.getEditingItem());
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_maintenance_history, container, false);

        recyclerHistory = view.findViewById(R.id.recyclerMaintenanceHistory);
        recyclerHistory.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new MaintenanceAdapter(new ArrayList<>(), this);
        recyclerHistory.setAdapter(adapter);

        if (savedInstanceState != null) {
            restoredEditId = savedInstanceState.getInt("editing_maint_id", -1);
            restoredDraft = (Maintenance) savedInstanceState.getSerializable("draft_maint_item");
            if (restoredEditId != -1) {
                adapter.setEditingId(restoredEditId);
            }
        }

        // Fix conflict between Swipe and ViewPager2
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
                Maintenance maint = adapter.getMaintAt(position);

                if (direction == ItemTouchHelper.LEFT) {
                    // EDIT
                    adapter.setEditingPosition(position);
                } else if (direction == ItemTouchHelper.RIGHT) {
                    // DELETE
                    showDeleteConfirmation(maint, position);
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

    private void showDeleteConfirmation(Maintenance maint, int position) {
        UiHelper.showBottomSheetConfirm(
                requireContext(),
                "Excluir Manutenção?",
                "Deseja realmente excluir esta manutenção?",
                "EXCLUIR",
                () -> {
                    new Thread(() -> {
                        AppDatabase.getInstance(getContext()).appDao().deleteMaintenance(maint);
                        updateHistory();
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                Toast.makeText(getContext(), "Manutenção excluída", Toast.LENGTH_SHORT).show();
                                CloudSyncHelper.syncNow(requireContext(), "Manutenção excluída");
                            });
                        }
                    }).start();
                }
        );
    }

    private void updateHistory() {
        int currentEditId = (adapter != null) ? adapter.getEditingId() : restoredEditId;
        Maintenance draft = (adapter != null && adapter.getEditingId() != -1) ? adapter.getEditingItem() : restoredDraft;

        List<Maintenance> allMaint = AppDatabase.getInstance(getContext()).appDao().getAllMaintenance();
        
        if (currentEditId != -1 && draft != null) {
            for (int i = 0; i < allMaint.size(); i++) {
                if (allMaint.get(i).id == currentEditId) {
                    allMaint.set(i, draft);
                    break;
                }
            }
        }

        adapter.setMaintenanceList(allMaint);
        if (currentEditId != -1) {
            adapter.setEditingId(currentEditId);
        }
    }

    @Override
    public void onMaintClick(Maintenance maintenance) {
        // Inline editing handled by adapter
    }

    @Override
    public void onSaveEdit(Maintenance maintenance, int position) {
        AppDatabase.getInstance(getContext()).appDao().updateMaintenance(maintenance);
        updateHistory();
        Toast.makeText(getContext(), "Manutenção atualizada!", Toast.LENGTH_SHORT).show();
        
        // Trigger auto cloud sync if enabled
        CloudSyncHelper.syncNow(requireContext());
    }

    @Override
    public void onResume() {
        super.onResume();
        updateHistory();
    }
}
