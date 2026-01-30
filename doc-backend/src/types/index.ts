import { Request } from 'express';
export interface AuthRequest extends Request {
  userId?: number;
  userCedula?: string;
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
export type AccountStatus = 'pending' | 'active' | 'inactive';
