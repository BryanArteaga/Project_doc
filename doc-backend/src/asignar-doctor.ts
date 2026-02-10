import { PrismaPg } from '@prisma/adapter-pg';
import { Pool } from 'pg';
import path from 'path';
import dotenv from 'dotenv';

dotenv.config();

// Import from generated client
// eslint-disable-next-line @typescript-eslint/no-var-requires
const { PrismaClient } = require(path.join(__dirname, '../generated/prisma/index.js'));

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

const prisma = new PrismaClient({
  adapter,
  log: ['error', 'warn'],
});

async function asignarDoctorAUsuarios() {
  console.log('🔧 Actualizando usuarios sin doctor asignado...');

  // ID del doctor por defecto (2 = doctor, 1 = admin)
  const defaultDoctorId = parseInt(process.env.DEFAULT_DOCTOR_ID || '2');

  // Buscar usuarios que tienen idDoctorAsignado = 1 (admin) o null
  const usuariosSinDoctor = await prisma.usuario.findMany({
    where: {
      OR: [
        { idDoctorAsignado: null },
        { idDoctorAsignado: 1 },  // Reasignar los que están con admin
      ]
    },
    select: {
      id: true,
      nombre: true,
      apellido: true,
    },
  });

  console.log(`📊 Encontrados ${usuariosSinDoctor.length} usuarios para reasignar`);

  if (usuariosSinDoctor.length === 0) {
    console.log('✅ No hay usuarios para reasignar');
    return;
  }

  // Actualizar todos a la vez
  const resultado = await prisma.usuario.updateMany({
    where: {
      OR: [
        { idDoctorAsignado: null },
        { idDoctorAsignado: 1 },
      ]
    },
    data: {
      idDoctorAsignado: defaultDoctorId,
    },
  });

  console.log(`✅ ${resultado.count} usuarios actualizados con Doctor ID: ${defaultDoctorId}`);
  
  usuariosSinDoctor.forEach((usuario: any) => {
    console.log(`   - ${usuario.nombre} ${usuario.apellido} (ID: ${usuario.id})`);
  });

  console.log('🎉 Actualización completada!');
}

asignarDoctorAUsuarios()
  .catch((e) => {
    console.error(e);
    process.exit(1);
  })
  .finally(async () => {
    await prisma.$disconnect();
  });
