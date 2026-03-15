import api from './api';

export type Enfermedad = 'Hipertensión' | 'Diabetes' | 'Obesidad' | 'Colesterol alto';

export interface Consejo {
  id: number;
  consejo: string;
  enfermedad: Enfermedad;
  creadoEn?: string;
  actualizadoEn?: string;
}

export interface CreateConsejoRequest {
  consejo: string;
  enfermedad: Enfermedad;
}

export interface PaginationInfo {
  total: number;
  pagina: number;
  limit: number;
  totalPaginas: number;
}

export interface PaginatedConsejosResponse {
  consejos: Consejo[];
  paginacion: PaginationInfo;
}

export const consejoService = {
  // Listar consejos con paginación (doctor: catálogo, paciente: por enfermedad)
  async listarConsejos(params?: {
    page?: number;
    limit?: number;
    enfermedad?: Enfermedad;
  }): Promise<PaginatedConsejosResponse> {
    const response = await api.get<PaginatedConsejosResponse>('/api/consejos', { 
      params: {
        page: params?.page || 1,
        limit: params?.limit || 10,
        ...(params?.enfermedad && { enfermedad: params.enfermedad })
      }
    });
    return response.data;
  },

  // Crear consejo en catálogo (solo doctores)
  async crearConsejo(data: CreateConsejoRequest): Promise<Consejo> {
    try {
      const response = await api.post<Consejo>('/api/consejos', data);
      return response.data;
    } catch (error: any) {
      // Si es error 409, significa consejo duplicado
      if (error.response?.status === 409) {
        const err = new Error(error.response.data.error || 'Consejo duplicado');
        (err as any).isDuplicate = true;
        throw err;
      }
      throw error;
    }
  },

  // Eliminar consejo (solo doctores)
  async eliminarConsejo(id: number): Promise<void> {
    await api.delete(`/api/consejos/${id}`);
  },

  // Actualizar consejo (solo doctores)
  async actualizarConsejo(id: number, data: Partial<CreateConsejoRequest>): Promise<Consejo> {
    try {
      const response = await api.patch<Consejo>(`/api/consejos/${id}`, data);
      return response.data;
    } catch (error: any) {
      if (error.response?.status === 409) {
        const err = new Error(error.response.data.error || 'Consejo duplicado');
        (err as any).isDuplicate = true;
        throw err;
      }
      throw error;
    }
  }
};
