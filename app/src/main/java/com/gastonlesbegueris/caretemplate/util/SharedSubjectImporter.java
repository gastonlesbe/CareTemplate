package com.gastonlesbegueris.caretemplate.util;

import android.content.Context;
import com.gastonlesbegueris.caretemplate.data.local.AppDb;
import com.gastonlesbegueris.caretemplate.data.local.EventDao;
import com.gastonlesbegueris.caretemplate.data.local.EventEntity;
import com.gastonlesbegueris.caretemplate.data.local.SubjectDao;
import com.gastonlesbegueris.caretemplate.data.local.SubjectEntity;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Helper para importar sujetos compartidos desde shared_subjects.
 * Lee los datos completos directamente desde shared_subjects (más seguro).
 */
public class SharedSubjectImporter {
    
    private final Context context;
    private final SubjectDao subjectDao;
    private final EventDao eventDao;
    
    public SharedSubjectImporter(Context context) {
        this.context = context.getApplicationContext();
        AppDb db = AppDb.get(this.context);
        this.subjectDao = db.subjectDao();
        this.eventDao = db.eventDao();
    }
    
    /**
     * Importa un sujeto y sus eventos desde los datos compartidos
     */
    public void importFromSharedData(Map<String, Object> subjectData, List<Map<String, Object>> eventsData, Runnable onSuccess, Runnable onError) {
        // Crear una variable final para usar en el Thread
        final List<Map<String, Object>> finalEventsData = (eventsData == null) ? new java.util.ArrayList<>() : eventsData;
        
        new Thread(() -> {
            try {
                // Obtener userId del usuario que recibe
                String currentUserId = getCurrentUserId();
                if (currentUserId == null) {
                    android.util.Log.e("SharedSubjectImporter", "❌ No se pudo obtener userId del usuario actual");
                    if (onError != null) {
                        onError.run();
                    }
                    return;
                }
                
                // Crear nuevo sujeto local con los datos del compartido
                // Asignar al usuario que recibe: generar ID único con su userId
                SubjectEntity newSubject = new SubjectEntity();
                newSubject.id = currentUserId + "_" + System.currentTimeMillis(); // ID único por usuario
                newSubject.uid = currentUserId; // Asignar uid del usuario que recibe
                newSubject.appType = (String) subjectData.get("appType");
                newSubject.name = (String) subjectData.get("name");
                
                Object bd = subjectData.get("birthDate");
                newSubject.birthDate = (bd instanceof Long) ? (Long) bd : (bd instanceof Number) ? ((Number) bd).longValue() : null;
                
                Object cm = subjectData.get("currentMeasure");
                newSubject.currentMeasure = (cm instanceof Double) ? (Double) cm : (cm instanceof Number) ? ((Number) cm).doubleValue() : null;
                
                newSubject.notes = (String) subjectData.get("notes");
                newSubject.iconKey = (String) subjectData.get("iconKey");
                newSubject.colorHex = (String) subjectData.get("colorHex");
                newSubject.updatedAt = System.currentTimeMillis();
                newSubject.deleted = 0;
                newSubject.dirty = 1; // Marcar para sincronizar
                
                // Insertar el sujeto
                subjectDao.insert(newSubject);
                android.util.Log.d("SharedSubjectImporter", "✅ Sujeto insertado: " + newSubject.name + " (ID: " + newSubject.id + ")");
                
                // Importar eventos del sujeto compartido
                android.util.Log.d("SharedSubjectImporter", "📊 Importando " + finalEventsData.size() + " eventos para el sujeto: " + newSubject.name);
                
                int importedCount = 0;
                for (Map<String, Object> eventData : finalEventsData) {
                    try {
                        EventEntity event = new EventEntity();
                        event.id = UUID.randomUUID().toString(); // Nuevo ID
                        event.uid = currentUserId; // ID del usuario que recibe
                        String eventAppType = (String) eventData.get("appType");
                        if (eventAppType == null || eventAppType.isEmpty()) {
                            eventAppType = newSubject.appType; // fallback para datos antiguos
                        }
                        event.appType = eventAppType;
                        event.subjectId = newSubject.id; // ID del nuevo sujeto
                        event.title = (String) eventData.get("title");
                        if (event.title == null || event.title.trim().isEmpty()) {
                            event.title = "Evento";
                        }
                        event.note = (String) eventData.get("note");
                        
                        Object due = eventData.get("dueAt");
                        event.dueAt = (due instanceof Long) ? (Long) due : (due instanceof Number) ? ((Number) due).longValue() : System.currentTimeMillis();
                        
                        Object up = eventData.get("updatedAt");
                        event.updatedAt = (up instanceof Long) ? (Long) up : (up instanceof Number) ? ((Number) up).longValue() : System.currentTimeMillis();
                        
                        Object del = eventData.get("deleted");
                        if (del instanceof Boolean) {
                            event.deleted = ((Boolean) del) ? 1 : 0;
                        } else {
                            event.deleted = (del instanceof Long) ? ((Long) del).intValue() : (del instanceof Number) ? ((Number) del).intValue() : 0;
                        }
                        
                        Object real = eventData.get("realized");
                        if (real instanceof Boolean) {
                            event.realized = ((Boolean) real) ? 1 : 0;
                        } else {
                            event.realized = (real instanceof Long) ? ((Long) real).intValue() : (real instanceof Number) ? ((Number) real).intValue() : 0;
                        }
                        
                        Object realAt = eventData.get("realizedAt");
                        event.realizedAt = (realAt instanceof Long) ? (Long) realAt : (realAt instanceof Number) ? ((Number) realAt).longValue() : null;
                        
                        Object cost = eventData.get("cost");
                        event.cost = (cost instanceof Double) ? (Double) cost : (cost instanceof Number) ? ((Number) cost).doubleValue() : null;
                        
                        Object km = eventData.get("kilometersAtEvent");
                        event.kilometersAtEvent = (km instanceof Double) ? (Double) km : (km instanceof Number) ? ((Number) km).doubleValue() : null;
                        
                        event.repeatType = (String) eventData.get("repeatType");
                        
                        Object repInt = eventData.get("repeatInterval");
                        Integer repeatInterval = null;
                        if (repInt instanceof Number) {
                            repeatInterval = ((Number) repInt).intValue();
                        } else if (repInt instanceof String) {
                            try {
                                repeatInterval = Integer.parseInt((String) repInt);
                            } catch (Exception ignored) {
                                repeatInterval = null;
                            }
                        }
                        event.repeatInterval = repeatInterval;
                        
                        Object repEnd = eventData.get("repeatEndDate");
                        event.repeatEndDate = (repEnd instanceof Long) ? (Long) repEnd : (repEnd instanceof Number) ? ((Number) repEnd).longValue() : null;
                        
                        Object repCount = eventData.get("repeatCount");
                        Integer repeatCount = null;
                        if (repCount instanceof Number) {
                            repeatCount = ((Number) repCount).intValue();
                        } else if (repCount instanceof String) {
                            try {
                                repeatCount = Integer.parseInt((String) repCount);
                            } catch (Exception ignored) {
                                repeatCount = null;
                            }
                        }
                        event.repeatCount = repeatCount;
                        
                        event.originalEventId = (String) eventData.get("originalEventId");
                        
                        Object notifMin = eventData.get("notificationMinutesBefore");
                        Integer notificationMinutesBefore = null;
                        if (notifMin instanceof Number) {
                            notificationMinutesBefore = ((Number) notifMin).intValue();
                        } else if (notifMin instanceof String) {
                            try {
                                notificationMinutesBefore = Integer.parseInt((String) notifMin);
                            } catch (Exception ignored) {
                                notificationMinutesBefore = null;
                            }
                        }
                        event.notificationMinutesBefore = notificationMinutesBefore;
                        
                        event.dirty = 1; // Marcar para sincronizar
                        
                        eventDao.insert(event);
                        importedCount++;
                        android.util.Log.d("SharedSubjectImporter", "  ✅ Evento insertado: " + event.title + " (realized: " + event.realized + ")");
                    } catch (Exception e) {
                        android.util.Log.e("SharedSubjectImporter", "❌ Error insertando evento: " + e.getMessage(), e);
                    }
                }
                
                android.util.Log.d("SharedSubjectImporter", "✅ Total eventos importados: " + importedCount + " de " + finalEventsData.size());
                
                if (!finalEventsData.isEmpty() && importedCount == 0) {
                    android.util.Log.e("SharedSubjectImporter", "❌ Ningún evento pudo importarse aunque venían " + finalEventsData.size() + " eventos");
                    if (onError != null) {
                        onError.run();
                    }
                    return;
                }
                
                if (onSuccess != null) {
                    onSuccess.run();
                }
            } catch (Exception e) {
                if (onError != null) {
                    onError.run();
                }
            }
        }).start();
    }
    
