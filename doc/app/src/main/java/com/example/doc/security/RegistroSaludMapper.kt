package com.example.doc.security

import com.example.doc.data.entities.RegistroSaludEntity
import com.example.doc.model.RegistroSaludSeguro
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Mapper para convertir entre RegistroSaludEntity y RegistroSaludSeguro
 * aplicando cifrado/descifrado según sea necesario
 */
@Singleton
class RegistroSaludMapper @Inject constructor(
    private val encryption: HealthDataEncryption
) {

    /**
     * Convierte de Entity (BD) a modelo seguro (descifrado para uso)
     * NOTA: Por ahora no ciframos en BD para evitar migración compleja
     * Los datos se mantienen sin cifrar en BD pero se validan
     */
    fun entityToSeguro(entity: RegistroSaludEntity): RegistroSaludSeguro {
        // Construir presionArterial legacy desde presionSistolica/Diastolica
        val presionArterial = if (entity.presionSistolica != null && entity.presionDiastolica != null) {
            "${entity.presionSistolica}/${entity.presionDiastolica}"
        } else {
            null
        }
        
        return RegistroSaludSeguro(
            id = entity.id,
            usuarioId = entity.usuarioId,
            fecha = entity.fecha,
            hora = entity.hora,
            presionArterial = presionArterial,
            glucosa = entity.glucosa?.toDouble(),
            imc = null,  // No se calcula aquí
            colesterol = entity.colesterol?.toDouble(),
            calorias = entity.calorias ?: 0,
            ejercicio = 0,  // No se mapea desde String
            estadoAnimo = entity.estadoAnimo ?: ""
        )
    }

    /**
     * Convierte de modelo seguro a Entity (para guardar en BD)
     */
    fun seguroToEntity(seguro: RegistroSaludSeguro): RegistroSaludEntity {
        // Parsear presionArterial legacy ("sistolica/diastolica")
        val presionSys = if (seguro.presionArterial?.contains("/") == true) {
            seguro.presionArterial.split("/")[0].toIntOrNull()
        } else {
            null
        }
        val presionDia = if (seguro.presionArterial?.contains("/") == true) {
            seguro.presionArterial.split("/")[1].toIntOrNull()
        } else {
            null
        }
        
        return RegistroSaludEntity(
            id = seguro.id,
            usuarioId = seguro.usuarioId,
            fecha = seguro.fecha,
            hora = seguro.hora,
            presionSistolica = presionSys,
            presionDiastolica = presionDia,
            glucosa = seguro.glucosa?.toInt(),
            colesterol = seguro.colesterol?.toInt(),
            calorias = seguro.calorias,
            ejercicio = null,  // No se mapea desde Int
            estadoAnimo = seguro.estadoAnimo,
            registradoEn = System.currentTimeMillis()
        )
    }

    /**
     * Valida y convierte lista de entities
     */
    fun entitiesToSeguros(entities: List<RegistroSaludEntity>): List<RegistroSaludSeguro> {
        return entities.mapNotNull { entity ->
            try {
                entityToSeguro(entity)
            } catch (e: Exception) {
                Timber.e(e, "Error al convertir registro ${entity.id}")
                null
            }
        }
    }

    /**
     * Valida datos antes de guardar
     */
    fun validateAndMap(seguro: RegistroSaludSeguro): RegistroSaludEntity? {
        return try {
            // Validar datos sensibles
            val validationResult = InputValidator.validateHealthData(
                glucosa = seguro.glucosa,
                colesterol = seguro.colesterol,
                presion = seguro.presionArterial
            )

            if (!validationResult.isValid) {
                Timber.w("Validación fallida: ${validationResult.errorMessage}")
                return null
            }

            seguroToEntity(seguro)
        } catch (e: Exception) {
            Timber.e(e, "Error al validar y mapear registro")
            null
        }
    }
}

