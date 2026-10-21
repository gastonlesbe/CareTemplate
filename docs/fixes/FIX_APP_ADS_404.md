# 🔧 Solución al Error 404 de app-ads.txt

## ❌ Problema Actual

Estás intentando acceder a:
```
https://gastonlesbe.github.io/app-ads.txt
```

Pero el archivo está en el repositorio `CareTemplate`, por lo que está disponible en:
```
https://gastonlesbe.github.io/CareTemplate/app-ads.txt
```

AdMob necesita que el archivo esté en la **raíz del dominio**, no en una subcarpeta.

## ✅ Solución: Crear Repositorio Especial

Para que el archivo esté disponible en `https://gastonlesbe.github.io/app-ads.txt`, necesitas crear un repositorio con el nombre exacto: **`gastonlesbe.github.io`**

Este es un nombre especial de GitHub Pages que sirve archivos desde la raíz del dominio.

## 🚀 Pasos para Solucionarlo

### Opción 1: Crear Repositorio Nuevo (Recomendado)

1. **Ve a GitHub y crea un nuevo repositorio:**
   - URL: https://github.com/new
   - **Nombre del repositorio**: `gastonlesbe.github.io` (exactamente así, sin mayúsculas)
   - **Visibilidad**: Público
   - **NO inicialices** con README, .gitignore o licencia
   - Haz clic en "Create repository"

2. **Clona el repositorio en tu máquina:**
   ```bash
   cd ~
   git clone https://github.com/gastonlesbe/gastonlesbe.github.io.git
   cd gastonlesbe.github.io
   ```

3. **Copia el archivo app-ads.txt:**
   ```bash
   cp /home/gaston/AndroidStudioProjects/Caretemplate/app-ads.txt .
   ```

4. **Haz commit y push:**
   ```bash
   git add app-ads.txt
   git commit -m "Add app-ads.txt for AdMob verification"
   git push origin main
   ```

5. **Habilita GitHub Pages:**
   - Ve a: https://github.com/gastonlesbe/gastonlesbe.github.io/settings/pages
   - En "Source", selecciona la rama `main` (o `master`)
   - En "Folder", selecciona `/ (root)`
   - Haz clic en "Save"

6. **Espera 2-3 minutos** y verifica:
   ```
   https://gastonlesbe.github.io/app-ads.txt
   ```

### Opción 2: Usar el Repositorio Actual (Alternativa)

Si prefieres mantener todo en el repositorio `CareTemplate`, puedes:

1. **Actualizar Google Play Console** para usar la URL completa:
   - Ve a: https://play.google.com/console
   - Configuración de la app → Detalles de la app
   - Campo "Sitio web": `https://gastonlesbe.github.io/CareTemplate`
   - Guarda

2. **AdMob buscará el archivo en:**
   ```
   https://gastonlesbe.github.io/CareTemplate/app-ads.txt
   ```

   **PERO** esto puede no funcionar porque AdMob busca el archivo en la raíz del dominio especificado en Google Play Console.

## 🎯 Recomendación

**Usa la Opción 1** (crear `gastonlesbe.github.io`). Es la forma estándar y más confiable para servir archivos desde la raíz del dominio en GitHub Pages.

## ✅ Verificación Final

Después de configurar el repositorio `gastonlesbe.github.io`:

1. **Verifica la URL:**
   ```
   https://gastonlesbe.github.io/app-ads.txt
   ```
   Debe mostrar:
   ```
   google.com, pub-9841764898906750, DIRECT, f08c47fec0942fa0
   ```

2. **Actualiza Google Play Console:**
   - Sitio web: `https://gastonlesbe.github.io`
   - (Sin `/CareTemplate` ni `/app-ads.txt`)

3. **Verifica en AdMob:**
   - AdMob buscará automáticamente en `https://gastonlesbe.github.io/app-ads.txt`
   - Espera hasta 24 horas para la verificación

## 📝 Script de Ayuda

Ejecuta el script `fix_app_ads_404.sh` para automatizar estos pasos.

