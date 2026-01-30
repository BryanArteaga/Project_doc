package com.example.doc.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.doc.viewmodel.EjercicioViewModel
import com.example.doc.viewmodel.UsuarioViewModel
import com.example.doc.R
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

fun Context.findActivity(): Activity? {
    var currentContext = this
    while (currentContext is ContextWrapper) {
        if (currentContext is Activity) {
            return currentContext
        }
        currentContext = currentContext.baseContext
    }
    return null
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseTimerScreen(
    navController: NavController,
    usuarioViewModel: UsuarioViewModel,
    ejercicioViewModel: EjercicioViewModel,
    ejercicioId: Int
) {
    val usuario by usuarioViewModel.usuario.collectAsState()
    val ejercicios by ejercicioViewModel.ejercicios.collectAsState()
    val ejercicio = ejercicios.find { it.id == ejercicioId } ?: return

    // Estados del temporizador
    var timerRunning by remember { mutableStateOf(false) }
    var timerPaused by remember { mutableStateOf(false) }
    var timeLeft by remember { mutableStateOf(ejercicio.duracion) } // Duración de cada serie en segundos
    var showExitDialog by remember { mutableStateOf(false) }
    var showCompletionDialog by remember { mutableStateOf(false) }

    // Estados para series y descansos
    var currentSerie by remember { mutableStateOf(1) }
    var isResting by remember { mutableStateOf(false) }
    var restTimeLeft by remember { mutableStateOf(0) }

    val context = LocalContext.current
    val sdf = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val hoy = sdf.format(Date())

    // Mantener la pantalla encendida cuando el cronómetro está activo
    val activity = context.findActivity()
    DisposableEffect(timerRunning, activity) {
        if (activity != null) {
            if (timerRunning) {
                activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            } else {
                activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // Handle lifecycle events for pause/resume
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    if (timerRunning && !timerPaused) {
                        timerPaused = true
                        timerRunning = false
                    }
                    activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
                Lifecycle.Event.ON_RESUME -> {
                    if (timerPaused) {
                        timerRunning = false
                    }
                    if (timerRunning) {
                        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    }
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // Timer logic con series y descansos
    LaunchedEffect(timerRunning, timerPaused, timeLeft, isResting, restTimeLeft, currentSerie) {
        if (timerRunning && !timerPaused) {
            delay(1000L)

            if (isResting) {
                // Modo descanso entre series
                if (restTimeLeft > 0) {
                    restTimeLeft--
                } else {
                    // Termina el descanso, siguiente serie
                    isResting = false
                    timeLeft = ejercicio.duracion
                    currentSerie++
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                        vibrator?.vibrate(VibrationEffect.createOneShot(300, VibrationEffect.DEFAULT_AMPLITUDE))
                    }
                }
            } else {
                // Modo ejercicio
                if (timeLeft > 0) {
                    timeLeft--

                    // Serie completada
                    if (timeLeft == 0) {
                        if (currentSerie < ejercicio.series) {
                            // Hay más series, iniciar descanso
                            isResting = true
                            restTimeLeft = ejercicio.descansoEntreSeries
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                                vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 200, 100, 200), -1))
                            }
                        } else {
                            // Todas las series completadas
                            timerRunning = false
                            timerPaused = false
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                                vibrator?.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
                            }
                            usuario?.let {
                                ejercicioViewModel.marcarRealizado(it.id, ejercicio.id, hoy)
                            }
                            showCompletionDialog = true
                        }
                    }
                }
            }
        }
    }

    // Handle back button
    BackHandler(enabled = timerRunning && !timerPaused) {
        timerPaused = true
        timerRunning = false
        showExitDialog = true
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isSmallScreen = maxWidth < 360.dp
        val imageSize = if (isSmallScreen) 150.dp else 200.dp
        val cardWidth = if (isSmallScreen) maxWidth - 32.dp else 400.dp

        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(id = R.drawable.background_2),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Indicador de Series
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isResting) Color(0xFFFFE0B2).copy(alpha = 0.95f) else Color(0xFFE3F2FD).copy(alpha = 0.95f)
                        ),
                        modifier = Modifier
                            .width(cardWidth)
                            .padding(vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    "Serie Actual",
                                    fontSize = 14.sp,
                                    color = Color(0xFF757575),
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    "$currentSerie / ${ejercicio.series}",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isResting) Color(0xFFF57C00) else Color(0xFF1976D2)
                                )
                            }

                            // Indicadores visuales de series
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                repeat(ejercicio.series) { index ->
                                    Box(
                                        modifier = Modifier
                                            .size(if (index + 1 == currentSerie) 14.dp else 10.dp)
                                            .background(
                                                color = when {
                                                    index + 1 < currentSerie -> Color(0xFF4CAF50)
                                                    index + 1 == currentSerie -> if (isResting) Color(0xFFFFA726) else Color(0xFF2196F3)
                                                    else -> Color(0xFFBDBDBD)
                                                },
                                                shape = CircleShape
                                            )
                                    )
                                }
                            }
                        }
                    }
                }

                // Badge de DESCANSO
                item {
                    if (isResting) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xFFFFA726),
                            modifier = Modifier.padding(vertical = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("⏸", fontSize = 18.sp, color = Color.White)
                                Text(
                                    "DESCANSO",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    letterSpacing = 1.sp
                                )
                            }
                        }
                    }
                }

                // Card del ejercicio con temporizador
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        elevation = CardDefaults.cardElevation(8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.94f)),
                        modifier = Modifier
                            .width(cardWidth)
                            .wrapContentHeight()
                            .padding(vertical = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                ejercicio.nombre,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isResting) Color(0xFFF57C00) else Color(0xFF2196F3),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            AsyncImage(
                                model = ejercicio.imagenResId,
                                contentDescription = "Imagen ejercicio",
                                modifier = Modifier
                                    .size(imageSize)
                                    .clip(RoundedCornerShape(16.dp))
                                    .alpha(if (isResting) 0.5f else 1f),
                                contentScale = ContentScale.Fit
                            )
                            Spacer(modifier = Modifier.height(20.dp))

                            // Temporizador
                            if (isResting) {
                                Text(
                                    "Tiempo de descanso",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFFF57C00)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "${restTimeLeft}s",
                                    fontSize = 48.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFFA726)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "Serie ${currentSerie + 1} comienza en...",
                                    fontSize = 13.sp,
                                    color = Color(0xFF757575)
                                )
                            } else {
                                Text(
                                    "Tiempo de serie",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF1976D2)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    if (timeLeft >= 60) {
                                        "${timeLeft / 60}:${(timeLeft % 60).toString().padStart(2, '0')}"
                                    } else {
                                        "${timeLeft}s"
                                    },
                                    fontSize = 48.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2196F3)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                if (timeLeft <= 5 && timeLeft > 0) {
                                    Text(
                                        "¡Último esfuerzo!",
                                        fontSize = 13.sp,
                                        color = Color(0xFFFF5722),
                                        fontWeight = FontWeight.Bold
                                    )
                                } else {
                                    Text(
                                        "Serie $currentSerie de ${ejercicio.series}",
                                        fontSize = 13.sp,
                                        color = Color(0xFF757575)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Barra de progreso
                            LinearProgressIndicator(
                                progress = if (isResting) {
                                    if (ejercicio.descansoEntreSeries > 0)
                                        (ejercicio.descansoEntreSeries - restTimeLeft).toFloat() / ejercicio.descansoEntreSeries
                                    else 0f
                                } else {
                                    if (ejercicio.duracion > 0)
                                        (ejercicio.duracion - timeLeft).toFloat() / ejercicio.duracion
                                    else 1f
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(10.dp)
                                    .clip(RoundedCornerShape(5.dp)),
                                color = if (isResting) Color(0xFFFFA726) else Color(0xFF2196F3),
                                trackColor = Color(0xFFE0E0E0)
                            )
                        }
                    }
                }

                // Botones de control de descanso (solo visibles durante el descanso)
                item {
                    if (isResting) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.width(cardWidth),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Botón -5s
                                OutlinedButton(
                                    onClick = {
                                        if (restTimeLeft > 5) {
                                            restTimeLeft -= 5
                                        } else {
                                            restTimeLeft = 1
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(2.dp, Color(0xFF2196F3)),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = Color(0xFF2196F3)
                                    )
                                ) {
                                    Text(
                                        "-5s",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                // Botón Omitir
                                Button(
                                    onClick = {
                                        isResting = false
                                        restTimeLeft = 0
                                        timeLeft = ejercicio.duracion
                                        currentSerie++
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                                            vibrator?.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFFFF9800)
                                    )
                                ) {
                                    Text(
                                        "Omitir",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }

                                // Botón +5s
                                OutlinedButton(
                                    onClick = {
                                        restTimeLeft += 5
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(2.dp, Color(0xFF4CAF50)),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = Color(0xFF4CAF50)
                                    )
                                ) {
                                    Text(
                                        "+5s",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                // Botones de control principales
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            if (!timerRunning && !timerPaused) {
                                timerRunning = true
                                timerPaused = false
                                if (timeLeft == 0 && currentSerie > ejercicio.series) {
                                    timeLeft = ejercicio.duracion
                                    currentSerie = 1
                                    isResting = false
                                }
                            } else if (timerRunning && !timerPaused) {
                                timerRunning = false
                                timerPaused = true
                            } else {
                                timerRunning = true
                                timerPaused = false
                            }
                        },
                        modifier = Modifier
                            .width(cardWidth)
                            .height(60.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isResting && timerRunning) Color(0xFFFFA726) else Color(0xFF2196F3)
                        )
                    ) {
                        Text(
                            when {
                                !timerRunning && currentSerie == 1 && timeLeft == ejercicio.duracion -> "Iniciar"
                                timerRunning && !timerPaused -> "Pausar"
                                else -> "Continuar"
                            },
                            fontSize = 20.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            if (timerRunning && !timerPaused) {
                                timerPaused = true
                                timerRunning = false
                                showExitDialog = true
                            } else {
                                navController.popBackStack()
                            }
                        },
                        modifier = Modifier
                            .width(cardWidth)
                            .height(55.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE0E0E0))
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Volver",
                                tint = Color.DarkGray
                            )
                            Text(
                                "Volver",
                                fontSize = 18.sp,
                                color = Color.DarkGray,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }
            }

            // Diálogos
            if (showExitDialog) {
                AlertDialog(
                    onDismissRequest = {
                        showExitDialog = false
                        timerRunning = true
                        timerPaused = false
                    },
                    title = { Text("¿Salir del ejercicio?", fontWeight = FontWeight.Bold) },
                    text = { Text("El cronómetro está en curso. Si sales, se reiniciará.") },
                    confirmButton = {
                        Button(
                            onClick = {
                                showExitDialog = false
                                timerRunning = false
                                timerPaused = false
                                timeLeft = 0
                                navController.popBackStack()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                        ) {
                            Text("Salir", color = Color.White)
                        }
                    },
                    dismissButton = {
                        Button(
                            onClick = {
                                showExitDialog = false
                                timerRunning = true
                                timerPaused = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))
                        ) {
                            Text("Continuar ejercicio", color = Color.White)
                        }
                    }
                )
            }

            if (showCompletionDialog) {
                AlertDialog(
                    onDismissRequest = {
                        showCompletionDialog = false
                        navController.popBackStack()
                    },
                    title = { Text("¡Felicidades!", fontWeight = FontWeight.Bold) },
                    text = { Text("Has completado todas las ${ejercicio.series} series. ¡Excelente trabajo!") },
                    confirmButton = {
                        Button(
                            onClick = {
                                showCompletionDialog = false
                                navController.popBackStack()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                        ) {
                            Text("Aceptar", color = Color.White)
                        }
                    }
                )
            }
        }
    }
}