import * as schedule from 'node-schedule';
import { prisma } from '../lib/prisma';
import { sendPushToUser } from '../controllers/notification.controller';
import * as admin from 'firebase-admin';

/**
 * Servicio de programación de notificaciones
 * Ejecuta automáticamente cada minuto para enviar notificaciones push
 * en los momentos programados por los usuarios
 */

export class NotificationScheduler {
  /**
   * Iniciar el scheduler de notificaciones
   */
  static start() {
    console.log('📅 Iniciando NotificationScheduler...');

    // Ejecutar cada minuto
    const job = schedule.scheduleJob('* * * * *', async () => {
      try {
        console.log('🔔 Verificando notificaciones programadas...');
        await NotificationScheduler.checkAndSendNotifications();
      } catch (error) {
        console.error(`❌ Error en NotificationScheduler: ${error}`);
      }
    });

    console.log('✅ NotificationScheduler iniciado (ejecuta cada minuto)');
    return job;
  }

  /**
   * Verificar y enviar notificaciones según la configuración del usuario
   */
  private static async checkAndSendNotifications() {
    try {
      // 1. Obtener todos los usuarios con notificaciones habilitadas
      const usuarios = await prisma.usuario.findMany({
        where: {
          notifEnabled: true,
          estadoCuenta: 'ACTIVE'
        },
        include: {
          dispositivos: {
            where: { activo: true }
          }
        }
      });

      console.log(`📋 Verificando ${usuarios.length} usuarios con notificaciones habilitadas`);

      for (const usuario of usuarios) {
        // 2. Verificar notificación de salud
        await NotificationScheduler.checkHealthNotification(usuario);

        // 3. Verificar notificación de ejercicio
        await NotificationScheduler.checkExerciseNotification(usuario);
      }
    } catch (error) {
      console.error(`Error verificando notificaciones: ${error}`);
    }
  }

  /**
   * Verificar si debe enviar notificación de salud
   */
  private static async checkHealthNotification(usuario: any) {
    try {
      const config = JSON.parse(usuario.notifDetails || '{"hour":20,"minute":0}');
      const ahora = new Date();
      const horaActual = ahora.getHours();
      const minutoActual = ahora.getMinutes();

      let debeEnviar = false;

      switch (usuario.notifType) {
        case 'daily_fixed':
          debeEnviar = horaActual === config.hour && minutoActual === config.minute;
          break;

        case 'weekly_fixed':
          const diaActual = ahora.getDay();
          const diaConfigurable = config.dayOfWeek || 2; // 2 = Lunes por defecto
          debeEnviar =
            diaActual === diaConfigurable &&
            horaActual === config.hour &&
            minutoActual === config.minute;
          break;

        case 'monthly_fixed':
          const fechaActual = ahora.getDate();
          const fechaConfigurable = config.dayOfMonth || 1;
          debeEnviar =
            fechaActual === fechaConfigurable &&
            horaActual === config.hour &&
            minutoActual === config.minute;
          break;
      }

      if (debeEnviar) {
        // Verificar que no ya fue enviada hoy/esta semana/este mes
        const yaFueEnviada = await NotificationScheduler.yaFueEnviada(
          usuario.id,
          'health_reminder',
          usuario.notifType
        );

        if (!yaFueEnviada) {
          console.log(
            `📤 Enviando notificación de salud al usuario ${usuario.id} (${usuario.nombre})`
          );

          // Enviar push
          await sendPushToUser(
            usuario.id,
            'Recordatorio de Salud',
            '¿Ya registraste tus datos de salud hoy?',
            { type: 'health_reminder', navigate_to: 'registro_salud' }
          );

          // Registrar en BD para evitar duplicados
          await prisma.notificacion.create({
            data: {
              usuarioId: usuario.id,
              tipo: 'health_reminder',
              titulo: 'Recordatorio de Salud',
              mensaje: '¿Ya registraste tus datos de salud hoy?',
              leido: false,
              enviada: new Date()
            }
          });

          console.log(`✅ Notificación de salud enviada a usuario ${usuario.id}`);
        }
      }
    } catch (error) {
      console.error(`Error verificando notificación de salud para usuario ${usuario.id}: ${error}`);
    }
  }

