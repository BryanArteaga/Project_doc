import { Request, Response } from 'express';
import { prisma } from '../lib/prisma';
import { z } from 'zod';
import { Timber, DebugTree } from '@crazyorr/timber';
import { SyncPushRequest, SyncPullResponse, SyncPushResponse } from '../types/sync.dto';

// Plant a default debug tree which directs logs to console
Timber.plant(new DebugTree());

// Esquemas de validación Zod
const RegistroSaludSchema = z.object({
  // usuarioId NO se requiere aquí (viene en el nivel superior del body)
  fecha: z.string(), // Acepta fecha con formato flexible "YYYY-MM-DD" o "YYYY-MM-DD HH:mm:ss"
  hora: z.string().optional(),
  presionSistolica: z.number().optional(),
  presionDiastolica: z.number().optional(),
  glucosa: z.number().optional(),
  colesterol: z.number().optional(),
  temperatura: z.number().optional(),
  peso: z.number().optional(),
  calorias: z.number().optional(),
  ejercicio: z.number().optional(),
  estadoAnimo: z.string().optional(),
  notas: z.string().optional()
});

const EjercicioRealizadoSchema = z.object({
  // usuarioId NO se requiere aquí (viene en el nivel superior del body)
  ejercicioId: z.number().positive(),
  asignacionId: z.number().optional(),
  fecha: z.string(), // Acepta fecha con formato flexible "YYYY-MM-DD" o "YYYY-MM-DD HH:mm:ss"
  completadoEn: z.string() // ISO timestamp
});

const SyncPushSchema = z.object({
  usuarioId: z.number().positive(), // El usuarioId viene aquí
  registrosSalud: z.array(RegistroSaludSchema),
  ejerciciosRealizados: z.array(EjercicioRealizadoSchema),
  ultimaSincronizacion: z.string().optional() // Opcional, puede ser null
});

/**
 * POST /api/sync/push
 * Recibe datos de la app móvil y los guarda en la BD
 * 
 * NOTA: Sin autenticación JWT. El usuarioId viene en el body.
 * En producción, debe agregarse autenticación.
 */
export const push = async (req: Request, res: Response) => {
  try {
    // Obtener usuarioId del body de la request (sin JWT)
    const userId = req.body.usuarioId;
    
    if (!userId || typeof userId !== 'number') {
      return res.status(400).json({ error: 'usuarioId requerido en el body' });
    }

    // Validar datos
    let syncData: SyncPushRequest;
    try {
      syncData = SyncPushSchema.parse(req.body);
    } catch (validationError: any) {
      return res.status(400).json({ 
        error: 'Datos inválidos',
        details: validationError.errors 
      });
    }

    let registrosSincronizados = 0;
    let ejerciciosSincronizados = 0;
    const errores: { registros?: string[]; ejercicios?: string[] } = {};

    // ============ SINCRONIZAR REGISTROS DE SALUD ============
    const registrosErrores: string[] = [];

    for (const registro of syncData.registrosSalud) {
      try {
        // Crear registro con el usuarioId del body
        await prisma.registroSalud.create({
          data: {
            usuarioId: userId,  // Usar el usuarioId del body de la request
            fecha: registro.fecha,
            hora: registro.hora,
            presionSistolica: registro.presionSistolica,
            presionDiastolica: registro.presionDiastolica,
            glucosa: registro.glucosa,
            colesterol: registro.colesterol,
            temperatura: registro.temperatura,
            peso: registro.peso,
            calorias: registro.calorias,
            ejercicio: registro.ejercicio,
            estadoAnimo: registro.estadoAnimo,
            notas: registro.notas
          }
        });

        registrosSincronizados++;
        Timber.debug(`✅ Registro de salud guardado para usuario ${userId} en fecha ${registro.fecha}`);
      } catch (error: any) {
        const errorMsg = `Error guardando registro ${registro.fecha}: ${error.message}`;
        registrosErrores.push(errorMsg);
        Timber.error(errorMsg);
      }
    }

    if (registrosErrores.length > 0) {
      errores.registros = registrosErrores;
    }

    // ============ SINCRONIZAR EJERCICIOS REALIZADOS ============
    const ejerciciosErrores: string[] = [];

    for (const ejercicio of syncData.ejerciciosRealizados) {
      try {
        // Verificar que el ejercicio existe
        const ejercicioExiste = await prisma.ejercicio.findUnique({
          where: { id: ejercicio.ejercicioId }
        });

        if (!ejercicioExiste) {
          ejerciciosErrores.push(`Ejercicio con ID ${ejercicio.ejercicioId} no existe`);
          continue;
        }

        // Crear ejercicio realizado
        await prisma.ejercicioRealizado.create({
          data: {
            usuarioId: userId,
            ejercicioId: ejercicio.ejercicioId,
            asignacionId: ejercicio.asignacionId,
            fecha: ejercicio.fecha,
            completadoEn: ejercicio.completadoEn ? new Date(ejercicio.completadoEn) : new Date()
          }
        });

        ejerciciosSincronizados++;
        Timber.debug(`✅ Ejercicio realizado guardado para usuario ${userId}`);
      } catch (error: any) {
        const errorMsg = `Error guardando ejercicio ${ejercicio.ejercicioId}: ${error.message}`;
        ejerciciosErrores.push(errorMsg);
        Timber.error(errorMsg);
      }
    }

    if (ejerciciosErrores.length > 0) {
      errores.ejercicios = ejerciciosErrores;
    }

    // Respuesta
    const response: SyncPushResponse = {
      success: registrosSincronizados > 0 || ejerciciosSincronizados > 0,
      message: `Sincronización completada: ${registrosSincronizados} registros, ${ejerciciosSincronizados} ejercicios`,
      registrosSincronizados,
      ejerciciosSincronizados,
      timestamp: new Date().toISOString()
    };

    if (Object.keys(errores).length > 0) {
      response.errores = errores;
    }

    // 📤 Log de sincronización exitosa
    Timber.debug(`✅ PUSH exitoso para usuario ${userId}:`);
    Timber.debug(`   📋 Registros sincronizados: ${registrosSincronizados}`);
    Timber.debug(`   🏃 Ejercicios sincronizados: ${ejerciciosSincronizados}`);

    res.json(response);
  } catch (error: any) {
    Timber.error(`Error en PUSH: ${error.message}`);
    return res.status(500).json({ 
      success: false,
      error: 'Error procesando sincronización PUSH',
      details: error.message
    });
  }
};

