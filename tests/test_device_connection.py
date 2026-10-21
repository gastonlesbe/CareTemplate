"""
Test de conexión con el dispositivo real
"""
import pytest
from appium import webdriver
from appium.options.android import UiAutomator2Options
import subprocess
import time


def test_device_connected():
    """Verifica que el dispositivo esté conectado"""
    result = subprocess.run(['adb', 'devices'], capture_output=True, text=True, timeout=5)
    devices = [line.split()[0] for line in result.stdout.split('\n') 
               if 'device' in line and 'List' not in line and line.strip()]
    assert len(devices) > 0, "No hay dispositivos conectados"
    print(f"Dispositivo conectado: {devices[0]}")


def test_appium_server_running():
    """Verifica que Appium Server esté corriendo"""
    import requests
    try:
        response = requests.get('http://localhost:4723/status', timeout=2)
        assert response.status_code == 200, "Appium Server está corriendo"
        print("Appium Server está listo")
    except requests.exceptions.ConnectionError:
        pytest.fail("Appium Server no está corriendo. Inicia con: npx appium")


def test_app_launch():
    """Test básico de lanzamiento de la app (requiere app instalada)"""
    # Obtener el ID del dispositivo
    adb_result = subprocess.run(['adb', 'devices'], capture_output=True, text=True, timeout=5)
    devices = [line.split()[0] for line in adb_result.stdout.split('\n') 
               if 'device' in line and 'List' not in line and line.strip()]
    
    if not devices:
        pytest.skip("No hay dispositivos conectados")
    
    device_id = devices[0]
    
    # Configurar opciones
    options = UiAutomator2Options()
    options.platform_name = "Android"
    options.device_name = device_id
    options.udid = device_id
    options.app_package = "com.gastonlesbegueris.caretemplate.pets"
    options.app_activity = "com.gastonlesbegueris.caretemplate.ui.MainActivity"
    options.automation_name = "UiAutomator2"
    options.no_reset = True
    
    # Conectar con Appium
    driver = webdriver.Remote('http://localhost:4723', options=options)
    
    try:
        # Esperar a que la app cargue
        time.sleep(3)
        
        # Verificar que la app se lanzó
        current_package = driver.current_package
        assert current_package == "com.gastonlesbegueris.caretemplate.pets", \
            f"La app no se lanzó correctamente. Package actual: {current_package}"
        
        print(f"✅ App lanzada correctamente. Package: {current_package}")
        
    finally:
        driver.quit()

