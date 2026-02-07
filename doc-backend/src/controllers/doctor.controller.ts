import { Request, Response } from 'express';
import { prisma } from '../lib/prisma';
import { AuthRequest } from '../types';
import { cacheService } from '../utils/cache';

// Obtener todos los usuarios pendientes de activación
export const getPendingUsers = async (req: Request, res: Response) => {
  try {
    // Usar caché con TTL de 2 minutos
    const pendingUsers = await cacheService.get(
      'pending_users',
      async () => {
        return await prisma.usuario.findMany({
          where: {
            estadoCuenta: 'INACTIVE'
          },
          orderBy: {
            registradoEn: 'desc'
          },
          take: 50, // Limitar resultados
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
      },
      2 * 60 * 1000 // 2 minutos
    );

    res.json(pendingUsers);
  } catch (error) {
    console.error('Error al obtener usuarios pendientes:', error);
    res.status(500).json({ error: 'Error al obtener usuarios pendientes' });
  }
};

// Obtener todos los usuarios activos
export const getActiveUsers = async (req: Request, res: Response) => {
  try {
    const page = parseInt(req.query.page as string) || 1;
    const limit = parseInt(req.query.limit as string) || 20;
    const skip = (page - 1) * limit;

    // Usar caché con paginación
    const cacheKey = `active_users_page_${page}_limit_${limit}`;
    
    const activeUsers = await cacheService.get(
      cacheKey,
      async () => {
        return await prisma.usuario.findMany({
          where: {
            estadoCuenta: 'ACTIVE'
          },
          orderBy: {
            activadoEn: 'desc'
          },
          skip,
          take: limit,
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
      },
      2 * 60 * 1000 // 2 minutos
    );

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

    // Invalidar caché al activar usuario
    cacheService.invalidate('pending_users');
    cacheService.invalidatePattern('active_users_');

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
    const [totalPacientes, pacientesPendientes, pacientesActivos, pacientesInactivos] = await Promise.all([
      prisma.usuario.count(),
      prisma.usuario.count({ where: { estadoCuenta: 'INACTIVE' } }),
      prisma.usuario.count({ where: { estadoCuenta: 'ACTIVE' } }),
      prisma.usuario.count({ where: { estadoCuenta: 'SUSPENDED' } })
    ]);

    // Obtener registros de salud de hoy
    const hoy = new Date();
    hoy.setHours(0, 0, 0, 0);
    const manana = new Date(hoy);
    manana.setDate(manana.getDate() + 1);

    const registrosHoy = await prisma.registroSalud.count({
      where: {
        fecha: {
          gte: hoy.toISOString(),
          lt: manana.toISOString()
        }
      }
    });

    res.json({
      totalPacientes,
      pacientesPendientes,
      pacientesActivos,
      pacientesInactivos,
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

// Obtener todos los doctores (solo admin)
export const getAllDoctors = async (req: Request, res: Response) => {
  try {
    const doctors = await prisma.doctor.findMany({
      orderBy: {
        creadoEn: 'desc'
      },
      select: {
        id: true,
        email: true,
        nombre: true,
        apellido: true,
        cedula: true,
        telefono: true,
        especialidad: true,
        rol: true,
        activo: true,
        creadoEn: true
      }
    });

    res.json(doctors);
  } catch (error) {
    console.error('Error al obtener doctores:', error);
    res.status(500).json({ error: 'Error al obtener doctores' });
  }
};

// Actualizar doctor (solo admin)
export const updateDoctor = async (req: Request, res: Response) => {
  try {
    const { id } = req.params;
    const { nombre, apellido, especialidad, telefono, activo, rol } = req.body;

    const doctor = await prisma.doctor.findUnique({
      where: { id: parseInt(id) }
    });

    if (!doctor) {
      return res.status(404).json({ error: 'Doctor no encontrado' });
    }

    const updatedDoctor = await prisma.doctor.update({
      where: { id: parseInt(id) },
      data: {
        ...(nombre && { nombre }),
        ...(apellido && { apellido }),
        ...(especialidad !== undefined && { especialidad }),
        ...(telefono !== undefined && { telefono }),
        ...(activo !== undefined && { activo }),
        ...(rol && { rol })
      },
      select: {
        id: true,
        email: true,
        nombre: true,
        apellido: true,
        cedula: true,
        telefono: true,
        especialidad: true,
        rol: true,
        activo: true,
        creadoEn: true
      }
    });

    res.json({
      message: 'Doctor actualizado exitosamente',
      doctor: updatedDoctor
    });
  } catch (error) {
    console.error('Error al actualizar doctor:', error);
    res.status(500).json({ error: 'Error al actualizar doctor' });
  }
};

