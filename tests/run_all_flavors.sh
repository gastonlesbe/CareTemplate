#!/bin/bash
# Script para ejecutar tests de todos los flavors

echo "🚀 Ejecutando tests para todos los flavors..."

# Crear directorio de reportes si no existe
mkdir -p reports

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

# Ejecutar tests para cada flavor
echo "📱 Ejecutando tests para Family..."
pytest tests/test_family_flavor.py -v --html=reports/family_test_report.html --self-contained-html || true

echo "📱 Ejecutando tests para Pets..."
pytest tests/test_pets_flavor.py -v --html=reports/pets_test_report.html --self-contained-html || true

echo "📱 Ejecutando tests para Cars..."
pytest tests/test_cars_flavor.py -v --html=reports/cars_test_report.html --self-contained-html || true

echo "📱 Ejecutando tests para House..."
pytest tests/test_house_flavor.py -v --html=reports/house_test_report.html --self-contained-html || true

echo "✅ Todos los tests completados. Reportes en: reports/"
