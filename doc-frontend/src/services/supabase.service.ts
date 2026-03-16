import { createClient } from '@supabase/supabase-js';

/**
 * Cliente de Supabase para el frontend
 * Usado para upload/delete de imágenes en Storage
 */
const VITE_SUPABASE_URL = import.meta.env.VITE_SUPABASE_URL;
const VITE_SUPABASE_ANON_KEY = import.meta.env.VITE_SUPABASE_ANON_KEY;

if (!VITE_SUPABASE_URL || !VITE_SUPABASE_ANON_KEY) {
  console.warn('⚠️ Faltan credenciales de Supabase. Variables requeridas:');
  console.warn('  - VITE_SUPABASE_URL');
  console.warn('  - VITE_SUPABASE_ANON_KEY');
}

export const supabase = createClient(
  VITE_SUPABASE_URL || '',
  VITE_SUPABASE_ANON_KEY || ''
);

const STORAGE_BUCKET = 'ejercicios';
const EJERCICIO_FOLDER = 'ejercicio';

/**
 * Servicio para manejar imágenes en Supabase Storage
 */
export const imagenService = {
  /**
   * Subir imagen de ejercicio
   * @param file - Archivo de imagen
   * @param ejercicioId - ID del ejercicio (para nombrar el archivo)
   * @returns URL pública de la imagen
   */
  async subirImagenEjercicio(file: File, ejercicioId?: number): Promise<string> {
    try {
      if (!file) {
        throw new Error('No file provided');
      }

      // Validar que sea imagen
      if (!file.type.startsWith('image/')) {
        throw new Error('El archivo debe ser una imagen');
      }

      // Crear nombre único: ejercicio_[timestamp]_[random]
      const timestamp = Date.now();
      const random = Math.random().toString(36).substring(7);
      const extension = file.name.split('.').pop() || 'jpg';
      const nombreArchivo = `${EJERCICIO_FOLDER}/ejercicio_${timestamp}_${random}.${extension}`;

      console.log(`📤 Subiendo imagen a Supabase: ${nombreArchivo}`);

      const { data, error } = await supabase.storage
        .from(STORAGE_BUCKET)
        .upload(nombreArchivo, file, {
          cacheControl: '3600',
          upsert: false
        });

      if (error) {
        console.error('❌ Error al subir imagen:', error);
        throw error;
      }

      // Obtener URL pública
      const { data: publicUrlData } = supabase.storage
        .from(STORAGE_BUCKET)
        .getPublicUrl(data.path);

      const urlPublica = publicUrlData.publicUrl;
      console.log('✅ Imagen subida exitosamente:', urlPublica);

      return urlPublica;
    } catch (error: any) {
      console.error('❌ Error en subirImagenEjercicio:', error.message);
      throw error;
    }
  },

  /**
   * Borrar imagen por URL pública
   * @param url - URL pública de la imagen
   */
  async borrarImagenPorUrl(url: string): Promise<void> {
    try {
      if (!url) {
        console.warn('⚠️ URL vacía, no se puede borrar');
        return;
      }

      // Extraer el path del bucket desde la URL
      // URL formato: https://xxx.com/storage/v1/object/public/ejercicios/...
      const match = url.match(new RegExp(`${STORAGE_BUCKET}/(.+)$`));
      if (!match) {
        console.warn('⚠️ No se pudo extraer el path de la URL:', url);
        return;
      }

      const path = match[1];
      console.log(`🗑️ Borrando imagen: ${path}`);

      const { error } = await supabase.storage
        .from(STORAGE_BUCKET)
        .remove([path]);

      if (error) {
        console.error('❌ Error al borrar imagen:', error);
        throw error;
      }

      console.log('✅ Imagen borrada exitosamente');
    } catch (error: any) {
      console.error('❌ Error en borrarImagenPorUrl:', error.message);
      throw error;
    }
  },

  /**
   * Actualizar imagen: borra la antigua y sube la nueva
   * @param archivoNuevo - Nuevo archivo de imagen
   * @param urlAntigua - URL de la imagen anterior (si existe)
   * @returns URL de la nueva imagen
   */
  async actualizarImagen(archivoNuevo: File, urlAntigua?: string): Promise<string> {
    try {
      // Si hay imagen anterior, borrarla primero
      if (urlAntigua) {
        await this.borrarImagenPorUrl(urlAntigua);
      }

      // Subir nueva imagen
      const urlNueva = await this.subirImagenEjercicio(archivoNuevo);
      return urlNueva;
    } catch (error: any) {
      console.error('❌ Error en actualizarImagen:', error.message);
      throw error;
    }
  }
};
