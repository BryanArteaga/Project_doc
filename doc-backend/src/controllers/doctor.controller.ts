import { Request, Response } from 'express';
import { prisma } from '../lib/prisma';
import { AuthRequest } from '../types';

// Obtener todos los usuarios pendientes de activación
export const getPendingUsers = async (req: Request, res: Response) => {
  try {
    const pendingUsers = await prisma.usuario.findMany({
      where: {
        estadoCuenta: 'INACTIVE'
      },
      orderBy: {
        registradoEn: 'desc'
      },
      select: {
        id: true,
        cedula: true,
        nombre: true,
        edad: true,
        peso: true,
        altura: true,
        sexo: true,
        enfermedad: true,
        colorEnfermedad: true,
        registradoEn: true,
        glucosaInicial: true,
        colesterolInicial: true
      }
    });

    res.json(pendingUsers);
  } catch (error) {
    console.error('Error al obtener usuarios pendientes:', error);
    res.status(500).json({ error: 'Error al obtener usuarios pendientes' });
  }
};

// Obtener todos los usuarios activos
export const getActiveUsers = async (req: Request, res: Response) => {
  try {
    const activeUsers = await prisma.usuario.findMany({
      where: {
        estadoCuenta: 'ACTIVE'
      },
      orderBy: {
        activadoEn: 'desc'
      },
      include: {
        registrosSalud: {
          orderBy: { fecha: 'desc' },
          take: 5
        },
        ejerciciosRealizados: {
          orderBy: { fecha: 'desc' },
          take: 5
        }
      }
    });

    res.json(activeUsers);
  } catch (error) {
    console.error('Error al obtener usuarios activos:', error);
    res.status(500).json({ error: 'Error al obtener usuarios activos' });
  }
};

// Activar cuenta de usuario
export const activateUser = async (req: Request, res: Response) => {
  try {
    const { userId } = req.params;
    const { doctorCedula } = req.body;

    if (!doctorCedula) {
      return res.status(400).json({ error: 'Se requiere la cédula del doctor' });
    }

    const usuario = await prisma.usuario.findUnique({
      where: { id: parseInt(userId) }
    });

    if (!usuario) {
      return res.status(404).json({ error: 'Usuario no encontrado' });
    }

    if (usuario.estadoCuenta === 'ACTIVE') {
      return res.status(400).json({ error: 'El usuario ya está activo' });
    }

    // Activar usuario
    const updatedUser = await prisma.usuario.update({
      where: { id: parseInt(userId) },
      data: {
        estadoCuenta: 'ACTIVE',
        activadoEn: new Date(),
        activadoPor: doctorCedula
      }
    });

    res.json({
      message: 'Usuario activado exitosamente',
      usuario: {
        id: updatedUser.id,
        cedula: updatedUser.cedula,
        nombre: updatedUser.nombre,
        estadoCuenta: updatedUser.estadoCuenta,
        activadoEn: updatedUser.activadoEn,
        activadoPor: updatedUser.activadoPor
      }
    });

    // TODO: Aquí se debería enviar una notificación push al usuario
    // notificando que su cuenta fue activada
  } catch (error) {
    console.error('Error al activar usuario:', error);
    res.status(500).json({ error: 'Error al activar usuario' });
  }
};

// Desactivar cuenta de usuario
export const deactivateUser = async (req: Request, res: Response) => {
  try {
    const { userId } = req.params;
    const { doctorCedula } = req.body;

    if (!doctorCedula) {
      return res.status(400).json({ error: 'Se requiere la cédula del doctor' });
    }

    const updatedUser = await prisma.usuario.update({
      where: { id: parseInt(userId) },
      data: {
        estadoCuenta: 'INACTIVE'
      }
    });

    res.json({
      message: 'Usuario desactivado exitosamente',
      usuario: {
        id: updatedUser.id,
        cedula: updatedUser.cedula,
        nombre: updatedUser.nombre,
        estadoCuenta: updatedUser.estadoCuenta
      }
    });
  } catch (error) {
    console.error('Error al desactivar usuario:', error);
    res.status(500).json({ error: 'Error al desactivar usuario' });
  }
};

// Obtener estadísticas generales
export const getDashboardStats = async (req: Request, res: Response) => {
  try {
    const [totalUsers, pendingUsers, activeUsers, inactiveUsers] = await Promise.all([
      prisma.usuario.count(),
      prisma.usuario.count({ where: { estadoCuenta: 'INACTIVE' } }),
      prisma.usuario.count({ where: { estadoCuenta: 'ACTIVE' } }),
      prisma.usuario.count({ where: { estadoCuenta: 'INACTIVE' } })
    ]);

    const registrosHoy = await prisma.registroSalud.count({
      where: {
        fecha: new Date().toISOString().split('T')[0] // formato yyyy-MM-dd
      }
    });

    res.json({
      totalUsers,
      pendingUsers,
      activeUsers,
      inactiveUsers,
      registrosHoy
    });
  } catch (error) {
    console.error('Error al obtener estadísticas:', error);
    res.status(500).json({ error: 'Error al obtener estadísticas' });
  }
};

// Obtener detalles de un usuario específico
export const getUserDetails = async (req: Request, res: Response) => {
  try {
    const { userId } = req.params;

    const usuario = await prisma.usuario.findUnique({
      where: { id: parseInt(userId) },
      include: {
        registrosSalud: {
          orderBy: { fecha: 'desc' },
          take: 30
        },
        ejerciciosRealizados: {
          orderBy: { fecha: 'desc' },
          take: 30
        }
      }
    });

    if (!usuario) {
      return res.status(404).json({ error: 'Usuario no encontrado' });
    }

    res.json(usuario);
  } catch (error) {
    console.error('Error al obtener detalles de usuario:', error);
    res.status(500).json({ error: 'Error al obtener detalles de usuario' });
  }
};

