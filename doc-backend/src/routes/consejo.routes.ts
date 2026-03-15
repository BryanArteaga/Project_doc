import { Router } from 'express';
import {
  getConsejos,
  getConsejo,
  createConsejo,
  updateConsejo,
  deleteConsejo,
} from '../controllers/consejo.controller';
import { verifyAnyUser, requireDoctor } from '../middleware/auth.middleware';

const router = Router();

// GET - Catálogo de consejos (permite tanto doctor como paciente)
router.get('/', verifyAnyUser, getConsejos);

// POST - Crear consejo en catálogo (solo doctores)
router.post('/', requireDoctor, createConsejo);

// GET /:id - Obtener consejo específico (solo doctores)
router.get('/:id', requireDoctor, getConsejo);

// PATCH /:id - Actualizar consejo (solo doctores)
router.patch('/:id', requireDoctor, updateConsejo);

// DELETE /:id - Eliminar consejo (solo doctores)
router.delete('/:id', requireDoctor, deleteConsejo);

export default router;
