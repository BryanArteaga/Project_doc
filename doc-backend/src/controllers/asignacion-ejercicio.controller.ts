import { Response, Request } from 'express';
import { prisma } from '../lib/prisma';
import { CreateAsignacionEjercicioDTO, UpdateAsignacionEjercicioDTO } from '../types';
import { z } from 'zod';

// ✅ CREAR asignación de ejercicio (general o individual)
export const crearAsignacion = async (req: Request, res: Response) => {
  try {
    const doctorId = req.doctorDbId;
    const data: CreateAsignacionEjercicioDTO = req.body;
    console.log('📝 [crearAsignacion] Iniciando - doctorId:', doctorId, 'data:', JSON.stringify(data));

    // Validación schema
    const schema = z.object({
      ejercicioId: z.number().min(1),
      usuarioId: z.number().optional().nullable(),     // null = general
      enfermedad: z.string().optional(),
      descripcion: z.string().optional(),
      fechaExpiracion: z.string().optional()
    });

    let validated;
    try {
      validated = schema.parse(data);
      console.log('✅ Validación exitosa:', JSON.stringify(validated));
    } catch (parseError: any) {
      console.error('❌ Error de validación Zod:', parseError.errors);
      return res.status(400).json({ 
        error: 'Datos inválidos',
        details: parseError.errors
      });
    }

    // Verificar que el ejercicio existe
    const ejercicio = await prisma.ejercicio.findUnique({
      where: { id: validated.ejercicioId }
    });

    if (!ejercicio) {
      return res.status(404).json({ error: 'Ejercicio no encontrado' });
    }

    // Si es asignación individual, verificar que usuario existe
    if (validated.usuarioId) {
      const usuario = await prisma.usuario.findUnique({
        where: { id: validated.usuarioId }
      });
      if (!usuario) {
        return res.status(404).json({ error: 'Usuario no encontrado' });
      }

      // ✅ VERIFICAR si esta asignación ya existe (duplicado)
      const asignacionExistente = await prisma.asignacionEjercicio.findUnique({
        where: {
          ejercicioId_doctorId_usuarioId: {
            ejercicioId: validated.ejercicioId,
            doctorId: doctorId!,
            usuarioId: validated.usuarioId
          }
        }
      });

      if (asignacionExistente) {
        return res.status(400).json({ 
          error: 'Este paciente ya tiene asignado este ejercicio',
          skipToast: true
        });
      }
    }

    // Crear asignación
    const asignacion = await prisma.asignacionEjercicio.create({
      data: {
        ejercicioId: validated.ejercicioId,
        doctorId: doctorId!,
        usuarioId: validated.usuarioId || null,           // null = general
        enfermedad: validated.enfermedad,
        descripcion: validated.descripcion,
        fechaExpiracion: validated.fechaExpiracion ? new Date(validated.fechaExpiracion) : null,
        activo: true
      },
      include: {
        ejercicio: {
          select: {
            id: true,
            nombre: true,
            descripcion: true,
            duracion: true,
            series: true,
            intensidad: true,
            urlImagen: true,
            urlVideo: true
          }
        }
      }
    });

    res.status(201).json(asignacion);
  } catch (error: any) {
    console.error('❌ Error al crear asignación:', error.message, error.code);
    if (error.code === 'P2002') {
      return res.status(400).json({ 
        error: 'Esta asignación ya existe',
        skipToast: true
      });
    }
    res.status(500).json({ error: 'Error al crear asignación', details: error.message });
  }
};

