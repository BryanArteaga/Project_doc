import { PrismaPg } from '@prisma/adapter-pg';
import { Pool } from 'pg';
import dotenv from 'dotenv';
import path from 'path';

// Import the actual client from the generated output
// eslint-disable-next-line @typescript-eslint/no-var-requires
const { PrismaClient } = require(path.join(__dirname, '../../generated/prisma/index.js'));

// Asegurar que las variables de entorno estén cargadas
dotenv.config();

let healthCheckInterval: NodeJS.Timeout | null = null;
let isHealthy = true;
let reconnectAttempts = 0;
const MAX_RECONNECT_ATTEMPTS = 5;
const RECONNECT_DELAY = 5000; // 5 segundos

const prismaClientSingleton = () => {
  // Usar DIRECT_URL para la aplicación también (evita problemas con pgbouncer)
  const connectionString = process.env.DIRECT_URL || process.env.DATABASE_URL;
  
  if (!connectionString) {
    throw new Error('DATABASE_URL o DIRECT_URL no está configurada');
  }
  
  // Configurar pool de conexiones con límites agresivos para plan gratuito
  const pool = new Pool({ 
    connectionString,
    max: 5, // Reducido de 10 a 5 para ahorrar memoria
    min: 1, // Mínimo de conexiones
    idleTimeoutMillis: 30000, // Cerrar conexiones inactivas después de 30s
    connectionTimeoutMillis: 10000, // Timeout de conexión 10s (aumentado de 2s)
    allowExitOnIdle: false, // NO permitir exit si hay conexiones
    // Configuración adicional para mejor manejo de errores
    statement_timeout: 10000, // Timeout de queries 10s
    query_timeout: 10000, // Timeout de queries 10s
  });

  // ============ EVENT HANDLERS ============
  pool.on('error', (err: any) => {
    console.error('❌ Error en pool de conexiones:', {
      code: err.code,
      message: err.message,
      syscall: err.syscall,
    });
    isHealthy = false;
  });

  pool.on('connect', () => {
    isHealthy = true;
    reconnectAttempts = 0;
    if (process.env.NODE_ENV === 'development') {
      console.log('✅ Nueva conexión a la base de datos establecida');
    }
  });

  pool.on('remove', () => {
    if (process.env.NODE_ENV === 'development') {
      console.log('🔄 Conexión removida del pool');
    }
  });

  // ============ HEALTH CHECK (POLLING) ============
  const startHealthCheck = () => {
    // Limpiar health check anterior si existe
    if (healthCheckInterval) {
      clearInterval(healthCheckInterval);
    }

    // Ejecutar health check cada 30 segundos
    healthCheckInterval = setInterval(async () => {
      try {
        const client = await pool.connect();
        try {
          // Simple ping query para verificar conexión
          await client.query('SELECT 1');
          
          if (!isHealthy) {
            console.log('✅ Pool de conexiones recuperado');
            isHealthy = true;
            reconnectAttempts = 0;
          }
        } finally {
          client.release();
        }
      } catch (err: any) {
        console.warn('⚠️ Health check failed:', err.message);
        isHealthy = false;
        
        // Intentar reconectar
        if (reconnectAttempts < MAX_RECONNECT_ATTEMPTS) {
          reconnectAttempts++;
          console.log(`🔄 Intento de reconexión ${reconnectAttempts}/${MAX_RECONNECT_ATTEMPTS}...`);
          
          setTimeout(() => {
            try {
              pool.query('SELECT 1').catch(() => {
                console.error('❌ Reconexión fallida');
              });
            } catch (e) {
              console.error('❌ Error durante reconexión:', e);
            }
          }, RECONNECT_DELAY);
        }
      }
    }, 30000); // Cada 30 segundos

    // También ejecutar health check al iniciar
    pool.connect()
      .then(client => {
        client.query('SELECT 1').then(() => {
          console.log('✅ Verificación inicial de BD exitosa');
          isHealthy = true;
        }).catch(err => {
          console.error('❌ Verificación inicial de BD falló:', err.message);
          isHealthy = false;
        }).finally(() => client.release());
      })
      .catch(err => {
        console.error('❌ No se pudo conectar para health check inicial:', err.message);
        isHealthy = false;
      });
  };

  // Iniciar health check después de un pequeño delay
  setTimeout(() => startHealthCheck(), 2000);
  
  // @ts-ignore - Conflicto de tipos entre pg y @prisma/adapter-pg
  const adapter = new PrismaPg(pool);
  
  const prismaClient = new PrismaClient({
    adapter,
    log: process.env.NODE_ENV === 'development' ? ['error', 'warn'] : ['error'],
  });

  return prismaClient;
};

declare global {
  var prisma: undefined | ReturnType<typeof prismaClientSingleton>;
}

const prisma = globalThis.prisma ?? prismaClientSingleton();

if (process.env.NODE_ENV !== 'production') globalThis.prisma = prisma;

// Función para limpiar recursos
export const cleanupPrisma = async () => {
  if (healthCheckInterval) {
    clearInterval(healthCheckInterval);
    console.log('✅ Health check interval limpiado');
  }
  
  try {
    await prisma.$disconnect();
    console.log('✅ Prisma desconectado correctamente');
  } catch (err) {
    console.error('❌ Error al desconectar Prisma:', err);
  }
};

export { prisma };

