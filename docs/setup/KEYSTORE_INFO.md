# Información del Keystore de PetPal

## ⚠️ IMPORTANTE - LEE ESTO

**Este keystore es necesario para actualizar la app en Google Play Store.**

### Información del Keystore Creado:

- **Archivo**: `petpal-release-key.jks`
- **Alias**: `petpal-key`
- **Contraseña del keystore**: `petpal2024`
- **Contraseña de la clave**: `petpal2024`
- **Validez**: 10,000 días (~27 años)

### ⚠️ ACCIONES REQUERIDAS:

1. **Guarda el archivo `petpal-release-key.jks` en un lugar seguro**
   - Haz una copia de seguridad
   - Guárdalo en un lugar seguro (nube cifrada, USB, etc.)
   - Sin este archivo NO podrás actualizar la app

2. **Guarda las contraseñas de forma segura**
   - Contraseña actual: `petpal2024`
   - Considera cambiarla por una más segura si lo deseas

3. **El archivo `keystore.properties` NO se subirá a git** (está en .gitignore)

### Cambiar la Contraseña (Opcional):

Si quieres cambiar la contraseña por una más segura:

```bash
keytool -storepasswd -keystore petpal-release-key.jks
```

Luego actualiza `keystore.properties` con la nueva contraseña.

### Generar Release:

Ahora puedes generar el AAB firmado:

```bash
./gradlew bundlePetsRelease
```

El archivo estará en: `app/build/outputs/bundle/petsRelease/app-pets-release.aab`

