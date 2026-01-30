# 🚀 PROMPT COMPLETO: DESARROLLO PLATAFORMA WEB DOC

## 📋 CONTEXTO DEL PROYECTO

Soy el desarrollador de **DOC App**, una aplicación Android para fisioterapeutas que permite:
- Gestionar pacientes
- Crear y asignar ejercicios de rehabilitación
- Registrar datos de salud (presión, glucosa, temperatura, etc.)
- Enviar notificaciones programadas
- Generar reportes

**SITUACIÓN ACTUAL:**
- ✅ App Android ya desarrollada y funcionando
- ✅ **FUNCIONA 100% OFFLINE** (Room Database local)
- ✅ Notificaciones LOCALES con WorkManager y AlarmManager
- ✅ Usuario puede usar la app sin conexión a internet
- ❌ NO tiene backend (todo es local)
- ❌ NO tiene plataforma web
- ❌ NO sincroniza datos con servidor
- ❌ Doctor no puede ver datos de sus pacientes remotamente

**ARQUITECTURA ACTUAL:**
```
APP ANDROID (Offline)
    ↓
Room Database Local
    ↓
Todo funciona sin internet
```

---

## 💡 CONCEPTO CLAVE: OFFLINE-FIRST CON SINCRONIZACIÓN

### **¿Cómo funcionará la app?**

**OFFLINE-FIRST:**
- La app **SIEMPRE funciona sin internet**
- Todos los datos se guardan primero en Room Database (local)
- Usuario puede registrar salud, hacer ejercicios, ver historial SIN conexión
- Las notificaciones locales (ejercicios, salud) funcionan sin internet

**SINCRONIZACIÓN SILENCIOSA:**
- Cuando hay internet, la app sincroniza en segundo plano
- **Sincronización bidireccional:**
  - **PUSH:** Envía al backend los datos creados localmente (registros de salud, ejercicios completados)
  - **PULL:** Descarga del backend nuevos datos (ejercicios asignados por doctor, cambio de estado de cuenta, mensajes)
- La sincronización es **automática y silenciosa** (sin molestar al usuario)
- Ocurre al abrir la app, al cerrarla, o periódicamente en segundo plano

**NOTIFICACIONES:**
- **Ejercicios y salud:** Siempre locales (WorkManager/AlarmManager)
- **Activación de cuenta:** Se detecta al sincronizar, se muestra notificación local
- **Mensajes del doctor:** Se detectan al sincronizar, se muestran como notificación local
- **NO hay push notifications** desde servidor
- La sincronización es silenciosa (sin notificar "Sincronizando...")

### **Flujo de sincronización:**

```
USUARIO USA LA APP (OFFLINE)
    ↓
Registra salud, completa ejercicios
    ↓
Todo se guarda en Room Database local
    ↓
App funciona perfectamente SIN internet
    ↓
    ↓ (Cuando HAY internet)
    ↓
SINCRONIZACIÓN AUTOMÁTICA EN SEGUNDO PLANO
    ↓
┌─────────────────────────────────────┐
│ PUSH: Envía datos locales nuevos   │
│ - Registros de salud                │
│ - Ejercicios completados            │
│                                     │
│ PULL: Descarga datos remotos nuevos │
│ - Ejercicios asignados por doctor   │
│ - Estado de cuenta actualizado      │
│ - Mensajes del doctor               │
└─────────────────────────────────────┘
    ↓
Datos sincronizados (usuario NO se entera)
    ↓
Si hay notificaciones nuevas (activación, mensajes)
    ↓
Se muestran como notificación LOCAL de Android
```

### **Ventajas de este enfoque:**
✅ App funciona **siempre**, con o sin internet
✅ Usuario no depende de conexión
✅ Datos sincronizados cuando es posible
✅ Doctor puede ver datos de pacientes desde web
✅ Paciente puede ver sus datos desde app o web

---

## 🎯 OBJETIVO: CREAR PLATAFORMA WEB

### **¿Qué necesito?**

