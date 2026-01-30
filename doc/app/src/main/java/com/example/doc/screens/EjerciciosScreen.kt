package com.example.doc.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import coil.compose.AsyncImage
import com.example.doc.navigation.Routes
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EjerciciosScreen(
    navController: NavController,
    usuarioViewModel: UsuarioViewModel,
    ejercicioViewModel: EjercicioViewModel
) {
    val usuario by usuarioViewModel.usuario.collectAsState()
    val ejercicios by ejercicioViewModel.ejercicios.collectAsState()
    val realizadosHoy by ejercicioViewModel.realizadosHoy.collectAsState()
    val sdf = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val hoy = sdf.format(Date())

    LaunchedEffect(usuario, hoy) {
        usuario?.let {
            ejercicioViewModel.cargarEjerciciosPorEnfermedad(it.enfermedad, hoy)
            ejercicioViewModel.cargarRealizadosHoy(it.id, hoy)
        }
    }

    val adaptiveTopPadding = com.example.doc.utils.getAdaptiveTopPadding()

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
                .padding(
                    top = adaptiveTopPadding + 16.dp,
                    start = 20.dp,
                    end = 20.dp,
                    bottom = 20.dp
                ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Botón de regresar y título
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
                    // Botón de volver
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

                    Spacer(modifier = Modifier.height(16.dp))

                    // Título mejorado
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
                }
            }

            // Indicador de progreso del día
            item {
                val totalEjercicios = ejercicios.take(6).size
                val ejerciciosRealizados = realizadosHoy.size
                val progreso = if (totalEjercicios > 0) ejerciciosRealizados.toFloat() / totalEjercicios else 0f

                Card(
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF2196F3).copy(alpha = 0.1f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
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
                                text = "$ejerciciosRealizados/$totalEjercicios completados",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF757575)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = progreso,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = Color(0xFF4CAF50),
                            trackColor = Color(0xFFE0E0E0)
                        )
                    }
                }
            }

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
                            .clickable { navController.navigate(Routes.exerciseDetails(ejercicio.id)) }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(16.dp)
                        ) {
                            // Imagen del ejercicio con badge si está realizado
                            Box {
                                AsyncImage(
                                    model = ejercicio.imagenResId,
                                    contentDescription = "Imagen ejercicio",
                                    modifier = Modifier
                                        .size(80.dp)
                                        .clip(RoundedCornerShape(16.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                if (realizado) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFF4CAF50),
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.CheckCircle,
                                            contentDescription = "Completado",
                                            tint = Color.White,
                                            modifier = Modifier
                                                .size(24.dp)
                                                .padding(4.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = ejercicio.nombre,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp,
                                    color = if (realizado) Color(0xFF2E7D32) else Color(0xFF2196F3),
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = ejercicio.descripcion,
                                    fontSize = 14.sp,
                                    color = Color(0xFF666666),
                                    maxLines = 2,
                                    lineHeight = 18.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFF2196F3).copy(alpha = 0.1f)
                                    ) {
                                        val duracionTexto = when {
                                            ejercicio.duracion >= 60 -> "${ejercicio.duracion / 60}:${(ejercicio.duracion % 60).toString().padStart(2, '0')} min"
                                            else -> "${ejercicio.duracion}s"
                                        }
                                        Text(
                                            text = duracionTexto,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color(0xFF2196F3),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = when (ejercicio.intensidad.lowercase()) {
                                            "baja" -> Color(0xFF4CAF50).copy(alpha = 0.1f)
                                            "media" -> Color(0xFFFFA726).copy(alpha = 0.1f)
                                            else -> Color(0xFFF44336).copy(alpha = 0.1f)
                                        }
                                    ) {
                                        Text(
                                            text = ejercicio.intensidad,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = when (ejercicio.intensidad.lowercase()) {
                                                "baja" -> Color(0xFF2E7D32)
                                                "media" -> Color(0xFFF57C00)
                                                else -> Color(0xFFC62828)
                                            },
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
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