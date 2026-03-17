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
  ejercicioMinutos: z.number().optional(),  // ✅ Minutos de ejercicio (Int), no nombre (String)
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
        // 🔧 CORRECCIÓN: Usar la fecha del app, no la fecha de sincronización
        // Permitir que registradoEn sea la fecha de creación original del app
        let registradoEn: Date;
        
        if (registro.fecha) {
          // Si viene fecha, parsearla (formato: YYYY-MM-DD o YYYY-MM-DD HH:mm:ss)
          const horaCompleta = registro.hora ? `${registro.fecha} ${registro.hora}` : registro.fecha;
          registradoEn = new Date(horaCompleta);
          
          // Validar que sea una fecha válida
          if (isNaN(registradoEn.getTime())) {
            registradoEn = new Date();  // Fallback a ahora si la fecha es inválida
          }
        } else {
          registradoEn = new Date();  // Usar ahora si no viene fecha
        }

        // 🔧 FIX: Verificar si ya existe un registro con la misma fecha/hora para evitar duplicados
        const registroExistente = await prisma.registroSalud.findFirst({
          where: {
            usuarioId: userId,
            fecha: registro.fecha,
            hora: registro.hora
          }
        });

        if (registroExistente) {
          // Si existe, actualizar en lugar de crear nuevo
          await prisma.registroSalud.update({
            where: { id: registroExistente.id },
            data: {
              presionSistolica: registro.presionSistolica,
              presionDiastolica: registro.presionDiastolica,
              glucosa: registro.glucosa,
              colesterol: registro.colesterol,
              temperatura: registro.temperatura,
              peso: registro.peso,
              calorias: registro.calorias,
              ejercicioMinutos: registro.ejercicioMinutos || 0,
              estadoAnimo: registro.estadoAnimo,
              notas: registro.notas
            }
          });
          Timber.debug(`♻️ Registro de salud actualizado (duplicado detectado) para usuario ${userId} en fecha ${registro.fecha}`);
        } else {
          // Crear registro nuevo
          await prisma.registroSalud.create({
            data: {
              usuarioId: userId,  // Usar el usuarioId del body de la request
              fecha: registro.fecha,
              hora: registro.hora,
              registradoEn,  // 🔧 Usar la fecha de creación original del app
              presionSistolica: registro.presionSistolica,
              presionDiastolica: registro.presionDiastolica,
              glucosa: registro.glucosa,
              colesterol: registro.colesterol,
              temperatura: registro.temperatura,
              peso: registro.peso,
              calorias: registro.calorias,
              ejercicioMinutos: registro.ejercicioMinutos || 0,  // ✅ Usar ejercicioMinutos (Int)
              estadoAnimo: registro.estadoAnimo,
              notas: registro.notas
            }
          });
          Timber.debug(`✅ Registro de salud guardado para usuario ${userId} en fecha ${registro.fecha} (registradoEn: ${registradoEn.toISOString()})`);
        }
        
        registrosSincronizados++;
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

    // Obtener registros de salud del usuario (históricos)
    const registrosSalud = await prisma.registroSalud.findMany({
      where: { usuarioId: userId },
      select: {
        id: true,
        fecha: true,
        hora: true,
        presionSistolica: true,
        presionDiastolica: true,
        glucosa: true,
        colesterol: true,
        temperatura: true,
        peso: true,
        calorias: true,
        ejercicioMinutos: true,  // ✅ Cambiar de ejercicio a ejercicioMinutos
        estadoAnimo: true,
        notas: true,
        registradoEn: true
      },
      orderBy: { fecha: 'desc' }
    });

    // 🔧 Mapear registros para convertir registradoEn a timestamp (Long)
    const registrosSaludMapeados = registrosSalud.map((r: any) => ({
      id: r.id,
      fecha: r.fecha,
      hora: r.hora,
      presionSistolica: r.presionSistolica,
      presionDiastolica: r.presionDiastolica,
      glucosa: r.glucosa,
      colesterol: r.colesterol,
      temperatura: r.temperatura,
      peso: r.peso,
      calorias: r.calorias,
      ejercicioMinutos: r.ejercicioMinutos,  // ✅ Cambiar de ejercicio a ejercicioMinutos
      estadoAnimo: r.estadoAnimo,
      notas: r.notas,
      registradoEn: r.registradoEn.getTime() // Convertir DateTime a timestamp (milliseconds)
    }));

    // Obtener asignaciones PRIMERO para después obtener solo los ejercicios asignados
    // - Directas (usuarioId = usuario.id)
    // - Generales (usuarioId = null AND enfermedad = usuario.enfermedad)
    // - Activas y no expiradas
    const ahora = new Date();
    const asignaciones = await prisma.asignacionEjercicio.findMany({
      where: {
        activo: true,
        AND: [
          {
            OR: [
              { usuarioId: userId },                                  // Asignación directa
              { usuarioId: null, enfermedad: usuario.enfermedad }     // Asignación general por enfermedad
            ]
          },
          {
            OR: [
              { fechaExpiracion: null },                              // Sin expiración
              { fechaExpiracion: { gt: ahora } }                      // No expirada
            ]
          }
        ]
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
            urlVideo: true,
            enfermedad: true,
            descansoEntreSeries: true,
            creadoEn: true
          }
        },
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

    // 🔧 Extraer SOLO los ejercicios que fueron asignados (sin duplicados)
    const ejerciciosAsignados = asignaciones.map((a: any) => a.ejercicio);
    const ejercicioIdsAsignados = new Set(ejerciciosAsignados.map((e: any) => e.id));
    
    // ✅ NUEVO: Obtener también ejercicios generales del catálogo (no solo asignados)
    // para que no solo vea los ejercicios que le asignó el doctor
    const ejerciciosGenerales = await prisma.ejercicio.findMany({
      where: {
        enfermedad: usuario.enfermedad,
        tipo: 'POR_ENFERMEDAD'  // Solo ejercicios generales, no "por pacientes"
      },
      select: {
        id: true,
        nombre: true,
        descripcion: true,
        duracion: true,
        series: true,
        intensidad: true,
        urlImagen: true,
        urlVideo: true,
        enfermedad: true,
        descansoEntreSeries: true,
        creadoEn: true
      }
    });

    // 🔧 Combinar: Asignados primero (más recientes) + Generales (ordenados DESC)
    const ejerciciosAsignadosDedup = Array.from(ejercicioIdsAsignados).map((id: any) => 
      ejerciciosAsignados.find((e: any) => e.id === id)
    ).filter(Boolean) as any[];
    
    // Combinar y ordenar: asignados primero por creadoEn, luego generales por creadoEn
    const ejercicios = [
      ...ejerciciosAsignadosDedup.sort((a: any, b: any) => 
        new Date(b.creadoEn).getTime() - new Date(a.creadoEn).getTime()
      ),
      ...ejerciciosGenerales
        .filter((eg: any) => !ejercicioIdsAsignados.has(eg.id))  // Filtrar duplicados
        .sort((a: any, b: any) => 
          new Date(b.creadoEn).getTime() - new Date(a.creadoEn).getTime()
        )
    ];

    Timber.debug(`📊 [pull] Ejercicios para usuario ${userId}:`);
    Timber.debug(`   - Asignados: ${ejerciciosAsignadosDedup.length}`);
    Timber.debug(`   - Del catálogo: ${ejerciciosGenerales.length - ejerciciosAsignadosDedup.length}`);
    Timber.debug(`   - Total: ${ejercicios.length}`);

    // 🔧 Mapear asignaciones para convertir timestamp
    const asignacionesMapeadas = asignaciones.map((a: any) => ({
      id: a.id,
      ejercicioId: a.ejercicioId,
      doctorId: a.doctorId,
      usuarioId: a.usuarioId,
      enfermedad: a.enfermedad,
      descripcion: a.descripcion,
      activo: a.activo,
      fechaAsignacion: a.fechaAsignacion.getTime().toString(),     // Convertir a timestamp string
      fechaExpiracion: a.fechaExpiracion?.getTime().toString(),     // Convertir a timestamp string
      ejercicio: a.ejercicio,
      doctor: a.doctor
    }));

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

    // ✅ NUEVO: Obtener consejos por enfermedad del usuario
    const consejos = await prisma.consejo.findMany({
      where: {
        enfermedad: usuario.enfermedad
      },
      select: {
        id: true,
        consejo: true,
        enfermedad: true,
        creadoEn: true
      },
      orderBy: { consejo: 'asc' }
    });
    Timber.debug(`📝 [pull] Consejos obtenidos para ${usuario.enfermedad}: ${consejos.length}`);

    const response: SyncPullResponse = {
      success: true,
      usuario: {
        id: usuario.id,
        nombre: usuario.nombre,
        apellido: usuario.apellido,
        email: usuario.email,
        telefono: usuario.telefonoPersonal,
        // 🔧 Agregar datos personales que faltaban en el PULL
        edad: usuario.edad,
        peso: usuario.peso,
        altura: usuario.altura,
        sexo: usuario.sexo,
        enfermedad: usuario.enfermedad,
        colorEnfermedad: usuario.colorEnfermedad,
        // ✅ NUEVO: Configuración de notificaciones (no mostrar pop-up si ya existe)
        notifEnabled: usuario.notifEnabled,
        notifType: usuario.notifType,
        notifDetails: usuario.notifDetails
      },
      registrosSalud: registrosSaludMapeados,  // 🔧 Usar versión mapeada con timestamps
      asignaciones: asignacionesMapeadas,      // ✅ NUEVO: Asignaciones de ejercicios
      ejercicios,
      consejos,                                 // ✅ NUEVO: Consejos por enfermedad
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
 * 
 * IMPORTANTE: userId es REQUERIDO porque no se puede crear dispositivo sin usuario en Prisma.
 * El token se registra cuando el usuario se crea (vía tokenDispositivo en POST /usuarios/create)
 */
export const registerFCMToken = async (req: Request, res: Response) => {
  try {
    // Obtener userId de middleware (autenticado) o del body
    const userId = (req as any).userId || req.body.userId;

    const { token, deviceId } = req.body;

    if (!token || typeof token !== 'string') {
      return res.status(400).json({ error: 'Token de FCM requerido' });
    }

    if (!userId) {
      return res.status(400).json({ 
        error: 'userId es requerido. Registra el token al crear usuario con tokenDispositivo.' 
      });
    }

    // Buscar si ya existe un dispositivo con este token para este usuario
    const existingDevice = await prisma.dispositivo.findFirst({
      where: {
        usuarioId: userId,
        fcmToken: token,
      },
    });

    if (existingDevice) {
      // Actualizar última vez visto
      await prisma.dispositivo.update({
        where: { id: existingDevice.id },
        data: { ultimaVezVisto: new Date() },
      });

      Timber.debug(`✅ FCM token actualizado para usuario ${userId}`);

      return res.json({
        success: true,
        message: 'Token FCM actualizado',
        deviceId: existingDevice.id,
      });
    }

    // Crear nuevo registro de dispositivo
    const device = await prisma.dispositivo.create({
      data: {
        usuarioId: userId,
        fcmToken: token,
        deviceId: deviceId || `device_${Date.now()}`,
        activo: true,
      },
    });

    // También actualizar el campo legacy tokenDispositivo del usuario
    await prisma.usuario.update({
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
      userId,
      deviceId: device.id
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

/**
 * GET /api/sync/check-updates
 * Verifica si hay cambios en ejercicios, consejos y asignaciones desde el último sync
 * Retorna flags para que la app sepa si necesita hacer PULL
 * 
 * Query params:
 * - usuarioId: ID del usuario (requerido)
 * - lastSync: Timestamp en milisegundos del último sync (opcional)
 */
export const checkUpdates = async (req: Request, res: Response) => {
  try {
    const userId = parseInt(req.query.usuarioId as string);
    const lastSyncMs = parseInt(req.query.lastSync as string) || 0;
    const lastSync = new Date(lastSyncMs);

    if (!userId || isNaN(userId)) {
      return res.status(400).json({ error: 'usuarioId requerido en query params' });
    }

    Timber.debug(`🔍 [checkUpdates] Verificando cambios para usuario: ${userId}, lastSync: ${lastSync.toISOString()}`);

    // Obtener usuario para verificar su enfermedad
    const usuario = await prisma.usuario.findUnique({
      where: { id: userId }
    });

    if (!usuario) {
      return res.status(404).json({ error: 'Usuario no encontrado' });
    }

    // 1️⃣ Verificar cambios en EJERCICIOS
    const exercisesUpdated = await prisma.ejercicio.count({
      where: {
        actualizadoEn: {
          gte: lastSync
        }
      }
    });

    // 2️⃣ Verificar cambios en CONSEJOS
    const adviceUpdated = await prisma.consejo.count({
      where: {
        enfermedad: usuario.enfermedad,
        actualizadoEn: {
          gte: lastSync
        }
      }
    });

    // 3️⃣ Verificar cambios en ASIGNACIONES (directas + generales por enfermedad)
    const assignmentsUpdated = await prisma.asignacionEjercicio.count({
      where: {
        AND: [
          {
            OR: [
              { usuarioId: userId },
              { usuarioId: null, enfermedad: usuario.enfermedad }
            ]
          },
          {
            actualizadoEn: {
              gte: lastSync
            }
          }
        ]
      }
    });

    const hasExerciseUpdates = exercisesUpdated > 0;
    const hasAdviceUpdates = adviceUpdated > 0;
    const hasAssignmentUpdates = assignmentsUpdated > 0;
    const shouldPull = hasExerciseUpdates || hasAdviceUpdates || hasAssignmentUpdates;

    Timber.debug(`✅ [checkUpdates] Resultados - Ejercicios: ${hasExerciseUpdates}, Consejos: ${hasAdviceUpdates}, Asignaciones: ${hasAssignmentUpdates}, shouldPull: ${shouldPull}`);

    res.json({
      shouldPull,
      hasExerciseUpdates,
      hasAdviceUpdates,
      hasAssignmentUpdates,
      lastBackendSync: new Date().getTime()
    });
  } catch (error: any) {
    Timber.error(`❌ Error en checkUpdates: ${error.message}`);
    return res.status(500).json({ 
      error: 'Error verificando actualizaciones',
      details: error.message
    });
  }
};

/**
 * PUT /api/sync/update-notifications
 * Actualizar configuración de notificaciones del usuario
 * FIX: Agregar endpoint que faltaba para actualizar notificaciones
 */
export const updateNotifications = async (req: Request, res: Response) => {
  try {
    const { usuarioId, notifEnabled, notifType, notifDetails } = req.body;

    if (!usuarioId) {
      return res.status(400).json({ error: 'usuarioId requerido' });
    }

    const updated = await prisma.usuario.update({
      where: { id: usuarioId },
      data: {
        ...(notifEnabled !== undefined && { notifEnabled }),
        ...(notifType !== undefined && { notifType }),
        ...(notifDetails !== undefined && { notifDetails })
      }
    });

    Timber.debug(`✅ Notificaciones actualizadas para usuario ${usuarioId}`);
    res.status(200).json(updated);
  } catch (error: any) {
    Timber.error(`❌ Error actualizando notificaciones: ${error.message}`);
    res.status(500).json({ error: 'Error al actualizar notificaciones' });
  }
};

/**
 * PUT /api/sync/update-profile
 * Actualizar perfil del usuario desde sync
 * FIX: Agregar endpoint que faltaba para actualizar perfil vía sync
 */
export const updateSyncProfile = async (req: Request, res: Response) => {
  try {
    const { 
      usuarioId,
      cedula,
      nombre,
      apellido,
      edad,
      peso,
      altura,
      sexo,
      enfermedad,
      glucosaInicial,
      colesterolInicial,
      confirmDataChange
    } = req.body;

    if (!usuarioId) {
      return res.status(400).json({ error: 'usuarioId requerido' });
    }

    // 🔍 OPCIÓN B: Verificar si el usuario YA EXISTÍA (por cédula)
    let isExistingUser = false;
    if (cedula) {
      const userByCedula = await prisma.usuario.findFirst({
        where: { cedula: cedula }
      });
      if (userByCedula && userByCedula.id === usuarioId) {
        const now = new Date();
        const createdAt = new Date(userByCedula.creadoEn);
        const minutesSinceCreation = (now.getTime() - createdAt.getTime()) / (1000 * 60);
        isExistingUser = minutesSinceCreation > 2;
      }
    }

    // 🔍 Detectar si está intentando cambiar datos personales
    const isChangingPersonalData = nombre !== undefined || apellido !== undefined || 
                                    edad !== undefined || peso !== undefined || 
                                    altura !== undefined || sexo !== undefined || enfermedad !== undefined;

    // ⚠️ OPCIÓN B: Si es usuario existente Y está cambiando datos personales
    // Requiere confirmación explícita
    if (isExistingUser && isChangingPersonalData && !confirmDataChange) {
      console.warn(`⚠️ Usuario existente (ID: ${usuarioId}) vía sync intenta cambiar datos sin confirmación`);
      const currentUser = await prisma.usuario.findUnique({ where: { id: usuarioId } });
      return res.status(403).json({
        error: 'Confirmación requerida para cambiar datos personales',
        requiresConfirmation: true,
        currentData: {
          nombre: currentUser?.nombre,
          apellido: currentUser?.apellido,
          edad: currentUser?.edad,
          peso: currentUser?.peso,
          altura: currentUser?.altura,
          sexo: currentUser?.sexo,
          enfermedad: currentUser?.enfermedad
        },
        proposedData: {
          nombre,
          apellido,
          edad,
          peso,
          altura,
          sexo,
          enfermedad
        },
        message: 'Usuario debe confirmar cambios explícitamente'
      });
    }

    const updateData: any = {};

    // ✅ Permitir cambiar datos personales si:
    // - No es usuario existente, O
    // - Es usuario existente PERO viene confirmDataChange: true
    if (!isExistingUser || confirmDataChange) {
      if (nombre !== undefined) updateData.nombre = nombre.trim();
      if (apellido !== undefined) updateData.apellido = apellido.trim();
      if (edad !== undefined) updateData.edad = parseInt(edad);
      if (peso !== undefined) updateData.peso = parseFloat(parseFloat(peso).toFixed(1));
      if (altura !== undefined) updateData.altura = parseFloat(parseFloat(altura).toFixed(2));
      if (sexo !== undefined) updateData.sexo = sexo.toUpperCase();
      if (enfermedad !== undefined) updateData.enfermedad = enfermedad;
    }

    // 🔒 CEDULA NUNCA SE ACTUALIZA - Es un documento de identidad único e inmutable
    // Ignorar si viene en el request
    if (cedula !== undefined) {
      const currentUser = await prisma.usuario.findUnique({ where: { id: usuarioId } });
      if (currentUser && currentUser.cedula !== cedula) {
        Timber.warn(`⚠️ Intento de cambiar cédula via sync bloqueado. ID: ${usuarioId}, Cédula anterior: ${currentUser.cedula}, Cédula propuesta: ${cedula}`);
      }
    }

    // ✅ SIEMPRE permitir: glucosa, colesterol
    if (glucosaInicial !== undefined) updateData.glucosaInicial = parseFloat(parseFloat(glucosaInicial).toFixed(1));
    if (colesterolInicial !== undefined) updateData.colesterolInicial = parseFloat(parseFloat(colesterolInicial).toFixed(1));

    const updated = await prisma.usuario.update({
      where: { id: usuarioId },
      data: updateData
    });

    const successMessage = confirmDataChange && isExistingUser
      ? 'Datos personales actualizados vía sync con confirmación'
      : 'Perfil actualizado vía sync';

    Timber.debug(`✅ ${successMessage} para usuario ${usuarioId}`);
    res.status(200).json({
      usuario: updated,
      isExistingUser: isExistingUser,
      message: successMessage
    });
  } catch (error: any) {
    Timber.error(`❌ Error actualizando perfil: ${error.message}`);
    res.status(500).json({ error: 'Error al actualizar perfil' });
  }
};