1. **Sistema de autenticación con roles:**
   - **Doctores** (fisioterapeutas): Pueden ver y gestionar todos sus pacientes
   - **Pacientes**: Solo pueden ver sus propios datos

2. **Flujo de registro y activación:**
   ```
   PACIENTE SE REGISTRA
        ↓
   Cuenta en estado "INACTIVO" (sin funciones)
        ↓
   Puede entrar a la app pero solo ve mensaje:
   "Tu cuenta está pendiente de activación"
        ↓
   DOCTOR aprueba la cuenta desde panel web/app
        ↓
   Backend actualiza estado a "ACTIVE"
        ↓
   Paciente abre la app → Sincroniza → Detecta activación
        ↓
   App muestra notificación LOCAL: "Tu cuenta ha sido activada"
        ↓
   Se desbloquean todas las funciones de la app
   ```

3. **Funcionalidades de la plataforma web:**
   
   **PARA DOCTORES:**
   - Dashboard con estadísticas de sus pacientes
   - Lista de pacientes (activos, inactivos, pendientes)
   - Ver registros de salud de cada paciente (gráficos)
   - Ver ejercicios asignados y su progreso
   - Crear y asignar nuevos ejercicios
   - Activar/desactivar cuentas de pacientes
   - Enviar mensajes/avisos (se verán al sincronizar)
   - Generar reportes en PDF

   **PARA PACIENTES:**
   - Ver sus propios registros de salud
   - Ver ejercicios asignados
   - Marcar ejercicios como completados
   - Ver historial y progreso
   - Perfil y configuración

4. **Sincronización bidireccional (offline-first):**
   - App funciona **100% offline** (todo en Room Database)
   - Cuando hay internet: sincronización automática en segundo plano
   - **PUSH:** Los datos creados en la app (registros, ejercicios completados) suben al backend
   - **PULL:** Los datos creados en la web (ejercicios asignados, mensajes) bajan a la app
   - Sincronización **silenciosa** (sin notificaciones de "Sincronizando...")
   - Botón manual "Sincronizar" para forzar sincronización
   - Si no hay internet, la app sigue funcionando normalmente

---

## 🛠️ STACK TECNOLÓGICO (100% GRATIS)

### **Base de Datos: Supabase**
```
Plan: Free (gratis para siempre)
Storage: 500 MB
Compute hours: ILIMITADO
Conexiones: 100 simultáneas
Backup: 7 días automático

✅ PostgreSQL completo
✅ Dashboard visual
✅ Sin tarjeta de crédito
✅ API REST automática (opcional)

Capacidad: 80-100 pacientes activos
URL: https://supabase.com/
```

### **Backend: Render.com**
```
Plan: Free (gratis para siempre)
RAM: 512 MB
CPU: Compartido
Horas: 750/mes (24/7)
Limitación: Duerme tras 15 min de inactividad

✅ Node.js/Express
✅ Deploy automático desde GitHub
✅ Variables de entorno
✅ Logs en tiempo real

Solución al "dormir": UptimeRobot hace ping cada 5 min
URL: https://render.com/
```

### **Frontend: Vercel**
```
Plan: Hobby (gratis para siempre)
Bandwidth: 100 GB/mes
Builds: Ilimitados
Dominios: 1 custom gratis

✅ Next.js optimizado
✅ Deploy automático desde GitHub
✅ Edge functions
✅ Sin limitaciones reales para 100 pacientes

URL: https://vercel.com/
```

### **Keep-Alive: UptimeRobot**
```
Plan: Free (gratis para siempre)
Monitores: 50
Intervalo: Cada 5 minutos

✅ Hace ping al backend de Render
✅ Evita que se "duerma"
✅ Alertas si cae el servicio

URL: https://uptimerobot.com/
```

### **Despliegue App Android:**
```
FASE 1 (0-50 pacientes): GitHub Releases
- Gratis, ilimitado
- Auto-actualización implementable

FASE 2 (50-100 pacientes): Firebase App Distribution
- Gratis, hasta 200 testers
- Notificaciones automáticas

FASE 3 (100+ pacientes): Google Play Store
- $25 USD una sola vez
- Actualizaciones automáticas
```

