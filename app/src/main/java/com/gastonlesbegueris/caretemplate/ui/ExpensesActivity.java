package com.gastonlesbegueris.caretemplate.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.gastonlesbegueris.caretemplate.R;
import com.gastonlesbegueris.caretemplate.data.local.*;
import com.google.android.material.appbar.MaterialToolbar;

import java.io.*;
import java.text.DateFormatSymbols;
import java.util.*;

public class ExpensesActivity extends AppCompatActivity {

    private String appType;
    private EventDao eventDao;
    private SubjectDao subjectDao;

    private SubjectSummaryAdapter adapter;
    private long monthStart, monthEnd;
    private final Calendar cal = Calendar.getInstance();

    @Override protected void onCreate(Bundle s) {
        super.onCreate(s);
        setContentView(R.layout.activity_expenses);

        appType = getString(R.string.app_type);
        AppDb db = AppDb.get(this);
        eventDao = db.eventDao();
        subjectDao = db.subjectDao();

        MaterialToolbar tb = findViewById(R.id.toolbarExpenses);
        setSupportActionBar(tb);
        tb.setNavigationOnClickListener(v -> finish());

        RecyclerView rv = findViewById(R.id.rvExpenses);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SubjectSummaryAdapter();
        rv.setAdapter(adapter);

        Button prev = findViewById(R.id.btnPrev);
        Button next = findViewById(R.id.btnNext);
        Button export = findViewById(R.id.btnExport);

        setMonth(System.currentTimeMillis());
        loadAndRender();

        prev.setOnClickListener(v -> { shiftMonth(-1); });
        next.setOnClickListener(v -> { shiftMonth( 1); });
        export.setOnClickListener(v -> exportCsvAndShare());
    }

    private void shiftMonth(int delta) {
        cal.setTimeInMillis(monthStart);
        cal.add(Calendar.MONTH, delta);
        setMonth(cal.getTimeInMillis());
        loadAndRender();
    }

    private void setMonth(long millis) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(millis);
        c.set(Calendar.DAY_OF_MONTH, 1);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        monthStart = c.getTimeInMillis();
        c.add(Calendar.MONTH, 1);
        monthEnd = c.getTimeInMillis();

        String[] months = new DateFormatSymbols(Locale.getDefault()).getMonths();
        Calendar tmp = Calendar.getInstance(); tmp.setTimeInMillis(monthStart);
        String title = months[tmp.get(Calendar.MONTH)] + " " + tmp.get(Calendar.YEAR);
        ((TextView)findViewById(R.id.tvMonthTitle)).setText(title);
    }

    private void loadAndRender() {
        new Thread(() -> {
            // 1) cargar subjects (map id->name) y lista
            List<SubjectEntity> subs = subjectDao.listActiveNow(appType);
            Map<String, SubjectCostSummary> map = new LinkedHashMap<>();
            for (SubjectEntity s : subs) {
                map.put(s.id, new SubjectCostSummary(s.id, s.name));
            }

            // 2) cargar eventos del rango
            List<EventEntity> events = eventDao.listInRange(appType, monthStart, monthEnd); // crea este método si no lo tenés
            if (events != null) {
                for (EventEntity e : events) {
                    SubjectCostSummary scs = map.get(e.subjectId);
                    if (scs == null) {
                        // por si hay eventos antiguos con sujeto borrado
                        scs = new SubjectCostSummary(e.subjectId == null ? "-" : e.subjectId, "(sin sujeto)");
                        map.put(e.subjectId == null ? UUID.randomUUID().toString() : e.subjectId, scs);
                    }
                    scs.count++;
                    if (e.cost != null) {
                        if (e.realized == 1) scs.realized += e.cost;
                        else                 scs.planned  += e.cost;
                    }
                }
            }

            // 3) totales mes
            double totalPlanned = 0, totalReal = 0;
            List<SubjectCostSummary> data = new ArrayList<>(map.values());
            for (SubjectCostSummary x : data) { totalPlanned += x.planned; totalReal += x.realized; }

            // 4) ordenar por mayor Realizado (opcional)
            data.sort((a,b) -> Double.compare(b.realized, a.realized));

            List<SubjectCostSummary> finalData = data;
            double fp = totalPlanned, fr = totalReal;
            runOnUiThread(() -> {
                adapter.submit(finalData);
                ((TextView)findViewById(R.id.tvTotals))
                        .setText(String.format(Locale.getDefault(), "Plan: $%.2f · Real: $%.2f", fp, fr));
            });
        }).start();
    }

    private void exportCsvAndShare() {
        new Thread(() -> {
            try {
                // Reconstruimos dataset para export
                List<SubjectEntity> subs = subjectDao.listActiveNow(appType);
                Map<String, SubjectCostSummary> map = new LinkedHashMap<>();
                for (SubjectEntity s : subs) map.put(s.id, new SubjectCostSummary(s.id, s.name));

                List<EventEntity> events = eventDao.listInRange(appType, monthStart, monthEnd);
                if (events != null) for (EventEntity e : events) {
                    SubjectCostSummary scs = map.get(e.subjectId);
                    if (scs == null) {
                        scs = new SubjectCostSummary(e.subjectId == null ? "-" : e.subjectId, "(sin sujeto)");
                        map.put(e.subjectId == null ? UUID.randomUUID().toString() : e.subjectId, scs);
                    }
                    scs.count++;
                    if (e.cost != null) {
                        if (e.realized == 1) scs.realized += e.cost; else scs.planned += e.cost;
                    }
                }

                // CSV
                StringBuilder sb = new StringBuilder();
                sb.append("Subject,Count,Planned,Realized\n");
                for (SubjectCostSummary x : map.values()) {
                    sb.append(safe(x.subjectName)).append(",")
                            .append(x.count).append(",")
                            .append(String.format(Locale.US, "%.2f", x.planned)).append(",")
                            .append(String.format(Locale.US, "%.2f", x.realized)).append("\n");
                }

                java.io.File dir = new java.io.File(getExternalFilesDir(null), "exports");
                if (!dir.exists()) dir.mkdirs();
                String fname = "expenses_" + System.currentTimeMillis() + ".csv";
                java.io.File file = new java.io.File(dir, fname);
                try (FileOutputStream fos = new FileOutputStream(file)) {
                    fos.write(sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
                }

                Uri uri = FileProvider.getUriForFile(
                        this,
                        getPackageName() + ".fileprovider",
                        file
                );

                Intent share = new Intent(Intent.ACTION_SEND);
                share.setType("text/csv");
                share.putExtra(Intent.EXTRA_STREAM, uri);
                share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                runOnUiThread(() -> startActivity(Intent.createChooser(share, "Compartir CSV")));

            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Export error: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private String safe(String s) {
        if (s == null) return "";
        // envolver entre comillas si contiene coma
        if (s.contains(",") || s.contains("\"")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }
}
