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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class LocalEventAdapter extends RecyclerView.Adapter<LocalEventAdapter.VH> {

    public interface OnEventClick {
        void onEdit(EventEntity e);
        void onDelete(EventEntity e);
    }

    private final OnEventClick listener;

    // Lista de eventos que renderiza el adapter
    private final List<EventEntity> items = new ArrayList<>();

    // Mapa sujetoId -> nombre sujeto, para prefijar "Sujeto — Evento"
    private Map<String, String> subjectsMap = Collections.emptyMap();

    private final SimpleDateFormat fmt = new SimpleDateFormat("dd/MM HH:mm", Locale.getDefault());

    public LocalEventAdapter(OnEventClick l) {
        this.listener = l;
    }

    /** Cargar / refrescar eventos */
    public void submit(List<EventEntity> events) {
        items.clear();
        if (events != null) items.addAll(events);
        notifyDataSetChanged();
    }

    /** Setear el map de sujetos para el prefijo en título */
    public void setSubjectsMap(Map<String, String> map) {
        this.subjectsMap = (map == null) ? Collections.emptyMap() : map;
        notifyDataSetChanged();
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_event, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        EventEntity e = items.get(position);

        // Prefijo con nombre de sujeto
        String subjectName = (subjectsMap == null) ? null : subjectsMap.get(e.subjectId);
        String composedTitle = (subjectName == null || subjectName.isEmpty())
                ? e.title
                : subjectName + " — " + e.title;

        h.tvTitle.setText(composedTitle);

        // Fecha/hora
        h.tvWhen.setText(fmt.format(new Date(e.dueAt)));

        // Nota (si querés mostrarla)
        if (h.tvNote != null) {
            if (e.note != null && !e.note.trim().isEmpty()) {
                h.tvNote.setVisibility(View.VISIBLE);
                h.tvNote.setText(e.note);
            } else {
                h.tvNote.setVisibility(View.GONE);
            }
        }

        // Clicks
        h.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onEdit(e);
        });
        h.itemView.setOnLongClickListener(v -> {
            if (listener != null) listener.onDelete(e);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvTitle;
        TextView tvWhen;
        TextView tvNote; // opcional si tu layout lo tiene

        VH(@NonNull View v) {
            super(v);
            tvTitle = v.findViewById(R.id.tvTitle);
            tvWhen  = v.findViewById(R.id.tvWhen);
            tvNote  = v.findViewById(R.id.tvNote); // puede ser null si no existe
        }
    }
}
