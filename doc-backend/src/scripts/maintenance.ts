import { prisma } from '../lib/prisma';
import { cacheService } from '../utils/cache';

/**
 * Limpiar tokens expirados de Supabase Auth
 */
async function cleanupExpiredTokens() {
  try {
    console.log('🧹 Limpiando tokens expirados...');

    // Limpiar refresh tokens antiguos (más de 7 días)
    await prisma.$executeRawUnsafe(`
      DELETE FROM auth.refresh_tokens 
      WHERE created_at < NOW() - INTERVAL '7 days'
    `);

    // Limpiar sesiones antiguas (más de 7 días)
    await prisma.$executeRawUnsafe(`
      DELETE FROM auth.sessions 
      WHERE created_at < NOW() - INTERVAL '7 days'
    `);

    // Limpiar one-time tokens antiguos (más de 1 día)
    await prisma.$executeRawUnsafe(`
      DELETE FROM auth.one_time_tokens 
      WHERE created_at < NOW() - INTERVAL '1 day'
    `);

    console.log('✅ Tokens expirados limpiados exitosamente');
  } catch (error) {
    console.error('❌ Error limpiando tokens:', error);
    throw error;
  }
}

/**
 * Obtener estadísticas de la base de datos
 */
export async function getDatabaseStats() {
  try {
    console.log('📊 Obteniendo estadísticas de la base de datos...');

    // Conexiones activas
    const connections = await prisma.$queryRawUnsafe<any[]>(`
      SELECT count(*) as count 
      FROM pg_stat_activity 
      WHERE datname = current_database()
    `);

    // Tamaño de la base de datos
    const dbSize = await prisma.$queryRawUnsafe<any[]>(`
      SELECT pg_size_pretty(pg_database_size(current_database())) as size
    `);

    // Tamaño de tablas principales
    const tableSizes = await prisma.$queryRawUnsafe<any[]>(`
      SELECT 
        tablename,
        pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) as size
      FROM pg_tables
      WHERE schemaname = 'public'
      ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC
      LIMIT 10
    `);

    // Convertir BigInt a Number para JSON.stringify
    const stats = {
      connections: Number(connections[0].count),
      databaseSize: dbSize[0].size,
      tables: tableSizes,
      cacheStats: cacheService.getStats()
    };

    console.log('📈 Estadísticas:', JSON.stringify(stats, null, 2));
    return stats;
  } catch (error) {
    console.error('❌ Error obteniendo estadísticas:', error);
    throw error;
  }
}

/**
 * Limpiar registros antiguos de salud (opcional)
 * Solo mantener los últimos 1000 registros por usuario
 */
async function cleanupOldHealthRecords() {
  try {
    console.log('🧹 Limpiando registros antiguos de salud...');

    // Obtener usuarios con muchos registros
    const users = await prisma.usuario.findMany({
      select: { id: true },
      where: {
        registrosSalud: {
          some: {}
        }
      }
    });

    let totalDeleted = 0;

    for (const user of users) {
      // Contar registros del usuario
      const count = await prisma.registroSalud.count({
        where: { usuarioId: user.id }
      });

      // Si tiene más de 1000, eliminar los más antiguos
      if (count > 1000) {
        const toDelete = count - 1000;
        
        // Obtener IDs de los registros más antiguos
        const oldRecords = await prisma.registroSalud.findMany({
          where: { usuarioId: user.id },
          orderBy: { fecha: 'asc' },
          take: toDelete,
          select: { id: true }
        });

        const ids = oldRecords.map(r => r.id);

        // Eliminar registros antiguos
        const deleted = await prisma.registroSalud.deleteMany({
          where: {
            id: { in: ids }
          }
        });

        totalDeleted += deleted.count;
        console.log(`  Usuario ${user.id}: eliminados ${deleted.count} registros antiguos`);
      }
    }

    console.log(`✅ Total de registros eliminados: ${totalDeleted}`);
  } catch (error) {
    console.error('❌ Error limpiando registros antiguos:', error);
    throw error;
  }
}

/**
 * Limpiar caché en memoria
 */
function clearCache() {
  console.log('🧹 Limpiando caché en memoria...');
  cacheService.clear();
  console.log('✅ Caché limpiado');
}

/**
 * Ejecutar todas las tareas de mantenimiento
 */
export async function runMaintenance() {
  console.log('🚀 Iniciando mantenimiento de base de datos...');
  console.log('Fecha:', new Date().toISOString());
  console.log('---');

  try {
    // 1. Limpiar tokens expirados
    await cleanupExpiredTokens();
    console.log('');

    // 2. Obtener estadísticas
    await getDatabaseStats();
    console.log('');

    // 3. Limpiar caché
    clearCache();
    console.log('');

    // 4. Opcional: Limpiar registros muy antiguos
    // Descomentar solo si es necesario
    // await cleanupOldHealthRecords();
    // console.log('');

    console.log('✅ Mantenimiento completado exitosamente');
    return true;
  } catch (error) {
    console.error('❌ Error durante el mantenimiento:', error);
    return false;
  }
}

// Si se ejecuta directamente
if (require.main === module) {
  runMaintenance()
    .then(() => {
      console.log('Saliendo...');
      process.exit(0);
    })
    .catch((error) => {
      console.error('Error fatal:', error);
      process.exit(1);
    });
}
