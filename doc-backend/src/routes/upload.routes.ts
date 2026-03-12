import { Router } from 'express';
import multer from 'multer';
import { uploadEjercicioImagen, deleteEjercicioImagen } from '../controllers/upload.controller';
import { verifyDoctorToken } from '../middleware/auth.middleware';

const router = Router();

// Configurar multer para manejo de archivos en memoria
const upload = multer({
  storage: multer.memoryStorage(),
  limits: {
    fileSize: 5 * 1024 * 1024, // 5MB
  },
  fileFilter: (req, file, cb) => {
    // Permitir solo imágenes
    if (file.mimetype.startsWith('image/')) {
      cb(null, true);
    } else {
      cb(new Error('Solo se permiten archivos de imagen'));
    }
  }
});

/**
 * POST /api/upload/ejercicio-imagen
 * Subir imagen de ejercicio a Supabase Storage
 */
router.post('/ejercicio-imagen', verifyDoctorToken, upload.single('imagen'), uploadEjercicioImagen);

/**
 * DELETE /api/upload/ejercicio-imagen/:fileName
 * Eliminar imagen de ejercicio de Supabase Storage
 */
router.delete('/ejercicio-imagen/:fileName', verifyDoctorToken, deleteEjercicioImagen);

export default router;
