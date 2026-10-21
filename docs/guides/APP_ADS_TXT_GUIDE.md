# Guía para Publicar app-ads.txt - PetPal

## 📋 ¿Qué es app-ads.txt?

El archivo `app-ads.txt` es un estándar de IAB Tech Lab que permite a los editores declarar qué empresas están autorizadas a vender su inventario de anuncios digitales. Google AdMob requiere este archivo para verificar la propiedad de tu app.

## ✅ Archivo Creado

Se ha creado el archivo `app-ads.txt` en la raíz del proyecto con el contenido:

```
google.com, pub-9841764898906750, DIRECT, f08c47fec0942fa0
```

## 🌐 Dónde Publicarlo

Debes publicar este archivo en el **dominio raíz de tu sitio web de desarrollador** que aparece en Google Play Console.

### Opción 1: Google Sites (Recomendado si ya usas Google Sites)

Si ya tienes un sitio en Google Sites para tu política de privacidad:

1. **Crea una nueva página o usa la existente**
   - Ve a tu sitio de Google Sites
   - Crea una nueva página llamada "app-ads.txt" (o usa la página principal)

2. **Agrega el contenido como texto plano**
   - Inserta un cuadro de texto
   - Copia y pega exactamente:
     ```
     google.com, pub-9841764898906750, DIRECT, f08c47fec0942fa0
     ```
   - **IMPORTANTE**: No agregues ningún formato, solo texto plano

3. **Publica el sitio**
   - Asegúrate de que el sitio esté publicado públicamente
   - La URL debe ser accesible sin autenticación

4. **Verifica la URL**
   - El archivo debe estar disponible en: `https://tusitio.google.com/app-ads.txt`
   - O si está en la raíz: `https://tusitio.google.com/app-ads.txt`

### Opción 2: Sitio Web Propio

Si tienes tu propio dominio:

1. **Sube el archivo a la raíz del dominio**
   - El archivo debe estar en: `https://tudominio.com/app-ads.txt`
   - No en una subcarpeta, debe estar en la raíz

2. **Verifica que sea accesible**
   - Abre la URL en un navegador
   - Debe mostrar el contenido del archivo como texto plano

### Opción 3: GitHub Pages (Gratis)

Si no tienes un sitio web, puedes usar GitHub Pages:

1. **Crea un repositorio público** (o usa uno existente)
2. **Crea el archivo `app-ads.txt`** en la raíz del repositorio
3. **Habilita GitHub Pages** en la configuración del repositorio
4. **El archivo estará en**: `https://tuusuario.github.io/app-ads.txt`

## 🔍 Verificar el Dominio en Google Play Console

1. Ve a [Google Play Console](https://play.google.com/console)
2. Selecciona tu app "PetPal"
3. Ve a **"Configuración de la app"** → **"Detalles de la app"**
4. Busca el campo **"Sitio web"** o **"Developer website"**
5. **Anota el dominio exacto** que aparece ahí
6. Ese es el dominio donde debes publicar `app-ads.txt`

## ✅ Verificar que Funciona

1. **Abre la URL en el navegador**:
   ```
   https://tudominio.com/app-ads.txt
   ```
   O
   ```
   https://tusitio.google.com/app-ads.txt
   ```

2. **Debe mostrar**:
   ```
   google.com, pub-9841764898906750, DIRECT, f08c47fec0942fa0
   ```

3. **Verifica en AdMob**:
   - Ve a [AdMob Console](https://apps.admob.com/)
   - Selecciona tu app "PetPal"
   - Ve a **"Configuración"** → **"app-ads.txt"**
   - Haz clic en **"Verificar"** o **"Rastrear"**
   - Espera unos minutos (puede tardar hasta 24 horas)

## ⚠️ Problemas Comunes

### El archivo no se encuentra (404)

- **Causa**: El archivo no está en la raíz del dominio
- **Solución**: Asegúrate de que la URL sea exactamente `https://tudominio.com/app-ads.txt`

### El contenido no es correcto

- **Causa**: El archivo tiene formato HTML o caracteres extra
- **Solución**: Debe ser texto plano, sin HTML, sin formato

### El dominio no coincide

- **Causa**: El dominio en Google Play Console no coincide con donde publicaste el archivo
- **Solución**: Actualiza el dominio en Google Play Console o publica el archivo en el dominio correcto

### Google Sites muestra HTML

- **Causa**: Google Sites puede agregar HTML automáticamente
- **Solución**: 
  - Usa "Insertar" → "Texto" → "Texto sin formato"
  - O publica el archivo como descarga y configura el servidor para servirlo como texto plano

## 📝 Contenido del Archivo

El archivo `app-ads.txt` debe contener exactamente:

```
google.com, pub-9841764898906750, DIRECT, f08c47fec0942fa0
```

**Formato**:
- `google.com`: El dominio del vendedor de anuncios
- `pub-9841764898906750`: Tu Publisher ID de AdMob
- `DIRECT`: Indica que es una relación directa
- `f08c47fec0942fa0`: El certificador ID de Google

## 🔄 Para CarPal y Otros Flavors

Cuando publiques CarPal u otros flavors, necesitarás:
1. Obtener los IDs de AdMob para cada flavor
2. Crear un archivo `app-ads.txt` específico o agregar líneas adicionales
3. Publicarlo en el mismo dominio

## 📚 Referencias

- [IAB Tech Lab app-ads.txt](https://iabtechlab.com/ads-txt/)
- [Google AdMob app-ads.txt](https://support.google.com/admob/answer/9363762)
- [Verificar app-ads.txt](https://adstxt.guru/)

## ✅ Checklist

- [ ] Archivo `app-ads.txt` creado con el contenido correcto
- [ ] Dominio verificado en Google Play Console
- [ ] Archivo publicado en la raíz del dominio
- [ ] URL accesible en el navegador
- [ ] Contenido se muestra como texto plano
- [ ] Verificación iniciada en AdMob Console
- [ ] Esperando confirmación (puede tardar hasta 24 horas)

