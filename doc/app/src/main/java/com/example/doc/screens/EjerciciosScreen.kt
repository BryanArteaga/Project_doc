package com.example.doc.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.geometry.CornerRadius
import androidx.navigation.NavController
import com.example.doc.viewmodel.EjercicioViewModel
import com.example.doc.viewmodel.UsuarioViewModel
import com.example.doc.R
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.style.TextOverflow
import timber.log.Timber
import coil.compose.AsyncImage
import com.example.doc.navigation.Routes
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EjerciciosScreen(
    navController: NavController,
    usuarioViewModel: UsuarioViewModel,
    ejercicioViewModel: EjercicioViewModel
) {
    // ✅ Función para convertir segundos a mm:ss
    fun secsToMinSec(segundos: Int): String {
        val minutos = segundos / 60
        val secs = segundos % 60
        return String.format("%d:%02d", minutos, secs)
    }
    val usuario by usuarioViewModel.usuario.collectAsState()
    val ejercicios by ejercicioViewModel.ejercicios.collectAsState()
    val realizadosHoy by ejercicioViewModel.realizadosHoy.collectAsState()
    val sdf = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val hoy = sdf.format(Date())
    val scope = rememberCoroutineScope()

    // ✅ Estado para actualizar ejercicios
    var isRefreshing by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    LaunchedEffect(usuario, hoy) {
        usuario?.let {
            Timber.d("📱 Carga INICIAL de ejercicios")
            ejercicioViewModel.cargarEjerciciosPorEnfermedad(it.enfermedad, hoy)
            ejercicioViewModel.cargarRealizadosHoy(it.id, hoy)
        }
    }

    val adaptiveTopPadding = com.example.doc.utils.getAdaptiveTopPadding()

    Box(modifier = Modifier.fillMaxSize()) {
        // Background con imagen drawable
        Image(
            painter = painterResource(id = R.drawable.background_2),
            contentDescription = "Fondo",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alpha = 0.50f
        )

        Column(modifier = Modifier.fillMaxSize()) {
            // 🔒 HEADER ESTÁTICO (NO SCROLLEA)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        top = adaptiveTopPadding + 12.dp,
                        start = 20.dp,
                        end = 20.dp,
                        bottom = 12.dp
                    )
            ) {
                // Botón volver
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF2196F3).copy(alpha = 0.1f)
                        ),
                        modifier = Modifier.clickable { navController.popBackStack() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.ArrowBack,
                                contentDescription = "Volver",
                                tint = Color(0xFF2196F3),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Inicio",
                                color = Color(0xFF2196F3),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    
                    // 🔄 Botón recargar ejercicios
                    var buttonRotation by remember { mutableStateOf(0f) }
                    
                    LaunchedEffect(isRefreshing) {
                        if (isRefreshing) {
                            while (isRefreshing) {
                                buttonRotation = (buttonRotation + 6f) % 360f
                                delay(16)
                            }
                        } else {
                            buttonRotation = 0f
                        }
                    }
                    
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        elevation = CardDefaults.cardElevation(0.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isRefreshing) Color(0xFFFF9800).copy(alpha = 0.15f) else Color(0xFF4CAF50).copy(alpha = 0.1f)
                        ),
                        modifier = Modifier.clickable(enabled = !isRefreshing) {
                            if (!isRefreshing) {
                                isRefreshing = true
                                usuario?.let { u ->
                                    scope.launch {
                                        try {
                                            Timber.d("🔄 Recargando ejercicios manualmente")
                                            ejercicioViewModel.cargarEjerciciosActualizados(u.enfermedad, hoy)
                                            delay(500) // Mostrar el loading un mínimo
                                        } finally {
                                            isRefreshing = false
                                        }
                                    }
                                }
                            }
                        }
                    ) {
                        Icon(
                            Icons.Filled.Refresh,
                            contentDescription = if (isRefreshing) "Actualizando..." else "Recargar ejercicios",
                            tint = if (isRefreshing) Color(0xFFFF9800) else Color(0xFF4CAF50),
                            modifier = Modifier
                                .size(40.dp)
                                .padding(8.dp)
                                .rotate(buttonRotation)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Título
                Text(
                    text = "Ejercicios Recomendados",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2196F3),
                    modifier = Modifier.padding(start = 4.dp)
                )
                Text(
                    text = "Para tu condición de salud",
                    fontSize = 14.sp,
                    color = Color(0xFF757575),
                    modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Barra de progreso
                val totalEjercicios = ejercicios.take(6).size
                val ejerciciosRealizados = realizadosHoy.size
                val progreso = if (totalEjercicios > 0) ejerciciosRealizados.toFloat() / totalEjercicios else 0f

                Card(
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(0.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF2196F3).copy(alpha = 0.1f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Progreso de hoy",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2196F3)
                            )
                            Text(
                                text = "$ejerciciosRealizados/$totalEjercicios",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF757575)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        // 🎨 Barra de progreso simple y compatible
                        LinearProgressIndicator(
                            progress = { progreso },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp),
                            color = Color(0xFF4CAF50),
                            trackColor = Color(0xFFE0E0E0),
                            strokeCap = StrokeCap.Round
                        )
                    }
                }
            }

            // 📜 LISTA DE EJERCICIOS (SCROLLEABLE)
            Box(modifier = Modifier.weight(1f)) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (ejercicios.isEmpty()) {
                        item {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color.White.copy(alpha = 0.9f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 20.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        "No hay ejercicios disponibles",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF757575)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        "Intenta volver más tarde",
                                        fontSize = 14.sp,
                                        color = Color(0xFF9E9E9E)
                                    )
                                }
                            }
                        }
                    } else {
                        items(ejercicios.take(6)) { ejercicio ->
                            val realizado = realizadosHoy.contains(ejercicio.id)
                            val scale by animateFloatAsState(if (realizado) 1.02f else 1.0f)

                            Card(
                                shape = RoundedCornerShape(20.dp),
                                elevation = CardDefaults.cardElevation(
                                    defaultElevation = if (realizado) 12.dp else 6.dp
                                ),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (realizado)
                                        Color(0xFFE8F5E9)
                                    else
                                        Color.White.copy(alpha = 0.95f)
                                ),
                                border = if (realizado)
                                    BorderStroke(2.dp, Color(0xFF4CAF50))
                                else
                                    null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .scale(scale)
                                    .clickable(enabled = !realizado) { 
                                        if (!realizado) {
                                            navController.navigate(Routes.exerciseDetails(ejercicio.id))
                                        }
                                    }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    // 🖼️ Imagen
                                    if (ejercicio.urlImagen?.isNotEmpty() == true) {
                                        AsyncImage(
                                            model = ejercicio.urlImagen,
                                            contentDescription = ejercicio.nombre,
                                            modifier = Modifier
                                                .size(100.dp)
                                                .clip(RoundedCornerShape(16.dp))
                                                .background(Color(0xFFE0E0E0)),
                                            contentScale = ContentScale.Crop,
                                        )
                                    } else {
                                        Image(
                                            painter = painterResource(id = ejercicio.imagenResId),
                                            contentDescription = ejercicio.nombre,
                                            modifier = Modifier
                                                .size(100.dp)
                                                .clip(RoundedCornerShape(16.dp))
                                                .background(Color(0xFFE0E0E0)),
                                            contentScale = ContentScale.Crop
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(16.dp))

                                    // Información del ejercicio
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .align(Alignment.Top)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Column(
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text(
                                                    text = ejercicio.nombre,
                                                    fontSize = 16.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF212121),
                                                    modifier = Modifier.padding(bottom = 2.dp),
                                                    maxLines = 1
                                                )

                                                Text(
                                                    text = ejercicio.descripcion,
                                                    fontSize = 12.sp,
                                                    color = Color(0xFF757575),
                                                    modifier = Modifier.padding(bottom = 4.dp),
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis
                                                )

                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Badge(
                                                        modifier = Modifier.height(24.dp),
                                                        containerColor = Color(0xFF2196F3).copy(alpha = 0.2f)
                                                    ) {
                                                        Text(
                                                            text = secsToMinSec(ejercicio.duracion),
                                                            fontSize = 11.sp,
                                                            color = Color(0xFF2196F3),
                                                            modifier = Modifier.padding(
                                                                horizontal = 6.dp,
                                                                vertical = 2.dp
                                                            )
                                                        )
                                                    }

                                                    Badge(
                                                        modifier = Modifier.height(24.dp),
                                                        containerColor = when (ejercicio.intensidad.lowercase()) {
                                                            "baja" -> Color(0xFF2E7D32).copy(alpha = 0.2f)
                                                            "media" -> Color(0xFFF57C00).copy(alpha = 0.2f)
                                                            else -> Color(0xFFC62828).copy(alpha = 0.2f)
                                                        }
                                                    ) {
                                                        Text(
                                                            text = ejercicio.intensidad,
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Medium,
                                                            color = when (ejercicio.intensidad.lowercase()) {
                                                                "baja" -> Color(0xFF2E7D32)
                                                                "media" -> Color(0xFFF57C00)
                                                                else -> Color(0xFFC62828)
                                                            },
                                                            modifier = Modifier.padding(
                                                                horizontal = 6.dp,
                                                                vertical = 2.dp
                                                            )
                                                        )
                                                    }
                                                }
                                            }

                                            // ✅ Estado visual (NO clickable)
                                            if (realizado) {
                                                Icon(
                                                    Icons.Filled.CheckCircle,
                                                    contentDescription = "Completado - No se puede cambiar",
                                                    tint = Color(0xFF4CAF50),
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            } else {
                                                // ❌ No marcado - solo visual, sin interactividad (blanco con borde)
                                                Box(
                                                    modifier = Modifier
                                                        .size(24.dp)
                                                        .background(
                                                            Color.White,
                                                            shape = RoundedCornerShape(50.dp)
                                                        )
                                                        .border(
                                                            2.dp,
                                                            Color(0xFFBDBDBD),
                                                            RoundedCornerShape(50.dp)
                                                        )
                                                )
                                            }
                                        }

                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 🔄 OVERLAY DE CARGANDO
        if (isRefreshing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(0.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    ),
                    modifier = Modifier
                        .size(180.dp)
                ) {
                    val spinRotation by animateFloatAsState(
                        targetValue = 360f,
                        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                            animation = androidx.compose.animation.core.tween(1200, easing = androidx.compose.animation.core.LinearEasing),
                            repeatMode = androidx.compose.animation.core.RepeatMode.Restart
                        ),
                        label = "spinRotation"
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Filled.Refresh,
                            contentDescription = "Cargando",
                            tint = Color(0xFF2196F3),
                            modifier = Modifier
                                .size(52.dp)
                                .rotate(spinRotation)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Actualizando...",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF2196F3)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Por favor espera",
                            fontSize = 12.sp,
                            color = Color(0xFF757575)
                        )
                    }
                }
            }
        }
    }
}
