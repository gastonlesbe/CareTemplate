package com.gastonlesbegueris.caretemplate.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.gastonlesbegueris.caretemplate.R;
import com.gastonlesbegueris.caretemplate.data.local.AppDb;
import com.gastonlesbegueris.caretemplate.data.local.EventDao;
import com.gastonlesbegueris.caretemplate.data.local.EventEntity;
import com.gastonlesbegueris.caretemplate.data.model.DaySummary;
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

    private androidx.recyclerview.widget.RecyclerView rv;
    private DaySummaryAdapter adapter;
    private TextView tvTotals;
    private final SimpleDateFormat fmtMonth = new SimpleDateFormat("MMMM yyyy", new Locale("es"));

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_agenda_month);

        appType = getString(R.string.app_type);
        eventDao = AppDb.get(this).eventDao();

        MaterialToolbar toolbar = findViewById(R.id.toolbarMonth);
        setSupportActionBar(toolbar);
        toolbar.setNavigationIcon(com.google.android.material.R.drawable.abc_ic_ab_back_material);

        toolbar.setNavigationOnClickListener(v -> finish());
        toolbar.setTitle("Agenda mensual");

        rv = findViewById(R.id.rvMonth);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new DaySummaryAdapter(summary -> {
            // Si querés abrir el día, acá podrías lanzar una pantalla por día
        });
        rv.setAdapter(adapter);

        tvTotals = findViewById(R.id.tvMonthTotals);

        loadMonth();
    }

    private void loadMonth() {
        new Thread(() -> {
            long[] range = monthRange(System.currentTimeMillis());
            long start = range[0];
            long end   = range[1];

            List<EventEntity> all = eventDao.listInRange(appType, start, end);

            // Agrupar por día (inicio del día)
            Map<Long, List<EventEntity>> byDay = new LinkedHashMap<>();
            Calendar c = Calendar.getInstance();
            for (EventEntity e : all) {
                long k = dayStart(e.dueAt, c);
                byDay.computeIfAbsent(k, kk -> new ArrayList<>()).add(e);
            }

            List<DaySummary> summaries = new ArrayList<>();
            double plannedSum = 0.0;
            double realizedSum = 0.0;

            for (Map.Entry<Long, List<EventEntity>> en : byDay.entrySet()) {
                long day = en.getKey();
                List<EventEntity> list = en.getValue();
                int total = list.size();
                int done  = 0;
                Double expenses = null;

                for (EventEntity e : list) {
                    if (e.realized == 1) {
                        done++;
                        if (e.cost != null) {
                            realizedSum += e.cost;
                            expenses = (expenses == null ? 0.0 : expenses) + e.cost;
                        }
                    } else {
                        if (e.cost != null) plannedSum += e.cost;
                    }
                }
                summaries.add(new DaySummary(day, total, done, expenses));
            }

            String monthTitle = fmtMonth.format(start);
            String totalsText = String.format(Locale.getDefault(),
                    "%s · Planificado: $%.2f · Realizado: $%.2f", monthTitle, plannedSum, realizedSum);

            runOnUiThread(() -> {
                adapter.submit(summaries);
                tvTotals.setText(totalsText);
                tvTotals.setVisibility(View.VISIBLE);
            });
        }).start();
    }

    private long[] monthRange(long anyTimeMillis) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(anyTimeMillis);
        c.set(Calendar.DAY_OF_MONTH, 1);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        long start = c.getTimeInMillis();

        c.add(Calendar.MONTH, 1);
        long end = c.getTimeInMillis() - 1;
        return new long[]{start, end};
    }

    private long dayStart(long time, Calendar c) {
        c.setTimeInMillis(time);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }
}
