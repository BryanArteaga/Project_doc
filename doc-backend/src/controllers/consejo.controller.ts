import { Response, Request } from 'express';
import { prisma } from '../lib/prisma';
import { AuthRequest } from '../types';
import { z } from 'zod';

/**
 * CREAR CONSEJO EN EL CATÁLOGO (solo doctores)
 */
export const createConsejo = async (req: Request, res: Response) => {
  try {
    if (!req.doctorDbId) {
      return res.status(401).json({ error: 'Solo doctores pueden crear consejos' });
    }

    const schema = z.object({
      consejo: z.string().min(1),
      enfermedad: z.enum(['Hipertensión', 'Diabetes', 'Obesidad', 'Colesterol alto'])
    });

    const validated = schema.parse(req.body);

    // Validar: ¿Ya existe este consejo para esta enfermedad?
    const consejoExistente = await prisma.consejo.findFirst({
      where: {
        consejo: validated.consejo,
        enfermedad: validated.enfermedad
      }
    });

    if (consejoExistente) {
      console.log(`⚠️ [createConsejo] Consejo duplicado para ${validated.enfermedad}:`, validated.consejo.substring(0, 50));
      return res.status(409).json({ 
        error: `Este consejo ya existe para ${validated.enfermedad}`,
        consejoId: consejoExistente.id
      });
    }

    const data = {
      consejo: validated.consejo,
      enfermedad: validated.enfermedad
    };

    const consejoCreado = await prisma.consejo.create({ data });

    console.log('✅ [createConsejo] Consejo creado:', consejoCreado.id, 'para', validated.enfermedad);
    res.status(201).json(consejoCreado);
  } catch (error: any) {
    console.error('❌ Error al crear consejo:', error.message);
    res.status(500).json({ error: 'Error al crear consejo', details: error.message });
  }
};

/**
 * OBTENER CONSEJOS (doctores ven catálogo, pacientes ven consejos por enfermedad)
 */
export const getConsejos = async (req: AuthRequest, res: Response) => {
  try {
    const page = parseInt(req.query.page as string) || 1;
    const limit = parseInt(req.query.limit as string) || 10;
    const { enfermedad } = req.query;
    
    console.log('📚 [getConsejos] Iniciando - doctorDbId:', req.doctorDbId, ' userId:', req.userId, ' page:', page, ' limit:', limit);
    
    if (req.doctorDbId) {
      // DOCTOR: Retornar catálogo con paginación
      console.log('👨‍⚕️ [getConsejos] Modo DOCTOR - retornando catálogo de consejos');
      
      const where: any = {};
      if (enfermedad) {
        where.enfermedad = enfermedad as string;
      }

      const skip = (page - 1) * limit;

      const [consejos, total] = await Promise.all([
        prisma.consejo.findMany({
          where,
          orderBy: { consejo: 'asc' },
          skip,
          take: limit
        }),
        prisma.consejo.count({ where })
      ]);

      const totalPaginas = Math.ceil(total / limit);

      console.log('✅ [getConsejos] Consejos encontrados:', consejos.length, '/', total);
      return res.json({
        consejos,
        paginacion: {
          total,
          pagina: page,
          limit,
          totalPaginas
        }
      });
    }
    
    if (req.userId) {
      // PACIENTE: Retornar consejos por su enfermedad
      console.log('👤 [getConsejos] Modo PACIENTE - obteniendo consejos por enfermedad');
      
      const usuario = await prisma.usuario.findUnique({
        where: { id: req.userId }
      });

      if (!usuario || !usuario.enfermedad) {
        return res.json({
          consejos: [],
          paginacion: {
            total: 0,
            pagina: page,
            limit,
            totalPaginas: 0
          }
        });
      }

      const skip = (page - 1) * limit;

      const [consejos, total] = await Promise.all([
        prisma.consejo.findMany({
          where: { enfermedad: usuario.enfermedad },
          orderBy: { consejo: 'asc' },
          skip,
          take: limit
        }),
        prisma.consejo.count({ where: { enfermedad: usuario.enfermedad } })
      ]);

      const totalPaginas = Math.ceil(total / limit);

      console.log('✅ [getConsejos] Consejos encontrados para enfermedad', usuario.enfermedad, ':', consejos.length, '/', total);
      return res.json({
        consejos,
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
    console.error('❌ Error al obtener consejos:', error.message);
    res.status(500).json({ error: 'Error al obtener consejos', details: error.message });
  }
};

/**
 * OBTENER UN CONSEJO (solo doctores)
 */
export const getConsejo = async (req: Request, res: Response) => {
  try {
    if (!req.doctorDbId) {
      return res.status(401).json({ error: 'Solo doctores pueden ver consejos' });
    }

    const { id } = req.params;
    const consejo = await prisma.consejo.findUnique({
      where: { id: parseInt(id) }
    });

    if (!consejo) {
      return res.status(404).json({ error: 'Consejo no encontrado' });
    }

    res.json(consejo);
  } catch (error: any) {
    console.error('❌ Error al obtener consejo:', error.message);
    res.status(500).json({ error: 'Error al obtener consejo' });
  }
};

/**
 * ACTUALIZAR CONSEJO (solo doctores)
 */
export const updateConsejo = async (req: Request, res: Response) => {
  try {
    if (!req.doctorDbId) {
      return res.status(401).json({ error: 'Solo doctores pueden editar consejos' });
    }

    const { id } = req.params;
    const schema = z.object({
      consejo: z.string().min(1).optional(),
      enfermedad: z.enum(['Hipertensión', 'Diabetes', 'Obesidad', 'Colesterol alto']).optional()
    });

    const validated = schema.parse(req.body);

    const consejo = await prisma.consejo.update({
      where: { id: parseInt(id) },
      data: validated
    });

    console.log('✅ [updateConsejo] Consejo actualizado:', consejo.id);
    res.json(consejo);
  } catch (error: any) {
    console.error('❌ Error al actualizar consejo:', error.message);
    res.status(500).json({ error: 'Error al actualizar consejo' });
  }
};

/**
 * ELIMINAR CONSEJO (solo doctores)
 */
export const deleteConsejo = async (req: Request, res: Response) => {
  try {
    if (!req.doctorDbId) {
      return res.status(401).json({ error: 'Solo doctores pueden eliminar consejos' });
    }

    const { id } = req.params;

    await prisma.consejo.delete({
      where: { id: parseInt(id) }
    });

    console.log('✅ [deleteConsejo] Consejo eliminado:', id);
    res.json({ mensaje: 'Consejo eliminado correctamente' });
  } catch (error: any) {
    console.error('❌ Error al eliminar consejo:', error.message);
    res.status(500).json({ error: 'Error al eliminar consejo' });
  }
};
