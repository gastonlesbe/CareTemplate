package com.gastonlesbegueris.caretemplate.util;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.gastonlesbegueris.caretemplate.R;
import com.gastonlesbegueris.caretemplate.ui.SubjectHistoryActivity;

/**
 * BroadcastReceiver para recibir alarmas de notificaciones de eventos
 */
public class EventNotificationReceiver extends BroadcastReceiver {
    
    private static final String CHANNEL_ID = "event_notifications";
    private static final int NOTIFICATION_ID_BASE = 1000;
    
    // Mapa para guardar los Ringtones activos por eventId
    private static final java.util.Map<String, android.media.Ringtone> activeRingtones = new java.util.concurrent.ConcurrentHashMap<>();
    
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) {
            return;
        }
        
        // Manejar reprogramación de alarma cuando el usuario borra la notificación
        if ("com.gastonlesbegueris.caretemplate.REPEAT_NOTIFICATION".equals(intent.getAction())) {
            String eventId = intent.getStringExtra("eventId");
            boolean isReminder = intent.getBooleanExtra("isReminder", false);
            
            android.util.Log.d("EventNotificationReceiver", "REPEAT_NOTIFICATION recibido para evento: " + eventId + ", isReminder: " + isReminder);
            
            if (eventId != null && !isReminder) {
                // Solo reprogramar notificaciones de vencimiento (no recordatorios)
                android.util.Log.d("EventNotificationReceiver", "Iniciando reprogramación de alarma para evento: " + eventId);
                rescheduleAlarmIn2Minutes(context, eventId);
            } else {
                android.util.Log.d("EventNotificationReceiver", "No se reprograma: eventId=" + eventId + ", isReminder=" + isReminder);
            }
            return;
        }
        
        // Asegurar que el dispositivo se mantenga despierto para procesar la notificación
        android.os.PowerManager powerManager = (android.os.PowerManager) context.getSystemService(Context.POWER_SERVICE);
        android.os.PowerManager.WakeLock wakeLock = null;
        if (powerManager != null) {
            wakeLock = powerManager.newWakeLock(android.os.PowerManager.PARTIAL_WAKE_LOCK, "EventNotificationReceiver::WakeLock");
            wakeLock.acquire(10 * 60 * 1000L); // 10 minutos máximo
        }
        
        try {
        
        String eventId = intent.getStringExtra("eventId");
        String eventTitle = intent.getStringExtra("eventTitle");
        long eventDueAt = intent.getLongExtra("eventDueAt", 0);
        String subjectId = intent.getStringExtra("subjectId");
        String appType = intent.getStringExtra("appType");
        boolean isReminder = intent.getBooleanExtra("isReminder", false);
        
        if (eventId == null || eventTitle == null) {
            return;
        }
        
        // Crear canal de notificación (requerido en Android 8.0+)
        createNotificationChannel(context);
        
        // Crear Intent para abrir el historial del sujeto
        Intent openIntent = new Intent(context, SubjectHistoryActivity.class);
        openIntent.putExtra("subjectId", subjectId);
        openIntent.putExtra("appType", appType);
        openIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        
        PendingIntent pendingIntent = PendingIntent.getActivity(
            context,
            eventId.hashCode(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        // Formatear fecha/hora del evento
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault());
        String eventDate = sdf.format(new java.util.Date(eventDueAt));
        
        // Determinar el mensaje según el tipo de notificación
        String notificationText;
        if (isReminder) {
            // Notificación previa (recordatorio)
            notificationText = context.getString(R.string.notification_event_reminder, eventDate);
        } else {
            // Notificación de vencimiento
            notificationText = context.getString(R.string.notification_event_due, eventDate);
        }
        
        // Crear Intent para reprogramar la alarma en 2 minutos cuando se borra la notificación
        // Solo para notificaciones de vencimiento (no recordatorios)
        PendingIntent deletePendingIntent = null;
        if (!isReminder) {
            Intent deleteIntent = new Intent(context, EventNotificationReceiver.class);
            deleteIntent.setAction("com.gastonlesbegueris.caretemplate.REPEAT_NOTIFICATION");
            deleteIntent.putExtra("eventId", eventId);
            deleteIntent.putExtra("isReminder", isReminder);
            
            deletePendingIntent = PendingIntent.getBroadcast(
                context,
                ("repeat_" + eventId).hashCode(),
                deleteIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
        }
        
        // Crear la notificación
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_event)
            .setContentTitle(eventTitle)
            .setContentText(notificationText)
            .setStyle(new NotificationCompat.BigTextStyle()
                .bigText(notificationText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_SOUND | NotificationCompat.DEFAULT_VIBRATE)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent);
        
        // Solo agregar deleteIntent para notificaciones de vencimiento
        if (deletePendingIntent != null) {
            builder.setDeleteIntent(deletePendingIntent);
        }
        
        // Si es una notificación de vencimiento (no recordatorio), reproducir alarma
        if (!isReminder) {
            playAlarmSound(context, eventTitle, eventId);
        }
        
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            int notificationId = NOTIFICATION_ID_BASE + eventId.hashCode();
            notificationManager.notify(notificationId, builder.build());
        }
        } finally {
            // Liberar el WakeLock
            if (wakeLock != null && wakeLock.isHeld()) {
                wakeLock.release();
            }
        }
    }
    
    /**
     * Reproduce un sonido de alarma del sistema
     * Funciona tanto para eventos nuevos como existentes que tienen notificaciones programadas
     */
    private void playAlarmSound(Context context, String eventTitle, String eventId) {
        try {
            // Usar el sonido de alarma por defecto del sistema
            android.media.RingtoneManager ringtoneManager = new android.media.RingtoneManager(context);
            ringtoneManager.setType(android.media.RingtoneManager.TYPE_ALARM);
            android.net.Uri alarmUri = ringtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_ALARM);
            
            // Si no hay alarma por defecto, usar notificación
            if (alarmUri == null) {
                alarmUri = ringtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION);
            }
            
            if (alarmUri != null) {
                // Usar AudioManager para asegurar que el sonido se reproduzca correctamente
                android.media.AudioManager audioManager = (android.media.AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
                if (audioManager != null) {
                    // Asegurar que el volumen de alarma esté activo
                    int currentVolume = audioManager.getStreamVolume(android.media.AudioManager.STREAM_ALARM);
                    if (currentVolume == 0) {
                        // Si el volumen está en 0, intentar aumentar ligeramente (solo si el usuario lo permite)
                        // Nota: No podemos cambiar el volumen sin permiso del usuario, pero podemos intentar reproducir
                        android.util.Log.d("EventNotificationReceiver", "Volumen de alarma en 0, intentando reproducir de todas formas");
                    }
                }
                
                android.media.Ringtone ringtone = ringtoneManager.getRingtone(context, alarmUri);
                if (ringtone != null) {
                    // Configurar el tipo de stream para alarmas
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        ringtone.setAudioAttributes(new android.media.AudioAttributes.Builder()
                            .setUsage(android.media.AudioAttributes.USAGE_ALARM)
                            .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build());
                    }
                    
                    // Guardar la referencia al Ringtone para poder detenerlo después
                    if (eventId != null) {
                        // Detener cualquier Ringtone anterior para este evento
                        stopAlarmSound(eventId);
                        // Guardar la referencia al nuevo Ringtone
                        activeRingtones.put(eventId, ringtone);
                    }
                    
                    ringtone.play();
                    android.util.Log.d("EventNotificationReceiver", "Alarma reproducida para evento: " + eventTitle);
                } else {
                    android.util.Log.w("EventNotificationReceiver", "No se pudo obtener ringtone para alarma");
                }
            } else {
                android.util.Log.w("EventNotificationReceiver", "No se encontró URI de alarma por defecto");
            }
        } catch (SecurityException e) {
            android.util.Log.e("EventNotificationReceiver", "Error de seguridad al reproducir alarma (puede necesitar permisos)", e);
        } catch (Exception e) {
            android.util.Log.e("EventNotificationReceiver", "Error al reproducir alarma", e);
        }
    }
    
    /**
     * Detiene el sonido de alarma que está sonando para un evento
     * @param eventId ID del evento
     */
    public static void stopAlarmSound(String eventId) {
        if (eventId == null) {
            return;
        }
        
        android.media.Ringtone ringtone = activeRingtones.remove(eventId);
        if (ringtone != null && ringtone.isPlaying()) {
            ringtone.stop();
            android.util.Log.d("EventNotificationReceiver", "Alarma detenida para evento: " + eventId);
        }
    }
    
    /**
     * Detiene todas las alarmas que están sonando
     */
    public static void stopAllAlarmSounds() {
        for (java.util.Map.Entry<String, android.media.Ringtone> entry : activeRingtones.entrySet()) {
            android.media.Ringtone ringtone = entry.getValue();
            if (ringtone != null && ringtone.isPlaying()) {
                ringtone.stop();
            }
        }
        activeRingtones.clear();
        android.util.Log.d("EventNotificationReceiver", "Todas las alarmas detenidas");
    }
    
    /**
     * Reprograma la alarma para que suene de nuevo en 2 minutos
     * Solo si el evento no está realizado
     */
    private void rescheduleAlarmIn2Minutes(Context context, String eventId) {
        // Ejecutar en un hilo de fondo porque Room requiere acceso fuera del hilo principal
        new Thread(() -> {
            try {
                android.util.Log.d("EventNotificationReceiver", "Buscando evento en BD: " + eventId);
                
                // Verificar que el evento no esté realizado
                com.gastonlesbegueris.caretemplate.data.local.AppDb db = com.gastonlesbegueris.caretemplate.data.local.AppDb.get(context);
                com.gastonlesbegueris.caretemplate.data.local.EventDao eventDao = db.eventDao();
                com.gastonlesbegueris.caretemplate.data.local.EventEntity event = eventDao.findOne(eventId);
                
                if (event == null) {
                    android.util.Log.w("EventNotificationReceiver", "Evento no encontrado en BD: " + eventId);
                    return;
                }
                
                android.util.Log.d("EventNotificationReceiver", "Evento encontrado: " + event.title + ", realized=" + event.realized + ", deleted=" + event.deleted);
                
                if (event.realized == 1 || event.deleted == 1) {
                    android.util.Log.d("EventNotificationReceiver", "Evento realizado o eliminado, no se reprograma alarma: " + eventId);
                    return;
                }
                
                // Reprogramar la alarma para que suene en 2 minutos
                long nextAlarmTime = System.currentTimeMillis() + (2 * 60 * 1000L); // 2 minutos
                
                AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                if (alarmManager == null) {
                    return;
                }
                
                Intent repeatIntent = new Intent(context, EventNotificationReceiver.class);
                repeatIntent.setAction(NotificationHelper.ACTION_NOTIFY_EVENT);
                repeatIntent.putExtra("eventId", event.id);
                repeatIntent.putExtra("eventTitle", event.title);
                repeatIntent.putExtra("eventDueAt", event.dueAt);
                repeatIntent.putExtra("subjectId", event.subjectId);
                repeatIntent.putExtra("appType", event.appType);
                repeatIntent.putExtra("isReminder", false); // Siempre es notificación de vencimiento
                
                int requestCode = ("due_" + eventId).hashCode();
                PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    repeatIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                );
                
                // Programar la alarma para 2 minutos desde ahora
                java.util.Date nextAlarmDate = new java.util.Date(nextAlarmTime);
                android.util.Log.d("EventNotificationReceiver", "Programando alarma para: " + nextAlarmDate + " (en 2 minutos)");
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    try {
                        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextAlarmTime, pendingIntent);
                        android.util.Log.d("EventNotificationReceiver", "Alarma reprogramada exitosamente para 2 minutos: " + eventId);
                    } catch (SecurityException e) {
                        android.util.Log.w("EventNotificationReceiver", "No se puede usar setExactAndAllowWhileIdle, usando setExact", e);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                            alarmManager.setExact(AlarmManager.RTC_WAKEUP, nextAlarmTime, pendingIntent);
                            android.util.Log.d("EventNotificationReceiver", "Alarma reprogramada con setExact: " + eventId);
                        } else {
                            alarmManager.set(AlarmManager.RTC_WAKEUP, nextAlarmTime, pendingIntent);
                            android.util.Log.d("EventNotificationReceiver", "Alarma reprogramada con set: " + eventId);
                        }
                    }
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, nextAlarmTime, pendingIntent);
                    android.util.Log.d("EventNotificationReceiver", "Alarma reprogramada con setExact: " + eventId);
                } else {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, nextAlarmTime, pendingIntent);
                    android.util.Log.d("EventNotificationReceiver", "Alarma reprogramada con set: " + eventId);
                }
            } catch (Exception e) {
                android.util.Log.e("EventNotificationReceiver", "Error al reprogramar alarma", e);
            }
        }).start();
    }
    
    private void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (notificationManager == null) {
                return;
            }
            
            NotificationChannel channel = notificationManager.getNotificationChannel(CHANNEL_ID);
            if (channel == null) {
                channel = new NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.notification_channel_name),
                    NotificationManager.IMPORTANCE_HIGH
                );
                channel.setDescription(context.getString(R.string.notification_channel_description));
                channel.enableVibration(true);
                channel.enableLights(true);
                // Configurar sonido de alarma para el canal
                android.net.Uri alarmSound = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_ALARM);
                if (alarmSound != null) {
                    android.media.AudioAttributes audioAttributes = new android.media.AudioAttributes.Builder()
                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(android.media.AudioAttributes.USAGE_ALARM)
                        .build();
                    channel.setSound(alarmSound, audioAttributes);
                }
                notificationManager.createNotificationChannel(channel);
            }
        }
    }
}

