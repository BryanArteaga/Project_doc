import { Response, Request } from 'express';
import { supabaseAdmin } from '../lib/supabase';
import { AuthRequest } from '../types';

/**
 * SUBIR IMAGEN A SUPABASE STORAGE
 * POST /api/upload/ejercicio-imagen
 */
export const uploadEjercicioImagen = async (req: AuthRequest, res: Response) => {
  try {
    // Verificar que es doctor
    if (!req.doctorDbId) {
      return res.status(401).json({ error: 'Solo doctores pueden subir imágenes' });
    }

    // Verificar que hay archivo
    if (!req.file) {
      return res.status(400).json({ error: 'No se envió archivo' });
    }

    // Validar tipo de archivo
    const allowedMimes = ['image/jpeg', 'image/png', 'image/webp', 'image/gif'];
    if (!allowedMimes.includes(req.file.mimetype)) {
      return res.status(400).json({ error: 'Tipo de archivo no permitido. Solo imágenes: JPEG, PNG, WebP, GIF' });
    }

    // Validar tamaño (máximo 5MB)
    const maxSize = 5 * 1024 * 1024; // 5MB
    if (req.file.size > maxSize) {
      return res.status(400).json({ error: 'Archivo demasiado grande. Máximo 5MB' });
    }

    // Generar nombre único para el archivo
    const timestamp = Date.now();
    const randomStr = Math.random().toString(36).substring(2, 8);
    const extension = req.file.mimetype.split('/')[1];
    const fileName = `ejercicio-${timestamp}-${randomStr}.${extension}`;

    // Subir archivo a Supabase Storage
    const { data, error } = await supabaseAdmin.storage
      .from('ejercicios')
      .upload(fileName, req.file.buffer, {
        contentType: req.file.mimetype,
        upsert: false
      });

    if (error) {
      console.error('❌ Error al subir archivo:', error);
      return res.status(500).json({ error: 'Error al subir imagen', details: error.message });
    }

    // Obtener URL pública
    const { data: publicUrlData } = supabaseAdmin.storage
      .from('ejercicios')
      .getPublicUrl(fileName);

    const publicUrl = publicUrlData?.publicUrl;

    if (!publicUrl) {
      return res.status(500).json({ error: 'Error al obtener URL pública' });
    }

    console.log('✅ [uploadEjercicioImagen] Imagen subida correctamente:', fileName);

    res.status(200).json({
      success: true,
      fileName,
      imageUrl: publicUrl,
      message: 'Imagen subida exitosamente'
    });
  } catch (error: any) {
    console.error('❌ Error en uploadEjercicioImagen:', error);
    res.status(500).json({ 
      error: 'Error al subir imagen', 
      details: error.message 
    });
  }
};

/**
 * ELIMINAR IMAGEN DE SUPABASE STORAGE
 * DELETE /api/upload/ejercicio-imagen/:fileName
 */
export const deleteEjercicioImagen = async (req: AuthRequest, res: Response) => {
  try {
    if (!req.doctorDbId) {
      return res.status(401).json({ error: 'Solo doctores pueden eliminar imágenes' });
    }

    const { fileName } = req.params;

    if (!fileName) {
      return res.status(400).json({ error: 'Nombre de archivo requerido' });
    }

    const { error } = await supabaseAdmin.storage
      .from('ejercicios')
      .remove([fileName]);

    if (error) {
      console.error('❌ Error al eliminar archivo:', error);
      return res.status(500).json({ error: 'Error al eliminar imagen' });
    }

    console.log('✅ [deleteEjercicioImagen] Imagen eliminada:', fileName);

    res.status(200).json({
      success: true,
      message: 'Imagen eliminada exitosamente'
    });
  } catch (error: any) {
    console.error('❌ Error en deleteEjercicioImagen:', error);
    res.status(500).json({ 
      error: 'Error al eliminar imagen', 
      details: error.message 
    });
  }
};
