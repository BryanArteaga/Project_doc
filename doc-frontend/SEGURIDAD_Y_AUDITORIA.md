# Sistema de Seguridad y Auditoría - Doc Health Frontend

## 📋 Resumen de Implementación

Este documento describe el sistema de seguridad y auditoría implementado en el frontend de Doc Health.

---

## 🔒 Utilidades de Seguridad (`utils/security.ts`)

### Validaciones Implementadas

#### 1. **Sanitización de HTML y Texto**
```typescript
sanitizeHTML(str: string): string
sanitizeInput(input: string): string
```
- Prevención de ataques XSS
- Limpieza de caracteres peligrosos

#### 2. **Validaciones de Formato**
```typescript
isValidEmail(email: string): boolean
isValidCedula(cedula: string): boolean
isValidPhone(phone: string): boolean
```
- Validación de email con regex
- Validación de cédula (7-10 dígitos)
- Validación de teléfono

#### 3. **Validación de Contraseña Fuerte**
```typescript
isStrongPassword(password: string): { valid: boolean; message: string }
```
Requisitos:
- Mínimo 8 caracteres
- Al menos 1 mayúscula
- Al menos 1 minúscula
- Al menos 1 número

#### 4. **Validación JWT**
```typescript
isValidJWT(token: string): boolean
```
- Verifica estructura del token
- Valida que no esté expirado

#### 5. **Otras Utilidades**
- `preventSQLInjection()` - Limpia caracteres peligrosos
- `simpleHash()` - Hash SHA-256 para verificación
- `validateNumberRange()` - Valida rangos numéricos
- `sanitizeObject()` - Limpia objetos completos
- `hasPermission()` - Verifica jerarquía de roles
- `encryptData()` / `decryptData()` - Encriptación básica para localStorage

---

## 📊 Sistema de Auditoría (`utils/audit.ts`)

### Acciones Auditables

```typescript
enum AuditAction {
  LOGIN,
  LOGOUT,
  VIEW_PATIENTS,
  VIEW_PATIENT_DETAIL,
  ACTIVATE_PATIENT,
  DEACTIVATE_PATIENT,
  UPDATE_DOCTOR,
  REGISTER_DOCTOR,
  VIEW_REPORTS,
  UPDATE_SETTINGS,
  CHANGE_PASSWORD,
  ERROR,
  UNAUTHORIZED_ACCESS
}
```

### Niveles de Auditoría

```typescript
enum AuditLevel {
  INFO,        // Información general
  WARNING,     // Advertencias
  ERROR,       // Errores
  CRITICAL     // Errores críticos
}
```

### Estructura de Log

```typescript
interface AuditLog {
  timestamp: string;
  action: AuditAction;
  level: AuditLevel;
  userId?: string;
  userName?: string;
  userRole?: string;
  details?: string;
  metadata?: Record<string, any>;
  ip?: string;
  userAgent?: string;
}
```

### Funcionalidades del Sistema

#### 1. **Registro de Eventos**
```typescript
auditService.log(action, level, details, metadata)
```

#### 2. **Almacenamiento**
- Logs en memoria (últimos 1000)
- Logs críticos en localStorage (últimos 50)
- Logs en consola durante desarrollo

#### 3. **Consultas**
```typescript
getAllLogs()
getLogsByAction(action)
getLogsByLevel(level)
getLogsByTimeRange(startDate, endDate)
getCriticalLogs()
```

#### 4. **Exportación**
```typescript
exportLogsAsJSON()
exportLogsAsCSV()
```

#### 5. **Gestión**
```typescript
clearLogs()
setEnabled(enabled)
```

### Helpers de Auditoría

```typescript
logLogin(userName)
logLogout(userName)
logError(error, context)
logUnauthorizedAccess(attemptedRoute)
```

---

## 🛡️ Integración en la Aplicación

### 1. **AuthContext** (`context/AuthContext.tsx`)

#### Auditoría de Autenticación
- ✅ Login exitoso
- ✅ Login fallido
- ✅ Logout
- ✅ Token JWT inválido
- ✅ Token expirado

#### Validaciones
- Validación de formato JWT
- Verificación de expiración de token
- Registro de intentos fallidos

### 2. **Rutas Protegidas** (`App.tsx`)

#### ProtectedRoute
- Audita accesos no autorizados
- Redirige a login

#### AdminRoute
- Audita intentos de acceso sin permisos
- Registra usuario y rol que intenta acceder
- Redirige según permisos

### 3. **Página de Configuración** (`pages/Settings/Settings.tsx`)

#### Validaciones de Perfil
- Email válido
- Campos requeridos
- Sanitización de inputs

#### Validaciones de Contraseña
- Contraseña actual requerida
- Contraseña fuerte
- Confirmación de contraseña

#### Auditoría
- Actualización de perfil
- Cambio de contraseña
- Errores en actualización

### 4. **Gestión de Pacientes** (`pages/ManagePatients/ManagePatients.tsx`)

#### Auditoría
- Acceso a página de pacientes
- Activación de pacientes
- Errores en activación

---

## 📈 Panel de Auditoría (`pages/AuditLogs`)

### Características

#### 1. **Visualización de Logs**
- Lista ordenada por timestamp (más recientes primero)
- Codificación por colores según nivel
- Detalles expandibles con metadata

