package com.gastonlesbegueris.caretemplate.ui;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import com.google.android.material.button.MaterialButton;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
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
    private final HashMap<String, String> subjectIconKeys = new HashMap<>();
    private final HashMap<String, String> subjectColorHex = new HashMap<>();
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

    public void setSubjectIconKeys(java.util.Map<String, String> map) {
        subjectIconKeys.clear();
        if (map != null) subjectIconKeys.putAll(map);
        notifyDataSetChanged();
    }

    public void setSubjectColorHex(java.util.Map<String, String> map) {
        subjectColorHex.clear();
        if (map != null) subjectColorHex.putAll(map);
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
        String iconKey = subjectIconKeys.getOrDefault(e.subjectId, null);

        h.tvTitle.setText(e.title);
        h.tvSubjectName.setText(subjName);

        // Set subject icon with color (same as SubjectAdapter)
        int iconRes = getIconResForSubject(iconKey);
        
        // Color dinámico (tint) - ajustar según tema
        String hex = subjectColorHex.getOrDefault(e.subjectId, "#03DAC5");
        int color;
        try {
            color = Color.parseColor(hex);
        } catch (Exception ex) {
            color = Color.parseColor("#03DAC5");
        }
        
        // Ajustar color según el tema (claro/oscuro)
        color = adjustColorForTheme(h.ivSubjectIcon.getContext(), color);
        
        // Obtener el drawable del recurso, aplicar tint y establecerlo
        android.graphics.drawable.Drawable drawable = ContextCompat.getDrawable(h.ivSubjectIcon.getContext(), iconRes);
        if (drawable != null) {
            drawable = drawable.mutate();
            drawable.setTint(color);
            h.ivSubjectIcon.setImageDrawable(drawable);
        } else {
            // Fallback si no se puede obtener el drawable
            h.ivSubjectIcon.setImageResource(iconRes);
            h.ivSubjectIcon.setColorFilter(color, PorterDuff.Mode.SRC_IN);
        }
        h.ivSubjectIcon.setVisibility(View.VISIBLE);

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

        h.btnEdit.setOnClickListener(v -> {
            if (listener != null) listener.onEdit(e);
        });

        h.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDelete(e);
        });
    }

    @Override public int getItemCount() { return items.size(); }

    /**
     * Ajusta el color del icono según el tema actual (claro/oscuro).
     * En modo oscuro, aclara los colores oscuros para mejor contraste.
     * En modo claro, oscurece los colores muy claros si es necesario.
     */
    private int adjustColorForTheme(android.content.Context context, int color) {
        // Detectar si estamos en modo oscuro
        int nightMode = context.getResources().getConfiguration().uiMode & 
                       android.content.res.Configuration.UI_MODE_NIGHT_MASK;
        boolean isDarkTheme = (nightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES);
        
        if (isDarkTheme) {
            // En modo oscuro: si el color es muy oscuro, aclararlo
            float[] hsv = new float[3];
            Color.colorToHSV(color, hsv);
            
            // Si el brillo (brightness) es muy bajo (< 0.3), aumentarlo
            if (hsv[2] < 0.3f) {
                hsv[2] = Math.max(0.5f, hsv[2] * 1.8f); // Aumentar brillo
                color = Color.HSVToColor(hsv);
            }
            
            // También aumentar la saturación ligeramente para colores más vivos
            if (hsv[1] < 0.5f) {
                hsv[1] = Math.min(1.0f, hsv[1] * 1.2f);
                color = Color.HSVToColor(hsv);
            }
        } else {
            // En modo claro: si el color es muy claro, oscurecerlo ligeramente
            float[] hsv = new float[3];
            Color.colorToHSV(color, hsv);
            
            // Si el brillo es muy alto (> 0.9), reducirlo un poco
            if (hsv[2] > 0.9f) {
                hsv[2] = 0.8f;
                color = Color.HSVToColor(hsv);
            }
        }
        
        return color;
    }

    private int getIconResForSubject(String key) {
        if (key == null) return R.drawable.ic_line_user;
        switch (key) {
            // Pets
            case "cat":   return R.drawable.ic_line_cat;
            case "dog":   return R.drawable.ic_line_dog;
            // Family
            case "man":   return R.drawable.ic_line_man;
            case "woman": return R.drawable.ic_line_woman;
            // House
            case "apartment": return R.drawable.ic_line_apartment;
            case "house": return R.drawable.ic_line_house;
            case "office": return R.drawable.ic_line_office;
            case "local": return R.drawable.ic_line_local;
            case "store": return R.drawable.ic_line_store;
            // Vehicles
            case "car":   return R.drawable.ic_line_car;
            case "bike":  return R.drawable.ic_line_bike;
            case "motorbike": return R.drawable.ic_line_motorbike;
            case "truck": return R.drawable.ic_line_truck;
            case "pickup": return R.drawable.ic_line_pickup;
            case "suv":   return R.drawable.ic_line_suv;
            // Default
            case "user":  return R.drawable.ic_line_user;
            default:      return R.drawable.ic_line_user;
        }
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvTitle, tvSubjectName, tvWhen, tvCost;
        ImageView ivSubjectIcon;
        CheckBox cbDone;
        MaterialButton btnDelete, btnEdit;
        VH(@NonNull View v) {
            super(v);
            tvTitle = v.findViewById(R.id.tvTitle);
            tvSubjectName = v.findViewById(R.id.tvSubject);
            tvWhen = v.findViewById(R.id.tvWhen);
            tvCost = v.findViewById(R.id.tvCost);
            ivSubjectIcon = v.findViewById(R.id.ivSubjectIcon);
            cbDone = v.findViewById(R.id.cbDone);
            btnEdit   = v.findViewById(R.id.btnEdit);
            btnDelete = v.findViewById(R.id.btnDelete);
        }
    }
}
