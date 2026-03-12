import api from './api';

interface UploadResponse {
  success: boolean;
  fileName: string;
  imageUrl: string;
  message: string;
}

export const uploadService = {
  /**
   * Subir imagen de ejercicio
   * POST /api/upload/ejercicio-imagen
   */
  async uploadEjercicioImagen(file: File): Promise<UploadResponse> {
    const formData = new FormData();
    formData.append('imagen', file);

    try {
      const response = await api.post<UploadResponse>('/api/upload/ejercicio-imagen', formData, {
        headers: {
          'Content-Type': 'multipart/form-data'
        }
      });
      return response.data;
    } catch (error: any) {
      console.error('Error al subir imagen:', error);
      throw error;
    }
  },

  /**
   * Eliminar imagen de ejercicio
   * DELETE /api/upload/ejercicio-imagen/:fileName
   */
  async deleteEjercicioImagen(fileName: string): Promise<void> {
    try {
      await api.delete(`/api/upload/ejercicio-imagen/${fileName}`);
    } catch (error: any) {
      console.error('Error al eliminar imagen:', error);
      throw error;
    }
  },

  /**
   * Extraer nombre del archivo de la URL pública
   */
  extractFileNameFromUrl(imageUrl: string): string | null {
    try {
      const url = new URL(imageUrl);
      const pathParts = url.pathname.split('/');
      return pathParts[pathParts.length - 1];
    } catch {
      return null;
    }
  }
};
