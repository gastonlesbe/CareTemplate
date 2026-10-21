# 🔧 Solución al Error "failed_precondition" en Firestore

## ❌ Error Encontrado

```
pull subject error: failed_precondition
```

Este error indica que Firestore no puede ejecutar la consulta debido a:
1. **Reglas de seguridad** que no permiten la lectura
2. **Estructura de datos** que no existe (documento padre faltante)
3. **Índice compuesto** faltante para la consulta

## 🔍 Análisis del Código

El código intenta leer de:
```
/users/{uid}/apps/CareTemplate/subjects
```

Y hace una consulta con:
- `whereEqualTo("appType", appType)`
- `whereGreaterThan("updatedAt", last)`

## ✅ Solución Paso a Paso

### Paso 1: Configurar Reglas de Seguridad en Firestore

1. Ve a [Firebase Console](https://console.firebase.google.com/)
2. Proyecto: **care-template-78bb1**
3. Ve a **Firestore Database** → **Reglas**
4. Reemplaza las reglas con estas (permite lectura/escritura para usuarios autenticados):

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Permitir acceso a usuarios autenticados (incluyendo anónimos)
    match /users/{userId} {
      // Permitir acceso solo al propio usuario
      allow read, write: if request.auth != null && request.auth.uid == userId;
      
      // Permitir acceso a apps y sus subcolecciones
      match /apps/{appId} {
        allow read, write: if request.auth != null && request.auth.uid == userId;
        
        match /subjects/{subjectId} {
          allow read, write: if request.auth != null && request.auth.uid == userId;
        }
        
        match /events/{eventId} {
          allow read, write: if request.auth != null && request.auth.uid == userId;
        }
      }
      
      // Permitir acceso a metadata
      match /metadata/{document=**} {
        allow read, write: if request.auth != null && request.auth.uid == userId;
      }
    }
  }
}
```

5. Haz clic en **Publicar**

### Paso 2: Crear la Estructura Inicial en Firestore

El error puede ocurrir porque el documento padre `apps/CareTemplate` no existe. 

**Opción A: Crear manualmente en Firebase Console**

1. Ve a **Firestore Database** → **Datos**
2. Crea la siguiente estructura:
   - Colección: `users`
   - Documento: `{tu-uid}` (el UID del usuario autenticado)
   - Subcolección: `apps`
   - Documento: `CareTemplate` (sin datos, solo el documento)
   - Subcolecciones: `subjects` y `events` (se crearán automáticamente cuando se agreguen datos)

**Opción B: Crear automáticamente con código**

El código debería crear automáticamente la estructura cuando hace `push`, pero si no existe, podemos agregar un método para inicializarla.

### Paso 3: Verificar que el Usuario Esté Autenticado

Asegúrate de que el usuario esté autenticado antes de hacer el pull:

```java
FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
if (user == null) {
    // Autenticar primero
    FirebaseAuth.getInstance().signInAnonymously()...
}
```

### Paso 4: Crear Índice Compuesto (Si es Necesario)

Si Firestore te muestra un error sobre índice faltante:

1. Ve a **Firestore Database** → **Índices**
2. Haz clic en el enlace del error (si aparece)
3. O crea manualmente:
   - Colección: `subjects`
   - Campos indexados:
     - `appType` (Ascendente)
     - `updatedAt` (Ascendente)
   - Alcance: Colección

## 🔍 Verificación

### Verificar que las Reglas Estén Publicadas

1. Firebase Console → Firestore Database → Reglas
2. Verifica que las reglas estén guardadas y publicadas
3. Deberías ver: "Última publicación: hace X minutos"

### Verificar Autenticación

En los logs de la app, verifica que:
- El usuario esté autenticado: `FirebaseAuth.getInstance().getCurrentUser() != null`
- El UID esté disponible

### Verificar Estructura en Firestore

1. Firebase Console → Firestore Database → Datos
2. Navega a: `users/{uid}/apps/CareTemplate`
3. Verifica que el documento `CareTemplate` exista (puede estar vacío)

## 🚨 Solución Temporal: Crear Documento Inicial

Si el problema persiste, podemos modificar el código para crear el documento padre automáticamente antes de hacer el pull. Esto se puede hacer agregando un método que inicialice la estructura.

## 📝 Checklist

- [ ] Reglas de Firestore configuradas y publicadas
- [ ] Usuario autenticado (anónimo o no)
- [ ] Documento `users/{uid}/apps/CareTemplate` existe en Firestore
- [ ] Índice compuesto creado (si es necesario)
- [ ] App recompilada y probada

## 🔗 Referencias

- [Firestore Security Rules](https://firebase.google.com/docs/firestore/security/get-started)
- [Firestore Indexes](https://firebase.google.com/docs/firestore/query-data/indexing)

