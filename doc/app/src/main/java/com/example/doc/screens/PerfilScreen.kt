package com.example.doc.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.doc.viewmodel.UsuarioViewModel
import com.example.doc.R
import com.example.doc.utils.isLandscape
import kotlinx.coroutines.delay
import kotlinx.coroutines.time.delay
import org.json.JSONObject
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerfilScreen(
    navController: NavController,
    usuarioViewModel: UsuarioViewModel
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val usuario by usuarioViewModel.usuario.collectAsState()
    var isEditing by remember { mutableStateOf(false) }
    var nombre by rememberSaveable { mutableStateOf(usuario?.nombre ?: "") }
    var apellido by rememberSaveable { mutableStateOf(usuario?.apellido ?: "") }  // Nuevo: apellido
    var cedula by rememberSaveable { mutableStateOf(usuario?.cedula ?: "") }
    var edad by rememberSaveable { mutableStateOf(usuario?.edad?.toString() ?: "") }
    var peso by rememberSaveable { mutableStateOf(usuario?.peso?.toString() ?: "") }
    var altura by rememberSaveable {
        mutableStateOf(usuario?.altura?.let { String.format(java.util.Locale.US, "%.2f", it) } ?: "")
    }

    val isLandscape = isLandscape()

    var notifEnabled by remember { mutableStateOf(usuario?.notifEnabled ?: true) }
    var notifType by remember { mutableStateOf(usuario?.notifType ?: "daily_fixed") }
    var hour by remember { mutableStateOf(JSONObject(usuario?.notifDetails ?: "{\"hour\":20,\"minute\":0}").optInt("hour", 20)) }
    var minute by remember { mutableStateOf(JSONObject(usuario?.notifDetails ?: "{\"hour\":20,\"minute\":0}").optInt("minute", 0)) }
    var dayOfWeek by remember { mutableStateOf(JSONObject(usuario?.notifDetails ?: "{\"dayOfWeek\":2}").optInt("dayOfWeek", 2)) } // 2 = Monday
    var dayOfMonth by remember { mutableStateOf(JSONObject(usuario?.notifDetails ?: "{\"dayOfMonth\":1}").optInt("dayOfMonth", 1)) }

    // Actualizar estados cuando cambia el usuario
    LaunchedEffect(usuario) {
        usuario?.let { user ->
            notifEnabled = user.notifEnabled
            notifType = user.notifType
            val json = JSONObject(user.notifDetails)
            hour = json.optInt("hour", 20)
            minute = json.optInt("minute", 0)
            dayOfWeek = json.optInt("dayOfWeek", 2)
            dayOfMonth = json.optInt("dayOfMonth", 1)
        }
    }

    var expanded by remember { mutableStateOf(false) }
    var weekExpanded by remember { mutableStateOf(false) }
    var errorMessages by remember { mutableStateOf<List<String>>(emptyList()) }
    var showConfirmationDialog by remember { mutableStateOf(false) }
    var confirmationMessage by remember { mutableStateOf("") }
    
    // 🔍 OPCIÓN B: Estados para confirmación de cambios en datos personales
    var showPersonalDataConfirmation by remember { mutableStateOf(false) }
    var pendingUpdatedUser by remember { mutableStateOf<com.example.doc.model.Usuario?>(null) }
    var oldPersonalData by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var newPersonalData by remember { mutableStateOf<Map<String, String>>(emptyMap()) }

    val notifOptions = listOf(
        "Diaria (hora fija)" to "daily_fixed",
        "Semanal (día fijo)" to "weekly_fixed",
        "Mensual (día fijo)" to "monthly_fixed"
    )

    val daysOfWeek = listOf(
        "Lunes" to 2, "Martes" to 3, "Miércoles" to 4, "Jueves" to 5,
        "Viernes" to 6, "Sábado" to 7, "Domingo" to 1
    )

    if (showConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmationDialog = false },
            title = { Text("Configuración Guardada") },
            text = { Text(confirmationMessage) },
            confirmButton = {
                TextButton(onClick = { showConfirmationDialog = false }) {
                    Text("Aceptar")
                }
            }
        )
    }

    // 🔍 OPCIÓN B: AlertDialog para confirmar cambios en datos personales
    if (showPersonalDataConfirmation && pendingUpdatedUser != null) {
        AlertDialog(
            onDismissRequest = { 
                showPersonalDataConfirmation = false
                pendingUpdatedUser = null
            },
            title = { Text("¿Cambiar datos personales?") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        "Está a punto de cambiar sus datos personales:",
                        modifier = Modifier.fillMaxWidth(),
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Mostrar cambios
                    oldPersonalData.forEach { (field, oldValue) ->
                        val newValue = newPersonalData[field] ?: oldValue
                        if (oldValue != newValue) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    "$field:",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    color = Color(0xFF666666)
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "\"$oldValue\"",
                                        fontSize = 12.sp,
                                        color = Color.Red,
                                        modifier = Modifier.weight(0.5f)
                                    )
                                    Text(" → ", fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.weight(0.2f))
                                    Text(
                                        "\"$newValue\"",
                                        fontSize = 12.sp,
                                        color = Color(0xFF2196F3),
                                        modifier = Modifier.weight(0.5f),
                                        textAlign = TextAlign.End
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                    
                    Text(
                        "Esta acción no se puede deshacer.",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        fontSize = 12.sp,
                        color = Color(0xFFFF6B00),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingUpdatedUser?.let { updatedUser ->
                            usuarioViewModel.actualizarUsuario(updatedUser)
                            usuarioViewModel.sincronizarManualmente()
                            isEditing = false
                        }
                        showPersonalDataConfirmation = false
                        pendingUpdatedUser = null
                    }
                ) {
                    Text("Sí, Confirmar", color = Color(0xFF2196F3), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showPersonalDataConfirmation = false
                        pendingUpdatedUser = null
                    }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }


    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.background_1),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        val adaptiveTopPadding = com.example.doc.utils.getAdaptiveTopPadding()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(if (isLandscape) 16.dp else 24.dp)
        ) {
            Spacer(modifier = Modifier.height(adaptiveTopPadding))

            // Header mejorado con botones más visibles
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Botón de regresar mejorado
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF2196F3).copy(alpha = 0.1f)
                    ),
                    modifier = Modifier.clickable { navController.popBackStack() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = Color(0xFF2196F3),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Inicio",
                            color = Color(0xFF2196F3),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Título centrado
                Text(
                    text = if (isEditing) "Editar Perfil" else "Mi Perfil",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2196F3)
                )

                // Botón de editar mejorado
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isEditing)
                            Color(0xFF43A047).copy(alpha = 0.15f)
                        else
                            Color(0xFF2196F3).copy(alpha = 0.1f)
                    ),
                    modifier = Modifier.clickable { isEditing = !isEditing }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (isEditing) Icons.Filled.Check else Icons.Filled.Edit,
                            contentDescription = if (isEditing) "Guardar" else "Editar",
                            tint = if (isEditing) Color(0xFF43A047) else Color(0xFF2196F3),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isEditing) "Listo" else "Editar",
                            color = if (isEditing) Color(0xFF43A047) else Color(0xFF2196F3),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
            Card(
                shape = RoundedCornerShape(if (isLandscape) 24.dp else 32.dp),
                elevation = CardDefaults.cardElevation(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.95f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (isLandscape) Modifier.widthIn(max = 600.dp)
                        else Modifier
                    )
            ) {
                if (isEditing) {
                    Column(
                        modifier = Modifier.padding(if (isLandscape) 20.dp else 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Editar datos personales",
                            fontSize = if (isLandscape) 20.sp else 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2196F3),
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(if (isLandscape) 14.dp else 18.dp))

                        if (isLandscape) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    OutlinedTextField(
                                        value = nombre,
                                        onValueChange = { nombre = it },
                                        label = { Text("Nombre") },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedTextField(
                                        value = apellido,
                                        onValueChange = { apellido = it },
                                        label = { Text("Apellido") },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    OutlinedTextField(
                                        value = cedula,
                                        onValueChange = {},  // 🔒 CEDULA READ-ONLY: No permitir cambios
                                        label = { Text("Cédula") },
                                        modifier = Modifier.fillMaxWidth(),
                                        readOnly = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            disabledBorderColor = Color(0xFFBDBDBD),
                                            disabledTextColor = Color(0xFF757575)
                                        )
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    OutlinedTextField(
                                        value = peso,
                                        onValueChange = { peso = it },
                                        label = { Text("Peso (kg)") },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    OutlinedTextField(
                                        value = edad,
                                        onValueChange = { edad = it.filter { it.isDigit() } },
                                        label = { Text("Edad") },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedTextField(
                                        value = altura,
                                        onValueChange = { altura = it },
                                        label = { Text("Altura (cm)") },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        } else {
                            OutlinedTextField(
                                value = nombre,
                                onValueChange = { nombre = it },
                                label = { Text("Nombre") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(
                                value = apellido,
                                onValueChange = { apellido = it },
                                label = { Text("Apellido") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(
                                value = cedula,
                                onValueChange = {},  // 🔒 CEDULA READ-ONLY: No permitir cambios
                                label = { Text("Cédula") },
                                modifier = Modifier.fillMaxWidth(),
                                readOnly = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    disabledBorderColor = Color(0xFFBDBDBD),
                                    disabledTextColor = Color(0xFF757575)
                                )
                            )
                            OutlinedTextField(
                                value = edad,
                                onValueChange = { edad = it.filter { it.isDigit() } },
                                label = { Text("Edad") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = peso,
                                onValueChange = { peso = it },
                                label = { Text("Peso (kg)") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = altura,
                                onValueChange = { altura = it },
                                label = { Text("Altura (cm)") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Button(
                                onClick = {
                                    isEditing = false
                                    nombre = usuario?.nombre ?: ""
                                    apellido = usuario?.apellido ?: ""
                                    cedula = usuario?.cedula ?: ""
                                    edad = usuario?.edad?.toString() ?: ""
                                    peso = usuario?.peso?.toString() ?: ""
                                    altura = usuario?.altura?.let { String.format(java.util.Locale.US, "%.2f", it) } ?: ""
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(if (isLandscape) 48.dp else 54.dp)
                                    .padding(end = 8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE0E0E0))
                            ) {
                                Text("Cancelar", fontSize = 17.sp, color = Color.DarkGray, fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = {
                                    errorMessages = emptyList()
                                    val errors = mutableListOf<String>()

                                    // Validaciones
                                    if (nombre.isBlank()) errors.add("El nombre no puede estar vacío")
                                    if (nombre.length !in 2..40 || !nombre.all { it.isLetter() || it.isWhitespace() }) {
                                        errors.add("El nombre debe contener solo letras")
                                    }

                                    // 🔒 CEDULA NO SE VALIDA - Es READ-ONLY
                                    
                                    if (edad.toIntOrNull() !in 8..100) {
                                        errors.add("La edad invalida")
                                    }
                                    peso.toDoubleOrNull()?.let {
                                        if (it !in 30.0..250.0) {
                                            errors.add("El peso Invalido")
                                        }
                                    }
                                    val alturaValue = altura.toDoubleOrNull()?.let {
                                        String.format(java.util.Locale.US, "%.2f", it).toDouble()
                                    }
                                    if (alturaValue == null || alturaValue !in 1.0..2.5) {
                                        errors.add("La altura Invalida")
                                    }

                                    if (errors.isNotEmpty()) {
                                        errorMessages = errors
                                        return@Button
                                    }
                                    val updatedUser = usuario!!.copy(
                                        nombre = nombre,
                                        apellido = apellido.trim(),
                                        edad = edad.toIntOrNull() ?: usuario!!.edad,
                                        peso = peso.toDoubleOrNull() ?: usuario!!.peso,
                                        altura = altura.toDoubleOrNull() ?: usuario!!.altura
                                    )
                                    
                                    // 🔍 OPCIÓN B: Verificar si hay cambios en datos personales
                                    val hasPersonalDataChanges = usuario!!.nombre != nombre.trim() ||
                                                                usuario!!.apellido.trim() != apellido.trim() ||
                                                                usuario!!.edad.toString() != edad ||
                                                                usuario!!.peso.toString() != peso ||
                                                                usuario!!.altura.toString() != altura
                                    
                                    if (hasPersonalDataChanges) {
                                        // Guardar datos antiguos y nuevos para mostrar en el diálogo
                                        oldPersonalData = mapOf(
                                            "Nombre" to (usuario!!.nombre ?: ""),
                                            "Apellido" to (usuario!!.apellido ?: ""),
                                            "Edad" to usuario!!.edad.toString(),
                                            "Peso" to usuario!!.peso.toString(),
                                            "Altura" to usuario!!.altura.toString()
                                        )
                                        newPersonalData = mapOf(
                                            "Nombre" to nombre.trim(),
                                            "Apellido" to apellido.trim(),
                                            "Edad" to edad,
                                            "Peso" to peso,
                                            "Altura" to altura
                                        )
                                        pendingUpdatedUser = updatedUser
                                        showPersonalDataConfirmation = true
                                    } else {
                                        // Sin cambios en datos personales, guardar directamente
                                        usuarioViewModel.actualizarUsuario(updatedUser)
                                        usuarioViewModel.sincronizarManualmente()
                                        isEditing = false
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(if (isLandscape) 48.dp else 54.dp)
                                    .padding(start = 8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))
                            ) {
                                Text("Guardar", fontSize = 17.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                            LaunchedEffect(errorMessages) {
                                if (errorMessages.isNotEmpty()) {
                                    delay(2000)
                                    errorMessages = emptyList()
                                }
                            }
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier.padding(if (isLandscape) 20.dp else 24.dp)
                    ) {
                        Text(
                            "Datos Personales",
                            fontSize = if (isLandscape) 20.sp else 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2196F3),
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(if (isLandscape) 14.dp else 18.dp))
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Text("Nombre: ${usuario?.nombre}", fontSize = 17.sp, fontWeight = FontWeight.Medium)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Apellido: ${usuario?.apellido ?: "No especificado"}", fontSize = 17.sp, fontWeight = FontWeight.Medium)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Cédula: ${usuario?.cedula}", fontSize = 17.sp, fontWeight = FontWeight.Medium)
                            Text("Edad: ${usuario?.edad}", fontSize = 17.sp, fontWeight = FontWeight.Medium)
                            Text("Peso: ${usuario?.peso} kg", fontSize = 17.sp, fontWeight = FontWeight.Medium)
                            Text(
                                "Altura: ${usuario?.altura?.let { String.format(java.util.Locale.US, "%.2f", it) } ?: "0.00"} m",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            // Card de notificaciones
            Card(
                shape = RoundedCornerShape(if (isLandscape) 24.dp else 32.dp),
                elevation = CardDefaults.cardElevation(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.95f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (isLandscape) Modifier.widthIn(max = 600.dp) else Modifier)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0xFFF5F6F5), Color(0xFFE8ECEF))
                        ),
                        shape = RoundedCornerShape(if (isLandscape) 24.dp else 32.dp)
                    )
            ) {
                Column(
                    modifier = Modifier
                        .padding(if (isLandscape) 20.dp else 24.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        "Configuración de Notificaciones",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2196F3),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Habilitar notificaciones",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = notifEnabled,
                            onCheckedChange = { notifEnabled = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFF2196F3),
                                checkedTrackColor = Color(0xFFBBDEFB)
                            )
                        )
                    }
                    if (notifEnabled) {
                        Spacer(modifier = Modifier.height(12.dp))
                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = { expanded = !expanded }
                        ) {
                            OutlinedTextField(
                                value = notifOptions.find { it.second == notifType }?.first ?: "Seleccionar",
                                onValueChange = {},
                                label = { Text("Frecuencia") },
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White,
                                    focusedIndicatorColor = Color(0xFF2196F3),
                                    focusedLabelColor = Color(0xFF2196F3)
                                )
                            )
                            ExposedDropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                notifOptions.forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(option.first, style = MaterialTheme.typography.bodyLarge) },
                                        onClick = {
                                            notifType = option.second
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        when (notifType) {
                            "daily_fixed" -> {
                                Button(
                                    onClick = {
                                        val calendar = Calendar.getInstance().apply {
                                            set(Calendar.HOUR_OF_DAY, hour)
                                            set(Calendar.MINUTE, minute)
                                        }
                                        TimePickerDialog(
                                            context,
                                            { _, h, m ->
                                                hour = h
                                                minute = m
                                            },
                                            calendar.get(Calendar.HOUR_OF_DAY),
                                            calendar.get(Calendar.MINUTE),
                                            true
                                        ).show()
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            Icons.Filled.AccessTime,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            "Hora: ${String.format("%02d:%02d", hour, minute)}",
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = Color.White,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                            "weekly_fixed" -> {
                                ExposedDropdownMenuBox(
                                    expanded = weekExpanded,
                                    onExpandedChange = { weekExpanded = !weekExpanded }
                                ) {
                                    OutlinedTextField(
                                        value = daysOfWeek.find { it.second == dayOfWeek }?.first ?: "Seleccionar",
                                        onValueChange = {},
                                        label = { Text("Día de la semana") },
                                        readOnly = true,
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = weekExpanded) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .menuAnchor(),
                                        colors = TextFieldDefaults.colors(
                                            focusedContainerColor = Color.White,
                                            unfocusedContainerColor = Color.White,
                                            focusedIndicatorColor = Color(0xFF2196F3),
                                            focusedLabelColor = Color(0xFF2196F3)
                                        )
                                    )
                                    ExposedDropdownMenu(
                                        expanded = weekExpanded,
                                        onDismissRequest = { weekExpanded = false }
                                    ) {
                                        daysOfWeek.forEach { day ->
                                            DropdownMenuItem(
                                                text = { Text(day.first, style = MaterialTheme.typography.bodyLarge) },
                                                onClick = {
                                                    dayOfWeek = day.second
                                                    weekExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = {
                                        val calendar = Calendar.getInstance().apply {
                                            set(Calendar.HOUR_OF_DAY, hour)
                                            set(Calendar.MINUTE, minute)
                                        }
                                        TimePickerDialog(
                                            context,
                                            { _, h, m ->
                                                hour = h
                                                minute = m
                                            },
                                            calendar.get(Calendar.HOUR_OF_DAY),
                                            calendar.get(Calendar.MINUTE),
                                            true
                                        ).show()
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            Icons.Filled.AccessTime,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            "Hora: ${String.format("%02d:%02d", hour, minute)}",
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = Color.White,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                            "monthly_fixed" -> {
                                Button(
                                    onClick = {
                                        val calendar = Calendar.getInstance().apply {
                                            set(Calendar.DAY_OF_MONTH, dayOfMonth)
                                        }
                                        DatePickerDialog(
                                            context,
                                            { _, _, _, day ->
                                                dayOfMonth = day
                                            },
                                            calendar.get(Calendar.YEAR),
                                            calendar.get(Calendar.MONTH),
                                            calendar.get(Calendar.DAY_OF_MONTH)
                                        ).show()
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            Icons.Filled.CalendarToday,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            "Día del mes: $dayOfMonth",
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = Color.White,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = {
                                        val calendar = Calendar.getInstance().apply {
                                            set(Calendar.HOUR_OF_DAY, hour)
                                            set(Calendar.MINUTE, minute)
                                        }
                                        TimePickerDialog(
                                            context,
                                            { _, h, m ->
                                                hour = h
                                                minute = m
                                            },
                                            calendar.get(Calendar.HOUR_OF_DAY),
                                            calendar.get(Calendar.MINUTE),
                                            true
                                        ).show()
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            Icons.Filled.AccessTime,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            "Hora: ${String.format("%02d:%02d", hour, minute)}",
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = Color.White,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }
                    errorMessages.forEach { error ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            error,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            errorMessages = emptyList()
                            val errors = mutableListOf<String>()
                            if (notifEnabled) {
                                if (hour !in 0..23 || minute !in 0..59) errors.add("Hora inválida")
                                if (notifType == "weekly_fixed" && dayOfWeek !in 1..7) errors.add("Día de semana inválido")
                                if (notifType == "monthly_fixed" && dayOfMonth !in 1..31) errors.add("Día del mes inválido")
                            }
                            if (errors.isNotEmpty()) {
                                errorMessages = errors
                                return@Button
                            }
                            val details = JSONObject().apply {
                                put("hour", hour)
                                put("minute", minute)
                                if (notifType == "weekly_fixed") put("dayOfWeek", dayOfWeek)
                                if (notifType == "monthly_fixed") put("dayOfMonth", dayOfMonth)
                            }.toString()
                            usuarioViewModel.updateNotifications(notifEnabled, notifType, details)
                            confirmationMessage = when {
                                !notifEnabled -> "Notificaciones desactivadas con éxito."
                                notifType == "daily_fixed" -> "Notificaciones activadas: Diaria a las ${String.format("%02d:%02d", hour, minute)}."
                                notifType == "weekly_fixed" -> "Notificaciones activadas: Cada ${daysOfWeek.find { it.second == dayOfWeek }?.first} a las ${String.format("%02d:%02d", hour, minute)}."
                                notifType == "monthly_fixed" -> "Notificaciones activadas: Cada día $dayOfMonth del mes a las ${String.format("%02d:%02d", hour, minute)}."
                                else -> "Configuración de notificaciones guardada con éxito."
                            }
                            showConfirmationDialog = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(if (isLandscape) 48.dp else 56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            "Guardar",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
        AnimatedVisibility(
            visible = errorMessages.isNotEmpty(),
            enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 }),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(16.dp)
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
}