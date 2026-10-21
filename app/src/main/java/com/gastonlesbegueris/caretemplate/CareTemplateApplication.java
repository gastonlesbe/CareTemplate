package com.gastonlesbegueris.caretemplate;

import android.app.Application;
import android.content.Context;

import androidx.appcompat.app.AppCompatDelegate;

import com.gastonlesbegueris.caretemplate.data.local.AppDb;
import com.gastonlesbegueris.caretemplate.data.local.EventDao;
import com.gastonlesbegueris.caretemplate.data.local.EventEntity;
import com.gastonlesbegueris.caretemplate.util.NotificationHelper;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;

import java.util.List;

public class CareTemplateApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // Apply saved theme (Light / Dark / System); defaults to System if never set
        com.gastonlesbegueris.caretemplate.util.ThemeHelper.applyTheme(this);
        
        // Inicializar AdMob SDK solo si los anuncios están habilitados para este flavor
        if (getResources().getBoolean(R.bool.ads_enabled)) {
            String appId = getString(getResources().getIdentifier("admob_app_id", "string", getPackageName()));
            MobileAds.initialize(this, new OnInitializationCompleteListener() {
                @Override
                public void onInitializationComplete(InitializationStatus initializationStatus) {
                    android.util.Log.d("CareTemplateApp", "AdMob initialized successfully");
                }
            });
        }
        
        // Reprogramar todas las notificaciones cuando la app se inicia
        // Esto asegura que las notificaciones funcionen incluso si la app fue cerrada
        rescheduleAllNotifications();
    }
    
    /**
     * Reprograma todas las notificaciones de eventos pendientes
     * Esto se ejecuta cuando la app se inicia para asegurar que las notificaciones
     * estén activas incluso si la app fue cerrada o el dispositivo se reinició
     */
    private void rescheduleAllNotifications() {
        new Thread(() -> {
            try {
                AppDb db = AppDb.get(this);
                EventDao eventDao = db.eventDao();
                
                long now = System.currentTimeMillis();
                int scheduled = 0;
                int dueScheduled = 0;
                
                // 1. Obtener todos los eventos pendientes (no realizados, no eliminados, futuros)
                List<EventEntity> pendingEvents = eventDao.listPendingEvents("pets", now);
                pendingEvents.addAll(eventDao.listPendingEvents("cars", now));
                
                // Reprogramar notificaciones para todos los eventos pendientes futuros
                for (EventEntity event : pendingEvents) {
                    if (event.deleted == 0 && event.realized == 0) {
                        NotificationHelper.scheduleNotification(this, event);
                        scheduled++;
                    }
                }
                
                // 2. Obtener eventos vencidos (no realizados, no eliminados, que ya vencieron)
                // Estos necesitan una alarma inmediata porque ya pasaron
                List<EventEntity> dueEvents = eventDao.listDueUnrealized("pets", now);
                dueEvents.addAll(eventDao.listDueUnrealized("cars", now));
                
                // Programar alarmas inmediatas para eventos vencidos
                for (EventEntity event : dueEvents) {
                    if (event.deleted == 0 && event.realized == 0) {
                        // Programar alarma para que suene en 30 segundos (dar tiempo a que la app termine de cargar)
                        scheduleImmediateAlarmForDueEvent(this, event);
                        dueScheduled++;
                    }
                }
                
                android.util.Log.d("CareTemplateApp", "Notificaciones reprogramadas: " + scheduled + " futuros, " + dueScheduled + " vencidos");
            } catch (Exception e) {
                android.util.Log.e("CareTemplateApp", "Error al reprogramar notificaciones", e);
            }
        }).start();
    }
    
    /**
     * Programa una alarma inmediata para un evento vencido
     * La alarma sonará en 30 segundos para dar tiempo a que la app termine de cargar
     */
    private void scheduleImmediateAlarmForDueEvent(Context context, EventEntity event) {
        try {
            android.app.AlarmManager alarmManager = (android.app.AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (alarmManager == null) {
                return;
            }
            
            // Programar para 30 segundos desde ahora
            long alarmTime = System.currentTimeMillis() + (30 * 1000L);
            
            android.content.Intent intent = new android.content.Intent(context, com.gastonlesbegueris.caretemplate.util.EventNotificationReceiver.class);
            intent.setAction(com.gastonlesbegueris.caretemplate.util.NotificationHelper.ACTION_NOTIFY_EVENT);
            intent.putExtra("eventId", event.id);
            intent.putExtra("eventTitle", event.title);
            intent.putExtra("eventDueAt", event.dueAt);
            intent.putExtra("subjectId", event.subjectId);
            intent.putExtra("appType", event.appType);
            intent.putExtra("isReminder", false); // Es notificación de vencimiento
            
            int requestCode = ("due_" + event.id).hashCode();
            android.app.PendingIntent pendingIntent = android.app.PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT | android.app.PendingIntent.FLAG_IMMUTABLE
            );
            
            // Programar la alarma
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                try {
                    alarmManager.setExactAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, alarmTime, pendingIntent);
                } catch (SecurityException e) {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                        alarmManager.setExact(android.app.AlarmManager.RTC_WAKEUP, alarmTime, pendingIntent);
                    } else {
                        alarmManager.set(android.app.AlarmManager.RTC_WAKEUP, alarmTime, pendingIntent);
                    }
                }
            } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                alarmManager.setExact(android.app.AlarmManager.RTC_WAKEUP, alarmTime, pendingIntent);
            } else {
                alarmManager.set(android.app.AlarmManager.RTC_WAKEUP, alarmTime, pendingIntent);
            }
            
            android.util.Log.d("CareTemplateApp", "Alarma inmediata programada para evento vencido: " + event.title);
        } catch (Exception e) {
            android.util.Log.e("CareTemplateApp", "Error al programar alarma inmediata para evento vencido", e);
        }
    }
}

