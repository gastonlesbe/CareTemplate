#!/bin/bash
# Script para generar Feature Graphic 1024x500 para Google Play Store
# Versión que crea directamente con ImageMagick

set -e

PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
OUTPUT_FILE="$PROJECT_DIR/feature_graphic_1024x500.png"

echo "🎨 Generando Feature Graphic 1024x500 para PetPal..."

# Verificar que ImageMagick esté instalado
if ! command -v convert &> /dev/null; then
    echo "❌ ImageMagick no está instalado. Instálalo con: sudo apt install imagemagick"
    exit 1
fi

# Crear fondo con gradiente
convert -size 1024x500 \
  \( xc:"#03DAC5" xc:"#018786" +append \) \
  -resize 1024x500\! \
  gradient.png

# Crear huella de perro en un archivo temporal más grande
convert -size 200x200 xc:transparent \
  -fill "#FFFFFF" -opacity 90% \
  -draw "circle 50,80 50,70" \
  -draw "circle 80,50 80,40" \
  -draw "circle 120,50 120,40" \
  -draw "circle 150,80 150,70" \
  -draw "path 'M 130,100 A 20,20 0 0,1 110,100 A 15,15 0 0,1 90,100 A 20,20 0 0,1 70,100 A 25,25 0 0,1 100,140 A 25,25 0 0,1 130,100 Z'" \
  paw_temp.png

# Mejor método: crear directamente el feature graphic
convert -size 1024x500 \
  gradient:\(#03DAC5-#018786\) \
  -fill white -font Arial-Bold -pointsize 72 -gravity West -annotate +500+150 "PetPal" \
  -fill white -font Arial -pointsize 32 -gravity West -annotate +500+220 "Tu asistente para el cuidado de mascotas" \
  -fill white -font Arial -pointsize 24 -gravity West -annotate +500+280 "📅 Recordatorios  •  💰 Gastos  •  ☁️ Sincronización" \
  "$OUTPUT_FILE"

# Agregar huella de perro (simplificada)
convert "$OUTPUT_FILE" \
  \( -size 200x200 xc:none \
     -fill white -opacity 90% \
     -draw "circle 100,100 100,85" \
     -draw "circle 100,100 100,75" \
     -draw "circle 100,100 100,65" \
     -draw "circle 100,100 100,55" \
     -draw "ellipse 100,120 40,30 0,360" \) \
  -geometry +100+150 -composite \
  "$OUTPUT_FILE"

# Optimizar
convert "$OUTPUT_FILE" -strip -quality 95 "$OUTPUT_FILE"

# Limpiar temporales
rm -f gradient.png paw_temp.png

# Verificar el resultado
if [ -f "$OUTPUT_FILE" ]; then
    SIZE=$(identify -format "%wx%h" "$OUTPUT_FILE")
    FILESIZE=$(ls -lh "$OUTPUT_FILE" | awk '{print $5}')
    COLORSPACE=$(identify -format "%[colorspace]" "$OUTPUT_FILE")
    echo "✅ Feature Graphic generado exitosamente!"
    echo "   Archivo: $OUTPUT_FILE"
    echo "   Tamaño: $SIZE"
    echo "   Peso: $FILESIZE"
    echo "   Colorspace: $COLORSPACE"
else
    echo "❌ Error al generar el feature graphic"
    exit 1
fi

