import { Router } from 'express';
import { register, login, me, updateDeviceToken } from '../controllers/auth.controller';
import { verifyToken } from '../middleware/auth.middleware';

const router = Router();

// Rutas públicas
router.post('/register', register);
router.post('/login', login);

// Rutas protegidas
router.get('/me', verifyToken, me);
router.post('/device-token', verifyToken, updateDeviceToken);

export default router;
