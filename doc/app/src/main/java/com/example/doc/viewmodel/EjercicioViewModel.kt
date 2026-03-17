package com.example.doc.viewmodel

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.doc.model.Ejercicio
import com.example.doc.repository.EjercicioRepository
import com.example.doc.repository.EjercicioRealizadoRepository
import com.example.doc.repository.SyncRepository
import com.example.doc.utils.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
    private val syncRepository: SyncRepository,
    private val prefs: SharedPreferences
) : ViewModel() {

    private val _ejercicios = MutableStateFlow<List<Ejercicio>>(emptyList())
    val ejercicios: StateFlow<List<Ejercicio>> = _ejercicios

    private val _realizadosHoy = MutableStateFlow<Set<Int>>(emptySet())
    private var dbObserverJob: Job? = null
    val realizadosHoy: StateFlow<Set<Int>> = _realizadosHoy

    @Suppress("unused")
    private val _uiState = MutableStateFlow<UiState<List<Ejercicio>>>(UiState.Idle)
    val uiState: StateFlow<UiState<List<Ejercicio>>> = _uiState

    fun cargarEjerciciosPorEnfermedad(enfermedad: String, currentDate: String) {
        viewModelScope.launch {
            try {
                _uiState.value = UiState.Loading
                
                // Pequeño delay para que corregirImagenesLocales() complete en onOpen
                delay(150)
                
                // Obtener usuarioId desde SharedPreferences
                val usuarioId = prefs.getInt("user_id", 0)
                Timber.d("📱 Cargando ejercicios: enfermedad=$enfermedad, usuarioId=$usuarioId")
                
                // ✅ Obtener ejercicios generales + asignaciones directas, ordenados DESC
                val allEjercicios = try {
                    ejercicioRepo.obtenerEjerciciosConAsignaciones(enfermedad, usuarioId)
                } catch (e: Exception) {
                    Timber.w(e, "⚠️ Error al obtener ejercicios con asignaciones, usando solo generales")
                    ejercicioRepo.obtenerEjerciciosPorEnfermedad(enfermedad)
                }
                
                Timber.d("📊 Total ejercicios obtenidos: ${allEjercicios.size}")
                
                // ✅ IMPORTANTE: Respetar el orden guardado en SharedPreferences
                // Si el usuario cambió un ejercicio, mantener ese orden local
                val savedIdsJson = prefs.getString("current_exercise_ids", null)
                val newList = if (savedIdsJson != null) {
                    try {
                        val savedIds = org.json.JSONArray(savedIdsJson)
                        val idList = mutableListOf<Int>()
                        for (i in 0 until savedIds.length()) {
                            idList.add(savedIds.getInt(i))
                        }
                        Timber.d("🔄 IDs guardados: $idList, Total disponibles: ${allEjercicios.size}")
                        
                        // Restaurar en el mismo orden que el usuario estableció
                        val ejercicioMap = allEjercicios.associateBy { it.id }
                        val restored = idList.mapNotNull { id -> ejercicioMap[id] }
                        
                        // ✅ Si algunos IDs guardados no existen, agregar nuevos hasta tener 6
                        val result = restored.toMutableList()
                        if (result.size < 6) {
                            val faltantes = 6 - result.size
                            val nuevosEjercicios = allEjercicios
                                .filter { !result.contains(it) }  // No agregar duplicados
                                .take(faltantes)
                            result.addAll(nuevosEjercicios)
                            Timber.d("✅ Restaurados ${restored.size} + Agregados ${nuevosEjercicios.size}")
                        }
                        // Limitar a 6 ejercicios máximo
                        if (result.size > 6) {
                            result.take(6)
                        } else {
                            result
                        }
                    } catch (e: Exception) {
                        Timber.w(e, "⚠️ Error al restaurar orden guardado, usando primeros 6")
                        allEjercicios.take(6)
                    }
                } else {
                    // Primera carga: tomar primeros 6
                    allEjercicios.take(6)
                }
                
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
                        descansoEntreSeries = entity.descansoEntreSeries,
                        urlImagen = entity.urlImagen,
                        urlVideo = entity.urlVideo
                    )
                }
                
                val idsJson = JSONArray(_ejercicios.value.map { it.id }).toString()
                prefs.edit().putString("current_exercise_ids", idsJson)
                    .putString("last_update_date", currentDate)
                    .apply()
                Timber.d("✅ Ejercicios cargados (con asignaciones): $enfermedad, total: ${_ejercicios.value.size}")

                // Observer reactivo PASIVO:
                // ✅ Solo actualiza datos (imagenResId, urlImagen, nombre, descripción)
                // ❌ NO agrega nuevos ejercicios - eso es manual o en otra pantalla
                dbObserverJob?.cancel()
                dbObserverJob = viewModelScope.launch {
                    ejercicioRepo.obtenerEjerciciosPorEnfermedadFlow(enfermedad)
                        .collect { entities ->
                            val entityMap = entities.associateBy { it.id }
                            val current = _ejercicios.value
                            
                            if (current.isNotEmpty()) {
                                // ♻️ Solo actualizar los ejercicios que ya están en la lista
                                val updated = current.map { ej ->
                                    val e = entityMap[ej.id]
                                    if (e != null) {
                                        ej.copy(
                                            imagenResId = e.imagenResId,
                                            urlImagen = e.urlImagen,
                                            nombre = e.nombre,  // Si lo editaron
                                            descripcion = e.descripcion,
                                            duracion = e.duracion,
                                            intensidad = e.intensidad,
                                            series = e.series,
                                            descansoEntreSeries = e.descansoEntreSeries,
                                            urlVideo = e.urlVideo
                                        )
                                    } else ej
                                }
                                
                                if (updated != current) {
                                    _ejercicios.value = updated
                                    // Guardar nuevo orden por si cambió algo
                                    val idsJson = JSONArray(updated.map { it.id }).toString()
                                    prefs.edit().putString("current_exercise_ids", idsJson).apply()
                                    Timber.d("♻️ Ejercicios actualizados (datos del DB)")
                                }
                            }
                        }
                }
                _uiState.value = UiState.Success(_ejercicios.value)
            } catch (e: Exception) {
                Timber.e(e, "Error al cargar ejercicios")
                _uiState.value = UiState.Error("Error al cargar ejercicios", e)
            }
        }
    }

    fun cargarEjerciciosActualizados(enfermedad: String, currentDate: String) {
        viewModelScope.launch {
            try {
                _uiState.value = UiState.Loading
                Timber.d("🔄 REFRESH FORZADO: Descargando nuevos ejercicios del backend...")
                
                val usuarioId = prefs.getInt("user_id", 0)
                
                // 1️⃣ Forzar PULL desde backend
                val syncResult = syncRepository.fullSync(usuarioId, forcePull = true)
                Timber.d("📡 PULL resultado: $syncResult")
                
                // 2️⃣ Cargar ejercicios (ahora con datos actualizados del backend)
                val allEjercicios = try {
                    ejercicioRepo.obtenerEjerciciosConAsignaciones(enfermedad, usuarioId)
                } catch (e: Exception) {
                    Timber.w(e, "⚠️ Error al obtener nuevos ejercicios, usando generales")
                    ejercicioRepo.obtenerEjerciciosPorEnfermedad(enfermedad)
                }
                
                Timber.d("📊 Nuevos ejercicios descargados: ${allEjercicios.size}")
                
                // 3️⃣ Mostrar primeros 6 en ASC (más antiguos primero), SIN restaurar guardados
                val newList = allEjercicios.reversed().take(6)
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
                        descansoEntreSeries = entity.descansoEntreSeries,
                        urlImagen = entity.urlImagen,
                        urlVideo = entity.urlVideo
                    )
                }
                
                // 4️⃣ Guardar nuevos IDs
                val idsJson = JSONArray(_ejercicios.value.map { it.id }).toString()
                prefs.edit()
                    .putString("current_exercise_ids", idsJson)
                    .putString("last_update_date", currentDate)
                    .apply()
                
                Timber.d("✅ REFRESH completado: ${_ejercicios.value.size} nuevos ejercicios mostrados")
                _uiState.value = UiState.Success(_ejercicios.value)
            } catch (e: Exception) {
                Timber.e(e, "❌ Error en REFRESH forzado")
                _uiState.value = UiState.Error("Error al actualizar", e)
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
                        fecha = fecha,
                        isSynced = false  // Explícitamente NO sincronizado
                    )
                )
                cargarRealizadosHoy(usuarioId, fecha)
                Timber.d("✅ Ejercicio marcado como realizado: $ejercicioId (isSynced=false)")
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
                Timber.d("🔄 Iniciando cambio de ejercicio: $ejercicioId")
                val usuarioId = prefs.getInt("user_id", 0)
                
                val currentEjercicios = _ejercicios.value
                val oldEjercicio = currentEjercicios.find { it.id == ejercicioId }
                val currentIds = currentEjercicios.map { it.id }.toSet()
                
                // ✅ Obtener TODOS los ejercicios disponibles (generales + asignaciones)
                val availableEjercicios = ejercicioRepo.obtenerEjerciciosConAsignaciones(enfermedad, usuarioId)
                    .filter { it.id !in currentIds }
                
                val newEjercicio = availableEjercicios.shuffled().firstOrNull()
                
                if (newEjercicio != null) {
                    // ✅ Actualizar UI localmente INMEDIATAMENTE
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
                                descansoEntreSeries = newEjercicio.descansoEntreSeries,
                                urlImagen = newEjercicio.urlImagen,
                                urlVideo = newEjercicio.urlVideo
                            )
                        } else {
                            ejercicio
                        }
                    }
                    _ejercicios.value = newList
                    val idsJson = JSONArray(newList.map { it.id }).toString()
                    prefs.edit().putString("current_exercise_ids", idsJson).apply()
                    Timber.d("✅ CAMBIO VISUAL: ${oldEjercicio?.nombre} ($ejercicioId) → ${newEjercicio.nombre} (${newEjercicio.id})")
                    
                    // ✅ Sincronizar con Backend (PUSH) - sin recargar
                    try {
                        syncRepository.fullSync(usuarioId)
                        Timber.d("✅ Cambio sincronizado con Backend")
                    } catch (e: Exception) {
                        Timber.w(e, "⚠️ Sync falló, pero cambio local se mantuvo: ${newEjercicio.nombre}")
                    }
                } else {
                    Timber.w("⚠️ No hay ejercicios disponibles para cambiar (todos ya en lista)")
                }
            } catch (e: Exception) {
                Timber.e(e, "❌ Error al cambiar ejercicio")
            }
        }
    }
}