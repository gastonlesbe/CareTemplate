package com.gastonlesbegueris.caretemplate.data.remote;

import static java.util.stream.Collectors.toMap;

import com.gastonlesbegueris.caretemplate.BuildConfig;
import com.gastonlesbegueris.caretemplate.data.model.Event;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Maneja la conexión con Firestore:
 *  - Alta, modificación y borrado lógico de eventos
 *  - Lectura en tiempo real (snapshot listener)
 *  - Filtrado automático por appType (pets, cars o family)
 */
public class EventService {

    private final FirebaseFirestore db;
    private final String uid;
    private final String appType;

    public ListenerRegistration listen(EventsListener eventsListener) {
        return null;
    }

    // Interface para devolver resultados al Activity (callback simple)
    public interface EventsListener {
        void onChanged(List<Event> events);

        void onError(Exception e);
    }

    public interface Callback {
        void onSuccess();

        void onError(Exception e);
    }

    public EventService() {
        this.db = FirebaseFirestore.getInstance();
        this.uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;
        this.appType = BuildConfig.FLAVOR; // "pets" | "cars" | "family"
    }

    // Colección principal: users/{uid}/events
    private CollectionReference col() {
        if (uid == null) throw new IllegalStateException("UID null en EventService.col()");
        return db.collection("users").document(uid).collection("events");
    }


    /**
     * Crea un nuevo evento
     */
    public void add(String title, long dueAt, String note, final Callback cb) {
        if (uid == null) {
            cb.onError(new IllegalStateException("UID null: no hay usuario autenticado aún"));
            return;
        }
        // Normalizamos valores para evitar nulls
        String safeTitle = (title == null) ? "" : title.trim();
        String safeNote  = (note  == null) ? "" : note;
        String safeApp   = (appType == null) ? "" : appType;

        Map<String, Object> data = new HashMap<>();
        data.put("title", safeTitle);
        data.put("dueAt", dueAt);
        data.put("note", safeNote);
        data.put("updatedAt", System.currentTimeMillis());
        data.put("deleted", 0);
        data.put("appType", safeApp);

        // IMPORTANTE: 'data' nunca es null
        col().add(data)
                .addOnSuccessListener(ref -> cb.onSuccess())
                .addOnFailureListener(cb::onError);
    }


    private Object toMap(Event e) {
        return null;
    }

    /**
     * Actualiza un evento existente
     */
    public void update(String id, String title, long dueAt, String note, final Callback cb) {
        Map<String, Object> up = new HashMap<>();
        up.put("title", title);
        up.put("dueAt", dueAt);
        up.put("note", note);
        up.put("updatedAt", System.currentTimeMillis());

        col().document(id).set(up, SetOptions.merge())
                .addOnSuccessListener(v -> cb.onSuccess())
                .addOnFailureListener(cb::onError);
    }

    public void softDelete(String id, final Callback cb) {
        Map<String, Object> up = new HashMap<>();
        up.put("deleted", 1);
        up.put("updatedAt", System.currentTimeMillis());

        col().document(id).set(up, SetOptions.merge())
                .addOnSuccessListener(v -> cb.onSuccess())
                .addOnFailureListener(cb::onError);
    }

}

    /** Borrado lógico: marca el evento como eliminado */


