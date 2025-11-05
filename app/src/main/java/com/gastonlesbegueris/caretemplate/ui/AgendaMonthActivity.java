package com.gastonlesbegueris.caretemplate.ui;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.gastonlesbegueris.caretemplate.R;
import com.gastonlesbegueris.caretemplate.data.local.AppDb;
import com.gastonlesbegueris.caretemplate.data.local.EventDao;
import com.gastonlesbegueris.caretemplate.data.local.EventEntity;
import com.google.android.material.appbar.MaterialToolbar;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AgendaMonthActivity extends AppCompatActivity {

    private EventDao eventDao;
    private String appType;

    private androidx.recyclerview.widget.RecyclerView rvMonth;
    private TextView tvMonthTotals;
    private DaySummaryAdapter adapter;

    @Override
    protected void onCreate(Bundle s) {
        super.onCreate(s);
        setContentView(R.layout.activity_agenda_month);

        // DAO
        eventDao = AppDb.get(this).eventDao();

        // appType (desde resValue del flavor)
        appType = getString(R.string.app_type);

        // Toolbar
        MaterialToolbar tb = findViewById(R.id.toolbarMonth);
        setSupportActionBar(tb);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        tb.setNavigationOnClickListener(v -> finish());
        tb.setTitle(tituloMesActual());

        tvMonthTotals = findViewById(R.id.tvMonthTotals);
        rvMonth = findViewById(R.id.rvMonth);
        rvMonth.setLayoutManager(new LinearLayoutManager(this));
        adapter = new DaySummaryAdapter();
        rvMonth.setAdapter(adapter);

        // Observar los eventos y refrescar mes actual
        eventDao.observeActive(appType).observe(this, this::renderMonth);
    }

    private String tituloMesActual() {
        Calendar c = Calendar.getInstance();
        SimpleDateFormat df = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
        return df.format(c.getTime());
    }

    private void renderMonth(List<EventEntity> data) {
        if (data == null) data = new ArrayList<>();

        // Rango del mes actual
        long start = monthStartMillis();
        long end = monthEndMillis();

        // Agrupar por día (solo eventos del mes)
        Map<Long, List<EventEntity>> byDay = new LinkedHashMap<>();
        for (EventEntity e : data) {
            if (e.dueAt < start || e.dueAt >= end) continue;
            long dayStart = dayStartMillis(e.dueAt);
            List<EventEntity> list = byDay.get(dayStart);
            if (list == null) {
                list = new ArrayList<>();
                byDay.put(dayStart, list);
            }
            list.add(e);
        }

        // Construir summaries
        List<DaySummary> summaries = new ArrayList<>();
        int totalPlanned = 0;
        int totalRealized = 0;
        double totalExpenses = 0.0;

        for (Map.Entry<Long, List<EventEntity>> entry : byDay.entrySet()) {
            long dayStart = entry.getKey();
            List<EventEntity> events = entry.getValue();

            int eventsCount = events.size();
            int realizedCount = 0;
            double sum = 0.0;

            for (EventEntity e : events) {
                if (e.realized == 1) {
                    realizedCount++;
                    if (e.cost != null) sum += e.cost;
                }
            }

            summaries.add(new DaySummary(
                    dayStart,
                    eventsCount,
                    realizedCount,
                    events.isEmpty() ? 0.0 : sum
            ));

            totalPlanned += eventsCount;
            totalRealized += realizedCount;
            totalExpenses += sum;
        }

        adapter.submit(summaries);

        // Totales del mes
        String resumen = "Eventos: " + totalPlanned +
                "   ·   Realizados: " + totalRealized +
                "   ·   Gastos: $" + String.format(Locale.getDefault(), "%.2f", totalExpenses);
        tvMonthTotals.setText(resumen);
    }

    private long dayStartMillis(long anyMillis) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(anyMillis);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    private long monthStartMillis() {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.DAY_OF_MONTH, 1);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    private long monthEndMillis() {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.DAY_OF_MONTH, c.getActualMaximum(Calendar.DAY_OF_MONTH));
        c.set(Calendar.HOUR_OF_DAY, 23);
        c.set(Calendar.MINUTE, 59);
        c.set(Calendar.SECOND, 59);
        c.set(Calendar.MILLISECOND, 999);
        return c.getTimeInMillis();
    }
}
