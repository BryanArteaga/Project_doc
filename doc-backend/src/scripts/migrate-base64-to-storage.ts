import { PrismaPg } from '@prisma/adapter-pg';
import { Pool } from 'pg';
import path from 'path';
import dotenv from 'dotenv';
import { createClient } from '@supabase/supabase-js';

dotenv.config();

// Supabase client
const SUPABASE_URL = process.env.SUPABASE_URL;
const SUPABASE_SERVICE_KEY = process.env.SUPABASE_SERVICE_KEY;

if (!SUPABASE_URL || !SUPABASE_SERVICE_KEY) {
  throw new Error('SUPABASE_URL y SUPABASE_SERVICE_KEY no están configuradas');
}

const supabase = createClient(SUPABASE_URL, SUPABASE_SERVICE_KEY);

// Prisma setup
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

const adapter = new PrismaPg(pool);
const { PrismaClient } = require(path.join(__dirname, '../../generated/prisma/index.js'));
const prisma = new PrismaClient({
  adapter,
  log: ['error', 'warn'],
});

async function migrateBase64ToStorage() {
  console.log('🚀 Iniciando migración de imágenes base64 a Supabase Storage...\n');

  try {
    // 1️⃣ Crear bucket si no existe
    const { data: buckets } = await supabase.storage.listBuckets();
    const bucketExists = buckets?.some(b => b.name === 'ejercicios');

    if (!bucketExists) {
      console.log('📦 Creando bucket "ejercicios"...');
      await supabase.storage.createBucket('ejercicios', {
        public: true,
      });
      console.log('✅ Bucket creado\n');
    } else {
      console.log('✅ Bucket "ejercicios" ya existe\n');
    }

    // 2️⃣ Obtener todos los ejercicios
    const ejercicios = await prisma.ejercicio.findMany({
      select: {
        id: true,
        nombre: true,
        urlImagen: true,
      },
    });

    console.log(`📊 Total de ejercicios: ${ejercicios.length}\n`);

    let uploadedCount = 0;
    const errors: any[] = [];

    // 3️⃣ Procesar cada ejercicio
    for (const ejercicio of ejercicios) {
      if (!ejercicio.urlImagen) {
        console.log(`⏭️  Ejercicio #${ejercicio.id} (${ejercicio.nombre}): No tiene imagen, saltando...`);
        continue;
      }

      // Si ya es una URL (comienza con http), saltarlo
      if (ejercicio.urlImagen.startsWith('http')) {
        console.log(`✅ Ejercicio #${ejercicio.id} (${ejercicio.nombre}): Ya es URL, saltando...`);
        continue;
      }

      // Si es base64, procesarlo
      if (ejercicio.urlImagen.startsWith('data:image')) {
        try {
          console.log(`📤 Procesando ejercicio #${ejercicio.id}: ${ejercicio.nombre}...`);

          // Convertir base64 a Buffer
          const base64Data = ejercicio.urlImagen.split(',')[1];
          if (!base64Data) {
            throw new Error('No se pudo extraer datos base64');
          }

          const buffer = Buffer.from(base64Data, 'base64');

          // Detectar tipo MIME
          const mimeType = ejercicio.urlImagen.split(';')[0].split(':')[1] || 'image/jpeg';
          const extension = mimeType.split('/')[1] || 'jpg';

          // Nombre único para la imagen
          const fileName = `ejercicio-${ejercicio.id}.${extension}`;
          const filePath = `ejercicios/${fileName}`;

          // Subir a Supabase Storage
          const { data, error: uploadError } = await supabase.storage
            .from('ejercicios')
            .upload(filePath, buffer, {
              contentType: mimeType,
              upsert: true, // Sobrescribir si ya existe
            });

          if (uploadError) {
            throw uploadError;
          }

          // Obtener URL pública
          const { data: publicUrlData } = supabase.storage
            .from('ejercicios')
            .getPublicUrl(filePath);

          const publicUrl = publicUrlData.publicUrl;

          // Actualizar BD con la URL pública
          await prisma.ejercicio.update({
            where: { id: ejercicio.id },
            data: {
              urlImagen: publicUrl,
            },
          });

          console.log(`✅ Ejercicio #${ejercicio.id}: Subido a Storage`);
          console.log(`   🔗 URL: ${publicUrl}\n`);
          uploadedCount++;
        } catch (error: any) {
          const errorMsg = `❌ Error en ejercicio #${ejercicio.id}: ${error.message}`;
          console.log(errorMsg);
          errors.push({ ejercicioId: ejercicio.id, error: error.message });
        }
      } else {
        console.log(`⚠️  Ejercicio #${ejercicio.id}: Formato desconocido, saltando...\n`);
      }
    }

    // Resumen
    console.log('\n' + '='.repeat(60));
    console.log('📊 RESUMEN DE MIGRACIÓN');
    console.log('='.repeat(60));
    console.log(`✅ Imágenes subidas exitosamente: ${uploadedCount}`);
    console.log(`❌ Errores: ${errors.length}`);

    if (errors.length > 0) {
      console.log('\n⚠️  Errores encontrados:');
      errors.forEach(e => {
        console.log(`   - Ejercicio #${e.ejercicioId}: ${e.error}`);
      });
    }

    console.log('='.repeat(60) + '\n');
  } catch (error: any) {
    console.error('❌ Error fatal en migración:', error.message);
    process.exit(1);
  } finally {
    await prisma.$disconnect();
    await pool.end();
  }
}

// Ejecutar
migrateBase64ToStorage();
