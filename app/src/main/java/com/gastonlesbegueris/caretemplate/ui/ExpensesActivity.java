package com.gastonlesbegueris.caretemplate.ui;

import android.content.res.Configuration;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.gastonlesbegueris.caretemplate.R;
import com.gastonlesbegueris.caretemplate.data.local.AppDb;
import com.gastonlesbegueris.caretemplate.data.local.EventDao;
import com.gastonlesbegueris.caretemplate.data.model.MonthTotal;
import com.gastonlesbegueris.caretemplate.util.FabHelper;
import com.google.android.material.appbar.MaterialToolbar;
import com.gastonlesbegueris.caretemplate.util.AppodealHelper;

import java.util.List;
import java.util.Locale;

public class ExpensesActivity extends AppCompatActivity {

    private EventDao eventDao;
    private String appType;
    private com.gastonlesbegueris.caretemplate.util.MenuHelper menuHelper;

    private RecyclerView rv;
    private ExpensesAdapter adapter;
    private TextView tvTotal;

    @Override
    protected void onCreate(Bundle s) {
        super.onCreate(s);
        setContentView(R.layout.activity_expenses);

        appType = getString(R.string.app_type);
        eventDao = AppDb.get(this).eventDao();
        menuHelper = new com.gastonlesbegueris.caretemplate.util.MenuHelper(this, appType);

        MaterialToolbar toolbar = findViewById(R.id.toolbarExpenses);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationIcon(R.drawable.ic_back);
        toolbar.setNavigationOnClickListener(v -> finish());
        String appName = getString(R.string.app_name);
        String sectionName = getString(R.string.menu_expenses);
        toolbar.setTitle(appName + " - " + sectionName);

        tvTotal = findViewById(R.id.tvTotalExpenses);

        rv = findViewById(R.id.rvExpenses);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ExpensesAdapter();
        rv.setAdapter(adapter);

        // Appodeal Banner
        initAppodeal();

        // FAB para agregar eventos (comportamiento uniforme)
        FabHelper.initFabSpeedDial(this, R.id.fabAdd, R.id.fabAddSubject, R.id.fabAddEvent, rv);

        loadData();
    }

    private void initAppodeal() {
        String appKey = getString(R.string.appodeal_app_key);
        AppodealHelper.initialize(this, appKey);
        AppodealHelper.showBanner(this, R.id.adView);
    }

    @Override
    protected void onPause() {
        super.onPause();
        AppodealHelper.hideBanner(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        String appKey = getString(R.string.appodeal_app_key);
        AppodealHelper.initialize(this, appKey);
        AppodealHelper.showBanner(this, R.id.adView);
    }

    @Override
    protected void onDestroy() {
        AppodealHelper.hideBanner(this);
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
                tvTotal.setText(getString(R.string.expenses_total, finalTotal));
            });
        }).start();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // Establecer la versión dinámicamente
        MenuItem versionItem = menu.findItem(R.id.action_version);
        if (versionItem != null) {
            try {
                String versionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
                versionItem.setTitle("v" + versionName);
            } catch (android.content.pm.PackageManager.NameNotFoundException e) {
                versionItem.setTitle("v1.4");
            }
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (menuHelper.handleMenuSelection(item, ExpensesActivity.class)) {
            return true;
        }
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
}
