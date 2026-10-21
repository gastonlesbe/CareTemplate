#!/bin/bash
# Script para generar release de PetPal

set -e

FLAVOR=${1:-pets}
BUILD_TYPE=${2:-release}

echo "🚀 Generando release para flavor: $FLAVOR"
echo "📦 Tipo: $BUILD_TYPE"
echo ""

# Verificar que estamos en el directorio correcto
if [ ! -f "app/build.gradle" ]; then
    echo "❌ Error: Ejecuta este script desde la raíz del proyecto"
    exit 1
fi

# Opciones disponibles
echo "Opciones disponibles:"
echo "  - AAB (Android App Bundle) - Para Google Play Store"
echo "  - APK - Para testing o distribución externa"
echo ""

read -p "¿Qué quieres generar? (aab/apk): " choice

case $choice in
    aab|AAB)
        echo "📦 Generando AAB (Android App Bundle)..."
        ./gradlew bundle${FLAVOR^}Release
        echo ""
        echo "✅ AAB generado en:"
        echo "   app/build/outputs/bundle/${FLAVOR}Release/app-${FLAVOR}-release.aab"
        echo ""
        echo "📤 Este archivo es el que debes subir a Google Play Console"
        ;;
    apk|APK)
        echo "📦 Generando APK..."
        ./gradlew assemble${FLAVOR^}Release
        echo ""
        echo "✅ APK generado en:"
        echo "   app/build/outputs/apk/${FLAVOR}/release/app-${FLAVOR}-release.apk"
        echo ""
        echo "📤 Este archivo puedes instalarlo directamente en dispositivos"
        ;;
    *)
        echo "❌ Opción inválida. Usa 'aab' o 'apk'"
        exit 1
        ;;
esac

echo ""
echo "📝 Verifica antes de publicar:"
echo "  - Version Code y Version Name en app/build.gradle"
echo "  - App ID de AdMob configurado"
echo "  - App probada en dispositivo real"
echo "  - Privacy Policy URL configurada (requerida por Google Play)"

