package com.gastonlesbegueris.caretemplate.ui;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
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
        tb.setNavigationOnClickListener(v -> finish());

        // Recycler
        RecyclerView rv = findViewById(R.id.rvDay);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new EventAdapter(new EventAdapter.OnEventClick() {
            @Override public void onEdit(EventEntity e) { /* podrías abrir editor */ }
            @Override public void onDelete(EventEntity e) { /* opcional */ }
        });
        rv.setAdapter(adapter);

        // Día por defecto = hoy
        setDay(System.currentTimeMillis());
        observeDay();

        // Picker de día
        Button btnPick = findViewById(R.id.btnPickDay);
        btnPick.setOnClickListener(v -> showDayPicker());
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
                tv.setText(String.format(Locale.getDefault(), "Plan: $%.2f · Real: $%.2f", planned, realized));
            });
        }).start();
    }

    private void showDayPicker() {
        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Elegí un día")
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
            startActivity(new android.content.Intent(this, ExpensesActivity.class));
            return true;
        } else if (id == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
