# 🧪 Tests Automatizados con Appium

Tests automatizados con Appium y Python para verificar que la app funcione correctamente en todos los flavors (Family, Pets, Cars, House).

## 📋 Requisitos Previos

### 1. Instalar Appium Server
```bash
npm install -g appium
npm install -g appium-driver-uiautomator2
```

### 2. Instalar Dependencias de Python
```bash
cd tests
python3 -m venv venv
source venv/bin/activate  # En Windows: venv\Scripts\activate
pip install -r requirements.txt
```

### 3. Configurar Android SDK
- Asegúrate de que `ANDROID_HOME` esté configurado
- Verifica que `adb` esté en tu PATH:
  ```bash
  export PATH=$PATH:$ANDROID_HOME/platform-tools
  ```

### 4. Preparar el Emulador o Dispositivo
- Inicia un emulador desde Android Studio, o
- Conecta un dispositivo físico con USB debugging habilitado
- Verifica la conexión:
  ```bash
  adb devices
  ```

## 🚀 Inicio Rápido

### 1. Iniciar Appium Server
En una terminal:
```bash
appium
```

### 2. Compilar las APKs de Debug
En otra terminal, desde la raíz del proyecto:
```bash
./gradlew assembleFamilyDebug assemblePetsDebug assembleCarsDebug assembleHouseDebug
```

### 3. Instalar las APKs en el Dispositivo
```bash
adb install -r app/build/outputs/apk/familyDebug/app-family-debug.apk
adb install -r app/build/outputs/apk/petsDebug/app-pets-debug.apk
adb install -r app/build/outputs/apk/carsDebug/app-cars-debug.apk
adb install -r app/build/outputs/apk/houseDebug/app-house-debug.apk
```

### 4. Ejecutar Tests

#### Opción A: Usar los Scripts (Recomendado)
```bash
# Activar entorno virtual
source venv/bin/activate

# Ejecutar tests de un flavor
./run_tests_family.sh
./run_tests_pets.sh
./run_tests_cars.sh
./run_tests_house.sh

# O ejecutar todos
./run_all_flavors.sh
```

#### Opción B: Usar pytest directamente
```bash
# Activar entorno virtual
source venv/bin/activate

# Ejecutar tests de un flavor
pytest tests/test_family_flavor.py -v
pytest tests/test_pets_flavor.py -v
pytest tests/test_cars_flavor.py -v
pytest tests/test_house_flavor.py -v

# Ejecutar tests funcionales
pytest tests/test_functional_family.py -v
pytest tests/test_functional_pets.py -v

# Ejecutar todos
pytest tests/test_*_flavor.py tests/test_functional_*.py -v
```

## 📁 Estructura de Tests

```
tests/
├── conftest_flavors.py      # Configuración de fixtures para cada flavor
├── test_family_flavor.py     # Tests básicos para CarePal Family
├── test_pets_flavor.py       # Tests básicos para CarePal Pets
├── test_cars_flavor.py        # Tests básicos para CarePal Cars
├── test_house_flavor.py      # Tests básicos para CarePal Houses
├── test_functional_family.py # Tests funcionales para Family (crear, editar, etc.)
├── test_functional_pets.py   # Tests funcionales para Pets
├── run_tests_family.sh       # Script para ejecutar tests de Family
├── run_tests_pets.sh         # Script para ejecutar tests de Pets
├── run_tests_cars.sh         # Script para ejecutar tests de Cars
├── run_tests_house.sh        # Script para ejecutar tests de House
└── run_all_flavors.sh        # Script para ejecutar todos los tests
```

## 🧪 Tests Disponibles

### Tests Básicos (Solo Verificación)
- `test_app_launches` - Verifica que la app se lance
- `test_app_name_displayed` - Verifica el nombre de la app
- `test_menu_button_exists` - Verifica que el botón de menú exista
- `test_fab_button_exists` - Verifica que el botón FAB exista
- `test_navigate_to_subjects` - Verifica navegación a sujetos

### Tests Funcionales (Con Interacción)
- `test_create_subject` - Crea un sujeto
- `test_create_event` - Crea un evento
- `test_navigate_to_expenses` - Navega a la pantalla de gastos
- `test_use_demo_data` - Usa la opción de datos de demostración
- `test_view_tutorial` - Abre el tutorial

## 📊 Generar Reportes HTML

```bash
pytest tests/test_family_flavor.py -v --html=reports/family_test_report.html --self-contained-html
```

Los reportes se guardan en `tests/reports/`.

## ⚙️ Configuración Avanzada

### Variables de Entorno
```bash
export APPIUM_SERVER_URL=http://localhost:4723
export DEVICE_UDID=emulator-5554
```

### Ejecutar Tests en Paralelo
```bash
pytest tests/ -n auto
```

### Ejecutar Tests con Filtros
```bash
pytest tests/test_family_flavor.py -k "menu" -v
```

## 🔧 Troubleshooting

### Appium no se conecta
```bash
# Verificar que Appium esté corriendo
curl http://localhost:4723/status

# Si no está corriendo, iniciarlo:
appium
```

### Dispositivo no encontrado
```bash
# Verificar dispositivos conectados
adb devices

# Si no hay dispositivos, iniciar un emulador desde Android Studio
```

### La app no está instalada
Los scripts intentan instalar automáticamente las APKs si no están instaladas. Si falla, instala manualmente:
```bash
adb install -r app/build/outputs/apk/familyDebug/app-family-debug.apk
```

### Elementos no encontrados
1. Usa Appium Inspector para inspeccionar la app:
   ```bash
   appium inspector
   ```
2. Verifica los IDs de los elementos en los layouts XML
3. Ajusta los selectores en los archivos de test

### Timeouts
Si los tests fallan por timeouts, puedes aumentar los tiempos de espera en `conftest_flavors.py`:
```python
options.new_command_timeout = 600  # 10 minutos
options.implicit_wait = 20  # 20 segundos
```

## 📝 Agregar Nuevos Tests

Para agregar nuevos tests a un flavor específico, edita el archivo correspondiente:
- `test_family_flavor.py` para Family
- `test_pets_flavor.py` para Pets
- `test_cars_flavor.py` para Cars
- `test_house_flavor.py` para House

Ejemplo de test:
```python
def test_new_feature(self, driver_family):
    """Descripción del test"""
    time.sleep(2)
    
    try:
        element = driver_family.find_element(
            AppiumBy.ID,
            "com.gastonlesbegueris.caretemplate.family:id/element_id"
        )
        element.click()
        assert True
    except Exception as e:
        pytest.fail(f"Test falló: {e}")
```

## 📚 Recursos

- [Documentación de Appium](https://appium.io/docs/en/about-appium/intro/)
- [Appium Python Client](https://github.com/appium/python-client)
- [Pytest Documentation](https://docs.pytest.org/)