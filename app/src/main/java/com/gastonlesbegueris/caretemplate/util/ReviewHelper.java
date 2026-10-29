package com.gastonlesbegueris.caretemplate.util;

import com.gastonlesbegueris.caretemplate.R;
import android.app.Activity;
import android.content.SharedPreferences;
import android.util.Log;
import com.google.android.play.core.review.ReviewInfo;
import com.google.android.play.core.review.ReviewManager;
import com.google.android.play.core.review.ReviewManagerFactory;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

/**
 * Helper class para solicitar reseñas de la app en Google Play Store
 * Muestra un diálogo personalizado antes de solicitar la reseña oficial
 */
public class ReviewHelper {
    private static final String TAG = "ReviewHelper";
    private static final String PREFS_NAME = "review_prefs";
    private static final String KEY_REVIEW_COMPLETED = "review_completed"; // Se completó la reseña, no volver a pedir
    private static final String KEY_TOTAL_LOAD_COUNT = "total_load_count"; // Contador total de cargas (eventos + gastos)
    private static final String KEY_REVIEW_ATTEMPT_COUNT = "review_attempt_count"; // Cuántas veces se ha mostrado el diálogo
    private static final String KEY_APP_OPEN_COUNT = "app_open_count"; // Contador de veces que se abre la app
    
    // Solicitar reseña cada 5 cargas (eventos o gastos)
    private static final int LOADS_PER_REVIEW_REQUEST = 5;
    // Primera solicitud de reseña al décimo inicio; luego cada 20 inicios
    private static final int FIRST_REVIEW_OPENS = 10;
    private static final int REVIEW_OPENS_INTERVAL = 20;

    /**
     * Verifica si se debe mostrar la solicitud de reseña y la muestra si corresponde
     * @param activity La actividad actual
     * @param appName El nombre de la app (PetPal, CarPal, FamilyPal, HousePal)
     * @param isExpense true si se acaba de crear un gasto, false si es un evento
     */
    public static void checkAndRequestReview(Activity activity, String appName, boolean isExpense) {
        if (activity == null || appName == null) {
            Log.w(TAG, "Activity o appName es null, no se puede solicitar reseña");
            return;
        }

        SharedPreferences prefs = activity.getSharedPreferences(PREFS_NAME, Activity.MODE_PRIVATE);
        
        // Si ya se completó la reseña, no volver a mostrar nunca más
        if (prefs.getBoolean(KEY_REVIEW_COMPLETED, false)) {
            Log.d(TAG, "Reseña ya completada, no volver a solicitar");
            return;
        }

        // Incrementar contador total de cargas (eventos + gastos)
        int totalLoads = prefs.getInt(KEY_TOTAL_LOAD_COUNT, 0);
        totalLoads++;
        prefs.edit().putInt(KEY_TOTAL_LOAD_COUNT, totalLoads).apply();
        
        Log.d(TAG, "Total de cargas: " + totalLoads);

        // Verificar si se cumple el criterio: cada 5 cargas
        if (totalLoads % LOADS_PER_REVIEW_REQUEST == 0) {
            // Esperar un poco antes de mostrar el diálogo (mejor UX)
            activity.getWindow().getDecorView().postDelayed(() -> {
                showReviewDialog(activity, appName);
            }, 1500); // 1.5 segundos después de guardar
        }
    }

