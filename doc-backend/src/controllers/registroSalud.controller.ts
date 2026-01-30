import { Response } from 'express';
import { PrismaClient } from '@prisma/client';
import { AuthRequest, RegistroSaludDTO } from '../types';
const prisma = new PrismaClient();
export const createRegistro = async (req: AuthRequest, res: Response) => {
  try {
    const data: RegistroSaludDTO = req.body;
    const registro = await prisma.registroSalud.create({
      data: {
        usuarioId: req.userId!,
        ...data
      }
    });
    res.status(201).json(registro);
  } catch (error) {
    console.error('Error al crear registro:', error);
    res.status(500).json({ error: 'Error al crear registro de salud' });
  }
};
export const getRegistros = async (req: AuthRequest, res: Response) => {
  try {
    const { limit = '30', desde, hasta } = req.query;
    const where: any = {
      usuarioId: req.userId
    };
    if (desde || hasta) {
      where.fecha = {};
      if (desde) where.fecha.gte = new Date(desde as string);
      if (hasta) where.fecha.lte = new Date(hasta as string);
    }
    const registros = await prisma.registroSalud.findMany({
      where,
      orderBy: { fecha: 'desc' },
      take: parseInt(limit as string)
    });
    res.json(registros);
  } catch (error) {
    console.error('Error al obtener registros:', error);
    res.status(500).json({ error: 'Error al obtener registros' });
  }
};
export const getRegistroById = async (req: AuthRequest, res: Response) => {
  try {
    const { id } = req.params;
    const registro = await prisma.registroSalud.findFirst({
      where: {
        id: parseInt(id),
        usuarioId: req.userId
      }
    });
    if (!registro) {
      return res.status(404).json({ error: 'Registro no encontrado' });
    }
    res.json(registro);
  } catch (error) {
    console.error('Error al obtener registro:', error);
    res.status(500).json({ error: 'Error al obtener registro' });
  }
};
export const updateRegistro = async (req: AuthRequest, res: Response) => {
  try {
    const { id } = req.params;
    const data: RegistroSaludDTO = req.body;
    const registro = await prisma.registroSalud.findFirst({
      where: {
        id: parseInt(id),
        usuarioId: req.userId
      }
    });
    if (!registro) {
      return res.status(404).json({ error: 'Registro no encontrado' });
    }
    const updated = await prisma.registroSalud.update({
      where: { id: parseInt(id) },
      data
    });
    res.json(updated);
  } catch (error) {
    console.error('Error al actualizar registro:', error);
    res.status(500).json({ error: 'Error al actualizar registro' });
  }
};
export const deleteRegistro = async (req: AuthRequest, res: Response) => {
  try {
    const { id } = req.params;
    const registro = await prisma.registroSalud.findFirst({
      where: {
        id: parseInt(id),
        usuarioId: req.userId
      }
    });
    if (!registro) {
      return res.status(404).json({ error: 'Registro no encontrado' });
    }
    await prisma.registroSalud.delete({
      where: { id: parseInt(id) }
    });
    res.json({ message: 'Registro eliminado exitosamente' });
  } catch (error) {
    console.error('Error al eliminar registro:', error);
    res.status(500).json({ error: 'Error al eliminar registro' });
  }
};
