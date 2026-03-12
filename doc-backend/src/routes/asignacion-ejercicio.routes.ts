import { Router } from 'express';
import {
  crearAsignacion,
  listarAsignaciones,
  obtenerAsignacion,
  actualizarAsignacion,
  eliminarAsignacion,
  miasAsignacionesActivas,
  estadisticasAsignaciones
} from '../controllers/asignacion-ejercicio.controller';
import { requireDoctor, requireAuth } from '../middleware/auth.middleware';

const router = Router();

// ✅ RUTAS PARA DOCTORES (requieren autenticación de doctor)
router.use(requireDoctor);

// POST /api/asignaciones - Crear nueva asignación (general o individual)
router.post('/', crearAsignacion);

// GET /api/asignaciones - Listar asignaciones del doctor
router.get('/', listarAsignaciones);

// GET /api/asignaciones/stats - Estadísticas de asignaciones
router.get('/stats', estadisticasAsignaciones);

// GET /api/asignaciones/:id - Obtener una asignación específica
router.get('/:id', obtenerAsignacion);

// PATCH /api/asignaciones/:id - Actualizar asignación
router.patch('/:id', actualizarAsignacion);

// DELETE /api/asignaciones/:id - Eliminar asignación
router.delete('/:id', eliminarAsignacion);

// ✅ RUTA PARA PACIENTES (en ruta separada para no interferir)
// Usar GET /api/pacientes/mis-asignaciones en lugar de aquí

export default router;
