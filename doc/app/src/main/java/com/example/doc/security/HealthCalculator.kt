package com.example.doc.security

import com.example.doc.model.RegistroSalud
import com.example.doc.model.Usuario
import timber.log.Timber

/**
 * Calculador automático del estado de salud basado en rangos médicos
 */
object HealthCalculator {

    /**
     * Resultado de evaluación de salud
     */
    data class HealthEvaluation(
        val estado: EstadoSalud,
        val mensaje: String,
        val detalles: List<String>,
        val color: String,
        val recomendaciones: List<String>
    )

    /**
     * Estados de salud posibles
     */
    enum class EstadoSalud {
        OPTIMO,      // Verde - Todo en rangos ideales
        BUENO,       // Verde claro - Valores normales
        PRECAUCION,  // Amarillo - Valores en límite
        RIESGO,      // Naranja - Valores fuera de rango
        PELIGRO      // Rojo - Valores críticos
    }

    /**
     * Rangos de valores para Diabetes
     */
    object RangosDiabetes {
        // Glucosa en ayunas (mg/dL)
        const val GLUCOSA_OPTIMA_MIN = 70.0
        const val GLUCOSA_OPTIMA_MAX = 100.0
        const val GLUCOSA_NORMAL_MAX = 125.0
        const val GLUCOSA_PREDIABETES_MAX = 199.0
        const val GLUCOSA_DIABETES_MIN = 200.0

        // Colesterol (mg/dL)
        const val COLESTEROL_OPTIMO_MAX = 200.0
        const val COLESTEROL_NORMAL_MAX = 239.0
        const val COLESTEROL_ALTO_MIN = 240.0
    }

    /**
     * Rangos de valores para Hipertensión
     */
    object RangosHipertension {
        // Presión sistólica (mmHg)
        const val SISTOLICA_OPTIMA_MAX = 120
        const val SISTOLICA_NORMAL_MAX = 129
        const val SISTOLICA_ELEVADA_MAX = 139
        const val SISTOLICA_HIPERTENSION_1_MAX = 179
        const val SISTOLICA_CRISIS = 180

        // Presión diastólica (mmHg)
        const val DIASTOLICA_OPTIMA_MAX = 80
        const val DIASTOLICA_NORMAL_MAX = 84
        const val DIASTOLICA_ELEVADA_MAX = 89
        const val DIASTOLICA_HIPERTENSION_1_MAX = 119
        const val DIASTOLICA_CRISIS = 120
    }

    /**
     * Rangos de IMC
     */
    object RangosIMC {
        const val BAJO_PESO_MAX = 18.5
        const val NORMAL_MAX = 24.9
        const val SOBREPESO_MAX = 29.9
        const val OBESIDAD_1_MAX = 34.9
        const val OBESIDAD_2_MAX = 39.9
        // > 40 = Obesidad mórbida
    }

