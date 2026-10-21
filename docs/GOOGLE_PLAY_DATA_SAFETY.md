# Declaración de Seguridad de Datos - Google Play Console

## Problema
Google Play detectó que la aplicación está usando AppLovin SDK (a través de Appodeal) que recopila **Device IDs** y otros datos, pero esto no está declarado en la Política de Seguridad de Datos.

## Solución: Declarar los Datos en Google Play Console

### Pasos a seguir:

1. **Ir a Google Play Console** → Tu app → **Contenido de la aplicación** → **Seguridad de los datos**

2. **Sección: Recopilación de datos y seguridad**
   - Seleccionar **"Sí"** (tu app recopila datos)

3. **Sección: Tipos de datos**
   - Buscar y seleccionar: **"Dispositivo u otros IDs"**
   - Indicar:
     - **¿Se recopilan?** → **Sí**
     - **¿Se comparten?** → **Sí** (con AppLovin/Appodeal)
     - **Finalidad**: **Publicidad o marketing**
     - **Datos compartidos**: **Sí, con terceros**

4. **Sección: Datos compartidos**
   - Indicar que los datos se comparten con **terceros** (AppLovin/Appodeal)

5. **Guardar y enviar** los cambios para revisión

### Nota sobre AppLovin
AppLovin es un SDK de publicidad que se incluye automáticamente como parte de Appodeal SDK. Recopila Device IDs para:
- Mostrar anuncios relevantes
- Medir el rendimiento de los anuncios
- Prevenir fraudes

### Alternativa
Si no quieres declarar estos datos, puedes remover completamente Appodeal SDK del proyecto (esto eliminará todos los banners de publicidad).



