package com.example.doc.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.doc.repository.UsuarioRepository
import com.example.doc.data.entities.UsuarioEntity
import com.example.doc.model.Usuario
import com.example.doc.utils.UiState
import com.example.doc.security.InputValidator
import com.example.doc.utils.NotificationHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class UsuarioViewModel @Inject constructor(
    private val repository: UsuarioRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val prefs = context.getSharedPreferences("doc_prefs", Context.MODE_PRIVATE)

    private val _usuario = MutableStateFlow<Usuario?>(null)
    val usuario: StateFlow<Usuario?> = _usuario

    @Suppress("unused")
    private val _uiState = MutableStateFlow<UiState<Usuario>>(UiState.Idle)
    val uiState: StateFlow<UiState<Usuario>> = _uiState

    init {
        viewModelScope.launch {
            try {
                repository.obtenerUsuarioFlow()
                    .map { entity ->
                        entity?.let {
                            Usuario(
                                id = it.id,
                                cedula = it.cedula,
                                nombre = it.nombre,
                                edad = it.edad,
                                peso = it.peso,
                                altura = it.altura,
                                sexo = it.sexo,
                                enfermedad = it.enfermedad,
                                colorEnfermedad = it.colorEnfermedad,
                                notifEnabled = it.notifEnabled,
                                notifType = it.notifType,
                                notifDetails = it.notifDetails,
                                glucosaInicial = it.glucosaInicial,
                                colesterolInicial = it.colesterolInicial
                            )
                        }
                    }
                    .stateIn(viewModelScope)
                    .collect { usuario ->
                        _usuario.value = usuario
                        usuario?.let {
                            setupNotifications(it)
                            Timber.d("Usuario cargado: ${it.nombre}")
                        }
                    }
            } catch (e: kotlinx.coroutines.CancellationException) {
                // Cancelación normal de coroutine - no es un error
                Timber.d("Flow de usuario cancelado (comportamiento normal)")
                throw e // Re-lanzar para que la coroutine se cancele correctamente
            } catch (e: Exception) {
                Timber.e(e, "Error al cargar usuario")
                _uiState.value = UiState.Error("Error al cargar usuario", e)
            }
        }
    }

    fun registrarUsuario(usuario: Usuario) {
        viewModelScope.launch {
            try {
                _uiState.value = UiState.Loading

                Timber.d("Registrando usuario: ${usuario.nombre}")

                // Validar datos antes de guardar
                if (usuario.cedula != null) {
                    val validationResult = InputValidator.validateUserRegistration(
                        cedula = usuario.cedula,
                        nombre = usuario.nombre,
                        edad = usuario.edad,
                        peso = usuario.peso,
                        altura = usuario.altura
                    )

                    if (!validationResult.isValid) {
                        _uiState.value = UiState.Error(
                            validationResult.errorMessage ?: "Datos inválidos",
                            null
                        )
                        Timber.w("Validación fallida: ${validationResult.errorMessage}")
                        return@launch
                    }
                } else {
                    // Validar solo los demás campos si no hay cédula
                    if (!InputValidator.isValidName(usuario.nombre)) {
                        _uiState.value = UiState.Error("Nombre inválido", null)
                        return@launch
                    }
                    if (!InputValidator.isValidAge(usuario.edad)) {
                        _uiState.value = UiState.Error("Edad inválida", null)
                        return@launch
                    }
                    if (!InputValidator.isValidPeso(usuario.peso)) {
                        _uiState.value = UiState.Error("Peso inválido", null)
                        return@launch
                    }
                    if (!InputValidator.isValidAltura(usuario.altura)) {
                        _uiState.value = UiState.Error("Altura inválida", null)
                        return@launch
                    }
                }

                val entity = UsuarioEntity(
                    id = usuario.id,
                    cedula = usuario.cedula,
                    nombre = usuario.nombre,
                    edad = usuario.edad,
                    peso = usuario.peso,
                    altura = usuario.altura,
                    sexo = usuario.sexo,
                    enfermedad = usuario.enfermedad,
                    colorEnfermedad = usuario.colorEnfermedad,
                    notifEnabled = usuario.notifEnabled,
                    notifType = usuario.notifType,
                    notifDetails = usuario.notifDetails,
                    glucosaInicial = usuario.glucosaInicial,
                    colesterolInicial = usuario.colesterolInicial
                )
                repository.guardarUsuario(entity)
                _uiState.value = UiState.Success(usuario)
                Timber.d("✅ Usuario registrado exitosamente: ${usuario.nombre}")

                // Forzar recarga del usuario
                cargarUsuario()
            } catch (e: Exception) {
                Timber.e(e, "❌ Error al registrar usuario")
                _uiState.value = UiState.Error("Error al registrar usuario", e)
            }
        }
    }

    /**
     * Método para forzar la carga del usuario desde la base de datos
     */
    fun cargarUsuario() {
        viewModelScope.launch {
            try {
                Timber.d("🔄 Forzando carga de usuario...")
                val entity = repository.obtenerUsuario()
                if (entity != null) {
                    val usuario = Usuario(
                        id = entity.id,
                        cedula = entity.cedula,
                        nombre = entity.nombre,
                        edad = entity.edad,
                        peso = entity.peso,
                        altura = entity.altura,
                        sexo = entity.sexo,
                        enfermedad = entity.enfermedad,
                        colorEnfermedad = entity.colorEnfermedad,
                        notifEnabled = entity.notifEnabled,
                        notifType = entity.notifType,
                        notifDetails = entity.notifDetails,
                        glucosaInicial = entity.glucosaInicial,
                        colesterolInicial = entity.colesterolInicial
                    )
                    _usuario.value = usuario
                    Timber.d("✅ Usuario cargado: ${usuario.nombre}")
                } else {
                    Timber.w("⚠️ No se encontró usuario en la base de datos")
                }
            } catch (e: Exception) {
                Timber.e(e, "❌ Error al cargar usuario")
            }
        }
    }

    fun actualizarUsuario(usuario: Usuario) {
        viewModelScope.launch {
            try {
                _uiState.value = UiState.Loading
                val entity = UsuarioEntity(
                    id = usuario.id,
                    cedula = usuario.cedula,
                    nombre = usuario.nombre,
                    edad = usuario.edad,
                    peso = usuario.peso,
                    altura = usuario.altura,
                    sexo = usuario.sexo,
                    enfermedad = usuario.enfermedad,
                    colorEnfermedad = usuario.colorEnfermedad,
                    notifEnabled = usuario.notifEnabled,
                    notifType = usuario.notifType,
                    notifDetails = usuario.notifDetails,
                    glucosaInicial = usuario.glucosaInicial,
                    colesterolInicial = usuario.colesterolInicial
                )
                repository.actualizarUsuario(entity)
                setupNotifications(usuario)
                _uiState.value = UiState.Success(usuario)
                Timber.d("Usuario actualizado: ${usuario.nombre}")
            } catch (e: Exception) {
                Timber.e(e, "Error al actualizar usuario")
                _uiState.value = UiState.Error("Error al actualizar usuario", e)
            }
        }
    }

    @Suppress("unused")
    fun actualizarPeso(peso: Double) {
        viewModelScope.launch {
            try {
                _usuario.value?.let { usuario ->
                    val updatedUser = usuario.copy(peso = peso)
                    val entity = UsuarioEntity(
                        id = updatedUser.id,
                        cedula = updatedUser.cedula,
                        nombre = updatedUser.nombre,
                        edad = updatedUser.edad,
                        peso = updatedUser.peso,
                        altura = updatedUser.altura,
                        sexo = updatedUser.sexo,
                        enfermedad = updatedUser.enfermedad,
                        colorEnfermedad = updatedUser.colorEnfermedad,
                        notifEnabled = updatedUser.notifEnabled,
                        notifType = updatedUser.notifType,
                        notifDetails = updatedUser.notifDetails,
                        glucosaInicial = updatedUser.glucosaInicial,
                        colesterolInicial = updatedUser.colesterolInicial
                    )
                    repository.actualizarUsuario(entity)
                    Timber.d("Peso actualizado a: $peso kg")
                }
            } catch (e: Exception) {
                Timber.e(e, "Error al actualizar peso")
                _uiState.value = UiState.Error("Error al actualizar peso", e)
            }
        }
    }

    fun updateNotifications(enabled: Boolean, type: String, details: String) {
        viewModelScope.launch {
            try {
                _usuario.value?.let { user ->
                    val updated = user.copy(
                        notifEnabled = enabled,
                        notifType = type,
                        notifDetails = details
                    )
                    val entity = UsuarioEntity(
                        id = updated.id,
                        cedula = updated.cedula,
                        nombre = updated.nombre,
                        edad = updated.edad,
                        peso = updated.peso,
                        altura = updated.altura,
                        sexo = updated.sexo,
                        enfermedad = updated.enfermedad,
                        colorEnfermedad = updated.colorEnfermedad,
                        notifEnabled = updated.notifEnabled,
                        notifType = updated.notifType,
                        notifDetails = updated.notifDetails,
                        glucosaInicial = updated.glucosaInicial,
                        colesterolInicial = updated.colesterolInicial
                    )
                    repository.actualizarUsuario(entity)
                    setupNotifications(updated)
                    Timber.d("Notificaciones actualizadas: enabled=$enabled, type=$type")
                }
            } catch (e: Exception) {
                Timber.e(e, "Error al actualizar notificaciones")
                _uiState.value = UiState.Error("Error al actualizar notificaciones", e)
            }
        }
    }

    private fun setupNotifications(user: Usuario) {
        try {
            Timber.d("Configurando notificaciones para usuario: ${user.nombre}")

            // Crear canal de notificaciones
            NotificationHelper.createNotificationChannel(context)

            if (!user.notifEnabled) {
                // Cancelar notificaciones si están deshabilitadas
                NotificationHelper.cancelNotifications(context)
                NotificationHelper.cancelExerciseNotifications(context)
                Timber.d("Notificaciones deshabilitadas para usuario")
                return
            }

            // Guardar detalles de notificación en SharedPreferences para el receiver
            prefs.edit().apply {
                putInt("user_id", user.id)
                putString("user_name", user.nombre)
                putBoolean("notif_enabled_${user.id}", user.notifEnabled)
                putString("notif_type_${user.id}", user.notifType)
                putString("notif_details_${user.id}", user.notifDetails)
                apply()
            }

            // Programar notificaciones de registro de salud según configuración
            NotificationHelper.scheduleNotifications(
                context = context,
                notifType = user.notifType,
                notifDetails = user.notifDetails
            )

            // Programar notificaciones de ejercicios (2 por día: 10:00 AM y 6:00 PM)
            NotificationHelper.scheduleExerciseNotifications(context)

            Timber.d("Notificaciones configuradas exitosamente (Salud + Ejercicios)")
        } catch (e: Exception) {
            Timber.e(e, "Error al configurar notificaciones")
        }
    }

    @Suppress("unused")
    fun limpiarUsuario() {
        viewModelScope.launch {
            try {
                repository.eliminarTodos()
                NotificationHelper.cancelNotifications(context)
                NotificationHelper.cancelExerciseNotifications(context)
                Timber.d("Usuario limpiado exitosamente")
            } catch (e: Exception) {
                Timber.e(e, "Error al limpiar usuario")
                _uiState.value = UiState.Error("Error al limpiar usuario", e)
            }
        }
    }
}