    /**
     * Evalúa el estado de salud completo
     */
    fun evaluarSalud(usuario: Usuario, registro: RegistroSalud): HealthEvaluation {
        val detalles = mutableListOf<String>()
        val recomendaciones = mutableListOf<String>()
        var estadoGeneral = EstadoSalud.OPTIMO
        var puntajeTotal = 0
        var factoresRiesgo = 0

        // Evaluar según enfermedad
        when (usuario.enfermedad.lowercase()) {
            "diabetes" -> {
                val evalGlucosa = evaluarGlucosa(registro.glucosa)
                detalles.add(evalGlucosa.mensaje)
                recomendaciones.addAll(evalGlucosa.recomendaciones)
                if (evalGlucosa.estado.ordinal > estadoGeneral.ordinal) {
                    estadoGeneral = evalGlucosa.estado
                }
                puntajeTotal += evalGlucosa.estado.ordinal
                if (evalGlucosa.estado.ordinal >= EstadoSalud.PRECAUCION.ordinal) factoresRiesgo++

                val evalColesterol = evaluarColesterol(registro.colesterol)
                detalles.add(evalColesterol.mensaje)
                recomendaciones.addAll(evalColesterol.recomendaciones)
                if (evalColesterol.estado.ordinal > estadoGeneral.ordinal) {
                    estadoGeneral = evalColesterol.estado
                }
                puntajeTotal += evalColesterol.estado.ordinal
                if (evalColesterol.estado.ordinal >= EstadoSalud.PRECAUCION.ordinal) factoresRiesgo++
            }

            "hipertension", "hipertensión" -> {
                val evalPresion = evaluarPresion(registro.presionArterial)
                detalles.add(evalPresion.mensaje)
                recomendaciones.addAll(evalPresion.recomendaciones)
                if (evalPresion.estado.ordinal > estadoGeneral.ordinal) {
                    estadoGeneral = evalPresion.estado
                }
                puntajeTotal += evalPresion.estado.ordinal
                if (evalPresion.estado.ordinal >= EstadoSalud.PRECAUCION.ordinal) factoresRiesgo++
            }

            else -> {
                // Evaluación general
                detalles.add("Evaluación general de salud")
                estadoGeneral = EstadoSalud.BUENO
            }
        }

        // Evaluar IMC (para todos)
        registro.imc?.let {
            val evalIMC = evaluarIMC(it)
            detalles.add(evalIMC.mensaje)
            recomendaciones.addAll(evalIMC.recomendaciones)
            if (evalIMC.estado.ordinal > EstadoSalud.BUENO.ordinal &&
                evalIMC.estado.ordinal > estadoGeneral.ordinal) {
                estadoGeneral = evalIMC.estado
            }
            if (evalIMC.estado.ordinal >= EstadoSalud.PRECAUCION.ordinal) factoresRiesgo++
        }

        // Evaluar actividad física
        if (registro.ejercicio < 30) {
            detalles.add("⚠️ Ejercicio insuficiente: ${registro.ejercicio} minutos (recomendado: 30+ min)")
            recomendaciones.add("Aumentar actividad física a al menos 30 minutos diarios")
            factoresRiesgo++
        } else {
            detalles.add("✅ Ejercicio adecuado: ${registro.ejercicio} minutos")
        }

        // Mensaje general según factores de riesgo
        val mensajeGeneral = when {
            estadoGeneral == EstadoSalud.PELIGRO -> "🚨 SITUACIÓN CRÍTICA - Consulte a su médico INMEDIATAMENTE"
            estadoGeneral == EstadoSalud.RIESGO -> "⚠️ RIESGO ALTO - Contacte a su médico pronto"
            factoresRiesgo >= 2 -> "⚠️ Múltiples factores de riesgo detectados"
            estadoGeneral == EstadoSalud.PRECAUCION -> "⚠️ Precaución - Monitorear valores"
            estadoGeneral == EstadoSalud.BUENO -> "✅ Estado de salud bueno"
            else -> "✅ Estado de salud óptimo"
        }

        val color = when (estadoGeneral) {
            EstadoSalud.OPTIMO -> "#4CAF50"      // Verde
            EstadoSalud.BUENO -> "#8BC34A"       // Verde claro
            EstadoSalud.PRECAUCION -> "#FFC107"  // Amarillo
            EstadoSalud.RIESGO -> "#FF9800"      // Naranja
            EstadoSalud.PELIGRO -> "#F44336"     // Rojo
        }

        Timber.d("Evaluación completa - Estado: $estadoGeneral, Factores de riesgo: $factoresRiesgo")

        return HealthEvaluation(
            estado = estadoGeneral,
            mensaje = mensajeGeneral,
            detalles = detalles,
            color = color,
            recomendaciones = recomendaciones.distinct()
        )
    }

