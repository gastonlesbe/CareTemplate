package com.gastonlesbegueris.caretemplate.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Gestiona la recuperación de datos del usuario cuando reinstala la app o cambia de dispositivo.
 * 
 * Estrategia:
 * 1. Genera un código de recuperación único que el usuario puede guardar
 * 2. Vincula el código con el UID del usuario en Firestore
 * 3. Permite recuperar el UID usando el código de recuperación
 */
public class UserRecoveryManager {
    
    private static final String TAG = "UserRecoveryManager";
    private static final String PREFS_NAME = "user_prefs";
    private static final String KEY_RECOVERY_CODE = "recovery_code";
    private static final String KEY_RECOVERY_CODE_SYNCED = "recovery_code_synced";
    
    private final Context context;
    private final SharedPreferences prefs;
    private final FirebaseFirestore firestore;
    
    public UserRecoveryManager(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = this.context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.firestore = FirebaseFirestore.getInstance();
    }
    
    /**
     * Genera o recupera el código de recuperación del usuario.
     * Si no existe, lo genera y lo sincroniza con Firestore.
     * 
     * @param callback Callback con el código de recuperación
     */
    public void getOrGenerateRecoveryCode(@Nullable RecoveryCodeCallback callback) {
        String recoveryCode = prefs.getString(KEY_RECOVERY_CODE, null);
        
        if (recoveryCode == null) {
            // Generar nuevo código de recuperación (formato: XXXX-XXXX-XXXX)
            recoveryCode = generateRecoveryCode();
            prefs.edit().putString(KEY_RECOVERY_CODE, recoveryCode).apply();
            Log.d(TAG, "Generated new recovery code: " + recoveryCode);
        }
        
        // Sincronizar con Firestore
        syncRecoveryCodeToFirestore(recoveryCode, callback);
    }
    
