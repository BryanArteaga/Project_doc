import { Response, Request } from 'express';
import { z } from 'zod';
import { NotaMedicaService } from '../services/notaMedica.service';
import { AuthRequest } from '../types';

/**
 * CREAR NOTA MÉDICA (solo doctores)
 */
export const createNotaMedica = async (req: AuthRequest, res: Response) => {
  try {
    if (!req.doctorDbId) {
      return res.status(401).json({ error: 'Solo doctores pueden crear notas médicas' });
    }

    const schema = z.object({
      pacienteId: z.number().int().min(1),
      contenido: z.string().min(1, 'La nota no puede estar vacía')
    });

    const validated = schema.parse(req.body);

    const nota = await NotaMedicaService.crear({
      pacienteId: validated.pacienteId,
      doctorId: req.doctorDbId,
      contenido: validated.contenido
    });

    res.status(201).json(nota);
  } catch (error: any) {
    console.error('❌ Error al crear nota médica:', error.message);
    
    if (error instanceof z.ZodError) {
      return res.status(400).json({ error: 'Validación fallida', details: error.errors });
    }

    res.status(500).json({ error: error.message || 'Error al crear nota médica' });
  }
};

/**
 * OBTENER NOTAS DE UN PACIENTE (doctor asignado o el paciente mismo)
 */
export const getNotasPorPaciente = async (req: AuthRequest, res: Response) => {
  try {
    const { pacienteId } = req.params;
    const pacId = parseInt(pacienteId);

    if (isNaN(pacId)) {
      return res.status(400).json({ error: 'ID de paciente inválido' });
    }

    // Verificar permisos (solo el doctor asignado o admin pueden ver)
    if (!req.doctorDbId) {
      return res.status(401).json({ error: 'No autorizado' });
    }

    // Opcional: Verificar que el doctor está asignado al paciente
    // Aquí puedes agregar lógica de permisos si es necesario

    const notas = await NotaMedicaService.obtenerPorPaciente(pacId);

    res.status(200).json(notas);
  } catch (error: any) {
    console.error('❌ Error al obtener notas:', error.message);
    res.status(500).json({ error: error.message || 'Error al obtener notas' });
  }
};

/**
 * OBTENER UNA NOTA ESPECÍFICA
 */
export const getNotaById = async (req: AuthRequest, res: Response) => {
  try {
    const { notaId } = req.params;
    const id = parseInt(notaId);

    if (isNaN(id)) {
      return res.status(400).json({ error: 'ID de nota inválido' });
    }

    const nota = await NotaMedicaService.obtenerPorId(id);

    res.status(200).json(nota);
  } catch (error: any) {
    console.error('❌ Error al obtener nota:', error.message);
    
    if (error.message === 'Nota médica no encontrada') {
      return res.status(404).json({ error: error.message });
    }

    res.status(500).json({ error: error.message || 'Error al obtener nota' });
  }
};

/**
 * ACTUALIZAR NOTA MÉDICA (solo el doctor que la creó)
 */
export const updateNotaMedica = async (req: AuthRequest, res: Response) => {
  try {
    if (!req.doctorDbId) {
      return res.status(401).json({ error: 'Solo doctores pueden actualizar notas' });
    }

    const { notaId } = req.params;
    const id = parseInt(notaId);

    if (isNaN(id)) {
      return res.status(400).json({ error: 'ID de nota inválido' });
    }

    const schema = z.object({
      contenido: z.string().min(1, 'La nota no puede estar vacía')
    });

    const validated = schema.parse(req.body);

    const nota = await NotaMedicaService.actualizar(id, req.doctorDbId, {
      contenido: validated.contenido
    });

    res.status(200).json(nota);
  } catch (error: any) {
    console.error('❌ Error al actualizar nota médica:', error.message);
    
    if (error instanceof z.ZodError) {
      return res.status(400).json({ error: 'Validación fallida', details: error.errors });
    }

    if (error.message.includes('permiso')) {
      return res.status(403).json({ error: error.message });
    }

    if (error.message === 'Nota médica no encontrada') {
      return res.status(404).json({ error: error.message });
    }

    res.status(500).json({ error: error.message || 'Error al actualizar nota médica' });
  }
};

/**
 * ELIMINAR NOTA MÉDICA (solo el doctor que la creó)
 */
export const deleteNotaMedica = async (req: AuthRequest, res: Response) => {
  try {
    if (!req.doctorDbId) {
      return res.status(401).json({ error: 'Solo doctores pueden eliminar notas' });
    }

    const { notaId } = req.params;
    const id = parseInt(notaId);

    if (isNaN(id)) {
      return res.status(400).json({ error: 'ID de nota inválido' });
    }

    const result = await NotaMedicaService.eliminar(id, req.doctorDbId);

    res.status(200).json(result);
  } catch (error: any) {
    console.error('❌ Error al eliminar nota médica:', error.message);
    
    if (error.message.includes('permiso')) {
      return res.status(403).json({ error: error.message });
    }

    if (error.message === 'Nota médica no encontrada') {
      return res.status(404).json({ error: error.message });
    }

    res.status(500).json({ error: error.message || 'Error al eliminar nota médica' });
  }
};
