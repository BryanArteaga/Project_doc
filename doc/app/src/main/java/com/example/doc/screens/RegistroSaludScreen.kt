package com.example.doc.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.doc.model.RegistroSalud
import com.example.doc.viewmodel.RegistroSaludViewModel
import com.example.doc.viewmodel.UsuarioViewModel
import com.example.doc.R
import com.example.doc.utils.SafeDecimalVisualTransformation
import com.example.doc.utils.SafeIntegerVisualTransformation
import com.example.doc.viewmodel.EjercicioViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistroSaludScreen(
    navController: NavController,
    usuarioViewModel: UsuarioViewModel,
    registroViewModel: RegistroSaludViewModel,
    ejercicioViewModel: EjercicioViewModel,
    currentDate: String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
) {
    val scrollState = rememberScrollState()
    val usuario by usuarioViewModel.usuario.collectAsState()
    val ejercicios by ejercicioViewModel.ejercicios.collectAsState()
    val realizadosHoy by ejercicioViewModel.realizadosHoy.collectAsState()
    var errorMessages by remember { mutableStateOf<List<String>>(emptyList()) }
    var isSaving by remember { mutableStateOf(false) }
    var showConfirmNoEjercicioDialog by remember { mutableStateOf(false) }
    var showConfirmNoExtraDialog by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // Controlar loading inicial
    LaunchedEffect(ejercicios, realizadosHoy) {
        if (ejercicios.isNotEmpty() || realizadosHoy.isNotEmpty()) {
            isLoading = false
        }
    }

    if (usuario == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No hay usuario registrado.", fontSize = 16.sp)
        }
        return
    }

    val user = usuario!!

    val sdf = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    sdf.format(Date())

    var presionSistolica by remember { mutableStateOf("") }
    var presionDiastolica by remember { mutableStateOf("") }
    var glucosa by remember { mutableStateOf("") }
    var colesterol by remember { mutableStateOf("") }
    var pesoNuevo by remember { mutableStateOf("") }
    var pulso by remember { mutableStateOf("") }  // ✅ Pulso (opcional, solo Obesidad)
    var presionSistolicaColesterol by remember { mutableStateOf("") }  // ✅ Presión para Colesterol
    var presionDiastolicaColesterol by remember { mutableStateOf("") }  // ✅ Presión para Colesterol
    var calorias by remember { mutableStateOf("") }
    var ejercicioHoras by remember { mutableStateOf("") }
    var ejercicioMinutos by remember { mutableStateOf("") }
    var ejercicioHorasExtra by remember { mutableStateOf("") }
    var ejercicioMinutosExtra by remember { mutableStateOf("") }
    var animo by remember { mutableStateOf("") }
    var hizoEjercicioSinApp by remember { mutableStateOf<Boolean?>(null) }
    var hizoEjercicioExtra by remember { mutableStateOf<Boolean?>(null) }

    fun calcularMinutos(horas: String, minutos: String): Int {
        val h = horas.toIntOrNull() ?: 0
        val m = minutos.toIntOrNull() ?: 0
        return h * 60 + m
    }

    LaunchedEffect(user, currentDate) {
        ejercicioViewModel.cargarEjerciciosPorEnfermedad(user.enfermedad, currentDate)
        ejercicioViewModel.cargarRealizadosHoy(user.id, currentDate)
    }

    val totalEjercicioApp by remember(ejercicios, realizadosHoy) {
        derivedStateOf {
            val segundosTotales = realizadosHoy.sumOf { ejercicioId ->
                val ej = ejercicios.find { it.id == ejercicioId }
                // duracion está en SEGUNDOS, series es cantidad de repeticiones
                // Ejemplo: 30seg × 3 series = 90 segundos = 1.5 minutos (redondear a 2)
                (ej?.duracion?.toLong() ?: 0L) * (ej?.series?.toLong() ?: 1L)
            }
            // ✅ Convertir segundos a minutos y redondear correctamente
            kotlin.math.round(segundosTotales / 60.0).toInt()
        }
    }

    val hizoEjerciciosApp = totalEjercicioApp > 0
    val enableEjercicioInput = !hizoEjerciciosApp && hizoEjercicioSinApp == true
    val enableEjercicioExtraInput = hizoEjercicioExtra == true

    val ejercicioTotal = when {
        hizoEjerciciosApp || hizoEjercicioSinApp == true || hizoEjercicioExtra == true -> {
            val minutosApp = if (hizoEjerciciosApp) totalEjercicioApp else 0
            val minutosSinApp =
                if (enableEjercicioInput) calcularMinutos(ejercicioHoras, ejercicioMinutos) else 0
            val minutosExtra = if (enableEjercicioExtraInput) calcularMinutos(
                ejercicioHorasExtra,
                ejercicioMinutosExtra
            ) else 0
            minutosApp + minutosSinApp + minutosExtra
        }

        else -> 0
    }

    LaunchedEffect(totalEjercicioApp) {
        if (hizoEjerciciosApp) {
            ejercicioHoras = (totalEjercicioApp / 60).toString()
            ejercicioMinutos = (totalEjercicioApp % 60).toString()
        } else {
            ejercicioHoras = ""
            ejercicioMinutos = ""
        }
    }

    val opcionesEstado = listOf("Bueno", "Regular", "Malo")
    var expandedEstado by remember { mutableStateOf(false) }

    val colorCondicion = Color(android.graphics.Color.parseColor(user.colorEnfermedad))

    val imc: Double? = when (user.enfermedad) {
        "Obesidad" -> {
            val pesoValue = pesoNuevo.trim().toDoubleOrNull() ?: user.peso
            if (user.altura > 0.0) pesoValue / (user.altura * user.altura) else null
        }

        else -> null
    }

    if (showConfirmNoEjercicioDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmNoEjercicioDialog = false },
            title = { Text("Confirmar") },
            text = { Text("¿Está seguro que desea marcar el ejercicio como no hecho?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        hizoEjercicioSinApp = false
                        ejercicioHoras = ""
                        ejercicioMinutos = ""
                        showConfirmNoEjercicioDialog = false
                    }
                ) {
                    Text("Confirmar")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showConfirmNoEjercicioDialog = false }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (showConfirmNoExtraDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmNoExtraDialog = false },
            title = { Text("Confirmar") },
            text = { Text("¿Está seguro que no hizo más tiempo de ejercicio adicional?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        hizoEjercicioExtra = false
                        ejercicioHorasExtra = ""
                        ejercicioMinutosExtra = ""
                        showConfirmNoExtraDialog = false
                    }
                ) {
                    Text("Confirmar")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showConfirmNoExtraDialog = false }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Detecta si hay al menos un parámetro llenado
    val hayParametroLlenado = listOf(
        presionSistolica, presionDiastolica, glucosa, colesterol, pesoNuevo, pulso, calorias, animo
    ).any { it.isNotBlank() } || hizoEjercicioSinApp != null || hizoEjercicioExtra != null

    fun intentarGuardarRegistro() {
        if (!hayParametroLlenado || isSaving) return

        isSaving = true
        errorMessages = emptyList()
        val errors = mutableListOf<String>()
        var filledFields = 0

        when (user.enfermedad) {
            "Hipertensión" -> {
                if (presionSistolica.isNotBlank()) filledFields++
                if (presionDiastolica.isNotBlank()) filledFields++
            }

            "Diabetes" -> if (glucosa.isNotBlank()) filledFields++
            "Obesidad" -> if (pesoNuevo.isNotBlank()) filledFields++
            "Colesterol alto" -> if (colesterol.isNotBlank()) filledFields++
        }
        if (calorias.isNotBlank()) filledFields++
        if (animo.isNotBlank()) filledFields++
        if (hizoEjerciciosApp || hizoEjercicioSinApp != null || hizoEjercicioExtra != null) {
            filledFields++
        }

        val requiredFields = when (user.enfermedad) {
            "Hipertensión" -> 4
            else -> 2
        }
        if (filledFields < requiredFields) {
            errors.add("Por favor, complete todos los parámetros requeridos")
        }

        // Validación de ejercicio obligatorio
        if (!hizoEjerciciosApp && hizoEjercicioSinApp == null) {
            errors.add("Por favor, indique si realizó ejercicio fuera de la app")
        }
        if (hizoEjerciciosApp && hizoEjercicioExtra == null) {
            errors.add("Por favor, indique si realizó ejercicio adicional fuera de la app")
        }

        if (user.enfermedad == "Hipertensión") {
            if (presionSistolica.isNotBlank()) {
                val sistolica = presionSistolica.toIntOrNull() ?: 0
                if (sistolica !in 70..200) {
                    errors.add("La presión sistólica debe estar entre 70 y 200 mmHg")
                }
            }
            if (presionDiastolica.isNotBlank()) {
                val diastolica = presionDiastolica.toIntOrNull() ?: 0
                if (diastolica !in 40..130) {
                    errors.add("La presión diastólica debe estar entre 40 y 130 mmHg")
                }
            }
        }
        if (user.enfermedad == "Diabetes" && glucosa.isNotBlank()) {
            val glucosaValue = glucosa.toIntOrNull() ?: 0
            if (glucosaValue !in 50..400) {
                errors.add("La glucosa debe estar entre 50 y 400 mg/dL")
            }
        }
        if (user.enfermedad == "Obesidad" && pesoNuevo.isNotBlank()) {
            val pesoValue = pesoNuevo.toDoubleOrNull() ?: 0.0
            if (pesoValue !in 30.0..250.0) {
                errors.add("El peso debe estar entre 30 y 250 kg")
            }
        }
        if (user.enfermedad == "Obesidad" && pulso.isNotBlank()) {
            val pulsoValue = pulso.toIntOrNull() ?: 0
            if (pulsoValue !in 30..220) {
                errors.add("El pulso debe estar entre 30 y 220 latidos/min")
            }
        }
        if (user.enfermedad == "Colesterol alto" && colesterol.isNotBlank()) {
            val colesterolValue = colesterol.toIntOrNull() ?: 0
            if (colesterolValue !in 100..400) {
                errors.add("El colesterol debe estar entre 100 y 400 mg/dL")
            }
        }
        if (user.enfermedad == "Colesterol alto") {
            if (presionSistolicaColesterol.isNotBlank()) {
                val sistolica = presionSistolicaColesterol.toIntOrNull() ?: 0
                if (sistolica !in 70..200) {
                    errors.add("La presión sistólica debe estar entre 70 y 200 mmHg")
                }
            }
            if (presionDiastolicaColesterol.isNotBlank()) {
                val diastolica = presionDiastolicaColesterol.toIntOrNull() ?: 0
                if (diastolica !in 40..130) {
                    errors.add("La presión diastólica debe estar entre 40 y 130 mmHg")
                }
            }
        }
        if (calorias.isNotBlank()) {
            val caloriasValue = calorias.toIntOrNull() ?: 0
            if (caloriasValue !in 500..5000) {
                errors.add("Las calorías deben estar entre 500 y 5000 kcal")
            }
        }
        if (enableEjercicioInput && (ejercicioHoras.isNotBlank() || ejercicioMinutos.isNotBlank())) {
            val horas = ejercicioHoras.toIntOrNull() ?: 0
            val minutos = ejercicioMinutos.toIntOrNull() ?: 0
            if (horas !in 0..24) {
                errors.add("Las horas de ejercicio deben estar entre 0 y 24")
            }
            if (minutos !in 0..59) {
                errors.add("Los minutos de ejercicio deben estar entre 0 y 59")
            }
            val totalMinutos = horas * 60 + minutos
            if (totalMinutos > 1440) {
                errors.add("El tiempo de ejercicio no puede exceder 24 horas")
            }
        }
        if (enableEjercicioExtraInput && (ejercicioHorasExtra.isNotBlank() || ejercicioMinutosExtra.isNotBlank())) {
            val horasExtra = ejercicioHorasExtra.toIntOrNull() ?: 0
            val minutosExtra = ejercicioMinutosExtra.toIntOrNull() ?: 0
            if (horasExtra !in 0..24) {
                errors.add("Las horas de ejercicio adicional deben estar entre 0 y 24")
            }
            if (minutosExtra !in 0..59) {
                errors.add("Los minutos de ejercicio adicional deben estar entre 0 y 59")
            }
            val totalMinutosExtra = horasExtra * 60 + minutosExtra
            if (totalMinutosExtra > 1440) {
                errors.add("El tiempo de ejercicio adicional no puede exceder 24 horas")
            }
        }
        if (animo.isBlank()) {
            errors.add("El estado de ánimo es obligatorio")
        }

        if (errors.isNotEmpty()) {
            errorMessages = errors
            isSaving = false
            return
        }

        coroutineScope.launch {
            if (user.enfermedad == "Obesidad" && pesoNuevo.isNotBlank()) {
                // ✅ Redondear peso a 1 decimal para evitar 75.1999969482422
                val nuevoPeso = (pesoNuevo.toDoubleOrNull() ?: user.peso).let { 
                    kotlin.math.round(it * 10.0) / 10.0 
                }
                usuarioViewModel.actualizarUsuario(user.copy(peso = nuevoPeso))
            }
            val presionFinal = if (user.enfermedad == "Hipertensión") {
                "${presionSistolica.trim()}/${presionDiastolica.trim()}"
            } else null
            
            val presionFinalColesterol = if (user.enfermedad == "Colesterol alto" && 
                (presionSistolicaColesterol.isNotBlank() || presionDiastolicaColesterol.isNotBlank())) {
                val syst = presionSistolicaColesterol.trim().ifBlank { null }
                val dias = presionDiastolicaColesterol.trim().ifBlank { null }
                if (syst != null && dias != null) "$syst/$dias" else null
            } else null

            val registro = RegistroSalud(
                usuarioId = user.id,
                fecha = currentDate,
                presionArterial = presionFinal,
                presionSistolica = if (user.enfermedad == "Hipertensión") presionSistolica.trim().toIntOrNull() else presionSistolicaColesterol.trim().toIntOrNull(),
                presionDiastolica = if (user.enfermedad == "Hipertensión") presionDiastolica.trim().toIntOrNull() else presionDiastolicaColesterol.trim().toIntOrNull(),
                glucosa = glucosa.trim().toIntOrNull(),
                imc = imc,
                colesterol = colesterol.trim().toIntOrNull(),
                // ✅ Redondear peso a 1 decimal al guardar
                peso = pesoNuevo.trim().toDoubleOrNull()?.let { kotlin.math.round(it * 10.0) / 10.0 },
                pulso = pulso.trim().toIntOrNull(),  // ✅ Pasar pulso al registro
                calorias = calorias.trim().toIntOrNull(),
                ejercicioMinutos = ejercicioTotal,  // Pasar minutos al registro
                estadoAnimo = animo.trim()
            )
            try {
                registroViewModel.agregarRegistro(user, registro)

                navController.previousBackStackEntry
                    ?.savedStateHandle
                    ?.set("registro_guardado", true)

            } catch (e: Exception) {
                navController.previousBackStackEntry
                    ?.savedStateHandle
                    ?.set("registro_error", e.message ?: "Error desconocido")
            }

            // MANTENER isSaving EN TRUE hasta que navegue
            navController.navigate("main") {
                popUpTo(navController.graph.startDestinationId)
                launchSingleTop = true
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Image(
            painter = painterResource(id = R.drawable.background_2),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Overlay más claro para mejorar contraste y legibilidad
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.25f))
        )

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(
                        color = Color(0xFF2196F3),
                        strokeWidth = 4.dp,
                        modifier = Modifier.size(60.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Cargando datos...",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { innerPadding ->
            val adaptiveTopPadding = com.example.doc.utils.getAdaptiveTopPadding()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(innerPadding)
                    .padding(horizontal = 24.dp)
                    .padding(top = adaptiveTopPadding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Card(
                    shape = RoundedCornerShape(32.dp),
                    elevation = CardDefaults.cardElevation(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.95f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Botón de regresar mejorado
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = colorCondicion.copy(alpha = 0.1f)
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
                                        tint = colorCondicion,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Inicio",
                                        color = colorCondicion,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            Text(
                                "Registro de Salud",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = colorCondicion,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )

                            Spacer(modifier = Modifier.width(80.dp))
                        }

                        when (user.enfermedad) {
                            "Hipertensión" -> {
                                Text(
                                    "Presión arterial (ej: 120/80)",
                                    fontWeight = FontWeight.Medium
                                )
                                Row(
                                    Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = presionSistolica,
                                        onValueChange = {
                                            presionSistolica = it.filter { c -> c.isDigit() }
                                        },
                                        label = { Text("Sistólica") },
                                        placeholder = { Text("120", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)) },
                                        modifier = Modifier.weight(1f),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        visualTransformation = SafeIntegerVisualTransformation("000")
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("/", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    OutlinedTextField(
                                        value = presionDiastolica,
                                        onValueChange = {
                                            presionDiastolica = it.filter { c -> c.isDigit() }
                                        },
                                        label = { Text("Diastólica") },
                                        placeholder = { Text("80", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)) },
                                        modifier = Modifier.weight(1f),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        visualTransformation = SafeIntegerVisualTransformation("00")
                                    )
                                }
                            }

                            "Diabetes" -> OutlinedTextField(
                                value = glucosa,
                                onValueChange = { glucosa = it.filter { it.isDigit() } },
                                label = { Text("Glucosa (mg/dL)") },
                                placeholder = { Text("000", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)) },
                                textStyle = LocalTextStyle.current.copy(fontSize = 16.sp),
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                visualTransformation = SafeIntegerVisualTransformation("000")
                            )

                            "Obesidad" -> {
                                OutlinedTextField(
                                    value = pesoNuevo,
                                    onValueChange = {
                                        // ✅ Arreglar precisión decimal: permitir solo un decimal
                                        val filtered = it.filter { c -> c.isDigit() || c == '.' }
                                        pesoNuevo = if (filtered.count { c -> c == '.' } <= 1) {
                                            val parts = filtered.split('.')
                                            if (parts.size == 2 && parts[1].length > 1) {
                                                parts[0] + "." + parts[1].substring(0, 1)
                                            } else {
                                                filtered
                                            }
                                        } else {
                                            filtered.replace(".", "", false).substring(0, filtered.length - 1)
                                        }
                                    },
                                    label = { Text("Peso actual (kg, opcional)") },
                                    placeholder = { Text("75.5", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)) },
                                    textStyle = LocalTextStyle.current.copy(fontSize = 16.sp),
                                    modifier = Modifier.fillMaxWidth(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    visualTransformation = SafeDecimalVisualTransformation("00.0")
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = pulso,
                                    onValueChange = { pulso = it.filter { it.isDigit() } },
                                    label = { Text("Pulso (latidos/min, opcional)") },
                                    placeholder = { Text("70", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)) },
                                    textStyle = LocalTextStyle.current.copy(fontSize = 16.sp),
                                    modifier = Modifier.fillMaxWidth(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    visualTransformation = SafeIntegerVisualTransformation("000")
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                // IMC como parámetro no editable
                                OutlinedTextField(
                                    value = imc?.let {
                                        String.format("%.2f", it)
                                    } ?: "--",
                                    onValueChange = {},  // No permite edición
                                    label = { Text("IMC (calculado)") },
                                    enabled = false,  // Campo deshabilitado (no editable)
                                    textStyle = LocalTextStyle.current.copy(fontSize = 16.sp),
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        disabledTextColor = colorCondicion,
                                        disabledBorderColor = colorCondicion.copy(alpha = 0.5f),
                                        disabledLabelColor = colorCondicion.copy(alpha = 0.7f)
                                    )
                                )
                            }

                            "Colesterol alto" -> {
                                OutlinedTextField(
                                    value = colesterol,
                                    onValueChange = { colesterol = it.filter { it.isDigit() } },
                                    label = { Text("Colesterol (mg/dL)") },
                                    placeholder = { Text("200", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)) },
                                    textStyle = LocalTextStyle.current.copy(fontSize = 16.sp),
                                    modifier = Modifier.fillMaxWidth(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    visualTransformation = SafeIntegerVisualTransformation("000")
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "Presión arterial (opcional)",
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp
                                )
                                Row(
                                    Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = presionSistolicaColesterol,
                                        onValueChange = {
                                            presionSistolicaColesterol = it.filter { c -> c.isDigit() }
                                        },
                                        label = { Text("Sistólica") },
                                        placeholder = { Text("120", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)) },
                                        modifier = Modifier.weight(1f),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        visualTransformation = SafeIntegerVisualTransformation("000")
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("/", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    OutlinedTextField(
                                        value = presionDiastolicaColesterol,
                                        onValueChange = {
                                            presionDiastolicaColesterol = it.filter { c -> c.isDigit() }
                                        },
                                        label = { Text("Diastólica") },
                                        placeholder = { Text("80", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)) },
                                        modifier = Modifier.weight(1f),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        visualTransformation = SafeIntegerVisualTransformation("00")
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = calorias,
                            onValueChange = { calorias = it.filter { it.isDigit() } },
                            label = { Text("Calorías consumidas (ej: 2200)") },
                            placeholder = { Text("0000", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)) },
                            textStyle = LocalTextStyle.current.copy(fontSize = 16.sp),
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            visualTransformation = SafeIntegerVisualTransformation("0000")
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        if (hizoEjerciciosApp) {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(
                                        0xFF43A047
                                    ).copy(alpha = 0.1f)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Filled.Info,
                                        contentDescription = null,
                                        tint = Color(0xFF43A047),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "¡Bien hecho! Has realizado $totalEjercicioApp minutos de ejercicio con la app",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF43A047)
                                    )
                                }
                            }
                            if (hizoEjercicioExtra == null) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    "¿Realizaste ejercicio adicional fuera de la app?",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2196F3)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    Button(
                                        onClick = { hizoEjercicioExtra = true },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF2196F3)
                                        ),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            "Sí",
                                            fontSize = 16.sp,
                                            color = Color.White,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(
                                        onClick = { showConfirmNoExtraDialog = true },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFFC62828)
                                        ),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            "No",
                                            fontSize = 16.sp,
                                            color = Color.White,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                            if (enableEjercicioExtraInput) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    "Tiempo de ejercicio adicional",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2196F3)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = ejercicioHorasExtra,
                                        onValueChange = {
                                            ejercicioHorasExtra = it.filter { c -> c.isDigit() }
                                        },
                                        label = { Text("Horas") },
                                        placeholder = { Text("0", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)) },
                                        modifier = Modifier.weight(1f),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        visualTransformation = SafeIntegerVisualTransformation("0")
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    OutlinedTextField(
                                        value = ejercicioMinutosExtra,
                                        onValueChange = {
                                            ejercicioMinutosExtra =
                                                it.filter { c -> c.isDigit() }
                                        },
                                        label = { Text("Minutos") },
                                        placeholder = { Text("00", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)) },
                                        modifier = Modifier.weight(1f),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        visualTransformation = SafeIntegerVisualTransformation("00")
                                    )
                                }
                            }
                        } else {
                            Text(
                                "Recomendamos hacer ejercicios en la app",
                                fontSize = 16.sp,
                                color = Color(0xFF2196F3),
                                fontWeight = FontWeight.Bold
                            )
                            if (hizoEjercicioSinApp == null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "¿Ha hecho ejercicio sin la app?",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2196F3)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    Button(
                                        onClick = { hizoEjercicioSinApp = true },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF2196F3)
                                        ),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            "Sí",
                                            fontSize = 16.sp,
                                            color = Color.White,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(
                                        onClick = { showConfirmNoEjercicioDialog = true },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFFC62828)
                                        ),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            "No",
                                            fontSize = 16.sp,
                                            color = Color.White,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                            if (enableEjercicioInput) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    "Tiempo de ejercicio",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2196F3)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = ejercicioHoras,
                                        onValueChange = {
                                            ejercicioHoras = it.filter { c -> c.isDigit() }
                                        },
                                        label = { Text("Horas") },
                                        placeholder = { Text("0", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)) },
                                        modifier = Modifier.weight(1f),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        visualTransformation = SafeIntegerVisualTransformation("0")
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    OutlinedTextField(
                                        value = ejercicioMinutos,
                                        onValueChange = {
                                            ejercicioMinutos = it.filter { c -> c.isDigit() }
                                        },
                                        label = { Text("Minutos") },
                                        placeholder = { Text("00", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)) },
                                        modifier = Modifier.weight(1f),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        visualTransformation = SafeIntegerVisualTransformation("00")
                                    )
                                }
                            } else if (hizoEjercicioSinApp == false) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Card(
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = Color(
                                            0xFFD32F2F
                                        ).copy(alpha = 0.1f)
                                    ),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Filled.Info,
                                            contentDescription = null,
                                            tint = Color(0xFFD32F2F),
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            "No se registró actividad física hoy",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color(0xFFD32F2F)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        ExposedDropdownMenuBox(
                            expanded = expandedEstado,
                            onExpandedChange = { expandedEstado = !expandedEstado }
                        ) {
                            OutlinedTextField(
                                value = animo,
                                onValueChange = {},
                                label = { Text("Estado de ánimo") },
                                readOnly = true,
                                modifier = Modifier.menuAnchor()
                            )
                            ExposedDropdownMenu(
                                expanded = expandedEstado,
                                onDismissRequest = { expandedEstado = false }
                            ) {
                                opcionesEstado.forEach { opcion ->
                                    DropdownMenuItem(
                                        text = { Text(opcion) },
                                        onClick = {
                                            animo = opcion
                                            expandedEstado = false
                                        }
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(18.dp))
                        Button(
                            onClick = { intentarGuardarRegistro() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            enabled = hayParametroLlenado && !isSaving,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF2196F3),
                                disabledContainerColor = Color(0xFFBBBBBB)
                            )
                        ) {
                            Text(
                                text = if (isSaving) "Guardando..." else "Guardar registro",
                                fontSize = 18.sp,
                                color = if (isSaving) Color.Gray else Color.White
                            )
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = errorMessages.isNotEmpty(),
            enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 })
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFE0E0)),
                    elevation = CardDefaults.cardElevation(8.dp),
                    modifier = Modifier.fillMaxWidth(0.9f)
                ) {
                    Text(
                        text = errorMessages.firstOrNull() ?: "",
                        color = Color(0xFFD32F2F),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
        LaunchedEffect(errorMessages) {
            if (errorMessages.isNotEmpty()) {
                delay(2000) // visible 2 segundos
                errorMessages = emptyList()
            }
        }

        // Loading overlay para bloquear acciones
        if (isSaving) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(
                        color = Color(0xFF2196F3),
                        strokeWidth = 4.dp,
                        modifier = Modifier.size(60.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Guardando registro...",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

