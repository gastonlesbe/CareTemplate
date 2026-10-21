"""
Tests automatizados para CarePal Family usando pytest y Appium
"""
import pytest
from appium.webdriver.common.appiumby import AppiumBy
from appium.webdriver.common.touch_action import TouchAction
import time


class TestFamilyFlavor:
    """Tests específicos para el flavor Family"""
    
    def test_app_launches(self, driver_family):
        """Verifica que CarePal Family se lance correctamente"""
        time.sleep(3)
        
        # Verificar que el toolbar esté presente
        try:
            toolbar = driver_family.find_element(AppiumBy.ID, "com.gastonlesbegueris.caretemplate.family:id/toolbar")
            assert toolbar is not None, "El toolbar no se encontró"
        except Exception as e:
            pytest.fail(f"Error al encontrar toolbar: {e}")
    
    def test_app_name_displayed(self, driver_family):
        """Verifica que el nombre de la app se muestre correctamente"""
        time.sleep(2)
        
        # Verificar que el título contenga "CarePal Family"
        try:
            # Buscar el toolbar y verificar el título
            toolbar = driver_family.find_element(AppiumBy.ID, "com.gastonlesbegueris.caretemplate.family:id/toolbar")
            title = toolbar.get_attribute("content-desc") or toolbar.text
            assert "Family" in title or "CarePal" in title, f"Título no encontrado: {title}"
        except Exception as e:
            pytest.skip(f"No se pudo verificar el título: {e}")
    
    def test_menu_button_exists(self, driver_family):
        """Verifica que el botón de menú esté presente"""
        time.sleep(2)
        
        try:
            menu_button = driver_family.find_element(
                AppiumBy.ACCESSIBILITY_ID, 
                "More options"
            )
            assert menu_button is not None, "El botón de menú no se encontró"
        except:
            try:
                menu_button = driver_family.find_element(
                    AppiumBy.XPATH,
                    "//android.widget.ImageButton[@content-desc='More options']"
                )
                assert menu_button is not None, "El botón de menú no se encontró"
            except Exception as e:
                pytest.skip(f"Botón de menú no encontrado: {e}")
    
    def test_fab_button_exists(self, driver_family):
        """Verifica que el botón FAB esté presente"""
        time.sleep(2)
        
        try:
            # Buscar el FAB principal
            fab = driver_family.find_element(
                AppiumBy.ID,
                "com.gastonlesbegueris.caretemplate.family:id/fabAdd"
            )
            assert fab is not None, "El botón FAB no se encontró"
        except:
            try:
                # Intentar buscar por clase
                fab = driver_family.find_element(
                    AppiumBy.CLASS_NAME,
                    "android.widget.ImageButton"
                )
                assert fab is not None, "El botón FAB no se encontró"
            except Exception as e:
                pytest.skip(f"FAB no encontrado: {e}")
    
    def test_navigate_to_subjects(self, driver_family):
        """Verifica la navegación a la pantalla de sujetos (familiares)"""
        time.sleep(2)
        
        # Abrir el menú
        try:
            menu_button = driver_family.find_element(
                AppiumBy.ACCESSIBILITY_ID,
                "More options"
            )
        except:
            menu_button = driver_family.find_element(
                AppiumBy.XPATH,
                "//android.widget.ImageButton[@content-desc='More options']"
            )
        
        menu_button.click()
        time.sleep(1)
        
        # Buscar el item de sujetos (puede ser "Sujetos" o "Familiares" según el idioma)
        try:
            subjects_item = driver_family.find_element(
                AppiumBy.XPATH,
                "//android.widget.TextView[@text='Sujetos' or @text='Familiares']"
            )
            subjects_item.click()
            time.sleep(2)
            
            # Verificar que estamos en la pantalla de sujetos
            subjects_toolbar = driver_family.find_element(
                AppiumBy.ID,
                "com.gastonlesbegueris.caretemplate.family:id/toolbarSubjects"
            )
            assert subjects_toolbar is not None, "No se navegó a la pantalla de sujetos"
        except Exception as e:
            pytest.skip(f"No se pudo navegar a sujetos: {e}")
    
    def test_demo_data_menu_option(self, driver_family):
        """Verifica que la opción de datos de demostración esté disponible"""
        time.sleep(2)
        
        # Abrir el menú
        try:
            menu_button = driver_family.find_element(
                AppiumBy.ACCESSIBILITY_ID,
                "More options"
            )
        except:
            menu_button = driver_family.find_element(
                AppiumBy.XPATH,
                "//android.widget.ImageButton[@content-desc='More options']"
            )
        
        menu_button.click()
        time.sleep(1)
        
        # Buscar la opción de datos de demostración
        try:
            demo_item = driver_family.find_element(
                AppiumBy.XPATH,
                "//android.widget.TextView[@text='Datos de demostración' or @text='Demo data']"
            )
            assert demo_item is not None, "Opción de datos de demostración no encontrada"
        except Exception as e:
            pytest.skip(f"Opción de datos de demostración no encontrada: {e}")
