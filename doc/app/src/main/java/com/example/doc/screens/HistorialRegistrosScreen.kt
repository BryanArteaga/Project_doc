package com.example.doc.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.example.doc.data.entities.RegistroSaludEntity
import com.example.doc.domain.SaludEvaluator
import com.example.doc.model.EvaluacionSalud
import com.example.doc.model.RegistroSalud
import com.example.doc.model.Usuario
import com.example.doc.utils.abrirWhatsAppDoctor
import com.example.doc.viewmodel.RegistroSaludViewModel
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistorialRegistrosScreen(
    navController: NavController,
    registroSaludViewModel: RegistroSaludViewModel,
    usuarioId: Int,
    usuario: Usuario? = null
) {
    // Obtener registros paginados (solo carga lo necesario)
    val registrosPaginados: LazyPagingItems<RegistroSaludEntity> =
        registroSaludViewModel.getRegistrosPaginados(usuarioId).collectAsLazyPagingItems()

    // Estado para el popup de evaluación
    val showEvaluationPopup = remember { mutableStateOf(false) }
    val evaluationMessage = remember { mutableStateOf("") }
    val evaluationColor = remember { mutableStateOf(Color.White) }
    val evaluationDetails = remember { mutableStateOf("") }
    val evaluationEstado = remember { mutableStateOf("NORMAL") }
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Historial completo") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(
                count = registrosPaginados.itemCount,
                key = registrosPaginados.itemKey { it.id }
            ) { index ->
                val registro = registrosPaginados[index]
                registro?.let {
                    RegistroCard(
                        registro = it,
                        onClicked = {
                            if (usuario != null) {
                                // Convertir RegistroSaludEntity a RegistroSalud
                                val registroSalud = RegistroSalud(
                                    id = it.id,
                                    usuarioId = it.usuarioId,
                                    fecha = it.fecha,
                                    hora = it.hora,
                                    presionSistolica = it.presionSistolica,
                                    presionDiastolica = it.presionDiastolica,
                                    glucosa = it.glucosa,
                                    colesterol = it.colesterol,
                                    temperatura = it.temperatura,
                                    peso = it.peso,
                                    imc = it.imc,
                                    pulso = it.pulso,
                                    calorias = it.calorias,
                                    ejercicio = it.ejercicio,
                                    ejercicioMinutos = it.ejercicioMinutos,
                                    estadoAnimo = it.estadoAnimo,
                                    notas = it.notas,
                                    registradoEn = it.registradoEn
                                )
                                
                                val eval = SaludEvaluator.evaluarRegistroDetallado(usuario, registroSalud)
                                evaluationMessage.value = eval.mensaje
                                evaluationColor.value = when (eval.estado) {
                                    EvaluacionSalud.Estado.NORMAL -> Color(0xFF4CAF50)
                                    EvaluacionSalud.Estado.LEVE -> Color(0xFFA3DC39)
                                    EvaluacionSalud.Estado.ADVERTENCIA -> Color(0xFFFFA500)
                                    EvaluacionSalud.Estado.EMERGENCIA -> Color(0xFFD32F2F)
                                }
                                evaluationDetails.value = eval.detalles.joinToString("\n")
                                evaluationEstado.value = eval.estado.toString()
                                showEvaluationPopup.value = true
                            }
                        }
                    )
                }
            }

            // Indicador de carga al final
            when {
                registrosPaginados.itemCount == 0 -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No hay registros",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }
        }
    }

    // Popup de evaluación con WhatsApp
    if (showEvaluationPopup.value) {
        AlertDialog(
            onDismissRequest = { showEvaluationPopup.value = false },
            title = {
                Text(
                    text = evaluationMessage.value,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = evaluationColor.value
                )
            },
            text = {
                Text(text = evaluationDetails.value)
            },
            confirmButton = {
                if (evaluationEstado.value != "NORMAL") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Botón WhatsApp
                        Button(
                            onClick = {
                                val mensaje = "¿Por qué me sale en ${when(evaluationEstado.value) {
                                    "LEVE" -> "amarillo"
                                    "ADVERTENCIA" -> "naranja"
                                    "EMERGENCIA" -> "rojo"
                                    else -> evaluationEstado.value
                                }}?"
                                abrirWhatsAppDoctor(context, usuario, mensaje, evaluationDetails.value)
                                showEvaluationPopup.value = false
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                        ) {
                            Text("¿Por qué?")
                        }

                        // Botón Cerrar
                        Button(
                            onClick = { showEvaluationPopup.value = false },
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Gray
                            )
                        ) {
                            Text("Cerrar")
                        }
                    }
                } else {
                    Button(
                        onClick = { showEvaluationPopup.value = false },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                    ) {
                        Text("Aceptar")
                    }
                }
            }
        )
    }
}

@Composable
private fun RegistroCard(
    registro: RegistroSaludEntity,
    onClicked: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                enabled = true,
                onClickLabel = "Ver detalles del registro",
                onClick = onClicked
            ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(16.dp)
        ) {
            // Fecha
            Text(
                text = formatearFecha(registro.fecha),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2196F3)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Datos de salud
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    // Mostrar presión sistólica/diastólica
                    if (registro.presionSistolica != null || registro.presionDiastolica != null) {
                        val presionText = "${registro.presionSistolica ?: "?"}/${registro.presionDiastolica ?: "?"}"
                        InfoItem("Presión:", "$presionText mmHg")
                    }
                    registro.glucosa?.let {
                        InfoItem("Glucosa:", "$it mg/dL")
                    }
                    registro.temperatura?.let {
                        InfoItem("Temperatura:", String.format("%.1f°C", it))
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    registro.colesterol?.let {
                        InfoItem("Colesterol:", "$it mg/dL")
                    }
                    registro.peso?.let {
                        InfoItem("Peso:", String.format("%.1f kg", it))
                    }
                    registro.calorias?.let {
                        InfoItem("Calorías:", "$it kcal")
                    }
                }
            }

            // Estado de ánimo
            registro.estadoAnimo?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Estado: $it",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
private fun InfoItem(label: String, value: String) {
    Row {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontSize = 14.sp
        )
    }
}

private fun formatearFecha(fecha: String): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val outputFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val date = inputFormat.parse(fecha)
        date?.let { outputFormat.format(it) } ?: fecha
    } catch (e: Exception) {
        fecha
    }
}

