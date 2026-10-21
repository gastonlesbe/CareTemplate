package com.gastonlesbegueris.caretemplate.util;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.gastonlesbegueris.caretemplate.data.local.EventEntity;

/**
 * Helper para gestionar notificaciones de eventos
 */
public class NotificationHelper {
    
    public static final String ACTION_NOTIFY_EVENT = "com.gastonlesbegueris.caretemplate.NOTIFY_EVENT";
    
    /**
     * Programa una notificación para un evento
     * Programa DOS notificaciones:
     * 1. Una ANTES del evento (si notificationMinutesBefore > 0)
     * 2. Una CUANDO el evento vence (siempre, a menos que esté realizado)
     * @param context Contexto de la aplicación
     * @param event Evento para el cual programar la notificación
     */
    public static void scheduleNotification(Context context, EventEntity event) {
        android.util.Log.d("NotificationHelper", "Intentando programar notificación para evento: " + event.id + ", título: " + event.title);
        
        if (event.realized == 1) {
            android.util.Log.d("NotificationHelper", "Evento ya realizado, no se programa notificación");
            return;
        }
        
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            android.util.Log.e("NotificationHelper", "AlarmManager es null");
            return;
        }
        
        android.util.Log.d("NotificationHelper", "Evento dueAt: " + new java.util.Date(event.dueAt) + ", notificationMinutesBefore: " + event.notificationMinutesBefore);
        
