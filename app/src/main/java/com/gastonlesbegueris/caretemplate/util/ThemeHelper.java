package com.gastonlesbegueris.caretemplate.util;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatDelegate;
import com.gastonlesbegueris.caretemplate.R;

public class ThemeHelper {

    private static final String PREFS_NAME = "theme_prefs";
    private static final String KEY_THEME_MODE = "theme_mode";

    public static final int MODE_SYSTEM = 0;
    public static final int MODE_LIGHT  = 1;
    public static final int MODE_DARK   = 2;

    /** Apply the saved theme. Call from Application.onCreate() before any Activity starts. */
    public static void applyTheme(Context context) {
        switch (getSavedMode(context)) {
            case MODE_LIGHT:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case MODE_DARK:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            default:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        }
    }

    /**
     * Show a single-choice dialog with Light / Dark / System options.
     * Saves the choice and recreates the activity so the change takes effect immediately.
     */
    public static void showThemeDialog(Activity activity) {
        int current = getSavedMode(activity);

        String[] labels = {
            activity.getString(R.string.theme_light),
            activity.getString(R.string.theme_dark),
            activity.getString(R.string.theme_system)
        };
        // Map label index to our MODE constants: 0→Light, 1→Dark, 2→System
        int checkedItem = current == MODE_LIGHT ? 0 : current == MODE_DARK ? 1 : 2;

        new com.google.android.material.dialog.MaterialAlertDialogBuilder(activity)
                .setTitle(activity.getString(R.string.theme_choose))
                .setSingleChoiceItems(labels, checkedItem, (dialog, which) -> {
                    int mode = which == 0 ? MODE_LIGHT : which == 1 ? MODE_DARK : MODE_SYSTEM;
                    saveMode(activity, mode);
                    dialog.dismiss();
                    applyTheme(activity);
                    activity.recreate();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    public static int getSavedMode(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getInt(KEY_THEME_MODE, MODE_SYSTEM);
    }

    private static void saveMode(Context context, int mode) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putInt(KEY_THEME_MODE, mode).apply();
    }
}
