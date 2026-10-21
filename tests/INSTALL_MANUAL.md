# Instalación Manual - Tests con Pytest y Appium

## Estado Actual

Ya tienes instalado:
- ✅ Python 3.12.3
- ✅ pip
- ✅ Node.js v18.19.1
- ✅ npm 9.2.0
- ✅ Android SDK (adb)
- ✅ Java 21

## Pasos para Completar la Instalación

### 1. Instalar Appium (puede tardar varios minutos)

Abre una terminal y ejecuta:

```bash
sudo npm install -g appium
```

Si tienes problemas con permisos, puedes instalarlo sin sudo (recomendado):

```bash
npm install -g appium
```

### 2. Instalar el driver de UiAutomator2 para Android

```bash
appium driver install uiautomator2
```

### 3. Instalar dependencias de Python

```bash
cd /home/gaston/AndroidStudioProjects/Caretemplate/tests
pip3 install --user -r requirements.txt
```

O si prefieres usar un entorno virtual (recomendado):

```bash
cd /home/gaston/AndroidStudioProjects/Caretemplate/tests
python3 -m venv venv
source venv/bin/activate
pip install -r requirements.txt
```

### 4. Verificar la instalación

```bash
# Verificar Appium
appium --version

# Verificar Python packages
pip3 list | grep -i appium
pip3 list | grep -i pytest
```

### 5. Configurar variables de entorno (opcional)

Agrega estas líneas a tu `~/.bashrc` o `~/.zshrc`:

```bash
export ANDROID_HOME=/usr/lib/android-sdk
export PATH=$PATH:$ANDROID_HOME/platform-tools:$ANDROID_HOME/tools
```

Luego recarga:
```bash
source ~/.bashrc
```

## Próximos Pasos

1. **Conectar un dispositivo o emulador:**
   ```bash
   adb devices
   ```

2. **Iniciar Appium Server** (en una terminal):
   ```bash
   appium
   ```

3. **Ejecutar los tests** (en otra terminal):
   ```bash
   cd /home/gaston/AndroidStudioProjects/Caretemplate/tests
   pytest tests/
   ```

## Solución de Problemas

### Si Appium no se instala con npm:
- Verifica que npm tenga permisos: `npm config get prefix`
- O instala sin sudo usando nvm (Node Version Manager)

### Si no encuentra adb:
- Verifica que ANDROID_HOME esté configurado
- O instala: `sudo apt install android-sdk-platform-tools`

### Si los tests fallan:
- Asegúrate de que Appium Server esté corriendo
- Verifica que el dispositivo esté conectado: `adb devices`
- Ajusta los IDs de elementos en los tests según tu app

