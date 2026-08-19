package com.example.drivelog;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import java.util.Locale;

public class WeeklyAdapter extends RecyclerView.Adapter<WeeklyAdapter.WeeklyViewHolder> {

    private List<WeeklyReport> reports;

    public WeeklyAdapter(List<WeeklyReport> reports) {
        this.reports = reports;
    }

    public void setReports(List<WeeklyReport> reports) {
        this.reports = reports;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public WeeklyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_weekly_report, parent, false);
        return new WeeklyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull WeeklyViewHolder holder, int position) {
        WeeklyReport report = reports.get(position);
        holder.textPeriod.setText(report.period);
        holder.textEarnings.setText(String.format(Locale.getDefault(), "Ganhos: R$ %.2f", report.totalEarnings));
        holder.textFuel.setText(String.format(Locale.getDefault(), "Gastos: R$ %.2f", report.totalFuel));
    }

    @Override
    public int getItemCount() {
        return reports.size();
    }

    static class WeeklyViewHolder extends RecyclerView.ViewHolder {
        TextView textPeriod, textEarnings, textFuel;
        public WeeklyViewHolder(@NonNull View itemView) {
            super(itemView);
            textPeriod = itemView.findViewById(R.id.textWeeklyPeriod);
            textEarnings = itemView.findViewById(R.id.textWeeklyEarnings);
            textFuel = itemView.findViewById(R.id.textWeeklyFuel);
        }
    }
}