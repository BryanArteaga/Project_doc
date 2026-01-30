import { Router } from 'express';
import { createEjercicio, getEjercicios } from '../controllers/ejercicio.controller';
import { authenticateToken } from '../middleware/auth.middleware';
const router = Router();
router.use(authenticateToken);
router.post('/', createEjercicio);
router.get('/', getEjercicios);
export default router;
