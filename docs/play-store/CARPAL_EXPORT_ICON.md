# Cómo Exportar el Icono 512x512 de CarPal desde Image Asset

## ✅ El icono se ve bien en Image Asset (fondo blanco)
## ❌ Pero el PNG generado tiene fondo negro

## 🔧 Solución: Exportar Manualmente

### Opción 1: Desde Image Asset (Recomendado)

1. En **Image Asset**, asegúrate de que:
   - **Foreground Layer**: Vector Drawable → `@drawable/ic_launcher_foreground`
   - **Background Layer**: **Color** → `#FFFFFF` (blanco) ⚠️ **IMPORTANTE: Usa Color, NO Vector Drawable**

2. En la pestaña **"Legacy Icon"**:
   - Marca la casilla **"Generate"**
   - Esto generará los iconos en diferentes tamaños

3. **Exportar el icono de 512x512:**
   - Los iconos se generan en: `app/src/cars/res/mipmap-xxxhdpi/ic_launcher.png`
   - Este archivo debería ser de 192x192, pero puedes redimensionarlo

4. **Redimensionar a 512x512:**
   ```bash
   convert app/src/cars/res/mipmap-xxxhdpi/ic_launcher.png \
     -resize 512x512! \
     -background white \
     -flatten \
     carpal_icon_512x512.png
   ```

### Opción 2: Usar el Script Actualizado

El script `generate_carpal_icon_512.sh` ya genera el icono con fondo blanco. Ejecuta:

```bash
./scripts/generate_carpal_icon_512.sh
```

Esto generará `carpal_icon_512x512.png` con fondo blanco.

### Opción 3: Desde Android Studio - Preview

1. En Image Asset, haz clic derecho en el **preview del icono** (donde se ve bien)
2. Selecciona **"Export Icon"** o **"Save Icon"**
3. Guarda como PNG de 512x512

## ⚠️ Si el PNG sigue con fondo negro

1. **Verifica en Image Asset:**
   - Background Layer debe ser **Color** → `#FFFFFF`
   - NO uses Vector Drawable para el background

2. **Regenera el icono:**
   - Elimina los iconos generados en `app/src/cars/res/mipmap-*/`
   - Vuelve a abrir Image Asset
   - Configura Background como Color blanco
   - Genera nuevamente

3. **Usa el script:**
   - El script `generate_carpal_icon_512.sh` genera correctamente con fondo blanco
   - Úsalo como alternativa

## 📋 Checklist Final

- [ ] Icono se ve bien en Image Asset (fondo blanco)
- [ ] Background Layer configurado como **Color** → `#FFFFFF`
- [ ] PNG exportado o generado con fondo blanco
- [ ] Tamaño: 512x512 píxeles
- [ ] Formato: PNG
- [ ] Listo para subir a Google Play Console
