import { PrismaClient } from '../../generated/prisma';
import { PrismaPg } from '@prisma/adapter-pg';
import { Pool } from 'pg';
import dotenv from 'dotenv';

// Asegurar que las variables de entorno estén cargadas
dotenv.config();

const prismaClientSingleton = () => {
  // Usar DIRECT_URL para la aplicación también (evita problemas con pgbouncer)
  const connectionString = process.env.DIRECT_URL || process.env.DATABASE_URL;
  
  if (!connectionString) {
    throw new Error('DATABASE_URL o DIRECT_URL no está configurada');
  }
  
  const pool = new Pool({ connectionString });
  const adapter = new PrismaPg(pool);
  
  return new PrismaClient({
    adapter,
    log: process.env.NODE_ENV === 'development' ? ['query', 'error', 'warn'] : ['error'],
  });
};

declare global {
  var prisma: undefined | ReturnType<typeof prismaClientSingleton>;
}

const prisma = globalThis.prisma ?? prismaClientSingleton();

if (process.env.NODE_ENV !== 'production') globalThis.prisma = prisma;

export { prisma };

