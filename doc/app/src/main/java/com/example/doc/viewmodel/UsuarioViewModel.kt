package com.example.doc.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.doc.repository.UsuarioRepository
import com.google.firebase.messaging.FirebaseMessaging
import com.example.doc.repository.SyncRepository
import com.example.doc.data.entities.UsuarioEntity
import com.example.doc.model.Usuario
import com.example.doc.utils.UiState
import com.example.doc.security.InputValidator
import com.example.doc.utils.NotificationHelper
import com.example.doc.network.ApiService
import com.example.doc.network.CreateUsuarioRequest
import com.example.doc.utils.NetworkMonitor
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

enum class SyncState {
    IDLE,              // No sincronizando, ocultar icono
    SYNCING,           // Sincronizando...
    SYNCED,            // Sincronizado ✓
    PENDING,           // Pendiente (sin internet o error)
    ERROR              // Error en sincronización
}

@HiltViewModel
class UsuarioViewModel @Inject constructor(
    private val repository: UsuarioRepository,
    private val syncRepository: SyncRepository,
    private val apiService: ApiService,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val prefs = context.getSharedPreferences("doc_prefs", Context.MODE_PRIVATE)

    private val _usuario = MutableStateFlow<Usuario?>(null)
    val usuario: StateFlow<Usuario?> = _usuario

    // Estados de sincronización mejorados
    private val _syncState = MutableStateFlow(SyncState.IDLE)
    val syncState: StateFlow<SyncState> = _syncState

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing

    private val _isSynced = MutableStateFlow(true)
    val isSynced: StateFlow<Boolean> = _isSynced

    private val _lastSyncTime = MutableStateFlow<String?>(null)
    val lastSyncTime: StateFlow<String?> = _lastSyncTime

    // 🔄 Overlay de sincronización profesional
    private val _syncOverlayVisible = MutableStateFlow(false)
    val syncOverlayVisible: StateFlow<Boolean> = _syncOverlayVisible

    private val _syncOverlayMessage = MutableStateFlow("Sincronizando...")
    val syncOverlayMessage: StateFlow<String> = _syncOverlayMessage

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
                                apellido = it.apellido,
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
                                colesterolInicial = it.colesterolInicial,
                                estadoCuenta = it.estadoCuenta
                            )
                        }
                    }
                    .stateIn(viewModelScope)
                    .collect { usuario ->
                        _usuario.value = usuario
                        usuario?.let {
                            setupNotifications(it)
                            Timber.d("Usuario cargado: ${it.nombre}")

                            // Ejecutar sincronización automática al cargar usuario
                            performAutoSync(it.id)
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

        // Observar cambios de conectividad y sincronizar cuando hay internet
        viewModelScope.launch {
            var lastSyncCheckTime = 0L
            com.example.doc.utils.NetworkMonitor.isConnected.collect { isConnected ->
                if (isConnected) {
                    val usuario = _usuario.value
                    if (usuario != null) {
                        // Evitar sincronizaciones más de 1 vez por minuto
                        val now = System.currentTimeMillis()
                        if (now - lastSyncCheckTime > 60000) { // 60 segundos
                            lastSyncCheckTime = now
                            
                            Timber.d("📡 Internet disponible - Evaluando si sincronizar...")
                            
                            // Verificar si ya se ejecutó el overlay de forma exitosa
                            val alreadyExecutedFlag = prefs.getBoolean("auto_sync_overlay_executed_${usuario.id}", false)
                            
                            // Si NO se ha ejecutado de forma exitosa, intentar sincronizar
                            if (!alreadyExecutedFlag) {
                                Timber.d("🔄 Reconexión detectada - Sincronizando automáticamente (Flag: $alreadyExecutedFlag)")
                                sincronizarAutomaticamente()
                            } else {
                                Timber.d("✅ Ya fue sincronizado exitosamente, sin acción necesaria")
                            }
                        }
                    }
                }
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

                // 1️⃣ Primero, crear el usuario en el backend (Supabase)
                Timber.d("📤 Sincronizando usuario con backend...")
                val fcmToken = prefs.getString("fcm_token", null)
                
                // Calcular fecha de nacimiento aproximada si tenemos edad
                val fechaNacimiento = usuario.edad?.let { edad ->
                    val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
                    val birthYear = currentYear - edad
                    "$birthYear-01-01"  // Usar 1 de enero como fecha aproximada
                }
                
                val createUsuarioRequest = CreateUsuarioRequest(
                    nombre = usuario.nombre,
                    apellido = usuario.apellido ?: "",
                    cedula = usuario.cedula,
                    fechaNacimiento = fechaNacimiento,
                    edad = usuario.edad,
                    peso = usuario.peso,
                    altura = usuario.altura,
                    sexo = usuario.sexo,
                    enfermedad = usuario.enfermedad,
                    colorEnfermedad = usuario.colorEnfermedad,
                    tokenDispositivo = fcmToken,
                    estadoCuenta = "ACTIVE"  // Por defecto ACTIVE
                )

                val backendResponse = try {
                    val response = apiService.createUsuario(createUsuarioRequest)
                    if (response.isSuccessful && response.body() != null) {
                        Timber.d("✅ Usuario creado en backend con ID: ${response.body()!!.usuario.id}")
                        response.body()
                    } else {
                        Timber.w("⚠️ Error al crear usuario en backend: ${response.code()} - ${response.message()}")
                        null
                    }
                } catch (e: Exception) {
                    Timber.e(e, "❌ Error en llamada backend")
                    null
                }

                // 2️⃣ Usar el ID del backend si fue exitoso, si no usar el local
                val usuarioFinal = if (backendResponse != null) {
                    // Actualizar ID del usuario con el ID del backend
                    usuario.copy(id = backendResponse.usuario.id)
                } else {
                    Timber.w("⚠️ No se pudo sincronizar con backend, usando ID local")
                    usuario
                }

                // 3️⃣ Guardar el usuario localmente con el ID correcto
                val entity = UsuarioEntity(
                    id = usuarioFinal.id,
                    cedula = usuarioFinal.cedula,
                    nombre = usuarioFinal.nombre,
                    apellido = usuarioFinal.apellido,
                    edad = usuarioFinal.edad,
                    peso = usuarioFinal.peso,
                    altura = usuarioFinal.altura,
                    sexo = usuarioFinal.sexo,
                    enfermedad = usuarioFinal.enfermedad,
                    colorEnfermedad = usuarioFinal.colorEnfermedad,
                    notifEnabled = usuarioFinal.notifEnabled,
                    notifType = usuarioFinal.notifType,
                    notifDetails = usuarioFinal.notifDetails,
                    glucosaInicial = usuarioFinal.glucosaInicial,
                    colesterolInicial = usuarioFinal.colesterolInicial,
                    estadoCuenta = "ACTIVE"
                )
                repository.guardarUsuario(entity)

                // Guardar ID en SharedPreferences para sincronización
                prefs.edit().putInt("user_id", usuarioFinal.id).apply()
                
                // ✅ LIMPIAR flag de PULL para que se sincronice en el próximo arranque
                prefs.edit().remove("first_pull_done_${usuarioFinal.id}").apply()
                Timber.d("🔄 Flag de PULL limpiado para usuario ${usuarioFinal.id}")

                // Suscribirse al tema de notificaciones personales inmediatamente
                FirebaseMessaging.getInstance().subscribeToTopic("user_${usuarioFinal.id}")
                    .addOnSuccessListener {
                        Timber.d("✅ Suscrito al tema personal: user_${usuarioFinal.id}")
                    }
                    .addOnFailureListener { e ->
                        Timber.e(e, "❌ Error al suscribirse a tema personal: user_${usuarioFinal.id}")
                    }

                _uiState.value = UiState.Success(usuarioFinal)
                Timber.d("✅ Usuario registrado exitosamente: ${usuarioFinal.nombre} (ID: ${usuarioFinal.id})")

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
                        apellido = entity.apellido,
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

    /**
     * 🔍 Verificar si un usuario existe localmente por cédula
     * (Usado para detectar si es un registro nuevo o usuario existente)
     */
    suspend fun usuarioExisteLocalmente(cedula: String?): Boolean {
        return try {
            repository.existePorCedula(cedula)
        } catch (e: Exception) {
            Timber.e(e, "❌ Error verificando usuario por cédula")
            false
        }
    }

    /**
     * 🔍 Obtener usuario localmente por cédula
     * (Usado para cargar datos de usuario existente)
     */
    suspend fun obtenerUsuarioLocalmente(cedula: String?): UsuarioEntity? {
        return try {
            repository.obtenerPorCedula(cedula)
        } catch (e: Exception) {
            Timber.e(e, "❌ Error obteniendo usuario por cédula")
            null
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
                    apellido = usuario.apellido,
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

                // Forzar sincronización inmediata al cambiar datos de usuario
                forzarSincronizacion()
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
                        apellido = updatedUser.apellido,
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
                    // Actualizar localmente
                    repository.actualizarUsuario(entity)
                    setupNotifications(updated)
                    Timber.d("Notificaciones actualizadas: enabled=$enabled, type=$type")

                    // 🔄 SINCRONIZAR CON BACKEND
                    try {
                        val response = syncRepository.actualizarNotificacionesEnBackend(
                            usuarioId = updated.id,
                            notifEnabled = enabled,
                            notifType = type,
                            notifDetails = details
                        )
                        if (response != null) {
                            Timber.d("✅ Notificaciones sincronizadas con backend")
                        } else {
                            Timber.w("⚠️ Error al sincronizar notificaciones con backend")
                        }
                    } catch (syncError: Exception) {
                        Timber.e(syncError, "❌ Error sincronizando notificaciones")
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error al actualizar notificaciones")
                _uiState.value = UiState.Error("Error al actualizar notificaciones", e)
            }
        }
    }

    /**
     * Sincronización automática cuando se carga el usuario
     */
    private fun performAutoSync(usuarioId: Int) {
        viewModelScope.launch {
            try {
                _isSyncing.value = true

                Timber.d("🔄 Iniciando sincronización automática para usuario: $usuarioId")

                // 🔍 NUEVO: Si es la primera sincronización y hay cédula, intentar recuperar cuenta
                val usuario = _usuario.value
                if (usuario?.cedula != null && 
                    prefs.getBoolean("primer_sync_intento", true) &&
                    NetworkMonitor.isConnected.value) {
                    
                    Timber.d("🔍 Primer intento de sync con internet - Buscando usuario por cédula...")
                    val searchResult = syncRepository.searchAndSyncByCedula(usuario.cedula!!)
                    
                    if (searchResult.isSuccess && searchResult.getOrNull() != null) {
                        Timber.d("✅ Usuario recuperado por cédula, sincronización completada")
                        prefs.edit().putBoolean("primer_sync_intento", false).apply()
                        _isSynced.value = true
                        _isSyncing.value = false
                        return@launch
                    } else {
                        Timber.d("ℹ️ Usuario no encontrado por cédula, continuando con fullSync normal")
                        prefs.edit().putBoolean("primer_sync_intento", false).apply()
                    }
                }

                // Sync normal
                val result = syncRepository.fullSync(usuarioId)

                result.onSuccess {
                    Timber.d("✅ Sincronización automática completada")
                    _isSynced.value = true

                    // Actualizar tiempo de última sincronización
                    val lastSync = prefs.getString("last_sync", null)
                    _lastSyncTime.value = lastSync
                }

                result.onFailure { error ->
                    Timber.e(error, "❌ Error en sincronización automática")
                    _isSynced.value = false
                }
            } catch (e: Exception) {
                Timber.e(e, "❌ Excepción en sincronización automática")
                _isSynced.value = false
            } finally {
                _isSyncing.value = false
            }
        }
    }

    private fun setupNotifications(user: Usuario) {
        try {
            Timber.d("Configurando notificaciones para usuario: ${user.nombre}")

            // Crear canal de notificaciones
            NotificationHelper.createNotificationChannel(context)

            // Guardar detalles de notificación en SharedPreferences
            // NO programar alarmas aquí - HybridNotificationManager lo hace automáticamente
            prefs.edit().apply {
                putInt("user_id", user.id)
                putString("user_name", user.nombre)
                putBoolean("notif_enabled_${user.id}", user.notifEnabled)
                putString("notif_type_${user.id}", user.notifType)
                putString("notif_details_${user.id}", user.notifDetails)
                apply()
            }

            if (!user.notifEnabled) {
                // Si notificaciones están deshabilitadas, cancelar alarmas existentes
                NotificationHelper.cancelNotifications(context)
                NotificationHelper.cancelExerciseNotifications(context)
                Timber.d("Notificaciones deshabilitadas para usuario")
                return
            }

            // HybridNotificationManager manejará automáticamente la programación de alarmas
            // según si hay conexión a internet o no.
            // Solo guardar configuración en prefs para que HybridNotificationManager acceda
            Timber.d("Configuración de notificaciones guardada - HybridNotificationManager manejará programación")
        } catch (e: Exception) {
            Timber.e(e, "Error al configurar notificaciones")
        }
    }

    @Suppress("unused")
    fun limpiarUsuario() {
        viewModelScope.launch {
            try {
                // Limpiar todas las banderas de sync para este usuario antes de eliminarlo
                val usuarioActual = usuario.value
                if (usuarioActual != null) {
                    prefs.edit().remove("auto_sync_overlay_executed_${usuarioActual.id}").apply()
                }
                
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

    /**
     * Sincronización manual (presionar icono)
     * Muestra estados: SYNCING → SYNCED (oculta después 2s) o PENDING
     */
    fun sincronizarManualmente() {
        viewModelScope.launch {
            try {
                _usuario.value?.let { usuario ->
                    Timber.d("📲 Sincronización MANUAL iniciada para usuario: ${usuario.nombre}")
                    _syncState.value = SyncState.SYNCING
                    _isSyncing.value = true

                    val resultado = syncRepository.fullSync(usuario.id)

                    if (resultado.isSuccess) {
                        _syncState.value = SyncState.SYNCED
                        _isSynced.value = true
                        _lastSyncTime.value = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
                        Timber.d("✅ Sincronización manual completada exitosamente")

                        // Ocultar icono después de 2 segundos
                        kotlinx.coroutines.delay(2000)
                        _syncState.value = SyncState.IDLE
                    } else {
                        // Detectar si el error es por falta de internet
                        val esFaltaInternet = isNetworkError(resultado.exceptionOrNull())
                        _syncState.value = if (esFaltaInternet) SyncState.PENDING else SyncState.ERROR
                        _isSynced.value = false
                        Timber.e("⚠️ Sincronización manual falló: ${resultado.exceptionOrNull()?.message}")
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "❌ Error en sincronización manual")
                _syncState.value = if (isNetworkError(e)) SyncState.PENDING else SyncState.ERROR
                _isSyncing.value = false
            } finally {
                _isSyncing.value = false
            }
        }
    }

    /**
     * Sincronización automática (cada 1 minuto)
     * Se llama desde SyncWorker O cuando se reconecta internet
     */
    fun sincronizarAutomaticamente() {
        viewModelScope.launch {
            try {
                _usuario.value?.let { usuario ->
                    Timber.d("⏰ Sincronización AUTOMÁTICA iniciada para usuario: ${usuario.nombre}")
                    
                    // 🔍 Intentar recuperar por cédula si es el primer intento
                    if (usuario.cedula != null && 
                        prefs.getBoolean("primer_sync_intento", true) &&
                        NetworkMonitor.isConnected.value) {
                        
                        Timber.d("🔍 Primer intento de sync - Buscando usuario por cédula...")
                        val searchResult = syncRepository.searchAndSyncByCedula(usuario.cedula!!)
                        
                        if (searchResult.isSuccess && searchResult.getOrNull() != null) {
                            Timber.d("✅ Usuario recuperado por cédula en sincronización automática")
                            prefs.edit().putBoolean("primer_sync_intento", false).apply()
                            _syncState.value = SyncState.IDLE
                            _isSynced.value = true
                            _lastSyncTime.value = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
                            return@launch
                        } else {
                            Timber.d("ℹ️ Usuario no encontrado por cédula, continuando con fullSync")
                            prefs.edit().putBoolean("primer_sync_intento", false).apply()
                        }
                    }
                    
                    // Sync normal
                    val resultado = syncRepository.fullSync(usuario.id)

                    if (resultado.isSuccess) {
                        _syncState.value = SyncState.IDLE
                        _isSynced.value = true
                        _lastSyncTime.value = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
                        Timber.d("✅ Sincronización automática completada exitosamente")
                    } else {
                        // Detectar si el error es por falta de internet
                        val esFaltaInternet = isNetworkError(resultado.exceptionOrNull())
                        _syncState.value = if (esFaltaInternet) SyncState.PENDING else SyncState.ERROR
                        _isSynced.value = false
                        Timber.e("⚠️ Sincronización automática falló: ${resultado.exceptionOrNull()?.message}")
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "❌ Error en sincronización automática")
                _syncState.value = if (isNetworkError(e)) SyncState.PENDING else SyncState.ERROR
                _isSynced.value = false
            }
        }
    }

    /**
     * Forzar sincronización inmediata (al cambiar datos de usuario)
     */
    fun forzarSincronizacion() {
        viewModelScope.launch {
            try {
                _usuario.value?.let { usuario ->
                    Timber.d("🔄 Sincronización FORZADA iniciada para usuario: ${usuario.nombre}")
                    _syncState.value = SyncState.SYNCING
                    _isSyncing.value = true
                    val resultado = syncRepository.fullSync(usuario.id)

                    if (resultado.isSuccess) {
                        _syncState.value = SyncState.SYNCED
                        _isSynced.value = true
                        _lastSyncTime.value = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
                        Timber.d("✅ Sincronización forzada completada exitosamente")

                        // Ocultar después de 2 segundos
                        kotlinx.coroutines.delay(2000)
                        _syncState.value = SyncState.IDLE
                    } else {
                        // Detectar si el error es por falta de internet
                        val esFaltaInternet = isNetworkError(resultado.exceptionOrNull())
                        _syncState.value = if (esFaltaInternet) SyncState.PENDING else SyncState.ERROR
                        _isSynced.value = false
                        Timber.e("❌ Error en sincronización forzada: ${resultado.exceptionOrNull()?.message}")
                    }
                    _isSyncing.value = false
                }
            } catch (e: Exception) {
                Timber.e(e, "❌ Error al forzar sincronización")
                _syncState.value = if (isNetworkError(e)) SyncState.PENDING else SyncState.ERROR
                _isSyncing.value = false
            }
        }
    }

    /**
     * Sincronización automática profesional CON OVERLAY
     * Se muestra una pantalla bonita mientras se sincroniza
     * Ideal para cuando el usuario ya estaba registrado y vuelve a entrar
     * Se ejecuta solo UNA VEZ por usuario (persiste entre sesiones)
     */
    fun performAutoSyncWithOverlay(usuarioId: Int) {
        viewModelScope.launch {
            try {
                // Evitar ejecutar múltiples veces si usuario se actualiza constantemente
                // Usar SharedPreferences para que persista entre sesiones
                val alreadyExecuted = prefs.getBoolean("auto_sync_overlay_executed_$usuarioId", false)
                if (alreadyExecuted) {
                    Timber.d("↩️ Auto-sync con overlay ya ejecutado para usuario $usuarioId, saltando...")
                    return@launch
                }
                
                // ⚠️ NO marcar como ejecutado aquí, solo DESPUÉS de sincronizar exitosamente

                // Mostrar overlay
                _syncOverlayVisible.value = true
                _syncOverlayMessage.value = "Verificando usuario..."
                Timber.d("🔄 Iniciando sincronización automática CON OVERLAY para usuario: $usuarioId")

                // Pequeño delay para que se vea la animación
                kotlinx.coroutines.delay(500)

                // 🔍 Si es la primera sincronización y hay cédula, intentar recuperar cuenta
                val usuario = _usuario.value
                if (usuario?.cedula != null && 
                    prefs.getBoolean("primer_sync_intento", true) &&
                    NetworkMonitor.isConnected.value) {
                    
                    _syncOverlayMessage.value = "Cargando perfil..."
                    Timber.d("🔍 Primer intento de sync con internet - Buscando usuario por cédula...")
                    val searchResult = syncRepository.searchAndSyncByCedula(usuario.cedula!!)
                    
                    if (searchResult.isSuccess && searchResult.getOrNull() != null) {
                        Timber.d("✅ Usuario recuperado por cédula")
                        prefs.edit().putBoolean("primer_sync_intento", false).apply()
                        
                        _syncOverlayMessage.value = "Sincronizando datos personales..."
                        kotlinx.coroutines.delay(1000)
                        
                        _syncOverlayMessage.value = "Cargando ejercicios..."
                        kotlinx.coroutines.delay(800)
                        
                        _syncOverlayMessage.value = "Finalizando..."
                        kotlinx.coroutines.delay(500)
                        
                        _isSynced.value = true
                        _syncOverlayVisible.value = false
                        
                        // ✅ Sincronización exitosa por cédula - marcar bandera
                        prefs.edit().putBoolean("auto_sync_overlay_executed_$usuarioId", true).apply()
                        Timber.d("✅ Sincronización completada con overlay - Bandera grabada")
                        return@launch
                    } else {
                        Timber.d("ℹ️ Usuario no encontrado por cédula, continuando con fullSync normal")
                        prefs.edit().putBoolean("primer_sync_intento", false).apply()
                    }
                }

                // Sync normal
                _syncOverlayMessage.value = "Sincronizando datos personales..."
                val result = syncRepository.fullSync(usuarioId)

                result.onSuccess {
                    Timber.d("✅ Sincronización automática completada con overlay")
                    
                    _syncOverlayMessage.value = "Cargando ejercicios..."
                    kotlinx.coroutines.delay(800)
                    
                    _syncOverlayMessage.value = "Finalizando..."
                    kotlinx.coroutines.delay(500)
                    
                    _isSynced.value = true
                    _lastSyncTime.value = prefs.getString("last_sync", null)
                    
                    // ✅ Sincronización exitosa - marcar bandera SOLO AQUÍ
                    prefs.edit().putBoolean("auto_sync_overlay_executed_$usuarioId", true).apply()
                    Timber.d("✅ Bandera de auto-sync guardada en SharedPreferences")
                }

                result.onFailure { error ->
                    Timber.e(error, "❌ Error en sincronización automática con overlay")
                    _isSynced.value = false
                    
                    // ❌ NO marcar bandera si falla - permitir reintento cuando se reconecte
                    Timber.d("⚠️ NO se marcó bandera - permitirá reintento cuando haya internet")
                }
            } catch (e: Exception) {
                Timber.e(e, "❌ Excepción en sincronización automática con overlay")
                _isSynced.value = false
                // ❌ NO marcar bandera si falla
            } finally {
                // Ocultar overlay al finalizar
                _syncOverlayVisible.value = false
            }
        }
    }

    /**
     * Detectar si el error es causado por problemas de red
     */
    private fun isNetworkError(exception: Throwable?): Boolean {
        if (exception == null) return false
        val message = exception.message?.lowercase() ?: ""
        return message.contains("enetunreach") ||
                message.contains("unreachable") ||
                message.contains("connection refused") ||
                message.contains("failed to connect") ||
                message.contains("network unreachable") ||
                message.contains("no network") ||
                exception is java.net.ConnectException ||
                exception is java.net.UnknownHostException ||
                exception is java.net.SocketException ||
                exception.cause?.let { isNetworkError(it) } ?: false
    }
}