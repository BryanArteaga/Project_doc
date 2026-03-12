import { Router } from 'express';
import { getProfile, updateProfile, createUsuario, searchByCedula } from '../controllers/usuario.controller';
import { getMisAsignacionesEjercicios } from '../controllers/patient.controller';
import { authenticateToken } from '../middleware/auth.middleware';

const router = Router();

// ⚠️ RUTAS PÚBLICAS (DESARROLLO SOLAMENTE)
router.post('/create', createUsuario);
router.put('/profile', updateProfile);  // Sin autenticación para que el app móvil pueda actualizar sin JWT

// 🔍 Búsqueda por cédula (para sincronización offline-first y recuperación de cuenta)
router.get('/by-cedula/:cedula', searchByCedula);

// Rutas protegidas
router.use(authenticateToken);
router.get('/profile', getProfile);

// ✅ Obtener mis asignaciones de ejercicios
router.get('/asignaciones/ejercicios', getMisAsignacionesEjercicios);

export default router;
