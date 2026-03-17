package com.example.doc.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.doc.data.entities.ConsejoEntity
import com.example.doc.repository.ConsejoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class ConsejoViewModel @Inject constructor(
    private val consejoRepo: ConsejoRepository
) : ViewModel() {
    private val _consejos = MutableStateFlow<List<ConsejoEntity>>(emptyList())
    val consejos: StateFlow<List<ConsejoEntity>> = _consejos

    // FIX #6: Estado para consejo aleatorio sin cargar lista completa en memoria
    private val _consejoAleatorio = MutableStateFlow<ConsejoEntity?>(null)
    val consejoAleatorio: StateFlow<ConsejoEntity?> = _consejoAleatorio

    fun cargarConsejos(enfermedad: String) {
        viewModelScope.launch {
            try {
                _consejos.value = consejoRepo.obtenerConsejosPorEnfermedad(enfermedad)
                Timber.d("Consejos cargados para $enfermedad: ${_consejos.value.size}")
            } catch (e: Exception) {
                Timber.e(e, "Error al cargar consejos")
                _consejos.value = emptyList()
            }
        }
    }

    // FIX #6: Cargar consejo aleatorio directamente de BD (O(1), no carga lista)
    fun cargarConsejoAleatorio(enfermedad: String) {
        viewModelScope.launch {
            try {
                _consejoAleatorio.value = consejoRepo.obtenerConsejoAleatorio(enfermedad)
                Timber.d("Consejo aleatorio cargado de BD: ${_consejoAleatorio.value?.consejo}")
            } catch (e: Exception) {
                Timber.e(e, "Error al cargar consejo aleatorio")
                _consejoAleatorio.value = null
            }
        }
    }

    @Suppress("unused")
    fun consejoAleatorio(): String {
        return try {
            // FIX: Acceder por indice en lugar de randomOrNull()
            // Esto evita O(n) random generation y reasignacion innecesaria
            if (_consejos.value.isEmpty()) "Cuida tu salud diariamente."
            else _consejos.value[(Math.random() * _consejos.value.size).toInt()].consejo
        } catch (e: Exception) {
            Timber.e(e, "Error al obtener consejo aleatorio")
            "Cuida tu salud diariamente."
        }
    }
}