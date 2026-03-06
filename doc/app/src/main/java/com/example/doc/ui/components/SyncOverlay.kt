package com.example.doc.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Overlay profesional para mostrar durante la sincronización
 * Aparece cuando se sincroniza un usuario registrado anteriormente
 */
@Composable
fun SyncOverlay(
    visible: Boolean,
    syncStep: String = "Sincronizando datos...",
    progress: Float = 0f  // 0-1, 0 = indeterminado
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f)),
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(12.dp),
                modifier = Modifier
                    .width(280.dp)
                    .padding(24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Círculo animado de carga
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .background(
                                color = Color(0xFF2196F3).copy(alpha = 0.1f),
                                shape = RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(48.dp),
                            color = Color(0xFF2196F3),
                            strokeWidth = 4.dp
                        )
                    }

                    // Título
                    Text(
                        text = "Sincronizando",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2196F3)
                        ),
                        textAlign = TextAlign.Center
                    )

                    // Mensaje de estado
                    Text(
                        text = syncStep,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color.Gray,
                            fontSize = 14.sp
                        ),
                        textAlign = TextAlign.Center
                    )

                    // Progreso opcional
                    if (progress > 0f && progress < 1f) {
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth().height(4.dp),
                            color = Color(0xFF2196F3),
                            trackColor = Color(0xFF2196F3).copy(alpha = 0.2f)
                        )
                        Text(
                            text = "${(progress * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray
                        )
                    }
                }
            }
        }
    }
}

/**
 * Estados de sincronización con mensajes amigables
 */
enum class SyncOverlayState(val mensaje: String) {
    IDLE(""),
    CHECKING_USER("Verificando usuario..."),
    LOADING_PROFILE("Cargando perfil..."),
    SYNCING_DATA("Sincronizando datos personales..."),
    LOADING_EXERCISES("Cargando ejercicios..."),
    SYNCING_RECORDS("Sincronizando registros..."),
    FINALIZING("Finalizando sincronización..."),
    DONE("¡Listo!")
}
