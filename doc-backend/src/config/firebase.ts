import admin from 'firebase-admin';
import { Timber, DebugTree } from '@crazyorr/timber';
import path from 'path';
import fs from 'fs';

// Plant a default debug tree which directs logs to console
Timber.plant(new DebugTree());

/**
 * Inicializar Firebase Admin SDK
 * 
 * Se requiere el archivo firebase-service-account.json en la raíz del proyecto
 * 
 * Pasos para obtenerlo:
 * 1. Ir a Firebase Console → Project Settings → Service Accounts
 * 2. Hacer click en "Generate New Private Key"
 * 3. Se descargará un JSON
 * 4. Guardar como firebase-service-account.json en la raíz del proyecto
 * 5. Agregar a .gitignore para no commitir
 */
export function initializeFirebase() {
  try {
    // Si ya está inicializado, no hacer nada
    if (admin.apps && admin.apps.length > 0) {
      Timber.debug('✅ Firebase Admin SDK ya inicializado');
      return;
    }

    // Ruta al archivo de credenciales
    const serviceAccountPath = path.join(
      process.cwd(),
      'firebase-service-account.json'
    );

    // Verificar si existe el archivo
    if (!fs.existsSync(serviceAccountPath)) {
      Timber.warn('firebase-service-account.json no encontrado.');
      Timber.warn('   Las notificaciones push no funcionarán sin este archivo.');
      Timber.warn('   Pasos para configurarlo:');
      Timber.warn('   1. Ir a Firebase Console → Project Settings → Service Accounts');
      Timber.warn('   2. Hacer click en "Generate New Private Key"');
      Timber.warn('   3. Guardar como firebase-service-account.json en la raíz del proyecto');
      return;
    }

    // Leer y parsear el archivo de credenciales
    const serviceAccountJson = fs.readFileSync(serviceAccountPath, 'utf8');
    const serviceAccount = JSON.parse(serviceAccountJson);

    // Inicializar Firebase Admin
    admin.initializeApp({
      credential: admin.credential.cert(serviceAccount),
      databaseURL: process.env.FIREBASE_DATABASE_URL,
    });

    Timber.debug('Firebase Admin SDK inicializado correctamente');
  } catch (error: any) {
    Timber.error('Error al inicializar Firebase:', error.message);
    // No lanzar error, permitir que la app continúe sin Firebase
    // Las notificaciones simplemente no se enviarán
  }
}

/**
 * Verificar si Firebase está inicializado
 */
export function isFirebaseInitialized(): boolean {
  return admin.apps && admin.apps.length > 0;
}

export default admin;
