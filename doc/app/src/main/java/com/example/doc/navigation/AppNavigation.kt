package com.example.doc.navigation

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.doc.screens.*
import com.example.doc.viewmodel.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


@Composable
fun AppNavigation(
    startDestination: String,
    onUiReady: () -> Unit
) {
    val navController = rememberNavController()
    val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    // Obtener ViewModels compartidos a nivel de NavController (no se cancelan al navegar)
    val usuarioViewModel: UsuarioViewModel = hiltViewModel()
    val registroSaludViewModel: RegistroSaludViewModel = hiltViewModel()
    val ejercicioViewModel: EjercicioViewModel = hiltViewModel()
    val consejoViewModel: ConsejoViewModel = hiltViewModel()

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Routes.Welcome) {
            WelcomeScreen(navController, onUiReady)
        }

        composable(Routes.Register) {
            RegisterScreen(navController, usuarioViewModel)
        }

        composable(
            route = "${Routes.Condition}?cedula={cedula}&name={name}&edad={edad}&peso={peso}&altura={altura}&sexo={sexo}",
            arguments = listOf(
                navArgument("name") { type = NavType.StringType },
                navArgument("cedula") { type = NavType.StringType },
                navArgument("edad") { type = NavType.IntType },
                navArgument("peso") { type = NavType.FloatType },
                navArgument("altura") { type = NavType.FloatType },
                navArgument("sexo") {
                    type = NavType.StringType
                    nullable = true
                }
            )
        ) { backStackEntry ->
            val name = backStackEntry.arguments?.getString("name") ?: ""
            val cedula = backStackEntry.arguments?.getString("cedula")
            val edad = backStackEntry.arguments?.getInt("edad") ?: 0
            val peso = backStackEntry.arguments?.getFloat("peso")?.toDouble() ?: 0.0
            val altura = backStackEntry.arguments?.getFloat("altura")?.toDouble() ?: 0.0
            val sexo = backStackEntry.arguments?.getString("sexo")

            ConditionSelectionScreen(
                navController,
                usuarioViewModel,
                cedula,
                name,
                edad,
                peso,
                altura,
                sexo
            ) { _, _ ->
                // Esperar un momento para que el usuario se guarde en la BD
                timber.log.Timber.d("⏱️ Esperando 300ms para que se guarde el usuario...")
                GlobalScope.launch(Dispatchers.Main) {
                    delay(300) // Dar tiempo para que Room guarde

                    timber.log.Timber.d("🚀 Navegando a MainScreen...")
                    navController.navigate(Routes.Main) {
                        // Eliminar TODAS las pantallas anteriores (Welcome, Register, Condition)
                        popUpTo(0) {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                }
            }
        }

        composable(Routes.Main) {
            MainScreen(
                navController = navController,
                usuarioViewModel = usuarioViewModel,
                registroSaludViewModel = registroSaludViewModel,
                consejoViewModel = consejoViewModel,
                onUiReady = onUiReady
            )
        }

        composable(Routes.RegistroSalud) {
            RegistroSaludScreen(
                navController = navController,
                usuarioViewModel = usuarioViewModel,
                registroViewModel = registroSaludViewModel,
                ejercicioViewModel = ejercicioViewModel,
                currentDate = currentDate
            )
        }

        composable(Routes.Ejercicios) {
            EjerciciosScreen(navController, usuarioViewModel, ejercicioViewModel)
        }

        composable(
            route = "${Routes.ExerciseDetails}/{ejercicioId}",
            arguments = listOf(navArgument("ejercicioId") { type = NavType.IntType })
        ) { backStackEntry ->
            val ejercicioId = backStackEntry.arguments?.getInt("ejercicioId") ?: 0
            ExerciseDetailsScreen(navController, usuarioViewModel, ejercicioViewModel, ejercicioId)
        }

        composable(
            route = "${Routes.ExerciseTimer}/{ejercicioId}",
            arguments = listOf(navArgument("ejercicioId") { type = NavType.IntType })
        ) { backStackEntry ->
            val ejercicioId = backStackEntry.arguments?.getInt("ejercicioId") ?: 0
            ExerciseTimerScreen(navController, usuarioViewModel, ejercicioViewModel, ejercicioId)
        }

        composable(Routes.Perfil) {
            PerfilScreen(navController, usuarioViewModel)
        }
    }
}

