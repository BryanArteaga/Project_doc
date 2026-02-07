// Tipos de usuario
export interface Doctor {
  id: number;
  email: string;
  nombre: string;
  apellido: string;
  cedula: string;
  telefono?: string;
  especialidad?: string;
  rol: 'ADMIN' | 'DOCTOR';
  activo: boolean;
  supabaseUserId: string;
  creadoEn: string;
}

// Auth
export interface LoginCredentials {
  email: string;
  password: string;
}

export interface LoginResponse {
  message: string;
  token: string;
  refreshToken: string;
  doctor: Doctor;
}

export interface AuthContextType {
  doctor: Doctor | null;
  token: string | null;
  isAuthenticated: boolean;
  isAdmin: boolean;
  isDoctor: boolean;
  login: (credentials: LoginCredentials) => Promise<LoginResponse>;
  logout: () => void;
  loading: boolean;  loggingOut?: boolean;}

// Paciente
export interface Patient {
  id: number;
  email: string;
  nombre: string;
  apellido: string;
  cedula?: string;
  telefonoPersonal?: string;
  fechaNacimiento?: string;
  edad?: number;
  peso?: number;
  altura?: number;
  sexo?: string;
  enfermedad?: string;
  colorEnfermedad?: string;
  estadoCuenta: 'INACTIVE' | 'ACTIVE' | 'SUSPENDED';
  idDoctorAsignado?: number;
  glucosaInicial?: number;
  colesterolInicial?: number;
  registradoEn: string;
  activadoEn?: string;
}

// Stats
export interface DashboardStats {
  totalPacientes: number;
  pacientesPendientes: number;
  pacientesActivos: number;
  pacientesInactivos: number;
  registrosHoy: number;
}
