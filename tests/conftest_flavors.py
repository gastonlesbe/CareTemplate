"""
Configuración de pytest para tests de Appium con soporte para múltiples flavors
"""
import pytest
from appium import webdriver
from appium.options.android import UiAutomator2Options
import os
import subprocess


# Configuración de flavors
FLAVORS = {
    "family": {
        "package": "com.gastonlesbegueris.caretemplate.family",
        "activity": "com.gastonlesbegueris.caretemplate.ui.MainActivity",
        "app_name": "CarePal Family"
    },
    "pets": {
        "package": "com.gastonlesbegueris.caretemplate.pets",
        "activity": "com.gastonlesbegueris.caretemplate.ui.MainActivity",
        "app_name": "CarePal: Pets"
    },
    "cars": {
        "package": "com.gastonlesbegueris.caretemplate.cars",
        "activity": "com.gastonlesbegueris.caretemplate.ui.MainActivity",
        "app_name": "CarePal: Cars"
    },
    "house": {
        "package": "com.gastonlesbegueris.caretemplate.house",
        "activity": "com.gastonlesbegueris.caretemplate.ui.MainActivity",
        "app_name": "CarePal: Houses"
    }
}


def get_device_udid():
    """Obtiene el UDID del primer dispositivo conectado"""
    try:
        result = subprocess.run(['adb', 'devices'], capture_output=True, text=True, timeout=5)
        lines = result.stdout.strip().split('\n')[1:]  # Skip header
        for line in lines:
            if line.strip() and 'device' in line:
                return line.split()[0]
    except Exception as e:
        print(f"Error getting device UDID: {e}")
    return os.getenv('DEVICE_UDID', 'emulator-5554')


@pytest.fixture(scope="session", params=["family", "pets", "cars", "house"])
def flavor(request):
    """Fixture que parametriza los tests para cada flavor"""
    return request.param


@pytest.fixture(scope="session")
def appium_driver_family():
    """Driver de Appium para flavor Family"""
    return create_driver_for_flavor("family")


@pytest.fixture(scope="session")
def appium_driver_pets():
    """Driver de Appium para flavor Pets"""
    return create_driver_for_flavor("pets")


@pytest.fixture(scope="session")
def appium_driver_cars():
    """Driver de Appium para flavor Cars"""
    return create_driver_for_flavor("cars")


@pytest.fixture(scope="session")
def appium_driver_house():
    """Driver de Appium para flavor House"""
    return create_driver_for_flavor("house")


def create_driver_for_flavor(flavor_name):
    """Crea un driver de Appium para un flavor específico"""
    if flavor_name not in FLAVORS:
        raise ValueError(f"Flavor desconocido: {flavor_name}")
    
    flavor_config = FLAVORS[flavor_name]
    appium_server_url = os.getenv('APPIUM_SERVER_URL', 'http://localhost:4723')
    device_udid = get_device_udid()
    
    options = UiAutomator2Options()
    options.platform_name = "Android"
    options.device_name = device_udid
    options.udid = device_udid
    options.app_package = flavor_config["package"]
    options.app_activity = flavor_config["activity"]
    options.automation_name = "UiAutomator2"
    options.no_reset = True
    options.full_reset = False
    options.auto_grant_permissions = True
    
    # Timeouts
    options.new_command_timeout = 300
    options.implicit_wait = 10
    
    driver = webdriver.Remote(appium_server_url, options=options)
    return driver


@pytest.fixture(scope="function")
def driver_family(appium_driver_family):
    """Fixture para tests de Family flavor"""
    yield appium_driver_family


@pytest.fixture(scope="function")
def driver_pets(appium_driver_pets):
    """Fixture para tests de Pets flavor"""
    yield appium_driver_pets


@pytest.fixture(scope="function")
def driver_cars(appium_driver_cars):
    """Fixture para tests de Cars flavor"""
    yield appium_driver_cars


@pytest.fixture(scope="function")
def driver_house(appium_driver_house):
    """Fixture para tests de House flavor"""
    yield appium_driver_house


@pytest.fixture(scope="function")
def driver(flavor, request):
    """Fixture genérico que selecciona el driver según el flavor parametrizado"""
    driver_map = {
        "family": request.getfixturevalue("driver_family"),
        "pets": request.getfixturevalue("driver_pets"),
        "cars": request.getfixturevalue("driver_cars"),
        "house": request.getfixturevalue("driver_house")
    }
    return driver_map.get(flavor)


def get_flavor_config(flavor_name):
    """Obtiene la configuración de un flavor"""
    return FLAVORS.get(flavor_name, {})
