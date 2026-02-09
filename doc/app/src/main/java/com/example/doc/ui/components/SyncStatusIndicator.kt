package com.example.doc.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
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

/**
 * SyncStatusIndicator - Indicador sutil de estado de sincronización
 * 
 * Mostrado en la esquina superior derecha sin interrumpir el diseño
 * Estados:
 * - Sincronizado: Check verde
 * - Sincronizando: Spinner animado
 * - Error: Info naranja
 */
@Composable
fun SyncStatusIndicator(
    isSynced: Boolean,
    isSyncing: Boolean,
    lastSyncTime: String? = null,
    modifier: Modifier = Modifier
) {
    val rotation by animateFloatAsState(
        targetValue = if (isSyncing) 360f else 0f,
        animationSpec = tween(durationMillis = 1000, delayMillis = 0),
        label = "sync_rotation"
    )

    val alpha by animateFloatAsState(
        targetValue = if (isSyncing || !isSynced) 1f else 0.6f,
        animationSpec = tween(durationMillis = 300),
        label = "sync_alpha"
    )

    if (!isSyncing && isSynced) {
        // Estado: Sincronizado - Mostrar discretamente
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
                modifier = Modifier
                    .size(16.dp)
                    .alpha(0.7f),
                tint = Color(0xFF2D6A4F)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Sincronizado",
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF2D6A4F).copy(alpha = 0.7f),
                maxLines = 1
            )
        }
    } else if (isSyncing) {
        // Estado: Sincronizando - Mostrar spinner
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
                    .rotate(rotation)
                    .alpha(0.8f),
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
    } else {
        // Estado: No sincronizado - Mostrar con color de alerta
        Row(
            modifier = modifier
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFF57C00).copy(alpha = 0.1f))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Info,
                contentDescription = "Pendiente de sincronizar",
                modifier = Modifier
                    .size(16.dp)
                    .alpha(0.7f),
                tint = Color(0xFFF57C00)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Pendiente",
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFFF57C00).copy(alpha = 0.7f),
                maxLines = 1
            )
        }
    }
}
