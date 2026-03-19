import { PrismaPg } from '@prisma/adapter-pg';
import { Pool } from 'pg';
import path from 'path';
import dotenv from 'dotenv';

dotenv.config();

const connectionString = process.env.DIRECT_URL || process.env.DATABASE_URL;
if (!connectionString) {
  throw new Error('DATABASE_URL o DIRECT_URL no está configurada');
}

const pool = new Pool({
  connectionString,
  max: 5,
  min: 1,
  idleTimeoutMillis: 20000,
  connectionTimeoutMillis: 2000,
  allowExitOnIdle: true,
});

// @ts-ignore - Conflicto de tipos entre pg y @prisma/adapter-pg
const adapter = new PrismaPg(pool);
const { PrismaClient } = require(path.join(__dirname, '../../generated/prisma/index.js'));
const prisma = new PrismaClient({
  adapter,
  log: ['error', 'warn'],
});

/**
 * Script para verificar/corregir imágenes de ejercicios
 * 
 * Usos:
 * npx ts-node src/scripts/fix-exercise-images.ts --list
 *   → Listar todos los ejercicios y sus URLs de imagen
 *
 * npx ts-node src/scripts/fix-exercise-images.ts --update <ejercicioId> <nuevoURL>
 *   → Actualizar la imagen de un ejercicio específico
 *
 * npx ts-node src/scripts/fix-exercise-images.ts --clear-null
 *   → Limpiar ejercicios con urlImagen = NULL
 */

const args = process.argv.slice(2);
const command = args[0];

async function listExercises() {
  console.log('\n📋 LISTA DE EJERCICIOS Y SUS IMÁGENES\n');
  console.log('=' .repeat(100));

  const ejercicios = await prisma.ejercicio.findMany({
    select: {
      id: true,
      nombre: true,
      enfermedad: true,
      urlImagen: true,
    },
    orderBy: { id: 'asc' },
  });

  ejercicios.forEach((ej: any, idx: number) => {
    const url = ej.urlImagen || '(sin imagen)';
    const urlDisplay = url.length > 60 ? url.substring(0, 60) + '...' : url;
    console.log(`#${String(ej.id).padEnd(3)} | ${ej.nombre.padEnd(40)} | ${ej.enfermedad.padEnd(15)} | ${urlDisplay}`);
  });

  console.log('=' .repeat(100) + '\n');
}

async function updateExerciseImage(ejercicioId: number, newUrl: string) {
  console.log(`\n🔄 Actualizando ejercicio #${ejercicioId}...`);

  const ejercicio = await prisma.ejercicio.findUnique({
    where: { id: ejercicioId },
  });

  if (!ejercicio) {
    console.log(`❌ Ejercicio #${ejercicioId} no encontrado\n`);
    return;
  }

  console.log(`📝 Antiguo URL: ${ejercicio.urlImagen?.substring(0, 60) || '(sin imagen)'}`);
  console.log(`📝 Nuevo URL: ${newUrl.substring(0, 60)}\n`);

  await prisma.ejercicio.update({
    where: { id: ejercicioId },
    data: {
      urlImagen: newUrl,
    },
  });

  console.log(`✅ Actualizado exitosamente\n`);
}

async function clearNullImages() {
  console.log('\n🗑️  Limpiando ejercicios sin imagen...\n');

  const ejerciciosSinImagen = await prisma.ejercicio.findMany({
    where: {
      urlImagen: null,
    },
    select: {
      id: true,
      nombre: true,
    },
  });

  console.log(`📊 Encontrados ${ejerciciosSinImagen.length} ejercicios sin imagen:\n`);

  ejerciciosSinImagen.forEach((ej: any) => {
    console.log(`   - #${ej.id}: ${ej.nombre}`);
  });

  console.log('\n💡 Estos ejercicios pueden necesitar una imagen asignada desde el dashboard\n');
}

async function main() {
  try {
    if (!command || command === '--help') {
      console.log(`
📚 Script de corrección de imágenes de ejercicios

Uso:
  npx ts-node src/scripts/fix-exercise-images.ts [COMANDO] [ARGS]

Comandos:
  --list              Listar todos los ejercicios y sus URLs de imagen
  --update <id> <url> Actualizar la imagen de un ejercicio específico
  --clear-null        Mostrar ejercicios sin imagen
  --help              Mostrar esta ayuda
      `);
      return;
    }

    if (command === '--list') {
      await listExercises();
    } else if (command === '--update' && args[1] && args[2]) {
      const ejercicioId = parseInt(args[1]);
      const newUrl = args[2];
      await updateExerciseImage(ejercicioId, newUrl);
    } else if (command === '--clear-null') {
      await clearNullImages();
    } else {
      console.log('❌ Comando no reconocido. Usa --help para ayuda\n');
    }
  } catch (error: any) {
    console.error('❌ Error:', error.message);
  } finally {
    await prisma.$disconnect();
    await pool.end();
  }
}

main();
