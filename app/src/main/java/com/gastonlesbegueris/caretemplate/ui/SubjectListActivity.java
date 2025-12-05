package com.gastonlesbegueris.caretemplate.ui;

import android.content.res.Configuration;
import android.os.Bundle;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
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
    
    // Flag para indicar que estamos en modo de recuperación silenciosa
    private boolean isSilentRecoveryMode = false;
    
    // Referencia al diálogo de recuperación
    private androidx.appcompat.app.AlertDialog recoverDialog;
    
    // FAB Speed Dial
    private boolean fabMenuOpen = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_subjects);

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
                java.util.List<SubjectAdapter.SubjectRow> rows = new java.util.ArrayList<>();
                for (SubjectEntity subj : list) {
                    String info  = buildInfoLine(subj);
                    String extra = buildExtraLine(subj);
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

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(getString(R.string.new_subject))
                .setView(view)
                .setPositiveButton(getString(R.string.button_save), (d,w)-> {
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
                    insertSubjectFull(name, birthMillis, measure, notes, selectedIconKey[0]);
                })
                .setNegativeButton(getString(R.string.button_cancel), null)
                .show();
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

        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(getString(R.string.edit_subject))
                .setView(view)
                .setPositiveButton(getString(R.string.button_save), null)
                .setNegativeButton(getString(R.string.button_cancel), null)
                .create();

        // Si es pets, agregar botón de eliminar
        if ("pets".equals(appType)) {
            dialog.setButton(androidx.appcompat.app.AlertDialog.BUTTON_NEUTRAL, "Eliminar", (d, w) -> {
                new androidx.appcompat.app.AlertDialog.Builder(this)
                        .setTitle(getString(R.string.delete_subject))
                        .setMessage(getString(R.string.delete_subject_confirmation, subj.name))
                        .setPositiveButton(getString(R.string.button_delete), (d2, w2) -> softDelete(subj.id))
                        .setNegativeButton(getString(R.string.button_cancel), null)
                        .show();
            });
        }

        dialog.setOnShowListener(d -> {
            dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
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
        });

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

    private String buildExtraLine(SubjectEntity subj) {
        EventDao eventDao = db.eventDao();
        long now = System.currentTimeMillis();
        EventEntity next = eventDao.nextEvent(appType, subj.id, now);
        if (next == null) return getString(R.string.next_event_none);
        String when = new java.text.SimpleDateFormat(getString(R.string.event_date_format)).format(new java.util.Date(next.dueAt));
        if (next.title == null || next.title.isEmpty()) {
            return getString(R.string.next_event_label, when);
        } else {
            return getString(R.string.next_event_with_title, when, next.title);
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
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
            // Iniciar sincronización
            doSync();
            return true;
        } else if (id == R.id.action_recovery) {
            // Mostrar el código de recuperación
            showRecoveryCodeDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
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
    
    // ===== Helper Methods =====
    
    private void refreshSubjectsList() {
        dao.observeActive(appType).observe(this, (List<SubjectEntity> list) -> {
            new Thread(() -> {
                java.util.List<SubjectAdapter.SubjectRow> rows = new java.util.ArrayList<>();
                for (SubjectEntity subj : list) {
                    String info  = buildInfoLine(subj);
                    String extra = buildExtraLine(subj);
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
                    
                    // Activar modo silencioso ANTES de iniciar la recuperación
                    isSilentRecoveryMode = true;
                    Log.d("SubjectListActivity", "Modo de recuperación silenciosa activado");
                    
                    recoverUserFromCode(code);
                })
                .setNegativeButton(getString(R.string.button_cancel), null)
                .setCancelable(true)
                .create();
        
        recoverDialog.show();
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
                    Toast.makeText(SubjectListActivity.this, getString(R.string.recovery_error, error.getMessage()), Toast.LENGTH_LONG).show();
                });
            }
        });
    }
    
    private void performSyncWithUserId(String userId, boolean silentErrors) {
        // Guardar el userId recuperado para uso futuro
        getSharedPreferences("user_prefs", MODE_PRIVATE)
                .edit()
                .putString("user_id", userId)
                .putString("firebase_uid", userId)
                .apply();
        
        // Intentar autenticarse con Firebase
        com.google.firebase.auth.FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        
        if (currentUser == null || !currentUser.getUid().equals(userId)) {
            FirebaseAuth.getInstance().signInAnonymously()
                    .addOnSuccessListener(authResult -> {
                        if (authResult != null && authResult.getUser() != null) {
                            String firebaseUid = authResult.getUser().getUid();
                            String syncUid = firebaseUid.equals(userId) ? firebaseUid : userId;
                            performSyncSilent(syncUid, silentErrors);
                        } else {
                            performSyncSilent(userId, silentErrors);
                        }
                    })
                    .addOnFailureListener(e -> {
                        performSyncSilent(userId, silentErrors);
                    });
        } else {
            performSyncSilent(userId, silentErrors);
        }
    }
    
    private void performSyncSilent(String uid, boolean silentErrors) {
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
                                        if (errorMsg != null && (
                                            errorMsg.contains("failed_precondition") || 
                                            errorMsg.contains("FAILED_PRECONDITION") ||
                                            errorMsg.contains("PERMISSION_DENIED") ||
                                            errorMsg.contains("permission_denied"))) {
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
                            if (errorMsg != null && (
                                errorMsg.contains("failed_precondition") || 
                                errorMsg.contains("FAILED_PRECONDITION") ||
                                errorMsg.contains("PERMISSION_DENIED") ||
                                errorMsg.contains("permission_denied"))) {
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
                        Log.w("SubjectListActivity", "Error al subir eventos durante recuperación (silenciado): " + e.getMessage());
                        Toast.makeText(this, getString(R.string.data_recovered), Toast.LENGTH_SHORT).show();
                        refreshSubjectsList();
                    }
                }));
                    }, e -> runOnUiThread(() -> {
                        if (silentErrors) {
                            isSilentRecoveryMode = false;
                            closeRecoverDialog();
                            Log.w("SubjectListActivity", "Error al subir sujetos durante recuperación (silenciado): " + e.getMessage());
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
                                Toast.makeText(this, getString(R.string.sync_error_auth), Toast.LENGTH_LONG).show();
                            });
                        });
                return;
            }
            
            performSync(user.getUid());
        } catch (Exception e) {
            runOnUiThread(() -> {
                Toast.makeText(this, getString(R.string.sync_error, e.getMessage()), Toast.LENGTH_LONG).show();
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
                                    Toast.makeText(this, getString(R.string.sync_success), Toast.LENGTH_SHORT).show();
                                    refreshSubjectsList();
                                }),
                                e -> runOnUiThread(() -> {
                                    showSyncError("Error al recuperar datos", e);
                                })
                        );
                    }, e -> runOnUiThread(() -> {
                        showSyncError("Error al recuperar sujetos", e);
                    }));
                }, e -> runOnUiThread(() -> {
                    showSyncError("Error al subir eventos", e);
                }));
                    }, e -> runOnUiThread(() -> {
                        showSyncError("Error al subir sujetos", e);
                    }));
        } catch (SecurityException e) {
            runOnUiThread(() -> {
                Toast.makeText(this, getString(R.string.sync_error_security), Toast.LENGTH_LONG).show();
            });
        } catch (Exception e) {
            runOnUiThread(() -> {
                showSyncError("Error al iniciar sync", e);
            });
        }
    }
    
    private void showSyncError(String context, Exception e) {
        String errorMsg = e != null ? e.getMessage() : "null";
        
        // PERMISSION_DENIED y failed_precondition son normales cuando no hay datos
        // No mostrar estos errores al usuario
        // Verificar también variaciones del mensaje (case-insensitive, con espacios, etc.)
        if (errorMsg != null) {
            String errorMsgLower = errorMsg.toLowerCase();
            if (errorMsgLower.contains("permission") && errorMsgLower.contains("denied") ||
                errorMsgLower.contains("failed_precondition") ||
                errorMsgLower.contains("failed precondition") ||
                errorMsg.contains("PERMISSION_DENIED") || 
                errorMsg.contains("permission_denied") || 
                errorMsg.contains("failed_precondition") ||
                errorMsg.contains("FAILED_PRECONDITION")) {
                // Estos errores son normales cuando no hay datos - solo loguear
                Log.d("SubjectListActivity", "Error normal durante sincronización (sin datos): " + context + " - " + errorMsg);
                // Mostrar mensaje de éxito en su lugar
                Toast.makeText(this, getString(R.string.sync_success), Toast.LENGTH_SHORT).show();
                refreshSubjectsList();
                return;
            }
        }
        
        // Para otros errores, mostrar mensaje apropiado
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
            userMessage = context + ": " + errorMsg;
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

        // Verificar que las vistas se encontraron
        if (etTitle == null || etCost == null || sp == null) {
            Toast.makeText(this, getString(R.string.error_load_dialog), Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Mostrar campo de kilómetros solo para "cars"
        if ("cars".equals(appType) && tilKilometers != null && etKilometers != null) {
            tilKilometers.setVisibility(View.VISIBLE);
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

                new androidx.appcompat.app.AlertDialog.Builder(this)
                        .setTitle(getString(R.string.new_event))
                        .setView(view)
                        .setPositiveButton(getString(R.string.button_choose_date_time), (d, w) -> {
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

                            final String fTitle = title;
                            final String fSubjectId = subjectId;
                            final Double fCost = cost;
                            final Double fKilometers = kilometersAtEvent;

                            pickDateTime(0, dueAt -> insertLocal(fTitle, fSubjectId, fCost, dueAt, fKilometers));
                        })
                        .setNegativeButton(getString(R.string.button_cancel), null)
                        .show();
            });
        }).start();
    }

    private void insertLocal(String title, String subjectId, Double cost, long dueAt, Double kilometersAtEvent) {
        new Thread(() -> {
            // Obtener UID del usuario actual
            String uid = getCurrentUserId();
            
            EventEntity e = new EventEntity();
            e.id = UUID.randomUUID().toString();
            e.uid = uid; // Usar UID del usuario actual
            e.appType = appType;
            e.subjectId = subjectId;     // sujeto elegido
            e.title = title;
            e.note = "";
            e.cost = cost;               // costo opcional
            e.kilometersAtEvent = kilometersAtEvent; // km del auto al momento del evento (solo para cars)
            e.realized = 0;              // aún no realizado
            e.dueAt = dueAt;
            e.updatedAt = System.currentTimeMillis();
            e.deleted = 0;
            e.dirty = 1;
            eventDao.insert(e);
            com.gastonlesbegueris.caretemplate.util.LimitGuard.onEventCreated(this, appType);

            runOnUiThread(() -> Toast.makeText(this, getString(R.string.event_saved), Toast.LENGTH_SHORT).show());
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
    
    /**
     * Obtiene el UID del usuario actual (Firebase UID o UUID local)
     */
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
