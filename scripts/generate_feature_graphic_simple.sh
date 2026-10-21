#!/bin/bash
# Script simple para generar Feature Graphic 1024x500

set -e

PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
OUTPUT_FILE="$PROJECT_DIR/feature_graphic_1024x500.png"

echo "🎨 Generando Feature Graphic 1024x500 para PetPal..."

# Crear fondo con gradiente de color
convert -size 1024x500 \
  gradient:"#03DAC5-#018786" \
  "$OUTPUT_FILE"

# Agregar texto (si las fuentes están disponibles)
convert "$OUTPUT_FILE" \
  -fill white -gravity West \
  -font Arial-Bold -pointsize 72 -annotate +100+150 "PetPal" \
  -font Arial -pointsize 32 -annotate +100+230 "Tu asistente para el cuidado" \
  -font Arial -pointsize 32 -annotate +100+270 "de mascotas" \
  -font Arial -pointsize 24 -annotate +100+330 "Recordatorios • Gastos • Sincronización" \
  "$OUTPUT_FILE" 2>/dev/null || {
  # Si falla con fuentes, crear sin texto (puedes agregarlo después con un editor)
  echo "⚠️  No se pudieron agregar fuentes, creando solo el fondo con gradiente"
}

# Forzar RGB
convert "$OUTPUT_FILE" -colorspace RGB -type TrueColor "$OUTPUT_FILE"

# Optimizar
convert "$OUTPUT_FILE" -strip -quality 95 "$OUTPUT_FILE"

# Verificar
if [ -f "$OUTPUT_FILE" ]; then
    SIZE=$(identify -format "%wx%h" "$OUTPUT_FILE")
    FILESIZE=$(ls -lh "$OUTPUT_FILE" | awk '{print $5}')
    echo "✅ Feature Graphic generado!"
    echo "   Archivo: $OUTPUT_FILE"
    echo "   Tamaño: $SIZE"
    echo "   Peso: $FILESIZE"
    echo ""
    echo "💡 Puedes abrir el archivo y agregar texto/imágenes con un editor gráfico"
else
    echo "❌ Error al generar"
    exit 1
fi

