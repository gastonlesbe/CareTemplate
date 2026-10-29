package com.gastonlesbegueris.caretemplate.util;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.gastonlesbegueris.caretemplate.R;
import com.gastonlesbegueris.caretemplate.data.local.AppDb;
import com.gastonlesbegueris.caretemplate.data.local.EventDao;
import com.gastonlesbegueris.caretemplate.data.local.SubjectDao;

/**
 * Helper class para mostrar tutorial de primera vez
 * Muestra un overlay que resalta el botón FAB y explica cómo agregar el primer sujeto o evento
 */
public class TutorialHelper {
    private static final String TAG = "TutorialHelper";
    private static final String PREFS_NAME = "tutorial_prefs";
    private static final String KEY_TUTORIAL_SUBJECT_COMPLETED = "tutorial_subject_completed";
    private static final String KEY_TUTORIAL_EVENT_COMPLETED = "tutorial_event_completed";
    
    /**
     * Verifica si se debe mostrar el tutorial y lo muestra si corresponde
     * @param activity La actividad actual
     * @param appType El tipo de app (pets, cars, family, house)
     */
    public static void checkAndShowTutorial(Activity activity, String appType) {
        checkAndShowTutorial(activity, appType, false);
    }
    
    /**
     * Verifica si se debe mostrar el tutorial y lo muestra si corresponde
     * @param activity La actividad actual
     * @param appType El tipo de app (pets, cars, family, house)
     * @param forceShow Si es true, muestra el tutorial incluso si hay sujetos y eventos
     */
    public static void checkAndShowTutorial(Activity activity, String appType, boolean forceShow) {
        if (activity == null || appType == null) {
            Log.w(TAG, "Activity o appType es null, no se puede mostrar tutorial");
            return;
        }

        Log.d(TAG, "=== INICIO checkAndShowTutorial ===");
        Log.d(TAG, "appType: " + appType);
        Log.d(TAG, "Activity: " + activity.getClass().getSimpleName());
        Log.d(TAG, "Thread: " + Thread.currentThread().getName());

        // Verificar estado en un hilo separado para no bloquear la UI
        new Thread(() -> {
            try {
                Log.d(TAG, "Thread de verificación iniciado: " + Thread.currentThread().getName());
                AppDb db = AppDb.get(activity);
                SubjectDao subjectDao = db.subjectDao();
                EventDao eventDao = db.eventDao();
                
                int subjectCount = subjectDao.countForApp(appType);
                int eventCount = eventDao.countEventsForApp(appType);
                
                Log.d(TAG, "Subject count: " + subjectCount + ", Event count: " + eventCount);
                
                SharedPreferences prefs = activity.getSharedPreferences(PREFS_NAME, Activity.MODE_PRIVATE);
                boolean subjectTutorialCompleted = prefs.getBoolean(KEY_TUTORIAL_SUBJECT_COMPLETED, false);
                boolean eventTutorialCompleted = prefs.getBoolean(KEY_TUTORIAL_EVENT_COMPLETED, false);
                
                Log.d(TAG, "Tutorial sujetos completado: " + subjectTutorialCompleted);
                Log.d(TAG, "Tutorial eventos completado: " + eventTutorialCompleted);
                Log.d(TAG, "SharedPreferences name: " + PREFS_NAME);
                
                // Si no hay sujetos, mostrar tutorial de sujetos
                // IMPORTANTE: Si no hay sujetos, siempre mostrar el tutorial, incluso si está marcado como completado
                if (subjectCount == 0) {
                    if (!subjectTutorialCompleted) {
                        Log.d(TAG, "✓ Condiciones cumplidas: Mostrando tutorial de sujetos");
                    } else {
                        Log.w(TAG, "⚠ Tutorial marcado como completado pero no hay sujetos. Reseteando y mostrando tutorial.");
                        // Resetear el flag ya que no hay sujetos
                        prefs.edit().putBoolean(KEY_TUTORIAL_SUBJECT_COMPLETED, false).apply();
                    }
                    Log.d(TAG, "Ejecutando runOnUiThread para mostrar tutorial");
                    activity.runOnUiThread(() -> {
                        Log.d(TAG, "En runOnUiThread, verificando activity");
                        if (!activity.isFinishing() && !activity.isDestroyed()) {
                            Log.d(TAG, "Activity válida, programando showSubjectTutorialStep1");
                            activity.getWindow().getDecorView().postDelayed(() -> {
                                Log.d(TAG, "En postDelayed, verificando activity nuevamente");
                                if (!activity.isFinishing() && !activity.isDestroyed()) {
                                    Log.d(TAG, "Ejecutando showSubjectTutorialStep1");
                                    showSubjectTutorialStep1(activity, appType);
                                } else {
                                    Log.w(TAG, "Activity destruida antes de mostrar tutorial");
                                }
                            }, 500);
                        } else {
                            Log.w(TAG, "Activity destruida antes de programar tutorial");
                        }
                    });
                }
                // Si hay sujetos pero no eventos, mostrar tutorial de eventos
                else if (eventCount == 0) {
                    if (!eventTutorialCompleted) {
                        Log.d(TAG, "✓ Condiciones cumplidas: Mostrando tutorial de eventos");
                        activity.runOnUiThread(() -> {
                            if (!activity.isFinishing() && !activity.isDestroyed()) {
                                activity.getWindow().getDecorView().postDelayed(() -> {
                                    if (!activity.isFinishing() && !activity.isDestroyed()) {
                                        Log.d(TAG, "Ejecutando showEventTutorialStep1");
                                        showEventTutorialStep1(activity, appType);
                                    } else {
                                        Log.w(TAG, "Activity destruida antes de mostrar tutorial");
                                    }
                                }, 1000);
                            } else {
                                Log.w(TAG, "Activity destruida antes de programar tutorial");
                            }
                        });
                    } else {
                        Log.d(TAG, "✗ Tutorial de eventos ya completado, no se muestra");
                    }
                } else {
                    if (forceShow) {
                        // Si se fuerza la visualización, mostrar el tutorial de eventos (más avanzado)
                        Log.d(TAG, "Forzando visualización: Mostrando tutorial de eventos");
                        activity.runOnUiThread(() -> {
                            if (!activity.isFinishing() && !activity.isDestroyed()) {
                                activity.getWindow().getDecorView().postDelayed(() -> {
                                    if (!activity.isFinishing() && !activity.isDestroyed()) {
                                        Log.d(TAG, "Ejecutando showEventTutorialStep1 (forzado)");
                                        showEventTutorialStep1(activity, appType);
                                    } else {
                                        Log.w(TAG, "Activity destruida antes de mostrar tutorial");
                                    }
                                }, 1000);
                            } else {
                                Log.w(TAG, "Activity destruida antes de programar tutorial");
                            }
                        });
                    } else {
                        Log.d(TAG, "✗ Hay sujetos y eventos, no se muestra tutorial");
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error al verificar tutorial: " + e.getMessage(), e);
                e.printStackTrace();
            }
        }).start();
        
        Log.d(TAG, "=== FIN checkAndShowTutorial ===");
    }
    
    /**
     * Paso 1: Mostrar tutorial de sujetos resaltando el FAB principal
     */
    private static void showSubjectTutorialStep1(Activity activity, String appType) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            Log.w(TAG, "Activity null o destruida en showSubjectTutorialStep1");
            return;
        }

        Log.d(TAG, "showSubjectTutorialStep1 llamado");

        View fabMain = activity.findViewById(R.id.fabAdd);
        if (fabMain == null) {
            Log.w(TAG, "FAB principal no encontrado");
            return;
        }
        
        Log.d(TAG, "FAB principal encontrado");

        // Crear overlay
        FrameLayout overlay = createOverlay(activity);
        ViewGroup rootView = activity.findViewById(android.R.id.content);
        rootView.addView(overlay);

        // Obtener posición del FAB (relativa a la ventana)
        int[] fabLocation = new int[2];
        fabMain.getLocationInWindow(fabLocation);
        Rect fabRect = new Rect();
        fabMain.getHitRect(fabRect);
        fabRect.offset(fabLocation[0], fabLocation[1]);

        // Obtener mensaje según el flavor
        String message = getTutorialMessageStep1(activity, appType);

        // Configurar overlay
        setupOverlayForFab(overlay, fabRect, message, () -> {
            // Cuando se hace clic en el overlay, cerrar el paso 1 primero
            rootView.removeView(overlay);
            // Esperar a que el overlay se remueva completamente antes de continuar
            activity.getWindow().getDecorView().postDelayed(() -> {
                // Simular clic en el FAB para abrirlo
                fabMain.performClick();
                // Esperar a que el FAB se abra antes de mostrar el paso 2
                activity.getWindow().getDecorView().postDelayed(() -> {
                    showSubjectTutorialStep2(activity, appType, rootView);
                }, 400);
            }, 200);
        });
    }
    