  /**
   * Verificar si debe enviar notificación de ejercicio (10:00 AM y 6:00 PM)
   */
  private static async checkExerciseNotification(usuario: any) {
    try {
      const ahora = new Date();
      const horaActual = ahora.getHours();
      const minutoActual = ahora.getMinutes();

      // Ejercicios a las 10:00 AM (10) y 6:00 PM (18)
      const horasEjercicio = [10, 18];

      if (horasEjercicio.includes(horaActual) && minutoActual === 0) {
        // Verificar que no ya fue enviada en esta hora hoy
        const yaFueEnviada = await NotificationScheduler.yaFueEnviadaEjercicio(
          usuario.id,
          horaActual
        );

        if (!yaFueEnviada) {
          const horaTexto = horaActual === 10 ? '10:00 AM' : '6:00 PM';

          console.log(
            `📤 Enviando notificación de ejercicio al usuario ${usuario.id} a las ${horaTexto}`
          );

          // Enviar push
          await sendPushToUser(
            usuario.id,
            'Recordatorio de Ejercicio',
            `¡Es hora de realizar tu rutina de ejercicios! (${horaTexto})`,
            { type: 'exercise_reminder', navigate_to: 'ejercicios' }
          );

          // Registrar en BD
          await prisma.notificacion.create({
            data: {
              usuarioId: usuario.id,
              tipo: 'exercise_reminder',
              titulo: 'Recordatorio de Ejercicio',
              mensaje: `¡Es hora de realizar tu rutina de ejercicios! (${horaTexto})`,
              leido: false,
              enviada: new Date()
            }
          });

          console.log(
            `✅ Notificación de ejercicio enviada a usuario ${usuario.id} a las ${horaTexto}`
          );
        }
      }
    } catch (error) {
      console.error(`Error verificando notificación de ejercicio para usuario ${usuario.id}: ${error}`);
    }
  }

  /**
   * Verificar si la notificación de salud ya fue enviada
   */
  private static async yaFueEnviada(
    usuarioId: number,
    tipo: string,
    notifType: string
  ): Promise<boolean> {
    try {
      const ahora = new Date();
      let desde: Date;
      let hasta: Date;

      // Determinar rango de tiempo según tipo de notificación
      if (notifType === 'daily_fixed') {
        // Mismo día
        desde = new Date(ahora);
        desde.setHours(0, 0, 0, 0);
        hasta = new Date(ahora);
        hasta.setHours(23, 59, 59, 999);
      } else if (notifType === 'weekly_fixed') {
        // Esta semana (lunes a domingo)
        desde = new Date(ahora);
        desde.setDate(desde.getDate() - desde.getDay() + 1); // Lunes
        desde.setHours(0, 0, 0, 0);
        hasta = new Date(desde);
        hasta.setDate(hasta.getDate() + 6); // Domingo
        hasta.setHours(23, 59, 59, 999);
      } else {
        // monthly_fixed - Este mes
        desde = new Date(ahora.getFullYear(), ahora.getMonth(), 1, 0, 0, 0, 0);
        hasta = new Date(ahora.getFullYear(), ahora.getMonth() + 1, 0, 23, 59, 59, 999);
      }

      const notificacion = await prisma.notificacion.findFirst({
        where: {
          usuarioId,
          tipo,
          enviada: {
            gte: desde,
            lte: hasta
          }
        }
      });

      return notificacion != null;
    } catch (error) {
      console.error(`Error verificando si fue enviada: ${error}`);
      return false;
    }
  }

  /**
   * Verificar si la notificación de ejercicio ya fue enviada a esta hora hoy
   */
  private static async yaFueEnviadaEjercicio(
    usuarioId: number,
    horaEjercicio: number
  ): Promise<boolean> {
    try {
      const ahora = new Date();
      const hoy = new Date(ahora);
      hoy.setHours(0, 0, 0, 0);
      const manana = new Date(hoy);
      manana.setDate(manana.getDate() + 1);

      const notificacion = await prisma.notificacion.findFirst({
        where: {
          usuarioId,
          tipo: 'exercise_reminder',
          enviada: {
            gte: hoy,
            lt: manana
          },
          // Verificar aproximadamente a esta hora (dentro de 5 minutos)
          mensaje: {
            contains: horaEjercicio === 10 ? '10:00 AM' : '6:00 PM'
          }
        }
      });

      return notificacion != null;
    } catch (error) {
      console.error(`Error verificando si fue enviada notificación de ejercicio: ${error}`);
      return false;
    }
  }
}
