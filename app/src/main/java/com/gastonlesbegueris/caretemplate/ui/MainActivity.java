package com.gastonlesbegueris.caretemplate.ui;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

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
import com.gastonlesbegueris.caretemplate.data.sync.CloudSync;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private AppDb db;
    private EventDao eventDao;
    private SubjectDao subjectDao;
    private EventAdapter adapter;


    //private LocalEventAdapter adapter;
    private String appType;              // "pets" | "cars" | "family" | "house"
    private String currentSubjectId;     // sujeto seleccionado (opcional)

    private MenuItem menuItemSync;
    private boolean fabMenuOpen = false;

    @Override
    protected void onCreate(Bundle s) {
        super.onCreate(s);
        setContentView(R.layout.activity_main);

        // 1) appType desde recursos del flavor
        appType = getString(R.string.app_type);

        // 2) DB/DAOs ANTES de usarlos
        db = AppDb.get(this);
        eventDao = db.eventDao();
        subjectDao = db.subjectDao();

        // 3) Toolbar
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_header_flavor);
        toolbar.setTitle(getString(R.string.app_name));
        toolbar.setSubtitle(null);

        // 4) Lista + Adapter
        setupEventsList();               // crea adapter y setea RecyclerView

        // 5) Observadores
        observeLocal();                  // observa eventos y refresca UI
        observeSubjectsForAdapter();     // alimenta el mapa sujetoId->nombre al adapter

        // 6) Restaurar sujeto seleccionado (si había)
        currentSubjectId = getSharedPreferences("prefs", MODE_PRIVATE)
                .getString("currentSubjectId_" + appType, null);

        // 7) Redirigir a Sujetos si es primera vez / no hay sujetos
        new Thread(() -> {
            int count = subjectDao.countForApp(appType);
            boolean firstRunDone = getSharedPreferences("prefs", MODE_PRIVATE)
                    .getBoolean("first_run_done_" + appType, false);

            if (count == 0 && !firstRunDone) {
                runOnUiThread(() -> {
                    getSharedPreferences("prefs", MODE_PRIVATE)
                            .edit().putBoolean("first_run_done_" + appType, true).apply();
                    startActivity(new android.content.Intent(this, SubjectListActivity.class));
                    Toast.makeText(this, "Primero creá un sujeto 🐶🚗👨‍👩‍👧🏠", Toast.LENGTH_LONG).show();
                });
            }
        }).start();

        // 8) FAB speed-dial
        initFabSpeedDial();
    }

    // ===== Toolbar / Menú =====
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        menuItemSync = menu.findItem(R.id.action_sync);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_agenda) {
            startActivity(new android.content.Intent(this, AgendaMonthActivity.class));
            return true;

    } else if (id == R.id.action_sync) {
            startSyncIconAnimation();
            if (FirebaseAuth.getInstance().getCurrentUser() == null) {
                FirebaseAuth.getInstance().signInAnonymously()
                        .addOnSuccessListener(a -> doSync())
                        .addOnFailureListener(e -> {
                            stopSyncIconAnimation();
                            Toast.makeText(this, "Auth error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        });
            } else {
                doSync();
            }
            return true;
        } else if (id == R.id.action_subjects) {
            startActivity(new android.content.Intent(this, SubjectListActivity.class));
            return true;
        }
        else if (id == R.id.action_expenses) {
            startActivity(new android.content.Intent(this, ExpensesActivity.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void startSyncIconAnimation() {
        if (menuItemSync == null) return;
        android.widget.ImageView iv = new android.widget.ImageView(this);
        iv.setImageResource(R.drawable.ic_sync);
        int pad = (int) (8 * getResources().getDisplayMetrics().density);
        iv.setPadding(pad, pad, pad, pad);
        android.view.animation.Animation rotate =
                android.view.animation.AnimationUtils.loadAnimation(this, R.anim.rotate_indefinite);
        iv.startAnimation(rotate);
        menuItemSync.setActionView(iv);
        menuItemSync.setEnabled(false);
    }

    private void stopSyncIconAnimation() {
        if (menuItemSync == null) return;
        View v = menuItemSync.getActionView();
        if (v != null) v.clearAnimation();
        menuItemSync.setActionView(null);
        menuItemSync.setEnabled(true);
    }

    // ===== Sync Cloud <-> Local =====
    private void doSync() {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        CloudSync sync = new CloudSync(
                eventDao,
                subjectDao,
                FirebaseFirestore.getInstance(),
                uid,
                "CareTemplate",   // nombre lógico de la app en Firestore
                appType
        );

        sync.pushSubjects(() -> {
            sync.push(() -> {
                runOnUiThread(() -> Toast.makeText(this, "Push OK ✅", Toast.LENGTH_SHORT).show());
                sync.pullSubjects(() -> {
                    sync.pull(
                            () -> runOnUiThread(() -> {
                                Toast.makeText(this, "Pull OK ✅", Toast.LENGTH_SHORT).show();
                                stopSyncIconAnimation();
                                refreshHeader();
                            }),
                            e -> runOnUiThread(() -> {
                                Toast.makeText(this, "Pull error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                stopSyncIconAnimation();
                            })
                    );
                }, e -> runOnUiThread(() -> {
                    Toast.makeText(this, "Pull subjects error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    stopSyncIconAnimation();
                }));
            }, e -> runOnUiThread(() -> {
                Toast.makeText(this, "Push events error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                stopSyncIconAnimation();
            }));
        }, e -> runOnUiThread(() -> {
            Toast.makeText(this, "Push subjects error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            stopSyncIconAnimation();
        }));
    }

    // ===== Lista de eventos =====
    private void setupEventsList() {
        adapter = new EventAdapter(new EventAdapter.OnEventClick() {
            @Override public void onEdit(com.gastonlesbegueris.caretemplate.data.local.EventEntity e) { showEditDialog(e); }
            @Override public void onDelete(com.gastonlesbegueris.caretemplate.data.local.EventEntity e) { softDelete(e.id); }

            @Override public void onToggleRealized(com.gastonlesbegueris.caretemplate.data.local.EventEntity e, boolean realized) {
                if (realized) {
                    if (e.cost == null) {
                        // pedir costo si falta
                        askCostThenRealize(e);
                    } else {
                        setRealized(e.id, true, null);
                    }
                } else {
                    setRealized(e.id, false, null);
                }
            }
        });
        RecyclerView rv = findViewById(R.id.rvEvents);
        rv.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this));
        rv.setAdapter(adapter);
    }

    private void askCostThenRealize(com.gastonlesbegueris.caretemplate.data.local.EventEntity e) {
        final android.widget.EditText et = new android.widget.EditText(this);
        et.setHint("Costo (opcional)");
        et.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Marcar como realizado")
                .setMessage("Podés guardar el costo ahora.")
                .setView(et)
                .setPositiveButton("Guardar", (d,w) -> {
                    Double cost = null;
                    String t = et.getText().toString().trim();
                    try { if (!t.isEmpty()) cost = Double.parseDouble(t); } catch (Exception ignore) {}
                    setRealized(e.id, true, cost);
                })
                .setNegativeButton("Solo marcar", (d,w) -> setRealized(e.id, true, null))
                .setNeutralButton("Cancelar", null)
                .show();
    }

    private void setRealized(String id, boolean realized, Double costOrNull) {
        new Thread(() -> {
            long now = System.currentTimeMillis();
            if (realized) {
                if (costOrNull != null) eventDao.setCost(id, costOrNull, now);
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

    private void observeLocal() {
        eventDao.observeActive(appType).observe(this, new Observer<List<EventEntity>>() {
            @Override public void onChanged(List<EventEntity> events) {
                adapter.submit(events);
                findViewById(R.id.emptyState)
                        .setVisibility((events == null || events.isEmpty()) ? View.VISIBLE : View.GONE);
                refreshHeader();
            }
        });
    }

    // 👇 Esto va afuera, no dentro de observeLocal()
    private void observeSubjectsForAdapter() {
        subjectDao.observeActive(appType).observe(this, subjects -> {
            java.util.Map<String, String> map = new java.util.HashMap<>();
            if (subjects != null) {
                for (com.gastonlesbegueris.caretemplate.data.local.SubjectEntity s : subjects) {
                    map.put(s.id, s.name);
                }
            }
            adapter.setSubjectsMap(map);
        });
    }
    // ===== Header dinámico =====
    private void refreshHeader() {
        new Thread(() -> {
            SubjectEntity s = (currentSubjectId == null) ? null : subjectDao.findOne(currentSubjectId);
            EventEntity next = (currentSubjectId == null) ? null : eventDao.nextEvent(appType, currentSubjectId, System.currentTimeMillis());

            runOnUiThread(() -> {
                MaterialToolbar toolbar = findViewById(R.id.toolbar);
                if (toolbar == null) return;

                if (s != null) {
                    String m1;
                    if ("cars".equals(appType) || "house".equals(appType)) {
                        m1 = (s.currentMeasure == null ? "-" : (Math.round(s.currentMeasure) + " km"));
                    } else {
                        m1 = "Edad: " + formatAge(s.birthDate);
                        if (s.currentMeasure != null) m1 += " · Peso: " + s.currentMeasure + " kg";
                    }
                    String m2 = (next == null) ? "Próximo: —"
                            : "Próximo: " + new java.text.SimpleDateFormat("dd/MM HH:mm")
                            .format(new java.util.Date(next.dueAt));

                    toolbar.setTitle(getString(R.string.app_name));
                    toolbar.setSubtitle(m1 + "   |   " + m2);

                    int iconRes = getIconResForSubject(s.iconKey);
                    toolbar.setNavigationIcon(iconRes);
                    String hex = (s.colorHex == null || s.colorHex.isEmpty()) ? "#03DAC5" : s.colorHex;
                    try {
                        if (toolbar.getNavigationIcon() != null) {
                            toolbar.getNavigationIcon().setTint(android.graphics.Color.parseColor(hex));
                        }
                    } catch (Exception ignore) {}
                } else {
                    toolbar.setTitle(getString(R.string.app_name));
                    toolbar.setSubtitle(null);
                    toolbar.setNavigationIcon(R.drawable.ic_header_flavor);
                }
            });
        }).start();
    }

    private int getIconResForSubject(String key) {
        if (key == null) return R.drawable.ic_line_user;
        switch (key) {
            case "cat":   return R.drawable.ic_line_cat;
            case "dog":   return R.drawable.ic_line_dog;
            case "car":   return R.drawable.ic_line_car;
            case "house": return R.drawable.ic_line_house;
            case "user":  return R.drawable.ic_line_user;
            default:      return R.drawable.ic_line_user;
        }
    }

    private String formatAge(Long birthDate) {
        if (birthDate == null) return "-";
        java.util.Calendar b = java.util.Calendar.getInstance();
        b.setTimeInMillis(birthDate);
        java.util.Calendar now = java.util.Calendar.getInstance();
        int years = now.get(java.util.Calendar.YEAR) - b.get(java.util.Calendar.YEAR);
        int months = now.get(java.util.Calendar.MONTH) - b.get(java.util.Calendar.MONTH);
        if (months < 0) { years--; months += 12; }
        return years > 0 ? years + "a " + months + "m" : months + "m";
    }

    // ===== FAB Speed Dial =====
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
            showQuickAddSubjectDialog();
        });
        fabEvent.setOnClickListener(v -> {
            closeFabMenu();
            showAddEventDialog();
        });

        RecyclerView rv = findViewById(R.id.rvEvents);
        rv.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                if (fabMenuOpen && Math.abs(dy) > 6) closeFabMenu();
            }
        });
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

    // ===== Crear / Editar / Borrar eventos =====
    private void showAddEventDialog() {
        final android.view.View view = getLayoutInflater().inflate(R.layout.dialog_add_event, null);
        final android.widget.EditText etTitle = view.findViewById(R.id.etTitle);
        final android.widget.EditText etCost  = view.findViewById(R.id.etCost);
        final android.widget.Spinner sp       = view.findViewById(R.id.spSubject);

        // cargar sujetos en background
        new Thread(() -> {
            final java.util.List<SubjectEntity> loaded = subjectDao.listActiveNow(appType);
            runOnUiThread(() -> {
                final java.util.List<SubjectEntity> subjects =
                        (loaded == null) ? java.util.Collections.emptyList() : new java.util.ArrayList<>(loaded);

                java.util.List<String> names = new java.util.ArrayList<>();
                for (SubjectEntity s : subjects) names.add(s.name);
                android.widget.ArrayAdapter<String> ad =
                        new android.widget.ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, names);
                sp.setAdapter(ad);

                if (currentSubjectId != null && !subjects.isEmpty()) {
                    int idx = -1;
                    for (int i = 0; i < subjects.size(); i++) {
                        if (currentSubjectId.equals(subjects.get(i).id)) { idx = i; break; }
                    }
                    if (idx >= 0) sp.setSelection(idx);
                }

                new androidx.appcompat.app.AlertDialog.Builder(this)
                        .setTitle("Nuevo evento")
                        .setView(view)
                        .setPositiveButton("Elegir fecha/hora", (d, w) -> {
                            final String title = etTitle.getText().toString().trim();
                            if (title.isEmpty()) {
                                Toast.makeText(this, "Título requerido", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            if (subjects.isEmpty()) {
                                Toast.makeText(this, "Primero creá un sujeto", Toast.LENGTH_LONG).show();
                                return;
                            }
                            final int pos = sp.getSelectedItemPosition();
                            final String subjectId = subjects.get(pos).id;

                            final String c = etCost.getText().toString().trim();
                            final Double cost = c.isEmpty() ? null : safeParseDouble(c);

                            final String fTitle = title;
                            final String fSubjectId = subjectId;
                            final Double fCost = cost;

                            pickDateTime(0, dueAt -> insertLocal(fTitle, fSubjectId, fCost, dueAt));
                        })
                        .setNegativeButton("Cancelar", null)
                        .show();
            });
        }).start();
    }

    private Double safeParseDouble(String s) {
        try { return Double.parseDouble(s); } catch (Exception e) { return null; }
    }

    private void insertLocal(String title, String subjectId, Double cost, long dueAt) {
        new Thread(() -> {
            EventEntity e = new EventEntity();
            e.id = UUID.randomUUID().toString();
            e.uid = "";
            e.appType = appType;
            e.subjectId = subjectId;     // sujeto elegido
            e.title = title;
            e.note = "";
            e.cost = cost;               // costo opcional
            e.realized = 0;              // aún no realizado
            e.dueAt = dueAt;
            e.updatedAt = System.currentTimeMillis();
            e.deleted = 0;
            e.dirty = 1;
            eventDao.insert(e);
            runOnUiThread(() -> Toast.makeText(this, "Guardado local ✅", Toast.LENGTH_SHORT).show());
        }).start();
    }

    private void showEditDialog(EventEntity e) {
        final android.widget.EditText et = new android.widget.EditText(this);
        et.setText(e.title);
        et.setInputType(android.text.InputType.TYPE_CLASS_TEXT);

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Editar evento")
                .setView(et)
                .setPositiveButton("Elegir fecha/hora", (d, w) -> {
                    String title = et.getText().toString().trim();
                    if (title.isEmpty()) {
                        Toast.makeText(this, "Título requerido", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    pickDateTime(e.dueAt, dueAt -> updateLocal(e, title, dueAt));
                })
                .setNeutralButton("Eliminar", (d, w) -> softDelete(e.id))
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void updateLocal(EventEntity e, String title, long dueAt) {
        new Thread(() -> {
            e.title = title;
            e.dueAt = dueAt;
            e.updatedAt = System.currentTimeMillis();
            e.dirty = 1;
            eventDao.update(e);
            runOnUiThread(() -> Toast.makeText(this, "Actualizado local ✅", Toast.LENGTH_SHORT).show());
        }).start();
    }

    private void softDelete(String id) {
        new Thread(() -> {
            eventDao.softDelete(id, System.currentTimeMillis());
            runOnUiThread(() -> Toast.makeText(this, "Eliminado ✅", Toast.LENGTH_SHORT).show());
        }).start();
    }

    // ===== Picker de fecha/hora =====
    private interface DateTimeCallback { void onPicked(long dueAtMillis); }
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

    // ===== Auto-realizar eventos vencidos =====
    @Override
    protected void onResume() {
        super.onResume();
        autoRealizePastEvents();
    }

    private void autoRealizePastEvents() {
        new Thread(() -> {
            long now = System.currentTimeMillis();
            List<EventEntity> due = eventDao.listDueUnrealized(appType, now);
            if (due == null || due.isEmpty()) return;

            java.util.List<String> ids = new java.util.ArrayList<>();
            for (EventEntity e : due) ids.add(e.id);

            eventDao.markRealized(ids, now);

            runOnUiThread(() ->
                    Toast.makeText(this, "Marcados como realizados: " + ids.size(), Toast.LENGTH_SHORT).show());
        }).start();
    }

    // ===== Crear sujeto rápido =====
    private void showQuickAddSubjectDialog() {
        final android.widget.EditText et = new android.widget.EditText(this);
        et.setHint("Nombre");
        et.setInputType(android.text.InputType.TYPE_CLASS_TEXT);

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Nuevo sujeto")
                .setView(et)
                .setPositiveButton("Guardar", (d, w) -> {
                    String name = et.getText().toString().trim();
                    if (name.isEmpty()) {
                        Toast.makeText(this, "Nombre requerido", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    insertSubjectMinimal(name);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void insertSubjectMinimal(String name) {
        new Thread(() -> {
            SubjectEntity s = new SubjectEntity();
            s.id = UUID.randomUUID().toString();
            s.appType = appType;
            s.name = name;
            s.birthDate = null;
            s.currentMeasure = null;
            s.notes = "";
            s.iconKey = defaultIconForFlavor();
            s.colorHex = "#03DAC5";
            s.updatedAt = System.currentTimeMillis();
            s.deleted = 0;
            s.dirty = 1;
            subjectDao.insert(s);

            currentSubjectId = s.id;
            getSharedPreferences("prefs", MODE_PRIVATE)
                    .edit().putString("currentSubjectId_" + appType, currentSubjectId).apply();

            runOnUiThread(() -> {
                Toast.makeText(this, "Sujeto creado ✅", Toast.LENGTH_SHORT).show();
                refreshHeader();
            });
        }).start();
    }

    private String defaultIconForFlavor() {
        if ("pets".equals(appType))   return "dog";
        if ("cars".equals(appType))   return "car";
        if ("house".equals(appType))  return "house";
        return "user";
    }
}
