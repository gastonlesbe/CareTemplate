# Checklist para Publicar PetPal v1.0

## ✅ Antes de Generar el Release

### Configuración Básica
- [ ] Version Code: 1 (primera versión)
- [ ] Version Name: "1.0"
- [ ] App ID de AdMob configurado en `app/src/pets/res/values/strings.xml`
- [ ] Banner ID de AdMob configurado

### Keystore (Firma)
- [ ] Keystore creado (`./create_keystore.sh`)
- [ ] `keystore.properties` configurado (NO subir a git)
- [ ] Keystore guardado en lugar seguro
- [ ] Contraseñas guardadas de forma segura

### Testing
- [ ] App probada en dispositivo real
- [ ] Todos los features funcionando
- [ ] AdMob banners mostrándose correctamente
- [ ] Firebase sync funcionando
- [ ] Sin crashes conocidos

### Google Play Console
- [ ] Cuenta de desarrollador creada ($25 USD una vez)
- [ ] App creada en Play Console
- [ ] Store listing preparada:
  - [ ] Título: "PetPal"
  - [ ] Descripción corta
  - [ ] Descripción completa
  - [ ] Screenshots (mínimo 2)
  - [ ] Icono de la app (512x512)
  - [ ] Feature graphic (1024x500)
- [ ] Privacy Policy URL (requerida)
- [ ] Categoría seleccionada
- [ ] Contenido calificado (PEGI/ESRB)

## 📦 Generar Release

### Opción 1: AAB (Recomendado para Google Play)
```bash
./gradlew bundlePetsRelease
```
Archivo: `app/build/outputs/bundle/petsRelease/app-pets-release.aab`

### Opción 2: APK (Para testing)
```bash
./gradlew assemblePetsRelease
```
Archivo: `app/build/outputs/apk/pets/release/app-pets-release.apk`

## 📤 Subir a Google Play

1. Ve a [Google Play Console](https://play.google.com/console)
2. Selecciona tu app "PetPal"
3. Ve a "Producción" → "Crear nueva versión"
4. Sube el archivo `.aab`
5. Completa la información de la versión
6. Revisa y publica

## ⚠️ Recordatorios Importantes

- **Keystore**: Sin el keystore NO podrás actualizar la app. Guárdalo de forma segura.
- **Version Code**: Debe incrementarse en cada release (1, 2, 3...)
- **Version Name**: Puede ser cualquier string ("1.0", "1.1", "2.0"...)
- **Privacy Policy**: Google Play requiere una URL de política de privacidad
- **Tiempo de revisión**: Primera publicación puede tardar varios días

## 🎯 Próximos Pasos Después de Publicar

- [ ] Monitorear crash reports en Play Console
- [ ] Revisar comentarios de usuarios
- [ ] Preparar actualizaciones según feedback
- [ ] Considerar lanzar otros flavors (CarPal, FamilyPal, HousePal)