/**
 * GET /api/sync/pull
 * Envía datos del servidor a la app móvil
 * 
 * NOTA: Sin autenticación JWT. El usuarioId viene en query params.
 * En producción, debe agregarse autenticación.
 */
export const pull = async (req: Request, res: Response) => {
  try {
    // Obtener usuarioId del query params (sin JWT)
    const userId = parseInt(req.query.usuarioId as string);
    
    if (!userId || isNaN(userId)) {
      return res.status(400).json({ error: 'usuarioId requerido en query params' });
    }

    // Obtener usuario para verificar estado
    const usuario = await prisma.usuario.findUnique({
      where: { id: userId }
    });

    if (!usuario) {
      return res.status(404).json({ error: 'Usuario no encontrado' });
    }

    // Obtener ejercicios activos para este usuario
    // (Se obtienen a través de su doctor)
    const ejercicios = await prisma.ejercicio.findMany({
      select: {
        id: true,
        nombre: true,
        descripcion: true,
        intensidad: true,
        duracion: true
      }
    });

    // Obtener notificaciones pendientes/recientes
    const notificaciones = await prisma.notificacion.findMany({
      where: {
        usuarioId: userId,
        creadoEn: {
          gte: new Date(Date.now() - 7 * 24 * 60 * 60 * 1000) // Últimos 7 días
        }
      },
      select: {
        id: true,
        titulo: true,
        mensaje: true,
        tipo: true,
        leido: true,
        creadoEn: true
      },
      orderBy: { creadoEn: 'desc' },
      take: 20
    });

    const response: SyncPullResponse = {
      success: true,
      usuario: {
        id: usuario.id,
        nombre: usuario.nombre,
        apellido: usuario.apellido,
        email: usuario.email,
        telefono: usuario.telefonoPersonal
      },
      ejercicios,
      notificaciones,
      estadoCuenta: {
        activo: usuario.estadoCuenta === 'ACTIVE',
        verificado: usuario.estadoCuenta !== 'INACTIVE',
        ultimaSincronizacion: new Date().toISOString()
      }
    };

    res.json(response);
  } catch (error: any) {
    Timber.error(`Error en PULL: ${error.message}`);
    return res.status(500).json({ 
      success: false,
      error: 'Error obteniendo datos de sincronización',
      details: error.message
    });
  }
};

/**
 * POST /api/sync/register-fcm-token
 * Registra el token de FCM del dispositivo
 */
export const registerFCMToken = async (req: Request, res: Response) => {
  try {
    const userId = (req as any).userId;
    
    if (!userId) {
      return res.status(401).json({ error: 'No autenticado' });
    }

    const { token } = req.body;

    if (!token || typeof token !== 'string') {
      return res.status(400).json({ error: 'Token de FCM requerido' });
    }

    // Actualizar o crear registro de token FCM
    const usuario = await prisma.usuario.update({
      where: { id: userId },
      data: {
        tokenDispositivo: token,
        ultimaSincronizacion: new Date()
      }
    });

    Timber.debug(`✅ FCM token registrado para usuario ${userId}`);

    res.json({
      success: true,
      message: 'Token FCM registrado exitosamente',
      userId
    });
  } catch (error: any) {
    Timber.error(`Error registrando FCM token: ${error.message}`);
    return res.status(500).json({ 
      success: false,
      error: 'Error registrando token FCM'
    });
  }
};

/**
 * GET /api/sync/status
 * Obtiene el estado de sincronización
 */
export const getSyncStatus = async (req: Request, res: Response) => {
  try {
    const userId = (req as any).userId;
    
    if (!userId) {
      return res.status(401).json({ error: 'No autenticado' });
    }

    const usuario = await prisma.usuario.findUnique({
      where: { id: userId },
      select: {
        id: true,
        tokenDispositivo: true,
        ultimaSincronizacion: true,
        estadoCuenta: true,
        creadoEn: true,
        actualizadoEn: true
      }
    });

    if (!usuario) {
      return res.status(404).json({ error: 'Usuario no encontrado' });
    }

    // Contar datos pendientes
    const registrosSalud = await prisma.registroSalud.count({
      where: { usuarioId: userId }
    });

    const ejerciciosRealizados = await prisma.ejercicioRealizado.count({
      where: { usuarioId: userId }
    });

    const notificacionesNoLeidas = await prisma.notificacion.count({
      where: {
        usuarioId: userId,
        leido: false
      }
    });

    res.json({
      success: true,
      usuario,
      estadisticas: {
        registrosSalud,
        ejerciciosRealizados,
        notificacionesNoLeidas
      },
      conectado: usuario.tokenDispositivo ? true : false,
      ultimaSincronizacion: usuario.ultimaSincronizacion
    });
  } catch (error: any) {
    Timber.error(`Error obteniendo estado de sincronización: ${error.message}`);
    return res.status(500).json({ 
      success: false,
      error: 'Error obteniendo estado de sincronización'
    });
  }
};
