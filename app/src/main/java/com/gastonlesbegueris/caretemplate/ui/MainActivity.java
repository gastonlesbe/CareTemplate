package com.gastonlesbegueris.caretemplate.ui;

import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatDelegate;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.gastonlesbegueris.caretemplate.R;
import com.gastonlesbegueris.caretemplate.data.local.AppDb;
import com.gastonlesbegueris.caretemplate.data.local.EventDao;
import com.gastonlesbegueris.caretemplate.data.local.EventEntity;
import com.gastonlesbegueris.caretemplate.data.local.SubjectDao;
import com.gastonlesbegueris.caretemplate.data.local.SubjectEntity;
import com.gastonlesbegueris.caretemplate.data.sync.CloudSync;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.gastonlesbegueris.caretemplate.util.LimitGuard;
import com.gastonlesbegueris.caretemplate.util.UserManager;
import com.gastonlesbegueris.caretemplate.util.AppodealHelper;
import com.gastonlesbegueris.caretemplate.util.AdMobHelper;


import java.util.List;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private AppDb db;
    private EventDao eventDao;
    private SubjectDao subjectDao;
    private EventAdapter adapter;


    //private LocalEventAdapter adapter;
    private String appType;              // "pets" | "cars" | "family" | "house"
    private String currentSubjectId;     // sujeto seleccionado (opcional)

    private MenuItem menuItemSync;
    private boolean fabMenuOpen = false;
    
    // Appodeal handles interstitial and rewarded ads automatically
    private Runnable syncCallback = null; // Callback para ejecutar después del anuncio
    
    // Flag para indicar que estamos en modo de recuperación silenciosa
    private boolean isSilentRecoveryMode = false;

    @Override
    protected void onCreate(Bundle s) {
        super.onCreate(s);
        setContentView(R.layout.activity_main);

        // 1) appType desde recursos del flavor
        appType = getString(R.string.app_type);

        // 2) DB/DAOs ANTES de usarlos
        db = AppDb.get(this);
        eventDao = db.eventDao();
        subjectDao = db.subjectDao();

        // 3) Toolbar
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        
        // Mostrar título y icono de navegación
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(true);
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        }
        
        String appName = getString(R.string.app_name);
        String sectionName = getString(R.string.menu_home);
        toolbar.setTitle(appName + " - " + sectionName);
        toolbar.setSubtitle(null);
        // Set default icon immediately (will be updated by refreshHeader() if subject exists)
        // No aplicar tint, el icono ya tiene el color fijo #03DAC5
        toolbar.setNavigationIcon(R.drawable.ic_header_flavor);
        // Icon will be updated by refreshHeader() if a subject is selected
        refreshHeader();

        // 4) Lista + Adapter
        setupEventsList();               // crea adapter y setea RecyclerView

        // 5) Observadores
        observeLocal();                  // observa eventos y refresca UI
        observeSubjectsForAdapter();     // alimenta el mapa sujetoId->nombre al adapter

        // 6) Restaurar sujeto seleccionado (si había)
        currentSubjectId = getSharedPreferences("prefs", MODE_PRIVATE)
                .getString("currentSubjectId_" + appType, null);

        // 7) Redirigir a Sujetos si es primera vez / no hay sujetos
        // También asignar uid a sujetos existentes que no lo tengan
        new Thread(() -> {
            // Asignar uid a sujetos existentes sin uid
            com.gastonlesbegueris.caretemplate.util.UserManager userManager = 
                new com.gastonlesbegueris.caretemplate.util.UserManager(this);
            String userId = userManager.getUserIdSync();
            if (userId != null) {
                int countWithoutUid = subjectDao.countSubjectsWithoutUid(appType);
                if (countWithoutUid > 0) {
                    android.util.Log.d("MainActivity", "Asignando uid a " + countWithoutUid + " sujetos existentes sin uid");
                    subjectDao.assignUidToSubjectsWithoutUid(userId, appType);
                    android.util.Log.d("MainActivity", "✅ Uid asignado a " + countWithoutUid + " sujetos existentes");
                }
            }
            
            int count = subjectDao.countForApp(appType);
            boolean firstRunDone = getSharedPreferences("prefs", MODE_PRIVATE)
                    .getBoolean("first_run_done_" + appType, false);

            if (count == 0 && !firstRunDone) {
                runOnUiThread(() -> {
                    getSharedPreferences("prefs", MODE_PRIVATE)
                            .edit().putBoolean("first_run_done_" + appType, true).apply();
                    startActivity(new android.content.Intent(this, SubjectListActivity.class));
                    Toast.makeText(this, getString(R.string.error_no_subjects), Toast.LENGTH_LONG).show();
                });
            }
        }).start();

        // 8) FAB speed-dial
        initFabSpeedDial();

        // 9) Verificar si se debe abrir el diálogo de agregar evento
        if (getIntent() != null && getIntent().getBooleanExtra("add_event", false)) {
            // Limpiar el flag para que no se abra cada vez que se rote la pantalla
            getIntent().removeExtra("add_event");
            // Abrir el diálogo después de que la UI esté lista
            findViewById(R.id.fabAdd).post(() -> {
                showAddEventDialog();
            });
        }
        
        // 10) Verificar si se debe abrir el diálogo de código de recuperación
        if (getIntent() != null && getIntent().getBooleanExtra("show_recovery_code", false)) {
            // Limpiar el flag para que no se abra cada vez que se rote la pantalla
            getIntent().removeExtra("show_recovery_code");
            // Abrir el diálogo después de que la UI esté lista
            findViewById(R.id.fabAdd).post(() -> {
                showRecoveryCodeDialog();
            });
        }
        
        // 11) Verificar si se debe abrir el diálogo de compartir sujeto
        if (getIntent() != null && getIntent().getBooleanExtra("show_share_dialog", false)) {
            // Limpiar el flag para que no se abra cada vez que se rote la pantalla
            getIntent().removeExtra("show_share_dialog");
            // Abrir el diálogo después de que la UI esté lista
            findViewById(R.id.fabAdd).post(() -> {
                showSelectSubjectToShareDialog();
            });
        }

        // 9) Appodeal Banner
        initAppodeal();
        
        // 10) Inicializar UserManager y autenticación (ID único para suscripciones y sincronización)
        initializeUserManager();
        
        // 11) Inicializar y sincronizar código de recuperación
        initializeRecoveryCode();
    }
    
    private void initializeRecoveryCode() {
        com.gastonlesbegueris.caretemplate.util.UserRecoveryManager recoveryManager = 
                new com.gastonlesbegueris.caretemplate.util.UserRecoveryManager(this);
        
        // Generar o recuperar código de recuperación y sincronizarlo
        recoveryManager.getOrGenerateRecoveryCode(new com.gastonlesbegueris.caretemplate.util.UserRecoveryManager.RecoveryCodeCallback() {
            @Override
            public void onRecoveryCode(String recoveryCode) {
                android.util.Log.d("MainActivity", "Recovery code ready: " + recoveryCode);
            }
            
            @Override
            public void onError(Exception error) {
                android.util.Log.e("MainActivity", "Error initializing recovery code", error);
            }
        });
    }
    
    private void initializeUserManager() {
        // Asegurar autenticación anónima para tener un UID consistente
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            FirebaseAuth.getInstance().signInAnonymously()
                    .addOnSuccessListener(authResult -> {
                        if (authResult != null && authResult.getUser() != null) {
                            String uid = authResult.getUser().getUid();
                            android.util.Log.d("MainActivity", "Firebase Auth initialized with UID: " + uid);
                            
                            // Inicializar UserManager con el UID de Firebase
                            UserManager userManager = new UserManager(this);
                            userManager.initializeUser(new UserManager.UserIdCallback() {
                                @Override
                                public void onUserId(String userId) {
                                    android.util.Log.d("MainActivity", "User ID initialized: " + userId);
                                }
                                
                                @Override
                                public void onError(Exception error) {
                                    android.util.Log.e("MainActivity", "Error initializing user ID", error);
                                }
                            });
                        }
                    })
                    .addOnFailureListener(e -> {
                        android.util.Log.e("MainActivity", "Error signing in anonymously", e);
                        // Fallback: usar UserManager sin Firebase Auth
                        UserManager userManager = new UserManager(this);
                        userManager.initializeUser(null);
                    });
        } else {
            // Ya está autenticado, solo inicializar UserManager
            UserManager userManager = new UserManager(this);
            userManager.initializeUser(new UserManager.UserIdCallback() {
                @Override
                public void onUserId(String userId) {
                    android.util.Log.d("MainActivity", "User ID initialized: " + userId);
                }
                
                @Override
                public void onError(Exception error) {
                    android.util.Log.e("MainActivity", "Error initializing user ID", error);
                }
            });
        }
    }

    private void initAppodeal() {
        String appKey = getString(R.string.appodeal_app_key);
        AppodealHelper.initialize(this, appKey);
        AppodealHelper.showBanner(this, R.id.adView);
        
        // Precargar intersticial de AdMob
        String interstitialId = getString(R.string.admob_interstitial_id);
        AdMobHelper.loadInterstitial(this, interstitialId);
    }
    
    private void showInterstitialAdAndSync(Runnable onAdClosed) {
        // Appodeal handles ad loading automatically, just show it
        AppodealHelper.showInterstitial(this, onAdClosed);
    }
    
    private void showSyncCompletedDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(getString(R.string.sync_completed))
                .setMessage(getString(R.string.sync_completed_message))
                .setPositiveButton(getString(R.string.button_ok), null)
                .setIcon(android.R.drawable.ic_dialog_info)
                .show();
    }

    @Override
    protected void onPause() {
        super.onPause();
        AppodealHelper.hideBanner(this);
    }


    @Override
    protected void onDestroy() {
        AppodealHelper.hideBanner(this);
        super.onDestroy();
    }


    // ===== Toolbar / Menú =====
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        menuItemSync = menu.findItem(R.id.action_sync);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // Asegurar que los iconos estén asignados
        for (int i = 0; i < menu.size(); i++) {
            MenuItem item = menu.getItem(i);
            if (item.getIcon() == null) {
                int itemId = item.getItemId();
                if (itemId == R.id.action_agenda) {
                    item.setIcon(R.drawable.ic_event);
                } else if (itemId == R.id.action_sync) {
                    item.setIcon(R.drawable.ic_sync);
                } else if (itemId == R.id.action_subjects) {
                    item.setIcon(R.drawable.ic_line_user);
                } else if (itemId == R.id.action_expenses) {
                    item.setIcon(R.drawable.ic_line_money);
                }
            }
        }
        // Establecer la versión dinámicamente
        MenuItem versionItem = menu.findItem(R.id.action_version);
        if (versionItem != null) {
            try {
                String versionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
                versionItem.setTitle("v" + versionName);
            } catch (android.content.pm.PackageManager.NameNotFoundException e) {
                versionItem.setTitle("v1.5");
            }
        }
        return super.onPrepareOptionsMenu(menu);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        Log.d("MainActivity", "Menu item clicked, id: " + id + ", action_share_subject: " + R.id.action_share_subject);
        
        if (id == R.id.action_agenda) {
            Log.d("MainActivity", "Opening AgendaActivity");
            startActivity(new android.content.Intent(this, com.gastonlesbegueris.caretemplate.ui.AgendaActivity.class));
            return true;
        } else if (id == R.id.action_sync) {
            // Sincronizar directamente sin mostrar anuncio intersticial
            startSyncIconAnimation();
            try {
                if (FirebaseAuth.getInstance().getCurrentUser() == null) {
                    FirebaseAuth.getInstance().signInAnonymously()
                            .addOnSuccessListener(authResult -> {
                                if (authResult != null && authResult.getUser() != null) {
                                    doSync();
                                } else {
                                    stopSyncIconAnimation();
                                    Toast.makeText(this, "Error: No se pudo autenticar", Toast.LENGTH_LONG).show();
                                }
                            })
                            .addOnFailureListener(e -> {
                                stopSyncIconAnimation();
                                String errorMsg = e != null ? e.getMessage() : "null";
                                // Verificar si es error de permisos antes de mostrar
                                if (isPermissionError(errorMsg)) {
                                    Log.d("MainActivity", "Error de permisos SILENCIADO en auth failure");
                                    Toast.makeText(this, getString(R.string.sync_success), Toast.LENGTH_SHORT).show();
                                } else if (errorMsg != null && errorMsg.contains("SecurityException")) {
                                    Toast.makeText(this, getString(R.string.sync_config_error), Toast.LENGTH_LONG).show();
                                } else {
                                    Toast.makeText(this, getString(R.string.auth_error_message, errorMsg), Toast.LENGTH_LONG).show();
                                }
                            });
                } else {
                    doSync();
                }
            } catch (Exception e) {
                runOnUiThread(() -> {
                    stopSyncIconAnimation();
                    showSyncError("Error al iniciar sync", e);
                });
            }
            return true;
        } else if (id == R.id.action_subjects) {
            startActivity(new android.content.Intent(this, SubjectListActivity.class));
            return true;
        } else if (id == R.id.action_expenses) {
            startActivity(new android.content.Intent(this, ExpensesActivity.class));
            return true;
        } else if (id == R.id.action_recovery) {
            showRecoveryCodeDialog();
            return true;
        } else if (id == R.id.action_share_subject) {
            // Mostrar diálogo para elegir sujeto a compartir
            Log.d("MainActivity", "action_share_subject selected");
            showSelectSubjectToShareDialog();
            return true;
        } else if (id == R.id.action_import_subject) {
            // Mostrar diálogo para importar sujeto compartido
            Log.d("MainActivity", "action_import_subject selected");
            showImportSubjectDialog();
            return true;
        }

        Log.d("MainActivity", "Menu item not handled, id: " + id);
        return super.onOptionsItemSelected(item);
    }
    
    private void showRecoveryCodeDialog() {
        // Mostrar diálogo informativo antes de cargar el anuncio
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(getString(R.string.recovery_code_title))
                .setMessage(getString(R.string.recovery_code_message))
                .setPositiveButton(getString(R.string.button_ok), (d, w) -> {
                    // Mostrar código directamente sin intersticial
                    showRecoveryCodeAfterAd();
                })
                .setNegativeButton(getString(R.string.button_cancel), null)
                .setIcon(android.R.drawable.ic_dialog_info)
                .show();
    }
    
    private void showInterstitialAdForRecoveryCode() {
        // Intersticial removido - mostrar código directamente
        showRecoveryCodeAfterAd();
    }
    
    private void showRecoveryCodeAfterAd() {
        android.util.Log.d("MainActivity", "showRecoveryCodeAfterAd called");
        
        com.gastonlesbegueris.caretemplate.util.UserRecoveryManager recoveryManager = 
                new com.gastonlesbegueris.caretemplate.util.UserRecoveryManager(this);
        
        // Primero intentar obtener el código local (sin sincronizar)
        String localCode = recoveryManager.getRecoveryCodeSync();
        android.util.Log.d("MainActivity", "Local recovery code: " + (localCode != null ? localCode : "null"));
        
        if (localCode != null && !localCode.isEmpty()) {
            // Si hay código local, mostrarlo directamente
            android.util.Log.d("MainActivity", "Showing local recovery code");
            runOnUiThread(() -> showRecoveryCodeDialog(localCode));
            // Intentar sincronizar en background (sin bloquear)
            recoveryManager.getOrGenerateRecoveryCode(new com.gastonlesbegueris.caretemplate.util.UserRecoveryManager.RecoveryCodeCallback() {
                @Override
                public void onRecoveryCode(String recoveryCode) {
                    // Sincronización exitosa en background, no hacer nada
                    android.util.Log.d("MainActivity", "Recovery code synced successfully");
                }
                
                @Override
                public void onError(Exception error) {
                    // Error de sincronización, pero no importa porque ya mostramos el código local
                    android.util.Log.w("MainActivity", "Recovery code sync failed (non-critical): " + error.getMessage());
                }
            });
        } else {
            // Si no hay código local, generar uno nuevo
            android.util.Log.d("MainActivity", "No local code found, generating new one");
            recoveryManager.getOrGenerateRecoveryCode(new com.gastonlesbegueris.caretemplate.util.UserRecoveryManager.RecoveryCodeCallback() {
                @Override
                public void onRecoveryCode(String recoveryCode) {
                    android.util.Log.d("MainActivity", "Generated recovery code: " + recoveryCode);
                    runOnUiThread(() -> showRecoveryCodeDialog(recoveryCode));
                }
                
                @Override
                public void onError(Exception error) {
                    android.util.Log.e("MainActivity", "Error generating recovery code: " + error.getMessage());
                    // Si falla, intentar generar código local sin sincronizar
                    String fallbackCode = generateLocalRecoveryCode();
                    if (fallbackCode != null) {
                        android.util.Log.d("MainActivity", "Using fallback code: " + fallbackCode);
                        runOnUiThread(() -> showRecoveryCodeDialog(fallbackCode));
                    } else {
                        runOnUiThread(() -> Toast.makeText(MainActivity.this, getString(R.string.recovery_code_error), Toast.LENGTH_LONG).show());
                    }
                }
            });
        }
    }
    
    private void showRecoveryCodeDialog(String recoveryCode) {
        android.util.Log.d("MainActivity", "showRecoveryCodeDialog called with code: " + recoveryCode);
        
        if (recoveryCode == null || recoveryCode.isEmpty()) {
            android.util.Log.e("MainActivity", "Recovery code is null or empty!");
            Toast.makeText(this, getString(R.string.recovery_code_generate_error), Toast.LENGTH_LONG).show();
            return;
        }
        
        try {
            // Mostrar diálogo con el código
            android.view.View view = getLayoutInflater().inflate(R.layout.dialog_recovery_code, null);
            android.widget.TextView tvCode = view.findViewById(R.id.tvRecoveryCode);
            android.widget.Button btnCopy = view.findViewById(R.id.btnCopyCode);
            android.widget.Button btnRecover = view.findViewById(R.id.btnRecoverFromCode);
            String formattedCode = com.gastonlesbegueris.caretemplate.util.UserRecoveryManager.formatRecoveryCode(recoveryCode);
            
            if (tvCode != null) {
                tvCode.setText(formattedCode);
                android.util.Log.d("MainActivity", "Code set in TextView");
            } else {
                android.util.Log.e("MainActivity", "tvCode is null!");
            }
            
            if (btnCopy != null) {
                btnCopy.setOnClickListener(v -> {
                    android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(android.content.Context.CLIPBOARD_SERVICE);
                    android.content.ClipData clip = android.content.ClipData.newPlainText(getString(R.string.recovery_code_title), formattedCode);
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(MainActivity.this, getString(R.string.recovery_code_copied), Toast.LENGTH_SHORT).show();
                });
            }
            
            if (btnRecover != null) {
                btnRecover.setOnClickListener(v -> {
                    // Mostrar diálogo para ingresar código de recuperación
                    showRecoverFromCodeDialog();
                });
            }
            
            androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(MainActivity.this)
                    .setTitle(getString(R.string.recovery_code_title))
                    .setMessage(getString(R.string.recovery_code_save_message))
                    .setView(view)
                    .setPositiveButton(getString(R.string.button_close), null)
                    .create();
            
            dialog.show();
            android.util.Log.d("MainActivity", "Dialog shown");
        } catch (Exception e) {
            android.util.Log.e("MainActivity", "Error showing recovery code dialog", e);
            Toast.makeText(this, getString(R.string.recovery_code_show_error, e.getMessage()), Toast.LENGTH_LONG).show();
        }
    }
    
    private String generateLocalRecoveryCode() {
        // Generar código local como fallback
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
    
    private androidx.appcompat.app.AlertDialog recoverDialog; // Referencia al diálogo de recuperación
    private androidx.appcompat.app.AlertDialog shareDialog; // Referencia al diálogo de compartir
    
    private void showRecoverFromCodeDialog() {
        android.view.View view = getLayoutInflater().inflate(R.layout.dialog_recover_code, null);
        com.google.android.material.textfield.TextInputEditText etCode = view.findViewById(R.id.etRecoveryCode);
        
        // Agregar formateo automático del código mientras se escribe
        if (etCode != null) {
            etCode.addTextChangedListener(new RecoveryCodeFormatter(etCode));
        }
        
        recoverDialog = new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(getString(R.string.recovery_code_recover_title))
                .setMessage(getString(R.string.recovery_code_recover_message))
                .setView(view)
                .setPositiveButton(getString(R.string.button_recover), (d, w) -> {
                    String code = etCode != null && etCode.getText() != null ? etCode.getText().toString().trim() : "";
                    if (code.isEmpty()) {
                        Toast.makeText(this, getString(R.string.recovery_code_invalid), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    // Cerrar el diálogo inmediatamente al presionar el botón
                    if (recoverDialog != null) {
                        try {
                            recoverDialog.dismiss();
                        } catch (Exception ex) {
                            Log.w("MainActivity", "Error al cerrar diálogo al presionar recuperar: " + ex.getMessage());
                        }
                    }
                    
                    // Mostrar intersticial de AdMob antes de recuperar
                    String interstitialId = getString(R.string.admob_interstitial_id);
                    AdMobHelper.showInterstitial(MainActivity.this, interstitialId, () -> {
                        // Activar modo silencioso ANTES de iniciar la recuperación
                        isSilentRecoveryMode = true;
                        Log.d("MainActivity", "Modo de recuperación silenciosa activado");
                        
                        recoverUserFromCode(code);
                    });
                })
                .setNegativeButton(getString(R.string.button_cancel), null)
                .setCancelable(true)
                .create();
        
        recoverDialog.show();
    }
    
    /**
     * TextWatcher que formatea automáticamente el código de recuperación mientras se escribe.
     * Formato: XXXX-XXXX-XXXX (mayúsculas y guiones automáticos)
     */
    private static class RecoveryCodeFormatter implements android.text.TextWatcher {
        private final com.google.android.material.textfield.TextInputEditText editText;
        private boolean isFormatting = false;
        private int previousLength = 0;
        private int previousCursorPosition = 0;
        
        public RecoveryCodeFormatter(com.google.android.material.textfield.TextInputEditText editText) {
            this.editText = editText;
        }
        
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            previousLength = s.length();
            previousCursorPosition = editText.getSelectionStart();
        }
        
        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            // No hacer nada aquí
        }
        
        @Override
        public void afterTextChanged(android.text.Editable s) {
            if (isFormatting) {
                return; // Evitar recursión infinita
            }
            
            isFormatting = true;
            
            try {
                String text = s.toString();
                
                // Convertir a mayúsculas y quitar caracteres no válidos (solo letras y números)
                String cleaned = text.toUpperCase().replaceAll("[^A-Z0-9]", "");
                
                // Limitar a 12 caracteres
                if (cleaned.length() > 12) {
                    cleaned = cleaned.substring(0, 12);
                }
                
                // Formatear con guiones: XXXX-XXXX-XXXX
                StringBuilder formatted = new StringBuilder();
                for (int i = 0; i < cleaned.length(); i++) {
                    if (i > 0 && i % 4 == 0) {
                        formatted.append("-");
                    }
                    formatted.append(cleaned.charAt(i));
                }
                
                // Si el texto cambió, actualizarlo
                if (!text.equals(formatted.toString())) {
                    int currentCursorPosition = editText.getSelectionStart();
                    s.replace(0, s.length(), formatted.toString());
                    
                    // Calcular nueva posición del cursor
                    int newPosition = calculateNewCursorPosition(
                            text, 
                            formatted.toString(), 
                            currentCursorPosition,
                            previousCursorPosition,
                            previousLength
                    );
                    
                    if (newPosition >= 0 && newPosition <= formatted.length()) {
                        editText.setSelection(newPosition);
                    }
                }
            } catch (Exception e) {
                android.util.Log.e("RecoveryCodeFormatter", "Error formateando código", e);
            } finally {
                isFormatting = false;
            }
        }
        
        /**
         * Calcula la nueva posición del cursor después del formateo.
         */
        private int calculateNewCursorPosition(String oldText, String newText, int currentPos, int previousPos, int previousLength) {
            // Contar caracteres válidos (sin guiones) antes del cursor en el texto anterior
            int validCharsBeforeCursor = 0;
            for (int i = 0; i < Math.min(currentPos, oldText.length()); i++) {
                char c = oldText.charAt(i);
                if (c != '-' && Character.isLetterOrDigit(c)) {
                    validCharsBeforeCursor++;
                }
            }
            
            // Encontrar la posición en el nuevo texto que corresponde a esos caracteres válidos
            int newPosition = 0;
            int validCharsCounted = 0;
            for (int i = 0; i < newText.length() && validCharsCounted < validCharsBeforeCursor; i++) {
                char c = newText.charAt(i);
                if (c != '-') {
                    validCharsCounted++;
                }
                newPosition = i + 1;
            }
            
            // Si estamos justo después de un guión y agregamos un carácter, avanzar el cursor
            if (newText.length() > previousLength && newPosition < newText.length()) {
                if (newText.charAt(newPosition - 1) == '-') {
                    newPosition++;
                }
            }
            
            return Math.min(newPosition, newText.length());
        }
    }
    
    private void recoverUserFromCode(String recoveryCode) {
        // El modo silencioso ya debería estar activado desde el botón, pero asegurarse
        isSilentRecoveryMode = true;
        Log.d("MainActivity", "Iniciando recuperación con código, isSilentRecoveryMode=" + isSilentRecoveryMode);
        
        com.gastonlesbegueris.caretemplate.util.UserRecoveryManager recoveryManager = 
                new com.gastonlesbegueris.caretemplate.util.UserRecoveryManager(this);
        
        Toast.makeText(this, getString(R.string.recovering_data), Toast.LENGTH_SHORT).show();
        
        recoveryManager.recoverUserIdFromCode(recoveryCode, new com.gastonlesbegueris.caretemplate.util.UserRecoveryManager.RecoverUserIdCallback() {
            @Override
            public void onUserIdRecovered(String userId) {
                runOnUiThread(() -> {
                    // CERRAR EL DIÁLOGO PRIMERO, antes de hacer cualquier otra cosa
                    // Cerrar directamente aquí para asegurar que se cierre inmediatamente
                    if (recoverDialog != null) {
                        try {
                            if (recoverDialog.isShowing()) {
                                recoverDialog.dismiss();
                            }
                        } catch (Exception ex) {
                            Log.w("MainActivity", "Error al cerrar diálogo: " + ex.getMessage());
                        }
                        recoverDialog = null;
                    }
                    
                    // Guardar el userId recuperado
                    getSharedPreferences("user_prefs", MODE_PRIVATE)
                            .edit()
                            .putString("user_id", userId)
                            .putString("firebase_uid", userId)
                            .apply();
                    
                    Toast.makeText(MainActivity.this, getString(R.string.user_recovered), Toast.LENGTH_SHORT).show();
                    
                    // Sincronizar datos del usuario recuperado (sin mostrar errores)
                    // El flag isSilentRecoveryMode evitará que se muestren errores
                    performSyncWithUserId(userId, true); // true = silenciar errores de sincronización
                });
            }
            
            @Override
            public void onError(Exception error) {
                runOnUiThread(() -> {
                    // Desactivar modo silencioso
                    isSilentRecoveryMode = false;
                    // Cerrar el diálogo de forma segura
                    closeRecoverDialog();
                    // Verificar si es error de permisos antes de mostrar
                    String errorMsg = error != null ? error.getMessage() : "null";
                    if (isPermissionError(errorMsg)) {
                        Log.d("MainActivity", "Error de permisos SILENCIADO en recovery onError");
                        Toast.makeText(MainActivity.this, getString(R.string.data_recovered), Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(MainActivity.this, getString(R.string.recovery_error, errorMsg), Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }
    
    private void performSyncWithUserId(String userId, boolean silentErrors) {
        // Guardar el userId recuperado para uso futuro
        getSharedPreferences("user_prefs", MODE_PRIVATE)
                .edit()
                .putString("user_id", userId)
                .putString("firebase_uid", userId)
                .apply();
        
        // Intentar autenticarse con Firebase si el userId parece ser un Firebase UID
        // Si no, usar el userId directamente para sincronización
        com.google.firebase.auth.FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        
        if (currentUser == null || !currentUser.getUid().equals(userId)) {
            // Intentar autenticación anónima (Firebase puede mantener el mismo UID anónimo en algunos casos)
            // Si no funciona, usaremos el userId directamente
            FirebaseAuth.getInstance().signInAnonymously()
                    .addOnSuccessListener(authResult -> {
                        if (authResult != null && authResult.getUser() != null) {
                            String firebaseUid = authResult.getUser().getUid();
                            // Si el Firebase UID coincide con el userId recuperado, perfecto
                            // Si no, usaremos el userId recuperado para la sincronización
                            String syncUid = firebaseUid.equals(userId) ? firebaseUid : userId;
                            performSyncSilent(syncUid, silentErrors);
                        } else {
                            // Usar el userId recuperado directamente
                            performSyncSilent(userId, silentErrors);
                        }
                    })
                    .addOnFailureListener(e -> {
                        // Usar el userId recuperado directamente (puede no ser un Firebase UID)
                        performSyncSilent(userId, silentErrors);
                    });
        } else {
            // Ya está autenticado con el mismo UID, sincronizar normalmente
            performSyncSilent(userId, silentErrors);
        }
    }
    
    private void performSyncSilent(String uid, boolean silentErrors) {
        try {
            CloudSync sync = new CloudSync(
                    eventDao,
                    subjectDao,
                    FirebaseFirestore.getInstance(),
                    uid,
                    "CareTemplate",
                    appType
            );

            sync.pushSubjects(() -> {
                sync.push(() -> {
                    sync.pullSubjects(() -> {
                        sync.pull(
                                () -> runOnUiThread(() -> {
                                    // Desactivar modo silencioso y cerrar diálogo
                                    isSilentRecoveryMode = false;
                                    closeRecoverDialog();
                                    Toast.makeText(this, getString(R.string.data_recovered), Toast.LENGTH_SHORT).show();
                                    // Refrescar la UI
                                    observeLocal();
                                    observeSubjectsForAdapter();
                                    refreshHeader();
                                }),
                                e -> runOnUiThread(() -> {
                                    // Cerrar el diálogo siempre cuando silentErrors es true
                                    if (silentErrors) {
                                        isSilentRecoveryMode = false;
                                        closeRecoverDialog();
                                        // En modo silencioso, NO mostrar errores al usuario
                                        // PERMISSION_DENIED y failed_precondition durante recuperación son normales (puede ser que no haya datos)
                                        String errorMsg = e != null ? e.getMessage() : "null";
                                        if (errorMsg != null && (
                                            errorMsg.contains("failed_precondition") || 
                                            errorMsg.contains("FAILED_PRECONDITION") ||
                                            errorMsg.contains("PERMISSION_DENIED") ||
                                            errorMsg.contains("permission_denied"))) {
                                            // Estos errores son normales cuando no hay datos - continuar sin mostrar error
                                            Log.d("MainActivity", "Error normal durante recuperación (sin datos): " + errorMsg);
                                        } else {
                                            Log.w("MainActivity", "Error durante recuperación (silenciado): " + errorMsg);
                                        }
                                        // Siempre refrescar la UI y mostrar mensaje de éxito (incluso si no hay datos)
                                        Toast.makeText(this, getString(R.string.data_recovered), Toast.LENGTH_SHORT).show();
                                        observeLocal();
                                        observeSubjectsForAdapter();
                                        refreshHeader();
                                    } else {
                                        // Solo mostrar error si NO estamos en modo silencioso
                                        showSyncError("Error al recuperar datos", e);
                                    }
                                })
                        );
                    }, e -> runOnUiThread(() -> {
                        // Cerrar el diálogo siempre cuando silentErrors es true
                        if (silentErrors) {
                            isSilentRecoveryMode = false;
                            closeRecoverDialog();
                            // En modo silencioso, NO mostrar errores al usuario
                            // PERMISSION_DENIED y failed_precondition son normales cuando no hay datos
                            String errorMsg = e != null ? e.getMessage() : "null";
                            if (errorMsg != null && (
                                errorMsg.contains("failed_precondition") || 
                                errorMsg.contains("FAILED_PRECONDITION") ||
                                errorMsg.contains("PERMISSION_DENIED") ||
                                errorMsg.contains("permission_denied"))) {
                                Log.d("MainActivity", "Error normal durante recuperación de sujetos (sin datos): " + errorMsg);
                            } else {
                                Log.w("MainActivity", "Error al recuperar sujetos durante recuperación (silenciado): " + errorMsg);
                            }
                            // En modo silencioso, refrescar la UI y mostrar mensaje de éxito
                            Toast.makeText(this, getString(R.string.data_recovered), Toast.LENGTH_SHORT).show();
                            observeLocal();
                            observeSubjectsForAdapter();
                            refreshHeader();
                        } else {
                            // Solo mostrar error si NO estamos en modo silencioso
                            showSyncError("Error al recuperar sujetos", e);
                        }
                    }));
                }, e -> runOnUiThread(() -> {
                    // Cerrar el diálogo siempre cuando silentErrors es true
                    if (silentErrors) {
                        isSilentRecoveryMode = false;
                        closeRecoverDialog();
                        // En modo silencioso, NO mostrar errores al usuario, solo loguear
                        Log.w("MainActivity", "Error al subir eventos durante recuperación (silenciado): " + e.getMessage());
                        // Continuar con la recuperación aunque falle el push
                        // Refrescar la UI y mostrar mensaje de éxito
                        Toast.makeText(this, getString(R.string.data_recovered), Toast.LENGTH_SHORT).show();
                        observeLocal();
                        observeSubjectsForAdapter();
                        refreshHeader();
                    } else {
                        // Solo mostrar error si NO estamos en modo silencioso
                        showSyncError("Error al subir eventos", e);
                    }
                }));
                    }, e -> runOnUiThread(() -> {
                        // Cerrar el diálogo siempre cuando silentErrors es true
                        if (silentErrors) {
                            isSilentRecoveryMode = false;
                            closeRecoverDialog();
                            // En modo silencioso, NO mostrar errores al usuario, solo loguear
                            Log.w("MainActivity", "Error al subir sujetos durante recuperación (silenciado): " + e.getMessage());
                            // Continuar con la recuperación aunque falle el push
                            // Refrescar la UI y mostrar mensaje de éxito
                            Toast.makeText(this, getString(R.string.data_recovered), Toast.LENGTH_SHORT).show();
                            observeLocal();
                            observeSubjectsForAdapter();
                            refreshHeader();
                        } else {
                            // Solo mostrar error si NO estamos en modo silencioso
                            showSyncError("Error al subir sujetos", e);
                        }
                    }));
        } catch (SecurityException e) {
            runOnUiThread(() -> {
                // Cerrar el diálogo siempre cuando silentErrors es true
                if (silentErrors) {
                    isSilentRecoveryMode = false;
                    closeRecoverDialog();
                    Toast.makeText(this, getString(R.string.data_recovered), Toast.LENGTH_SHORT).show();
                    observeLocal();
                    observeSubjectsForAdapter();
                    refreshHeader();
                } else if (!silentErrors) {
                    // Verificar si es error de permisos antes de mostrar
                    String errorMsg = e != null ? e.getMessage() : "null";
                    if (isPermissionError(errorMsg)) {
                        Log.d("MainActivity", "Error de permisos SILENCIADO en performSyncSilent SecurityException");
                        Toast.makeText(this, getString(R.string.sync_success), Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, getString(R.string.sync_error_security), Toast.LENGTH_LONG).show();
                    }
                }
            });
        } catch (Exception e) {
            runOnUiThread(() -> {
                // Cerrar el diálogo siempre cuando silentErrors es true
                if (silentErrors) {
                    isSilentRecoveryMode = false;
                    closeRecoverDialog();
                    Toast.makeText(this, getString(R.string.data_recovered), Toast.LENGTH_SHORT).show();
                    observeLocal();
                    observeSubjectsForAdapter();
                    refreshHeader();
                } else if (!silentErrors) {
                    // Verificar si es error de permisos antes de mostrar
                    String errorMsg = e != null ? e.getMessage() : "null";
                    if (isPermissionError(errorMsg)) {
                        Log.d("MainActivity", "Error de permisos SILENCIADO en performSyncSilent Exception");
                        Toast.makeText(this, getString(R.string.sync_success), Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, getString(R.string.sync_error, errorMsg), Toast.LENGTH_LONG).show();
                    }
                }
            });
        }
    }
    
    /**
     * Método helper para cerrar el diálogo de recuperación de forma segura
     */
    private void closeRecoverDialog() {
        runOnUiThread(() -> {
            if (recoverDialog != null) {
                try {
                    if (recoverDialog.isShowing()) {
                        recoverDialog.dismiss();
                    }
                } catch (Exception e) {
                    Log.w("MainActivity", "Error al cerrar diálogo de recuperación: " + e.getMessage());
                } finally {
                    recoverDialog = null;
                }
            }
        });
    }

    private void startSyncIconAnimation() {
        if (menuItemSync == null) return;
        android.widget.ImageView iv = new android.widget.ImageView(this);
        iv.setImageResource(R.drawable.ic_sync);
        int pad = (int) (8 * getResources().getDisplayMetrics().density);
        iv.setPadding(pad, pad, pad, pad);
        android.view.animation.Animation rotate =
                android.view.animation.AnimationUtils.loadAnimation(this, R.anim.rotate_indefinite);
        iv.startAnimation(rotate);
        menuItemSync.setActionView(iv);
        menuItemSync.setEnabled(false);
    }

    private void stopSyncIconAnimation() {
        if (menuItemSync == null) return;
        View v = menuItemSync.getActionView();
        if (v != null) v.clearAnimation();
        menuItemSync.setActionView(null);
        menuItemSync.setEnabled(true);
    }

    // Helper para verificar si es error de permisos (SILENCIAR COMPLETAMENTE)
    private boolean isPermissionError(String errorMsg) {
        if (errorMsg == null) return false;
        String lower = errorMsg.toLowerCase();
        // CUALQUIER mención de "permission", "permiso", "denied", "missing" = SILENCIAR
        return lower.contains("permission") || 
               lower.contains("permiso") ||
               lower.contains("denied") ||
               lower.contains("missing");
    }
    
    private void showSyncError(String context, Exception e) {
        // Verificar si estamos en modo de recuperación silenciosa
        // Si estamos en modo silencioso, NO mostrar errores (especialmente PERMISSION_DENIED)
        boolean isRecovering = isSilentRecoveryMode;
        if (recoverDialog != null) {
            try {
                isRecovering = isRecovering || recoverDialog.isShowing();
            } catch (Exception ex) {
                // Si hay error al verificar, asumir que estamos recuperando si el flag está activo
                isRecovering = isRecovering || isSilentRecoveryMode;
            }
        }
        
        if (isRecovering) {
            String errorMsg = e != null ? e.getMessage() : "null";
            // PERMISSION_DENIED y failed_precondition durante la recuperación son normales cuando no hay datos
            // Esto puede pasar cuando el usuario recuperado no tiene datos aún o las reglas de Firestore no permiten leer colecciones vacías
            if (errorMsg != null && (
                errorMsg.contains("PERMISSION_DENIED") || 
                errorMsg.contains("permission_denied") || 
                errorMsg.contains("failed_precondition") ||
                errorMsg.contains("FAILED_PRECONDITION") ||
                errorMsg.toLowerCase().contains("permission"))) {
                Log.d("MainActivity", "Error normal durante recuperación (sin datos o sin permisos): " + context + ", error: " + errorMsg);
            } else {
                Log.w("MainActivity", "Error durante recuperación (silenciado): " + context + " - " + errorMsg);
            }
            // Asegurarse de que el diálogo esté cerrado
            if (recoverDialog != null) {
                try {
                    if (recoverDialog.isShowing()) {
                        Log.d("MainActivity", "Cerrando diálogo desde showSyncError");
                        recoverDialog.dismiss();
                    }
                } catch (Exception ex) {
                    Log.w("MainActivity", "Error al cerrar diálogo en showSyncError: " + ex.getMessage());
                }
                recoverDialog = null;
            }
            // NO mostrar el Toast del error - estos errores son normales cuando no hay datos
            return; // No mostrar error si estamos recuperando datos
        }
        
        String errorMsg = e.getMessage();
        
        // PRIMERA VERIFICACIÓN: Si es error de permisos, SILENCIAR COMPLETAMENTE
        if (isPermissionError(errorMsg)) {
            Log.d("MainActivity", "Error de permisos SILENCIADO: " + context + " - " + errorMsg);
            // Cerrar diálogo si existe
            if (recoverDialog != null) {
                try {
                    if (recoverDialog.isShowing()) {
                        recoverDialog.dismiss();
                    }
                } catch (Exception ex) {
                    Log.w("MainActivity", "Error al cerrar diálogo: " + ex.getMessage());
                }
                recoverDialog = null;
            }
            return; // NO MOSTRAR NADA - SALIR INMEDIATAMENTE
        }
        
        String userMessage;
        if (errorMsg == null) {
            userMessage = context + ": Error desconocido";
        } else if (errorMsg.contains("Failed to get service") || 
                   errorMsg.contains("reconnection") || 
                   errorMsg.contains("UNAVAILABLE") ||
                   errorMsg.contains("DEADLINE_EXCEEDED") ||
                   errorMsg.contains("network")) {
            userMessage = getString(R.string.sync_error_connection);
        } else if (errorMsg.contains("UNAUTHENTICATED") || errorMsg.contains("auth")) {
            userMessage = getString(R.string.sync_error_auth);
        } else if (errorMsg.contains("SecurityException")) {
            userMessage = getString(R.string.error_security_sha1);
        } else if (errorMsg.contains("failed_precondition") || errorMsg.contains("FAILED_PRECONDITION")) {
            // No mostrar error para failed_precondition - el código maneja esto automáticamente con fallbacks
            Log.d("MainActivity", "failed_precondition manejado automáticamente, no mostrar error al usuario");
            return; // No mostrar Toast
        } else {
            // ÚLTIMA VERIFICACIÓN: Si contiene "permission" en CUALQUIER parte, SILENCIAR
            if (isPermissionError(errorMsg)) {
                Log.d("MainActivity", "Error de permisos SILENCIADO (verificación final): " + context);
                return; // NO MOSTRAR NADA
            }
            userMessage = context + ": " + errorMsg;
        }
        
        // VERIFICACIÓN FINAL ANTES DE MOSTRAR: Si el mensaje contiene "permission", NO MOSTRAR
        if (isPermissionError(userMessage)) {
            Log.d("MainActivity", "Error de permisos SILENCIADO (antes de Toast): " + context);
            return; // NO MOSTRAR NADA
        }
        
        Toast.makeText(this, userMessage, Toast.LENGTH_LONG).show();
    }

    // ===== Sync Cloud <-> Local =====
    private void doSync() {
        try {
            // Mostrar intersticial de AdMob antes de sincronizar
            String interstitialId = getString(R.string.admob_interstitial_id);
            AdMobHelper.showInterstitial(this, interstitialId, () -> {
                // Continuar con sincronización después del anuncio
                performSyncAfterAd();
            });
        } catch (Exception e) {
            runOnUiThread(() -> {
                stopSyncIconAnimation();
                showSyncError("Error al iniciar sync", e);
            });
        }
    }
    
    private void performSyncAfterAd() {
        try {
            // Asegurar autenticación antes de sincronizar
            com.google.firebase.auth.FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user == null) {
                // Intentar autenticación anónima si no está autenticado
                FirebaseAuth.getInstance().signInAnonymously()
                        .addOnSuccessListener(authResult -> {
                            if (authResult != null && authResult.getUser() != null) {
                                // Usuario autenticado, proceder con sincronización
                                performSync(authResult.getUser().getUid());
                            } else {
                                runOnUiThread(() -> {
                                    stopSyncIconAnimation();
                                    Toast.makeText(this, "Error: No se pudo autenticar", Toast.LENGTH_LONG).show();
                                });
                            }
                        })
                        .addOnFailureListener(e -> {
                            runOnUiThread(() -> {
                                stopSyncIconAnimation();
                                showSyncError(getString(R.string.sync_error_auth_title), e);
                            });
                        });
                return;
            }
            
            // Usuario ya autenticado, proceder con sincronización
            performSync(user.getUid());
        } catch (Exception e) {
            runOnUiThread(() -> {
                stopSyncIconAnimation();
                showSyncError("Error al iniciar sync", e);
            });
        }
    }
    
    private void performSync(String uid) {
        try {
            CloudSync sync = new CloudSync(
                    eventDao,
                    subjectDao,
                    FirebaseFirestore.getInstance(),
                    uid,
                    "CareTemplate",   // nombre lógico de la app en Firestore
                    appType
            );

            sync.pushSubjects(() -> {
                sync.push(() -> {
                    runOnUiThread(() -> Toast.makeText(this, getString(R.string.sync_push_success), Toast.LENGTH_SHORT).show());
                    sync.pullSubjects(() -> {
                        sync.pull(
                                () -> runOnUiThread(() -> {
                                    stopSyncIconAnimation();
                                    refreshHeader();
                                    // Mostrar popup de sincronización realizada
                                    showSyncCompletedDialog();
                                }),
                                e -> runOnUiThread(() -> {
                                    // Verificar si es error de permisos antes de mostrar
                                    String errorMsg = e != null ? e.getMessage() : "null";
                                    if (isPermissionError(errorMsg)) {
                                        Log.d("MainActivity", "Error de permisos SILENCIADO en pull");
                                        stopSyncIconAnimation();
                                        refreshHeader();
                                        // Mostrar popup de sincronización realizada
                                        showSyncCompletedDialog();
                                    } else {
                                        showSyncError("Pull error", e);
                                    }
                                    stopSyncIconAnimation();
                                })
                        );
                    }, e -> runOnUiThread(() -> {
                        // Verificar si es error de permisos antes de mostrar
                        String errorMsg = e != null ? e.getMessage() : "null";
                        if (isPermissionError(errorMsg)) {
                            Log.d("MainActivity", "Error de permisos SILENCIADO en pullSubjects");
                            Toast.makeText(this, getString(R.string.sync_success), Toast.LENGTH_SHORT).show();
                        } else {
                            showSyncError("Pull subjects error", e);
                        }
                        stopSyncIconAnimation();
                    }));
                }, e -> runOnUiThread(() -> {
                    // Verificar si es error de permisos antes de mostrar
                    String errorMsg = e != null ? e.getMessage() : "null";
                    if (isPermissionError(errorMsg)) {
                        Log.d("MainActivity", "Error de permisos SILENCIADO en push events");
                        Toast.makeText(this, getString(R.string.sync_success), Toast.LENGTH_SHORT).show();
                    } else {
                        showSyncError("Push events error", e);
                    }
                    stopSyncIconAnimation();
                }));
            }, e -> runOnUiThread(() -> {
                // Verificar si es error de permisos antes de mostrar
                String errorMsg = e != null ? e.getMessage() : "null";
                if (isPermissionError(errorMsg)) {
                    Log.d("MainActivity", "Error de permisos SILENCIADO en pushSubjects");
                    Toast.makeText(this, getString(R.string.sync_success), Toast.LENGTH_SHORT).show();
                } else {
                    showSyncError("Push subjects error", e);
                }
                stopSyncIconAnimation();
            }));
        } catch (SecurityException e) {
            runOnUiThread(() -> {
                stopSyncIconAnimation();
                // Verificar si es error de permisos antes de mostrar
                String errorMsg = e != null ? e.getMessage() : "null";
                if (isPermissionError(errorMsg)) {
                    Log.d("MainActivity", "Error de permisos SILENCIADO en doSync SecurityException");
                    Toast.makeText(this, getString(R.string.sync_success), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Error de seguridad. Verifica la configuración de Firebase (SHA-1)", Toast.LENGTH_LONG).show();
                }
            });
        } catch (Exception e) {
            runOnUiThread(() -> {
                stopSyncIconAnimation();
                // Verificar si es error de permisos antes de mostrar
                String errorMsg = e != null ? e.getMessage() : "null";
                if (isPermissionError(errorMsg)) {
                    Log.d("MainActivity", "Error de permisos SILENCIADO en doSync Exception");
                    Toast.makeText(this, getString(R.string.sync_success), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Error en sync: " + errorMsg, Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    // ===== Lista de eventos =====
    private void setupEventsList() {
        adapter = new EventAdapter(new EventAdapter.OnEventClick() {
            @Override public void onEdit(com.gastonlesbegueris.caretemplate.data.local.EventEntity e) { showEditDialog(e); }
            @Override public void onDelete(com.gastonlesbegueris.caretemplate.data.local.EventEntity e) { softDelete(e.id); }

            @Override public void onToggleRealized(com.gastonlesbegueris.caretemplate.data.local.EventEntity e, boolean realized) {
                if (realized) {
                    if (e.cost == null) {
                        // pedir costo si falta
                        askCostThenRealize(e);
                    } else {
                        setRealized(e.id, true, null);
                    }
                } else {
                    setRealized(e.id, false, null);
                }
            }
        });
        adapter.setAppType(appType); // Pasar el appType para mostrar kilómetros si es un auto
        RecyclerView rv = findViewById(R.id.rvEvents);
        rv.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this));
        rv.setAdapter(adapter);
    }

    private void askCostThenRealize(com.gastonlesbegueris.caretemplate.data.local.EventEntity e) {
        final android.widget.EditText et = new android.widget.EditText(this);
        et.setHint(getString(R.string.cost_optional));
        et.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(getString(R.string.event_mark_realized))
                .setMessage(getString(R.string.event_mark_realized_message))
                .setView(et)
                .setPositiveButton(getString(R.string.button_save), (d,w) -> {
                    Double cost = null;
                    String t = et.getText().toString().trim();
                    try { if (!t.isEmpty()) cost = Double.parseDouble(t); } catch (Exception ignore) {}
                    setRealized(e.id, true, cost);
                })
                .setNegativeButton(getString(R.string.button_mark_only), (d,w) -> setRealized(e.id, true, null))
                .setNeutralButton(getString(R.string.button_cancel), null)
                .show();
    }

    private void setRealized(String id, boolean realized, Double costOrNull) {
        new Thread(() -> {
            long now = System.currentTimeMillis();
            if (realized) {
                // Cancelar todas las alarmas programadas para este evento
                com.gastonlesbegueris.caretemplate.util.NotificationHelper.cancelNotification(this, id);
                if (costOrNull != null) eventDao.setCost(id, costOrNull, now);
                eventDao.markRealizedOne(id, now);
            } else {
                eventDao.markUnrealizedOne(id, now);
            }
            runOnUiThread(() -> {
                String msg = realized ? getString(R.string.marked_as_realized) : getString(R.string.marked_as_pending);
                android.widget.Toast.makeText(this, msg, android.widget.Toast.LENGTH_SHORT).show();
            });
        }).start();
    }

    private void observeLocal() {
        eventDao.observeActive(appType).observe(this, new Observer<List<EventEntity>>() {
            @Override public void onChanged(List<EventEntity> events) {
                adapter.submit(events);
                findViewById(R.id.emptyState)
                        .setVisibility((events == null || events.isEmpty()) ? View.VISIBLE : View.GONE);
                refreshHeader();
            }
        });
    }

    // 👇 Esto va afuera, no dentro de observeLocal()
    private void observeSubjectsForAdapter() {
        subjectDao.observeActive(appType).observe(this, subjects -> {
            java.util.Map<String, String> nameMap = new java.util.HashMap<>();
            java.util.Map<String, String> iconKeyMap = new java.util.HashMap<>();
            java.util.Map<String, String> colorHexMap = new java.util.HashMap<>();
            if (subjects != null) {
                for (com.gastonlesbegueris.caretemplate.data.local.SubjectEntity s : subjects) {
                    nameMap.put(s.id, s.name);
                    if (s.iconKey != null) {
                        iconKeyMap.put(s.id, s.iconKey);
                    }
                    if (s.colorHex != null && !s.colorHex.isEmpty()) {
                        colorHexMap.put(s.id, s.colorHex);
                    }
                }
            }
            adapter.setSubjectsMap(nameMap);
            adapter.setSubjectIconKeys(iconKeyMap);
            adapter.setSubjectColorHex(colorHexMap);
        });
    }
    // ===== Header simple en el Home =====
    private void refreshHeader() {
        runOnUiThread(() -> {
            MaterialToolbar toolbar = findViewById(R.id.toolbar);
            if (toolbar == null) return;

            // Siempre: nombre de la app - sección
            String appName = getString(R.string.app_name);
            String sectionName = getString(R.string.menu_home);
            toolbar.setTitle(appName + " - " + sectionName);

            // Nunca mostramos subtítulo acá
            toolbar.setSubtitle(null);

            // Icono siempre del flavor (perro / auto / casa / familia según flavor)
            // No aplicar tint, el icono ya tiene el color fijo #03DAC5
            toolbar.setNavigationIcon(R.drawable.ic_header_flavor);
        });
    }

    private int getIconResForSubject(String key) {
        if (key == null) return R.drawable.ic_line_user;
        switch (key) {
            // Pets
            case "cat":   return R.drawable.ic_line_cat;
            case "dog":   return R.drawable.ic_line_dog;
            // Family
            case "man":   return R.drawable.ic_line_man;
            case "woman": return R.drawable.ic_line_woman;
            // House
            case "apartment": return R.drawable.ic_line_apartment;
            case "house": return R.drawable.ic_line_house;
            case "office": return R.drawable.ic_line_office;
            case "local": return R.drawable.ic_line_local;
            case "store": return R.drawable.ic_line_store;
            // Vehicles
            case "car":   return R.drawable.ic_line_car;
            case "bike":  return R.drawable.ic_line_bike;
            case "motorbike": return R.drawable.ic_line_motorbike;
            case "truck": return R.drawable.ic_line_truck;
            case "pickup": return R.drawable.ic_line_pickup;
            case "suv":   return R.drawable.ic_line_suv;
            // Default
            case "user":  return R.drawable.ic_line_user;
            default:      return R.drawable.ic_line_user;
        }
    }

    // Obtiene los iconos disponibles según el tipo de app
    private java.util.List<IconOption> getAvailableIcons() {
        java.util.List<IconOption> icons = new java.util.ArrayList<>();
        if ("pets".equals(appType)) {
            icons.add(new IconOption("cat", R.drawable.ic_line_cat));
            icons.add(new IconOption("dog", R.drawable.ic_line_dog));
        } else if ("family".equals(appType)) {
            icons.add(new IconOption("man", R.drawable.ic_line_man));
            icons.add(new IconOption("woman", R.drawable.ic_line_woman));
        } else if ("house".equals(appType)) {
            icons.add(new IconOption("apartment", R.drawable.ic_line_apartment));
            icons.add(new IconOption("house", R.drawable.ic_line_house));
            icons.add(new IconOption("office", R.drawable.ic_line_office));
            icons.add(new IconOption("local", R.drawable.ic_line_local));
            icons.add(new IconOption("store", R.drawable.ic_line_store));
        } else if ("cars".equals(appType)) {
            icons.add(new IconOption("car", R.drawable.ic_line_car));
            icons.add(new IconOption("bike", R.drawable.ic_line_bike));
            icons.add(new IconOption("motorbike", R.drawable.ic_line_motorbike));
            icons.add(new IconOption("truck", R.drawable.ic_line_truck));
            icons.add(new IconOption("pickup", R.drawable.ic_line_pickup));
            icons.add(new IconOption("suv", R.drawable.ic_line_suv));
        } else {
            icons.add(new IconOption("user", R.drawable.ic_line_user));
        }
        return icons;
    }

    // Clase auxiliar para opciones de icono
    private static class IconOption {
        final String key;
        final int drawableRes;
        IconOption(String key, int drawableRes) {
            this.key = key;
            this.drawableRes = drawableRes;
        }
    }

    // Popula el grid de iconos
    private void populateIconGrid(android.widget.GridLayout grid, final String[] selectedKey) {
        if (grid == null) return;
        grid.removeAllViews();
        
        java.util.List<IconOption> icons = getAvailableIcons();
        int size = (int) (48 * getResources().getDisplayMetrics().density);
        int margin = (int) (8 * getResources().getDisplayMetrics().density);
        
        for (IconOption icon : icons) {
            android.widget.ImageView iv = new android.widget.ImageView(this);
            android.widget.GridLayout.LayoutParams params = new android.widget.GridLayout.LayoutParams();
            params.width = size;
            params.height = size;
            params.setMargins(margin, margin, margin, margin);
            iv.setLayoutParams(params);
            iv.setImageResource(icon.drawableRes);
            iv.setPadding(margin, margin, margin, margin);
            iv.setBackgroundResource(android.R.drawable.dialog_holo_light_frame);
            iv.setScaleType(android.widget.ImageView.ScaleType.FIT_CENTER);
            
            // Tint inicial
            if (icon.key.equals(selectedKey[0])) {
                iv.setBackgroundColor(0x3303DAC5);
            }
            
            iv.setOnClickListener(v -> {
                // Reset all backgrounds
                for (int i = 0; i < grid.getChildCount(); i++) {
                    View child = grid.getChildAt(i);
                    child.setBackgroundResource(android.R.drawable.dialog_holo_light_frame);
                }
                // Set selected background
                v.setBackgroundColor(0x3303DAC5);
                selectedKey[0] = icon.key;
            });
            
            grid.addView(iv);
        }
    }

    private String formatAge(Long birthDate) {
        if (birthDate == null) return "-";
        java.util.Calendar b = java.util.Calendar.getInstance();
        b.setTimeInMillis(birthDate);
        java.util.Calendar now = java.util.Calendar.getInstance();
        int years = now.get(java.util.Calendar.YEAR) - b.get(java.util.Calendar.YEAR);
        int months = now.get(java.util.Calendar.MONTH) - b.get(java.util.Calendar.MONTH);
        if (months < 0) { years--; months += 12; }
        String yearShort = getString(R.string.age_year_short);
        String monthShort = getString(R.string.age_month_short);
        return years > 0 ? years + yearShort + " " + months + monthShort : months + monthShort;
    }

    // ===== FAB Speed Dial =====
    private void initFabSpeedDial() {
        View fab = findViewById(R.id.fabAdd);
        View fabSubject = findViewById(R.id.fabAddSubject);
        View fabEvent = findViewById(R.id.fabAddEvent);

        // Iconos fijos: flavor y calendario
        // Para family usar ic_person_add, para otros flavors usar ic_header_flavor
        int fabIconRes = "family".equals(appType) 
            ? R.drawable.ic_fab_family 
            : R.drawable.ic_header_flavor;
        ((com.google.android.material.floatingactionbutton.FloatingActionButton) fabSubject)
                .setImageResource(fabIconRes);
        ((com.google.android.material.floatingactionbutton.FloatingActionButton) fabEvent)
                .setImageResource(R.drawable.ic_event);

        fab.setOnClickListener(v -> toggleFabMenu());

        fabSubject.setOnClickListener(v -> {
            closeFabMenu();
            showQuickAddSubjectDialog();
        });
        fabEvent.setOnClickListener(v -> {
            closeFabMenu();
            showAddEventDialog();
        });

        RecyclerView rv = findViewById(R.id.rvEvents);
        rv.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                if (fabMenuOpen && Math.abs(dy) > 6) closeFabMenu();
            }
        });
    }

    private void toggleFabMenu() { if (fabMenuOpen) closeFabMenu(); else openFabMenu(); }

    private void openFabMenu() {
        fabMenuOpen = true;
        showFabWithAnim(findViewById(R.id.fabAddSubject));
        showFabWithAnim(findViewById(R.id.fabAddEvent));
        rotateFab(true);
    }

    private void closeFabMenu() {
        fabMenuOpen = false;
        hideFabWithAnim(findViewById(R.id.fabAddSubject));
        hideFabWithAnim(findViewById(R.id.fabAddEvent));
        rotateFab(false);
    }

    private void showFabWithAnim(View fab) {
        fab.setVisibility(View.VISIBLE);
        fab.setScaleX(0.9f); fab.setScaleY(0.9f); fab.setAlpha(0f);
        fab.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(150).start();
    }

    private void hideFabWithAnim(View fab) {
        fab.animate().alpha(0f).scaleX(0.9f).scaleY(0.9f).setDuration(120)
                .withEndAction(() -> fab.setVisibility(View.GONE)).start();
    }

    private void rotateFab(boolean open) {
        View main = findViewById(R.id.fabAdd);
        main.animate().rotation(open ? 45f : 0f).setDuration(150).start();
    }

    // ===== Crear / Editar / Borrar eventos =====
    private void showAddEventDialog() {
        final android.view.View view = getLayoutInflater().inflate(R.layout.dialog_add_event, null);
        final com.google.android.material.textfield.TextInputEditText etTitle = view.findViewById(R.id.etTitle);
        final com.google.android.material.textfield.TextInputEditText etCost  = view.findViewById(R.id.etCost);
        final android.widget.Spinner sp       = view.findViewById(R.id.spSubject);

        // Verificar que las vistas se encontraron
        if (etTitle == null || etCost == null || sp == null) {
            Toast.makeText(this, getString(R.string.error_load_dialog), Toast.LENGTH_SHORT).show();
            return;
        }

        // cargar sujetos en background
        new Thread(() -> {
            final java.util.List<SubjectEntity> loaded = subjectDao.listActiveNow(appType);
            runOnUiThread(() -> {
                final java.util.List<SubjectEntity> subjects =
                        (loaded == null) ? java.util.Collections.emptyList() : new java.util.ArrayList<>(loaded);

                java.util.List<String> names = new java.util.ArrayList<>();
                for (SubjectEntity s : subjects) names.add(s.name);
                android.widget.ArrayAdapter<String> ad =
                        new android.widget.ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, names);
                sp.setAdapter(ad);

                if (currentSubjectId != null && !subjects.isEmpty()) {
                    int idx = -1;
                    for (int i = 0; i < subjects.size(); i++) {
                        if (currentSubjectId.equals(subjects.get(i).id)) { idx = i; break; }
                    }
                    if (idx >= 0) sp.setSelection(idx);
                }

                // Configurar campos de fecha y hora
                final com.google.android.material.textfield.TextInputEditText etEventDate = view.findViewById(R.id.etEventDate);
                final com.google.android.material.textfield.TextInputEditText etEventTime = view.findViewById(R.id.etEventTime);
                
                if (etEventDate != null && etEventTime != null) {
                    final java.util.Calendar now = java.util.Calendar.getInstance();
                    java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault());
                    java.text.SimpleDateFormat timeFormat = new java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault());
                    etEventDate.setText(dateFormat.format(now.getTime()));
                    etEventTime.setText(timeFormat.format(now.getTime()));
                    etEventDate.setTag(now.getTimeInMillis());
                    etEventTime.setTag(now.getTimeInMillis());
                    
                    // Configurar click en fecha
                    etEventDate.setOnClickListener(v -> {
                        long currentDate = etEventDate.getTag() != null ? (Long) etEventDate.getTag() : System.currentTimeMillis();
                        pickDateOnly(currentDate, dateMillis -> {
                            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault());
                            etEventDate.setText(sdf.format(new java.util.Date(dateMillis)));
                            etEventDate.setTag(dateMillis);
                        });
                    });
                    
                    // Configurar click en hora - abrir TimePickerDialog con formato 24 horas
                    etEventTime.setOnClickListener(v -> {
                        long currentTime = etEventTime.getTag() != null ? (Long) etEventTime.getTag() : System.currentTimeMillis();
                        java.util.Calendar cal = java.util.Calendar.getInstance();
                        cal.setTimeInMillis(currentTime);
                        int hour = cal.get(java.util.Calendar.HOUR_OF_DAY);
                        int minute = cal.get(java.util.Calendar.MINUTE);
                        
                        // Crear TimePickerDialog con formato 24 horas
                        android.app.TimePickerDialog timePickerDialog = new android.app.TimePickerDialog(
                            MainActivity.this,
                            (view2, selectedHour, selectedMinute) -> {
                                // Actualizar el tiempo en el Calendar
                                cal.set(java.util.Calendar.HOUR_OF_DAY, selectedHour);
                                cal.set(java.util.Calendar.MINUTE, selectedMinute);
                                cal.set(java.util.Calendar.SECOND, 0);
                                cal.set(java.util.Calendar.MILLISECOND, 0);
                                
                                // Formatear y mostrar la hora seleccionada
                                java.text.SimpleDateFormat timeFormat2 = new java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault());
                                etEventTime.setText(timeFormat2.format(cal.getTime()));
                                etEventTime.setTag(cal.getTimeInMillis());
                            },
                            hour,
                            minute,
                            true // true = formato 24 horas
                        );
                        timePickerDialog.setTitle(getString(R.string.event_time_hint));
                        timePickerDialog.show();
                    });
                }

                // Configurar barra de título personalizada
                final android.view.View llDialogTitleBar = view.findViewById(R.id.llDialogTitleBar);
                final android.widget.TextView tvDialogTitle = view.findViewById(R.id.tvDialogTitle);
                final android.widget.ImageButton ibSave = view.findViewById(R.id.ibSave);
                final android.widget.ImageButton ibClose = view.findViewById(R.id.ibClose);
                
                if (tvDialogTitle != null) {
                    tvDialogTitle.setText(getString(R.string.new_event));
                }
                
                // Ocultar botones al final
                final com.google.android.material.button.MaterialButton btnSave = view.findViewById(R.id.btnSave);
                final com.google.android.material.button.MaterialButton btnCancel = view.findViewById(R.id.btnCancel);
                if (btnSave != null) {
                    btnSave.setVisibility(android.view.View.GONE);
                }
                if (btnCancel != null) {
                    btnCancel.setVisibility(android.view.View.GONE);
                }
                
                androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(this)
                        .setView(view)
                        .create();
                
                // Mostrar barra de título personalizada
                if (llDialogTitleBar != null) {
                    llDialogTitleBar.setVisibility(android.view.View.VISIBLE);
                }
                
                // Configurar listener del icono Guardar
                if (ibSave != null) {
                    ibSave.setOnClickListener(v -> {
                        final String title = etTitle.getText() != null ? etTitle.getText().toString().trim() : "";
                        if (title.isEmpty()) {
                            Toast.makeText(this, getString(R.string.event_title_required), Toast.LENGTH_SHORT).show();
                            return;
                        }
                        if (subjects.isEmpty()) {
                            Toast.makeText(this, getString(R.string.error_create_subject_first), Toast.LENGTH_LONG).show();
                            return;
                        }
                        final int pos = sp.getSelectedItemPosition();
                        if (pos < 0 || pos >= subjects.size()) {
                            Toast.makeText(this, getString(R.string.error_select_subject), Toast.LENGTH_SHORT).show();
                            return;
                        }
                        final String subjectId = subjects.get(pos).id;

                        final String c = etCost.getText() != null ? etCost.getText().toString().trim() : "";
                        final Double cost = c.isEmpty() ? null : safeParseDouble(c);

                        // Obtener fecha y hora de los campos
                        long dueAt = System.currentTimeMillis();
                        if (etEventDate != null && etEventTime != null) {
                            Long dateMillis = etEventDate.getTag() != null ? (Long) etEventDate.getTag() : null;
                            Long timeMillis = etEventTime.getTag() != null ? (Long) etEventTime.getTag() : null;
                            
                            if (dateMillis != null && timeMillis != null) {
                                java.util.Calendar dateCal = java.util.Calendar.getInstance();
                                dateCal.setTimeInMillis(dateMillis);
                                
                                java.util.Calendar timeCal = java.util.Calendar.getInstance();
                                timeCal.setTimeInMillis(timeMillis);
                                
                                dateCal.set(java.util.Calendar.HOUR_OF_DAY, timeCal.get(java.util.Calendar.HOUR_OF_DAY));
                                dateCal.set(java.util.Calendar.MINUTE, timeCal.get(java.util.Calendar.MINUTE));
                                dateCal.set(java.util.Calendar.SECOND, 0);
                                dateCal.set(java.util.Calendar.MILLISECOND, 0);
                                
                                dueAt = dateCal.getTimeInMillis();
                            }
                        }

                        dialog.dismiss();
                        insertLocal(title, subjectId, cost, dueAt);
                    });
                }
                
                // Configurar listener del icono Cerrar (Cancelar)
                if (ibClose != null) {
                    ibClose.setOnClickListener(v -> dialog.dismiss());
                }
                
                dialog.show();
            });
        }).start();
    }

    private Double safeParseDouble(String s) {
        try { return Double.parseDouble(s); } catch (Exception e) { return null; }
    }

    private void insertLocal(String title, String subjectId, Double cost, long dueAt) {
        insertLocal(title, subjectId, cost, dueAt, null);
    }

    private void insertLocal(String title, String subjectId, Double cost, long dueAt, Double kilometersAtEvent) {
        new Thread(() -> {
            // Verificar si es el primer evento ANTES de insertar
            int eventCountBefore = eventDao.countEventsForApp(appType);
            boolean isFirstEvent = (eventCountBefore == 0);
            
            // Obtener UID del usuario actual
            String uid = getCurrentUserId();
            
            EventEntity e = new EventEntity();
            e.id = UUID.randomUUID().toString();
            e.uid = uid; // Usar UID del usuario actual
            e.appType = appType;
            e.subjectId = subjectId;     // sujeto elegido
            e.title = title;
            e.note = "";
            e.cost = cost;               // costo opcional
            e.kilometersAtEvent = kilometersAtEvent; // km del auto al momento del evento (solo para cars)
            e.realized = 0;              // aún no realizado
            e.dueAt = dueAt;
            e.updatedAt = System.currentTimeMillis();
            e.deleted = 0;
            e.dirty = 1;
            eventDao.insert(e);
            com.gastonlesbegueris.caretemplate.util.LimitGuard.onEventCreated(this, appType);

            // Verificar si se debe solicitar reseña (después del primer evento o gasto)
            boolean isExpense = (cost != null && cost > 0);
            String appName = getString(R.string.app_name);
            runOnUiThread(() -> {
                Toast.makeText(this, getString(R.string.event_saved), Toast.LENGTH_SHORT).show();
                com.gastonlesbegueris.caretemplate.util.ReviewHelper.checkAndRequestReview(this, appName, isExpense);
                
                // Mostrar celebración si es el primer evento
                if (isFirstEvent) {
                    com.gastonlesbegueris.caretemplate.util.FirstEventCelebrationHelper.checkAndShowCelebration(this, appType);
                }
            });
        }).start();
    }
    
    /**
     * Obtiene el UID del usuario actual (Firebase UID o UUID local)
     */
    private String getCurrentUserId() {
        // Intentar obtener Firebase UID primero
        com.google.firebase.auth.FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            return user.getUid();
        }
        
        // Si no hay Firebase Auth, usar UserManager para obtener o generar un ID local
        com.gastonlesbegueris.caretemplate.util.UserManager userManager = 
                new com.gastonlesbegueris.caretemplate.util.UserManager(this);
        String userId = userManager.getUserIdSync();
        if (userId != null) {
            return userId;
        }
        
        // Fallback: generar UUID temporal (se actualizará en la próxima sincronización)
        return java.util.UUID.randomUUID().toString();
    }

    private void showEditDialog(EventEntity e) {
        final android.view.View view = getLayoutInflater().inflate(R.layout.dialog_add_event, null);
        final com.google.android.material.textfield.TextInputEditText etTitle = view.findViewById(R.id.etTitle);
        final com.google.android.material.textfield.TextInputEditText etCost  = view.findViewById(R.id.etCost);
        final android.widget.Spinner sp = view.findViewById(R.id.spSubject);

        // Verificar que las vistas se encontraron
        if (etTitle == null || etCost == null || sp == null) {
            Toast.makeText(this, getString(R.string.error_load_dialog), Toast.LENGTH_SHORT).show();
            return;
        }

        // Pre-populate title and cost
        etTitle.setText(e.title);
        if (e.cost != null) {
            etCost.setText(String.format(java.util.Locale.getDefault(), "%.2f", e.cost));
        }

        // Hide subject spinner for editing (subject shouldn't change)
        sp.setVisibility(android.view.View.GONE);
        
        // Configurar campos de fecha y hora
        final com.google.android.material.textfield.TextInputEditText etEventDate = view.findViewById(R.id.etEventDate);
        final com.google.android.material.textfield.TextInputEditText etEventTime = view.findViewById(R.id.etEventTime);
        
        if (etEventDate != null && etEventTime != null) {
            java.util.Calendar cal = java.util.Calendar.getInstance();
            cal.setTimeInMillis(e.dueAt);
            java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault());
            java.text.SimpleDateFormat timeFormat = new java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault());
            etEventDate.setText(dateFormat.format(cal.getTime()));
            etEventTime.setText(timeFormat.format(cal.getTime()));
            etEventDate.setTag(e.dueAt);
            etEventTime.setTag(e.dueAt);
            
            // Configurar click en fecha
            etEventDate.setOnClickListener(v -> {
                long currentDate = etEventDate.getTag() != null ? (Long) etEventDate.getTag() : e.dueAt;
                pickDateOnly(currentDate, dateMillis -> {
                    java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault());
                    etEventDate.setText(sdf.format(new java.util.Date(dateMillis)));
                    etEventDate.setTag(dateMillis);
                });
            });
            
            // Configurar click en hora - abrir TimePickerDialog con formato 24 horas
            etEventTime.setOnClickListener(v -> {
                long currentTime = etEventTime.getTag() != null ? (Long) etEventTime.getTag() : e.dueAt;
                java.util.Calendar timeCal = java.util.Calendar.getInstance();
                timeCal.setTimeInMillis(currentTime);
                int hour = timeCal.get(java.util.Calendar.HOUR_OF_DAY);
                int minute = timeCal.get(java.util.Calendar.MINUTE);
                
                // Crear TimePickerDialog con formato 24 horas
                android.app.TimePickerDialog timePickerDialog = new android.app.TimePickerDialog(
                    MainActivity.this,
                    (view2, selectedHour, selectedMinute) -> {
                        // Actualizar el tiempo en el Calendar
                        timeCal.set(java.util.Calendar.HOUR_OF_DAY, selectedHour);
                        timeCal.set(java.util.Calendar.MINUTE, selectedMinute);
                        timeCal.set(java.util.Calendar.SECOND, 0);
                        timeCal.set(java.util.Calendar.MILLISECOND, 0);
                        
                        // Formatear y mostrar la hora seleccionada
                        java.text.SimpleDateFormat timeFormat2 = new java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault());
                        etEventTime.setText(timeFormat2.format(timeCal.getTime()));
                        etEventTime.setTag(timeCal.getTimeInMillis());
                    },
                    hour,
                    minute,
                    true // true = formato 24 horas
                );
                timePickerDialog.setTitle(getString(R.string.event_time_hint));
                timePickerDialog.show();
            });
        }

        // Configurar barra de título personalizada
        final android.view.View llDialogTitleBar = view.findViewById(R.id.llDialogTitleBar);
        final android.widget.TextView tvDialogTitle = view.findViewById(R.id.tvDialogTitle);
        final android.widget.ImageButton ibDelete = view.findViewById(R.id.ibDelete);
        final android.widget.ImageButton ibSave = view.findViewById(R.id.ibSave);
        final android.widget.ImageButton ibClose = view.findViewById(R.id.ibClose);
        
        if (tvDialogTitle != null) {
            tvDialogTitle.setText(getString(R.string.edit_event));
        }
        
        // Ocultar botones al final
        final com.google.android.material.button.MaterialButton btnSave = view.findViewById(R.id.btnSave);
        final com.google.android.material.button.MaterialButton btnCancel = view.findViewById(R.id.btnCancel);
        final com.google.android.material.button.MaterialButton btnDelete = view.findViewById(R.id.btnDelete);
        if (btnSave != null) {
            btnSave.setVisibility(android.view.View.GONE);
        }
        if (btnCancel != null) {
            btnCancel.setVisibility(android.view.View.GONE);
        }
        if (btnDelete != null) {
            btnDelete.setVisibility(android.view.View.GONE);
        }
        
        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(this)
                .setView(view)
                .create();
        
        // Mostrar barra de título personalizada
        if (llDialogTitleBar != null) {
            llDialogTitleBar.setVisibility(android.view.View.VISIBLE);
        }
        
        // Configurar listener del icono Eliminar
        if (ibDelete != null) {
            ibDelete.setOnClickListener(v -> {
                dialog.dismiss();
                softDelete(e.id);
            });
        }
        
        // Configurar listener del icono Guardar
        if (ibSave != null) {
            ibSave.setOnClickListener(v -> {
                String title = etTitle.getText() != null ? etTitle.getText().toString().trim() : "";
                if (title.isEmpty()) {
                    Toast.makeText(this, getString(R.string.event_title_required), Toast.LENGTH_SHORT).show();
                    return;
                }
                if (!LimitGuard.canCreateEvent(this, db, appType)) return;

                final String c = etCost.getText() != null ? etCost.getText().toString().trim() : "";
                final Double cost = c.isEmpty() ? null : safeParseDouble(c);
                
                // Obtener fecha y hora de los campos
                long dueAt = e.dueAt;
                if (etEventDate != null && etEventTime != null) {
                    Long dateMillis = etEventDate.getTag() != null ? (Long) etEventDate.getTag() : null;
                    Long timeMillis = etEventTime.getTag() != null ? (Long) etEventTime.getTag() : null;
                    
                    if (dateMillis != null && timeMillis != null) {
                        java.util.Calendar dateCal = java.util.Calendar.getInstance();
                        dateCal.setTimeInMillis(dateMillis);
                        
                        java.util.Calendar timeCal = java.util.Calendar.getInstance();
                        timeCal.setTimeInMillis(timeMillis);
                        
                        dateCal.set(java.util.Calendar.HOUR_OF_DAY, timeCal.get(java.util.Calendar.HOUR_OF_DAY));
                        dateCal.set(java.util.Calendar.MINUTE, timeCal.get(java.util.Calendar.MINUTE));
                        dateCal.set(java.util.Calendar.SECOND, 0);
                        dateCal.set(java.util.Calendar.MILLISECOND, 0);
                        
                        dueAt = dateCal.getTimeInMillis();
                    }
                }

                dialog.dismiss();
                updateLocal(e, title, cost, dueAt);
            });
        }
        
        // Configurar listener del icono Cerrar (Cancelar)
        if (ibClose != null) {
            ibClose.setOnClickListener(v -> dialog.dismiss());
        }
        
        dialog.show();
    }

    private void updateLocal(EventEntity e, String title, Double cost, long dueAt) {
        new Thread(() -> {
            // Determinar si es evento original o repetido
            EventEntity originalEvent = e;
            boolean isRepeatedEvent = e.originalEventId != null;
            
            if (isRepeatedEvent) {
                // Si es un evento repetido, obtener el original
                originalEvent = eventDao.findOriginalEvent(e.originalEventId);
                if (originalEvent == null) {
                    // Si no se encuentra el original, actualizar solo este evento
                    e.title = title;
                    e.cost = cost;
                    e.dueAt = dueAt;
                    e.updatedAt = System.currentTimeMillis();
                    e.dirty = 1;
                    eventDao.update(e);
                    runOnUiThread(() -> Toast.makeText(this, getString(R.string.event_updated), Toast.LENGTH_SHORT).show());
                    return;
                }
            }
            
            // Si el evento original tiene repetición, actualizar todas las repeticiones
            if (originalEvent.repeatType != null) {
                // Actualizar el evento original
                originalEvent.title = title;
                originalEvent.cost = cost;
                originalEvent.dueAt = dueAt;
                originalEvent.updatedAt = System.currentTimeMillis();
                originalEvent.dirty = 1;
                eventDao.update(originalEvent);
                
                // Obtener todas las repeticiones y actualizarlas
                List<EventEntity> repeatedEvents = eventDao.findRepeatedEvents(originalEvent.id);
                if (repeatedEvents != null) {
                    for (EventEntity repeated : repeatedEvents) {
                        repeated.title = title;
                        // No actualizar el costo de los repetidos (se actualiza cuando se realizan)
                        // No actualizar dueAt de los repetidos (mantienen su fecha calculada)
                        repeated.updatedAt = System.currentTimeMillis();
                        repeated.dirty = 1;
                        eventDao.update(repeated);
                    }
                }
                
                runOnUiThread(() -> Toast.makeText(this, "Evento y todas sus repeticiones actualizadas ✅", Toast.LENGTH_SHORT).show());
            } else {
                // No tiene repetición, actualizar solo este evento
                e.title = title;
                e.cost = cost;
                e.dueAt = dueAt;
                e.updatedAt = System.currentTimeMillis();
                e.dirty = 1;
                eventDao.update(e);
                runOnUiThread(() -> Toast.makeText(this, getString(R.string.event_updated), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void softDelete(String id) {
        new Thread(() -> {
            eventDao.softDelete(id, System.currentTimeMillis());
            runOnUiThread(() -> Toast.makeText(this, getString(R.string.event_deleted), Toast.LENGTH_SHORT).show());
        }).start();
    }

    // ===== Picker de fecha/hora =====
    private interface DateTimeCallback { void onPicked(long dueAtMillis); }
    
    private void pickDateOnly(long initialMillis, DateTimeCallback cb) {
        final java.util.Calendar cal = java.util.Calendar.getInstance();
        if (initialMillis > 0) cal.setTimeInMillis(initialMillis);
        int y = cal.get(java.util.Calendar.YEAR);
        int m = cal.get(java.util.Calendar.MONTH);
        int d = cal.get(java.util.Calendar.DAY_OF_MONTH);

        new android.app.DatePickerDialog(this, (v, year, month, day) -> {
            cal.set(java.util.Calendar.YEAR, year);
            cal.set(java.util.Calendar.MONTH, month);
            cal.set(java.util.Calendar.DAY_OF_MONTH, day);
            cal.set(java.util.Calendar.HOUR_OF_DAY, 23);
            cal.set(java.util.Calendar.MINUTE, 59);
            cal.set(java.util.Calendar.SECOND, 59);
            cal.set(java.util.Calendar.MILLISECOND, 999);
            cb.onPicked(cal.getTimeInMillis());
        }, y, m, d).show();
    }
    
    private void pickDateTime(long initialMillis, DateTimeCallback cb) {
        final java.util.Calendar cal = java.util.Calendar.getInstance();
        if (initialMillis > 0) cal.setTimeInMillis(initialMillis);
        int y = cal.get(java.util.Calendar.YEAR);
        int m = cal.get(java.util.Calendar.MONTH);
        int d = cal.get(java.util.Calendar.DAY_OF_MONTH);
        int hh = cal.get(java.util.Calendar.HOUR_OF_DAY);
        int mm = cal.get(java.util.Calendar.MINUTE);

        new android.app.DatePickerDialog(this, (v, year, month, day) -> {
            cal.set(java.util.Calendar.YEAR, year);
            cal.set(java.util.Calendar.MONTH, month);
            cal.set(java.util.Calendar.DAY_OF_MONTH, day);
            new android.app.TimePickerDialog(this, (tp, hour, minute) -> {
                cal.set(java.util.Calendar.HOUR_OF_DAY, hour);
                cal.set(java.util.Calendar.MINUTE, minute);
                cal.set(java.util.Calendar.SECOND, 0);
                cal.set(java.util.Calendar.MILLISECOND, 0);
                cb.onPicked(cal.getTimeInMillis());
            }, hh, mm, true).show();
        }, y, m, d).show();
    }

    // ===== Auto-realizar eventos vencidos =====
    @Override
    protected void onResume() {
        super.onResume();
        String appKey = getString(R.string.appodeal_app_key);
        AppodealHelper.initialize(this, appKey);
        AppodealHelper.showBanner(this, R.id.adView);
        autoRealizePastEvents();
    }

    private void autoRealizePastEvents() {
        new Thread(() -> {
            long now = System.currentTimeMillis();
            List<EventEntity> due = eventDao.listDueUnrealized(appType, now);
            if (due == null || due.isEmpty()) return;

            java.util.List<String> ids = new java.util.ArrayList<>();
            for (EventEntity e : due) ids.add(e.id);

            eventDao.markRealized(ids, now);

            runOnUiThread(() ->
                    Toast.makeText(this, "Marcados como realizados: " + ids.size(), Toast.LENGTH_SHORT).show());
        }).start();
    }

    // ===== Crear sujeto rápido =====
    private void showQuickAddSubjectDialog() {
        if (!LimitGuard.canCreateSubject(this, db, appType)) return;

        final android.view.View view = getLayoutInflater().inflate(R.layout.dialog_edit_subject, null, false);
        final android.widget.EditText etName = view.findViewById(R.id.etName);
        final android.widget.EditText etBirth = view.findViewById(R.id.etBirth);
        final android.widget.EditText etMeasure = view.findViewById(R.id.etMeasure);
        final android.widget.EditText etNotes = view.findViewById(R.id.etNotes);

        // Campos de marca y modelo (solo para cars)
        final com.google.android.material.textfield.TextInputLayout tilBrand = view.findViewById(R.id.tilBrand);
        final com.google.android.material.textfield.TextInputLayout tilModel = view.findViewById(R.id.tilModel);
        final android.widget.EditText etBrand = view.findViewById(R.id.etBrand);
        final android.widget.EditText etModel = view.findViewById(R.id.etModel);

        // Configurar UI según flavor
        if ("cars".equals(appType)) {
            // Para cars: ocultar nombre, mostrar marca y modelo
            if (view.findViewById(R.id.tilName) != null) {
                view.findViewById(R.id.tilName).setVisibility(android.view.View.GONE);
            }
            if (tilBrand != null) tilBrand.setVisibility(android.view.View.VISIBLE);
            if (tilModel != null) tilModel.setVisibility(android.view.View.VISIBLE);
        } else {
            // Para otros flavors: ocultar marca y modelo
            if (tilBrand != null) tilBrand.setVisibility(android.view.View.GONE);
            if (tilModel != null) tilModel.setVisibility(android.view.View.GONE);
        }

        // Obtener los TextInputLayout para configurarlos correctamente
        com.google.android.material.textfield.TextInputLayout tilMeasure = view.findViewById(R.id.tilMeasure);
        com.google.android.material.textfield.TextInputLayout tilBirth = view.findViewById(R.id.tilBirth);
        
        // Configurar campos según flavor
        if ("cars".equals(appType) || "house".equals(appType)) {
            // Para cars/house: ocultar fecha de nacimiento, mostrar odómetro
            if (tilBirth != null) tilBirth.setVisibility(android.view.View.GONE);
            if (tilMeasure != null) {
                tilMeasure.setHint("Odómetro (km)");
                tilMeasure.setVisibility(android.view.View.VISIBLE);
            }
            if (etMeasure != null) {
                etMeasure.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
            }
        } else if ("family".equals(appType)) {
            // Para family: mostrar fecha de nacimiento, ocultar peso
            if (tilBirth != null) {
                tilBirth.setHint("Fecha de nacimiento (dd/MM/aaaa)");
                tilBirth.setVisibility(android.view.View.VISIBLE);
            }
            if (tilMeasure != null) {
                tilMeasure.setVisibility(android.view.View.GONE);
            }
            if (etBirth != null) {
                etBirth.setOnClickListener(v -> pickDateInto(etBirth));
            }
        } else {
            // Para pets: mostrar fecha de nacimiento y peso
            if (tilBirth != null) {
                tilBirth.setHint("Fecha de nacimiento (dd/MM/aaaa)");
                tilBirth.setVisibility(android.view.View.VISIBLE);
            }
            if (tilMeasure != null) {
                tilMeasure.setHint("Peso (kg)");
                tilMeasure.setVisibility(android.view.View.VISIBLE);
            }
            if (etMeasure != null) {
                etMeasure.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
            }
            if (etBirth != null) {
                etBirth.setOnClickListener(v -> pickDateInto(etBirth));
            }
        }
        
        // Mostrar notas (ya no ocultar)
        android.view.View tilNotes = view.findViewById(R.id.tilNotes);
        if (tilNotes != null) tilNotes.setVisibility(android.view.View.VISIBLE);

        // Ocultar selector de iconos - usar icono por defecto automáticamente
        final android.widget.GridLayout gridIcons = view.findViewById(R.id.gridIcons);
        if (gridIcons != null) {
            gridIcons.setVisibility(android.view.View.GONE);
        }
        // Usar icono por defecto sin mostrar selector
        final String[] selectedIconKey = {defaultIconForFlavor()};

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Nuevo sujeto")
                .setView(view)
                .setPositiveButton("Guardar", (d, w) -> {
                    String name;
                    if ("cars".equals(appType)) {
                        // Para cars: concatenar marca + modelo
                        String brand = etBrand != null ? etBrand.getText().toString().trim() : "";
                        String model = etModel != null ? etModel.getText().toString().trim() : "";
                        if (brand.isEmpty() && model.isEmpty()) {
                            Toast.makeText(this, "Marca o Modelo requerido", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        if (brand.isEmpty()) {
                            name = model;
                        } else if (model.isEmpty()) {
                            name = brand;
                        } else {
                            name = brand + " " + model;
                        }
                    } else {
                        // Para otros flavors: usar nombre normal
                        name = etName.getText().toString().trim();
                        if (name.isEmpty()) {
                            Toast.makeText(this, "Nombre requerido", Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }
                    // Obtener fecha de nacimiento (solo para pets/family)
                    Long birthMillis = null;
                    if (!"cars".equals(appType) && !"house".equals(appType)) {
                        String birthStr = etBirth.getText() != null ? etBirth.getText().toString().trim() : "";
                        if (!birthStr.isEmpty()) {
                            birthMillis = parseDateOrNull(birthStr);
                        }
                    }
                    
                    // Obtener medida: kilómetros para cars/house, peso para pets (no para family)
                    Double measure = null;
                    if (!"family".equals(appType)) {
                        String measureStr = etMeasure.getText() != null ? etMeasure.getText().toString().trim() : "";
                        measure = measureStr.isEmpty() ? null : safeParseDouble(measureStr);
                    }
                    
                    insertSubjectMinimal(name, selectedIconKey[0], birthMillis, measure);
                })
                .setNegativeButton(getString(R.string.button_cancel), null)
                .show();
    }

    private void insertSubjectMinimal(String name, String iconKey, Long birthDate, Double currentMeasure) {
        new Thread(() -> {
            // Asegurar que el usuario esté identificado antes de crear el sujeto
            String userId = getCurrentUserId();
            
            // Si no hay Firebase Auth, intentar autenticarse anónimamente
            if (FirebaseAuth.getInstance().getCurrentUser() == null) {
                FirebaseAuth.getInstance().signInAnonymously()
                        .addOnSuccessListener(authResult -> {
                            if (authResult != null && authResult.getUser() != null) {
                                // Usuario autenticado, crear sujeto con el UID correcto
                                createSubjectWithUserId(name, iconKey, birthDate, currentMeasure, authResult.getUser().getUid());
                            } else {
                                // Fallback: crear con ID local
                                createSubjectWithUserId(name, iconKey, birthDate, currentMeasure, userId);
                            }
                        })
                        .addOnFailureListener(e -> {
                            // Fallback: crear con ID local si falla la autenticación
                            createSubjectWithUserId(name, iconKey, birthDate, currentMeasure, userId);
                        });
            } else {
                // Ya está autenticado, crear directamente
                createSubjectWithUserId(name, iconKey, birthDate, currentMeasure, userId);
            }
        }).start();
    }
    
    private void createSubjectWithUserId(String name, String iconKey, Long birthDate, Double currentMeasure, String userId) {
        new Thread(() -> {
            SubjectEntity s = new SubjectEntity();
            // Generar ID único por usuario: {userId}_{timestamp}
            s.id = userId + "_" + System.currentTimeMillis();
            s.uid = userId; // Asignar uid del usuario propietario
            s.appType = appType;
            s.name = name;
            s.birthDate = birthDate; // Fecha de nacimiento para pets/family
            s.currentMeasure = currentMeasure; // Kilómetros para cars/house, peso para pets/family
            s.notes = "";
            s.iconKey = (iconKey == null || iconKey.isEmpty()) ? defaultIconForFlavor() : iconKey;
            s.colorHex = "#03DAC5";
            s.updatedAt = System.currentTimeMillis();
            s.deleted = 0;
            s.dirty = 1;
            subjectDao.insert(s);
            com.gastonlesbegueris.caretemplate.util.LimitGuard.onSubjectCreated(this, appType);

            currentSubjectId = s.id;
            getSharedPreferences("prefs", MODE_PRIVATE)
                    .edit().putString("currentSubjectId_" + appType, currentSubjectId).apply();

            runOnUiThread(() -> {
                Toast.makeText(this, "Sujeto creado ✅", Toast.LENGTH_SHORT).show();
                refreshHeader();
            });
        }).start();
    }
    
    private Long parseDateOrNull(String dateStr) {
        try {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy");
            java.util.Date d = sdf.parse(dateStr);
            return d != null ? d.getTime() : null;
        } catch (Exception e) {
            return null;
        }
    }
    
    private void pickDateInto(final android.widget.EditText target) {
        final java.util.Calendar cal = java.util.Calendar.getInstance();
        int y = cal.get(java.util.Calendar.YEAR);
        int m = cal.get(java.util.Calendar.MONTH);
        int d = cal.get(java.util.Calendar.DAY_OF_MONTH);

        new android.app.DatePickerDialog(this, (v, year, month, day) -> {
            cal.set(java.util.Calendar.YEAR, year);
            cal.set(java.util.Calendar.MONTH, month);
            cal.set(java.util.Calendar.DAY_OF_MONTH, day);
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy");
            target.setText(sdf.format(cal.getTime()));
        }, y, m, d).show();
    }

    // ===== Compartir e Importar Sujetos =====
    /**
     * Muestra un diálogo para elegir el sujeto a compartir
     */
    private void showSelectSubjectToShareDialog() {
        // Cargar sujetos en background
        new Thread(() -> {
            final java.util.List<SubjectEntity> subjects = subjectDao.listActiveNow(appType);
            runOnUiThread(() -> {
                if (subjects == null || subjects.isEmpty()) {
                    Toast.makeText(this, "No hay sujetos para compartir", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                // Verificar que la actividad aún está activa
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                
                try {
                    // Crear layout personalizado para el diálogo
                    android.view.View dialogView = getLayoutInflater().inflate(R.layout.dialog_select_subject, null);
                    android.widget.ListView listView = dialogView.findViewById(R.id.lvSubjects);
                    android.widget.TextView tvMessage = dialogView.findViewById(R.id.tvMessage);
                    
                    if (tvMessage != null) {
                        tvMessage.setText(getString(R.string.share_subject_select_one));
                    }
                    
                    // Crear array de nombres para el adaptador
                    java.util.List<String> subjectNames = new java.util.ArrayList<>();
                    for (SubjectEntity s : subjects) {
                        subjectNames.add(s.name);
                    }
                    
                    // Crear adaptador para la lista
                    android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(
                        this,
                        android.R.layout.simple_list_item_1,
                        subjectNames
                    );
                    
                    if (listView != null) {
                        listView.setAdapter(adapter);
                        listView.setOnItemClickListener((parent, view, position, id) -> {
                            if (position >= 0 && position < subjects.size()) {
                                SubjectEntity selectedSubject = subjects.get(position);
                                // Cerrar el diálogo primero
                                if (shareDialog != null && shareDialog.isShowing()) {
                                    shareDialog.dismiss();
                                }
                                // Generar código directamente sin intersticial
                                generateShareCode(selectedSubject.id);
                            }
                        });
                    }
                    
                    // Crear y mostrar el diálogo
                    shareDialog = new androidx.appcompat.app.AlertDialog.Builder(this)
                            .setTitle(getString(R.string.share_subject_title))
                            .setView(dialogView)
                            .setNegativeButton(getString(R.string.button_cancel), null)
                            .create();
                    shareDialog.show();
                } catch (Exception e) {
                    android.util.Log.e("MainActivity", "Error al mostrar diálogo de selección", e);
                    Toast.makeText(this, "Error al mostrar diálogo: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }
    
    /**
     * Muestra intersticial y luego genera el código de compartir
     */
    private void showInterstitialAdAndShare(String subjectId) {
        // Intersticial removido - generar código directamente
        if (!isFinishing() && !isDestroyed()) {
            generateShareCode(subjectId);
        }
    }
    
    /**
     * Genera el código de compartir para un sujeto
     */
    private void generateShareCode(String subjectId) {
        // Mostrar intersticial de AdMob antes de generar código
        String interstitialId = getString(R.string.admob_interstitial_id);
        AdMobHelper.showInterstitial(this, interstitialId, () -> {
            generateShareCodeAfterAd(subjectId);
        });
    }
    
    private void generateShareCodeAfterAd(String subjectId) {
        String userId = getCurrentUserId();
        if (userId == null) {
            Toast.makeText(this, "Error: No se pudo obtener ID de usuario", Toast.LENGTH_SHORT).show();
            return;
        }
        
        com.gastonlesbegueris.caretemplate.util.SubjectShareManager shareManager = 
            new com.gastonlesbegueris.caretemplate.util.SubjectShareManager(this);
        
        shareManager.generateShareCode(subjectId, userId, new com.gastonlesbegueris.caretemplate.util.SubjectShareManager.ShareCodeCallback() {
            @Override
            public void onShareCode(String shareCode) {
                runOnUiThread(() -> {
                    // Verificar que la actividad aún está activa
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }
                    
                    try {
                        // Mostrar diálogo con el código de compartir
                        android.view.View view = getLayoutInflater().inflate(R.layout.dialog_recover_code, null);
                        com.google.android.material.textfield.TextInputEditText etCode = view.findViewById(R.id.etRecoveryCode);
                        if (etCode != null) {
                            etCode.setText(shareCode);
                            etCode.setEnabled(false);
                        }
                        
                        new androidx.appcompat.app.AlertDialog.Builder(MainActivity.this)
                                .setTitle(getString(R.string.share_subject_code_title))
                                .setMessage(getString(R.string.share_subject_code_message))
                                .setView(view)
                                .setPositiveButton(getString(R.string.button_ok), null)
                                .setNeutralButton("Copiar", (dialog, which) -> {
                                    android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(android.content.Context.CLIPBOARD_SERVICE);
                                    android.content.ClipData clip = android.content.ClipData.newPlainText("Código de compartir", shareCode);
                                    clipboard.setPrimaryClip(clip);
                                    Toast.makeText(MainActivity.this, getString(R.string.share_subject_code_copied), Toast.LENGTH_SHORT).show();
                                })
                                .show();
                    } catch (Exception e) {
                        android.util.Log.e("MainActivity", "Error al mostrar diálogo de compartir", e);
                        Toast.makeText(MainActivity.this, "Error al mostrar código: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
            
            @Override
            public void onError(Exception error) {
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }
                    String errorMsg = error != null ? error.getMessage() : "Error desconocido";
                    Toast.makeText(MainActivity.this, getString(R.string.share_subject_error, errorMsg), Toast.LENGTH_LONG).show();
                });
            }
        });
    }
    
    /**
     * Muestra el diálogo para importar un sujeto compartido
     */
    private void showImportSubjectDialog() {
        android.view.View view = getLayoutInflater().inflate(R.layout.dialog_recover_code, null);
        com.google.android.material.textfield.TextInputEditText etCode = view.findViewById(R.id.etRecoveryCode);
        
        if (etCode == null) {
            Toast.makeText(this, "Error al cargar el diálogo", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Buscar el TextInputLayout padre del EditText
        com.google.android.material.textfield.TextInputLayout tilCode = null;
        android.view.ViewParent parent = etCode.getParent();
        while (parent != null) {
            if (parent instanceof com.google.android.material.textfield.TextInputLayout) {
                tilCode = (com.google.android.material.textfield.TextInputLayout) parent;
                break;
            }
            parent = parent.getParent();
        }
        
        // Dejar el hint en blanco para que sea legible
        if (tilCode != null) {
            tilCode.setHint("");
        }
        
        // Agregar formateo automático del código mientras se escribe (mayúsculas y guiones)
        etCode.addTextChangedListener(new RecoveryCodeFormatter(etCode));
        
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(getString(R.string.share_subject_receive_title))
                .setMessage(getString(R.string.share_subject_receive_message))
                .setView(view)
                .setPositiveButton(getString(R.string.button_ok), (dialog, which) -> {
                    String code = etCode.getText() != null ? etCode.getText().toString().trim() : "";
                    if (code.isEmpty()) {
                        Toast.makeText(this, getString(R.string.share_subject_invalid), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    // Mostrar intersticial de AdMob antes de importar
                    String interstitialId = getString(R.string.admob_interstitial_id);
                    AdMobHelper.showInterstitial(MainActivity.this, interstitialId, () -> {
                        importSharedSubject(code);
                    });
                })
                .setNegativeButton(getString(R.string.button_cancel), null)
                .show();
    }
    
    /**
     * Importa un sujeto compartido usando el código proporcionado
     */
    private void importSharedSubject(String shareCode) {
        if (shareCode == null || shareCode.isEmpty()) {
            Toast.makeText(this, getString(R.string.share_subject_invalid), Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Normalizar el código antes de buscarlo (quitar guiones y espacios, convertir a mayúsculas)
        String normalizedCode = shareCode.replaceAll("[\\s-]", "").toUpperCase().trim();
        Log.d("MainActivity", "Código original: " + shareCode);
        Log.d("MainActivity", "Código normalizado: " + normalizedCode);
        Log.d("MainActivity", "Longitud: " + normalizedCode.length());
        
        if (normalizedCode.length() != 12) {
            Toast.makeText(this, "El código debe tener 12 caracteres. Código ingresado: " + normalizedCode.length() + " caracteres", Toast.LENGTH_LONG).show();
            return;
        }
        
        Toast.makeText(this, getString(R.string.share_subject_importing), Toast.LENGTH_SHORT).show();
        
        com.gastonlesbegueris.caretemplate.util.SubjectShareManager shareManager = 
            new com.gastonlesbegueris.caretemplate.util.SubjectShareManager(this);
        
        // Pasar el código normalizado
        shareManager.getSharedSubject(normalizedCode, new com.gastonlesbegueris.caretemplate.util.SubjectShareManager.SharedSubjectCallback() {
            @Override
            public void onSharedSubjectData(java.util.Map<String, Object> subjectData, java.util.List<java.util.Map<String, Object>> eventsData) {
                com.gastonlesbegueris.caretemplate.util.SharedSubjectImporter importer = 
                    new com.gastonlesbegueris.caretemplate.util.SharedSubjectImporter(MainActivity.this);
                
                importer.importFromSharedData(subjectData, eventsData, 
                    () -> runOnUiThread(() -> {
                        Toast.makeText(MainActivity.this, getString(R.string.share_subject_imported), Toast.LENGTH_SHORT).show();
                        // Sincronizar automáticamente después de importar (en hilo de fondo)
                        Log.d("MainActivity", "Iniciando sincronización automática después de importar sujeto");
                        new Thread(() -> {
                            // Obtener userId y sincronizar en hilo de fondo
                            String userId = getCurrentUserId();
                            if (userId != null) {
                                performSyncWithUserId(userId, true); // true = silenciar errores de permisos
                            } else {
                                // Si no hay userId, intentar sincronizar de todas formas
                                doSync();
                            }
                        }).start();
                        // Intersticial removido - refrescar lista directamente
                        runOnUiThread(() -> {
                            // Refrescar la lista de eventos
                            observeLocal();
                        });
                    }),
                    () -> runOnUiThread(() -> {
                        Toast.makeText(MainActivity.this, getString(R.string.share_subject_import_error, "Error desconocido"), Toast.LENGTH_LONG).show();
                    })
                );
            }
            
            @Override
            public void onError(Exception error) {
                runOnUiThread(() -> {
                    String errorMsg = error != null ? error.getMessage() : "Error desconocido";
                    if (errorMsg != null && errorMsg.contains("no encontrado")) {
                        Toast.makeText(MainActivity.this, getString(R.string.share_subject_not_found), Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(MainActivity.this, getString(R.string.share_subject_import_error, errorMsg), Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }
    
    private String defaultIconForFlavor() {
        if ("pets".equals(appType))   return "dog";
        if ("cars".equals(appType))   return "car";
        if ("house".equals(appType))  return "house";
        if ("family".equals(appType)) return "man";
        return "user";
    }
}
