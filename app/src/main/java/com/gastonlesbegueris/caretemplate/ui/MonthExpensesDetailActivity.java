package com.gastonlesbegueris.caretemplate.ui;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
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
import com.google.android.material.appbar.MaterialToolbar;
import com.gastonlesbegueris.caretemplate.util.AppodealHelper;
import com.gastonlesbegueris.caretemplate.util.MenuHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MonthExpensesDetailActivity extends AppCompatActivity {

    private EventDao eventDao;
    private SubjectDao subjectDao;
    private EventAdapter adapter;
    private String appType;
    private String monthStart; // "YYYY-MM-01"
    private TextView tvTotal;
    private TextView tvEmpty;
    private RecyclerView rv;
    private MenuHelper menuHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_month_expenses_detail);

        // Obtener mes del intent
        monthStart = getIntent().getStringExtra("monthStart");
        if (monthStart == null) {
            finish();
            return;
        }

        appType = getString(R.string.app_type);
        AppDb db = AppDb.get(this);
        eventDao = db.eventDao();
        subjectDao = db.subjectDao();
        menuHelper = new MenuHelper(this, appType);

        // Toolbar
        MaterialToolbar toolbar = findViewById(R.id.toolbarMonthExpenses);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationIcon(R.drawable.ic_back);
        toolbar.setNavigationOnClickListener(v -> finish());
        
        // Configurar título con el mes
        try {
            SimpleDateFormat inputFmt = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat outputFmt = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
            java.util.Date date = inputFmt.parse(monthStart);
            String monthTitle = outputFmt.format(date);
            String appName = getString(R.string.app_name);
            toolbar.setTitle(appName + " - " + monthTitle);
        } catch (Exception e) {
            String appName = getString(R.string.app_name);
            toolbar.setTitle(appName + " - " + monthStart);
        }

        tvTotal = findViewById(R.id.tvTotalMonth);
        tvEmpty = findViewById(R.id.tvEmpty);
        rv = findViewById(R.id.rvMonthExpenses);
        rv.setLayoutManager(new LinearLayoutManager(this));
        
        adapter = new EventAdapter(new EventAdapter.OnEventClick() {
            @Override
            public void onEdit(EventEntity e) {
                // No permitir editar desde aquí, solo ver
            }

            @Override
            public void onDelete(EventEntity e) {
                // No permitir eliminar desde aquí
            }

            @Override
            public void onToggleRealized(EventEntity e, boolean realized) {
                // No permitir cambiar estado
            }
        });
        adapter.setAppType(appType);
        rv.setAdapter(adapter);

        // Appodeal Banner
        initAppodeal();

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
        super.onDestroy();
        AppodealHelper.hideBanner(this);
    }

    private void loadData() {
        new Thread(() -> {
            // Calcular rango del mes
            long[] range = calculateMonthRange(monthStart);
            long start = range[0];
            long end = range[1];

            // Obtener eventos realizados del mes (con costo) usando realizedAt para consistencia con listMonthTotals
            List<EventEntity> events = eventDao.listRealizedByRealizedAtInRange(appType, start, end);
            
            // Filtrar solo los que tienen costo
            List<EventEntity> eventsWithCost = new ArrayList<>();
            double total = 0.0;
            for (EventEntity e : events) {
                if (e.cost != null && e.cost > 0) {
                    eventsWithCost.add(e);
                    total += e.cost;
                }
            }

            // Obtener información de sujetos para el adapter
            List<SubjectEntity> subjects = subjectDao.listActiveNow(appType);
            Map<String, String> subjectNames = new HashMap<>();
            Map<String, String> subjectIconKeys = new HashMap<>();
            Map<String, String> subjectColorHex = new HashMap<>();
            
            if (subjects != null) {
                for (SubjectEntity s : subjects) {
                    subjectNames.put(s.id, s.name);
                    subjectIconKeys.put(s.id, s.iconKey);
                    subjectColorHex.put(s.id, s.colorHex);
                }
            }

            final List<EventEntity> finalEvents = eventsWithCost;
            final double finalTotal = total;
            final Map<String, String> finalSubjectNames = subjectNames;
            final Map<String, String> finalSubjectIconKeys = subjectIconKeys;
            final Map<String, String> finalSubjectColorHex = subjectColorHex;

            runOnUiThread(() -> {
                adapter.setSubjectsMap(finalSubjectNames);
                adapter.setSubjectIconKeys(finalSubjectIconKeys);
                adapter.setSubjectColorHex(finalSubjectColorHex);
                adapter.submit(finalEvents);
                
                if (finalEvents.isEmpty()) {
                    tvEmpty.setVisibility(View.VISIBLE);
                    rv.setVisibility(View.GONE);
                    tvTotal.setVisibility(View.GONE);
                } else {
                    tvEmpty.setVisibility(View.GONE);
                    rv.setVisibility(View.VISIBLE);
                    tvTotal.setVisibility(View.VISIBLE);
                    tvTotal.setText(getString(R.string.expenses_total, finalTotal));
                }
            });
        }).start();
    }

    /**
     * Calcula el rango de timestamps para un mes dado en formato "YYYY-MM-01"
     */
    private long[] calculateMonthRange(String monthStart) {
        try {
            SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            java.util.Date date = fmt.parse(monthStart);
            
            Calendar c = Calendar.getInstance();
            c.setTime(date);
            c.set(Calendar.DAY_OF_MONTH, 1);
            c.set(Calendar.HOUR_OF_DAY, 0);
            c.set(Calendar.MINUTE, 0);
            c.set(Calendar.SECOND, 0);
            c.set(Calendar.MILLISECOND, 0);
            long start = c.getTimeInMillis();

            c.add(Calendar.MONTH, 1);
            long end = c.getTimeInMillis() - 1;
            
            return new long[]{start, end};
        } catch (Exception e) {
            // Fallback: usar mes actual
            Calendar c = Calendar.getInstance();
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
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem versionItem = menu.findItem(R.id.action_version);
        if (versionItem != null) {
            try {
                String versionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
                versionItem.setTitle("v" + versionName);
            } catch (android.content.pm.PackageManager.NameNotFoundException e) {
                versionItem.setTitle("v" + com.gastonlesbegueris.caretemplate.BuildConfig.VERSION_NAME);
            }
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (menuHelper.handleMenuSelection(item, MonthExpensesDetailActivity.class)) {
            return true;
        }
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
