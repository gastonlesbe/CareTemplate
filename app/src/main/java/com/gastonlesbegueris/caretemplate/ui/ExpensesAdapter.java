package com.gastonlesbegueris.caretemplate.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.gastonlesbegueris.caretemplate.R;
import com.gastonlesbegueris.caretemplate.data.model.MonthTotal;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ExpensesAdapter extends RecyclerView.Adapter<ExpensesAdapter.VH> {

    private final List<MonthTotal> items = new ArrayList<>();
    private final SimpleDateFormat fmt = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());

    public void submit(List<MonthTotal> data) {
        items.clear();
        if (data != null) items.addAll(data);
        notifyDataSetChanged();
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_month_total, parent, false);
        return new VH(v);
    }

    @Override public void onBindViewHolder(@NonNull VH h, int pos) {
        MonthTotal m = items.get(pos);
        h.tvMonth.setText(fmt.format(new Date(m.monthStart)));
        double planned  = (m.plannedSum  == null) ? 0.0 : m.plannedSum;
        double realized = (m.realizedSum == null) ? 0.0 : m.realizedSum;
        h.tvPlanned.setText(String.format(Locale.getDefault(), "Planificado: $%.2f", planned));
        h.tvRealized.setText(String.format(Locale.getDefault(), "Realizado: $%.2f", realized));
    }

    @Override public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvMonth, tvPlanned, tvRealized;
        VH(@NonNull View v) {
            super(v);
            tvMonth    = v.findViewById(R.id.tvMonth);
            tvPlanned  = v.findViewById(R.id.tvPlanned);
            tvRealized = v.findViewById(R.id.tvRealized);
        }
    }
}
