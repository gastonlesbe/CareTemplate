# Guía para Generar Release de PetPal

## Recomendación: AAB (Android App Bundle)

**Para Google Play Store: Usa AAB** ✅
- Google Play requiere AAB desde agosto 2021
- Archivos más pequeños para los usuarios
- Mejor optimización automática

**APK solo para:**
- Testing interno
- Distribución fuera de Play Store
- Instalación directa

## Pasos para Generar Release

### 1. Configurar Firma (Keystore)

Si no tienes un keystore, créalo:

```bash
keytool -genkey -v -keystore petpal-release-key.jks \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -alias petpal-key \
  -storepass [TU_PASSWORD] \
  -keypass [TU_PASSWORD]
```

**⚠️ IMPORTANTE:** Guarda este archivo y las contraseñas de forma segura. Sin ellos no podrás actualizar la app.

### 2. Configurar signing en build.gradle

Crea `app/keystore.properties`:
```properties
storeFile=../petpal-release-key.jks
storePassword=TU_PASSWORD
keyAlias=petpal-key
keyPassword=TU_PASSWORD
```

Agrega al `app/build.gradle`:
```gradle
android {
    // ... configuración existente ...
    
    signingConfigs {
        release {
            def keystorePropertiesFile = rootProject.file("keystore.properties")
            def keystoreProperties = new Properties()
            if (keystorePropertiesFile.exists()) {
                keystoreProperties.load(new FileInputStream(keystorePropertiesFile))
                storeFile file(keystoreProperties['storeFile'])
                storePassword keystoreProperties['storePassword']
                keyAlias keystoreProperties['keyAlias']
                keyPassword keystoreProperties['keyPassword']
            }
        }
    }
    
    buildTypes {
        release {
            signingConfig signingConfigs.release
            minifyEnabled false
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
    }
}
```

### 3. Generar AAB (Recomendado para Play Store)

```bash
./gradlew bundlePetsRelease
```

El archivo estará en:
`app/build/outputs/bundle/petsRelease/app-pets-release.aab`

### 4. Generar APK (Para testing o distribución externa)

```bash
./gradlew assemblePetsRelease
```

El archivo estará en:
`app/build/outputs/apk/pets/release/app-pets-release.apk`

## Verificar antes de Publicar

- [ ] Version Code incrementado (actualmente: 1)
- [ ] Version Name correcta (actualmente: "1.0")
- [ ] App ID de AdMob configurado correctamente
- [ ] App probada en dispositivo real
- [ ] Permisos necesarios declarados en AndroidManifest
- [ ] Privacy Policy URL configurada (requerida por Google Play)

## Subir a Google Play Console

1. Ve a [Google Play Console](https://play.google.com/console)
2. Crea una nueva app "PetPal"
3. Sube el archivo `.aab` generado
4. Completa la información de la store listing
5. Configura la política de privacidad (requerida)
6. Envía para revisión

## Notas

- El AAB será convertido automáticamente a APK por Google Play
- Los usuarios solo descargarán lo necesario para su dispositivo
- Puedes generar ambos formatos, pero solo sube AAB a Play Store

