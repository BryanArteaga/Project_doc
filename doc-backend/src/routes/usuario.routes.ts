import { Router } from 'express';
import { getProfile, updateProfile, createUsuario } from '../controllers/usuario.controller';
import { authenticateToken } from '../middleware/auth.middleware';
const router = Router();

// ⚠️ RUTA PÚBLICA (DESARROLLO SOLAMENTE)
router.post('/create', createUsuario);

// Rutas protegidas
router.use(authenticateToken);
router.get('/profile', getProfile);
router.put('/profile', updateProfile);

export default router;
