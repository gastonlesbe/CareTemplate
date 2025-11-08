package com.gastonlesbegueris.caretemplate.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.gastonlesbegueris.caretemplate.R;
import com.gastonlesbegueris.caretemplate.data.local.EventEntity;
import java.text.SimpleDateFormat;
import java.util.*;

public class ExpensesAdapter extends RecyclerView.Adapter<ExpensesAdapter.VH> {

    private final List<EventEntity> items = new ArrayList<>();
    private Map<String,String> subjectNames = new HashMap<>();
    private final SimpleDateFormat fmt = new SimpleDateFormat("dd/MM HH:mm", Locale.getDefault());

    public void submit(List<EventEntity> data, Map<String,String> names) {
        items.clear();
        if (data != null) items.addAll(data);
        subjectNames = (names == null) ? new HashMap<>() : names;
        notifyDataSetChanged();
    }

    @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup p, int v) {
        View view = LayoutInflater.from(p.getContext()).inflate(R.layout.item_expense, p, false);
        return new VH(view);
    }

    @Override public void onBindViewHolder(@NonNull VH h, int pos) {
        EventEntity e = items.get(pos);
        String subject = (e.subjectId != null && subjectNames.containsKey(e.subjectId))
                ? subjectNames.get(e.subjectId) : "—";
        h.tv1.setText(subject + " · " + e.title);

        String money = (e.cost == null) ? "$0" :
                String.format(Locale.getDefault(), "$%.2f", e.cost);
        h.tv2.setText(fmt.format(new Date(e.updatedAt)) + " · " + money);
    }

    @Override public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tv1, tv2;
        VH(@NonNull View v) { super(v);
            tv1 = v.findViewById(R.id.tvLine1);
            tv2 = v.findViewById(R.id.tvLine2);
        }
    }
}
