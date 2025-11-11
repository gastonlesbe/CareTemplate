package com.gastonlesbegueris.caretemplate.ui;

import android.view.*;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.gastonlesbegueris.caretemplate.R;
import com.gastonlesbegueris.caretemplate.data.model.MonthTotal;
import java.util.*;

public class ExpensesListAdapter extends RecyclerView.Adapter<ExpensesListAdapter.VH> {
    private final List<MonthTotal> items = new ArrayList<>();

    public void submit(List<MonthTotal> data) {
        items.clear();
        if (data != null) items.addAll(data);
        notifyDataSetChanged();
    }

    @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup p, int v) {
        View view = LayoutInflater.from(p.getContext()).inflate(R.layout.item_month_total, p, false);
        return new VH(view);
    }

    @Override public void onBindViewHolder(@NonNull VH h, int pos) {
        MonthTotal m = items.get(pos);
        h.tvMonth.setText(m.monthStart); // "YYYY-MM-01"
        h.tvPlanned.setText(String.format(Locale.getDefault(),"Planificado: $%.2f", m.plannedSum == null? 0.0 : m.plannedSum));
        h.tvRealized.setText(String.format(Locale.getDefault(),"Realizado: $%.2f", m.realizedSum == null? 0.0 : m.realizedSum));
    }

    @Override public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvMonth, tvPlanned, tvRealized;
        VH(@NonNull View v) {
            super(v);
            tvMonth   = v.findViewById(R.id.tvMonth);
            tvPlanned = v.findViewById(R.id.tvPlanned);
            tvRealized= v.findViewById(R.id.tvRealized);
        }
    }
}
