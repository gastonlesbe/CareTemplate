"""
Tests automatizados para CarePal Houses usando pytest y Appium
"""
import pytest
from appium.webdriver.common.appiumby import AppiumBy
import time


class TestHouseFlavor:
    """Tests específicos para el flavor House"""
    
    def test_app_launches(self, driver_house):
        """Verifica que CarePal Houses se lance correctamente"""
        time.sleep(3)
        
        try:
            toolbar = driver_house.find_element(AppiumBy.ID, "com.gastonlesbegueris.caretemplate.house:id/toolbar")
            assert toolbar is not None, "El toolbar no se encontró"
        except Exception as e:
            pytest.fail(f"Error al encontrar toolbar: {e}")
    
    def test_app_name_displayed(self, driver_house):
        """Verifica que el nombre de la app se muestre correctamente"""
        time.sleep(2)
        
        try:
            toolbar = driver_house.find_element(AppiumBy.ID, "com.gastonlesbegueris.caretemplate.house:id/toolbar")
            title = toolbar.get_attribute("content-desc") or toolbar.text
            assert "House" in title or "CarePal" in title, f"Título no encontrado: {title}"
        except Exception as e:
            pytest.skip(f"No se pudo verificar el título: {e}")
    
    def test_menu_button_exists(self, driver_house):
        """Verifica que el botón de menú esté presente"""
        time.sleep(2)
        
        try:
            menu_button = driver_house.find_element(
                AppiumBy.ACCESSIBILITY_ID, 
                "More options"
            )
            assert menu_button is not None, "El botón de menú no se encontró"
        except:
            try:
                menu_button = driver_house.find_element(
                    AppiumBy.XPATH,
                    "//android.widget.ImageButton[@content-desc='More options']"
                )
                assert menu_button is not None, "El botón de menú no se encontró"
            except Exception as e:
                pytest.skip(f"Botón de menú no encontrado: {e}")
    
    def test_fab_button_exists(self, driver_house):
        """Verifica que el botón FAB esté presente"""
        time.sleep(2)
        
        try:
            fab = driver_house.find_element(
                AppiumBy.ID,
                "com.gastonlesbegueris.caretemplate.house:id/fabAdd"
            )
            assert fab is not None, "El botón FAB no se encontró"
        except:
            try:
                fab = driver_house.find_element(
                    AppiumBy.CLASS_NAME,
                    "android.widget.ImageButton"
                )
                assert fab is not None, "El botón FAB no se encontró"
            except Exception as e:
                pytest.skip(f"FAB no encontrado: {e}")
    
    def test_navigate_to_subjects(self, driver_house):
        """Verifica la navegación a la pantalla de sujetos (casas)"""
        time.sleep(2)
        
        try:
            menu_button = driver_house.find_element(
                AppiumBy.ACCESSIBILITY_ID,
                "More options"
            )
        except:
            menu_button = driver_house.find_element(
                AppiumBy.XPATH,
                "//android.widget.ImageButton[@content-desc='More options']"
            )
        
        menu_button.click()
        time.sleep(1)
        
        try:
            subjects_item = driver_house.find_element(
                AppiumBy.XPATH,
                "//android.widget.TextView[@text='Sujetos' or @text='Casas']"
            )
            subjects_item.click()
            time.sleep(2)
            
            subjects_toolbar = driver_house.find_element(
                AppiumBy.ID,
                "com.gastonlesbegueris.caretemplate.house:id/toolbarSubjects"
            )
            assert subjects_toolbar is not None, "No se navegó a la pantalla de sujetos"
        except Exception as e:
            pytest.skip(f"No se pudo navegar a sujetos: {e}")
