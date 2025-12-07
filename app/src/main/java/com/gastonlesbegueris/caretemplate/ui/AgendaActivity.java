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
        adapter = new EventAdapter(new EventAdapter.OnEventClick() {
            @Override public void onEdit(EventEntity e) { showEditDialog(e); }
            @Override public void onDelete(EventEntity e) { softDelete(e.id); }
            
            @Override
            public void onToggleRealized(EventEntity e, boolean realized) {
                setRealized(e.id, realized);
            }
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

    // ===== Edit Event Dialog =====
    private void showEditDialog(EventEntity e) {
        final android.view.View view = getLayoutInflater().inflate(R.layout.dialog_add_event, null);
        final com.google.android.material.textfield.TextInputEditText etTitle = view.findViewById(R.id.etTitle);
        final com.google.android.material.textfield.TextInputEditText etCost = view.findViewById(R.id.etCost);
        final android.widget.Spinner sp = view.findViewById(R.id.spSubject);
        
        // Controles de repetición
        final android.widget.Spinner spRepeatType = view.findViewById(R.id.spRepeatType);
        final android.view.View llRepeatOptions = view.findViewById(R.id.llRepeatOptions);
        final com.google.android.material.textfield.TextInputEditText etRepeatInterval = view.findViewById(R.id.etRepeatInterval);
        final com.google.android.material.textfield.TextInputEditText etRepeatEndDate = view.findViewById(R.id.etRepeatEndDate);
        final com.google.android.material.textfield.TextInputEditText etRepeatCount = view.findViewById(R.id.etRepeatCount);
        
        // Controles de notificación
        final android.widget.Spinner spNotification = view.findViewById(R.id.spNotification);

        if (etTitle == null || etCost == null) {
            android.widget.Toast.makeText(this, getString(R.string.error_load_dialog), android.widget.Toast.LENGTH_SHORT).show();
            return;
        }

        // Pre-populate fields
        etTitle.setText(e.title);
        if (e.cost != null) {
            etCost.setText(String.format(java.util.Locale.getDefault(), "%.2f", e.cost));
        }

        // Hide subject spinner for editing
        if (sp != null) {
            sp.setVisibility(android.view.View.GONE);
        }
        
        // Configurar Spinner de repetición
        if (spRepeatType != null) {
            java.util.List<String> repeatOptions = new java.util.ArrayList<>();
            repeatOptions.add(getString(R.string.event_repeat_none));
            repeatOptions.add(getString(R.string.event_repeat_hourly));
            repeatOptions.add(getString(R.string.event_repeat_daily));
            repeatOptions.add(getString(R.string.event_repeat_monthly));
            repeatOptions.add(getString(R.string.event_repeat_yearly));
            android.widget.ArrayAdapter<String> repeatAdapter = new android.widget.ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, repeatOptions);
            spRepeatType.setAdapter(repeatAdapter);
            
            // Seleccionar el tipo de repetición actual
            if (e.repeatType != null) {
                int position = 0;
                switch (e.repeatType) {
                    case "hourly": position = 1; break;
                    case "daily": position = 2; break;
                    case "monthly": position = 3; break;
                    case "yearly": position = 4; break;
                }
                spRepeatType.setSelection(position);
            }
            
            // Pre-populate repeat interval
            if (e.repeatInterval != null && etRepeatInterval != null) {
                etRepeatInterval.setText(String.valueOf(e.repeatInterval));
            }
            
            // Pre-populate repeat end date
            if (e.repeatEndDate != null && etRepeatEndDate != null) {
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault());
                etRepeatEndDate.setText(sdf.format(new java.util.Date(e.repeatEndDate)));
                etRepeatEndDate.setTag(e.repeatEndDate);
            }
            
            // Pre-populate repeat count
            if (e.repeatCount != null && etRepeatCount != null) {
                etRepeatCount.setText(String.valueOf(e.repeatCount));
            }
            
            // Mostrar/ocultar opciones de repetición según la selección
            spRepeatType.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View view, int position, long id) {
                    if (llRepeatOptions != null) {
                        llRepeatOptions.setVisibility(position == 0 ? android.view.View.GONE : android.view.View.VISIBLE);
                    }
                }
                @Override
                public void onNothingSelected(android.widget.AdapterView<?> parent) {}
            });
            
            // Mostrar opciones si hay repetición
            if (llRepeatOptions != null && e.repeatType != null) {
                llRepeatOptions.setVisibility(android.view.View.VISIBLE);
            }
            
            // Configurar click en fecha de fin
            if (etRepeatEndDate != null) {
                etRepeatEndDate.setOnClickListener(v -> {
                    long initialDate = e.repeatEndDate != null ? e.repeatEndDate : System.currentTimeMillis();
                    pickDateOnly(initialDate, dateMillis -> {
                        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault());
                        etRepeatEndDate.setText(sdf.format(new java.util.Date(dateMillis)));
                        etRepeatEndDate.setTag(dateMillis);
                    });
                });
            }
        }
        
        // Configurar Spinner de notificación
        if (spNotification != null) {
            java.util.List<String> notificationOptions = new java.util.ArrayList<>();
            notificationOptions.add(getString(R.string.event_notification_none));
            notificationOptions.add(getString(R.string.event_notification_5min));
            notificationOptions.add(getString(R.string.event_notification_15min));
            notificationOptions.add(getString(R.string.event_notification_30min));
            notificationOptions.add(getString(R.string.event_notification_1hour));
            notificationOptions.add(getString(R.string.event_notification_2hours));
            notificationOptions.add(getString(R.string.event_notification_1day));
            notificationOptions.add(getString(R.string.event_notification_2days));
            notificationOptions.add(getString(R.string.event_notification_1week));
            android.widget.ArrayAdapter<String> notificationAdapter = new android.widget.ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, notificationOptions);
            spNotification.setAdapter(notificationAdapter);
            
            // Seleccionar la notificación actual
            if (e.notificationMinutesBefore != null) {
                int position = 0;
                switch (e.notificationMinutesBefore) {
                    case 5: position = 1; break;
                    case 15: position = 2; break;
                    case 30: position = 3; break;
                    case 60: position = 4; break;
                    case 120: position = 5; break;
                    case 1440: position = 6; break;
                    case 2880: position = 7; break;
                    case 10080: position = 8; break;
                }
                spNotification.setSelection(position);
            }
        }

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(getString(R.string.edit_event))
                .setView(view)
                .setPositiveButton(getString(R.string.button_choose_date_time), (d, w) -> {
                    String title = etTitle.getText() != null ? etTitle.getText().toString().trim() : "";
                    if (title.isEmpty()) {
                        android.widget.Toast.makeText(this, getString(R.string.event_title_required), android.widget.Toast.LENGTH_SHORT).show();
                        return;
                    }

                    final String c = etCost.getText() != null ? etCost.getText().toString().trim() : "";
                    final Double cost = c.isEmpty() ? null : safeParseDouble(c);
                    
                    // Capturar configuración de repetición
                    String repeatType = null;
                    Integer repeatInterval = null;
                    Long repeatEndDate = null;
                    Integer repeatCount = null;
                    
                    if (spRepeatType != null && spRepeatType.getSelectedItemPosition() > 0) {
                        int repeatPos = spRepeatType.getSelectedItemPosition();
                        switch (repeatPos) {
                            case 1: repeatType = "hourly"; break;
                            case 2: repeatType = "daily"; break;
                            case 3: repeatType = "monthly"; break;
                            case 4: repeatType = "yearly"; break;
                        }
                        
                        if (repeatType != null) {
                            if (etRepeatInterval != null) {
                                String intervalStr = etRepeatInterval.getText() != null ? etRepeatInterval.getText().toString().trim() : "";
                                if (!intervalStr.isEmpty()) {
                                    try {
                                        repeatInterval = Integer.parseInt(intervalStr);
                                        if (repeatInterval < 1) repeatInterval = 1;
                                    } catch (Exception ex) {
                                        repeatInterval = 1;
                                    }
                                } else {
                                    repeatInterval = 1;
                                }
                            } else {
                                repeatInterval = 1;
                            }
                            
                            if (etRepeatEndDate != null && etRepeatEndDate.getTag() != null) {
                                repeatEndDate = (Long) etRepeatEndDate.getTag();
                            } else if (etRepeatCount != null) {
                                String countStr = etRepeatCount.getText() != null ? etRepeatCount.getText().toString().trim() : "";
                                if (!countStr.isEmpty()) {
                                    try {
                                        repeatCount = Integer.parseInt(countStr);
                                        if (repeatCount < 1) repeatCount = null;
                                    } catch (Exception ex) {
                                        // Ignorar
                                    }
                                }
                            }
                        }
                    }
                    
                    final String fRepeatType = repeatType;
                    final Integer fRepeatInterval = repeatInterval;
                    final Long fRepeatEndDate = repeatEndDate;
                    final Integer fRepeatCount = repeatCount;
                    
                    // Capturar configuración de notificación
                    Integer notificationMinutesBefore = null;
                    if (spNotification != null && spNotification.getSelectedItemPosition() > 0) {
                        int notificationPos = spNotification.getSelectedItemPosition();
                        switch (notificationPos) {
                            case 1: notificationMinutesBefore = 5; break;
                            case 2: notificationMinutesBefore = 15; break;
                            case 3: notificationMinutesBefore = 30; break;
                            case 4: notificationMinutesBefore = 60; break;
                            case 5: notificationMinutesBefore = 120; break;
                            case 6: notificationMinutesBefore = 1440; break;
                            case 7: notificationMinutesBefore = 2880; break;
                            case 8: notificationMinutesBefore = 10080; break;
                        }
                    }
                    
                    final Integer fNotificationMinutesBefore = notificationMinutesBefore;

                    pickDateTime(e.dueAt, dueAt -> updateLocal(e, title, cost, dueAt, fRepeatType, fRepeatInterval, fRepeatEndDate, fRepeatCount, fNotificationMinutesBefore));
                })
                .setNeutralButton(getString(R.string.button_delete), (d, w) -> softDelete(e.id))
                .setNegativeButton(getString(R.string.button_cancel), null)
                .show();
    }

    private Double safeParseDouble(String s) {
        try {
            return Double.parseDouble(s);
        } catch (Exception e) {
            return null;
        }
    }

    private void pickDateTime(long initialMillis, DateTimeCallback cb) {
        final Calendar cal = Calendar.getInstance();
        if (initialMillis > 0) cal.setTimeInMillis(initialMillis);
        int y = cal.get(Calendar.YEAR);
        int m = cal.get(Calendar.MONTH);
        int d = cal.get(Calendar.DAY_OF_MONTH);
        int hh = cal.get(Calendar.HOUR_OF_DAY);
        int mm = cal.get(Calendar.MINUTE);

        new android.app.DatePickerDialog(this, (v, year, month, day) -> {
            cal.set(Calendar.YEAR, year);
            cal.set(Calendar.MONTH, month);
            cal.set(Calendar.DAY_OF_MONTH, day);
            new android.app.TimePickerDialog(this, (tp, hour, minute) -> {
                cal.set(Calendar.HOUR_OF_DAY, hour);
                cal.set(Calendar.MINUTE, minute);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);
                cb.onPicked(cal.getTimeInMillis());
            }, hh, mm, true).show();
        }, y, m, d).show();
    }
    
    private void pickDateOnly(long initialMillis, DateTimeCallback cb) {
        final Calendar cal = Calendar.getInstance();
        if (initialMillis > 0) cal.setTimeInMillis(initialMillis);
        int y = cal.get(Calendar.YEAR);
        int m = cal.get(Calendar.MONTH);
        int d = cal.get(Calendar.DAY_OF_MONTH);

        new android.app.DatePickerDialog(this, (v, year, month, day) -> {
            cal.set(Calendar.YEAR, year);
            cal.set(Calendar.MONTH, month);
            cal.set(Calendar.DAY_OF_MONTH, day);
            cal.set(Calendar.HOUR_OF_DAY, 23);
            cal.set(Calendar.MINUTE, 59);
            cal.set(Calendar.SECOND, 59);
            cal.set(Calendar.MILLISECOND, 999);
            cb.onPicked(cal.getTimeInMillis());
        }, y, m, d).show();
    }

    private interface DateTimeCallback {
        void onPicked(long millis);
    }

    private void updateLocal(EventEntity e, String title, Double cost, long dueAt, 
                             String repeatType, Integer repeatInterval, Long repeatEndDate, Integer repeatCount,
                             Integer notificationMinutesBefore) {
        new Thread(() -> {
            // Cancelar notificación anterior si existía
            if (e.notificationMinutesBefore != null && e.notificationMinutesBefore > 0) {
                com.gastonlesbegueris.caretemplate.util.NotificationHelper.cancelNotification(this, e.id);
            }
            
            e.title = title;
            e.cost = cost;
            e.dueAt = dueAt;
            e.repeatType = repeatType;
            e.repeatInterval = repeatInterval;
            e.repeatEndDate = repeatEndDate;
            e.repeatCount = repeatCount;
            e.notificationMinutesBefore = notificationMinutesBefore;
            e.updatedAt = System.currentTimeMillis();
            e.dirty = 1;
            dao.update(e);
            
            // Programar nueva notificación si es necesario
            if (notificationMinutesBefore != null && notificationMinutesBefore > 0) {
                com.gastonlesbegueris.caretemplate.util.NotificationHelper.scheduleNotification(this, e);
            }
            
            runOnUiThread(() -> android.widget.Toast.makeText(this, getString(R.string.event_updated), android.widget.Toast.LENGTH_SHORT).show());
        }).start();
    }

    private void softDelete(String id) {
        new Thread(() -> {
            EventEntity event = dao.findOne(id);
            if (event != null && event.notificationMinutesBefore != null && event.notificationMinutesBefore > 0) {
                com.gastonlesbegueris.caretemplate.util.NotificationHelper.cancelNotification(this, id);
            }
            dao.softDelete(id, System.currentTimeMillis());
            runOnUiThread(() -> android.widget.Toast.makeText(this, getString(R.string.event_deleted), android.widget.Toast.LENGTH_SHORT).show());
        }).start();
    }
    
    private void setRealized(String id, boolean realized) {
        new Thread(() -> {
            long now = System.currentTimeMillis();
            if (realized) {
                dao.markRealizedOne(id, now);
            } else {
                dao.markUnrealizedOne(id, now);
            }
            runOnUiThread(() -> {
                String msg = realized ? getString(R.string.event_marked_realized) : getString(R.string.event_marked_pending);
                android.widget.Toast.makeText(this, msg, android.widget.Toast.LENGTH_SHORT).show();
            });
        }).start();
    }

}
