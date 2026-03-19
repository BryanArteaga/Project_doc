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

// @ts-ignore - Conflicto de tipos entre pg y @prisma/adapter-pg
const adapter = new PrismaPg(pool);

const prisma = new PrismaClient({
  adapter,
  log: ['error', 'warn'],
});

async function seedEjercicios() {
  console.log('🌱 Iniciando seed de ejercicios...');

  // Ejercicios para Hipertensión
  const ejerciciosHipertension = [
    { id: 1, nombre: 'Caminata rápida', descripcion: 'Camina a paso ligero.', enfermedad: 'Hipertensión', duracion: 600, intensidad: 'Media', series: 3, descansoEntreSeries: 30 },
    { id: 2, nombre: 'Sentadillas', descripcion: 'Haz 15 sentadillas por serie.', enfermedad: 'Hipertensión', duracion: 60, intensidad: 'Media', series: 3, descansoEntreSeries: 45 },
    { id: 3, nombre: 'Subir escaleras', descripcion: 'Sube y baja escaleras.', enfermedad: 'Hipertensión', duracion: 180, intensidad: 'Media', series: 2, descansoEntreSeries: 60 },
    { id: 4, nombre: 'Marcha en el lugar', descripcion: 'Marcha en el lugar.', enfermedad: 'Hipertensión', duracion: 180, intensidad: 'Baja', series: 3, descansoEntreSeries: 30 },
    { id: 5, nombre: 'Estiramientos suaves', descripcion: 'Inclina el tronco hacia adelante para estirar espalda y piernas, lleva brazos arriba para estirar hombros. Mantén cada posición 20 segundos.', enfermedad: 'Hipertensión', duracion: 240, intensidad: 'Baja', series: 2, descansoEntreSeries: 20 },
    { id: 6, nombre: 'Elevaciones de piernas', descripcion: 'Acostado, eleva piernas alternadamente. 12 por pierna.', enfermedad: 'Hipertensión', duracion: 90, intensidad: 'Baja', series: 3, descansoEntreSeries: 30 },
    { id: 7, nombre: 'Flexiones de pared', descripcion: 'Haz 10 flexiones apoyado en la pared.', enfermedad: 'Hipertensión', duracion: 60, intensidad: 'Media', series: 3, descansoEntreSeries: 40 },
    { id: 8, nombre: 'Respiración diafragmática', descripcion: 'Siéntate en una silla con la espalda recta. Coloca una mano en el abdomen y otra en el pecho. Inhala por la nariz 4 segundos, exhala por la boca 6 segundos.', enfermedad: 'Hipertensión', duracion: 180, intensidad: 'Baja', series: 2, descansoEntreSeries: 20 },
    { id: 9, nombre: 'Puentes de glúteo', descripcion: 'Acostado, eleva caderas. 15 repeticiones.', enfermedad: 'Hipertensión', duracion: 75, intensidad: 'Media', series: 3, descansoEntreSeries: 35 },
    { id: 10, nombre: 'Plancha con apoyo de rodilla', descripcion: 'Mantén posición de plancha con rodillas apoyadas.', enfermedad: 'Hipertensión', duracion: 30, intensidad: 'Media', series: 3, descansoEntreSeries: 45 },
    { id: 11, nombre: 'Estocadas estáticas', descripcion: 'Haz 10 estocadas por pierna.', enfermedad: 'Hipertensión', duracion: 90, intensidad: 'Media', series: 3, descansoEntreSeries: 40 },
    { id: 12, nombre: 'Rotación de brazos', descripcion: 'Rota brazos hacia adelante y atrás.', enfermedad: 'Hipertensión', duracion: 90, intensidad: 'Baja', series: 2, descansoEntreSeries: 20 },
    { id: 13, nombre: 'Elevación de talones', descripcion: 'De pie, eleva talones. 20 repeticiones.', enfermedad: 'Hipertensión', duracion: 60, intensidad: 'Baja', series: 3, descansoEntreSeries: 25 },
    { id: 14, nombre: 'Caminata lenta', descripcion: 'Camina lentamente en casa mientras cuentas mentalmente tus pasos y acompasas la respiración.', enfermedad: 'Hipertensión', duracion: 300, intensidad: 'Baja', series: 2, descansoEntreSeries: 30 },
    { id: 15, nombre: 'Estiramiento de espalda', descripcion: 'Sentado, estira espalda hacia adelante.', enfermedad: 'Hipertensión', duracion: 150, intensidad: 'Baja', series: 2, descansoEntreSeries: 20 },
    { id: 16, nombre: 'Rotaciones de cuello', descripcion: 'Gira suavemente el cuello en círculos.', enfermedad: 'Hipertensión', duracion: 60, intensidad: 'Baja', series: 2, descansoEntreSeries: 15 },
    { id: 17, nombre: 'Elevaciones laterales', descripcion: 'Eleva brazos lateralmente con pesas ligeras. 12 repeticiones.', enfermedad: 'Hipertensión', duracion: 60, intensidad: 'Media', series: 3, descansoEntreSeries: 35 },
    { id: 18, nombre: 'Puntas de pies', descripcion: 'Camina de puntillas por la casa.', enfermedad: 'Hipertensión', duracion: 90, intensidad: 'Baja', series: 2, descansoEntreSeries: 20 },
    { id: 19, nombre: 'Estiramiento de hombros', descripcion: 'Estira tus hombros alternadamente.', enfermedad: 'Hipertensión', duracion: 150, intensidad: 'Baja', series: 2, descansoEntreSeries: 20 },
    { id: 20, nombre: 'Movilidad de muñecas', descripcion: 'Rota muñecas suavemente.', enfermedad: 'Hipertensión', duracion: 90, intensidad: 'Baja', series: 2, descansoEntreSeries: 15 },
  ];

  // Ejercicios para Diabetes
  const ejerciciosDiabetes = [
    { id: 21, nombre: 'Sentadillas con silla', descripcion: 'Levántate y siéntate de una silla 15 veces.', enfermedad: 'Diabetes', duracion: 60, intensidad: 'Media', series: 3, descansoEntreSeries: 45 },
    { id: 22, nombre: 'Caminata en el parque', descripcion: 'Camina 30 minutos al aire libre.', enfermedad: 'Diabetes', duracion: 600, intensidad: 'Media', series: 3, descansoEntreSeries: 30 },
    { id: 23, nombre: 'Flexiones de rodillas', descripcion: 'Flexiones apoyado en rodillas. 8 repeticiones.', enfermedad: 'Diabetes', duracion: 45, intensidad: 'Media', series: 3, descansoEntreSeries: 40 },
    { id: 24, nombre: 'Ejercicios con pesas ligeras', descripcion: 'Curles de bíceps con botellas de agua.', enfermedad: 'Diabetes', duracion: 90, intensidad: 'Media', series: 3, descansoEntreSeries: 35 },
    { id: 25, nombre: 'Puente de glúteo', descripcion: 'Acostado, eleva caderas. 15 repeticiones.', enfermedad: 'Diabetes', duracion: 60, intensidad: 'Media', series: 3, descansoEntreSeries: 35 },
    { id: 26, nombre: 'Plancha con apoyo de rodilla', descripcion: 'Plancha con rodillas apoyadas 30 segundos.', enfermedad: 'Diabetes', duracion: 30, intensidad: 'Media', series: 3, descansoEntreSeries: 45 },
    { id: 27, nombre: 'Elevaciones de talones', descripcion: 'De pie, eleva talones. 20 repeticiones.', enfermedad: 'Diabetes', duracion: 60, intensidad: 'Baja', series: 3, descansoEntreSeries: 25 },
    { id: 28, nombre: 'Marcha con rodillas altas', descripcion: 'Marcha elevando rodillas.', enfermedad: 'Diabetes', duracion: 180, intensidad: 'Media', series: 2, descansoEntreSeries: 30 },
    { id: 29, nombre: 'Respiración diafragmática', descripcion: 'Siéntate en una silla con la espalda recta. Coloca una mano en el abdomen y otra en el pecho. Inhala por la nariz 4 segundos, exhala por la boca 6 segundos.', enfermedad: 'Diabetes', duracion: 180, intensidad: 'Baja', series: 2, descansoEntreSeries: 20 },
    { id: 30, nombre: 'Rotación de cadera', descripcion: 'Rotaciones circulares de cadera.', enfermedad: 'Diabetes', duracion: 120, intensidad: 'Baja', series: 2, descansoEntreSeries: 20 },
    { id: 31, nombre: 'Elevaciones de piernas laterales', descripcion: 'Acostado de lado, eleva pierna. 15 por lado.', enfermedad: 'Diabetes', duracion: 90, intensidad: 'Media', series: 3, descansoEntreSeries: 35 },
    { id: 32, nombre: 'Flexiones de tríceps', descripcion: 'En silla, flexiona codos hacia atrás. 12 repeticiones.', enfermedad: 'Diabetes', duracion: 60, intensidad: 'Media', series: 3, descansoEntreSeries: 35 },
    { id: 33, nombre: 'Caminata con pesas o botellas', descripcion: 'Camina en el lugar con pesas o botellas ligeras.', enfermedad: 'Diabetes', duracion: 300, intensidad: 'Media', series: 3, descansoEntreSeries: 30 },
    { id: 34, nombre: 'Rotaciones de tobillos', descripcion: 'Rota tobillos en ambas direcciones para las 2 piernas.', enfermedad: 'Diabetes', duracion: 120, intensidad: 'Baja', series: 2, descansoEntreSeries: 15 },
    { id: 35, nombre: 'Ejercicios de equilibrio', descripcion: 'Mantén equilibrio sobre un pie 30 segundos.', enfermedad: 'Diabetes', duracion: 60, intensidad: 'Baja', series: 2, descansoEntreSeries: 20 },
    { id: 36, nombre: 'Estiramiento de isquiotibiales', descripcion: 'Sentado, estira piernas hacia adelante.', enfermedad: 'Diabetes', duracion: 180, intensidad: 'Baja', series: 2, descansoEntreSeries: 20 },
    { id: 37, nombre: 'Marcha con talones', descripcion: 'Camina tocando talones con manos.', enfermedad: 'Diabetes', duracion: 180, intensidad: 'Baja', series: 2, descansoEntreSeries: 20 },
  ];

  // Ejercicios para Obesidad
  const ejerciciosObesidad = [
    { id: 38, nombre: 'Caminata en el lugar', descripcion: 'Camina suavemente 15 minutos.', enfermedad: 'Obesidad', duracion: 300, intensidad: 'Baja', series: 3, descansoEntreSeries: 30 },
    { id: 39, nombre: 'Sentadillas con apoyo', descripcion: 'Sentadillas usando silla como apoyo. 10 repeticiones.', enfermedad: 'Obesidad', duracion: 60, intensidad: 'Baja', series: 3, descansoEntreSeries: 40 },
    { id: 40, nombre: 'Elevaciones de piernas', descripcion: 'Acostado, eleva piernas alternadamente. 10 por pierna.', enfermedad: 'Obesidad', duracion: 90, intensidad: 'Baja', series: 3, descansoEntreSeries: 30 },
    { id: 41, nombre: 'Marcha suave', descripcion: 'Marcha en el lugar 10 minutos.', enfermedad: 'Obesidad', duracion: 180, intensidad: 'Baja', series: 3, descansoEntreSeries: 25 },
    { id: 42, nombre: 'Puente de glúteo', descripcion: 'Elevación de caderas acostado. 3 series de 8.', enfermedad: 'Obesidad', duracion: 60, intensidad: 'Baja', series: 3, descansoEntreSeries: 30 },
    { id: 43, nombre: 'Respiración diafragmática', descripcion: 'Siéntate en una silla con la espalda recta. Coloca una mano en el abdomen y otra en el pecho. Inhala por la nariz 4 segundos, exhala por la boca 6 segundos. Repite durante 5 minutos.', enfermedad: 'Obesidad', duracion: 180, intensidad: 'Baja', series: 2, descansoEntreSeries: 15 },
    { id: 44, nombre: 'Rotación de brazos', descripcion: 'Rotaciones suaves de hombros 5 minutos.', enfermedad: 'Obesidad', duracion: 120, intensidad: 'Baja', series: 2, descansoEntreSeries: 20 },
    { id: 45, nombre: 'Step básico', descripcion: 'Subir y bajar escalón bajo 5 minutos.', enfermedad: 'Obesidad', duracion: 120, intensidad: 'Baja', series: 3, descansoEntreSeries: 30 },
    { id: 46, nombre: 'Flexiones de pared', descripcion: 'Flexiones apoyado en pared. 3 series de 5.', enfermedad: 'Obesidad', duracion: 45, intensidad: 'Baja', series: 3, descansoEntreSeries: 35 },
    { id: 47, nombre: 'Elevación de talones', descripcion: 'De pie, eleva talones. 3 series de 10.', enfermedad: 'Obesidad', duracion: 45, intensidad: 'Baja', series: 3, descansoEntreSeries: 20 },
    { id: 48, nombre: 'Estiramiento de espalda', descripcion: 'Estiramientos suaves para espalda.', enfermedad: 'Obesidad', duracion: 180, intensidad: 'Baja', series: 2, descansoEntreSeries: 20 },
    { id: 49, nombre: 'Marcha con rodillas bajas', descripcion: 'Marcha elevando rodillas levemente.', enfermedad: 'Obesidad', duracion: 150, intensidad: 'Baja', series: 2, descansoEntreSeries: 25 },
    { id: 50, nombre: 'Movimientos de brazos', descripcion: 'Circulos con brazos hacia adelante y atrás 5min.', enfermedad: 'Obesidad', duracion: 120, intensidad: 'Baja', series: 2, descansoEntreSeries: 20 },
    { id: 51, nombre: 'Estiramiento de gemelos', descripcion: 'Estira pantorrillas contra pared 3 minutos.', enfermedad: 'Obesidad', duracion: 90, intensidad: 'Baja', series: 2, descansoEntreSeries: 15 },
    { id: 52, nombre: 'Rotación de cadera sentado', descripcion: 'Sentado, gira cadera suavemente 3 minutos.', enfermedad: 'Obesidad', duracion: 90, intensidad: 'Baja', series: 2, descansoEntreSeries: 15 },
    { id: 53, nombre: 'Estiramiento de cuello', descripcion: 'Inclina cabeza suavemente hacia lados 3min.', enfermedad: 'Obesidad', duracion: 90, intensidad: 'Baja', series: 2, descansoEntreSeries: 15 },
    { id: 54, nombre: 'Respiración abdominal', descripcion: 'Acostado, respira profundamente 5 minutos.', enfermedad: 'Obesidad', duracion: 180, intensidad: 'Baja', series: 2, descansoEntreSeries: 15 },
    { id: 55, nombre: 'Movilidad de tobillos', descripcion: 'Rota tobillos suavemente 3 minutos.', enfermedad: 'Obesidad', duracion: 90, intensidad: 'Baja', series: 2, descansoEntreSeries: 15 },
    { id: 56, nombre: 'Elevaciones de talones sentado', descripcion: 'Sentado, eleva talones. 3 series de 15.', enfermedad: 'Obesidad', duracion: 60, intensidad: 'Baja', series: 3, descansoEntreSeries: 20 },
  ];

  // Ejercicios para Colesterol alto
  const ejerciciosColesterol = [
    { id: 57, nombre: 'Saltos sin impacto', descripcion: 'Simula saltos sin despegar pies del suelo.', enfermedad: 'Colesterol alto', duracion: 120, intensidad: 'Media', series: 3, descansoEntreSeries: 35 },
    { id: 58, nombre: 'Flexiones inclinadas', descripcion: 'Flexiones apoyado en mesa o pared. 10 repeticiones.', enfermedad: 'Colesterol alto', duracion: 60, intensidad: 'Media', series: 3, descansoEntreSeries: 40 },
    { id: 59, nombre: 'Step con elevación de rodillas', descripcion: 'Sube y baja escalera elevando rodillas alternadamente.', enfermedad: 'Colesterol alto', duracion: 300, intensidad: 'Media', series: 3, descansoEntreSeries: 45 },
    { id: 60, nombre: 'Patadas de glúteo', descripcion: 'De pie, patadas hacia atrás. 15 por pierna.', enfermedad: 'Colesterol alto', duracion: 90, intensidad: 'Media', series: 3, descansoEntreSeries: 40 },
    { id: 61, nombre: 'Elevaciones frontales', descripcion: 'Eleva brazos al frente con peso. 12 repeticiones.', enfermedad: 'Colesterol alto', duracion: 60, intensidad: 'Media', series: 3, descansoEntreSeries: 35 },
    { id: 62, nombre: 'Abdominales bicicleta', descripcion: 'Acostado, simula pedaleo. 20 repeticiones.', enfermedad: 'Colesterol alto', duracion: 60, intensidad: 'Media', series: 3, descansoEntreSeries: 35 },
    { id: 63, nombre: 'Jumping jacks', descripcion: 'Abre y cierra brazos y piernas saltando.', enfermedad: 'Colesterol alto', duracion: 90, intensidad: 'Media', series: 3, descansoEntreSeries: 40 },
    { id: 64, nombre: 'Caminata rápida en casa', descripcion: 'Camina a paso rápido.', enfermedad: 'Colesterol alto', duracion: 600, intensidad: 'Media', series: 3, descansoEntreSeries: 30 },
    { id: 65, nombre: 'Sentadillas', descripcion: '12 sentadillas por serie.', enfermedad: 'Colesterol alto', duracion: 60, intensidad: 'Media', series: 3, descansoEntreSeries: 45 },
    { id: 66, nombre: 'Step casero', descripcion: 'Sube y baja un escalón.', enfermedad: 'Colesterol alto', duracion: 300, intensidad: 'Media', series: 3, descansoEntreSeries: 40 },
    { id: 67, nombre: 'Estocadas', descripcion: '10 estocadas por pierna.', enfermedad: 'Colesterol alto', duracion: 75, intensidad: 'Media', series: 3, descansoEntreSeries: 40 },
    { id: 68, nombre: 'Flexiones modificadas', descripcion: 'Flexiones con rodillas apoyadas.', enfermedad: 'Colesterol alto', duracion: 60, intensidad: 'Media', series: 3, descansoEntreSeries: 40 },
    { id: 69, nombre: 'Jumping jacks suave', descripcion: 'Saltos de tijera a baja intensidad.', enfermedad: 'Colesterol alto', duracion: 90, intensidad: 'Media', series: 3, descansoEntreSeries: 35 },
    { id: 70, nombre: 'Burpees', descripcion: 'Realiza una flexion luego levantate y salta.', enfermedad: 'Colesterol alto', duracion: 90, intensidad: 'Media', series: 3, descansoEntreSeries: 50 },
    { id: 71, nombre: 'Plancha', descripcion: 'Mantén plancha 30 segundos.', enfermedad: 'Colesterol alto', duracion: 30, intensidad: 'Media', series: 3, descansoEntreSeries: 45 },
    { id: 72, nombre: 'Mountain climbers', descripcion: 'Escaladora en el suelo 30 segundos.', enfermedad: 'Colesterol alto', duracion: 30, intensidad: 'Media', series: 3, descansoEntreSeries: 40 },
  ];

  const todosEjercicios = [
    ...ejerciciosHipertension,
    ...ejerciciosDiabetes,
    ...ejerciciosObesidad,
    ...ejerciciosColesterol,
  ];

  console.log(`📊 Total de ejercicios a insertar: ${todosEjercicios.length}`);

  for (const ejercicio of todosEjercicios) {
    try {
      await prisma.ejercicio.upsert({
        where: { id: ejercicio.id },
        update: {},
        create: ejercicio,
      });
      console.log(`✅ Ejercicio ${ejercicio.id}: ${ejercicio.nombre}`);
    } catch (error) {
      console.error(`❌ Error insertando ejercicio ${ejercicio.id}:`, error);
    }
  }

  console.log('🎉 Seed completado!');
}

seedEjercicios()
  .catch((e) => {
    console.error(e);
    process.exit(1);
  })
  .finally(async () => {
    await prisma.$disconnect();
  });
