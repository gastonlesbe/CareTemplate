#!/bin/bash
# Script para generar Feature Graphic 1024x500 para CarPal en Google Play Store

set -e

PROJECT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
OUTPUT_FILE="$PROJECT_DIR/carpal_feature_graphic_1024x500.png"

echo "🚗 Generando Feature Graphic 1024x500 para CarPal..."

# Verificar que ImageMagick esté instalado
if ! command -v convert &> /dev/null; then
    echo "❌ ImageMagick no está instalado. Instálalo con: sudo apt install imagemagick"
    exit 1
fi

# Crear un archivo SVG temporal para el feature graphic
TEMP_SVG=$(mktemp /tmp/carpal_feature_XXXXXX.svg)

cat > "$TEMP_SVG" << 'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<svg width="1024" height="500" xmlns="http://www.w3.org/2000/svg">
  <!-- Fondo blanco -->
  <rect width="1024" height="500" fill="#FFFFFF"/>
  
  <defs>
    <!-- Color turquesa/cyan del icono - más cyan, menos verde -->
    <style>
      .turquoise { fill: rgb(0, 188, 212); }
    </style>
  </defs>
  
  <!-- Auto grande a la izquierda -->
  <g transform="translate(150, 150) scale(12)">
    <!-- Cuerpo del auto -->
    <path
        class="turquoise"
        d="M18.92,6.01C18.72,5.42 18.16,5 17.5,5h-11c-0.66,0 -1.21,0.42 -1.42,1.01L3,12v8c0,0.55 0.45,1 1,1h1c0.55,0 1,-0.45 1,-1v-1h12v1c0,0.55 0.45,1 1,1h1c0.55,0 1,-0.45 1,-1v-8l-2.08,-5.99zM6.5,16c-0.83,0 -1.5,-0.67 -1.5,-1.5S5.67,13 6.5,13s1.5,0.67 1.5,1.5S7.33,16 6.5,16zM17.5,16c-0.83,0 -1.5,-0.67 -1.5,-1.5s0.67,-1.5 1.5,-1.5 1.5,0.67 1.5,1.5 -0.67,1.5 -1.5,1.5zM5,11l1.5,-4.5h11L19,11H5z"/>
  </g>
  
  <!-- Texto "CarPal" -->
  <text x="550" y="200" font-family="Arial, sans-serif" font-size="64" font-weight="bold" class="turquoise">
    CarPal
  </text>
  
  <!-- Subtítulo "Gestión Vehículos" -->
  <text x="550" y="260" font-family="Arial, sans-serif" font-size="36" class="turquoise">
    Gestión Vehículos
  </text>
  
  <!-- Características en dos líneas para que quepa todo -->
  <text x="550" y="320" font-family="Arial, sans-serif" font-size="22" class="turquoise">
    📅 Recordatorios  •  💰 Gastos
  </text>
  <text x="550" y="360" font-family="Arial, sans-serif" font-size="22" class="turquoise">
    ☁️ Sincronización
  </text>
</svg>
EOF

# Convertir SVG a PNG 1024x500 con colores RGB preservados
convert "$TEMP_SVG" -resize 1024x500! -colorspace sRGB -type TrueColor "$OUTPUT_FILE"

# Optimizar el tamaño del archivo manteniendo colores sRGB
convert "$OUTPUT_FILE" -strip -quality 95 -colorspace sRGB "$OUTPUT_FILE"

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
    echo ""
    echo "📤 Listo para subir a Google Play Console"
else
    echo "❌ Error al generar el feature graphic"
    exit 1
fi
