# 🚀 Deploy en Render + Workaround Sleep

## Paso 1: Deploy en Render

1. Ve a https://render.com
2. Conecta tu GitHub
3. Busca el repo `Project_doc`
4. Click: "New Web Service"
5. Selecciona `doc-backend`
6. Llena así:

```
Name: project-doc-backend
Runtime: Node
Build Command: npm install
Start Command: npm run build && npm start
Plan: Free
```

## Paso 2: Variables de Entorno

En Render Dashboard > Environment:

```
DATABASE_URL = postgresql://postgres.zpvxpfuxbdywcmngebxd:hvFWs5Qh87jmKRyx@aws-0-us-west-2.pooler.supabase.com:6543/postgres?pgbouncer=true

DIRECT_URL = postgresql://postgres.zpvxpfuxbdywcmngebxd:hvFWs5Qh87jmKRyx@aws-0-us-west-2.pooler.supabase.com:5432/postgres

JWT_SECRET = secreta456

DOCTOR_TOKEN = doctor_secret_token_2026

NODE_ENV = production

PORT = 3000
```

## Paso 3: Workaround Sleep (IMPORTANTE!)

Render duerme tras 15 min de inactividad. Usar uptime monitor:

### Opción A: Better Stack (Recomendado, Gratis)
1. Ve a https://betterstack.com
2. Sign up gratuito
3. Dashboard > Monitors > New Monitor
4. URL: `https://tu-app.onrender.com/health`
5. Check interval: **14 minutes** (antes que duerma)
6. ¡Listo! Hará ping automático

### Opción B: Pingping (Gratis)
1. Ve a https://ping.pingping.io
2. Crea monitor
3. URL: `https://tu-app.onrender.com/health`
4. Interval: 14 minutos

### Opción C: Cron-job.org (Gratis)
1. Ve a https://cron-job.org
2. Crea nuevo job
3. URL: `https://tu-app.onrender.com/health`
4. Interval: 14 minutos

## Paso 4: Endpoint Health Check

En tu `src/server.ts`, agrega:

```typescript
app.get('/health', (req, res) => {
  res.status(200).json({ status: 'OK', timestamp: new Date() })
})
```

O si ya existe, déjalo tal cual.

## Paso 5: Deploy

```bash
git push origin master
```

Render auto-deploya en ~2 min.

## Paso 6: URL de tu Backend

Render te da una URL como:
```
https://project-doc-backend.onrender.com
```

Copia esto y úsalo en:
- Frontend `VITE_API_URL`
- CORS en backend: `ALLOWED_ORIGINS`

## ✅ Verificar

```bash
curl https://tu-app.onrender.com/health
```

Debe retornar:
```json
{ "status": "OK", "timestamp": "2026-03-19T..." }
```

## 🎯 Resultado Final

- ✅ Backend deployado en Render Free
- ✅ No se duerme (gracias al uptime monitor)
- ✅ Base de datos Supabase
- ✅ Auto-deploy desde GitHub

---

**Costo: $0/mes** 💚

---

## 🆘 Troubleshooting

**"Build failed"**
- Revisa que `npm run build` funcione localmente
- `npm install` debe instalar todo

**"Cannot find module"**
- Verifica imports en `src/server.ts`
- Revisa `package.json` tiene todas las dependencies

**"Connection refused"**
- Revisa DATABASE_URL está correcto
- Supabase DB está activo

**"Sleep problema"**
- Confirma uptime monitor está corriendo (Better Stack logs)
- Interval debe ser < 15 minutos

---

📧 Soporte: soporte@docplus.app
