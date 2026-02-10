import { Request, Response } from 'express';
import { prisma } from '../lib/prisma';
import { z } from 'zod';
import admin from 'firebase-admin';

const PaginationSchema = z.object({
  page: z.string().optional().default('1').pipe(z.coerce.number().min(1)),
  limit: z.string().optional().default('20').pipe(z.coerce.number().min(1).max(100)),
});

/**
 * Obtener notificaciones del usuario
 */
export const getNotifications = async (req: Request, res: Response) => {
  try {
    if (!req.userId) {
      return res.status(401).json({ error: 'No autenticado' });
    }

    const { page, limit } = PaginationSchema.parse(req.query);
    const skip = (page - 1) * limit;

    const [notifications, total] = await Promise.all([
      prisma.notificacion.findMany({
        where: { usuarioId: req.userId },
        orderBy: { creadoEn: 'desc' },
        skip,
        take: limit,
      }),
      prisma.notificacion.count({
        where: { usuarioId: req.userId },
      }),
    ]);

    res.json({
      notifications,
      pagination: {
        page,
        limit,
        total,
        pages: Math.ceil(total / limit),
      },
    });
  } catch (error: any) {
    console.error('Error al obtener notificaciones:', error);
    res.status(500).json({ error: error.mensaje || 'Error al obtener notificaciones' });
  }
};

/**
 * Obtener notificaciones no leídas
 */
export const getUnreadNotifications = async (req: Request, res: Response) => {
  try {
    if (!req.userId) {
      return res.status(401).json({ error: 'No autenticado' });
    }

    const notifications = await prisma.notificacion.findMany({
      where: {
        usuarioId: req.userId,
        leido: false,
      },
      orderBy: { creadoEn: 'desc' },
    });

    res.json({
      unreadCount: notifications.length,
      notifications,
    });
  } catch (error: any) {
    console.error('Error al obtener notificaciones no leídas:', error);
    res.status(500).json({ error: error.mensaje || 'Error al obtener notificaciones' });
  }
};

/**
 * Obtener una notificación por ID
 */
export const getNotification = async (req: Request, res: Response) => {
  try {
    if (!req.userId) {
      return res.status(401).json({ error: 'No autenticado' });
    }

    const id = req.params.id as string;
    // @ts-ignore - req.params siempre contiene strings
    const notificationId = parseInt(id);

    const notification = await prisma.notificacion.findUnique({
      where: { id: notificationId },
    });

    if (!notification) {
      return res.status(404).json({ error: 'Notificación no encontrada' });
    }

    // Verificar permisos
    if (notification.usuarioId !== req.userId) {
      return res.status(403).json({ error: 'No tienes permiso para ver esta notificación' });
    }

    // Marcar como leída automáticamente
    if (!notification.leido) {
      await prisma.notificacion.update({
        where: { id: notificationId },
        data: {
          leido: true,
          leidoEn: new Date(),
        },
      });
    }

    res.json(notification);
  } catch (error: any) {
    console.error('Error al obtener notificación:', error);
    res.status(500).json({ error: error.mensaje || 'Error al obtener notificación' });
  }
};

/**
 * Marcar notificación como leída
 */
export const markAsRead = async (req: Request, res: Response) => {
  try {
    if (!req.userId) {
      return res.status(401).json({ error: 'No autenticado' });
    }

    const id = req.params.id as string;
    // @ts-ignore - req.params siempre contiene strings
    const notificationId = parseInt(id);

    const notification = await prisma.notificacion.findUnique({
      where: { id: notificationId },
    });

    if (!notification) {
      return res.status(404).json({ error: 'Notificación no encontrada' });
    }

    if (notification.usuarioId !== req.userId) {
      return res.status(403).json({ error: 'No tienes permiso para modificar esta notificación' });
    }

    const updated = await prisma.notificacion.update({
      where: { id: notificationId },
      data: {
        leido: true,
        leidoEn: new Date(),
      },
    });

    res.json({
      message: 'Notificación marcada como leída',
      notification: updated,
    });
  } catch (error: any) {
    console.error('Error al marcar como leída:', error);
    res.status(500).json({ error: error.mensaje || 'Error al marcar como leída' });
  }
};

/**
 * Marcar todas las notificaciones como leídas
 */
export const markAllAsRead = async (req: Request, res: Response) => {
  try {
    if (!req.userId) {
      return res.status(401).json({ error: 'No autenticado' });
    }

    const result = await prisma.notificacion.updateMany({
      where: {
        usuarioId: req.userId,
        leido: false,
      },
      data: {
        leido: true,
        leidoEn: new Date(),
      },
    });

    res.json({
      message: 'Todas las notificaciones marcadas como leídas',
      updated: result.count,
    });
  } catch (error: any) {
    console.error('Error al marcar todas como leídas:', error);
    res.status(500).json({ error: error.mensaje || 'Error al marcar como leído' });
  }
};

/**
 * Eliminar una notificación
 */
export const deleteNotification = async (req: Request, res: Response) => {
  try {
    if (!req.userId) {
      return res.status(401).json({ error: 'No autenticado' });
    }

    const id = req.params.id as string;
    // @ts-ignore - req.params siempre contiene strings
    const notificationId = parseInt(id);

    const notification = await prisma.notificacion.findUnique({
      where: { id: notificationId },
    });

    if (!notification) {
      return res.status(404).json({ error: 'Notificación no encontrada' });
    }

    if (notification.usuarioId !== req.userId) {
      return res.status(403).json({ error: 'No tienes permiso para eliminar esta notificación' });
    }

    await prisma.notificacion.delete({
      where: { id: notificationId },
    });

    res.json({ message: 'Notificación eliminada exitosamente' });
  } catch (error: any) {
    console.error('Error al eliminar notificación:', error);
    res.status(500).json({ error: error.mensaje || 'Error al eliminar notificación' });
  }
};

