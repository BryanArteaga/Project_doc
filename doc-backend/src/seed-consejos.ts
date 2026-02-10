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

async function seedConsejos() {
  console.log('🌱 Iniciando seed de consejos...');

  const consejosHipertension = [
    { enfermedad: 'Hipertensión', consejo: 'Reduce el consumo de sal.' },
    { enfermedad: 'Hipertensión', consejo: 'Toma agua en vez de bebidas azucaradas.' },
    { enfermedad: 'Hipertensión', consejo: 'Evita el estrés con respiraciones profundas.' },
    { enfermedad: 'Hipertensión', consejo: 'Mantén un peso saludable.' },
    { enfermedad: 'Hipertensión', consejo: 'Realiza al menos 30 minutos de ejercicio moderado al día.' },
    { enfermedad: 'Hipertensión', consejo: 'Limita el consumo de alcohol.' },
    { enfermedad: 'Hipertensión', consejo: 'Aumenta el consumo de potasio (plátano, espinacas, aguacate).' },
    { enfermedad: 'Hipertensión', consejo: 'Deja de fumar.' },
    { enfermedad: 'Hipertensión', consejo: 'Duerme de 7 a 8 horas cada noche.' },
    { enfermedad: 'Hipertensión', consejo: 'Consume alimentos ricos en magnesio (frutos secos, legumbres).' },
    { enfermedad: 'Hipertensión', consejo: 'Monitorea tu presión arterial regularmente.' },
    { enfermedad: 'Hipertensión', consejo: 'Sigue la dieta DASH (Enfoques Dietéticos para Detener la Hipertensión).' },
    { enfermedad: 'Hipertensión', consejo: 'Evita los alimentos procesados y enlatados (altos en sodio).' },
    { enfermedad: 'Hipertensión', consejo: 'Toma la medicación exactamente como te la recetó el médico.' },
    { enfermedad: 'Hipertensión', consejo: 'Consume ajo, puede ayudar a relajar los vasos sanguíneos.' },
    { enfermedad: 'Hipertensión', consejo: 'Medita o practica yoga para controlar el estrés.' },
    { enfermedad: 'Hipertensión', consejo: 'Limita la cafeína.' },
    { enfermedad: 'Hipertensión', consejo: 'Aumenta el consumo de fibra (avena, vegetales, frutas).' },
    { enfermedad: 'Hipertensión', consejo: 'Usa hierbas y especias para sazonar en lugar de sal.' },
    { enfermedad: 'Hipertensión', consejo: 'Consulta a tu médico para revisiones periódicas.' },
  ];

  const consejosDiabetes = [
    { enfermedad: 'Diabetes', consejo: 'Controla tus niveles de glucosa regularmente.' },
    { enfermedad: 'Diabetes', consejo: 'Incluye verduras de hoja verde en cada comida.' },
    { enfermedad: 'Diabetes', consejo: 'Prefiere carbohidratos complejos como avena, quinoa sobre los simples.' },
    { enfermedad: 'Diabetes', consejo: 'Realiza al menos 30 minutos de actividad física al día.' },
    { enfermedad: 'Diabetes', consejo: 'Mantén un horario regular para tus comidas.' },
    { enfermedad: 'Diabetes', consejo: 'Evita las bebidas azucaradas y los jugos de fruta.' },
    { enfermedad: 'Diabetes', consejo: 'Consume fibra soluble, como manzana, zanahoria para ralentizar la absorción de glucosa.' },
    { enfermedad: 'Diabetes', consejo: 'Elige grasas saludables como aguacate, nueces y aceite de oliva.' },
    { enfermedad: 'Diabetes', consejo: 'Limita el consumo de alimentos ultraprocesados.' },
    { enfermedad: 'Diabetes', consejo: 'Lee las etiquetas nutricionales para identificar azúcares ocultos.' },
    { enfermedad: 'Diabetes', consejo: 'Mantente bien hidratado con agua natural.' },
    { enfermedad: 'Diabetes', consejo: 'Controla el tamaño de tus porciones.' },
    { enfermedad: 'Diabetes', consejo: 'Prefiere los edulcorantes naturales como la stevia con total moderación.' },
    { enfermedad: 'Diabetes', consejo: 'Incorpora legumbres como lentejas, frijoles y demas a tu dieta' },
    { enfermedad: 'Diabetes', consejo: 'Camina después de las comidas para reducir el pico de glucosa.' },
    { enfermedad: 'Diabetes', consejo: 'Duerme entre 7 y 8 horas por noche.' },
    { enfermedad: 'Diabetes', consejo: 'Consume pescado graso como sálmon, atun al menos dos veces por semana.' },
    { enfermedad: 'Diabetes', consejo: 'Evita el sedentarismo; levántate cada hora si trabajas sentado.' },
    { enfermedad: 'Diabetes', consejo: 'Elige frutas enteras en lugar de jugos.' },
    { enfermedad: 'Diabetes', consejo: 'Aprende a reconocer los síntomas de la hipoglucemia.' },
  ];

  const consejosObesidad = [
    { enfermedad: 'Obesidad', consejo: 'Establece metas de pérdida de peso realistas y a corto plazo.' },
    { enfermedad: 'Obesidad', consejo: 'Aumenta tu ingesta de agua a 2-3 litros al día.' },
    { enfermedad: 'Obesidad', consejo: 'Incorpora proteína magra (pollo, pescado) en cada comida para mayor saciedad.' },
    { enfermedad: 'Obesidad', consejo: 'Llena la mitad de tu plato con verduras.' },
    { enfermedad: 'Obesidad', consejo: 'Realiza ejercicio cardiovascular (caminar, nadar, ciclismo) regularmente.' },
    { enfermedad: 'Obesidad', consejo: 'Evita las dietas milagro; son peligrosas y el efecto rebote es común.' },
    { enfermedad: 'Obesidad', consejo: 'Mastica lentamente y saborea cada bocado.' },
    { enfermedad: 'Obesidad', consejo: 'Usa platos más pequeños para controlar las porciones.' },
    { enfermedad: 'Obesidad', consejo: 'Planifica tus comidas de la semana.' },
    { enfermedad: 'Obesidad', consejo: 'Prepara tus meriendas saludables con anticipación.' },
    { enfermedad: 'Obesidad', consejo: 'Limita o elimina el consumo de frituras.' },
    { enfermedad: 'Obesidad', consejo: 'Lee las etiquetas: cuidado con las calorías y azúcares en productos "light".' },
    { enfermedad: 'Obesidad', consejo: 'Duerme bien; la falta de sueño altera las hormonas del apetito.' },
    { enfermedad: 'Obesidad', consejo: 'Aumenta tu actividad diaria: usa las escaleras, camina distancias cortas.' },
    { enfermedad: 'Obesidad', consejo: 'Reduce el consumo de harinas refinadas (pan blanco, pasta blanca).' },
    { enfermedad: 'Obesidad', consejo: 'Busca apoyo en un grupo o con un profesional de la salud mental si comes por emociones.' },
    { enfermedad: 'Obesidad', consejo: 'Incorpora ejercicios de fuerza para aumentar tu masa muscular y metabolismo.' },
    { enfermedad: 'Obesidad', consejo: 'No compres comida chatarra; si no está en casa, no la comerás.' },
    { enfermedad: 'Obesidad', consejo: 'Come sentado en la mesa, sin distracciones como el televisor o el celular.' },
    { enfermedad: 'Obesidad', consejo: 'Bebe un vaso de agua antes de cada comida.' },
  ];

  const consejosColesterol = [
    { enfermedad: 'Colesterol alto', consejo: 'Reduce el consumo de grasas saturadas como carnes rojas, mantequilla, lácteos enteros, etc.' },
    { enfermedad: 'Colesterol alto', consejo: 'Elimina por completo las grasas trans productos horneados comerciales, frituras, margarinas.' },
    { enfermedad: 'Colesterol alto', consejo: 'Incorpora grasas insaturadas' },
    { enfermedad: 'Colesterol alto', consejo: 'Come más vegetales y frutas, ricos en antioxidantes.' },
    { enfermedad: 'Colesterol alto', consejo: 'Realiza al menos 150 minutos de ejercicio aeróbico a la semana.' },
    { enfermedad: 'Colesterol alto', consejo: 'Mantén un peso saludable.' },
    { enfermedad: 'Colesterol alto', consejo: 'Limita el consumo de yema de huevo a unas pocas por semana' },
    { enfermedad: 'Colesterol alto', consejo: 'Elige lácteos desnatados o bajos en grasa.' },
    { enfermedad: 'Colesterol alto', consejo: 'Retira la piel del pollo y la grasa visible de la carne.' },
    { enfermedad: 'Colesterol alto', consejo: 'Prefiere métodos de cocción al vapor, horno, plancha o hervido.' },
    { enfermedad: 'Colesterol alto', consejo: 'Lee las etiquetas: evita "aceites vegetales parcialmente hidrogenados".' },
    { enfermedad: 'Colesterol alto', consejo: 'Toma té verde, rico en antioxidantes.' },
    { enfermedad: 'Colesterol alto', consejo: 'El ajo puede tener un efecto modesto en la reducción del colesterol.' },
    { enfermedad: 'Colesterol alto', consejo: 'Modera el consumo de alcohol.' },
    { enfermedad: 'Colesterol alto', consejo: 'Mantén bajo control otras condiciones como la diabetes y la hipertensión.' },
    { enfermedad: 'Colesterol alto', consejo: 'Evita los alimentos ultraprocesados y la comida rápida.' },
    { enfermedad: 'Colesterol alto', consejo: 'Camina a paso rápido todos los días.' },
    { enfermedad: 'Colesterol alto', consejo: 'Consume probióticos para una salud intestinal óptima.' },
    { enfermedad: 'Colesterol alto', consejo: 'Agrega cúrcuma a tus comidas; la curcumina tiene beneficios cardiovasculares.' },
    { enfermedad: 'Colesterol alto', consejo: 'Sigue la dieta Mediterránea, reconocida por sus beneficios cardiovasculares.' },
  ];

  const todosConsejos = [
    ...consejosHipertension,
    ...consejosDiabetes,
    ...consejosObesidad,
    ...consejosColesterol,
  ];

  console.log(`📊 Total de consejos a insertar: ${todosConsejos.length}`);

  for (const consejo of todosConsejos) {
    try {
      await prisma.consejo.create({
        data: consejo,
      });
      console.log(`✅ Consejo: ${consejo.consejo.substring(0, 50)}...`);
    } catch (error) {
      console.error(`❌ Error insertando consejo:`, error);
    }
  }

  console.log('🎉 Seed de consejos completado!');
}

seedConsejos()
  .catch((e) => {
    console.error(e);
    process.exit(1);
  })
  .finally(async () => {
    await prisma.$disconnect();
  });