    /**
     * Muestra un diálogo personalizado antes de solicitar la reseña oficial
     * El mensaje cambia según cuántas veces se ha mostrado
     */
    private static void showReviewDialog(Activity activity, String appName) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            return;
        }

        SharedPreferences prefs = activity.getSharedPreferences(PREFS_NAME, Activity.MODE_PRIVATE);
        
        // Incrementar contador de intentos
        int attemptCount = prefs.getInt(KEY_REVIEW_ATTEMPT_COUNT, 0);
        attemptCount++;
        prefs.edit().putInt(KEY_REVIEW_ATTEMPT_COUNT, attemptCount).apply();
        
        // Obtener mensaje según el número de intento
        String message = getReviewMessage(activity, attemptCount, appName);
        String title = getReviewTitle(activity, attemptCount, appName);
        
        new MaterialAlertDialogBuilder(activity)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(R.string.review_yes, (dialog, which) -> {
                    requestReview(activity);
                })
                .setNegativeButton(R.string.review_not_now, (dialog, which) -> {
                    // No marcar como completado, seguirá pidiendo cada 5 cargas
                    dialog.dismiss();
                })
                .setCancelable(true)
                .show();
    }

    /**
     * Obtiene el título del diálogo según el número de intento
     */
    private static String getReviewTitle(android.content.Context ctx, int attemptCount, String appName) {
        if (attemptCount == 1) {
            return ctx.getString(R.string.review_title_1, appName);
        } else if (attemptCount == 2) {
            return ctx.getString(R.string.review_title_2);
        } else if (attemptCount == 3) {
            return ctx.getString(R.string.review_title_3);
        } else if (attemptCount == 4) {
            return ctx.getString(R.string.review_title_4);
        } else {
            return ctx.getString(R.string.review_title_5);
        }
    }

    /**
     * Obtiene el mensaje del diálogo según el número de intento
     */
    private static String getReviewMessage(android.content.Context ctx, int attemptCount, String appName) {
        switch (attemptCount) {
            case 1:
                // Primera vez: mensaje sencillo
                return ctx.getString(R.string.review_message_1, appName);
            
            case 2:
                // Segunda vez: recordatorio
                return ctx.getString(R.string.review_message_2, appName);
            
            case 3:
                // Tercera vez: disculpas pero aclarando que es importante
                return ctx.getString(R.string.review_message_3, appName);
            
            case 4:
                // Cuarta vez: mencionar que es desarrollador independiente
                return ctx.getString(R.string.review_message_4, appName);
            
            case 5:
            default:
                // Quinta vez (y siguientes): si no quiere ser molestado, que califique
                return ctx.getString(R.string.review_message_5, appName);
        }
    }

    /**
     * Solicita la reseña oficial usando Google Play In-App Review API
     */
    private static void requestReview(Activity activity) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            return;
        }

        try {
            ReviewManager manager = ReviewManagerFactory.create(activity);
            manager.requestReviewFlow()
                    .addOnSuccessListener(reviewInfo -> {
                        // Mostrar el diálogo de reseña de Google Play
                        manager.launchReviewFlow(activity, reviewInfo)
                                .addOnSuccessListener(result -> {
                                    // Marcar como completado para no volver a pedir nunca más
                                    SharedPreferences prefs = activity.getSharedPreferences(PREFS_NAME, Activity.MODE_PRIVATE);
                                    prefs.edit().putBoolean(KEY_REVIEW_COMPLETED, true).apply();
                                    Log.d(TAG, "Reseña completada, no se volverá a solicitar");
                                })
                                .addOnFailureListener(e -> {
                                    Log.w(TAG, "Error al mostrar diálogo de reseña: " + e.getMessage());
                                    // Si falla, no marcar como completado para poder intentar más tarde
                                });
                    })
                    .addOnFailureListener(e -> {
                        Log.w(TAG, "Error al solicitar flujo de reseña: " + e.getMessage());
                        // Si falla, no marcar como completado para poder intentar más tarde
                    });
        } catch (Exception e) {
            Log.e(TAG, "Excepción al solicitar reseña: " + e.getMessage(), e);
        }
    }

    /**
     * Verifica si se debe mostrar la solicitud de reseña basada en las veces que se abre la app
     * Se solicita después de la quinta vez que se abre la app
     * @param activity La actividad actual
     * @param appName El nombre de la app (PetPal, CarPal, FamilyPal, HousePal)
     */
    public static void checkAndRequestReviewOnAppOpen(Activity activity, String appName) {
        if (activity == null || appName == null) {
            Log.w(TAG, "Activity o appName es null, no se puede solicitar reseña");
            return;
        }

        SharedPreferences prefs = activity.getSharedPreferences(PREFS_NAME, Activity.MODE_PRIVATE);
        
        // Si ya se completó la reseña, no volver a mostrar nunca más
        if (prefs.getBoolean(KEY_REVIEW_COMPLETED, false)) {
            Log.d(TAG, "Reseña ya completada, no volver a solicitar");
            return;
        }

        // Incrementar contador de aperturas de la app
        int appOpenCount = prefs.getInt(KEY_APP_OPEN_COUNT, 0);
        appOpenCount++;
        prefs.edit().putInt(KEY_APP_OPEN_COUNT, appOpenCount).apply();
        
        Log.d(TAG, "Aperturas de app: " + appOpenCount);

        // Trigger at open 10, then every 20 after that (30, 50, 70 …)
        boolean shouldAsk = appOpenCount == FIRST_REVIEW_OPENS ||
                (appOpenCount > FIRST_REVIEW_OPENS &&
                 (appOpenCount - FIRST_REVIEW_OPENS) % REVIEW_OPENS_INTERVAL == 0);
        if (shouldAsk) {
            activity.getWindow().getDecorView().postDelayed(() -> {
                showReviewDialog(activity, appName);
            }, 2000);
        }
    }

    /**
     * Método de utilidad para resetear el estado de reseña (útil para testing)
     */
    public static void resetReviewState(Activity activity) {
        if (activity != null) {
            SharedPreferences prefs = activity.getSharedPreferences(PREFS_NAME, Activity.MODE_PRIVATE);
            prefs.edit()
                    .putBoolean(KEY_REVIEW_COMPLETED, false)
                    .putInt(KEY_TOTAL_LOAD_COUNT, 0)
                    .putInt(KEY_REVIEW_ATTEMPT_COUNT, 0)
                    .putInt(KEY_APP_OPEN_COUNT, 0)
                    .apply();
            Log.d(TAG, "Estado de reseña reseteado");
        }
    }
}
