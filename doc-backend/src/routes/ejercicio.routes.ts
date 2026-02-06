import { Router } from 'express';
import {
  getEjercicios,
  // getEjercicio, // TODO: implementar
  createEjercicio,
  // getAsignaciones, // TODO: implementar
  // getAsignacion, // TODO: implementar
  // asignarEjercicio, // TODO: implementar
  // updateAsignacion, // TODO: implementar
  // completarEjercicio, // TODO: implementar
} from '../controllers/ejercicio.controller';
import { verifyToken } from '../middleware/auth.middleware';

const router = Router();

// Todas las rutas requieren autenticación
router.use(verifyToken);

// Catálogo de ejercicios (público para todos)
router.get('/', getEjercicios);
// router.get('/:id', getEjercicio); // TODO
// router.post('/', createEjercicio); // TODO: solo doctores
// router.get('/asignaciones', getAsignaciones); // TODO
// router.get('/asignaciones/:id', getAsignacion); // TODO
// router.post('/asignaciones', asignarEjercicio); // TODO: solo doctores
// router.patch('/asignaciones/:id', updateAsignacion); // TODO: solo doctores
// router.post('/completar', completarEjercicio); // TODO: solo pacientes

export default router;

