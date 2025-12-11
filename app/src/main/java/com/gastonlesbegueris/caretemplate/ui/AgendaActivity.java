package com.gastonlesbegueris.caretemplate.ui;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.gastonlesbegueris.caretemplate.R;
import com.gastonlesbegueris.caretemplate.data.local.AppDb;
import com.gastonlesbegueris.caretemplate.data.local.EventDao;
import com.gastonlesbegueris.caretemplate.data.local.EventEntity;
import com.gastonlesbegueris.caretemplate.data.local.SubjectDao;
import com.gastonlesbegueris.caretemplate.data.local.SubjectEntity;
import com.google.android.material.appbar.MaterialToolbar;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Calendar;
import java.util.ArrayList;
import java.util.Locale;
import java.text.SimpleDateFormat;

public class AgendaActivity extends AppCompatActivity {

    private String appType;
    private EventDao dao;
    private SubjectDao subjectDao;
    private AgendaAdapter adapter;
    private com.gastonlesbegueris.caretemplate.util.MenuHelper menuHelper;
    private final SimpleDateFormat dayFmt = new SimpleDateFormat("EEE dd/MM", Locale.getDefault());

    @Override protected void onCreate(Bundle s) {
        super.onCreate(s);
        setContentView(R.layout.activity_agenda);

        appType = getString(R.string.app_type);
        AppDb db = AppDb.get(this);
        dao = db.eventDao();
        subjectDao = db.subjectDao();
        menuHelper = new com.gastonlesbegueris.caretemplate.util.MenuHelper(this, appType);

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
        RecyclerView rv = findViewById(R.id.rvAgenda);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AgendaAdapter(new AgendaAdapter.OnEventClick() {
            @Override public void onEdit(EventEntity e) { showEditDialog(e); }
            @Override public void onDelete(EventEntity e) { softDelete(e.id); }
            
            @Override
            public void onToggleRealized(EventEntity e, boolean realized) {
                setRealized(e.id, realized);
            }
        });
        adapter.setAppType(appType); // Pasar el appType para mostrar kilómetros si es un auto
        rv.setAdapter(adapter);

        observeUpcoming();
        observeSubjectsForAdapter();

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

    private void observeUpcoming() {
        dao.observeUpcomingOrdered(appType).observe(this, new Observer<List<EventEntity>>() {
            @Override public void onChanged(List<EventEntity> events) {
                adapter.submit(groupByDay(events));
            }
        });
    }

    private void observeSubjectsForAdapter() {
        if (subjectDao == null) return;
        subjectDao.observeActive(appType).observe(this, subjects -> {
            Map<String, String> nameMap = new HashMap<>();
            Map<String, String> iconKeyMap = new HashMap<>();
            Map<String, String> colorHexMap = new HashMap<>();
            if (subjects != null) {
                for (SubjectEntity s : subjects) {
                    nameMap.put(s.id, s.name);
                    if (s.iconKey != null) iconKeyMap.put(s.id, s.iconKey);
                    if (s.colorHex != null) colorHexMap.put(s.id, s.colorHex);
                }
            }
            adapter.setSubjectsMap(nameMap);
            adapter.setSubjectIconKeys(iconKeyMap);
            adapter.setSubjectColorHex(colorHexMap);
        });
    }

    private List<AgendaAdapter.Row> groupByDay(List<EventEntity> events) {
        List<AgendaAdapter.Row> rows = new ArrayList<>();
        if (events == null || events.isEmpty()) return rows;

        Calendar cal = Calendar.getInstance();
        long currentDayStart = -1;
        for (EventEntity e : events) {
            cal.setTimeInMillis(e.dueAt);
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            long dayStart = cal.getTimeInMillis();
            if (dayStart != currentDayStart) {
                currentDayStart = dayStart;
                rows.add(AgendaAdapter.Row.header(dayFmt.format(cal.getTime())));
            }
            rows.add(AgendaAdapter.Row.event(e));
        }
        return rows;
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
        if (menuHelper.handleMenuSelection(item, AgendaActivity.class)) {
            return true;
        }
        if (item.getItemId() == android.R.id.home) {
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

        androidx.recyclerview.widget.RecyclerView rv = findViewById(R.id.rvAgenda);
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
        // Verificar que la Activity aún existe
        if (isFinishing() || isDestroyed()) {
            return;
        }
        
        // Si es un evento repetido, necesitamos obtener el original en un hilo de fondo
        if (e.originalEventId != null) {
            // Buscar el original en un hilo de fondo antes de mostrar el diálogo
            new Thread(() -> {
                try {
                    EventEntity original = dao.findOriginalEvent(e.originalEventId);
                    runOnUiThread(() -> {
                        if (!isFinishing() && !isDestroyed()) {
                            // Mostrar el diálogo con los datos del original
                            showEditDialogWithOriginal(e, original);
                        }
                    });
                } catch (Exception ex) {
                    android.util.Log.e("AgendaActivity", "Error al buscar evento original", ex);
                    runOnUiThread(() -> {
                        if (!isFinishing() && !isDestroyed()) {
                            android.widget.Toast.makeText(this, "Error al cargar evento", android.widget.Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }).start();
            return;
        }
        
        // Si no es repetido, mostrar diálogo directamente
        showEditDialogWithOriginal(e, null);
    }
    
    private void showEditDialogWithOriginal(EventEntity e, EventEntity original) {
        // Verificar que la Activity aún existe
        if (isFinishing() || isDestroyed()) {
            return;
        }
        
        try {
            // Si es un evento repetido y tenemos el original, usar sus datos de repetición
            EventEntity eventToEdit = e;
            if (e.originalEventId != null && original != null) {
                // Usar los datos del evento repetido para título, costo, fecha, pero la repetición del original
                eventToEdit = new EventEntity();
                eventToEdit.id = e.id; // Mantener el ID del evento que se está editando
                eventToEdit.title = e.title;
                eventToEdit.cost = e.cost;
                eventToEdit.dueAt = e.dueAt;
                eventToEdit.notificationMinutesBefore = e.notificationMinutesBefore;
                // Usar la configuración de repetición del original
                eventToEdit.repeatType = original.repeatType;
                eventToEdit.repeatInterval = original.repeatInterval;
                eventToEdit.repeatEndDate = original.repeatEndDate;
                eventToEdit.repeatCount = original.repeatCount;
                eventToEdit.originalEventId = e.originalEventId;
            }
            
            final EventEntity finalEvent = eventToEdit;
        
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
        final android.widget.TextView tvRepeatIntervalUnit = view.findViewById(R.id.tvRepeatIntervalUnit);
        
        // Controles de notificación
        final android.widget.Spinner spNotification = view.findViewById(R.id.spNotification);
        
        // Controles de fecha y hora
        final com.google.android.material.textfield.TextInputEditText etEventDate = view.findViewById(R.id.etEventDate);
        final com.google.android.material.textfield.TextInputEditText etEventTime = view.findViewById(R.id.etEventTime);

        if (etTitle == null || etCost == null) {
            android.widget.Toast.makeText(this, getString(R.string.error_load_dialog), android.widget.Toast.LENGTH_SHORT).show();
            return;
        }

        // Pre-populate fields
        etTitle.setText(finalEvent.title);
        if (finalEvent.cost != null) {
            etCost.setText(String.format(java.util.Locale.getDefault(), "%.2f", finalEvent.cost));
        }
        
        // Pre-populate fecha y hora
        if (etEventDate != null && etEventTime != null) {
            java.util.Calendar cal = java.util.Calendar.getInstance();
            cal.setTimeInMillis(finalEvent.dueAt);
            java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault());
            java.text.SimpleDateFormat timeFormat = new java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault());
            etEventDate.setText(dateFormat.format(cal.getTime()));
            etEventTime.setText(timeFormat.format(cal.getTime()));
            etEventDate.setTag(finalEvent.dueAt);
            etEventTime.setTag(finalEvent.dueAt);
            
            // Configurar click en fecha
            etEventDate.setOnClickListener(v -> {
                long currentDate = etEventDate.getTag() != null ? (Long) etEventDate.getTag() : finalEvent.dueAt;
                pickDateOnly(currentDate, dateMillis -> {
                    java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault());
                    etEventDate.setText(sdf.format(new java.util.Date(dateMillis)));
                    etEventDate.setTag(dateMillis);
                });
            });
            
            // Configurar click en hora - abrir TimePickerDialog con formato 24 horas
            etEventTime.setOnClickListener(v -> {
                long currentTime = etEventTime.getTag() != null ? (Long) etEventTime.getTag() : finalEvent.dueAt;
                Calendar timeCal = Calendar.getInstance();
                timeCal.setTimeInMillis(currentTime);
                int hour = timeCal.get(Calendar.HOUR_OF_DAY);
                int minute = timeCal.get(Calendar.MINUTE);
                
                // Crear TimePickerDialog con formato 24 horas
                android.app.TimePickerDialog timePickerDialog = new android.app.TimePickerDialog(
                    AgendaActivity.this,
                    (timeView, selectedHour, selectedMinute) -> {
                        // Actualizar el tiempo en el Calendar
                        timeCal.set(Calendar.HOUR_OF_DAY, selectedHour);
                        timeCal.set(Calendar.MINUTE, selectedMinute);
                        timeCal.set(Calendar.SECOND, 0);
                        timeCal.set(Calendar.MILLISECOND, 0);
                        
                        // Formatear y mostrar la hora seleccionada
                        java.text.SimpleDateFormat timeFormat2 = new java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault());
                        etEventTime.setText(timeFormat2.format(timeCal.getTime()));
                        etEventTime.setTag(timeCal.getTimeInMillis());
                    },
                    hour,
                    minute,
                    true // true = formato 24 horas
                );
                timePickerDialog.setTitle(getString(R.string.event_time_hint));
                timePickerDialog.show();
            });
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
            if (finalEvent.repeatType != null) {
                int position = 0;
                switch (finalEvent.repeatType) {
                    case "hourly": position = 1; break;
                    case "daily": position = 2; break;
                    case "monthly": position = 3; break;
                    case "yearly": position = 4; break;
                }
                spRepeatType.setSelection(position);
            }
            
            // Pre-populate repeat interval
            if (finalEvent.repeatInterval != null && etRepeatInterval != null) {
                etRepeatInterval.setText(String.valueOf(finalEvent.repeatInterval));
            }
            
            // Pre-populate repeat end date
            if (finalEvent.repeatEndDate != null && etRepeatEndDate != null) {
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault());
                etRepeatEndDate.setText(sdf.format(new java.util.Date(finalEvent.repeatEndDate)));
                etRepeatEndDate.setTag(finalEvent.repeatEndDate);
            }
            
            // Pre-populate repeat count
            if (finalEvent.repeatCount != null && etRepeatCount != null) {
                etRepeatCount.setText(String.valueOf(finalEvent.repeatCount));
            }
            
            // Mostrar/ocultar opciones de repetición según la selección y actualizar unidad
            spRepeatType.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View view, int position, long id) {
                    if (llRepeatOptions != null) {
                        llRepeatOptions.setVisibility(position == 0 ? android.view.View.GONE : android.view.View.VISIBLE);
                    }
                    // Actualizar la etiqueta de unidad según el tipo seleccionado
                    if (tvRepeatIntervalUnit != null && position > 0) {
                        String unit = "";
                        switch (position) {
                            case 1: unit = getString(R.string.event_repeat_interval_hours); break;
                            case 2: unit = getString(R.string.event_repeat_interval_days); break;
                            case 3: unit = getString(R.string.event_repeat_interval_months); break;
                            case 4: unit = getString(R.string.event_repeat_interval_years); break;
                        }
                        tvRepeatIntervalUnit.setText(unit);
                    }
                }
                @Override
                public void onNothingSelected(android.widget.AdapterView<?> parent) {}
            });
            
            // Mostrar opciones si hay repetición y actualizar unidad
            if (llRepeatOptions != null && finalEvent.repeatType != null) {
                llRepeatOptions.setVisibility(android.view.View.VISIBLE);
                // Actualizar la etiqueta de unidad según el tipo de repetición del evento
                if (tvRepeatIntervalUnit != null) {
                    String unit = "";
                    switch (finalEvent.repeatType) {
                        case "hourly": unit = getString(R.string.event_repeat_interval_hours); break;
                        case "daily": unit = getString(R.string.event_repeat_interval_days); break;
                        case "monthly": unit = getString(R.string.event_repeat_interval_months); break;
                        case "yearly": unit = getString(R.string.event_repeat_interval_years); break;
                    }
                    tvRepeatIntervalUnit.setText(unit);
                }
            }
            
            // Configurar click en fecha de fin
            if (etRepeatEndDate != null) {
                etRepeatEndDate.setOnClickListener(v -> {
                    long initialDate = finalEvent.repeatEndDate != null ? finalEvent.repeatEndDate : System.currentTimeMillis();
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
            if (finalEvent.notificationMinutesBefore != null) {
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

        // Obtener referencias a la barra de título personalizada y sus iconos
        final android.view.View llDialogTitleBar = view.findViewById(R.id.llDialogTitleBar);
        final android.widget.ImageButton ibDelete = view.findViewById(R.id.ibDelete);
        final android.widget.ImageButton ibSave = view.findViewById(R.id.ibSave);
        final android.widget.ImageButton ibClose = view.findViewById(R.id.ibClose);
        
        // Ocultar botones al final para edición
        final com.google.android.material.button.MaterialButton btnSave = view.findViewById(R.id.btnSave);
        final com.google.android.material.button.MaterialButton btnCancel = view.findViewById(R.id.btnCancel);
        if (btnSave != null) {
            btnSave.setVisibility(android.view.View.GONE);
        }
        if (btnCancel != null) {
            btnCancel.setVisibility(android.view.View.GONE);
        }
        
        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(this)
                .setView(view)
                .create();
        
        // Mostrar barra de título personalizada con iconos
        if (llDialogTitleBar != null) {
            llDialogTitleBar.setVisibility(android.view.View.VISIBLE);
        }
        
        // Configurar listener del icono Eliminar
        if (ibDelete != null) {
            ibDelete.setOnClickListener(v -> {
                dialog.dismiss();
                softDelete(e.id);
            });
        }
        
        // Configurar listener del icono Cerrar (Cancelar)
        if (ibClose != null) {
            ibClose.setOnClickListener(v -> dialog.dismiss());
        }
        
        // Configurar listener del icono Guardar
        if (ibSave != null) {
            ibSave.setOnClickListener(v -> {
                String title = etTitle.getText() != null ? etTitle.getText().toString().trim() : "";
                if (title.isEmpty()) {
                    android.widget.Toast.makeText(this, getString(R.string.event_title_required), android.widget.Toast.LENGTH_SHORT).show();
                    return;
                }

                final String c = etCost.getText() != null ? etCost.getText().toString().trim() : "";
                final Double cost = c.isEmpty() ? null : safeParseDouble(c);
                
                // Capturar fecha y hora
                long dueAt = finalEvent.dueAt;
                if (etEventDate != null && etEventTime != null) {
                    java.util.Calendar cal = java.util.Calendar.getInstance();
                    
                    // Obtener fecha
                    if (etEventDate.getTag() != null) {
                        cal.setTimeInMillis((Long) etEventDate.getTag());
                    } else {
                        cal.setTimeInMillis(finalEvent.dueAt);
                    }
                    
                    // Obtener hora
                    if (etEventTime.getTag() != null) {
                        java.util.Calendar timeCal = java.util.Calendar.getInstance();
                        timeCal.setTimeInMillis((Long) etEventTime.getTag());
                        cal.set(java.util.Calendar.HOUR_OF_DAY, timeCal.get(java.util.Calendar.HOUR_OF_DAY));
                        cal.set(java.util.Calendar.MINUTE, timeCal.get(java.util.Calendar.MINUTE));
                    }
                    
                    cal.set(java.util.Calendar.SECOND, 0);
                    cal.set(java.util.Calendar.MILLISECOND, 0);
                    dueAt = cal.getTimeInMillis();
                }
                
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

                updateLocal(e, title, cost, dueAt, fRepeatType, fRepeatInterval, fRepeatEndDate, fRepeatCount, fNotificationMinutesBefore);
                dialog.dismiss();
            });
        }
        
        dialog.show();
        } catch (Exception ex) {
            android.util.Log.e("AgendaActivity", "Error al mostrar diálogo de edición", ex);
            if (!isFinishing() && !isDestroyed()) {
                android.widget.Toast.makeText(this, "Error al cargar el diálogo de edición", android.widget.Toast.LENGTH_SHORT).show();
            }
        }
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
            try {
                // Determinar si es evento original o repetido
                EventEntity originalEvent = e;
                boolean isRepeatedEvent = e.originalEventId != null;
                
                if (isRepeatedEvent) {
                    // Si es un evento repetido, obtener el original
                    originalEvent = dao.findOriginalEvent(e.originalEventId);
                    if (originalEvent == null) {
                        // Si no se encuentra el original, actualizar solo este evento
                        updateSingleEvent(e, title, cost, dueAt, repeatType, repeatInterval, repeatEndDate, repeatCount, notificationMinutesBefore);
                        return;
                    }
                }
                
                // Verificar si la configuración de repetición cambió
                boolean repeatChanged = !java.util.Objects.equals(originalEvent.repeatType, repeatType) ||
                                       !java.util.Objects.equals(originalEvent.repeatInterval, repeatInterval) ||
                                       !java.util.Objects.equals(originalEvent.repeatEndDate, repeatEndDate) ||
                                       !java.util.Objects.equals(originalEvent.repeatCount, repeatCount);
                
                if (repeatChanged && (repeatType != null || originalEvent.repeatType != null)) {
                    // La repetición cambió, necesitamos regenerar los eventos repetidos
                    updateEventWithRepeat(originalEvent, title, cost, dueAt, repeatType, repeatInterval, 
                                        repeatEndDate, repeatCount, notificationMinutesBefore);
                } else {
                    // Solo actualizar el evento individual (o el original si no hay repetición)
                    if (isRepeatedEvent) {
                        // Actualizar solo el evento repetido que se editó
                        updateSingleEvent(e, title, cost, dueAt, null, null, null, null, notificationMinutesBefore);
                    } else {
                        // Actualizar el evento original
                        updateSingleEvent(originalEvent, title, cost, dueAt, repeatType, repeatInterval, 
                                        repeatEndDate, repeatCount, notificationMinutesBefore);
                    }
                }
            } catch (Exception ex) {
                android.util.Log.e("AgendaActivity", "Error al actualizar evento", ex);
                runOnUiThread(() -> {
                    if (!isFinishing() && !isDestroyed()) {
                        android.widget.Toast.makeText(this, "Error al actualizar evento: " + ex.getMessage(), android.widget.Toast.LENGTH_LONG).show();
                    }
                });
            }
        }).start();
    }
    
    private void updateSingleEvent(EventEntity e, String title, Double cost, long dueAt,
                                  String repeatType, Integer repeatInterval, Long repeatEndDate, Integer repeatCount,
                                  Integer notificationMinutesBefore) {
        final boolean[] updateSuccess = {false};
        try {
            // Cancelar notificación anterior si existía (no crítico si falla)
            try {
                if (e.notificationMinutesBefore != null && e.notificationMinutesBefore > 0) {
                    com.gastonlesbegueris.caretemplate.util.NotificationHelper.cancelNotification(getApplicationContext(), e.id);
                }
            } catch (Exception notifEx) {
                android.util.Log.w("AgendaActivity", "Error al cancelar notificación anterior (no crítico)", notifEx);
            }
            
            e.title = title;
            e.cost = cost;
            e.dueAt = dueAt;
            if (repeatType != null || e.originalEventId == null) {
                // Solo actualizar repetición si es el evento original o si se está eliminando la repetición
                e.repeatType = repeatType;
                e.repeatInterval = repeatInterval;
                e.repeatEndDate = repeatEndDate;
                e.repeatCount = repeatCount;
            }
            e.notificationMinutesBefore = notificationMinutesBefore;
            e.updatedAt = System.currentTimeMillis();
            e.dirty = 1;
            dao.update(e);
            updateSuccess[0] = true;
            
            // Programar nueva notificación si es necesario (no crítico si falla)
            try {
                if (notificationMinutesBefore != null && notificationMinutesBefore > 0) {
                    com.gastonlesbegueris.caretemplate.util.NotificationHelper.scheduleNotification(getApplicationContext(), e);
                }
            } catch (Exception notifEx) {
                android.util.Log.w("AgendaActivity", "Error al programar notificación (no crítico)", notifEx);
            }
            
            runOnUiThread(() -> {
                if (!isFinishing() && !isDestroyed()) {
                    android.widget.Toast.makeText(this, getString(R.string.event_updated), android.widget.Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception ex) {
            android.util.Log.e("AgendaActivity", "Error al actualizar evento individual: " + ex.getMessage(), ex);
            runOnUiThread(() -> {
                if (!isFinishing() && !isDestroyed()) {
                    if (updateSuccess[0]) {
                        // Si la actualización fue exitosa pero hubo un error menor, mostrar éxito
                        android.widget.Toast.makeText(this, getString(R.string.event_updated), android.widget.Toast.LENGTH_SHORT).show();
                    } else {
                        android.widget.Toast.makeText(this, "Error al actualizar evento", android.widget.Toast.LENGTH_LONG).show();
                    }
                }
            });
        }
    }
    
    private void updateEventWithRepeat(EventEntity originalEvent, String title, Double cost, long dueAt,
                                       String repeatType, Integer repeatInterval, Long repeatEndDate, Integer repeatCount,
                                       Integer notificationMinutesBefore) {
        final boolean[] updateSuccess = {false};
        try {
            // Cancelar notificaciones de eventos repetidos existentes (no crítico si falla)
            try {
                java.util.List<EventEntity> repeatedEvents = dao.findRepeatedEvents(originalEvent.id);
                for (EventEntity repeated : repeatedEvents) {
                    if (repeated != null && repeated.notificationMinutesBefore != null && repeated.notificationMinutesBefore > 0) {
                        com.gastonlesbegueris.caretemplate.util.NotificationHelper.cancelNotification(getApplicationContext(), repeated.id);
                    }
                }
            } catch (Exception notifEx) {
                android.util.Log.w("AgendaActivity", "Error al cancelar notificaciones repetidas (no crítico)", notifEx);
            }
            
            // Cancelar notificación del original (no crítico si falla)
            try {
                if (originalEvent.notificationMinutesBefore != null && originalEvent.notificationMinutesBefore > 0) {
                    com.gastonlesbegueris.caretemplate.util.NotificationHelper.cancelNotification(getApplicationContext(), originalEvent.id);
                }
            } catch (Exception notifEx) {
                android.util.Log.w("AgendaActivity", "Error al cancelar notificación original (no crítico)", notifEx);
            }
            
            // Eliminar eventos repetidos existentes
            long now = System.currentTimeMillis();
            dao.softDeleteRepeatedEvents(originalEvent.id, now);
            
            // Actualizar el evento original
            originalEvent.title = title;
            originalEvent.cost = cost;
            originalEvent.dueAt = dueAt;
            originalEvent.repeatType = repeatType;
            originalEvent.repeatInterval = repeatInterval;
            originalEvent.repeatEndDate = repeatEndDate;
            originalEvent.repeatCount = repeatCount;
            originalEvent.notificationMinutesBefore = notificationMinutesBefore;
            originalEvent.updatedAt = now;
            originalEvent.dirty = 1;
            dao.update(originalEvent);
            updateSuccess[0] = true;
            
            // Programar notificación para el original (no crítico si falla)
            try {
                if (notificationMinutesBefore != null && notificationMinutesBefore > 0) {
                    com.gastonlesbegueris.caretemplate.util.NotificationHelper.scheduleNotification(getApplicationContext(), originalEvent);
                }
            } catch (Exception notifEx) {
                android.util.Log.w("AgendaActivity", "Error al programar notificación original (no crítico)", notifEx);
            }
            
            // Si hay repetición, crear nuevos eventos repetidos (no crítico si falla parcialmente)
            try {
                if (repeatType != null && repeatInterval != null && repeatInterval > 0) {
                    createRepeatedEvents(originalEvent, repeatType, repeatInterval, repeatEndDate, repeatCount, notificationMinutesBefore);
                }
            } catch (Exception repeatEx) {
                android.util.Log.w("AgendaActivity", "Error al crear eventos repetidos (no crítico)", repeatEx);
            }
            
            runOnUiThread(() -> {
                if (!isFinishing() && !isDestroyed()) {
                    String message = getString(R.string.event_updated);
                    if (repeatType != null) {
                        message += " (repetición actualizada)";
                    }
                    android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception ex) {
            android.util.Log.e("AgendaActivity", "Error al actualizar evento con repetición: " + ex.getMessage(), ex);
            runOnUiThread(() -> {
                if (!isFinishing() && !isDestroyed()) {
                    if (updateSuccess[0]) {
                        // Si la actualización fue exitosa pero hubo un error menor, mostrar éxito
                        String message = getString(R.string.event_updated);
                        if (repeatType != null) {
                            message += " (repetición actualizada)";
                        }
                        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show();
                    } else {
                        android.widget.Toast.makeText(this, "Error al actualizar evento", android.widget.Toast.LENGTH_LONG).show();
                    }
                }
            });
        }
    }
    
    private void createRepeatedEvents(EventEntity originalEvent, String repeatType, Integer repeatInterval,
                                     Long repeatEndDate, Integer repeatCount, Integer notificationMinutesBefore) {
        try {
            long dueAt = originalEvent.dueAt;
            java.util.Calendar cal = java.util.Calendar.getInstance();
            cal.setTimeInMillis(dueAt);
            
            int eventsCreated = 1; // Ya contamos el original
            long nextDueAt = dueAt;
            
            while (true) {
                java.util.Calendar nextCal = java.util.Calendar.getInstance();
                nextCal.setTimeInMillis(nextDueAt);
                
                switch (repeatType) {
                    case "hourly":
                        nextCal.add(java.util.Calendar.HOUR_OF_DAY, repeatInterval);
                        break;
                    case "daily":
                        nextCal.add(java.util.Calendar.DAY_OF_MONTH, repeatInterval);
                        break;
                    case "monthly":
                        nextCal.add(java.util.Calendar.MONTH, repeatInterval);
                        break;
                    case "yearly":
                        nextCal.add(java.util.Calendar.YEAR, repeatInterval);
                        break;
                }
                
                nextDueAt = nextCal.getTimeInMillis();
                
                boolean shouldStop = false;
                if (repeatEndDate != null && nextDueAt > repeatEndDate) {
                    shouldStop = true;
                } else if (repeatCount != null && eventsCreated >= repeatCount) {
                    shouldStop = true;
                } else if (repeatEndDate == null && repeatCount == null) {
                    // Si no hay límite, crear eventos por 10 años por defecto (prácticamente sin límite)
                    java.util.Calendar limitCal = java.util.Calendar.getInstance();
                    limitCal.setTimeInMillis(dueAt);
                    limitCal.add(java.util.Calendar.YEAR, 10);
                    if (nextDueAt > limitCal.getTimeInMillis()) {
                        shouldStop = true;
                    }
                }
                
                if (shouldStop) break;
                
                // Crear evento repetido
                EventEntity repeatedEvent = new EventEntity();
                repeatedEvent.id = java.util.UUID.randomUUID().toString();
                repeatedEvent.uid = originalEvent.uid;
                repeatedEvent.appType = originalEvent.appType;
                repeatedEvent.subjectId = originalEvent.subjectId;
                repeatedEvent.title = originalEvent.title;
                repeatedEvent.note = originalEvent.note;
                repeatedEvent.cost = null;
                repeatedEvent.kilometersAtEvent = null;
                repeatedEvent.realized = 0;
                repeatedEvent.dueAt = nextDueAt;
                repeatedEvent.updatedAt = System.currentTimeMillis();
                repeatedEvent.deleted = 0;
                repeatedEvent.dirty = 1;
                repeatedEvent.repeatType = null;
                repeatedEvent.repeatInterval = null;
                repeatedEvent.repeatEndDate = null;
                repeatedEvent.repeatCount = null;
                repeatedEvent.originalEventId = originalEvent.id;
                repeatedEvent.notificationMinutesBefore = notificationMinutesBefore;
                
                dao.insert(repeatedEvent);
                
                if (notificationMinutesBefore != null && notificationMinutesBefore > 0) {
                    com.gastonlesbegueris.caretemplate.util.NotificationHelper.scheduleNotification(getApplicationContext(), repeatedEvent);
                }
                
                eventsCreated++;
            }
        } catch (Exception ex) {
            android.util.Log.e("AgendaActivity", "Error al crear eventos repetidos", ex);
        }
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
