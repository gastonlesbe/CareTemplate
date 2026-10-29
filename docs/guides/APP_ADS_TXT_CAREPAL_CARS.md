# Configuración de app-ads.txt para CarePal: Cars

## ✅ Archivo app-ads.txt

El archivo `app-ads.txt` ya está creado en la raíz del proyecto con el contenido correcto:

```
google.com, pub-9841764898906750, DIRECT, f08c47fec0942fa0
```

## 📋 Pasos para CarePal: Cars

### Paso 1: Verificar que el archivo esté publicado

El archivo debe estar disponible en:
```
https://gastonlesbe.github.io/app-ads.txt
```

**Verifica que funcione:**
1. Abre tu navegador
2. Ve a: `https://gastonlesbe.github.io/app-ads.txt`
3. Debe mostrar exactamente:
   ```
   google.com, pub-9841764898906750, DIRECT, f08c47fec0942fa0
   ```

Si no funciona, sigue estos pasos:

#### Publicar en GitHub Pages

1. **Ve a tu repositorio de GitHub**: `gastonlesbe.github.io`
   - Si no existe, créalo con ese nombre exacto

2. **Asegúrate de que el archivo `app-ads.txt` esté en la raíz del repositorio**

3. **Habilita GitHub Pages**:
   - Ve a Settings → Pages
   - En "Source", selecciona la rama `main` o `master`
   - Guarda

4. **Verifica la URL**: `https://gastonlesbe.github.io/app-ads.txt`

---

### Paso 2: Verificar el dominio en Google Play Console

1. **Ve a Google Play Console**: https://play.google.com/console

2. **Selecciona la app "CarePal: Cars"**

3. **Ve a la configuración del sitio web**:
   - Opción A: **"Store presence"** → **"Store listing"** → **"App details"** → **"Website"**
   - Opción B: **"Configuración de la app"** → **"Detalles de la app"** → **"Sitio web"**
   - Opción C: **"App content"** → **"App details"** → **"Website"**

4. **El dominio debe ser exactamente**:
   ```
   https://gastonlesbe.github.io
   ```
   
   **❌ NO debe tener:**
   - `/app-ads.txt`
   - `/CareTemplate`
   - Cualquier otra ruta

5. **Si no coincide, actualiza el dominio**:
   - Edita el campo "Sitio web" o "Website"
   - Pon: `https://gastonlesbe.github.io`
   - Guarda los cambios

---

### Paso 3: Rastrear el archivo en AdMob

1. **Ve a AdMob Console**: https://apps.admob.com/

2. **Selecciona la app "CarePal: Cars"**

3. **Ve a la configuración de app-ads.txt**:
   - Opción A: **"Configuración"** → **"app-ads.txt"**
   - Opción B: Busca "app-ads.txt" en el menú
   - Opción C: Ve a **"Monetización"** → **"app-ads.txt"**

4. **Haz clic en "Rastrear" o "Verificar"**:
   - AdMob buscará automáticamente el archivo en: `https://gastonlesbe.github.io/app-ads.txt`
   - Basándose en el dominio que configuraste en Google Play Console

5. **Espera la verificación**:
   - Puede tardar unos minutos
   - A veces puede tardar hasta 24 horas
   - AdMob verificará:
     - Que el archivo exista (no 404)
     - Que el contenido sea correcto
     - Que el Publisher ID coincida: `pub-9841764898906750`

---

## ✅ Checklist de Verificación

Antes de rastrear en AdMob, verifica:

- [ ] El archivo `app-ads.txt` está en GitHub: `gastonlesbe.github.io`
- [ ] La URL funciona: `https://gastonlesbe.github.io/app-ads.txt`
- [ ] El contenido es correcto: `google.com, pub-9841764898906750, DIRECT, f08c47fec0942fa0`
- [ ] El dominio en Google Play Console es: `https://gastonlesbe.github.io` (sin rutas adicionales)
- [ ] El Publisher ID en el archivo coincide con el de AdMob: `pub-9841764898906750`

---

## 🔍 Información de CarePal: Cars

- **App ID de AdMob**: `ca-app-pub-9841764898906750~6384453597`
- **Banner ID**: `ca-app-pub-9841764898906750/2106866114`
- **Rewarded ID**: `ca-app-pub-9841764898906750/9935354864`
- **Publisher ID**: `pub-9841764898906750`

---

## ⚠️ Problemas Comunes

### El archivo no se encuentra (404)

**Causa**: El archivo no está en la raíz del dominio o GitHub Pages no está habilitado.

**Solución**:
1. Verifica que el archivo esté en la raíz del repositorio `gastonlesbe.github.io`
2. Verifica que GitHub Pages esté habilitado
3. Espera unos minutos después de publicar (puede tardar en propagarse)

### El contenido no es correcto

**Causa**: El archivo tiene formato HTML o caracteres extra.

**Solución**: 
- Debe ser texto plano, sin HTML
- Sin espacios al final de las líneas
- Sin líneas vacías adicionales (una línea vacía al final está bien)

### El dominio no coincide

**Causa**: El dominio en Google Play Console no coincide con donde publicaste el archivo.

**Solución**: 
- Actualiza el dominio en Google Play Console a: `https://gastonlesbe.github.io`
- O publica el archivo en el dominio que tienes configurado

### AdMob no encuentra el archivo

**Causa**: AdMob busca el archivo basándose en el dominio de Google Play Console.

**Solución**:
1. Verifica que el dominio en Google Play Console sea exactamente: `https://gastonlesbe.github.io`
2. Verifica que el archivo esté accesible en: `https://gastonlesbe.github.io/app-ads.txt`
3. Espera hasta 24 horas (a veces tarda en actualizarse)

---

## 📞 Siguiente Paso

Una vez que hayas completado estos pasos:

1. **Rastrea el archivo en AdMob** (Paso 3)
2. **Espera la verificación** (puede tardar hasta 24 horas)
3. **Verifica el estado** en AdMob Console → Configuración → app-ads.txt

El estado debería cambiar de "No verificado" a "Verificado" una vez que AdMob confirme que el archivo es correcto.

