package com.gastonlesbegueris.caretemplate.ui;

import android.view.*;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.gastonlesbegueris.caretemplate.R;
import java.util.*;

public class SubjectSummaryAdapter extends RecyclerView.Adapter<SubjectSummaryAdapter.VH> {

    private final List<SubjectCostSummary> items = new ArrayList<>();

    public void submit(List<SubjectCostSummary> data) {
        items.clear();
        if (data != null) items.addAll(data);
        notifyDataSetChanged();
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_subject_summary, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        SubjectCostSummary s = items.get(position);
        h.tvSubjectName.setText(s.subjectName);
        h.tvCount.setText("Eventos: " + s.count);
        h.tvPlanned.setText(String.format(Locale.getDefault(), "Plan: $%.2f", s.planned));
        h.tvRealized.setText(String.format(Locale.getDefault(), "Real: $%.2f", s.realized));
    }

    @Override public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvSubjectName, tvCount, tvPlanned, tvRealized;
        VH(@NonNull View v) {
            super(v);
            tvSubjectName = v.findViewById(R.id.tvSubjectName);
            tvCount       = v.findViewById(R.id.tvCount);
            tvPlanned     = v.findViewById(R.id.tvPlanned);
            tvRealized    = v.findViewById(R.id.tvRealized);
        }
    }
}
