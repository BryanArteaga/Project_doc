import { Response } from 'express';
import { prisma } from '../lib/prisma';
import { AuthRequest, EjercicioRealizadoDTO } from '../types';
export const createEjercicio = async (req: AuthRequest, res: Response) => {
  try {
    const data: EjercicioRealizadoDTO = req.body;
    const ejercicio = await prisma.ejercicioRealizado.create({
      data: {
        usuarioId: req.userId!,
        ...data
      }
    });
    res.status(201).json(ejercicio);
  } catch (error) {
    console.error('Error al crear ejercicio:', error);
    res.status(500).json({ error: 'Error al registrar ejercicio' });
  }
};
export const getEjercicios = async (req: AuthRequest, res: Response) => {
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
    const ejercicios = await prisma.ejercicioRealizado.findMany({
      where,
      orderBy: { fecha: 'desc' },
      take: parseInt(limit as string)
    });
    res.json(ejercicios);
  } catch (error) {
    console.error('Error al obtener ejercicios:', error);
    res.status(500).json({ error: 'Error al obtener ejercicios' });
  }
};
