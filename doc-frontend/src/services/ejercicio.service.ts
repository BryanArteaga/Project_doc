import api from './api';

export type Enfermedad = 'Hipertensión' | 'Diabetes' | 'Obesidad' | 'Colesterol alto';
export type Intensidad = 'Baja' | 'Media' | 'Alta';
export type TipoEjercicio = 'POR_ENFERMEDAD' | 'POR_PACIENTES';

export interface Ejercicio {
  id: number;
  nombre: string;
  descripcion?: string;
  duracion: number; // en segundos
  intensidad: Intensidad;
  series: number;
  descansoEntreSeries: number;
  enfermedad?: Enfermedad;
  tipo?: TipoEjercicio;
  urlImagen?: string;
  urlVideo?: string;
  createdAt?: string;
}

export interface CreateEjercicioRequest {
  nombre: string;
  descripcion?: string;
  duracion: number;
  intensidad: Intensidad;
  series: number;
  descansoEntreSeries: number;
  enfermedad?: Enfermedad;
  tipo?: TipoEjercicio;
  urlImagen?: string;
  urlVideo?: string;
}

export interface PaginationInfo {
  total: number;
  pagina: number;
  limit: number;
  totalPaginas: number;
}

export interface PaginatedEjerciciosResponse {
  ejercicios: Ejercicio[];
  paginacion: PaginationInfo;
}

export const ejercicioService = {
  // Listar ejercicios con paginación (doctor: catálogo, paciente: sus ejercicios realizados)
  async listarEjercicios(params?: {
    page?: number;
    limit?: number;
    enfermedad?: Enfermedad;
    intensidad?: Intensidad;
  }): Promise<PaginatedEjerciciosResponse> {
    const response = await api.get<PaginatedEjerciciosResponse>('/api/ejercicios', { 
      params: {
        page: params?.page || 1,
        limit: params?.limit || 10,
        ...(params?.enfermedad && { enfermedad: params.enfermedad }),
        ...(params?.intensidad && { intensidad: params.intensidad })
      }
    });
    return response.data;
  },

  // Crear ejercicio en catálogo (solo doctores)
  async crearEjercicio(data: CreateEjercicioRequest): Promise<Ejercicio> {
    const response = await api.post<Ejercicio>('/api/ejercicios', data);
    return response.data;
  },

  // Eliminar ejercicio (solo doctores)
  async eliminarEjercicio(id: number): Promise<void> {
    await api.delete(`/api/ejercicios/${id}`);
  },

  // Actualizar ejercicio (solo doctores)
  async actualizarEjercicio(id: number, data: Partial<CreateEjercicioRequest>): Promise<Ejercicio> {
    const response = await api.patch<Ejercicio>(`/api/ejercicios/${id}`, data);
    return response.data;
  }
};
