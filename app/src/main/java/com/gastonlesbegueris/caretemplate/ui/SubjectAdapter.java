package com.gastonlesbegueris.caretemplate.ui;

import android.graphics.Color;
import android.graphics.PorterDuff;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.gastonlesbegueris.caretemplate.R;
import com.gastonlesbegueris.caretemplate.data.local.SubjectEntity;

import java.util.ArrayList;
import java.util.List;

public class SubjectAdapter extends RecyclerView.Adapter<SubjectAdapter.VH> {

    public interface OnClick { void onEdit(SubjectEntity s); void onDelete(SubjectEntity s); }

    /** Fila enriquecida: entidad + líneas de info */
    public static class SubjectRow {
        public final SubjectEntity subject;
        public final String info;   // edad/peso o km
        public final String extra;  // próximo evento
        public SubjectRow(SubjectEntity s, String info, String extra) {
            this.subject = s; this.info = info; this.extra = extra;
        }
    }

    private final List<SubjectRow> rows = new ArrayList<>();
    private final OnClick listener;

    public SubjectAdapter(OnClick l){ this.listener = l; }

    public void submitRows(List<SubjectRow> data){
        rows.clear(); if (data != null) rows.addAll(data);
        notifyDataSetChanged();
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_subject, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        SubjectRow row = rows.get(position);
        SubjectEntity s = row.subject;

        // Ícono según key
        int iconRes = getIconRes(s.iconKey);
        h.ivIcon.setImageResource(iconRes);

        // Color dinámico (tint)
        String hex = (s.colorHex == null || s.colorHex.isEmpty()) ? "#03DAC5" : s.colorHex;
        try {
            int color = Color.parseColor(hex);
            h.ivIcon.setColorFilter(color, PorterDuff.Mode.SRC_IN);
        } catch (Exception e) {
            h.ivIcon.setColorFilter(Color.parseColor("#03DAC5"), PorterDuff.Mode.SRC_IN);
        }

        h.tvName.setText(s.name == null ? "" : s.name);
        h.tvInfo.setText(row.info == null ? "" : row.info);
        h.tvExtra.setText(row.extra == null ? "" : row.extra);

        h.itemView.setOnClickListener(v -> listener.onEdit(s));
        h.itemView.setOnLongClickListener(v -> { listener.onDelete(s); return true; });
    }

    @Override public int getItemCount() { return rows.size(); }

    private int getIconRes(String key) {
        if (key == null) return R.drawable.ic_line_user;
        switch (key) {
            case "cat":   return R.drawable.ic_line_cat;
            case "dog":   return R.drawable.ic_line_dog;
            case "car":   return R.drawable.ic_line_car;
            case "house": return R.drawable.ic_line_house;
            case "user":  return R.drawable.ic_line_user;
            default:      return R.drawable.ic_line_user;
        }
    }

    static class VH extends RecyclerView.ViewHolder {
        ImageView ivIcon;
        TextView tvName, tvInfo, tvExtra;
        VH(@NonNull View v) {
            super(v);
            ivIcon = v.findViewById(R.id.ivIcon);
            tvName = v.findViewById(R.id.tvSubjectName);
            tvInfo = v.findViewById(R.id.tvSubjectInfo);
            tvExtra= v.findViewById(R.id.tvSubjectExtra);
        }
    }
}
