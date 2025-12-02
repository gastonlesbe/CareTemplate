package com.gastonlesbegueris.caretemplate.ui;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.gastonlesbegueris.caretemplate.R;
import com.gastonlesbegueris.caretemplate.data.local.AppDb;
import com.gastonlesbegueris.caretemplate.data.local.EventDao;
import com.gastonlesbegueris.caretemplate.data.model.MonthTotal;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;

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
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationIcon(R.drawable.ic_back);
        toolbar.setNavigationOnClickListener(v -> finish());
        toolbar.setTitle("Gastos");

        tvTotal = findViewById(R.id.tvTotalExpenses);

        rv = findViewById(R.id.rvExpenses);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ExpensesAdapter();
        rv.setAdapter(adapter);

        // AdMob Banner
        initAdMob();

        loadData();
    }

    private void initAdMob() {
        MobileAds.initialize(this, initializationStatus -> {});
        AdView adView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        adView.loadAd(adRequest);
    }

    @Override
    protected void onPause() {
        super.onPause();
        AdView adView = findViewById(R.id.adView);
        if (adView != null) {
            adView.pause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        AdView adView = findViewById(R.id.adView);
        if (adView != null) {
            adView.resume();
        }
    }

    @Override
    protected void onDestroy() {
        AdView adView = findViewById(R.id.adView);
        if (adView != null) {
            adView.destroy();
        }
        super.onDestroy();
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_home) {
            startActivity(new android.content.Intent(this, com.gastonlesbegueris.caretemplate.ui.MainActivity.class));
            finish();
            return true;
        } else if (id == R.id.action_agenda) {
            startActivity(new android.content.Intent(this, AgendaMonthActivity.class));
            return true;
        } else if (id == R.id.action_subjects) {
            startActivity(new android.content.Intent(this, SubjectListActivity.class));
            return true;
        } else if (id == R.id.action_expenses) {
            // Already on expenses page
            return true;
        } else if (id == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
