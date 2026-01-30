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

    @Suppress("unused")
    fun consejoAleatorio(): String {
        return try {
            _consejos.value.randomOrNull()?.consejo ?: "Cuida tu salud diariamente."
        } catch (e: Exception) {
            Timber.e(e, "Error al obtener consejo aleatorio")
            "Cuida tu salud diariamente."
        }
    }
}