#!/bin/bash
# Script rápido de configuración

set -e

echo "🚀 Configurando entorno de tests..."

cd "$(dirname "$0")"

# Crear entorno virtual si no existe
if [ ! -d "venv" ]; then
    echo "📦 Creando entorno virtual..."
    python3 -m venv venv
fi

# Activar entorno virtual e instalar dependencias
echo "📥 Instalando dependencias de Python..."
source venv/bin/activate
pip install --upgrade pip
pip install -r requirements.txt

echo ""
echo "✅ Entorno Python configurado!"
echo ""
echo "📝 Para instalar Appium, ejecuta:"
echo "   npm install -g appium"
echo "   appium driver install uiautomator2"
echo ""
echo "📝 Para usar el entorno virtual:"
echo "   source venv/bin/activate"
echo ""
echo "📝 Para ejecutar tests:"
echo "   source venv/bin/activate"
echo "   pytest tests/"

