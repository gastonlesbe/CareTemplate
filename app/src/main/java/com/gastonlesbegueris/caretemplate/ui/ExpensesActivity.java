package com.gastonlesbegueris.caretemplate.ui;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.gastonlesbegueris.caretemplate.R;
import com.gastonlesbegueris.caretemplate.data.local.AppDb;
import com.gastonlesbegueris.caretemplate.data.local.EventDao;
import com.gastonlesbegueris.caretemplate.data.model.MonthTotal;
import com.google.android.material.appbar.MaterialToolbar;

import java.util.List;
import java.util.Locale;

public class ExpensesActivity extends AppCompatActivity {

    private EventDao eventDao;
    private String appType;

    private RecyclerView rv;
    private ExpensesAdapter adapter;
    private TextView tvTotal;

    @Override
    protected void onCreate(Bundle s) {
        super.onCreate(s);
        setContentView(R.layout.activity_expenses);

        appType = getString(R.string.app_type);
        eventDao = AppDb.get(this).eventDao();

        MaterialToolbar toolbar = findViewById(R.id.toolbarExpenses);
        setSupportActionBar(toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_back); // 👈 asegurate que sea ic_back, no ic_black
        toolbar.setNavigationOnClickListener(v -> finish());
        toolbar.setTitle("Gastos");

        tvTotal = findViewById(R.id.tvTotalExpenses);

        rv = findViewById(R.id.rvExpenses);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ExpensesAdapter();
        rv.setAdapter(adapter);

        loadData();
    }

    private void loadData() {
        new Thread(() -> {
            List<MonthTotal> rows = eventDao.listMonthTotals(appType);

            double total = 0.0;
            if (rows != null) {
                for (MonthTotal m : rows) {
                    if (m.realizedSum != null) total += m.realizedSum;
                }
            }

            final double finalTotal = total;
            runOnUiThread(() -> {
                adapter.submit(rows);
                tvTotal.setText(String.format(Locale.getDefault(), "Total gastado: $%.2f", finalTotal));
            });
        }).start();
    }
}
