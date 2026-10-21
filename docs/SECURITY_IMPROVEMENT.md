# 🔐 Mejora de Seguridad para Compartir Sujetos

## ⚠️ Problema Actual

Las reglas de Firestore actuales permiten que **cualquier usuario autenticado** pueda leer los datos de **cualquier otro usuario** si conoce el `userId` y `subjectId`. Esto es un problema de seguridad.

### Vulnerabilidad

```javascript
// Reglas actuales (INSEGURAS)
match /users/{userId}/apps/{appId}/subjects/{subjectId} {
  allow read: if request.auth != null; // ❌ Cualquier usuario autenticado puede leer
}
```

**Riesgo**: Si alguien obtiene un código de compartir, puede ver el `ownerUserId` en `shared_subjects`, y luego intentar leer otros datos de ese usuario.

## ✅ Solución Segura

### Opción 1: Copiar datos completos a `shared_subjects` (RECOMENDADA)

Cuando se comparte un sujeto, copiar **todos los datos** del sujeto y sus eventos directamente a `shared_subjects`. Así, al importar, se lee desde `shared_subjects` sin necesidad de acceder a los datos del usuario dueño.

**Ventajas**:
- ✅ No requiere permitir lectura de datos de otros usuarios
- ✅ Más seguro: solo se puede acceder a datos que están explícitamente compartidos
- ✅ Las reglas pueden ser más restrictivas

**Desventajas**:
- ⚠️ Requiere modificar el código para copiar datos al compartir
- ⚠️ Los datos compartidos ocupan más espacio en Firestore

### Opción 2: Reglas más restrictivas (ACTUAL - MENOS SEGURA)

Mantener las reglas actuales pero con la limitación de que solo se puede leer si el sujeto está en `shared_subjects`. Sin embargo, esto es difícil de implementar en Firestore rules porque requiere hacer consultas.

## 🛡️ Reglas Seguras Recomendadas

```javascript
rules_version = '2';

service cloud.firestore {
  match /databases/{database}/documents {
    
    // Códigos de recuperación (público, necesario)
    match /recovery_codes/{code} {
      allow read: if true;
      allow write: if request.auth != null;
    }
    
    // Sujetos compartidos (público, pero solo contiene datos compartidos)
    match /shared_subjects/{code} {
      allow read: if true; // Cualquiera puede leer códigos compartidos
      allow write: if request.auth != null; // Solo usuarios autenticados pueden compartir
    }
    
    // Datos de usuarios (SOLO el dueño puede acceder)
    match /users/{userId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
      
      match /apps/{appId} {
        allow read, write: if request.auth != null && request.auth.uid == userId;
        
        match /subjects/{subjectId} {
          allow read, write: if request.auth != null && request.auth.uid == userId;
        }
        
        match /events/{eventId} {
          allow read, write: if request.auth != null && request.auth.uid == userId;
        }
      }
    }
  }
}
```

## 📋 Implementación Recomendada

1. **Al compartir**: Copiar datos completos del sujeto y eventos a `shared_subjects/{code}`
2. **Al importar**: Leer directamente de `shared_subjects/{code}` sin acceder a `/users/{ownerUserId}`
3. **Reglas**: Solo permitir lectura/escritura de `/users/{userId}` al propio usuario

## 🔒 Nivel de Seguridad

- **Actual**: ⚠️ Medio - Cualquier usuario autenticado puede leer datos de otros si conoce el `userId`
- **Recomendado**: ✅ Alto - Solo se puede acceder a datos explícitamente compartidos en `shared_subjects`

