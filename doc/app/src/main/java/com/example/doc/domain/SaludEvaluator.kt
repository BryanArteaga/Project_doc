package com.example.doc.domain

import com.example.doc.model.EvaluacionSalud
import com.example.doc.model.RegistroSalud
import com.example.doc.model.Usuario

object SaludEvaluator {

    fun evaluarRegistroDetallado(usuario: Usuario, registro: RegistroSalud): EvaluacionSalud {
        val estadoPrincipal: EvaluacionSalud.Estado = when (usuario.enfermedad) {
            "Hipertensión" -> evaluarHipertension(registro)
            "Diabetes" -> evaluarDiabetes(registro)
            "Obesidad" -> evaluarObesidad(registro)
            "Colesterol alto" -> evaluarColesterol(registro)
            else -> EvaluacionSalud.Estado.NORMAL
        }

        val evaluacionCalorias: Pair<EvaluacionSalud.Estado, List<String>> = evaluarCalorias(
            registro.calorias, usuario.edad, usuario.peso, usuario.altura, usuario.sexo, registro.ejercicio
        )
        val evaluacionEjercicio: Pair<EvaluacionSalud.Estado, List<String>> = evaluarEjercicio(registro.ejercicio, usuario.edad)
        val evaluacionAnimo: Pair<EvaluacionSalud.Estado, List<String>> = evaluarAnimo(registro.estadoAnimo)

        // Contar estados
        val estados = listOf(estadoPrincipal, evaluacionCalorias.first, evaluacionEjercicio.first, evaluacionAnimo.first)
        val optimos = estados.count { it == EvaluacionSalud.Estado.NORMAL }
        val leves = estados.count { it == EvaluacionSalud.Estado.LEVE }
        val advertencias = estados.count { it == EvaluacionSalud.Estado.ADVERTENCIA }
        val emergencias = estados.count { it == EvaluacionSalud.Estado.EMERGENCIA }

        // Estado general
        val estadoGeneral: EvaluacionSalud.Estado = when {
            emergencias > 0 -> EvaluacionSalud.Estado.EMERGENCIA
            advertencias >= 2 || (advertencias == 1 && leves >= 1) -> EvaluacionSalud.Estado.ADVERTENCIA
            leves >= 1 -> EvaluacionSalud.Estado.LEVE
            optimos == estados.size -> EvaluacionSalud.Estado.NORMAL
            else -> EvaluacionSalud.Estado.NORMAL // Fallback si todos son normales
        }

        val mensaje: String = when (estadoGeneral) {
            EvaluacionSalud.Estado.NORMAL -> "¡Tu salud está excelente, sigue así!"
            EvaluacionSalud.Estado.LEVE -> "Estás bien, pero pequeños ajustes pueden optimizar tu salud."
            EvaluacionSalud.Estado.ADVERTENCIA -> "Algunos valores necesitan atención, revisa los detalles."
            EvaluacionSalud.Estado.EMERGENCIA -> "Consulta a tu médico de inmediato."
        }

        val detalles: MutableList<String> = mutableListOf()

        // Agregar detalles según enfermedad con emojis indicativos
        when (usuario.enfermedad) {
            "Hipertensión" -> registro.presionArterial?.let {
                val icono = when (estadoPrincipal) {
                    EvaluacionSalud.Estado.NORMAL -> "✅"
                    EvaluacionSalud.Estado.LEVE -> "⚠️"
                    EvaluacionSalud.Estado.ADVERTENCIA -> "⚠️"
                    EvaluacionSalud.Estado.EMERGENCIA -> "🚨"
                }
                detalles.add("$icono Presión arterial: $it mmHg (${estadoPrincipal.name})")
            }
            "Diabetes" -> registro.glucosa?.let {
                val icono = when (estadoPrincipal) {
                    EvaluacionSalud.Estado.NORMAL -> "✅"
                    EvaluacionSalud.Estado.LEVE -> "⚠️"
                    EvaluacionSalud.Estado.ADVERTENCIA -> "⚠️"
                    EvaluacionSalud.Estado.EMERGENCIA -> "🚨"
                }
                val mensaje = when {
                    it < 70 -> "HIPOGLUCEMIA"
                    it >= 250 -> "HIPERGLUCEMIA SEVERA"
                    it >= 200 -> "Diabetes descontrolada"
                    it >= 126 -> "Glucosa elevada (Prediabetes)"
                    else -> "Glucosa normal"
                }
                detalles.add("$icono Glucosa: $it mg/dL - $mensaje")
            }
            "Obesidad" -> registro.imc?.let {
                val icono = when (estadoPrincipal) {
                    EvaluacionSalud.Estado.NORMAL -> "✅"
                    EvaluacionSalud.Estado.LEVE -> "⚠️"
                    EvaluacionSalud.Estado.ADVERTENCIA -> "⚠️"
                    EvaluacionSalud.Estado.EMERGENCIA -> "🚨"
                }
                detalles.add("$icono IMC: ${String.format(java.util.Locale.US, "%.2f", it)} (${estadoPrincipal.name})")
            }
            "Colesterol alto" -> registro.colesterol?.let {
                val icono = when (estadoPrincipal) {
                    EvaluacionSalud.Estado.NORMAL -> "✅"
                    EvaluacionSalud.Estado.LEVE -> "⚠️"
                    EvaluacionSalud.Estado.ADVERTENCIA -> "⚠️"
                    EvaluacionSalud.Estado.EMERGENCIA -> "🚨"
                }
                detalles.add("$icono Colesterol: $it mg/dL (${estadoPrincipal.name})")
            }
        }

        detalles.addAll(evaluacionCalorias.second)
        detalles.addAll(evaluacionEjercicio.second)
        detalles.addAll(evaluacionAnimo.second)
        registro.hora?.let { detalles.add("📅 Hora del registro: $it") }

        return EvaluacionSalud(estado = estadoGeneral, mensaje = mensaje, detalles = detalles)
    }

