import api from './api';

export interface Doctor {
  id: number;
  nombre: string;
  apellido: string;
  especialidad?: string;
}

export interface Paciente {
  id: number;
  nombre: string;
  apellido: string;
}

export interface NotaMedica {
  id: number;
  pacienteId: number;
  doctorId: number;
  contenido: string;
  fecha: string;
  actualizadoEn: string;
  doctor?: Doctor;
  paciente?: Paciente;
}

export interface CreateNotaMedicaRequest {
  pacienteId: number;
  contenido: string;
}

export interface UpdateNotaMedicaRequest {
  contenido: string;
}

export const notaMedicaService = {
  /**
   * Crear una nueva nota médica
   */
  async crear(data: CreateNotaMedicaRequest): Promise<NotaMedica> {
    try {
      const response = await api.post<NotaMedica>('/api/notas-medicas', data);
      return response.data;
    } catch (error: any) {
      console.error('❌ Error al crear nota médica:', error);
      throw error;
    }
  },

  /**
   * Obtener todas las notas de un paciente
   */
  async obtenerPorPaciente(pacienteId: number): Promise<NotaMedica[]> {
    try {
      const response = await api.get<NotaMedica[]>(`/api/notas-medicas/paciente/${pacienteId}`);
      return response.data;
    } catch (error: any) {
      console.error('❌ Error al obtener notas:', error);
      throw error;
    }
  },

  /**
   * Obtener una nota específica
   */
  async obtenerPorId(notaId: number): Promise<NotaMedica> {
    try {
      const response = await api.get<NotaMedica>(`/api/notas-medicas/${notaId}`);
      return response.data;
    } catch (error: any) {
      console.error('❌ Error al obtener nota:', error);
      throw error;
    }
  },

  /**
   * Actualizar una nota médica
   */
  async actualizar(notaId: number, data: UpdateNotaMedicaRequest): Promise<NotaMedica> {
    try {
      const response = await api.patch<NotaMedica>(`/api/notas-medicas/${notaId}`, data);
      return response.data;
    } catch (error: any) {
      console.error('❌ Error al actualizar nota médica:', error);
      throw error;
    }
  },

  /**
   * Eliminar una nota médica
   */
  async eliminar(notaId: number): Promise<{ message: string }> {
    try {
      const response = await api.delete<{ message: string }>(`/api/notas-medicas/${notaId}`);
      return response.data;
    } catch (error: any) {
      console.error('❌ Error al eliminar nota médica:', error);
      throw error;
    }
  }
};
