import { Request } from 'express';

export interface AuthRequest extends Request {
  userId?: number;
  userCedula?: string;
  doctorId?: string;
  doctorDbId?: number;
  doctorRole?: string;
  estadoCuenta?: string;
}

export interface RegisterDTO {
  cedula: string;
  nombre: string;
  edad: number;
  peso: number;
  altura: number;
  sexo: string;
  enfermedad: string;
  colorEnfermedad: string;
  glucosaInicial?: number;
  colesterolInicial?: number;
}

export interface LoginDTO {
  cedula: string;
}

export interface RegistroSaludDTO {
  glucosa?: number;
  colesterol?: number;
  presionSist?: number;
  presionDias?: number;
  peso?: number;
  observaciones?: string;
}

export interface EjercicioRealizadoDTO {
  ejercicioId: number;
  nombreEjercicio: string;
  duracion: number;
  completado?: boolean;
}

export interface CodigoActivacionDTO {
  doctorCedula: string;
  expiraEnDias?: number;
}

// ✅ AsignacionEjercicio DTOs
export interface CreateAsignacionEjercicioDTO {
  ejercicioId: number;
  usuarioId?: number | null;              // null = general; number = individual
  enfermedad?: string;                    // Filtro: asignar solo a pacientes con esta enfermedad
  descripcion?: string;
  fechaExpiracion?: string;               // ISO date string (2026-03-31T23:59:59Z)
}

export interface UpdateAsignacionEjercicioDTO {
  descripcion?: string;
  fechaExpiracion?: string;
  activo?: boolean;
}

export interface AsignacionEjercicioDTO {
  id: number;
  ejercicioId: number;
  doctorId: number;
  usuarioId: number | null;
  enfermedad?: string | null;
  descripcion?: string | null;
  activo: boolean;
  fechaAsignacion: string;
  fechaExpiracion?: string | null;
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
}

export type AccountStatus = 'pending' | 'active' | 'inactive';
