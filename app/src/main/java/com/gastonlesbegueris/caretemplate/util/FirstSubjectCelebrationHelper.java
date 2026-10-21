package com.gastonlesbegueris.caretemplate.util;

import android.app.Activity;
import android.content.SharedPreferences;
import android.util.Log;

import com.gastonlesbegueris.caretemplate.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

/**
 * Helper class para mostrar felicitación cuando se crea el primer sujeto
 * e invitar a crear el primer evento
 */
public class FirstSubjectCelebrationHelper {
    private static final String TAG = "FirstSubjectCelebration";
    private static final String PREFS_NAME = "celebration_prefs";
    private static final String KEY_FIRST_SUBJECT_CELEBRATED = "first_subject_celebrated";
    
    /**
     * Verifica si se debe mostrar la felicitación y la muestra si corresponde
     * @param activity La actividad actual
     * @param appType El tipo de app (pets, cars, family, house)
     * @param onCreateEvent Callback para crear el primer evento
     */
    public static void checkAndShowCelebration(Activity activity, String appType, Runnable onCreateEvent) {
        if (activity == null || appType == null) {
            Log.w(TAG, "Activity o appType es null, no se puede mostrar celebración");
            return;
        }

        SharedPreferences prefs = activity.getSharedPreferences(PREFS_NAME, Activity.MODE_PRIVATE);
        
        // Si ya se mostró la celebración, no volver a mostrar
        if (prefs.getBoolean(KEY_FIRST_SUBJECT_CELEBRATED + "_" + appType, false)) {
            Log.d(TAG, "Celebración ya mostrada para " + appType);
            return;
        }

        // Marcar como mostrada antes de mostrar el diálogo
        prefs.edit().putBoolean(KEY_FIRST_SUBJECT_CELEBRATED + "_" + appType, true).apply();

        // Esperar un poco para que el toast de "creado" se muestre primero
        activity.getWindow().getDecorView().postDelayed(() -> {
            showCelebrationDialog(activity, appType, onCreateEvent);
        }, 1500);
    }
    
    /**
     * Muestra el diálogo de felicitación e invitación a crear el primer evento
     */
    private static void showCelebrationDialog(Activity activity, String appType, Runnable onCreateEvent) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            return;
        }

        String subjectType = getSubjectTypeName(activity, appType);
        
        // Crear mensaje personalizado usando recursos de strings
        String title = activity.getString(R.string.celebration_first_subject_title);
        String message = activity.getString(R.string.celebration_first_subject_message, subjectType);
        
        new MaterialAlertDialogBuilder(activity)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(activity.getString(R.string.celebration_first_subject_button_yes), (dialog, which) -> {
                    dialog.dismiss();
                    if (onCreateEvent != null) {
                        // Esperar un poco antes de abrir el diálogo de evento
                        activity.getWindow().getDecorView().postDelayed(() -> {
                            onCreateEvent.run();
                        }, 300);
                    }
                })
                .setNegativeButton(activity.getString(R.string.celebration_first_subject_button_later), (dialog, which) -> {
                    dialog.dismiss();
                })
                .setCancelable(true)
                .show();
    }
    
    /**
     * Obtiene el nombre del tipo de sujeto según el flavor usando recursos de strings
     */
    private static String getSubjectTypeName(Activity activity, String appType) {
        int stringResId;
        switch (appType) {
            case "pets":
                stringResId = R.string.subject_type_pet;
                break;
            case "cars":
                stringResId = R.string.subject_type_car;
                break;
            case "family":
                stringResId = R.string.subject_type_family;
                break;
            case "house":
                stringResId = R.string.subject_type_house;
                break;
            default:
                stringResId = R.string.subject_type_default;
                break;
        }
        return activity.getString(stringResId);
    }
    
    /**
     * Resetea el estado de la celebración (útil para testing)
     */
    public static void resetCelebration(Activity activity, String appType) {
        if (activity != null && appType != null) {
            SharedPreferences prefs = activity.getSharedPreferences(PREFS_NAME, Activity.MODE_PRIVATE);
            prefs.edit().putBoolean(KEY_FIRST_SUBJECT_CELEBRATED + "_" + appType, false).apply();
            Log.d(TAG, "Celebración reseteada para " + appType);
        }
    }
}

