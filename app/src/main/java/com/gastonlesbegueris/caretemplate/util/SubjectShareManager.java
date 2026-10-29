package com.gastonlesbegueris.caretemplate.util;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.gastonlesbegueris.caretemplate.data.local.AppDb;
import com.gastonlesbegueris.caretemplate.data.local.EventDao;
import com.gastonlesbegueris.caretemplate.data.local.EventEntity;
import com.gastonlesbegueris.caretemplate.data.local.SubjectDao;
import com.gastonlesbegueris.caretemplate.data.local.SubjectEntity;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Gestiona el compartir de sujetos individuales entre usuarios.
 * 
 * Estrategia:
 * 1. Genera un código de compartir único para un sujeto específico
 * 2. Vincula el código con el subjectId y userId del dueño en Firestore
 * 3. Permite que otro usuario recupere el sujeto usando el código
 */
public class SubjectShareManager {
    
    private static final String TAG = "SubjectShareManager";
    
    private final Context context;
    private final FirebaseFirestore firestore;
    private final SubjectDao subjectDao;
    private final EventDao eventDao;
    
    public SubjectShareManager(Context context) {
        this.context = context.getApplicationContext();
        this.firestore = FirebaseFirestore.getInstance();
        AppDb db = AppDb.get(this.context);
        this.subjectDao = db.subjectDao();
        this.eventDao = db.eventDao();
    }
    
    /**
     * Genera un código de compartir para un sujeto específico.
     * Copia todos los datos del sujeto y sus eventos a shared_subjects para mayor seguridad.
     * 
     * @param subjectId ID del sujeto a compartir
     * @param userId ID del usuario dueño del sujeto
     * @param callback Callback con el código de compartir
     */
    public void generateShareCode(String subjectId, String userId, @Nullable ShareCodeCallback callback) {
        if (subjectId == null || subjectId.isEmpty() || userId == null || userId.isEmpty()) {
            if (callback != null) {
                callback.onError(new IllegalArgumentException("SubjectId y userId son requeridos"));
            }
            return;
        }
        
        // Verificar autenticación antes de escribir
        com.google.firebase.auth.FirebaseUser currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Log.e(TAG, "Usuario no autenticado al intentar compartir");
            if (callback != null) {
                callback.onError(new Exception("Usuario no autenticado. Por favor, intenta de nuevo."));
            }
            return;
        }
        
        // Generar código único (formato: XXXX-XXXX-XXXX)
        String shareCode = generateShareCode();
        String normalizedCode = shareCode.replaceAll("[\\s-]", "").toUpperCase();
        
        Log.d(TAG, "Generando código compartido. Usuario: " + currentUser.getUid());
        Log.d(TAG, "Ruta: /shared_subjects/" + normalizedCode);
        
