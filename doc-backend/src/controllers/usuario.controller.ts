import { Response } from 'express';
import { prisma } from '../lib/prisma';
import { AuthRequest } from '../types';
export const getProfile = async (req: AuthRequest, res: Response) => {
  try {
    const usuario = await prisma.usuario.findUnique({
      where: { id: req.userId }
    });
    if (!usuario) {
      return res.status(404).json({ error: 'Usuario no encontrado' });
    }
    res.json(usuario);
  } catch (error) {
    console.error('Error al obtener perfil:', error);
    res.status(500).json({ error: 'Error al obtener perfil' });
  }
};
export const updateProfile = async (req: AuthRequest, res: Response) => {
  try {
    const { peso, notifEnabled, notifType, notifDetails } = req.body;
    const updatedUser = await prisma.usuario.update({
      where: { id: req.userId },
      data: {
        ...(peso !== undefined && { peso }),
        ...(notifEnabled !== undefined && { notifEnabled }),
        ...(notifType !== undefined && { notifType }),
        ...(notifDetails !== undefined && { notifDetails })
      }
    });
    res.json({
      message: 'Perfil actualizado exitosamente',
      usuario: updatedUser
    });
  } catch (error) {
    console.error('Error al actualizar perfil:', error);
    res.status(500).json({ error: 'Error al actualizar perfil' });
  }
};
