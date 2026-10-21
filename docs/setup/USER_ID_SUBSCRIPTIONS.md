# Sistema de ID Único de Usuario para Suscripciones

## 📋 Resumen

Se ha implementado un sistema de gestión de ID único de usuario que permite identificar a cada usuario de forma consistente para implementar suscripciones en el futuro.

## 🔑 Características

- **ID único persistente**: Cada usuario tiene un ID único que se mantiene entre sesiones
- **Firebase UID como preferencia**: Si el usuario está autenticado con Firebase, se usa su UID
- **UUID local como fallback**: Si no hay Firebase Auth, se genera un UUID local
- **Sincronización con Firestore**: El ID se sincroniza automáticamente con Firestore
- **Almacenamiento local**: El ID se guarda en SharedPreferences para acceso rápido

## 🚀 Uso Básico

### Obtener el userId (síncrono)

```java
import com.gastonlesbegueris.caretemplate.util.UserHelper;

// Obtener userId desde cualquier Activity/Fragment
String userId = UserHelper.getUserId(this);
if (userId != null) {
    // Usar userId para suscripciones, analytics, etc.
    Log.d("MyActivity", "User ID: " + userId);
}
```

### Obtener el userId (asíncrono)

```java
UserHelper.getUserIdAsync(this, new UserManager.UserIdCallback() {
    @Override
    public void onUserId(String userId) {
        // userId disponible
        // Usar para suscripciones, sincronización, etc.
    }
    
    @Override
    public void onError(Exception error) {
        // Manejar error
    }
});
```

### Inicializar al inicio de la app

Ya está integrado en `MainActivity.onCreate()`, pero puedes inicializarlo en otras actividades:

```java
UserHelper.initializeUser(this, new UserManager.UserIdCallback() {
    @Override
    public void onUserId(String userId) {
        // Usuario inicializado
    }
});
```

## 📦 Estructura en Firestore

El userId se sincroniza automáticamente a:

```
/users/{userId}/metadata/user_info
```

Contiene:
- `userId`: ID único del usuario
- `firebaseUid`: Firebase UID (si está autenticado)
- `isAnonymous`: Si la autenticación es anónima
- `createdAt`: Timestamp de creación
- `updatedAt`: Timestamp de última actualización

## 💳 Integración con Suscripciones

### Google Play Billing

Para usar con Google Play Billing, puedes usar el userId así:

```java
// Obtener userId
String userId = UserHelper.getUserId(this);

// Al verificar suscripción, asociar el purchase token con el userId
// Esto permite verificar suscripciones desde tu backend
String purchaseToken = purchase.getPurchaseToken();

// Guardar en Firestore o tu backend
Map<String, Object> subscription = new HashMap<>();
subscription.put("userId", userId);
subscription.put("purchaseToken", purchaseToken);
subscription.put("productId", purchase.getProducts().get(0));
subscription.put("purchaseTime", purchase.getPurchaseTime());

firestore.collection("subscriptions")
    .document(userId)
    .set(subscription);
```

### Verificar suscripción activa

```java
// En tu backend o Cloud Function
public boolean isUserSubscribed(String userId) {
    // Verificar en Firestore o tu base de datos
    DocumentReference subDoc = firestore
        .collection("subscriptions")
        .document(userId);
    
    // Verificar si tiene suscripción activa
    // ...
}
```

## 🔄 Flujo de Autenticación

1. **Primera vez (sin Firebase Auth)**:
   - Se genera un UUID local
   - Se guarda en SharedPreferences
   - Se sincroniza con Firestore

2. **Con Firebase Auth**:
   - Se usa el Firebase UID como userId
   - Se actualiza SharedPreferences
   - Se sincroniza con Firestore

3. **Cambio de autenticación**:
   - Si el Firebase UID cambia, se actualiza automáticamente
   - El userId siempre refleja el Firebase UID actual

## 📝 Ejemplo Completo: Verificar Suscripción

```java
public class SubscriptionManager {
    private final Context context;
    private final FirebaseFirestore firestore;
    
    public SubscriptionManager(Context context) {
        this.context = context;
        this.firestore = FirebaseFirestore.getInstance();
    }
    
    public void checkSubscription(SubscriptionCallback callback) {
        String userId = UserHelper.getUserId(context);
        if (userId == null) {
            callback.onResult(false, "No user ID available");
            return;
        }
        
        firestore.collection("subscriptions")
            .document(userId)
            .get()
            .addOnSuccessListener(document -> {
                if (document.exists()) {
                    boolean isActive = document.getBoolean("isActive");
                    long expiresAt = document.getLong("expiresAt");
                    boolean valid = isActive && expiresAt > System.currentTimeMillis();
                    callback.onResult(valid, valid ? "Active" : "Expired");
                } else {
                    callback.onResult(false, "No subscription found");
                }
            })
            .addOnFailureListener(e -> {
                callback.onResult(false, "Error: " + e.getMessage());
            });
    }
    
    public interface SubscriptionCallback {
        void onResult(boolean isSubscribed, String message);
    }
}
```

## ⚠️ Consideraciones Importantes

1. **Persistencia**: El userId se mantiene entre sesiones, incluso si el usuario desinstala y reinstala la app (si usa Firebase Auth anónima persistente)

2. **Privacidad**: El userId es único pero no contiene información personal identificable

3. **Migración**: Si cambias de sistema de autenticación, el userId puede cambiar. Considera mantener un mapeo en tu backend.

4. **Backend**: Para suscripciones robustas, verifica las suscripciones desde tu backend usando el userId y el purchase token de Google Play.

## 🔐 Seguridad

- El userId se almacena localmente en SharedPreferences
- Se sincroniza con Firestore (asegúrate de tener reglas de seguridad apropiadas)
- Para suscripciones, siempre verifica en tu backend, no confíes solo en el cliente

## 📚 Próximos Pasos

1. **Implementar Google Play Billing**: Integrar la librería de Google Play Billing
2. **Backend para verificación**: Crear Cloud Functions o backend para verificar suscripciones
3. **UI de suscripciones**: Crear pantalla para mostrar planes y gestionar suscripciones
4. **Límites premium**: Usar el userId para verificar límites premium vs. free

## 🛠️ Archivos Relacionados

- `UserManager.java`: Clase principal para gestionar el userId
- `UserHelper.java`: Helper estático para acceso fácil
- `MainActivity.java`: Inicialización del UserManager