---

## 📊 ARQUITECTURA DEL SISTEMA

```
┌─────────────────────────────────────────────────────────────┐
│                      USUARIOS                               │
├──────────────────────┬──────────────────────────────────────┤
│   DOCTORES (Web)     │      PACIENTES (App Android)         │
│   - Dashboard        │      - Ver ejercicios                │
│   - Gestión          │      - Registrar salud               │
│   - Reportes         │      - Ver progreso                  │
└──────────┬───────────┴────────────────┬─────────────────────┘
           │                            │
           ▼                            ▼
    ┌──────────────┐          ┌──────────────────┐
    │   VERCEL     │          │   APP ANDROID    │
    │  (Frontend)  │          │  (Kotlin/Jetpack)│
    │   Next.js    │          │   Room + Retrofit│
    └──────┬───────┘          └────────┬─────────┘
           │                           │
           │         ┌─────────────────┘
           │         │
           ▼         ▼
    ┌──────────────────────┐
    │    RENDER.COM        │
    │   (Backend API)      │
    │  Express + Prisma    │
    └──────────┬───────────┘
               │
               ▼
    ┌──────────────────────┐
    │     SUPABASE         │
    │  (PostgreSQL DB)     │
    │   500 MB Storage     │
    └──────────────────────┘

    ┌──────────────────────┐
    │   UPTIMEROBOT        │
    │  (Keep Backend Alive)│
    │   Ping cada 5 min    │
    └──────────────────────┘

    ┌──────────────────────────────────────────┐
    │   NOTIFICACIONES LOCALES (App Android)   │
    │   - WorkManager (recordatorios ejercicio)│
    │   - AlarmManager (recordatorios salud)   │
    │   - Notification API (cuando sincroniza) │
    └──────────────────────────────────────────┘
```

---

## 🗄️ ESQUEMA DE BASE DE DATOS (PRISMA)

### **Modelos principales:**

**1. User (Usuario):**
- Información personal: email, password, nombre, apellido, teléfono, fecha nacimiento, género
- Role: DOCTOR o PATIENT
- Estado: INACTIVE (registrado sin aprobar), ACTIVE (activado por doctor), SUSPENDED
- Relación: Doctor tiene muchos pacientes
- Timestamp de última sincronización con la app

**2. HealthRecord (Registro de Salud):**
- Pertenece a un User (paciente)
- Tipo: presión, glucosa, temperatura, peso, etc.
- Valor y fecha del registro
- Notas opcionales
- syncId: ID único del registro local (evita duplicados al sincronizar)
- syncedAt: Timestamp de cuándo se sincronizó

**3. Exercise (Catálogo de Ejercicios):**
- Nombre, descripción
- Duración, repeticiones, series
- Imágenes y videos (URLs)
- Categoría (cuello, espalda, piernas, etc.)

**4. ExerciseAssignment (Asignación de Ejercicio):**
- Relaciona Exercise con Patient
- Doctor asigna ejercicio a paciente
- Fecha de asignación, inicio, fin
- Estado: PENDING, IN_PROGRESS, COMPLETED, CANCELLED
- Progreso (0-100%)
- Configuración de notificaciones (cada X horas)
- syncId y syncedAt para sincronización

**5. ExerciseCompletion (Registro de Completación):**
- Paciente marca ejercicio como completado
- Fecha, duración real, dificultad (1-5), dolor (1-10)
- Notas opcionales
- syncId y syncedAt

**6. Notification (Notificaciones):**
- Pertenece a un User
- Tipo: account_activated, exercise_assigned, message_from_doctor
- Título, mensaje, estado leída/no leída
- Metadata adicional (JSON)
- Se guardan en BD, la app las muestra localmente al sincronizar

**7. SystemConfig (Configuración del Sistema):**
- Key-value para configuraciones generales