        // 1. Programar notificación ANTES del evento (si está configurada)
        if (event.notificationMinutesBefore != null && event.notificationMinutesBefore > 0) {
            long notificationTime = event.dueAt - (event.notificationMinutesBefore * 60 * 1000L);
            
            // Solo programar si el tiempo de notificación aún no pasó
            if (notificationTime > System.currentTimeMillis()) {
                Intent intent = new Intent(context, EventNotificationReceiver.class);
                intent.setAction(ACTION_NOTIFY_EVENT);
                intent.putExtra("eventId", event.id);
                intent.putExtra("eventTitle", event.title);
                intent.putExtra("eventDueAt", event.dueAt);
                intent.putExtra("subjectId", event.subjectId);
                intent.putExtra("appType", event.appType);
                intent.putExtra("isReminder", true); // Marcar como recordatorio previo
                
                // RequestCode único para la notificación previa
                int requestCode = ("reminder_" + event.id).hashCode();
                
                PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                );
                
                // Programar la alarma
                // Usar setExactAndAllowWhileIdle para que funcione incluso cuando la app está cerrada
                // y el dispositivo está en modo de ahorro de batería
                try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    try {
                        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, notificationTime, pendingIntent);
                        android.util.Log.d("NotificationHelper", "Notificación previa programada para: " + new java.util.Date(notificationTime));
                    } catch (SecurityException e) {
                        // Si no tenemos permiso para alarmas exactas, intentar con setExact
                        android.util.Log.w("NotificationHelper", "No se puede usar setExactAndAllowWhileIdle, usando setExact");
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                            alarmManager.setExact(AlarmManager.RTC_WAKEUP, notificationTime, pendingIntent);
                        } else {
                            alarmManager.set(AlarmManager.RTC_WAKEUP, notificationTime, pendingIntent);
                        }
                        } catch (IllegalStateException e) {
                            // Límite de alarmas alcanzado
                            android.util.Log.w("NotificationHelper", "Límite de alarmas alcanzado, no se puede programar notificación previa: " + e.getMessage());
                            throw e; // Re-lanzar para que el llamador lo maneje
                    }
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, notificationTime, pendingIntent);
                } else {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, notificationTime, pendingIntent);
                    }
                } catch (IllegalStateException e) {
                    // Límite de alarmas alcanzado - re-lanzar para que el llamador lo maneje
                    throw e;
                }
            }
        }
        
        // 2. Programar notificación CUANDO el evento vence (siempre, a menos que ya venció)
        if (event.dueAt > System.currentTimeMillis()) {
            Intent intent = new Intent(context, EventNotificationReceiver.class);
            intent.setAction(ACTION_NOTIFY_EVENT);
            intent.putExtra("eventId", event.id);
            intent.putExtra("eventTitle", event.title);
            intent.putExtra("eventDueAt", event.dueAt);
            intent.putExtra("subjectId", event.subjectId);
            intent.putExtra("appType", event.appType);
            intent.putExtra("isReminder", false); // Marcar como notificación de vencimiento
            
            // RequestCode único para la notificación de vencimiento
            int requestCode = ("due_" + event.id).hashCode();
            
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            
            // Programar la alarma para cuando el evento vence
            // Usar setExactAndAllowWhileIdle para que funcione incluso cuando la app está cerrada
            try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                try {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, event.dueAt, pendingIntent);
                    android.util.Log.d("NotificationHelper", "Notificación de vencimiento programada para: " + new java.util.Date(event.dueAt));
                } catch (SecurityException e) {
                    // Si no tenemos permiso para alarmas exactas, intentar con setExact
                    android.util.Log.w("NotificationHelper", "No se puede usar setExactAndAllowWhileIdle, usando setExact");
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        alarmManager.setExact(AlarmManager.RTC_WAKEUP, event.dueAt, pendingIntent);
                    } else {
                        alarmManager.set(AlarmManager.RTC_WAKEUP, event.dueAt, pendingIntent);
                    }
                    } catch (IllegalStateException e) {
                        // Límite de alarmas alcanzado
                        android.util.Log.w("NotificationHelper", "Límite de alarmas alcanzado, no se puede programar notificación de vencimiento: " + e.getMessage());
                        throw e; // Re-lanzar para que el llamador lo maneje
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, event.dueAt, pendingIntent);
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, event.dueAt, pendingIntent);
                }
            } catch (IllegalStateException e) {
                // Límite de alarmas alcanzado - re-lanzar para que el llamador lo maneje
                throw e;
            }
        } else {
            android.util.Log.d("NotificationHelper", "Evento ya venció, no se programa notificación de vencimiento");
        }
    }
    
    /**
     * Cancela todas las notificaciones programadas para un evento
     * Cancela tanto la notificación previa como la de vencimiento
     * También detiene el sonido de alarma si está sonando
     * @param context Contexto de la aplicación
     * @param eventId ID del evento
     */
    public static void cancelNotification(Context context, String eventId) {
        // Detener el sonido de alarma si está sonando
        EventNotificationReceiver.stopAlarmSound(eventId);
        
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            return;
        }
        
        // Cancelar notificación previa (reminder)
        Intent reminderIntent = new Intent(context, EventNotificationReceiver.class);
        reminderIntent.setAction(ACTION_NOTIFY_EVENT);
        reminderIntent.putExtra("eventId", eventId);
        reminderIntent.putExtra("isReminder", true);
        
        int reminderRequestCode = ("reminder_" + eventId).hashCode();
        PendingIntent reminderPendingIntent = PendingIntent.getBroadcast(
            context,
            reminderRequestCode,
            reminderIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        alarmManager.cancel(reminderPendingIntent);
        reminderPendingIntent.cancel();
        
        // Cancelar notificación de vencimiento (due)
        Intent dueIntent = new Intent(context, EventNotificationReceiver.class);
        dueIntent.setAction(ACTION_NOTIFY_EVENT);
        dueIntent.putExtra("eventId", eventId);
        dueIntent.putExtra("isReminder", false);
        
        int dueRequestCode = ("due_" + eventId).hashCode();
        PendingIntent duePendingIntent = PendingIntent.getBroadcast(
            context,
            dueRequestCode,
            dueIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        alarmManager.cancel(duePendingIntent);
        duePendingIntent.cancel();
    }
    
    /**
     * Programa notificaciones para todos los eventos pendientes
     * @param context Contexto de la aplicación
     * @param events Lista de eventos
     */
    public static void scheduleNotificationsForEvents(Context context, java.util.List<EventEntity> events) {
        if (events == null) {
            return;
        }
        
        for (EventEntity event : events) {
            scheduleNotification(context, event);
        }
    }
}

