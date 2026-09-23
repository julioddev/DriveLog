package com.example.drivelog;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import java.util.ArrayList;
import java.util.List;

public class QuadraAdapter extends RecyclerView.Adapter<QuadraAdapter.QuadraViewHolder> {

    private List<CorrectedQuadra> quadrasList = new ArrayList<>();
    private OnQuadraClickListener listener;
    private String currentUserId = "";

    public interface OnQuadraClickListener {
        void onShowOnMap(CorrectedQuadra quadra);
        void onDelete(CorrectedQuadra quadra);
    }

    public QuadraAdapter(OnQuadraClickListener listener, String currentUserId) {
        this.listener = listener;
        this.currentUserId = currentUserId != null ? currentUserId : "";
    }

    public void setQuadrasList(List<CorrectedQuadra> list) {
        this.quadrasList = list != null ? list : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public QuadraViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_quadra, parent, false);
        return new QuadraViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull QuadraViewHolder holder, int position) {
        CorrectedQuadra q = quadrasList.get(position);

        holder.textName.setText(q.name != null ? q.name : "Quadra");

        StringBuilder locBuilder = new StringBuilder();
        if (q.neighborhood != null && !q.neighborhood.isEmpty()) {
            locBuilder.append(q.neighborhood);
        }
        if (q.city != null && !q.city.isEmpty()) {
            if (locBuilder.length() > 0) locBuilder.append(", ");
            locBuilder.append(q.city);
        }
        if (locBuilder.length() == 0) locBuilder.append("Localização personalizada");
        holder.textLocation.setText(locBuilder.toString());

        if (q.notes != null && !q.notes.isEmpty()) {
            holder.textNotes.setText(q.notes);
            holder.textNotes.setVisibility(View.VISIBLE);
        } else {
            holder.textNotes.setVisibility(View.GONE);
        }

        String creatorText = "Comunidade";
        if (q.creatorName != null && !q.creatorName.isEmpty()) {
            creatorText = "Inserido por: " + q.creatorName;
        }
        holder.textCreator.setText(creatorText);

        boolean isMine = (q.creatorId != null && q.creatorId.equals(currentUserId));
        holder.btnDelete.setVisibility(isMine ? View.VISIBLE : View.GONE);

        holder.btnShowOnMap.setOnClickListener(v -> {
            if (listener != null) listener.onShowOnMap(q);
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDelete(q);
        });
    }

    @Override
    public int getItemCount() {
        return quadrasList.size();
    }

    static class QuadraViewHolder extends RecyclerView.ViewHolder {
        TextView textName, textLocation, textNotes, textCreator;
        ImageButton btnDelete;
        MaterialButton btnShowOnMap;

        public QuadraViewHolder(@NonNull View itemView) {
            super(itemView);
            textName = itemView.findViewById(R.id.textQuadraName);
            textLocation = itemView.findViewById(R.id.textQuadraLocation);
            textNotes = itemView.findViewById(R.id.textQuadraNotes);
            textCreator = itemView.findViewById(R.id.textQuadraCreator);
            btnDelete = itemView.findViewById(R.id.btnDeleteQuadra);
            btnShowOnMap = itemView.findViewById(R.id.btnShowQuadraOnMap);
        }
    }
}
