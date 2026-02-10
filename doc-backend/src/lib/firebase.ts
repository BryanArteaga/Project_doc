import admin from 'firebase-admin';

// Inicializar Firebase Admin SDK
if (!admin.apps.length) {
  try {
    // Opción 1: Usar credenciales del archivo de servicio (recomendado para producción)
    // const serviceAccount = require('../../firebase-service-account.json');
    // admin.initializeApp({
    //   credential: admin.credential.cert(serviceAccount),
    // });

    // Opción 2: Usar Application Default Credentials (para desarrollo con gcloud)
    admin.initializeApp({
      credential: admin.credential.applicationDefault(),
    });

    console.log('✅ Firebase Admin inicializado correctamente');
  } catch (error) {
    console.error('❌ Error al inicializar Firebase Admin:', error);
    // Continuar sin Firebase si falla (no bloquear la app)
  }
}

export { admin };
