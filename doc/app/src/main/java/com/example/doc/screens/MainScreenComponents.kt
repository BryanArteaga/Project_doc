package com.example.doc.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import com.example.doc.R
import com.example.doc.domain.SaludEvaluator
import com.example.doc.model.EvaluacionSalud
import com.example.doc.model.RegistroSalud
import com.example.doc.model.Usuario
import com.example.doc.ui.components.SyncIconButton
import com.example.doc.utils.HealthFormatUtils
import com.example.doc.utils.abrirWhatsAppDoctor
import com.example.doc.viewmodel.PopupViewModel
import com.example.doc.viewmodel.SyncState

// ✅ COMPONENTE 1: Header - Solo recompone si usuario/syncState cambia
@Composable
fun MainScreenHeader(
    usuario: Usuario,
    navController: NavController,
    syncState: SyncState,
    onSyncClick: () -> Unit
) {
    val colorFondo = Color(android.graphics.Color.parseColor(usuario.colorEnfermedad))

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
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Hola, ${usuario.nombre}",
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

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.End
            ) {
                SyncIconButton(
                    syncState = syncState,
                    onSyncClick = onSyncClick
                )
            }
        }
    }
}

// ✅ COMPONENTE 2: Consejo Card - Solo recompone si consejos cambia
@Composable
fun MainScreenConsejoCard(
    usuario: Usuario,
    consejoTexto: String,
    onConsejoTap: (String) -> Unit,
    consejos: List<*>  // List<ConsejoEntity>
) {
    val colorFondo = Color(android.graphics.Color.parseColor(usuario.colorEnfermedad))
    val imagenId = obtenerImagen(usuario.enfermedad)

    Card(
        colors = CardDefaults.cardColors(containerColor = colorFondo),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable {
                // Cambio de consejo al tocar
                @Suppress("UNCHECKED_CAST")
                val consejosList = consejos as List<com.example.doc.data.entities.ConsejoEntity>
                val nuevoConsejo = consejosList.randomOrNull()?.consejo ?: "Cargando..."
                onConsejoTap(nuevoConsejo)
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
                Image(
                    painter = painterResource(id = imagenId),
                    contentDescription = "Imagen representativa de ${usuario.enfermedad}",
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
}

// ✅ COMPONENTE 3: Acciones Card - Sin dependencias de observables (recompone si props cambian)
@Composable
fun MainScreenAccionesCard(navController: NavController) {
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
}

// ✅ COMPONENTE 4: Registros Card - Solo recompone si registros/usuario cambia
@Composable
fun MainScreenRegistrosCard(
    usuario: Usuario,
    registros: List<RegistroSalud>,
    popupViewModel: PopupViewModel,
    registrosScroll: LazyListState = rememberLazyListState()
) {
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
                val registrosLimitados = registros.take(5)
                items(
                    count = registrosLimitados.size,
                    key = { index -> registrosLimitados[index].id }
                ) { index ->
                    val reg = registrosLimitados[index]
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
                                val eval = SaludEvaluator.evaluarRegistroDetallado(usuario, reg)
                                popupViewModel.evaluationColor.value = when (eval.estado) {
                                    EvaluacionSalud.Estado.NORMAL -> Color(0xFF4CAF50)
                                    EvaluacionSalud.Estado.LEVE -> Color(0xFFA3DC39)
                                    EvaluacionSalud.Estado.ADVERTENCIA -> Color(0xFFFFA500)
                                    EvaluacionSalud.Estado.EMERGENCIA -> Color(0xFFD32F2F)
                                }
                                // 📲 OPCIÓN B: Guardar estado para mostrar FAB WhatsApp
                                popupViewModel.setEvaluationEstado(eval.estado.toString())
                                popupViewModel.evaluationMessage.value = eval.mensaje
                                popupViewModel.evaluationDetails.value = eval.detalles
                                popupViewModel.showEvaluationPopup.value = true
                            }
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = reg.fecha.substring(0, 10),
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2196F3),
                                fontSize = 15.sp
                            )

                            // Parámetros específicos por enfermedad
                            when (usuario.enfermedad) {
                                "Hipertensión" -> {
                                    reg.presionSistolica?.let { syst ->
                                        reg.presionDiastolica?.let { dias ->
                                            Text("Presión: $syst/$dias mmHg", fontSize = 14.sp)
                                        }
                                    }
                                }
                                "Diabetes" -> {
                                    reg.glucosa?.let { Text("Glucosa: $it mg/dL", fontSize = 14.sp) }
                                }
                                "Obesidad" -> {
                                    reg.peso?.let {
                                        Text(
                                            "Peso: ${String.format(java.util.Locale.US, "%.1f", it)} kg",
                                            fontSize = 14.sp
                                        )
                                    }
                                    reg.pulso?.let { Text("Pulso: $it lat/min", fontSize = 14.sp) }
                                    reg.imc?.let {
                                        Text(
                                            "IMC: ${String.format(java.util.Locale.US, "%.2f", it)}",
                                            fontSize = 14.sp
                                        )
                                    }
                                }
                                "Colesterol alto" -> {
                                    reg.colesterol?.let { Text("Colesterol: $it mg/dL", fontSize = 14.sp) }
                                    reg.presionSistolica?.let { syst ->
                                        reg.presionDiastolica?.let { dias ->
                                            if (syst > 0) Text(
                                                "Presión: $syst/$dias mmHg",
                                                fontSize = 14.sp
                                            )
                                        }
                                    }
                                }
                            }

                            Text("Calorías: ${reg.calorias}", fontSize = 14.sp)
                            Text(
                                "Ejercicio: ${HealthFormatUtils.formatearMinutos(reg.ejercicioMinutos)}",
                                fontSize = 14.sp
                            )
                            Text("Ánimo: ${reg.estadoAnimo}", fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }
}

// ✅ COMPONENTE 5: Result Popup - Separado para mejor manejo de anim
@Composable
fun MainScreenResultPopup(
    isVisible: Boolean,
    message: String,
    color: Color,
    onDismiss: () -> Unit
) {
    if (isVisible) {
        // FIX #8: Usar updateTransition para consolidar animaciones relacionadas
        val transition = updateTransition(
            targetState = isVisible,
            label = "ResultPopupTransition"
        )
        
        val scale by transition.animateFloat(
            targetValueByState = { visible -> if (visible) 1f else 0.8f },
            transitionSpec = { tween(durationMillis = 300) },
            label = "PopupScale"
        )
        
        val alpha by transition.animateFloat(
            targetValueByState = { visible -> if (visible) 1f else 0f },
            transitionSpec = { tween(durationMillis = 300) },
            label = "PopupAlpha"
        )

        Dialog(
            onDismissRequest = onDismiss
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = color,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(10.dp),
                modifier = Modifier
                    .padding(horizontal = 32.dp)
                    .fillMaxWidth(0.85f)
                    .scale(scale)
                    .alpha(alpha)
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = if (message.contains("✔")) {
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
                        text = message,
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
}

// ✅ COMPONENTE 6: Evaluation Popup - Separado
@Composable
fun MainScreenEvaluationPopup(
    isVisible: Boolean,
    message: String,
    color: Color,
    details: List<String>,
    estado: String = "NORMAL",
    onDismiss: () -> Unit,
    context: android.content.Context? = null,
    usuario: Usuario? = null
) {
    if (isVisible) {
        // FIX #8: Usar updateTransition para sincronizar animaciones relacionadas
        val transition = updateTransition(
            targetState = isVisible,
            label = "EvaluationPopupTransition"
        )
        
        val scale by transition.animateFloat(
            targetValueByState = { visible -> if (visible) 1f else 0.8f },
            transitionSpec = { tween(durationMillis = 300) },
            label = "PopupScale"
        )
        
        val alpha by transition.animateFloat(
            targetValueByState = { visible -> if (visible) 1f else 0f },
            transitionSpec = { tween(durationMillis = 300) },
            label = "PopupAlpha"
        )

        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(
                usePlatformDefaultWidth = false
            )
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = color,
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
                        imageVector = when (color) {
                            Color(0xFF4CAF50) -> Icons.Filled.CheckCircle
                            Color(0xFFA3DC39) -> Icons.Filled.Info
                            Color(0xFFFFA500) -> Icons.Filled.Warning
                            Color(0xFFD32F2F) -> Icons.Filled.Error
                            else -> Icons.Filled.Info
                        },
                        contentDescription = "Estado de evaluación",
                        modifier = Modifier.size(56.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = message,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Divider(color = Color.White.copy(alpha = 0.6f), thickness = 1.dp)
                    Spacer(modifier = Modifier.height(12.dp))
                    details.forEach { detail ->
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
                    
                    // 📲 OPCIÓN B: Mostrar botón WhatsApp solo si NO es NORMAL
                    if (estado != "NORMAL") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    // Abrir WhatsApp con pregunta contextual + detalles del registro
                                    val mensaje = when (estado) {
                                        "LEVE" -> "¿Por qué mi registro se muestra en amarillo?"
                                        "ADVERTENCIA" -> "¿Por qué mi registro se muestra en naranja?"
                                        "EMERGENCIA" -> "¿Por qué mi registro se muestra en rojo?"
                                        else -> "¿Qué significa mi registro?"
                                    }
                                    val detallesTexto = details.joinToString("\n")
                                    abrirWhatsAppDoctor(context, usuario, mensaje, detallesTexto)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF25D366),  // WhatsApp green
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Call,
                                    contentDescription = "WhatsApp",
                                    modifier = Modifier
                                        .size(18.dp)
                                        .padding(end = 4.dp)
                                )
                                Text(
                                    "¿Por qué?",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                            
                            Button(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White,
                                    contentColor = Color(0xFF2196F3)
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    "Cerrar",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    } else {
                        Button(
                            onClick = onDismiss,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
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
    }
}
