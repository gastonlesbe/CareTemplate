# 🔐 Sistema de Recuperación de Datos

## 📋 Resumen

Se ha implementado un sistema de **código de recuperación** que permite al usuario recuperar sus datos si:
- Borra la app y la reinstala
- Cambia de teléfono
- Pierde acceso a su dispositivo

## 🔑 Cómo Funciona

### 1. **Generación Automática del Código**

Cuando el usuario usa la app por primera vez:
- Se genera automáticamente un **código de recuperación único** (formato: `XXXX-XXXX-XXXX`)
- El código se guarda localmente en el dispositivo
- El código se sincroniza con Firebase Firestore
- El código vincula el dispositivo con el `userId` del usuario

### 2. **Estructura en Firestore**

El código se guarda en dos lugares:

```
/recovery_codes/{codigo_normalizado}
  - code: "ABCD1234EFGH"
  - userId: "firebase_uid_o_uuid_local"
  - createdAt: timestamp
  - updatedAt: timestamp

/users/{userId}/recovery_codes/current
  - code: "ABCD1234EFGH"
  - userId: "firebase_uid_o_uuid_local"
  - createdAt: timestamp
  - updatedAt: timestamp
```

### 3. **Recuperación de Datos**

Si el usuario reinstala la app o cambia de dispositivo:

1. Abre la app
2. Va al menú → **"Código de recuperación"**
3. Selecciona **"Recuperar datos con código"**
4. Ingresa su código de recuperación guardado
5. La app:
   - Busca el código en Firestore
   - Recupera el `userId` asociado
   - Descarga todos los datos del usuario desde Firebase
   - Restaura eventos, sujetos y configuraciones

## 📱 Uso para el Usuario

### Ver tu Código de Recuperación

1. Abre la app
2. Toca el menú (☰)
3. Selecciona **"Código de recuperación"**
4. Verás tu código único (ejemplo: `ABCD-1234-EFGH`)
5. **IMPORTANTE**: Copia y guarda este código en un lugar seguro:
   - Notas del teléfono
   - Email a ti mismo
   - Documento en la nube
   - Papel físico

### Recuperar Datos con el Código

1. Abre la app (nueva instalación o nuevo dispositivo)
2. Toca el menú (☰)
3. Selecciona **"Código de recuperación"**
4. Toca **"Recuperar datos con código"**
5. Ingresa tu código (puedes pegar si lo copiaste)
6. Toca **"Recuperar"**
7. Espera a que se descarguen tus datos
8. ¡Listo! Tus eventos, sujetos y configuraciones estarán restaurados

## ⚠️ Importante

### ¿Qué pasa si pierdo mi código?

- **Sin el código, NO podrás recuperar tus datos** si reinstalas la app o cambias de dispositivo
- El código es único y no se puede recuperar si se pierde
- **Guarda el código en un lugar seguro**

### ¿Qué pasa si cambio de teléfono?

1. **Antes de cambiar**: Asegúrate de tener tu código de recuperación guardado
2. **En el nuevo teléfono**: Instala la app e ingresa tu código de recuperación
3. Tus datos se descargarán automáticamente desde Firebase

### ¿Qué pasa si borro la app?

1. **Antes de borrar**: Asegúrate de tener tu código de recuperación guardado
2. **Después de reinstalar**: Ingresa tu código de recuperación
3. Tus datos se restaurarán desde Firebase

## 🔧 Detalles Técnicos

### Autenticación Anónima de Firebase

- La app usa **Firebase Auth anónimo** para identificar usuarios
- Cada dispositivo obtiene un UID único de Firebase
- El código de recuperación vincula este UID con el código

### Sincronización

- Los datos se sincronizan automáticamente con Firebase cuando hay conexión
- Al recuperar con el código, se descargan todos los datos del usuario
- Los datos se guardan localmente en Room Database

### Seguridad

- El código de recuperación es único y no se puede adivinar
- Solo el usuario que tiene el código puede acceder a sus datos
- Los datos están protegidos por las reglas de seguridad de Firestore

## 🚀 Mejoras Futuras

Posibles mejoras al sistema:

1. **Autenticación con Google/Email**: Permitir que el usuario se autentique con su cuenta de Google o email para tener un UID persistente sin necesidad de código
2. **Backup automático**: Hacer backup automático del código en Google Drive o iCloud
3. **Código QR**: Generar un código QR del código de recuperación para facilitar el guardado
4. **Múltiples códigos**: Permitir generar múltiples códigos de recuperación

## 📝 Notas para Desarrolladores

### Clases Principales

- `UserRecoveryManager`: Gestiona la generación, sincronización y recuperación de códigos
- `UserManager`: Gestiona el ID único del usuario
- `CloudSync`: Sincroniza datos entre dispositivo y Firebase

### Flujo de Recuperación

1. Usuario ingresa código → `UserRecoveryManager.recoverUserIdFromCode()`
2. Se busca el código en Firestore → `/recovery_codes/{codigo}`
3. Se obtiene el `userId` asociado
4. Se guarda el `userId` localmente
5. Se sincronizan los datos del usuario → `CloudSync.pull()`

### Reglas de Firestore Necesarias

Asegúrate de que las reglas de Firestore permitan:

```javascript
// Lectura de códigos de recuperación (solo el código correcto)
match /recovery_codes/{code} {
  allow read: if true; // Permitir lectura para recuperación
  allow write: if request.auth != null; // Solo usuarios autenticados pueden escribir
}

// Datos del usuario (solo el usuario puede acceder)
match /users/{userId} {
  allow read, write: if request.auth != null && request.auth.uid == userId;
}
```

