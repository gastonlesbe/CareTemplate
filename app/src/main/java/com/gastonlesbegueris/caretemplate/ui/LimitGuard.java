package com.gastonlesbegueris.caretemplate.ui;

import android.content.Context;
import androidx.appcompat.app.AlertDialog;
import com.gastonlesbegueris.caretemplate.data.local.*;

public class LimitGuard {

    public static final int FREE_SUBJECTS_MAX = 3;
    public static final int FREE_EVENTS_MAX   = 30;

    public static boolean canCreateSubject(Context ctx, AppDb db, String appType) {
        int count = db.subjectDao().countForApp(appType);
        if (count >= FREE_SUBJECTS_MAX) {
            new AlertDialog.Builder(ctx)
                    .setTitle("Límite alcanzado")
                    .setMessage("En la versión gratuita podés crear hasta " + FREE_SUBJECTS_MAX +
                            " sujetos. Próximamente: Premium para desbloquear ilimitado.")
                    .setPositiveButton("Ok", null)
                    .show();
            return false;
        }
        return true;
    }

    public static boolean canCreateEvent(Context ctx, AppDb db, String appType) {
        int count = db.eventDao().countEventsForApp(appType);
        if (count >= FREE_EVENTS_MAX) {
            new AlertDialog.Builder(ctx)
                    .setTitle("Límite alcanzado")
                    .setMessage("En la versión gratuita podés crear hasta " + FREE_EVENTS_MAX +
                            " eventos. Próximamente: Premium para desbloquear ilimitado.")
                    .setPositiveButton("Ok", null)
                    .show();
            return false;
        }
        return true;
    }
}
