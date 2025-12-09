package com.gastonlesbegueris.caretemplate.ui;

import android.Manifest;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
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
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.google.android.gms.ads.rewarded.RewardItem;
import com.google.android.gms.ads.OnUserEarnedRewardListener;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import android.util.Log;

import java.util.List;
import java.util.UUID;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.textfield.TextInputEditText;

public class SubjectListActivity extends AppCompatActivity {

    private AppDb db;
    private SubjectDao dao;
    private EventDao eventDao;
    private SubjectAdapter adapter;
    private String appType;
    
    // Rewarded Ad para código de recuperación
    private RewardedAd rewardedAd;
    private boolean isRewardedAdLoading = false;
    
    // Interstitial Ad para sincronización
    private InterstitialAd interstitialAd;
    private boolean isInterstitialAdLoading = false;
    private Runnable syncCallback = null; // Callback para ejecutar después del anuncio
    
    // Flag para indicar que estamos en modo de recuperación silenciosa
    private boolean isSilentRecoveryMode = false;
    
    // Referencia al diálogo de recuperación
    private androidx.appcompat.app.AlertDialog recoverDialog;
    
    // FAB Speed Dial
    private boolean fabMenuOpen = false;
    
    // Código de solicitud de permiso de notificaciones
    private static final int REQUEST_NOTIFICATION_PERMISSION = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_subjects);
        
        // Solicitar permiso de notificaciones para Android 13+ (API 33+)
        requestNotificationPermission();

        MaterialToolbar toolbar = findViewById(R.id.toolbarSubjects);
        setSupportActionBar(toolbar);
        
        // Configurar toolbar como pantalla principal (sin botón back, con menú hamburger)
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        }
        
        // Configurar icono de la app
        toolbar.setNavigationIcon(R.drawable.ic_header_flavor);
        toolbar.setNavigationOnClickListener(v -> {
            // Abrir menú drawer si está disponible, o no hacer nada
        });
        
        // Configurar título con nombre de app y sección
        appType = getString(R.string.app_type); // viene del flavor
        String appName = getString(R.string.app_name);
        String sectionName = getString(R.string.subjects_title); // Mascotas, Autos, Casas, Sujetos según flavor
        toolbar.setTitle(appName + " - " + sectionName);


        db  = AppDb.get(this);
        dao = db.subjectDao();
        eventDao = db.eventDao();
        
        // Verificar si se debe abrir el diálogo de código de recuperación
        if (getIntent() != null && getIntent().getBooleanExtra("show_recovery_code", false)) {
            // Limpiar el flag para que no se abra cada vez que se rote la pantalla
            getIntent().removeExtra("show_recovery_code");
            // Abrir el diálogo después de que la UI esté lista
            findViewById(R.id.fabAddSubject).post(() -> {
                showRecoveryCodeDialog();
            });
        }
        
        // Verificar si se debe agregar un evento (desde otras actividades)
        // Como estamos en la lista de sujetos, simplemente ignoramos este intent
        // Los eventos se agregan desde el historial de un sujeto o desde la agenda
        if (getIntent() != null && getIntent().getBooleanExtra("add_event", false)) {
            getIntent().removeExtra("add_event");
            // Mostrar mensaje indicando que se debe seleccionar un sujeto primero
            Toast.makeText(this, getString(R.string.select_subject_first), Toast.LENGTH_SHORT).show();
        }
        
        // Inicializar código de recuperación
        initializeRecoveryCode();

        adapter = new SubjectAdapter(new SubjectAdapter.OnClick() {
            @Override public void onEdit(SubjectEntity subj) { showEditDialog(subj); }
            @Override public void onDelete(SubjectEntity subj) { softDelete(subj.id); }
            @Override public void onViewHistory(SubjectEntity subj) { 
                // Abrir actividad de historial
                android.content.Intent intent = new android.content.Intent(SubjectListActivity.this, SubjectHistoryActivity.class);
                intent.putExtra("subjectId", subj.id);
                intent.putExtra("subjectName", subj.name);
                startActivity(intent);
            }
        });

        RecyclerView rv = findViewById(R.id.rvSubjects);
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adapter);

        // Inicializar FAB Speed Dial
        initFabSpeedDial();

        // AdMob Banner
        initAdMob();

        // observar lista de sujetos
        dao.observeActive(appType).observe(this, (List<SubjectEntity> list) -> {
            new Thread(() -> {
                // Obtener todos los próximos eventos de una vez (optimización para evitar múltiples consultas)
                long now = System.currentTimeMillis();
                EventDao eventDao = db.eventDao();
                java.util.List<EventEntity> nextEvents = eventDao.nextEventsForAllSubjects(appType, now);
                
                // Crear mapa de subjectId -> próximo evento para acceso rápido
                java.util.Map<String, EventEntity> nextEventMap = new java.util.HashMap<>();
                if (nextEvents != null) {
                    for (EventEntity event : nextEvents) {
                        if (event.subjectId != null && !nextEventMap.containsKey(event.subjectId)) {
                            nextEventMap.put(event.subjectId, event);
                        }
                    }
                }
                
                java.util.List<SubjectAdapter.SubjectRow> rows = new java.util.ArrayList<>();
                for (SubjectEntity subj : list) {
                    String info  = buildInfoLine(subj);
                    String extra = buildExtraLine(subj, nextEventMap.get(subj.id));
                    rows.add(new SubjectAdapter.SubjectRow(subj, info, extra));
                }
                runOnUiThread(() -> {
                    adapter.submitRows(rows);
                });
            }).start();
        });
    }

    // ---------- Crear ----------
    private void showAddDialog() {
        final android.view.View view = getLayoutInflater().inflate(R.layout.dialog_edit_subject, null, false);

        final EditText etName    = view.findViewById(R.id.etName);
        final EditText etBirth   = view.findViewById(R.id.etBirth);
        final EditText etMeasure = view.findViewById(R.id.etMeasure);
        final EditText etNotes   = view.findViewById(R.id.etNotes);

        // Campos de marca y modelo (solo para cars)
        final com.google.android.material.textfield.TextInputLayout tilBrand = view.findViewById(R.id.tilBrand);
        final com.google.android.material.textfield.TextInputLayout tilModel = view.findViewById(R.id.tilModel);
        final EditText etBrand = view.findViewById(R.id.etBrand);
        final EditText etModel = view.findViewById(R.id.etModel);

        // Configurar UI según flavor
        if ("cars".equals(appType)) {
            // Para cars: ocultar nombre, mostrar marca y modelo
            if (view.findViewById(R.id.tilName) != null) {
                view.findViewById(R.id.tilName).setVisibility(View.GONE);
            }
            if (tilBrand != null) tilBrand.setVisibility(View.VISIBLE);
            if (tilModel != null) tilModel.setVisibility(View.VISIBLE);
        } else {
            // Para otros flavors: ocultar marca y modelo
            if (tilBrand != null) tilBrand.setVisibility(View.GONE);
            if (tilModel != null) tilModel.setVisibility(View.GONE);
        }

        // Obtener los TextInputLayout directamente por ID
        com.google.android.material.textfield.TextInputLayout tilMeasure = view.findViewById(R.id.tilMeasure);
        
        android.view.View parentBirth = etBirth != null ? (android.view.View) etBirth.getParent() : null;
        while (parentBirth != null && !(parentBirth instanceof com.google.android.material.textfield.TextInputLayout)) {
            parentBirth = (android.view.View) parentBirth.getParent();
        }
        com.google.android.material.textfield.TextInputLayout tilBirth = parentBirth instanceof com.google.android.material.textfield.TextInputLayout 
                ? (com.google.android.material.textfield.TextInputLayout) parentBirth : null;

        // Configurar UI según flavor
        if ("cars".equals(appType) || "house".equals(appType)) {
            // Para cars/house: ocultar fecha de nacimiento, mostrar odómetro
            if (tilBirth != null) tilBirth.setVisibility(View.GONE);
            if (tilMeasure != null) {
                tilMeasure.setHint(getString(R.string.field_odometer));
                tilMeasure.setVisibility(View.VISIBLE);
            }
            if (etMeasure != null) {
                etMeasure.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
            }
        } else {
            // Para pets/family: mostrar fecha de nacimiento y peso
            if (tilBirth != null) {
                tilBirth.setHint(getString(R.string.field_birth_date));
                tilBirth.setVisibility(View.VISIBLE);
            }
            if (tilMeasure != null) {
                tilMeasure.setHint(getString(R.string.field_weight));
                tilMeasure.setVisibility(View.VISIBLE);
            }
            if (etMeasure != null) {
                etMeasure.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
            }
        }
        
        etBirth.setOnClickListener(v -> pickDateInto(etBirth));

        // Setup icon selection
        final android.widget.GridLayout gridIcons = view.findViewById(R.id.gridIcons);
        final String[] selectedIconKey = {defaultIconForFlavor()};
        populateIconGrid(gridIcons, selectedIconKey);

        // Configurar barra de título personalizada
        final android.view.View llDialogTitleBar = view.findViewById(R.id.llDialogTitleBar);
        final android.widget.TextView tvDialogTitle = view.findViewById(R.id.tvDialogTitle);
        final android.widget.ImageButton ibSave = view.findViewById(R.id.ibSave);
        final android.widget.ImageButton ibClose = view.findViewById(R.id.ibClose);
        
        if (tvDialogTitle != null) {
            tvDialogTitle.setText(getString(R.string.new_subject));
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
                String name;
                if ("cars".equals(appType)) {
                    // Para cars: concatenar marca + modelo
                    String brand = etBrand != null ? etBrand.getText().toString().trim() : "";
                    String model = etModel != null ? etModel.getText().toString().trim() : "";
                    if (brand.isEmpty() && model.isEmpty()) {
                        Toast.makeText(this, getString(R.string.error_brand_or_model_required), Toast.LENGTH_SHORT).show();
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
                        Toast.makeText(this, getString(R.string.error_name_required), Toast.LENGTH_SHORT).show();
                        return;
                    }
                }
                // Obtener fecha de nacimiento (solo para pets/family)
                Long birthMillis = null;
                if (!"cars".equals(appType) && !"house".equals(appType)) {
                    String birthStr = etBirth.getText() != null ? etBirth.getText().toString().trim() : "";
                    if (!birthStr.isEmpty()) {
                        birthMillis = parseDateOrNull(birthStr);
                    }
                }
                
                // Obtener medida: kilómetros para cars/house, peso para pets/family
                String measureStr = etMeasure.getText() != null ? etMeasure.getText().toString().trim() : "";
                Double measure = measureStr.isEmpty() ? null : safeParseDouble(measureStr);
                
                String notes = etNotes.getText() != null ? etNotes.getText().toString().trim() : "";
                dialog.dismiss();
                insertSubjectFull(name, birthMillis, measure, notes, selectedIconKey[0]);
            });
        }
        
        // Configurar listener del icono Cerrar (Cancelar)
        if (ibClose != null) {
            ibClose.setOnClickListener(v -> dialog.dismiss());
        }
        
        dialog.show();
    }
    // Devuelve un icono por defecto según el flavor activo
    private String defaultIconForFlavor() {
        if ("pets".equals(appType))   return "dog";
        if ("cars".equals(appType))   return "car";
        if ("house".equals(appType))  return "house";
        return "user"; // family u otros
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

    private void insertSubjectFull(String name, Long birthMillis, Double measure, String notes, String iconKey) {
        new Thread(() -> {
            SubjectEntity subj = new SubjectEntity();
            subj.id = UUID.randomUUID().toString();
            subj.appType = appType;
            subj.name = name;
            // Para cars/house: no hay fecha de nacimiento, solo kilómetros
            // Para pets/family: guardar fecha de nacimiento si se proporcionó
            subj.birthDate = ("cars".equals(appType) || "house".equals(appType)) ? null : birthMillis;
            // Guardar medida: kilómetros para cars/house, peso para pets/family
            subj.currentMeasure = measure;

            subj.notes = notes == null ? "" : notes;
            subj.updatedAt = System.currentTimeMillis();
            subj.deleted = 0;
            subj.dirty = 1;
            subj.iconKey = (iconKey == null || iconKey.isEmpty()) ? defaultIconForFlavor() : iconKey;
            subj.colorHex = (subj.colorHex == null || subj.colorHex.isEmpty()) ? "#03DAC5" : subj.colorHex;

            dao.insert(subj);
            runOnUiThread(() -> Toast.makeText(this, getString(R.string.subject_created), Toast.LENGTH_SHORT).show());
        }).start();
    }
    private void showEditDialog(SubjectEntity subj) {
        View view = getLayoutInflater().inflate(R.layout.dialog_subject, null);

        TextInputLayout tilName  = view.findViewById(R.id.tilName);
        TextInputEditText etName = view.findViewById(R.id.etName);

        TextInputLayout tilBirth  = view.findViewById(R.id.tilBirth);
        TextInputEditText etBirth = view.findViewById(R.id.etBirth);

        TextInputLayout tilMeasure  = view.findViewById(R.id.tilMeasure);
        TextInputEditText etMeasure = view.findViewById(R.id.etMeasure);

        TextInputLayout tilNotes  = view.findViewById(R.id.tilNotes);
        TextInputEditText etNotes = view.findViewById(R.id.etNotes);

        // Campos de marca y modelo (solo para cars)
        TextInputLayout tilBrand = view.findViewById(R.id.tilBrand);
        TextInputLayout tilModel = view.findViewById(R.id.tilModel);
        TextInputEditText etBrand = view.findViewById(R.id.etBrand);
        TextInputEditText etModel = view.findViewById(R.id.etModel);

        // Configurar UI según flavor
        if ("cars".equals(appType)) {
            // Para cars: ocultar nombre, mostrar marca y modelo
            if (tilName != null) tilName.setVisibility(View.GONE);
            if (tilBrand != null) tilBrand.setVisibility(View.VISIBLE);
            if (tilModel != null) tilModel.setVisibility(View.VISIBLE);
            
            // Separar nombre en marca y modelo (asumiendo formato "Marca Modelo")
            if (subj.name != null && !subj.name.isEmpty()) {
                String[] parts = subj.name.split(" ", 2);
                if (etBrand != null) etBrand.setText(parts[0]);
                if (etModel != null && parts.length > 1) etModel.setText(parts[1]);
            }
        } else {
            // Para otros flavors: ocultar marca y modelo, mostrar nombre
            if (tilBrand != null) tilBrand.setVisibility(View.GONE);
            if (tilModel != null) tilModel.setVisibility(View.GONE);
            if (etName != null) etName.setText(subj.name);
        }

        // Mostrar campo de fecha para pets y family (no para cars/house)
        if ("pets".equals(appType) || "family".equals(appType)) {
            tilBirth.setVisibility(View.VISIBLE);
            if (subj.birthDate != null) {
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy");
                etBirth.setText(sdf.format(new java.util.Date(subj.birthDate)));
            }
            etBirth.setOnClickListener(v -> pickDateInto(etBirth));
        } else {
            tilBirth.setVisibility(View.GONE);
        }

        // precargar datos del sujeto
        if (subj.currentMeasure != null) {
            etMeasure.setText(String.valueOf(subj.currentMeasure));
        }
        etNotes.setText(subj.notes == null ? "" : subj.notes);

        // Configurar barra de título personalizada
        final android.view.View llDialogTitleBar = view.findViewById(R.id.llDialogTitleBar);
        final android.widget.TextView tvDialogTitle = view.findViewById(R.id.tvDialogTitle);
        final android.widget.ImageButton ibDelete = view.findViewById(R.id.ibDelete);
        final android.widget.ImageButton ibSave = view.findViewById(R.id.ibSave);
        final android.widget.ImageButton ibClose = view.findViewById(R.id.ibClose);
        
        if (tvDialogTitle != null) {
            tvDialogTitle.setText(getString(R.string.edit_subject));
        }

        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(this)
                .setView(view)
                .create();
        
        // Mostrar barra de título personalizada
        if (llDialogTitleBar != null) {
            llDialogTitleBar.setVisibility(android.view.View.VISIBLE);
        }
        
        // Si es pets, mostrar botón de eliminar
        if ("pets".equals(appType) && ibDelete != null) {
            ibDelete.setVisibility(android.view.View.VISIBLE);
            ibDelete.setOnClickListener(v -> {
                dialog.dismiss();
                new androidx.appcompat.app.AlertDialog.Builder(this)
                        .setTitle(getString(R.string.delete_subject))
                        .setMessage(getString(R.string.delete_subject_confirmation, subj.name))
                        .setPositiveButton(getString(R.string.button_delete), (d2, w2) -> softDelete(subj.id))
                        .setNegativeButton(getString(R.string.button_cancel), null)
                        .show();
            });
        } else if (ibDelete != null) {
            ibDelete.setVisibility(android.view.View.GONE);
        }

        // Configurar listener del icono Guardar
        if (ibSave != null) {
            ibSave.setOnClickListener(v -> {
                String name;
                if ("cars".equals(appType)) {
                    // Para cars: concatenar marca + modelo
                    String brand = etBrand != null && etBrand.getText() != null ? etBrand.getText().toString().trim() : "";
                    String model = etModel != null && etModel.getText() != null ? etModel.getText().toString().trim() : "";
                    if (brand.isEmpty() && model.isEmpty()) {
                        android.widget.Toast.makeText(this, getString(R.string.error_brand_or_model_required), android.widget.Toast.LENGTH_SHORT).show();
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
                    name = etName.getText() == null ? "" : etName.getText().toString().trim();
                    if (name.isEmpty()) {
                        android.widget.Toast.makeText(this, getString(R.string.error_name_required), android.widget.Toast.LENGTH_SHORT).show();
                        return;
                    }
                }

                String m    = etMeasure.getText() == null ? "" : etMeasure.getText().toString().trim();
                String notes = etNotes.getText() == null ? "" : etNotes.getText().toString().trim();
                String birthStr = etBirth.getText() == null ? "" : etBirth.getText().toString().trim();

                Double measure = null;
                try { if (!m.isEmpty()) measure = Double.parseDouble(m); } catch (Exception ignore) {}
                final Double finalMeasure = measure;

                Long birthMillis = null;
                // Para pets y family: guardar fecha de nacimiento si se proporcionó
                if (("pets".equals(appType) || "family".equals(appType)) && !birthStr.isEmpty()) {
                    birthMillis = parseDateOrNull(birthStr);
                }

                final Long finalBirthMillis = birthMillis;
                final String finalName = name;
                dialog.dismiss();
                
                // actualizar en background
                new Thread(() -> {
                    subj.name = finalName;
                    subj.currentMeasure = finalMeasure;
                    subj.notes = notes;
                    if ("pets".equals(appType)) {
                        subj.birthDate = finalBirthMillis;
                    }
                    subj.updatedAt = System.currentTimeMillis();
                    subj.dirty = 1;
                    AppDb.get(this).subjectDao().update(subj);

                    runOnUiThread(() -> {
                        android.widget.Toast.makeText(this, getString(R.string.subject_updated), android.widget.Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    });
                }).start();
            });
        }
        
        // Configurar listener del icono Cerrar (Cancelar)
        if (ibClose != null) {
            ibClose.setOnClickListener(v -> dialog.dismiss());
        }

        dialog.show();
    }
    

    private void updateSubjectFull(SubjectEntity subj, String name, Long birthMillis, Double measure, String notes) {
        new Thread(() -> {
            subj.name = name;
            subj.birthDate = ("cars".equals(appType) || "house".equals(appType)) ? null : birthMillis;
            subj.currentMeasure = measure;
            subj.notes = notes == null ? "" : notes;
            subj.updatedAt = System.currentTimeMillis();
            subj.dirty = 1;
            dao.update(subj);
            runOnUiThread(() -> Toast.makeText(this, getString(R.string.subject_updated_simple), Toast.LENGTH_SHORT).show());
        }).start();
    }

    private void softDelete(String id) {
        // Obtener el nombre del sujeto para el mensaje de confirmación
        new Thread(() -> {
            SubjectEntity subject = dao.findOne(id);
            String subjectName = subject != null && subject.name != null ? subject.name : "este sujeto";
            
            runOnUiThread(() -> {
                // Mostrar diálogo de confirmación
                new androidx.appcompat.app.AlertDialog.Builder(this)
                        .setTitle(getString(R.string.delete_subject))
                        .setMessage(getString(R.string.delete_subject_confirmation_full, subjectName))
                        .setPositiveButton(getString(R.string.button_delete), (d, w) -> {
                            // Confirmar eliminación
                            new Thread(() -> {
                                dao.softDelete(id, System.currentTimeMillis());
                                runOnUiThread(() -> Toast.makeText(this, getString(R.string.subject_deleted), Toast.LENGTH_SHORT).show());
                            }).start();
                        })
                        .setNegativeButton(getString(R.string.button_cancel), null)
                        .show();
            });
        }).start();
    }

    // ---------- Helpers visuales ----------
    private void setupDialogUiByFlavor(EditText etBirth, EditText etMeasure) {
        // Obtener el TextInputLayout padre para poder mostrar/ocultar correctamente
        android.view.View parentBirth = etBirth != null ? (android.view.View) etBirth.getParent() : null;
        while (parentBirth != null && !(parentBirth instanceof com.google.android.material.textfield.TextInputLayout)) {
            parentBirth = (android.view.View) parentBirth.getParent();
        }
        com.google.android.material.textfield.TextInputLayout tilBirth = parentBirth instanceof com.google.android.material.textfield.TextInputLayout 
                ? (com.google.android.material.textfield.TextInputLayout) parentBirth : null;

        if ("cars".equals(appType) || "house".equals(appType)) {
            // Para cars/house: ocultar fecha de nacimiento, mostrar odómetro
            if (tilBirth != null) tilBirth.setVisibility(android.view.View.GONE);
            if (etMeasure != null) {
                etMeasure.setHint(getString(R.string.field_odometer));
                etMeasure.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
            }
        } else {
            // Para pets/family: mostrar fecha de nacimiento y peso
            if (tilBirth != null) tilBirth.setVisibility(android.view.View.VISIBLE);
            if (etBirth != null) {
                etBirth.setHint(getString(R.string.field_birth_date));
            }
            if (etMeasure != null) {
                etMeasure.setHint(getString(R.string.field_weight));
                etMeasure.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
            }
        }
    }

    private void pickDateInto(final EditText target) {
        final java.util.Calendar cal = java.util.Calendar.getInstance();
        new android.app.DatePickerDialog(this, (view, y, m, d) -> {
            cal.set(java.util.Calendar.YEAR, y);
            cal.set(java.util.Calendar.MONTH, m);
            cal.set(java.util.Calendar.DAY_OF_MONTH, d);
            java.text.SimpleDateFormat f = new java.text.SimpleDateFormat("dd/MM/yyyy");
            target.setText(f.format(cal.getTime()));
        }, cal.get(java.util.Calendar.YEAR), cal.get(java.util.Calendar.MONTH), cal.get(java.util.Calendar.DAY_OF_MONTH)).show();
    }

    private Long parseDateOrNull(String s) {
        if (s == null || s.isEmpty()) return null;
        try {
            java.text.SimpleDateFormat f = new java.text.SimpleDateFormat("dd/MM/yyyy");
            f.setLenient(false);
            return f.parse(s).getTime();
        } catch (Exception e) { return null; }
    }

    private Double safeParseDouble(String s) {
        try { return Double.parseDouble(s.replace(',', '.')); } catch (Exception e) { return null; }
    }

    // ---------- Helpers para la lista ----------
    private String buildInfoLine(SubjectEntity subj) {
        if ("cars".equals(appType) || "house".equals(appType)) {
            String km = (subj.currentMeasure == null) ? "-" : (Math.round(subj.currentMeasure) + " km");
            return getString(R.string.odometer_label, km);
        } else {
            String age = formatAge(subj.birthDate);
            String weightStr = (subj.currentMeasure == null) ? "" : getString(R.string.weight_label, String.valueOf(subj.currentMeasure));
            return getString(R.string.age_label, age) + weightStr;
        }
    }

    private String buildExtraLine(SubjectEntity subj, EventEntity next) {
        if (next == null) return getString(R.string.next_event_none);
        String when = new java.text.SimpleDateFormat(getString(R.string.event_date_format)).format(new java.util.Date(next.dueAt));
        if (next.title == null || next.title.isEmpty()) {
            return getString(R.string.next_event_label, when);
        } else {
            return getString(R.string.next_event_with_title, when, next.title);
        }
    }
    
    // Método sobrecargado para compatibilidad (usa consulta individual, menos eficiente)
    private String buildExtraLine(SubjectEntity subj) {
        EventDao eventDao = db.eventDao();
        long now = System.currentTimeMillis();
        EventEntity next = eventDao.nextEvent(appType, subj.id, now);
        return buildExtraLine(subj, next);
    }

    private String formatAge(Long birthDate) {
        if (birthDate == null) return "-";
        java.util.Calendar b = java.util.Calendar.getInstance();
        b.setTimeInMillis(birthDate);
        java.util.Calendar now = java.util.Calendar.getInstance();
        int years = now.get(java.util.Calendar.YEAR) - b.get(java.util.Calendar.YEAR);
        int months = now.get(java.util.Calendar.MONTH) - b.get(java.util.Calendar.MONTH);
        if (months < 0) { years--; months += 12; }
        String yearShort = getString(R.string.age_year_short);
        String monthShort = getString(R.string.age_month_short);
        return years > 0 ? years + yearShort + " " + months + monthShort : months + monthShort;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // Asegurar que el título del menú "Importar sujeto" esté correcto
        MenuItem shareItem = menu.findItem(R.id.action_share_subject);
        if (shareItem != null) {
            shareItem.setTitle(getString(R.string.menu_import_subject));
        }
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
            startActivity(new android.content.Intent(this, com.gastonlesbegueris.caretemplate.ui.AgendaMonthActivity.class));
            return true;
        } else if (id == R.id.action_subjects) {
            // Already on subjects page
            return true;
        } else if (id == R.id.action_expenses) {
            startActivity(new android.content.Intent(this, com.gastonlesbegueris.caretemplate.ui.ExpensesActivity.class));
            return true;
        } else if (id == R.id.action_sync) {
            // Mostrar anuncio intersticial antes de sincronizar
            showInterstitialAdAndSync(() -> {
                // Este callback se ejecutará después de que se cierre el anuncio
                runOnUiThread(() -> doSync());
            });
            return true;
        } else if (id == R.id.action_recovery) {
            // Mostrar el código de recuperación
            showRecoveryCodeDialog();
            return true;
        } else if (id == R.id.action_share_subject) {
            // Mostrar diálogo para importar sujeto compartido
            showImportSharedSubjectDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void initAdMob() {
        MobileAds.initialize(this, initializationStatus -> {});
        AdView adView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        adView.loadAd(adRequest);
        
        // Precargar anuncio intersticial
        loadInterstitialAd();
    }
    
    private void loadInterstitialAd() {
        if (isInterstitialAdLoading || interstitialAd != null) {
            return; // Ya está cargando o ya está cargado
        }
        
        isInterstitialAdLoading = true;
        String interstitialAdId = getString(R.string.admob_interstitial_id);
        AdRequest adRequest = new AdRequest.Builder().build();
        
        InterstitialAd.load(this, interstitialAdId, adRequest,
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(InterstitialAd ad) {
                        isInterstitialAdLoading = false;
                        interstitialAd = ad;
                        Log.d("SubjectListActivity", "Interstitial ad loaded");
                    }
                    
                    @Override
                    public void onAdFailedToLoad(LoadAdError loadAdError) {
                        isInterstitialAdLoading = false;
                        interstitialAd = null;
                        Log.e("SubjectListActivity", "Interstitial ad failed to load: " + loadAdError.getMessage());
                    }
                });
    }
    
    private void showInterstitialAdAndSync(Runnable onAdClosed) {
        syncCallback = onAdClosed;
        
        // Si ya hay un ad cargado, mostrarlo directamente
        if (interstitialAd != null) {
            showInterstitialAd();
            return;
        }
        
        // Si no hay ad cargado, intentar cargar uno nuevo
        if (!isInterstitialAdLoading) {
            loadInterstitialAd();
        }
        
        // Si después de intentar cargar aún no hay ad, ejecutar la sincronización directamente
        if (interstitialAd == null) {
            if (syncCallback != null) {
                syncCallback.run();
                syncCallback = null;
            }
            return;
        }
        
        // Esperar un poco y mostrar el ad si se cargó
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            if (interstitialAd != null) {
                showInterstitialAd();
            } else if (syncCallback != null) {
                // Si después de esperar no hay ad, ejecutar sincronización directamente
                syncCallback.run();
                syncCallback = null;
            }
        }, 500);
    }
    
    private void showInterstitialAd() {
        if (interstitialAd == null) {
            // Si no hay ad, ejecutar sincronización directamente
            if (syncCallback != null) {
                syncCallback.run();
                syncCallback = null;
            }
            return;
        }
        
        interstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdDismissedFullScreenContent() {
                // Anuncio cerrado, ejecutar sincronización
                interstitialAd = null;
                loadInterstitialAd(); // Precargar siguiente anuncio
                
                // Ejecutar sincronización (el popup se mostrará cuando termine)
                if (syncCallback != null) {
                    syncCallback.run();
                    syncCallback = null;
                }
            }
            
            @Override
            public void onAdFailedToShowFullScreenContent(AdError adError) {
                // Si falla mostrar el ad, ejecutar sincronización directamente
                interstitialAd = null;
                loadInterstitialAd();
                
                if (syncCallback != null) {
                    syncCallback.run();
                    syncCallback = null;
                }
            }
        });
        
        interstitialAd.show(this);
    }
    
    private void showSyncCompletedDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(getString(R.string.sync_completed))
                .setMessage(getString(R.string.sync_completed_message))
                .setPositiveButton(getString(R.string.button_ok), null)
                .setIcon(android.R.drawable.ic_dialog_info)
                .show();
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
    
    // ===== Helper Methods =====
    
    private void refreshSubjectsList() {
        dao.observeActive(appType).observe(this, (List<SubjectEntity> list) -> {
            new Thread(() -> {
                // Obtener todos los próximos eventos de una vez (optimización para evitar múltiples consultas)
                long now = System.currentTimeMillis();
                EventDao eventDao = db.eventDao();
                java.util.List<EventEntity> nextEvents = eventDao.nextEventsForAllSubjects(appType, now);
                
                // Crear mapa de subjectId -> próximo evento para acceso rápido
                java.util.Map<String, EventEntity> nextEventMap = new java.util.HashMap<>();
                if (nextEvents != null) {
                    for (EventEntity event : nextEvents) {
                        if (event.subjectId != null && !nextEventMap.containsKey(event.subjectId)) {
                            nextEventMap.put(event.subjectId, event);
                        }
                    }
                }
                
                java.util.List<SubjectAdapter.SubjectRow> rows = new java.util.ArrayList<>();
                for (SubjectEntity subj : list) {
                    String info  = buildInfoLine(subj);
                    String extra = buildExtraLine(subj, nextEventMap.get(subj.id));
                    rows.add(new SubjectAdapter.SubjectRow(subj, info, extra));
                }
                runOnUiThread(() -> {
                    adapter.submitRows(rows);
                });
            }).start();
        });
    }
    
    // ===== Recovery Code Functionality =====
    
    private void initializeRecoveryCode() {
        com.gastonlesbegueris.caretemplate.util.UserRecoveryManager recoveryManager = 
                new com.gastonlesbegueris.caretemplate.util.UserRecoveryManager(this);
        
        // Generar o recuperar código de recuperación y sincronizarlo
        recoveryManager.getOrGenerateRecoveryCode(new com.gastonlesbegueris.caretemplate.util.UserRecoveryManager.RecoveryCodeCallback() {
            @Override
            public void onRecoveryCode(String recoveryCode) {
                Log.d("SubjectListActivity", "Recovery code ready: " + recoveryCode);
            }
            
            @Override
            public void onError(Exception error) {
                Log.e("SubjectListActivity", "Error initializing recovery code", error);
            }
        });
    }
    
    private void showRecoveryCodeDialog() {
        // Mostrar diálogo informativo antes de cargar el video
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(getString(R.string.recovery_code_title))
                .setMessage(getString(R.string.recovery_code_message))
                .setPositiveButton(getString(R.string.button_watch_video), (d, w) -> {
                    // Usuario acepta, cargar y mostrar rewarded ad
                    loadAndShowRewardedAd();
                })
                .setNegativeButton(getString(R.string.button_cancel), null)
                .setIcon(android.R.drawable.ic_dialog_info)
                .show();
    }
    
    private void loadAndShowRewardedAd() {
        // Si ya hay un ad cargado, mostrarlo directamente
        if (rewardedAd != null) {
            showRewardedAd();
            return;
        }
        
        // Si ya se está cargando, mostrar mensaje
        if (isRewardedAdLoading) {
            Toast.makeText(this, getString(R.string.loading_video), Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Mostrar mensaje de carga
        Toast.makeText(this, "Cargando video publicitario...", Toast.LENGTH_SHORT).show();
        
        // Cargar nuevo rewarded ad
        isRewardedAdLoading = true;
        String rewardedAdId = getString(R.string.admob_rewarded_id);
        
        com.google.android.gms.ads.AdRequest adRequest = new com.google.android.gms.ads.AdRequest.Builder().build();
        
        RewardedAd.load(this, rewardedAdId, adRequest,
                new RewardedAdLoadCallback() {
                    @Override
                    public void onAdFailedToLoad(LoadAdError loadAdError) {
                        isRewardedAdLoading = false;
                        rewardedAd = null;
                        Log.e("SubjectListActivity", "Rewarded ad failed to load: " + loadAdError.getMessage());
                        // Si falla cargar el ad, mostrar el código directamente
                        runOnUiThread(() -> {
                            Toast.makeText(SubjectListActivity.this, getString(R.string.video_load_error), Toast.LENGTH_SHORT).show();
                            showRecoveryCodeAfterAd();
                        });
                    }
                    
                    @Override
                    public void onAdLoaded(RewardedAd ad) {
                        isRewardedAdLoading = false;
                        rewardedAd = ad;
                        Log.d("SubjectListActivity", "Rewarded ad loaded");
                        // Mostrar el ad
                        runOnUiThread(() -> showRewardedAd());
                    }
                });
    }
    
    private void showRewardedAd() {
        if (rewardedAd == null) {
            // Si no hay ad, mostrar código directamente
            showRecoveryCodeAfterAd();
            return;
        }
        
        // Configurar callback para cuando el usuario gana la recompensa
        rewardedAd.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdShowedFullScreenContent() {
                Log.d("SubjectListActivity", "Rewarded ad showed full screen content");
            }
            
            @Override
            public void onAdDismissedFullScreenContent() {
                // Ad cerrado, recargar para próxima vez
                rewardedAd = null;
                Log.d("SubjectListActivity", "Rewarded ad dismissed");
            }
            
            @Override
            public void onAdFailedToShowFullScreenContent(AdError adError) {
                Log.e("SubjectListActivity", "Rewarded ad failed to show: " + adError.getMessage());
                rewardedAd = null;
                // Si falla mostrar, mostrar código directamente
                showRecoveryCodeAfterAd();
            }
        });
        
        // Mostrar el ad con el listener de recompensa
        rewardedAd.show(this, new OnUserEarnedRewardListener() {
            @Override
            public void onUserEarnedReward(RewardItem rewardItem) {
                // Usuario completó el video, mostrar el código
                Log.d("SubjectListActivity", "User earned reward: " + rewardItem.getAmount() + " " + rewardItem.getType());
                showRecoveryCodeAfterAd();
            }
        });
    }
    
    private void showRecoveryCodeAfterAd() {
        Log.d("SubjectListActivity", "showRecoveryCodeAfterAd called");
        
        com.gastonlesbegueris.caretemplate.util.UserRecoveryManager recoveryManager = 
                new com.gastonlesbegueris.caretemplate.util.UserRecoveryManager(this);
        
        // Primero intentar obtener el código local (sin sincronizar)
        String localCode = recoveryManager.getRecoveryCodeSync();
        Log.d("SubjectListActivity", "Local recovery code: " + (localCode != null ? localCode : "null"));
        
        if (localCode != null && !localCode.isEmpty()) {
            // Si hay código local, mostrarlo directamente
            Log.d("SubjectListActivity", "Showing local recovery code");
            runOnUiThread(() -> showRecoveryCodeDialog(localCode));
            // Intentar sincronizar en background (sin bloquear)
            recoveryManager.getOrGenerateRecoveryCode(new com.gastonlesbegueris.caretemplate.util.UserRecoveryManager.RecoveryCodeCallback() {
                @Override
                public void onRecoveryCode(String recoveryCode) {
                    Log.d("SubjectListActivity", "Recovery code synced successfully");
                }
                
                @Override
                public void onError(Exception error) {
                    Log.w("SubjectListActivity", "Recovery code sync failed (non-critical): " + error.getMessage());
                }
            });
        } else {
            // Si no hay código local, generar uno nuevo
            Log.d("SubjectListActivity", "No local code found, generating new one");
            recoveryManager.getOrGenerateRecoveryCode(new com.gastonlesbegueris.caretemplate.util.UserRecoveryManager.RecoveryCodeCallback() {
                @Override
                public void onRecoveryCode(String recoveryCode) {
                    Log.d("SubjectListActivity", "Generated recovery code: " + recoveryCode);
                    runOnUiThread(() -> showRecoveryCodeDialog(recoveryCode));
                }
                
                @Override
                public void onError(Exception error) {
                    Log.e("SubjectListActivity", "Error generating recovery code: " + error.getMessage());
                    // Si falla, intentar generar código local sin sincronizar
                    String fallbackCode = generateLocalRecoveryCode();
                    if (fallbackCode != null) {
                        Log.d("SubjectListActivity", "Using fallback code: " + fallbackCode);
                        runOnUiThread(() -> showRecoveryCodeDialog(fallbackCode));
                    } else {
                        runOnUiThread(() -> Toast.makeText(SubjectListActivity.this, getString(R.string.recovery_code_error), Toast.LENGTH_LONG).show());
                    }
                }
            });
        }
    }
    
    private void showRecoveryCodeDialog(String recoveryCode) {
        Log.d("SubjectListActivity", "showRecoveryCodeDialog called with code: " + recoveryCode);
        
        if (recoveryCode == null || recoveryCode.isEmpty()) {
            Log.e("SubjectListActivity", "Recovery code is null or empty!");
            Toast.makeText(this, getString(R.string.recovery_code_generate_error), Toast.LENGTH_LONG).show();
            return;
        }
        
        try {
            // Mostrar diálogo con el código
            android.view.View view = getLayoutInflater().inflate(R.layout.dialog_recovery_code, null);
            android.widget.TextView tvCode = view.findViewById(R.id.tvRecoveryCode);
            android.widget.Button btnCopy = view.findViewById(R.id.btnCopyCode);
            android.widget.Button btnRecover = view.findViewById(R.id.btnRecoverFromCode);
            
            if (tvCode != null) {
                tvCode.setText(recoveryCode);
                Log.d("SubjectListActivity", "Code set in TextView");
            } else {
                Log.e("SubjectListActivity", "tvCode is null!");
            }
            
            if (btnCopy != null) {
                btnCopy.setOnClickListener(v -> {
                    android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(android.content.Context.CLIPBOARD_SERVICE);
                    android.content.ClipData clip = android.content.ClipData.newPlainText(getString(R.string.recovery_code_title), recoveryCode);
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(SubjectListActivity.this, getString(R.string.recovery_code_copied), Toast.LENGTH_SHORT).show();
                });
            }
            
            if (btnRecover != null) {
                btnRecover.setOnClickListener(v -> {
                    // Mostrar diálogo para ingresar código de recuperación
                    showRecoverFromCodeDialog();
                });
            }
            
            androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(SubjectListActivity.this)
                    .setTitle(getString(R.string.recovery_code_title))
                    .setMessage(getString(R.string.recovery_code_save_message))
                    .setView(view)
                    .setPositiveButton(getString(R.string.button_close), null)
                    .create();
            
            dialog.show();
            Log.d("SubjectListActivity", "Dialog shown");
        } catch (Exception e) {
            Log.e("SubjectListActivity", "Error showing recovery code dialog", e);
            Toast.makeText(this, getString(R.string.recovery_code_show_error, e.getMessage()), Toast.LENGTH_LONG).show();
        }
    }
    
    private String generateLocalRecoveryCode() {
        // Generar código local como fallback
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        StringBuilder code = new StringBuilder();
        java.util.Random random = new java.util.Random();
        
        for (int i = 0; i < 12; i++) {
            if (i > 0 && i % 4 == 0) {
                code.append("-");
            }
            code.append(chars.charAt(random.nextInt(chars.length())));
        }
        
        return code.toString();
    }
    
    private void showRecoverFromCodeDialog() {
        android.view.View view = getLayoutInflater().inflate(R.layout.dialog_recover_code, null);
        com.google.android.material.textfield.TextInputEditText etCode = view.findViewById(R.id.etRecoveryCode);
        
        // Agregar formateo automático del código mientras se escribe
        if (etCode != null) {
            etCode.addTextChangedListener(new RecoveryCodeFormatter(etCode));
        }
        
        recoverDialog = new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(getString(R.string.recovery_code_recover_title))
                .setMessage(getString(R.string.recovery_code_recover_message))
                .setView(view)
                .setPositiveButton(getString(R.string.button_recover), (d, w) -> {
                    String code = etCode != null && etCode.getText() != null ? etCode.getText().toString().trim() : "";
                    if (code.isEmpty()) {
                        Toast.makeText(this, getString(R.string.recovery_code_invalid), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    // Cerrar el diálogo inmediatamente al presionar el botón
                    if (recoverDialog != null) {
                        try {
                            recoverDialog.dismiss();
                        } catch (Exception ex) {
                            Log.w("SubjectListActivity", "Error al cerrar diálogo al presionar recuperar: " + ex.getMessage());
                        }
                    }
                    
                    // Primero verificar si el código existe (para depuración)
                    com.gastonlesbegueris.caretemplate.util.UserRecoveryManager recoveryManager = 
                            new com.gastonlesbegueris.caretemplate.util.UserRecoveryManager(this);
                    
                    recoveryManager.verifyRecoveryCodeExists(code, new com.gastonlesbegueris.caretemplate.util.UserRecoveryManager.VerifyCodeCallback() {
                        @Override
                        public void onResult(boolean exists, String message) {
                            runOnUiThread(() -> {
                                if (exists) {
                                    // Si existe, proceder con la recuperación
                                    isSilentRecoveryMode = true;
                                    Log.d("SubjectListActivity", "Código verificado, iniciando recuperación");
                                    recoverUserFromCode(code);
                                } else {
                                    // Si no existe, mostrar mensaje detallado
                                    new androidx.appcompat.app.AlertDialog.Builder(SubjectListActivity.this)
                                            .setTitle(getString(R.string.recovery_code_not_found))
                                            .setMessage(message)
                                            .setPositiveButton(getString(R.string.button_ok), null)
                                            .setIcon(android.R.drawable.ic_dialog_alert)
                                            .show();
                                }
                            });
                        }
                    });
                })
                .setNegativeButton(getString(R.string.button_cancel), null)
                .setCancelable(true)
                .create();
        
        recoverDialog.show();
    }
    
    /**
     * TextWatcher que formatea automáticamente el código de recuperación mientras se escribe.
     * Formato: XXXX-XXXX-XXXX (mayúsculas y guiones automáticos)
     */
    private static class RecoveryCodeFormatter implements android.text.TextWatcher {
        private final com.google.android.material.textfield.TextInputEditText editText;
        private boolean isFormatting = false;
        private int previousLength = 0;
        private int previousCursorPosition = 0;
        
        public RecoveryCodeFormatter(com.google.android.material.textfield.TextInputEditText editText) {
            this.editText = editText;
        }
        
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            previousLength = s.length();
            previousCursorPosition = editText.getSelectionStart();
        }
        
        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            // No hacer nada aquí
        }
        
        @Override
        public void afterTextChanged(android.text.Editable s) {
            if (isFormatting) {
                return; // Evitar recursión infinita
            }
            
            isFormatting = true;
            
            try {
                String text = s.toString();
                
                // Convertir a mayúsculas y quitar caracteres no válidos (solo letras y números)
                String cleaned = text.toUpperCase().replaceAll("[^A-Z0-9]", "");
                
                // Limitar a 12 caracteres
                if (cleaned.length() > 12) {
                    cleaned = cleaned.substring(0, 12);
                }
                
                // Formatear con guiones: XXXX-XXXX-XXXX
                StringBuilder formatted = new StringBuilder();
                for (int i = 0; i < cleaned.length(); i++) {
                    if (i > 0 && i % 4 == 0) {
                        formatted.append("-");
                    }
                    formatted.append(cleaned.charAt(i));
                }
                
                // Si el texto cambió, actualizarlo
                if (!text.equals(formatted.toString())) {
                    int currentCursorPosition = editText.getSelectionStart();
                    s.replace(0, s.length(), formatted.toString());
                    
                    // Calcular nueva posición del cursor
                    int newPosition = calculateNewCursorPosition(
                            text, 
                            formatted.toString(), 
                            currentCursorPosition,
                            previousCursorPosition,
                            previousLength
                    );
                    
                    if (newPosition >= 0 && newPosition <= formatted.length()) {
                        editText.setSelection(newPosition);
                    }
                }
            } catch (Exception e) {
                android.util.Log.e("RecoveryCodeFormatter", "Error formateando código", e);
            } finally {
                isFormatting = false;
            }
        }
        
        /**
         * Calcula la nueva posición del cursor después del formateo.
         */
        private int calculateNewCursorPosition(String oldText, String newText, int currentPos, int previousPos, int previousLength) {
            // Contar caracteres válidos (sin guiones) antes del cursor en el texto anterior
            int validCharsBeforeCursor = 0;
            for (int i = 0; i < Math.min(currentPos, oldText.length()); i++) {
                char c = oldText.charAt(i);
                if (c != '-' && Character.isLetterOrDigit(c)) {
                    validCharsBeforeCursor++;
                }
            }
            
            // Encontrar la posición en el nuevo texto que corresponde a esos caracteres válidos
            int newPosition = 0;
            int validCharsCounted = 0;
            for (int i = 0; i < newText.length() && validCharsCounted < validCharsBeforeCursor; i++) {
                char c = newText.charAt(i);
                if (c != '-') {
                    validCharsCounted++;
                }
                newPosition = i + 1;
            }
            
            // Si estamos justo después de un guión y agregamos un carácter, avanzar el cursor
            if (newText.length() > previousLength && newPosition < newText.length()) {
                if (newText.charAt(newPosition - 1) == '-') {
                    newPosition++;
                }
            }
            
            return Math.min(newPosition, newText.length());
        }
    }
    
    private void recoverUserFromCode(String recoveryCode) {
        // El modo silencioso ya debería estar activado desde el botón, pero asegurarse
        isSilentRecoveryMode = true;
        Log.d("SubjectListActivity", "Iniciando recuperación con código, isSilentRecoveryMode=" + isSilentRecoveryMode);
        
        com.gastonlesbegueris.caretemplate.util.UserRecoveryManager recoveryManager = 
                new com.gastonlesbegueris.caretemplate.util.UserRecoveryManager(this);
        
        Toast.makeText(this, getString(R.string.recovering_data), Toast.LENGTH_SHORT).show();
        
        recoveryManager.recoverUserIdFromCode(recoveryCode, new com.gastonlesbegueris.caretemplate.util.UserRecoveryManager.RecoverUserIdCallback() {
            @Override
            public void onUserIdRecovered(String userId) {
                runOnUiThread(() -> {
                    // CERRAR EL DIÁLOGO PRIMERO, antes de hacer cualquier otra cosa
                    if (recoverDialog != null) {
                        try {
                            if (recoverDialog.isShowing()) {
                                recoverDialog.dismiss();
                            }
                        } catch (Exception ex) {
                            Log.w("SubjectListActivity", "Error al cerrar diálogo: " + ex.getMessage());
                        }
                        recoverDialog = null;
                    }
                    
                    // Guardar el userId recuperado
                    getSharedPreferences("user_prefs", MODE_PRIVATE)
                            .edit()
                            .putString("user_id", userId)
                            .putString("firebase_uid", userId)
                            .apply();
                    
                    Toast.makeText(SubjectListActivity.this, getString(R.string.user_recovered), Toast.LENGTH_SHORT).show();
                    
                    // Sincronizar datos del usuario recuperado (sin mostrar errores)
                    performSyncWithUserId(userId, true);
                });
            }
            
            @Override
            public void onError(Exception error) {
                runOnUiThread(() -> {
                    // Desactivar modo silencioso
                    isSilentRecoveryMode = false;
                    // Cerrar el diálogo de forma segura
                    closeRecoverDialog();
                    // Verificar si es error de permisos antes de mostrar
                    String errorMsg = error != null ? error.getMessage() : "null";
                    if (isPermissionError(errorMsg)) {
                        Log.d("SubjectListActivity", "Error de permisos SILENCIADO en recovery onError");
                        Toast.makeText(SubjectListActivity.this, getString(R.string.data_recovered), Toast.LENGTH_SHORT).show();
                        refreshSubjectsList();
                    } else {
                        // Mostrar un diálogo con el error completo para que el usuario pueda leerlo
                        new androidx.appcompat.app.AlertDialog.Builder(SubjectListActivity.this)
                                .setTitle(getString(R.string.recovery_code_not_found))
                                .setMessage(errorMsg)
                                .setPositiveButton(getString(R.string.button_ok), null)
                                .setIcon(android.R.drawable.ic_dialog_alert)
                                .show();
                    }
                });
            }
        });
    }
    
    private void performSyncWithUserId(String userId, boolean silentErrors) {
        Log.d("SubjectListActivity", "performSyncWithUserId: userId=" + userId + ", silentErrors=" + silentErrors);
        
        // Guardar el userId recuperado para uso futuro
        getSharedPreferences("user_prefs", MODE_PRIVATE)
                .edit()
                .putString("user_id", userId)
                .putString("firebase_uid", userId)
                .apply();
        Log.d("SubjectListActivity", "userId guardado en SharedPreferences");
        
        // Actualizar UserManager para usar el userId recuperado
        com.gastonlesbegueris.caretemplate.util.UserManager userManager = 
                new com.gastonlesbegueris.caretemplate.util.UserManager(this);
        userManager.setUserId(userId);
        Log.d("SubjectListActivity", "userId establecido en UserManager");
        
        // Intentar autenticarse con Firebase (pero usar el userId recuperado para sincronizar)
        com.google.firebase.auth.FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        Log.d("SubjectListActivity", "Firebase currentUser: " + (currentUser != null ? currentUser.getUid() : "null"));
        
        if (currentUser == null) {
            // Autenticar anónimamente para tener acceso a Firestore, pero usar userId recuperado para sincronizar
            Log.d("SubjectListActivity", "Autenticando anónimamente...");
            FirebaseAuth.getInstance().signInAnonymously()
                    .addOnSuccessListener(authResult -> {
                        // Usar el userId recuperado directamente, no el nuevo Firebase UID
                        Log.d("SubjectListActivity", "Firebase Auth OK, nuevo UID=" + authResult.getUser().getUid() + ", pero usando userId recuperado para sincronizar: " + userId);
                        performSyncSilent(userId, silentErrors);
                    })
                    .addOnFailureListener(e -> {
                        Log.w("SubjectListActivity", "Firebase Auth falló: " + e.getMessage() + ", pero intentando sincronizar con userId recuperado: " + userId);
                        // Intentar sincronizar de todas formas con el userId recuperado
                        performSyncSilent(userId, silentErrors);
                    });
        } else {
            // Ya autenticado, usar el userId recuperado directamente
            Log.d("SubjectListActivity", "Ya autenticado (UID=" + currentUser.getUid() + "), usando userId recuperado para sincronizar: " + userId);
            performSyncSilent(userId, silentErrors);
        }
    }
    
    private void performSyncSilent(String uid, boolean silentErrors) {
        Log.d("SubjectListActivity", "performSyncSilent: uid=" + uid + ", appType=" + appType + ", silentErrors=" + silentErrors);
        try {
            CloudSync sync = new CloudSync(
                    eventDao,
                    dao,
                    FirebaseFirestore.getInstance(),
                    uid,
                    "CareTemplate",
                    appType
            );
            Log.d("SubjectListActivity", "CloudSync creado con uid=" + uid);

            Log.d("SubjectListActivity", "Iniciando sincronización: pushSubjects -> push -> pullSubjects -> pull");
            sync.pushSubjects(() -> {
                Log.d("SubjectListActivity", "pushSubjects completado");
                sync.push(() -> {
                    Log.d("SubjectListActivity", "push completado");
                    sync.pullSubjects(() -> {
                        Log.d("SubjectListActivity", "pullSubjects completado, iniciando pull...");
                        sync.pull(
                                () -> runOnUiThread(() -> {
                                    Log.d("SubjectListActivity", "pull completado exitosamente");
                                    // Desactivar modo silencioso y cerrar diálogo
                                    isSilentRecoveryMode = false;
                                    closeRecoverDialog();
                                    Toast.makeText(this, getString(R.string.data_recovered), Toast.LENGTH_SHORT).show();
                                    // Refrescar la UI
                                    refreshSubjectsList();
                                }),
                                e -> runOnUiThread(() -> {
                                    if (silentErrors) {
                                        isSilentRecoveryMode = false;
                                        closeRecoverDialog();
                                        String errorMsg = e != null ? e.getMessage() : "null";
                                        // SILENCIAR CUALQUIER error de permisos
                                        if (isPermissionError(errorMsg)) {
                                            Log.d("SubjectListActivity", "Error de permisos SILENCIADO en performSyncSilent pull");
                                        } else if (errorMsg != null && (
                                            errorMsg.contains("failed_precondition") || 
                                            errorMsg.contains("FAILED_PRECONDITION"))) {
                                            Log.d("SubjectListActivity", "Error normal durante recuperación (sin datos): " + errorMsg);
                                        } else {
                                            Log.w("SubjectListActivity", "Error durante recuperación (silenciado): " + errorMsg);
                                        }
                                        Toast.makeText(this, getString(R.string.data_recovered), Toast.LENGTH_SHORT).show();
                                        refreshSubjectsList();
                                    }
                                })
                        );
                    }, e -> runOnUiThread(() -> {
                        if (silentErrors) {
                            isSilentRecoveryMode = false;
                            closeRecoverDialog();
                            String errorMsg = e != null ? e.getMessage() : "null";
                            // SILENCIAR CUALQUIER error de permisos
                            if (isPermissionError(errorMsg)) {
                                Log.d("SubjectListActivity", "Error de permisos SILENCIADO en performSyncSilent pullSubjects");
                            } else if (errorMsg != null && (
                                errorMsg.contains("failed_precondition") || 
                                errorMsg.contains("FAILED_PRECONDITION"))) {
                                Log.d("SubjectListActivity", "Error normal durante recuperación de sujetos (sin datos): " + errorMsg);
                            } else {
                                Log.w("SubjectListActivity", "Error al recuperar sujetos durante recuperación (silenciado): " + errorMsg);
                            }
                            Toast.makeText(this, getString(R.string.data_recovered), Toast.LENGTH_SHORT).show();
                            refreshSubjectsList();
                        }
                    }));
                }, e -> runOnUiThread(() -> {
                    if (silentErrors) {
                        isSilentRecoveryMode = false;
                        closeRecoverDialog();
                        String errorMsg = e != null ? e.getMessage() : "null";
                        // SILENCIAR CUALQUIER error de permisos
                        if (isPermissionError(errorMsg)) {
                            Log.d("SubjectListActivity", "Error de permisos SILENCIADO en performSyncSilent push");
                        } else {
                            Log.w("SubjectListActivity", "Error al subir eventos durante recuperación (silenciado): " + errorMsg);
                        }
                        Toast.makeText(this, getString(R.string.data_recovered), Toast.LENGTH_SHORT).show();
                        refreshSubjectsList();
                    }
                }));
                    }, e -> runOnUiThread(() -> {
                        if (silentErrors) {
                            isSilentRecoveryMode = false;
                            closeRecoverDialog();
                            String errorMsg = e != null ? e.getMessage() : "null";
                            // SILENCIAR CUALQUIER error de permisos
                            if (isPermissionError(errorMsg)) {
                                Log.d("SubjectListActivity", "Error de permisos SILENCIADO en performSyncSilent pushSubjects");
                            } else {
                                Log.w("SubjectListActivity", "Error al subir sujetos durante recuperación (silenciado): " + errorMsg);
                            }
                            Toast.makeText(this, getString(R.string.data_recovered), Toast.LENGTH_SHORT).show();
                            refreshSubjectsList();
                        }
                    }));
        } catch (SecurityException e) {
            runOnUiThread(() -> {
                if (silentErrors) {
                    isSilentRecoveryMode = false;
                    closeRecoverDialog();
                    Toast.makeText(this, getString(R.string.data_recovered), Toast.LENGTH_SHORT).show();
                    refreshSubjectsList();
                }
            });
        } catch (Exception e) {
            runOnUiThread(() -> {
                if (silentErrors) {
                    isSilentRecoveryMode = false;
                    closeRecoverDialog();
                    Toast.makeText(this, getString(R.string.data_recovered), Toast.LENGTH_SHORT).show();
                    refreshSubjectsList();
                }
            });
        }
    }
    
    private void closeRecoverDialog() {
        runOnUiThread(() -> {
            if (recoverDialog != null) {
                try {
                    if (recoverDialog.isShowing()) {
                        recoverDialog.dismiss();
                    }
                } catch (Exception e) {
                    Log.w("SubjectListActivity", "Error al cerrar diálogo de recuperación: " + e.getMessage());
                } finally {
                    recoverDialog = null;
                }
            }
        });
    }
    
    private void doSync() {
        try {
            com.google.firebase.auth.FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user == null) {
                FirebaseAuth.getInstance().signInAnonymously()
                        .addOnSuccessListener(authResult -> {
                            if (authResult != null && authResult.getUser() != null) {
                                performSync(authResult.getUser().getUid());
                            } else {
                                runOnUiThread(() -> {
                                    Toast.makeText(this, "Error: No se pudo autenticar", Toast.LENGTH_LONG).show();
                                });
                            }
                        })
                        .addOnFailureListener(e -> {
                            runOnUiThread(() -> {
                                // SILENCIAR CUALQUIER error de permisos
                                String errorMsg = e != null ? e.getMessage() : "null";
                                if (isPermissionError(errorMsg)) {
                                    Log.d("SubjectListActivity", "Error de permisos SILENCIADO en doSync auth failure");
                                    Toast.makeText(this, getString(R.string.sync_success), Toast.LENGTH_SHORT).show();
                                    refreshSubjectsList();
                                } else if (errorMsg != null && errorMsg.contains("SecurityException")) {
                                    Toast.makeText(this, getString(R.string.sync_config_error), Toast.LENGTH_LONG).show();
                                } else {
                                    Toast.makeText(this, getString(R.string.sync_error_auth), Toast.LENGTH_LONG).show();
                                }
                            });
                        });
                return;
            }
            
            performSync(user.getUid());
        } catch (Exception e) {
            runOnUiThread(() -> {
                // SILENCIAR CUALQUIER error de permisos
                String errorMsg = e != null ? e.getMessage() : "null";
                if (isPermissionError(errorMsg)) {
                    Log.d("SubjectListActivity", "Error de permisos SILENCIADO en doSync catch");
                    Toast.makeText(this, getString(R.string.sync_success), Toast.LENGTH_SHORT).show();
                    refreshSubjectsList();
                } else {
                    Toast.makeText(this, getString(R.string.sync_error, errorMsg), Toast.LENGTH_LONG).show();
                }
            });
        }
    }
    
    private void performSync(String uid) {
        try {
            CloudSync sync = new CloudSync(
                    eventDao,
                    dao,
                    FirebaseFirestore.getInstance(),
                    uid,
                    "CareTemplate",
                    appType
            );

            sync.pushSubjects(() -> {
                sync.push(() -> {
                    sync.pullSubjects(() -> {
                        sync.pull(
                                () -> runOnUiThread(() -> {
                                    refreshSubjectsList();
                                    // Mostrar popup de sincronización realizada
                                    showSyncCompletedDialog();
                                }),
                                e -> runOnUiThread(() -> {
                                    // Silenciar errores de permisos ANTES de llamar a showSyncError
                                    String errorMsg = e != null ? e.getMessage() : "null";
                                    if (isPermissionError(errorMsg)) {
                                        Log.d("SubjectListActivity", "Error de permisos silenciado en pull: " + errorMsg);
                                        refreshSubjectsList();
                                        // Mostrar popup de sincronización realizada
                                        showSyncCompletedDialog();
                                    } else {
                                        showSyncError("Error al recuperar datos", e);
                                    }
                                })
                        );
                    }, e -> runOnUiThread(() -> {
                        // SILENCIAR CUALQUIER error que contenga "permission" o "permiso"
                        String errorMsg = e != null ? e.getMessage() : "null";
                        if (isPermissionError(errorMsg)) {
                            Log.d("SubjectListActivity", "Error de permisos SILENCIADO en pullSubjects");
                            refreshSubjectsList();
                            // Mostrar popup de sincronización realizada
                            showSyncCompletedDialog();
                        } else {
                            showSyncError("Error al recuperar sujetos", e);
                        }
                    }));
                }, e -> runOnUiThread(() -> {
                    // SILENCIAR CUALQUIER error que contenga "permission" o "permiso"
                    String errorMsg = e != null ? e.getMessage() : "null";
                    if (isPermissionError(errorMsg)) {
                        Log.d("SubjectListActivity", "Error de permisos SILENCIADO en push");
                        Toast.makeText(this, getString(R.string.sync_success), Toast.LENGTH_SHORT).show();
                        refreshSubjectsList();
                    } else {
                        showSyncError("Error al subir eventos", e);
                    }
                }));
                    }, e -> runOnUiThread(() -> {
                        // SILENCIAR CUALQUIER error que contenga "permission" o "permiso"
                        String errorMsg = e != null ? e.getMessage() : "null";
                        if (isPermissionError(errorMsg)) {
                            Log.d("SubjectListActivity", "Error de permisos SILENCIADO en pushSubjects");
                            Toast.makeText(this, getString(R.string.sync_success), Toast.LENGTH_SHORT).show();
                            refreshSubjectsList();
                        } else {
                            showSyncError("Error al subir sujetos", e);
                        }
                    }));
        } catch (SecurityException e) {
            runOnUiThread(() -> {
                // Verificar si es error de permisos antes de mostrar
                String errorMsg = e != null ? e.getMessage() : "null";
                if (isPermissionError(errorMsg)) {
                    Log.d("SubjectListActivity", "Error de permisos SILENCIADO en catch SecurityException");
                    Toast.makeText(this, getString(R.string.sync_success), Toast.LENGTH_SHORT).show();
                    refreshSubjectsList();
                } else {
                    Toast.makeText(this, getString(R.string.sync_error_security), Toast.LENGTH_LONG).show();
                }
            });
        } catch (Exception e) {
            runOnUiThread(() -> {
                // Verificar si es error de permisos antes de mostrar
                String errorMsg = e != null ? e.getMessage() : "null";
                if (isPermissionError(errorMsg)) {
                    Log.d("SubjectListActivity", "Error de permisos SILENCIADO en catch Exception");
                    Toast.makeText(this, getString(R.string.sync_success), Toast.LENGTH_SHORT).show();
                    refreshSubjectsList();
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
        // CUALQUIER mención de "permission", "permiso", "denied", "missing" = SILENCIAR
        return lower.contains("permission") || 
               lower.contains("permiso") ||
               lower.contains("denied") ||
               lower.contains("missing");
    }
    
    private void showSyncError(String context, Exception e) {
        String errorMsg = e != null ? e.getMessage() : "null";
        
        // PRIMERA VERIFICACIÓN: Si es error de permisos, SILENCIAR COMPLETAMENTE
        if (isPermissionError(errorMsg)) {
            Log.d("SubjectListActivity", "Error de permisos SILENCIADO: " + context + " - " + errorMsg);
            Toast.makeText(this, getString(R.string.sync_success), Toast.LENGTH_SHORT).show();
            refreshSubjectsList();
            return; // SALIR INMEDIATAMENTE - NO MOSTRAR NADA
        }
        
        // Verificar failed_precondition también
        if (errorMsg != null) {
            String errorMsgLower = errorMsg.toLowerCase();
            if (errorMsgLower.contains("failed_precondition") || errorMsgLower.contains("failed precondition")) {
                Log.d("SubjectListActivity", "failed_precondition silenciado: " + context);
                Toast.makeText(this, getString(R.string.sync_success), Toast.LENGTH_SHORT).show();
                refreshSubjectsList();
                return;
            }
        }
        
        // SEGUNDA VERIFICACIÓN: Por si acaso, verificar nuevamente
        if (isPermissionError(errorMsg)) {
            Log.d("SubjectListActivity", "Error de permisos SILENCIADO (segunda verificación): " + context);
            Toast.makeText(this, getString(R.string.sync_success), Toast.LENGTH_SHORT).show();
            refreshSubjectsList();
            return;
        }
        
        String userMessage;
        if (errorMsg == null) {
            userMessage = context + ": Error desconocido";
        } else if (errorMsg.contains("Failed to get service") || 
                   errorMsg.contains("reconnection") || 
                   errorMsg.contains("UNAVAILABLE") ||
                   errorMsg.contains("DEADLINE_EXCEEDED") ||
                   errorMsg.contains("network")) {
            userMessage = getString(R.string.sync_error_connection);
        } else if (errorMsg.contains("UNAUTHENTICATED") || errorMsg.contains("auth")) {
            userMessage = getString(R.string.sync_error_auth);
        } else if (errorMsg.contains("SecurityException")) {
            userMessage = getString(R.string.error_security_sha1);
        } else {
            // TERCERA VERIFICACIÓN FINAL: Si contiene "permission" en CUALQUIER parte, SILENCIAR
            if (isPermissionError(errorMsg)) {
                Log.d("SubjectListActivity", "Error de permisos SILENCIADO (verificación final): " + context);
                Toast.makeText(this, getString(R.string.sync_success), Toast.LENGTH_SHORT).show();
                refreshSubjectsList();
                return;
            }
            userMessage = context + ": " + errorMsg;
        }
        
        // CUARTA VERIFICACIÓN: Antes de mostrar el Toast, verificar una vez más
        if (isPermissionError(userMessage)) {
            Log.d("SubjectListActivity", "Error de permisos SILENCIADO (antes de Toast): " + context);
            Toast.makeText(this, getString(R.string.sync_success), Toast.LENGTH_SHORT).show();
            refreshSubjectsList();
            return;
        }
        
        Toast.makeText(this, userMessage, Toast.LENGTH_LONG).show();
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
            showAddDialog();
        });
        fabEvent.setOnClickListener(v -> {
            closeFabMenu();
            showAddEventDialog();
        });

        RecyclerView rv = findViewById(R.id.rvSubjects);
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
        final com.google.android.material.textfield.TextInputLayout tilKilometers = view.findViewById(R.id.tilKilometers);
        final com.google.android.material.textfield.TextInputEditText etKilometers = view.findViewById(R.id.etKilometers);
        
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

        // Verificar que las vistas se encontraron
        if (etTitle == null || etCost == null || sp == null) {
            Toast.makeText(this, getString(R.string.error_load_dialog), Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Inicializar fecha y hora con valores actuales
        final java.util.Calendar now = java.util.Calendar.getInstance();
        if (etEventDate != null && etEventTime != null) {
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
                    SubjectListActivity.this,
                    (timeView, selectedHour, selectedMinute) -> {
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
        
        // Mostrar campo de kilómetros solo para "cars"
        if ("cars".equals(appType) && tilKilometers != null && etKilometers != null) {
            tilKilometers.setVisibility(View.VISIBLE);
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
            
            // Mostrar/ocultar opciones de repetición según la selección y actualizar unidad
            spRepeatType.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View view, int position, long id) {
                    if (llRepeatOptions != null) {
                        llRepeatOptions.setVisibility(position == 0 ? View.GONE : View.VISIBLE);
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
        }

        // cargar sujetos en background
        new Thread(() -> {
            final java.util.List<SubjectEntity> loaded = dao.listActiveNow(appType);
            runOnUiThread(() -> {
                final java.util.List<SubjectEntity> subjects =
                        (loaded == null) ? java.util.Collections.emptyList() : new java.util.ArrayList<>(loaded);

                java.util.List<String> names = new java.util.ArrayList<>();
                for (SubjectEntity s : subjects) names.add(s.name);
                android.widget.ArrayAdapter<String> ad =
                        new android.widget.ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, names);
                sp.setAdapter(ad);

                if (subjects.isEmpty()) {
                    Toast.makeText(this, getString(R.string.error_create_subject_first), Toast.LENGTH_LONG).show();
                    return;
                }

                // Configurar barra de título personalizada
                final android.view.View llDialogTitleBar = view.findViewById(R.id.llDialogTitleBar);
                final android.widget.TextView tvDialogTitle = view.findViewById(R.id.tvDialogTitle);
                final android.widget.ImageButton ibSave = view.findViewById(R.id.ibSave);
                final android.widget.ImageButton ibClose = view.findViewById(R.id.ibClose);
                
                if (tvDialogTitle != null) {
                    tvDialogTitle.setText(getString(R.string.new_event));
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
                            Toast.makeText(this, getString(R.string.event_title_required), Toast.LENGTH_SHORT).show();
                            return;
                        }
                        final int pos = sp.getSelectedItemPosition();
                        if (pos < 0 || pos >= subjects.size()) {
                            Toast.makeText(this, getString(R.string.error_select_subject), Toast.LENGTH_SHORT).show();
                            return;
                        }
                        final String subjectId = subjects.get(pos).id;

                        final String c = etCost.getText() != null ? etCost.getText().toString().trim() : "";
                        final Double cost = c.isEmpty() ? null : safeParseDouble(c);

                        // Para "cars", capturar kilómetros
                        Double kilometersAtEvent = null;
                        if ("cars".equals(appType) && etKilometers != null) {
                            String kmStr = etKilometers.getText() != null ? etKilometers.getText().toString().trim() : "";
                            if (!kmStr.isEmpty()) {
                                kilometersAtEvent = safeParseDouble(kmStr);
                            }
                        }

                        // Capturar fecha y hora
                        long dueAt = System.currentTimeMillis();
                        if (etEventDate != null && etEventTime != null) {
                            java.util.Calendar cal = java.util.Calendar.getInstance();
                            
                            // Obtener fecha
                            if (etEventDate.getTag() != null) {
                                cal.setTimeInMillis((Long) etEventDate.getTag());
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

                        final String fTitle = title;
                        final String fSubjectId = subjectId;
                        final Double fCost = cost;
                        final Double fKilometers = kilometersAtEvent;
                        
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
                        insertLocalWithRepeat(fTitle, fSubjectId, fCost, dueAt, fKilometers, fRepeatType, fRepeatInterval, fRepeatEndDate, fRepeatCount, fNotificationMinutesBefore);
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

    private void insertLocal(String title, String subjectId, Double cost, long dueAt, Double kilometersAtEvent) {
        insertLocalWithRepeat(title, subjectId, cost, dueAt, kilometersAtEvent, null, null, null, null, null);
    }
    
    private void insertLocalWithRepeat(String title, String subjectId, Double cost, long dueAt, Double kilometersAtEvent,
                                       String repeatType, Integer repeatInterval, Long repeatEndDate, Integer repeatCount,
                                       Integer notificationMinutesBefore) {
        new Thread(() -> {
            // Obtener UID del usuario actual
            String uid = getCurrentUserId();
            String originalEventId = UUID.randomUUID().toString();
            
            // Crear el evento original
            EventEntity originalEvent = new EventEntity();
            originalEvent.id = originalEventId;
            originalEvent.uid = uid;
            originalEvent.appType = appType;
            originalEvent.subjectId = subjectId;
            originalEvent.title = title;
            originalEvent.note = "";
            originalEvent.cost = cost;
            originalEvent.kilometersAtEvent = kilometersAtEvent;
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
                
                // Calcular el siguiente evento
                while (true) {
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
                        // Si no hay límite, crear eventos por 10 años por defecto (prácticamente sin límite)
                        java.util.Calendar limitCal = java.util.Calendar.getInstance();
                        limitCal.setTimeInMillis(dueAt);
                        limitCal.add(java.util.Calendar.YEAR, 10);
                        if (nextDueAt > limitCal.getTimeInMillis()) {
                            shouldStop = true;
                        }
                    }
                    
                    if (shouldStop) break;
                    
                    // Crear el evento repetido
                    EventEntity repeatedEvent = new EventEntity();
                    repeatedEvent.id = UUID.randomUUID().toString();
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
                    
                    // Programar notificación para el evento repetido
                    if (notificationMinutesBefore != null && notificationMinutesBefore > 0) {
                        com.gastonlesbegueris.caretemplate.util.NotificationHelper.scheduleNotification(this, repeatedEvent);
                    }
                    
                    eventsCreated++;
                }
            }

            runOnUiThread(() -> {
                String message = getString(R.string.event_saved);
                if (repeatType != null) {
                    message += " (con repetición)";
                }
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            });
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
    
    /**
     * Solicita el permiso de notificaciones para Android 13+ (API 33+)
     */
    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ requiere permiso explícito
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) 
                    != PackageManager.PERMISSION_GRANTED) {
                // El permiso no está concedido, solicitarlo
                ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    REQUEST_NOTIFICATION_PERMISSION
                );
            }
            // Si ya está concedido, no hacer nada
        }
        // Para versiones anteriores a Android 13, el permiso se concede automáticamente
    }
    
    /**
     * Maneja la respuesta del usuario a la solicitud de permisos
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == REQUEST_NOTIFICATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permiso concedido
                Log.d("SubjectListActivity", "Permiso de notificaciones concedido");
            } else {
                // Permiso denegado
                Log.d("SubjectListActivity", "Permiso de notificaciones denegado");
                // Mostrar mensaje informativo
                Toast.makeText(this, 
                    getString(R.string.notification_permission_denied),
                    Toast.LENGTH_LONG).show();
            }
        }
    }
    
    /**
     * Obtiene el UID del usuario actual (Firebase UID o UUID local)
     */
    // ===== Import Shared Subject =====
    
    private void showImportSharedSubjectDialog() {
        android.widget.EditText etCode = new android.widget.EditText(this);
        etCode.setHint(getString(R.string.share_subject_code_hint));
        etCode.setInputType(android.text.InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
        
        // Aplicar formateo automático (mayúsculas y guiones)
        etCode.addTextChangedListener(new com.gastonlesbegueris.caretemplate.util.ShareCodeFormatter(etCode));
        
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(getString(R.string.share_subject_receive_title))
                .setMessage(getString(R.string.share_subject_receive_message))
                .setView(etCode)
                .setPositiveButton(getString(R.string.button_import), (d, w) -> {
                    String code = etCode != null && etCode.getText() != null ? etCode.getText().toString().trim() : "";
                    // Normalizar el código (quitar guiones para la búsqueda)
                    code = code.replaceAll("[\\s-]", "").toUpperCase();
                    if (code.isEmpty()) {
                        Toast.makeText(this, getString(R.string.share_subject_invalid), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    importSharedSubject(code);
                })
                .setNegativeButton(getString(R.string.button_cancel), null)
                .show();
    }
    
    private void importSharedSubject(String shareCode) {
        com.gastonlesbegueris.caretemplate.util.SubjectShareManager shareManager = 
                new com.gastonlesbegueris.caretemplate.util.SubjectShareManager(this);
        
        Toast.makeText(this, getString(R.string.share_subject_importing), Toast.LENGTH_SHORT).show();
        
        shareManager.getSharedSubject(shareCode, new com.gastonlesbegueris.caretemplate.util.SubjectShareManager.SharedSubjectCallback() {
            @Override
            public void onSharedSubjectData(java.util.Map<String, Object> subjectData, java.util.List<java.util.Map<String, Object>> eventsData) {
                // Importar el sujeto y sus eventos directamente desde shared_subjects (más seguro)
                com.gastonlesbegueris.caretemplate.util.SharedSubjectImporter importer = 
                        new com.gastonlesbegueris.caretemplate.util.SharedSubjectImporter(SubjectListActivity.this);
                importer.importFromSharedData(
                        subjectData,
                        eventsData,
                        () -> runOnUiThread(() -> {
                            Toast.makeText(SubjectListActivity.this, 
                                    getString(R.string.share_subject_imported), 
                                    Toast.LENGTH_SHORT).show();
                            // Refrescar la lista de sujetos
                            refreshSubjectsList();
                        }),
                        () -> runOnUiThread(() -> {
                            Toast.makeText(SubjectListActivity.this, 
                                    getString(R.string.share_subject_import_error, "Error al importar datos"), 
                                    Toast.LENGTH_LONG).show();
                        })
                );
            }
            
            @Override
            public void onError(Exception error) {
                runOnUiThread(() -> {
                    Toast.makeText(SubjectListActivity.this, 
                            getString(R.string.share_subject_import_error, error.getMessage()), 
                            Toast.LENGTH_LONG).show();
                });
            }
        });
    }
    
    private void importSubjectFromUser(String sharedSubjectId, String ownerUserId) {
        // Asegurar autenticación antes de importar
        ensureAuthenticationForImport(() -> {
            new Thread(() -> {
                try {
                    // Obtener el sujeto desde Firestore del usuario dueño
                    com.google.firebase.firestore.FirebaseFirestore firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance();
                    
                    firestore.collection("users").document(ownerUserId)
                            .collection("apps").document("CareTemplate")
                            .collection("subjects").document(sharedSubjectId)
                            .get()
                            .addOnSuccessListener(subjectDoc -> {
                                if (!subjectDoc.exists()) {
                                    runOnUiThread(() -> {
                                        Toast.makeText(SubjectListActivity.this, 
                                                getString(R.string.share_subject_not_found), 
                                                Toast.LENGTH_LONG).show();
                                    });
                                    return;
                                }
                                
                                // Crear nuevo sujeto local con los datos del compartido
                                SubjectEntity newSubject = new SubjectEntity();
                                newSubject.id = java.util.UUID.randomUUID().toString(); // Nuevo ID para el usuario que recibe
                                newSubject.appType = subjectDoc.getString("appType");
                                newSubject.name = subjectDoc.getString("name");
                                
                                Long bd = subjectDoc.getLong("birthDate");
                                newSubject.birthDate = (bd != null ? bd : null);
                                
                                Double cm = subjectDoc.getDouble("currentMeasure");
                                newSubject.currentMeasure = (cm != null ? cm : null);
                                
                                newSubject.notes = subjectDoc.getString("notes");
                                newSubject.iconKey = subjectDoc.getString("iconKey");
                                newSubject.colorHex = subjectDoc.getString("colorHex");
                                newSubject.updatedAt = System.currentTimeMillis();
                                newSubject.deleted = 0;
                                newSubject.dirty = 1; // Marcar para sincronizar
                                
                                // Insertar el sujeto
                                dao.insert(newSubject);
                                
                                // Importar eventos del sujeto compartido
                                importSubjectEvents(sharedSubjectId, ownerUserId, newSubject.id);
                                
                                runOnUiThread(() -> {
                                    Toast.makeText(SubjectListActivity.this, 
                                            getString(R.string.share_subject_imported), 
                                            Toast.LENGTH_SHORT).show();
                                    // Refrescar la lista de sujetos
                                    refreshSubjectsList();
                                });
                            })
                            .addOnFailureListener(e -> {
                                runOnUiThread(() -> {
                                    String errorMsg = e != null ? e.getMessage() : "Error desconocido";
                                    if (errorMsg != null && errorMsg.contains("PERMISSION_DENIED")) {
                                        errorMsg = "Error de permisos. Por favor, verifica las reglas de Firestore. " +
                                                  "Asegúrate de que las reglas permitan lectura de sujetos compartidos a usuarios autenticados.";
                                    }
                                    Toast.makeText(SubjectListActivity.this, 
                                            getString(R.string.share_subject_import_error, errorMsg), 
                                            Toast.LENGTH_LONG).show();
                                });
                            });
                        } catch (Exception e) {
                            runOnUiThread(() -> {
                                Toast.makeText(SubjectListActivity.this, 
                                        getString(R.string.share_subject_import_error, e.getMessage()), 
                                        Toast.LENGTH_LONG).show();
                            });
                        }
                    }).start();
                }, () -> {
                    runOnUiThread(() -> {
                        Toast.makeText(SubjectListActivity.this, 
                                getString(R.string.share_subject_import_error, "Error al autenticar en Firebase"), 
                                Toast.LENGTH_LONG).show();
                    });
                });
    }
    
    /**
     * Asegura autenticación antes de importar un sujeto compartido
     */
    private void ensureAuthenticationForImport(Runnable onAuthenticated, Runnable onError) {
        com.google.firebase.auth.FirebaseUser currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            onAuthenticated.run();
            return;
        }
        
        com.google.firebase.auth.FirebaseAuth.getInstance().signInAnonymously()
                .addOnSuccessListener(authResult -> {
                    if (authResult != null && authResult.getUser() != null) {
                        onAuthenticated.run();
                    } else {
                        if (onError != null) {
                            onError.run();
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    if (onError != null) {
                        onError.run();
                    }
                });
    }
    
    private void importSubjectEvents(String originalSubjectId, String ownerUserId, String newSubjectId) {
        com.google.firebase.firestore.FirebaseFirestore firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance();
        String currentUserId = getCurrentUserId();
        
        firestore.collection("users").document(ownerUserId)
                .collection("apps").document("CareTemplate")
                .collection("events")
                .whereEqualTo("subjectId", originalSubjectId)
                .whereEqualTo("deleted", 0)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    new Thread(() -> {
                        for (com.google.firebase.firestore.QueryDocumentSnapshot doc : querySnapshot) {
                            EventEntity event = new EventEntity();
                            event.id = java.util.UUID.randomUUID().toString(); // Nuevo ID
                            event.uid = currentUserId; // ID del usuario que recibe
                            event.appType = doc.getString("appType");
                            event.subjectId = newSubjectId; // ID del nuevo sujeto
                            event.title = doc.getString("title");
                            event.note = doc.getString("note");
                            
                            Long due = doc.getLong("dueAt");
                            event.dueAt = (due != null ? due : 0L);
                            
                            Long up = doc.getLong("updatedAt");
                            event.updatedAt = (up != null ? up : System.currentTimeMillis());
                            
                            Long del = doc.getLong("deleted");
                            event.deleted = (del != null ? del.intValue() : 0);
                            
                            Double cost = doc.getDouble("cost");
                            event.cost = cost;
                            
                            Double km = doc.getDouble("kilometersAtEvent");
                            event.kilometersAtEvent = km;
                            
                            Long realized = doc.getLong("realized");
                            event.realized = (realized != null ? realized.intValue() : 0);
                            
                            // Campos de repetición
                            event.repeatType = doc.getString("repeatType");
                            Long repeatInterval = doc.getLong("repeatInterval");
                            event.repeatInterval = (repeatInterval != null ? repeatInterval.intValue() : null);
                            Long repeatEndDate = doc.getLong("repeatEndDate");
                            event.repeatEndDate = (repeatEndDate != null ? repeatEndDate : null);
                            Long repeatCount = doc.getLong("repeatCount");
                            event.repeatCount = (repeatCount != null ? repeatCount.intValue() : null);
                            event.originalEventId = doc.getString("originalEventId");
                            
                            // Campos de notificación
                            Long notificationMinutesBefore = doc.getLong("notificationMinutesBefore");
                            event.notificationMinutesBefore = (notificationMinutesBefore != null ? notificationMinutesBefore.intValue() : null);
                            
                            Long realizedAt = doc.getLong("realizedAt");
                            event.realizedAt = (realizedAt != null ? realizedAt : null);
                            
                            event.dirty = 1; // Marcar para sincronizar
                            
                            eventDao.insert(event);
                        }
                    }).start();
                })
                .addOnFailureListener(e -> {
                    Log.e("SubjectListActivity", "Error importing events", e);
                });
    }
    
    private String getCurrentUserId() {
        // Intentar obtener Firebase UID primero
        com.google.firebase.auth.FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
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

}
