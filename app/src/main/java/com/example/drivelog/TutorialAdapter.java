package com.example.drivelog;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class TutorialAdapter extends RecyclerView.Adapter<TutorialAdapter.ViewHolder> {

    public static class TutorialSlide {
        int iconResId;
        String title;
        String description;

        public TutorialSlide(int iconResId, String title, String description) {
            this.iconResId = iconResId;
            this.title = title;
            this.description = description;
        }
    }

    private final List<TutorialSlide> slides;

    public TutorialAdapter(List<TutorialSlide> slides) {
        this.slides = slides;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_tutorial_slide, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TutorialSlide slide = slides.get(position);
        holder.imageIcon.setImageResource(slide.iconResId);
        holder.textTitle.setText(slide.title);
        holder.textDescription.setText(slide.description);
    }

    @Override
    public int getItemCount() {
        return slides.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageIcon;
        TextView textTitle;
        TextView textDescription;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageIcon = itemView.findViewById(R.id.imageTutorialIcon);
            textTitle = itemView.findViewById(R.id.textTutorialTitle);
            textDescription = itemView.findViewById(R.id.textTutorialDescription);
        }
    }
}