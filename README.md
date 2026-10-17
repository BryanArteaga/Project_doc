# DocControl

Sistema integral de gestión de salud de pacientes, compuesto por una app móvil, una API, un panel clínico y una landing page.

> 📱 **Android** · 🖥️ **Panel web** · ⚙️ **API** · 🌐 **Landing**

## 🧩 Arquitectura

El proyecto está organizado en cuatro subsistemas:

| Carpeta | Componente | Stack |
|---|---|---|
| [`doc/`](./doc) | **App móvil Android** | Kotlin / Android |
| [`doc-backend/`](./doc-backend) | **API REST** | Node.js · Express · TypeScript · Prisma · PostgreSQL |
| [`doc-frontend/`](./doc-frontend) | **Panel web (DocControl)** | React · Vite · TypeScript · Tailwind |
| [`doc-web/`](./doc-web) | **Landing page** | React · Vite |

## ✨ Funcionalidades

- 🔐 Autenticación con JWT y sincronización con Supabase.
- ✅ Registro de pacientes con flujo de activación por un doctor.
- 📊 Registro de datos de salud: glucosa, colesterol, presión, temperatura y peso.
- 🏃 Seguimiento de ejercicios realizados y asignados.
- 👨‍⚕️ Panel clínico para que el personal médico gestione pacientes.

## 🚀 Inicio rápido

### 1. Backend

```bash
cd doc-backend
npm install
npm run prisma:generate
npm run prisma:migrate
npm run dev
```

Consulta [`doc-backend/README.md`](./doc-backend/README.md) para la configuración del `.env`, la lista completa de endpoints y más detalles.

### 2. Panel web

```bash
cd doc-frontend
npm install
npm run dev
```

### 3. Landing page

```bash
cd doc-web
npm install
npm run dev
```

### 4. App Android

Abre la carpeta `doc/` con **Android Studio** y ejecuta el proyecto en un emulador o dispositivo.

## 🗂 Estructura del repositorio

```
.
├── doc/              # App Android
├── doc-backend/      # API REST (Node + Prisma)
├── doc-frontend/     # Panel web (React + Vite)
├── doc-web/          # Landing page
└── .github/workflows # CI/CD y mantenimiento
```

## ⚙️ CI/CD

- **CI/CD Pipeline**: construye backend y frontend en cada push.
- **Database Maintenance**: ejecuta tareas de mantenimiento programadas (requiere el secret `DATABASE_URL` en GitHub Actions).

## 🔒 Seguridad

- Las credenciales (`firebase-service-account.json`, `google-services.json`, `.env`) están excluidas del repositorio mediante `.gitignore` y **nunca** deben subirse a Git.
- Documentación adicional de auditoría en [`doc-frontend/SEGURIDAD_Y_AUDITORIA.md`](./doc-frontend/SEGURIDAD_Y_AUDITORIA.md).

## 📄 Licencia

Este proyecto está bajo la licencia [MIT](./LICENSE).
