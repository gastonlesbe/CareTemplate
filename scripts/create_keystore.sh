#!/bin/bash
# Script para crear el keystore de release para PetPal

set -e

KEYSTORE_NAME="petpal-release-key.jks"
KEY_ALIAS="petpal-key"
VALIDITY_DAYS=10000

echo "🔐 Creando keystore para PetPal Release"
echo ""

# Verificar si el keystore ya existe
if [ -f "$KEYSTORE_NAME" ]; then
    echo "⚠️  El keystore $KEYSTORE_NAME ya existe."
    read -p "¿Quieres sobrescribirlo? (s/N): " overwrite
    if [[ ! $overwrite =~ ^[Ss]$ ]]; then
        echo "Operación cancelada."
        exit 0
    fi
    rm "$KEYSTORE_NAME"
fi

# Solicitar información
echo "Por favor, completa la siguiente información:"
read -sp "Contraseña del keystore (mínimo 6 caracteres): " STORE_PASSWORD
echo ""
read -sp "Confirma la contraseña del keystore: " STORE_PASSWORD_CONFIRM
echo ""

if [ "$STORE_PASSWORD" != "$STORE_PASSWORD_CONFIRM" ]; then
    echo "❌ Las contraseñas no coinciden"
    exit 1
fi

if [ ${#STORE_PASSWORD} -lt 6 ]; then
    echo "❌ La contraseña debe tener al menos 6 caracteres"
    exit 1
fi

read -sp "Contraseña de la clave (puede ser la misma): " KEY_PASSWORD
echo ""

if [ -z "$KEY_PASSWORD" ]; then
    KEY_PASSWORD="$STORE_PASSWORD"
fi

read -p "Nombre completo (CN): " CN
read -p "Organización (O): " ORG
read -p "Unidad organizacional (OU): " OU
read -p "Ciudad (L): " CITY
read -p "Estado/Provincia (ST): " STATE
read -p "Código de país (C) [AR]: " COUNTRY

COUNTRY=${COUNTRY:-AR}

echo ""
echo "📝 Creando keystore..."

keytool -genkey -v \
  -keystore "$KEYSTORE_NAME" \
  -alias "$KEY_ALIAS" \
  -keyalg RSA \
  -keysize 2048 \
  -validity $VALIDITY_DAYS \
  -storepass "$STORE_PASSWORD" \
  -keypass "$KEY_PASSWORD" \
  -dname "CN=$CN, OU=$OU, O=$ORG, L=$CITY, ST=$STATE, C=$COUNTRY"

echo ""
echo "✅ Keystore creado: $KEYSTORE_NAME"
echo ""

# Crear archivo keystore.properties
KEYSTORE_PROPERTIES="keystore.properties"
if [ ! -f "$KEYSTORE_PROPERTIES" ]; then
    cat > "$KEYSTORE_PROPERTIES" << EOF
storeFile=$KEYSTORE_NAME
storePassword=$STORE_PASSWORD
keyAlias=$KEY_ALIAS
keyPassword=$KEY_PASSWORD
EOF
    chmod 600 "$KEYSTORE_PROPERTIES"
    echo "✅ Archivo keystore.properties creado"
    echo ""
    echo "⚠️  IMPORTANTE:"
    echo "   - Guarda este keystore y las contraseñas de forma segura"
    echo "   - Sin ellos NO podrás actualizar la app en Google Play"
    echo "   - El archivo keystore.properties NO se subirá a git (está en .gitignore)"
else
    echo "⚠️  El archivo keystore.properties ya existe. Actualízalo manualmente con:"
    echo "   storeFile=$KEYSTORE_NAME"
    echo "   storePassword=[TU_PASSWORD]"
    echo "   keyAlias=$KEY_ALIAS"
    echo "   keyPassword=[TU_PASSWORD]"
fi

echo ""
echo "🎉 Configuración completada!"
echo ""
echo "Ahora puedes generar el release con:"
echo "  ./gradlew bundlePetsRelease    (para AAB)"
echo "  ./gradlew assemblePetsRelease (para APK)"

