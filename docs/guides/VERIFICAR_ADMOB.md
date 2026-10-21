# Verificar app-ads.txt en AdMob Console

## 🎯 Pasos para Verificar en AdMob

### Paso 1: Acceder a AdMob Console

1. **Ve a**: https://apps.admob.com/
2. **Inicia sesión** con tu cuenta de Google (la misma que usas para Google Play)

### Paso 2: Seleccionar tu App

1. En la página principal, busca tu app **"PetPal"**
2. **Haz clic** en el nombre de la app o en la tarjeta de la app

### Paso 3: Ir a la Configuración de app-ads.txt

Hay varias formas de llegar, prueba estas opciones:

#### Opción A: Desde el Menú de la App

1. Una vez dentro de la app "PetPal"
2. En el menú lateral izquierdo, busca:
   - **"App settings"** (Configuración de la app)
   - O **"Settings"** (Configuración)
   - O **"Configuración"**
3. Haz clic ahí
4. Busca la sección **"app-ads.txt"** o **"App-ads.txt"**
5. Haz clic en esa sección

#### Opción B: Desde la Página Principal de la App

1. En la página principal de "PetPal"
2. Busca una tarjeta o sección que diga:
   - **"App-ads.txt"**
   - O **"Verification"** (Verificación)
   - O **"Website verification"** (Verificación del sitio web)
3. Haz clic ahí

#### Opción C: Buscar en la Barra de Búsqueda

1. En la parte superior de AdMob Console, hay una barra de búsqueda
2. Escribe: **"app-ads.txt"** o **"app ads"**
3. Debería mostrarte dónde está

### Paso 4: Verificar el Archivo

Una vez que encuentres la sección de app-ads.txt:

1. **Verás un campo** donde puedes ingresar o verificar la URL
2. **O verás un botón** que dice:
   - **"Verify"** (Verificar)
   - O **"Track"** (Rastrear)
   - O **"Check"** (Verificar)
   - O **"Verificar"** (en español)

3. **Haz clic en el botón de verificación**

### Paso 5: Ingresar el Dominio (si es necesario)

Si te pide ingresar el dominio:

1. **Ingresa**: `https://gastonlesbe.github.io`
   - Solo el dominio, sin `/CareTemplate` ni `/app-ads.txt`
   - Con `https://`

2. **O si te pide la URL completa del archivo**:
   ```
   https://gastonlesbe.github.io/CareTemplate/app-ads.txt
   ```

### Paso 6: Esperar la Verificación

1. **AdMob comenzará a verificar** el archivo
2. **Puede tardar**:
   - Desde unos minutos
   - Hasta 24 horas
3. **Verás un estado** como:
   - ✅ "Verified" (Verificado)
   - ⏳ "Verifying" (Verificando)
   - ❌ "Failed" (Fallido)
   - ⚠️ "Pending" (Pendiente)

## 📋 Qué Buscar Visualmente

### En la Página de app-ads.txt

Deberías ver algo como:

```
App-ads.txt
─────────────
Status: [Verifying/Pending/Verified/Failed]

Domain: https://gastonlesbe.github.io

[Verify] [Track] [Check] button
```

O puede mostrar:

```
Website verification
────────────────────
Enter your website domain:
[___________________]  [Verify]

Current status: Pending verification
```

## ✅ Checklist de Verificación

Antes de verificar en AdMob, asegúrate de que:

- [ ] El archivo `app-ads.txt` está en GitHub
- [ ] GitHub Pages está habilitado
- [ ] La URL funciona: https://gastonlesbe.github.io/CareTemplate/app-ads.txt
- [ ] El dominio está actualizado en Google Play Console
- [ ] Puedes ver el contenido del archivo en el navegador

## 🔍 Si No Encuentras la Opción

### Alternativa 1: Buscar en "Monetize"

1. En el menú lateral, busca **"Monetize"** o **"Monetizar"**
2. Haz clic ahí
3. Busca opciones relacionadas con verificación o configuración

### Alternativa 2: Buscar en "Account"

1. En la parte superior derecha, haz clic en tu **avatar/perfil**
2. Busca **"Account settings"** o **"Configuración de cuenta"**
3. Busca opciones de verificación

### Alternativa 3: Usar la Barra de Búsqueda

1. En la parte superior de AdMob, hay una barra de búsqueda
2. Escribe: **"app-ads.txt"**
3. Debería mostrarte dónde está

## ⚠️ Problemas Comunes

### "File not found" (Archivo no encontrado)

**Causa**: El archivo no está accesible en la URL

**Solución**:
1. Verifica que GitHub Pages esté habilitado
2. Abre la URL en el navegador: https://gastonlesbe.github.io/CareTemplate/app-ads.txt
3. Debe mostrar el contenido del archivo
4. Espera 2-3 minutos después de habilitar Pages

### "Domain mismatch" (Dominio no coincide)

**Causa**: El dominio en Google Play Console no coincide

**Solución**:
1. Verifica que en Google Play Console el dominio sea: `https://gastonlesbe.github.io`
2. Debe coincidir exactamente con donde está el archivo

### "Invalid format" (Formato inválido)

**Causa**: El archivo tiene formato incorrecto

**Solución**:
1. Verifica que el archivo contenga exactamente:
   ```
   google.com, pub-9841764898906750, DIRECT, f08c47fec0942fa0
   ```
2. Sin espacios extra
3. Sin líneas en blanco adicionales

## 📝 Notas Importantes

- **La verificación puede tardar hasta 24 horas**
- **No necesitas hacer nada más** una vez que inicies la verificación
- **AdMob verificará automáticamente** el archivo periódicamente
- **Si falla**, revisa los problemas comunes arriba

## 🎯 Resumen de URLs

- **AdMob Console**: https://apps.admob.com/
- **Archivo app-ads.txt**: https://gastonlesbe.github.io/CareTemplate/app-ads.txt
- **Google Play Console**: https://play.google.com/console

## ✅ Una Vez Verificado

Cuando AdMob verifique correctamente:
- ✅ Verás un estado "Verified" (Verificado)
- ✅ Los anuncios seguirán funcionando normalmente
- ✅ No necesitas hacer nada más

## 🔄 Si Necesitas Ayuda

Si no encuentras la opción de app-ads.txt:
1. **Toma una captura de pantalla** del menú de AdMob
2. O describe qué opciones ves en el menú lateral
3. Con esa información puedo darte instrucciones más específicas


