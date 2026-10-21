package com.gastonlesbegueris.caretemplate.ui;

import android.content.res.Configuration;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
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
import com.google.android.material.tabs.TabLayout;
import com.gastonlesbegueris.caretemplate.util.AppodealHelper;
import com.gastonlesbegueris.caretemplate.util.AdMobHelper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SubjectHistoryActivity extends AppCompatActivity {

    private EventDao eventDao;
    private SubjectDao subjectDao;
    private EventAdapter adapter;
    private String appType;
    private String subjectId;
    private String subjectName;
    private TabLayout tabLayout;
    private View emptyState;
    private List<EventEntity> historyEvents = new java.util.ArrayList<>();
    private List<EventEntity> upcomingEvents = new java.util.ArrayList<>();
    private com.gastonlesbegueris.caretemplate.util.MenuHelper menuHelper;
    // Appodeal handles interstitial ads automatically
    private Runnable syncCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_subject_history);

        // Obtener datos del intent
        subjectId = getIntent().getStringExtra("subjectId");
        subjectName = getIntent().getStringExtra("subjectName");

        if (subjectId == null) {
            finish();
            return;
        }

        appType = getString(R.string.app_type);
        AppDb db = AppDb.get(this);
        eventDao = db.eventDao();
        subjectDao = db.subjectDao();
        menuHelper = new com.gastonlesbegueris.caretemplate.util.MenuHelper(this, appType);

        // Toolbar
        MaterialToolbar toolbar = findViewById(R.id.toolbarHistory);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationIcon(R.drawable.ic_back);
        toolbar.setNavigationOnClickListener(v -> finish());
        String appName = getString(R.string.app_name);
        String displayTitle = subjectName != null ? subjectName : getString(R.string.history_title);
        toolbar.setTitle(appName + " - " + displayTitle);

        // RecyclerView
        RecyclerView rv = findViewById(R.id.rvEvents);
        rv.setLayoutManager(new LinearLayoutManager(this));
        
        adapter = new EventAdapter(new EventAdapter.OnEventClick() {
            @Override
            public void onEdit(EventEntity e) {
                showEditDialog(e);
            }

            @Override
            public void onDelete(EventEntity e) {
                softDelete(e.id);
            }

            @Override
            public void onToggleRealized(EventEntity e, boolean realized) {
                // Permitir cambiar estado de realizado
                setRealized(e.id, realized);
            }
        });
        adapter.setAppType(appType); // Pasar el appType para mostrar kilómetros si es un auto
        rv.setAdapter(adapter);

        tabLayout = findViewById(R.id.tabEvents);
        emptyState = findViewById(R.id.emptyState);
        setupTabs();

        // Observar eventos del sujeto (histórico y próximos)
        observeSubjectHistory();
        observeSubjectUpcoming();

        // Observar sujetos para el mapa de nombres
        observeSubjectsForAdapter();

        // FAB para agregar eventos
        com.google.android.material.floatingactionbutton.FloatingActionButton fabAddEvent = findViewById(R.id.fabAddEvent);
        if (fabAddEvent != null) {
            fabAddEvent.setOnClickListener(v -> showAddEventDialog());
        }

        // Appodeal
        initAppodeal();
    }

    private void observeSubjectHistory() {
        eventDao.observeSubjectHistory(appType, subjectId).observe(this, events -> {
            historyEvents = events != null ? events : new java.util.ArrayList<>();
            refreshTabData();
        });
    }

    private void observeSubjectUpcoming() {
        eventDao.observeSubjectUpcoming(appType, subjectId).observe(this, events -> {
            upcomingEvents = events != null ? events : new java.util.ArrayList<>();
            refreshTabData();
        });
    }

    private void setupTabs() {
        if (tabLayout == null) return;
        tabLayout.addTab(tabLayout.newTab().setText(getString(R.string.tab_upcoming)));
        tabLayout.addTab(tabLayout.newTab().setText(getString(R.string.tab_history)));
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) { refreshTabData(); }
            @Override public void onTabUnselected(TabLayout.Tab tab) { }
            @Override public void onTabReselected(TabLayout.Tab tab) { refreshTabData(); }
        });
        TabLayout.Tab defaultTab = tabLayout.getTabAt(0);
        if (defaultTab != null) defaultTab.select();
        refreshTabData();
    }

    private void refreshTabData() {
        boolean showHistory = tabLayout != null && tabLayout.getSelectedTabPosition() == 1;
        List<EventEntity> data = showHistory ? historyEvents : upcomingEvents;
        adapter.submit(data);
        if (emptyState != null) {
            boolean isEmpty = data == null || data.isEmpty();
            emptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
            if (emptyState instanceof android.widget.TextView) {
                ((android.widget.TextView) emptyState).setText(
                        getString(showHistory ? R.string.no_events_history : R.string.no_events_upcoming)
                );
            }
        }
    }

    private void observeSubjectsForAdapter() {
        subjectDao.observeActive(appType).observe(this, subjects -> {
            Map<String, String> nameMap = new HashMap<>();
            Map<String, String> iconKeyMap = new HashMap<>();
            Map<String, String> colorHexMap = new HashMap<>();
            if (subjects != null) {
                for (SubjectEntity s : subjects) {
                    nameMap.put(s.id, s.name);
                    if (s.iconKey != null) {
                        iconKeyMap.put(s.id, s.iconKey);
                    }
                    if (s.colorHex != null && !s.colorHex.isEmpty()) {
                        colorHexMap.put(s.id, s.colorHex);
                    }
                }
            }
            adapter.setSubjectsMap(nameMap);
            adapter.setSubjectIconKeys(iconKeyMap);
            adapter.setSubjectColorHex(colorHexMap);
        });
    }

    private void setRealized(String id, boolean realized) {
        new Thread(() -> {
            long now = System.currentTimeMillis();
            if (realized) {
                // Cancelar todas las alarmas programadas para este evento
                com.gastonlesbegueris.caretemplate.util.NotificationHelper.cancelNotification(this, id);
                eventDao.markRealizedOne(id, now);
            } else {
                eventDao.markUnrealizedOne(id, now);
            }
            runOnUiThread(() -> {
                String msg = realized ? "Marcado como realizado" : "Marcado como pendiente";
                android.widget.Toast.makeText(this, msg, android.widget.Toast.LENGTH_SHORT).show();
            });
        }).start();
    }

    private void showAddEventDialog() {
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

        if (etTitle == null || etCost == null || sp == null) {
            android.widget.Toast.makeText(this, "Error al cargar el diálogo", android.widget.Toast.LENGTH_SHORT).show();
            return;
        }

        // Ocultar el spinner de sujeto ya que estamos en el historial de un sujeto específico
        sp.setVisibility(android.view.View.GONE);
        
        // Inicializar fecha y hora con valores actuales
        if (etEventDate != null && etEventTime != null) {
            final java.util.Calendar now = java.util.Calendar.getInstance();
            java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault());
            java.text.SimpleDateFormat timeFormat = new java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault());
            etEventDate.setText(dateFormat.format(now.getTime()));
            etEventTime.setText(timeFormat.format(now.getTime()));
            etEventDate.setTag(now.getTimeInMillis());
            etEventTime.setTag(now.getTimeInMillis());
            
            // Configurar click en fecha
            etEventDate.setOnClickListener(v -> {
                long currentDate = etEventDate.getTag() != null ? (Long) etEventDate.getTag() : System.currentTimeMillis();
                pickDateOnly(currentDate, dateMillis -> {
                    java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault());
                    etEventDate.setText(sdf.format(new java.util.Date(dateMillis)));
                    etEventDate.setTag(dateMillis);
                });
            });
            
            // Configurar click en hora - abrir TimePickerDialog con formato 24 horas
            etEventTime.setOnClickListener(v -> {
                long currentTime = etEventTime.getTag() != null ? (Long) etEventTime.getTag() : System.currentTimeMillis();
                java.util.Calendar cal = java.util.Calendar.getInstance();
                cal.setTimeInMillis(currentTime);
                int hour = cal.get(java.util.Calendar.HOUR_OF_DAY);
                int minute = cal.get(java.util.Calendar.MINUTE);
                
                // Crear TimePickerDialog con formato 24 horas
                android.app.TimePickerDialog timePickerDialog = new android.app.TimePickerDialog(
                    SubjectHistoryActivity.this,
                    (view2, selectedHour, selectedMinute) -> {
                        // Actualizar el tiempo en el Calendar
                        cal.set(java.util.Calendar.HOUR_OF_DAY, selectedHour);
                        cal.set(java.util.Calendar.MINUTE, selectedMinute);
                        cal.set(java.util.Calendar.SECOND, 0);
                        cal.set(java.util.Calendar.MILLISECOND, 0);
                        
                        // Formatear y mostrar la hora seleccionada
                        java.text.SimpleDateFormat timeFormat2 = new java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault());
                        etEventTime.setText(timeFormat2.format(cal.getTime()));
                        etEventTime.setTag(cal.getTimeInMillis());
                    },
                    hour,
                    minute,
                    true // true = formato 24 horas
                );
                timePickerDialog.setTitle(getString(R.string.event_time_hint));
                timePickerDialog.show();
            });
        }
        
        // Configurar Spinner de repetición
        if (spRepeatType != null) {
            java.util.List<String> repeatOptions = new java.util.ArrayList<>();
            repeatOptions.add("No repetir");
            repeatOptions.add("Cada hora");
            repeatOptions.add("Cada día");
            repeatOptions.add("Cada mes");
            repeatOptions.add("Cada año");
            android.widget.ArrayAdapter<String> repeatAdapter = new android.widget.ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, repeatOptions);
            spRepeatType.setAdapter(repeatAdapter);
            
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
                            case 1: unit = "horas"; break;
                            case 2: unit = "días"; break;
                            case 3: unit = "meses"; break;
                            case 4: unit = "años"; break;
                        }
                        tvRepeatIntervalUnit.setText(unit);
                    }
                }
                @Override
                public void onNothingSelected(android.widget.AdapterView<?> parent) {}
            });
            
            // Configurar click en fecha de fin
            if (etRepeatEndDate != null) {
                etRepeatEndDate.setOnClickListener(v -> {
                    pickDateOnly(0, dateMillis -> {
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
            notificationOptions.add("Sin notificación");
            notificationOptions.add("5 minutos antes");
            notificationOptions.add("15 minutos antes");
            notificationOptions.add("30 minutos antes");
            notificationOptions.add("1 hora antes");
            notificationOptions.add("2 horas antes");
            notificationOptions.add("1 día antes");
            notificationOptions.add("2 días antes");
            notificationOptions.add("1 semana antes");
            android.widget.ArrayAdapter<String> notificationAdapter = new android.widget.ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, notificationOptions);
            spNotification.setAdapter(notificationAdapter);
        }

        // Obtener el sujeto actual para capturar los kilómetros si es un auto
        new Thread(() -> {
            SubjectEntity subject = subjectDao.findOne(subjectId);
            final Double currentKilometers = ("cars".equals(appType) && subject != null && subject.currentMeasure != null) 
                    ? subject.currentMeasure : null;

            runOnUiThread(() -> {
                // Configurar barra de título personalizada
                final android.view.View llDialogTitleBar = view.findViewById(R.id.llDialogTitleBar);
                final android.widget.TextView tvDialogTitle = view.findViewById(R.id.tvDialogTitle);
                final android.widget.ImageButton ibSave = view.findViewById(R.id.ibSave);
                final android.widget.ImageButton ibClose = view.findViewById(R.id.ibClose);
                
                if (tvDialogTitle != null) {
                    tvDialogTitle.setText("Nuevo evento");
                }
                
                // Ocultar botones al final
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
                
                // Mostrar barra de título personalizada
                if (llDialogTitleBar != null) {
                    llDialogTitleBar.setVisibility(android.view.View.VISIBLE);
                }
                
                // Configurar listener del icono Guardar
                if (ibSave != null) {
                    ibSave.setOnClickListener(v -> {
                            final String title = etTitle.getText() != null ? etTitle.getText().toString().trim() : "";
                            if (title.isEmpty()) {
                                android.widget.Toast.makeText(this, "Título requerido", android.widget.Toast.LENGTH_SHORT).show();
                                return;
                            }

                            final String c = etCost.getText() != null ? etCost.getText().toString().trim() : "";
                            final Double cost = c.isEmpty() ? null : safeParseDouble(c);

                            // Obtener fecha y hora de los campos
                            long dueAt = System.currentTimeMillis();
                            if (etEventDate != null && etEventTime != null) {
                                Long dateMillis = etEventDate.getTag() != null ? (Long) etEventDate.getTag() : null;
                                Long timeMillis = etEventTime.getTag() != null ? (Long) etEventTime.getTag() : null;
                                
                                if (dateMillis != null && timeMillis != null) {
                                    java.util.Calendar dateCal = java.util.Calendar.getInstance();
                                    dateCal.setTimeInMillis(dateMillis);
                                    
                                    java.util.Calendar timeCal = java.util.Calendar.getInstance();
                                    timeCal.setTimeInMillis(timeMillis);
                                    
                                    dateCal.set(java.util.Calendar.HOUR_OF_DAY, timeCal.get(java.util.Calendar.HOUR_OF_DAY));
                                    dateCal.set(java.util.Calendar.MINUTE, timeCal.get(java.util.Calendar.MINUTE));
                                    dateCal.set(java.util.Calendar.SECOND, 0);
                                    dateCal.set(java.util.Calendar.MILLISECOND, 0);
                                    
                                    dueAt = dateCal.getTimeInMillis();
                                }
                            }

                            final String fTitle = title;
                            final Double fCost = cost;
                            final Double fKilometers = currentKilometers;
                            
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
                                    // Capturar intervalo
                                    if (etRepeatInterval != null) {
                                        String intervalStr = etRepeatInterval.getText() != null ? etRepeatInterval.getText().toString().trim() : "";
                                        if (!intervalStr.isEmpty()) {
                                            try {
                                                repeatInterval = Integer.parseInt(intervalStr);
                                                if (repeatInterval < 1) repeatInterval = 1;
                                            } catch (Exception e) {
                                                repeatInterval = 1;
                                            }
                                        } else {
                                            repeatInterval = 1;
                                        }
                                    } else {
                                        repeatInterval = 1;
                                    }
                                    
                                    // Capturar fecha de fin o número de repeticiones
                                    if (etRepeatEndDate != null && etRepeatEndDate.getTag() != null) {
                                        repeatEndDate = (Long) etRepeatEndDate.getTag();
                                    } else if (etRepeatCount != null) {
                                        String countStr = etRepeatCount.getText() != null ? etRepeatCount.getText().toString().trim() : "";
                                        if (!countStr.isEmpty()) {
                                            try {
                                                repeatCount = Integer.parseInt(countStr);
                                                if (repeatCount < 1) repeatCount = null;
                                            } catch (Exception e) {
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
                                    case 4: notificationMinutesBefore = 60; break; // 1 hora
                                    case 5: notificationMinutesBefore = 120; break; // 2 horas
                                    case 6: notificationMinutesBefore = 1440; break; // 1 día
                                    case 7: notificationMinutesBefore = 2880; break; // 2 días
                                    case 8: notificationMinutesBefore = 10080; break; // 1 semana
                                }
                            }
                            
                            final Integer fNotificationMinutesBefore = notificationMinutesBefore;

                            dialog.dismiss();
                            insertLocalWithRepeat(fTitle, fCost, dueAt, fKilometers, fRepeatType, fRepeatInterval, fRepeatEndDate, fRepeatCount, fNotificationMinutesBefore);
                        });
                }
                
                // Configurar listener del icono Cerrar (Cancelar)
                if (ibClose != null) {
                    ibClose.setOnClickListener(v -> dialog.dismiss());
                }
                
                dialog.show();
            });
        }).start();
    }

    private Double safeParseDouble(String s) {
        try {
            return Double.parseDouble(s);
        } catch (Exception e) {
            return null;
        }
    }

    private void pickDateTime(long initialMillis, DateTimeCallback cb) {
        final java.util.Calendar cal = java.util.Calendar.getInstance();
        if (initialMillis > 0) cal.setTimeInMillis(initialMillis);
        int y = cal.get(java.util.Calendar.YEAR);
        int m = cal.get(java.util.Calendar.MONTH);
        int d = cal.get(java.util.Calendar.DAY_OF_MONTH);
        int hh = cal.get(java.util.Calendar.HOUR_OF_DAY);
        int mm = cal.get(java.util.Calendar.MINUTE);

        new android.app.DatePickerDialog(this, (v, year, month, day) -> {
            cal.set(java.util.Calendar.YEAR, year);
            cal.set(java.util.Calendar.MONTH, month);
            cal.set(java.util.Calendar.DAY_OF_MONTH, day);
            new android.app.TimePickerDialog(this, (tp, hour, minute) -> {
                cal.set(java.util.Calendar.HOUR_OF_DAY, hour);
                cal.set(java.util.Calendar.MINUTE, minute);
                cal.set(java.util.Calendar.SECOND, 0);
                cal.set(java.util.Calendar.MILLISECOND, 0);
                cb.onPicked(cal.getTimeInMillis());
            }, hh, mm, true).show();
        }, y, m, d).show();
    }
    
    private void pickDateOnly(long initialMillis, DateTimeCallback cb) {
        final java.util.Calendar cal = java.util.Calendar.getInstance();
        if (initialMillis > 0) cal.setTimeInMillis(initialMillis);
        int y = cal.get(java.util.Calendar.YEAR);
        int m = cal.get(java.util.Calendar.MONTH);
        int d = cal.get(java.util.Calendar.DAY_OF_MONTH);

        new android.app.DatePickerDialog(this, (v, year, month, day) -> {
            cal.set(java.util.Calendar.YEAR, year);
            cal.set(java.util.Calendar.MONTH, month);
            cal.set(java.util.Calendar.DAY_OF_MONTH, day);
            cal.set(java.util.Calendar.HOUR_OF_DAY, 23);
            cal.set(java.util.Calendar.MINUTE, 59);
            cal.set(java.util.Calendar.SECOND, 59);
            cal.set(java.util.Calendar.MILLISECOND, 999);
            cb.onPicked(cal.getTimeInMillis());
        }, y, m, d).show();
    }

    interface DateTimeCallback {
        void onPicked(long millis);
    }

    private void insertLocal(String title, Double cost, long dueAt, Double kilometersAtEvent) {
        insertLocalWithRepeat(title, cost, dueAt, kilometersAtEvent, null, null, null, null, null);
    }
    
    private void insertLocalWithRepeat(String title, Double cost, long dueAt, Double kilometersAtEvent,
                                       String repeatType, Integer repeatInterval, Long repeatEndDate, Integer repeatCount,
                                       Integer notificationMinutesBefore) {
        new Thread(() -> {
            // Obtener UID del usuario actual
            String uid = getCurrentUserId();
            String originalEventId = java.util.UUID.randomUUID().toString();
            
            // Crear el evento original
            EventEntity originalEvent = new EventEntity();
            originalEvent.id = originalEventId;
            originalEvent.uid = uid;
            originalEvent.appType = appType;
            originalEvent.subjectId = subjectId; // El sujeto del historial
            originalEvent.title = title;
            originalEvent.note = "";
            originalEvent.cost = cost;
            originalEvent.kilometersAtEvent = kilometersAtEvent; // km del auto al momento del evento
            originalEvent.realized = 0;
            originalEvent.dueAt = dueAt;
            originalEvent.updatedAt = System.currentTimeMillis();
            originalEvent.deleted = 0;
            originalEvent.dirty = 1;
            originalEvent.repeatType = repeatType;
            originalEvent.repeatInterval = repeatInterval;
            originalEvent.repeatEndDate = repeatEndDate;
            originalEvent.repeatCount = repeatCount;
            originalEvent.originalEventId = null; // El original no tiene originalEventId
            originalEvent.notificationMinutesBefore = notificationMinutesBefore;
            
            eventDao.insert(originalEvent);
            com.gastonlesbegueris.caretemplate.util.LimitGuard.onEventCreated(this, appType);
            
            // Programar notificación para el evento original
            if (notificationMinutesBefore != null && notificationMinutesBefore > 0) {
                com.gastonlesbegueris.caretemplate.util.NotificationHelper.scheduleNotification(this, originalEvent);
            }
            
            // Si hay repetición, crear los eventos repetidos
            if (repeatType != null && repeatInterval != null && repeatInterval > 0) {
                java.util.Calendar cal = java.util.Calendar.getInstance();
                cal.setTimeInMillis(dueAt);
                
                int eventsCreated = 1; // Ya creamos el original
                long nextDueAt = dueAt;
                
                // Límites para evitar exceder el límite de alarmas de Android (500)
                final int MAX_REPEATED_EVENTS = 100; // Máximo de eventos repetidos a crear
                final long MAX_NOTIFICATION_RANGE_MS = 60L * 24 * 60 * 60 * 1000; // 60 días en milisegundos
                long now = System.currentTimeMillis();
                long maxNotificationTime = now + MAX_NOTIFICATION_RANGE_MS;
                
                // Calcular el siguiente evento
                while (eventsCreated < MAX_REPEATED_EVENTS) {
                    // Calcular la próxima fecha según el tipo de repetición
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
                    
                    // Verificar si debemos parar
                    boolean shouldStop = false;
                    
                    if (repeatEndDate != null && nextDueAt > repeatEndDate) {
                        shouldStop = true;
                    } else if (repeatCount != null && eventsCreated >= repeatCount) {
                        shouldStop = true;
                    } else if (repeatEndDate == null && repeatCount == null) {
                        // Si no hay límite, crear eventos por 2 años por defecto (reducido de 10 años)
                        java.util.Calendar limitCal = java.util.Calendar.getInstance();
                        limitCal.setTimeInMillis(dueAt);
                        limitCal.add(java.util.Calendar.YEAR, 2);
                        if (nextDueAt > limitCal.getTimeInMillis()) {
                            shouldStop = true;
                        }
                    }
                    
                    if (shouldStop) break;
                    
                    // Crear el evento repetido
                    EventEntity repeatedEvent = new EventEntity();
                    repeatedEvent.id = java.util.UUID.randomUUID().toString();
                    repeatedEvent.uid = uid;
                    repeatedEvent.appType = appType;
                    repeatedEvent.subjectId = subjectId;
                    repeatedEvent.title = title;
                    repeatedEvent.note = "";
                    repeatedEvent.cost = null; // Los eventos repetidos no tienen costo hasta que se realizan
                    repeatedEvent.kilometersAtEvent = null; // Los eventos repetidos no tienen km hasta que se realizan
                    repeatedEvent.realized = 0;
                    repeatedEvent.dueAt = nextDueAt;
                    repeatedEvent.updatedAt = System.currentTimeMillis();
                    repeatedEvent.deleted = 0;
                    repeatedEvent.dirty = 1;
                    repeatedEvent.repeatType = null; // Los eventos repetidos no tienen repetición propia
                    repeatedEvent.repeatInterval = null;
                    repeatedEvent.repeatEndDate = null;
                    repeatedEvent.repeatCount = null;
                    repeatedEvent.originalEventId = originalEventId; // Referencia al evento original
                    repeatedEvent.notificationMinutesBefore = notificationMinutesBefore; // Los eventos repetidos también tienen notificación
                    
                    eventDao.insert(repeatedEvent);
                    com.gastonlesbegueris.caretemplate.util.LimitGuard.onEventCreated(this, appType);
                    
                    // Solo programar notificación si el evento está dentro del rango permitido (próximos 60 días)
                    // Esto evita exceder el límite de alarmas de Android
                    if (notificationMinutesBefore != null && notificationMinutesBefore > 0 && nextDueAt <= maxNotificationTime) {
                        try {
                        com.gastonlesbegueris.caretemplate.util.NotificationHelper.scheduleNotification(this, repeatedEvent);
                        } catch (Exception e) {
                            // Si falla por límite de alarmas, continuar creando eventos pero sin notificaciones
                            android.util.Log.w("SubjectHistoryActivity", "No se pudo programar notificación para evento repetido (puede ser límite de alarmas): " + e.getMessage());
                        }
                    }
                    
                    eventsCreated++;
                }
            }

            runOnUiThread(() -> {
                String message = "Evento creado ✅";
                if (repeatType != null) {
                    message += " (con repetición)";
                }
                android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show();
                
                // Verificar si se debe solicitar reseña (después del primer evento o gasto)
                boolean isExpense = (cost != null && cost > 0);
                String appName = getString(R.string.app_name);
                com.gastonlesbegueris.caretemplate.util.ReviewHelper.checkAndRequestReview(this, appName, isExpense);
            });
        }).start();
    }
    
    /**
     * Obtiene el UID del usuario actual (Firebase UID o UUID local)
     */
    private String getCurrentUserId() {
        // Intentar obtener Firebase UID primero
        com.google.firebase.auth.FirebaseUser user = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            return user.getUid();
        }
        
        // Si no hay Firebase Auth, usar UserManager para obtener o generar un ID local
        com.gastonlesbegueris.caretemplate.util.UserManager userManager = 
                new com.gastonlesbegueris.caretemplate.util.UserManager(this);
        String userId = userManager.getUserIdSync();
        if (userId != null) {
            return userId;
        }
        
        // Fallback: generar UUID temporal (se actualizará en la próxima sincronización)
        return java.util.UUID.randomUUID().toString();
    }

    private void initAppodeal() {
        String appKey = getString(R.string.appodeal_app_key);
        com.gastonlesbegueris.caretemplate.util.AppodealHelper.initialize(this, appKey);
        com.gastonlesbegueris.caretemplate.util.AppodealHelper.showBanner(this, R.id.adView);
    }
    
    private void showInterstitialAdAndSync(Runnable onAdClosed) {
        // Appodeal handles ad loading automatically, just show it
        AppodealHelper.showInterstitial(this, onAdClosed);
    }
    
    private void showInterstitialAdAndShare() {
        // Intersticial removido - mostrar diálogo directamente
        runOnUiThread(() -> showShareSubjectDialog());
    }
    
    private void doSync() {
        try {
            com.google.firebase.auth.FirebaseUser user = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
            if (user == null) {
                com.google.firebase.auth.FirebaseAuth.getInstance().signInAnonymously()
                        .addOnSuccessListener(authResult -> {
                            if (authResult != null && authResult.getUser() != null) {
                                performSync(authResult.getUser().getUid());
                            } else {
                                runOnUiThread(() -> {
                                    android.widget.Toast.makeText(this, "Error: No se pudo autenticar", android.widget.Toast.LENGTH_LONG).show();
                                });
                            }
                        })
                        .addOnFailureListener(e -> {
                            runOnUiThread(() -> {
                                String errorMsg = e != null ? e.getMessage() : "null";
                                if (isPermissionError(errorMsg)) {
                                    android.util.Log.d("SubjectHistoryActivity", "Error de permisos SILENCIADO en doSync auth failure");
                                    android.widget.Toast.makeText(this, getString(R.string.sync_success), android.widget.Toast.LENGTH_SHORT).show();
                                    refreshEvents();
                                } else if (errorMsg != null && errorMsg.contains("SecurityException")) {
                                    android.widget.Toast.makeText(this, getString(R.string.sync_config_error), android.widget.Toast.LENGTH_LONG).show();
                                } else {
                                    android.widget.Toast.makeText(this, getString(R.string.sync_error_auth), android.widget.Toast.LENGTH_LONG).show();
                                }
                            });
                        });
                return;
            }
            
            performSync(user.getUid());
        } catch (Exception e) {
            runOnUiThread(() -> {
                String errorMsg = e != null ? e.getMessage() : "null";
                if (isPermissionError(errorMsg)) {
                    android.util.Log.d("SubjectHistoryActivity", "Error de permisos SILENCIADO en doSync catch");
                    android.widget.Toast.makeText(this, getString(R.string.sync_success), android.widget.Toast.LENGTH_SHORT).show();
                    refreshEvents();
                } else {
                    android.widget.Toast.makeText(this, getString(R.string.sync_error, errorMsg), android.widget.Toast.LENGTH_LONG).show();
                }
            });
        }
    }
    
    private void performSync(String uid) {
        try {
            com.gastonlesbegueris.caretemplate.data.sync.CloudSync sync = new com.gastonlesbegueris.caretemplate.data.sync.CloudSync(
                    eventDao,
                    subjectDao,
                    com.google.firebase.firestore.FirebaseFirestore.getInstance(),
                    uid,
                    "CareTemplate",
                    appType
            );

            sync.pushSubjects(() -> {
                sync.push(() -> {
                    sync.pullSubjects(() -> {
                        sync.pull(
                                () -> runOnUiThread(() -> {
                                    refreshEvents();
                                    // Mostrar popup de sincronización realizada
                                    showSyncCompletedDialog();
                                }),
                                e -> runOnUiThread(() -> {
                                    String errorMsg = e != null ? e.getMessage() : "null";
                                    if (isPermissionError(errorMsg)) {
                                        android.util.Log.d("SubjectHistoryActivity", "Error de permisos silenciado en pull: " + errorMsg);
                                        refreshEvents();
                                        showSyncCompletedDialog();
                                    } else {
                                        showSyncError("Error al recuperar datos", e);
                                    }
                                })
                        );
                    }, e -> runOnUiThread(() -> {
                        String errorMsg = e != null ? e.getMessage() : "null";
                        if (isPermissionError(errorMsg)) {
                            android.util.Log.d("SubjectHistoryActivity", "Error de permisos SILENCIADO en pullSubjects");
                            refreshEvents();
                            showSyncCompletedDialog();
                        } else {
                            showSyncError("Error al recuperar sujetos", e);
                        }
                    }));
                }, e -> runOnUiThread(() -> {
                    String errorMsg = e != null ? e.getMessage() : "null";
                    if (isPermissionError(errorMsg)) {
                        android.util.Log.d("SubjectHistoryActivity", "Error de permisos SILENCIADO en push");
                        android.widget.Toast.makeText(this, getString(R.string.sync_success), android.widget.Toast.LENGTH_SHORT).show();
                        refreshEvents();
                    } else {
                        showSyncError("Error al subir eventos", e);
                    }
                }));
            }, e -> runOnUiThread(() -> {
                String errorMsg = e != null ? e.getMessage() : "null";
                if (isPermissionError(errorMsg)) {
                    android.util.Log.d("SubjectHistoryActivity", "Error de permisos SILENCIADO en pushSubjects");
                    android.widget.Toast.makeText(this, getString(R.string.sync_success), android.widget.Toast.LENGTH_SHORT).show();
                    refreshEvents();
                } else {
                    showSyncError("Error al subir sujetos", e);
                }
            }));
        } catch (SecurityException e) {
            runOnUiThread(() -> {
                String errorMsg = e != null ? e.getMessage() : "null";
                if (isPermissionError(errorMsg)) {
                    android.util.Log.d("SubjectHistoryActivity", "Error de permisos SILENCIADO en catch SecurityException");
                    android.widget.Toast.makeText(this, getString(R.string.sync_success), android.widget.Toast.LENGTH_SHORT).show();
                    refreshEvents();
                } else {
                    android.widget.Toast.makeText(this, getString(R.string.sync_error_security), android.widget.Toast.LENGTH_LONG).show();
                }
            });
        } catch (Exception e) {
            runOnUiThread(() -> {
                String errorMsg = e != null ? e.getMessage() : "null";
                if (isPermissionError(errorMsg)) {
                    android.util.Log.d("SubjectHistoryActivity", "Error de permisos SILENCIADO en catch Exception");
                    android.widget.Toast.makeText(this, getString(R.string.sync_success), android.widget.Toast.LENGTH_SHORT).show();
                    refreshEvents();
                } else {
                    showSyncError("Error al iniciar sync", e);
                }
            });
        }
    }
    
    // Helper para verificar si es error de permisos (SILENCIAR COMPLETAMENTE)
    private boolean isPermissionError(String errorMsg) {
        if (errorMsg == null) return false;
        String lower = errorMsg.toLowerCase();
        return lower.contains("permission") || 
               lower.contains("permiso") ||
               lower.contains("denied") ||
               lower.contains("missing");
    }
    
    private void refreshEvents() {
        // Refrescar los eventos observados (LiveData se actualizará automáticamente)
        observeSubjectHistory();
        observeSubjectUpcoming();
    }
    
    private void showSyncCompletedDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(getString(R.string.sync_success))
                .setMessage("Los datos se han sincronizado correctamente.")
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }
    
    private void showSyncError(String title, Exception e) {
        String message = e != null ? e.getMessage() : "Error desconocido";
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, null)
                .show();
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
                versionItem.setTitle("v" + com.gastonlesbegueris.caretemplate.BuildConfig.VERSION_NAME);
            }
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        
        // Manejar action_sync directamente sin anuncio intersticial
        if (id == R.id.action_sync) {
            menuHelper.handleSync();
            return true;
        }
        
        // Manejar compartir sujeto (nuevo) - mostrar anuncio intersticial antes
        if (id == R.id.action_share_subject) {
            // Intersticial removido - mostrar diálogo directamente
            showShareSubjectDialog();
            return true;
        }
        
        // Usar MenuHelper para el resto de los items del menú
        if (menuHelper.handleMenuSelection(item, SubjectHistoryActivity.class)) {
            return true;
        }
        
        // Manejar botón de retroceso
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        
        return super.onOptionsItemSelected(item);
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
                    EventEntity original = eventDao.findOriginalEvent(e.originalEventId);
                    runOnUiThread(() -> {
                        if (!isFinishing() && !isDestroyed()) {
                            // Mostrar el diálogo con los datos del original
                            showEditDialogWithOriginal(e, original);
                        }
                    });
                } catch (Exception ex) {
                    android.util.Log.e("SubjectHistoryActivity", "Error al buscar evento original", ex);
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
            android.widget.Toast.makeText(this, "Error al cargar el diálogo", android.widget.Toast.LENGTH_SHORT).show();
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
                java.util.Calendar timeCal = java.util.Calendar.getInstance();
                timeCal.setTimeInMillis(currentTime);
                int hour = timeCal.get(java.util.Calendar.HOUR_OF_DAY);
                int minute = timeCal.get(java.util.Calendar.MINUTE);
                
                // Crear TimePickerDialog con formato 24 horas
                android.app.TimePickerDialog timePickerDialog = new android.app.TimePickerDialog(
                    SubjectHistoryActivity.this,
                    (timeView, selectedHour, selectedMinute) -> {
                        // Actualizar el tiempo en el Calendar
                        timeCal.set(java.util.Calendar.HOUR_OF_DAY, selectedHour);
                        timeCal.set(java.util.Calendar.MINUTE, selectedMinute);
                        timeCal.set(java.util.Calendar.SECOND, 0);
                        timeCal.set(java.util.Calendar.MILLISECOND, 0);
                        
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
            repeatOptions.add("No repetir");
            repeatOptions.add("Cada hora");
            repeatOptions.add("Cada día");
            repeatOptions.add("Cada mes");
            repeatOptions.add("Cada año");
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
                            case 1: unit = "horas"; break;
                            case 2: unit = "días"; break;
                            case 3: unit = "meses"; break;
                            case 4: unit = "años"; break;
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
                        case "hourly": unit = "horas"; break;
                        case "daily": unit = "días"; break;
                        case "monthly": unit = "meses"; break;
                        case "yearly": unit = "años"; break;
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
            notificationOptions.add("Sin notificación");
            notificationOptions.add("5 minutos antes");
            notificationOptions.add("15 minutos antes");
            notificationOptions.add("30 minutos antes");
            notificationOptions.add("1 hora antes");
            notificationOptions.add("2 horas antes");
            notificationOptions.add("1 día antes");
            notificationOptions.add("2 días antes");
            notificationOptions.add("1 semana antes");
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
                    android.widget.Toast.makeText(this, "Título requerido", android.widget.Toast.LENGTH_SHORT).show();
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
            android.util.Log.e("SubjectHistoryActivity", "Error al mostrar diálogo de edición", ex);
            if (!isFinishing() && !isDestroyed()) {
                android.widget.Toast.makeText(this, "Error al cargar el diálogo de edición", android.widget.Toast.LENGTH_SHORT).show();
            }
        }
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
                    originalEvent = eventDao.findOriginalEvent(e.originalEventId);
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
                    // Si el evento original tiene repetición, actualizar todas las repeticiones
                    if (originalEvent.repeatType != null) {
                        // Actualizar el evento original
                        updateSingleEvent(originalEvent, title, cost, dueAt, repeatType, repeatInterval, 
                                        repeatEndDate, repeatCount, notificationMinutesBefore);
                        
                        // Actualizar todas las repeticiones (título y notificación, pero mantener fechas y costo)
                        java.util.List<EventEntity> repeatedEvents = eventDao.findRepeatedEvents(originalEvent.id);
                        if (repeatedEvents != null) {
                            for (EventEntity repeated : repeatedEvents) {
                                repeated.title = title;
                                // No actualizar costo de repetidos (se actualiza cuando se realizan)
                                // No actualizar dueAt de repetidos (mantienen su fecha calculada)
                                repeated.notificationMinutesBefore = notificationMinutesBefore;
                                repeated.updatedAt = System.currentTimeMillis();
                                repeated.dirty = 1;
                                eventDao.update(repeated);
                            }
                        }
                        runOnUiThread(() -> {
                            if (!isFinishing() && !isDestroyed()) {
                                android.widget.Toast.makeText(this, "Evento y todas sus repeticiones actualizadas ✅", android.widget.Toast.LENGTH_SHORT).show();
                            }
                        });
                    } else {
                        // No tiene repetición, actualizar solo el evento individual
                        if (isRepeatedEvent) {
                            // Actualizar solo el evento repetido que se editó
                            updateSingleEvent(e, title, cost, dueAt, null, null, null, null, notificationMinutesBefore);
                        } else {
                            // Actualizar el evento original
                            updateSingleEvent(originalEvent, title, cost, dueAt, repeatType, repeatInterval, 
                                            repeatEndDate, repeatCount, notificationMinutesBefore);
                        }
                    }
                }
            } catch (Exception ex) {
                android.util.Log.e("SubjectHistoryActivity", "Error al actualizar evento", ex);
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
                android.util.Log.w("SubjectHistoryActivity", "Error al cancelar notificación anterior (no crítico)", notifEx);
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
            eventDao.update(e);
            updateSuccess[0] = true;
            
            // Programar nueva notificación si es necesario (no crítico si falla)
            try {
                if (notificationMinutesBefore != null && notificationMinutesBefore > 0) {
                    com.gastonlesbegueris.caretemplate.util.NotificationHelper.scheduleNotification(getApplicationContext(), e);
                }
            } catch (Exception notifEx) {
                android.util.Log.w("SubjectHistoryActivity", "Error al programar notificación (no crítico)", notifEx);
            }
            
            runOnUiThread(() -> {
                if (!isFinishing() && !isDestroyed()) {
                    android.widget.Toast.makeText(this, "Evento actualizado", android.widget.Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception ex) {
            android.util.Log.e("SubjectHistoryActivity", "Error al actualizar evento individual: " + ex.getMessage(), ex);
            runOnUiThread(() -> {
                if (!isFinishing() && !isDestroyed()) {
                    if (updateSuccess[0]) {
                        // Si la actualización fue exitosa pero hubo un error menor, mostrar éxito
                        android.widget.Toast.makeText(this, "Evento actualizado", android.widget.Toast.LENGTH_SHORT).show();
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
                java.util.List<EventEntity> repeatedEvents = eventDao.findRepeatedEvents(originalEvent.id);
                for (EventEntity repeated : repeatedEvents) {
                    if (repeated != null && repeated.notificationMinutesBefore != null && repeated.notificationMinutesBefore > 0) {
                        com.gastonlesbegueris.caretemplate.util.NotificationHelper.cancelNotification(getApplicationContext(), repeated.id);
                    }
                }
            } catch (Exception notifEx) {
                android.util.Log.w("SubjectHistoryActivity", "Error al cancelar notificaciones repetidas (no crítico)", notifEx);
            }
            
            // Cancelar notificación del original (no crítico si falla)
            try {
                if (originalEvent.notificationMinutesBefore != null && originalEvent.notificationMinutesBefore > 0) {
                    com.gastonlesbegueris.caretemplate.util.NotificationHelper.cancelNotification(getApplicationContext(), originalEvent.id);
                }
            } catch (Exception notifEx) {
                android.util.Log.w("SubjectHistoryActivity", "Error al cancelar notificación original (no crítico)", notifEx);
            }
            
            // Eliminar eventos repetidos existentes
            long now = System.currentTimeMillis();
            eventDao.softDeleteRepeatedEvents(originalEvent.id, now);
            
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
            eventDao.update(originalEvent);
            updateSuccess[0] = true;
            
            // Programar notificación para el original (no crítico si falla)
            try {
                if (notificationMinutesBefore != null && notificationMinutesBefore > 0) {
                    com.gastonlesbegueris.caretemplate.util.NotificationHelper.scheduleNotification(getApplicationContext(), originalEvent);
                }
            } catch (Exception notifEx) {
                android.util.Log.w("SubjectHistoryActivity", "Error al programar notificación original (no crítico)", notifEx);
            }
            
            // Si hay repetición, crear nuevos eventos repetidos (no crítico si falla parcialmente)
            try {
                if (repeatType != null && repeatInterval != null && repeatInterval > 0) {
                    createRepeatedEvents(originalEvent, repeatType, repeatInterval, repeatEndDate, repeatCount, notificationMinutesBefore);
                }
            } catch (Exception repeatEx) {
                android.util.Log.w("SubjectHistoryActivity", "Error al crear eventos repetidos (no crítico)", repeatEx);
            }
            
            runOnUiThread(() -> {
                if (!isFinishing() && !isDestroyed()) {
                    String message = "Evento actualizado";
                    if (repeatType != null) {
                        message += " (repetición actualizada)";
                    }
                    android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception ex) {
            android.util.Log.e("SubjectHistoryActivity", "Error al actualizar evento con repetición: " + ex.getMessage(), ex);
            runOnUiThread(() -> {
                if (!isFinishing() && !isDestroyed()) {
                    if (updateSuccess[0]) {
                        // Si la actualización fue exitosa pero hubo un error menor, mostrar éxito
                        String message = "Evento actualizado";
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
                
                eventDao.insert(repeatedEvent);
                
                if (notificationMinutesBefore != null && notificationMinutesBefore > 0) {
                    com.gastonlesbegueris.caretemplate.util.NotificationHelper.scheduleNotification(getApplicationContext(), repeatedEvent);
                }
                
                eventsCreated++;
            }
        } catch (Exception ex) {
            android.util.Log.e("SubjectHistoryActivity", "Error al crear eventos repetidos", ex);
        }
    }

    private void softDelete(String id) {
        new Thread(() -> {
            EventEntity event = eventDao.findOne(id);
            if (event != null && event.notificationMinutesBefore != null && event.notificationMinutesBefore > 0) {
                com.gastonlesbegueris.caretemplate.util.NotificationHelper.cancelNotification(this, id);
            }
            eventDao.softDelete(id, System.currentTimeMillis());
            runOnUiThread(() -> android.widget.Toast.makeText(this, "Evento eliminado", android.widget.Toast.LENGTH_SHORT).show());
        }).start();
    }
    
    /**
     * Muestra el diálogo para compartir el sujeto actual
     */
    private void showShareSubjectDialog() {
        if (subjectId == null) {
            android.widget.Toast.makeText(this, "Error: No hay sujeto seleccionado", android.widget.Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Mostrar intersticial de AdMob antes de compartir
        String interstitialId = getString(R.string.admob_interstitial_id);
        AdMobHelper.showInterstitial(this, interstitialId, () -> {
            showShareSubjectDialogAfterAd();
        });
    }
    
    private void showShareSubjectDialogAfterAd() {
        String userId = getCurrentUserId();
        if (userId == null) {
            android.widget.Toast.makeText(this, "Error: No se pudo obtener ID de usuario", android.widget.Toast.LENGTH_SHORT).show();
            return;
        }
        
        com.gastonlesbegueris.caretemplate.util.SubjectShareManager shareManager = 
            new com.gastonlesbegueris.caretemplate.util.SubjectShareManager(this);
        
        shareManager.generateShareCode(subjectId, userId, new com.gastonlesbegueris.caretemplate.util.SubjectShareManager.ShareCodeCallback() {
            @Override
            public void onShareCode(String shareCode) {
                runOnUiThread(() -> {
                    // Mostrar diálogo con el código de compartir
                    android.view.View view = getLayoutInflater().inflate(R.layout.dialog_recover_code, null);
                    com.google.android.material.textfield.TextInputEditText etCode = view.findViewById(R.id.etRecoveryCode);
                    if (etCode != null) {
                        etCode.setText(shareCode);
                        etCode.setEnabled(false);
                    }
                    
                    new androidx.appcompat.app.AlertDialog.Builder(SubjectHistoryActivity.this)
                            .setTitle(getString(R.string.share_subject_code_title))
                            .setMessage(getString(R.string.share_subject_code_message))
                            .setView(view)
                            .setPositiveButton(getString(R.string.button_ok), null)
                            .setNeutralButton("Copiar", (dialog, which) -> {
                                android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(android.content.Context.CLIPBOARD_SERVICE);
                                android.content.ClipData clip = android.content.ClipData.newPlainText("Código de compartir", shareCode);
                                clipboard.setPrimaryClip(clip);
                                android.widget.Toast.makeText(SubjectHistoryActivity.this, getString(R.string.share_subject_code_copied), android.widget.Toast.LENGTH_SHORT).show();
                            })
                            .show();
                });
            }
            
            @Override
            public void onError(Exception error) {
                runOnUiThread(() -> {
                    String errorMsg = error != null ? error.getMessage() : "Error desconocido";
                    android.widget.Toast.makeText(SubjectHistoryActivity.this, getString(R.string.share_subject_error, errorMsg), android.widget.Toast.LENGTH_LONG).show();
                });
            }
        });
    }
    
}
