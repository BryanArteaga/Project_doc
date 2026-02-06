import { prisma } from '../lib/prisma';
import { NotificationType } from '../types/prisma';

interface PushData {
  registrosSalud?: Array<{
    idSincronizacion: string;
    presionSistolica?: number;
    presionDiastolica?: number;
    glucosa?: number;
    colesterol?: number;
    temperatura?: number;
    peso?: number;
    notas?: string;
    registradoEn: string;
  }>;
  ejerciciosRealizados?: Array<{
    idSincronizacion: string;
    asignacionId: number;
    ejercicioId: number;
    duracionReal?: number;
    dificultad?: number;
    dolor?: number;
    notas?: string;
    completadoEn: string;
  }>;
}

interface PullResponse {
  registrosSalud: any[];
  asignacionesEjercicio: any[];
  notificaciones: any[];
  ultimaSincronizacion: string;
}

export class SyncService {
  static async pushLocalChanges(usuarioId: number, data: PushData) {
    const results = {
      registrosSaludGuardados: 0,
      ejerciciosRealizadosGuardados: 0,
      errores: [] as string[],
    };

    try {
      if (data.registrosSalud && data.registrosSalud.length > 0) {
        for (const record of data.registrosSalud) {
          try {
            const existing = await prisma.registroSalud.findFirst({
              where: {
                idSincronizacion: record.idSincronizacion,
                usuarioId,
              },
            });

            if (!existing) {
              await prisma.registroSalud.create({
                data: {
                  usuarioId,
                  idSincronizacion: record.idSincronizacion,
                  presionSistolica: record.presionSistolica,
                  presionDiastolica: record.presionDiastolica,
                  glucosa: record.glucosa,
                  colesterol: record.colesterol,
                  temperatura: record.temperatura,
                  peso: record.peso,
                  notas: record.notas,
                  registradoEn: new Date(record.registradoEn),
                  sincronizadoEn: new Date(),
                },
              });
              results.registrosSaludGuardados++;
            }
          } catch (error: any) {
            results.errores.push(`Error al guardar registro de salud: ${error.message}`);
          }
        }
      }

      if (data.ejerciciosRealizados && data.ejerciciosRealizados.length > 0) {
        for (const completion of data.ejerciciosRealizados) {
          try {
            const existing = await prisma.ejercicioRealizado.findFirst({
              where: {
                idSincronizacion: completion.idSincronizacion,
              },
            });

            if (!existing) {
              await prisma.ejercicioRealizado.create({
                data: {
                  asignacionId: completion.asignacionId,
                  ejercicioId: completion.ejercicioId,
                  usuarioId,
                  idSincronizacion: completion.idSincronizacion,
                  duracionReal: completion.duracionReal,
                  dificultad: completion.dificultad,
                  dolor: completion.dolor,
                  notas: completion.notas,
                  completadoEn: new Date(completion.completadoEn),
                  sincronizadoEn: new Date(),
                },
              });
              results.ejerciciosRealizadosGuardados++;
            }
          } catch (error: any) {
            results.errores.push(`Error al guardar ejercicio realizado: ${error.message}`);
          }
        }
      }

      await prisma.usuario.update({
        where: { id: usuarioId },
        data: { ultimaSincronizacion: new Date() },
      });

      return results;
    } catch (error: any) {
      throw new Error(`Error en sincronización PUSH: ${error.message}`);
    }
  }

