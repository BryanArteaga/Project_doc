package com.example.doc.screens

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.doc.R
import com.example.doc.model.Usuario
import com.example.doc.viewmodel.UsuarioViewModel
import timber.log.Timber

@Composable
fun ConditionSelectionScreen(
    navController: NavController,
    usuarioViewModel: UsuarioViewModel,
    cedula: String?,
    name: String,
    edad: Int,
    peso: Double,
    altura: Double,
    sexo: String?,
    onConditionSelected: (String, String) -> Unit
) {
    val configuration = LocalConfiguration.current
    val scroll = rememberScrollState()

    val imc = if (altura > 0) peso / (altura * altura) else 0.0
    val sugerencia = when {
        imc >= 30 -> "Obesidad"
        imc >= 25 -> "Colesterol alto"
        else -> ""
    }

    val conditions = listOf(
        Triple("Hipertensión", Color.Red, Icons.Filled.Favorite),
        Triple("Diabetes", Color(0xFFFFC107), Icons.Filled.Opacity),
        Triple("Obesidad", Color(0xFF81D4FA), Icons.Filled.FitnessCenter),
        Triple("Colesterol alto", Color(0xFFFFA726), Icons.Filled.EmojiEmotions)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 24.dp)
    ) {
        Image(
            painter = painterResource(id = R.drawable.background_1),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scroll)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                shape = RoundedCornerShape(32.dp),
                elevation = CardDefaults.cardElevation(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.88f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Selecciona tu condición",
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    if (sugerencia.isNotBlank()) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF81D4FA).copy(alpha = 0.18f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Sugerencia: $sugerencia",
                                modifier = Modifier.padding(12.dp),
                                color = Color(0xFF2196F3),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    conditions.forEach { (condicion, color, icon) ->
                        Button(
                            onClick = {
                                Timber.d("🔘 Botón de enfermedad clickeado: $condicion")

                                val usuario = Usuario(
                                    cedula = cedula,
                                    nombre = name,
                                    edad = edad,
                                    peso = peso,
                                    altura = String.format(java.util.Locale.US, "%.2f", altura).toDouble(),
                                    sexo = sexo,
                                    enfermedad = condicion,
                                    colorEnfermedad = color.toHexColor(),
                                    notifEnabled = true,
                                    notifType = "daily_fixed",
                                    notifDetails = "{\"hour\":20}"
                                )

                                Timber.d("✍️ Registrando usuario: ${usuario.nombre}, enfermedad: ${usuario.enfermedad}")
                                usuarioViewModel.registrarUsuario(usuario)

                                Timber.d("💾 Usuario enviado a guardar, ID local: ${usuario.id}")
                                val prefs = navController.context.getSharedPreferences("medicini_prefs", Context.MODE_PRIVATE)
                                prefs.edit().putInt("user_id", usuario.id).apply()
                                Timber.d("📝 SharedPreferences actualizado con user_id: ${usuario.id}")

                                Timber.d("🚀 Llamando callback de navegación...")
                                onConditionSelected(condicion, color.toHexColor())
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(65.dp)
                                .padding(vertical = 8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = color),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = condicion,
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = condicion,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

fun Color.toHexColor(): String {
    val r = (red * 255).toInt().coerceIn(0, 255)
    val g = (green * 255).toInt().coerceIn(0, 255)
    val b = (blue * 255).toInt().coerceIn(0, 255)
    return String.format("#%02X%02X%02X", r, g, b)
}