### **Estrategia de sincronización:**
- Cada modelo crítico tiene `syncId` (ID local de Room) y `syncedAt` (timestamp)
- Esto permite:
  - Evitar duplicados al sincronizar
  - Saber qué datos ya fueron sincronizados
  - Detectar conflictos (last-write-wins)
  - Sincronización incremental (solo cambios desde último sync)

---

## 🔧 ESTRUCTURA DEL PROYECTO BACKEND

### **Tecnologías:**
- Node.js + Express.js (TypeScript)
- Prisma ORM (conexión a Supabase PostgreSQL)
- JWT para autenticación
- Zod para validación
- Bcrypt para hash de contraseñas

### **Organización:**
- **Rutas (routes/):** Endpoints REST API para auth, usuarios, pacientes, salud, ejercicios, sincronización
- **Controladores (controllers/):** Lógica de negocio para cada endpoint
- **Servicios (services/):** Lógica reutilizable (autenticación, sincronización, reportes)
- **Middleware:** Verificación de JWT, roles, manejo de errores
- **Utils:** Helpers para JWT, validaciones

### **Endpoints principales:**

**Autenticación:**
- POST /auth/register - Registro de usuario (INACTIVE por defecto)
- POST /auth/login - Login (devuelve JWT)
- GET /auth/me - Info del usuario actual

**Sincronización:**
- POST /sync/push - Recibe datos locales del paciente (registros salud, ejercicios completados)
- GET /sync/pull - Devuelve datos nuevos para el paciente (ejercicios asignados, mensajes, estado cuenta)

**Pacientes (solo doctores):**
- GET /patients - Lista de pacientes del doctor
- GET /patients/:id - Detalle de un paciente
- PATCH /patients/:id/activate - Activar cuenta de paciente
- PATCH /patients/:id/deactivate - Desactivar cuenta

**Registros de Salud:**
- GET /health - Obtener registros (doctor ve todos sus pacientes, paciente solo los suyos)
- POST /health - Crear registro (desde app o web)
- GET /health/stats - Estadísticas y gráficos

**Ejercicios:**
- GET /exercises - Catálogo de ejercicios
- POST /exercises - Crear ejercicio (doctor)
- GET /assignments - Ejercicios asignados
- POST /assignments - Asignar ejercicio a paciente (doctor)
- POST /completions - Registrar completación de ejercicio (paciente)

**Notificaciones:**
- GET /notifications - Obtener notificaciones del usuario
- PATCH /notifications/:id/read - Marcar como leída

---

## 🎨 ESTRUCTURA DEL PROYECTO FRONTEND

### **Tecnologías:**
- Next.js 14 (App Router) con TypeScript
- Tailwind CSS para estilos
- shadcn/ui para componentes (botones, modales, formularios)
- Recharts para gráficos
- React Hook Form + Zod para formularios
- NextAuth.js para autenticación
- Axios para llamadas HTTP

### **Organización:**

**Páginas principales:**

**Para doctores:**
- Dashboard con estadísticas (total pacientes, pendientes de activación, registros hoy)
- Lista de pacientes (filtros por estado: activos, inactivos, pendientes)
- Detalle de paciente individual:
  - Información personal
  - Gráficos de salud (presión, glucosa, peso en el tiempo)
  - Ejercicios asignados y su progreso
  - Historial de sesiones
- Catálogo de ejercicios (crear, editar, eliminar)
- Asignar ejercicios a pacientes
- Ver y responder mensajes

**Para pacientes:**
- Mis registros de salud (con gráficos)
- Mis ejercicios asignados
- Marcar ejercicios como completados
- Historial de progreso
- Perfil y configuración

**Autenticación:**
- Login (email + password)
- Registro (con opción de rol: doctor o paciente)

### **Componentes principales:**
- PatientCard - Tarjeta de paciente con info resumida
- HealthChart - Gráfico de línea para evolución de registros de salud
- ExerciseCard - Tarjeta de ejercicio con botón de completar
- ExerciseAssigner - Modal para asignar ejercicios a pacientes
- Navbar - Navegación principal
- Sidebar - Menú lateral con opciones según rol

