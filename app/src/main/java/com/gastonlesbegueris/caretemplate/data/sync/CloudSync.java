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

                final List<String> toDelete = Collections.synchronizedList(new ArrayList<>());
                
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

                    // Si el sujeto está borrado, marcarlo para eliminación física después de sincronizar
                    if (s.deleted == 1) {
                        toDelete.add(s.id);
                    }

                    subjectsCol().document(s.id).set(data)
                            .addOnSuccessListener(a -> {
                                cleaned.add(s.id);
                                if (left.decrementAndGet() == 0) {
                                    new Thread(() -> {
                                        subjectDao.markClean(cleaned);
                                        // Eliminar físicamente los sujetos borrados después de sincronizar exitosamente
                                        for (String idToDelete : toDelete) {
                                            subjectDao.deletePermanently(idToDelete);
                                            Log.d("CloudSync", "Sujeto borrado eliminado físicamente después de sync: " + idToDelete);
                                        }
                                        if (ok != null) ok.run();
                                    }).start();
                                }
                            })
                            .addOnFailureListener(e -> {
                                // Verificar si es un error de permisos que podemos ignorar
                                String errorMsg = e != null ? e.getMessage() : "null";
                                if (errorMsg != null) {
                                    String errorMsgLower = errorMsg.toLowerCase();
                                    boolean isPermissionError = 
                                        (errorMsgLower.contains("permission") && (errorMsgLower.contains("denied") || errorMsgLower.contains("missing"))) ||
                                        errorMsgLower.contains("permission_denied") ||
                                        errorMsgLower.contains("permission denied") ||
                                        errorMsg.contains("PERMISSION_DENIED") ||
                                        errorMsgLower.contains("missing permission") ||
                                        errorMsgLower.contains("missing_permission");
                                    
                                    if (isPermissionError) {
                                        Log.d("CloudSync", "Error de permisos en pushSubjects (puede ser normal): " + errorMsg);
                                        // Continuar como si fuera exitoso para no bloquear la sincronización
                                        cleaned.add(s.id);
                                        if (left.decrementAndGet() == 0) {
                                            new Thread(() -> {
                                                subjectDao.markClean(cleaned);
                                                // Eliminar físicamente los sujetos borrados después de sincronizar exitosamente
                                                for (String idToDelete : toDelete) {
                                                    subjectDao.deletePermanently(idToDelete);
                                                    Log.d("CloudSync", "Sujeto borrado eliminado físicamente después de sync: " + idToDelete);
                                                }
                                                if (ok != null) ok.run();
                                            }).start();
                                        }
                                        return;
                                    }
                                }
                                // Verificar si es error de permisos antes de propagar
                                String finalErrorMsg = e != null ? e.getMessage() : "null";
                                String finalErrorMsgLower = finalErrorMsg != null ? finalErrorMsg.toLowerCase() : "";
                                boolean isPermissionError = 
                                    (finalErrorMsgLower.contains("permission") && (finalErrorMsgLower.contains("denied") || finalErrorMsgLower.contains("missing"))) ||
                                    finalErrorMsgLower.contains("permission_denied") ||
                                    finalErrorMsgLower.contains("permission denied") ||
                                    finalErrorMsg.contains("PERMISSION_DENIED") ||
                                    finalErrorMsgLower.contains("missing permission") ||
                                    finalErrorMsgLower.contains("missing_permission");
                                
                                if (isPermissionError) {
                                    Log.d("CloudSync", "Error de permisos en pushSubjects (silenciado): " + finalErrorMsg);
                                    // Continuar como si fuera exitoso
                                    cleaned.add(s.id);
                                    if (left.decrementAndGet() == 0) {
                                        new Thread(() -> {
                                            subjectDao.markClean(cleaned);
                                            for (String idToDelete : toDelete) {
                                                subjectDao.deletePermanently(idToDelete);
                                                Log.d("CloudSync", "Sujeto borrado eliminado físicamente después de sync: " + idToDelete);
                                            }
                                            if (ok != null) ok.run();
                                        }).start();
                                    }
                                } else {
                                    if (err != null) err.run(e);
                                }
                            });
                }
            } catch (Exception e) {
                // Verificar si es error de permisos antes de propagar
                String errorMsg = e != null ? e.getMessage() : "null";
                String errorMsgLower = errorMsg != null ? errorMsg.toLowerCase() : "";
                boolean isPermissionError = 
                    (errorMsgLower.contains("permission") && (errorMsgLower.contains("denied") || errorMsgLower.contains("missing"))) ||
                    errorMsgLower.contains("permission_denied") ||
                    errorMsgLower.contains("permission denied") ||
                    errorMsg.contains("PERMISSION_DENIED") ||
                    errorMsgLower.contains("missing permission") ||
                    errorMsgLower.contains("missing_permission");
                
                if (isPermissionError) {
                    Log.d("CloudSync", "Error de permisos en pushSubjects catch (silenciado): " + errorMsg);
                    if (ok != null) ok.run();
                } else {
                    if (err != null) err.run(e);
                }
            }
        }).start();
    }

    /** Baja de Firestore los subjects con updatedAt > lastLocalUpdated. */
    public void pullSubjects(Ok ok, @Nullable Err err) {
        new Thread(() -> {
            Long lastLong = subjectDao.lastUpdatedForApp(appType);
            final long last = (lastLong != null ? lastLong : 0L);
            Log.d("CloudSync", "pullSubjects: uid=" + uid + ", app=" + app + ", appType=" + appType + ", last=" + last);
            
            // Primero, asegurar que el documento padre existe
            fs.collection("users").document(uid)
                    .collection("apps").document(app)
                    .set(Collections.singletonMap("createdAt", System.currentTimeMillis()), SetOptions.merge())
                    .addOnSuccessListener(a -> {
                        Log.d("CloudSync", "Documento padre creado/actualizado: users/" + uid + "/apps/" + app);
                        // Si es la primera sincronización (last = 0), traer todos los sujetos sin filtro de updatedAt
                        // para asegurar que se importen todos los datos
                        if (last == 0L) {
                            Log.d("CloudSync", "Primera sincronización (last=0), trayendo todos los sujetos sin filtro de updatedAt");
                            subjectsCol()
                                    .whereEqualTo("appType", appType)
                                    .get()
                                    .addOnSuccessListener(qs -> {
                                        Log.d("CloudSync", "✅ Query exitosa: " + qs.size() + " sujetos encontrados");
                                        new Thread(() -> {
                                            int importedCount = 0;
                                            for (QueryDocumentSnapshot doc : qs) {
                                                Long del = doc.getLong("deleted");
                                                int deleted = (del == null ? 0 : del.intValue());
                                                
                                                // Si el sujeto está borrado en Firebase, eliminarlo físicamente de la base local
                                                if (deleted == 1) {
                                                    String subjectId = doc.getString("id");
                                                    if (subjectId != null) {
                                                        subjectDao.deletePermanently(subjectId);
                                                        Log.d("CloudSync", "Sujeto borrado eliminado físicamente: " + subjectId);
                                                    }
                                                } else {
                                                    // Si no está borrado, actualizar/insertar normalmente
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

                                                    s.deleted = 0;
                                                    s.dirty = 0; // limpio al bajar del cloud
                                                    subjectDao.insert(s);
                                                    importedCount++;
                                                    Log.d("CloudSync", "✅ Sujeto importado: " + s.name + " (ID: " + s.id + ")");
                                                }
                                            }
                                            Log.d("CloudSync", "✅ Total sujetos importados: " + importedCount + " de " + qs.size());
                                            if (ok != null) ok.run();
                                        }).start();
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e("CloudSync", "❌ Error en pullSubjects (primera sincronización): " + e.getMessage(), e);
                                        if (err != null) err.run(e);
                                    });
                            return;
                        }
                        
                        // Para sincronizaciones posteriores, usar el filtro de updatedAt
                        // Ahora hacer la consulta
                        subjectsCol()
                                .whereEqualTo("appType", appType)
                                .whereGreaterThan("updatedAt", last)
                                .get()
                                .addOnSuccessListener(qs -> {
                                    new Thread(() -> {
                                        for (QueryDocumentSnapshot doc : qs) {
                                            Long del = doc.getLong("deleted");
                                            int deleted = (del == null ? 0 : del.intValue());
                                            
                                            // Si el sujeto está borrado en Firebase, eliminarlo físicamente de la base local
                                            if (deleted == 1) {
                                                String subjectId = doc.getString("id");
                                                if (subjectId != null) {
                                                    subjectDao.deletePermanently(subjectId);
                                                    Log.d("CloudSync", "Sujeto borrado eliminado físicamente: " + subjectId);
                                                }
                                            } else {
                                                // Si no está borrado, actualizar/insertar normalmente
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

                                                s.deleted = 0;
                                                s.dirty = 0; // limpio al bajar del cloud
                                                subjectDao.insert(s);
                                            }
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
                                                            Long del = doc.getLong("deleted");
                                                            int deleted = (del == null ? 0 : del.intValue());
                                                            
                                                            // Si el sujeto está borrado en Firebase, eliminarlo físicamente de la base local
                                                            if (deleted == 1) {
                                                                String subjectId = doc.getString("id");
                                                                if (subjectId != null) {
                                                                    subjectDao.deletePermanently(subjectId);
                                                                    Log.d("CloudSync", "Sujeto borrado eliminado físicamente (fallback): " + subjectId);
                                                                }
                                                            } else {
                                                                // Si no está borrado, actualizar/insertar normalmente
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

                                                                s.deleted = 0;
                                                                s.dirty = 0;
                                                                subjectDao.insert(s);
                                                            }
                                                        }
                                                        if (ok != null) ok.run();
                                                    }).start();
                                                })
                                                .addOnFailureListener(e2 -> { 
                                                    Log.w("CloudSync", "Error en pullSubjects (sin whereGreaterThan): " + e2.getMessage(), e2);
                                                    String errorMsg2 = e2.getMessage();
                                                    
                                                    // Si el error es failed_precondition o permission_denied, puede ser que no haya datos aún
                                                    // o que las reglas no permitan la consulta. Intentar consulta sin filtros.
                                                    String errorMsg2Lower = errorMsg2 != null ? errorMsg2.toLowerCase() : "";
                                                    boolean isPermissionError2 = 
                                                        (errorMsg2Lower.contains("permission") && (errorMsg2Lower.contains("denied") || errorMsg2Lower.contains("missing"))) ||
                                                        errorMsg2Lower.contains("permission_denied") ||
                                                        errorMsg2Lower.contains("permission denied") ||
                                                        errorMsg2.contains("PERMISSION_DENIED") ||
                                                        errorMsg2Lower.contains("missing permission") ||
                                                        errorMsg2Lower.contains("missing_permission");
                                                    
                                                    if (errorMsg2 != null && (errorMsg2.contains("failed_precondition") || 
                                                                              errorMsg2.contains("FAILED_PRECONDITION") ||
                                                                              isPermissionError2)) {
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
                                                                    // PERMISSION_DENIED y missing permission son normales cuando no hay datos - no mostrar error
                                                                    String errorMsg3Lower = errorMsg3 != null ? errorMsg3.toLowerCase() : "";
                                                                    boolean isPermissionError3 = 
                                                                        (errorMsg3Lower.contains("permission") && (errorMsg3Lower.contains("denied") || errorMsg3Lower.contains("missing"))) ||
                                                                        errorMsg3Lower.contains("permission_denied") ||
                                                                        errorMsg3Lower.contains("permission denied") ||
                                                                        errorMsg3.contains("PERMISSION_DENIED") ||
                                                                        errorMsg3Lower.contains("missing permission") ||
                                                                        errorMsg3Lower.contains("missing_permission");
                                                                    
                                                                    if (errorMsg3 != null && isPermissionError3) {
                                                                        Log.d("CloudSync", "Error de permisos durante pullSubjects (sin datos). Continuando sin error.");
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
                                        String errorMsgLower = errorMsg != null ? errorMsg.toLowerCase() : "";
                                        boolean isPermissionError = 
                                            (errorMsgLower.contains("permission") && (errorMsgLower.contains("denied") || errorMsgLower.contains("missing"))) ||
                                            errorMsgLower.contains("permission_denied") ||
                                            errorMsgLower.contains("permission denied") ||
                                            errorMsg.contains("PERMISSION_DENIED") ||
                                            errorMsgLower.contains("missing permission") ||
                                            errorMsgLower.contains("missing_permission");
                                        
                                        if (errorMsg != null && isPermissionError) {
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
                    if (e.realizedAt != null) data.put("realizedAt", e.realizedAt);
                    
                    // Campos de repetición (opcionales)
                    if (e.repeatType != null) data.put("repeatType", e.repeatType);
                    if (e.repeatInterval != null) data.put("repeatInterval", e.repeatInterval);
                    if (e.repeatEndDate != null) data.put("repeatEndDate", e.repeatEndDate);
                    if (e.repeatCount != null) data.put("repeatCount", e.repeatCount);
                    if (e.originalEventId != null) data.put("originalEventId", e.originalEventId);
                    
                    // Campo de notificación (opcional)
                    if (e.notificationMinutesBefore != null) data.put("notificationMinutesBefore", e.notificationMinutesBefore);

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
                            .addOnFailureListener(ex -> { 
                                // Verificar si es error de permisos antes de propagar
                                String errorMsg = ex != null ? ex.getMessage() : "null";
                                String errorMsgLower = errorMsg != null ? errorMsg.toLowerCase() : "";
                                boolean isPermissionError = 
                                    (errorMsgLower.contains("permission") && (errorMsgLower.contains("denied") || errorMsgLower.contains("missing"))) ||
                                    errorMsgLower.contains("permission_denied") ||
                                    errorMsgLower.contains("permission denied") ||
                                    errorMsg.contains("PERMISSION_DENIED") ||
                                    errorMsgLower.contains("missing permission") ||
                                    errorMsgLower.contains("missing_permission");
                                
                                if (isPermissionError) {
                                    Log.d("CloudSync", "Error de permisos en push events (silenciado): " + errorMsg);
                                    // Continuar como si fuera exitoso
                                    cleaned.add(e.id);
                                    if (left.decrementAndGet() == 0) {
                                        new Thread(() -> {
                                            eventDao.markClean(cleaned);
                                            if (ok != null) ok.run();
                                        }).start();
                                    }
                                } else {
                                    if (err != null) err.run(ex);
                                }
                            });
                }
            } catch (Exception ex) {
                // Verificar si es error de permisos antes de propagar
                String errorMsg = ex != null ? ex.getMessage() : "null";
                String errorMsgLower = errorMsg != null ? errorMsg.toLowerCase() : "";
                boolean isPermissionError = 
                    (errorMsgLower.contains("permission") && (errorMsgLower.contains("denied") || errorMsgLower.contains("missing"))) ||
                    errorMsgLower.contains("permission_denied") ||
                    errorMsgLower.contains("permission denied") ||
                    errorMsg.contains("PERMISSION_DENIED") ||
                    errorMsgLower.contains("missing permission") ||
                    errorMsgLower.contains("missing_permission");
                
                if (isPermissionError) {
                    Log.d("CloudSync", "Error de permisos en push catch (silenciado): " + errorMsg);
                    if (ok != null) ok.run();
                } else {
                    if (err != null) err.run(ex);
                }
            }
        }).start();
    }

    /** Baja de Firestore los events con updatedAt > lastLocalUpdated. */
    public void pull(Ok ok, @Nullable Err err) {
        new Thread(() -> {
            Long lastLong = eventDao.lastUpdatedForApp(appType);
            final long last = (lastLong != null ? lastLong : 0L);
            Log.d("CloudSync", "pull events: uid=" + uid + ", app=" + app + ", appType=" + appType + ", last=" + last);
            
            // Primero, asegurar que el documento padre existe
            fs.collection("users").document(uid)
                    .collection("apps").document(app)
                    .set(Collections.singletonMap("createdAt", System.currentTimeMillis()), SetOptions.merge())
                    .addOnSuccessListener(a -> {
                        // Si es la primera sincronización (last = 0), traer todos los eventos sin filtro de updatedAt
                        // para asegurar que se importen todos los datos
                        if (last == 0L) {
                            Log.d("CloudSync", "Primera sincronización (last=0), trayendo todos los eventos sin filtro de updatedAt");
                            eventsCol()
                                    .whereEqualTo("appType", appType)
                                    .get()
                                    .addOnSuccessListener(qs -> {
                                        Log.d("CloudSync", "✅ Query eventos exitosa: " + qs.size() + " eventos encontrados");
                                        new Thread(() -> {
                                            int importedCount = 0;
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
                                                
                                                Long realizedAt = doc.getLong("realizedAt");
                                                e.realizedAt = realizedAt;
                                                
                                                // Campos de repetición (opcionales)
                                                e.repeatType = doc.getString("repeatType");
                                                Long repeatInterval = doc.getLong("repeatInterval");
                                                e.repeatInterval = (repeatInterval == null ? null : repeatInterval.intValue());
                                                Long repeatEndDate = doc.getLong("repeatEndDate");
                                                e.repeatEndDate = (repeatEndDate == null ? null : repeatEndDate);
                                                Long repeatCount = doc.getLong("repeatCount");
                                                e.repeatCount = (repeatCount == null ? null : repeatCount.intValue());
                                                e.originalEventId = doc.getString("originalEventId");
                                                
                                                // Campo de notificación (opcional)
                                                Long notificationMinutesBefore = doc.getLong("notificationMinutesBefore");
                                                e.notificationMinutesBefore = (notificationMinutesBefore == null ? null : notificationMinutesBefore.intValue());

                                                e.dirty = 0; // limpio al bajar del cloud
                                                eventDao.insert(e);
                                                importedCount++;
                                            }
                                            Log.d("CloudSync", "✅ Total eventos importados: " + importedCount + " de " + qs.size());
                                            if (ok != null) ok.run();
                                        }).start();
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e("CloudSync", "❌ Error en pull events (primera sincronización): " + e.getMessage(), e);
                                        if (err != null) err.run(e);
                                    });
                            return;
                        }
                        
                        // Para sincronizaciones posteriores, usar el filtro de updatedAt
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
                                            
                                            Long realizedAt = doc.getLong("realizedAt");
                                            e.realizedAt = realizedAt;
                                            
                                            // Campos de repetición (opcionales)
                                            e.repeatType = doc.getString("repeatType");
                                            Long repeatInterval = doc.getLong("repeatInterval");
                                            e.repeatInterval = (repeatInterval == null ? null : repeatInterval.intValue());
                                            Long repeatEndDate = doc.getLong("repeatEndDate");
                                            e.repeatEndDate = (repeatEndDate == null ? null : repeatEndDate);
                                            Long repeatCount = doc.getLong("repeatCount");
                                            e.repeatCount = (repeatCount == null ? null : repeatCount.intValue());
                                            e.originalEventId = doc.getString("originalEventId");
                                            
                                            // Campo de notificación (opcional)
                                            Long notificationMinutesBefore = doc.getLong("notificationMinutesBefore");
                                            e.notificationMinutesBefore = (notificationMinutesBefore == null ? null : notificationMinutesBefore.intValue());

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
                                    // Verificar si es un error que podemos ignorar antes de intentar el fallback
                                    if (errorMsg != null && (errorMsg.contains("permission_denied") || 
                                                             errorMsg.contains("PERMISSION_DENIED"))) {
                                        // PERMISSION_DENIED es normal cuando no hay datos - no mostrar error
                                        Log.d("CloudSync", "PERMISSION_DENIED durante pull (sin datos). Continuando sin error.");
                                        if (ok != null) ok.run();
                                    } else {
                                        // Intentar consulta sin whereGreaterThan
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
                                                            
                                                            Long realizedAt = doc.getLong("realizedAt");
                                                            event.realizedAt = realizedAt;
                                                            
                                                            // Campos de repetición (opcionales)
                                                            event.repeatType = doc.getString("repeatType");
                                                            Long repeatInterval = doc.getLong("repeatInterval");
                                                            event.repeatInterval = (repeatInterval == null ? null : repeatInterval.intValue());
                                                            Long repeatEndDate = doc.getLong("repeatEndDate");
                                                            event.repeatEndDate = (repeatEndDate == null ? null : repeatEndDate);
                                                            Long repeatCount = doc.getLong("repeatCount");
                                                            event.repeatCount = (repeatCount == null ? null : repeatCount.intValue());
                                                            event.originalEventId = doc.getString("originalEventId");
                                                            
                                                            // Campo de notificación (opcional)
                                                            Long notificationMinutesBefore = doc.getLong("notificationMinutesBefore");
                                                            event.notificationMinutesBefore = (notificationMinutesBefore == null ? null : notificationMinutesBefore.intValue());

                                                            event.dirty = 0;
                                                            eventDao.insert(event);
                                                        }
                                                        if (ok != null) ok.run();
                                                    }).start();
                                                })
                                                .addOnFailureListener(e2 -> { 
                                                    Log.w("CloudSync", "Error en pull (sin whereGreaterThan): " + e2.getMessage(), e2);
                                                    String errorMsg2 = e2 != null ? e2.getMessage() : "null";
                                                    
                                                    // Si el error es failed_precondition o permission_denied, puede ser que no haya datos aún
                                                    String errorMsg2Lower = errorMsg2 != null ? errorMsg2.toLowerCase() : "";
                                                    boolean isPermissionError2 = 
                                                        (errorMsg2Lower.contains("permission") && (errorMsg2Lower.contains("denied") || errorMsg2Lower.contains("missing"))) ||
                                                        errorMsg2Lower.contains("permission_denied") ||
                                                        errorMsg2Lower.contains("permission denied") ||
                                                        errorMsg2.contains("PERMISSION_DENIED") ||
                                                        errorMsg2Lower.contains("missing permission") ||
                                                        errorMsg2Lower.contains("missing_permission");
                                                    
                                                    if (errorMsg2 != null && (errorMsg2.contains("failed_precondition") || 
                                                                              errorMsg2.contains("FAILED_PRECONDITION") ||
                                                                              isPermissionError2)) {
                                                        Log.d("CloudSync", "Intentando consulta sin filtros para verificar acceso...");
                                                        eventsCol().limit(1).get()
                                                                .addOnSuccessListener(qs -> {
                                                                    Log.d("CloudSync", "Acceso verificado. Continuando sin datos (primera sincronización o sin datos)");
                                                                    if (ok != null) ok.run();
                                                                })
                                                                .addOnFailureListener(e3 -> {
                                                                    String errorMsg3 = e3 != null ? e3.getMessage() : "null";
                                                                    // PERMISSION_DENIED y missing permission son normales cuando no hay datos - no mostrar error
                                                                    String errorMsg3Lower = errorMsg3 != null ? errorMsg3.toLowerCase() : "";
                                                                    boolean isPermissionError3 = 
                                                                        (errorMsg3Lower.contains("permission") && (errorMsg3Lower.contains("denied") || errorMsg3Lower.contains("missing"))) ||
                                                                        errorMsg3Lower.contains("permission_denied") ||
                                                                        errorMsg3Lower.contains("permission denied") ||
                                                                        errorMsg3.contains("PERMISSION_DENIED") ||
                                                                        errorMsg3Lower.contains("missing permission") ||
                                                                        errorMsg3Lower.contains("missing_permission");
                                                                    
                                                                    if (errorMsg3 != null && isPermissionError3) {
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
                                    }
                                });
                    })
                    .addOnFailureListener(e -> { 
                        String errorMsg = e != null ? e.getMessage() : "null";
                        Log.w("CloudSync", "Error al crear documento padre en pull: " + errorMsg, e);
                        // Verificar si es PERMISSION_DENIED antes de propagar el error
                        String errorMsgLower = errorMsg != null ? errorMsg.toLowerCase() : "";
                        boolean isPermissionError = 
                            (errorMsgLower.contains("permission") && (errorMsgLower.contains("denied") || errorMsgLower.contains("missing"))) ||
                            errorMsgLower.contains("permission_denied") ||
                            errorMsgLower.contains("permission denied") ||
                            errorMsg.contains("PERMISSION_DENIED") ||
                            errorMsgLower.contains("missing permission") ||
                            errorMsgLower.contains("missing_permission");
                        
                        if (isPermissionError) {
                            Log.d("CloudSync", "PERMISSION_DENIED al crear documento padre (sin datos). Continuando sin error.");
                            if (ok != null) ok.run();
                        } else {
                            if (err != null) err.run(e);
                        }
                    });
        }).start();
    }
}

