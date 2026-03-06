import { prisma } from './src/lib/prisma';

async function resetDatabase() {
  try {
    console.log('🔄 Reseteando base de datos...');

    // Eliminar datos en orden de dependencias (por foreign keys)
    console.log('🗑️  Eliminando ejercicios realizados...');
    await prisma.ejercicioRealizado.deleteMany({});
    console.log('✅ Ejercicios realizados eliminados');

    console.log('🗑️  Eliminando registros de salud...');
    await prisma.registroSalud.deleteMany({});
    console.log('✅ Registros de salud eliminados');

    console.log('🗑️  Eliminando usuarios...');
    await prisma.usuario.deleteMany({});
    console.log('✅ Usuarios eliminados');

    console.log('🗑️  Eliminando dispositivos...');
    await prisma.dispositivo.deleteMany({});
    console.log('✅ Dispositivos eliminados');

    console.log('🗑️  Eliminando notificaciones...');
    await prisma.notificacion.deleteMany({});
    console.log('✅ Notificaciones eliminadas');

    console.log('\n✅ Base de datos reseteada exitosamente');
    process.exit(0);
  } catch (error) {
    console.error('❌ Error al resetear base de datos:', error);
    process.exit(1);
  }
}

resetDatabase();
