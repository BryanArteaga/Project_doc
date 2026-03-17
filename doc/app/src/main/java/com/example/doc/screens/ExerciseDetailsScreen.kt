package com.example.doc.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.style.TextAlign
import coil.compose.AsyncImage
import com.example.doc.navigation.Routes
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseDetailsScreen(
    navController: NavController,
    usuarioViewModel: UsuarioViewModel,
    ejercicioViewModel: EjercicioViewModel,
    ejercicioId: Int
) {
    val usuario by usuarioViewModel.usuario.collectAsState()
    val ejercicios by ejercicioViewModel.ejercicios.collectAsState()
    val realizadosHoy by ejercicioViewModel.realizadosHoy.collectAsState()
    val ejercicio = ejercicios.find { it.id == ejercicioId } ?: return
    val sdf = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val hoy = sdf.format(Date())
    val realizado = realizadosHoy.contains(ejercicioId)

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
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Botón de regresar arriba
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = com.example.doc.utils.getAdaptiveTopPadding()),
                    horizontalArrangement = Arrangement.Start
                ) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White.copy(alpha = 0.9f)
                        ),
                        elevation = CardDefaults.cardElevation(4.dp),
                        modifier = Modifier.clickable { navController.popBackStack() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ArrowBack,
                                contentDescription = "Volver",
                                tint = Color(0xFF2196F3),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Volver",
                                color = Color(0xFF2196F3),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // Card principal con la información del ejercicio
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White.copy(alpha = 0.95f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Badge de estado
                        if (realizado) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF4CAF50),
                                modifier = Modifier.padding(bottom = 12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.CheckCircle,
                                        contentDescription = "Completado",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Completado hoy",
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // Título del ejercicio
                        Text(
                            text = ejercicio.nombre,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2196F3),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // Imagen del ejercicio con prioridad URL
                        when {
                            ejercicio.urlImagen != null && ejercicio.urlImagen.isNotEmpty() -> {
                                AsyncImage(
                                    model = ejercicio.urlImagen,
                                    contentDescription = "Imagen ejercicio",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(300.dp)
                                        .clip(RoundedCornerShape(20.dp)),
                                    contentScale = ContentScale.Fit
                                )
                            }
                            ejercicio.imagenResId > 0 -> {
                                Image(
                                    painter = painterResource(id = ejercicio.imagenResId),
                                    contentDescription = "Imagen ejercicio",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(300.dp)
                                        .clip(RoundedCornerShape(20.dp)),
                                    contentScale = ContentScale.Fit
                                )
                            }
                            else -> {
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(300.dp)
                                        .clip(RoundedCornerShape(20.dp)),
                                    color = Color.LightGray
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Filled.CheckCircle,
                                            contentDescription = "Sin imagen",
                                            tint = Color.Gray,
                                            modifier = Modifier.size(80.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Información en cards
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Card de duración
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFF2196F3).copy(alpha = 0.1f)
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    val duracionTexto = when {
                                        ejercicio.duracion >= 60 -> "${ejercicio.duracion / 60}:${(ejercicio.duracion % 60).toString().padStart(2, '0')}"
                                        else -> "${ejercicio.duracion}s"
                                    }
                                    val unidadTexto = if (ejercicio.duracion >= 60) "min" else "por serie"

                                    Text(
                                        text = duracionTexto,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF2196F3)
                                    )
                                    Text(
                                        text = unidadTexto,
                                        fontSize = 11.sp,
                                        color = Color(0xFF757575)
                                    )
                                }
                            }

                            // Card de intensidad
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = when (ejercicio.intensidad.lowercase()) {
                                        "baja" -> Color(0xFF4CAF50).copy(alpha = 0.1f)
                                        "media" -> Color(0xFFFFA726).copy(alpha = 0.1f)
                                        else -> Color(0xFFF44336).copy(alpha = 0.1f)
                                    }
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = ejercicio.intensidad,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = when (ejercicio.intensidad.lowercase()) {
                                            "baja" -> Color(0xFF2E7D32)
                                            "media" -> Color(0xFFF57C00)
                                            else -> Color(0xFFC62828)
                                        }
                                    )
                                    Text(
                                        text = "intensidad",
                                        fontSize = 11.sp,
                                        color = Color(0xFF757575)
                                    )
                                }
                            }

                            // Card de series
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFFFFE0B2)
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = ejercicio.series.toString(),
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFE65100)
                                    )
                                    Text(
                                        text = "series",
                                        fontSize = 11.sp,
                                        color = Color(0xFF757575)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Descripción
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFF5F5F5)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Text(
                                    text = "Descripción",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2196F3)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = ejercicio.descripcion,
                                    fontSize = 15.sp,
                                    color = Color(0xFF555555),
                                    lineHeight = 22.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }

            // Botones de acción
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Botón principal: Iniciar/Volver a realizar
                    Button(
                        onClick = {
                            navController.navigate(Routes.exerciseTimer(ejercicio.id))
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2196F3)
                        ),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 6.dp,
                            pressedElevation = 8.dp
                        )
                    ) {
                        Text(
                            text = if (realizado) "Volver a realizar" else "Iniciar ejercicio",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    // Botón secundario: Cambiar ejercicio (solo si no está realizado)
                    if (!realizado) {
                        Button(
                            onClick = {
                                usuario?.let {
                                    ejercicioViewModel.cambiarEjercicio(it.enfermedad, ejercicio.id, hoy)
                                }
                                navController.popBackStack()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFF9800)
                            ),
                            elevation = ButtonDefaults.buttonElevation(
                                defaultElevation = 4.dp,
                                pressedElevation = 6.dp
                            )
                        ) {
                            Text(
                                text = "Cambiar ejercicio",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            // Espaciado final
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}