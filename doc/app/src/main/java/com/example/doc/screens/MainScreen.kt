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
    val usuario by usuarioViewModel.usuario.collectAsState()
    val registros by registroSaludViewModel.registros.collectAsState()
    val consejos by consejoViewModel.consejos.collectAsState()

    // Estados de sincronización
    val isSyncing by usuarioViewModel.isSyncing.collectAsState()
    val isSynced by usuarioViewModel.isSynced.collectAsState()
    val lastSyncTime by usuarioViewModel.lastSyncTime.collectAsState()
    val syncState by usuarioViewModel.syncState.collectAsState()

    val showNotifDialog = remember { mutableStateOf(false) }
    val context = LocalContext.current
    val sharedPrefs = context.getSharedPreferences("DocPrefs", Context.MODE_PRIVATE)
    var notifDialogShown by remember {
        mutableStateOf(sharedPrefs.getBoolean("notifDialogShown_${usuario?.id}", false))
    }
    val showResultPopup by popupViewModel.showResultPopup
    val resultPopupMessage by popupViewModel.resultPopupMessage
    val resultPopupColor = popupViewModel.resultPopupColor
    val showEvaluationPopup by popupViewModel.showEvaluationPopup
    val evaluationMessage by popupViewModel.evaluationMessage
    val evaluationColor = popupViewModel.evaluationColor
    val evaluationDetails by popupViewModel.evaluationDetails

    // Estado de carga
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
    LaunchedEffect(usuario) {
        if (usuario != null) {
            try {
                android.util.Log.d("MainScreen", "Usuario disponible: ${usuario!!.nombre} (ID: ${usuario!!.id})")

                // Ejecutar onUiReady
                onUiReady()

                // Cargar registros y consejos
                android.util.Log.d("MainScreen", "Cargando registros y consejos...")
                registroSaludViewModel.cargarRegistrosDesdeBD(usuario!!.id)
                consejoViewModel.cargarConsejos(usuario!!.enfermedad)

                // Verificar notificaciones
                if (!notifDialogShown && usuario!!.notifType == "daily_fixed" && usuario!!.notifDetails == "{\"hour\":20}") {
                    android.util.Log.d("MainScreen", "Mostrando diálogo de notificaciones")
                    showNotifDialog.value = true
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

    // Obtener estado de navegación
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
                    val evaluacionDetallada = SaludEvaluator.evaluarRegistroDetallado(usuario!!, registros.first())

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
            android.util.Log.e("MainScreen", "Error en LaunchedEffect resultados", e)
        }
    }

    var consejoTexto by remember(consejos) {
        mutableStateOf(consejos.randomOrNull()?.consejo ?: "Cargando...")
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
                // 👇 Si la lista puede seguir haciendo scroll, consumir el gesto
                return if (
                    (available.y < 0 && registrosScroll.canScrollForward) ||
                    (available.y > 0 && registrosScroll.canScrollBackward)
                ) {
                    Offset.Zero
                } else {
                    // 👇 Dejar que el padre maneje el scroll cuando está en el tope
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

                // Header mejorado - más natural
                Card(
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.95f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 16.dp)
                    ) {
                        // Primera fila: Saludo y perfil
                        Row(
                            modifier = Modifier
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Hola, ${user.nombre}",
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colorFondo
                                )
                                Text(
                                    text = "¿Cómo te sientes hoy?",
                                    fontSize = 16.sp,
                                    color = Color.Gray,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                            IconButton(
                                onClick = { navController.navigate("perfil") },
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(colorFondo.copy(alpha = 0.1f))
                            ) {
                                Icon(
                                    Icons.Filled.Person,
                                    contentDescription = "Perfil",
                                    tint = colorFondo,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }

                        // Segunda fila: Indicador de sincronización
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp),
                            horizontalArrangement = Arrangement.End
                        ) {
                            SyncIconButton(
                                syncState = syncState,
                                onSyncClick = { usuarioViewModel.sincronizarManualmente() }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = colorFondo),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .clickable {
                            // Cambio de consejo al tocar el card
                            consejoTexto = consejos.randomOrNull()?.consejo ?: "Cargando..."
                        }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        colorFondo.copy(alpha = 0.95f),
                                        colorFondo
                                    )
                                )
                            )
                            .padding(20.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            // Imagen representativa con diseño circular, borde y sombra
                            Image(
                                painter = painterResource(id = imagenId),
                                contentDescription = "Imagen representativa de ${user.enfermedad}",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(140.dp)
                                    .clip(RoundedCornerShape(100.dp))
                                    .border(5.dp, Color.White, RoundedCornerShape(100.dp))
                                    .shadow(
                                        elevation = 12.dp,
                                        shape = RoundedCornerShape(100.dp),
                                        ambientColor = Color.Black.copy(alpha = 0.3f),
                                        spotColor = Color.Black.copy(alpha = 0.3f)
                                    )
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            // Texto del consejo con animación y mejor legibilidad
                            AnimatedContent(
                                targetState = consejoTexto,
                                label = "ConsejoAnim"
                            ) { consejo ->
                                Text(
                                    text = consejo,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Medium,
                                    lineHeight = 22.sp,
                                    color = Color.White,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 18.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Divider(
                                color = Color.White.copy(alpha = 0.4f),
                                thickness = 1.dp,
                                modifier = Modifier
                                    .fillMaxWidth(0.6f)
                                    .padding(top = 4.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
                Card(
                    shape = RoundedCornerShape(32.dp),
                    elevation = CardDefaults.cardElevation(8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.93f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { navController.navigate("registro_salud") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))
                        ) {
                            Text(
                                "Registrar datos de salud",
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            )
                        }
                        Button(
                            onClick = { navController.navigate("ejercicios") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))
                        ) {
                            Text(
                                "Ejercicios recomendados",
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
                Card(
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(6.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.90f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp)
                    ) {
                        Text(
                            "Registros recientes",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        LazyColumn(
                            state = registrosScroll,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 120.dp, max = 260.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(registros.take(5)) { reg ->
                                Card(
                                    shape = RoundedCornerShape(18.dp),
                                    elevation = CardDefaults.cardElevation(6.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = Color.White.copy(alpha = 0.88f)
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp)
                                        .clickable {
                                            usuario?.let { user ->
                                                val eval = SaludEvaluator.evaluarRegistroDetallado(user, reg)
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
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Text(text = reg.fecha.substring(0, 10), fontWeight = FontWeight.Bold, color = Color(0xFF2196F3), fontSize = 15.sp)
                                        reg.presionArterial?.let { Text("Presión: $it", fontSize = 14.sp) }
                                        reg.glucosa?.let { Text("Glucosa: $it mg/dL", fontSize = 14.sp) }
                                        reg.imc?.let { Text("IMC: ${String.format(java.util.Locale.US, "%.2f", it)}", fontSize = 14.sp) }
                                        reg.colesterol?.let { Text("Colesterol: $it mg/dL", fontSize = 14.sp) }
                                        Text("Calorías: ${reg.calorias}", fontSize = 14.sp)
                                        Text("Ejercicio: ${reg.ejercicio} min", fontSize = 14.sp)
                                        Text("Ánimo: ${reg.estadoAnimo}", fontSize = 14.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }  // Fin de usuario?.let
            // --- POPUPS overlays (SIEMPRE al final del Box) ---
            if (showResultPopup) {
                android.util.Log.d("MainScreen", "Mostrando ResultPopup: $resultPopupMessage")

                // animación de entrada (se usa aunque esté dentro del Dialog)
                val scale by animateFloatAsState(
                    targetValue = if (showResultPopup) 1f else 0.8f,
                    animationSpec = tween(durationMillis = 300)
                )
                val alpha by animateFloatAsState(
                    targetValue = if (showResultPopup) 1f else 0f,
                    animationSpec = tween(durationMillis = 300)
                )

                Dialog(
                    onDismissRequest = {
                        // al tocar fuera: cerrar resultado y abrir evaluación (si procede)
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
                ) {
                    // Nota: no ocupes fillMaxSize aquí para que el "fuera" del dialog sea realmente fuera.
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = resultPopupColor.value,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(10.dp),
                        modifier = Modifier
                            .padding(horizontal = 32.dp)
                            .fillMaxWidth(0.85f)
                            .scale(scale)
                            .alpha(alpha) // si quieres que también vaya desvaneciendo
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(20.dp)
                                .fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = if (resultPopupMessage.contains("✔")) {
                                    Icons.Filled.CheckCircle
                                } else {
                                    Icons.Filled.Error
                                },
                                contentDescription = "Estado del registro",
                                modifier = Modifier.size(48.dp),
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = resultPopupMessage,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
            }

            if (showEvaluationPopup) {
                android.util.Log.d("MainScreen", "Mostrando EvaluationPopup: $evaluationMessage")

                val scale by animateFloatAsState(
                    targetValue = if (showEvaluationPopup) 1f else 0.8f,
                    animationSpec = tween(durationMillis = 300)
                )
                val alpha by animateFloatAsState(
                    targetValue = if (showEvaluationPopup) 1f else 0f,
                    animationSpec = tween(durationMillis = 300)
                )

                Dialog(
                    onDismissRequest = {
                        // tocar fuera cierra la evaluación
                        popupViewModel.showEvaluationPopup.value = false
                    },
                    properties = androidx.compose.ui.window.DialogProperties(
                        usePlatformDefaultWidth = false
                    )
                ) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = evaluationColor.value,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(20.dp),
                        elevation = CardDefaults.cardElevation(12.dp),
                        modifier = Modifier
                            .padding(horizontal = 24.dp)
                            .fillMaxWidth(0.95f)
                            .scale(scale)
                            .alpha(alpha)
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(24.dp)
                                .fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = when (evaluationColor.value) {
                                    Color(0xFF4CAF50) -> Icons.Filled.CheckCircle // ÓPTIMO
                                    Color(0xFFA3DC39) -> Icons.Filled.Info // LEVE
                                    Color(0xFFFFA500) -> Icons.Filled.Warning // ADVERTENCIA
                                    Color(0xFFD32F2F) -> Icons.Filled.Error // EMERGENCIA
                                    else -> Icons.Filled.Info
                                },
                                contentDescription = "Estado de evaluación",
                                modifier = Modifier.size(56.dp),
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = evaluationMessage,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Divider(color = Color.White.copy(alpha = 0.6f), thickness = 1.dp)
                            Spacer(modifier = Modifier.height(12.dp))
                            evaluationDetails.forEach { detail ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Info,
                                        contentDescription = "Detalle",
                                        modifier = Modifier
                                            .size(20.dp)
                                            .padding(end = 8.dp),
                                        tint = Color.White.copy(alpha = 0.8f)
                                    )
                                    Text(
                                        text = detail,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium,
                                        color = Color.White,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(20.dp))
                            Button(
                                onClick = { popupViewModel.showEvaluationPopup.value = false },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White,
                                    contentColor = Color(0xFF2196F3)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    "Aceptar",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }
                }
            }
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
        } // Fin del Box global
    } // Fin de MainScreen
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