/**
 * Registrar/Actualizar token FCM del dispositivo
 */
export const registerFCMToken = async (req: Request, res: Response) => {
  try {
    if (!req.userId) {
      return res.status(401).json({ error: 'No autenticado' });
    }

    const { token, deviceId } = req.body;

    if (!token) {
      return res.status(400).json({ error: 'Token FCM requerido' });
    }

    // Buscar si ya existe un dispositivo con este token
    const existingDevice = await prisma.dispositivo.findFirst({
      where: {
        usuarioId: req.userId,
        fcmToken: token,
      },
    });

    if (existingDevice) {
      // Actualizar última vez visto
      await prisma.dispositivo.update({
        where: { id: existingDevice.id },
        data: { ultimaVezVisto: new Date() },
      });

      return res.json({
        message: 'Token actualizado',
        deviceId: existingDevice.id,
      });
    }

    // Crear nuevo registro de dispositivo
    const device = await prisma.dispositivo.create({
      data: {
        usuarioId: req.userId,
        fcmToken: token,
        deviceId: deviceId || `device_${Date.now()}`,
        activo: true,
      },
    });

    res.json({
      message: 'Token registrado exitosamente',
      deviceId: device.id,
    });
  } catch (error: any) {
    console.error('Error al registrar token FCM:', error);
    res.status(500).json({ error: error.message || 'Error al registrar token' });
  }
};

/**
 * Eliminar token FCM (logout del dispositivo)
 */
export const removeFCMToken = async (req: Request, res: Response) => {
  try {
    if (!req.userId) {
      return res.status(401).json({ error: 'No autenticado' });
    }

    const { token } = req.body;

    if (!token) {
      return res.status(400).json({ error: 'Token FCM requerido' });
    }

    await prisma.dispositivo.deleteMany({
      where: {
        usuarioId: req.userId,
        fcmToken: token,
      },
    });

    res.json({ message: 'Token eliminado exitosamente' });
  } catch (error: any) {
    console.error('Error al eliminar token FCM:', error);
    res.status(500).json({ error: error.message || 'Error al eliminar token' });
  }
};

/**
 * Enviar notificación push a un usuario específico
 */
export const sendPushToUser = async (
  usuarioId: number,
  title: string,
  body: string,
  data?: Record<string, string>
) => {
  try {
    // Obtener todos los dispositivos activos del usuario
    const devices = await prisma.dispositivo.findMany({
      where: {
        usuarioId,
        activo: true,
        fcmToken: {
          not: ""  // No vacío
        }
      },
    });

    if (devices.length === 0) {
      console.log(`No hay dispositivos activos para el usuario ${usuarioId}`);
      return { success: false, message: 'No hay dispositivos registrados' };
    }

    const tokens = devices.map((d: any) => d.fcmToken).filter(Boolean) as string[];

    // Mensaje de FCM
    const message: admin.messaging.MulticastMessage = {
      notification: {
        title,
        body,
      },
      data: data || {},
      tokens,
      android: {
        priority: 'high',
        notification: {
          sound: 'default',
          channelId: 'fcm_reminders',
        },
      },
    };

    // Enviar notificación
    const response = await admin.messaging().sendEachForMulticast(message);

    console.log(`Notificaciones enviadas: ${response.successCount}/${tokens.length}`);

    // Remover tokens inválidos
    if (response.failureCount > 0) {
      const failedTokens: string[] = [];
      response.responses.forEach((resp, idx) => {
        if (!resp.success) {
          failedTokens.push(tokens[idx]);
        }
      });

      await prisma.dispositivo.deleteMany({
        where: { fcmToken: { in: failedTokens } },
      });

      console.log(`Tokens inválidos eliminados: ${failedTokens.length}`);
    }

    return {
      success: true,
      successCount: response.successCount,
      failureCount: response.failureCount,
    };
  } catch (error: any) {
    console.error('Error al enviar push:', error);
    return { success: false, error: error.message };
  }
};

/**
 * Enviar notificación de cuenta activada
 */
export const sendAccountActivatedPush = async (req: Request, res: Response) => {
  try {
    const { userId } = req.params;
    const usuarioId = parseInt(userId);

    const usuario = await prisma.usuario.findUnique({
      where: { id: usuarioId },
    });

    if (!usuario) {
      return res.status(404).json({ error: 'Usuario no encontrado' });
    }

    const result = await sendPushToUser(
      usuarioId,
      '¡Cuenta Activada!',
      'Tu médico ha activado tu cuenta. Ya puedes usar todas las funciones de la app.',
      {
        type: 'account_activated',
        navigate_to: 'main',
      }
    );

    // También crear notificación en la base de datos
    await prisma.notificacion.create({
      data: {
        usuarioId,
        tipo: 'cuenta_activada',
        titulo: '¡Cuenta Activada!',
        mensaje: 'Tu médico ha activado tu cuenta. Ya puedes usar todas las funciones.',
        leido: false,
      },
    });

    res.json({
      message: 'Notificación enviada',
      result,
    });
  } catch (error: any) {
    console.error('Error al enviar notificación de activación:', error);
    res.status(500).json({ error: error.message });
  }
};
