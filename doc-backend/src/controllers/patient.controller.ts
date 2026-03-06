import { Request, Response } from 'express';
import { prisma } from '../lib/prisma';
import { AccountStatus, NotificationType } from '../types/prisma';
import { z } from 'zod';
import { sendPushToUser } from './notification.controller';

// Schemas de validación
const PaginationSchema = z.object({
  page: z.string().optional().default('1').pipe(z.coerce.number().min(1)),
  limit: z.string().optional().default('10').pipe(z.coerce.number().min(1).max(100)),
});

/**
 * Obtener lista de pacientes del doctor
 */
export const getPatients = async (req: Request, res: Response) => {
  try {
    // Soportar tanto doctores (req.doctorDbId) como pacientes (req.userId)
    const doctorId = req.doctorDbId || req.userId;
    
    if (!doctorId) {
      return res.status(401).json({ error: 'No autenticado' });
    }

    const { page, limit } = PaginationSchema.parse(req.query);
    const skip = (page - 1) * limit;

    // Obtener pacientes del doctor
    const [patients, total] = await Promise.all([
      prisma.usuario.findMany({
        where: {
          idDoctorAsignado: doctorId,
        },
        select: {
          id: true,
          nombre: true,
          apellido: true,
          cedula: true,
          estadoCuenta: true,
          registradoEn: true,
          activadoEn: true,
          edad: true,
          sexo: true,
          enfermedad: true,
        },
        skip,
        take: limit,
        orderBy: { registradoEn: 'desc' },
      }),
      prisma.usuario.count({
        where: {
          idDoctorAsignado: doctorId,
        },
      }),
    ]);

    res.json({
      patients,
      pagination: {
        page,
        limit,
        total,
        pages: Math.ceil(total / limit),
      },
    });
  } catch (error: any) {
    console.error('Error al obtener pacientes:', error);
    res.status(500).json({ error: error.mensaje || 'Error al obtener pacientes' });
  }
};

/**
 * Obtener detalle de un paciente
 */
export const getPatient = async (req: Request, res: Response) => {
  try {
    // Soportar tanto doctores (req.doctorDbId) como pacientes (req.userId)
    const doctorId = req.doctorDbId || req.userId;
    
    if (!doctorId) {
      return res.status(401).json({ error: 'No autenticado' });
    }

    const id = req.params.id as string;
    // @ts-ignore - req.params siempre contiene strings
    const patientId = parseInt(id);

    const patient = await prisma.usuario.findUnique({
      where: { id: patientId },
      include: {
        registrosSalud: {
          orderBy: { registradoEn: 'desc' },
          take: 20,
        },
        ejerciciosRealizados: {
          orderBy: { completadoEn: 'desc' },
          take: 50,
          include: {
            ejercicio: {
              select: {
                nombre: true,
                descripcion: true,
                duracion: true,
              }
            },
          },
        },
      },
    });

    if (!patient) {
      return res.status(404).json({ error: 'Paciente no encontrado' });
    }

    // Verificar que el doctor sea el asignado
    if (patient.idDoctorAsignado !== doctorId) {
      return res.status(403).json({ error: 'No tienes permiso para ver este paciente' });
    }

    res.json(patient);
  } catch (error: any) {
    console.error('Error al obtener detalle de paciente:', error);
    res.status(500).json({ error: error.mensaje || 'Error al obtener paciente' });
  }
};

/**
 * Obtener pacientes pendientes de activación
 */
export const getPendingPatients = async (req: Request, res: Response) => {
  try {
    // Soportar tanto doctores (req.doctorDbId) como pacientes (req.userId)
    const doctorId = req.doctorDbId || req.userId;
    
    if (!doctorId) {
      return res.status(401).json({ error: 'No autenticado' });
    }

    const { page, limit } = PaginationSchema.parse(req.query);
    const skip = (page - 1) * limit;

    const [patients, total] = await Promise.all([
      prisma.usuario.findMany({
        where: {
          estadoCuenta: AccountStatus.INACTIVE,
          idDoctorAsignado: doctorId,
        },
        select: {
          id: true,
          nombre: true,
          apellido: true,
          cedula: true,
          registradoEn: true,
          estadoCuenta: true,
        },
        skip,
        take: limit,
        orderBy: { registradoEn: 'asc' },
      }),
      prisma.usuario.count({
        where: {
          estadoCuenta: AccountStatus.INACTIVE,
          idDoctorAsignado: doctorId,
        },
      }),
    ]);

    res.json({
      patients,
      pagination: {
        page,
        limit,
        total,
        pages: Math.ceil(total / limit),
      },
    });
  } catch (error: any) {
    console.error('Error al obtener pacientes pendientes:', error);
    res.status(500).json({ error: error.mensaje || 'Error al obtener pacientes pendientes' });
  }
};

/**
 * Asignar paciente a doctor
 */
export const assignPatientToDoctor = async (req: Request, res: Response) => {
  try {
    // Soportar tanto doctores (req.doctorDbId) como pacientes (req.userId)
    const doctorId = req.doctorDbId || req.userId;
    
    if (!doctorId) {
      return res.status(401).json({ error: 'No autenticado' });
    }

    const { patientId } = req.body;

    if (!patientId) {
      return res.status(400).json({ error: 'patientId es requerido' });
    }

    const patient = await prisma.usuario.findUnique({
      where: { id: patientId },
    });

    if (!patient) {
      return res.status(404).json({ error: 'Paciente no encontrado' });
    }

    const updated = await prisma.usuario.update({
      where: { id: patientId },
      data: {
        idDoctorAsignado: doctorId,
      },
    });

    res.json({
      message: 'Paciente asignado al doctor exitosamente',
      patient: {
        id: updated.id,
        nombre: updated.nombre,
        apellido: updated.apellido,
        cedula: updated.cedula,
        idDoctorAsignado: updated.idDoctorAsignado,
      },
    });
  } catch (error: any) {
    console.error('Error al asignar paciente:', error);
    res.status(500).json({ error: error.mensaje || 'Error al asignar paciente' });
  }
};
