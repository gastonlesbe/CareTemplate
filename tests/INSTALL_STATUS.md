# Estado de la Instalación

## ✅ Instalado Correctamente

1. **Python 3.12.3** - ✅ Instalado
2. **Entorno Virtual** - ✅ Creado en `tests/venv/`
3. **Dependencias de Python** - ✅ Instaladas:
   - pytest
   - Appium-Python-Client
   - selenium
   - pytest-html
   - pytest-xdist
4. **Appium 2.1.3** - ✅ Instalado localmente en `tests/node_modules/`

## ⚠️ Pendiente

**Driver UiAutomator2**: Hay un conflicto de versiones. El driver más reciente requiere Appium 3.x, pero tenemos Appium 2.x (compatible con Node 18).

### Soluciones:

#### Opción 1: Usar Appium sin driver (recomendado para empezar)
Puedes ejecutar Appium y los tests deberían funcionar. El driver se instalará automáticamente cuando sea necesario.

#### Opción 2: Actualizar Node.js a v20+
```bash
# Instalar nvm (Node Version Manager)
curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.39.0/install.sh | bash
source ~/.bashrc

# Instalar Node.js 20
nvm install 20
nvm use 20

# Reinstalar Appium
cd tests
npm install appium@latest
npx appium driver install uiautomator2
```

## Cómo Usar

### 1. Activar entorno virtual:
```bash
cd /home/gaston/AndroidStudioProjects/Caretemplate/tests
source venv/bin/activate
```

### 2. Iniciar Appium (en una terminal):
```bash
cd /home/gaston/AndroidStudioProjects/Caretemplate/tests
./start_appium.sh
# O directamente:
npx appium
```

### 3. Conectar dispositivo:
```bash
adb devices
```

### 4. Ejecutar tests (en otra terminal):
```bash
cd /home/gaston/AndroidStudioProjects/Caretemplate/tests
source venv/bin/activate
pytest tests/
```

## Notas

- Appium está instalado localmente, así que siempre usa `npx appium` o `./start_appium.sh`
- Si tienes problemas, considera actualizar Node.js a la versión 20 o superior

