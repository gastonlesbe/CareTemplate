package com.gastonlesbegueris.caretemplate.ui;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.gastonlesbegueris.caretemplate.R;
import com.gastonlesbegueris.caretemplate.data.local.EventEntity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class EventAdapter extends RecyclerView.Adapter<EventAdapter.VH> {

    public interface OnEventClick {
        void onEdit(EventEntity e);
        void onDelete(EventEntity e);
        default void onToggleRealized(EventEntity e, boolean realized) {}
    }

    private final List<EventEntity> items = new ArrayList<>();
    private final HashMap<String, String> subjectNames = new HashMap<>();
    private final OnEventClick listener;

    public EventAdapter(OnEventClick l) { this.listener = l; }

    public void submit(List<EventEntity> data) {
        items.clear();
        if (data != null) items.addAll(data);
        notifyDataSetChanged();
    }

    public void setSubjectsMap(java.util.Map<String, String> map) {
        subjectNames.clear();
        if (map != null) subjectNames.putAll(map);
        notifyDataSetChanged();
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_event, parent, false);
        return new VH(v);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        EventEntity e = items.get(pos);
        String subjName = subjectNames.getOrDefault(e.subjectId, "—");

        h.tvTitle.setText(e.title);
        h.tvSubjectName.setText(subjName);

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM HH:mm", Locale.getDefault());
        h.tvWhen.setText(sdf.format(new Date(e.dueAt)));

        if (e.cost != null) {
            h.tvCost.setText("Costo: $" + String.format(Locale.getDefault(), "%.2f", e.cost));
            h.tvCost.setVisibility(View.VISIBLE);
        } else {
            h.tvCost.setVisibility(View.GONE);
        }

        h.cbDone.setOnCheckedChangeListener(null);
        h.cbDone.setChecked(e.realized == 1);

        h.cbDone.setOnCheckedChangeListener((btn, checked) -> {
            if (listener != null) listener.onToggleRealized(e, checked);
        });

        h.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onEdit(e);
        });

        h.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDelete(e);
        });
    }

    @Override public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvTitle, tvSubjectName, tvWhen, tvCost;
        CheckBox cbDone;
        ImageButton btnDelete, btnEdit;
        VH(@NonNull View v) {
            super(v);
            tvTitle = v.findViewById(R.id.tvTitle);
            tvSubjectName = v.findViewById(R.id.tvSubject);
            tvWhen = v.findViewById(R.id.tvWhen);
            tvCost = v.findViewById(R.id.tvCost);
            cbDone = v.findViewById(R.id.cbDone);
            btnEdit   = v.findViewById(R.id.btnEdit);
            btnDelete = v.findViewById(R.id.btnDelete);
        }
    }
}
