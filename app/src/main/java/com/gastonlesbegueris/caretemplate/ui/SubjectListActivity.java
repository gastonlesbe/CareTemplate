package com.gastonlesbegueris.caretemplate.ui;

import android.os.Bundle;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

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
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;

import java.util.List;
import java.util.UUID;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.textfield.TextInputEditText;

public class SubjectListActivity extends AppCompatActivity {

    private AppDb db;
    private SubjectDao dao;
    private SubjectAdapter adapter;
    private String appType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_subjects);

        MaterialToolbar toolbar = findViewById(R.id.toolbarSubjects);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationIcon(R.drawable.ic_back);
        toolbar.setNavigationOnClickListener(v -> finish());
        toolbar.setTitle("Sujetos");


        appType = getString(R.string.app_type); // viene del flavor
        db  = AppDb.get(this);
        dao = db.subjectDao();

        adapter = new SubjectAdapter(new SubjectAdapter.OnClick() {
            @Override public void onEdit(SubjectEntity subj) { showEditDialog(subj); }
            @Override public void onDelete(SubjectEntity subj) { softDelete(subj.id); }
        });

        RecyclerView rv = findViewById(R.id.rvSubjects);
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adapter);

        findViewById(R.id.btnAddSubject).setOnClickListener(v -> showAddDialog());

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

        setupDialogUiByFlavor(etBirth, etMeasure);
        etBirth.setOnClickListener(v -> pickDateInto(etBirth));

        // Setup icon selection
        final android.widget.GridLayout gridIcons = view.findViewById(R.id.gridIcons);
        final String[] selectedIconKey = {defaultIconForFlavor()};
        populateIconGrid(gridIcons, selectedIconKey);

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Nuevo sujeto")
                .setView(view)
                .setPositiveButton("Guardar", (d,w)-> {
                    String name;
                    if ("cars".equals(appType)) {
                        // Para cars: concatenar marca + modelo
                        String brand = etBrand != null ? etBrand.getText().toString().trim() : "";
                        String model = etModel != null ? etModel.getText().toString().trim() : "";
                        if (brand.isEmpty() && model.isEmpty()) {
                            Toast.makeText(this,"Marca o Modelo requerido",Toast.LENGTH_SHORT).show();
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
                            Toast.makeText(this,"Nombre requerido",Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }
                    Long birthMillis = parseDateOrNull(etBirth.getText().toString().trim());
                    Double measure = safeParseDouble(etMeasure.getText().toString().trim());
                    String notes = etNotes.getText().toString().trim();
                    insertSubjectFull(name, birthMillis, measure, notes, selectedIconKey[0]);
                })
                .setNegativeButton("Cancelar", null)
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
            subj.birthDate = ("cars".equals(appType) || "house".equals(appType)) ? null : birthMillis;
            subj.currentMeasure = measure;

            subj.notes = notes == null ? "" : notes;
            subj.updatedAt = System.currentTimeMillis();
            subj.deleted = 0;
            subj.dirty = 1;
            subj.iconKey = (iconKey == null || iconKey.isEmpty()) ? defaultIconForFlavor() : iconKey;
            subj.colorHex = (subj.colorHex == null || subj.colorHex.isEmpty()) ? "#03DAC5" : subj.colorHex;

            dao.insert(subj);
            runOnUiThread(() -> Toast.makeText(this, "Sujeto creado ✅", Toast.LENGTH_SHORT).show());
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

        // Mostrar campo de fecha solo para pets
        if ("pets".equals(appType)) {
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
                .setTitle("Editar sujeto")
                .setView(view)
                .setPositiveButton("Guardar", null)
                .setNegativeButton("Cancelar", null)
                .create();

        // Si es pets, agregar botón de eliminar
        if ("pets".equals(appType)) {
            dialog.setButton(androidx.appcompat.app.AlertDialog.BUTTON_NEUTRAL, "Eliminar", (d, w) -> {
                new androidx.appcompat.app.AlertDialog.Builder(this)
                        .setTitle("Eliminar sujeto")
                        .setMessage("¿Estás seguro de que querés eliminar a " + subj.name + "?")
                        .setPositiveButton("Eliminar", (d2, w2) -> softDelete(subj.id))
                        .setNegativeButton("Cancelar", null)
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
                        android.widget.Toast.makeText(this, "Marca o Modelo requerido", android.widget.Toast.LENGTH_SHORT).show();
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
                        android.widget.Toast.makeText(this, "Nombre requerido", android.widget.Toast.LENGTH_SHORT).show();
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
                if ("pets".equals(appType) && !birthStr.isEmpty()) {
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
                        android.widget.Toast.makeText(this, "Sujeto actualizado ✅", android.widget.Toast.LENGTH_SHORT).show();
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
            runOnUiThread(() -> Toast.makeText(this, "Actualizado ✅", Toast.LENGTH_SHORT).show());
        }).start();
    }

    private void softDelete(String id) {
        new Thread(() -> {
            dao.softDelete(id, System.currentTimeMillis());
            runOnUiThread(() -> Toast.makeText(this, "Eliminado ✅", Toast.LENGTH_SHORT).show());
        }).start();
    }

    // ---------- Helpers visuales ----------
    private void setupDialogUiByFlavor(EditText etBirth, EditText etMeasure) {
        if ("cars".equals(appType) || "house".equals(appType)) {
            etBirth.setVisibility(android.view.View.GONE);
            etMeasure.setHint("Odómetro (km)");
            etMeasure.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        } else {
            etBirth.setVisibility(android.view.View.VISIBLE);
            etBirth.setHint("Fecha de nacimiento (dd/MM/aaaa)");
            etMeasure.setHint("Peso (kg)");
            etMeasure.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
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
            return "Odómetro: " + km;
        } else {
            String age = formatAge(subj.birthDate);
            String weight = (subj.currentMeasure == null) ? "" : (" · Peso: " + subj.currentMeasure + " kg");
            return "Edad: " + age + weight;
        }
    }

    private String buildExtraLine(SubjectEntity subj) {
        EventDao eventDao = db.eventDao();
        long now = System.currentTimeMillis();
        EventEntity next = eventDao.nextEvent(appType, subj.id, now);
        if (next == null) return "Próximo: —";
        String when = new java.text.SimpleDateFormat("dd/MM HH:mm").format(new java.util.Date(next.dueAt));
        return "Próximo: " + when + (next.title == null || next.title.isEmpty() ? "" : " · " + next.title);
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
        if (id == R.id.action_home) {
            startActivity(new android.content.Intent(this, com.gastonlesbegueris.caretemplate.ui.MainActivity.class));
            finish();
            return true;
        } else if (id == R.id.action_agenda) {
            startActivity(new android.content.Intent(this, com.gastonlesbegueris.caretemplate.ui.AgendaMonthActivity.class));
            return true;
        } else if (id == R.id.action_subjects) {
            // Already on subjects page
            return true;
        } else if (id == R.id.action_expenses) {
            startActivity(new android.content.Intent(this, com.gastonlesbegueris.caretemplate.ui.ExpensesActivity.class));
            return true;
        } else if (id == android.R.id.home) {
            finish();
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

}
