"""
Configuración global de pytest para tests de Appium
"""
import pytest
from appium import webdriver
from appium.options.android import UiAutomator2Options
import os


@pytest.fixture(scope="session")
def appium_driver():
    """
    Fixture que crea y configura el driver de Appium para Android
    """
    # Configuración de Appium
    appium_server_url = os.getenv('APPIUM_SERVER_URL', 'http://localhost:4723')
    
    # Opciones para Android
    options = UiAutomator2Options()
    options.platform_name = "Android"
    # Obtener el ID del dispositivo desde adb
    import subprocess
    try:
        adb_result = subprocess.run(['adb', 'devices'], capture_output=True, text=True, timeout=5)
        devices = [line.split()[0] for line in adb_result.stdout.split('\n') if 'device' in line and 'List' not in line]
        device_name = devices[0] if devices else os.getenv('DEVICE_NAME', 'emulator-5554')
    except:
        device_name = os.getenv('DEVICE_NAME', 'emulator-5554')
    
    options.device_name = device_name
    options.udid = device_name  # Especificar el UDID del dispositivo
    options.app_package = os.getenv('APP_PACKAGE', 'com.gastonlesbegueris.caretemplate.pets')
    options.app_activity = os.getenv('APP_ACTIVITY', 'com.gastonlesbegueris.caretemplate.ui.MainActivity')
    options.automation_name = "UiAutomator2"
    options.no_reset = True  # No resetear el estado de la app entre tests
    options.full_reset = False
    
    # Si tienes un APK específico, puedes usar:
    # options.app = os.path.join(os.path.dirname(__file__), 'app', 'app-debug.apk')
    
    # Crear el driver
    driver = webdriver.Remote(appium_server_url, options=options)
    
    yield driver
    
    # Limpiar después de los tests
    driver.quit()


@pytest.fixture(scope="function")
def driver(appium_driver):
    """
    Fixture que asegura que la app esté en el estado inicial antes de cada test
    """
    # Resetear la app al estado inicial si es necesario
    appium_driver.reset()
    yield appium_driver

