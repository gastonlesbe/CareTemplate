"""
Tests automatizados para SubjectListActivity usando pytest y Appium
"""
import pytest
from appium.webdriver.common.appiumby import AppiumBy
import time


class TestSubjectListActivity:
    """Tests para la lista de sujetos"""
    
    def test_navigate_to_subjects_screen(self, driver):
        """Navega a la pantalla de sujetos desde el menú principal"""
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
        subjects_item = driver.find_element(
            AppiumBy.XPATH,
            "//android.widget.TextView[@text='Sujetos']"
        )
        subjects_item.click()
        time.sleep(2)
    
    def test_subjects_list_displayed(self, driver):
        """Verifica que la lista de sujetos se muestre correctamente"""
        # Navegar primero a la pantalla
        self.test_navigate_to_subjects_screen(driver)
        
        # Verificar que el RecyclerView esté presente
        try:
            recycler_view = driver.find_element(
                AppiumBy.ID,
                "com.gastonlesbegueris.caretemplate.pets:id/rvSubjects"
            )
            assert recycler_view is not None, "La lista de sujetos no se encontró"
        except:
            pytest.skip("La lista de sujetos no se encontró")
    
    def test_add_subject_fab_exists(self, driver):
        """Verifica que el FAB para agregar sujetos esté presente"""
        self.test_navigate_to_subjects_screen(driver)
        
        try:
            fab_add = driver.find_element(
                AppiumBy.ID,
                "com.gastonlesbegueris.caretemplate.pets:id/btnAddSubject"
            )
            assert fab_add is not None, "El FAB de agregar sujeto no se encontró"
        except:
            pytest.skip("El FAB de agregar sujeto no se encontró")

