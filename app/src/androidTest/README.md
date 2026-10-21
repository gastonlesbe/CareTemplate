# 📊 Carga de Datos de Demostración

Este directorio contiene tests de instrumentación para poblar la base de datos local con datos de demostración. Los datos se crean directamente en Room (base de datos local), no en Firebase.

## 🎯 Propósito

Poblar la base de datos con datos de ejemplo para hacer demos de la app:
- **1 sujeto** por flavor con datos apropiados
- **5 eventos básicos** (2 pasados, 3 futuros)
- **6 meses de gastos** con **4 gastos cada mes** = **24 gastos totales** (eventos realizados con costos)

## 🚀 Cómo Ejecutar

> **⚠️ IMPORTANTE**: Los tests de instrumentación **NO se pueden ejecutar fácilmente desde la terminal** con `./gradlew connectedAndroidTest --tests`. El flag `--tests` no funciona con `connectedAndroidTest`. **Usa Android Studio** para ejecutar tests específicos.

### Opción 1: Desde Android Studio (Recomendado)

1. **Abre Android Studio**
2. **Conecta un dispositivo o inicia un emulador:**
   - Ve a `Tools > Device Manager`
   - Crea o inicia un emulador (API 24+)
   - O conecta un dispositivo físico con USB debugging habilitado

3. **Navega al archivo:**
   - `app > src > androidTest > java > com.gastonlesbegueris.caretemplate > SimpleImportDemoData.java`
   - O usa `Ctrl+Shift+N` (o `Cmd+Shift+O` en Mac) y busca "SimpleImportDemoData"

4. **Selecciona el flavor correcto:**
   - En la barra de herramientas, selecciona el flavor que quieras (House, Family, Pets, o Cars)
   - Ejemplo: Para House, selecciona "houseDebug"

5. **Click derecho en el método que quieras ejecutar:**
   - `importHouseData()` - Para poblar datos de House
   - `importFamilyData()` - Para poblar datos de Family
   - `importPetsData()` - Para poblar datos de Pets
   - `importCarsData()` - Para poblar datos de Cars

6. **Selecciona:** `Run 'import...Data()'`

7. **Espera a que termine** (verás los logs en la pestaña "Run" o "Logcat")
   
   **Tiempo estimado:** ~30-60 segundos

8. **Verifica los datos:**
   - El test abrirá la app automáticamente al final
   - Deberías ver el sujeto creado con eventos y gastos

## 📋 Qué Hace el Test

El test `SimpleImportDemoData` ejecuta dos partes:

1. **PARTE 1**: Crea 1 sujeto + 5 eventos básicos
   - 2 eventos pasados (realizados)
   - 3 eventos futuros (pendientes)

2. **PARTE 2**: Crea 24 gastos
   - Distribuidos en los últimos 6 meses
   - 4 gastos por mes
   - Todos son eventos realizados con costos

## ⚠️ Notas Importantes

- **El test verifica si ya existe un sujeto**: Si ya hay un sujeto para el flavor, el test mostrará un mensaje pero no creará datos duplicados
- **Los datos se guardan en la base de datos local** (Room), no en Firebase
- **Para limpiar datos**, desinstala y reinstala la app
- **El test abre la app al final** para evitar que Android la desinstale

## 🐛 Troubleshooting

### "Test no se ejecuta"
- Verifica que tengas un dispositivo/emulador conectado
- Verifica que la app esté instalada en el dispositivo
- Sincroniza el proyecto: `File > Sync Project with Gradle Files`

### "Test falla con timeout"
- El test espera hasta 30 segundos para la parte 1 y 60 segundos para la parte 2
- Si falla, verifica que no haya problemas de conexión
- Revisa los logs en Logcat para ver qué está pasando

### "No se crean los datos"
- Revisa los logs en Logcat (busca `SimpleImportDemo` o `DemoDataHelper`)
- Verifica que el flavor sea correcto
- Asegúrate de que la base de datos esté accesible
- Verifica que no haya datos existentes (el test no crea si ya existen)

### "La app se desinstala después del test"
- El test ahora abre la app al final para evitar esto
- Si sigue pasando, verifica los logs para ver si hay errores
- Asegúrate de que el test termine exitosamente

### "Timed out waiting for process" o "failed to attach" desde terminal
- Este error ocurre cuando ejecutas `./gradlew connectedAndroidTest` desde la terminal
- **Solución**: Usa Android Studio para ejecutar tests específicos
- O usa el script ADB: `./scripts/import_demo_data_adb.sh house`
- Si necesitas usar la terminal, ejecuta TODOS los tests: `./gradlew connectedHouseDebugAndroidTest` (sin `--tests`)

### "El test no aparece en Android Studio" o "No aparece la opción Run"
1. **Sincroniza el proyecto**: `File > Sync Project with Gradle Files`
2. **Reconstruye el proyecto**: `Build > Rebuild Project`
3. **Verifica que `testInstrumentationRunner` esté configurado** en `build.gradle`:
   ```gradle
   defaultConfig {
       testInstrumentationRunner "androidx.test.runner.AndroidJUnitRunner"
   }
   ```
4. **Invalidar caché**: `File > Invalidate Caches... > Invalidate and Restart`
5. **Verifica que el archivo esté en la ubicación correcta**: `app/src/androidTest/java/...`

## 📁 Archivos

- **`SimpleImportDemoData.java`**: Test de instrumentación que importa los datos usando `DemoDataHelper`
- **`TROUBLESHOOTING.md`**: Guía detallada de solución de problemas

## 🔄 Alternativas

### Opción 2: Script ADB

Si el test desde Android Studio no funciona, puedes usar el script ADB:

```bash
./scripts/import_demo_data_adb.sh house
```

Este script ejecuta el test de forma más controlada y mantiene la app abierta.

### Opción 3: Ejecutar desde Terminal (Solo TODOS los tests)

Si necesitas ejecutar desde la terminal, **debes ejecutar TODOS los tests** (no puedes ejecutar uno específico):

```bash
# Ejecutar TODOS los tests de instrumentación para House
./gradlew connectedHouseDebugAndroidTest

# Ejecutar TODOS los tests de instrumentación para Family
./gradlew connectedFamilyDebugAndroidTest

# Ejecutar TODOS los tests de instrumentación para Pets
./gradlew connectedPetsDebugAndroidTest

# Ejecutar TODOS los tests de instrumentación para Cars
./gradlew connectedCarsDebugAndroidTest
```

**Nota**: Esto ejecutará TODOS los tests de instrumentación, no solo `SimpleImportDemoData`. Si solo quieres ejecutar un test específico, **usa Android Studio** (Opción 1).

### Opción 4: ADB Shell (Avanzado)

Si realmente necesitas ejecutar un test específico desde la terminal:

```bash
# 1. Instalar el APK de test
./gradlew :app:installHouseDebugAndroidTest

# 2. Ejecutar el test específico
adb shell am instrument -w -r \
  -e class com.gastonlesbegueris.caretemplate.SimpleImportDemoData#importHouseData \
  com.gastonlesbegueris.caretemplate.house.test/androidx.test.runner.AndroidJUnitRunner
```

**Recomendación**: Usa Android Studio (Opción 1), es mucho más simple y confiable.
