# Solución de Advertencias de Google Play Console

## ✅ Cambios Realizados

### 1. Reducción del Tamaño del APK
- ✅ Habilitado `minifyEnabled true` en `build.gradle`
- ✅ Habilitado `shrinkResources true` para eliminar recursos no usados
- ✅ Agregadas reglas de ProGuard necesarias

Esto reducirá significativamente el tamaño del APK.

### 2. Archivo de Desofuscación
El archivo de desofuscación se generará automáticamente cuando compiles con `minifyEnabled true`. Se encuentra en:
```
app/build/outputs/mapping/petsRelease/mapping.txt
```

**Para subirlo a Play Console:**
1. Después de compilar, busca el archivo `mapping.txt` en la ruta anterior
2. En Play Console, ve a "Versión" → "Código" → "Archivos de desofuscación"
3. Sube el archivo `mapping.txt` correspondiente a tu flavor

### 3. Símbolos Nativos (NDK)
Para generar los símbolos nativos (necesarios para Appodeal SDK que incluye código nativo):

**Opción 1: Habilitar automáticamente (Recomendado)**
Agrega esto a `app/build.gradle` en la sección `android`:

```gradle
android {
    // ... otras configuraciones ...
    
    buildTypes {
        release {
            // ... otras configuraciones ...
            ndk {
                debugSymbolLevel 'FULL'
            }
        }
    }
}
```

Luego recompila y el archivo `.so.sym.zip` se generará automáticamente en:
```
app/build/outputs/native-debug-symbols/petsRelease/native-debug-symbols.zip
```

**Para subirlo a Play Console:**
1. Busca el archivo `.so.sym.zip` en la ruta anterior
2. En Play Console, ve a "Versión" → "Código" → "Símbolos nativos"
3. Sube el archivo `.so.sym.zip`

### 4. Dispositivos No Compatibles
Esta advertencia puede deberse a:
- **targetSdk 35 (Android 15)**: Algunos dispositivos muy antiguos pueden no ser compatibles
- **Appodeal SDK 3.12.0.0**: Puede requerir características más nuevas

**Opciones:**
1. **Aceptar la pérdida de compatibilidad** (recomendado si son dispositivos muy antiguos)
2. **Reducir targetSdk** a 34 (Android 14) si necesitas mantener más compatibilidad
3. **Revisar qué dispositivos se ven afectados** en Play Console antes de decidir

## 📝 Pasos para Compilar y Subir

1. **Compilar el AAB con minify habilitado:**
```bash
./gradlew clean bundlePetsRelease
```

2. **Buscar los archivos generados:**
```bash
# Archivo de desofuscación
ls -lh app/build/outputs/mapping/petsRelease/mapping.txt

# Símbolos nativos (si se configuró)
ls -lh app/build/outputs/native-debug-symbols/petsRelease/*.zip
```

3. **Subir a Play Console:**
   - AAB: `app/build/outputs/bundle/petsRelease/app-pets-release.aab`
   - Mapping: `app/build/outputs/mapping/petsRelease/mapping.txt`
   - Símbolos nativos: `app/build/outputs/native-debug-symbols/petsRelease/native-debug-symbols.zip`

## ⚠️ Nota Importante

Si después de habilitar minify encuentras errores en tiempo de ejecución, puede ser necesario agregar más reglas de ProGuard. Revisa los logs de la app para identificar qué clases necesitan ser preservadas.



