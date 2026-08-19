package com.example.drivelog;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class EarningsHistoryFragment extends Fragment implements EarningsAdapter.OnEarningsClickListener {

    private RecyclerView recyclerHistory;
    private EarningsAdapter adapter;
    private int restoredEditId = -1;
    private Earnings restoredDraft = null;

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (adapter != null && adapter.getEditingId() != -1) {
            outState.putInt("editing_id", adapter.getEditingId());
            outState.putSerializable("draft_item", adapter.getEditingItem());
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_earnings_history, container, false);

        recyclerHistory = view.findViewById(R.id.recyclerEarningsHistory);
        recyclerHistory.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new EarningsAdapter(new ArrayList<>(), this);
        recyclerHistory.setAdapter(adapter);

        if (savedInstanceState != null) {
            restoredEditId = savedInstanceState.getInt("editing_id", -1);
            restoredDraft = (Earnings) savedInstanceState.getSerializable("draft_item");
            if (restoredEditId != -1) {
                adapter.setEditingId(restoredEditId);
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
                Earnings earnings = adapter.getEarningsAt(position);

                if (direction == ItemTouchHelper.LEFT) {
                    adapter.setEditingPosition(position);
                } else if (direction == ItemTouchHelper.RIGHT) {
                    showDeleteConfirmation(earnings, position);
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
                        paint.setColor(Color.parseColor("#FFC107")); // Material Amber/Yellow
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

    private void showDeleteConfirmation(Earnings earnings, int position) {
        new AlertDialog.Builder(getContext())
                .setTitle("Confirmar Exclusão")
                .setMessage("Deseja realmente excluir este registro de ganho?")
                .setPositiveButton("Excluir", (dialog, which) -> {
                    AppDatabase.getInstance(getContext()).appDao().deleteEarnings(earnings);
                    updateHistory();
                    Toast.makeText(getContext(), "Registro excluído", Toast.LENGTH_SHORT).show();
                    
                    // Trigger auto cloud sync if enabled
                    CloudSyncHelper.syncNow(requireContext());
                })
                .setNegativeButton("Cancelar", (dialog, which) -> adapter.notifyItemChanged(position))
                .setOnCancelListener(dialog -> adapter.notifyItemChanged(position))
                .show();
    }

    private void updateHistory() {
        int currentEditId = (adapter != null) ? adapter.getEditingId() : restoredEditId;
        Earnings draft = (adapter != null && adapter.getEditingId() != -1) ? adapter.getEditingItem() : restoredDraft;

        List<Earnings> allEarnings = AppDatabase.getInstance(getContext()).appDao().getAllEarnings();
        
        if (currentEditId != -1 && draft != null) {
            for (int i = 0; i < allEarnings.size(); i++) {
                if (allEarnings.get(i).id == currentEditId) {
                    allEarnings.set(i, draft); // Aplica o rascunho salvo (digitado) sobre o dado original do banco
                    break;
                }
            }
        }

        adapter.setEarningsList(allEarnings);
        if (currentEditId != -1) {
            adapter.setEditingId(currentEditId);
        }
    }

    @Override
    public void onEarningsClick(Earnings earnings) {
        // We now use inline editing, so clicking can either do nothing or 
        // trigger the same inline edit. For now, let's keep it simple.
    }

    @Override
    public void onSaveEdit(Earnings earnings, int position) {
        AppDatabase.getInstance(getContext()).appDao().updateEarnings(earnings);
        Toast.makeText(getContext(), "Registro atualizado", Toast.LENGTH_SHORT).show();
        
        // Trigger auto cloud sync if enabled
        CloudSyncHelper.syncNow(requireContext());
    }

    @Override
    public void onEarningsLongClick(Earnings earnings) {}

    @Override
    public void onResume() {
        super.onResume();
        updateHistory();
    }
}