---

## 📱 MODIFICACIONES EN LA APP ANDROID

### **Agregar capa de sincronización:**

**ApiService (Retrofit):**
- Definir endpoints para comunicarse con el backend
- Login, registro
- Push de datos locales (sincronización)
- Pull de datos remotos (sincronización)
- CRUD de registros de salud
- CRUD de ejercicios y asignaciones

**SyncManager:**
- Servicio que maneja la sincronización bidireccional
- **syncAll():** Función principal que ejecuta push y pull
- **pushLocalChanges():** Envía al backend todos los datos locales sin sincronizar (registros de salud, ejercicios completados)
- **pullRemoteChanges():** Descarga del backend datos nuevos (ejercicios asignados, cambio de estado, notificaciones)
- **Manejo de errores:** Si falla la sincronización, se reintenta más tarde
- **Sincronización silenciosa:** No muestra notificaciones al usuario, solo logs

**Triggers de sincronización:**
- Al abrir la app (si hay internet)
- Al cerrar la app (si hay internet)
- Periódicamente en segundo plano con WorkManager (cada 6-12 horas)
- Botón manual "Sincronizar" en configuración

**Interceptar cuenta inactiva:**
- Al hacer login o sincronizar, verificar estado de cuenta
- Si estado es INACTIVE, mostrar pantalla especial:
  - "Tu cuenta está pendiente de activación"
  - "Recibirás una notificación cuando sea activada"
  - Botón "Verificar estado" (fuerza sincronización)
- Bloquear todas las funciones de la app
- Cuando sincroniza y detecta estado ACTIVE:
  - Mostrar notificación local "¡Tu cuenta ha sido activada!"
  - Desbloquear funciones

**Detectar nuevas notificaciones al sincronizar:**
- Después de pullRemoteChanges(), revisar si hay notificaciones sin leer
- Si hay nuevas:
  - Mostrar notificación LOCAL de Android
  - Al hacer tap, abrir la pantalla correspondiente

**Room Database:**
- Mantener todos los modelos existentes
- Agregar campos syncId y syncedAt a modelos críticos
- Esto permite saber qué registros ya fueron sincronizados

---

## 🔐 AUTENTICACIÓN Y AUTORIZACIÓN

### **JWT Strategy:**
- Login devuelve JWT token
- Token incluye: userId, role, estado
- Frontend guarda token en localStorage/cookies
- Android guarda token en DataStore encriptado
- Cada request incluye: `Authorization: Bearer <token>`

### **Middleware de autorización:**
- **requireAuth:** Verifica que el request tenga un JWT válido
- **requireRole:** Verifica que el usuario tenga el rol necesario (DOCTOR o PATIENT)
- **requireActive:** Verifica que la cuenta esté en estado ACTIVE (no INACTIVE ni SUSPENDED)
- Se aplican en las rutas según sea necesario:
  - Activar paciente: requireAuth + requireRole(DOCTOR)
  - Ver mis registros: requireAuth + requireActive
  - Ver pacientes: requireAuth + requireRole(DOCTOR) + requireActive

---

## 🔔 SISTEMA DE NOTIFICACIONES

### **Tipos de notificaciones:**

1. **account_activated**: Cuando doctor activa una cuenta (se muestra al sincronizar)
2. **exercise_reminder**: Recordatorios de ejercicios (WorkManager - LOCAL)
3. **health_check**: Recordatorios de registrar salud (AlarmManager - LOCAL)
4. **exercise_assigned**: Nuevo ejercicio asignado (se muestra al sincronizar)
5. **message_from_doctor**: Mensajes personalizados del doctor (se muestra al sincronizar)

### **Flujo:**

**Notificaciones desde backend (activación, mensajes):**
```
Backend crea notificación en BD
    ↓
Paciente abre la app
    ↓
App sincroniza con backend
    ↓
Detecta nuevas notificaciones sin leer
    ↓
Muestra notificación LOCAL de Android
    ↓
Usuario tap → App abre en pantalla correspondiente
    ↓
Marca notificación como leída en BD
```

