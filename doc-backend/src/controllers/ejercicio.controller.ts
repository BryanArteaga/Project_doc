import { Response, Request } from 'express';
import { prisma } from '../lib/prisma';
import { AuthRequest, EjercicioRealizadoDTO } from '../types';
import { z } from 'zod';

/**
 * CREAR EJERCICIO EN EL CATÁLOGO (solo doctores)
 */
export const createEjercicio = async (req: Request, res: Response) => {
  try {
    if (!req.doctorDbId) {
      return res.status(401).json({ error: 'Solo doctores pueden crear ejercicios' });
    }

    const schema = z.object({
      nombre: z.string().min(1),
      descripcion: z.string().optional(),
      duracion: z.number().min(1), // en segundos
      intensidad: z.enum(['Baja', 'Media', 'Alta']),
      series: z.number().min(1),
      descansoEntreSeries: z.number().min(0),
      enfermedad: z.enum(['Hipertensión', 'Diabetes', 'Obesidad', 'Colesterol alto']),
      urlImagen: z.string().optional().nullable(),
      urlVideo: z.string().optional().nullable()
    });

    const validated = schema.parse(req.body);

    // Ensure no id is sent from the client
    const data = {
      nombre: validated.nombre,
      descripcion: validated.descripcion || null,
      duracion: validated.duracion,
      intensidad: validated.intensidad,
      series: validated.series,
      descansoEntreSeries: validated.descansoEntreSeries,
      enfermedad: validated.enfermedad,
      urlImagen: validated.urlImagen || null,
      urlVideo: validated.urlVideo || null
    };

    const ejercicio = await prisma.ejercicio.create({ data });

    console.log('✅ [createEjercicio] Ejercicio creado:', ejercicio.id);
    res.status(201).json(ejercicio);
  } catch (error: any) {
    console.error('❌ Error al crear ejercicio:', error.message);
    res.status(500).json({ error: 'Error al crear ejercicio', details: error.message });
  }
};

/**
 * CREAR EJERCICIO REALIZADO (solo pacientes)
 */
export const createEjercicioRealizado = async (req: AuthRequest, res: Response) => {
  try {
    if (!req.userId) {
      return res.status(401).json({ error: 'Solo pacientes pueden registrar ejercicios realizados' });
    }

    const data: EjercicioRealizadoDTO = req.body;
    const ejercicio = await prisma.ejercicioRealizado.create({
      data: {
        usuarioId: req.userId,
        ...data
      }
    });
    res.status(201).json(ejercicio);
  } catch (error: any) {
    console.error('❌ Error al crear ejercicio realizado:', error.message);
    res.status(500).json({ error: 'Error al registrar ejercicio' });
  }
};
export const getEjercicios = async (req: AuthRequest, res: Response) => {
  try {
    const page = parseInt(req.query.page as string) || 1;
    const limit = parseInt(req.query.limit as string) || 10;
    const { enfermedad, intensidad } = req.query;
    
    console.log('📚 [getEjercicios] Iniciando - doctorDbId:', req.doctorDbId, ' userId:', req.userId, ' page:', page, ' limit:', limit);
    
    if (req.doctorDbId) {
      // DOCTOR: Retornar catálogo con paginación
      console.log('👨‍⚕️ [getEjercicios] Modo DOCTOR - retornando catálogo de ejercicios');
      
      const where: any = {};
      if (enfermedad) {
        where.enfermedad = enfermedad as string;
      }
      if (intensidad) {
        where.intensidad = intensidad as string;
      }

      const skip = (page - 1) * limit;

      const [ejercicios, total] = await Promise.all([
        prisma.ejercicio.findMany({
          where,
          orderBy: { nombre: 'asc' },
          skip,
          take: limit
        }),
        prisma.ejercicio.count({ where })
      ]);

      const totalPaginas = Math.ceil(total / limit);

      console.log('✅ [getEjercicios] Ejercicios encontrados:', ejercicios.length, '/', total);
      return res.json({
        ejercicios,
        paginacion: {
          total,
          pagina: page,
          limit,
          totalPaginas
        }
      });
    }
    
    if (req.userId) {
      // PACIENTE: Retornar ejercicios realizados con paginación
      console.log('👤 [getEjercicios] Modo PACIENTE - filtrando por usuarioId:', req.userId);
      
      const skip = (page - 1) * limit;

      const [ejercicios, total] = await Promise.all([
        prisma.ejercicioRealizado.findMany({
          where: { usuarioId: req.userId },
          orderBy: { fecha: 'desc' },
          skip,
          take: limit
        }),
        prisma.ejercicioRealizado.count({ where: { usuarioId: req.userId } })
      ]);

      const totalPaginas = Math.ceil(total / limit);

      console.log('✅ [getEjercicios] Ejercicios realizados encontrados:', ejercicios.length, '/', total);
      return res.json({
        ejercicios,
        paginacion: {
          total,
          pagina: page,
          limit,
          totalPaginas
        }
      });
    }
    
    // Si no es doctor ni paciente, retornar error
    return res.status(401).json({ error: 'No autenticado' });
    
  } catch (error: any) {
    console.error('❌ Error al obtener ejercicios:', error.message);
    res.status(500).json({ error: 'Error al obtener ejercicios', details: error.message });
  }
};

