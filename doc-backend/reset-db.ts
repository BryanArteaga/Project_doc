import { prisma } from './src/lib/prisma';

async function resetDatabase() {
  try {
    console.log('🔄 Reseteando base de datos...\n');

    // Eliminar datos en orden de dependencias (por foreign keys)
    console.log('🗑️  Eliminando ejercicios realizados...');
    await prisma.ejercicioRealizado.deleteMany({});
    console.log('✅ Ejercicios realizados eliminados');

    console.log('🗑️  Eliminando asignaciones de ejercicios...');
    await prisma.asignacionEjercicio.deleteMany({});
    console.log('✅ Asignaciones de ejercicios eliminadas');

    console.log('🗑️  Eliminando registros de salud...');
    await prisma.registroSalud.deleteMany({});
    console.log('✅ Registros de salud eliminados');

    console.log('🗑️  Eliminando notificaciones...');
    await prisma.notificacion.deleteMany({});
    console.log('✅ Notificaciones eliminadas');

    console.log('🗑️  Eliminando dispositivos...');
    await prisma.dispositivo.deleteMany({});
    console.log('✅ Dispositivos eliminados');

    console.log('🗑️  Eliminando usuarios...');
    await prisma.usuario.deleteMany({});
    console.log('✅ Usuarios eliminados');

    console.log('🗑️  Eliminando doctores...');
    await prisma.doctor.deleteMany({});
    console.log('✅ Doctores eliminados');

    console.log('🗑️  Eliminando ejercicios...');
    await prisma.ejercicio.deleteMany({});
    console.log('✅ Ejercicios eliminados');

    console.log('🗑️  Eliminando consejos...');
    await prisma.consejo.deleteMany({});
    console.log('✅ Consejos eliminados');

    // ✅ RESETEAR SECUENCIAS DE IDS EN POSTGRESQL
    console.log('\n🔄 Reseteando secuencias de IDs...');
    try {
      // Resetear ALL sequences en el schema public
      // IMPORTANTE: RESTART WITH 0 hace que el siguiente ID sea 1
      await prisma.$executeRawUnsafe(`
        SELECT setval(pg_get_serial_sequence(tablename, 'id'), 0)
        FROM pg_tables 
        WHERE schemaname = 'public' AND tablename IN (
          'usuarios', 'doctores', 'registros_salud', 'ejercicios', 
          'asignaciones_ejercicios', 'dispositivo', 'notificacion', 'consejos'
        );
      `);
      console.log('✅ Secuencias reseteadas - próximo ID será 1');
    } catch (seqError: any) {
      console.warn(`⚠️  No se pudieron resetear todas las secuencias: ${seqError.message}`);
      console.log('📝 Intentando resetear secuencias individuales...');
      
      const sequences = [
        'usuarios_id_seq',
        'doctores_id_seq', 
        'registros_salud_id_seq',
        'ejercicios_id_seq',
        'asignaciones_ejercicios_id_seq',
        'ejercicios_realizados_id_seq',
        'dispositivos_id_seq',
        'notificaciones_id_seq',
        'consejos_id_seq'
      ];

      for (const seq of sequences) {
        try {
          await prisma.$executeRawUnsafe(`ALTER SEQUENCE ${seq} RESTART;`);
          console.log(`✅ ${seq} reseteada`);
        } catch (e: any) {
          console.warn(`⚠️  ${seq} no existe o no se pudo resetear`);
        }
      }
    }

    console.log('\n✅ Base de datos reseteada exitosamente - TODO limpio');
    console.log('📝 Los IDs ahora empezarán desde 1\n');
    process.exit(0);
  } catch (error) {
    console.error('❌ Error al resetear base de datos:', error);
    process.exit(1);
  }
}

resetDatabase();
