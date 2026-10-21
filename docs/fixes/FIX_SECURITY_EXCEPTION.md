# 🔧 Solución al Error SecurityException en CarPal

## ❌ Error Encontrado

```
java.lang.SecurityException: Unknown calling package name 'com.google.android.gms'
```

Este error ocurre cuando Google Play Services no puede verificar la identidad de la app porque falta el SHA-1/SHA-256 del keystore que estás usando.

## 🔍 Causa del Problema

El error `SecurityException: Unknown calling package name` generalmente ocurre cuando:

1. **Estás ejecutando un build de DEBUG** pero solo tienes configurados los SHA del keystore de **RELEASE** en Firebase
2. **O viceversa**: estás usando release pero solo tienes los SHA de debug
3. El package name no coincide exactamente con lo configurado en Firebase

## ✅ Solución

### Opción 1: Agregar SHA-1/SHA-256 del Keystore de Debug (Recomendado para Testing)

Si estás probando la app con un build de debug, necesitas agregar también los SHA del keystore de debug a Firebase:

#### 1. Obtener SHA-1 y SHA-256 del Keystore de Debug

El keystore de debug por defecto está en:
- **Ubuntu/Linux**: `~/.android/debug.keystore`
- **Windows**: `C:\Users\<usuario>\.android\debug.keystore`
- **macOS**: `~/.android/debug.keystore`

**Contraseña por defecto**: `android`
**Alias por defecto**: `androiddebugkey`

Ejecuta este comando para obtener los SHA:

```bash
# Linux/macOS
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android | grep -E "(SHA1|SHA256)"

# Windows (PowerShell)
keytool -list -v -keystore $env:USERPROFILE\.android\debug.keystore -alias androiddebugkey -storepass android
```

#### 2. Agregar SHA de Debug a Firebase

1. Ve a [Firebase Console](https://console.firebase.google.com/)
2. Proyecto: **care-template-78bb1**
3. **⚙️ Configuración del proyecto** → **Tus aplicaciones**
4. Selecciona la app: `com.gastonlesbegueris.caretemplate.cars`
5. Haz clic en **"Agregar huella digital"**
6. Agrega el **SHA-1 de debug** que obtuviste
7. Agrega el **SHA-256 de debug** que obtuviste
8. Guarda los cambios

#### 3. Descargar nuevo google-services.json (Opcional)

Después de agregar los SHA, puedes descargar un nuevo `google-services.json`, pero no es estrictamente necesario si ya tienes el correcto.

### Opción 2: Usar Build de Release para Testing

Si prefieres usar el build de release (que ya tiene los SHA configurados):

```bash
./gradlew installCarsRelease
```

**Nota**: Necesitarás un dispositivo conectado o un emulador configurado para instalar builds de release.

### Opción 3: Verificar que estás usando el Package Correcto

Asegúrate de que el package name en Firebase sea exactamente:
```
com.gastonlesbegueris.caretemplate.cars
```

Sin espacios, sin mayúsculas (excepto donde corresponda).

## 🔍 Verificación

### Verificar SHA Configurados en Firebase

1. Firebase Console → Proyecto → Configuración → Tus aplicaciones
2. Selecciona `com.gastonlesbegueris.caretemplate.cars`
3. Verifica que aparezcan **ambos**:
   - SHA-1 del keystore de release
   - SHA-1 del keystore de debug (si estás usando debug builds)

### Verificar Package Name

En `app/build.gradle`, verifica que el `applicationIdSuffix` para cars sea:
```gradle
cars {
    dimension "vertical"
    applicationIdSuffix ".cars"
    ...
}
```

El package final será: `com.gastonlesbegueris.caretemplate.cars` ✅

## 📝 Resumen de SHA Necesarios

### Para Release (Ya configurados):
- **SHA-1**: `51:71:E1:39:64:46:47:31:88:E1:62:0E:BB:B8:61:9E:09:9E:58:C1`
- **SHA-256**: `98:F3:3D:14:BB:1E:BA:04:F0:FE:CA:67:C4:EF:E1:90:CF:01:D1:D8:5B:B5:8A:55:7A:F6:70:51:F8:68:8C:AB`

### Para Debug (Ya obtenidos):
- **SHA-1**: `06:2A:22:6A:D8:B6:50:9F:69:CC:D6:C6:5A:C1:FB:E9:FE:CB:48:21`
- **SHA-256**: `E0:2A:4A:53:75:BC:AB:21:08:85:64:97:8E:F2:55:44:A6:7A:0B:91:43:80:4A:03:AE:D6:04:FF:B2:17:AE:78`

**⚠️ IMPORTANTE**: Agrega estos SHA a Firebase Console para que funcione con builds de debug.

## ✅ Checklist

- [ ] SHA-1 de release agregado en Firebase ✅
- [ ] SHA-256 de release agregado en Firebase ✅
- [x] SHA-1 de debug obtenido ✅ `06:2A:22:6A:D8:B6:50:9F:69:CC:D6:C6:5A:C1:FB:E9:FE:CB:48:21`
- [ ] SHA-1 de debug agregado en Firebase
- [x] SHA-256 de debug obtenido ✅ `E0:2A:4A:53:75:BC:AB:21:08:85:64:97:8E:F2:55:44:A6:7A:0B:91:43:80:4A:03:AE:D6:04:FF:B2:17:AE:78`
- [ ] SHA-256 de debug agregado en Firebase
- [ ] Autenticación anónima habilitada en Firebase
- [ ] App recompilada después de los cambios
- [ ] Error resuelto

## 🚨 Si el Problema Persiste

1. **Limpia el build completamente**:
   ```bash
   ./gradlew clean
   rm -rf app/build
   ```

2. **Recompila desde cero**:
   ```bash
   ./gradlew assembleCarsDebug
   ```

3. **Desinstala la app completamente** del dispositivo/emulador

4. **Reinstala**:
   ```bash
   ./gradlew installCarsDebug
   ```

5. **Verifica los logs** para ver si hay más información del error