#### 2. **Filtros**
- Por nivel (INFO, WARNING, ERROR, CRITICAL)
- Por búsqueda de texto
- Por acción

#### 3. **Estadísticas**
- Total de logs
- Logs filtrados
- Cantidad de errores

#### 4. **Exportación**
- Exportar como CSV
- Exportar como JSON
- Descargar con timestamp

#### 5. **Gestión**
- Actualizar logs
- Limpiar logs (con confirmación)

### Acceso
- **Ruta**: `/admin/audit-logs`
- **Permisos**: Solo administradores
- **Navegación**: AdminDashboard → Logs de Auditoría

---

## 🎯 Casos de Uso

### 1. **Investigación de Incidentes**
```typescript
// Obtener todos los errores críticos
const criticalLogs = auditService.getLogsByLevel(AuditLevel.CRITICAL);

// Errores en rango de tiempo
const errors = auditService.getLogsByTimeRange(
  new Date('2026-02-01'),
  new Date('2026-02-07')
);
```

### 2. **Monitoreo de Accesos**
```typescript
// Ver intentos de acceso no autorizados
const unauthorizedAttempts = auditService.getLogsByAction(
  AuditAction.UNAUTHORIZED_ACCESS
);
```

### 3. **Auditoría de Cambios**
```typescript
// Ver cambios de configuración
const settingsChanges = auditService.getLogsByAction(
  AuditAction.UPDATE_SETTINGS
);

// Ver cambios de contraseña
const passwordChanges = auditService.getLogsByAction(
  AuditAction.CHANGE_PASSWORD
);
```

### 4. **Exportación para Análisis**
```typescript
// Exportar para análisis externo
const csvData = auditService.exportLogsAsCSV();
const jsonData = auditService.exportLogsAsJSON();
```

---

## 🔐 Mejores Prácticas Implementadas

### 1. **Sanitización de Inputs**
- Todos los inputs de usuario se sanitizan antes de procesar
- Se eliminan caracteres peligrosos (`<>`, quotes)
- Se trimean espacios en blanco

### 2. **Validación de Datos**
- Emails validados con regex
- Contraseñas fuertes obligatorias
- Teléfonos y cédulas con formato específico

### 3. **Protección de Tokens**
- Validación de estructura JWT
- Verificación de expiración
- Rechazo de tokens inválidos

### 4. **Auditoría Completa**
- Todos los eventos importantes se registran
- Información de contexto (usuario, rol, timestamp)
- Logs críticos se persisten en localStorage

### 5. **Manejo de Errores**
- Todos los errores se registran
- Stack traces en metadata
- Contexto adicional para debugging

---

## 🚀 Próximas Mejoras (Sugeridas)

### Backend
- [ ] Endpoint de auditoría para persistir logs en base de datos
- [ ] Análisis de patrones de acceso
- [ ] Alertas automáticas por eventos críticos
- [ ] Rotación de logs por fecha

### Frontend
- [ ] Dashboard de métricas de seguridad
- [ ] Alertas en tiempo real
- [ ] Filtros avanzados por fecha/hora
- [ ] Gráficos de actividad

### Seguridad
- [ ] Implementar CSP (Content Security Policy)
- [ ] Rate limiting en frontend
- [ ] Two-factor authentication (2FA)
- [ ] Biometría para acciones críticas
- [ ] Encriptación end-to-end para datos sensibles

---

## 📝 Notas Técnicas

### Almacenamiento
- **Memoria**: Últimos 1000 logs
- **localStorage**: Últimos 50 logs críticos
- **Clave**: `critical_logs`

### Performance
- Sistema ligero sin impacto significativo
- Logs en consola solo en desarrollo
- Limitación automática de cantidad de logs

### Compatibilidad
- ✅ Funciona en todos los navegadores modernos
- ✅ Compatible con modo oscuro/claro
- ✅ Responsive design

---

## 🔍 Debugging

### Ver logs en consola (desarrollo)
Los logs aparecen automáticamente en consola con colores:
- 🔵 INFO - Azul (#5B65DC)
- 🟡 WARNING - Amarillo (#f59e0b)
- 🔴 ERROR - Rojo (#ef4444)
- 🔴 CRITICAL - Rojo oscuro (#dc2626)

### Acceder a logs desde consola
```javascript
// En desarrollo, puedes acceder directamente:
import { auditService } from './utils/audit';

// Ver todos los logs
console.log(auditService.getAllLogs());

// Ver logs críticos
console.log(auditService.getCriticalLogs());
```

---

## ✅ Checklist de Seguridad Implementada

- [x] Sanitización de inputs
- [x] Validación de emails
- [x] Validación de contraseñas fuertes
- [x] Validación de tokens JWT
- [x] Prevención XSS
- [x] Prevención SQL Injection básica
- [x] Auditoría de login/logout
- [x] Auditoría de accesos no autorizados
- [x] Auditoría de cambios de configuración
- [x] Auditoría de activación de pacientes
- [x] Logs críticos persistentes
- [x] Exportación de logs
- [x] Panel de administración de logs
- [x] Protección de rutas por rol
- [x] Manejo seguro de errores

---

## 📞 Soporte

Para dudas o mejoras, contactar al equipo de desarrollo.

**Fecha de implementación**: 7 de febrero de 2026
**Versión**: 1.0.0
