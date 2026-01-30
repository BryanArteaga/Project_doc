import { Router } from 'express';
import { getProfile, updateProfile } from '../controllers/usuario.controller';
import { authenticateToken } from '../middleware/auth.middleware';
const router = Router();
router.use(authenticateToken);
router.get('/profile', getProfile);
router.put('/profile', updateProfile);
export default router;
