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
import com.gastonlesbegueris.caretemplate.util.LimitGuard;
import com.gastonlesbegueris.caretemplate.util.UserManager;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;


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
        
        // Mostrar título y icono de navegación
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(true);
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        }
        
        toolbar.setTitle(getString(R.string.app_name));
        toolbar.setSubtitle(null);
        // Set default icon immediately (will be updated by refreshHeader() if subject exists)
        // No aplicar tint, el icono ya tiene el color fijo #03DAC5
        toolbar.setNavigationIcon(R.drawable.ic_header_flavor);
        // Icon will be updated by refreshHeader() if a subject is selected
        refreshHeader();

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

        // 9) AdMob Banner
        initAdMob();
        
        // 10) Inicializar UserManager (ID único para suscripciones)
        initializeUserManager();
    }
    
    private void initializeUserManager() {
        UserManager userManager = new UserManager(this);
        userManager.initializeUser(new UserManager.UserIdCallback() {
            @Override
            public void onUserId(String userId) {
                // ID único del usuario disponible
                // Este ID se usará para suscripciones y sincronización
                android.util.Log.d("MainActivity", "User ID initialized: " + userId);
            }
            
            @Override
            public void onError(Exception error) {
                android.util.Log.e("MainActivity", "Error initializing user ID", error);
            }
        });
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
    protected void onDestroy() {
        AdView adView = findViewById(R.id.adView);
        if (adView != null) {
            adView.destroy();
        }
        super.onDestroy();
    }

    // ===== Toolbar / Menú =====
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        menuItemSync = menu.findItem(R.id.action_sync);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // Asegurar que los iconos estén asignados
        for (int i = 0; i < menu.size(); i++) {
            MenuItem item = menu.getItem(i);
            if (item.getIcon() == null) {
                int itemId = item.getItemId();
                if (itemId == R.id.action_home) {
                    item.setIcon(R.drawable.ic_home);
                } else if (itemId == R.id.action_agenda) {
                    item.setIcon(R.drawable.ic_event);
                } else if (itemId == R.id.action_sync) {
                    item.setIcon(R.drawable.ic_sync);
                } else if (itemId == R.id.action_subjects) {
                    item.setIcon(R.drawable.ic_line_user);
                } else if (itemId == R.id.action_expenses) {
                    item.setIcon(R.drawable.ic_line_money);
                }
            }
        }
        return super.onPrepareOptionsMenu(menu);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_home) {
            // Ya estamos en MainActivity, solo hacer scroll al inicio y limpiar filtros
            // Limpiar filtro de sujeto (si hay uno)
            if (currentSubjectId != null) {
                currentSubjectId = null;
                getSharedPreferences("prefs", MODE_PRIVATE)
                        .edit().remove("currentSubjectId_" + appType).apply();
            }
            
            // Hacer scroll al inicio del RecyclerView
            RecyclerView rv = findViewById(R.id.rvEvents);
            if (rv != null) {
                // Forzar scroll incluso si ya está en la posición 0
                androidx.recyclerview.widget.LinearLayoutManager layoutManager = 
                        (androidx.recyclerview.widget.LinearLayoutManager) rv.getLayoutManager();
                if (layoutManager != null) {
                    int firstVisible = layoutManager.findFirstVisibleItemPosition();
                    if (firstVisible > 5) {
                        // Si está lejos del inicio, hacer smooth scroll
                        rv.smoothScrollToPosition(0);
                    } else {
                        // Si está cerca, hacer scroll inmediato para que se note
                        rv.scrollToPosition(0);
                    }
                } else {
                    rv.smoothScrollToPosition(0);
                }
            }
            
            // Refrescar header
            refreshHeader();
            
            return true;
        } else if (id == R.id.action_agenda) {
            startActivity(new android.content.Intent(this, AgendaMonthActivity.class));
            return true;

    } else if (id == R.id.action_sync) {
            startSyncIconAnimation();
            try {
                if (FirebaseAuth.getInstance().getCurrentUser() == null) {
                    FirebaseAuth.getInstance().signInAnonymously()
                            .addOnSuccessListener(authResult -> {
                                if (authResult != null && authResult.getUser() != null) {
                                    doSync();
                                } else {
                                    stopSyncIconAnimation();
                                    Toast.makeText(this, "Error: No se pudo autenticar", Toast.LENGTH_LONG).show();
                                }
                            })
                            .addOnFailureListener(e -> {
                                stopSyncIconAnimation();
                                String errorMsg = e.getMessage();
                                if (errorMsg != null && errorMsg.contains("SecurityException")) {
                                    Toast.makeText(this, "Error de configuración. Verifica Firebase Console y SHA-1", Toast.LENGTH_LONG).show();
                                } else {
                                    Toast.makeText(this, "Auth error: " + errorMsg, Toast.LENGTH_LONG).show();
                                }
                            });
                } else {
                    doSync();
                }
            } catch (Exception e) {
                stopSyncIconAnimation();
                Toast.makeText(this, "Error al iniciar sync: " + e.getMessage(), Toast.LENGTH_LONG).show();
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

    private void showSyncError(String context, Exception e) {
        String errorMsg = e.getMessage();
        String userMessage;
        
        if (errorMsg == null) {
            userMessage = context + ": Error desconocido";
        } else if (errorMsg.contains("Failed to get service") || 
                   errorMsg.contains("reconnection") || 
                   errorMsg.contains("UNAVAILABLE") ||
                   errorMsg.contains("DEADLINE_EXCEEDED") ||
                   errorMsg.contains("network")) {
            userMessage = "Error de conexión. Verifica tu internet e intenta de nuevo";
        } else if (errorMsg.contains("PERMISSION_DENIED") || errorMsg.contains("permission")) {
            userMessage = "Error de permisos. Verifica la configuración de Firestore";
        } else if (errorMsg.contains("UNAUTHENTICATED") || errorMsg.contains("auth")) {
            userMessage = "Error de autenticación. Intenta sincronizar de nuevo";
        } else if (errorMsg.contains("SecurityException")) {
            userMessage = "Error de seguridad. Verifica SHA-1 en Firebase Console";
        } else {
            userMessage = context + ": " + errorMsg;
        }
        
        Toast.makeText(this, userMessage, Toast.LENGTH_LONG).show();
    }

    // ===== Sync Cloud <-> Local =====
    private void doSync() {
        try {
            com.google.firebase.auth.FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user == null) {
                runOnUiThread(() -> {
                    stopSyncIconAnimation();
                    Toast.makeText(this, "Error: Usuario no autenticado", Toast.LENGTH_LONG).show();
                });
                return;
            }
            
            String uid = user.getUid();
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
                                    showSyncError("Pull error", e);
                                    stopSyncIconAnimation();
                                })
                        );
                    }, e -> runOnUiThread(() -> {
                        showSyncError("Pull subjects error", e);
                        stopSyncIconAnimation();
                    }));
                }, e -> runOnUiThread(() -> {
                    showSyncError("Push events error", e);
                    stopSyncIconAnimation();
                }));
            }, e -> runOnUiThread(() -> {
                showSyncError("Push subjects error", e);
                stopSyncIconAnimation();
            }));
        } catch (SecurityException e) {
            runOnUiThread(() -> {
                stopSyncIconAnimation();
                Toast.makeText(this, "Error de seguridad. Verifica la configuración de Firebase (SHA-1)", Toast.LENGTH_LONG).show();
            });
        } catch (Exception e) {
            runOnUiThread(() -> {
                stopSyncIconAnimation();
                Toast.makeText(this, "Error en sync: " + e.getMessage(), Toast.LENGTH_LONG).show();
            });
        }
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
            java.util.Map<String, String> nameMap = new java.util.HashMap<>();
            java.util.Map<String, String> iconKeyMap = new java.util.HashMap<>();
            java.util.Map<String, String> colorHexMap = new java.util.HashMap<>();
            if (subjects != null) {
                for (com.gastonlesbegueris.caretemplate.data.local.SubjectEntity s : subjects) {
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
    // ===== Header simple en el Home =====
    private void refreshHeader() {
        runOnUiThread(() -> {
            MaterialToolbar toolbar = findViewById(R.id.toolbar);
            if (toolbar == null) return;

            // Siempre: nombre de la app
            toolbar.setTitle(getString(R.string.app_name));

            // Nunca mostramos subtítulo acá
            toolbar.setSubtitle(null);

            // Icono siempre del flavor (perro / auto / casa / familia según flavor)
            // No aplicar tint, el icono ya tiene el color fijo #03DAC5
            toolbar.setNavigationIcon(R.drawable.ic_header_flavor);
        });
    }

    private int getIconResForSubject(String key) {
        if (key == null) return R.drawable.ic_line_user;
        switch (key) {
            // Pets
            case "cat":   return R.drawable.ic_line_cat;
            case "dog":   return R.drawable.ic_line_dog;
            // Family
            case "man":   return R.drawable.ic_line_man;
            case "woman": return R.drawable.ic_line_woman;
            // House
            case "apartment": return R.drawable.ic_line_apartment;
            case "house": return R.drawable.ic_line_house;
            case "office": return R.drawable.ic_line_office;
            case "local": return R.drawable.ic_line_local;
            case "store": return R.drawable.ic_line_store;
            // Vehicles
            case "car":   return R.drawable.ic_line_car;
            case "bike":  return R.drawable.ic_line_bike;
            case "motorbike": return R.drawable.ic_line_motorbike;
            case "truck": return R.drawable.ic_line_truck;
            case "pickup": return R.drawable.ic_line_pickup;
            case "suv":   return R.drawable.ic_line_suv;
            // Default
            case "user":  return R.drawable.ic_line_user;
            default:      return R.drawable.ic_line_user;
        }
    }

    // Obtiene los iconos disponibles según el tipo de app
    private java.util.List<IconOption> getAvailableIcons() {
        java.util.List<IconOption> icons = new java.util.ArrayList<>();
        if ("pets".equals(appType)) {
            icons.add(new IconOption("cat", R.drawable.ic_line_cat));
            icons.add(new IconOption("dog", R.drawable.ic_line_dog));
        } else if ("family".equals(appType)) {
            icons.add(new IconOption("man", R.drawable.ic_line_man));
            icons.add(new IconOption("woman", R.drawable.ic_line_woman));
        } else if ("house".equals(appType)) {
            icons.add(new IconOption("apartment", R.drawable.ic_line_apartment));
            icons.add(new IconOption("house", R.drawable.ic_line_house));
            icons.add(new IconOption("office", R.drawable.ic_line_office));
            icons.add(new IconOption("local", R.drawable.ic_line_local));
            icons.add(new IconOption("store", R.drawable.ic_line_store));
        } else if ("cars".equals(appType)) {
            icons.add(new IconOption("car", R.drawable.ic_line_car));
            icons.add(new IconOption("bike", R.drawable.ic_line_bike));
            icons.add(new IconOption("motorbike", R.drawable.ic_line_motorbike));
            icons.add(new IconOption("truck", R.drawable.ic_line_truck));
            icons.add(new IconOption("pickup", R.drawable.ic_line_pickup));
            icons.add(new IconOption("suv", R.drawable.ic_line_suv));
        } else {
            icons.add(new IconOption("user", R.drawable.ic_line_user));
        }
        return icons;
    }

    // Clase auxiliar para opciones de icono
    private static class IconOption {
        final String key;
        final int drawableRes;
        IconOption(String key, int drawableRes) {
            this.key = key;
            this.drawableRes = drawableRes;
        }
    }

    // Popula el grid de iconos
    private void populateIconGrid(android.widget.GridLayout grid, final String[] selectedKey) {
        if (grid == null) return;
        grid.removeAllViews();
        
        java.util.List<IconOption> icons = getAvailableIcons();
        int size = (int) (48 * getResources().getDisplayMetrics().density);
        int margin = (int) (8 * getResources().getDisplayMetrics().density);
        
        for (IconOption icon : icons) {
            android.widget.ImageView iv = new android.widget.ImageView(this);
            android.widget.GridLayout.LayoutParams params = new android.widget.GridLayout.LayoutParams();
            params.width = size;
            params.height = size;
            params.setMargins(margin, margin, margin, margin);
            iv.setLayoutParams(params);
            iv.setImageResource(icon.drawableRes);
            iv.setPadding(margin, margin, margin, margin);
            iv.setBackgroundResource(android.R.drawable.dialog_holo_light_frame);
            iv.setScaleType(android.widget.ImageView.ScaleType.FIT_CENTER);
            
            // Tint inicial
            if (icon.key.equals(selectedKey[0])) {
                iv.setBackgroundColor(0x3303DAC5);
            }
            
            iv.setOnClickListener(v -> {
                // Reset all backgrounds
                for (int i = 0; i < grid.getChildCount(); i++) {
                    View child = grid.getChildAt(i);
                    child.setBackgroundResource(android.R.drawable.dialog_holo_light_frame);
                }
                // Set selected background
                v.setBackgroundColor(0x3303DAC5);
                selectedKey[0] = icon.key;
            });
            
            grid.addView(iv);
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
        final com.google.android.material.textfield.TextInputEditText etTitle = view.findViewById(R.id.etTitle);
        final com.google.android.material.textfield.TextInputEditText etCost  = view.findViewById(R.id.etCost);
        final android.widget.Spinner sp       = view.findViewById(R.id.spSubject);

        // Verificar que las vistas se encontraron
        if (etTitle == null || etCost == null || sp == null) {
            Toast.makeText(this, "Error al cargar el diálogo", Toast.LENGTH_SHORT).show();
            return;
        }

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
                            final String title = etTitle.getText() != null ? etTitle.getText().toString().trim() : "";
                            if (title.isEmpty()) {
                                Toast.makeText(this, "Título requerido", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            if (subjects.isEmpty()) {
                                Toast.makeText(this, "Primero creá un sujeto", Toast.LENGTH_LONG).show();
                                return;
                            }
                            final int pos = sp.getSelectedItemPosition();
                            if (pos < 0 || pos >= subjects.size()) {
                                Toast.makeText(this, "Seleccioná un sujeto", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            final String subjectId = subjects.get(pos).id;

                            final String c = etCost.getText() != null ? etCost.getText().toString().trim() : "";
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
            com.gastonlesbegueris.caretemplate.util.LimitGuard.onEventCreated(this, appType);

            runOnUiThread(() -> Toast.makeText(this, "Guardado local ✅", Toast.LENGTH_SHORT).show());
        }).start();
    }

    private void showEditDialog(EventEntity e) {
        final android.view.View view = getLayoutInflater().inflate(R.layout.dialog_add_event, null);
        final com.google.android.material.textfield.TextInputEditText etTitle = view.findViewById(R.id.etTitle);
        final com.google.android.material.textfield.TextInputEditText etCost  = view.findViewById(R.id.etCost);
        final android.widget.Spinner sp = view.findViewById(R.id.spSubject);

        // Verificar que las vistas se encontraron
        if (etTitle == null || etCost == null || sp == null) {
            Toast.makeText(this, "Error al cargar el diálogo", Toast.LENGTH_SHORT).show();
            return;
        }

        // Pre-populate title and cost
        etTitle.setText(e.title);
        if (e.cost != null) {
            etCost.setText(String.format(java.util.Locale.getDefault(), "%.2f", e.cost));
        }

        // Hide subject spinner for editing (subject shouldn't change)
        sp.setVisibility(android.view.View.GONE);

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Editar evento")
                .setView(view)
                .setPositiveButton("Elegir fecha/hora", (d, w) -> {
                    String title = etTitle.getText() != null ? etTitle.getText().toString().trim() : "";
                    if (title.isEmpty()) {
                        Toast.makeText(this, "Título requerido", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (!LimitGuard.canCreateEvent(this, db, appType)) return;

                    final String c = etCost.getText() != null ? etCost.getText().toString().trim() : "";
                    final Double cost = c.isEmpty() ? null : safeParseDouble(c);

                    pickDateTime(e.dueAt, dueAt -> updateLocal(e, title, cost, dueAt));
                })
                .setNeutralButton("Eliminar", (d, w) -> softDelete(e.id))
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void updateLocal(EventEntity e, String title, Double cost, long dueAt) {
        new Thread(() -> {
            e.title = title;
            e.cost = cost;
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
        AdView adView = findViewById(R.id.adView);
        if (adView != null) {
            adView.resume();
        }
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
        if (!LimitGuard.canCreateSubject(this, db, appType)) return;

        final android.view.View view = getLayoutInflater().inflate(R.layout.dialog_edit_subject, null, false);
        final android.widget.EditText etName = view.findViewById(R.id.etName);
        final android.widget.EditText etBirth = view.findViewById(R.id.etBirth);
        final android.widget.EditText etMeasure = view.findViewById(R.id.etMeasure);
        final android.widget.EditText etNotes = view.findViewById(R.id.etNotes);

        // Campos de marca y modelo (solo para cars)
        final com.google.android.material.textfield.TextInputLayout tilBrand = view.findViewById(R.id.tilBrand);
        final com.google.android.material.textfield.TextInputLayout tilModel = view.findViewById(R.id.tilModel);
        final android.widget.EditText etBrand = view.findViewById(R.id.etBrand);
        final android.widget.EditText etModel = view.findViewById(R.id.etModel);

        // Configurar UI según flavor
        if ("cars".equals(appType)) {
            // Para cars: ocultar nombre, mostrar marca y modelo
            if (view.findViewById(R.id.tilName) != null) {
                view.findViewById(R.id.tilName).setVisibility(android.view.View.GONE);
            }
            if (tilBrand != null) tilBrand.setVisibility(android.view.View.VISIBLE);
            if (tilModel != null) tilModel.setVisibility(android.view.View.VISIBLE);
        } else {
            // Para otros flavors: ocultar marca y modelo
            if (tilBrand != null) tilBrand.setVisibility(android.view.View.GONE);
            if (tilModel != null) tilModel.setVisibility(android.view.View.GONE);
        }

        // Hide fields not needed for quick add
        if (etBirth != null) etBirth.setVisibility(android.view.View.GONE);
        if (etMeasure != null) etMeasure.setVisibility(android.view.View.GONE);
        if (etNotes != null) etNotes.setVisibility(android.view.View.GONE);

        // Setup icon selection
        final android.widget.GridLayout gridIcons = view.findViewById(R.id.gridIcons);
        final String[] selectedIconKey = {defaultIconForFlavor()};
        populateIconGrid(gridIcons, selectedIconKey);

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Nuevo sujeto")
                .setView(view)
                .setPositiveButton("Guardar", (d, w) -> {
                    String name;
                    if ("cars".equals(appType)) {
                        // Para cars: concatenar marca + modelo
                        String brand = etBrand != null ? etBrand.getText().toString().trim() : "";
                        String model = etModel != null ? etModel.getText().toString().trim() : "";
                        if (brand.isEmpty() && model.isEmpty()) {
                            Toast.makeText(this, "Marca o Modelo requerido", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        if (brand.isEmpty()) {
                            name = model;
                        } else if (model.isEmpty()) {
                            name = brand;
                        } else {
                            name = brand + " " + model;
                        }
                    } else {
                        // Para otros flavors: usar nombre normal
                        name = etName.getText().toString().trim();
                        if (name.isEmpty()) {
                            Toast.makeText(this, "Nombre requerido", Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }
                    insertSubjectMinimal(name, selectedIconKey[0]);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void insertSubjectMinimal(String name, String iconKey) {
        new Thread(() -> {
            SubjectEntity s = new SubjectEntity();
            s.id = UUID.randomUUID().toString();
            s.appType = appType;
            s.name = name;
            s.birthDate = null;
            s.currentMeasure = null;
            s.notes = "";
            s.iconKey = (iconKey == null || iconKey.isEmpty()) ? defaultIconForFlavor() : iconKey;
            s.colorHex = "#03DAC5";
            s.updatedAt = System.currentTimeMillis();
            s.deleted = 0;
            s.dirty = 1;
            subjectDao.insert(s);
            com.gastonlesbegueris.caretemplate.util.LimitGuard.onSubjectCreated(this, appType);

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
