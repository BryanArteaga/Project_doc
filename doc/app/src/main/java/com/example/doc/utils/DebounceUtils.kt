package com.example.doc.utils

import androidx.compose.runtime.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Debounce: Retrasa la ejecución de una acción hasta que no haya cambios durante el tiempo especificado
 * Útil para validaciones de campos de texto que no necesitan ejecutarse en cada tecla presionada
 */
class Debouncer(
    private val delayMillis: Long = 300L,
    private val coroutineScope: CoroutineScope
) {
    private var debounceJob: Job? = null

    fun debounce(action: suspend () -> Unit) {
        debounceJob?.cancel()
        debounceJob = coroutineScope.launch {
            delay(delayMillis)
            action()
        }
    }

    fun cancel() {
        debounceJob?.cancel()
    }
}

/**
 * Composable para crear un debouncer que se cancela automáticamente cuando sale de composición
 */
@Composable
fun rememberDebouncer(
    delayMillis: Long = 300L
): Debouncer {
    val scope = rememberCoroutineScope()
    return remember(delayMillis) {
        Debouncer(delayMillis, scope)
    }
}

/**
 * Hook para usar un valor con debounce
 * El valor debouncedValue solo cambiará después de que pase el delay sin nuevos cambios
 */
@Composable
fun <T> rememberDebouncedValue(
    value: T,
    delayMillis: Long = 300L
): T {
    var debouncedValue by remember { mutableStateOf(value) }

    LaunchedEffect(value) {
        delay(delayMillis)
        debouncedValue = value
    }

    return debouncedValue
}

/**
 * Throttle: Limita la frecuencia de ejecución de una acción
 * Ejecuta la acción inmediatamente y luego ignora llamadas durante el tiempo especificado
 * Útil para botones que no deben presionarse repetidamente
 */
class Throttler(
    private val delayMillis: Long = 1000L,
    private val coroutineScope: CoroutineScope
) {
    private var lastExecutionTime = 0L

    fun throttle(action: suspend () -> Unit) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastExecutionTime >= delayMillis) {
            lastExecutionTime = currentTime
            coroutineScope.launch {
                action()
            }
        }
    }
}

/**
 * Composable para crear un throttler que se cancela automáticamente cuando sale de composición
 */
@Composable
fun rememberThrottler(
    delayMillis: Long = 1000L
): Throttler {
    val scope = rememberCoroutineScope()
    return remember(delayMillis) {
        Throttler(delayMillis, scope)
    }
}

