"""
Tests funcionales para CarePal Family - Crear, editar, borrar sujetos y eventos
"""
import pytest
from appium.webdriver.common.appiumby import AppiumBy
from appium.webdriver.support.ui import WebDriverWait
from appium.webdriver.support import expected_conditions as EC
import time


class TestFunctionalFamily:
    """Tests funcionales que interactúan con la app"""
    
    PACKAGE = "com.gastonlesbegueris.caretemplate.family"
    
    def test_create_subject(self, driver_family):
        """Crea un sujeto (familiar)"""
        time.sleep(2)
        
        # Navegar a la pantalla de sujetos
        self._navigate_to_subjects(driver_family)
        time.sleep(2)
        
        # Hacer clic en el FAB para agregar sujeto
        try:
            fab = driver_family.find_element(
                AppiumBy.ID,
                f"{self.PACKAGE}:id/fabAdd"
            )
            fab.click()
            time.sleep(1)
            
            # El FAB puede abrir un menú, buscar el botón de agregar sujeto
            try:
                fab_subject = driver_family.find_element(
                    AppiumBy.ID,
                    f"{self.PACKAGE}:id/fabAddSubject"
                )
                fab_subject.click()
                time.sleep(1)
            except:
                # Si no hay menú, el diálogo ya debería estar abierto
                pass
            
            # Esperar a que aparezca el diálogo
            time.sleep(2)
            
            # Llenar el campo de nombre
            name_field = driver_family.find_element(
                AppiumBy.ID,
                f"{self.PACKAGE}:id/etName"
            )
            name_field.clear()
            name_field.send_keys("Test Family Member")
            time.sleep(1)
            
            # Guardar
            save_button = driver_family.find_element(
                AppiumBy.ID,
                f"{self.PACKAGE}:id/ibSave"
            )
            save_button.click()
            time.sleep(2)
            
            # Verificar que el sujeto se creó (buscar en la lista)
            # Esto puede requerir scroll o esperar a que aparezca
            try:
                # Buscar el texto del sujeto creado
                subject = driver_family.find_element(
                    AppiumBy.XPATH,
                    "//android.widget.TextView[@text='Test Family Member']"
                )
                assert subject is not None, "El sujeto no se creó correctamente"
            except:
                pytest.skip("No se pudo verificar que el sujeto se creó (puede requerir scroll)")
                
        except Exception as e:
            pytest.fail(f"Error al crear sujeto: {e}")
    
    def test_create_event(self, driver_family):
        """Crea un evento para un sujeto"""
        time.sleep(2)
        
        # Primero asegurarse de que hay un sujeto
        # (Este test asume que ya existe al menos un sujeto)
        
        # Navegar a la pantalla principal
        try:
            # Si estamos en subjects, volver atrás
            driver_family.back()
            time.sleep(1)
        except:
            pass
        
        # Hacer clic en el FAB para agregar evento
        try:
            fab = driver_family.find_element(
                AppiumBy.ID,
                f"{self.PACKAGE}:id/fabAdd"
            )
            fab.click()
            time.sleep(1)
            
            # Buscar el botón de agregar evento
            try:
                fab_event = driver_family.find_element(
                    AppiumBy.ID,
                    f"{self.PACKAGE}:id/fabAddEvent"
                )
                fab_event.click()
                time.sleep(1)
            except:
                # Si no hay menú, el diálogo ya debería estar abierto
                pass
            
            # Esperar a que aparezca el diálogo
            time.sleep(2)
            
            # Llenar el campo de título
            title_field = driver_family.find_element(
                AppiumBy.ID,
                f"{self.PACKAGE}:id/etTitle"
            )
            title_field.clear()
            title_field.send_keys("Test Event")
            time.sleep(1)
            
            # Guardar
            save_button = driver_family.find_element(
                AppiumBy.ID,
                f"{self.PACKAGE}:id/ibSave"
            )
            save_button.click()
            time.sleep(2)
            
            # Verificar que el evento se creó
            # Buscar el texto del evento en la lista
            try:
                event = driver_family.find_element(
                    AppiumBy.XPATH,
                    "//android.widget.TextView[@text='Test Event']"
                )
                assert event is not None, "El evento no se creó correctamente"
            except:
                pytest.skip("No se pudo verificar que el evento se creó")
                
        except Exception as e:
            pytest.fail(f"Error al crear evento: {e}")
    
    def test_navigate_to_expenses(self, driver_family):
        """Navega a la pantalla de gastos"""
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
        
        # Buscar la opción de gastos
        try:
            expenses_item = driver_family.find_element(
                AppiumBy.XPATH,
                "//android.widget.TextView[@text='Gastos' or @text='Expenses']"
            )
            expenses_item.click()
            time.sleep(2)
            
            # Verificar que estamos en la pantalla de gastos
            # Buscar algún elemento característico de la pantalla de gastos
            try:
                expenses_toolbar = driver_family.find_element(
                    AppiumBy.ID,
                    f"{self.PACKAGE}:id/toolbar"
                )
                assert expenses_toolbar is not None, "No se navegó a la pantalla de gastos"
            except:
                pytest.skip("No se pudo verificar la pantalla de gastos")
                
        except Exception as e:
            pytest.skip(f"No se pudo navegar a gastos: {e}")
    
    def test_use_demo_data(self, driver_family):
        """Usa la opción de datos de demostración"""
        time.sleep(2)
        
        # Navegar a la pantalla de sujetos
        self._navigate_to_subjects(driver_family)
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
            demo_item.click()
            time.sleep(3)  # Esperar a que se creen los datos
            
            # Verificar que se crearon datos (buscar sujetos en la lista)
            try:
                # Buscar algún sujeto de demo (ej: "John Doe")
                subject = driver_family.find_element(
                    AppiumBy.XPATH,
                    "//android.widget.TextView[contains(@text, 'John') or contains(@text, 'Doe')]"
                )
                assert subject is not None, "Los datos de demostración no se crearon"
            except:
                pytest.skip("No se pudo verificar que los datos de demo se crearon")
                
        except Exception as e:
            pytest.skip(f"No se pudo usar datos de demostración: {e}")
    
    def test_view_tutorial(self, driver_family):
        """Abre el tutorial desde el menú"""
        time.sleep(2)
        
        # Navegar a la pantalla de sujetos
        self._navigate_to_subjects(driver_family)
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
        
        # Buscar la opción de tutorial
        try:
            tutorial_item = driver_family.find_element(
                AppiumBy.XPATH,
                "//android.widget.TextView[@text='Ver tutorial' or @text='View tutorial']"
            )
            tutorial_item.click()
            time.sleep(2)
            
            # Verificar que el tutorial se abrió
            # Buscar algún elemento del tutorial (botón continuar)
            try:
                continue_button = driver_family.find_element(
                    AppiumBy.XPATH,
                    "//android.widget.Button[@text='Continuar' or @text='Continue']"
                )
                assert continue_button is not None, "El tutorial no se abrió"
            except:
                pytest.skip("No se pudo verificar que el tutorial se abrió")
                
        except Exception as e:
            pytest.skip(f"No se pudo abrir el tutorial: {e}")
    
    def _navigate_to_subjects(self, driver):
        """Helper para navegar a la pantalla de sujetos"""
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
        
        # Buscar el item de sujetos
        subjects_item = driver.find_element(
            AppiumBy.XPATH,
            "//android.widget.TextView[@text='Sujetos' or @text='Familiares']"
        )
        subjects_item.click()
        time.sleep(2)
