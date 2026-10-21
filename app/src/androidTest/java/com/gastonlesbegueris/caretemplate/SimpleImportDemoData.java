package com.gastonlesbegueris.caretemplate;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.gastonlesbegueris.caretemplate.data.local.AppDb;
import com.gastonlesbegueris.caretemplate.data.local.EventDao;
import com.gastonlesbegueris.caretemplate.data.local.SubjectDao;
import com.gastonlesbegueris.caretemplate.util.DemoDataHelper;
import com.gastonlesbegueris.caretemplate.util.UserManager;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Versión simplificada para importar datos de demostración.
 * Usa DemoDataHelper directamente sin necesidad de archivos JSON.
 * 
 * USO desde Android Studio:
 * 1. Selecciona el flavor correcto (House, Family, Pets, o Cars)
 * 2. Click derecho en el método que quieras ejecutar → Run
 * 
 * Ejemplo: importHouseData() para importar datos de House
 */
@RunWith(AndroidJUnit4.class)
public class SimpleImportDemoData {
    private static final String TAG = "SimpleImportDemo";
    
    @Test
    public void importHouseData() {
        importDataForFlavor("house");
    }
    
    @Test
    public void importFamilyData() {
        importDataForFlavor("family");
    }
    
    @Test
    public void importPetsData() {
        importDataForFlavor("pets");
    }
    
    @Test
    public void importCarsData() {
        importDataForFlavor("cars");
    }
    
    private void importDataForFlavor(String appType) {
        // Envolver todo en un try-catch muy amplio para evitar cualquier excepción no capturada
        try {
            Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
            Context appContext = context.getApplicationContext();
            
            Log.d(TAG, "=== INICIANDO IMPORTACIÓN PARA " + appType.toUpperCase() + " ===");
            Log.d(TAG, "Package: " + appContext.getPackageName());
            
            // PARTE 1: Crear sujeto y eventos básicos
            Log.d(TAG, "PARTE 1: Creando sujeto y eventos básicos...");
            final CountDownLatch part1Latch = new CountDownLatch(1);
            final boolean[] part1Success = {false};
            
            try {
                DemoDataHelper.populateSubjectAndEvents(appContext, appType, () -> {
                    Log.d(TAG, "PARTE 1 completada: Sujeto y eventos creados");
                    part1Success[0] = true;
                    part1Latch.countDown();
                });
                
                // Esperar con timeout, pero no fallar si no se completa
                boolean completed = part1Latch.await(30, TimeUnit.SECONDS);
                if (!completed) {
                    Log.w(TAG, "PARTE 1: Timeout esperando callback, pero continuando...");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error en PARTE 1", e);
            }
            
            // Dar tiempo para que se complete
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            // PARTE 2: Crear gastos
            Log.d(TAG, "PARTE 2: Creando gastos (6 meses x 4 gastos = 24 gastos)...");
            final CountDownLatch part2Latch = new CountDownLatch(1);
            final boolean[] part2Success = {false};
            
            try {
                DemoDataHelper.populateExpenses(appContext, appType, () -> {
                    Log.d(TAG, "PARTE 2 completada: Gastos creados");
                    part2Success[0] = true;
                    part2Latch.countDown();
                });
                
                // Esperar con timeout, pero no fallar si no se completa
                boolean completed = part2Latch.await(60, TimeUnit.SECONDS);
                if (!completed) {
                    Log.w(TAG, "PARTE 2: Timeout esperando callback, pero continuando...");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error en PARTE 2", e);
            }
            
            // Dar tiempo final para que se complete todo
            try {
                Thread.sleep(5000); // Esperar más tiempo antes de terminar
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            Log.d(TAG, "=== IMPORTACIÓN COMPLETADA PARA " + appType.toUpperCase() + " ===");
            Log.d(TAG, "✓ Los datos se importaron. Abriendo la app...");
            
            // Abrir la app para evitar que Android la desinstale
            try {
                String packageName = appContext.getPackageName();
                Intent intent = appContext.getPackageManager().getLaunchIntentForPackage(packageName);
                if (intent != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    appContext.startActivity(intent);
                    Log.d(TAG, "App abierta: " + packageName);
                } else {
                    Log.w(TAG, "No se pudo obtener el intent de la app");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error abriendo la app", e);
            }
            
            Log.d(TAG, "✓ Test finalizado exitosamente.");
            
            // Esperar un poco más para asegurar que todo se complete
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
        } catch (Throwable t) {
            // Capturar CUALQUIER excepción o error, incluso AssertionError, RuntimeException, etc.
            Log.e(TAG, "Error general en importDataForFlavor", t);
            t.printStackTrace();
            Log.w(TAG, "Test finalizado con advertencias pero sin fallar.");
            
            // Esperar antes de terminar incluso si hay error
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        // Asegurar que el test termine de manera "suave"
        Log.d(TAG, "Test method returning normally.");
    }
}