// ✅ LISTAR asignaciones del doctor
export const listarAsignaciones = async (req: Request, res: Response) => {
  try {
    const doctorId = req.doctorDbId;
    console.log('📋 [listarAsignaciones] Iniciando - doctorId:', doctorId, 'req.doctorDbId:', req.doctorDbId);
    
    if (!doctorId) {
      console.error('❌ [listarAsignaciones] doctorId es undefined/null!');
      return res.status(401).json({ error: 'No autenticado - doctorId no encontrado' });
    }
    
    const { usuarioId, enfermedad, activo, page = '1', limit = '20' } = req.query;

    const where: any = {
      doctorId: doctorId!
    };

    if (usuarioId) {
      where.usuarioId = parseInt(usuarioId as string);
    }

    if (enfermedad) {
      where.enfermedad = { contains: enfermedad as string, mode: 'insensitive' };
    }

    if (activo !== undefined) {
      where.activo = activo === 'true';
    }

    const skip = (parseInt(page as string) - 1) * parseInt(limit as string);

    console.log('🔍 [listarAsignaciones] Query con where:', JSON.stringify(where));

    const [asignaciones, total] = await Promise.all([
      prisma.asignacionEjercicio.findMany({
        where,
        include: {
          ejercicio: {
            select: {
              id: true,
              nombre: true,
              descripcion: true,
              duracion: true,
              series: true,
              intensidad: true,
              urlImagen: true,
              urlVideo: true
            }
          },
          usuario: {
            select: {
              id: true,
              nombre: true,
              apellido: true,
              cedula: true,
              enfermedad: true
            }
          }
        },
        orderBy: { fechaAsignacion: 'desc' },
        skip,
        take: parseInt(limit as string)
      }),
      prisma.asignacionEjercicio.count({ where })
    ]);

    console.log('✅ [listarAsignaciones] Resultados encontrados:', asignaciones.length);

    res.json({
      asignaciones,
      paginacion: {
        total,
        pagina: parseInt(page as string),
        limite: parseInt(limit as string),
        totalPaginas: Math.ceil(total / parseInt(limit as string))
      }
    });
  } catch (error: any) {
    console.error('❌ Error al listar asignaciones:', error.message, error.code);
    console.error('Stack:', error.stack);
    res.status(500).json({ error: 'Error al listar asignaciones', details: error.message });
  }
};

// ✅ OBTENER una asignación específica
export const obtenerAsignacion = async (req: Request, res: Response) => {
  try {
    const id = (req.params.id as string);
    const doctorId = req.doctorDbId;

    const asignacion = await prisma.asignacionEjercicio.findUnique({
      where: { id: parseInt(id) },
      include: {
        ejercicio: true,
        usuario: {
          select: {
            id: true,
            nombre: true,
            apellido: true,
            cedula: true,
            enfermedad: true,
            edad: true,
            peso: true
          }
        }
      }
    });

    if (!asignacion) {
      return res.status(404).json({ error: 'Asignación no encontrada' });
    }

    // Verificar que pertenece al doctor
    if (asignacion.doctorId !== doctorId) {
      return res.status(403).json({ error: 'No tienes permisos para ver esta asignación' });
    }

    res.json(asignacion);
  } catch (error) {
    console.error('Error al obtener asignación:', error);
    res.status(500).json({ error: 'Error al obtener asignación' });
  }
};

// ✅ ACTUALIZAR asignación
export const actualizarAsignacion = async (req: Request, res: Response) => {
  try {
    const id = (req.params.id as string);
    const doctorId = req.doctorDbId;
    const data: UpdateAsignacionEjercicioDTO = req.body;

    const schema = z.object({
      descripcion: z.string().optional(),
      fechaExpiracion: z.string().optional().nullable(),
      activo: z.boolean().optional()
    });

    const validated = schema.parse(data);

    // Verificar permisos
    const asignacion = await prisma.asignacionEjercicio.findUnique({
      where: { id: parseInt(id) }
    });

    if (!asignacion) {
      return res.status(404).json({ error: 'Asignación no encontrada' });
    }

    if (asignacion.doctorId !== doctorId) {
      return res.status(403).json({ error: 'No tienes permisos para actualizar esta asignación' });
    }

    // Actualizar
    const actualizada = await prisma.asignacionEjercicio.update({
      where: { id: parseInt(id) },
      data: {
        descripcion: validated.descripcion,
        fechaExpiracion: validated.fechaExpiracion ? new Date(validated.fechaExpiracion) : validated.fechaExpiracion === null ? null : undefined,
        activo: validated.activo,
        actualizadoEn: new Date()
      },
      include: {
        ejercicio: true,
        usuario: true
      }
    });

    res.json(actualizada);
  } catch (error) {
    console.error('Error al actualizar asignación:', error);
    res.status(500).json({ error: 'Error al actualizar asignación' });
  }
};

