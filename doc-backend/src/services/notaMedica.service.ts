import { prisma } from '../lib/prisma';

interface CreateNotaMedicaInput {
  pacienteId: number;
  doctorId: number;
  contenido: string;
}

interface UpdateNotaMedicaInput {
  contenido: string;
}

export class NotaMedicaService {
  /**
   * Crear nueva nota médica
   */
  static async crear(data: CreateNotaMedicaInput) {
    try {
      // Validar que el paciente existe
      const paciente = await prisma.usuario.findUnique({
        where: { id: data.pacienteId }
      });

      if (!paciente) {
        throw new Error('Paciente no encontrado');
      }

      // Validar que el doctor existe
      const doctor = await prisma.doctor.findUnique({
        where: { id: data.doctorId }
      });

      if (!doctor) {
        throw new Error('Doctor no encontrado');
      }

      // Crear nota
      const nota = await prisma.notaMedica.create({
        data: {
          pacienteId: data.pacienteId,
          doctorId: data.doctorId,
          contenido: data.contenido
        },
        include: {
          doctor: {
            select: {
              id: true,
              nombre: true,
              apellido: true,
              especialidad: true
            }
          }
        }
      });

      console.log('✅ [NotaMedicaService] Nota creada:', nota.id);
      return nota;
    } catch (error: any) {
      console.error('❌ [NotaMedicaService] Error al crear nota:', error.message);
      throw error;
    }
  }

  /**
   * Obtener notas de un paciente
   */
  static async obtenerPorPaciente(pacienteId: number) {
    try {
      const notas = await prisma.notaMedica.findMany({
        where: { pacienteId },
        include: {
          doctor: {
            select: {
              id: true,
              nombre: true,
              apellido: true,
              especialidad: true
            }
          }
        },
        orderBy: { fecha: 'desc' }
      });

      console.log(`✅ [NotaMedicaService] Se obtuvieron ${notas.length} notas del paciente ${pacienteId}`);
      return notas;
    } catch (error: any) {
      console.error('❌ [NotaMedicaService] Error al obtener notas:', error.message);
      throw error;
    }
  }

  /**
   * Obtener una nota específica
   */
  static async obtenerPorId(notaId: number) {
    try {
      const nota = await prisma.notaMedica.findUnique({
        where: { id: notaId },
        include: {
          doctor: {
            select: {
              id: true,
              nombre: true,
              apellido: true,
              especialidad: true
            }
          },
          paciente: {
            select: {
              id: true,
              nombre: true,
              apellido: true
            }
          }
        }
      });

      if (!nota) {
        throw new Error('Nota médica no encontrada');
      }

      console.log(`✅ [NotaMedicaService] Nota obtenida: ${notaId}`);
      return nota;
    } catch (error: any) {
      console.error('❌ [NotaMedicaService] Error al obtener nota:', error.message);
      throw error;
    }
  }

  /**
   * Actualizar nota médica (solo el doctor que la creó)
   */
  static async actualizar(notaId: number, doctorId: number, data: UpdateNotaMedicaInput) {
    try {
      // Verificar que la nota existe y pertenece al doctor
      const nota = await prisma.notaMedica.findUnique({
        where: { id: notaId }
      });

      if (!nota) {
        throw new Error('Nota médica no encontrada');
      }

      if (nota.doctorId !== doctorId) {
        throw new Error('No tienes permiso para actualizar esta nota');
      }

      // Actualizar
      const notaActualizada = await prisma.notaMedica.update({
        where: { id: notaId },
        data: {
          contenido: data.contenido
        },
        include: {
          doctor: {
            select: {
              id: true,
              nombre: true,
              apellido: true,
              especialidad: true
            }
          }
        }
      });

      console.log('✅ [NotaMedicaService] Nota actualizada:', notaId);
      return notaActualizada;
    } catch (error: any) {
      console.error('❌ [NotaMedicaService] Error al actualizar nota:', error.message);
      throw error;
    }
  }

  /**
   * Eliminar nota médica (solo el doctor que la creó)
   */
  static async eliminar(notaId: number, doctorId: number) {
    try {
      // Verificar que la nota existe y pertenece al doctor
      const nota = await prisma.notaMedica.findUnique({
        where: { id: notaId }
      });

      if (!nota) {
        throw new Error('Nota médica no encontrada');
      }

      if (nota.doctorId !== doctorId) {
        throw new Error('No tienes permiso para eliminar esta nota');
      }

      // Eliminar
      await prisma.notaMedica.delete({
        where: { id: notaId }
      });

      console.log('✅ [NotaMedicaService] Nota eliminada:', notaId);
      return { message: 'Nota eliminada correctamente' };
    } catch (error: any) {
      console.error('❌ [NotaMedicaService] Error al eliminar nota:', error.message);
      throw error;
    }
  }
}