        // Leer el sujeto y eventos desde la base de datos local (más confiable)
        android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
        new Thread(() -> {
            try {
                // Paso 1: Leer el sujeto desde la base de datos local
                SubjectEntity subject = subjectDao.findOne(subjectId);
                if (subject == null) {
                    Log.e(TAG, "Sujeto no encontrado localmente: " + subjectId);
                    mainHandler.post(() -> {
                        if (callback != null) {
                            callback.onError(new Exception("Sujeto no encontrado"));
                        }
                    });
                    return;
                }
                
                // Paso 2: Leer todos los eventos del sujeto desde la base de datos local
                List<EventEntity> events = eventDao.listAllForSubject(subject.appType, subjectId);
                if (events == null || events.isEmpty()) {
                    // Fallback: sujetos antiguos pueden no tener appType en eventos
                    events = eventDao.listAllForSubjectAnyApp(subjectId);
                }
                if (events == null) {
                    events = new java.util.ArrayList<>();
                }
                
                // Paso 3: Construir el documento completo con todos los datos
                Map<String, Object> shareData = new HashMap<>();
                
                // Metadatos
                shareData.put("code", normalizedCode);
                shareData.put("subjectId", subjectId);
                shareData.put("ownerUserId", userId);
                shareData.put("createdAt", System.currentTimeMillis());
                shareData.put("updatedAt", System.currentTimeMillis());
                
                // Copiar todos los campos del sujeto
                Map<String, Object> subjectData = new HashMap<>();
                subjectData.put("id", subject.id);
                subjectData.put("uid", subject.uid != null ? subject.uid : userId); // Incluir uid del dueño original
                subjectData.put("appType", subject.appType);
                subjectData.put("name", subject.name);
                subjectData.put("birthDate", subject.birthDate);
                subjectData.put("currentMeasure", subject.currentMeasure);
                subjectData.put("notes", subject.notes);
                subjectData.put("iconKey", subject.iconKey);
                subjectData.put("colorHex", subject.colorHex);
                subjectData.put("updatedAt", subject.updatedAt);
                subjectData.put("deleted", subject.deleted);
                shareData.put("subject", subjectData);
                
                // Copiar todos los eventos
                java.util.List<Map<String, Object>> eventsList = new java.util.ArrayList<>();
                int realizedCount = 0;
                int pendingCount = 0;
                for (EventEntity event : events) {
                    if (event.realized == 1) {
                        realizedCount++;
                    } else {
                        pendingCount++;
                    }
                    Map<String, Object> eventData = new HashMap<>();
                    eventData.put("id", event.id);
                    eventData.put("uid", event.uid);
                    eventData.put("appType", event.appType);
                    eventData.put("subjectId", event.subjectId);
                    eventData.put("title", event.title);
                    eventData.put("note", event.note);
                    eventData.put("dueAt", event.dueAt);
                    eventData.put("realized", event.realized);
                    eventData.put("realizedAt", event.realizedAt);
                    eventData.put("cost", event.cost);
                    eventData.put("kilometersAtEvent", event.kilometersAtEvent);
                    eventData.put("repeatType", event.repeatType);
                    eventData.put("repeatInterval", event.repeatInterval);
                    eventData.put("repeatEndDate", event.repeatEndDate);
                    eventData.put("repeatCount", event.repeatCount);
                    eventData.put("originalEventId", event.originalEventId);
                    eventData.put("notificationMinutesBefore", event.notificationMinutesBefore);
                    eventData.put("updatedAt", event.updatedAt);
                    eventData.put("deleted", event.deleted);
                    eventsList.add(eventData);
                }
                shareData.put("events", eventsList);
                Log.d(TAG, "📦 Eventos preparados para compartir: " + eventsList.size() + " (realizados: " + realizedCount + ", pendientes: " + pendingCount + ")");
                
                // Paso 4: Guardar en shared_subjects (en el hilo principal)
                mainHandler.post(() -> {
                    DocumentReference shareDoc = firestore.collection("shared_subjects")
                            .document(normalizedCode);
                    
                    shareDoc.set(shareData, SetOptions.merge())
                            .addOnSuccessListener(aVoid -> {
                                Log.d(TAG, "✅ Share code generated and saved with complete data: " + shareCode);
                                if (callback != null) {
                                    callback.onShareCode(shareCode);
                                }
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "❌ Error saving share code", e);
                                String errorMsg = e != null ? e.getMessage() : "Error desconocido";
                                if (errorMsg != null && errorMsg.contains("PERMISSION_DENIED")) {
                                    errorMsg = "Error de permisos en Firestore. Por favor, verifica las reglas de seguridad en Firebase Console. " +
                                              "Asegúrate de que la regla 'match /shared_subjects/{code}' esté al nivel superior con 'allow write: if request.auth != null;'";
                                }
                                if (callback != null) {
                                    callback.onError(new Exception(errorMsg));
                                }
                            });
                });
            } catch (Exception e) {
                Log.e(TAG, "❌ Error reading subject/events from local database", e);
                mainHandler.post(() -> {
                    if (callback != null) {
                        callback.onError(new Exception("Error al leer el sujeto: " + e.getMessage()));
                    }
                });
            }
        }).start();
    }
    
    /**
     * Recupera la información de un sujeto compartido usando el código.
     * 
     * @param shareCode Código de compartir proporcionado
     * @param callback Callback con la información del sujeto compartido
     */
    public void getSharedSubject(String shareCode, @Nullable SharedSubjectCallback callback) {
        if (shareCode == null || shareCode.isEmpty()) {
            if (callback != null) {
                callback.onError(new IllegalArgumentException("Share code cannot be empty"));
            }
            return;
        }
        
        // Normalizar el código
        String normalizedCode = shareCode.replaceAll("[\\s-]", "").toUpperCase();
        Log.d(TAG, "═══════════════════════════════════════════════════════════");
        Log.d(TAG, "BÚSQUEDA DE CÓDIGO COMPARTIDO");
        Log.d(TAG, "Código original: " + shareCode);
        Log.d(TAG, "Código normalizado: " + normalizedCode);
        Log.d(TAG, "Longitud: " + normalizedCode.length() + " caracteres");
        Log.d(TAG, "═══════════════════════════════════════════════════════════");
        
        // Asegurar autenticación antes de leer de Firestore
        ensureAuthentication(() -> {
            // Verificar que la autenticación se completó
            com.google.firebase.auth.FirebaseUser currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser != null) {
                Log.d(TAG, "✅ Autenticación confirmada antes de buscar: " + currentUser.getUid());
            } else {
                Log.e(TAG, "❌ ERROR: Autenticación no completada antes de buscar");
            }
            
            // Estrategia 1: Buscar directamente por ID del documento (normalizado)
            searchByDocumentId(normalizedCode, callback);
        }, callback);
    }
    
    /**
     * Busca el código compartido por ID del documento
     */
    private void searchByDocumentId(String normalizedCode, @Nullable SharedSubjectCallback callback) {
        Log.d(TAG, "Estrategia 1: Buscando por ID del documento: " + normalizedCode);
        
        firestore.collection("shared_subjects")
                .document(normalizedCode)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Log.d(TAG, "✅ Código encontrado por ID del documento");
                        processFoundDocument(documentSnapshot, callback);
                        return;
                    }
                    
                    Log.w(TAG, "❌ Código no encontrado por ID del documento, intentando búsqueda por campo 'code'...");
                    // Estrategia 2: Buscar por el campo "code" en la colección
                    searchByCodeField(normalizedCode, callback);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Error buscando por ID del documento", e);
                    String errorMsg = e != null ? e.getMessage() : "Error desconocido";
                    if (errorMsg != null && errorMsg.contains("PERMISSION_DENIED")) {
                        errorMsg = "Error de permisos en Firestore. Por favor, verifica las reglas de seguridad en Firebase Console. " +
                                  "Asegúrate de que la regla 'match /shared_subjects/{code}' esté al nivel superior con 'allow read: if true;'";
                    }
                    if (callback != null) {
                        callback.onError(new Exception(errorMsg));
                    }
                });
    }
    
    /**
     * Busca el código compartido por el campo "code" en la colección
     */
    private void searchByCodeField(String normalizedCode, @Nullable SharedSubjectCallback callback) {
        Log.d(TAG, "Estrategia 2: Buscando por campo 'code': " + normalizedCode);
        
        firestore.collection("shared_subjects")
                .whereEqualTo("code", normalizedCode)
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        Log.d(TAG, "✅ Código encontrado por campo 'code'");
                        processFoundDocument(querySnapshot.getDocuments().get(0), callback);
                        return;
                    }
                    
                    Log.w(TAG, "❌ Código no encontrado por campo 'code'");
                    // Estrategia 3: Intentar con el código formateado (con guiones)
                    String formattedCode = formatCode(normalizedCode);
                    Log.d(TAG, "Estrategia 3: Intentando con código formateado: " + formattedCode);
                    searchByFormattedCode(formattedCode, normalizedCode, callback);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Error buscando por campo 'code'", e);
                    if (callback != null) {
                        callback.onError(new ShareCodeNotFoundException("Código de compartir no encontrado. Verifica que el código sea correcto."));
                    }
                });
    }
    
    /**
     * Busca el código compartido usando el código formateado (con guiones)
     */
    private void searchByFormattedCode(String formattedCode, String normalizedCode, @Nullable SharedSubjectCallback callback) {
        // Intentar buscar con el código formateado como ID del documento
        firestore.collection("shared_subjects")
                .document(formattedCode)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Log.d(TAG, "✅ Código encontrado con formato con guiones");
                        processFoundDocument(documentSnapshot, callback);
                        return;
                    }
                    
                    Log.w(TAG, "❌ Código no encontrado en ninguna estrategia");
                    if (callback != null) {
                        callback.onError(new ShareCodeNotFoundException("Código de compartir no encontrado. Verifica que el código sea correcto y que el sujeto haya sido compartido correctamente."));
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Error en búsqueda final", e);
                    if (callback != null) {
                        callback.onError(new ShareCodeNotFoundException("Código de compartir no encontrado. Verifica que el código sea correcto."));
                    }
                });
    }
    
    /**
     * Procesa el documento encontrado y extrae la información del sujeto compartido
     * Ahora lee los datos completos desde shared_subjects (más seguro)
     */
    @SuppressWarnings("unchecked")
    private void processFoundDocument(com.google.firebase.firestore.DocumentSnapshot documentSnapshot, @Nullable SharedSubjectCallback callback) {
        // Verificar que el documento tiene la estructura esperada
        if (!documentSnapshot.contains("subject")) {
            Log.e(TAG, "❌ Código de compartir inválido: faltan datos del sujeto");
            if (callback != null) {
                callback.onError(new Exception("Código de compartir inválido: faltan datos del sujeto"));
            }
            return;
        }
        
        // Extraer los datos del sujeto y eventos
        Map<String, Object> subjectData = (Map<String, Object>) documentSnapshot.get("subject");
        
        if (subjectData == null) {
            Log.e(TAG, "❌ Código de compartir inválido: datos del sujeto vacíos");
            if (callback != null) {
                callback.onError(new Exception("Código de compartir inválido: datos del sujeto vacíos"));
            }
            return;
        }
        
        // Extraer eventos (puede venir como List<Map> o como Map<String, Map> en formatos antiguos)
        java.util.List<Map<String, Object>> eventsData = new java.util.ArrayList<>();
        if (documentSnapshot.contains("events")) {
            Object eventsObj = documentSnapshot.get("events");
            if (eventsObj instanceof java.util.List) {
                for (Object eventObj : (java.util.List<?>) eventsObj) {
                    if (eventObj instanceof Map) {
                        eventsData.add((Map<String, Object>) eventObj);
                    }
                }
            } else if (eventsObj instanceof Map) {
                for (Object eventObj : ((Map<?, ?>) eventsObj).values()) {
                    if (eventObj instanceof Map) {
                        eventsData.add((Map<String, Object>) eventObj);
                    }
                }
                Log.d(TAG, "ℹ️ Campo 'events' recibido como Map (formato legacy), convertido a lista");
            }
        }
        
        Log.d(TAG, "✅ Shared subject found with complete data");
        Log.d(TAG, "  - Subject: " + subjectData.get("name"));
        Log.d(TAG, "  - Events count: " + eventsData.size());
        if (!eventsData.isEmpty()) {
            Log.d(TAG, "  - First event title: " + eventsData.get(0).get("title"));
        }
        
        if (callback != null) {
            // Pasar los datos completos al callback
            callback.onSharedSubjectData(subjectData, eventsData);
        }
    }
    
    /**
     * Formatea un código normalizado con guiones (XXXX-XXXX-XXXX)
     */
    private String formatCode(String normalizedCode) {
        if (normalizedCode.length() != 12) {
            return normalizedCode; // Retornar sin cambios si no tiene 12 caracteres
        }
        return normalizedCode.substring(0, 4) + "-" + normalizedCode.substring(4, 8) + "-" + normalizedCode.substring(8, 12);
    }
    
    /**
     * Asegura que el usuario esté autenticado en Firebase antes de realizar operaciones.
     */
    private void ensureAuthentication(Runnable onAuthenticated, @Nullable SharedSubjectCallback errorCallback) {
        com.google.firebase.auth.FirebaseUser currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            Log.d(TAG, "Usuario ya autenticado: " + currentUser.getUid());
            onAuthenticated.run();
            return;
        }
        
        Log.d(TAG, "Usuario no autenticado, autenticando anónimamente...");
        com.google.firebase.auth.FirebaseAuth.getInstance().signInAnonymously()
                .addOnSuccessListener(authResult -> {
                    if (authResult != null && authResult.getUser() != null) {
                        String uid = authResult.getUser().getUid();
                        Log.d(TAG, "✅ Autenticación anónima exitosa: " + uid);
                        Log.d(TAG, "Usuario autenticado, procediendo con búsqueda...");
                        onAuthenticated.run();
                    } else {
                        Log.e(TAG, "❌ Autenticación anónima falló: authResult o user es null");
                        if (errorCallback != null) {
                            errorCallback.onError(new Exception("Error al autenticar en Firebase: authResult es null"));
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    String errorMsg = e != null ? e.getMessage() : "Error desconocido";
                    Log.e(TAG, "❌ Error en autenticación anónima: " + errorMsg);
                    if (errorCallback != null) {
                        errorCallback.onError(new Exception("Error al autenticar en Firebase: " + errorMsg));
                    }
                });
    }
    
    /**
     * Genera un código de compartir único (formato: XXXX-XXXX-XXXX)
     */
    private String generateShareCode() {
        // Generar 12 caracteres alfanuméricos y formatearlos
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // Sin I, O, 0, 1 para evitar confusión
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < 12; i++) {
            int index = (int) (Math.random() * chars.length());
            code.append(chars.charAt(index));
        }
        
        // Formatear como XXXX-XXXX-XXXX
        return code.substring(0, 4) + "-" + code.substring(4, 8) + "-" + code.substring(8, 12);
    }
    
    // ===== Callbacks =====
    
    /**
     * The share code does not exist in Firestore. The UI checks for this type
     * instead of matching the (displayed) message text.
     */
    public static class ShareCodeNotFoundException extends Exception {
        public ShareCodeNotFoundException(String message) {
            super(message);
        }
    }

    public interface ShareCodeCallback {
        void onShareCode(String shareCode);
        void onError(Exception error);
    }
    
    public interface SharedSubjectCallback {
        // Método antiguo (mantener para compatibilidad)
        @Deprecated
        default void onSharedSubject(String subjectId, String ownerUserId) {
            // Por defecto, no hacer nada (las implementaciones nuevas usarán onSharedSubjectData)
        }
        
        // Nuevo método que recibe los datos completos
        default void onSharedSubjectData(Map<String, Object> subjectData, java.util.List<Map<String, Object>> eventsData) {
            // Por defecto, llamar al método antiguo para compatibilidad
            String subjectId = (String) subjectData.get("id");
            String ownerUserId = (String) subjectData.get("uid");
            if (subjectId != null && ownerUserId != null) {
                onSharedSubject(subjectId, ownerUserId);
            }
        }
        
        void onError(Exception error);
    }
}
