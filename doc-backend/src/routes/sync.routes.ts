import { Router } from 'express';
import {
  push,
  pull,
  getSyncSummary,
  markNotificationsAsRead,
} from '../controllers/sync.controller';
import { verifyToken } from '../middleware/auth.middleware';

const router = Router();

// Todas las rutas requieren autenticación (pero no necesitan estar activas)
// porque la app necesita sincronizar incluso si la cuenta está inactiva
router.use(verifyToken);

// Rutas de sincronización
router.post('/push', push);
router.get('/pull', pull);
router.get('/summary', getSyncSummary);
router.post('/notifications/read', markNotificationsAsRead);

export default router;
