# Solución: Invalid Data Safety Declaration - HousePal

## Problema

Google Play Console detectó que HousePal está transmitiendo **"Device or other IDs"** (IDs de dispositivo u otros) sin haberlo declarado en la Data Safety declaration.

**Ubicación del problema:**
- Seguridad de los datos (información no declarada: Dispositivo u otros IDs)
- Código de la versión: 5

## Causa del Problema

HousePal utiliza dos servicios que recopilan automáticamente IDs de dispositivo:

1. **Appodeal SDK** (Publicidad)
   - Recopila el **Advertising ID** (ID de publicidad de Google)
   - Necesario para mostrar anuncios personalizados y maximizar ingresos
   - Appodeal internamente usa AdMob y otras redes publicitarias

2. **Firebase Analytics**
   - Recopila IDs de dispositivo para análisis y estadísticas
   - Datos anónimos y agregados

## Solución: Actualizar Data Safety Declaration

### Paso 1: Acceder a Data Safety en Google Play Console

1. Ve a [Google Play Console](https://play.google.com/console)
2. Selecciona la app **HousePal**
3. En el menú lateral, ve a **Política** → **Seguridad de los datos** (Data Safety)
4. Haz clic en **"Iniciar"** o **"Editar"** si ya tienes una declaración

### Paso 2: Declarar "Device or other IDs"

#### 2.1 Datos Recopilados

**¿Recopila o comparte datos?**
- Selecciona: **Sí**

**Tipos de datos recopilados:**
- Busca y selecciona: **"Dispositivo u otros IDs"** (Device or other IDs)
  - Dentro de esta categoría, marca específicamente:
    - ✅ **Advertising ID** (ID de publicidad)

#### 2.2 Propósito de la Recopilación

Para **"Dispositivo u otros IDs"**, indica que se usa para:

1. **Publicidad o marketing**
   - ✅ **Sí** - Se usa para publicidad
   - Explicación: "Usamos el Advertising ID para mostrar anuncios relevantes a través de Appodeal SDK. Este ID se comparte con redes publicitarias para personalizar anuncios."

2. **Análisis**
   - ✅ **Sí** - Se usa para análisis
   - Explicación: "Firebase Analytics recopila IDs de dispositivo de forma anónima para analizar el uso de la aplicación y mejorar nuestros servicios."

#### 2.3 Compartir de Datos

**¿Se comparten estos datos con terceros?**
- ✅ **Sí**

**Con quién se comparte:**
1. **Appodeal** (Proveedor de publicidad)
   - Tipo: Proveedor de servicios
   - Propósito: Publicidad
   - Datos compartidos: Advertising ID

2. **Google (Firebase Analytics)**
   - Tipo: Proveedor de servicios
   - Propósito: Análisis
   - Datos compartidos: IDs de dispositivo (anónimos)

#### 2.4 Requisitos de Seguridad

**¿Se encriptan los datos en tránsito?**
- ✅ **Sí** - Todos los datos se transmiten mediante HTTPS

**¿Se puede solicitar la eliminación de datos?**
- ✅ **Sí** - Los usuarios pueden solicitar la eliminación de datos

### Paso 3: Verificar Consistencia con Política de Privacidad

Asegúrate de que tu política de privacidad mencione la recopilación de IDs de dispositivo. Si no tienes una política específica para HousePal, puedes usar la de FamilyPal como base y adaptarla.

**Elementos que debe incluir la política de privacidad:**

1. Mención de recopilación de Advertising ID
2. Explicación del uso para publicidad (Appodeal)
3. Mención de Firebase Analytics
4. Enlaces a políticas de terceros:
   - [Política de Privacidad de Appodeal](https://appodeal.com/privacy-policy)
   - [Política de Privacidad de Google Analytics](https://policies.google.com/privacy)

### Paso 4: Guardar y Enviar para Revisión

1. Revisa toda la información ingresada
2. Haz clic en **"Guardar"**
3. Si es necesario, actualiza la versión de la app o envía los cambios para revisión

## Información Detallada para la Declaración

### Resumen de Datos Recopilados

| Tipo de Dato | Recopilado | Compartido | Propósito |
|--------------|------------|------------|-----------|
| Advertising ID | ✅ Sí | ✅ Sí (Appodeal) | Publicidad |
| Device IDs (Analytics) | ✅ Sí | ✅ Sí (Google) | Análisis |

### Texto Sugerido para Explicaciones

**Para Publicidad:**
```
HousePal utiliza Appodeal SDK para mostrar anuncios en la aplicación. 
Appodeal recopila el Advertising ID (ID de publicidad de Google) para 
mostrar anuncios personalizados y relevantes. Este ID se comparte con 
redes publicitarias asociadas a Appodeal (incluyendo Google AdMob) 
únicamente para fines publicitarios.
```

**Para Análisis:**
```
Firebase Analytics recopila IDs de dispositivo de forma anónima para 
analizar el uso de la aplicación, identificar problemas y mejorar 
nuestros servicios. Estos datos son agregados y no identifican 
personalmente a los usuarios.
```

## Verificación Post-Declaración

Después de actualizar la declaración:

1. **Espera 24-48 horas** para que Google Play Console procese los cambios
2. Verifica que el problema desaparezca de la sección de problemas
3. Si el problema persiste, revisa que:
   - Todos los campos estén completos
   - La política de privacidad sea accesible y consistente
   - No haya contradicciones en la declaración

## Notas Importantes

⚠️ **No deshabilites la recopilación de Advertising ID** si quieres mantener los ingresos por publicidad. Appodeal necesita este ID para funcionar correctamente.

✅ **Es normal y legal** recopilar Advertising ID para publicidad, siempre que:
- Se declare correctamente en Data Safety
- Se informe en la política de privacidad
- Se respeten las preferencias del usuario (si opta por no recibir publicidad personalizada)

## Referencias

- [Guía de Data Safety de Google Play](https://support.google.com/googleplay/android-developer/answer/10787469)
- [Política de Privacidad de Appodeal](https://appodeal.com/privacy-policy)
- [Política de Privacidad de Google Analytics](https://policies.google.com/privacy)

---

**Última actualización:** Diciembre 2024
