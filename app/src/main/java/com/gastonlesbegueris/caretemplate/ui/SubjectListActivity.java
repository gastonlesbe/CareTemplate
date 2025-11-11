package com.gastonlesbegueris.caretemplate.ui;

import android.os.Bundle;
import android.text.InputType;
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

import java.util.List;
import java.util.UUID;

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
        toolbar.setTitle("Sujetos");
        toolbar.setNavigationIcon(R.drawable.ic_back);
        toolbar.setNavigationOnClickListener(v -> finish());

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

        findViewById(R.id.fabAddSubject).setOnClickListener(v -> showAddDialog());

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
                    findViewById(R.id.tvEmptySubjects)
                            .setVisibility(rows.isEmpty() ? android.view.View.VISIBLE : android.view.View.GONE);
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

        setupDialogUiByFlavor(etBirth, etMeasure);
        etBirth.setOnClickListener(v -> pickDateInto(etBirth));

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Nuevo sujeto")
                .setView(view)
                .setPositiveButton("Guardar", (d,w)-> {
                    String name = etName.getText().toString().trim();
                    if (name.isEmpty()) {
                        Toast.makeText(this,"Nombre requerido",Toast.LENGTH_SHORT).show();
                        return;
                    }
                    Long birthMillis = parseDateOrNull(etBirth.getText().toString().trim());
                    Double measure = safeParseDouble(etMeasure.getText().toString().trim());
                    String notes = etNotes.getText().toString().trim();
                    insertSubjectFull(name, birthMillis, measure, notes);
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

    private void insertSubjectFull(String name, Long birthMillis, Double measure, String notes) {
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
            subj.iconKey = (subj.iconKey == null) ? defaultIconForFlavor() : subj.iconKey;
            subj.colorHex = (subj.colorHex == null || subj.colorHex.isEmpty()) ? "#03DAC5" : subj.colorHex;

            dao.insert(subj);
            runOnUiThread(() -> Toast.makeText(this, "Sujeto creado ✅", Toast.LENGTH_SHORT).show());
        }).start();
    }

    // ---------- Editar ----------
    private void showEditDialog(SubjectEntity subj) {
        final android.view.View view = getLayoutInflater().inflate(R.layout.dialog_edit_subject, null, false);

        final EditText etName    = view.findViewById(R.id.etName);
        final EditText etBirth   = view.findViewById(R.id.etBirth);
        final EditText etMeasure = view.findViewById(R.id.etMeasure);
        final EditText etNotes   = view.findViewById(R.id.etNotes);

        setupDialogUiByFlavor(etBirth, etMeasure);

        etName.setText(subj.name);
        if (subj.birthDate != null) {
            java.text.SimpleDateFormat f = new java.text.SimpleDateFormat("dd/MM/yyyy");
            etBirth.setText(f.format(new java.util.Date(subj.birthDate)));
        }
        if (subj.currentMeasure != null) etMeasure.setText(String.valueOf(subj.currentMeasure));
        etNotes.setText(subj.notes == null ? "" : subj.notes);
        etBirth.setOnClickListener(v -> pickDateInto(etBirth));

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Editar sujeto")
                .setView(view)
                .setPositiveButton("Guardar", (d,w)-> {
                    String name = etName.getText().toString().trim();
                    if (name.isEmpty()) {
                        Toast.makeText(this,"Nombre requerido",Toast.LENGTH_SHORT).show();
                        return;
                    }
                    Long birthMillis = parseDateOrNull(etBirth.getText().toString().trim());
                    Double measure = safeParseDouble(etMeasure.getText().toString().trim());
                    String notes = etNotes.getText().toString().trim();
                    updateSubjectFull(subj, name, birthMillis, measure, notes);
                })
                .setNeutralButton("Eliminar", (d,w)-> softDelete(subj.id))
                .setNegativeButton("Cancelar", null)
                .show();
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

}
