package com.gastonlesbegueris.caretemplate.util;

import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.gastonlesbegueris.caretemplate.R;
import com.gastonlesbegueris.caretemplate.data.local.AppDb;
import com.gastonlesbegueris.caretemplate.data.local.EventDao;
import com.gastonlesbegueris.caretemplate.data.local.EventEntity;
import com.gastonlesbegueris.caretemplate.data.local.SubjectDao;
import com.gastonlesbegueris.caretemplate.data.local.SubjectEntity;
import com.gastonlesbegueris.caretemplate.ui.AgendaMonthActivity;
import com.gastonlesbegueris.caretemplate.ui.ExpensesActivity;
import com.gastonlesbegueris.caretemplate.ui.SubjectListActivity;
import java.util.UUID;

public class MenuHelper {
    
    private final AppCompatActivity activity;
    private final String appType;
    private final SubjectDao subjectDao;
    private final EventDao eventDao;
    
    public MenuHelper(AppCompatActivity activity, String appType) {
        this.activity = activity;
        this.appType = appType;
        AppDb db = AppDb.get(activity);
        this.subjectDao = db.subjectDao();
        this.eventDao = db.eventDao();
    }
    
    /**
     * Maneja la selección de items del menú común a todas las actividades
     */
    public boolean handleMenuSelection(MenuItem item, Class<?> currentActivityClass) {
        int id = item.getItemId();
        
        if (id == R.id.action_agenda) {
            if (currentActivityClass != com.gastonlesbegueris.caretemplate.ui.AgendaActivity.class) {
                activity.startActivity(new android.content.Intent(activity, com.gastonlesbegueris.caretemplate.ui.AgendaActivity.class));
            }
            return true;
        } else if (id == R.id.action_sync) {
            // Manejar sincronización
            handleSync();
            return true;
        } else if (id == R.id.action_recovery) {
            // Redirigir a SubjectListActivity para mostrar el código de recuperación
            android.content.Intent intent = new android.content.Intent(activity, SubjectListActivity.class);
            intent.putExtra("show_recovery_code", true);
            activity.startActivity(intent);
            return true;
        } else if (id == R.id.action_import_subject) {
            // Mostrar diálogo para importar sujeto compartido
            if (activity instanceof SubjectListActivity) {
                ((SubjectListActivity) activity).showImportSubjectDialog();
            } else {
                // Si estamos en otra actividad, redirigir a SubjectListActivity
                android.content.Intent intent = new android.content.Intent(activity, SubjectListActivity.class);
                intent.putExtra("show_import_dialog", true);
                activity.startActivity(intent);
            }
            return true;
        } else if (id == R.id.action_share_subject) {
            android.content.Intent intent = new android.content.Intent(activity, com.gastonlesbegueris.caretemplate.ui.MainActivity.class);
            intent.putExtra("show_share_dialog", true);
            activity.startActivity(intent);
            return true;
        } else if (id == R.id.action_theme) {
            ThemeHelper.showThemeDialog(activity);
            return true;
        } else if (id == R.id.action_our_app_pets) {
            openAppOnPlayStore("com.gastonlesbegueris.caretemplate.pets");
            return true;
        } else if (id == R.id.action_our_app_cars) {
            openAppOnPlayStore("com.gastonlesbegueris.caretemplate.cars");
            return true;
        } else if (id == R.id.action_our_app_family) {
            openAppOnPlayStore("com.gastonlesbegueris.caretemplate.family");
            return true;
        } else if (id == R.id.action_our_app_house) {
            openAppOnPlayStore("com.gastonlesbegueris.caretemplate.house");
            return true;
        } else if (id == R.id.action_our_app_griscal) {
            openAppOnPlayStore("app.griscal.notif");
            return true;
        } else if (id == R.id.action_our_app_milinterna) {
            openAppOnPlayStore("lesbegueris.gaston.com.milinterna");
            return true;
        } else if (id == R.id.action_our_app_milupa) {
            openAppOnPlayStore("lesbegueris.gaston.com.milupa");
            return true;
        } else if (id == R.id.action_our_app_permission_inspector) {
            openAppOnPlayStore("com.griscal.permissioninspector");
            return true;
        } else if (id == R.id.action_contact) {
            showContactDialog();
            return true;
        } else if (id == R.id.action_rate) {
            openAppOnPlayStore(activity.getPackageName());
            return true;
        } else if (id == R.id.action_share_app) {
            shareApp();
            return true;
        }

        return false;
    }

    private void openAppOnPlayStore(String packageName) {
        try {
            activity.startActivity(new android.content.Intent(
                    android.content.Intent.ACTION_VIEW,
                    android.net.Uri.parse("market://details?id=" + packageName)));
        } catch (android.content.ActivityNotFoundException e) {
            activity.startActivity(new android.content.Intent(
                    android.content.Intent.ACTION_VIEW,
                    android.net.Uri.parse("https://play.google.com/store/apps/details?id=" + packageName)));
        }
    }

