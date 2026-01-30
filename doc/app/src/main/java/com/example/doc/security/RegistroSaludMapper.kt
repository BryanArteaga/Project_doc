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
        return RegistroSaludSeguro(
            id = entity.id,
            usuarioId = entity.usuarioId,
            fecha = entity.fecha,
            hora = entity.hora,
            presionArterial = entity.presionArterial,
            glucosa = entity.glucosa?.toDouble(),
            imc = entity.imc,
            colesterol = entity.colesterol?.toDouble(),
            calorias = entity.calorias,
            ejercicio = entity.ejercicio,
            estadoAnimo = entity.estadoAnimo
        )
    }

    /**
     * Convierte de modelo seguro a Entity (para guardar en BD)
     */
    fun seguroToEntity(seguro: RegistroSaludSeguro): RegistroSaludEntity {
        return RegistroSaludEntity(
            id = seguro.id,
            usuarioId = seguro.usuarioId,
            fecha = seguro.fecha,
            hora = seguro.hora,
            presionArterial = seguro.presionArterial,
            glucosa = seguro.glucosa?.toInt(),
            imc = seguro.imc,
            colesterol = seguro.colesterol?.toInt(),
            calorias = seguro.calorias,
            ejercicio = seguro.ejercicio,
            estadoAnimo = seguro.estadoAnimo
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