    /**
     * Evalúa niveles de glucosa
     */
    private fun evaluarGlucosa(glucosa: Int?): HealthEvaluation {
        if (glucosa == null) {
            return HealthEvaluation(
                estado = EstadoSalud.BUENO,
                mensaje = "ℹ️ Glucosa no registrada",
                detalles = emptyList(),
                color = "#9E9E9E",
                recomendaciones = listOf("Registre su nivel de glucosa para mejor seguimiento")
            )
        }

        val glucosaDouble = glucosa.toDouble()
        return when {
            glucosaDouble < 70 -> HealthEvaluation(
                estado = EstadoSalud.PELIGRO,
                mensaje = "🚨 HIPOGLUCEMIA: $glucosa mg/dL (muy bajo)",
                detalles = listOf("Nivel crítico bajo de glucosa"),
                color = "#F44336",
                recomendaciones = listOf(
                    "Consumir azúcar o jugo inmediatamente",
                    "Verificar nivel en 15 minutos",
                    "Contactar a su médico si persiste"
                )
            )
            glucosaDouble <= RangosDiabetes.GLUCOSA_OPTIMA_MAX -> HealthEvaluation(
                estado = EstadoSalud.OPTIMO,
                mensaje = "✅ Glucosa óptima: $glucosa mg/dL",
                detalles = listOf("Nivel de glucosa en rango ideal"),
                color = "#4CAF50",
                recomendaciones = listOf("Mantener hábitos saludables")
            )
            glucosaDouble <= RangosDiabetes.GLUCOSA_NORMAL_MAX -> HealthEvaluation(
                estado = EstadoSalud.BUENO,
                mensaje = "✅ Glucosa normal: $glucosa mg/dL",
                detalles = listOf("Nivel de glucosa aceptable"),
                color = "#8BC34A",
                recomendaciones = listOf("Continuar con control regular")
            )
            glucosaDouble <= RangosDiabetes.GLUCOSA_PREDIABETES_MAX -> HealthEvaluation(
                estado = EstadoSalud.PRECAUCION,
                mensaje = "⚠️ Prediabetes: $glucosa mg/dL",
                detalles = listOf("Nivel elevado de glucosa"),
                color = "#FFC107",
                recomendaciones = listOf(
                    "Reducir consumo de azúcares",
                    "Aumentar actividad física",
                    "Consultar con su médico"
                )
            )
            glucosaDouble < RangosDiabetes.GLUCOSA_DIABETES_MIN -> HealthEvaluation(
                estado = EstadoSalud.RIESGO,
                mensaje = "⚠️ Glucosa alta: $glucosa mg/dL",
                detalles = listOf("Nivel preocupante de glucosa"),
                color = "#FF9800",
                recomendaciones = listOf(
                    "Consultar médico pronto",
                    "Revisar medicación",
                    "Control estricto de dieta"
                )
            )
            else -> HealthEvaluation(
                estado = EstadoSalud.PELIGRO,
                mensaje = "🚨 HIPERGLUCEMIA: $glucosa mg/dL (muy alto)",
                detalles = listOf("Nivel crítico alto de glucosa"),
                color = "#F44336",
                recomendaciones = listOf(
                    "Contactar médico INMEDIATAMENTE",
                    "Verificar medicación",
                    "Monitorear síntomas"
                )
            )
        }
    }

