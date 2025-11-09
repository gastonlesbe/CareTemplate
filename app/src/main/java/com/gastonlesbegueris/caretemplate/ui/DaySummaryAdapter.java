package com.gastonlesbegueris.caretemplate.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.gastonlesbegueris.caretemplate.R;
import com.gastonlesbegueris.caretemplate.data.model.DaySummary;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DaySummaryAdapter extends RecyclerView.Adapter<DaySummaryAdapter.VH> {

    public interface OnDayClick { void onClick(DaySummary summary); }
    public DaySummaryAdapter() { this.listener = null; } // si no usas clicks

    private final List<DaySummary> items = new ArrayList<>();
    private final OnDayClick listener;
    private final SimpleDateFormat fmtDay = new SimpleDateFormat("EEE dd", Locale.getDefault());

    public DaySummaryAdapter(OnDayClick listener) { this.listener = listener; }

    public void submit(List<DaySummary> data) {
        items.clear();
        if (data != null) items.addAll(data);
        notifyDataSetChanged();
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_day_summary, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        DaySummary s = items.get(position);

        long   dayStart = s.getDayStartMillis();
        int    evCount  = s.getEventsCount();
        int    done     = s.getRealizedCount();
        Double sum      = s.getExpensesSum();

        h.tvDay.setText(fmtDay.format(new Date(dayStart)));
        h.tvCounts.setText("Eventos: " + evCount + " · Realizados: " + done);

        if (sum != null) {
            h.tvExpenses.setVisibility(View.VISIBLE);
            h.tvExpenses.setText(String.format(Locale.getDefault(), "Gastos: $%.2f", sum));
        } else {
            h.tvExpenses.setVisibility(View.GONE);
        }

        h.itemView.setOnClickListener(v -> { if (listener != null) listener.onClick(s); });
    }

    @Override public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvDay, tvCounts, tvExpenses;
        VH(@NonNull View v) {
            super(v);
            tvDay      = v.findViewById(R.id.tvDay);
            tvCounts   = v.findViewById(R.id.tvCounts);
            tvExpenses = v.findViewById(R.id.tvExpenses);
        }
    }
}
