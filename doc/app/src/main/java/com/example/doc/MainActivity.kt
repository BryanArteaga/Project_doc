package com.example.doc

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.example.doc.repository.UsuarioRepository
import com.example.doc.ui.theme.DocTheme
import com.example.doc.navigation.AppNavigation
import com.example.doc.navigation.Routes
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var usuarioRepository: UsuarioRepository

    @Inject
    lateinit var prefs: SharedPreferences

    // Launcher para solicitar permiso de ignorar optimización de batería
    private val requestBatteryOptimizationLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        checkBatteryOptimization()
    }

    // Launcher para solicitar permiso de alarmas exactas
    private val requestExactAlarmLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        checkExactAlarmPermission()
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        val splashScreen = installSplashScreen()
        var isDataLoaded by mutableStateOf(false)
        var isUiReady by mutableStateOf(false)
        var startDestination by mutableStateOf(Routes.Welcome)
        splashScreen.setKeepOnScreenCondition { true }

        super.onCreate(savedInstanceState)

        // Solicitar permiso de notificaciones para Android 13+ (API 33+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
                Timber.d("📢 Permiso de notificaciones: ${if (isGranted) "concedido ✅" else "denegado ❌"}")
                if (isGranted) {
                    // Después de conceder notificaciones, verificar otros permisos
                    requestSpecialPermissions()
                }
            }.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            // En versiones anteriores, solicitar permisos especiales directamente
            requestSpecialPermissions()
        }

        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val userId = prefs.getInt("user_id", -1)
                    Timber.d("Stored user_id: $userId")
                    val user = usuarioRepository.obtenerUsuarioFlow().firstOrNull()
                    Timber.d("Loaded user: ${user?.nombre}")
                    if (user != null) {
                        prefs.edit().putInt("user_id", user.id).apply()
                        startDestination = Routes.Main
                    }
                }
                isDataLoaded = true
                Timber.d("Data loaded, startDestination=$startDestination")
            } catch (e: Exception) {
                Timber.e(e, "Error al cargar datos iniciales")
                isDataLoaded = true
            }
        }

        splashScreen.setKeepOnScreenCondition {
            Timber.d("SplashScreen visible: isDataLoaded=$isDataLoaded, isUiReady=$isUiReady")
            !isDataLoaded || !isUiReady
        }

        setContent {
            DocTheme {
                AppNavigation(
                    startDestination = startDestination,
                    onUiReady = { isUiReady = true }
                )
            }
        }
    }

    /**
     * Solicitar permisos especiales necesarios para Android 12+
     */
    private fun requestSpecialPermissions() {
        // 1. Verificar y solicitar permiso de alarmas exactas (Android 12+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                Timber.w("⚠️ No se puede programar alarmas exactas, solicitando permiso...")
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = Uri.parse("package:$packageName")
                }
                requestExactAlarmLauncher.launch(intent)
            } else {
                Timber.d("✅ Permiso de alarmas exactas ya concedido")
            }
        }

        // 2. Verificar y solicitar desactivar optimización de batería (Android 6+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
                Timber.w("Optimización de batería activa, solicitando exención...")
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:$packageName")
                }
                requestBatteryOptimizationLauncher.launch(intent)
            } else {
                Timber.d("✅ Optimización de batería ya desactivada")
            }
        }
    }

    /**
     * Verificar estado del permiso de alarmas exactas
     */
    private fun checkExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (alarmManager.canScheduleExactAlarms()) {
                Timber.d("✅ Permiso de alarmas exactas concedido")
            } else {
                Timber.w("❌ Permiso de alarmas exactas denegado - Las notificaciones pueden no funcionar correctamente")
            }
        }
    }

    /**
     * Verificar estado de optimización de batería
     */
    private fun checkBatteryOptimization() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            if (powerManager.isIgnoringBatteryOptimizations(packageName)) {
                Timber.d("✅ Optimización de batería desactivada")
            } else {
                Timber.w("❌ Optimización de batería activa - Las notificaciones en segundo plano pueden no funcionar")
            }
        }
    }
}
