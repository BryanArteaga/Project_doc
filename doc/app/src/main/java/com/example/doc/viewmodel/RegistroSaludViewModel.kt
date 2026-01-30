package com.example.doc.viewmodel

import androidx.lifecycle.ViewModel
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.doc.repository.RegistroSaludRepository
import com.example.doc.data.entities.RegistroSaludEntity
import com.example.doc.model.EvaluacionSalud
import com.example.doc.domain.SaludEvaluator
import com.example.doc.model.Usuario
import com.example.doc.utils.UiState
import androidx.lifecycle.viewModelScope
import com.example.doc.data.daos.EjercicioDao
import com.example.doc.data.daos.EjercicioRealizadoDao
import com.example.doc.data.daos.RegistroSaludDao
import com.example.doc.model.RegistroSalud
import com.example.doc.security.InputValidator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class RegistroSaludViewModel @Inject constructor(
    private val repository: RegistroSaludRepository,
    private val ejercicioRealizadoDao: EjercicioRealizadoDao,
    private val ejercicioDao: EjercicioDao,
    private val registroSaludDao: RegistroSaludDao
) : ViewModel() {

    private val _registros = MutableStateFlow<List<RegistroSalud>>(emptyList())
    val registros: StateFlow<List<RegistroSalud>> = _registros

    // Flow paginado para listas grandes de registros (Optimización con Paging3)
    private var _currentUsuarioId: Int? = null

    fun getRegistrosPaginados(usuarioId: Int): Flow<PagingData<RegistroSaludEntity>> {
        _currentUsuarioId = usuarioId
        return Pager(
            config = PagingConfig(
                pageSize = 20, // Cargar 20 registros por página
                prefetchDistance = 5, // Precargar cuando falten 5 items
                enablePlaceholders = false,
                initialLoadSize = 20
            ),
            pagingSourceFactory = { registroSaludDao.getRegistrosPaginados(usuarioId) }
        ).flow.cachedIn(viewModelScope) // Cachear en el scope del ViewModel
    }

    // Flow para registros recientes (para MainScreen)
    fun getRegistrosRecientesFlow(usuarioId: Int, limit: Int = 10): Flow<List<RegistroSaludEntity>> {
        return registroSaludDao.getRegistrosRecientesFlow(usuarioId, limit)
    }

    @Suppress("unused")
    private val _evaluacion = MutableStateFlow<EvaluacionSalud?>(null)
    val evaluacion: StateFlow<EvaluacionSalud?> = _evaluacion

    @Suppress("unused")
    private val _totalExerciseTimeToday = MutableStateFlow(0)
    val totalExerciseTimeToday: StateFlow<Int> = _totalExerciseTimeToday

    @Suppress("unused")
    private val _uiState = MutableStateFlow<UiState<RegistroSalud>>(UiState.Idle)
    val uiState: StateFlow<UiState<RegistroSalud>> = _uiState

    fun agregarRegistro(usuario: Usuario, nuevo: RegistroSalud) {
        viewModelScope.launch {
            try {
                _uiState.value = UiState.Loading

                // Validar datos de salud
                val validationResult = InputValidator.validateHealthData(
                    glucosa = nuevo.glucosa?.toDouble(),
                    colesterol = nuevo.colesterol?.toDouble(),
                    presion = nuevo.presionArterial
                )

                if (!validationResult.isValid) {
                    _uiState.value = UiState.Error(
                        validationResult.errorMessage ?: "Datos de salud inválidos",
                        null
                    )
                    Timber.w("Validación de datos de salud fallida: ${validationResult.errorMessage}")
                    return@launch
                }

                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                val fechaHoraActual = sdf.format(Date())
                val registroFinal = nuevo.copy(fecha = fechaHoraActual)
                val entity = RegistroSaludEntity(
                    id = registroFinal.id,
                    usuarioId = registroFinal.usuarioId,
                    fecha = registroFinal.fecha,
                    presionArterial = registroFinal.presionArterial,
                    glucosa = registroFinal.glucosa ?: 0, // Convertir Int? a Int
                    imc = registroFinal.imc,
                    colesterol = registroFinal.colesterol,
                    calorias = registroFinal.calorias,
                    ejercicio = registroFinal.ejercicio,
                    estadoAnimo = registroFinal.estadoAnimo
                )
                repository.guardarRegistro(entity)
                cargarRegistrosDesdeBD(usuario.id)
                Timber.d("Registro guardado exitosamente")

                // Evaluar salud con SaludEvaluator mejorado
                val evaluacion = SaludEvaluator.evaluarRegistroDetallado(usuario, registroFinal)

                _evaluacion.value = evaluacion
                _uiState.value = UiState.Success(registroFinal)
                Timber.d("Evaluación completada: ${evaluacion.estado.name} - ${evaluacion.mensaje}")
            } catch (e: Exception) {
                Timber.e(e, "Error al agregar registro")
                _uiState.value = UiState.Error("Error al guardar registro", e)
            }
        }
    }

    fun cargarRegistrosDesdeBD(usuarioId: Int) {
        viewModelScope.launch {
            try {
                val entidades = repository.obtenerRegistros(usuarioId)
                _registros.value = entidades.map { entidad ->
                    RegistroSalud(
                        id = entidad.id,
                        usuarioId = entidad.usuarioId,
                        fecha = entidad.fecha,
                        presionArterial = entidad.presionArterial,
                        glucosa = entidad.glucosa?.takeIf { it > 0 }, // Convertir Int? a Int? con validación
                        imc = entidad.imc,
                        colesterol = entidad.colesterol,
                        calorias = entidad.calorias,
                        ejercicio = entidad.ejercicio,
                        estadoAnimo = entidad.estadoAnimo
                    )
                }
                    .sortedByDescending { it.fecha }
                Timber.d("Registros cargados: ${_registros.value.size}")
            } catch (e: Exception) {
                Timber.e(e, "Error al cargar registros")
            }
        }
    }

    @Suppress("unused")
    fun limpiarRegistros(usuarioId: Int) {
        viewModelScope.launch {
            try {
                repository.eliminarRegistrosDeUsuario(usuarioId)
                _registros.value = emptyList()
                Timber.d("Registros limpiados para usuario: $usuarioId")
            } catch (e: Exception) {
                Timber.e(e, "Error al limpiar registros")
            }
        }
    }

    @Suppress("unused")
    fun limpiarEvaluacion() {
        _evaluacion.value = null
    }

    @Suppress("unused")
    fun getTotalExerciseTimeToday(usuarioId: Int) {
        viewModelScope.launch {
            try {
                val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                val realizados = ejercicioRealizadoDao.getEjerciciosRealizadosHoy(usuarioId, today)
                val totalTime = realizados.sumOf { realizado ->
                    ejercicioDao.getEjercicioById(realizado.ejercicioId)?.duracion ?: 0
                }
                _totalExerciseTimeToday.value = totalTime
                Timber.d("Tiempo total de ejercicio hoy: $totalTime minutos")
            } catch (e: Exception) {
                Timber.e(e, "Error al calcular tiempo de ejercicio")
            }
        }
    }
//
    /**
     * FUNCIÓN DE PRUEBA - Solo para testing de paginación
     * Genera registros de prueba para verificar que Paging3 funciona correctamente
     * NOTA: Esta función solo debe usarse en DEBUG mode
     */
    @Suppress("unused")
    fun generarRegistrosDePrueba(usuarioId: Int, cantidad: Int = 100) {
        viewModelScope.launch {
            try {
                Timber.d("🧪 Iniciando generación de $cantidad registros de prueba...")
                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                val calendar = java.util.Calendar.getInstance()

                repeat(cantidad) { index ->
                    calendar.add(java.util.Calendar.DAY_OF_YEAR, -1) // Un día atrás
                    val fecha = sdf.format(calendar.time)

                    val registro = RegistroSaludEntity(
                        id = 0,
                        usuarioId = usuarioId,
                        fecha = fecha,
                        presionArterial = "${120 + index % 20}/${80 + index % 15}",
                        glucosa = (90.0 + (index % 30)).toInt(), // Convertir a Int
                        imc = 22.0 + (index % 8),
                        colesterol = (180 + (index % 40)), // Mantener como Int
                        calorias = 2000 + (index % 500),
                        ejercicio = if (index % 2 == 0) 1 else 0, // Convertir Boolean a Int
                        estadoAnimo = listOf("Bien", "Normal", "Cansado")[index % 3]
                    )

                    repository.guardarRegistro(registro)
                }

                cargarRegistrosDesdeBD(usuarioId)
                Timber.d("✅ Generados $cantidad registros de prueba exitosamente")
            } catch (e: Exception) {
                Timber.e(e, "❌ Error al generar registros de prueba")
            }
        }
    }
    //
}