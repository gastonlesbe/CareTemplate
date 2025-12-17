package com.gastonlesbegueris.caretemplate.ui;

import android.graphics.Color;
import android.graphics.PorterDuff;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.gastonlesbegueris.caretemplate.R;
import com.gastonlesbegueris.caretemplate.data.local.SubjectEntity;

import java.util.ArrayList;
import java.util.List;

public class SubjectAdapter extends RecyclerView.Adapter<SubjectAdapter.VH> {

    public interface OnClick { 
        void onEdit(SubjectEntity s); 
        void onDelete(SubjectEntity s);
        default void onViewHistory(SubjectEntity s) {} // Nueva opción para ver historial
    }

    /** Fila enriquecida: entidad + líneas de info */
    public static class SubjectRow {
        public final SubjectEntity subject;
        public final String info;   // edad/peso o km
        public final String extra;  // próximo evento
        public final boolean hasDefeatedEvent;  // tiene evento vencido
        public final boolean hasEventDueToday;  // tiene evento hoy
        
        public SubjectRow(SubjectEntity s, String info, String extra, boolean hasDefeatedEvent, boolean hasEventDueToday) {
            this.subject = s; 
            this.info = info; 
            this.extra = extra;
            this.hasDefeatedEvent = hasDefeatedEvent;
            this.hasEventDueToday = hasEventDueToday;
        }
        
        // Constructor legacy para compatibilidad
        public SubjectRow(SubjectEntity s, String info, String extra) {
            this(s, info, extra, false, false);
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
        
        // Color dinámico (tint) - ajustar según tema
        String hex = (s.colorHex == null || s.colorHex.isEmpty()) ? "#03DAC5" : s.colorHex;
        int color;
        try {
            color = Color.parseColor(hex);
        } catch (Exception e) {
            color = Color.parseColor("#03DAC5");
        }
        
        // Ajustar color según el tema (claro/oscuro)
        color = adjustColorForTheme(h.ivIcon.getContext(), color);
        
        // Obtener el drawable del recurso, aplicar tint y establecerlo
        android.graphics.drawable.Drawable drawable = ContextCompat.getDrawable(h.ivIcon.getContext(), iconRes);
        if (drawable != null) {
            drawable = drawable.mutate();
            drawable.setTint(color);
            h.ivIcon.setImageDrawable(drawable);
        } else {
            // Fallback si no se puede obtener el drawable
            h.ivIcon.setImageResource(iconRes);
            h.ivIcon.setColorFilter(color, PorterDuff.Mode.SRC_IN);
        }

        h.tvName.setText(s.name == null ? "" : s.name);
        h.tvInfo.setText(row.info == null ? "" : row.info);
        h.tvExtra.setText(row.extra == null ? "" : row.extra);

        // Set background color based on event status
        setSubjectBackgroundColor(h.itemView, row.hasDefeatedEvent, row.hasEventDueToday);

        // Click simple en el item: ver historial
        h.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onViewHistory(s);
            }
        });
        
        // Botón Editar
        if (h.btnEdit != null) {
            h.btnEdit.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onEdit(s);
                }
            });
        }
        
        // Botón Borrar
        if (h.btnDelete != null) {
            h.btnDelete.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDelete(s);
                }
            });
        }
    }

    @Override public int getItemCount() { return rows.size(); }

    /**
     * Sets border color for subject item:
     * - Red border: has defeated event (priority)
     * - Yellow border: has event due today (if no defeated event)
     * - Green border: has future events (if no defeated/today events)
     * - Default: no border
     */
    private void setSubjectBackgroundColor(View itemView, boolean hasDefeatedEvent, boolean hasEventDueToday) {
        int borderColor;
        int borderWidth;
        if (hasDefeatedEvent) {
            // Red border for subjects with defeated events
            borderColor = Color.parseColor("#F44336"); // Red
            borderWidth = 4; // dp
        } else if (hasEventDueToday) {
            // Yellow border for subjects with events due today
            borderColor = Color.parseColor("#FFEB3B"); // Yellow
            borderWidth = 4; // dp
        } else {
            // Green border for subjects with future events (optional - you can remove this if not needed)
            // For now, no border if no special status
            borderColor = Color.TRANSPARENT;
            borderWidth = 0;
        }

        // Create a drawable with border for LinearLayout
        android.graphics.drawable.GradientDrawable drawable = new android.graphics.drawable.GradientDrawable();
        drawable.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
        drawable.setColor(Color.TRANSPARENT); // Transparent background
        
        float density = itemView.getContext().getResources().getDisplayMetrics().density;
        int borderWidthPx = (int) (borderWidth * density);
        
        drawable.setStroke(borderWidthPx, borderColor);
        drawable.setCornerRadius(8 * density); // Slight corner radius
        
        itemView.setBackground(drawable);
    }

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

    private int getIconRes(String key) {
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
        ImageView ivIcon;
        TextView tvName, tvInfo, tvExtra;
        android.widget.ImageButton btnEdit, btnDelete;
        VH(@NonNull View v) {
            super(v);
            ivIcon = v.findViewById(R.id.ivIcon);
            tvName = v.findViewById(R.id.tvSubjectName);
            tvInfo = v.findViewById(R.id.tvSubjectInfo);
            tvExtra= v.findViewById(R.id.tvSubjectExtra);
            btnEdit = v.findViewById(R.id.btnEdit);
            btnDelete = v.findViewById(R.id.btnDelete);
        }
    }
}
