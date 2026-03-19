import { prisma } from './src/lib/prisma';
import * as fs from 'fs';
import * as path from 'path';

interface EjercicioData {
  id: number;
  nombre: string;
  descripcion?: string;
  enfermedad?: string;
  duracion?: number;
  intensidad?: string;
  imagenResId?: number | null;
  series: number;
  descansoEntreSeries: number;
  urlImagen?: string | null;
  urlVideo?: string | null;
}

interface ConsejoData {
  id: number;
  enfermedad: string;
  consejo: string;
}

async function seedData() {
  try {
    console.log('🌱 Iniciando seed de datos...\n');

    // Leer archivos JSON
    const ejerciciosPath = path.join('/home/brste/Descargas', 'ejercicios_rows.json');
    const consejosPath = path.join('/home/brste/Descargas', 'consejos_rows.json');

    console.log('📖 Leyendo archivos JSON...');
    const ejerciciosData = JSON.parse(fs.readFileSync(ejerciciosPath, 'utf-8')) as EjercicioData[];
    const consejosData = JSON.parse(fs.readFileSync(consejosPath, 'utf-8')) as ConsejoData[];

    console.log(`✅ Leídos ${ejerciciosData.length} ejercicios`);
    console.log(`✅ Leídos ${consejosData.length} consejos\n`);

    // Insertar ejercicios
    console.log('💪 Insertando ejercicios...');
    for (const ejercicio of ejerciciosData) {
      try {
        await prisma.ejercicio.create({
          data: {
            nombre: ejercicio.nombre,
            descripcion: ejercicio.descripcion || null,
            enfermedad: ejercicio.enfermedad || null,
            duracion: ejercicio.duracion || null,
            intensidad: ejercicio.intensidad || null,
            imagenResId: ejercicio.imagenResId || null,
            series: ejercicio.series || 3,
            descansoEntreSeries: ejercicio.descansoEntreSeries || 30,
            urlImagen: ejercicio.urlImagen || null,
            urlVideo: ejercicio.urlVideo || null,
          },
        });
      } catch (error: any) {
        console.warn(`⚠️  No se pudo insertar ejercicio "${ejercicio.nombre}": ${error.message}`);
      }
    }
    console.log(`✅ ${ejerciciosData.length} ejercicios insertados\n`);

    // Insertar consejos
    console.log('📝 Insertando consejos...');
    for (const consejo of consejosData) {
      try {
        await prisma.consejo.create({
          data: {
            enfermedad: consejo.enfermedad,
            consejo: consejo.consejo,
          },
        });
      } catch (error: any) {
        console.warn(`⚠️  No se pudo insertar consejo para "${consejo.enfermedad}": ${error.message}`);
      }
    }
    console.log(`✅ ${consejosData.length} consejos insertados\n`);

    console.log('✅ Seed completado exitosamente');
    process.exit(0);
  } catch (error) {
    console.error('❌ Error al hacer seed:', error);
    process.exit(1);
  }
}

seedData();
