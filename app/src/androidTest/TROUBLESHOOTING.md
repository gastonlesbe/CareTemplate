# 🔧 Solución de Problemas - Tests de Instrumentación

## ❌ Problema: No aparece la opción "Run" al hacer click derecho

Si no ves la opción "Run" cuando haces click derecho en el test, sigue estos pasos:

### Paso 1: Sincronizar el Proyecto con Gradle

1. En Android Studio, ve a: **File > Sync Project with Gradle Files**
2. Espera a que termine la sincronización
3. Verifica que no haya errores en la pestaña "Build"

### Paso 2: Reconstruir el Proyecto

1. Ve a: **Build > Rebuild Project**
2. Espera a que termine la compilación
3. Verifica que no haya errores

### Paso 3: Verificar que el Test Runner esté Configurado

Abre `app/build.gradle` y verifica que tenga:

```gradle
defaultConfig {
    // ... otras configuraciones ...
    testInstrumentationRunner "androidx.test.runner.AndroidJUnitRunner"
}
```

Si no está, agrégalo y sincroniza nuevamente.

### Paso 4: Verificar que las Dependencias de Testing Estén

En `app/build.gradle`, debe haber:

```gradle
dependencies {
    // ... otras dependencias ...
    
    // Testing dependencies
    androidTestImplementation 'androidx.test.ext:junit:1.2.1'
    androidTestImplementation 'androidx.test:runner:1.6.2'
    androidTestImplementation 'androidx.test:rules:1.6.1'
    androidTestImplementation 'androidx.test.espresso:espresso-core:3.6.1'
}
```

### Paso 5: Invalidar Caché y Reiniciar

1. Ve a: **File > Invalidate Caches...**
2. Selecciona: **Invalidate and Restart**
3. Espera a que Android Studio se reinicie

### Paso 6: Verificar la Estructura del Proyecto

Asegúrate de que el archivo esté en la ubicación correcta:

```
app/
  src/
    androidTest/          ← Debe existir esta carpeta
      java/
        com/
          gastonlesbegueris/
            caretemplate/
              PopulateDemoDataTest.java  ← El archivo debe estar aquí
```

### Paso 7: Verificar que el Archivo Tenga las Anotaciones Correctas

El archivo debe tener:

```java
@RunWith(AndroidJUnit4.class)
public class PopulateDemoDataTest {
    @Test
    public void populateDemoDataFamily() {
        // ...
    }
}
```

## ❌ Problema: "No tests found" o "Test class not found"

### Solución 1: Limpiar y Reconstruir

```bash
# Desde la terminal, en la raíz del proyecto
./gradlew clean
./gradlew build
```

### Solución 2: Verificar que el Package Sea Correcto

El package debe ser exactamente:
```java
package com.gastonlesbegueris.caretemplate;
```

### Solución 3: Verificar que el Dispositivo Esté Conectado

```bash
# Verificar dispositivos conectados
adb devices

# Debe mostrar algo como:
# List of devices attached
# emulator-5554    device
```

## ❌ Problema: "Test execution failed"

### Verificar Logs

1. Abre la pestaña **Logcat** en Android Studio
2. Filtra por `PopulateDemoDataTest` o `DemoDataHelper`
3. Revisa los errores

## ✅ Checklist Antes de Ejecutar

- [ ] Proyecto sincronizado con Gradle
- [ ] Proyecto compilado sin errores
- [ ] Dispositivo/emulador conectado (`adb devices` muestra el dispositivo)
- [ ] App instalada en el dispositivo
- [ ] `testInstrumentationRunner` configurado en `build.gradle`
- [ ] Dependencias de testing agregadas en `build.gradle`
- [ ] El archivo está en `app/src/androidTest/java/...`

## 🚀 Alternativa: Ejecutar desde la Terminal

### Opción 1: Ejecutar todos los tests (Más simple)

```bash
# Ir a la carpeta del proyecto
cd /home/gaston/StudioProjects/Caretemplate

# Verificar que hay un dispositivo conectado
adb devices

# Ejecutar TODOS los tests de instrumentación para Family
./gradlew connectedFamilyDebugAndroidTest
```

**Nota**: El flag `--tests` no funciona con `connectedAndroidTest`. Si necesitas ejecutar un test específico, usa Android Studio.

### Opción 2: Usar adb shell am instrument (Avanzado)

Si realmente necesitas ejecutar un test específico desde la terminal:

```bash
# Primero, instalar el APK de test
./gradlew :app:installFamilyDebugAndroidTest

# Luego ejecutar el test específico
adb shell am instrument -w -r \
  -e class com.gastonlesbegueris.caretemplate.PopulateDemoDataTest#populateDemoDataFamily \
  com.gastonlesbegueris.caretemplate.family.test/androidx.test.runner.AndroidJUnitRunner
```

**Recomendación**: Usa Android Studio para ejecutar tests específicos, es mucho más simple.

## 📞 Si Nada Funciona

1. Cierra Android Studio completamente
2. Elimina las carpetas `.idea` y `.gradle` (haz backup primero)
3. Abre Android Studio nuevamente
4. Importa el proyecto como "Import Gradle Project"
5. Espera a que se sincronice completamente
