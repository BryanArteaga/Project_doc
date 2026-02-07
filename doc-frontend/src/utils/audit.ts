/**
 * Sistema de auditoría y logging para el frontend
 */

export enum AuditAction {
  LOGIN = 'LOGIN',
  LOGOUT = 'LOGOUT',
  VIEW_PATIENTS = 'VIEW_PATIENTS',
  VIEW_PATIENT_DETAIL = 'VIEW_PATIENT_DETAIL',
  ACTIVATE_PATIENT = 'ACTIVATE_PATIENT',
  DEACTIVATE_PATIENT = 'DEACTIVATE_PATIENT',
  UPDATE_DOCTOR = 'UPDATE_DOCTOR',
  REGISTER_DOCTOR = 'REGISTER_DOCTOR',
  VIEW_REPORTS = 'VIEW_REPORTS',
  UPDATE_SETTINGS = 'UPDATE_SETTINGS',
  CHANGE_PASSWORD = 'CHANGE_PASSWORD',
  ERROR = 'ERROR',
  UNAUTHORIZED_ACCESS = 'UNAUTHORIZED_ACCESS'
}

export enum AuditLevel {
  INFO = 'INFO',
  WARNING = 'WARNING',
  ERROR = 'ERROR',
  CRITICAL = 'CRITICAL'
}

export interface AuditLog {
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

class AuditService {
  private logs: AuditLog[] = [];
  private maxLogs = 1000;
  private isEnabled = true;

  // Registrar evento de auditoría
  log(
    action: AuditAction,
    level: AuditLevel = AuditLevel.INFO,
    details?: string,
    metadata?: Record<string, any>
  ): void {
    if (!this.isEnabled) return;

    const log: AuditLog = {
      timestamp: new Date().toISOString(),
      action,
      level,
      details,
      metadata,
      userAgent: navigator.userAgent,
    };

    // Agregar información del usuario si está disponible
    const doctorData = localStorage.getItem('doctor_data');
    if (doctorData) {
      try {
        const doctor = JSON.parse(doctorData);
        log.userId = doctor.id?.toString();
        log.userName = `${doctor.nombre} ${doctor.apellido}`;
        log.userRole = doctor.rol;
      } catch (error) {
        console.error('Error parsing doctor data for audit:', error);
      }
    }

    this.logs.push(log);

    // Mantener solo los últimos N logs
    if (this.logs.length > this.maxLogs) {
      this.logs = this.logs.slice(-this.maxLogs);
    }

    // Log en consola en desarrollo
    if (import.meta.env.DEV) {
      const color = this.getLevelColor(level);
      console.log(
        `%c[AUDIT ${level}] ${action}`,
        `color: ${color}; font-weight: bold`,
        details || '',
        metadata || ''
      );
    }

    // Guardar logs críticos en localStorage
    if (level === AuditLevel.CRITICAL || level === AuditLevel.ERROR) {
      this.saveCriticalLog(log);
    }

    // Enviar al backend si es necesario (implementar según necesidad)
    // this.sendToBackend(log);
  }

  // Obtener color para cada nivel
  private getLevelColor(level: AuditLevel): string {
    switch (level) {
      case AuditLevel.INFO: return '#5B65DC';
      case AuditLevel.WARNING: return '#f59e0b';
      case AuditLevel.ERROR: return '#ef4444';
      case AuditLevel.CRITICAL: return '#dc2626';
      default: return '#64748b';
    }
  }

  // Guardar logs críticos en localStorage
  private saveCriticalLog(log: AuditLog): void {
    try {
      const criticalLogs = this.getCriticalLogs();
      criticalLogs.push(log);
      
      // Mantener solo los últimos 50 logs críticos
      const recentLogs = criticalLogs.slice(-50);
      localStorage.setItem('critical_logs', JSON.stringify(recentLogs));
    } catch (error) {
      console.error('Error saving critical log:', error);
    }
  }

  // Obtener logs críticos
  getCriticalLogs(): AuditLog[] {
    try {
      const logs = localStorage.getItem('critical_logs');
      return logs ? JSON.parse(logs) : [];
    } catch {
      return [];
    }
  }

  // Obtener todos los logs
  getAllLogs(): AuditLog[] {
    return [...this.logs];
  }

  // Obtener logs por acción
  getLogsByAction(action: AuditAction): AuditLog[] {
    return this.logs.filter(log => log.action === action);
  }

  // Obtener logs por nivel
  getLogsByLevel(level: AuditLevel): AuditLog[] {
    return this.logs.filter(log => log.level === level);
  }

  // Obtener logs en rango de tiempo
  getLogsByTimeRange(startDate: Date, endDate: Date): AuditLog[] {
    return this.logs.filter(log => {
      const logDate = new Date(log.timestamp);
      return logDate >= startDate && logDate <= endDate;
    });
  }

  // Limpiar logs
  clearLogs(): void {
    this.logs = [];
  }

  // Habilitar/deshabilitar auditoría
  setEnabled(enabled: boolean): void {
    this.isEnabled = enabled;
  }

  // Exportar logs como JSON
  exportLogsAsJSON(): string {
    return JSON.stringify(this.logs, null, 2);
  }

  // Exportar logs como CSV
  exportLogsAsCSV(): string {
    const headers = ['Timestamp', 'Action', 'Level', 'User', 'Role', 'Details'];
    const rows = this.logs.map(log => [
      log.timestamp,
      log.action,
      log.level,
      log.userName || 'N/A',
      log.userRole || 'N/A',
      log.details || ''
    ]);

    const csv = [
      headers.join(','),
      ...rows.map(row => row.map(cell => `"${cell}"`).join(','))
    ].join('\n');

    return csv;
  }
}

// Exportar instancia singleton
export const auditService = new AuditService();

// Funciones helper para logging común
export const logLogin = (userName: string) => {
  auditService.log(
    AuditAction.LOGIN,
    AuditLevel.INFO,
    `Usuario ${userName} inició sesión`,
    { userName }
  );
};

export const logLogout = (userName: string) => {
  auditService.log(
    AuditAction.LOGOUT,
    AuditLevel.INFO,
    `Usuario ${userName} cerró sesión`,
    { userName }
  );
};

export const logError = (error: Error, context?: string) => {
  auditService.log(
    AuditAction.ERROR,
    AuditLevel.ERROR,
    `Error: ${error.message}`,
    { error: error.toString(), stack: error.stack, context }
  );
};

export const logUnauthorizedAccess = (attemptedRoute: string) => {
  auditService.log(
    AuditAction.UNAUTHORIZED_ACCESS,
    AuditLevel.WARNING,
    `Intento de acceso no autorizado a: ${attemptedRoute}`,
    { route: attemptedRoute }
  );
};
