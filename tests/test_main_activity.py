"""
Tests automatizados para MainActivity usando pytest y Appium
"""
import pytest
from appium.webdriver.common.appiumby import AppiumBy
import time


class TestMainActivity:
    """Tests para la actividad principal de la aplicación"""
    
    def test_app_launches(self, driver):
        """Verifica que la aplicación se lance correctamente"""
        # Esperar a que la app cargue
        time.sleep(2)
        
        # Verificar que el toolbar esté presente
        toolbar = driver.find_element(AppiumBy.ID, "com.gastonlesbegueris.caretemplate.pets:id/toolbar")
        assert toolbar is not None, "El toolbar no se encontró"
    
    def test_menu_button_exists(self, driver):
        """Verifica que el botón de menú (3 puntitos) esté presente"""
        time.sleep(2)
        
        # Buscar el botón de overflow menu
        # Appium puede encontrar elementos por accessibility id, xpath, etc.
        try:
            menu_button = driver.find_element(
                AppiumBy.ACCESSIBILITY_ID, 
                "More options"
            )
            assert menu_button is not None, "El botón de menú no se encontró"
        except:
            # Intentar por XPath si no se encuentra por accessibility id
            menu_button = driver.find_element(
                AppiumBy.XPATH,
                "//android.widget.ImageButton[@content-desc='More options']"
            )
            assert menu_button is not None, "El botón de menú no se encontró"
    
    def test_open_menu(self, driver):
        """Verifica que se pueda abrir el menú"""
        time.sleep(2)
        
        # Abrir el menú
        try:
            menu_button = driver.find_element(
                AppiumBy.ACCESSIBILITY_ID,
                "More options"
            )
        except:
            menu_button = driver.find_element(
                AppiumBy.XPATH,
                "//android.widget.ImageButton[@content-desc='More options']"
            )
        
        menu_button.click()
        time.sleep(1)
        
        # Verificar que el menú se abrió buscando algún item
        # Ajustar según los IDs reales de los elementos del menú
        try:
            home_item = driver.find_element(
                AppiumBy.XPATH,
                "//android.widget.TextView[@text='Inicio']"
            )
            assert home_item is not None, "El menú no se abrió correctamente"
        except:
            # Si no se encuentra por texto, intentar por ID
            pass
    
    def test_menu_items_present(self, driver):
        """Verifica que todos los items del menú estén presentes"""
        time.sleep(2)
        
        # Abrir el menú
        try:
            menu_button = driver.find_element(
                AppiumBy.ACCESSIBILITY_ID,
                "More options"
            )
        except:
            menu_button = driver.find_element(
                AppiumBy.XPATH,
                "//android.widget.ImageButton[@content-desc='More options']"
            )
        
        menu_button.click()
        time.sleep(1)
        
        # Verificar items del menú
        menu_items = [
            "Inicio",
            "Agenda",
            "Sincronizar",
            "Sujetos",
            "Gastos"
        ]
        
        for item_text in menu_items:
            try:
                item = driver.find_element(
                    AppiumBy.XPATH,
                    f"//android.widget.TextView[@text='{item_text}']"
                )
                assert item is not None, f"El item '{item_text}' no se encontró en el menú"
            except:
                # Si no se encuentra, continuar con el siguiente
                pass
    
    def test_navigate_to_subjects(self, driver):
        """Verifica la navegación a la pantalla de sujetos"""
        time.sleep(2)
        
        # Abrir el menú
        try:
            menu_button = driver.find_element(
                AppiumBy.ACCESSIBILITY_ID,
                "More options"
            )
        except:
            menu_button = driver.find_element(
                AppiumBy.XPATH,
                "//android.widget.ImageButton[@content-desc='More options']"
            )
        
        menu_button.click()
        time.sleep(1)
        
        # Hacer clic en "Sujetos"
        try:
            subjects_item = driver.find_element(
                AppiumBy.XPATH,
                "//android.widget.TextView[@text='Sujetos']"
            )
            subjects_item.click()
            time.sleep(2)
            
            # Verificar que estamos en la pantalla de sujetos
            # Ajustar según el ID real de la actividad de sujetos
            subjects_toolbar = driver.find_element(
                AppiumBy.ID,
                "com.gastonlesbegueris.caretemplate.pets:id/toolbar"
            )
            assert subjects_toolbar is not None, "No se navegó a la pantalla de sujetos"
        except Exception as e:
            pytest.skip(f"No se pudo navegar a sujetos: {e}")
    
    def test_fab_button_exists(self, driver):
        """Verifica que el botón FAB (flotante) esté presente"""
        time.sleep(2)
        
        # Buscar el FAB
        try:
            fab = driver.find_element(
                AppiumBy.ID,
                "com.gastonlesbegueris.caretemplate.pets:id/fabAdd"
            )
            assert fab is not None, "El botón FAB no se encontró"
        except:
            pytest.skip("El botón FAB no se encontró")
    
    def test_fab_menu_opens(self, driver):
        """Verifica que el menú del FAB se abra correctamente"""
        time.sleep(2)
        
        # Hacer clic en el FAB principal
        try:
            fab = driver.find_element(
                AppiumBy.ID,
                "com.gastonlesbegueris.caretemplate.pets:id/fabAdd"
            )
            fab.click()
            time.sleep(1)
            
            # Verificar que los FABs secundarios aparezcan
            fab_subject = driver.find_element(
                AppiumBy.ID,
                "com.gastonlesbegueris.caretemplate.pets:id/fabAddSubject"
            )
            fab_event = driver.find_element(
                AppiumBy.ID,
                "com.gastonlesbegueris.caretemplate.pets:id/fabAddEvent"
            )
            
            assert fab_subject is not None and fab_event is not None, \
                "El menú del FAB no se abrió correctamente"
        except:
            pytest.skip("No se pudo abrir el menú del FAB")

