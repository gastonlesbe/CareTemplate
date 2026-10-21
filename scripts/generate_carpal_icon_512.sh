#!/bin/bash
# Script para generar icono 512x512 para CarPal desde los archivos XML del adaptive icon

set -e

PROJECT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
OUTPUT_FILE="$PROJECT_DIR/carpal_icon_512x512.png"

echo "🚗 Generando icono 512x512 para CarPal..."

# Verificar que ImageMagick esté instalado
if ! command -v convert &> /dev/null; then
    echo "❌ ImageMagick no está instalado. Instálalo con: sudo apt install imagemagick"
    exit 1
fi

# Crear un archivo SVG temporal que combine background y foreground
TEMP_SVG=$(mktemp /tmp/carpal_icon_XXXXXX.svg)

cat > "$TEMP_SVG" << 'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<svg width="512" height="512" xmlns="http://www.w3.org/2000/svg">
  <defs>
    <!-- Gradiente sutil para forzar TrueColor (invisible al ojo) -->
    <linearGradient id="bgGrad" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" style="stop-color:#FFFFFF;stop-opacity:1" />
      <stop offset="100%" style="stop-color:#FEFFFF;stop-opacity:1" />
    </linearGradient>
  </defs>
  <!-- Fondo blanco con gradiente sutil -->
  <rect width="512" height="512" fill="url(#bgGrad)"/>
  
  <!-- Auto - escalado y centrado para 512x512 -->
  <!-- El auto original está en un viewport de 108x108, lo escalamos más para que sea más visible -->
  <!-- translate centra el auto (256, 256 es el centro de 512x512) -->
  <!-- scale(14) hace el auto mucho más grande y visible -->
  <g transform="translate(256, 256) scale(14) translate(-12, -12)">
    <!-- Cuerpo del auto (path original del XML) -->
    <!-- El path del auto tiene un tamaño aproximado de 24x24, con translate(-12, -12) lo centramos -->
    <path
        fill="#03DAC5"
        d="M18.92,6.01C18.72,5.42 18.16,5 17.5,5h-11c-0.66,0 -1.21,0.42 -1.42,1.01L3,12v8c0,0.55 0.45,1 1,1h1c0.55,0 1,-0.45 1,-1v-1h12v1c0,0.55 0.45,1 1,1h1c0.55,0 1,-0.45 1,-1v-8l-2.08,-5.99zM6.5,16c-0.83,0 -1.5,-0.67 -1.5,-1.5S5.67,13 6.5,13s1.5,0.67 1.5,1.5S7.33,16 6.5,16zM17.5,16c-0.83,0 -1.5,-0.67 -1.5,-1.5s0.67,-1.5 1.5,-1.5 1.5,0.67 1.5,1.5 -0.67,1.5 -1.5,1.5zM5,11l1.5,-4.5h11L19,11H5z"/>
  </g>
</svg>
EOF

# Convertir SVG a PNG 512x512 forzando TrueColor de 32 bits
# El gradiente sutil en el SVG fuerza TrueColor
convert "$TEMP_SVG" -resize 512x512! \
  -type TrueColor \
  -depth 8 \
  -define png:format=png32 \
  -colorspace sRGB \
  "$OUTPUT_FILE"

# Verificar y forzar TrueColor si es necesario
TYPE=$(identify -format "%r" "$OUTPUT_FILE" 2>/dev/null || echo "Unknown")
if [[ "$TYPE" == *"Palette"* ]] || [[ "$TYPE" == *"PseudoClass"* ]]; then
  echo "⚠️  Forzando TrueColor..."
  convert "$OUTPUT_FILE" \
    -type TrueColor \
    -depth 8 \
    -define png:format=png32 \
    "$OUTPUT_FILE"
fi

# Optimizar manteniendo TrueColor
convert "$OUTPUT_FILE" \
  -type TrueColor \
  -depth 8 \
  -strip \
  -quality 95 \
  "$OUTPUT_FILE"

# Limpiar archivo temporal
rm -f "$TEMP_SVG"

# Verificar el resultado
if [ -f "$OUTPUT_FILE" ]; then
    SIZE=$(identify -format "%wx%h" "$OUTPUT_FILE")
    FILESIZE=$(ls -lh "$OUTPUT_FILE" | awk '{print $5}')
    echo "✅ Icono generado exitosamente!"
    echo "   Archivo: $OUTPUT_FILE"
    echo "   Tamaño: $SIZE"
    echo "   Peso: $FILESIZE"
    echo ""
    echo "📤 Listo para subir a Google Play Console"
else
    echo "❌ Error al generar el icono"
    exit 1
fi
