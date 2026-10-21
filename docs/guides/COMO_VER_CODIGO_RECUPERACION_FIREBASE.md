# 🔍 Cómo Ver tu Código de Recuperación en Firebase Console

Esta guía te ayudará a encontrar tu código de recuperación en Firebase Console para que no pierdas tus datos.

## 📍 Ubicación del Código en Firebase

Tu código de recuperación se guarda **dentro de tu documento de usuario** como una subcolección:

```
/users/{tu_userId}/recovery_codes/current
```

**Pasos detallados para encontrarlo:**

1. **Abre Firebase Console**
   - Ve a https://console.firebase.google.com
   - Selecciona tu proyecto

2. **Ve a Firestore Database**
   - En el menú lateral, haz clic en **"Firestore Database"**

3. **Encuentra la colección `users`**
   - En la lista de colecciones, busca **`users`**
   - Haz clic en `users` para abrirla

4. **Encuentra tu documento de usuario**
   - Verás una lista de documentos (cada uno es un `userId`)
   - Tu `userId` puede ser:
     - **Si usas Firebase Authentication**: Tu UID de Firebase (ejemplo: `abc123xyz456`)
     - **Si no usas Auth**: Un UUID local (ejemplo: `550e8400-e29b-41d4-a716-446655440000`)

5. **Cómo identificar tu documento:**
   - **Opción A**: Si tienes datos sincronizados, busca el documento que tenga subcolecciones como:
     - `events` (tus eventos)
     - `subjects` (tus sujetos/mascotas/autos)
     - `metadata` (metadatos del usuario)
   - **Opción B**: Si tienes varios usuarios, abre cada documento y revisa cuál tiene tus datos
   - **Opción C**: Revisa los documentos más recientes (si acabas de sincronizar)

6. **Abre tu documento de usuario**
   - Haz clic en tu documento (el `userId`)
   - Verás las subcolecciones disponibles, por ejemplo:
     - `events`
     - `subjects`
     - `metadata`
     - **`recovery_codes`** ← **¡Esta es la que buscas!**

7. **Accede al código de recuperación**
   - Haz clic en la subcolección **`recovery_codes`**
   - Verás un documento llamado **`current`**
   - Haz clic en el documento `current`
   - En los campos verás:
     - **`code`**: Tu código de recuperación (formato: `ABCD1234EFGH` - sin guiones, todo en mayúsculas)
     - `userId`: Tu ID de usuario
     - `createdAt`: Fecha de creación (timestamp)
     - `updatedAt`: Última actualización (timestamp)

**Nota**: Si no ves la subcolección `recovery_codes` en tu documento, significa que el código aún no se ha sincronizado. En ese caso, ve a la app y usa la opción "Código de recuperación" del menú para generarlo y sincronizarlo.

## 🔑 Formato del Código

- **En la app**: Se muestra con guiones: `ABCD-1234-EFGH`
- **En Firebase**: Se guarda sin guiones y en mayúsculas: `ABCD1234EFGH`

Si ves el código en Firebase sin guiones, puedes agregarlos manualmente:
- Toma los primeros 4 caracteres, luego un guión, luego los siguientes 4, luego otro guión, y los últimos 4
- Ejemplo: `ABCD1234EFGH` → `ABCD-1234-EFGH`

## 💡 Consejos

1. **Guarda el código en múltiples lugares:**
   - Notas del teléfono
   - Email a ti mismo
   - Documento en la nube (Google Drive, Dropbox, etc.)
   - Papel físico

2. **Si no encuentras tu código:**
   - Asegúrate de haber sincronizado la app al menos una vez
   - Verifica que tengas permisos de lectura en Firestore
   - Revisa los logs de la app para ver si hubo errores de sincronización

3. **Para verificar que el código funciona:**
   - Usa la opción "Recuperar datos con código" en la app
   - Ingresa el código que encontraste en Firebase
   - Si funciona, tus datos se restaurarán

## 🆘 Si No Encuentras tu Código

Si no puedes encontrar tu código en Firebase:

1. **Abre la app en tu teléfono**
2. **Ve al menú → "Código de recuperación"**
3. **Verás el video publicitario** (si es necesario)
4. **Copia el código que se muestra**
5. **Guárdalo inmediatamente** en un lugar seguro

El código se generará automáticamente si no existe, y se sincronizará con Firebase la próxima vez que sincronices.

## 📱 Ver el Código desde la App (Más Fácil)

La forma más fácil de ver tu código es desde la app:

1. Abre la app
2. Toca el menú (☰)
3. Selecciona **"Código de recuperación"**
4. Verás un video publicitario (una vez)
5. Se mostrará tu código de recuperación
6. **Copia y guárdalo** en un lugar seguro

---

**Nota**: El código de recuperación es único para cada usuario y es la única forma de recuperar tus datos si reinstalas la app o cambias de dispositivo. ¡Guárdalo bien!

