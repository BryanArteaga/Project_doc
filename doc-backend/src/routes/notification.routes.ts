import { Router } from 'express';
import {
  getNotifications,
  getUnreadNotifications,
  getNotification,
  markAsRead,
  markAllAsRead,
  deleteNotification,
} from '../controllers/notification.controller';
import { verifyToken } from '../middleware/auth.middleware';

const router = Router();

// Todas las rutas requieren autenticación
router.use(verifyToken);

// Rutas de notificaciones
router.get('/', getNotifications);
router.get('/unread', getUnreadNotifications);
router.get('/:id', getNotification);
router.patch('/:id/read', markAsRead);
router.patch('/read-all', markAllAsRead);
router.delete('/:id', deleteNotification);

export default router;
