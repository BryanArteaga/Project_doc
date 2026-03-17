import { Router } from 'express';
import {
  push,
  pull,
  registerFCMToken,
  getSyncStatus,
  checkUpdates,
  updateNotifications,
  updateSyncProfile,
} from '../controllers/sync.controller';

const router = Router();

// ⚠️ RUTAS PÚBLICAS: Sin autenticación JWT
// El usuarioId se envía en el body de la request
// En producción, estas rutas deben tener autenticación

// Rutas de sincronización PUSH/PULL
router.post('/push', push);                      // App envía datos al servidor
router.get('/pull', pull);                       // App recibe datos del servidor
router.get('/check-updates', checkUpdates);      // Verificar si hay cambios sin descargar datos

// Rutas de tokens y estado
router.post('/register-fcm-token', registerFCMToken);  // Registrar token de FCM
router.get('/status', getSyncStatus);                  // Obtener estado de sincronización

// 🔧 FIX: Nuevos endpoints para actualizar configuración
router.put('/update-notifications', updateNotifications);  // Actualizar configuración de notificaciones
router.put('/update-profile', updateSyncProfile);          // Actualizar perfil desde sync

export default router;
