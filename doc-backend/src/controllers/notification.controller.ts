import { Request, Response } from 'express';
import { prisma } from '../lib/prisma';
import { z } from 'zod';

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
