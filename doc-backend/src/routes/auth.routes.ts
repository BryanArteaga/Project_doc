import { Router } from 'express';
import { register, login, checkAccountStatus } from '../controllers/auth.controller';
import { authenticateToken } from '../middleware/auth.middleware';
const router = Router();
router.post('/register', register);
router.post('/login', login);
router.get('/status', authenticateToken, checkAccountStatus);
export default router;
