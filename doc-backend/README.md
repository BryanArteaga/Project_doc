# DocControl — Backend API

API REST para la aplicación de gestión de salud de pacientes. Construida con **Node.js + Express + TypeScript** y **Prisma** sobre **PostgreSQL**.

## ✨ Características

- 🔐 Sistema de autenticación con JWT
- ✅ Registro de usuarios con estado pendiente
- 👨‍⚕️ Panel de doctor para activar/desactivar cuentas
- 📊 Registro de datos de salud (glucosa, colesterol, presión)
- 🏃 Seguimiento de ejercicios realizados
- 🔄 Sincronización con app móvil

## 🧭 Flujo de activación de cuentas

1. **Usuario se registra**: la cuenta se crea en estado `pending`.
2. **Usuario puede iniciar sesión**: pero con funciones limitadas.
3. **Doctor revisa usuarios pendientes**: desde el panel web.
4. **Doctor activa la cuenta**: cambia el estado a `active`.
5. **Usuario recibe notificación**: y se desbloquean todas las funciones.

## 🚀 Instalación

```bash
# Instalar dependencias
npm install

# Generar cliente de Prisma
npm run prisma:generate

# Aplicar migraciones a la base de datos
npm run prisma:migrate
```

## ⚙️ Configuración

El archivo `.env` debe contener:

```env
DATABASE_URL="postgresql://..."
DIRECT_URL="postgresql://..."
JWT_SECRET="..."
JWT_EXPIRES_IN="7d"
PORT=3000
NODE_ENV=development
ALLOWED_ORIGINS="http://localhost:5173,http://localhost:5174"
DOCTOR_TOKEN="..."
```

> ⚠️ **Nunca** subas `.env` ni credenciales a Git. Ya están en `.gitignore`.

## ▶️ Ejecutar

```bash
# Modo desarrollo (con hot reload)
npm run dev

# Compilar para producción
npm run build

# Ejecutar en producción
npm start
```

## 📡 Endpoints

### Autenticación

| Método | Ruta | Descripción |
|---|---|---|
| `POST` | `/api/auth/register` | Registrar nuevo usuario |
| `POST` | `/api/auth/login` | Iniciar sesión |
| `GET` | `/api/auth/status` | Verificar estado de cuenta |

### Usuario

| Método | Ruta | Descripción |
|---|---|---|
| `GET` | `/api/usuarios/profile` | Obtener perfil |
| `PUT` | `/api/usuarios/profile` | Actualizar perfil |

### Registros de Salud

| Método | Ruta | Descripción |
|---|---|---|
| `POST` | `/api/registros-salud` | Crear registro |
| `GET` | `/api/registros-salud` | Listar registros |
| `GET` | `/api/registros-salud/:id` | Obtener registro |
| `PUT` | `/api/registros-salud/:id` | Actualizar registro |
| `DELETE` | `/api/registros-salud/:id` | Eliminar registro |

### Ejercicios

| Método | Ruta | Descripción |
|---|---|---|
| `POST` | `/api/ejercicios` | Registrar ejercicio |
| `GET` | `/api/ejercicios` | Listar ejercicios |

### Doctor (requiere header `x-doctor-token`)

| Método | Ruta | Descripción |
|---|---|---|
| `GET` | `/api/doctor/stats` | Estadísticas generales |
| `GET` | `/api/doctor/users/pending` | Usuarios pendientes |
| `GET` | `/api/doctor/users/active` | Usuarios activos |
| `GET` | `/api/doctor/users/:userId` | Detalles de usuario |
| `POST` | `/api/doctor/users/:userId/activate` | Activar usuario |
| `POST` | `/api/doctor/users/:userId/deactivate` | Desactivar usuario |

## 🗂 Estructura del proyecto

```
src/
├── controllers/     # Lógica de negocio
├── routes/          # Definición de endpoints
├── middleware/      # Autenticación y validación
├── types/           # Tipos TypeScript
└── server.ts        # Punto de entrada
```

## 🧾 Estados de cuenta

| Estado | Significado |
|---|---|
| `pending` | Cuenta registrada pero sin activar |
| `active` | Cuenta activada por el doctor |
| `inactive` | Cuenta desactivada |

## 🗃 Prisma Studio

Para visualizar y editar la base de datos:

```bash
npm run prisma:studio
```

## 🔒 Mantenimiento

Existe un workflow de GitHub Actions (`.github/workflows/maintenance.yml`) que ejecuta tareas de mantenimiento programadas (`npm run maintenance`). Requiere el secret `DATABASE_URL`; si no está configurado, se omite con un aviso.