/**
 * OBTENER UN EJERCICIO (solo doctores)
 */
export const getEjercicio = async (req: Request, res: Response) => {
  try {
    if (!req.doctorDbId) {
      return res.status(401).json({ error: 'Solo doctores pueden ver ejercicios' });
    }

    const { id } = req.params;
    const ejercicio = await prisma.ejercicio.findUnique({
      where: { id: parseInt(id) }
    });

    if (!ejercicio) {
      return res.status(404).json({ error: 'Ejercicio no encontrado' });
    }

    res.json(ejercicio);
  } catch (error: any) {
    console.error('❌ Error al obtener ejercicio:', error.message);
    res.status(500).json({ error: 'Error al obtener ejercicio' });
  }
};

/**
 * ACTUALIZAR EJERCICIO (solo doctores)
 */
export const updateEjercicio = async (req: Request, res: Response) => {
  try {
    if (!req.doctorDbId) {
      return res.status(401).json({ error: 'Solo doctores pueden editar ejercicios' });
    }

    const { id } = req.params;
    const schema = z.object({
      nombre: z.string().min(1).optional(),
      descripcion: z.string().optional(),
      duracion: z.number().min(1).optional(),
      intensidad: z.enum(['Baja', 'Media', 'Alta']).optional(),
      series: z.number().min(1).optional(),
      descansoEntreSeries: z.number().min(0).optional(),
      enfermedad: z.enum(['Hipertensión', 'Diabetes', 'Obesidad', 'Colesterol alto']).optional(),
      urlImagen: z.string().optional().nullable(),
      urlVideo: z.string().optional().nullable()
    });

    const validated = schema.parse(req.body);

    const ejercicio = await prisma.ejercicio.update({
      where: { id: parseInt(id) },
      data: validated
    });

    console.log('✅ [updateEjercicio] Ejercicio actualizado:', ejercicio.id);
    res.json(ejercicio);
  } catch (error: any) {
    console.error('❌ Error al actualizar ejercicio:', error.message);
    res.status(500).json({ error: 'Error al actualizar ejercicio' });
  }
};

/**
 * ELIMINAR EJERCICIO (solo doctores)
 */
export const deleteEjercicio = async (req: Request, res: Response) => {
  try {
    if (!req.doctorDbId) {
      return res.status(401).json({ error: 'Solo doctores pueden eliminar ejercicios' });
    }

    const { id } = req.params;

    // Verificar si hay asignaciones activas
    const asignaciones = await prisma.asignacionEjercicio.findFirst({
      where: { ejercicioId: parseInt(id) }
    });

    if (asignaciones) {
      return res.status(400).json({ 
        error: 'No se puede eliminar ejercicio con asignaciones activas. Elimina las asignaciones primero.' 
      });
    }

    await prisma.ejercicio.delete({
      where: { id: parseInt(id) }
    });

    console.log('✅ [deleteEjercicio] Ejercicio eliminado:', id);
    res.json({ mensaje: 'Ejercicio eliminado correctamente' });
  } catch (error: any) {
    console.error('❌ Error al eliminar ejercicio:', error.message);
    res.status(500).json({ error: 'Error al eliminar ejercicio' });
  }
};
