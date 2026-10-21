package com.gastonlesbegueris.caretemplate.util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.gastonlesbegueris.caretemplate.data.local.AppDb;
import com.gastonlesbegueris.caretemplate.data.local.EventDao;
import com.gastonlesbegueris.caretemplate.data.local.EventEntity;

import java.util.List;

/**
 * BroadcastReceiver para reprogramar notificaciones después de que el dispositivo se reinicia
 */
public class BootReceiver extends BroadcastReceiver {
    
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) {
            return;
        }
        
        // Solo procesar si es un evento de reinicio
        String action = intent.getAction();
        if (!Intent.ACTION_BOOT_COMPLETED.equals(action) &&
            !"android.intent.action.QUICKBOOT_POWERON".equals(action) &&
            !"com.htc.intent.action.QUICKBOOT_POWERON".equals(action)) {
            return;
        }
        
        android.util.Log.d("BootReceiver", "Dispositivo reiniciado, reprogramando notificaciones...");
        
        // Reprogramar notificaciones en un hilo de fondo
        new Thread(() -> {
            try {
                AppDb db = AppDb.get(context);
                EventDao eventDao = db.eventDao();
                
                // Obtener todos los eventos pendientes (no realizados, no eliminados, futuros)
                long now = System.currentTimeMillis();
                List<EventEntity> pendingEvents = eventDao.listPendingEvents("pets", now);
                pendingEvents.addAll(eventDao.listPendingEvents("cars", now));
                
                // Reprogramar notificaciones para todos los eventos pendientes
                int scheduled = 0;
                for (EventEntity event : pendingEvents) {
                    if (event.deleted == 0 && event.realized == 0) {
                        NotificationHelper.scheduleNotification(context, event);
                        scheduled++;
                    }
                }
                
                android.util.Log.d("BootReceiver", "Notificaciones reprogramadas después del reinicio: " + scheduled);
            } catch (Exception e) {
                android.util.Log.e("BootReceiver", "Error al reprogramar notificaciones después del reinicio", e);
            }
        }).start();
    }
}

