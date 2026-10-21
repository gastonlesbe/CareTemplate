package com.gastonlesbegueris.caretemplate.util;

import android.content.Context;

import androidx.annotation.Nullable;

/**
 * Helper estático para acceder fácilmente al userId desde cualquier parte de la app.
 * 
 * Ejemplo de uso:
 * <pre>
 * String userId = UserHelper.getUserId(this);
 * if (userId != null) {
 *     // Usar userId para suscripciones, analytics, etc.
 * }
 * </pre>
 */
public class UserHelper {
    
    private static UserManager instance;
    
    /**
     * Obtiene el userId de forma síncrona (desde cache).
     * Si no existe, retorna null.
     * 
     * @param context Context de la aplicación
     * @return userId o null si no está disponible
     */
    @Nullable
    public static String getUserId(Context context) {
        if (instance == null) {
            instance = new UserManager(context);
        }
        return instance.getUserIdSync();
    }
    
    /**
     * Obtiene el userId de forma asíncrona (puede generar uno nuevo si no existe).
     * 
     * @param context Context de la aplicación
     * @param callback Callback con el userId
     */
    public static void getUserIdAsync(Context context, UserManager.UserIdCallback callback) {
        if (instance == null) {
            instance = new UserManager(context);
        }
        instance.getUserId(callback);
    }
    
    /**
     * Verifica si el usuario tiene un ID válido.
     */
    public static boolean hasUserId(Context context) {
        if (instance == null) {
            instance = new UserManager(context);
        }
        return instance.hasUserId();
    }
    
    /**
     * Inicializa el userId (asegura que esté disponible y sincronizado).
     * Útil para llamar al inicio de la app o después de login.
     */
    public static void initializeUser(Context context, @Nullable UserManager.UserIdCallback callback) {
        if (instance == null) {
            instance = new UserManager(context);
        }
        instance.initializeUser(callback);
    }
}

