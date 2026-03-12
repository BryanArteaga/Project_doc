import { Router } from 'express';
import {
  getEjercicios,
  getEjercicio,
  createEjercicio,
  updateEjercicio,
  deleteEjercicio,
} from '../controllers/ejercicio.controller';
import { verifyAnyUser, requireDoctor } from '../middleware/auth.middleware';

const router = Router();

// GET - Catálogo de ejercicios (permite tanto doctor como paciente)
router.get('/', verifyAnyUser, getEjercicios);

// POST - Crear ejercicio en catálogo (solo doctores)
router.post('/', requireDoctor, createEjercicio);

// GET /:id - Obtener ejercicio específico (solo doctores)
router.get('/:id', requireDoctor, getEjercicio);

// PATCH /:id - Actualizar ejercicio (solo doctores)
router.patch('/:id', requireDoctor, updateEjercicio);

// DELETE /:id - Eliminar ejercicio (solo doctores)
router.delete('/:id', requireDoctor, deleteEjercicio);

export default router;