// ✅ ELIMINAR asignación
export const eliminarAsignacion = async (req: Request, res: Response) => {
  try {
    const id = (req.params.id as string);
    const doctorId = req.doctorDbId;

    // Verificar permisos
    const asignacion = await prisma.asignacionEjercicio.findUnique({
      where: { id: parseInt(id) }
    });

    if (!asignacion) {
      return res.status(404).json({ error: 'Asignación no encontrada' });
    }

    if (asignacion.doctorId !== doctorId) {
      return res.status(403).json({ error: 'No tienes permisos para eliminar esta asignación' });
    }

    // Eliminar
    await prisma.asignacionEjercicio.delete({
      where: { id: parseInt(id) }
    });

    res.json({ mensaje: 'Asignación eliminada correctamente' });
  } catch (error) {
    console.error('Error al eliminar asignación:', error);
    res.status(500).json({ error: 'Error al eliminar asignación' });
  }
};

// ✅ ELIMINAR asignación por ejercicio y paciente
export const eliminarAsignacionPorEjercicioPaciente = async (req: Request, res: Response) => {
  try {
    const ejercicioId = (req.params.ejercicioId as string);
    const pacienteId = (req.params.pacienteId as string);
    const doctorId = req.doctorDbId;

    console.log('🗑️ [eliminarAsignacionPorEjercicioPaciente] doctorId:', doctorId, 'ejercicioId:', ejercicioId, 'pacienteId:', pacienteId);

    // Buscar la asignación con estos parámetros
    const asignacion = await prisma.asignacionEjercicio.findUnique({
      where: {
        ejercicioId_doctorId_usuarioId: {
          ejercicioId: parseInt(ejercicioId),
          doctorId: doctorId!,
          usuarioId: parseInt(pacienteId)
        }
      }
    });

    if (!asignacion) {
      console.log('❌ Asignación no encontrada');
      return res.status(404).json({ error: 'Asignación no encontrada' });
    }

    if (asignacion.doctorId !== doctorId) {
      console.log('❌ Permisos insuficientes. doctorId en DB:', asignacion.doctorId, 'doctorId del request:', doctorId);
      return res.status(403).json({ error: 'No tienes permisos para eliminar esta asignación' });
    }

    // Eliminar
    await prisma.asignacionEjercicio.delete({
      where: {
        ejercicioId_doctorId_usuarioId: {
          ejercicioId: parseInt(ejercicioId),
          doctorId: doctorId!,
          usuarioId: parseInt(pacienteId)
        }
      }
    });

    console.log('✅ Asignación eliminada correctamente');
    res.json({ mensaje: 'Asignación eliminada correctamente' });
  } catch (error) {
    console.error('Error al eliminar asignación por ejercicio/paciente:', error);
    res.status(500).json({ error: 'Error al eliminar asignación' });
  }
};

// ✅ OBTENER mis asignaciones (para pacientes - solo activas y no expiradas)
export const miasAsignacionesActivas = async (req: Request, res: Response) => {
  try {
    const usuarioId = req.userId;
    const ahora = new Date();

    // Obtener asignaciones:
    // 1. Directas (usuarioId = mi ID)
    // 2. Generales por mi enfermedad (usuarioId = null Y mi enfermedad coincide)
    const usuario = await prisma.usuario.findUnique({
      where: { id: usuarioId! },
      select: { enfermedad: true }
    });

    if (!usuario) {
      return res.status(404).json({ error: 'Usuario no encontrado' });
    }

    const asignaciones = await prisma.asignacionEjercicio.findMany({
      where: {
        activo: true,
        AND: [
          // Condición 1: Asignación directa O general por enfermedad
          {
            OR: [
              { usuarioId: usuarioId! },                        // Asignación directa
              { usuarioId: null, enfermedad: usuario.enfermedad }  // Asignación general por enfermedad
            ]
          },
          // Condición 2: No expirada
          {
            OR: [
              { fechaExpiracion: null },                        // Sin expiración
              { fechaExpiracion: { gt: ahora } }                // No expirada
            ]
          }
        ]
      },
      include: {
        ejercicio: true,
        doctor: {
          select: {
            id: true,
            nombre: true,
            apellido: true,
            especialidad: true
          }
        }
      },
      orderBy: { fechaAsignacion: 'desc' }
    });

    res.json(asignaciones);
  } catch (error) {
    console.error('Error al obtener asignaciones del paciente:', error);
    res.status(500).json({ error: 'Error al obtener asignaciones' });
  }
};

