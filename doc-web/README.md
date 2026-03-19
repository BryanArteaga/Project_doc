# DocPlus Landing Page

Landing page para descargar la app DocPlus, con instrucciones de instalación y galería de capturas.

## 📁 Estructura

```
doc-web/
├── index.html           # HTML principal
├── vite.config.js       # Configuración de Vite
├── package.json         # Dependencias
├── src/
│   ├── main.jsx        # Punto de entrada React
│   ├── App.jsx         # Componente principal
│   └── App.css         # Estilos
└── public/
    └── screenshots/    # 📸 Agregar aquí tus capturas
        ├── screenshot-1.png
        ├── screenshot-2.png
        ├── screenshot-3.png
        └── screenshot-4.png
```

## 🚀 Instalación y Desarrollo

```bash
cd doc-web
npm install
npm run dev
```

La página abrirá en `http://localhost:5173`

## 📸 Agregar Capturas

1. Crea la carpeta `public/screenshots/` si no existe
2. Agrega 4 screenshots en formato PNG:
   - `screenshot-1.png` - Monitoreo en Tiempo Real
   - `screenshot-2.png` - Historial de Registros
   - `screenshot-3.png` - Consejos Personalizados
   - `screenshot-4.png` - Contacto con Doctor

**Recomendaciones:**
- Tamaño: 1080 x 2340px (formato móvil)
- Estar normalizadas en tamaño
- Mostrar interfaces diferentes de la app

## 🔗 Configurar Link de Descarga

En `src/App.jsx`, reemplaza la URL de GitHub:

```javascript
href="https://github.com/yourusername/docplus-app/releases"
```

Por tu repositorio real de GitHub Releases.

## 📦 Versionamiento de APK

Para usar GitHub Releases:

1. Crea un release en GitHub: `v1.0`, `v1.1`, etc
2. Sube el APK como "Asset" en cada release
3. Copia el link de descarga
4. La landing page redirige directamente ahí

**Alternativa local:**
Si quieres servir el APK desde tu servidor, copia el APK a `public/` y actualiza el link.

## 🛠️ Build para Producción

```bash
npm run build
```

Los archivos compilados estarán en `dist/`

## 📝 Contenido a Personalizar

### En `src/App.jsx`:
- `screenshots` - Títulos y descripciones de capturas
- `features` - Las 4 características principales
- `instructions` - Los 5 pasos de instalación
- FAQ - Preguntas frecuentes
- Email y WhatsApp de soporte en Footer

### En `src/App.css`:
- Colores (variables CSS al principio)
- Tipografía
- Espaciados

## 🎨 Personalización

### Colores
Edita las variables en `src/App.css`:
```css
:root {
  --primary: #2196F3;      /* Azul principal */
  --secondary: #FF6F00;    /* Naranja descargas */
  --success: #4CAF50;      /* Verde */
  --danger: #F44336;       /* Rojo */
}
```

### Logo
Reemplaza el emoji 📱 por tu logo:
```jsx
<span className="logo-icon">📱</span>  // ← Cambiar aquí
```

## 📱 Responsive

- ✅ Desktop
- ✅ Tablet
- ✅ Mobile

## 🚢 Deploy

### Opción 1: Vercel (Recomendado)
```bash
npm install -g vercel
vercel
```

### Opción 2: Netlify
```bash
npm install -g netlify-cli
netlify deploy --prod --dir dist
```

### Opción 3: GitHub Pages
1. Push a GitHub
2. Habilita GitHub Pages en settings
3. Configura para servir desde `dist/`

## ✅ Checklist Final

- [ ] Agregar 4 screenshots en `public/screenshots/`
- [ ] Actualizar link de GitHub Releases
- [ ] Personalizar colores si es necesario
- [ ] Actualizar email y WhatsApp de soporte
- [ ] Revisar contenido (características, instrucciones, FAQ)
- [ ] Probar responsive en móvil
- [ ] Deploy en servidor

---

**Versión:** 1.0  
**Última actualización:** 19 de Marzo de 2026
