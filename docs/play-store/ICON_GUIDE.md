# Guía del Icono de la App - PetPal

## 📱 Requisitos de Google Play Store

- **Tamaño**: 512 x 512 píxeles
- **Formato**: PNG (32 bits con transparencia)
- **Tamaño máximo**: 1 MB
- **Fondo**: Puede ser transparente o sólido

## ✅ Icono Generado

**Archivo**: `app_icon_512x512.png`
- Tamaño: 512 x 512 px
- Formato: PNG
- Basado en el icono adaptativo de la app (huella de perro)

## 📤 Cómo Usar en Google Play Console

1. Ve a Google Play Console
2. Selecciona tu app "PetPal"
3. Ve a **"Store listing"**
4. Busca la sección **"Icono de la app"**
5. Haz clic en **"Subir"**
6. Selecciona el archivo `app_icon_512x512.png`
7. Guarda los cambios

## 🎨 Características del Icono

- **Diseño**: Huella de perro (paw print)
- **Color**: #03DAC5 (turquesa/cyan)
- **Fondo**: Blanco
- **Estilo**: Moderno y minimalista

## 🔄 Si Necesitas Regenerar el Icono

Si necesitas regenerar el icono con diferentes parámetros:

```bash
# Desde el directorio del proyecto
convert app/src/main/res/mipmap-xxxhdpi/ic_launcher.webp \
  -resize 512x512 \
  -background white \
  -alpha remove \
  -alpha off \
  app_icon_512x512.png
```

O con fondo transparente:
```bash
convert app/src/main/res/mipmap-xxxhdpi/ic_launcher.webp \
  -resize 512x512 \
  app_icon_512x512_transparent.png
```

## ⚠️ Notas Importantes

- El icono debe ser claro y reconocible a tamaño pequeño
- Evita texto en el icono (no se lee bien en tamaños pequeños)
- Usa colores contrastantes
- El icono debe verse bien tanto en fondos claros como oscuros

## 📋 Checklist

- [ ] Icono generado (512x512 px)
- [ ] Tamaño del archivo < 1 MB
- [ ] Formato PNG correcto
- [ ] Icono subido a Google Play Console
- [ ] Icono se ve correctamente en la preview

