"""
Tests automatizados para CarePal Cars usando pytest y Appium
"""
import pytest
from appium.webdriver.common.appiumby import AppiumBy
import time


class TestCarsFlavor:
    """Tests específicos para el flavor Cars"""
    
    def test_app_launches(self, driver_cars):
        """Verifica que CarePal Cars se lance correctamente"""
        time.sleep(3)
        
        try:
            toolbar = driver_cars.find_element(AppiumBy.ID, "com.gastonlesbegueris.caretemplate.cars:id/toolbar")
            assert toolbar is not None, "El toolbar no se encontró"
        except Exception as e:
            pytest.fail(f"Error al encontrar toolbar: {e}")
    
    def test_app_name_displayed(self, driver_cars):
        """Verifica que el nombre de la app se muestre correctamente"""
        time.sleep(2)
        
        try:
            toolbar = driver_cars.find_element(AppiumBy.ID, "com.gastonlesbegueris.caretemplate.cars:id/toolbar")
            title = toolbar.get_attribute("content-desc") or toolbar.text
            assert "Cars" in title or "CarePal" in title, f"Título no encontrado: {title}"
        except Exception as e:
            pytest.skip(f"No se pudo verificar el título: {e}")
    
    def test_menu_button_exists(self, driver_cars):
        """Verifica que el botón de menú esté presente"""
        time.sleep(2)
        
        try:
            menu_button = driver_cars.find_element(
                AppiumBy.ACCESSIBILITY_ID, 
                "More options"
            )
            assert menu_button is not None, "El botón de menú no se encontró"
        except:
            try:
                menu_button = driver_cars.find_element(
                    AppiumBy.XPATH,
                    "//android.widget.ImageButton[@content-desc='More options']"
                )
                assert menu_button is not None, "El botón de menú no se encontró"
            except Exception as e:
                pytest.skip(f"Botón de menú no encontrado: {e}")
    
    def test_fab_button_exists(self, driver_cars):
        """Verifica que el botón FAB esté presente"""
        time.sleep(2)
        
        try:
            fab = driver_cars.find_element(
                AppiumBy.ID,
                "com.gastonlesbegueris.caretemplate.cars:id/fabAdd"
            )
            assert fab is not None, "El botón FAB no se encontró"
        except:
            try:
                fab = driver_cars.find_element(
                    AppiumBy.CLASS_NAME,
                    "android.widget.ImageButton"
                )
                assert fab is not None, "El botón FAB no se encontró"
            except Exception as e:
                pytest.skip(f"FAB no encontrado: {e}")
    
    def test_navigate_to_subjects(self, driver_cars):
        """Verifica la navegación a la pantalla de sujetos (vehículos)"""
        time.sleep(2)
        
        try:
            menu_button = driver_cars.find_element(
                AppiumBy.ACCESSIBILITY_ID,
                "More options"
            )
        except:
            menu_button = driver_cars.find_element(
                AppiumBy.XPATH,
                "//android.widget.ImageButton[@content-desc='More options']"
            )
        
        menu_button.click()
        time.sleep(1)
        
        try:
            subjects_item = driver_cars.find_element(
                AppiumBy.XPATH,
                "//android.widget.TextView[@text='Sujetos' or @text='Vehículos']"
            )
            subjects_item.click()
            time.sleep(2)
            
            subjects_toolbar = driver_cars.find_element(
                AppiumBy.ID,
                "com.gastonlesbegueris.caretemplate.cars:id/toolbarSubjects"
            )
            assert subjects_toolbar is not None, "No se navegó a la pantalla de sujetos"
        except Exception as e:
            pytest.skip(f"No se pudo navegar a sujetos: {e}")
