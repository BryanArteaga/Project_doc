import { PrismaPg } from '@prisma/adapter-pg';
import { Pool } from 'pg';
import dotenv from 'dotenv';
import path from 'path';

// Import the actual client from the generated output
// eslint-disable-next-line @typescript-eslint/no-var-requires
const { PrismaClient } = require(path.join(__dirname, '../../generated/prisma/index.js'));

// Asegurar que las variables de entorno estén cargadas
dotenv.config();

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
    allowExitOnIdle: true, // Permitir que el proceso termine si no hay conexiones activas
    // Configuración adicional para mejor manejo de errores
    statement_timeout: 10000, // Timeout de queries 10s
    query_timeout: 10000, // Timeout de queries 10s
  });

  // Event handlers para debugging
  pool.on('error', (err) => {
    console.error('Error inesperado en pool de conexiones:', err);
  });

  pool.on('connect', () => {
    if (process.env.NODE_ENV === 'development') {
      console.log('✅ Nueva conexión a la base de datos establecida');
    }
  });
  
  const adapter = new PrismaPg(pool);
  
  return new PrismaClient({
    adapter,
    log: process.env.NODE_ENV === 'development' ? ['error', 'warn'] : ['error'],
  });
};

declare global {
  var prisma: undefined | ReturnType<typeof prismaClientSingleton>;
}

const prisma = globalThis.prisma ?? prismaClientSingleton();

if (process.env.NODE_ENV !== 'production') globalThis.prisma = prisma;

export { prisma };

