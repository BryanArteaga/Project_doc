# 🎯 Próximos Pasos - Landing Page DocPlus

## 1️⃣ Instala Dependencias

```bash
cd doc-web
npm install
```

## 2️⃣ Ejecuta en Desarrollo

```bash
npm run dev
```

Abrirá en `http://localhost:5173`

## 3️⃣ Captura Screenshots de tu App

Necesitas 4 capturas de la app Android:

**Screenshot 1 - Monitoreo en Tiempo Real**
- Muestra la pantalla principal con los últimos registros
- Ubicación: `public/screenshots/screenshot-1.png`

**Screenshot 2 - Historial de Registros**
- Muestra el historial con varios registros pasados
- Ubicación: `public/screenshots/screenshot-2.png`

**Screenshot 3 - Consejos Personalizados**
- Muestra los consejos para la salud del usuario
- Ubicación: `public/screenshots/screenshot-3.png`

**Screenshot 4 - Contacto con Doctor (WhatsApp)**
- Muestra el popup con el botón de WhatsApp "¿Por qué?"
- Ubicación: `public/screenshots/screenshot-4.png`

### 📱 Cómo Capturar:
1. En Android, presiona: **Power + Volumen Abajo** (mantén ambas)
2. O en emulador: **Ctrl + Z** en Android Studio
3. Copia las imágenes a `public/screenshots/`

## 4️⃣ Copia el APK

```bash
# Copia tu APK compilado a assets (opcional)
cp doc/app/build/outputs/apk/debug/app-debug.apk public/
```

O mejor: **Sube a GitHub Releases**

## 5️⃣ GitHub Releases (Recomendado)

1. Ve a tu repo de DocPlus en GitHub
2. Crea un nuevo Release: `v1.0`
3. Carga el APK como "Asset"
4. Copia el link de descarga
5. En `src/App.jsx`, reemplaza:

```javascript
href="https://github.com/TU_USUARIO/docplus-app/releases"
```

## 6️⃣ Personaliza (Opcional)

En `src/App.jsx`:
- Línea 65: Update email y WhatsApp de soporte
- Línea 34-47: Personaliza características
- Línea 49-62: Personaliza instrucciones
- `src/App.css`: Colores en `:root`

## 7️⃣ Build para Producción

```bash
npm run build
```

Genera carpeta `dist/` lista para deploy

## 8️⃣ Deploy

### Opción A: Vercel (Más fácil)
```bash
npm install -g vercel
vercel
```

### Opción B: Netlify
```bash
npm install -g netlify-cli
netlify deploy --prod --dir dist
```

### Opción C: GitHub Pages
Push a `main` y configura en GitHub Settings

---

## ✅ Checklist

- [ ] `npm install` completado
- [ ] `npm run dev` funciona
- [ ] 4 screenshots capturadas y en `public/screenshots/`
- [ ] APK compilado
- [ ] GitHub Release creado con el APK
- [ ] Link de descarga actualizado en App.jsx
- [ ] Email/WhatsApp actualizado
- [ ] `npm run build` sin errores
- [ ] Deployed en Vercel/Netlify/GitHub Pages

---

## 🆘 Troubleshooting

**¿Las screenshots no aparecen?**
- Verifica la ruta: `public/screenshots/screenshot-1.png`
- Refresh la página (Ctrl+F5)

**¿El link de descarga no funciona?**
- Verifica que el GitHub Release exista
- Abre el link en navegador para confirmar

**¿¿npm install falla?**
```bash
rm -rf node_modules package-lock.json
npm cache clean --force
npm install
```

---

📧 Soporte: soporte@docplus.app  
📱 WhatsApp: +593 96 731 3638
