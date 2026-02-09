import admin from 'firebase-admin';
import { Timber, DebugTree } from '@crazyorr/timber';

// Plant a default debug tree which directs logs to console
Timber.plant(new DebugTree());

/**
 * Servicio para enviar notificaciones push vía Firebase Cloud Messaging
 * 
 * Topics:
 * - exercise_reminders: Recordatorios de ejercicio (todos los usuarios)
 * - health_reminders: Recordatorios de salud (todos los usuarios)
 * - user_{userId}: Notificaciones personales (activación, mensajes)
 */
export class NotificationService {

  /**
   * Enviar notificación push a un usuario específico
   * @param userId ID del usuario (topic: user_{userId})
   * @param titulo Título de la notificación
   * @param mensaje Cuerpo del mensaje
   * @param tipo Tipo: exercise_reminder, health_reminder, etc.
   * @param data Datos adicionales (opcional)
   */
  static async sendPushToUser(
    userId: number,
    titulo: string,
    mensaje: string,
    tipo: string,
    data?: Record<string, string>
  ): Promise<void> {
    try {
      if (!admin.apps.length) {
        Timber.warn('Firebase no inicializado, notificación no enviada');
        return;
      }

      const message = {
        notification: {
          title: titulo,
          body: mensaje
        },
        data: {
          tipo,
          timestamp: new Date().toISOString(),
          ...data
        },
        topic: `user_${userId}`,
        android: {
          priority: 'high',
          notification: {
            clickAction: 'com.example.doc.NOTIFICATION_CLICK',
            sound: 'default',
            channelId: 'medicini_reminders'
          }
        }
      };

      const response = await admin.messaging().send(message as any);
      Timber.debug(`Notificación enviada a user_${userId}: ${response}`);
    } catch (error) {
      Timber.error(`Error enviando notificación a user_${userId}:`, error);
    }
  }

  /**
   * Enviar notificación de recordatorio de ejercicio a todos los usuarios
   * @param titulo Título
   * @param mensaje Mensaje
   * @param hora Hora (ej: "10:00")
   */
  static async sendExerciseReminder(
    titulo: string,
    mensaje: string,
    hora: string
  ): Promise<void> {
    try {
      if (!admin.apps.length) {
        Timber.warn('Firebase no inicializado');
        return;
      }

      const message = {
        notification: {
          title: titulo,
          body: mensaje
        },
        data: {
          tipo: 'exercise_reminder',
          hora,
          timestamp: new Date().toISOString()
        },
        topic: 'exercise_reminders',
        android: {
          priority: 'high',
          notification: {
            sound: 'default',
            channelId: 'medicini_reminders'
          }
        }
      };

      const response = await admin.messaging().send(message as any);
      Timber.debug(`✅ Recordatorio de ejercicio enviado: ${response}`);
    } catch (error) {
      Timber.error('❌ Error enviando recordatorio de ejercicio:', error);
    }
  }

  /**
   * Enviar notificación de recordatorio de salud a todos los usuarios
   * @param titulo Título
   * @param mensaje Mensaje
   * @param hora Hora (ej: "20:00")
   */
  static async sendHealthReminder(
    titulo: string,
    mensaje: string,
    hora: string
  ): Promise<void> {
    try {
      if (!admin.apps.length) {
        Timber.warn('Firebase no inicializado');
        return;
      }

      const message = {
        notification: {
          title: titulo,
          body: mensaje
        },
        data: {
          tipo: 'health_reminder',
          hora,
          timestamp: new Date().toISOString()
        },
        topic: 'health_reminders',
        android: {
          priority: 'high',
          notification: {
            sound: 'default',
            channelId: 'medicini_reminders'
          }
        }
      };

      const response = await admin.messaging().send(message as any);
      Timber.debug(`Recordatorio de salud enviado: ${response}`);
    } catch (error) {
      Timber.error('Error enviando recordatorio de salud:', error);
    }
  }

  /**
   * Suscribir usuarios a topic específico
   * Nota: En la app Android se hace con FirebaseMessaging.subscribeToTopic()
   */
  static async subscribeUserToTopic(usuarioId: number, topic: string): Promise<void> {
    try {
      // Esta función es más para referencias
      // Los usuarios se suscriben desde la app Android automáticamente
      Timber.debug(`Usuario ${usuarioId} debe estar suscrito a topic: ${topic}`);
    } catch (error) {
      Timber.error('Error en subscribeUserToTopic:', error);
    }
  }
}

export default NotificationService;
