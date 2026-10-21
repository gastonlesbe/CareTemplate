#!/bin/bash
# Script para generar icono 512x512 desde los archivos XML del adaptive icon

set -e

PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
RES_DIR="$PROJECT_DIR/app/src/main/res"
OUTPUT_FILE="$PROJECT_DIR/app_icon_512x512.png"

echo "🎨 Generando icono 512x512 para PetPal..."

# Verificar que ImageMagick esté instalado
if ! command -v convert &> /dev/null; then
    echo "❌ ImageMagick no está instalado. Instálalo con: sudo apt install imagemagick"
    exit 1
fi

# Crear un archivo SVG temporal que combine background y foreground
TEMP_SVG=$(mktemp /tmp/petpal_icon_XXXXXX.svg)

cat > "$TEMP_SVG" << 'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<svg width="512" height="512" xmlns="http://www.w3.org/2000/svg">
  <!-- Fondo blanco -->
  <rect width="512" height="512" fill="#FFFFFF"/>
  
  <!-- Huella de perro - escalada para 512x512 -->
  <g transform="translate(128, 128) scale(4.74)">
    <!-- Dedos superiores -->
    <circle cx="4.5" cy="9.5" r="2.5" fill="#03DAC5"/>
    <circle cx="9" cy="5.5" r="2.5" fill="#03DAC5"/>
    <circle cx="15" cy="5.5" r="2.5" fill="#03DAC5"/>
    <circle cx="19.5" cy="9.5" r="2.5" fill="#03DAC5"/>
    <!-- Palma -->
    <path d="M17.34,14.86c-0.87,-1.02 -1.6,-1.89 -2.48,-2.91 -0.46,-0.54 -1.05,-1.08 -1.75,-1.32 -0.11,-0.04 -0.22,-0.07 -0.33,-0.09 -0.25,-0.04 -0.52,-0.04 -0.78,-0.04s-0.53,0 -0.79,0.05c-0.11,0.02 -0.22,0.05 -0.33,0.09 -0.7,0.24 -1.28,0.78 -1.75,1.32 -0.87,1.02 -1.6,1.89 -2.48,2.91 -1.31,1.31 -2.92,2.76 -2.62,4.79 0.29,1.02 1.02,2.03 2.33,2.32 0.73,0.15 3.06,-0.44 5.54,-0.44h0.18c2.48,0 4.81,0.58 5.54,0.44 1.31,-0.29 2.04,-1.31 2.33,-2.32 0.31,-2.04 -1.3,-3.49 -2.61,-4.8z" fill="#03DAC5"/>
  </g>
</svg>
EOF

# Convertir SVG a PNG 512x512
convert "$TEMP_SVG" -resize 512x512 -background white -flatten "$OUTPUT_FILE"

# Optimizar el tamaño del archivo
convert "$OUTPUT_FILE" -strip -quality 95 "$OUTPUT_FILE"

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
else
    echo "❌ Error al generar el icono"
    exit 1
fi

