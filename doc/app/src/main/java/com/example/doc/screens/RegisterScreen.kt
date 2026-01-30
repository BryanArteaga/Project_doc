package com.example.doc.screens

import android.app.DatePickerDialog
import android.widget.DatePicker
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.doc.R
import com.example.doc.navigation.Routes
import com.example.doc.utils.SafeDecimalVisualTransformation
import com.example.doc.utils.rememberDebouncer
import com.example.doc.viewmodel.UsuarioViewModel
import kotlinx.coroutines.delay
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(navController: NavController, usuarioViewModel: UsuarioViewModel) {

    val context = LocalContext.current
    var cedula by rememberSaveable { mutableStateOf("") }
    var nombre by rememberSaveable { mutableStateOf("") }
    var fechaNacimiento by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(""))
    }
    var peso by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(""))
    }
    var altura by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(""))
    }
    var sexo by remember { mutableStateOf("") }
    var errorMessages by remember { mutableStateOf<List<String>>(emptyList()) }
    var showConsentDialog by remember { mutableStateOf(true) }
    val snackbarHostState = remember { SnackbarHostState() }
    val colorCondicion = Color(0xFF2196F3)
    val scrollState = rememberScrollState()

    // Debouncer para validaciones en tiempo real - Optimización de rendimiento
    val validationDebouncer = rememberDebouncer(delayMillis = 500L)

    // Estado para validaciones en tiempo real (opcional, no bloquea el envío)
    var cedulaError by remember { mutableStateOf<String?>(null) }
    var pesoError by remember { mutableStateOf<String?>(null) }
    var alturaError by remember { mutableStateOf<String?>(null) }

    val calendar = Calendar.getInstance()
    val currentYear = calendar.get(Calendar.YEAR) // Año actual dinámico
    val datePickerDialog = DatePickerDialog(
        context,
        { _: DatePicker, year: Int, month: Int, day: Int ->
            fechaNacimiento = TextFieldValue("%02d/%02d/%04d".format(day, month + 1, year))
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    fun calcularEdad(fechaStr: String): Int {
        return try {
            val parts = fechaStr.split("/", "-")
            if (parts.size != 3) return -1
            val day = parts[0].toIntOrNull() ?: return -1
            val month = parts[1].toIntOrNull() ?: return -1
            val year = parts[2].toIntOrNull() ?: return -1

            // Validar rangos
            if (day !in 1..31) return -1
            if (month !in 1..12) return -1
            if (year !in 1900..currentYear) return -1

            // Validar días por mes
            val daysInMonth = when (month) {
                4, 6, 9, 11 -> 30
                2 -> if (year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)) 29 else 28
                else -> 31
            }
            if (day > daysInMonth) return -1

            val birthDate = Calendar.getInstance()
            birthDate.set(year, month - 1, day)
            val today = Calendar.getInstance()
            var edad = today.get(Calendar.YEAR) - birthDate.get(Calendar.YEAR)
            if (today.get(Calendar.DAY_OF_YEAR) < birthDate.get(Calendar.DAY_OF_YEAR)) edad--
            if (edad !in 8..100) return -1
            edad
        } catch (e: Exception) {
            -1
        }
    }

    fun safeDouble(str: String): Double {
        // Reemplazar coma por punto para soportar ambos formatos
        val normalized = str.replace(',', '.')
        return normalized.toDoubleOrNull() ?: 0.0
    }

    fun validateDateFormat(input: TextFieldValue): TextFieldValue {
        val cleanInput = input.text.filter { it.isDigit() || it == '/' || it == '-' }
        val digits = cleanInput.filter { it.isDigit() }.take(8)
        var formatted = ""
        var cursor = input.selection.start

        when (digits.length) {
            0 -> formatted = ""
            in 1..2 -> {
                formatted = digits
                cursor = formatted.length
            }
            in 3..4 -> {
                val day = digits.take(2).toIntOrNull() ?: 0
                if (day in 1..31) {
                    formatted = "${digits.take(2)}/${digits.substring(2)}"
                    if (input.text.length > 2 && digits.length > 2 && !cleanInput.contains("/")) cursor++
                } else {
                    formatted = digits.take(2)
                    cursor = formatted.length
                }
            }
            in 5..8 -> {
                val day = digits.take(2).toIntOrNull() ?: 0
                val month = digits.substring(2, 4).toIntOrNull() ?: 0
                val year = digits.substring(4).toIntOrNull() ?: 0
                if (day in 1..31 && month in 1..12) {
                    formatted = "${digits.take(2)}/${digits.substring(2, 4)}/${digits.substring(4)}"
                    if (input.text.length > 2 && digits.length > 2 && !cleanInput.contains("/")) cursor++
                    if (input.text.length > 5 && digits.length > 4 && cleanInput.count { it == '/' || it == '-' } < 2) cursor++
                } else {
                    formatted = "${digits.take(2)}/${digits.substring(2, 4)}"
                    cursor = formatted.length
                }
            }
        }

        return TextFieldValue(formatted, TextRange(cursor.coerceIn(0, formatted.length)))
    }

    fun validatePesoFormat(input: TextFieldValue): TextFieldValue {
        val digits = input.text.filter { it.isDigit() }.take(4)
        var formatted = ""
        var cursor = input.selection.start

        when (digits.length) {
            0 -> formatted = ""
            1 -> formatted = digits
            2 -> {
                formatted = "${digits[0]}.${digits[1]}"
                if (input.text.length >= 2 && !input.text.contains(".")) cursor++
            }
            in 3..4 -> {
                val integerPart = digits.substring(0, digits.length - 1)
                val decimalPart = digits.takeLast(1)
                formatted = "$integerPart.$decimalPart"
                if (input.text.length >= digits.length && !input.text.contains(".")) cursor++
            }
        }

        return TextFieldValue(formatted, TextRange(cursor.coerceIn(0, formatted.length)))
    }

    fun validateAlturaFormat(input: TextFieldValue): TextFieldValue {
        val cleanInput = input.text.filter { it.isDigit() || it == '.' }.take(5)
        var formatted = ""
        var cursor = input.selection.start

        // Si la entrada son solo dígitos (por ejemplo, "176"), asumir metros con dos decimales
        if (!cleanInput.contains(".")) {
            val number = cleanInput.toDoubleOrNull()
            if (number != null && number in 100.0..250.0) {
                val meters = number / 100.0
                formatted = String.format(java.util.Locale.US, "%.2f", meters)
                cursor = formatted.length
            } else {
                formatted = cleanInput
                cursor = cleanInput.length
            }
        } else {
            // Si la entrada incluye un punto (por ejemplo, "1.76"), validar como metros
            val number = cleanInput.toDoubleOrNull()
            if (number != null && number in 1.0..2.5) {
                formatted = String.format(java.util.Locale.US, "%.2f", number)
                if (!input.text.contains(".") && cleanInput.length >= 2) cursor = formatted.indexOf(".") + 1
            } else {
                formatted = cleanInput
                cursor = cleanInput.length
            }
        }

        return TextFieldValue(formatted, TextRange(cursor.coerceIn(0, formatted.length)))
    }

    fun safeAltura(str: String): Double {
        // Reemplazar coma por punto para soportar ambos formatos
        val normalized = str.replace(',', '.')
        return (normalized.toDoubleOrNull() ?: 0.0).let {
            // Usar Locale.US para asegurar formato con punto decimal
            String.format(java.util.Locale.US, "%.2f", it).toDouble()
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(24.dp),
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
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(
                            onClick = { navController.popBackStack() },
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .size(36.dp)
                        ) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = "Regresar")
                        }

                        Text(
                            text = "Registro de usuario",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                color = Color(0xFF2196F3)
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(18.dp))
                    OutlinedTextField(
                        value = nombre,
                        onValueChange = {
                            nombre = it
                        },
                        label = { Text("Nombre*") },
                        placeholder = { Text("Juan Perez") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = cedula,
                        onValueChange = { newValue ->
                            cedula = newValue
                            // Validación con debounce para no ejecutar en cada tecla
                            validationDebouncer.debounce {
                                cedulaError = if (newValue.isNotEmpty() && newValue.length != 10) {
                                    "La cédula debe tener 10 dígitos"
                                } else if (newValue.isNotEmpty() && !newValue.all { it.isDigit() }) {
                                    "La cédula solo debe contener números"
                                } else {
                                    null
                                }
                            }
                        },
                        label = { Text("Cédula*") },
                        placeholder = { Text("1363846284") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = cedulaError != null,
                        supportingText = cedulaError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = fechaNacimiento,
                        onValueChange = { newValue ->
                            fechaNacimiento = validateDateFormat(newValue)
                        },
                        label = { Text("Fecha de nacimiento (dd/MM/yyyy)*") },
                        placeholder = { Text("dd/MM/yyyy") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        trailingIcon = {
                            IconButton(onClick = { datePickerDialog.show() }) {
                                Icon(Icons.Filled.CalendarToday, contentDescription = "DatePicker")
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = peso,
                        onValueChange = { newValue ->
                            peso = validatePesoFormat(newValue)
                            // Validación con debounce
                            validationDebouncer.debounce {
                                val pesoValue = peso.text.toDoubleOrNull()
                                pesoError = if (pesoValue != null && (pesoValue < 1.0 || pesoValue > 500.0)) {
                                    "El peso debe estar entre 1 y 500 kg"
                                } else {
                                    null
                                }
                            }
                        },
                        label = { Text("Peso (kg)*") },
                        placeholder = { Text("00.0") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        isError = pesoError != null,
                        supportingText = pesoError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = altura,
                        onValueChange = { newValue ->
                            altura = validateAlturaFormat(newValue)
                            // Validación con debounce
                            validationDebouncer.debounce {
                                val alturaValue = safeAltura(altura.text)
                                alturaError = if (alturaValue < 1.0 || alturaValue > 2.5) {
                                    "La altura debe estar entre 1.00 y 2.50 metros"
                                } else {
                                    null
                                }
                            }
                        },
                        label = { Text("Altura (cm)*") },
                        placeholder = { Text("1.65") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        isError = alturaError != null,
                        supportingText = alturaError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    var expanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded }
                    ) {
                        OutlinedTextField(
                            value = sexo,
                            onValueChange = { },
                            label = { Text("Sexo*") },
                            placeholder = { Text("Selecciona") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            readOnly = true,
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                            }
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Hombre") },
                                onClick = {
                                    sexo = "Hombre"
                                    expanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Mujer") },
                                onClick = {
                                    sexo = "Mujer"
                                    expanded = false
                                }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(18.dp))
                    Button(
                        onClick = {
                            errorMessages = emptyList()
                            val errors = mutableListOf<String>()

                            val emptyFields = mutableListOf<String>()
                            if (nombre.isBlank()) emptyFields.add("Nombre")
                            if (cedula.isBlank()) emptyFields.add("Cédula")
                            if (fechaNacimiento.text.isBlank()) emptyFields.add("Fecha de nacimiento")
                            if (peso.text.isBlank()) emptyFields.add("Peso")
                            if (altura.text.isBlank()) emptyFields.add("Altura")
                            if (sexo.isBlank()) emptyFields.add("Sexo")

                            // Si hay campos vacíos, mostrar mensaje genérico
                            if (emptyFields.isNotEmpty()) {
                                errors.add("Por favor, complete todos los parámetros requeridos")
                            }

                            // Validaciones de datos realistas
                            if (nombre.isNotBlank()) {
                                if (nombre.length !in 2..40 || !nombre.all { it.isLetter() || it.isWhitespace() }) {
                                    errors.add("El nombre debe contener solo letras")
                                }
                            }

                            //validacion numero de cedula
                            if (cedula.isNotBlank() && (cedula.length != 10 || !cedula.all { it.isDigit() })) {
                                errors.add("La cédula debe tener 10 dígitos numéricos")
                            }

                            if (fechaNacimiento.text.isNotBlank()) {
                                val parts = fechaNacimiento.text.split("/", "-")
                                if (parts.size != 3) {
                                    errors.add("La fecha de nacimiento debe tener el formato dd/MM/yyyy")
                                } else {
                                    val day = parts[0].toIntOrNull() ?: 0
                                    val month = parts[1].toIntOrNull() ?: 0
                                    val year = parts[2].toIntOrNull() ?: 0
                                    val daysInMonth = when (month) {
                                        4, 6, 9, 11 -> 30
                                        2 -> if (year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)) 29 else 28
                                        else -> 31
                                    }
                                    if (day !in 1..daysInMonth || month !in 1..12 || year !in 1900..currentYear) {
                                        errors.add("La fecha de nacimiento no es válida: día (1-$daysInMonth), mes (1-12), año (1900-$currentYear)")
                                    } else {
                                        val edad = calcularEdad(fechaNacimiento.text)
                                        if (edad !in 8..100) {
                                            errors.add("Fecha Invalidad")
                                        }
                                    }
                                }
                            }
                            if (peso.text.isNotBlank()) {
                                val pesoValue = safeDouble(peso.text)
                                if (pesoValue !in 30.0..250.0) {
                                    errors.add("El peso Invalido")
                                }
                            }
                            if (altura.text.isNotBlank()) {
                                val alturaValue = safeAltura(altura.text)
                                if (alturaValue !in 1.0..2.5) {
                                    errors.add("La altura Invalida")
                                }
                            }
                            if (sexo.isNotBlank()) {
                                if (sexo !in listOf("Hombre", "Mujer")) {
                                    errors.add("Seleccione un sexo válido")
                                }
                            }

                            if (errors.isNotEmpty()) {
                                errorMessages = errors
                                return@Button
                            }

                            navController.navigate(
                                "${Routes.Condition}?cedula=${cedula.trim()}&name=${nombre.trim()}&edad=${calcularEdad(fechaNacimiento.text)}&peso=${safeDouble(peso.text)}&altura=${safeAltura(altura.text)}&sexo=$sexo"
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = colorCondicion)
                    ) {
                        Text("Siguiente", style = MaterialTheme.typography.bodyLarge, color = Color.White)
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

        if (showConsentDialog) {
            AlertDialog(
                onDismissRequest = { },
                title = { Text("Consentimiento de datos") },
                text = { Text("Al registrarte, aceptas el uso de tus datos " +
                        "personales conforme a la Ley Orgánica " +
                        "de Protección de Datos Personales.") },
                confirmButton = {
                    Button(onClick = { showConsentDialog = false }) {
                        Text("Aceptar")
                    }
                }
            )
        }
        LaunchedEffect(errorMessages) {
            if (errorMessages.isNotEmpty()) {
                delay(2000) // visible 2 segundos
                errorMessages = emptyList()
            }
        }
    }
}