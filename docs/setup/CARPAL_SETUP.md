# Configuración de CarPal para Publicación

## 📋 Estado Actual

✅ **Completado:**
- Flavor "cars" configurado en `build.gradle`
- Application ID: `com.gastonlesbegueris.caretemplate.cars`
- App Name: "CarPal"
- Icono de la app creado (auto turquesa)
- `google-services.json` configurado

⏳ **Pendiente:**
- AdMob IDs (App ID y Banner ID)
- Generar icono 512x512 para Google Play
- Generar feature graphic 1024x500
- Generar AAB para release

## 🔧 Configuración de AdMob

Necesitas agregar los IDs de AdMob en:
`app/src/cars/res/values/strings.xml`

```xml
<resources>
    <string name="app_name">CarPal</string>
    <string name="admob_app_id">TU_APP_ID_AQUI</string>
    <string name="admob_banner_id">TU_BANNER_ID_AQUI</string>
</resources>
```

### Cómo obtener los IDs:

1. Ve a [AdMob Console](https://apps.admob.com/)
2. Crea una nueva app "CarPal" (si no existe)
3. Obtén el **App ID** (formato: `ca-app-pub-XXXXXXXX~XXXXXXXX`)
4. Crea una unidad de anuncios tipo **Banner**
5. Obtén el **Banner ID** (formato: `ca-app-pub-XXXXXXXX/XXXXXXXX`)

## 🎨 Icono de la App

El icono adaptativo ya está configurado:
- **Foreground**: Auto turquesa (`ic_launcher_foreground.xml`)
- **Background**: Blanco (`ic_launcher_background.xml`)

### Generar icono 512x512:

```bash
./generate_icon_512.sh
```

O manualmente desde Android Studio:
1. Right-click en `app/src/cars/res/mipmap-anydpi-v26/ic_launcher.xml`
2. "New" → "Image Asset"
3. Selecciona "Launcher Icons (Adaptive and Legacy)"
4. Foreground: `ic_launcher_foreground.xml`
5. Background: `ic_launcher_background.xml`
6. Exporta como PNG 512x512

## 📱 Feature Graphic

Genera el feature graphic 1024x500:

```bash
./generate_feature_graphic.sh
```

Luego edita el archivo para cambiar:
- Texto "PetPal" → "CarPal"
- Huella de perro → Icono de auto

O crea uno nuevo específico para CarPal.

## 📦 Generar Release

### 1. Verificar configuración

- [ ] AdMob IDs configurados
- [ ] Version Code y Version Name correctos
- [ ] Keystore configurado (`keystore.properties`)

### 2. Generar AAB

```bash
./generate_release.sh cars
```

O directamente:
```bash
./gradlew bundleCarsRelease
```

El archivo estará en:
`app/build/outputs/bundle/carsRelease/app-cars-release.aab`

## 📤 Publicar en Google Play

1. Crea una nueva app "CarPal" en Google Play Console
2. Completa el Store Listing:
   - Título: "CarPal"
   - Descripción corta: "Tu asistente para el cuidado de vehículos"
   - Descripción completa: (ver `APP_DESCRIPTION.md` adaptado para autos)
   - Icono 512x512
   - Feature graphic 1024x500
   - Screenshots
3. Sube el AAB en "Producción" → "Crear nueva versión"
4. Configura Privacy Policy URL (puede ser la misma que PetPal o una específica)

## 🔄 Próximos Pasos

1. **Obtener AdMob IDs** para CarPal
2. **Agregar IDs** a `app/src/cars/res/values/strings.xml`
3. **Generar icono 512x512** para Google Play
4. **Generar feature graphic** 1024x500
5. **Probar la app** en dispositivo real
6. **Generar AAB** para release
7. **Publicar** en Google Play Console

## 📝 Notas

- CarPal usa el mismo keystore que PetPal (o puedes crear uno nuevo)
- El `google-services.json` ya está configurado para el flavor "cars"
- La app comparte el mismo código base, solo cambia el flavor