    /**
     * Paso 2: Mostrar tutorial de sujetos resaltando el botón de agregar sujeto (cuando el FAB está abierto)
     */
    private static void showSubjectTutorialStep2(Activity activity, String appType, ViewGroup rootView) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            return;
        }

        // Asegurarse de que no haya overlays anteriores
        removeExistingOverlays(rootView);

        View fabSubject = activity.findViewById(R.id.fabAddSubject);
        if (fabSubject == null || fabSubject.getVisibility() != View.VISIBLE) {
            Log.w(TAG, "FAB de sujeto no encontrado o no visible");
            // Si no está visible, marcar tutorial como completado
            markTutorialCompleted(activity);
            return;
        }

        // Crear overlay
        FrameLayout overlay = createOverlay(activity);
        rootView.addView(overlay);

        // Esperar un poco más para asegurar que el FAB esté completamente visible y medido
        activity.getWindow().getDecorView().postDelayed(() -> {
            // Obtener posición del FAB de sujeto (relativa a la ventana)
            int[] fabLocation = new int[2];
            fabSubject.getLocationInWindow(fabLocation);
            Rect fabRect = new Rect();
            fabSubject.getHitRect(fabRect);
            fabRect.offset(fabLocation[0], fabLocation[1]);
            
            // Si el rect está vacío o inválido, intentar obtener las dimensiones del view
            if (fabRect.isEmpty() || fabRect.width() == 0 || fabRect.height() == 0) {
                fabRect.left = fabLocation[0];
                fabRect.top = fabLocation[1];
                fabRect.right = fabLocation[0] + fabSubject.getWidth();
                fabRect.bottom = fabLocation[1] + fabSubject.getHeight();
            }

            // Obtener mensaje según el flavor (con imagen del botón)
            CharSequence message = getTutorialMessageStep2WithIcon(activity, appType);

            // Configurar overlay
            setupOverlayForFab(overlay, fabRect, message, () -> {
                // Cuando se hace clic, cerrar el tutorial
                rootView.removeView(overlay);
                markSubjectTutorialCompleted(activity);
            });
        }, 200);
    }
    
    /**
     * Paso 1: Mostrar tutorial de eventos resaltando el FAB principal
     */
    private static void showEventTutorialStep1(Activity activity, String appType) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            return;
        }

        View fabMain = activity.findViewById(R.id.fabAdd);
        if (fabMain == null) {
            Log.w(TAG, "FAB principal no encontrado");
            return;
        }

        // Crear overlay
        FrameLayout overlay = createOverlay(activity);
        ViewGroup rootView = activity.findViewById(android.R.id.content);
        rootView.addView(overlay);

        // Obtener posición del FAB (relativa a la ventana)
        int[] fabLocation = new int[2];
        fabMain.getLocationInWindow(fabLocation);
        Rect fabRect = new Rect();
        fabMain.getHitRect(fabRect);
        fabRect.offset(fabLocation[0], fabLocation[1]);

        // Obtener mensaje según el flavor
        String message = getEventTutorialMessageStep1(activity, appType);

        // Configurar overlay
        setupOverlayForFab(overlay, fabRect, message, () -> {
            // Cuando se hace clic en el overlay, cerrar el paso 1 primero
            rootView.removeView(overlay);
            // Esperar a que el overlay se remueva completamente antes de continuar
            activity.getWindow().getDecorView().postDelayed(() -> {
                // Simular clic en el FAB para abrirlo
                fabMain.performClick();
                // Esperar a que el FAB se abra antes de mostrar el paso 2
                activity.getWindow().getDecorView().postDelayed(() -> {
                    showEventTutorialStep2(activity, appType, rootView);
                }, 400);
            }, 200);
        });
    }
    
    /**
     * Paso 2: Mostrar tutorial de eventos resaltando el botón de agregar evento (cuando el FAB está abierto)
     */
    private static void showEventTutorialStep2(Activity activity, String appType, ViewGroup rootView) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            return;
        }

        // Asegurarse de que no haya overlays anteriores
        removeExistingOverlays(rootView);

        View fabEvent = activity.findViewById(R.id.fabAddEvent);
        if (fabEvent == null || fabEvent.getVisibility() != View.VISIBLE) {
            Log.w(TAG, "FAB de evento no encontrado o no visible");
            // Si no está visible, marcar tutorial como completado
            markEventTutorialCompleted(activity);
            return;
        }

        // Crear overlay
        FrameLayout overlay = createOverlay(activity);
        rootView.addView(overlay);

        // Esperar un poco más para asegurar que el FAB esté completamente visible y medido
        activity.getWindow().getDecorView().postDelayed(() -> {
            // Obtener posición del FAB de evento (relativa a la ventana)
            int[] fabLocation = new int[2];
            fabEvent.getLocationInWindow(fabLocation);
            Rect fabRect = new Rect();
            fabEvent.getHitRect(fabRect);
            fabRect.offset(fabLocation[0], fabLocation[1]);
            
            // Si el rect está vacío o inválido, intentar obtener las dimensiones del view
            if (fabRect.isEmpty() || fabRect.width() == 0 || fabRect.height() == 0) {
                fabRect.left = fabLocation[0];
                fabRect.top = fabLocation[1];
                fabRect.right = fabLocation[0] + fabEvent.getWidth();
                fabRect.bottom = fabLocation[1] + fabEvent.getHeight();
            }

            // Obtener mensaje según el flavor (con imagen del botón)
            CharSequence message = getEventTutorialMessageStep2WithIcon(activity, appType);

            // Configurar overlay
            setupOverlayForFab(overlay, fabRect, message, () -> {
                // Cuando se hace clic, cerrar el tutorial
                rootView.removeView(overlay);
                markEventTutorialCompleted(activity);
            });
        }, 200);
    }
    
    /**
     * Crea el overlay base para el tutorial
     */
    private static FrameLayout createOverlay(Activity activity) {
        FrameLayout overlay = new FrameLayout(activity);
        overlay.setLayoutParams(new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        ));
        overlay.setBackgroundColor(0xCC000000); // Fondo semi-transparente
        overlay.setClickable(true);
        overlay.setFocusable(true);
        // Agregar un tag para identificar overlays del tutorial
        overlay.setTag("tutorial_overlay");
        return overlay;
    }
    
    /**
     * Remueve cualquier overlay del tutorial que pueda estar presente
     */
    private static void removeExistingOverlays(ViewGroup rootView) {
        if (rootView == null) return;
        
        // Buscar y remover todos los overlays del tutorial
        for (int i = rootView.getChildCount() - 1; i >= 0; i--) {
            View child = rootView.getChildAt(i);
            if (child instanceof FrameLayout && "tutorial_overlay".equals(child.getTag())) {
                rootView.removeView(child);
                Log.d(TAG, "Overlay anterior removido");
            }
        }
    }
    
    /**
     * Configura el overlay para resaltar un FAB específico
     */
    private static void setupOverlayForFab(FrameLayout overlay, Rect fabRect, CharSequence message, Runnable onDismiss) {
        Activity activity = (Activity) overlay.getContext();
        
        // Vista circular que resalta el FAB
        View circleHighlight = new View(activity);
        int circleSize = Math.max(fabRect.width(), fabRect.height()) + 40;
        FrameLayout.LayoutParams circleParams = new FrameLayout.LayoutParams(
            circleSize,
            circleSize
        );
        circleHighlight.setLayoutParams(circleParams);
        circleHighlight.setBackgroundResource(R.drawable.bg_tutorial_highlight);
        
        // Usar ViewTreeObserver para posicionar el círculo después de que el overlay esté medido
        overlay.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                overlay.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                
                // Obtener posición del overlay en la ventana
                int[] overlayLocation = new int[2];
                overlay.getLocationInWindow(overlayLocation);
                
                // Obtener posición del rootView (android.R.id.content) en la ventana
                ViewGroup rootView = (ViewGroup) overlay.getParent();
                int[] rootLocation = new int[2];
                if (rootView != null) {
                    rootView.getLocationInWindow(rootLocation);
                } else {
                    rootLocation[0] = 0;
                    rootLocation[1] = 0;
                }
                
                // Calcular posición relativa del FAB al overlay
                // fabRect está en coordenadas de ventana, overlayLocation también
                int relativeX = fabRect.centerX() - overlayLocation[0];
                int relativeY = fabRect.centerY() - overlayLocation[1];
                
                // Asegurar que las coordenadas sean válidas
                if (relativeX < 0) relativeX = 0;
                if (relativeY < 0) relativeY = 0;
                
                FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) circleHighlight.getLayoutParams();
                params.leftMargin = relativeX - circleSize / 2;
                params.topMargin = relativeY - circleSize / 2;
                circleHighlight.setLayoutParams(params);
                
                // Forzar redibujado
                circleHighlight.invalidate();
                overlay.invalidate();
            }
        });
        
        // Crear vista de mensaje
        TextView messageView = new TextView(activity);
        FrameLayout.LayoutParams messageParams = new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        );
        messageParams.gravity = android.view.Gravity.CENTER_HORIZONTAL;
        messageParams.setMargins(32, 0, 32, 0);
        
        messageView.setLayoutParams(messageParams);
        messageView.setText(message);
        messageView.setTextColor(0xFFFFFFFF);
        messageView.setTextSize(18);
        messageView.setPadding(24, 16, 24, 16);
        messageView.setBackgroundResource(R.drawable.bg_tutorial_message);
        messageView.setGravity(android.view.Gravity.CENTER);
        
        // Botón de continuar
        TextView continueButton = new TextView(activity);
        FrameLayout.LayoutParams buttonParams = new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        );
        buttonParams.gravity = android.view.Gravity.CENTER_HORIZONTAL;
        buttonParams.setMargins(0, 0, 0, 0);
        
        continueButton.setLayoutParams(buttonParams);
        continueButton.setText(activity.getString(R.string.tutorial_button_continue));
        continueButton.setTextColor(0xFFFFFFFF);
        continueButton.setTextSize(16);
        continueButton.setPadding(32, 16, 32, 16);
        continueButton.setBackgroundResource(R.drawable.bg_tutorial_button);
        continueButton.setGravity(android.view.Gravity.CENTER);
        continueButton.setOnClickListener(v -> {
            if (onDismiss != null) {
                onDismiss.run();
            }
        });
        
        overlay.addView(circleHighlight);
        overlay.addView(messageView);
        overlay.addView(continueButton);
        
        // Usar ViewTreeObserver para posicionar correctamente después del layout
        overlay.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                overlay.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                
                int overlayHeight = overlay.getHeight();
                int overlayWidth = overlay.getWidth();
                
                // Medir el mensaje para saber su altura
                messageView.measure(
                    View.MeasureSpec.makeMeasureSpec(overlayWidth - 64, View.MeasureSpec.AT_MOST),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
                );
                int messageHeight = messageView.getMeasuredHeight();
                
                // Centrar mensaje verticalmente en la pantalla (no arriba del FAB)
                FrameLayout.LayoutParams msgParams = (FrameLayout.LayoutParams) messageView.getLayoutParams();
                msgParams.gravity = android.view.Gravity.CENTER;
                msgParams.bottomMargin = 0;
                msgParams.topMargin = 0;
                msgParams.leftMargin = 32;
                msgParams.rightMargin = 32;
                messageView.setLayoutParams(msgParams);
                
                // Medir el botón para saber su altura
                continueButton.measure(
                    View.MeasureSpec.makeMeasureSpec(overlayWidth, View.MeasureSpec.AT_MOST),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
                );
                int buttonHeight = continueButton.getMeasuredHeight();
                
                // Reposicionar botón debajo del mensaje (centrado horizontalmente, debajo del mensaje)
                FrameLayout.LayoutParams btnParams = (FrameLayout.LayoutParams) continueButton.getLayoutParams();
                btnParams.gravity = android.view.Gravity.CENTER_HORIZONTAL;
                // Colocar el botón debajo del mensaje: centro + mitad altura mensaje + espacio + mitad altura botón
                btnParams.topMargin = overlayHeight / 2 + messageHeight / 2 + 40; // 40dp de espacio entre mensaje y botón
                btnParams.leftMargin = 0;
                btnParams.rightMargin = 0;
                continueButton.setLayoutParams(btnParams);
            }
        });
        
        // Hacer clic en el overlay también cierra el tutorial
        overlay.setOnClickListener(v -> {
            if (onDismiss != null) {
                onDismiss.run();
            }
        });
    }
    
    /**
     * Obtiene el mensaje del tutorial de sujetos paso 1 según el flavor
     */
    private static String getTutorialMessageStep1(Activity activity, String appType) {
        String subjectType = getSubjectTypeName(activity, appType);
        String welcome = activity.getString(R.string.tutorial_subject_step1_welcome);
        String message = activity.getString(R.string.tutorial_subject_step1_message, subjectType);
        return welcome + "\n\n" + message;
    }
    
    /**
     * Obtiene el mensaje del tutorial de sujetos paso 2 con imagen del botón
     */
    private static CharSequence getTutorialMessageStep2WithIcon(Activity activity, String appType) {
        String subjectType = getSubjectTypeName(activity, appType);
        String text = activity.getString(R.string.tutorial_subject_step2_message, subjectType);
        
        // Obtener el icono del botón según el flavor
        int iconRes;
        if ("family".equals(appType)) {
            iconRes = R.drawable.ic_fab_family;
        } else {
            iconRes = R.drawable.ic_header_flavor;
        }
        
        // Crear SpannableString con imagen
        android.text.SpannableString spannable = new android.text.SpannableString(text);
        
        // Obtener el drawable y escalarlo
        android.graphics.drawable.Drawable drawable = activity.getResources().getDrawable(iconRes);
        int size = (int) (activity.getResources().getDisplayMetrics().density * 24); // 24dp
        drawable.setBounds(0, 0, size, size);
        
        // Crear ImageSpan
        android.text.style.ImageSpan imageSpan = new android.text.style.ImageSpan(drawable, android.text.style.DynamicDrawableSpan.ALIGN_BASELINE);
        
        // El icono reemplaza el espacio del medio del hueco de tres espacios que deja el
        // string ("toca   para" / "tap   to"); así no dependemos del idioma del texto.
        // El string va entre comillas para que aapt no colapse los espacios.
        int gap = text.indexOf("   ");
        if (gap < 0) return text;
        int insertPos = gap + 1;
        
        // Insertar la imagen
        spannable.setSpan(imageSpan, insertPos, insertPos + 1, android.text.Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
        
        return spannable;
    }
    
    /**
     * Obtiene el mensaje del tutorial de eventos paso 1 según el flavor
     */
    private static String getEventTutorialMessageStep1(Activity activity, String appType) {
        String welcome = activity.getString(R.string.tutorial_event_step1_welcome);
        String message = activity.getString(R.string.tutorial_event_step1_message);
        return welcome + "\n\n" + message;
    }
    
    /**
     * Obtiene el mensaje del tutorial de eventos paso 2 con imagen del botón
     */
    private static CharSequence getEventTutorialMessageStep2WithIcon(Activity activity, String appType) {
        String text = activity.getString(R.string.tutorial_event_step2_message);
        
        // Obtener el icono del botón de eventos (calendario)
        int iconRes = R.drawable.ic_event;
        
        // Crear SpannableString con imagen
        android.text.SpannableString spannable = new android.text.SpannableString(text);
        
        // Obtener el drawable y escalarlo
        android.graphics.drawable.Drawable drawable = activity.getResources().getDrawable(iconRes);
        int size = (int) (activity.getResources().getDisplayMetrics().density * 24); // 24dp
        drawable.setBounds(0, 0, size, size);
        
        // Crear ImageSpan
        android.text.style.ImageSpan imageSpan = new android.text.style.ImageSpan(drawable, android.text.style.DynamicDrawableSpan.ALIGN_BASELINE);
        
        // El icono reemplaza el espacio del medio del hueco de tres espacios que deja el
        // string ("toca   para" / "tap   to"); así no dependemos del idioma del texto.
        // El string va entre comillas para que aapt no colapse los espacios.
        int gap = text.indexOf("   ");
        if (gap < 0) return text;
        int insertPos = gap + 1;
        
        // Insertar la imagen
        spannable.setSpan(imageSpan, insertPos, insertPos + 1, android.text.Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
        
        return spannable;
    }
    
    /**
     * Obtiene el nombre del tipo de sujeto según el flavor
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
     * Marca el tutorial de sujetos como completado
     */
    public static void markSubjectTutorialCompleted(Activity activity) {
        if (activity != null) {
            SharedPreferences prefs = activity.getSharedPreferences(PREFS_NAME, Activity.MODE_PRIVATE);
            prefs.edit().putBoolean(KEY_TUTORIAL_SUBJECT_COMPLETED, true).apply();
            Log.d(TAG, "Tutorial de sujetos marcado como completado");
        }
    }
    
    /**
     * Marca el tutorial de eventos como completado
     */
    public static void markEventTutorialCompleted(Activity activity) {
        if (activity != null) {
            SharedPreferences prefs = activity.getSharedPreferences(PREFS_NAME, Activity.MODE_PRIVATE);
            prefs.edit().putBoolean(KEY_TUTORIAL_EVENT_COMPLETED, true).apply();
            Log.d(TAG, "Tutorial de eventos marcado como completado");
        }
    }
    
    /**
     * Marca el tutorial como completado (compatibilidad con código existente)
     */
    public static void markTutorialCompleted(Activity activity) {
        markSubjectTutorialCompleted(activity);
    }
    
    /**
     * Verifica si el tutorial de sujetos ya fue completado
     */
    public static boolean isSubjectTutorialCompleted(Activity activity) {
        if (activity == null) return true;
        SharedPreferences prefs = activity.getSharedPreferences(PREFS_NAME, Activity.MODE_PRIVATE);
        return prefs.getBoolean(KEY_TUTORIAL_SUBJECT_COMPLETED, false);
    }
    
    /**
     * Verifica si el tutorial de eventos ya fue completado
     */
    public static boolean isEventTutorialCompleted(Activity activity) {
        if (activity == null) return true;
        SharedPreferences prefs = activity.getSharedPreferences(PREFS_NAME, Activity.MODE_PRIVATE);
        return prefs.getBoolean(KEY_TUTORIAL_EVENT_COMPLETED, false);
    }
    
    /**
     * Verifica si el tutorial ya fue completado (compatibilidad con código existente)
     */
    public static boolean isTutorialCompleted(Activity activity) {
        return isSubjectTutorialCompleted(activity);
    }
    
    /**
     * Resetea el estado del tutorial (útil para testing)
     */
    public static void resetTutorial(Activity activity) {
        if (activity != null) {
            SharedPreferences prefs = activity.getSharedPreferences(PREFS_NAME, Activity.MODE_PRIVATE);
            prefs.edit()
                .putBoolean(KEY_TUTORIAL_SUBJECT_COMPLETED, false)
                .putBoolean(KEY_TUTORIAL_EVENT_COMPLETED, false)
                .apply();
            Log.d(TAG, "Tutorial reseteado");
        }
    }
}

