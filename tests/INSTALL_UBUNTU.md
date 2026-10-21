# Guía de Instalación - Ubuntu Linux

Esta guía te ayudará a instalar todo lo necesario para ejecutar los tests automatizados con pytest y Appium en Ubuntu Linux.

## 1. Instalar Python y pip

```bash
# Actualizar el sistema
sudo apt update

# Instalar Python 3 y pip
sudo apt install -y python3 python3-pip python3-venv

# Verificar instalación
python3 --version
pip3 --version
```

## 2. Instalar Node.js y npm (para Appium)

```bash
# Instalar Node.js y npm
sudo apt install -y nodejs npm

# Verificar instalación
node --version
npm --version

# Si las versiones son muy antiguas, puedes instalar una versión más reciente:
# curl -fsSL https://deb.nodesource.com/setup_18.x | sudo -E bash -
# sudo apt install -y nodejs
```

## 3. Instalar Appium Server

```bash
# Instalar Appium globalmente
sudo npm install -g appium

# Instalar el driver de UiAutomator2 para Android
appium driver install uiautomator2

# Verificar instalación
appium --version
```

## 4. Instalar Android SDK y herramientas

### Opción A: Usando Android Studio (Recomendado)

1. Descargar Android Studio desde: https://developer.android.com/studio
2. Instalar Android Studio
3. Abrir Android Studio y ir a: **Tools > SDK Manager**
4. Instalar:
   - Android SDK Platform-Tools
   - Android SDK Build-Tools
   - Android SDK Platform (API 33 o superior)

### Opción B: Instalación manual (Command Line Tools)

```bash
# Crear directorio para Android SDK
mkdir -p ~/Android/Sdk
cd ~/Android/Sdk

# Descargar Command Line Tools
wget https://dl.google.com/android/repository/commandlinetools-linux-9477386_latest.zip
unzip commandlinetools-linux-9477386_latest.zip
mkdir -p cmdline-tools/latest
mv cmdline-tools/* cmdline-tools/latest/ 2>/dev/null || true

# Configurar variables de entorno (agregar a ~/.bashrc o ~/.zshrc)
echo '' >> ~/.bashrc
echo '# Android SDK' >> ~/.bashrc
echo 'export ANDROID_HOME=$HOME/Android/Sdk' >> ~/.bashrc
echo 'export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin' >> ~/.bashrc
echo 'export PATH=$PATH:$ANDROID_HOME/platform-tools' >> ~/.bashrc
echo 'export PATH=$PATH:$ANDROID_HOME/tools' >> ~/.bashrc
echo 'export PATH=$PATH:$ANDROID_HOME/tools/bin' >> ~/.bashrc

# Recargar configuración
source ~/.bashrc

# Instalar componentes necesarios
sdkmanager --install "platform-tools" "platforms;android-33" "build-tools;33.0.0"
```

## 5. Instalar adb (Android Debug Bridge)

```bash
# Si no está instalado con Android SDK
sudo apt install -y android-tools-adb

# Verificar instalación
adb version
```

## 6. Configurar dispositivo o emulador

### Opción A: Usar un dispositivo físico

1. Habilitar **Opciones de desarrollador** en tu dispositivo Android:
   - Ir a **Configuración > Acerca del teléfono**
   - Tocar 7 veces en **Número de compilación**

2. Habilitar **Depuración USB**:
   - Ir a **Configuración > Opciones de desarrollador**
   - Activar **Depuración USB**

3. Conectar el dispositivo por USB y verificar:
   ```bash
   adb devices
   ```
   Deberías ver tu dispositivo listado.

### Opción B: Usar un emulador

1. Abrir Android Studio
2. Ir a **Tools > Device Manager**
3. Crear un nuevo dispositivo virtual (AVD)
4. Iniciar el emulador
5. Verificar conexión:
   ```bash
   adb devices
   ```

## 7. Instalar dependencias de Python para los tests

```bash
# Navegar al directorio de tests
cd /home/gaston/AndroidStudioProjects/Caretemplate/tests

# Crear un entorno virtual (recomendado)
python3 -m venv venv

# Activar el entorno virtual
source venv/bin/activate

# Instalar dependencias
pip install -r requirements.txt

# Verificar instalación
pytest --version
```

## 8. Verificar que todo funciona

```bash
# 1. Verificar que Appium está instalado
appium --version

# 2. Verificar que adb puede ver dispositivos
adb devices

# 3. Verificar que Python puede importar Appium
python3 -c "from appium import webdriver; print('Appium OK')"

# 4. Iniciar Appium Server (en una terminal separada)
appium

# 5. En otra terminal, ejecutar un test simple
cd /home/gaston/AndroidStudioProjects/Caretemplate/tests
source venv/bin/activate
pytest tests/test_main_activity.py::TestMainActivity::test_app_launches -v
```

## Solución de Problemas

### Appium no se encuentra
```bash
# Verificar que npm está en el PATH
which npm
which appium

# Si appium no está, reinstalar
sudo npm install -g appium
```

### adb no encuentra dispositivos
```bash
# Reiniciar el servidor adb
adb kill-server
adb start-server
adb devices

# Si usas un dispositivo físico, verificar permisos USB
# Puede ser necesario agregar reglas udev
```

### Python no encuentra módulos
```bash
# Asegúrate de estar en el entorno virtual
source venv/bin/activate

# Reinstalar dependencias
pip install --upgrade -r requirements.txt
```

### Permisos de USB (para dispositivos físicos)
```bash
# Crear regla udev para dispositivos Android
sudo nano /etc/udev/rules.d/51-android.rules

# Agregar (reemplazar USERNAME con tu usuario):
SUBSYSTEM=="usb", ATTR{idVendor}=="04e8", MODE="0666", GROUP="plugdev"
SUBSYSTEM=="usb", ATTR{idVendor}=="18d1", MODE="0666", GROUP="plugdev"

# Recargar reglas
sudo udevadm control --reload-rules
sudo udevadm trigger

# Agregar tu usuario al grupo plugdev
sudo usermod -aG plugdev $USER
# Cerrar sesión y volver a iniciar para que tome efecto
```

## Próximos Pasos

Una vez que todo esté instalado:

1. Asegúrate de que tu aplicación esté instalada en el dispositivo/emulador
2. Inicia Appium Server: `appium`
3. Ejecuta los tests: `pytest tests/ -v`