  static async pullRemoteChanges(usuarioId: number, ultimaSincronizacion?: string): Promise<PullResponse> {
    try {
      const desde = ultimaSincronizacion ? new Date(ultimaSincronizacion) : new Date(0);

      const asignacionesEjercicio = await prisma.asignacionEjercicio.findMany({
        where: {
          pacienteId: usuarioId,
          actualizadoEn: { gte: desde },
        },
        include: {
          ejercicio: true,
          completaciones: true,
        },
      });

      const notificaciones = await prisma.notificacion.findMany({
        where: {
          usuarioId,
          leido: false,
        },
        orderBy: { creadoEn: 'desc' },
      });

      const registrosSalud = await prisma.registroSalud.findMany({
        where: {
          usuarioId,
          actualizadoEn: { gte: desde },
        },
      });

      return {
        asignacionesEjercicio: asignacionesEjercicio.map((ea: any) => ({
          id: ea.id,
          idSincronizacion: ea.idSincronizacion,
          ejercicioId: ea.ejercicioId,
          estado: ea.estado,
          progreso: ea.progreso,
          frecuenciaRecordatorio: ea.frecuenciaRecordatorio,
          asignadoEn: ea.asignadoEn,
          fechaInicio: ea.fechaInicio,
          fechaFin: ea.fechaFin,
          ejercicio: {
            id: ea.ejercicio.id,
            nombre: ea.ejercicio.nombre,
            descripcion: ea.ejercicio.descripcion,
            enfermedad: ea.ejercicio.enfermedad,
            duracion: ea.ejercicio.duracion,
            intensidad: ea.ejercicio.intensidad,
            series: ea.ejercicio.series,
            descansoEntreSeries: ea.ejercicio.descansoEntreSeries,
          },
          completaciones: ea.completaciones.length,
        })),
        notificaciones: notificaciones.map((n: any) => ({
          id: n.id,
          tipo: n.tipo,
          titulo: n.titulo,
          mensaje: n.mensaje,
          metadatos: n.metadatos,
          creadoEn: n.creadoEn,
        })),
        registrosSalud,
        ultimaSincronizacion: new Date().toISOString(),
      };
    } catch (error: any) {
      throw new Error(`Error en sincronización PULL: ${error.message}`);
    }
  }

  static async markNotificationsAsRead(usuarioId: number, notificacionIds: number[]) {
    try {
      const result = await prisma.notificacion.updateMany({
        where: {
          id: { in: notificacionIds },
          usuarioId,
        },
        data: {
          leido: true,
          leidoEn: new Date(),
        },
      });

      return {
        actualizadas: result.count,
      };
    } catch (error: any) {
      throw new Error(`Error al marcar notificaciones como leídas: ${error.message}`);
    }
  }

  static async createExerciseAssignedNotification(pacienteId: number, nombreEjercicio: string, doctorId: number) {
    try {
      const doctor = await prisma.usuario.findUnique({
        where: { id: doctorId },
        select: { nombre: true, apellido: true },
      });

      await prisma.notificacion.create({
        data: {
          usuarioId: pacienteId,
          tipo: NotificationType.exercise_assigned,
          titulo: 'Nuevo ejercicio asignado',
          mensaje: `${doctor?.nombre} ${doctor?.apellido} te ha asignado el ejercicio: ${nombreEjercicio}`,
          metadatos: {
            nombreEjercicio,
            asignadoPor: doctorId,
          } as any,
        },
      });
    } catch (error: any) {
      console.error('Error al crear notificación de ejercicio:', error);
    }
  }

  static async getSyncSummary(usuarioId: number) {
    try {
      const usuario = await prisma.usuario.findUnique({
        where: { id: usuarioId },
        select: { ultimaSincronizacion: true },
      });

      const notificacionesSinLeer = await prisma.notificacion.count({
        where: {
          usuarioId,
          leido: false,
        },
      });

      const ejerciciosPendientes = await prisma.asignacionEjercicio.count({
        where: {
          pacienteId: usuarioId,
          estado: 'PENDING',
        },
      });

      const registrosSaludSinSincronizar = await prisma.registroSalud.count({
        where: {
          usuarioId,
          sincronizadoEn: null,
        },
      });

      return {
        ultimaSincronizacion: usuario?.ultimaSincronizacion,
        notificacionesSinLeer,
        ejerciciosPendientes,
        registrosSaludSinSincronizar,
        requiereSincronizacion: registrosSaludSinSincronizar > 0 || notificacionesSinLeer > 0,
      };
    } catch (error: any) {
      throw new Error(`Error al obtener resumen de sincronización: ${error.message}`);
    }
  }
}
