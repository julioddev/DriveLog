package com.example.drivelog;

import android.app.DatePickerDialog;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class EarningsAdapter extends RecyclerView.Adapter<EarningsAdapter.EarningsViewHolder> {

    private List<Earnings> earningsList;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
    private OnEarningsClickListener listener;
    private int editingEarningsId = -1;

    public interface OnEarningsClickListener {
        void onEarningsClick(Earnings earnings);
        void onEarningsLongClick(Earnings earnings);
        void onSaveEdit(Earnings earnings, int position);
    }

    public EarningsAdapter(List<Earnings> earningsList, OnEarningsClickListener listener) {
        this.earningsList = earningsList;
        this.listener = listener;
    }

    public void setEarningsList(List<Earnings> earningsList) {
        this.earningsList = earningsList;
        // Removido o reset do editingEarningsId para manter a edição aberta ao minimizar/voltar
        notifyDataSetChanged();
    }

    public Earnings getEarningsAt(int position) {
        return earningsList.get(position);
    }

    public int getEditingId() {
        return editingEarningsId;
    }

    public Earnings getEditingItem() {
        if (editingEarningsId != -1) {
            for (Earnings e : earningsList) {
                if (e.id == editingEarningsId) return e;
            }
        }
        return null;
    }

    public void setEditingId(int id) {
        this.editingEarningsId = id;
        notifyDataSetChanged();
    }

    public void setEditingPosition(int position) {
        if (position >= 0 && position < earningsList.size()) {
            int newId = earningsList.get(position).id;
            if (editingEarningsId == newId) {
                editingEarningsId = -1;
            } else {
                editingEarningsId = newId;
            }
            notifyDataSetChanged();
        }
    }

    @NonNull
    @Override
    public EarningsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_earnings, parent, false);
        return new EarningsViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EarningsViewHolder holder, int pos) {
        final int position = holder.getBindingAdapterPosition();
        Earnings earnings = earningsList.get(position);
        holder.textDate.setText(dateFormat.format(new Date(earnings.date)));
        holder.textTotal.setText(String.format(Locale.getDefault(), "R$ %.2f", earnings.totalValue));
        holder.textDetails.setText(String.format(Locale.getDefault(), "Plataforma: %s | Base: R$ %.2f", 
                earnings.platforms, earnings.baseValue));

        if (earnings.extraValue > 0) {
            holder.textExtra.setVisibility(View.VISIBLE);
            holder.textExtra.setText(String.format(Locale.getDefault(), "+ R$ %.2f (Extra)", earnings.extraValue));
        } else {
            holder.textExtra.setVisibility(View.GONE);
        }

        // Inline Editing Logic
        if (earnings.id == editingEarningsId) {
            holder.layoutEdit.setVisibility(View.VISIBLE);
            // Agora preenchemos o campo Base com o valor base real, não o total
            holder.editBase.setText(String.format(Locale.getDefault(), "%.2f", earnings.baseValue));
            holder.editDate.setText(dateFormat.format(new Date(earnings.date)));
            
            if (earnings.extraValue > 0 || !"99".equals(earnings.platforms)) {
                holder.layoutEditExtra.setVisibility(View.VISIBLE);
                holder.editExtra.setText(String.format(Locale.getDefault(), "%.2f", earnings.extraValue));
            } else {
                holder.layoutEditExtra.setVisibility(View.GONE);
                holder.editExtra.setText("0.00");
            }

            holder.editPlatforms.setText(earnings.platforms);

            // Adiciona Watchers para atualizar o objeto em tempo real. 
            // Isso garante que se o app for minimizado e a lista recarregada, os dados digitados não sejam perdidos.
            TextWatcher draftWatcher = new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override public void afterTextChanged(Editable s) {
                    try {
                        earnings.baseValue = parseDouble(holder.editBase.getText().toString());
                        earnings.extraValue = parseDouble(holder.editExtra.getText().toString());
                        earnings.totalValue = earnings.baseValue + earnings.extraValue;
                        earnings.platforms = holder.editPlatforms.getText().toString();
                    } catch (Exception ignored) {}
                }
            };
            holder.editBase.addTextChangedListener(draftWatcher);
            holder.editExtra.addTextChangedListener(draftWatcher);
            holder.editPlatforms.addTextChangedListener(draftWatcher);

            holder.editDate.setOnClickListener(v -> {
                Calendar cal = Calendar.getInstance();
                cal.setTimeInMillis(earnings.date);
                new DatePickerDialog(holder.itemView.getContext(), (view1, year, month, dayOfMonth) -> {
                    cal.set(Calendar.YEAR, year);
                    cal.set(Calendar.MONTH, month);
                    cal.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    earnings.date = cal.getTimeInMillis();
                    holder.editDate.setText(dateFormat.format(new Date(earnings.date)));
                }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
            });

        } else {
            holder.layoutEdit.setVisibility(View.GONE);
        }

        holder.btnSave.setOnClickListener(v -> {
            try {
                // Ao salvar, somamos Base + Extra para gerar o Total
                double base = parseDouble(holder.editBase.getText().toString());
                double extra = parseDouble(holder.editExtra.getText().toString());
                
                earnings.baseValue = base;
                earnings.extraValue = extra;
                earnings.totalValue = base + extra;
                earnings.platforms = holder.editPlatforms.getText().toString();
                
                if (listener != null) {
                    listener.onSaveEdit(earnings, position);
                }
                editingEarningsId = -1;
                notifyItemChanged(position);
            } catch (Exception e) {
                // Handle error
            }
        });

        holder.btnCancel.setOnClickListener(v -> {
            editingEarningsId = -1;
            notifyItemChanged(position);
        });

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onEarningsClick(earnings);
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (listener != null) listener.onEarningsLongClick(earnings);
            return true;
        });
    }

    private double parseDouble(String value) {
        if (value == null || value.isEmpty()) return 0;
        try {
            return Double.parseDouble(value.replace(",", "."));
        } catch (Exception e) { return 0; }
    }

    @Override
    public int getItemCount() {
        return earningsList.size();
    }

    static class EarningsViewHolder extends RecyclerView.ViewHolder {
        TextView textDate, textTotal, textDetails, textExtra;
        View layoutEdit;
        EditText editBase, editExtra, editPlatforms, editDate;
        View layoutEditExtra;
        MaterialButton btnSave, btnCancel;

        public EarningsViewHolder(@NonNull View itemView) {
            super(itemView);
            textDate = itemView.findViewById(R.id.textEarningsDate);
            textTotal = itemView.findViewById(R.id.textEarningsTotal);
            textDetails = itemView.findViewById(R.id.textEarningsDetails);
            textExtra = itemView.findViewById(R.id.textEarningsExtra);
            
            layoutEdit = itemView.findViewById(R.id.layoutEditEarnings);
            editBase = itemView.findViewById(R.id.editEarningsBase);
            editExtra = itemView.findViewById(R.id.editEarningsExtra);
            layoutEditExtra = (View) editExtra.getParent().getParent();
            editPlatforms = itemView.findViewById(R.id.editEarningsPlatforms);
            editDate = itemView.findViewById(R.id.editEarningsDateInline);
            btnSave = itemView.findViewById(R.id.btnSaveEdit);
            btnCancel = itemView.findViewById(R.id.btnCancelEdit);
        }
    }
}