    /**
     * Obtiene el ID del usuario actual desde SharedPreferences
     */
    private String getCurrentUserId() {
        // 1) Preferir UserManager (usa la misma estrategia del resto de la app)
        String userId = new UserManager(context).getUserIdSync();
        if (userId != null && !userId.trim().isEmpty()) {
            return userId;
        }
        
        // 2) Fallback a FirebaseAuth si está autenticado
        com.google.firebase.auth.FirebaseUser firebaseUser = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser != null && firebaseUser.getUid() != null && !firebaseUser.getUid().trim().isEmpty()) {
            return firebaseUser.getUid();
        }
        
        // 3) Compatibilidad con claves legacy en SharedPreferences
        android.content.SharedPreferences prefs = context.getSharedPreferences("user_prefs", android.content.Context.MODE_PRIVATE);
        String prefsUserId = prefs.getString("user_id", null); // clave actual
        if (prefsUserId == null || prefsUserId.trim().isEmpty()) {
            prefsUserId = prefs.getString("userId", null); // clave legacy
        }
        if (prefsUserId == null || prefsUserId.trim().isEmpty()) {
            prefsUserId = prefs.getString("firebase_uid", null);
        }
        
        return (prefsUserId != null && !prefsUserId.trim().isEmpty()) ? prefsUserId : null;
    }
}
