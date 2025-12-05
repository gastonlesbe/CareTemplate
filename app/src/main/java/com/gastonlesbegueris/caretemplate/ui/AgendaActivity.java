package com.gastonlesbegueris.caretemplate.ui;

import android.content.res.Configuration;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.gastonlesbegueris.caretemplate.R;
import com.gastonlesbegueris.caretemplate.data.local.AppDb;
import com.gastonlesbegueris.caretemplate.data.local.EventDao;
import com.gastonlesbegueris.caretemplate.data.local.EventEntity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.datepicker.MaterialDatePicker;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AgendaActivity extends AppCompatActivity {

    private String appType;
    private EventDao dao;
    private EventAdapter adapter; // o LocalEventAdapter
    private long dayStart, dayEnd;

    private final SimpleDateFormat dayFmt = new SimpleDateFormat("EEE dd/MM", Locale.getDefault());

    @Override protected void onCreate(Bundle s) {
        super.onCreate(s);
        setContentView(R.layout.activity_agenda);

        appType = getString(R.string.app_type);
        dao = AppDb.get(this).eventDao();

        MaterialToolbar tb = findViewById(R.id.toolbarAgenda);
        setSupportActionBar(tb);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        tb.setNavigationIcon(R.drawable.ic_back);
        tb.setNavigationOnClickListener(v -> finish());
        String appName = getString(R.string.app_name);
        String sectionName = getString(R.string.menu_agenda);
        tb.setTitle(appName + " - " + sectionName);

        // Recycler
        RecyclerView rv = findViewById(R.id.rvDay);
        rv.setLayoutManager(new LinearLayoutManager(this));
        EventAdapter adapter = new EventAdapter(new EventAdapter.OnEventClick() {
            @Override public void onEdit(EventEntity e) { /* podrías abrir editor */ }
            @Override public void onDelete(EventEntity e) { /* opcional */ }
        });
        adapter.setAppType(appType); // Pasar el appType para mostrar kilómetros si es un auto
        rv.setAdapter(adapter);

        // Día por defecto = hoy
        setDay(System.currentTimeMillis());
        observeDay();

        // Picker de día
        Button btnPick = findViewById(R.id.btnPickDay);
        btnPick.setOnClickListener(v -> showDayPicker());

        // Inicializar FAB Speed Dial
        initFabSpeedDial();

        // AdMob
        initAdMob();
    }

    private void initAdMob() {
        com.google.android.gms.ads.MobileAds.initialize(this, initializationStatus -> {});
        com.google.android.gms.ads.AdView adView = findViewById(R.id.adView);
        if (adView != null) {
            com.google.android.gms.ads.AdRequest adRequest = new com.google.android.gms.ads.AdRequest.Builder().build();
            adView.loadAd(adRequest);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        com.google.android.gms.ads.AdView adView = findViewById(R.id.adView);
        if (adView != null) {
            adView.pause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        com.google.android.gms.ads.AdView adView = findViewById(R.id.adView);
        if (adView != null) {
            adView.resume();
        }
    }

    @Override
    protected void onDestroy() {
        com.google.android.gms.ads.AdView adView = findViewById(R.id.adView);
        if (adView != null) {
            adView.destroy();
        }
        super.onDestroy();
    }

    private void setDay(long millis) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(millis);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        dayStart = c.getTimeInMillis();
        c.add(Calendar.DAY_OF_MONTH, 1);
        dayEnd = c.getTimeInMillis();

        MaterialToolbar tb = findViewById(R.id.toolbarAgenda);
        tb.setSubtitle(dayFmt.format(new Date(dayStart)));

        updateTotals();
    }

    private void observeDay() {
        dao.observeByDay(appType, dayStart, dayEnd).observe(this, new Observer<List<EventEntity>>() {
            @Override public void onChanged(List<EventEntity> events) {
                adapter.submit(events);
            }
        });
    }

    private void updateTotals() {
        new Thread(() -> {
            double planned = dao.sumPlannedCostInRange(appType, dayStart, dayEnd);
            double realized = dao.sumRealizedCostInRange(appType, dayStart, dayEnd);
            runOnUiThread(() -> {
                TextView tv = findViewById(R.id.tvTotals);
                tv.setText(getString(R.string.expenses_plan_real, planned, realized));
            });
        }).start();
    }

    private void showDayPicker() {
        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(getString(R.string.choose_day))
                .setSelection(dayStart)
                .build();

        picker.addOnPositiveButtonClickListener(selection -> {
            setDay(selection);
            // renovar el observer para el nuevo rango
            observeDay();
        });

        picker.show(getSupportFragmentManager(), "DAY_PICKER");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
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
            startActivity(new android.content.Intent(this, ExpensesActivity.class));
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

        androidx.recyclerview.widget.RecyclerView rv = findViewById(R.id.rvEvents);
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
