#!/bin/bash
# Script para solucionar el error 404 de app-ads.txt
# Crea un repositorio gastonlesbe.github.io con el archivo app-ads.txt

set -e

GITHUB_USER="gastonlesbe"
REPO_NAME="${GITHUB_USER}.github.io"
APP_ADS_FILE="/home/gaston/AndroidStudioProjects/Caretemplate/app-ads.txt"
TEMP_DIR="$HOME/${REPO_NAME}"

echo "🔧 Solución al Error 404 de app-ads.txt"
echo "======================================"
echo ""
echo "Este script creará un repositorio especial llamado:"
echo "  ${REPO_NAME}"
echo ""
echo "Este repositorio servirá archivos desde la raíz del dominio:"
echo "  https://${GITHUB_USER}.github.io/"
echo ""

# Verificar que el archivo app-ads.txt existe
if [ ! -f "$APP_ADS_FILE" ]; then
    echo "❌ Error: No se encontró app-ads.txt en:"
    echo "   $APP_ADS_FILE"
    exit 1
fi

echo "✅ Archivo app-ads.txt encontrado"
echo ""
echo "📄 Contenido del archivo:"
cat "$APP_ADS_FILE"
echo ""
echo ""

# Verificar si el repositorio ya existe localmente
if [ -d "$TEMP_DIR" ]; then
    echo "⚠️  Ya existe un directorio: $TEMP_DIR"
    read -p "¿Deseas eliminarlo y empezar de nuevo? (s/n): " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Ss]$ ]]; then
        echo "🗑️  Eliminando directorio existente..."
        rm -rf "$TEMP_DIR"
    else
        echo "❌ Operación cancelada"
        exit 1
    fi
fi

echo ""
echo "📝 Pasos que se realizarán:"
echo ""
echo "1. Crear directorio local: $TEMP_DIR"
echo "2. Inicializar repositorio Git"
echo "3. Copiar app-ads.txt"
echo "4. Hacer commit inicial"
echo ""
echo "⚠️  IMPORTANTE: Debes crear el repositorio en GitHub primero:"
echo ""
echo "   Ve a: https://github.com/new"
echo "   Nombre: ${REPO_NAME}"
echo "   Visibilidad: Público"
echo "   NO marques ninguna opción (sin README, .gitignore, licencia)"
echo "   Haz clic en 'Create repository'"
echo ""
read -p "¿Ya creaste el repositorio en GitHub? (s/n): " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Ss]$ ]]; then
    echo ""
    echo "⏸️  Por favor, crea el repositorio primero y luego ejecuta este script de nuevo."
    echo ""
    echo "URL para crear el repositorio:"
    echo "  https://github.com/new"
    echo ""
    exit 0
fi

echo ""
echo "🚀 Iniciando configuración..."
echo ""

# Crear directorio
mkdir -p "$TEMP_DIR"
cd "$TEMP_DIR"

# Inicializar Git
echo "📦 Inicializando repositorio Git..."
git init
git branch -M main

# Copiar app-ads.txt
echo "📋 Copiando app-ads.txt..."
cp "$APP_ADS_FILE" .

# Agregar y commitear
echo "💾 Haciendo commit..."
git add app-ads.txt
git commit -m "Add app-ads.txt for AdMob verification"

# Agregar remote
echo "🔗 Configurando remote..."
git remote add origin "https://github.com/${GITHUB_USER}/${REPO_NAME}.git" || {
    echo "⚠️  El remote ya existe, actualizando..."
    git remote set-url origin "https://github.com/${GITHUB_USER}/${REPO_NAME}.git"
}

# Push
echo "📤 Subiendo a GitHub..."
echo ""
read -p "¿Deseas hacer push ahora? (s/n): " -n 1 -r
echo
if [[ $REPLY =~ ^[Ss]$ ]]; then
    git push -u origin main
    echo ""
    echo "✅ Push completado!"
else
    echo ""
    echo "⏸️  Push cancelado. Puedes hacerlo manualmente después:"
    echo "   cd $TEMP_DIR"
    echo "   git push -u origin main"
fi

echo ""
echo "✅ Configuración local completada!"
echo ""
echo "📋 Próximos pasos:"
echo ""
echo "1. Habilita GitHub Pages:"
echo "   https://github.com/${GITHUB_USER}/${REPO_NAME}/settings/pages"
echo "   - Source: Deploy from a branch"
echo "   - Branch: main"
echo "   - Folder: / (root)"
echo "   - Save"
echo ""
echo "2. Espera 2-3 minutos y verifica:"
echo "   https://${GITHUB_USER}.github.io/app-ads.txt"
echo ""
echo "3. Actualiza Google Play Console:"
echo "   - Ve a: https://play.google.com/console"
echo "   - Configuración de la app → Detalles de la app"
echo "   - Sitio web: https://${GITHUB_USER}.github.io"
echo "   - Guarda"
echo ""
echo "4. Verifica en AdMob:"
echo "   - Ve a: https://apps.admob.com/"
echo "   - Configuración → app-ads.txt"
echo "   - Haz clic en 'Verificar'"
echo ""
echo "📁 El repositorio local está en:"
echo "   $TEMP_DIR"
echo ""

