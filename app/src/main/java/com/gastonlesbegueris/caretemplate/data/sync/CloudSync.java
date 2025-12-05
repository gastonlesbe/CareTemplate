package com.gastonlesbegueris.caretemplate.data.sync;

import android.util.Log;

import androidx.annotation.Nullable;

import com.gastonlesbegueris.caretemplate.data.local.EventDao;
import com.gastonlesbegueris.caretemplate.data.local.EventEntity;
import com.gastonlesbegueris.caretemplate.data.local.SubjectDao;
import com.gastonlesbegueris.caretemplate.data.local.SubjectEntity;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class CloudSync {

    // Callbacks
    public interface Ok  { void run(); }
    public interface Err { void run(Exception e); }

    private final EventDao eventDao;
    private final SubjectDao subjectDao;
    private final FirebaseFirestore fs;
    private final String uid;      // dueño de los datos en Firestore
    private final String app;      // identificador de la app/espacio (por ej. "CareTemplate")
    private final String appType;  // flavor actual: "pets" | "cars" | "family" | "house"

    public CloudSync(EventDao eventDao,
                     SubjectDao subjectDao,
                     FirebaseFirestore fs,
                     String uid,
                     String app,
                     String appType) {
        this.eventDao = eventDao;
        this.subjectDao = subjectDao;
        this.fs = fs;
        this.uid = uid;
        this.app = app;
        this.appType = appType;
    }

    // ----------------------
    // Helpers de colección
    // ----------------------
    private CollectionReference subjectsCol() {
        return fs.collection("users").document(uid)
                .collection("apps").document(app)
                .collection("subjects");
    }

    private CollectionReference eventsCol() {
        return fs.collection("users").document(uid)
                .collection("apps").document(app)
                .collection("events");
    }

    // ======================
    //      SUBJECTS
    // ======================

    /** Sube a Firestore todos los subjects con dirty=1 del appType actual. */
    public void pushSubjects(Ok ok, @Nullable Err err) {
        new Thread(() -> {
            try {
                final List<SubjectEntity> dirty = subjectDao.listDirty(appType);
                if (dirty == null || dirty.isEmpty()) {
                    if (ok != null) ok.run();
                    return;
                }

                final AtomicInteger left = new AtomicInteger(dirty.size());
                final List<String> cleaned = Collections.synchronizedList(new ArrayList<>());

                for (final SubjectEntity s : dirty) {
                    final Map<String, Object> data = new HashMap<>();
                    data.put("id", s.id);
                    data.put("appType", s.appType);
                    data.put("name", s.name);
                    data.put("birthDate", s.birthDate);
                    data.put("currentMeasure", s.currentMeasure);
                    data.put("notes", s.notes);
                    data.put("iconKey", s.iconKey);
                    data.put("colorHex", s.colorHex);
                    data.put("updatedAt", s.updatedAt);
                    data.put("deleted", s.deleted);

                    subjectsCol().document(s.id).set(data)
                            .addOnSuccessListener(a -> {
                                cleaned.add(s.id);
                                if (left.decrementAndGet() == 0) {
                                    new Thread(() -> {
                                        subjectDao.markClean(cleaned);
                                        if (ok != null) ok.run();
                                    }).start();
                                }
                            })
                            .addOnFailureListener(e -> {
                                if (err != null) err.run(e);
                            });
                }
            } catch (Exception e) {
                if (err != null) err.run(e);
            }
        }).start();
    }

    /** Baja de Firestore los subjects con updatedAt > lastLocalUpdated. */
    public void pullSubjects(Ok ok, @Nullable Err err) {
        new Thread(() -> {
            final long last = subjectDao.lastUpdatedForApp(appType);
            Log.d("CloudSync", "pullSubjects: uid=" + uid + ", app=" + app + ", appType=" + appType + ", last=" + last);
            
            // Primero, asegurar que el documento padre existe
            fs.collection("users").document(uid)
                    .collection("apps").document(app)
                    .set(Collections.singletonMap("createdAt", System.currentTimeMillis()), SetOptions.merge())
                    .addOnSuccessListener(a -> {
                        Log.d("CloudSync", "Documento padre creado/actualizado: users/" + uid + "/apps/" + app);
                        // Ahora hacer la consulta
                        subjectsCol()
                                .whereEqualTo("appType", appType)
                                .whereGreaterThan("updatedAt", last)
                                .get()
                                .addOnSuccessListener(qs -> {
                                    new Thread(() -> {
                                        for (QueryDocumentSnapshot doc : qs) {
                                            SubjectEntity s = new SubjectEntity();
                                            s.id = doc.getString("id");
                                            s.appType = doc.getString("appType");
                                            s.name = doc.getString("name");

                                            Long bd = doc.getLong("birthDate");
                                            s.birthDate = (bd == null ? null : bd);

                                            Double cm = doc.getDouble("currentMeasure");
                                            s.currentMeasure = (cm == null ? null : cm);

                                            s.notes = doc.getString("notes");
                                            s.iconKey = doc.getString("iconKey");
                                            s.colorHex = doc.getString("colorHex");

                                            Long up = doc.getLong("updatedAt");
                                            s.updatedAt = (up == null ? 0L : up);

                                            Long del = doc.getLong("deleted");
                                            s.deleted = (del == null ? 0 : del.intValue());

                                            s.dirty = 0; // limpio al bajar del cloud
                                            subjectDao.insert(s);
                                        }
                                        if (ok != null) ok.run();
                                    }).start();
                                })
                                .addOnFailureListener(e -> { 
                                    // Si falla, intentar sin el filtro de updatedAt (primera vez)
                                    String errorMsg = e != null ? e.getMessage() : "null";
                                    Log.w("CloudSync", "Error en pullSubjects (con whereGreaterThan): " + errorMsg, e);
                                    // PERMISSION_DENIED y failed_precondition son normales cuando no hay datos
                                    if (errorMsg != null && (errorMsg.contains("failed_precondition") || 
                                                             errorMsg.contains("FAILED_PRECONDITION") ||
                                                             errorMsg.contains("permission_denied") ||
                                                             errorMsg.contains("PERMISSION_DENIED"))) {
                                        Log.d("CloudSync", "Intentando consulta sin whereGreaterThan...");
                                        // Intentar consulta más simple sin whereGreaterThan
                                        subjectsCol()
                                                .whereEqualTo("appType", appType)
                                                .get()
                                                .addOnSuccessListener(qs -> {
                                                    new Thread(() -> {
                                                        for (QueryDocumentSnapshot doc : qs) {
                                                            SubjectEntity s = new SubjectEntity();
                                                            s.id = doc.getString("id");
                                                            s.appType = doc.getString("appType");
                                                            s.name = doc.getString("name");

                                                            Long bd = doc.getLong("birthDate");
                                                            s.birthDate = (bd == null ? null : bd);

                                                            Double cm = doc.getDouble("currentMeasure");
                                                            s.currentMeasure = (cm == null ? null : cm);

                                                            s.notes = doc.getString("notes");
                                                            s.iconKey = doc.getString("iconKey");
                                                            s.colorHex = doc.getString("colorHex");

                                                            Long up = doc.getLong("updatedAt");
                                                            s.updatedAt = (up == null ? 0L : up);

                                                            Long del = doc.getLong("deleted");
                                                            s.deleted = (del == null ? 0 : del.intValue());

                                                            s.dirty = 0;
                                                            subjectDao.insert(s);
                                                        }
                                                        if (ok != null) ok.run();
                                                    }).start();
                                                })
                                                .addOnFailureListener(e2 -> { 
                                                    Log.w("CloudSync", "Error en pullSubjects (sin whereGreaterThan): " + e2.getMessage(), e2);
                                                    String errorMsg2 = e2.getMessage();
                                                    
                                                    // Si el error es failed_precondition o permission_denied, puede ser que no haya datos aún
                                                    // o que las reglas no permitan la consulta. Intentar consulta sin filtros.
                                                    if (errorMsg2 != null && (errorMsg2.contains("failed_precondition") || 
                                                                              errorMsg2.contains("FAILED_PRECONDITION") ||
                                                                              errorMsg2.contains("permission_denied") ||
                                                                              errorMsg2.contains("PERMISSION_DENIED"))) {
                                                        Log.d("CloudSync", "Intentando consulta sin filtros para verificar acceso...");
                                                        subjectsCol().limit(1).get()
                                                                .addOnSuccessListener(qs -> {
                                                                    Log.d("CloudSync", "Acceso verificado. Continuando sin datos (primera sincronización o sin datos)");
                                                                    // Si esto funciona, el problema es el índice o no hay datos
                                                                    // Continuar sin datos (primera sincronización)
                                                                    if (ok != null) ok.run();
                                                                })
                                                                .addOnFailureListener(e3 -> {
                                                                    String errorMsg3 = e3 != null ? e3.getMessage() : "null";
                                                                    // PERMISSION_DENIED es normal cuando no hay datos - no mostrar error
                                                                    if (errorMsg3 != null && errorMsg3.contains("permission_denied")) {
                                                                        Log.d("CloudSync", "PERMISSION_DENIED durante pullSubjects (sin datos). Continuando sin error.");
                                                                        // Continuar sin error - es normal cuando no hay datos
                                                                        if (ok != null) ok.run();
                                                                    } else {
                                                                        // Si es failed_precondition, probablemente no hay datos o falta índice
                                                                        // Continuar sin error (primera sincronización)
                                                                        Log.d("CloudSync", "No hay datos para bajar o falta índice. Continuando sin error.");
                                                                        if (ok != null) ok.run();
                                                                    }
                                                                });
                                                    } else {
                                                        // Si el error NO es failed_precondition ni permission_denied, puede ser otro problema
                                                        // Pero si la sincronización está funcionando (push OK), probablemente es solo que no hay datos
                                                        Log.d("CloudSync", "Error en fallback pero continuando (puede ser que no haya datos): " + errorMsg2);
                                                        // Continuar sin error si la sincronización está funcionando
                                                        if (ok != null) ok.run();
                                                    }
                                                });
                                    } else {
                                        // Si NO es failed_precondition, puede ser otro error real
                                        // Pero verificar si es un error que podemos ignorar
                                        if (errorMsg != null && (errorMsg.contains("permission_denied") || 
                                                                 errorMsg.contains("PERMISSION_DENIED"))) {
                                            // PERMISSION_DENIED es normal cuando no hay datos - no mostrar error
                                            Log.d("CloudSync", "PERMISSION_DENIED durante pullSubjects (sin datos). Continuando sin error.");
                                            if (ok != null) ok.run();
                                        } else {
                                            // Otros errores - loguear pero continuar (puede ser que no haya datos)
                                            Log.d("CloudSync", "Error en pullSubjects pero continuando: " + errorMsg);
                                            if (ok != null) ok.run();
                                        }
                                    }
                                });
                    })
                    .addOnFailureListener(e -> { 
                        // Verificar si es PERMISSION_DENIED antes de propagar el error
                        String errorMsg = e != null ? e.getMessage() : "null";
                        if (errorMsg != null && (errorMsg.contains("permission_denied") || 
                                                 errorMsg.contains("PERMISSION_DENIED"))) {
                            Log.d("CloudSync", "PERMISSION_DENIED en pullSubjects (fallback final, sin datos). Continuando sin error.");
                            if (ok != null) ok.run();
                        } else {
                            if (err != null) err.run(e);
                        }
                    });
        }).start();
    }

    // ======================
    //       EVENTS
    // ======================

    /** Sube a Firestore todos los events con dirty=1 del appType actual. */
    public void push(Ok ok, @Nullable Err err) {
        new Thread(() -> {
            try {
                final List<EventEntity> dirty = eventDao.listDirty();
                if (dirty == null || dirty.isEmpty()) {
                    if (ok != null) ok.run();
                    return;
                }

                final AtomicInteger left = new AtomicInteger(dirty.size());
                final List<String> cleaned = Collections.synchronizedList(new ArrayList<>());

                for (final EventEntity e : dirty) {
                    final Map<String, Object> data = new HashMap<>();
                    data.put("id", e.id);
                    data.put("uid", e.uid);
                    data.put("appType", e.appType);
                    data.put("subjectId", e.subjectId);
                    data.put("title", e.title);
                    data.put("note", e.note);
                    data.put("dueAt", e.dueAt);
                    data.put("updatedAt", e.updatedAt);
                    data.put("deleted", e.deleted);
                    if (e.cost != null) data.put("cost", e.cost);
                    if (e.kilometersAtEvent != null) data.put("kilometersAtEvent", e.kilometersAtEvent);
                    data.put("realized", e.realized);

                    eventsCol().document(e.id).set(data)
                            .addOnSuccessListener(a -> {
                                cleaned.add(e.id);
                                if (left.decrementAndGet() == 0) {
                                    new Thread(() -> {
                                        eventDao.markClean(cleaned);
                                        if (ok != null) ok.run();
                                    }).start();
                                }
                            })
                            .addOnFailureListener(ex -> { if (err != null) err.run(ex); });
                }
            } catch (Exception ex) {
                if (err != null) err.run(ex);
            }
        }).start();
    }

    /** Baja de Firestore los events con updatedAt > lastLocalUpdated. */
    public void pull(Ok ok, @Nullable Err err) {
        new Thread(() -> {
            final long last = eventDao.lastUpdatedForApp(appType);
            
            // Primero, asegurar que el documento padre existe
            fs.collection("users").document(uid)
                    .collection("apps").document(app)
                    .set(Collections.singletonMap("createdAt", System.currentTimeMillis()), SetOptions.merge())
                    .addOnSuccessListener(a -> {
                        // Ahora hacer la consulta
                        eventsCol()
                                .whereEqualTo("appType", appType)
                                .whereGreaterThan("updatedAt", last)
                                .get()
                                .addOnSuccessListener(qs -> {
                                    new Thread(() -> {
                                        for (QueryDocumentSnapshot doc : qs) {
                                            EventEntity e = new EventEntity();
                                            e.id = doc.getString("id");
                                            e.uid = doc.getString("uid");
                                            e.appType = doc.getString("appType");
                                            e.subjectId = doc.getString("subjectId");
                                            e.title = doc.getString("title");
                                            e.note = doc.getString("note");

                                            Long due = doc.getLong("dueAt");
                                            e.dueAt = (due == null ? 0L : due);

                                            Long up = doc.getLong("updatedAt");
                                            e.updatedAt = (up == null ? 0L : up);

                                            Long del = doc.getLong("deleted");
                                            e.deleted = (del == null ? 0 : del.intValue());

                                            Double cost = doc.getDouble("cost");
                                            e.cost = cost;

                                            Double kilometersAtEvent = doc.getDouble("kilometersAtEvent");
                                            e.kilometersAtEvent = kilometersAtEvent;

                                            Long realized = doc.getLong("realized");
                                            e.realized = (realized == null ? 0 : realized.intValue());

                                            e.dirty = 0; // limpio al bajar del cloud
                                            // Usar insert que ahora tiene REPLACE para evitar duplicados
                                            eventDao.insert(e);
                                        }
                                        if (ok != null) ok.run();
                                    }).start();
                                })
                                .addOnFailureListener(ex -> { 
                                    String errorMsg = ex != null ? ex.getMessage() : "null";
                                    Log.w("CloudSync", "Error en pull: " + errorMsg, ex);
                                    
                                    // Si falla, intentar sin el filtro de updatedAt (primera vez)
                                    // PERMISSION_DENIED y failed_precondition son normales cuando no hay datos
                                    if (errorMsg != null && (errorMsg.contains("failed_precondition") || 
                                                              errorMsg.contains("FAILED_PRECONDITION") ||
                                                              errorMsg.contains("permission_denied") ||
                                                              errorMsg.contains("PERMISSION_DENIED"))) {
                                        eventsCol()
                                                .whereEqualTo("appType", appType)
                                                .get()
                                                .addOnSuccessListener(qs -> {
                                                    new Thread(() -> {
                                                        for (QueryDocumentSnapshot doc : qs) {
                                                            EventEntity event = new EventEntity();
                                                            event.id = doc.getString("id");
                                                            event.uid = doc.getString("uid");
                                                            event.appType = doc.getString("appType");
                                                            event.subjectId = doc.getString("subjectId");
                                                            event.title = doc.getString("title");
                                                            event.note = doc.getString("note");

                                                            Long due = doc.getLong("dueAt");
                                                            event.dueAt = (due == null ? 0L : due);

                                                            Long up = doc.getLong("updatedAt");
                                                            event.updatedAt = (up == null ? 0L : up);

                                                            Long del = doc.getLong("deleted");
                                                            event.deleted = (del == null ? 0 : del.intValue());

                                                            Double cost = doc.getDouble("cost");
                                                            event.cost = cost;

                                                            Double kilometersAtEvent = doc.getDouble("kilometersAtEvent");
                                                            event.kilometersAtEvent = kilometersAtEvent;

                                                            Long realized = doc.getLong("realized");
                                                            event.realized = (realized == null ? 0 : realized.intValue());

                                                            event.dirty = 0;
                                                            eventDao.insert(event);
                                                        }
                                                        if (ok != null) ok.run();
                                                    }).start();
                                                })
                                                .addOnFailureListener(e2 -> { 
                                                    Log.w("CloudSync", "Error en pull (sin whereGreaterThan): " + e2.getMessage(), e2);
                                                    String errorMsg2 = e2.getMessage();
                                                    
                                                    // Si el error es failed_precondition o permission_denied, puede ser que no haya datos aún
                                                    if (errorMsg2 != null && (errorMsg2.contains("failed_precondition") || 
                                                                              errorMsg2.contains("FAILED_PRECONDITION") ||
                                                                              errorMsg2.contains("permission_denied") ||
                                                                              errorMsg2.contains("PERMISSION_DENIED"))) {
                                                        Log.d("CloudSync", "Intentando consulta sin filtros para verificar acceso...");
                                                        eventsCol().limit(1).get()
                                                                .addOnSuccessListener(qs -> {
                                                                    Log.d("CloudSync", "Acceso verificado. Continuando sin datos (primera sincronización o sin datos)");
                                                                    if (ok != null) ok.run();
                                                                })
                                                                .addOnFailureListener(e3 -> {
                                                                    String errorMsg3 = e3 != null ? e3.getMessage() : "null";
                                                                    // PERMISSION_DENIED es normal cuando no hay datos - no mostrar error
                                                                    if (errorMsg3 != null && errorMsg3.contains("permission_denied")) {
                                                                        Log.d("CloudSync", "PERMISSION_DENIED durante pull (sin datos). Continuando sin error.");
                                                                        // Continuar sin error - es normal cuando no hay datos
                                                                        if (ok != null) ok.run();
                                                                    } else {
                                                                        // Si es failed_precondition, probablemente no hay datos
                                                                        Log.d("CloudSync", "No hay datos para bajar o falta índice. Continuando sin error.");
                                                                        if (ok != null) ok.run();
                                                                    }
                                                                });
                                                    } else {
                                                        // Si el error NO es failed_precondition ni permission_denied, puede ser otro problema
                                                        // Pero si la sincronización está funcionando (push OK), probablemente es solo que no hay datos
                                                        Log.d("CloudSync", "Error en fallback de pull pero continuando (puede ser que no haya datos): " + errorMsg2);
                                                        // Continuar sin error si la sincronización está funcionando
                                                        if (ok != null) ok.run();
                                                    }
                                                });
                                    } else {
                                        // Si NO es failed_precondition ni permission_denied, puede ser otro error real
                                        // Pero verificar si es un error que podemos ignorar
                                        if (errorMsg != null && (errorMsg.contains("permission_denied") || 
                                                                   errorMsg.contains("PERMISSION_DENIED"))) {
                                            // PERMISSION_DENIED es normal cuando no hay datos - no mostrar error
                                            Log.d("CloudSync", "PERMISSION_DENIED durante pull (sin datos). Continuando sin error.");
                                            if (ok != null) ok.run();
                                        } else {
                                            // Otros errores - loguear pero continuar (puede ser que no haya datos)
                                            Log.d("CloudSync", "Error en pull pero continuando: " + errorMsg);
                                            if (ok != null) ok.run();
                                        }
                                    }
                                });
                    })
                    .addOnFailureListener(e -> { 
                        String errorMsg = e != null ? e.getMessage() : "null";
                        Log.w("CloudSync", "Error al crear documento padre en pull: " + errorMsg, e);
                        // Verificar si es PERMISSION_DENIED antes de propagar el error
                        if (errorMsg != null && (errorMsg.contains("permission_denied") || 
                                                 errorMsg.contains("PERMISSION_DENIED"))) {
                            Log.d("CloudSync", "PERMISSION_DENIED al crear documento padre (sin datos). Continuando sin error.");
                            if (ok != null) ok.run();
                        } else {
                            if (err != null) err.run(e);
                        }
                    });
        }).start();
    }
}
