package com.example.doc.viewmodel

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.doc.model.Ejercicio
import com.example.doc.repository.EjercicioRepository
import com.example.doc.repository.EjercicioRealizadoRepository
import com.example.doc.utils.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class EjercicioViewModel @Inject constructor(
    private val ejercicioRepo: EjercicioRepository,
    private val realizadoRepo: EjercicioRealizadoRepository,
    private val prefs: SharedPreferences
) : ViewModel() {

    private val _ejercicios = MutableStateFlow<List<Ejercicio>>(emptyList())
    val ejercicios: StateFlow<List<Ejercicio>> = _ejercicios

    private val _realizadosHoy = MutableStateFlow<Set<Int>>(emptySet())
    val realizadosHoy: StateFlow<Set<Int>> = _realizadosHoy

    @Suppress("unused")
    private val _uiState = MutableStateFlow<UiState<List<Ejercicio>>>(UiState.Idle)
    val uiState: StateFlow<UiState<List<Ejercicio>>> = _uiState

    fun cargarEjerciciosPorEnfermedad(enfermedad: String, currentDate: String) {
        viewModelScope.launch {
            try {
                _uiState.value = UiState.Loading
                val lastUpdateDate = prefs.getString("last_update_date", null)
                val storedIdsJson = prefs.getString("current_exercise_ids", null)

                if (lastUpdateDate != currentDate || storedIdsJson == null) {
                    val allEjercicios = ejercicioRepo.obtenerEjerciciosPorEnfermedad(enfermedad).shuffled()
                    val newList = allEjercicios.take(6)
                    _ejercicios.value = newList.map { entity ->
                        Ejercicio(
                            id = entity.id,
                            enfermedad = entity.enfermedad,
                            nombre = entity.nombre,
                            descripcion = entity.descripcion,
                            duracion = entity.duracion,
                            intensidad = entity.intensidad,
                            imagenResId = entity.imagenResId,
                            series = entity.series,
                            descansoEntreSeries = entity.descansoEntreSeries
                        )
                    }
                    val idsJson = JSONArray(_ejercicios.value.map { it.id }).toString()
                    prefs.edit().putString("current_exercise_ids", idsJson)
                        .putString("last_update_date", currentDate)
                        .apply()
                    Timber.d("Ejercicios refrescados para: $enfermedad, total: ${_ejercicios.value.size}")
                } else {
                    val idsArray = JSONArray(storedIdsJson)
                    val ids = (0 until idsArray.length()).map { idsArray.getInt(it) }
                    val entidades = ejercicioRepo.obtenerEjerciciosPorEnfermedad(enfermedad)
                        .filter { it.id in ids }
                        .sortedBy { ids.indexOf(it.id) }
                    _ejercicios.value = entidades.map { entity ->
                        Ejercicio(
                            id = entity.id,
                            enfermedad = entity.enfermedad,
                            nombre = entity.nombre,
                            descripcion = entity.descripcion,
                            duracion = entity.duracion,
                            intensidad = entity.intensidad,
                            imagenResId = entity.imagenResId,
                            series = entity.series,
                            descansoEntreSeries = entity.descansoEntreSeries
                        )
                    }
                    Timber.d("Ejercicios cargados desde caché: ${_ejercicios.value.size}")
                }
                _uiState.value = UiState.Success(_ejercicios.value)
            } catch (e: Exception) {
                Timber.e(e, "Error al cargar ejercicios")
                _uiState.value = UiState.Error("Error al cargar ejercicios", e)
            }
        }
    }

    fun cargarRealizadosHoy(usuarioId: Int, fecha: String) {
        viewModelScope.launch {
            try {
                val realizados = realizadoRepo.realizadosHoy(usuarioId, fecha)
                _realizadosHoy.value = realizados.map { it.ejercicioId }.toSet()
                Timber.d("Ejercicios realizados hoy: ${_realizadosHoy.value.size}")
            } catch (e: Exception) {
                Timber.e(e, "Error al cargar ejercicios realizados")
            }
        }
    }

    fun marcarRealizado(usuarioId: Int, ejercicioId: Int, fecha: String) {
        viewModelScope.launch {
            try {
                realizadoRepo.marcarRealizado(
                    com.example.doc.data.entities.EjercicioRealizadoEntity(
                        usuarioId = usuarioId,
                        ejercicioId = ejercicioId,
                        fecha = fecha
                    )
                )
                cargarRealizadosHoy(usuarioId, fecha)
                Timber.d("Ejercicio marcado como realizado: $ejercicioId")
            } catch (e: Exception) {
                Timber.e(e, "Error al marcar ejercicio")
            }
        }
    }

    @Suppress("unused")
    fun desmarcarRealizado(usuarioId: Int, ejercicioId: Int, fecha: String) {
        viewModelScope.launch {
            try {
                realizadoRepo.desmarcarRealizado(usuarioId, ejercicioId, fecha)
                cargarRealizadosHoy(usuarioId, fecha)
                Timber.d("Ejercicio desmarcado: $ejercicioId")
            } catch (e: Exception) {
                Timber.e(e, "Error al desmarcar ejercicio")
            }
        }
    }

    @Suppress("UNUSED_PARAMETER")
    fun cambiarEjercicio(enfermedad: String, ejercicioId: Int, currentDate: String) {
        viewModelScope.launch {
            try {
                val currentEjercicios = _ejercicios.value
                val currentIds = currentEjercicios.map { it.id }.toSet()
                val availableEjercicios = ejercicioRepo.obtenerEjerciciosPorEnfermedad(enfermedad)
                    .filter { it.id !in currentIds }
                val newEjercicio = availableEjercicios.shuffled().firstOrNull()
                if (newEjercicio != null) {
                    val newList = currentEjercicios.map { ejercicio ->
                        if (ejercicio.id == ejercicioId) {
                            Ejercicio(
                                id = newEjercicio.id,
                                enfermedad = newEjercicio.enfermedad,
                                nombre = newEjercicio.nombre,
                                descripcion = newEjercicio.descripcion,
                                duracion = newEjercicio.duracion,
                                intensidad = newEjercicio.intensidad,
                                imagenResId = newEjercicio.imagenResId,
                                series = newEjercicio.series,
                                descansoEntreSeries = newEjercicio.descansoEntreSeries
                            )
                        } else {
                            ejercicio
                        }
                    }
                    _ejercicios.value = newList
                    val idsJson = JSONArray(newList.map { it.id }).toString()
                    prefs.edit().putString("current_exercise_ids", idsJson).apply()
                    Timber.d("Ejercicio cambiado: $ejercicioId -> ${newEjercicio.id}")
                } else {
                    Timber.w("No hay ejercicios disponibles para cambiar")
                }
            } catch (e: Exception) {
                Timber.e(e, "Error al cambiar ejercicio")
            }
        }
    }
}