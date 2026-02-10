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
  ejercicio?: number;               // minutos
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

export interface UsuarioResponse {
  id: number;
  nombre: string;
  apellido: string;
  email: string;
  telefono: string | null;
}

export interface SyncPullResponse {
  success: boolean;
  usuario: UsuarioResponse;
  ejercicios: EjercicioResponse[];
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
