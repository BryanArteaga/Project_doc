package com.example.doc.data

import android.content.Context
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.doc.data.daos.EjercicioDao
import com.example.doc.data.entities.EjercicioEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import com.example.doc.R
import com.example.doc.data.daos.ConsejoDao
import com.example.doc.data.entities.ConsejoEntity
import timber.log.Timber

class AppDatabaseCallback(
    private val context: Context,
    private val scope: CoroutineScope
) : RoomDatabase.Callback() {

    private suspend fun repoblarSiVacio(ejercicioDao: EjercicioDao, consejoDao: ConsejoDao) {
        // FIX: Usar getCount() en lugar de cargar toda la tabla
        val ejerciciosCount = ejercicioDao.getCount()
        val consejos = consejoDao.getConsejosPorEnfermedad("Hipertensión")
        if (ejerciciosCount == 0 || consejos.isEmpty()) {
            populateInitialData(ejercicioDao, consejoDao)
        }
        
        // Corregir imagenResId incorrectos (se ejecuta siempre pero solo actualiza si es necesario)
        corregirImagenesLocales(ejercicioDao)
    }

    override fun onOpen(db: SupportSQLiteDatabase) {
        super.onOpen(db)
        scope.launch {
            val database = AppDatabase.getDatabase(context, scope)
            corregirImagenesLocales(database.ejercicioDao())
        }
    }

    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        scope.launch {
            val database = AppDatabase.getDatabase(context, scope)
            populateInitialData(database.ejercicioDao(), database.consejoDao())
        }
    }

    private suspend fun populateInitialData(ejercicioDao: EjercicioDao, consejoDao: ConsejoDao) {
        val ejerciciosHipertension = listOf(
            EjercicioEntity(enfermedad = "Hipertensión", nombre = "Caminata rápida", descripcion = "Camina a paso ligero.", duracion = 600, intensidad = "Media", imagenResId = R.drawable.ejercicio_caminar, series = 3, descansoEntreSeries = 30),
            EjercicioEntity(enfermedad = "Hipertensión", nombre = "Sentadillas", descripcion = "Haz 15 sentadillas por serie.", duracion = 60, intensidad = "Media", imagenResId = R.drawable.ejercicio_sentadilla, series = 3, descansoEntreSeries = 45),
            EjercicioEntity(enfermedad = "Hipertensión", nombre = "Subir escaleras", descripcion = "Sube y baja escaleras.", duracion = 180, intensidad = "Media", imagenResId = R.drawable.ejercicio_escalera, series = 2, descansoEntreSeries = 60),
            EjercicioEntity(enfermedad = "Hipertensión", nombre = "Marcha en el lugar", descripcion = "Marcha en el lugar.", duracion = 180, intensidad = "Baja", imagenResId = R.drawable.ejercicio_marcha, series = 3, descansoEntreSeries = 30),
            EjercicioEntity(enfermedad = "Hipertensión", nombre = "Estiramientos suaves", descripcion = "Inclina el tronco hacia adelante para estirar espalda y piernas, lleva brazos arriba para estirar hombros. Mantén cada posición 20 segundos.", duracion = 240, intensidad = "Baja", imagenResId = R.drawable.ejercicio_estiramiento, series = 2, descansoEntreSeries = 20),
            EjercicioEntity(enfermedad = "Hipertensión", nombre = "Elevaciones de piernas", descripcion = "Acostado, eleva piernas alternadamente. 12 por pierna.", duracion = 90, intensidad = "Baja", imagenResId = R.drawable.ejercicio_piernas, series = 3, descansoEntreSeries = 30),
            EjercicioEntity(enfermedad = "Hipertensión", nombre = "Flexiones de pared", descripcion = "Haz 10 flexiones apoyado en la pared.", duracion = 60, intensidad = "Media", imagenResId = R.drawable.ejercicio_flexiones, series = 3, descansoEntreSeries = 40),
            EjercicioEntity(enfermedad = "Hipertensión", nombre = "Respiración diafragmática", descripcion = "Siéntate en una silla con la espalda recta. Coloca una mano en el abdomen y otra en el pecho. Inhala por la nariz 4 segundos, exhala por la boca 6 segundos.", duracion = 180, intensidad = "Baja", imagenResId = R.drawable.ejercicio_respiracion, series = 2, descansoEntreSeries = 20),
            EjercicioEntity(enfermedad = "Hipertensión", nombre = "Puentes de glúteo", descripcion = "Acostado, eleva caderas. 15 repeticiones.", duracion = 75, intensidad = "Media", imagenResId = R.drawable.ejercicio_gluteos, series = 3, descansoEntreSeries = 35),
            EjercicioEntity(enfermedad = "Hipertensión", nombre = "Plancha con apoyo de rodilla", descripcion = "Mantén posición de plancha con rodillas apoyadas.", duracion = 30, intensidad = "Media", imagenResId = R.drawable.ejercicio_plancha, series = 3, descansoEntreSeries = 45),
            EjercicioEntity(enfermedad = "Hipertensión", nombre = "Estocadas estáticas", descripcion = "Haz 10 estocadas por pierna.", duracion = 90, intensidad = "Media", imagenResId = R.drawable.ejercicio_estocadas, series = 3, descansoEntreSeries = 40),
            EjercicioEntity(enfermedad = "Hipertensión", nombre = "Rotación de brazos", descripcion = "Rota brazos hacia adelante y atrás.", duracion = 90, intensidad = "Baja", imagenResId = R.drawable.ejercicio_brazos, series = 2, descansoEntreSeries = 20),
            EjercicioEntity(enfermedad = "Hipertensión", nombre = "Elevación de talones", descripcion = "De pie, eleva talones. 20 repeticiones.", duracion = 60, intensidad = "Baja", imagenResId = R.drawable.ejercicio_talones, series = 3, descansoEntreSeries = 25),
            EjercicioEntity(enfermedad = "Hipertensión", nombre = "Caminata lenta", descripcion = "Camina lentamente en casa mientras cuentas mentalmente tus pasos y acompasas la respiración.", duracion = 300, intensidad = "Baja", imagenResId = R.drawable.ejercicio_caminar, series = 2, descansoEntreSeries = 30),
            EjercicioEntity(enfermedad = "Hipertensión", nombre = "Estiramiento de espalda", descripcion = "Sentado, estira espalda hacia adelante.", duracion = 150, intensidad = "Baja", imagenResId = R.drawable.ejercicio_espalda, series = 2, descansoEntreSeries = 20),
            EjercicioEntity(enfermedad = "Hipertensión", nombre = "Rotaciones de cuello", descripcion = "Gira suavemente el cuello en círculos.", duracion = 60, intensidad = "Baja", imagenResId = R.drawable.ejercicio_cuello, series = 2, descansoEntreSeries = 15),
            EjercicioEntity(enfermedad = "Hipertensión", nombre = "Elevaciones laterales", descripcion = "Eleva brazos lateralmente con pesas ligeras. 12 repeticiones.", duracion = 60, intensidad = "Media", imagenResId = R.drawable.ejercicio_brazoslat, series = 3, descansoEntreSeries = 35),
            EjercicioEntity(enfermedad = "Hipertensión", nombre = "Puntas de pies", descripcion = "Camina de puntillas por la casa.", duracion = 90, intensidad = "Baja", imagenResId = R.drawable.ejercicio_puntas, series = 2, descansoEntreSeries = 20),
            EjercicioEntity(enfermedad = "Hipertensión", nombre = "Estiramiento de hombros", descripcion = "Estira tus hombros alternadamente.", duracion = 150, intensidad = "Baja", imagenResId = R.drawable.ejercicio_hombros, series = 2, descansoEntreSeries = 20),
            EjercicioEntity(enfermedad = "Hipertensión", nombre = "Movilidad de muñecas", descripcion = "Rota muñecas suavemente.", duracion = 90, intensidad = "Baja", imagenResId = R.drawable.ejercicio_munecas, series = 2, descansoEntreSeries = 15)
        )
        ejerciciosHipertension.forEach { ejercicioDao.insert(it) }

        val consejosHipertension = listOf(
            ConsejoEntity(enfermedad = "Hipertensión", consejo = "Reduce el consumo de sal."),
            ConsejoEntity(enfermedad = "Hipertensión", consejo = "Toma agua en vez de bebidas azucaradas."),
            ConsejoEntity(enfermedad = "Hipertensión", consejo = "Evita el estrés con respiraciones profundas."),
            ConsejoEntity(enfermedad = "Hipertensión", consejo = "Mantén un peso saludable."),
            ConsejoEntity(enfermedad = "Hipertensión", consejo = "Realiza al menos 30 minutos de ejercicio moderado al día."),
            ConsejoEntity(enfermedad = "Hipertensión", consejo = "Limita el consumo de alcohol."),
            ConsejoEntity(enfermedad = "Hipertensión", consejo = "Aumenta el consumo de potasio (plátano, espinacas, aguacate)."),
            ConsejoEntity(enfermedad = "Hipertensión", consejo = "Deja de fumar."),
            ConsejoEntity(enfermedad = "Hipertensión", consejo = "Duerme de 7 a 8 horas cada noche."),
            ConsejoEntity(enfermedad = "Hipertensión", consejo = "Consume alimentos ricos en magnesio (frutos secos, legumbres)."),
            ConsejoEntity(enfermedad = "Hipertensión", consejo = "Monitorea tu presión arterial regularmente."),
            ConsejoEntity(enfermedad = "Hipertensión", consejo = "Sigue la dieta DASH (Enfoques Dietéticos para Detener la Hipertensión)."),
            ConsejoEntity(enfermedad = "Hipertensión", consejo = "Evita los alimentos procesados y enlatados (altos en sodio)."),
            ConsejoEntity(enfermedad = "Hipertensión", consejo = "Toma la medicación exactamente como te la recetó el médico."),
            ConsejoEntity(enfermedad = "Hipertensión", consejo = "Consume ajo, puede ayudar a relajar los vasos sanguíneos."),
            ConsejoEntity(enfermedad = "Hipertensión", consejo = "Medita o practica yoga para controlar el estrés."),
            ConsejoEntity(enfermedad = "Hipertensión", consejo = "Limita la cafeína."),
            ConsejoEntity(enfermedad = "Hipertensión", consejo = "Aumenta el consumo de fibra (avena, vegetales, frutas)."),
            ConsejoEntity(enfermedad = "Hipertensión", consejo = "Usa hierbas y especias para sazonar en lugar de sal."),
            ConsejoEntity(enfermedad = "Hipertensión", consejo = "Consulta a tu médico para revisiones periódicas.")
        )
        consejosHipertension.forEach { consejoDao.insert(it) }

        // Ejercicios para Diabetes
        val ejerciciosDiabetes = listOf(
            EjercicioEntity(enfermedad = "Diabetes", nombre = "Sentadillas con silla", descripcion = "Levántate y siéntate de una silla 15 veces.", duracion = 60, intensidad = "Media", imagenResId = R.drawable.ejercicio_sentadilla, series = 3, descansoEntreSeries = 45),
            EjercicioEntity(enfermedad = "Diabetes", nombre = "Caminata en el parque", descripcion = "Camina 30 minutos al aire libre.", duracion = 600, intensidad = "Media", imagenResId = R.drawable.ejercicio_caminar, series = 3, descansoEntreSeries = 30),
            EjercicioEntity(enfermedad = "Diabetes", nombre = "Flexiones de rodillas", descripcion = "Flexiones apoyado en rodillas. 8 repeticiones.", duracion = 45, intensidad = "Media", imagenResId = R.drawable.ejercicio_flexionesrodillas, series = 3, descansoEntreSeries = 40),
            EjercicioEntity(enfermedad = "Diabetes", nombre = "Ejercicios con pesas ligeras", descripcion = "Curles de bíceps con botellas de agua.", duracion = 90, intensidad = "Media", imagenResId = R.drawable.ejercicio_pesas, series = 3, descansoEntreSeries = 35),
            EjercicioEntity(enfermedad = "Diabetes", nombre = "Puente de glúteo", descripcion = "Acostado, eleva caderas. 15 repeticiones.", duracion = 60, intensidad = "Media", imagenResId = R.drawable.ejercicio_gluteos, series = 3, descansoEntreSeries = 35),
            EjercicioEntity(enfermedad = "Diabetes", nombre = "Plancha con apoyo de rodilla", descripcion = "Plancha con rodillas apoyadas 30 segundos.", duracion = 30, intensidad = "Media", imagenResId = R.drawable.ejercicio_plancha, series = 3, descansoEntreSeries = 45),
            EjercicioEntity(enfermedad = "Diabetes", nombre = "Elevaciones de talones", descripcion = "De pie, eleva talones. 20 repeticiones.", duracion = 60, intensidad = "Baja", imagenResId = R.drawable.ejercicio_talones, series = 3, descansoEntreSeries = 25),
            EjercicioEntity(enfermedad = "Diabetes", nombre = "Marcha con rodillas altas", descripcion = "Marcha elevando rodillas.", duracion = 180, intensidad = "Media", imagenResId = R.drawable.ejercicio_marcha, series = 2, descansoEntreSeries = 30),
            EjercicioEntity(enfermedad = "Diabetes", nombre = "Respiración diafragmática", descripcion = "Siéntate en una silla con la espalda recta. Coloca una mano en el abdomen y otra en el pecho. Inhala por la nariz 4 segundos, exhala por la boca 6 segundos.", duracion = 180, intensidad = "Baja", imagenResId = R.drawable.ejercicio_respiracion, series = 2, descansoEntreSeries = 20),
            EjercicioEntity(enfermedad = "Diabetes", nombre = "Rotación de cadera", descripcion = "Rotaciones circulares de cadera.", duracion = 120, intensidad = "Baja", imagenResId = R.drawable.ejercicio_cadera, series = 2, descansoEntreSeries = 20),
            EjercicioEntity(enfermedad = "Diabetes", nombre = "Elevaciones de piernas laterales", descripcion = "Acostado de lado, eleva pierna. 15 por lado.", duracion = 90, intensidad = "Media", imagenResId = R.drawable.ejercicio_piernas_laterales, series = 3, descansoEntreSeries = 35),
            EjercicioEntity(enfermedad = "Diabetes", nombre = "Flexiones de tríceps", descripcion = "En silla, flexiona codos hacia atrás. 12 repeticiones.", duracion = 60, intensidad = "Media", imagenResId = R.drawable.ejercicio_triceps, series = 3, descansoEntreSeries = 35),
            EjercicioEntity(enfermedad = "Diabetes", nombre = "Caminata con pesas o botellas", descripcion = "Camina en el lugar con pesas o botellas ligeras.", duracion = 300, intensidad = "Media", imagenResId = R.drawable.ejercicio_caminar_pesas, series = 3, descansoEntreSeries = 30),
            EjercicioEntity(enfermedad = "Diabetes", nombre = "Rotaciones de tobillos", descripcion = "Rota tobillos en ambas direcciones para las 2 piernas.", duracion = 120, intensidad = "Baja", imagenResId = R.drawable.ejercicio_tobillos, series = 2, descansoEntreSeries = 15),
            EjercicioEntity(enfermedad = "Diabetes", nombre = "Ejercicios de equilibrio", descripcion = "Mantén equilibrio sobre un pie 30 segundos.", duracion = 60, intensidad = "Baja", imagenResId = R.drawable.ejercicio_equilibrio, series = 2, descansoEntreSeries = 20),
            EjercicioEntity(enfermedad = "Diabetes", nombre = "Estiramiento de isquiotibiales", descripcion = "Sentado, estira piernas hacia adelante.", duracion = 180, intensidad = "Baja", imagenResId = R.drawable.ejercicio_isquiotibiales, series = 2, descansoEntreSeries = 20),
            EjercicioEntity(enfermedad = "Diabetes", nombre = "Marcha con talones", descripcion = "Camina tocando talones con manos.", duracion = 180, intensidad = "Baja", imagenResId = R.drawable.ejercicio_marcha_talones, series = 2, descansoEntreSeries = 20)
        )
        ejerciciosDiabetes.forEach { ejercicioDao.insert(it) }

        val consejosDiabetes = listOf(
            ConsejoEntity(enfermedad = "Diabetes", consejo = "Controla tus niveles de glucosa regularmente."),
            ConsejoEntity(enfermedad = "Diabetes", consejo = "Incluye verduras de hoja verde en cada comida."),
            ConsejoEntity(enfermedad = "Diabetes", consejo = "Prefiere carbohidratos complejos como avena, quinoa sobre los simples."),
            ConsejoEntity(enfermedad = "Diabetes", consejo = "Realiza al menos 30 minutos de actividad física al día."),
            ConsejoEntity(enfermedad = "Diabetes", consejo = "Mantén un horario regular para tus comidas."),
            ConsejoEntity(enfermedad = "Diabetes", consejo = "Evita las bebidas azucaradas y los jugos de fruta."),
            ConsejoEntity(enfermedad = "Diabetes", consejo = "Consume fibra soluble, como manzana, zanahoria para ralentizar la absorción de glucosa."),
            ConsejoEntity(enfermedad = "Diabetes", consejo = "Elige grasas saludables como aguacate, nueces y aceite de oliva."),
            ConsejoEntity(enfermedad = "Diabetes", consejo = "Limita el consumo de alimentos ultraprocesados."),
            ConsejoEntity(enfermedad = "Diabetes", consejo = "Lee las etiquetas nutricionales para identificar azúcares ocultos."),
            ConsejoEntity(enfermedad = "Diabetes", consejo = "Mantente bien hidratado con agua natural."),
            ConsejoEntity(enfermedad = "Diabetes", consejo = "Controla el tamaño de tus porciones."),
            ConsejoEntity(enfermedad = "Diabetes", consejo = "Prefiere los edulcorantes naturales como la stevia con total moderación."),
            ConsejoEntity(enfermedad = "Diabetes", consejo = "Incorpora legumbres como lentejas, frijoles y demas a tu dieta"),
            ConsejoEntity(enfermedad = "Diabetes", consejo = "Camina después de las comidas para reducir el pico de glucosa."),
            ConsejoEntity(enfermedad = "Diabetes", consejo = "Duerme entre 7 y 8 horas por noche."),
            ConsejoEntity(enfermedad = "Diabetes", consejo = "Consume pescado graso como sálmon, atun al menos dos veces por semana."),
            ConsejoEntity(enfermedad = "Diabetes", consejo = "Evita el sedentarismo; levántate cada hora si trabajas sentado."),
            ConsejoEntity(enfermedad = "Diabetes", consejo = "Elige frutas enteras en lugar de jugos."),
            ConsejoEntity(enfermedad = "Diabetes", consejo = "Aprende a reconocer los síntomas de la hipoglucemia.")
        )
        consejosDiabetes.forEach { consejoDao.insert(it) }

        val ejerciciosObesidad = listOf(
            EjercicioEntity(enfermedad = "Obesidad", nombre = "Caminata en el lugar", descripcion = "Camina suavemente 15 minutos.", duracion = 300, intensidad = "Baja", imagenResId = R.drawable.ejercicio_caminar, series = 3, descansoEntreSeries = 30),
            EjercicioEntity(enfermedad = "Obesidad", nombre = "Sentadillas con apoyo", descripcion = "Sentadillas usando silla como apoyo. 10 repeticiones.", duracion = 60, intensidad = "Baja", imagenResId = R.drawable.ejercicio_sentadillapoyo, series = 3, descansoEntreSeries = 40),
            EjercicioEntity(enfermedad = "Obesidad", nombre = "Elevaciones de piernas", descripcion = "Acostado, eleva piernas alternadamente. 10 por pierna.", duracion = 90, intensidad = "Baja", imagenResId = R.drawable.ejercicio_piernas, series = 3, descansoEntreSeries = 30),
            EjercicioEntity(enfermedad = "Obesidad", nombre = "Marcha suave", descripcion = "Marcha en el lugar 10 minutos.", duracion = 180, intensidad = "Baja", imagenResId = R.drawable.ejercicio_marcha, series = 3, descansoEntreSeries = 25),
            EjercicioEntity(enfermedad = "Obesidad", nombre = "Puente de glúteo", descripcion = "Elevación de caderas acostado. 3 series de 8.", duracion = 60, intensidad = "Baja", imagenResId = R.drawable.ejercicio_gluteos, series = 3, descansoEntreSeries = 30),
            EjercicioEntity(enfermedad = "Obesidad", nombre = "Respiración diafragmática", descripcion = "Siéntate en una silla con la espalda recta. Coloca una mano en el abdomen y otra en el pecho. Inhala por la nariz 4 segundos, exhala por la boca 6 segundos. Repite durante 5 minutos.", duracion = 180, intensidad = "Baja", imagenResId = R.drawable.ejercicio_respiracion, series = 2, descansoEntreSeries = 15),
            EjercicioEntity(enfermedad = "Obesidad", nombre = "Rotación de brazos", descripcion = "Rotaciones suaves de hombros 5 minutos.", duracion = 120, intensidad = "Baja", imagenResId = R.drawable.ejercicio_brazoshombro, series = 2, descansoEntreSeries = 20),
            EjercicioEntity(enfermedad = "Obesidad", nombre = "Step básico", descripcion = "Subir y bajar escalón bajo 5 minutos.", duracion = 120, intensidad = "Baja", imagenResId = R.drawable.ejercicio_escalera, series = 3, descansoEntreSeries = 30),
            EjercicioEntity(enfermedad = "Obesidad", nombre = "Flexiones de pared", descripcion = "Flexiones apoyado en pared. 3 series de 5.", duracion = 45, intensidad = "Baja", imagenResId = R.drawable.ejercicio_flexiones, series = 3, descansoEntreSeries = 35),
            EjercicioEntity(enfermedad = "Obesidad", nombre = "Elevación de talones", descripcion = "De pie, eleva talones. 3 series de 10.", duracion = 45, intensidad = "Baja", imagenResId = R.drawable.ejercicio_talones, series = 3, descansoEntreSeries = 20),
            EjercicioEntity(enfermedad = "Obesidad", nombre = "Estiramiento de espalda", descripcion = "Estiramientos suaves para espalda.", duracion = 180, intensidad = "Baja", imagenResId = R.drawable.ejercicio_espalda, series = 2, descansoEntreSeries = 20),
            EjercicioEntity(enfermedad = "Obesidad", nombre = "Marcha con rodillas bajas", descripcion = "Marcha elevando rodillas levemente.", duracion = 150, intensidad = "Baja", imagenResId = R.drawable.ejercicio_marcha, series = 2, descansoEntreSeries = 25),
            EjercicioEntity(enfermedad = "Obesidad", nombre = "Movimientos de brazos", descripcion = "Circulos con brazos hacia adelante y atrás 5min.", duracion = 120, intensidad = "Baja", imagenResId = R.drawable.ejercicio_brazos, series = 2, descansoEntreSeries = 20),
            EjercicioEntity(enfermedad = "Obesidad", nombre = "Estiramiento de gemelos", descripcion = "Estira pantorrillas contra pared 3 minutos.", duracion = 90, intensidad = "Baja", imagenResId = R.drawable.ejercicio_gemelos, series = 2, descansoEntreSeries = 15),
            EjercicioEntity(enfermedad = "Obesidad", nombre = "Rotación de cadera sentado", descripcion = "Sentado, gira cadera suavemente 3 minutos.", duracion = 90, intensidad = "Baja", imagenResId = R.drawable.ejercicio_cadera_sentado, series = 2, descansoEntreSeries = 15),
            EjercicioEntity(enfermedad = "Obesidad", nombre = "Estiramiento de cuello", descripcion = "Inclina cabeza suavemente hacia lados 3min.", duracion = 90, intensidad = "Baja", imagenResId = R.drawable.ejercicio_cuello, series = 2, descansoEntreSeries = 15),
            EjercicioEntity(enfermedad = "Obesidad", nombre = "Respiración abdominal", descripcion = "Acostado, respira profundamente 5 minutos.", duracion = 180, intensidad = "Baja", imagenResId = R.drawable.ejercicio_respiracion_abdominal, series = 2, descansoEntreSeries = 15),
            EjercicioEntity(enfermedad = "Obesidad", nombre = "Movilidad de tobillos", descripcion = "Rota tobillos suavemente 3 minutos.", duracion = 90, intensidad = "Baja", imagenResId = R.drawable.ejercicio_tobillos, series = 2, descansoEntreSeries = 15),
            EjercicioEntity(enfermedad = "Obesidad", nombre = "Elevaciones de talones sentado", descripcion = "Sentado, eleva talones. 3 series de 15.", duracion = 60, intensidad = "Baja", imagenResId = R.drawable.ejercicio_talones_sentado, series = 3, descansoEntreSeries = 20)
        )
        ejerciciosObesidad.forEach { ejercicioDao.insert(it) }

        val consejosObesidad = listOf(
            ConsejoEntity(enfermedad = "Obesidad", consejo = "Establece metas de pérdida de peso realistas y a corto plazo."),
            ConsejoEntity(enfermedad = "Obesidad", consejo = "Aumenta tu ingesta de agua a 2-3 litros al día."),
            ConsejoEntity(enfermedad = "Obesidad", consejo = "Incorpora proteína magra (pollo, pescado) en cada comida para mayor saciedad."),
            ConsejoEntity(enfermedad = "Obesidad", consejo = "Llena la mitad de tu plato con verduras."),
            ConsejoEntity(enfermedad = "Obesidad", consejo = "Realiza ejercicio cardiovascular (caminar, nadar, ciclismo) regularmente."),
            ConsejoEntity(enfermedad = "Obesidad", consejo = "Evita las dietas milagro; son peligrosas y el efecto rebote es común."),
            ConsejoEntity(enfermedad = "Obesidad", consejo = "Mastica lentamente y saborea cada bocado."),
            ConsejoEntity(enfermedad = "Obesidad", consejo = "Usa platos más pequeños para controlar las porciones."),
            ConsejoEntity(enfermedad = "Obesidad", consejo = "Planifica tus comidas de la semana."),
            ConsejoEntity(enfermedad = "Obesidad", consejo = "Prepara tus meriendas saludables con anticipación."),
            ConsejoEntity(enfermedad = "Obesidad", consejo = "Limita o elimina el consumo de frituras."),
            ConsejoEntity(enfermedad = "Obesidad", consejo = "Lee las etiquetas: cuidado con las calorías y azúcares en productos \"light\"."),
            ConsejoEntity(enfermedad = "Obesidad", consejo = "Duerme bien; la falta de sueño altera las hormonas del apetito."),
            ConsejoEntity(enfermedad = "Obesidad", consejo = "Aumenta tu actividad diaria: usa las escaleras, camina distancias cortas."),
            ConsejoEntity(enfermedad = "Obesidad", consejo = "Reduce el consumo de harinas refinadas (pan blanco, pasta blanca)."),
            ConsejoEntity(enfermedad = "Obesidad", consejo = "Busca apoyo en un grupo o con un profesional de la salud mental si comes por emociones."),
            ConsejoEntity(enfermedad = "Obesidad", consejo = "Incorpora ejercicios de fuerza para aumentar tu masa muscular y metabolismo."),
            ConsejoEntity(enfermedad = "Obesidad", consejo = "No compres comida chatarra; si no está en casa, no la comerás."),
            ConsejoEntity(enfermedad = "Obesidad", consejo = "Come sentado en la mesa, sin distracciones como el televisor o el celular."),
            ConsejoEntity(enfermedad = "Obesidad", consejo = "Bebe un vaso de agua antes de cada comida.")
        )
        consejosObesidad.forEach { consejoDao.insert(it) }

        val ejerciciosColesterolalto = listOf(
            EjercicioEntity(enfermedad = "Colesterol alto", nombre = "Saltos sin impacto", descripcion = "Simula saltos sin despegar pies del suelo.", duracion = 120, intensidad = "Media", imagenResId = R.drawable.ejercicio_saltos_suaves, series = 3, descansoEntreSeries = 35),
            EjercicioEntity(enfermedad = "Colesterol alto", nombre = "Flexiones inclinadas", descripcion = "Flexiones apoyado en mesa o pared. 10 repeticiones.", duracion = 60, intensidad = "Media", imagenResId = R.drawable.ejercicio_flexiones_inclinadas, series = 3, descansoEntreSeries = 40),
            EjercicioEntity(enfermedad = "Colesterol alto", nombre = "Step con elevación de rodillas", descripcion = "Sube y baja escalera elevando rodillas alternadamente.", duracion = 300, intensidad = "Media", imagenResId = R.drawable.ejercicio_step_rodillas, series = 3, descansoEntreSeries = 45),
            EjercicioEntity(enfermedad = "Colesterol alto", nombre = "Patadas de glúteo", descripcion = "De pie, patadas hacia atrás. 15 por pierna.", duracion = 90, intensidad = "Media", imagenResId = R.drawable.ejercicio_patadas_gluteo, series = 3, descansoEntreSeries = 40),
            EjercicioEntity(enfermedad = "Colesterol alto", nombre = "Elevaciones frontales", descripcion = "Eleva brazos al frente con peso. 12 repeticiones.", duracion = 60, intensidad = "Media", imagenResId = R.drawable.ejercicio_elevaciones_frontales, series = 3, descansoEntreSeries = 35),
            EjercicioEntity(enfermedad = "Colesterol alto", nombre = "Abdominales bicicleta", descripcion = "Acostado, simula pedaleo. 20 repeticiones.", duracion = 60, intensidad = "Media", imagenResId = R.drawable.ejercicio_abdominales_bicicleta, series = 3, descansoEntreSeries = 35),
            EjercicioEntity(enfermedad = "Colesterol alto", nombre = "Jumping jacks", descripcion = "Abre y cierra brazos y piernas saltando.", duracion = 90, intensidad = "Media", imagenResId = R.drawable.ejercicio_jumping_modificado, series = 3, descansoEntreSeries = 40),
            EjercicioEntity(enfermedad = "Colesterol alto", nombre = "Caminata rápida en casa", descripcion = "Camina a paso rápido.", duracion = 600, intensidad = "Media", imagenResId = R.drawable.ejercicio_caminar, series = 3, descansoEntreSeries = 30),
            EjercicioEntity(enfermedad = "Colesterol alto", nombre = "Sentadillas", descripcion = "12 sentadillas por serie.", duracion = 60, intensidad = "Media", imagenResId = R.drawable.ejercicio_sentadilla, series = 3, descansoEntreSeries = 45),
            EjercicioEntity(enfermedad = "Colesterol alto", nombre = "Step casero", descripcion = "Sube y baja un escalón.", duracion = 300, intensidad = "Media", imagenResId = R.drawable.ejercicio_escalera, series = 3, descansoEntreSeries = 40),
            EjercicioEntity(enfermedad = "Colesterol alto", nombre = "Estocadas", descripcion = "10 estocadas por pierna.", duracion = 75, intensidad = "Media", imagenResId = R.drawable.ejercicio_estocadas, series = 3, descansoEntreSeries = 40),
            EjercicioEntity(enfermedad = "Colesterol alto", nombre = "Flexiones modificadas", descripcion = "Flexiones con rodillas apoyadas.", duracion = 60, intensidad = "Media", imagenResId = R.drawable.ejercicio_flexionesapoyo, series = 3, descansoEntreSeries = 40),
            EjercicioEntity(enfermedad = "Colesterol alto", nombre = "Jumping jacks suave", descripcion = "Saltos de tijera a baja intensidad.", duracion = 90, intensidad = "Media", imagenResId = R.drawable.ejercicio_jumping_modificado, series = 3, descansoEntreSeries = 35),
            EjercicioEntity(enfermedad = "Colesterol alto", nombre = "Burpees", descripcion = "Realiza una flexion luego levantate y salta.", duracion = 90, intensidad = "Media", imagenResId = R.drawable.ejercicio_burpees, series = 3, descansoEntreSeries = 50),
            EjercicioEntity(enfermedad = "Colesterol alto", nombre = "Plancha", descripcion = "Mantén plancha 30 segundos.", duracion = 30, intensidad = "Media", imagenResId = R.drawable.ejercicio_planchanormal, series = 3, descansoEntreSeries = 45),
            EjercicioEntity(enfermedad = "Colesterol alto", nombre = "Mountain climbers", descripcion = "Escaladora en el suelo 30 segundos.", duracion = 30, intensidad = "Media", imagenResId = R.drawable.ejercicio_escaladora, series = 3, descansoEntreSeries = 40)
        )
        ejerciciosColesterolalto.forEach { ejercicioDao.insert(it) }

        val consejosColesterolalto = listOf(
            ConsejoEntity(enfermedad = "Colesterol alto", consejo = "Reduce el consumo de grasas saturadas como carnes rojas, mantequilla, lácteos enteros, etc."),
            ConsejoEntity(enfermedad = "Colesterol alto", consejo = "Elimina por completo las grasas trans productos horneados comerciales, frituras, margarinas."),
            ConsejoEntity(enfermedad = "Colesterol alto", consejo = "Incorpora grasas insaturadas"),
            ConsejoEntity(enfermedad = "Colesterol alto", consejo = "Come más vegetales y frutas, ricos en antioxidantes."),
            ConsejoEntity(enfermedad = "Colesterol alto", consejo = "Realiza al menos 150 minutos de ejercicio aeróbico a la semana."),
            ConsejoEntity(enfermedad = "Colesterol alto", consejo = "Mantén un peso saludable."),
            ConsejoEntity(enfermedad = "Colesterol alto", consejo = "Limita el consumo de yema de huevo a unas pocas por semana"),
            ConsejoEntity(enfermedad = "Colesterol alto", consejo = "Elige lácteos desnatados o bajos en grasa."),
            ConsejoEntity(enfermedad = "Colesterol alto", consejo = "Retira la piel del pollo y la grasa visible de la carne."),
            ConsejoEntity(enfermedad = "Colesterol alto", consejo = "Prefiere métodos de cocción al vapor, horno, plancha o hervido."),
            ConsejoEntity(enfermedad = "Colesterol alto", consejo = "Lee las etiquetas: evita \"aceites vegetales parcialmente hidrogenados\"."),
            ConsejoEntity(enfermedad = "Colesterol alto", consejo = "Toma té verde, rico en antioxidantes."),
            ConsejoEntity(enfermedad = "Colesterol alto", consejo = "El ajo puede tener un efecto modesto en la reducción del colesterol."),
            ConsejoEntity(enfermedad = "Colesterol alto", consejo = "Modera el consumo de alcohol."),
            ConsejoEntity(enfermedad = "Colesterol alto", consejo = "Mantén bajo control otras condiciones como la diabetes y la hipertensión."),
            ConsejoEntity(enfermedad = "Colesterol alto", consejo = "Evita los alimentos ultraprocesados y la comida rápida."),
            ConsejoEntity(enfermedad = "Colesterol alto", consejo = "Camina a paso rápido todos los días."),
            ConsejoEntity(enfermedad = "Colesterol alto", consejo = "Consume probióticos para una salud intestinal óptima."),
            ConsejoEntity(enfermedad = "Colesterol alto", consejo = "Agrega cúrcuma a tus comidas; la curcumina tiene beneficios cardiovasculares."),
            ConsejoEntity(enfermedad = "Colesterol alto", consejo = "Sigue la dieta Mediterránea, reconocida por sus beneficios cardiovasculares.")
        )
        consejosColesterolalto.forEach { consejoDao.insert(it) }
    }

    private suspend fun corregirImagenesLocales(ejercicioDao: EjercicioDao) {
        // Mapa (nombre, enfermedad) → drawable correcto según el seed actual
        val mapeo: Map<Pair<String, String>, Int> = mapOf(
            // Hipertensión
            Pair("Caminata rápida",             "Hipertensión") to R.drawable.ejercicio_caminar,
            Pair("Sentadillas",                  "Hipertensión") to R.drawable.ejercicio_sentadilla,
            Pair("Subir escaleras",              "Hipertensión") to R.drawable.ejercicio_escalera,
            Pair("Marcha en el lugar",           "Hipertensión") to R.drawable.ejercicio_marcha,
            Pair("Estiramientos suaves",         "Hipertensión") to R.drawable.ejercicio_estiramiento,
            Pair("Elevaciones de piernas",       "Hipertensión") to R.drawable.ejercicio_piernas,
            Pair("Flexiones de pared",           "Hipertensión") to R.drawable.ejercicio_flexiones,
            Pair("Respiración diafragmática",    "Hipertensión") to R.drawable.ejercicio_respiracion,
            Pair("Puentes de glúteo",            "Hipertensión") to R.drawable.ejercicio_gluteos,
            Pair("Plancha con apoyo de rodilla", "Hipertensión") to R.drawable.ejercicio_plancha,
            Pair("Estocadas estáticas",          "Hipertensión") to R.drawable.ejercicio_estocadas,
            Pair("Rotación de brazos",           "Hipertensión") to R.drawable.ejercicio_brazos,
            Pair("Elevación de talones",         "Hipertensión") to R.drawable.ejercicio_talones,
            Pair("Caminata lenta",               "Hipertensión") to R.drawable.ejercicio_caminar,
            Pair("Estiramiento de espalda",      "Hipertensión") to R.drawable.ejercicio_espalda,
            Pair("Rotaciones de cuello",         "Hipertensión") to R.drawable.ejercicio_cuello,
            Pair("Elevaciones laterales",        "Hipertensión") to R.drawable.ejercicio_brazoslat,
            Pair("Puntas de pies",               "Hipertensión") to R.drawable.ejercicio_puntas,
            Pair("Estiramiento de hombros",      "Hipertensión") to R.drawable.ejercicio_hombros,
            Pair("Movilidad de muñecas",         "Hipertensión") to R.drawable.ejercicio_munecas,
            // Diabetes
            Pair("Sentadillas con silla",              "Diabetes") to R.drawable.ejercicio_sentadilla,
            Pair("Caminata en el parque",              "Diabetes") to R.drawable.ejercicio_caminar,
            Pair("Flexiones de rodillas",              "Diabetes") to R.drawable.ejercicio_flexionesrodillas,
            Pair("Ejercicios con pesas ligeras",       "Diabetes") to R.drawable.ejercicio_pesas,
            Pair("Puente de glúteo",                   "Diabetes") to R.drawable.ejercicio_gluteos,
            Pair("Plancha con apoyo de rodilla",       "Diabetes") to R.drawable.ejercicio_plancha,
            Pair("Elevaciones de talones",             "Diabetes") to R.drawable.ejercicio_talones,
            Pair("Marcha con rodillas altas",          "Diabetes") to R.drawable.ejercicio_marcha,
            Pair("Respiración diafragmática",          "Diabetes") to R.drawable.ejercicio_respiracion,
            Pair("Rotación de cadera",                 "Diabetes") to R.drawable.ejercicio_cadera,
            Pair("Elevaciones de piernas laterales",   "Diabetes") to R.drawable.ejercicio_piernas_laterales,
            Pair("Flexiones de tríceps",               "Diabetes") to R.drawable.ejercicio_triceps,
            Pair("Caminata con pesas o botellas",      "Diabetes") to R.drawable.ejercicio_caminar_pesas,
            Pair("Rotaciones de tobillos",             "Diabetes") to R.drawable.ejercicio_tobillos,
            Pair("Ejercicios de equilibrio",           "Diabetes") to R.drawable.ejercicio_equilibrio,
            Pair("Estiramiento de isquiotibiales",     "Diabetes") to R.drawable.ejercicio_isquiotibiales,
            Pair("Marcha con talones",                 "Diabetes") to R.drawable.ejercicio_marcha_talones,
            // Obesidad
            Pair("Caminata en el lugar",           "Obesidad") to R.drawable.ejercicio_caminar,
            Pair("Sentadillas con apoyo",           "Obesidad") to R.drawable.ejercicio_sentadillapoyo,
            Pair("Elevaciones de piernas",          "Obesidad") to R.drawable.ejercicio_piernas,
            Pair("Marcha suave",                    "Obesidad") to R.drawable.ejercicio_marcha,
            Pair("Puente de glúteo",                "Obesidad") to R.drawable.ejercicio_gluteos,
            Pair("Respiración diafragmática",       "Obesidad") to R.drawable.ejercicio_respiracion,
            Pair("Rotación de brazos",              "Obesidad") to R.drawable.ejercicio_brazoshombro,
            Pair("Step básico",                     "Obesidad") to R.drawable.ejercicio_escalera,
            Pair("Flexiones de pared",              "Obesidad") to R.drawable.ejercicio_flexiones,
            Pair("Elevación de talones",            "Obesidad") to R.drawable.ejercicio_talones,
            Pair("Estiramiento de espalda",         "Obesidad") to R.drawable.ejercicio_espalda,
            Pair("Marcha con rodillas bajas",       "Obesidad") to R.drawable.ejercicio_marcha,
            Pair("Movimientos de brazos",           "Obesidad") to R.drawable.ejercicio_brazos,
            Pair("Estiramiento de gemelos",         "Obesidad") to R.drawable.ejercicio_gemelos,
            Pair("Rotación de cadera sentado",      "Obesidad") to R.drawable.ejercicio_cadera_sentado,
            Pair("Estiramiento de cuello",          "Obesidad") to R.drawable.ejercicio_cuello,
            Pair("Respiración abdominal",           "Obesidad") to R.drawable.ejercicio_respiracion_abdominal,
            Pair("Movilidad de tobillos",           "Obesidad") to R.drawable.ejercicio_tobillos,
            Pair("Elevaciones de talones sentado",  "Obesidad") to R.drawable.ejercicio_talones_sentado,
            // Colesterol alto
            Pair("Saltos sin impacto",               "Colesterol alto") to R.drawable.ejercicio_saltos_suaves,
            Pair("Flexiones inclinadas",             "Colesterol alto") to R.drawable.ejercicio_flexiones_inclinadas,
            Pair("Step con elevación de rodillas",   "Colesterol alto") to R.drawable.ejercicio_step_rodillas,
            Pair("Patadas de glúteo",                "Colesterol alto") to R.drawable.ejercicio_patadas_gluteo,
            Pair("Elevaciones frontales",            "Colesterol alto") to R.drawable.ejercicio_elevaciones_frontales,
            Pair("Abdominales bicicleta",            "Colesterol alto") to R.drawable.ejercicio_abdominales_bicicleta,
            Pair("Jumping jacks",                    "Colesterol alto") to R.drawable.ejercicio_jumping_modificado,
            Pair("Caminata rápida en casa",          "Colesterol alto") to R.drawable.ejercicio_caminar,
            Pair("Sentadillas",                      "Colesterol alto") to R.drawable.ejercicio_sentadilla,
            Pair("Step casero",                      "Colesterol alto") to R.drawable.ejercicio_escalera,
            Pair("Estocadas",                        "Colesterol alto") to R.drawable.ejercicio_estocadas,
            Pair("Flexiones modificadas",            "Colesterol alto") to R.drawable.ejercicio_flexionesapoyo,
            Pair("Jumping jacks suave",              "Colesterol alto") to R.drawable.ejercicio_jumping_modificado,
            Pair("Burpees",                          "Colesterol alto") to R.drawable.ejercicio_burpees,
            Pair("Plancha",                          "Colesterol alto") to R.drawable.ejercicio_planchanormal,
            Pair("Mountain climbers",                "Colesterol alto") to R.drawable.ejercicio_escaladora
        )

        val todos = ejercicioDao.getAll(limit = 200)  // FIX: Cargar con limite, no todos
        Timber.d("Verificando ${todos.size} ejercicios para correccion...")
        val aActualizar = mutableListOf<EjercicioEntity>()
        todos.forEach { ejercicio ->
            // Solo corregir ejercicios locales (sin imagen de backend/Storage)
            if (ejercicio.urlImagen.isNullOrEmpty()) {
                val correctId = mapeo[Pair(ejercicio.nombre, ejercicio.enfermedad)]
                if (correctId != null && ejercicio.imagenResId != correctId) {
                    aActualizar.add(ejercicio.copy(imagenResId = correctId))
                    Timber.d("🔧 Corrigiendo imagen: '${ejercicio.nombre}' (${ejercicio.enfermedad}) ${ejercicio.imagenResId} → $correctId")
                }
            }
        }
        if (aActualizar.isNotEmpty()) {
            aActualizar.forEach { ejercicioDao.update(it) }
            Timber.d("✅ Corregidas ${aActualizar.size} imágenes de ejercicios locales")
        } else {
            Timber.d("⏭️ No hay imágenes que corregir")
        }
    }
}