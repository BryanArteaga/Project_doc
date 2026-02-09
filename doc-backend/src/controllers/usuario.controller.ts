import { Response, Request } from 'express';
import { prisma } from '../lib/prisma';
import { AuthRequest } from '../types';

/**
 * POST /api/usuarios/create
 * Crear usuario (DESARROLLO: sin autenticación)
 * Body: { nombre: string, apellido: string, estadoCuenta?: "ACTIVE" | "INACTIVE" }
 */
export const createUsuario = async (req: Request, res: Response) => {
  try {
    const { nombre, apellido, estadoCuenta = 'ACTIVE' } = req.body;
    
    if (!nombre || !apellido) {
      return res.status(400).json({ error: 'nombre y apellido requeridos' });
    }

    const usuario = await prisma.usuario.create({
      data: {
        nombre,
        apellido,
        estadoCuenta
      }
    });

    console.log(`✅ Usuario creado: ${usuario.nombre} ${usuario.apellido} (ID: ${usuario.id})`);
    res.status(201).json({
      message: 'Usuario creado exitosamente',
      usuario
    });
  } catch (error: any) {
    console.error('Error al crear usuario:', error);
    res.status(500).json({ error: 'Error al crear usuario', details: error.message });
  }
};

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
