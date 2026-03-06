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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.example.doc.utils.UiState
import com.example.doc.viewmodel.UsuarioViewModel
import timber.log.Timber

@Composable
fun ConditionSelectionScreen(
    navController: NavController,
    usuarioViewModel: UsuarioViewModel,
    cedula: String?,
    name: String,
    apellido: String?,  // Nuevo: apellido
    edad: Int,
    peso: Double,
    altura: Double,
    sexo: String?,
    onConditionSelected: (String, String) -> Unit
) {
    val configuration = LocalConfiguration.current
    val scroll = rememberScrollState()
    val uiState by usuarioViewModel.uiState.collectAsState()
    
    // Estados para manejar la navegación después del registro
    var selectedCondition by remember { mutableStateOf<String?>(null) }
    var selectedColor by remember { mutableStateOf<String?>(null) }
    var usuarioExisteLocal by remember { mutableStateOf(false) }
    var cargandoUsuarioLocal by remember { mutableStateOf(true) }

    val imc = if (altura > 0) peso / (altura * altura) else 0.0
    val sugerencia = when {
        imc >= 30 -> "Obesidad"
        imc >= 25 -> "Colesterol alto"
        else -> ""
    }

    // 🔍 Verificar si el usuario YA EXISTE LOCALMENTE por cédula
    // Si existe, significa que ya fue registrado antes y NO debe cambiar enfermedad
    LaunchedEffect(cedula) {
        try {
            val existe = usuarioViewModel.usuarioExisteLocalmente(cedula)
            usuarioExisteLocal = existe
            Timber.d("🔍 Verificación de usuario local por cédula '$cedula': existe=$existe")
            
            if (existe) {
                // Usuario ya existe localmente, cargar sus datos y NO mostrar ConditionScreen
                Timber.d("✅ Usuario ya existe localmente (cédula=$cedula), saltando ConditionScreen...")
                val prefs = navController.context.getSharedPreferences("medicini_prefs", android.content.Context.MODE_PRIVATE)
                val usuarioLocal = usuarioViewModel.obtenerUsuarioLocalmente(cedula)
                if (usuarioLocal != null) {
                    prefs.edit().putInt("user_id", usuarioLocal.id).apply()
                    Timber.d("📝 SharedPreferences actualizado con user_id (usuario existente): ${usuarioLocal.id}")
                    // Navegar directo al dashboard (no mostrar selección de enfermedad)
                    navController.navigate("dashboard") {
                        popUpTo("condition") { inclusive = true }
                    }
                }
            }
            cargandoUsuarioLocal = false
        } catch (e: Exception) {
            Timber.e(e, "❌ Error verificando usuario local")
            cargandoUsuarioLocal = false
        }
    }

    // Observar el estado de registro y navegar cuando esté listo
    LaunchedEffect(uiState) {
        when (uiState) {
            is UiState.Success -> {
                if (selectedCondition != null && selectedColor != null) {
                    Timber.d("✅ Usuario registrado exitosamente, navegando...")
                    val prefs = navController.context.getSharedPreferences("medicini_prefs", android.content.Context.MODE_PRIVATE)
                    val userId = (uiState as UiState.Success<Usuario>).data.id
                    prefs.edit().putInt("user_id", userId).apply()
                    Timber.d("📝 SharedPreferences actualizado con user_id: $userId")
                    
                    Timber.d("🚀 Llamando callback de navegación...")
                    onConditionSelected(selectedCondition!!, selectedColor!!)
                    
                    // Resetear los estados
                    selectedCondition = null
                    selectedColor = null
                }
            }
            is UiState.Error -> {
                val error = (uiState as UiState.Error).message
                Timber.e("❌ Error al registrar usuario: $error")
                // Aquí podrías mostrar un snackbar o dialog con el error
                selectedCondition = null
                selectedColor = null
            }
            else -> {}
        }
    }

    // Si el usuario existe localmente, mostrar loading hasta que se navegue
    if (cargandoUsuarioLocal || usuarioExisteLocal) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.background_1),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            CircularProgressIndicator(color = Color(0xFF2196F3))
        }
        return
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
                                Timber.d("Botón de enfermedad clickeado: $condicion")

                                val usuario = Usuario(
                                    cedula = cedula,
                                    nombre = name,
                                    apellido = apellido,
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

                                // Guardar la condición y color seleccionados para navegar después del registro
                                selectedCondition = condicion
                                selectedColor = color.toHexColor()

                                Timber.d("✍️ Registrando usuario: ${usuario.nombre}, enfermedad: ${usuario.enfermedad}")
                                usuarioViewModel.registrarUsuario(usuario)
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