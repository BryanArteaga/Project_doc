import { Request, Response } from 'express';
import { SyncService } from '../services/sync.service';
import { z } from 'zod';

// Schema de validación para PUSH
const PushSchema = z.object({
  registrosSalud: z.array(z.object({
    idSincronizacion: z.string(),
    presionSistolica: z.number().optional(),
    presionDiastolica: z.number().optional(),
    glucosa: z.number().optional(),
    colesterol: z.number().optional(),
    temperatura: z.number().optional(),
    peso: z.number().optional(),
    notas: z.string().optional(),
    registradoEn: z.string(),
  })).optional(),
  ejerciciosRealizados: z.array(z.object({
    idSincronizacion: z.string(),
    asignacionId: z.number(),
    ejercicioId: z.number(),
    duracionReal: z.number().optional(),
    dificultad: z.number().optional(),
    dolor: z.number().optional(),
    notas: z.string().optional(),
    completadoEn: z.string(),
  })).optional(),
});

/**
 * PUSH: Recibir datos locales del paciente
 */
export const push = async (req: Request, res: Response) => {
  try {
    if (!req.userId) {
      return res.status(401).json({ error: 'No autenticado' });
    }

    const data = PushSchema.parse(req.body);
    const result = await SyncService.pushLocalChanges(req.userId, data);

    res.json({
      message: 'Sincronización PUSH exitosa',
      result,
    });
  } catch (error: any) {
    console.error('Error en PUSH:', error);

    if (error instanceof z.ZodError) {
      return res.status(400).json({
        error: 'Validación fallida',
        details: error.errors,
      });
    }

    res.status(500).json({ error: error.mensaje || 'Error en sincronización' });
  }
};

/**
 * PULL: Enviar datos remotos nuevos al paciente
 */
export const pull = async (req: Request, res: Response) => {
  try {
    if (!req.userId) {
      return res.status(401).json({ error: 'No autenticado' });
    }

    const { lastSyncTime } = req.query;
    const result = await SyncService.pullRemoteChanges(
      req.userId,
      lastSyncTime as string | undefined
    );

    res.json({
      message: 'Sincronización PULL exitosa',
      data: result,
    });
  } catch (error: any) {
    console.error('Error en PULL:', error);
    res.status(500).json({ error: error.mensaje || 'Error en sincronización' });
  }
};

/**
 * Obtener resumen de sincronización
 */
export const getSyncSummary = async (req: Request, res: Response) => {
  try {
    if (!req.userId) {
      return res.status(401).json({ error: 'No autenticado' });
    }

    const summary = await SyncService.getSyncSummary(req.userId);
    res.json(summary);
  } catch (error: any) {
    console.error('Error al obtener resumen de sincronización:', error);
    res.status(500).json({ error: error.mensaje || 'Error al obtener resumen' });
  }
};

/**
 * Marcar notificaciones como leídas
 */
export const markNotificationsAsRead = async (req: Request, res: Response) => {
  try {
    if (!req.userId) {
      return res.status(401).json({ error: 'No autenticado' });
    }

    const { notificationIds } = req.body;

    if (!Array.isArray(notificationIds) || notificationIds.length === 0) {
      return res.status(400).json({ error: 'notificationIds debe ser un array no vacío' });
    }

    const result = await SyncService.markNotificationsAsRead(req.userId, notificationIds);
    res.json({
      message: 'Notificaciones marcadas como leídas',
      result,
    });
  } catch (error: any) {
    console.error('Error al marcar notificaciones:', error);
    res.status(500).json({ error: error.mensaje || 'Error al marcar notificaciones' });
  }
};
