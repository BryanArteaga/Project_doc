package com.example.doc
import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.os.Bundle
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
import com.google.firebase.messaging.FirebaseMessaging
import com.example.doc.ui.theme.DocTheme
import com.example.doc.navigation.AppNavigation
import com.example.doc.navigation.Routes
import com.example.doc.utils.FCMTokenManager
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
    lateinit var fcmTokenManager: FCMTokenManager
    
    @Inject
    lateinit var prefs: SharedPreferences
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
                Timber.d("📱 Permiso POST_NOTIFICATIONS: ${if (isGranted) "✅ concedido" else "❌ denegado"}")
                if (isGranted) {
                    Timber.d("✅ Proceediendo a obtener FCM token")
                    obtenerFCMToken()
                    fcmTokenManager.registerFCMToken(this@MainActivity)
                    requestSpecialPermissions()
                } else {
                    Timber.w("⚠️ POST_NOTIFICATIONS denegado - notificaciones no funcionarán")
                }
            }.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            obtenerFCMToken()
            fcmTokenManager.registerFCMToken(this)
            requestSpecialPermissions()
        }
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val userId = prefs.getInt("user_id", -1)
                    val user = usuarioRepository.obtenerUsuarioFlow().firstOrNull()
                    if (user != null) {
                        prefs.edit().putInt("user_id", user.id).apply()
                        
                        // Suscribirse al tema de notificaciones personales
                        FirebaseMessaging.getInstance().subscribeToTopic("user_${user.id}")
                            .addOnSuccessListener {
                                Timber.d("✅ Suscrito al tema personal: user_${user.id}")
                            }
                            .addOnFailureListener { e ->
                                Timber.e(e, "❌ Error al suscribirse a tema personal: user_${user.id}")
                            }
                        
                        startDestination = Routes.Main
                    }
                }
                isDataLoaded = true
            } catch (e: Exception) {
                Timber.e(e, "Error al cargar datos")
                isDataLoaded = true
            }
        }
        splashScreen.setKeepOnScreenCondition {
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
    private fun requestSpecialPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = Uri.parse("package:$packageName")
                }
                requestExactAlarmLauncher.launch(intent)
            }
        }
    }
    private fun checkExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (alarmManager.canScheduleExactAlarms()) {
                Timber.d("Permiso de alarmas exactas concedido")
            }
        }
    }
    private fun obtenerFCMToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                prefs.edit().putString("fcm_token", token).apply()
            }
        }
    }
}
