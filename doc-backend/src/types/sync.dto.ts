/**
 * DTOs para sincronización entre App Móvil y Backend
 * 
 * PUSH: Lo que envía la app al servidor
 * PULL: Lo que el servidor envía a la app
 */

// ============ PUSH REQUESTS (App → Server) ============

export interface PushRegistroSaludRequest {
  // usuarioId viene en el nivel superior (SyncPushRequest.usuarioId)
  fecha: string;                    // yyyy-MM-dd o yyyy-MM-dd HH:mm:ss
  hora?: string;                    // HH:mm (opcional si viene en fecha)
  presionSistolica?: number;
  presionDiastolica?: number;
  glucosa?: number;                 // mg/dL
  colesterol?: number;              // mg/dL
  temperatura?: number;
  peso?: number;
  calorias?: number;
  ejercicioMinutos?: number;        // ✅ Minutos de ejercicio (calculado en app)
  estadoAnimo?: string;             // Happy, Neutral, Sad
  notas?: string;
}

export interface PushEjercicioRealizadoRequest {
  // usuarioId viene en el nivel superior (SyncPushRequest.usuarioId)
  ejercicioId: number;
  asignacionId?: number;            // ID de la asignación (opcional)
  fecha: string;                    // yyyy-MM-dd o yyyy-MM-dd HH:mm:ss
  completadoEn: string;             // ISO timestamp
}

export interface SyncPushRequest {
  usuarioId: number;                // ID del usuario (sin JWT)
  registrosSalud: PushRegistroSaludRequest[];
  ejerciciosRealizados: PushEjercicioRealizadoRequest[];
  ultimaSincronizacion?: string;    // ISO timestamp (opcional)
}

export interface SyncPushResponse {
  success: boolean;
  message: string;
  registrosSincronizados: number;
  ejerciciosSincronizados: number;
  timestamp: string;
  errores?: {
    registros?: string[];
    ejercicios?: string[];
  };
}

// ============ PULL RESPONSES (Server → App) ============

export interface EjercicioResponse {
  id: number;
  nombre: string;
  duracion: number | null;
  descripcion: string | null;
  intensidad: string | null;
  enfermedad: string | null;
  series: number | null;
  descansoEntreSeries: number | null;
  urlImagen: string | null;
  urlVideo: string | null;
}

// ✅ DTO para AsignacionEjercicio en PULL
export interface AsignacionEjercicioResponse {
  id: number;
  ejercicioId: number;
  doctorId: number;
  usuarioId: number | null;
  enfermedad?: string | null;
  descripcion?: string | null;
  activo: boolean;
  fechaAsignacion: string;           // timestamp string
  fechaExpiracion?: string | null;    // timestamp string
  ejercicio?: {
    id: number;
    nombre: string;
    descripcion?: string;
    duracion?: number;
    series?: number;
    intensidad?: string;
    urlImagen?: string;
    urlVideo?: string;
  };
  doctor?: {
    id: number;
    nombre: string;
    apellido?: string;
    especialidad?: string;
  };
}

export interface RegistroSaludResponse {
  id: number;
  fecha: string;
  hora?: string;
  presionSistolica?: number;
  presionDiastolica?: number;
  glucosa?: number;
  colesterol?: number;
  temperatura?: number;
  peso?: number;
  calorias?: number;
  ejercicioMinutos?: number;        // ✅ Minutos de ejercicio
  estadoAnimo?: string;
  notas?: string;
  registradoEn: number; // Timestamp en milisegundos
}

export interface NotificacionResponse {
  id: number;
  titulo: string;
  mensaje: string;
  tipo: string;                     // account_activated, health_reminder, etc
  leido: boolean;
  creadoEn: Date;                   // DateTime
}

export interface EstadoAccountResponse {
  activo: boolean;
  verificado: boolean;
  ultimaSincronizacion: string;
}

// ✅ DTO para Consejo en PULL
export interface ConsejoResponse {
  id: number;
  consejo: string;
  enfermedad: string;
  creadoEn: Date;
}

export interface UsuarioResponse {
  id: number;
  nombre: string;
  apellido: string;
  email: string;
  telefono: string | null;
  edad?: number;
  peso?: number;
  altura?: number;
  sexo?: string;
  enfermedad?: string;
  colorEnfermedad?: string;
  // ✅ NUEVO: Configuración de notificaciones del usuario
  notifEnabled: boolean;
  notifType: string;
  notifDetails: string;
}

export interface SyncPullResponse {
  success: boolean;
  usuario: UsuarioResponse;
  registrosSalud: RegistroSaludResponse[];         // ✅ Registros de salud históricos
  asignaciones: AsignacionEjercicioResponse[];     // ✅ NUEVO: Asignaciones de ejercicios
  ejercicios: EjercicioResponse[];
  consejos: ConsejoResponse[];                     // ✅ NUEVO: Consejos por enfermedad
  notificaciones: NotificacionResponse[];
  estadoCuenta: EstadoAccountResponse;
}

// ============ DTOs ADICIONALES ============

/**
 * Cuando el usuario se registra, almacena el FCM token
 * para que el server pueda enviar notificaciones
 */
export interface RegisterFCMTokenRequest {
  usuarioId: number;
  fcmToken: string;
  plataforma: 'android' | 'ios';
}

/**
 * Error de sincronización
 */
export interface SyncErrorResponse {
  success: false;
  error: string;
  code: 'NETWORK_ERROR' | 'AUTH_ERROR' | 'VALIDATION_ERROR' | 'SERVER_ERROR';
  timestamp: string;
}

// ✅ NUEVO: Respuesta para verificar si hay cambios sin descargar datos
export interface CheckUpdatesResponse {
  shouldPull: boolean;                    // True si hay cambios y la app debe hacer PULL
  hasExerciseUpdates: boolean;            // Si hay ejercicios nuevos/actualizados
  hasAdviceUpdates: boolean;              // Si hay consejos nuevos/actualizados
  hasAssignmentUpdates: boolean;          // Si hay asignaciones nuevas/actualizadas
  lastBackendSync: number;                // Timestamp en milisegundos del servidor
}

// ============ TIPOS INTERNOS PARA BD ============

export interface RegistroSaludDB extends PushRegistroSaludRequest {
  id: number;
  isSynced: boolean;
  idSincronizacion?: string;
  sincronizadoEn?: string;
  intentosSincronizacion: number;
  ultimoErrorSincronizacion?: string;
  registradoEn: string;
  actualizadoEn: string;
}

export interface EjercicioRealizadoDB extends PushEjercicioRealizadoRequest {
  id: number;
  isSynced: boolean;
  idSincronizacion?: string;
  sincronizadoEn?: string;
  registradoEn: string;
  actualizadoEn: string;
}
