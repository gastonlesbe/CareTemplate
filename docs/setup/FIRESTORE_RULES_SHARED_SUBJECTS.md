# 🔐 Reglas de Firestore para Compartir Sujetos

## ❌ Error Actual

```
PERMISSION_DENIED: Missing or insufficient permissions.
Write failed at shared_subjects/QWA8TAMNGPY3
```

Este error indica que las reglas de Firestore no permiten escribir en la colección `shared_subjects`.

## ✅ Reglas Completas de Firestore

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
    
    // ===== COMPARTIR SUJETOS (NIVEL SUPERIOR) =====
    // IMPORTANTE: Esta regla debe estar al nivel superior para que funcione correctamente
    match /shared_subjects/{code} {
      allow read: if true; // Cualquiera puede leer códigos compartidos (necesario para importar)
      allow write: if request.auth != null; // Solo usuarios autenticados pueden compartir
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
  }
}
```

## 📝 Pasos para Corregir

1. Ve a [Firebase Console](https://console.firebase.google.com/)
2. Selecciona tu proyecto (care-template-78bb1 o el que corresponda)
3. Ve a **Firestore Database** → **Reglas**
4. Reemplaza TODAS las reglas con las de arriba (incluyendo la regla de `shared_subjects`)
5. Haz clic en **Publicar**
6. Espera 2-5 minutos para que se propaguen los cambios

## 🔍 Verificación

Después de publicar las reglas:

1. Verifica que aparezca "Última publicación: hace X minutos"
2. Espera 2-5 minutos para que se propaguen
3. Prueba compartir un sujeto desde la app
4. Debería funcionar sin errores de permisos

## ⚠️ Importante

- La regla `match /shared_subjects/{code}` **DEBE estar al nivel superior**, no dentro de `/users/{userId}`
- La ruta en Firestore es: `/shared_subjects/{codigo_normalizado}`
- Los usuarios deben estar autenticados (incluso anónimamente) para poder escribir
- Cualquiera puede leer los códigos compartidos (necesario para importar)

## 🎯 Estructura de Datos

Cuando se comparte un sujeto, se crea un documento en:
```
/shared_subjects/{codigo_normalizado}
```

Con los siguientes campos:
- `code`: Código normalizado (sin guiones, mayúsculas)
- `subjectId`: ID del sujeto compartido
- `ownerUserId`: ID del usuario dueño del sujeto
- `createdAt`: Timestamp de creación
- `updatedAt`: Timestamp de actualización

