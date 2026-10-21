"""
Test simple de verificación - no requiere dispositivo conectado
"""
import pytest
import os


def test_environment_setup():
    """Verifica que el entorno esté configurado correctamente"""
    # Verificar que las dependencias estén instaladas
    try:
        import appium
        import selenium
        import pytest as pytest_module
        assert True, "Todas las dependencias están instaladas"
    except ImportError as e:
        pytest.fail(f"Dependencia faltante: {e}")


def test_appium_config():
    """Verifica la configuración de Appium"""
    appium_server_url = os.getenv('APPIUM_SERVER_URL', 'http://localhost:4723')
    assert appium_server_url == 'http://localhost:4723', "URL de Appium configurada correctamente"
    
    app_package = os.getenv('APP_PACKAGE', 'com.gastonlesbegueris.caretemplate.pets')
    assert 'caretemplate' in app_package, "Package de la app configurado"


def test_adb_available():
    """Verifica que adb esté disponible"""
    import subprocess
    try:
        result = subprocess.run(['adb', '--version'], 
                              capture_output=True, 
                              text=True, 
                              timeout=5)
        assert result.returncode == 0, "adb está disponible"
    except FileNotFoundError:
        pytest.skip("adb no está en el PATH")
    except subprocess.TimeoutExpired:
        pytest.skip("adb no responde")


@pytest.mark.skip(reason="Requiere dispositivo conectado y Appium Server corriendo")
def test_appium_server_connection():
    """Verifica la conexión con Appium Server (requiere servidor corriendo)"""
    import requests
    try:
        response = requests.get('http://localhost:4723/status', timeout=2)
        assert response.status_code == 200, "Appium Server está corriendo"
    except requests.exceptions.ConnectionError:
        pytest.skip("Appium Server no está corriendo. Inicia con: npx appium")
    except requests.exceptions.Timeout:
        pytest.skip("Appium Server no responde")

