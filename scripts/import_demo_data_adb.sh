#!/bin/bash

# Script para importar datos de demostración usando ADB
# Este script ejecuta comandos directamente en el dispositivo sin usar tests de instrumentación

FLAVOR=$1

if [ -z "$FLAVOR" ]; then
    echo "Uso: ./scripts/import_demo_data_adb.sh <flavor>"
    echo "Flavors disponibles: house, family, pets, cars"
    exit 1
fi

PACKAGE="com.gastonlesbegueris.caretemplate.${FLAVOR}"
ACTIVITY="com.gastonlesbegueris.caretemplate.ui.MainActivity"

echo "=== Importando datos de demostración para ${FLAVOR} ==="
echo "Package: ${PACKAGE}"

# Verificar que el dispositivo esté conectado
if ! adb devices | grep -q "device$"; then
    echo "ERROR: No hay dispositivos conectados"
    exit 1
fi

# Capitalizar la primera letra del flavor para el nombre del método
FLAVOR_CAPITALIZED=$(echo "${FLAVOR:0:1}" | tr '[:lower:]' '[:upper:]')${FLAVOR:1}

# Verificar que la app esté instalada, si no, instalarla
if ! adb shell pm list packages | grep -q "${PACKAGE}"; then
    echo "La app ${PACKAGE} no está instalada. Instalando..."
    ./gradlew :app:install${FLAVOR_CAPITALIZED}Debug 2>&1 | tail -10
    sleep 2
fi

# Verificar que el APK de test esté instalado, si no, instalarlo
if ! adb shell pm list packages | grep -q "${PACKAGE}.test"; then
    echo "El APK de test no está instalado. Instalando..."
    ./gradlew :app:install${FLAVOR_CAPITALIZED}DebugAndroidTest 2>&1 | tail -10
    sleep 2
fi

# Abrir la app
echo "Abriendo la app..."
adb shell am start -n "${PACKAGE}/${ACTIVITY}"

# Esperar a que la app se abra
sleep 3

# Ejecutar el test de instrumentación de forma más suave
echo "Ejecutando importación de datos..."
echo "Ejecutando: import${FLAVOR_CAPITALIZED}Data()"

# Ejecutar el test
adb shell am instrument -w -r \
  -e class com.gastonlesbegueris.caretemplate.SimpleImportDemoData#import${FLAVOR_CAPITALIZED}Data \
  "${PACKAGE}.test/androidx.test.runner.AndroidJUnitRunner" 2>&1 | tee /tmp/import_demo_data.log

# Verificar el resultado
if grep -q "OK" /tmp/import_demo_data.log; then
    echo "✓ Datos importados exitosamente"
    echo "Abre la app para verificar los datos"
else
    echo "⚠ Hubo advertencias, pero los datos pueden haberse importado"
    echo "Revisa los logs en /tmp/import_demo_data.log"
fi

# Mantener la app abierta
echo "Manteniendo la app abierta..."
adb shell am start -n "${PACKAGE}/${ACTIVITY}"
