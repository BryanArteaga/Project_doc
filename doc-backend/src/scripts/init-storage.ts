/**
 * Script para inicializar el storage de Supabase
 * Crea el bucket 'ejercicios' si no existe
 * 
 * Uso: npm run init:storage
 */

import { createClient } from '@supabase/supabase-js';
import * as dotenv from 'dotenv';

dotenv.config();

const supabaseUrl = process.env.SUPABASE_URL;
const supabaseServiceKey = process.env.SUPABASE_SERVICE_KEY;

if (!supabaseUrl || !supabaseServiceKey) {
  console.error('❌ SUPABASE_URL o SUPABASE_SERVICE_KEY no definidas en .env');
  process.exit(1);
}

// Crear cliente admin con service role key
const supabaseAdmin = createClient(supabaseUrl, supabaseServiceKey, {
  auth: {
    autoRefreshToken: false,
    persistSession: false,
  },
});

async function initializeStorage() {
  try {
    console.log('🔍 Verificando buckets...');

    // Listar buckets existentes
    const { data: buckets, error: listError } = await supabaseAdmin.storage.listBuckets();

    if (listError) {
      console.error('❌ Error al listar buckets:', listError);
      process.exit(1);
    }

    console.log('📦 Buckets existentes:', buckets?.map(b => b.name) || []);

    // Verificar si el bucket 'ejercicios' existe
    const bucketExists = buckets?.some(b => b.name === 'ejercicios');

    if (bucketExists) {
      console.log('✅ Bucket "ejercicios" ya existe');
      return;
    }

    // Crear bucket 'ejercicios'
    console.log('📝 Creando bucket "ejercicios"...');
    
    const { data, error: createError } = await supabaseAdmin.storage.createBucket(
      'ejercicios',
      {
        public: true,
        fileSizeLimit: 5242880, // 5MB
      }
    );

    if (createError) {
      console.error('❌ Error al crear bucket:', createError);
      process.exit(1);
    }

    console.log('✅ Bucket "ejercicios" creado exitosamente');
    console.log('📊 Detalles:', data);

  } catch (error: any) {
    console.error('❌ Error en initializeStorage:', error);
    process.exit(1);
  }
}

initializeStorage().then(() => {
  console.log('\n🎉 Inicialización de storage completada');
  process.exit(0);
});
