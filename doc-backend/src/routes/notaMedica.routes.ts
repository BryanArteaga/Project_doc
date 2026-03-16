import { Router } from 'express';
import {
  createNotaMedica,
  getNotasPorPaciente,
  getNotaById,
  updateNotaMedica,
  deleteNotaMedica
} from '../controllers/notaMedica.controller';
import { requireDoctor } from '../middleware/auth.middleware';

const router = Router();

// POST - Crear nota médica (solo doctores)
router.post('/', requireDoctor, createNotaMedica);

// GET /paciente/:pacienteId - Obtener todas las notas de un paciente (solo doctores)
router.get('/paciente/:pacienteId', requireDoctor, getNotasPorPaciente);

// GET /:notaId - Obtener nota específica (solo doctores)
router.get('/:notaId', requireDoctor, getNotaById);

// PATCH /:notaId - Actualizar nota (solo el doctor que la creó)
router.patch('/:notaId', requireDoctor, updateNotaMedica);

// DELETE /:notaId - Eliminar nota (solo el doctor que la creó)
router.delete('/:notaId', requireDoctor, deleteNotaMedica);

export default router;
