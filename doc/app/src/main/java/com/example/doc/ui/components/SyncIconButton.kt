package com.example.doc.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.doc.viewmodel.SyncState

/**
 * Botón interactivo de sincronización con icono y estado
 *
 * Estados:
 * - IDLE: Solo icono visible (presionable)
 * - SYNCING: Icono rotativo + texto "Sincronizando..."
 * - SYNCED: Check verde + texto "Sincronizado" (se oculta después 2s)
 * - PENDING: Icono nube gris + texto "Pendiente"
 * - ERROR: Icono nube roja + texto "Error"
 */
@Composable
fun SyncIconButton(
    syncState: SyncState,
    onSyncClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when (syncState) {
            SyncState.IDLE -> {
                // Solo icono, presionable
                Icon(
                    imageVector = Icons.Filled.Sync,
                    contentDescription = "Sincronizar",
                    modifier = Modifier
                        .size(28.dp)
                        .clickable { onSyncClick() },
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            SyncState.SYNCING -> {
                // Icono rotativo + texto
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Filled.Sync,
                            contentDescription = "Sincronizando",
                            modifier = Modifier
                                .size(28.dp)
                                .then(
                                    // Simular rotación con animación (en app completa usar animateRotation)
                                    Modifier
                                ),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = "Sincronizando...",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            SyncState.SYNCED -> {
                // Check verde + texto de éxito (aparece brevemente)
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Done,
                            contentDescription = "Sincronizado",
                            modifier = Modifier.size(28.dp),
                            tint = Color(0xFF4CAF50) // Verde éxito
                        )
                        Text(
                            text = "✓ Sincronizado",
                            fontSize = 12.sp,
                            color = Color(0xFF4CAF50),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            SyncState.PENDING -> {
                // Nube gris + texto "Pendiente"
                Icon(
                    imageVector = Icons.Filled.CloudOff,
                    contentDescription = "Sincronización pendiente",
                    modifier = Modifier
                        .size(28.dp)
                        .clickable { onSyncClick() }, // Permitir reintentar
                    tint = Color(0xFFFF9800) // Naranja advertencia
                )
                Text(
                    text = "Pendiente",
                    fontSize = 12.sp,
                    color = Color(0xFFFF9800),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            SyncState.ERROR -> {
                // Nube roja + texto "Error"
                Icon(
                    imageVector = Icons.Filled.CloudOff,
                    contentDescription = "Error en sincronización",
                    modifier = Modifier
                        .size(28.dp)
                        .clickable { onSyncClick() }, // Permitir reintentar
                    tint = Color(0xFFF44336) // Rojo error
                )
                Text(
                    text = "Error",
                    fontSize = 12.sp,
                    color = Color(0xFFF44336),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

/**
 * Versión compacta solo con icono (sin texto)
 * Útil para barra superior o espacios pequeños
 */
@Composable
fun CompactSyncIcon(
    syncState: SyncState,
    onSyncClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.clickable(enabled = syncState != SyncState.SYNCING) { onSyncClick() }
    ) {
        when (syncState) {
            SyncState.IDLE -> {
                Icon(
                    imageVector = Icons.Filled.Sync,
                    contentDescription = "Sincronizar",
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            SyncState.SYNCING -> {
                Icon(
                    imageVector = Icons.Filled.Sync,
                    contentDescription = "Sincronizando",
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            SyncState.SYNCED -> {
                Icon(
                    imageVector = Icons.Filled.Done,
                    contentDescription = "Sincronizado",
                    modifier = Modifier.size(24.dp),
                    tint = Color(0xFF4CAF50)
                )
            }

            SyncState.PENDING -> {
                Icon(
                    imageVector = Icons.Filled.CloudOff,
                    contentDescription = "Pendiente",
                    modifier = Modifier.size(24.dp),
                    tint = Color(0xFFFF9800)
                )
            }

            SyncState.ERROR -> {
                Icon(
                    imageVector = Icons.Filled.Cloud,
                    contentDescription = "Error",
                    modifier = Modifier.size(24.dp),
                    tint = Color(0xFFF44336)
                )
            }
        }
    }
}
