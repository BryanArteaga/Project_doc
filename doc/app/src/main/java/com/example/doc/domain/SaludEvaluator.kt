package com.example.doc.domain

import com.example.doc.model.EvaluacionSalud
import com.example.doc.model.RegistroSalud
import com.example.doc.model.Usuario
import timber.log.Timber

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
            registro.calorias ?: 0, usuario.edad, usuario.peso, usuario.altura, usuario.sexo, registro.ejercicioMinutos
        )
        val evaluacionEjercicio: Pair<EvaluacionSalud.Estado, List<String>> = evaluarEjercicio(registro.ejercicioMinutos, usuario.edad)
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
            "Obesidad" -> {
                val icono = when (estadoPrincipal) {
                    EvaluacionSalud.Estado.NORMAL -> "✅"
                    EvaluacionSalud.Estado.LEVE -> "⚠️"
                    EvaluacionSalud.Estado.ADVERTENCIA -> "⚠️"
                    EvaluacionSalud.Estado.EMERGENCIA -> "🚨"
                }
                
                Timber.d("🔍 DEBUG Obesidad: IMC=${registro.imc}, Peso=${registro.peso}, Pulso=${registro.pulso}")
                
                // Mostrar IMC si está disponible con explicación clara
                if (registro.imc != null) {
                    val imcValor = registro.imc
                    val imcMensaje = when {
                        imcValor > 40 -> "Obesidad severa (Riesgo muy alto)"
                        imcValor in 30.0..40.0 -> "Obesidad (Riesgo alto)"
                        imcValor in 25.0..29.9 -> "Sobrepeso (Riesgo moderado)"
                        else -> "Peso normal (Saludable)"
                    }
                    detalles.add("$icono IMC: ${String.format(java.util.Locale.US, "%.2f", imcValor)} - $imcMensaje (${estadoPrincipal.name})")
                }
                
                // Mostrar peso actual si está disponible
                registro.peso?.let { peso ->
                    if (peso > 0.0) {
                        detalles.add("Peso actual: ${String.format(java.util.Locale.US, "%.1f", peso)} kg")
                    }
                }
                
                // Mostrar pulso si está disponible con explicación clara
                registro.pulso?.let { pulso ->
                    if (pulso > 0) {
                        val pulsoMensaje = when {
                            pulso > 100 -> "Elevado (Posible estrés o actividad)"
                            pulso in 60..100 -> "Normal"
                            pulso in 50..59 -> "Bajo"
                            else -> "Muy bajo (Consulta a médico)"
                        }
                        detalles.add("Pulso: $pulso lat/min - $pulsoMensaje")
                    }
                }
            }
            "Colesterol alto" -> {
                registro.colesterol?.let {
                    val icono = when (estadoPrincipal) {
                        EvaluacionSalud.Estado.NORMAL -> "✅"
                        EvaluacionSalud.Estado.LEVE -> "⚠️"
                        EvaluacionSalud.Estado.ADVERTENCIA -> "⚠️"
                        EvaluacionSalud.Estado.EMERGENCIA -> "🚨"
                    }
                    detalles.add("$icono Colesterol: $it mg/dL (${estadoPrincipal.name})")
                    
                    // Agregar presión arterial si está disponible (comorbilidad importante)
                    val sistolica = registro.presionSistolica
                    val diastolica = registro.presionDiastolica
                    if (sistolica != null && diastolica != null && sistolica > 0) {
                        val presionMensaje = when {
                            sistolica > 180 || diastolica > 120 -> "PELIGROSAMENTE ALTA"
                            sistolica in 140..180 || diastolica in 90..120 -> "Elevada"
                            sistolica in 130..139 || diastolica in 85..89 -> "Prehipertensión"
                            else -> "Normal"
                        }
                        detalles.add("🩸 Presión arterial: $sistolica/$diastolica mmHg - $presionMensaje")
                    }
                }
            }
        }

        detalles.addAll(evaluacionCalorias.second)
        detalles.addAll(evaluacionEjercicio.second)
        detalles.addAll(evaluacionAnimo.second)
        registro.hora?.let { detalles.add("📅 Hora del registro: $it") }

        return EvaluacionSalud(estado = estadoGeneral, mensaje = mensaje, detalles = detalles)
    }

    private fun evaluarHipertension(registro: RegistroSalud): EvaluacionSalud.Estado {
        // Soportar tanto formato legacy (presionArterial: "sistolica/diastolica") como nuevo
        val (sistolica, diastolica) = if (registro.presionArterial?.contains("/") == true) {
            registro.presionArterial.split("/").map { it.toIntOrNull() ?: 0 }
        } else if (registro.presionSistolica != null && registro.presionDiastolica != null) {
            listOf(registro.presionSistolica, registro.presionDiastolica)
        } else {
            return EvaluacionSalud.Estado.NORMAL
        }
        
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
        // Calcular IMC desde peso y altura del usuario si no viene en el registro
        // (Este será actualizado en la pantalla de registro)
        val imc: Double? = registro.imc
        
        // Evaluar precisamente considerando IMC y pulso
        return if (imc == null) {
            EvaluacionSalud.Estado.NORMAL
        } else {
            val imcEstado = when {
                imc > 40 -> EvaluacionSalud.Estado.EMERGENCIA
                imc in 30.0..40.0 -> EvaluacionSalud.Estado.ADVERTENCIA
                imc in 25.0..29.9 -> EvaluacionSalud.Estado.LEVE
                else -> EvaluacionSalud.Estado.NORMAL
            }
            
            // Si hay pulso, considerar frecuencia cardíaca
            val pulsoEstado = if (registro.pulso != null && registro.pulso!! > 0) {
                when {
                    registro.pulso!! > 100 -> EvaluacionSalud.Estado.ADVERTENCIA  // Taquicardia
                    registro.pulso!! in 60..100 -> EvaluacionSalud.Estado.NORMAL
                    registro.pulso!! in 50..59 -> EvaluacionSalud.Estado.LEVE  // Bradicardia
                    else -> EvaluacionSalud.Estado.ADVERTENCIA
                }
            } else {
                EvaluacionSalud.Estado.NORMAL
            }
            
            // Combinar evaluaciones: si hay un EMERGENCIA, es EMERGENCIA
            // Si hay ADVERTENCIA en ambos o ADVERTENCIA + LEVE, es ADVERTENCIA
            when {
                imcEstado == EvaluacionSalud.Estado.EMERGENCIA -> EvaluacionSalud.Estado.EMERGENCIA
                pulsoEstado == EvaluacionSalud.Estado.EMERGENCIA -> EvaluacionSalud.Estado.EMERGENCIA
                imcEstado == EvaluacionSalud.Estado.ADVERTENCIA && pulsoEstado == EvaluacionSalud.Estado.ADVERTENCIA -> EvaluacionSalud.Estado.ADVERTENCIA
                imcEstado == EvaluacionSalud.Estado.ADVERTENCIA || pulsoEstado == EvaluacionSalud.Estado.ADVERTENCIA -> EvaluacionSalud.Estado.ADVERTENCIA
                imcEstado == EvaluacionSalud.Estado.LEVE || pulsoEstado == EvaluacionSalud.Estado.LEVE -> EvaluacionSalud.Estado.LEVE
                else -> EvaluacionSalud.Estado.NORMAL
            }
        }
    }

    private fun evaluarColesterol(registro: RegistroSalud): EvaluacionSalud.Estado {
        val colesterol: Int? = registro.colesterol
        
        if (colesterol == null) {
            return EvaluacionSalud.Estado.NORMAL
        }
        
        val colesterolEstado = when {
            colesterol > 300 -> EvaluacionSalud.Estado.EMERGENCIA
            colesterol in 240..300 -> EvaluacionSalud.Estado.ADVERTENCIA
            colesterol in 200..239 -> EvaluacionSalud.Estado.LEVE
            else -> EvaluacionSalud.Estado.NORMAL
        }
        
        // Evaluar presión arterial si está disponible (comorbilidad importante para colesterol)
        val presionEstado = if (registro.presionSistolica != null && registro.presionDiastolica != null) {
            val sistolica = registro.presionSistolica!!
            val diastolica = registro.presionDiastolica!!
            when {
                sistolica > 180 || diastolica > 120 -> EvaluacionSalud.Estado.EMERGENCIA
                sistolica in 140..180 || diastolica in 90..120 -> EvaluacionSalud.Estado.ADVERTENCIA
                sistolica in 130..139 || diastolica in 85..89 -> EvaluacionSalud.Estado.LEVE
                else -> EvaluacionSalud.Estado.NORMAL
            }
        } else {
            EvaluacionSalud.Estado.NORMAL
        }
        
        // Combinar inteligentemente: el riesgo cardiovascular es la suma de ambos factores
        return when {
            colesterolEstado == EvaluacionSalud.Estado.EMERGENCIA || presionEstado == EvaluacionSalud.Estado.EMERGENCIA -> 
                EvaluacionSalud.Estado.EMERGENCIA
            colesterolEstado == EvaluacionSalud.Estado.ADVERTENCIA && presionEstado == EvaluacionSalud.Estado.ADVERTENCIA -> 
                EvaluacionSalud.Estado.EMERGENCIA  // Colesterol alto + presión alta = muy riesgoso
            colesterolEstado == EvaluacionSalud.Estado.ADVERTENCIA || presionEstado == EvaluacionSalud.Estado.ADVERTENCIA -> 
                EvaluacionSalud.Estado.ADVERTENCIA
            colesterolEstado == EvaluacionSalud.Estado.LEVE || presionEstado == EvaluacionSalud.Estado.LEVE -> 
                EvaluacionSalud.Estado.LEVE
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

    private fun evaluarAnimo(animo: String?): Pair<EvaluacionSalud.Estado, List<String>> {
        if (animo.isNullOrBlank()) {
            return Pair(EvaluacionSalud.Estado.NORMAL, emptyList())
        }
        
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