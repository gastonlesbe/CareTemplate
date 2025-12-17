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
import java.util.Map;

/**
 * Adapter para Agenda con cabeceras de día y eventos dentro de cada día.
 */
public class AgendaAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public interface OnEventClick {
        void onEdit(EventEntity e);
        void onDelete(EventEntity e);
        default void onToggleRealized(EventEntity e, boolean realized) {}
    }

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_EVENT = 1;

    private final List<Row> rows = new ArrayList<>();
    private final HashMap<String, String> subjectNames = new HashMap<>();
    private final HashMap<String, String> subjectIconKeys = new HashMap<>();
    private final HashMap<String, String> subjectColorHex = new HashMap<>();
    private final OnEventClick listener;
    private String appType;

    private final SimpleDateFormat timeFmt = new SimpleDateFormat("HH:mm", Locale.getDefault());

    public AgendaAdapter(OnEventClick l) { this.listener = l; }

    public void setAppType(String appType) {
        this.appType = appType;
        notifyDataSetChanged();
    }

    public void setSubjectsMap(Map<String, String> map) {
        subjectNames.clear();
        if (map != null) subjectNames.putAll(map);
        notifyDataSetChanged();
    }

    public void setSubjectIconKeys(Map<String, String> map) {
        subjectIconKeys.clear();
        if (map != null) subjectIconKeys.putAll(map);
        notifyDataSetChanged();
    }

    public void setSubjectColorHex(Map<String, String> map) {
        subjectColorHex.clear();
        if (map != null) subjectColorHex.putAll(map);
        notifyDataSetChanged();
    }

    public void submit(List<Row> data) {
        rows.clear();
        if (data != null) rows.addAll(data);
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return rows.get(position).isHeader ? TYPE_HEADER : TYPE_EVENT;
    }

    @NonNull @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_HEADER) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_agenda_header, parent, false);
            return new HeaderVH(v);
        }
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_event, parent, false);
        return new EventVH(v);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Row row = rows.get(position);
        if (row.isHeader) {
            ((HeaderVH) holder).bind(row.dayLabel);
            return;
        }

        EventVH h = (EventVH) holder;
        EventEntity e = row.event;
        String subjName = subjectNames.getOrDefault(e.subjectId, "—");
        String iconKey = subjectIconKeys.getOrDefault(e.subjectId, null);

        h.tvTitle.setText(e.title);
        h.tvSubjectName.setText(subjName);

        int iconRes = getIconResForSubject(iconKey);

        String hex = subjectColorHex.getOrDefault(e.subjectId, "#03DAC5");
        int color;
        try {
            color = Color.parseColor(hex);
        } catch (Exception ex) {
            color = Color.parseColor("#03DAC5");
        }

        color = adjustColorForTheme(h.ivSubjectIcon.getContext(), color);

        android.graphics.drawable.Drawable drawable = ContextCompat.getDrawable(h.ivSubjectIcon.getContext(), iconRes);
        if (drawable != null) {
            drawable = drawable.mutate();
            drawable.setTint(color);
            h.ivSubjectIcon.setImageDrawable(drawable);
        } else {
            h.ivSubjectIcon.setImageResource(iconRes);
            h.ivSubjectIcon.setColorFilter(color, PorterDuff.Mode.SRC_IN);
        }
        h.ivSubjectIcon.setVisibility(View.VISIBLE);

        h.tvWhen.setText(timeFmt.format(new Date(e.dueAt)));

        if (e.cost != null) {
            String costText = h.tvCost.getContext().getString(R.string.event_cost_format, e.cost);
            h.tvCost.setText(costText);
            h.tvCost.setVisibility(View.VISIBLE);
        } else {
            h.tvCost.setVisibility(View.GONE);
        }

        if ("cars".equals(appType) && e.kilometersAtEvent != null) {
            if (h.tvKilometers != null) {
                String kmText = h.tvKilometers.getContext().getString(R.string.event_kilometers, Math.round(e.kilometersAtEvent));
                h.tvKilometers.setText(kmText);
                h.tvKilometers.setVisibility(View.VISIBLE);
            }
        } else {
            if (h.tvKilometers != null) {
                h.tvKilometers.setVisibility(View.GONE);
            }
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

        h.btnEdit.setOnTouchListener((v, event) -> {
            if (event.getAction() == android.view.MotionEvent.ACTION_UP) {
                v.performClick();
            }
            return true;
        });

        h.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDelete(e);
        });

        h.btnDelete.setOnTouchListener((v, event) -> {
            if (event.getAction() == android.view.MotionEvent.ACTION_UP) {
                v.performClick();
            }
            return true;
        });

        // Set background color based on event status
        setEventBackgroundColor(h.itemView, e);
    }

    /**
     * Sets border color for event item:
     * - Red border: defeated (past due and not realized)
     * - Yellow border: due today (not realized)
     * - Green border: future events (not realized)
     * - Default: no border
     */
    private void setEventBackgroundColor(View itemView, EventEntity e) {
        long now = System.currentTimeMillis();
        boolean isDefeated = e.dueAt < now && e.realized == 0;
        boolean isDueToday = isDueToday(e.dueAt, now) && e.realized == 0;
        boolean isFuture = e.dueAt > now && e.realized == 0;

        int borderColor;
        int borderWidth;
        if (isDefeated) {
            // Red border for defeated events
            borderColor = Color.parseColor("#F44336"); // Red
            borderWidth = 4; // dp
        } else if (isDueToday) {
            // Yellow border for events due today
            borderColor = Color.parseColor("#FFEB3B"); // Yellow
            borderWidth = 4; // dp
        } else if (isFuture) {
            // Green border for future events
            borderColor = Color.parseColor("#4CAF50"); // Green
            borderWidth = 2; // dp (thinner for future events)
        } else {
            // No border
            borderColor = Color.TRANSPARENT;
            borderWidth = 0;
        }

        // Apply border to the CardView using a drawable
        if (itemView instanceof androidx.cardview.widget.CardView) {
            androidx.cardview.widget.CardView cardView = (androidx.cardview.widget.CardView) itemView;
            float density = itemView.getContext().getResources().getDisplayMetrics().density;
            int borderWidthPx = (int) (borderWidth * density);
            
            if (borderWidth > 0) {
                // Create a drawable with border
                android.graphics.drawable.GradientDrawable drawable = new android.graphics.drawable.GradientDrawable();
                drawable.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
                drawable.setColor(Color.TRANSPARENT); // Transparent background
                drawable.setStroke(borderWidthPx, borderColor);
                drawable.setCornerRadius(8 * density); // Match CardView corner radius
                
                // Set the drawable as background
                cardView.setBackground(drawable);
            } else {
                // Reset to default if no border
                cardView.setBackground(null);
            }
        }
    }

    /**
     * Checks if the event is due today (same calendar day)
     */
    private boolean isDueToday(long eventDueAt, long now) {
        java.util.Calendar eventCal = java.util.Calendar.getInstance();
        eventCal.setTimeInMillis(eventDueAt);
        java.util.Calendar todayCal = java.util.Calendar.getInstance();
        todayCal.setTimeInMillis(now);

        return eventCal.get(java.util.Calendar.YEAR) == todayCal.get(java.util.Calendar.YEAR) &&
               eventCal.get(java.util.Calendar.DAY_OF_YEAR) == todayCal.get(java.util.Calendar.DAY_OF_YEAR);
    }

    @Override public int getItemCount() { return rows.size(); }

    // ==== ViewHolders ====
    static class HeaderVH extends RecyclerView.ViewHolder {
        TextView tvDayHeader;
        HeaderVH(@NonNull View itemView) {
            super(itemView);
            tvDayHeader = itemView.findViewById(R.id.tvDayHeader);
        }
        void bind(String label) { tvDayHeader.setText(label); }
    }

    static class EventVH extends RecyclerView.ViewHolder {
        ImageView ivSubjectIcon;
        TextView tvSubjectName;
        TextView tvWhen;
        TextView tvTitle;
        TextView tvCost;
        TextView tvKilometers;
        CheckBox cbDone;
        com.google.android.material.button.MaterialButton btnEdit;
        com.google.android.material.button.MaterialButton btnDelete;
        EventVH(@NonNull View itemView) {
            super(itemView);
            ivSubjectIcon = itemView.findViewById(R.id.ivSubjectIcon);
            tvSubjectName = itemView.findViewById(R.id.tvSubject);
            tvWhen = itemView.findViewById(R.id.tvWhen);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvCost = itemView.findViewById(R.id.tvCost);
            tvKilometers = itemView.findViewById(R.id.tvKilometers);
            cbDone = itemView.findViewById(R.id.cbDone);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }

    // ==== Helpers ====
    private int getIconResForSubject(String iconKey) {
        if (iconKey == null) return R.drawable.ic_header_flavor;
        int resId = R.drawable.ic_header_flavor;
        try {
            resId = R.drawable.class.getField(iconKey).getInt(null);
        } catch (Exception ignored) {}
        return resId;
    }

    private int adjustColorForTheme(android.content.Context context, int color) {
        int nightMode = context.getResources().getConfiguration().uiMode &
                android.content.res.Configuration.UI_MODE_NIGHT_MASK;
        boolean isDarkTheme = (nightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES);

        if (isDarkTheme) {
            int r = (color >> 16) & 0xFF;
            int g = (color >> 8) & 0xFF;
            int b = color & 0xFF;
            r = Math.min(255, (int) (r * 1.2));
            g = Math.min(255, (int) (g * 1.2));
            b = Math.min(255, (int) (b * 1.2));
            color = Color.rgb(r, g, b);
        }
        return color;
    }

    /**
     * Modelo de fila (header o evento).
     */
    public static class Row {
        public final boolean isHeader;
        public final String dayLabel;
        public final EventEntity event;
        private Row(boolean isHeader, String dayLabel, EventEntity event) {
            this.isHeader = isHeader;
            this.dayLabel = dayLabel;
            this.event = event;
        }
        public static Row header(String label) { return new Row(true, label, null); }
        public static Row event(EventEntity e) { return new Row(false, null, e); }
    }
}
