# Verificar Banners de Appodeal en Todos los Flavors

## ✅ Cambios Realizados

Se corrigió el código para permitir que cada flavor inicialice Appodeal correctamente con su propia app key.

## 📋 Verificación de App Keys

Cada flavor tiene su propia app key de Appodeal. Verifica que estén correctamente configuradas:

### CarePal: Pets
- **Package Name**: `com.gastonlesbegueris.caretemplate.pets`
- **App Key**: `6816c9cac0c2ad9737e03dcfbe54da50740950c8c543d0e4`
- **Archivo**: `app/src/pets/res/values/strings.xml`
- **Estado**: ✅ Funciona

### CarePal: Cars
- **Package Name**: `com.gastonlesbegueris.caretemplate.cars`
- **App Key**: `3241651304ced785543b7d67248a52c72dd9c90d36f698ca`
- **Archivo**: `app/src/cars/res/values/strings.xml`
- **Estado**: ⚠️ Verificar en Appodeal Dashboard

### CarePal: Family
- **Package Name**: `com.gastonlesbegueris.caretemplate.family`
- **App Key**: `6fa8b7398b35115a560cd591b1c5ac04119aafe691a1a028`
- **Archivo**: `app/src/family/res/values/strings.xml`
- **Estado**: ⚠️ Verificar en Appodeal Dashboard

### CarePal: Houses
- **Package Name**: `com.gastonlesbegueris.caretemplate.house`
- **App Key**: `12d4ea28bdce8e90df4f41bab7bebe5adb144e8938eb260c`
- **Archivo**: `app/src/house/res/values/strings.xml`
- **Estado**: ⚠️ Verificar en Appodeal Dashboard

---

## 🔍 Pasos para Verificar en Appodeal Dashboard

1. **Ve a Appodeal Dashboard**: https://app.appodeal.com/

2. **Para cada app (Cars, Family, Houses)**:
   - Busca la app por su package name o app key
   - Verifica que:
     - ✅ La app esté **activa/approved**
     - ✅ El **package name** coincida exactamente
     - ✅ Los **banners estén habilitados**
     - ✅ La app key sea la correcta

3. **Verifica los Package Names**:
   - CarePal: Cars → `com.gastonlesbegueris.caretemplate.cars`
   - CarePal: Family → `com.gastonlesbegueris.caretemplate.family`
   - CarePal: Houses → `com.gastonlesbegueris.caretemplate.house`

---

## 🐛 Debugging

Si los banners aún no aparecen después de verificar en Appodeal:

### 1. Revisa los Logs

Usa `adb logcat` para ver los logs de Appodeal:

```bash
adb logcat | grep -i appodeal
```

Busca estos mensajes:
- `"Appodeal initialized successfully"` - Confirma que se inicializó
- `"Banner loaded successfully"` - Confirma que el banner se cargó
- `"Banner shown"` - Confirma que el banner se mostró
- `"Banner failed to load"` - Indica un problema

### 2. Verifica el Package Name

En los logs, verifica que el package name sea correcto:
```
Package name: com.gastonlesbegueris.caretemplate.cars
```

### 3. Verifica la App Key

En los logs, verifica que la app key sea correcta:
```
App key: 3241651304...
```

---

## ⚠️ Problemas Comunes

### Banner no aparece

**Posibles causas:**
1. **App no activa en Appodeal Dashboard**
   - Solución: Activa la app en Appodeal Dashboard

2. **Package name no coincide**
   - Solución: Verifica que el package name en Appodeal sea exactamente: `com.gastonlesbegueris.caretemplate.cars` (sin espacios, sin errores)

3. **App key incorrecta**
   - Solución: Verifica que la app key en `strings.xml` coincida con la del dashboard

4. **Banners no habilitados en Appodeal**
   - Solución: Habilita banners en la configuración de la app en Appodeal Dashboard

5. **No hay inventario de anuncios**
   - Solución: Espera unos minutos o verifica en modo test

### Banner aparece solo en Pets

**Causa**: Probablemente solo Pets está activa/approved en Appodeal Dashboard.

**Solución**: 
1. Ve a Appodeal Dashboard
2. Activa/approve las apps de Cars, Family y Houses
3. Verifica que los package names sean correctos
4. Espera unos minutos para que se propague

---

## ✅ Checklist de Verificación

Para cada flavor (Cars, Family, Houses):

- [ ] App existe en Appodeal Dashboard
- [ ] App está activa/approved
- [ ] Package name coincide exactamente
- [ ] App key coincide con la del código
- [ ] Banners están habilitados
- [ ] Logs muestran "Appodeal initialized successfully"
- [ ] Logs muestran "Banner loaded successfully" o "Banner shown"

---

## 📝 Notas

- Cada flavor es una app separada con su propio package name
- Cada app necesita estar registrada en Appodeal Dashboard
- Los banners pueden tardar unos minutos en aparecer después de activar la app
- En modo debug, Appodeal muestra anuncios de prueba automáticamente

