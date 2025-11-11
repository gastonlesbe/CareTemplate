package com.gastonlesbegueris.caretemplate.util;

import android.content.Context;

import com.gastonlesbegueris.caretemplate.data.local.AppDb;

/**
 * Guardas de límites de la versión gratuita.
 * Implementación mínima para COMPILAR sin requerir métodos adicionales en los DAO.
 * Ahora usa solo SharedPreferences; luego lo podemos mejorar para contar en DB.
 */
public final class LimitGuard {

    // Límites “free” (ajustables)
    private static final int FREE_EVENT_LIMIT   = 100; // por appType
    private static final int FREE_SUBJECT_LIMIT = 10;  // por appType

    private LimitGuard() {}

    // --- Eventos ---
    // Firma mantenida como la estás usando: (Context, AppDb, String)
    public static boolean canCreateEvent(Context ctx, AppDb db, String appType) {
        // No dependemos de DB. Contamos con SharedPreferences.
        int used = getInt(ctx, key("events_count_", appType), 0);
        return used < FREE_EVENT_LIMIT;
    }

    public static void onEventCreated(Context ctx, String appType) {
        inc(ctx, key("events_count_", appType));
    }

    // --- Sujetos ---
    public static boolean canCreateSubject(Context ctx, AppDb db, String appType) {
        int used = getInt(ctx, key("subjects_count_", appType), 0);
        return used < FREE_SUBJECT_LIMIT;
    }

    public static void onSubjectCreated(Context ctx, String appType) {
        inc(ctx, key("subjects_count_", appType));
    }

    // --- Helpers ---
    private static String key(String base, String appType) {
        return base + (appType == null ? "default" : appType);
    }

    private static int getInt(Context ctx, String k, int def) {
        return ctx.getSharedPreferences("limits", Context.MODE_PRIVATE).getInt(k, def);
    }

    private static void inc(Context ctx, String k) {
        int v = getInt(ctx, k, 0) + 1;
        ctx.getSharedPreferences("limits", Context.MODE_PRIVATE).edit().putInt(k, v).apply();
    }
}
