# Problema con Icono de CarPal en Google Play

## ❌ Error de Google Play

```
We found an issue in the following area(s):
Title (es-419): "CarPal - Gestión Vehículos"
Launch / On Device Icon: Please see attached screenshot LAUNCHER_ICON-334.png
```

## 🔍 Análisis del Problema

El error "Launch / On Device Icon" generalmente significa que:
1. El icono subido no coincide con el icono adaptativo de la app
2. El icono tiene problemas de formato o calidad
3. El icono no se ve correctamente en diferentes tamaños

## ✅ Soluciones

### Opción 1: Generar desde Android Studio (RECOMENDADO)

1. Abre Android Studio
2. Ve a `app/src/cars/res/mipmap-anydpi-v26/ic_launcher.xml`
3. Click derecho → **"New"** → **"Image Asset"**
4. Selecciona **"Launcher Icons (Adaptive and Legacy)"**
5. Configura:
   - **Foreground**: `ic_launcher_foreground.xml`
   - **Background**: `ic_launcher_background.xml`
6. En la pestaña **"Legacy Icon"**, marca **"Generate"**
7. Exporta el icono de **512x512** desde la carpeta generada

### Opción 2: Usar el icono generado por Android Build

Cuando compilas la app, Android genera automáticamente iconos en diferentes tamaños:

```bash
# Compilar la app (esto genera los iconos)
./gradlew assembleCarsDebug

# El icono de 512x512 estará en:
app/build/generated/res/pngs/cars/debug/mipmap-xxxhdpi/ic_launcher.png
```

Luego redimensiona a 512x512:
```bash
convert app/build/generated/res/pngs/cars/debug/mipmap-xxxhdpi/ic_launcher.png \
  -resize 512x512! \
  carpal_icon_512x512.png
```

### Opción 3: Verificar que el icono coincida visualmente

El icono debe:
- Tener el mismo diseño que el icono adaptativo
- Mostrar el auto turquesa sobre fondo blanco
- Estar centrado y bien proporcionado
- Verse claro a tamaño pequeño

## 📋 Checklist

- [ ] Icono generado desde Android Studio o desde build
- [ ] Tamaño: exactamente 512x512 píxeles
- [ ] Formato: PNG
- [ ] El icono coincide visualmente con el icono de la app
- [ ] El auto está centrado y bien visible
- [ ] Fondo blanco sólido
- [ ] Color del auto: #03DAC5 (turquesa)

## 🔄 Próximos Pasos

1. Genera el icono usando la Opción 1 (Android Studio)
2. Sube el nuevo icono a Google Play Console
3. Verifica que el icono se vea correctamente en la preview
4. Si el error persiste, verifica que el icono adaptativo de la app esté correctamente configurado
