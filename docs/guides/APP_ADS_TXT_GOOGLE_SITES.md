# Publicar app-ads.txt con Google Sites

## ⚠️ Limitación de Google Sites

Google Sites **NO puede servir archivos de texto plano directamente** como `app-ads.txt`. Google Sites está diseñado para páginas web HTML, no para archivos de texto plano.

## ✅ Soluciones

### Opción 1: GitHub Pages (Recomendado - Gratis)

Esta es la opción más fácil y gratuita:

1. **Crea un repositorio en GitHub** (o usa uno existente)
   - Ve a https://github.com
   - Crea un nuevo repositorio público (por ejemplo: `petpal-website`)

2. **Sube el archivo app-ads.txt**
   - Crea un archivo llamado `app-ads.txt` en la raíz del repositorio
   - Contenido:
     ```
     google.com, pub-9841764898906750, DIRECT, f08c47fec0942fa0
     ```

3. **Habilita GitHub Pages**
   - Ve a Settings → Pages
   - En "Source", selecciona la rama `main` o `master`
   - Guarda

4. **Tu URL será**:
   ```
   https://tuusuario.github.io/petpal-website/app-ads.txt
   ```
   O si el repositorio se llama igual que tu usuario:
   ```
   https://tuusuario.github.io/app-ads.txt
   ```

5. **Actualiza el dominio en Google Play Console**:
   - Ve a Google Play Console → Configuración de la app → Detalles de la app
   - En "Sitio web", pon: `https://tuusuario.github.io` (sin el `/app-ads.txt`)

### Opción 2: Netlify (Gratis - Muy Fácil)

1. **Ve a https://netlify.com**
2. **Crea una cuenta** (gratis)
3. **Arrastra y suelta** una carpeta con el archivo `app-ads.txt`
4. **Obtén tu URL**: `https://tu-sitio.netlify.app/app-ads.txt`
5. **Actualiza el dominio en Google Play Console**

### Opción 3: Vercel (Gratis)

Similar a Netlify, muy fácil de usar.

### Opción 4: Usar un Dominio Propio

Si tienes un dominio propio:

1. **Sube el archivo** a la raíz de tu servidor web
2. **Debe estar en**: `https://tudominio.com/app-ads.txt`
3. **Verifica** que sea accesible sin autenticación

## 🔍 Verificar el Dominio Actual

1. Ve a [Google Play Console](https://play.google.com/console)
2. Selecciona "PetPal"
3. Ve a **"Configuración de la app"** → **"Detalles de la app"**
4. Busca el campo **"Sitio web"** o **"Developer website"**
5. **Anota el dominio exacto** que aparece ahí

## 📝 Pasos Detallados para GitHub Pages

### Paso 1: Crear el Repositorio

```bash
# En tu terminal (opcional, también puedes hacerlo desde la web)
mkdir petpal-website
cd petpal-website
git init
```

### Paso 2: Crear el Archivo

Crea `app-ads.txt` con este contenido:
```
google.com, pub-9841764898906750, DIRECT, f08c47fec0942fa0
```

### Paso 3: Subir a GitHub

```bash
git add app-ads.txt
git commit -m "Add app-ads.txt"
git branch -M main
git remote add origin https://github.com/TU_USUARIO/petpal-website.git
git push -u origin main
```

O desde la interfaz web de GitHub:
1. Crea el repositorio
2. Haz clic en "Add file" → "Create new file"
3. Nombre: `app-ads.txt`
4. Contenido: `google.com, pub-9841764898906750, DIRECT, f08c47fec0942fa0`
5. Commit

### Paso 4: Habilitar GitHub Pages

1. Ve a Settings → Pages
2. Source: `main` branch
3. Folder: `/ (root)`
4. Save

### Paso 5: Verificar

Abre en el navegador:
```
https://TU_USUARIO.github.io/petpal-website/app-ads.txt
```

Debe mostrar el contenido del archivo.

### Paso 6: Actualizar Google Play Console

1. Ve a Google Play Console
2. Configuración de la app → Detalles de la app
3. En "Sitio web", pon: `https://TU_USUARIO.github.io` (o el dominio completo)
4. Guarda

### Paso 7: Verificar en AdMob

1. Ve a [AdMob Console](https://apps.admob.com/)
2. Selecciona "PetPal"
3. Ve a Configuración → app-ads.txt
4. Haz clic en "Verificar" o "Rastrear"
5. Espera unos minutos (puede tardar hasta 24 horas)

## ✅ Checklist

- [ ] Repositorio creado en GitHub (o servicio alternativo)
- [ ] Archivo `app-ads.txt` creado con el contenido correcto
- [ ] Archivo subido a la raíz del repositorio
- [ ] GitHub Pages habilitado (o servicio equivalente)
- [ ] URL verificada en el navegador
- [ ] Dominio actualizado en Google Play Console
- [ ] Verificación iniciada en AdMob Console

## 🎯 URL Final Esperada

Tu archivo debe estar accesible en:
```
https://TU_USUARIO.github.io/petpal-website/app-ads.txt
```

O si usas otro servicio:
```
https://tu-sitio.netlify.app/app-ads.txt
https://tu-sitio.vercel.app/app-ads.txt
https://tudominio.com/app-ads.txt
```

## ⚠️ Importante

- El archivo debe estar en la **raíz** del sitio, no en una subcarpeta
- Debe ser **accesible públicamente** (sin autenticación)
- Debe mostrar el contenido como **texto plano** (no HTML)
- El dominio en Google Play Console debe **coincidir** con donde publicaste el archivo

## 🔄 Si Ya Tienes un Sitio Web

Si ya tienes un sitio web (no Google Sites), simplemente:
1. Sube el archivo `app-ads.txt` a la raíz
2. Verifica que sea accesible
3. Actualiza el dominio en Google Play Console si es necesario