    /**
     * Evalúa niveles de colesterol
     */
    private fun evaluarColesterol(colesterol: Int?): HealthEvaluation {
        if (colesterol == null) {
            return HealthEvaluation(
                estado = EstadoSalud.BUENO,
                mensaje = "ℹ️ Colesterol no registrado",
                detalles = emptyList(),
                color = "#9E9E9E",
                recomendaciones = emptyList()
            )
        }

        val colesterolDouble = colesterol.toDouble()
        return when {
            colesterolDouble <= RangosDiabetes.COLESTEROL_OPTIMO_MAX -> HealthEvaluation(
                estado = EstadoSalud.OPTIMO,
                mensaje = "✅ Colesterol óptimo: $colesterol mg/dL",
                detalles = listOf("Nivel ideal de colesterol"),
                color = "#4CAF50",
                recomendaciones = listOf("Mantener dieta saludable")
            )
            colesterolDouble <= RangosDiabetes.COLESTEROL_NORMAL_MAX -> HealthEvaluation(
                estado = EstadoSalud.BUENO,
                mensaje = "✅ Colesterol normal: $colesterol mg/dL",
                detalles = listOf("Nivel aceptable de colesterol"),
                color = "#8BC34A",
                recomendaciones = listOf("Reducir grasas saturadas")
            )
            colesterolDouble < RangosDiabetes.COLESTEROL_ALTO_MIN + 40 -> HealthEvaluation(
                estado = EstadoSalud.PRECAUCION,
                mensaje = "⚠️ Colesterol elevado: $colesterol mg/dL",
                detalles = listOf("Nivel en límite alto"),
                color = "#FFC107",
                recomendaciones = listOf(
                    "Reducir grasas saturadas",
                    "Aumentar fibra en dieta",
                    "Ejercicio regular"
                )
            )
            else -> HealthEvaluation(
                estado = EstadoSalud.RIESGO,
                mensaje = "⚠️ Colesterol alto: $colesterol mg/dL",
                detalles = listOf("Nivel preocupante de colesterol"),
                color = "#FF9800",
                recomendaciones = listOf(
                    "Consultar médico",
                    "Dieta baja en grasas",
                    "Considerar medicación"
                )
            )
        }
    }

    /**
     * Evalúa presión arterial
     */
    private fun evaluarPresion(presion: String?): HealthEvaluation {
        if (presion.isNullOrBlank()) {
            return HealthEvaluation(
                estado = EstadoSalud.BUENO,
                mensaje = "ℹ️ Presión arterial no registrada",
                detalles = emptyList(),
                color = "#9E9E9E",
                recomendaciones = emptyList()
            )
        }

        val parts = presion.split("/")
        if (parts.size != 2) {
            return HealthEvaluation(
                estado = EstadoSalud.BUENO,
                mensaje = "⚠️ Formato de presión inválido",
                detalles = emptyList(),
                color = "#9E9E9E",
                recomendaciones = listOf("Use formato 120/80")
            )
        }

        val sistolica = parts[0].toIntOrNull() ?: return HealthEvaluation(
            estado = EstadoSalud.BUENO,
            mensaje = "⚠️ Valor de presión inválido",
            detalles = emptyList(),
            color = "#9E9E9E",
            recomendaciones = emptyList()
        )

        val diastolica = parts[1].toIntOrNull() ?: return HealthEvaluation(
            estado = EstadoSalud.BUENO,
            mensaje = "⚠️ Valor de presión inválido",
            detalles = emptyList(),
            color = "#9E9E9E",
            recomendaciones = emptyList()
        )

        return when {
            sistolica >= RangosHipertension.SISTOLICA_CRISIS ||
            diastolica >= RangosHipertension.DIASTOLICA_CRISIS -> HealthEvaluation(
                estado = EstadoSalud.PELIGRO,
                mensaje = "🚨 CRISIS HIPERTENSIVA: $presion mmHg",
                detalles = listOf("Presión arterial en nivel crítico"),
                color = "#F44336",
                recomendaciones = listOf(
                    "BUSCAR ATENCIÓN MÉDICA INMEDIATA",
                    "No esperar",
                    "Puede ser emergencia"
                )
            )
            sistolica >= RangosHipertension.SISTOLICA_ELEVADA_MAX ||
            diastolica >= RangosHipertension.DIASTOLICA_ELEVADA_MAX -> HealthEvaluation(
                estado = EstadoSalud.RIESGO,
                mensaje = "⚠️ Hipertensión: $presion mmHg",
                detalles = listOf("Presión arterial alta"),
                color = "#FF9800",
                recomendaciones = listOf(
                    "Consultar médico pronto",
                    "Reducir sal en dieta",
                    "Evitar estrés",
                    "Medicación si prescrita"
                )
            )
            sistolica >= RangosHipertension.SISTOLICA_NORMAL_MAX ||
            diastolica >= RangosHipertension.DIASTOLICA_NORMAL_MAX -> HealthEvaluation(
                estado = EstadoSalud.PRECAUCION,
                mensaje = "⚠️ Presión elevada: $presion mmHg",
                detalles = listOf("Presión en límite alto"),
                color = "#FFC107",
                recomendaciones = listOf(
                    "Monitorear regularmente",
                    "Reducir consumo de sal",
                    "Actividad física moderada"
                )
            )
            sistolica <= RangosHipertension.SISTOLICA_OPTIMA_MAX &&
            diastolica <= RangosHipertension.DIASTOLICA_OPTIMA_MAX -> HealthEvaluation(
                estado = EstadoSalud.OPTIMO,
                mensaje = "✅ Presión óptima: $presion mmHg",
                detalles = listOf("Presión arterial en rango ideal"),
                color = "#4CAF50",
                recomendaciones = listOf("Mantener hábitos saludables")
            )
            else -> HealthEvaluation(
                estado = EstadoSalud.BUENO,
                mensaje = "✅ Presión normal: $presion mmHg",
                detalles = listOf("Presión arterial aceptable"),
                color = "#8BC34A",
                recomendaciones = listOf("Continuar con control regular")
            )
        }
    }