    private fun evaluarHipertension(registro: RegistroSalud): EvaluacionSalud.Estado {
        val (sistolica, diastolica) = registro.presionArterial?.split("/")?.map { it.toIntOrNull() ?: 0 } ?: return EvaluacionSalud.Estado.NORMAL
        return when {
            sistolica > 180 || diastolica > 120 -> EvaluacionSalud.Estado.EMERGENCIA
            sistolica in 140..180 || diastolica in 90..120 -> EvaluacionSalud.Estado.ADVERTENCIA
            sistolica in 130..139 || diastolica in 85..89 -> EvaluacionSalud.Estado.LEVE
            else -> EvaluacionSalud.Estado.NORMAL
        }
    }

    private fun evaluarDiabetes(registro: RegistroSalud): EvaluacionSalud.Estado {
        val glucosa: Int? = registro.glucosa
        return if (glucosa == null) {
            EvaluacionSalud.Estado.NORMAL
        } else when {
            glucosa > 250 -> EvaluacionSalud.Estado.EMERGENCIA
            glucosa in 200..250 -> EvaluacionSalud.Estado.ADVERTENCIA
            glucosa in 140..199 -> EvaluacionSalud.Estado.LEVE
            else -> EvaluacionSalud.Estado.NORMAL
        }
    }

    private fun evaluarObesidad(registro: RegistroSalud): EvaluacionSalud.Estado {
        val imc: Double? = registro.imc
        return if (imc == null) {
            EvaluacionSalud.Estado.NORMAL
        } else when {
            imc > 40 -> EvaluacionSalud.Estado.EMERGENCIA
            imc in 30.0..40.0 -> EvaluacionSalud.Estado.ADVERTENCIA
            imc in 25.0..29.9 -> EvaluacionSalud.Estado.LEVE
            else -> EvaluacionSalud.Estado.NORMAL
        }
    }

    private fun evaluarColesterol(registro: RegistroSalud): EvaluacionSalud.Estado {
        val colesterol: Int? = registro.colesterol
        return if (colesterol == null) {
            EvaluacionSalud.Estado.NORMAL
        } else when {
            colesterol > 300 -> EvaluacionSalud.Estado.EMERGENCIA
            colesterol in 240..300 -> EvaluacionSalud.Estado.ADVERTENCIA
            colesterol in 200..239 -> EvaluacionSalud.Estado.LEVE
            else -> EvaluacionSalud.Estado.NORMAL
        }
    }

