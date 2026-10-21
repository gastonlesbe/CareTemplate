# 🔐 Reglas de Firestore Corregidas

## ❌ Problema Detectado

La regla para `recovery_codes` estaba dentro de `match /apps/{appId}`, cuando debería estar al nivel superior.

## ✅ Reglas Corregidas

Copia y pega estas reglas en Firebase Console:

```javascript
rules_version = '2';

service cloud.firestore {
  match /databases/{database}/documents {
    
    // ===== CÓDIGOS DE RECUPERACIÓN (NIVEL SUPERIOR) =====
    // IMPORTANTE: Esta regla debe estar al nivel superior, NO dentro de /users/{userId}/apps/{appId}
    match /recovery_codes/{code} {
      allow read: if true; // Permitir lectura pública (necesario para recuperación)
      allow write: if request.auth != null; // Solo usuarios autenticados pueden escribir
    }
    
    // ===== DATOS DE USUARIOS =====
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
      
      // Permitir acceso a recovery_codes dentro del usuario (opcional, para búsqueda alternativa)
      match /recovery_codes/{document=**} {
        allow read, write: if request.auth != null && request.auth.uid == userId;
      }
      
      // Permitir acceso a metadata
      match /metadata/{document=**} {
        allow read, write: if request.auth != null && request.auth.uid == userId;
      }
    }
    
    // ===== COMPARTIR SUJETOS (OPCIONAL) =====
    match /shared_subjects/{code} {
      allow read: if true; // Cualquiera puede leer códigos compartidos
      allow write: if request.auth != null; // Solo usuarios autenticados pueden compartir
    }
  }
}
```

## 🔍 Diferencias Clave

### ❌ Estructura Incorrecta (Actual)
```
match /users/{userId} {
  match /apps/{appId} {
    match /recovery_codes/{code} {  // ❌ MAL - está dentro de apps
      allow read: if true;
    }
  }
}
```

Esto busca el código en: `/users/{userId}/apps/{appId}/recovery_codes/{code}`

### ✅ Estructura Correcta
```
match /recovery_codes/{code} {  // ✅ CORRECTO - nivel superior
  allow read: if true;
}

match /users/{userId} {
  ...
}
```

Esto busca el código en: `/recovery_codes/{code}` (nivel superior)

## 📝 Pasos para Corregir

1. Ve a [Firebase Console](https://console.firebase.google.com/)
2. Selecciona tu proyecto
3. Ve a **Firestore Database** → **Reglas**
4. Reemplaza las reglas con las corregidas de arriba
5. Haz clic en **Publicar**
6. Espera 2-5 minutos para que se propaguen

## ✅ Verificación

Después de publicar las reglas corregidas:

1. Verifica que aparezca "Última publicación: hace X minutos"
2. Espera 2-5 minutos
3. Prueba la recuperación con el código `3WTH-Z6H5-5CEU`
4. Debería funcionar sin errores de permisos

## 🎯 Por Qué Es Importante

El código de recuperación se guarda en:
- `/recovery_codes/{codigo_normalizado}` (colección global)
- `/users/{userId}/recovery_codes/current` (dentro del usuario)

La búsqueda primero intenta en `/recovery_codes/{code}` (nivel superior), por lo que la regla debe estar al nivel superior, no dentro de `/users/{userId}/apps/{appId}`.

