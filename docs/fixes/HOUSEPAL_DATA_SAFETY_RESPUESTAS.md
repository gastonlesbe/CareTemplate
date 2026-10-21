# Respuestas para Data Safety Declaration - HousePal

## Preguntas y Respuestas Correctas

### ✅ Pregunta 1: ¿Tu app recopila o comparte alguno de los tipos de datos que se requieren de los usuarios?
**Respuesta:** **Sí** ✅ (Ya respondiste correctamente)

---

### ✅ Pregunta 2: ¿Todos los datos de los usuarios que recopila tu app se encriptan en tránsito?
**Respuesta:** **Sí** ✅ (Ya respondiste correctamente)

**Explicación:** Todos los datos se transmiten mediante HTTPS (Firebase, Appodeal, etc.)

---

### ❓ Pregunta 3: ¿Cuál de los siguientes métodos de creación de cuentas admite la app?

**Respuesta:** **"Mi app no permite que los usuarios creen una cuenta"** ✅

**Explicación:**
- HousePal usa **Firebase Authentication con autenticación anónima**
- Los usuarios NO crean cuentas explícitamente (no hay formulario de registro)
- Firebase crea automáticamente un usuario anónimo en segundo plano para sincronización
- El usuario no ingresa nombre de usuario, contraseña, email, ni ningún dato personal para "crear cuenta"
- No hay OAuth, login con Google, Facebook, etc.

**NO selecciones:**
- ❌ Nombre de usuario y contraseña
- ❌ Nombre de usuario y otra forma de autenticación
- ❌ Nombre de usuario, contraseña y otros tipos de autenticación
- ❌ OAuth
- ❌ Otro

---

### ❓ Pregunta 4: ¿Los usuarios pueden acceder a tu app con cuentas creadas fuera de ella?

**Respuesta:** **"No"** ✅

**Explicación:**
- No hay autenticación con cuentas externas (Google, Facebook, Twitter, etc.)
- No hay login con cuentas de terceros
- La app solo usa autenticación anónima automática de Firebase (sin intervención del usuario)

---

### ✅ Pregunta 5: ¿Pones a disposición de los usuarios un método para que soliciten que se borren sus datos?
**Respuesta:** **"Sí"** ✅ (Ya respondiste correctamente)

**Nota:** Asegúrate de tener un método de contacto (email) donde los usuarios puedan solicitar la eliminación de sus datos.

---

## Resumen de Respuestas

| Pregunta | Respuesta |
|----------|-----------|
| ¿Recopila o comparte datos? | ✅ Sí |
| ¿Datos encriptados en tránsito? | ✅ Sí |
| ¿Métodos de creación de cuentas? | ✅ **Mi app no permite que los usuarios creen una cuenta** |
| ¿Acceso con cuentas externas? | ✅ **No** |
| ¿Método para borrar datos? | ✅ Sí |

---

## Notas Importantes

1. **Autenticación anónima ≠ Creación de cuenta**
   - Aunque Firebase crea usuarios anónimos técnicamente, desde la perspectiva del usuario final NO hay un proceso de "creación de cuenta"
   - El usuario no proporciona información personal ni realiza ninguna acción para "crear cuenta"
   - Por lo tanto, la respuesta correcta es "Mi app no permite que los usuarios creen una cuenta"

2. **Sincronización automática**
   - La sincronización con Firebase ocurre automáticamente en segundo plano
   - No requiere que el usuario "inicie sesión" o "cree cuenta"
   - Es transparente para el usuario

3. **Consistencia**
   - Estas respuestas son consistentes con el uso de autenticación anónima de Firebase
   - No contradicen la recopilación de datos (Advertising ID, Analytics) que ya declaraste

---

## Siguiente Paso

Después de responder estas preguntas, continúa con el **Paso 3** de la declaración de Data Safety, donde deberás especificar:
- Los tipos de datos recopilados (Device or other IDs → Advertising ID)
- Los propósitos (Publicidad, Análisis)
- Con quién se comparten (Appodeal, Google/Firebase)
