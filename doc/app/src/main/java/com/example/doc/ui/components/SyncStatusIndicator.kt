package com.example.doc.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.doc.viewmodel.SyncState

/**
 * SyncStatusIndicator Mejorado - Indicador INTERACTIVO de sincronización
 *
 * Estados:
 * - IDLE: No mostrado (solo icono si lo presionas)
 * - SYNCING: Check giratorio + "Sincronizando..."
 * - SYNCED: Check verde + "✓ Sincronizado" (se oculta después 2s)
 * - PENDING: Nube gris + "Pendiente" (presionable para reintentar)
 * - ERROR: Nube roja + "Error" (presionable para reintentar)
 *
 * Presionar: Sincronización MANUAL
 */
@Composable
fun SyncStatusIndicator(
    isSynced: Boolean,
    isSyncing: Boolean,
    lastSyncTime: String? = null,
    syncState: SyncState = SyncState.IDLE,
    onSyncClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val rotation by animateFloatAsState(
        targetValue = if (isSyncing || syncState == SyncState.SYNCING) 360f else 0f,
        animationSpec = tween(durationMillis = 1000, delayMillis = 0),
        label = "sync_rotation"
    )

    // Mostrar diferentes estados según el SyncState
    when (syncState) {
        SyncState.IDLE -> {
            // No mostrar nada en estado IDLE (usuario no lo verá)
            if (lastSyncTime != null) {
                Text(
                    text = "Última sync: $lastSyncTime",
                    fontSize = 8.sp,
                    color = Color.Gray.copy(alpha = 0.5f),
                    modifier = modifier
                )
            }
        }

        SyncState.SYNCING -> {
            // Estado: Sincronizando - Mostrar spinner + texto
            Row(
                modifier = modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1976D2).copy(alpha = 0.1f))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Info,
                    contentDescription = "Sincronizando",
                    modifier = Modifier
                        .size(16.dp)
                        .rotate(rotation),
                    tint = Color(0xFF1976D2)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Sincronizando...",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF1976D2).copy(alpha = 0.8f),
                    maxLines = 1
                )
            }
        }

        SyncState.SYNCED -> {
            // Estado: Sincronizado - Mostrar check verde + texto
            Row(
                modifier = modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF2D6A4F).copy(alpha = 0.1f))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = "Sincronizado",
                    modifier = Modifier.size(16.dp),
                    tint = Color(0xFF2D6A4F)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "✓ Sincronizado",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF2D6A4F),
                    maxLines = 1
                )
            }
        }

        SyncState.PENDING -> {
            // Estado: Pendiente (sin internet) - Presionable para reintentar
            Row(
                modifier = modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFFF9800).copy(alpha = 0.1f))
                    .clickable { onSyncClick() }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.CloudOff,
                    contentDescription = "Pendiente",
                    modifier = Modifier.size(16.dp),
                    tint = Color(0xFFFF9800)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Pendiente",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFFFF9800),
                    maxLines = 1
                )
            }
        }

        SyncState.ERROR -> {
            // Estado: Error - Presionable para reintentar
            Row(
                modifier = modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF44336).copy(alpha = 0.1f))
                    .clickable { onSyncClick() }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.CloudOff,
                    contentDescription = "Error",
                    modifier = Modifier.size(16.dp),
                    tint = Color(0xFFF44336)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Error",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFFF44336),
                    maxLines = 1
                )
            }
        }
    }
}
