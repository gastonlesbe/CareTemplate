#!/bin/bash
# Script para generar Feature Graphic 1024x500 para Google Play Store

set -e

PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
OUTPUT_FILE="$PROJECT_DIR/feature_graphic_1024x500.png"

echo "🎨 Generando Feature Graphic 1024x500 para PetPal..."

# Verificar que ImageMagick esté instalado
if ! command -v convert &> /dev/null; then
    echo "❌ ImageMagick no está instalado. Instálalo con: sudo apt install imagemagick"
    exit 1
fi

# Crear un archivo SVG temporal para el feature graphic
TEMP_SVG=$(mktemp /tmp/petpal_feature_XXXXXX.svg)

cat > "$TEMP_SVG" << 'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<svg width="1024" height="500" xmlns="http://www.w3.org/2000/svg">
  <defs>
    <linearGradient id="bgGradient" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" style="stop-color:#03DAC5;stop-opacity:1" />
      <stop offset="100%" style="stop-color:#018786;stop-opacity:1" />
    </linearGradient>
  </defs>
  
  <!-- Fondo con gradiente -->
  <rect width="1024" height="500" fill="url(#bgGradient)"/>
  
  <!-- Huella de perro grande a la izquierda -->
  <g transform="translate(100, 100) scale(8)">
    <!-- Dedos superiores -->
    <circle cx="4.5" cy="9.5" r="2.5" fill="#FFFFFF" opacity="0.9"/>
    <circle cx="9" cy="5.5" r="2.5" fill="#FFFFFF" opacity="0.9"/>
    <circle cx="15" cy="5.5" r="2.5" fill="#FFFFFF" opacity="0.9"/>
    <circle cx="19.5" cy="9.5" r="2.5" fill="#FFFFFF" opacity="0.9"/>
    <!-- Palma -->
    <path d="M17.34,14.86c-0.87,-1.02 -1.6,-1.89 -2.48,-2.91 -0.46,-0.54 -1.05,-1.08 -1.75,-1.32 -0.11,-0.04 -0.22,-0.07 -0.33,-0.09 -0.25,-0.04 -0.52,-0.04 -0.78,-0.04s-0.53,0 -0.79,0.05c-0.11,0.02 -0.22,0.05 -0.33,0.09 -0.7,0.24 -1.28,0.78 -1.75,1.32 -0.87,1.02 -1.6,1.89 -2.48,2.91 -1.31,1.31 -2.92,2.76 -2.62,4.79 0.29,1.02 1.02,2.03 2.33,2.32 0.73,0.15 3.06,-0.44 5.54,-0.44h0.18c2.48,0 4.81,0.58 5.54,0.44 1.31,-0.29 2.04,-1.31 2.33,-2.32 0.31,-2.04 -1.3,-3.49 -2.61,-4.8z" fill="#FFFFFF" opacity="0.9"/>
  </g>
  
  <!-- Texto "PetPal" -->
  <text x="500" y="200" font-family="Arial, sans-serif" font-size="72" font-weight="bold" fill="#FFFFFF">
    PetPal
  </text>
  
  <!-- Subtítulo -->
  <text x="500" y="260" font-family="Arial, sans-serif" font-size="32" fill="#FFFFFF" opacity="0.95">
    Tu asistente para el cuidado de mascotas
  </text>
  
  <!-- Características -->
  <g transform="translate(500, 300)">
    <text x="0" y="0" font-family="Arial, sans-serif" font-size="24" fill="#FFFFFF" opacity="0.9">
      📅 Recordatorios  •  💰 Gastos  •  ☁️ Sincronización
    </text>
  </g>
</svg>
EOF

# Convertir SVG a PNG 1024x500 con colores RGB
convert "$TEMP_SVG" -resize 1024x500 -colorspace RGB -type TrueColor "$OUTPUT_FILE"

# Optimizar el tamaño del archivo manteniendo colores
convert "$OUTPUT_FILE" -strip -quality 95 -colorspace RGB "$OUTPUT_FILE"

# Limpiar archivo temporal
rm -f "$TEMP_SVG"

# Verificar el resultado
if [ -f "$OUTPUT_FILE" ]; then
    SIZE=$(identify -format "%wx%h" "$OUTPUT_FILE")
    FILESIZE=$(ls -lh "$OUTPUT_FILE" | awk '{print $5}')
    echo "✅ Feature Graphic generado exitosamente!"
    echo "   Archivo: $OUTPUT_FILE"
    echo "   Tamaño: $SIZE"
    echo "   Peso: $FILESIZE"
else
    echo "❌ Error al generar el feature graphic"
    exit 1
fi

