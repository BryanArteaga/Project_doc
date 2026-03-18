package com.example.doc.screens


import android.content.Context
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.window.Dialog
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.doc.R
import com.example.doc.domain.SaludEvaluator
import com.example.doc.model.EvaluacionSalud
import com.example.doc.navigation.Routes
import com.example.doc.ui.components.SyncIconButton
import com.example.doc.ui.components.SyncOverlay  // ✅ Importar overlay
import com.example.doc.viewmodel.ConsejoViewModel
import com.example.doc.viewmodel.PopupViewModel
import com.example.doc.viewmodel.RegistroSaludViewModel
import com.example.doc.viewmodel.UsuarioViewModel
import kotlinx.coroutines.delay
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    navController: NavController,
    usuarioViewModel: UsuarioViewModel,
    registroSaludViewModel: RegistroSaludViewModel,
    consejoViewModel: ConsejoViewModel,
    popupViewModel: PopupViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    onUiReady: () -> Unit
) {
    val scrollState = rememberScrollState()
    val registrosScroll = rememberLazyListState()
    
    // FIX #7: Reducir observables solo a lo esencial
    val usuario by usuarioViewModel.usuario.collectAsState()
    val registros by registroSaludViewModel.registros.collectAsState()
    val consejos by consejoViewModel.consejos.collectAsState()
    val syncState by usuarioViewModel.syncState.collectAsState()

    // Estados específicos del overlay (separado para no causa recomposiciones en UI principal)
    val syncOverlayVisible by usuarioViewModel.syncOverlayVisible.collectAsState()
    val syncOverlayMessage by usuarioViewModel.syncOverlayMessage.collectAsState()

    // Estados de popups (solo PopupViewModel)
    val showResultPopup by popupViewModel.showResultPopup
    val resultPopupMessage by popupViewModel.resultPopupMessage
    val resultPopupColor = popupViewModel.resultPopupColor
    val showEvaluationPopup by popupViewModel.showEvaluationPopup
    val evaluationMessage by popupViewModel.evaluationMessage
    val evaluationColor = popupViewModel.evaluationColor
    val evaluationDetails by popupViewModel.evaluationDetails

    val showNotifDialog = remember { mutableStateOf(false) }
    val context = LocalContext.current
    val sharedPrefs = context.getSharedPreferences("DocPrefs", Context.MODE_PRIVATE)
    var notifDialogShown by remember {
        mutableStateOf(sharedPrefs.getBoolean("notifDialogShown_${usuario?.id}", false))
    }

    // FIX #7: Estado local para consejo (no reactivo a cada cambio de consejos)
    var consejoTexto by remember(consejos) {
        mutableStateOf(consejos.randomOrNull()?.consejo ?: "Cargando...")
    }

    var isLoading by remember { mutableStateOf(true) }

    // Log inicial
    LaunchedEffect(Unit) {
        android.util.Log.d("MainScreen", "MainScreen iniciando...")
    }

    // Cargar datos del usuario una sola vez
    LaunchedEffect(Unit) {
        try {
            android.util.Log.d("MainScreen", "Iniciando carga de usuario...")
            // El ViewModel ya tiene un init que carga el usuario automáticamente
            // pero podemos forzar una recarga si es necesario
            delay(100) // Dar tiempo para que el init se complete
            android.util.Log.d("MainScreen", "Usuario inicial: ${usuario?.nombre}")
        } catch (e: Exception) {
            android.util.Log.e("MainScreen", "Error cargando usuario", e)
        }
    }

    // Una vez que el usuario esté disponible, cargar sus datos
    // 🔑 Observar solo el ID del usuario para evitar múltiples disparos durante la sincronización
    LaunchedEffect(usuario?.id) {
        if (usuario != null) {
            try {
                android.util.Log.d("MainScreen", "Usuario disponible: ${usuario!!.nombre} (ID: ${usuario!!.id})")

                // Ejecutar onUiReady
                onUiReady()

                // 🔄 Disparar sincronización automática CON OVERLAY para usuarios existentes
                // (Los que ya estaban registrados y vuelven a entrar a la app)
                android.util.Log.d("MainScreen", "Disparando sincronización automática con overlay...")
                usuarioViewModel.performAutoSyncWithOverlay(usuario!!.id)

                // Cargar registros y consejos
                android.util.Log.d("MainScreen", "Cargando registros y consejos...")
                registroSaludViewModel.cargarRegistrosDesdeBD(usuario!!.id)
                consejoViewModel.cargarConsejos(usuario!!.enfermedad)

                // Esperar un poco para que la sincronización se complete
                delay(500)

                // ✅ NUEVO: Mostrar diálogo de notificaciones SOLO si es usuario nuevo
                // (después del PULL, si sigue teniendo configuración por defecto = usuario nuevo)
                val usuarioActualizado = usuarioViewModel.usuario.value
                if (!notifDialogShown && 
                    usuarioActualizado?.notifType == "daily_fixed" && 
                    usuarioActualizado?.notifDetails == "{\"hour\":20}") {
                    android.util.Log.d("MainScreen", "Usuario nuevo detectado - mostrando diálogo de notificaciones")
                    showNotifDialog.value = true
                    notifDialogShown = true
                    sharedPrefs.edit().putBoolean("notifDialogShown_${usuario!!.id}", true).apply()
                } else {
                    android.util.Log.d("MainScreen", "Usuario con configuración personalizada - sin diálogo de notificaciones")
                    notifDialogShown = true
                    sharedPrefs.edit().putBoolean("notifDialogShown_${usuario!!.id}", true).apply()
                }

                isLoading = false
                android.util.Log.d("MainScreen", "MainScreen completamente cargado")
            } catch (e: Exception) {
                android.util.Log.e("MainScreen", "Error en LaunchedEffect usuario", e)
                isLoading = false
            }
        } else {
            android.util.Log.w("MainScreen", "Usuario es null, esperando...")
        }
    }

    // Obtener estado de navegación - guardar en previousBackStackEntry desde RegistroSaludScreen
    // Entonces se busca en currentBackStackEntry aquí en MainScreen
    val navBackStackEntry = navController.currentBackStackEntry
    val justSavedRegistro = navBackStackEntry?.savedStateHandle?.get<Boolean>("registro_guardado") == true
    val registroError = navBackStackEntry?.savedStateHandle?.get<String>("registro_error")

    // Manejar resultado de guardado de registro
    LaunchedEffect(justSavedRegistro, registroError) {
        try {
            if (justSavedRegistro) {
                android.util.Log.d("MainScreen", "Registro guardado, mostrando popup")

                // Mostrar popup de éxito
                popupViewModel.resultPopupMessage.value = "✔ Registro guardado correctamente"
                popupViewModel.resultPopupColor.value = Color(0xFF43A047)
                popupViewModel.showResultPopup.value = true

                // Limpiar estado
                navBackStackEntry?.savedStateHandle?.remove<Boolean>("registro_guardado")

                // Recargar registros
                usuario?.let { registroSaludViewModel.cargarRegistrosDesdeBD(it.id) }

                // Ocultar después de 500ms
                delay(500)
                popupViewModel.showResultPopup.value = false

                // Esperar a que carguen los registros
                var attempts = 0
                while (registros.isEmpty() && attempts < 20) {
                    delay(100)
                    attempts++
                }

                // Mostrar evaluación si hay datos
                if (usuario != null && registros.isNotEmpty()) {
                    android.util.Log.d("MainScreen", "Evaluando registro...")
                    val evaluacionDetallada =
                        SaludEvaluator.evaluarRegistroDetallado(usuario!!, registros.first())

                    popupViewModel.evaluationColor.value = when (evaluacionDetallada.estado) {
                        EvaluacionSalud.Estado.NORMAL -> Color(0xFF4CAF50)
                        EvaluacionSalud.Estado.LEVE -> Color(0xFFA3DC39)
                        EvaluacionSalud.Estado.ADVERTENCIA -> Color(0xFFFFA500)
                        EvaluacionSalud.Estado.EMERGENCIA -> Color(0xFFD32F2F)
                    }
                    popupViewModel.evaluationMessage.value = evaluacionDetallada.mensaje
                    popupViewModel.evaluationDetails.value = evaluacionDetallada.detalles
                    popupViewModel.showEvaluationPopup.value = true
                }
            } else if (registroError != null) {
                android.util.Log.e("MainScreen", "Error en registro: $registroError")

                // Mostrar popup de error
                popupViewModel.resultPopupMessage.value = "Error: $registroError"
                popupViewModel.resultPopupColor.value = Color(0xFFD32F2F)
                popupViewModel.showResultPopup.value = true

                navBackStackEntry?.savedStateHandle?.remove<String>("registro_error")

                // Ocultar después de 500ms
                delay(500)
                popupViewModel.showResultPopup.value = false
            }
        } catch (e: Exception) {
            android.util.Log.e("MainScreen", "Error en LaunchedEffect de registro guardado", e)
        }
    }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                // FIX #7: Si la lista puede seguir haciendo scroll, consumir el gesto
                return if (
                    (available.y < 0 && registrosScroll.canScrollForward) ||
                    (available.y > 0 && registrosScroll.canScrollBackward)
                ) {
                    Offset.Zero
                } else {
                    available
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // --- Imagen de fondo ---
        Image(
            painter = painterResource(id = R.drawable.background_2),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // --- Indicador de carga ---
        if (isLoading || usuario == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Cargando...",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White
                    )
                }
            }
            return@Box // Salir temprano si está cargando
        }

        // --- UI principal (solo si usuario != null) ---
        usuario?.let { user ->
            val colorFondo = Color(android.graphics.Color.parseColor(user.colorEnfermedad))
            val imagenId = obtenerImagen(user.enfermedad)
            val adaptiveTopPadding = com.example.doc.utils.getAdaptiveTopPadding()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .nestedScroll(nestedScrollConnection)
                    .padding(24.dp),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally
            ){
                Spacer(modifier = Modifier.height(adaptiveTopPadding))

                // FIX #7: Usar componentes separados para evitar recomposiciones cascading
                MainScreenHeader(
                    usuario = user,
                    navController = navController,
                    syncState = syncState,
                    onSyncClick = { usuarioViewModel.sincronizarManualmente() }
                )
                Spacer(modifier = Modifier.height(20.dp))
                
                MainScreenConsejoCard(
                    usuario = user,
                    consejoTexto = consejoTexto,
                    onConsejoTap = { nuevoConsejo -> consejoTexto = nuevoConsejo },
                    consejos = consejos
                )
                Spacer(modifier = Modifier.height(20.dp))
                
                MainScreenAccionesCard(navController = navController)
                Spacer(modifier = Modifier.height(20.dp))
                
                MainScreenRegistrosCard(
                    usuario = user,
                    registros = registros,
                    popupViewModel = popupViewModel,
                    registrosScroll = registrosScroll
                )
            }  // Fin de usuario?.let
            // --- POPUPS overlays (SIEMPRE al final del Box) ---
            // FIX #7: Usar componentes separados para popups
            MainScreenResultPopup(
                isVisible = showResultPopup,
                message = resultPopupMessage,
                color = resultPopupColor.value,
                onDismiss = {
                    popupViewModel.showResultPopup.value = false
                    usuario?.let { user ->
                        if (registros.isNotEmpty() && !popupViewModel.showEvaluationPopup.value) {
                            val eval = SaludEvaluator.evaluarRegistroDetallado(user, registros.first())
                            popupViewModel.evaluationColor.value = when (eval.estado) {
                                EvaluacionSalud.Estado.NORMAL -> Color(0xFF4CAF50)
                                EvaluacionSalud.Estado.LEVE -> Color(0xFFA3DC39)
                                EvaluacionSalud.Estado.ADVERTENCIA -> Color(0xFFFFA500)
                                EvaluacionSalud.Estado.EMERGENCIA -> Color(0xFFD32F2F)
                            }
                            popupViewModel.evaluationMessage.value = eval.mensaje
                            popupViewModel.evaluationDetails.value = eval.detalles
                            popupViewModel.showEvaluationPopup.value = true
                        }
                    }
                }
            )

            MainScreenEvaluationPopup(
                isVisible = showEvaluationPopup,
                message = evaluationMessage,
                color = evaluationColor.value,
                details = evaluationDetails,
                estado = popupViewModel.evaluationEstado.value,
                context = context,
                usuario = usuario,
                onDismiss = { popupViewModel.showEvaluationPopup.value = false }
            )

            if (showNotifDialog.value) {
                android.util.Log.d("MainScreen", "Mostrando NotifDialog")
                AlertDialog(
                    onDismissRequest = {
                        showNotifDialog.value = false
                        notifDialogShown = true
                        sharedPrefs.edit().putBoolean("notifDialogShown_${user.id}", true).apply()
                    },
                    title = {
                        Text(
                            "Configura tus notificaciones",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    text = { Text("Para recibir recordatorios personalizados, configura las notificaciones en Perfil.") },
                    confirmButton = {
                        Button(
                            onClick = {
                                showNotifDialog.value = false
                                notifDialogShown = true
                                sharedPrefs.edit().putBoolean("notifDialogShown_${user.id}", true).apply()
                                navController.navigate(Routes.Perfil)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF2196F3)
                            )
                        ) {
                            Text("Ir a Perfil", color = Color.White)
                        }
                    },
                    dismissButton = {
                        Button(
                            onClick = {
                                showNotifDialog.value = false
                                notifDialogShown = true
                                sharedPrefs.edit().putBoolean("notifDialogShown_${user.id}", true).apply()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFE0E0E0)
                            )
                        ) {
                            Text("Después", color = Color.DarkGray)
                        }
                    }
                )
            }
        } // Fin del usuario?.let

        // ✅ OVERLAY de sincronización profesional
        SyncOverlay(
            visible = syncOverlayVisible,
            syncStep = syncOverlayMessage
        )
    } // Fin del Box global
}
fun obtenerImagen(enfermedad: String): Int {
    return try {
        when (enfermedad) {
            "Hipertensión" -> R.drawable.hipertension
            "Diabetes" -> R.drawable.diabetes
            "Obesidad" -> R.drawable.obesidad
            "Colesterol alto" -> R.drawable.colesterol
            else -> R.drawable.ic_launcher_foreground
        }
    } catch (e: Exception) {
        R.drawable.ic_launcher_foreground
    }
}