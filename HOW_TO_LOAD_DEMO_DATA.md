# 📋 Cómo Cargar Datos de Demostración - Método Simple

## ✅ Opción Más Simple: Usar el Menú de la App

La app ya tiene un menú integrado para cargar datos. Es la forma más fácil:

1. **Abre la app** en el emulador o dispositivo
2. **Abre el menú** (tres puntos en la esquina superior derecha)
3. **Selecciona una de estas opciones:**
   - **"Datos de demostración (todo)"** - Crea sujeto + eventos + gastos (todo junto)
   - **"Crear sujeto y eventos"** - Solo crea el sujeto y los 5 eventos básicos
   - **"Crear gastos"** - Solo crea los 24 gastos (6 meses x 4 gastos)

4. **Espera unos segundos** - Verás un mensaje cuando termine

**¡Listo!** Los datos estarán en la base de datos local.

---

## 📍 Ubicación de la Base de Datos

Si necesitas acceder directamente a la base de datos:

**Nombre del archivo:** `caretemplate.db`

**Ubicación en el dispositivo:**
```
/data/data/com.gastonlesbegueris.caretemplate.[FLAVOR]/databases/caretemplate.db
```

**Flavors:**
- `house` → `com.gastonlesbegueris.caretemplate.house`
- `family` → `com.gastonlesbegueris.caretemplate.family`
- `pets` → `com.gastonlesbegueris.caretemplate.pets`
- `cars` → `com.gastonlesbegueris.caretemplate.cars`

**Para copiar la base de datos desde el dispositivo:**
```bash
# Ejemplo para house
adb pull /data/data/com.gastonlesbegueris.caretemplate.house/databases/caretemplate.db ./caretemplate_house.db
```

**Para copiar una base de datos al dispositivo:**
```bash
# Ejemplo para house
adb push ./caretemplate_house.db /data/data/com.gastonlesbegueris.caretemplate.house/databases/caretemplate.db
```

**⚠️ IMPORTANTE:** 
- Necesitas permisos root o un dispositivo con permisos de depuración
- La app debe estar cerrada cuando copias/pegas la base de datos
- Después de copiar, reinicia la app

---

## 🎯 Resumen

**La forma más fácil es usar el menú de la app.** No necesitas copiar archivos manualmente.
