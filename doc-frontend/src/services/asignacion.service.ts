import api from './api';

export interface Asignacion {
  id: number;
  ejercicioId: number;
  doctorId: number;
  usuarioId: number | null;
  enfermedad?: string;
  descripcion?: string;
  activo: boolean;
  fechaAsignacion: string;
  fechaExpiracion?: string;
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
  usuario?: {
    id: number;
    nombre: string;
    apellido: string;
    cedula?: string;
    enfermedad?: string;
  };
}

export interface CreateAsignacionRequest {
  ejercicioId: number;
  usuarioId?: number | null;
  enfermedad?: string;
  descripcion?: string;
  fechaExpiracion?: string;
}

export interface Ejercicio {
  id: number;
  nombre: string;
  descripcion?: string;
  duracion?: number;
  series?: number;
  intensidad?: string;
  urlImagen?: string;
  urlVideo?: string;
}

export const asignacionService = {
  // Crear nueva asignación
  async crearAsignacion(data: CreateAsignacionRequest): Promise<Asignacion> {
    const response = await api.post<Asignacion>('/api/asignaciones-ejercicios', data);
    return response.data;
  },

  // Listar asignaciones del doctor
  async listarAsignaciones(params?: {
    usuarioId?: number;
    enfermedad?: string;
    activo?: boolean;
    page?: number;
    limit?: number;
  }): Promise<{ asignaciones: Asignacion[], paginacion: any }> {
    const response = await api.get<{ asignaciones: Asignacion[], paginacion: any }>(
      '/api/asignaciones-ejercicios',
      { params }
    );
    return response.data;
  },

  // Obtener una asignación específica
  async obtenerAsignacion(id: number): Promise<Asignacion> {
    const response = await api.get<Asignacion>(`/api/asignaciones-ejercicios/${id}`);
    return response.data;
  },

  // Actualizar asignación
  async actualizarAsignacion(id: number, data: {
    descripcion?: string;
    fechaExpiracion?: string;
    activo?: boolean;
  }): Promise<Asignacion> {
    const response = await api.patch<Asignacion>(
      `/api/asignaciones-ejercicios/${id}`,
      data
    );
    return response.data;
  },

  // Eliminar asignación
  async eliminarAsignacion(id: number): Promise<void> {
    await api.delete(`/api/asignaciones-ejercicios/${id}`);
  },

  // Obtener estadísticas de asignaciones
  async obtenerEstadisticas(): Promise<any> {
    const response = await api.get('/api/asignaciones-ejercicios/stats');
    return response.data;
  },

  // Obtener lista de ejercicios (para crear asignaciones)
  async listarEjercicios(): Promise<Ejercicio[]> {
    const response = await api.get<{ ejercicios?: Ejercicio[] }>('/api/ejercicios');
    return response.data.ejercicios || [];
  },

  // Obtener pacientes del doctor (para asignación individual)
  async listarPacientes(): Promise<any[]> {
    const response = await api.get('/api/doctor/patients');
    return response.data.patients || [];
  }
};
