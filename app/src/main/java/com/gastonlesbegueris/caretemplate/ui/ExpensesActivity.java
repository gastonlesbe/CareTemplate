package com.gastonlesbegueris.caretemplate.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.gastonlesbegueris.caretemplate.R;
import com.gastonlesbegueris.caretemplate.data.local.AppDb;
import com.gastonlesbegueris.caretemplate.data.local.EventDao;
import com.gastonlesbegueris.caretemplate.data.local.EventEntity;
import com.gastonlesbegueris.caretemplate.data.local.SubjectDao;
import com.gastonlesbegueris.caretemplate.data.local.SubjectEntity;
import java.text.SimpleDateFormat;
import java.util.*;

public class ExpensesActivity extends AppCompatActivity {

    private EventDao eventDao;
    private SubjectDao subjectDao;
    private String appType;

    private RecyclerView rv;
    private TextView tvTotals;
    private Map<String,String> subjectNames = new java.util.HashMap<>();
    private final SimpleDateFormat fmt = new SimpleDateFormat("dd/MM HH:mm", Locale.getDefault());


    private String selectedSubjectId = null; // null = Todos

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_expenses);

        appType = getString(R.string.app_type);
        AppDb db = AppDb.get(this);
        subjectDao = db.subjectDao();
        eventDao   = db.eventDao();

        Spinner sp = findViewById(R.id.spSubjectsFilter);
        TextView tvTotal = findViewById(R.id.tvTotal);
        RecyclerView rv = findViewById(R.id.rvExpenses); // asumiendo que ya lo tenés
        rv.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this));
        ExpensesAdapter adapter = new ExpensesAdapter();  // tu adapter de gastos
        rv.setAdapter(adapter);
        subjectDao.observeActive(appType).observe(this, subjects -> {
            subjectNames.clear();
            if (subjects != null) {
                for (SubjectEntity s : subjects) {
                    subjectNames.put(s.id, s.name);
                }
            }
            reloadRange(adapter, tvTotal); // Refresca la pantalla si cambia el nombre
        });

        // cargar sujetos
        new Thread(() -> {
            java.util.List<SubjectEntity> subjects = subjectDao.listActiveNow(appType);
            java.util.List<String> names = new java.util.ArrayList<>();
            names.add("Todos");
            if (subjects != null) for (SubjectEntity s : subjects) names.add(s.name);

            runOnUiThread(() -> {
                ArrayAdapter<String> ad = new ArrayAdapter<>(this,
                        android.R.layout.simple_spinner_dropdown_item, names);
                sp.setAdapter(ad);

                sp.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
                    @Override public void onItemSelected(android.widget.AdapterView<?> parent, View view, int pos, long id) {
                        selectedSubjectId = (pos == 0) ? null : subjects.get(pos - 1).id;
                        reloadRange(adapter, tvTotal);
                    }
                    @Override public void onNothingSelected(android.widget.AdapterView<?> parent) { }
                });

                // disparo inicial
                reloadRange(adapter, tvTotal);
            });
        }).start();
    }

    private void reloadRange(ExpensesAdapter adapter, TextView tvTotal) {
        new Thread(() -> {
            long now = System.currentTimeMillis();
            long start = now - 30L * 24 * 60 * 60 * 1000; // últimos 30 días
            List<EventEntity> rows = eventDao.listRealizedInRange(appType, start, now);
            Double sum = eventDao.sumRealizedCostInRange(appType, start, now);

            runOnUiThread(() -> {
                adapter.submit(rows, subjectNames); // 👈 AHORA BIEN
                if (sum != null) tvTotal.setText("Total: $" + String.format("%.2f", sum));
                else tvTotal.setText("Total: $0.00");
            });
        }).start();
    }


    private void loadThisMonth() {
        new Thread(() -> {
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.DAY_OF_MONTH, 1);
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            long start = cal.getTimeInMillis();
            cal.add(Calendar.MONTH, 1);
            long end = cal.getTimeInMillis();

            List<EventEntity> list = eventDao.listRealizedInRange(appType, start, end);

            // mapa sujetoId -> nombre para mostrar "Sujeto · Evento"
            Map<String,String> subjectNames = new HashMap<>();
            for (SubjectEntity s : subjectDao.listActiveNow(appType)) subjectNames.put(s.id, s.name);

            double sum = 0d;
            if (list != null) {
                for (EventEntity e : list) if (e.cost != null) sum += e.cost;
            }
            final double fsum = sum;
            runOnUiThread(() -> {
                tvTotals.setText(String.format(Locale.getDefault(), "Total del mes: $%.2f", fsum));
                ((ExpensesAdapter) rv.getAdapter()).submit(list, subjectNames);
            });
        }).start();
    }

    // --- Adapter simple para gastos ---
    static class ExpensesAdapter extends RecyclerView.Adapter<ExpensesAdapter.VH> {
        private final List<EventEntity> items = new ArrayList<>();
        private Map<String,String> subjectNames = new HashMap<>();
        private final SimpleDateFormat fmt = new SimpleDateFormat("dd/MM HH:mm", Locale.getDefault());



        void submit(List<EventEntity> data, Map<String,String> names) {
            items.clear();
            if (data != null) items.addAll(data);
            subjectNames = (names == null) ? new HashMap<>() : names;
            notifyDataSetChanged();
        }

        @Override public VH onCreateViewHolder(android.view.ViewGroup p, int v) {
            android.view.View view = android.view.LayoutInflater.from(p.getContext())
                    .inflate(R.layout.item_expense, p, false);
            return new VH(view);
        }

        @Override public void onBindViewHolder(VH h, int pos) {
            EventEntity e = items.get(pos);
            String who = subjectNames.getOrDefault(e.subjectId, "—");
            h.tvTitle.setText(who + " · " + e.title);
            h.tvWhen.setText(fmt.format(new Date(e.dueAt)));
            h.tvCost.setText(String.format(Locale.getDefault(), "$%.2f", (e.cost == null ? 0d : e.cost)));
        }

        @Override public int getItemCount() { return items.size(); }

        static class VH extends RecyclerView.ViewHolder {
            TextView tvTitle, tvWhen, tvCost;
            VH(android.view.View v) { super(v);
                tvTitle = v.findViewById(R.id.tvTitle);
                tvWhen  = v.findViewById(R.id.tvWhen);
                tvCost  = v.findViewById(R.id.tvCost);
            }
        }

    }
}
