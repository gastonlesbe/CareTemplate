package com.gastonlesbegueris.caretemplate.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.gastonlesbegueris.caretemplate.R;
import com.gastonlesbegueris.caretemplate.data.local.*;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.*;

public class ExpensesActivity extends AppCompatActivity {

    private AppDb db;
    private EventDao eventDao;
    private SubjectDao subjectDao;
    private ExpensesAdapter adapter;
    private String appType;

    private List<EventEntity> currentData = new ArrayList<>();

    private final ActivityResultLauncher<String> createCsvLauncher =
            registerForActivityResult(new ActivityResultContracts.CreateDocument("text/csv"), this::writeCsvToUri);

    @Override protected void onCreate(Bundle s) {
        super.onCreate(s);
        setContentView(R.layout.activity_expenses);

        appType   = getString(R.string.app_type);
        db        = AppDb.get(this);
        eventDao  = db.eventDao();
        subjectDao= db.subjectDao();

        adapter = new ExpensesAdapter();
        androidx.recyclerview.widget.RecyclerView rv = findViewById(R.id.rvExpenses);
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adapter);

        Button btnExport = findViewById(R.id.btnExportCsv);
        btnExport.setOnClickListener(v -> exportCsv());

        loadMonth();
    }

    private void loadMonth() {
        new Thread(() -> {
            long[] range = monthRange(System.currentTimeMillis());
            List<EventEntity> rows = eventDao.listRealizedInRange(appType, range[0], range[1]);
            Double total = eventDao.sumRealizedCostInRange(appType, range[0], range[1]);

            // mapa sujetoId -> nombre
            Map<String,String> names = new HashMap<>();
            List<SubjectEntity> subs = subjectDao.listActiveNow(appType);
            if (subs != null) for (SubjectEntity s : subs) names.put(s.id, s.name);

            currentData = (rows == null) ? Collections.emptyList() : rows;

            runOnUiThread(() -> {
                ((TextView)findViewById(R.id.tvMonthTitle))
                        .setText("Gastos de " + new SimpleDateFormat("MMMM yyyy", Locale.getDefault())
                                .format(new Date(range[0])));
                ((TextView)findViewById(R.id.tvMonthTotal))
                        .setText("Total: $" + String.format(Locale.getDefault(), "%.2f", total == null ? 0.0 : total));
                adapter.submit(currentData, names);
            });
        }).start();
    }

    private long[] monthRange(long anyMillis) {
        Calendar c = Calendar.getInstance(); c.setTimeInMillis(anyMillis);
        c.set(Calendar.DAY_OF_MONTH, 1); c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0); c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0);
        long from = c.getTimeInMillis();
        c.add(Calendar.MONTH, 1);
        long to = c.getTimeInMillis() - 1;
        return new long[]{from, to};
    }

    private void exportCsv() {
        String fname = "gastos_" + new SimpleDateFormat("yyyy_MM", Locale.getDefault()).format(new Date()) + ".csv";
        createCsvLauncher.launch(fname);
    }

    private void writeCsvToUri(Uri uri) {
        if (uri == null) return;
        new Thread(() -> {
            try (OutputStream os = getContentResolver().openOutputStream(uri)) {
                String header = "fecha, sujeto, titulo, costo\n";
                os.write(header.getBytes());

                Map<String,String> names = new HashMap<>();
                List<SubjectEntity> subs = subjectDao.listActiveNow(appType);
                if (subs != null) for (SubjectEntity s : subs) names.put(s.id, s.name);

                SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
                for (EventEntity e : currentData) {
                    String date = fmt.format(new Date(e.updatedAt));
                    String subject = (e.subjectId != null && names.containsKey(e.subjectId)) ? names.get(e.subjectId) : "";
                    String title = e.title == null ? "" : e.title.replace(",", " ");
                    String cost  = (e.cost == null) ? "0" : String.format(Locale.getDefault(), "%.2f", e.cost);
                    String line = date + "," + subject + "," + title + "," + cost + "\n";
                    os.write(line.getBytes());
                }
            } catch (Exception ignore) {}
        }).start();
    }
}
