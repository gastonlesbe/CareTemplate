#!/bin/bash
# Script de instalación para tests con pytest y Appium en Ubuntu

set -e

echo "🚀 Instalando dependencias para tests automatizados..."

# Colores para output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 1. Verificar Python
echo -e "${YELLOW}1. Verificando Python...${NC}"
if ! command -v python3 &> /dev/null; then
    echo "❌ Python3 no encontrado. Instalando..."
    sudo apt update
    sudo apt install -y python3 python3-pip
else
    echo -e "${GREEN}✓ Python3 ya está instalado${NC}"
fi

# 2. Verificar Node.js
echo -e "${YELLOW}2. Verificando Node.js...${NC}"
if ! command -v node &> /dev/null; then
    echo "❌ Node.js no encontrado. Instalando..."
    curl -fsSL https://deb.nodesource.com/setup_18.x | sudo -E bash -
    sudo apt install -y nodejs
else
    echo -e "${GREEN}✓ Node.js ya está instalado: $(node --version)${NC}"
fi

# 3. Verificar Java (necesario para Appium con Android)
echo -e "${YELLOW}3. Verificando Java...${NC}"
if ! command -v java &> /dev/null; then
    echo "❌ Java no encontrado. Instalando OpenJDK 11..."
    sudo apt update
    sudo apt install -y openjdk-11-jdk
    export JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64
    echo "export JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64" >> ~/.bashrc
else
    echo -e "${GREEN}✓ Java ya está instalado${NC}"
fi

# 4. Verificar Android SDK
echo -e "${YELLOW}4. Verificando Android SDK...${NC}"
if ! command -v adb &> /dev/null; then
    echo "❌ Android SDK no encontrado. Instalando..."
    sudo apt update
    sudo apt install -y android-sdk android-sdk-platform-tools
    export ANDROID_HOME=/usr/lib/android-sdk
    export PATH=$PATH:$ANDROID_HOME/platform-tools:$ANDROID_HOME/tools
    echo "export ANDROID_HOME=/usr/lib/android-sdk" >> ~/.bashrc
    echo "export PATH=\$PATH:\$ANDROID_HOME/platform-tools:\$ANDROID_HOME/tools" >> ~/.bashrc
else
    echo -e "${GREEN}✓ Android SDK ya está instalado${NC}"
    if [ -z "$ANDROID_HOME" ]; then
        export ANDROID_HOME=/usr/lib/android-sdk
        echo "export ANDROID_HOME=/usr/lib/android-sdk" >> ~/.bashrc
    fi
fi

# 5. Instalar Appium globalmente
echo -e "${YELLOW}5. Instalando Appium...${NC}"
if ! command -v appium &> /dev/null; then
    echo "Instalando Appium Server..."
    sudo npm install -g appium
    
    echo "Instalando driver de UiAutomator2 para Android..."
    appium driver install uiautomator2
    
    echo -e "${GREEN}✓ Appium instalado correctamente${NC}"
else
    echo -e "${GREEN}✓ Appium ya está instalado${NC}"
fi

# 6. Instalar dependencias de Python
echo -e "${YELLOW}6. Instalando dependencias de Python...${NC}"
cd "$(dirname "$0")"
pip3 install --user -r requirements.txt
echo -e "${GREEN}✓ Dependencias de Python instaladas${NC}"

# 7. Verificar instalación
echo -e "${YELLOW}7. Verificando instalación...${NC}"
echo ""
echo "Versiones instaladas:"
python3 --version
node --version
npm --version
java -version 2>&1 | head -1
adb --version | head -1
appium --version 2>&1 | head -1 || echo "Appium: Instalado pero necesita reiniciar terminal"

echo ""
echo -e "${GREEN}✅ Instalación completada!${NC}"
echo ""
echo "📝 Próximos pasos:"
echo "1. Recarga tu terminal: source ~/.bashrc"
echo "2. Conecta un dispositivo Android o inicia un emulador"
echo "3. Verifica la conexión: adb devices"
echo "4. Inicia Appium Server: appium"
echo "5. En otra terminal, ejecuta los tests: pytest tests/"