**Notificaciones programadas locales (ejercicios, salud):**
```
Doctor asigna ejercicio con recordatorio cada X horas
    ↓
App sincroniza y obtiene la asignación
    ↓
WorkManager programa notificación local
    ↓
A la hora indicada → WorkManager dispara notificación
    ↓
Usuario ve notificación incluso si app está cerrada
    ↓
Tap → Abre app en pantalla de ejercicio
```

---

## 🚀 PLAN DE IMPLEMENTACIÓN

### **FASE 1: Setup Inicial (1-2 días)**
1. ✅ Crear proyecto en Supabase
2. ✅ Configurar Prisma con Supabase
3. ✅ Crear esquema de BD
4. ✅ Ejecutar migraciones
5. ✅ Setup backend básico en Render
6. ✅ Setup frontend básico en Vercel

### **FASE 2: Autenticación (2-3 días)**
7. ✅ Implementar registro de usuarios
8. ✅ Implementar login (JWT)
9. ✅ Sistema de roles (doctor/patient)
10. ✅ Estado de cuenta (activo/inactivo)
11. ✅ Pantalla de cuenta inactiva en app

### **FASE 3: Gestión de Pacientes (3-4 días)**
12. ✅ Dashboard doctor (web)
13. ✅ Lista de pacientes pendientes
14. ✅ Activar/desactivar cuentas
15. ✅ Ver detalle de paciente
16. ✅ Notificación de activación

### **FASE 4: Registros de Salud (3-4 días)**
17. ✅ CRUD registros de salud (web)
18. ✅ Gráficos de evolución
19. ✅ Filtros por tipo y fecha
20. ✅ Sincronización app ↔ backend

### **FASE 5: Ejercicios (4-5 días)**
21. ✅ Catálogo de ejercicios (web)
22. ✅ Asignar ejercicios a pacientes
23. ✅ Ver progreso de ejercicios
24. ✅ Marcar como completado
25. ✅ Sincronización app ↔ backend

### **FASE 6: Notificaciones (2-3 días)**
26. ✅ Sistema de notificaciones en BD
27. ✅ Crear notificación al activar cuenta
28. ✅ Crear notificación al asignar ejercicio
29. ✅ App muestra notificaciones al sincronizar
30. ✅ Historial de notificaciones

### **FASE 7: Sincronización (3-4 días)**
31. ✅ API de sincronización
32. ✅ Push de datos locales
33. ✅ Pull de datos remotos
34. ✅ Resolución de conflictos
35. ✅ Sincronización automática

### **FASE 8: Panel Paciente (2-3 días)**
36. ✅ Vista de mis ejercicios
37. ✅ Vista de mi salud
38. ✅ Perfil y configuración
39. ✅ Ver notificaciones

### **FASE 9: Reportes y Extras (2-3 días)**
40. ✅ Generar PDF de progreso
41. ✅ Estadísticas avanzadas
42. ✅ Exportar datos

### **FASE 10: Deploy y Testing (2-3 días)**
43. ✅ Deploy final en Render
44. ✅ Deploy final en Vercel
45. ✅ Configurar UptimeRobot
46. ✅ Testing end-to-end
47. ✅ Documentación

---

## 🎨 TECNOLOGÍAS FRONTEND

- **Framework:** Next.js 14 (App Router)
- **Lenguaje:** TypeScript
- **Estilos:** Tailwind CSS
- **Componentes UI:** shadcn/ui
- **Gráficos:** Recharts
- **Formularios:** React Hook Form + Zod
- **State Management:** Zustand o React Query
- **Autenticación:** NextAuth.js
- **HTTP Client:** Axios

---

## 🔧 TECNOLOGÍAS BACKEND

- **Runtime:** Node.js 20+
- **Framework:** Express.js
- **Lenguaje:** TypeScript
- **ORM:** Prisma ORM
- **Base de Datos:** PostgreSQL (Supabase)
- **Autenticación:** JWT (jsonwebtoken)
- **Validación:** Zod
- **Generación PDF:** pdfkit o jsPDF

