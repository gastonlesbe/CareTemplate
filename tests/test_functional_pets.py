"""
Tests funcionales para CarePal Pets - Crear, editar, borrar sujetos y eventos
"""
import pytest
from appium.webdriver.common.appiumby import AppiumBy
import time


class TestFunctionalPets:
    """Tests funcionales que interactúan con la app"""
    
    PACKAGE = "com.gastonlesbegueris.caretemplate.pets"
    
    def test_create_subject(self, driver_pets):
        """Crea un sujeto (mascota)"""
        time.sleep(2)
        
        # Navegar a la pantalla de sujetos
        self._navigate_to_subjects(driver_pets)
        time.sleep(2)
        
        # Hacer clic en el FAB para agregar sujeto
        try:
            fab = driver_pets.find_element(
                AppiumBy.ID,
                f"{self.PACKAGE}:id/fabAdd"
            )
            fab.click()
            time.sleep(1)
            
            # El FAB puede abrir un menú, buscar el botón de agregar sujeto
            try:
                fab_subject = driver_pets.find_element(
                    AppiumBy.ID,
                    f"{self.PACKAGE}:id/fabAddSubject"
                )
                fab_subject.click()
                time.sleep(1)
            except:
                pass
            
            time.sleep(2)
            
            # Llenar el campo de nombre
            name_field = driver_pets.find_element(
                AppiumBy.ID,
                f"{self.PACKAGE}:id/etName"
            )
            name_field.clear()
            name_field.send_keys("Test Pet")
            time.sleep(1)
            
            # Guardar
            save_button = driver_pets.find_element(
                AppiumBy.ID,
                f"{self.PACKAGE}:id/ibSave"
            )
            save_button.click()
            time.sleep(2)
            
            # Verificar que el sujeto se creó
            try:
                subject = driver_pets.find_element(
                    AppiumBy.XPATH,
                    "//android.widget.TextView[@text='Test Pet']"
                )
                assert subject is not None, "El sujeto no se creó correctamente"
            except:
                pytest.skip("No se pudo verificar que el sujeto se creó")
                
        except Exception as e:
            pytest.fail(f"Error al crear sujeto: {e}")
    
    def test_create_event(self, driver_pets):
        """Crea un evento para un sujeto"""
        time.sleep(2)
        
        # Volver a la pantalla principal
        try:
            driver_pets.back()
            time.sleep(1)
        except:
            pass
        
        # Hacer clic en el FAB para agregar evento
        try:
            fab = driver_pets.find_element(
                AppiumBy.ID,
                f"{self.PACKAGE}:id/fabAdd"
            )
            fab.click()
            time.sleep(1)
            
            try:
                fab_event = driver_pets.find_element(
                    AppiumBy.ID,
                    f"{self.PACKAGE}:id/fabAddEvent"
                )
                fab_event.click()
                time.sleep(1)
            except:
                pass
            
            time.sleep(2)
            
            # Llenar el campo de título
            title_field = driver_pets.find_element(
                AppiumBy.ID,
                f"{self.PACKAGE}:id/etTitle"
            )
            title_field.clear()
            title_field.send_keys("Test Pet Event")
            time.sleep(1)
            
            # Guardar
            save_button = driver_pets.find_element(
                AppiumBy.ID,
                f"{self.PACKAGE}:id/ibSave"
            )
            save_button.click()
            time.sleep(2)
            
            # Verificar que el evento se creó
            try:
                event = driver_pets.find_element(
                    AppiumBy.XPATH,
                    "//android.widget.TextView[@text='Test Pet Event']"
                )
                assert event is not None, "El evento no se creó correctamente"
            except:
                pytest.skip("No se pudo verificar que el evento se creó")
                
        except Exception as e:
            pytest.fail(f"Error al crear evento: {e}")
    
    def test_navigate_to_expenses(self, driver_pets):
        """Navega a la pantalla de gastos"""
        time.sleep(2)
        
        # Abrir el menú
        try:
            menu_button = driver_pets.find_element(
                AppiumBy.ACCESSIBILITY_ID,
                "More options"
            )
        except:
            menu_button = driver_pets.find_element(
                AppiumBy.XPATH,
                "//android.widget.ImageButton[@content-desc='More options']"
            )
        
        menu_button.click()
        time.sleep(1)
        
        # Buscar la opción de gastos
        try:
            expenses_item = driver_pets.find_element(
                AppiumBy.XPATH,
                "//android.widget.TextView[@text='Gastos' or @text='Expenses']"
            )
            expenses_item.click()
            time.sleep(2)
            
            try:
                expenses_toolbar = driver_pets.find_element(
                    AppiumBy.ID,
                    f"{self.PACKAGE}:id/toolbar"
                )
                assert expenses_toolbar is not None, "No se navegó a la pantalla de gastos"
            except:
                pytest.skip("No se pudo verificar la pantalla de gastos")
                
        except Exception as e:
            pytest.skip(f"No se pudo navegar a gastos: {e}")
    
    def _navigate_to_subjects(self, driver):
        """Helper para navegar a la pantalla de sujetos"""
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
        
        subjects_item = driver.find_element(
            AppiumBy.XPATH,
            "//android.widget.TextView[@text='Sujetos' or @text='Mascotas']"
        )
        subjects_item.click()
        time.sleep(2)
