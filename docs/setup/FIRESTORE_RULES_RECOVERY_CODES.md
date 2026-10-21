# 🔐 Reglas de Firestore para Códigos de Recuperación

## ⚠️ Problema Común

Si recibes el error `permission_denied: Missing or insufficient permissions` al intentar recuperar datos con un código, significa que las reglas de Firestore no permiten leer los códigos de recuperación.

## ✅ Solución: Configurar Reglas de Firestore

### Paso 1: Acceder a Firebase Console

1. Ve a [Firebase Console](https://console.firebase.google.com/)
2. Selecciona tu proyecto
3. Ve a **Firestore Database** → **Reglas**

### Paso 2: Agregar Reglas para Códigos de Recuperación

Agrega estas reglas a tu archivo de reglas de Firestore:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    
    // ===== CÓDIGOS DE RECUPERACIÓN (NIVEL SUPERIOR) =====
    // ⚠️ IMPORTANTE: Esta regla DEBE estar al nivel superior, NO dentro de /users/{userId}/apps/{appId}
    // Permitir lectura pública de códigos de recuperación (necesario para recuperar datos)
    // Permitir escritura solo a usuarios autenticados
    match /recovery_codes/{code} {
      allow read: if true; // Cualquiera puede leer (necesario para recuperación)
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
      
      // Permitir acceso a recovery_codes dentro del usuario (para búsqueda alternativa)
      match /recovery_codes/{document=**} {
        allow read, write: if request.auth != null && request.auth.uid == userId;
      }
      
      // Permitir acceso a metadata
      match /metadata/{document=**} {
        allow read, write: if request.auth != null && request.auth.uid == userId;
      }
    }
    
    // ===== COMPARTIR SUJETOS =====
    // Permitir lectura de códigos compartidos (para importar sujetos)
    match /shared_subjects/{code} {
      allow read: if true; // Cualquiera puede leer códigos compartidos
      allow write: if request.auth != null; // Solo usuarios autenticados pueden compartir
    }
  }
}
```

### Paso 3: Publicar las Reglas

1. Haz clic en **Publicar**
2. Espera a que se publiquen (puede tomar unos segundos)
3. Verifica que aparezca "Última publicación: hace X minutos"

## 🔍 Verificación

### Verificar que las Reglas Estén Publicadas

1. Firebase Console → Firestore Database → Reglas
2. Verifica que las reglas estén guardadas y publicadas
3. Deberías ver: "Última publicación: hace X minutos"

### Probar la Recuperación

1. Abre la app
2. Ve al menú → "Código de recuperación"
3. Toca "Recuperar datos con código"
4. Ingresa un código de recuperación
5. Debería funcionar sin errores de permisos

## ⚠️ Notas de Seguridad

### ¿Por qué permitir lectura pública de códigos de recuperación?

- Los códigos de recuperación son únicos y aleatorios (12 caracteres alfanuméricos)
- La probabilidad de adivinar un código es extremadamente baja (1 en 282,110,990,745,600)
- Solo el código correcto permite acceder a los datos del usuario
- Sin el código, no se puede acceder a ningún dato

### Alternativa más segura (opcional)

Si prefieres mayor seguridad, puedes requerir autenticación para leer códigos:

```javascript
match /recovery_codes/{code} {
  allow read: if request.auth != null; // Solo usuarios autenticados pueden leer
  allow write: if request.auth != null; // Solo usuarios autenticados pueden escribir
}
```

**Nota**: Con esta regla, el usuario debe estar autenticado (anónimamente) antes de poder recuperar datos. La app ya maneja esto automáticamente.

## 🐛 Solución de Problemas

### Error: "permission_denied: Missing or insufficient permissions"

**Causa**: Las reglas de Firestore no permiten leer `/recovery_codes/{code}`

**Solución**: 
1. Verifica que las reglas estén publicadas en Firebase Console
2. Asegúrate de que la regla `allow read: if true;` esté presente para `/recovery_codes/{code}`
3. Espera unos minutos después de publicar las reglas (pueden tardar en aplicarse)

### Error: "Código no encontrado"

**Causa**: El código no existe en Firestore

**Solución**:
1. Verifica que el código sea correcto
2. Asegúrate de haber sincronizado tus datos al menos una vez antes de intentar recuperarlos
3. Verifica en Firebase Console que el código exista en `/recovery_codes/{codigo_normalizado}`

## 📝 Checklist

- [ ] Reglas de Firestore configuradas y publicadas
- [ ] Regla `allow read: if true;` presente para `/recovery_codes/{code}`
- [ ] Reglas publicadas exitosamente (verificado en Firebase Console)
- [ ] App recompilada y probada
- [ ] Recuperación con código funciona sin errores de permisos

## 🔗 Referencias

- [Firestore Security Rules](https://firebase.google.com/docs/firestore/security/get-started)
- [Firestore Rules Syntax](https://firebase.google.com/docs/firestore/security/rules-conditions)

