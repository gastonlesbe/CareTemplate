#!/bin/bash
# Script para ejecutar tests del flavor Pets

echo "🚀 Ejecutando tests para CarePal Pets..."

# Verificar que Appium esté corriendo
if ! curl -s http://localhost:4723/status > /dev/null; then
    echo "❌ Error: Appium no está corriendo en http://localhost:4723"
    echo "   Por favor, inicia Appium con: appium"
    exit 1
fi

# Verificar que haya un dispositivo conectado
if ! adb devices | grep -q "device$"; then
    echo "❌ Error: No hay dispositivos Android conectados"
    echo "   Por favor, conecta un dispositivo o inicia un emulador"
    exit 1
fi

# Verificar que la app esté instalada
if ! adb shell pm list packages | grep -q "com.gastonlesbegueris.caretemplate.pets"; then
    echo "⚠️  Advertencia: CarePal Pets no está instalada"
    echo "   Instalando desde: app/build/outputs/apk/petsDebug/app-pets-debug.apk"
    
    if [ -f "app/build/outputs/apk/petsDebug/app-pets-debug.apk" ]; then
        adb install -r app/build/outputs/apk/petsDebug/app-pets-debug.apk
    else
        echo "❌ Error: APK no encontrado. Por favor, compila la app primero"
        exit 1
    fi
fi

# Ejecutar tests
echo "📱 Ejecutando tests..."
pytest tests/test_pets_flavor.py -v --html=reports/pets_test_report.html --self-contained-html

echo "✅ Tests completados. Reporte en: reports/pets_test_report.html"
