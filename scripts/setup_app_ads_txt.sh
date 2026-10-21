#!/bin/bash
# Script para ayudar a configurar app-ads.txt con GitHub Pages

set -e

PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
APP_ADS_FILE="$PROJECT_DIR/app-ads.txt"

echo "🚀 Configuración de app-ads.txt para PetPal"
echo "=========================================="
echo ""

# Verificar que el archivo existe
if [ ! -f "$APP_ADS_FILE" ]; then
    echo "❌ Error: No se encontró app-ads.txt"
    exit 1
fi

echo "✅ Archivo app-ads.txt encontrado"
echo ""

# Verificar contenido
echo "📄 Contenido del archivo:"
cat "$APP_ADS_FILE"
echo ""
echo ""

# Verificar si es un repositorio git
if [ -d "$PROJECT_DIR/.git" ]; then
    echo "✅ Este proyecto ya es un repositorio Git"
    
    # Verificar si hay un remote configurado
    if git remote get-url origin &>/dev/null; then
        REMOTE_URL=$(git remote get-url origin)
        echo "📍 Remote configurado: $REMOTE_URL"
        
        # Extraer usuario y repo de la URL
        if [[ $REMOTE_URL =~ github.com[:/]([^/]+)/([^/]+)\.git ]]; then
            GITHUB_USER="${BASH_REMATCH[1]}"
            REPO_NAME="${BASH_REMATCH[2]}"
            REPO_NAME="${REPO_NAME%.git}"
            
            echo ""
            echo "🎯 Información detectada:"
            echo "   Usuario: $GITHUB_USER"
            echo "   Repositorio: $REPO_NAME"
            echo ""
            echo "📝 Próximos pasos:"
            echo ""
            echo "1. Asegúrate de que app-ads.txt esté en la raíz del repositorio"
            echo "2. Haz commit y push del archivo:"
            echo "   git add app-ads.txt"
            echo "   git commit -m 'Add app-ads.txt for AdMob verification'"
            echo "   git push origin main"
            echo ""
            echo "3. Habilita GitHub Pages:"
            echo "   - Ve a: https://github.com/$GITHUB_USER/$REPO_NAME/settings/pages"
            echo "   - En 'Source', selecciona la rama 'main' o 'master'"
            echo "   - En 'Folder', selecciona '/ (root)'"
            echo "   - Haz clic en 'Save'"
            echo ""
            echo "4. Tu URL será:"
            echo "   https://$GITHUB_USER.github.io/$REPO_NAME/app-ads.txt"
            echo ""
            echo "5. Verifica que funcione:"
            echo "   Abre la URL en el navegador y debe mostrar el contenido del archivo"
            echo ""
            echo "6. Actualiza Google Play Console:"
            echo "   - Ve a: https://play.google.com/console"
            echo "   - Selecciona PetPal"
            echo "   - Configuración de la app → Detalles de la app"
            echo "   - En 'Sitio web', pon: https://$GITHUB_USER.github.io"
            echo ""
        else
            echo "⚠️  No se pudo detectar usuario/repo de GitHub automáticamente"
            echo "   URL del remote: $REMOTE_URL"
        fi
    else
        echo "⚠️  No hay remote 'origin' configurado"
        echo ""
        echo "📝 Para configurar GitHub Pages:"
        echo ""
        echo "1. Crea un repositorio en GitHub (o usa uno existente)"
        echo "2. Agrega el remote:"
        echo "   git remote add origin https://github.com/TU_USUARIO/TU_REPO.git"
        echo "3. Sigue los pasos anteriores"
    fi
else
    echo "ℹ️  Este proyecto NO es un repositorio Git"
    echo ""
    echo "📝 Opciones:"
    echo ""
    echo "OPCIÓN A: Crear repositorio nuevo en GitHub"
    echo "1. Ve a https://github.com y crea un nuevo repositorio público"
    echo "   (por ejemplo: 'petpal-website' o 'petpal-app-ads')"
    echo ""
    echo "2. Inicializa Git en este proyecto:"
    echo "   git init"
    echo "   git add app-ads.txt"
    echo "   git commit -m 'Add app-ads.txt'"
    echo "   git branch -M main"
    echo "   git remote add origin https://github.com/TU_USUARIO/TU_REPO.git"
    echo "   git push -u origin main"
    echo ""
    echo "3. Habilita GitHub Pages (ver pasos anteriores)"
    echo ""
    echo "OPCIÓN B: Usar Netlify (más fácil, sin Git)"
    echo "1. Ve a https://netlify.com"
    echo "2. Crea una cuenta (gratis)"
    echo "3. Arrastra una carpeta que contenga app-ads.txt"
    echo "4. Obtén tu URL: https://tu-sitio.netlify.app/app-ads.txt"
    echo ""
fi

echo ""
echo "✅ Checklist final:"
echo "   [ ] Archivo app-ads.txt creado"
echo "   [ ] Archivo publicado en GitHub Pages (o servicio alternativo)"
echo "   [ ] URL verificada en el navegador"
echo "   [ ] Dominio actualizado en Google Play Console"
echo "   [ ] Verificación iniciada en AdMob Console"
echo ""
echo "📚 Para más detalles, consulta:"
echo "   - APP_ADS_TXT_GOOGLE_SITES.md"
echo "   - APP_ADS_TXT_GUIDE.md"
echo ""