    /**
     * Recupera el UID del usuario usando un código de recuperación.
     * 
     * @param recoveryCode Código de recuperación proporcionado por el usuario
     * @param callback Callback con el UID recuperado
     */
    public void recoverUserIdFromCode(String recoveryCode, @Nullable RecoverUserIdCallback callback) {
        if (recoveryCode == null || recoveryCode.isEmpty()) {
            if (callback != null) {
                callback.onError(new IllegalArgumentException("Recovery code cannot be empty"));
            }
            return;
        }
        
        // Normalizar el código (quitar guiones y espacios)
        String normalizedCode = recoveryCode.replaceAll("[\\s-]", "").toUpperCase();
        Log.d(TAG, "═══════════════════════════════════════════════════════════");
        Log.d(TAG, "BÚSQUEDA DE CÓDIGO DE RECUPERACIÓN");
        Log.d(TAG, "Código original: " + recoveryCode);
        Log.d(TAG, "Código normalizado: " + normalizedCode);
        Log.d(TAG, "Longitud: " + normalizedCode.length() + " caracteres");
        Log.d(TAG, "═══════════════════════════════════════════════════════════");
        
        // Verificar estado de autenticación ANTES de asegurar autenticación
        com.google.firebase.auth.FirebaseUser currentUserBefore = FirebaseAuth.getInstance().getCurrentUser();
        Log.d(TAG, "Estado de autenticación inicial: " + (currentUserBefore != null ? "Autenticado (" + currentUserBefore.getUid() + ")" : "NO autenticado"));
        
        // Asegurar autenticación antes de buscar (necesario para leer Firestore)
        ensureAuthentication(() -> {
            // Verificar que la autenticación se completó
            com.google.firebase.auth.FirebaseUser currentUserAfter = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUserAfter != null) {
                Log.d(TAG, "✅ Autenticación confirmada antes de buscar: " + currentUserAfter.getUid());
            } else {
                Log.e(TAG, "❌ ERROR: Autenticación no completada antes de buscar");
            }
            
            // Intentar múltiples estrategias de búsqueda
            searchUserIdInGlobalCollection(normalizedCode, callback);
        }, callback);
    }
    
    /**
     * Asegura que el usuario esté autenticado en Firebase antes de realizar operaciones.
     */
    private void ensureAuthentication(Runnable onAuthenticated, @Nullable RecoverUserIdCallback errorCallback) {
        com.google.firebase.auth.FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            Log.d(TAG, "Usuario ya autenticado: " + currentUser.getUid());
            onAuthenticated.run();
            return;
        }
        
        Log.d(TAG, "Usuario no autenticado, autenticando anónimamente...");
        FirebaseAuth.getInstance().signInAnonymously()
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
     * Verifica si un código de recuperación existe en Firestore (método de depuración).
     * 
     * @param recoveryCode Código de recuperación a verificar
     * @param callback Callback con el resultado de la verificación
     */
    public void verifyRecoveryCodeExists(String recoveryCode, @Nullable VerifyCodeCallback callback) {
        if (recoveryCode == null || recoveryCode.isEmpty()) {
            if (callback != null) {
                callback.onResult(false, "Código vacío");
            }
            return;
        }
        
        String normalizedCode = recoveryCode.replaceAll("[\\s-]", "").toUpperCase();
        Log.d(TAG, "VERIFICANDO EXISTENCIA DEL CÓDIGO: " + normalizedCode);
        
        // Asegurar autenticación antes de verificar
        ensureAuthenticationForVerification(normalizedCode, callback);
    }
    
    /**
     * Asegura autenticación y luego verifica el código.
     */
    private void ensureAuthenticationForVerification(String normalizedCode, @Nullable VerifyCodeCallback callback) {
        com.google.firebase.auth.FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            Log.d(TAG, "Usuario ya autenticado para verificación: " + currentUser.getUid());
            performVerification(normalizedCode, callback);
            return;
        }
        
        Log.d(TAG, "Autenticando anónimamente para verificación...");
        FirebaseAuth.getInstance().signInAnonymously()
                .addOnSuccessListener(authResult -> {
                    if (authResult != null && authResult.getUser() != null) {
                        Log.d(TAG, "Autenticación exitosa para verificación: " + authResult.getUser().getUid());
                        performVerification(normalizedCode, callback);
                    } else {
                        if (callback != null) {
                            callback.onResult(false, "Error: No se pudo autenticar en Firebase");
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error en autenticación para verificación: " + e.getMessage());
                    String errorMsg = e.getMessage();
                    if (errorMsg != null && errorMsg.contains("permission_denied")) {
                        if (callback != null) {
                            callback.onResult(false, "Error de permisos: " + errorMsg + "\n\n" +
                                    "Las reglas de Firestore pueden tardar unos minutos en aplicarse.\n\n" +
                                    "Solución:\n" +
                                    "1. Verifica que las reglas estén publicadas en Firebase Console\n" +
                                    "2. Espera 2-5 minutos después de publicar las reglas\n" +
                                    "3. Vuelve a intentar\n\n" +
                                    "Regla necesaria:\n" +
                                    "match /recovery_codes/{code} {\n" +
                                    "  allow read: if true;\n" +
                                    "}");
                        }
                    } else {
                        if (callback != null) {
                            callback.onResult(false, "Error al autenticar: " + errorMsg);
                        }
                    }
                });
    }
    
    /**
     * Realiza la verificación del código después de autenticar.
     */
    private void performVerification(String normalizedCode, @Nullable VerifyCodeCallback callback) {
        // Verificar en colección global
        firestore.collection("recovery_codes")
                .document(normalizedCode)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    boolean exists = documentSnapshot.exists();
                    String userId = exists ? documentSnapshot.getString("userId") : null;
                    Log.d(TAG, "Resultado verificación global: exists=" + exists + ", userId=" + userId);
                    
                    if (exists && userId != null) {
                        if (callback != null) {
                            callback.onResult(true, "Código encontrado en colección global. userId: " + userId);
                        }
                        return;
                    }
                    
                    // Si no existe, verificar en collectionGroup
                    firestore.collectionGroup("recovery_codes")
                            .whereEqualTo("code", normalizedCode)
                            .limit(1)
                            .get()
                            .addOnSuccessListener(querySnapshot -> {
                                boolean foundInUsers = !querySnapshot.isEmpty();
                                String foundUserId = null;
                                if (foundInUsers) {
                                    com.google.firebase.firestore.DocumentSnapshot doc = querySnapshot.getDocuments().get(0);
                                    foundUserId = doc.getString("userId");
                                    Log.d(TAG, "Código encontrado en collectionGroup. userId: " + foundUserId);
                                } else {
                                    Log.d(TAG, "Código NO encontrado en ninguna colección");
                                }
                                
                                if (callback != null) {
                                    if (foundInUsers) {
                                        callback.onResult(true, "Código encontrado en colección de usuarios. userId: " + foundUserId);
                                    } else {
                                        callback.onResult(false, "Código no encontrado en Firestore. Verifica que:\n" +
                                                "1. El código sea correcto\n" +
                                                "2. Los datos se hayan sincronizado al menos una vez\n" +
                                                "3. El código esté en /recovery_codes/" + normalizedCode + " o en /users/{userId}/recovery_codes/current");
                                    }
                                }
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Error verificando en collectionGroup: " + e.getMessage());
                                String errorMsg = e.getMessage();
                                if (errorMsg != null && errorMsg.contains("permission_denied")) {
                                    if (callback != null) {
                                        callback.onResult(false, "Error de permisos: " + errorMsg + "\n\n" +
                                                "Las reglas de Firestore pueden tardar unos minutos en aplicarse.\n\n" +
                                                "Solución:\n" +
                                                "1. Verifica que las reglas estén publicadas en Firebase Console\n" +
                                                "2. Espera 2-5 minutos después de publicar las reglas\n" +
                                                "3. Vuelve a intentar\n\n" +
                                                "Regla necesaria:\n" +
                                                "match /recovery_codes/{code} {\n" +
                                                "  allow read: if true;\n" +
                                                "}");
                                    }
                                } else {
                                    if (callback != null) {
                                        callback.onResult(false, "Error al verificar: " + errorMsg);
                                    }
                                }
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error verificando en colección global: " + e.getMessage());
                    String errorMsg = e.getMessage();
                    if (errorMsg != null && errorMsg.contains("permission_denied")) {
                        if (callback != null) {
                            callback.onResult(false, "Error de permisos: " + errorMsg + "\n\n" +
                                    "Las reglas de Firestore pueden tardar unos minutos en aplicarse.\n\n" +
                                    "Solución:\n" +
                                    "1. Verifica que las reglas estén publicadas en Firebase Console\n" +
                                    "2. Espera 2-5 minutos después de publicar las reglas\n" +
                                    "3. Vuelve a intentar\n\n" +
                                    "Regla necesaria:\n" +
                                    "match /recovery_codes/{code} {\n" +
                                    "  allow read: if true;\n" +
                                    "}");
                        }
                    } else {
                        if (callback != null) {
                            callback.onResult(false, "Error al verificar: " + errorMsg);
                        }
                    }
                });
    }
    
    /**
     * Busca el userId en la colección global de códigos de recuperación.
     */
    private void searchUserIdInGlobalCollection(String normalizedCode, @Nullable RecoverUserIdCallback callback) {
        String path = "/recovery_codes/" + normalizedCode;
        Log.d(TAG, "═══════════════════════════════════════════════════════════");
        Log.d(TAG, "🔍 BÚSQUEDA 1/3: Colección global");
        Log.d(TAG, "   Ruta completa: " + path);
        Log.d(TAG, "   Código normalizado: " + normalizedCode);
        
        // Verificar autenticación antes de buscar
        com.google.firebase.auth.FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            Log.d(TAG, "   ✅ Usuario autenticado: " + currentUser.getUid());
        } else {
            Log.e(TAG, "   ❌ Usuario NO autenticado antes de buscar");
            Log.e(TAG, "   Esto puede causar errores de permisos");
        }
        
        Log.d(TAG, "   Intentando leer documento...");
        firestore.collection("recovery_codes")
                .document(normalizedCode)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    boolean exists = documentSnapshot.exists();
                    Log.d(TAG, "   ✅ Lectura exitosa");
                    Log.d(TAG, "   Resultado: " + (exists ? "✅ ENCONTRADO" : "❌ NO ENCONTRADO"));
                    if (exists) {
                        String userId = documentSnapshot.getString("userId");
                        Log.d(TAG, "   userId: " + userId);
                        if (userId != null && !userId.isEmpty()) {
                            Log.d(TAG, "═══════════════════════════════════════════════════════════");
                            Log.d(TAG, "✅ CÓDIGO ENCONTRADO EN COLECCIÓN GLOBAL");
                            Log.d(TAG, "═══════════════════════════════════════════════════════════");
                            handleUserIdFound(userId, normalizedCode, callback);
                            return;
                        }
                    }
                    // Si no se encontró, intentar búsqueda alternativa
                    Log.d(TAG, "   → Código no encontrado, intentando búsqueda alternativa...");
                    searchUserIdInUserCollection(normalizedCode, callback);
                })
                .addOnFailureListener(e -> {
                    String errorMsg = e != null ? e.getMessage() : "Error desconocido";
                    String errorClass = e != null ? e.getClass().getSimpleName() : "null";
                    Log.e(TAG, "═══════════════════════════════════════════════════════════");
                    Log.e(TAG, "❌ ERROR AL LEER DOCUMENTO");
                    Log.e(TAG, "   Mensaje: " + errorMsg);
                    Log.e(TAG, "   Tipo: " + errorClass);
                    Log.e(TAG, "   Ruta intentada: " + path);
                    
                    // Si es error de permisos, reportarlo claramente
                    if (errorMsg != null && (errorMsg.contains("permission_denied") || 
                                             errorMsg.contains("PERMISSION_DENIED") ||
                                             errorMsg.contains("Missing or insufficient permissions"))) {
                        Log.e(TAG, "   ⚠️ ERROR DE PERMISOS CONFIRMADO");
                        Log.e(TAG, "   ═══════════════════════════════════════════════════════════");
                        Log.e(TAG, "   PROBLEMA: Las reglas de Firestore no permiten leer:");
                        Log.e(TAG, "   /recovery_codes/" + normalizedCode);
                        Log.e(TAG, "   ═══════════════════════════════════════════════════════════");
                        Log.e(TAG, "   SOLUCIÓN:");
                        Log.e(TAG, "   1. Ve a Firebase Console → Firestore Database → Reglas");
                        Log.e(TAG, "   2. Asegúrate de que esta regla esté al NIVEL SUPERIOR:");
                        Log.e(TAG, "      match /recovery_codes/{code} {");
                        Log.e(TAG, "        allow read: if true;");
                        Log.e(TAG, "        allow write: if request.auth != null;");
                        Log.e(TAG, "      }");
                        Log.e(TAG, "   3. NO debe estar dentro de /users/{userId}/apps/{appId}");
                        Log.e(TAG, "   4. Publica las reglas y espera 2-5 minutos");
                        Log.e(TAG, "   ═══════════════════════════════════════════════════════════");
                        
                        if (callback != null) {
                            callback.onError(new Exception("❌ Error de permisos al buscar código de recuperación\n\n" +
                                    "Ruta intentada: /recovery_codes/" + normalizedCode + "\n\n" +
                                    "SOLUCIÓN:\n" +
                                    "1. Ve a Firebase Console → Firestore Database → Reglas\n" +
                                    "2. Agrega esta regla al NIVEL SUPERIOR (no dentro de /users/{userId}/apps/{appId}):\n\n" +
                                    "match /recovery_codes/{code} {\n" +
                                    "  allow read: if true;\n" +
                                    "  allow write: if request.auth != null;\n" +
                                    "}\n\n" +
                                    "3. Publica las reglas\n" +
                                    "4. Espera 2-5 minutos\n" +
                                    "5. Vuelve a intentar\n\n" +
                                    "Usuario autenticado: " + (currentUser != null ? "Sí (" + currentUser.getUid() + ")" : "No")));
                        }
                        return;
                    }
                    
                    Log.d(TAG, "   → Error no es de permisos, intentando búsqueda alternativa...");
                    // Intentar búsqueda alternativa solo si no es error de permisos
                    searchUserIdInUserCollection(normalizedCode, callback);
                });
    }
    
    /**
     * Busca el userId en la colección de usuarios usando collectionGroup.
     */
    private void searchUserIdInUserCollection(String normalizedCode, @Nullable RecoverUserIdCallback callback) {
        Log.d(TAG, "🔍 Búsqueda 2/3: CollectionGroup de usuarios");
        Log.d(TAG, "   Query: collectionGroup('recovery_codes').whereEqualTo('code', '" + normalizedCode + "')");
        firestore.collectionGroup("recovery_codes")
                .whereEqualTo("code", normalizedCode)
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    int size = querySnapshot.size();
                    Log.d(TAG, "   Resultado: " + size + " documento(s) encontrado(s)");
                    if (!querySnapshot.isEmpty()) {
                        com.google.firebase.firestore.DocumentSnapshot doc = querySnapshot.getDocuments().get(0);
                        String userId = doc.getString("userId");
                        String docPath = doc.getReference().getPath();
                        Log.d(TAG, "   ✅ ENCONTRADO en: " + docPath);
                        Log.d(TAG, "   userId: " + userId);
                        if (userId != null && !userId.isEmpty()) {
                            Log.d(TAG, "✅ Código encontrado en colección de usuarios");
                            handleUserIdFound(userId, normalizedCode, callback);
                            return;
                        }
                    }
                    // Si aún no se encontró, intentar búsqueda por código sin normalizar
                    Log.d(TAG, "   → Intentando búsqueda con código formateado...");
                    searchWithOriginalCode(normalizedCode, callback);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "   ❌ Error: " + e.getMessage());
                    Log.d(TAG, "   → Intentando búsqueda con código formateado...");
                    // Intentar búsqueda con código original
                    searchWithOriginalCode(normalizedCode, callback);
                });
    }
    
    /**
     * Busca el código con diferentes variaciones (con y sin guiones).
     */
    private void searchWithOriginalCode(String normalizedCode, @Nullable RecoverUserIdCallback callback) {
        // Intentar buscar también con el código formateado (con guiones)
        // Formato: XXXX-XXXX-XXXX
        if (normalizedCode.length() == 12) {
            String formattedCode = normalizedCode.substring(0, 4) + "-" + 
                                  normalizedCode.substring(4, 8) + "-" + 
                                  normalizedCode.substring(8, 12);
            String path = "/recovery_codes/" + formattedCode.toUpperCase();
            Log.d(TAG, "🔍 Búsqueda 3/3: Código formateado");
            Log.d(TAG, "   Ruta: " + path);
            
            // Buscar en la colección global con el código formateado
            firestore.collection("recovery_codes")
                    .document(formattedCode.toUpperCase())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        boolean exists = documentSnapshot.exists();
                        Log.d(TAG, "   Resultado: " + (exists ? "✅ ENCONTRADO" : "❌ NO ENCONTRADO"));
                        if (exists) {
                            String userId = documentSnapshot.getString("userId");
                            Log.d(TAG, "   userId: " + userId);
                            if (userId != null && !userId.isEmpty()) {
                                Log.d(TAG, "✅ Código encontrado con formato formateado");
                                handleUserIdFound(userId, normalizedCode, callback);
                                return;
                            }
                        }
                        // Si aún no se encontró, reportar error
                        Log.e(TAG, "═══════════════════════════════════════════════════════════");
                        Log.e(TAG, "❌ CÓDIGO NO ENCONTRADO EN NINGUNA COLECCIÓN");
                        Log.e(TAG, "Código buscado: " + normalizedCode);
                        Log.e(TAG, "Rutas verificadas:");
                        Log.e(TAG, "  1. /recovery_codes/" + normalizedCode);
                        Log.e(TAG, "  2. collectionGroup('recovery_codes').whereEqualTo('code', '" + normalizedCode + "')");
                        Log.e(TAG, "  3. /recovery_codes/" + formattedCode.toUpperCase());
                        Log.e(TAG, "═══════════════════════════════════════════════════════════");
                        if (callback != null) {
                            callback.onError(new Exception("Código de recuperación no encontrado.\n\n" +
                                    "Código buscado: " + normalizedCode + "\n\n" +
                                    "Posibles causas:\n" +
                                    "• El código no se sincronizó a la nube (necesitas sincronizar tus datos primero)\n" +
                                    "• El código es incorrecto\n" +
                                    "• Los datos fueron eliminados de Firebase\n\n" +
                                    "Solución: Si tienes el código correcto, asegúrate de haber sincronizado tus datos al menos una vez antes de intentar recuperarlos."));
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "   ❌ Error: " + e.getMessage());
                        Log.e(TAG, "═══════════════════════════════════════════════════════════");
                        Log.e(TAG, "❌ CÓDIGO NO ENCONTRADO - ERROR EN BÚSQUEDA");
                        Log.e(TAG, "═══════════════════════════════════════════════════════════");
                        if (callback != null) {
                            callback.onError(new Exception("Código de recuperación no encontrado. Verifica que el código sea correcto y que hayas sincronizado tus datos anteriormente."));
                        }
                    });
        } else {
            // Si el código no tiene 12 caracteres, reportar error
            Log.e(TAG, "❌ Código con formato inválido: " + normalizedCode + " (longitud: " + normalizedCode.length() + ")");
            if (callback != null) {
                callback.onError(new Exception("Código de recuperación con formato inválido. El código debe tener 12 caracteres (formato: XXXX-XXXX-XXXX)"));
            }
        }
    }
    
    /**
     * Maneja el userId encontrado.
     */
    private void handleUserIdFound(String userId, String normalizedCode, @Nullable RecoverUserIdCallback callback) {
        Log.d(TAG, "═══════════════════════════════════════════════════════════");
        Log.d(TAG, "✅ USERID RECUPERADO DESDE CÓDIGO");
        Log.d(TAG, "Código normalizado: " + normalizedCode);
        Log.d(TAG, "userId recuperado: " + userId);
        
        // Verificar el userId actual almacenado
        String currentUserId = prefs.getString("user_id", null);
        Log.d(TAG, "userId actual en SharedPreferences: " + currentUserId);
        
        if (currentUserId != null && !currentUserId.equals(userId)) {
            Log.w(TAG, "⚠️ ADVERTENCIA: El userId recuperado es diferente al almacenado localmente");
            Log.w(TAG, "userId local: " + currentUserId);
            Log.w(TAG, "userId recuperado: " + userId);
            Log.w(TAG, "Esto puede causar que los datos no coincidan");
        }
        
        Log.d(TAG, "═══════════════════════════════════════════════════════════");
        
        // Guardar el código localmente para futuras referencias
        String formatted = formatRecoveryCode(normalizedCode);
        prefs.edit()
                .putString(KEY_RECOVERY_CODE, formatted)
                .putString("user_id", userId)
                .apply();
        
        Log.d(TAG, "userId y código guardados en SharedPreferences");
        
        if (callback != null) {
            callback.onUserIdRecovered(userId);
        }
    }
    
    
    /**
     * Sincroniza el código de recuperación con Firestore.
     */
    private void syncRecoveryCodeToFirestore(String recoveryCode, @Nullable RecoveryCodeCallback callback) {
        // Obtener el userId actual
        UserManager userManager = new UserManager(context);
        String userId = userManager.getUserIdSync();
        
        if (userId == null || userId.isEmpty()) {
            Log.w(TAG, "Cannot sync recovery code: userId is null");
            if (callback != null) {
                callback.onError(new Exception("Usuario no identificado"));
            }
            return;
        }
        
        // Normalizar el código
        String normalizedCode = recoveryCode.replaceAll("[\\s-]", "").toUpperCase();
        Log.d(TAG, "═══════════════════════════════════════════════════════════");
        Log.d(TAG, "SINCRONIZANDO CÓDIGO DE RECUPERACIÓN");
        Log.d(TAG, "userId: " + userId);
        Log.d(TAG, "Código original: " + recoveryCode);
        Log.d(TAG, "Código normalizado: " + normalizedCode);
        Log.d(TAG, "═══════════════════════════════════════════════════════════");
        
        // Guardar en Firestore: /users/{userId}/recovery_codes/current
        Map<String, Object> recoveryData = new HashMap<>();
        recoveryData.put("code", normalizedCode);
        recoveryData.put("codeFormatted", recoveryCode.toUpperCase()); // También guardar con formato
        recoveryData.put("userId", userId);
        recoveryData.put("createdAt", System.currentTimeMillis());
        recoveryData.put("updatedAt", System.currentTimeMillis());
        
        // También guardar en una colección global para búsqueda rápida
        DocumentReference userRecoveryDoc = firestore.collection("users").document(userId)
                .collection("recovery_codes").document("current");
        
        // Guardar en la colección global con el código normalizado como ID del documento
        DocumentReference globalRecoveryDoc = firestore.collection("recovery_codes")
                .document(normalizedCode);
        
        // También guardar con el código formateado como ID alternativo (si tiene 12 caracteres)
        final DocumentReference globalRecoveryDocFormatted;
        if (normalizedCode.length() == 12) {
            String formattedCode = normalizedCode.substring(0, 4) + "-" + 
                                  normalizedCode.substring(4, 8) + "-" + 
                                  normalizedCode.substring(8, 12);
            globalRecoveryDocFormatted = firestore.collection("recovery_codes")
                    .document(formattedCode);
        } else {
            globalRecoveryDocFormatted = null;
        }
        
        // Verificar si el código ya existe con un userId diferente (colisión)
        globalRecoveryDoc.get()
                .addOnSuccessListener(existingDoc -> {
                    if (existingDoc.exists()) {
                        String existingUserId = existingDoc.getString("userId");
                        if (existingUserId != null && !existingUserId.equals(userId)) {
                            // COLISIÓN: El código ya existe con un userId diferente
                            Log.e(TAG, "═══════════════════════════════════════════════════════════");
                            Log.e(TAG, "⚠️ COLISIÓN DE CÓDIGO DETECTADA");
                            Log.e(TAG, "Código: " + normalizedCode);
                            Log.e(TAG, "userId actual: " + userId);
                            Log.e(TAG, "userId existente: " + existingUserId);
                            Log.e(TAG, "═══════════════════════════════════════════════════════════");
                            // Generar un nuevo código
                            String newCode = generateRecoveryCode();
                            String newNormalizedCode = newCode.replaceAll("[\\s-]", "").toUpperCase();
                            Log.d(TAG, "Generando nuevo código debido a colisión: " + newCode);
                            prefs.edit().putString(KEY_RECOVERY_CODE, newCode).apply();
                            // Intentar sincronizar el nuevo código (recursivo, pero con límite de intentos)
                            syncRecoveryCodeToFirestore(newCode, callback);
                            return;
                        } else {
                            Log.d(TAG, "Código ya existe con el mismo userId, actualizando...");
                        }
                    }
                    // Continuar con la sincronización normal
                    saveRecoveryCodeToFirestore(userId, recoveryCode, normalizedCode, recoveryData, 
                                                userRecoveryDoc, globalRecoveryDoc, globalRecoveryDocFormatted, callback);
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "No se pudo verificar si el código existe (continuando): " + e.getMessage());
                    // Continuar con la sincronización normal aunque falle la verificación
                    saveRecoveryCodeToFirestore(userId, recoveryCode, normalizedCode, recoveryData, 
                                                userRecoveryDoc, globalRecoveryDoc, globalRecoveryDocFormatted, callback);
                });
    }
    
    /**
     * Guarda el código de recuperación en Firestore (método auxiliar).
     */
    private void saveRecoveryCodeToFirestore(String userId, String recoveryCode, String normalizedCode,
                                             Map<String, Object> recoveryData,
                                             DocumentReference userRecoveryDoc,
                                             DocumentReference globalRecoveryDoc,
                                             DocumentReference globalRecoveryDocFormatted,
                                             @Nullable RecoveryCodeCallback callback) {
        // Guardar en todos los lugares posibles
        userRecoveryDoc.set(recoveryData, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Código guardado en colección de usuario");
                    // Guardar en colección global (normalizado)
                    globalRecoveryDoc.set(recoveryData, SetOptions.merge())
                            .addOnSuccessListener(aVoid2 -> {
                                Log.d(TAG, "Código guardado en colección global (normalizado)");
                                // Si hay código formateado, guardarlo también
                                if (globalRecoveryDocFormatted != null) {
                                    globalRecoveryDocFormatted.set(recoveryData, SetOptions.merge())
                                            .addOnSuccessListener(aVoid3 -> {
                                                Log.d(TAG, "Código guardado en colección global (formateado)");
                                                prefs.edit().putBoolean(KEY_RECOVERY_CODE_SYNCED, true).apply();
                                                Log.d(TAG, "Recovery code synced to Firestore: " + normalizedCode);
                                                if (callback != null) {
                                                    callback.onRecoveryCode(recoveryCode);
                                                }
                                            })
                                            .addOnFailureListener(e -> {
                                                Log.w(TAG, "Failed to sync formatted code (non-critical): " + e.getMessage());
                                                // Aún así, consideramos exitoso
                                                prefs.edit().putBoolean(KEY_RECOVERY_CODE_SYNCED, true).apply();
                                                if (callback != null) {
                                                    callback.onRecoveryCode(recoveryCode);
                                                }
                                            });
                                } else {
                                    prefs.edit().putBoolean(KEY_RECOVERY_CODE_SYNCED, true).apply();
                                    Log.d(TAG, "Recovery code synced to Firestore: " + normalizedCode);
                                    if (callback != null) {
                                        callback.onRecoveryCode(recoveryCode);
                                    }
                                }
                            })
                            .addOnFailureListener(e -> {
                                String errorMsg = e.getMessage();
                                // No mostrar error si es failed_precondition o similar (problemas de índices/reglas)
                                if (errorMsg != null && (errorMsg.contains("failed_precondition") || 
                                                         errorMsg.contains("FAILED_PRECONDITION") ||
                                                         errorMsg.contains("permission_denied"))) {
                                    Log.w(TAG, "Failed to sync recovery code to global collection (non-critical): " + errorMsg);
                                    // Aún así, consideramos exitoso si se guardó en el usuario
                                    prefs.edit().putBoolean(KEY_RECOVERY_CODE_SYNCED, true).apply();
                                    if (callback != null) {
                                        callback.onRecoveryCode(recoveryCode);
                                    }
                                } else {
                                    Log.w(TAG, "Failed to sync recovery code to global collection", e);
                                    // Aún así, consideramos exitoso si se guardó en el usuario
                                    prefs.edit().putBoolean(KEY_RECOVERY_CODE_SYNCED, true).apply();
                                    if (callback != null) {
                                        callback.onRecoveryCode(recoveryCode);
                                    }
                                }
                            });
                })
                .addOnFailureListener(e -> {
                    String errorMsg = e.getMessage();
                    // No mostrar error si es failed_precondition o similar (problemas de índices/reglas)
                    if (errorMsg != null && (errorMsg.contains("failed_precondition") || 
                                             errorMsg.contains("FAILED_PRECONDITION") ||
                                             errorMsg.contains("permission_denied"))) {
                        Log.w(TAG, "Failed to sync recovery code to Firestore (non-critical): " + errorMsg);
                        // Aún así, devolver el código local (la sincronización puede fallar pero el código es válido)
                        if (callback != null) {
                            callback.onRecoveryCode(recoveryCode);
                        }
                    } else {
                        Log.e(TAG, "Failed to sync recovery code to Firestore", e);
                        // Aún así, devolver el código local
                        if (callback != null) {
                            callback.onRecoveryCode(recoveryCode);
                        }
                    }
                });
    }
    
    /**
     * Genera un código de recuperación en formato XXXX-XXXX-XXXX.
     */
    private String generateRecoveryCode() {
        // Generar 12 caracteres alfanuméricos (sin caracteres confusos como 0, O, I, 1, l)
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
    
    /**
     * Obtiene el código de recuperación actual (desde cache).
     */
    @Nullable
    public String getRecoveryCodeSync() {
        return prefs.getString(KEY_RECOVERY_CODE, null);
    }
    
    /**
     * Verifica si el código de recuperación está sincronizado.
     */
    public boolean isRecoveryCodeSynced() {
        return prefs.getBoolean(KEY_RECOVERY_CODE_SYNCED, false);
    }
    
    /**
     * Fuerza la sincronización del código de recuperación actual con Firestore.
     * Útil cuando el código no se sincronizó correctamente la primera vez.
     */
    public void forceSyncRecoveryCode(@Nullable RecoveryCodeCallback callback) {
        String recoveryCode = prefs.getString(KEY_RECOVERY_CODE, null);
        if (recoveryCode == null || recoveryCode.isEmpty()) {
            if (callback != null) {
                callback.onError(new Exception("No hay código de recuperación para sincronizar"));
            }
            return;
        }
        
        Log.d(TAG, "Forzando sincronización del código de recuperación: " + recoveryCode);
        // Resetear el flag de sincronización para forzar una nueva sincronización
        prefs.edit().putBoolean(KEY_RECOVERY_CODE_SYNCED, false).apply();
        syncRecoveryCodeToFirestore(recoveryCode, callback);
    }
    
    /**
     * Callback para obtener el código de recuperación.
     */
    public interface RecoveryCodeCallback {
        void onRecoveryCode(String recoveryCode);
        default void onError(Exception error) {
            Log.e(TAG, "Error getting recovery code", error);
        }
    }
    
    /**
     * Callback para recuperar el userId desde un código.
     */
    public interface RecoverUserIdCallback {
        void onUserIdRecovered(String userId);
        default void onError(Exception error) {
            Log.e(TAG, "Error recovering userId", error);
        }
    }
    
    /**
     * Formatea el código de recuperación para mostrarlo (XXXX-XXXX-XXXX).
     * Acepta códigos con o sin guiones y los normaliza en mayúsculas.
     */
    public static String formatRecoveryCode(String code) {
        if (code == null) return "";
        String normalized = code.replaceAll("[\\s-]", "").toUpperCase();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < normalized.length(); i++) {
            if (i > 0 && i % 4 == 0) sb.append("-");
            sb.append(normalized.charAt(i));
        }
        return sb.toString();
    }
    
    /**
     * Callback para verificar si un código existe.
     */
    public interface VerifyCodeCallback {
        void onResult(boolean exists, String message);
    }
}