    /**
     * Evalúa IMC
     */
    private fun evaluarIMC(imc: Double): HealthEvaluation {
        return when {
            imc < RangosIMC.BAJO_PESO_MAX -> HealthEvaluation(
                estado = EstadoSalud.PRECAUCION,
                mensaje = "⚠️ Bajo peso: IMC ${"%.1f".format(imc)}",
                detalles = listOf("Índice de masa corporal bajo"),
                color = "#FFC107",
                recomendaciones = listOf(
                    "Aumentar ingesta calórica",
                    "Consultar nutricionista"
                )
            )
            imc <= RangosIMC.NORMAL_MAX -> HealthEvaluation(
                estado = EstadoSalud.OPTIMO,
                mensaje = "✅ Peso normal: IMC ${"%.1f".format(imc)}",
                detalles = listOf("Índice de masa corporal ideal"),
                color = "#4CAF50",
                recomendaciones = listOf("Mantener peso actual")
            )
            imc <= RangosIMC.SOBREPESO_MAX -> HealthEvaluation(
                estado = EstadoSalud.PRECAUCION,
                mensaje = "⚠️ Sobrepeso: IMC ${"%.1f".format(imc)}",
                detalles = listOf("Índice de masa corporal elevado"),
                color = "#FFC107",
                recomendaciones = listOf(
                    "Aumentar actividad física",
                    "Reducir calorías",
                    "Dieta balanceada"
                )
            )
            imc <= RangosIMC.OBESIDAD_1_MAX -> HealthEvaluation(
                estado = EstadoSalud.RIESGO,
                mensaje = "⚠️ Obesidad Grado I: IMC ${"%.1f".format(imc)}",
                detalles = listOf("Obesidad leve"),
                color = "#FF9800",
                recomendaciones = listOf(
                    "Consultar médico",
                    "Plan de pérdida de peso",
                    "Ejercicio regular"
                )
            )
            imc <= RangosIMC.OBESIDAD_2_MAX -> HealthEvaluation(
                estado = EstadoSalud.RIESGO,
                mensaje = "⚠️ Obesidad Grado II: IMC ${"%.1f".format(imc)}",
                detalles = listOf("Obesidad moderada"),
                color = "#FF5722",
                recomendaciones = listOf(
                    "Atención médica necesaria",
                    "Programa de pérdida de peso supervisado",
                    "Cambios de estilo de vida"
                )
            )
            else -> HealthEvaluation(
                estado = EstadoSalud.PELIGRO,
                mensaje = "🚨 Obesidad Grado III: IMC ${"%.1f".format(imc)}",
                detalles = listOf("Obesidad mórbida"),
                color = "#F44336",
                recomendaciones = listOf(
                    "CONSULTAR MÉDICO URGENTE",
                    "Evaluación médica completa",
                    "Tratamiento especializado necesario"
                )
            )
        }
    }
}

