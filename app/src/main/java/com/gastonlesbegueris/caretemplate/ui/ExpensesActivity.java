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
        String appName = getString(R.string.app_name);
        String sectionName = getString(R.string.menu_expenses);
        toolbar.setTitle(appName + " - " + sectionName);

        tvTotal = findViewById(R.id.tvTotalExpenses);

        rv = findViewById(R.id.rvExpenses);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ExpensesAdapter();
        rv.setAdapter(adapter);

        // AdMob Banner
        initAdMob();

        // FAB para agregar eventos
        // Inicializar FAB Speed Dial
        initFabSpeedDial();

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
                versionItem.setTitle("v1.2");
            }
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_agenda) {
            startActivity(new android.content.Intent(this, AgendaMonthActivity.class));
            return true;
        } else if (id == R.id.action_subjects) {
            startActivity(new android.content.Intent(this, SubjectListActivity.class));
            return true;
        } else if (id == R.id.action_expenses) {
            // Already on expenses page
            return true;
        } else if (id == R.id.action_recovery) {
            // Redirigir a SubjectListActivity para mostrar el código de recuperación
            android.content.Intent intent = new android.content.Intent(this, SubjectListActivity.class);
            intent.putExtra("show_recovery_code", true);
            startActivity(intent);
            return true;
        } else if (id == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    // ===== FAB Speed Dial =====
    private boolean fabMenuOpen = false;
    
    private void initFabSpeedDial() {
        View fab = findViewById(R.id.fabAdd);
        View fabSubject = findViewById(R.id.fabAddSubject);
        View fabEvent = findViewById(R.id.fabAddEvent);

        // Iconos fijos: flavor y calendario
        ((com.google.android.material.floatingactionbutton.FloatingActionButton) fabSubject)
                .setImageResource(R.drawable.ic_header_flavor);
        ((com.google.android.material.floatingactionbutton.FloatingActionButton) fabEvent)
                .setImageResource(R.drawable.ic_event);

        fab.setOnClickListener(v -> toggleFabMenu());

        fabSubject.setOnClickListener(v -> {
            closeFabMenu();
            // Redirigir a SubjectListActivity para agregar sujeto
            android.content.Intent intent = new android.content.Intent(this, SubjectListActivity.class);
            startActivity(intent);
        });
        fabEvent.setOnClickListener(v -> {
            closeFabMenu();
            // Redirigir a SubjectListActivity para agregar evento
            android.content.Intent intent = new android.content.Intent(this, SubjectListActivity.class);
            intent.putExtra("add_event", true);
            startActivity(intent);
        });

        androidx.recyclerview.widget.RecyclerView rv = findViewById(R.id.rvExpenses);
        if (rv != null) {
            rv.addOnScrollListener(new androidx.recyclerview.widget.RecyclerView.OnScrollListener() {
                @Override public void onScrolled(androidx.recyclerview.widget.RecyclerView recyclerView, int dx, int dy) {
                    if (fabMenuOpen && Math.abs(dy) > 6) closeFabMenu();
                }
            });
        }
    }

    private void toggleFabMenu() { if (fabMenuOpen) closeFabMenu(); else openFabMenu(); }

    private void openFabMenu() {
        fabMenuOpen = true;
        showFabWithAnim(findViewById(R.id.fabAddSubject));
        showFabWithAnim(findViewById(R.id.fabAddEvent));
        rotateFab(true);
    }

    private void closeFabMenu() {
        fabMenuOpen = false;
        hideFabWithAnim(findViewById(R.id.fabAddSubject));
        hideFabWithAnim(findViewById(R.id.fabAddEvent));
        rotateFab(false);
    }

    private void showFabWithAnim(View fab) {
        fab.setVisibility(View.VISIBLE);
        fab.setScaleX(0.9f); fab.setScaleY(0.9f); fab.setAlpha(0f);
        fab.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(150).start();
    }

    private void hideFabWithAnim(View fab) {
        fab.animate().alpha(0f).scaleX(0.9f).scaleY(0.9f).setDuration(120)
                .withEndAction(() -> fab.setVisibility(View.GONE)).start();
    }

    private void rotateFab(boolean open) {
        View main = findViewById(R.id.fabAdd);
        main.animate().rotation(open ? 45f : 0f).setDuration(150).start();
    }
}