---

## 📝 VARIABLES DE ENTORNO

### **Backend (.env):**
- DATABASE_URL: Conexión a PostgreSQL de Supabase
- JWT_SECRET: Clave secreta para firmar tokens JWT
- JWT_EXPIRES_IN: Tiempo de expiración del token (ej: "7d")
- PORT: Puerto del servidor (default: 3001)
- NODE_ENV: Entorno (development/production)
- FRONTEND_URL: URL del frontend para CORS

### **Frontend (.env.local):**
- NEXT_PUBLIC_API_URL: URL del backend en Render
- NEXTAUTH_URL: URL del frontend (para NextAuth)
- NEXTAUTH_SECRET: Clave secreta para NextAuth

---

## ✅ CRITERIOS DE ÉXITO

### **Funcional:**
- ✅ Doctor puede registrarse y activar pacientes
- ✅ Paciente no puede usar app hasta ser activado
- ✅ Paciente recibe notificación LOCAL al sincronizar
- ✅ Sincronización bidireccional funciona
- ✅ Notificaciones locales programadas funcionan (WorkManager/AlarmManager)
- ✅ Datos se reflejan correctamente tras sincronización

### **No Funcional:**
- ✅ Respuesta de API < 500ms
- ✅ Backend no se duerme (UptimeRobot)
- ✅ UI responsive (mobile + desktop)
- ✅ Carga inicial < 3 segundos
- ✅ 100% gratis para 100 pacientes

---

## 🎯 SIGUIENTE PASO INMEDIATO

**EMPEZAR CON:**

1. Configurar Supabase
2. Configurar Prisma y ejecutar migraciones
3. Crear estructura básica del backend
4. Implementar endpoint de autenticación
5. Implementar dashboard doctor básico

**¿ESTÁS LISTO PARA EMPEZAR?** 🚀

---

## 📚 RECURSOS ÚTILES

- Supabase Docs: https://supabase.com/docs
- Prisma Docs: https://www.prisma.io/docs
- Next.js Docs: https://nextjs.org/docs
- shadcn/ui: https://ui.shadcn.com/
- Render Docs: https://render.com/docs
- Vercel Docs: https://vercel.com/docs
- WorkManager (Android): https://developer.android.com/topic/libraries/architecture/workmanager
- AlarmManager (Android): https://developer.android.com/reference/android/app/AlarmManager

---

## ⚠️ NOTAS IMPORTANTES

1. **Seguridad:**
   - NUNCA commitear .env a Git
   - Usar variables de entorno en Render y Vercel
   - Hashear passwords con bcrypt
   - Validar TODOS los inputs

2. **Rendimiento:**
   - Implementar paginación en listas grandes
   - Cachear queries frecuentes
   - Optimizar imágenes en frontend
   - Usar índices en BD para queries frecuentes

3. **Sincronización:**
   - Usar timestamps para detectar cambios
   - Implementar resolución de conflictos (last-write-wins)
   - Permitir sincronización manual
   - Mostrar estado de sincronización al usuario

4. **Notificaciones:**
   - Las notificaciones de activación/mensajes se guardan en BD
   - App las muestra localmente al sincronizar
   - WorkManager y AlarmManager manejan recordatorios programados
   - Pedir permiso de notificaciones al usuario en Android

---

## 🎉 RESULTADO FINAL

Al terminar tendrás:

✅ **Plataforma web completa** para doctores gestionar pacientes
✅ **Sistema de activación** de cuentas con notificaciones
✅ **Sincronización bidireccional** app ↔ backend
✅ **Dashboard con gráficos** de progreso
✅ **Gestión de ejercicios** y asignaciones
✅ **Notificaciones locales** automáticas y personalizadas
✅ **Panel para pacientes** ver su progreso
✅ **100% GRATIS** hasta 100 pacientes
✅ **Escalable** a más usuarios cuando lo necesites

---

**¿COMENZAMOS A IMPLEMENTAR?** 🚀