    private fun evaluarCalorias(
        calorias: Int,
        edad: Int,
        peso: Double,
        altura: Double,
        sexo: String?,
        ejercicio: Int
    ): Pair<EvaluacionSalud.Estado, List<String>> {
        // Calcular TMB (Harris-Benedict)
        val tmb: Int = if (sexo == "Hombre") {
            (10 * peso + 6.25 * (altura * 100) - 5 * edad + 5).toInt()
        } else {
            (10 * peso + 6.25 * (altura * 100) - 5 * edad - 161).toInt()
        }
        // Ajustar rango según nivel de actividad
        val factorActividad: Double = when {
            ejercicio >= 60 -> 1.5 // Alta actividad
            ejercicio >= 30 -> 1.3 // Moderada
            else -> 1.1 // Baja
        }
        // Rango óptimo: ±10%, rango normal: ±15%
        val rangoOptimoMin: Int = (tmb * factorActividad * 0.90).toInt()
        val rangoOptimoMax: Int = (tmb * factorActividad * 1.10).toInt()
        val rangoNormalMin: Int = (tmb * factorActividad * 0.85).toInt()
        val rangoNormalMax: Int = (tmb * factorActividad * 1.15).toInt()
        // Ajustar para niños y adultos mayores
        val rangoAjustadoOptimo: IntArray = when {
            edad < 18 -> intArrayOf((rangoOptimoMin * 0.9).toInt(), (rangoOptimoMax * 0.95).toInt())
            edad >= 65 -> intArrayOf((rangoOptimoMin * 0.95).toInt(), (rangoOptimoMax * 1.0).toInt())
            else -> intArrayOf(rangoOptimoMin, rangoOptimoMax)
        }
        val rangoAjustadoNormal: IntArray = when {
            edad < 18 -> intArrayOf((rangoNormalMin * 0.9).toInt(), (rangoNormalMax * 0.95).toInt())
            edad >= 65 -> intArrayOf((rangoNormalMin * 0.95).toInt(), (rangoNormalMax * 1.0).toInt())
            else -> intArrayOf(rangoNormalMin, rangoNormalMax)
        }
        val estado: EvaluacionSalud.Estado = when {
            calorias < rangoAjustadoNormal[0] || calorias > rangoAjustadoNormal[1] -> EvaluacionSalud.Estado.ADVERTENCIA
            calorias < rangoAjustadoOptimo[0] || calorias > rangoAjustadoOptimo[1] -> EvaluacionSalud.Estado.LEVE
            else -> EvaluacionSalud.Estado.NORMAL
        }
        val detalles: List<String> = listOf(
            when (estado) {
                EvaluacionSalud.Estado.NORMAL -> "Calorías: $calorias kcal (Óptimo para tu actividad: $ejercicio min)"
                EvaluacionSalud.Estado.LEVE -> "Calorías: $calorias kcal (Ligeramente fuera del rango óptimo ${rangoAjustadoOptimo[0]}-${rangoAjustadoOptimo[1]} kcal)"
                EvaluacionSalud.Estado.ADVERTENCIA -> "Calorías: $calorias kcal (Fuera del rango recomendado ${rangoAjustadoNormal[0]}-${rangoAjustadoNormal[1]} kcal)"
                else -> "Calorías: $calorias kcal (Valor crítico, consulta a tu médico)"
            }
        )
        return Pair(estado, detalles)
    }

    private fun evaluarEjercicio(ejercicio: Int, edad: Int): Pair<EvaluacionSalud.Estado, List<String>> {
        val minimoEjercicio: Int = when {
            edad < 18 -> 60 // Niños: 60 min/día
            edad >= 65 -> 15 // Adultos mayores: 15 min/día
            else -> 30 // Adultos: 30 min/día
        }
        val optimoEjercicio: Int = when {
            edad < 18 -> 90
            edad >= 65 -> 30
            else -> 45
        }
        val estado: EvaluacionSalud.Estado = when {
            ejercicio < minimoEjercicio -> EvaluacionSalud.Estado.ADVERTENCIA
            ejercicio < optimoEjercicio -> EvaluacionSalud.Estado.LEVE
            else -> EvaluacionSalud.Estado.NORMAL
        }
        val detalles: List<String> = listOf(
            when (estado) {
                EvaluacionSalud.Estado.NORMAL -> "Ejercicio: $ejercicio min (Óptimo para tu edad: $edad)"
                EvaluacionSalud.Estado.LEVE -> "Ejercicio: $ejercicio min (Bueno, pero intenta llegar a $optimoEjercicio min/día)"
                EvaluacionSalud.Estado.ADVERTENCIA -> "Ejercicio: $ejercicio min (Recomendado mínimo $minimoEjercicio min/día)"
                else -> "Ejercicio: $ejercicio min (Valor crítico)"
            }
        )
        return Pair(estado, detalles)
    }

    private fun evaluarAnimo(animo: String): Pair<EvaluacionSalud.Estado, List<String>> {
        val estado: EvaluacionSalud.Estado = when (animo) {
            "Malo" -> EvaluacionSalud.Estado.ADVERTENCIA
            "Regular" -> EvaluacionSalud.Estado.LEVE
            else -> EvaluacionSalud.Estado.NORMAL // Bueno
        }
        val detalles: List<String> = listOf(
            when (estado) {
                EvaluacionSalud.Estado.NORMAL -> "Estado de ánimo: $animo (Excelente)"
                EvaluacionSalud.Estado.LEVE -> "Estado de ánimo: $animo (Regular, considera actividades que te animen)"
                EvaluacionSalud.Estado.ADVERTENCIA -> "Estado de ánimo: $animo (Malo, consulta a un especialista)"
                else -> "Estado de ánimo: $animo (Crítico)"
            }
        )
        return Pair(estado, detalles)
    }
}