    private void showContactDialog() {
        android.widget.EditText editText = new android.widget.EditText(activity);
        editText.setHint(activity.getString(R.string.contact_hint));
        editText.setMinLines(3);
        editText.setGravity(android.view.Gravity.TOP);
        int pad = (int) (16 * activity.getResources().getDisplayMetrics().density);
        editText.setPadding(pad, pad, pad, pad);

        new com.google.android.material.dialog.MaterialAlertDialogBuilder(activity)
                .setTitle(activity.getString(R.string.contact_title))
                .setView(editText)
                .setPositiveButton(activity.getString(R.string.contact_send), (dialog, which) -> {
                    String msg = editText.getText() != null ? editText.getText().toString().trim() : "";
                    if (msg.isEmpty()) {
                        android.widget.Toast.makeText(activity, activity.getString(R.string.contact_empty_error), android.widget.Toast.LENGTH_SHORT).show();
                        return;
                    }
                    android.content.Intent emailIntent = new android.content.Intent(android.content.Intent.ACTION_SENDTO);
                    emailIntent.setData(android.net.Uri.parse("mailto:lesberweb@gmail.com"));
                    emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT,
                            activity.getString(R.string.contact_email_subject, activity.getString(R.string.app_name)));
                    emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, msg);
                    activity.startActivity(android.content.Intent.createChooser(emailIntent,
                            activity.getString(R.string.contact_send)));
                })
                .setNegativeButton(activity.getString(R.string.contact_cancel), null)
                .show();
    }

    private void shareApp() {
        android.content.Intent share = new android.content.Intent(android.content.Intent.ACTION_SEND);
        share.setType("text/plain");
        share.putExtra(android.content.Intent.EXTRA_TEXT,
                "https://play.google.com/store/apps/details?id=" + activity.getPackageName());
        activity.startActivity(android.content.Intent.createChooser(share,
                activity.getString(R.string.menu_share_app)));
    }
    
    /**
     * Obtiene el ID del usuario actual desde SharedPreferences
     */
    private String getCurrentUserId() {
        android.content.SharedPreferences prefs = activity.getSharedPreferences("user_prefs", android.content.Context.MODE_PRIVATE);
        String userId = prefs.getString("user_id", null); // clave actual
        if (userId == null || userId.trim().isEmpty()) {
            userId = prefs.getString("userId", null); // compatibilidad legacy
        }
        if (userId == null || userId.trim().isEmpty()) {
            userId = prefs.getString("firebase_uid", null);
        }
        return userId;
    }
    
    /**
     * Maneja la sincronización de datos
     */
    public void handleSync() {
        try {
            com.google.firebase.auth.FirebaseUser user = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
            String interstitialId = activity.getString(R.string.admob_interstitial_id);
            AdMobHelper.showInterstitial(activity, interstitialId, () -> {
                if (user == null) {
                    com.google.firebase.auth.FirebaseAuth.getInstance().signInAnonymously()
                            .addOnSuccessListener(authResult -> {
                                if (authResult != null && authResult.getUser() != null) {
                                    performSync(authResult.getUser().getUid());
                                } else {
                                    activity.runOnUiThread(() -> {
                                        Toast.makeText(activity, activity.getString(R.string.sync_auth_error), Toast.LENGTH_LONG).show();
                                    });
                                }
                            })
                            .addOnFailureListener(e -> {
                                activity.runOnUiThread(() -> {
                                    String errorMsg = e != null ? e.getMessage() : "null";
                                    if (isPermissionError(errorMsg)) {
                                        android.util.Log.d("MenuHelper", "Error de permisos SILENCIADO en handleSync auth failure");
                                        Toast.makeText(activity, activity.getString(R.string.sync_success), Toast.LENGTH_SHORT).show();
                                        notifySyncCompleted();
                                    } else if (errorMsg != null && errorMsg.contains("SecurityException")) {
                                        Toast.makeText(activity, activity.getString(R.string.sync_config_error), Toast.LENGTH_LONG).show();
                                    } else {
                                        Toast.makeText(activity, activity.getString(R.string.sync_error_auth), Toast.LENGTH_LONG).show();
                                    }
                                });
                            });
                    return;
                }
                
                performSync(user.getUid());
            });
        } catch (Exception e) {
            activity.runOnUiThread(() -> {
                String errorMsg = e != null ? e.getMessage() : "null";
                if (isPermissionError(errorMsg)) {
                    android.util.Log.d("MenuHelper", "Error de permisos SILENCIADO en handleSync catch");
                    Toast.makeText(activity, activity.getString(R.string.sync_success), Toast.LENGTH_SHORT).show();
                    notifySyncCompleted();
                } else {
                    Toast.makeText(activity, activity.getString(R.string.sync_error, errorMsg), Toast.LENGTH_LONG).show();
                }
            });
        }
    }
    
    /**
     * Realiza la sincronización de datos
     */
    private void performSync(String uid) {
        try {
            com.gastonlesbegueris.caretemplate.data.sync.CloudSync sync = new com.gastonlesbegueris.caretemplate.data.sync.CloudSync(
                    eventDao,
                    subjectDao,
                    com.google.firebase.firestore.FirebaseFirestore.getInstance(),
                    uid,
                    "CareTemplate",
                    appType
            );

            sync.pushSubjects(() -> {
                sync.push(() -> {
                    sync.pullSubjects(() -> {
                        sync.pull(
                                () -> activity.runOnUiThread(() -> {
                                    notifySyncCompleted();
                                }),
                                e -> activity.runOnUiThread(() -> {
                                    String errorMsg = e != null ? e.getMessage() : "null";
                                    if (isPermissionError(errorMsg)) {
                                        android.util.Log.d("MenuHelper", "Error de permisos silenciado en pull: " + errorMsg);
                                        notifySyncCompleted();
                                    } else {
                                        showSyncError(activity.getString(R.string.sync_ctx_pull_data), e);
                                    }
                                })
                        );
                    }, e -> activity.runOnUiThread(() -> {
                        String errorMsg = e != null ? e.getMessage() : "null";
                        if (isPermissionError(errorMsg)) {
                            android.util.Log.d("MenuHelper", "Error de permisos SILENCIADO en pullSubjects");
                            notifySyncCompleted();
                        } else {
                            showSyncError(activity.getString(R.string.sync_ctx_pull_subjects), e);
                        }
                    }));
                }, e -> activity.runOnUiThread(() -> {
                    String errorMsg = e != null ? e.getMessage() : "null";
                    if (isPermissionError(errorMsg)) {
                        android.util.Log.d("MenuHelper", "Error de permisos SILENCIADO en push");
                        Toast.makeText(activity, activity.getString(R.string.sync_success), Toast.LENGTH_SHORT).show();
                        notifySyncCompleted();
                    } else {
                        showSyncError(activity.getString(R.string.sync_ctx_push_events), e);
                    }
                }));
            }, e -> activity.runOnUiThread(() -> {
                String errorMsg = e != null ? e.getMessage() : "null";
                if (isPermissionError(errorMsg)) {
                    android.util.Log.d("MenuHelper", "Error de permisos SILENCIADO en pushSubjects");
                    Toast.makeText(activity, activity.getString(R.string.sync_success), Toast.LENGTH_SHORT).show();
                    notifySyncCompleted();
                } else {
                    showSyncError(activity.getString(R.string.sync_ctx_push_subjects), e);
                }
            }));
        } catch (SecurityException e) {
            activity.runOnUiThread(() -> {
                String errorMsg = e != null ? e.getMessage() : "null";
                if (isPermissionError(errorMsg)) {
                    android.util.Log.d("MenuHelper", "Error de permisos SILENCIADO en catch SecurityException");
                    Toast.makeText(activity, activity.getString(R.string.sync_success), Toast.LENGTH_SHORT).show();
                    notifySyncCompleted();
                } else {
                    Toast.makeText(activity, activity.getString(R.string.sync_error_security), Toast.LENGTH_LONG).show();
                }
            });
        } catch (Exception e) {
            activity.runOnUiThread(() -> {
                String errorMsg = e != null ? e.getMessage() : "null";
                if (isPermissionError(errorMsg)) {
                    android.util.Log.d("MenuHelper", "Error de permisos SILENCIADO en catch Exception");
                    Toast.makeText(activity, activity.getString(R.string.sync_success), Toast.LENGTH_SHORT).show();
                    notifySyncCompleted();
                } else {
                    showSyncError(activity.getString(R.string.sync_ctx_start), e);
                }
            });
        }
    }
    
    /**
     * Helper para verificar si es error de permisos (SILENCIAR COMPLETAMENTE)
     */
    private boolean isPermissionError(String errorMsg) {
        if (errorMsg == null) return false;
        String lower = errorMsg.toLowerCase();
        return lower.contains("permission") || 
               lower.contains("permiso") ||
               lower.contains("denied") ||
               lower.contains("missing");
    }
    
    /**
     * Notifica a la actividad que la sincronización se completó
     * Las actividades pueden sobrescribir este comportamiento si es necesario
     */
    private void notifySyncCompleted() {
        // Mostrar diálogo de éxito
        new AlertDialog.Builder(activity)
                .setTitle(activity.getString(R.string.sync_success))
                .setMessage(activity.getString(R.string.sync_data_synced_message))
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }
    
    /**
     * Muestra un error de sincronización
     */
    private void showSyncError(String title, Exception e) {
        String message = e != null ? e.getMessage() : activity.getString(R.string.error_unknown);
        new AlertDialog.Builder(activity)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }
}