// ✅ ESTADÍSTICAS de asignaciones
export const estadisticasAsignaciones = async (req: Request, res: Response) => {
  try {
    const doctorId = req.doctorDbId;

    const stats = await prisma.asignacionEjercicio.groupBy({
      by: ['activo'],
      where: { doctorId: doctorId! },
      _count: true
    });

    const generales = await prisma.asignacionEjercicio.count({
      where: {
        doctorId: doctorId!,
        usuarioId: null
      }
    });

    const individuales = await prisma.asignacionEjercicio.count({
      where: {
        doctorId: doctorId!,
        usuarioId: { not: null }
      }
    });

    const expiradas = await prisma.asignacionEjercicio.count({
      where: {
        doctorId: doctorId!,
        fechaExpiracion: { lt: new Date() }
      }
    });

    res.json({
      total: stats.reduce((sum: number, s: any) => sum + s._count, 0),
      activas: stats.find((s: any) => s.activo === true)?._count || 0,
      inactivas: stats.find((s: any) => s.activo === false)?._count || 0,
      generales,
      individuales,
      expiradas
    });
  } catch (error) {
    console.error('Error al obtener estadísticas:', error);
    res.status(500).json({ error: 'Error al obtener estadísticas' });
  }
};

// ✅ OBTENER ASIGNACIONES POR EJERCICIO - Lista pacientes asignados a un ejercicio
export const obtenerAsignacionesPorEjercicio = async (req: Request, res: Response) => {
  try {
    const doctorId = req.doctorDbId;
    const { ejercicioId } = req.params;

    console.log('📝 [obtenerAsignacionesPorEjercicio] ejercicioId:', ejercicioId, 'doctorId:', doctorId);

    if (!doctorId) {
      return res.status(401).json({ error: 'No autenticado' });
    }

    if (!ejercicioId) {
      return res.status(400).json({ error: 'ejercicioId es requerido' });
    }

    const ejercicioIdNum = parseInt(ejercicioId as string);

    // Verificar que el ejercicio existe y que el doctor es dueño
    const ejercicio = await prisma.ejercicio.findUnique({
      where: { id: ejercicioIdNum }
    });

    if (!ejercicio) {
      return res.status(404).json({ error: 'Ejercicio no encontrado' });
    }

    // Obtener todas las asignaciones (generales e individuales) para este ejercicio del doctor
    const asignaciones = await prisma.asignacionEjercicio.findMany({
      where: {
        ejercicioId: ejercicioIdNum,
        doctorId: doctorId!,
        activo: true
      },
      include: {
        usuario: {
          select: {
            id: true,
            nombre: true,
            apellido: true,
            cedula: true,
            enfermedad: true,
            email: true
          }
        }
      },
      orderBy: [
        { usuarioId: 'asc' }, // null (generales) primero, luego individuales
        { fechaAsignacion: 'desc' }
      ]
    });

    console.log('✅ [obtenerAsignacionesPorEjercicio] Encontradas', asignaciones.length, 'asignaciones');

    res.json({
      ejercicioId: ejercicioIdNum,
      asignaciones: asignaciones.map((a: any) => ({
        id: a.id,
        usuarioId: a.usuarioId,
        enfermedad: a.enfermedad,
        usuario: a.usuario,
        activo: a.activo,
        fechaAsignacion: a.fechaAsignacion,
        fechaExpiracion: a.fechaExpiracion,
        descripcion: a.descripcion
      }))
    });
  } catch (error: any) {
    console.error('❌ Error al obtener asignaciones por ejercicio:', error.message);
    res.status(500).json({ error: 'Error al obtener asignaciones', details: error.message });
  }
};
