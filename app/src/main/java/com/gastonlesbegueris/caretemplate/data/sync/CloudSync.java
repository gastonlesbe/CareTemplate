package com.gastonlesbegueris.caretemplate.data.sync;

import androidx.annotation.Nullable;

import com.gastonlesbegueris.caretemplate.data.local.EventDao;
import com.gastonlesbegueris.caretemplate.data.local.EventEntity;
import com.gastonlesbegueris.caretemplate.data.local.SubjectDao;
import com.gastonlesbegueris.caretemplate.data.local.SubjectEntity;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

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
                    .addOnFailureListener(e -> { if (err != null) err.run(e); });
        }).start();
    }

    // ======================
    //       EVENTS
    // ======================

    /** Sube a Firestore todos los events con dirty=1 del appType actual. */
    public void push(Ok ok, @Nullable Err err) {
        new Thread(() -> {
            try {
                final List<EventEntity> dirty = eventDao.listDirty(appType);
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
                    data.put("cost", e.cost);
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

                                Long realized = doc.getLong("realized");
                                e.realized = (realized == null ? 0 : realized.intValue());

                                e.dirty = 0; // limpio al bajar del cloud
                                eventDao.insert(e);
                            }
                            if (ok != null) ok.run();
                        }).start();
                    })
                    .addOnFailureListener(e -> { if (err != null) err.run(e); });
        }).start();
    }
}
