package com.gastonlesbegueris.caretemplate.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Gestiona el ID único del usuario para suscripciones y sincronización.
 * 
 * Estrategia:
 * 1. Usa Firebase UID como ID principal (si está autenticado)
 * 2. Si no hay Firebase Auth, genera un UUID local y lo almacena
 * 3. Sincroniza el ID con Firestore en /users/{userId}/metadata
 * 4. El ID se persiste en SharedPreferences para acceso rápido
 */
public class UserManager {
    
    private static final String TAG = "UserManager";
    private static final String PREFS_NAME = "user_prefs";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_FIREBASE_UID = "firebase_uid";
    private static final String KEY_USER_CREATED_AT = "user_created_at";
    
    private final Context context;
    private final SharedPreferences prefs;
    private final FirebaseFirestore firestore;
    
    public UserManager(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = this.context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.firestore = FirebaseFirestore.getInstance();
    }
    
    /**
     * Obtiene el ID único del usuario.
     * Si no existe, lo genera y lo sincroniza con Firestore.
     * 
     * @param callback Callback con el userId (puede ser null si hay error)
     */
    public void getUserId(@Nullable UserIdCallback callback) {
        // 1. Intentar obtener Firebase UID (preferido)
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser != null) {
            String firebaseUid = firebaseUser.getUid();
            String storedFirebaseUid = prefs.getString(KEY_FIREBASE_UID, null);
            
            // Si el Firebase UID cambió, actualizar
            if (!firebaseUid.equals(storedFirebaseUid)) {
                prefs.edit()
                    .putString(KEY_USER_ID, firebaseUid)
                    .putString(KEY_FIREBASE_UID, firebaseUid)
                    .apply();
                
                // Sincronizar con Firestore
                syncUserIdToFirestore(firebaseUid, callback);
                return;
            }
            
            // Usar Firebase UID como userId
            String userId = prefs.getString(KEY_USER_ID, firebaseUid);
            if (userId == null || !userId.equals(firebaseUid)) {
                prefs.edit().putString(KEY_USER_ID, firebaseUid).apply();
            }
            
            if (callback != null) {
                callback.onUserId(firebaseUid);
            }
            
            // Sincronizar en background
            syncUserIdToFirestore(firebaseUid, null);
            return;
        }
        
        // 2. Si no hay Firebase Auth, usar ID local almacenado o generar uno nuevo
        String userId = prefs.getString(KEY_USER_ID, null);
        if (userId == null) {
            // Generar nuevo UUID local
            userId = UUID.randomUUID().toString();
            long createdAt = System.currentTimeMillis();
            
            prefs.edit()
                .putString(KEY_USER_ID, userId)
                .putLong(KEY_USER_CREATED_AT, createdAt)
                .apply();
            
            Log.d(TAG, "Generated new local user ID: " + userId);
        }
        
        if (callback != null) {
            callback.onUserId(userId);
        }
        
        // Intentar sincronizar con Firestore (si hay conexión, se actualizará cuando haya Firebase Auth)
        syncUserIdToFirestore(userId, null);
    }
    
    /**
     * Obtiene el ID único del usuario de forma síncrona (desde cache).
     * Si no existe, retorna null (usa getUserId() para generar uno).
     */
    @Nullable
    public String getUserIdSync() {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser != null) {
            return firebaseUser.getUid();
        }
        return prefs.getString(KEY_USER_ID, null);
    }
    
    /**
     * Sincroniza el userId con Firestore en /users/{userId}/metadata
     */
    private void syncUserIdToFirestore(String userId, @Nullable UserIdCallback callback) {
        if (userId == null || userId.isEmpty()) {
            if (callback != null) {
                callback.onError(new IllegalArgumentException("userId cannot be null or empty"));
            }
            return;
        }
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("userId", userId);
        metadata.put("updatedAt", System.currentTimeMillis());
        
        // Si es la primera vez, agregar createdAt
        long createdAt = prefs.getLong(KEY_USER_CREATED_AT, 0);
        if (createdAt == 0) {
            createdAt = System.currentTimeMillis();
            prefs.edit().putLong(KEY_USER_CREATED_AT, createdAt).apply();
        }
        metadata.put("createdAt", createdAt);
        
        // Agregar información del dispositivo si está disponible
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser != null) {
            metadata.put("firebaseUid", firebaseUser.getUid());
            metadata.put("isAnonymous", firebaseUser.isAnonymous());
        }
        
        DocumentReference userDoc = firestore.collection("users").document(userId)
            .collection("metadata").document("user_info");
        
        userDoc.set(metadata, SetOptions.merge())
            .addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "User ID synced to Firestore: " + userId);
                        if (callback != null) {
                            callback.onUserId(userId);
                        }
                    } else {
                        Log.w(TAG, "Failed to sync user ID to Firestore", task.getException());
                        if (callback != null) {
                            callback.onError(task.getException());
                        }
                    }
                }
            });
    }
    
    /**
     * Inicializa el usuario (llamar al inicio de la app o después de login).
     * Asegura que el userId esté disponible y sincronizado.
     */
    public void initializeUser(@Nullable UserIdCallback callback) {
        getUserId(callback);
    }
    
    /**
     * Establece el userId manualmente (útil para recuperación de datos).
     * @param userId El userId a establecer
     */
    public void setUserId(String userId) {
        if (userId == null || userId.isEmpty()) {
            Log.w(TAG, "Attempted to set null or empty userId");
            return;
        }
        
        prefs.edit()
            .putString(KEY_USER_ID, userId)
            .putString(KEY_FIREBASE_UID, userId)
            .apply();
        
        Log.d(TAG, "User ID set manually: " + userId);
        
        // Sincronizar con Firestore en background
        syncUserIdToFirestore(userId, null);
    }
    
    /**
     * Verifica si el usuario tiene un ID válido.
     */
    public boolean hasUserId() {
        return getUserIdSync() != null;
    }
    
    /**
     * Limpia el ID del usuario (útil para logout o reset).
     * ⚠️ CUIDADO: Esto eliminará el ID local. Solo usar si es necesario.
     */
    public void clearUserId() {
        prefs.edit()
            .remove(KEY_USER_ID)
            .remove(KEY_FIREBASE_UID)
            .remove(KEY_USER_CREATED_AT)
            .apply();
        Log.d(TAG, "User ID cleared");
    }
    
    /**
     * Callback para obtener el userId de forma asíncrona.
     */
    public interface UserIdCallback {
        void onUserId(String userId);
        default void onError(Exception error) {
            Log.e(TAG, "Error getting user ID", error);
        }
    }
}

