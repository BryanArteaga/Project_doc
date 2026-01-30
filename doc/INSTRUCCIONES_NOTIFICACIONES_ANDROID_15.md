# 🔔 Correcciones para Notificaciones en Android 15

## 📋 Resumen del Problema

Las notificaciones funcionaban perfectamente en Android 7, pero en Android 15 (y Android 12+) fallaban debido a:

1. **Falta de permisos críticos** para Android 12+
2. **Restricciones de batería** (Doze Mode)
3. **Restricciones de alarmas exactas** no solicitadas
4. **Falta de WakeLock** para despertar el dispositivo
5. **Configuración insuficiente** de notificaciones de alta prioridad

## ✅ Soluciones Implementadas

### 1. **Nuevos Permisos en AndroidManifest.xml**

Se agregaron los siguientes permisos críticos:

```xml
<!-- Despertar dispositivo en Doze Mode -->
<uses-permission android:name="android.permission.WAKE_LOCK" />

<!-- Ignorar optimizaciones de batería -->
<uses-permission android:name="android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS" />

<!-- Notificaciones en pantalla completa (Android 14+) -->
<uses-permission android:name="android.permission.USE_FULL_SCREEN_INTENT" />

<!-- Servicios en primer plano para Workers -->
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_DATA_SYNC" />

<!-- Internet y red (para futuro backend) -->
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

### 2. **BroadcastReceiver Mejorado**

- Se agregó `android:directBootAware="true"` para funcionar antes de desbloquear el dispositivo
- Soporte para múltiples acciones de reinicio:
  - `ACTION_BOOT_COMPLETED`
  - `ACTION_LOCKED_BOOT_COMPLETED`
  - `QUICKBOOT_POWERON`

### 3. **WakeLock en NotificationReceiver**

Ahora el receiver adquiere un WakeLock al recibir una alarma para asegurar que el dispositivo se despierte:

```kotlin
val wakeLock = powerManager.newWakeLock(
    PowerManager.PARTIAL_WAKE_LOCK,
    WAKELOCK_TAG
)
wakeLock.acquire(3 * 60 * 1000L) // 3 minutos
```

### 4. **Full Screen Intent para Android 14+**

Se agregó soporte de Full Screen Intent en los Workers de notificaciones para Android 10+ (especialmente crítico para Android 14-15):

```kotlin
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
    notificationBuilder.setFullScreenIntent(fullScreenPendingIntent, true)
}
```

### 5. **Solicitud Automática de Permisos en MainActivity**

La app ahora solicita automáticamente al inicio:

- ✅ **Permiso de notificaciones** (Android 13+)
- ✅ **Permiso de alarmas exactas** (Android 12+)
- ✅ **Desactivación de optimización de batería** (Android 6+)

## 🚀 Instrucciones para Probar

### Paso 1: Compilar la App

```bash
cd "/home/brste/Projects/Project doc/doc"
./gradlew clean assembleDebug
```

### Paso 2: Instalar en Dispositivo Android 15

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### Paso 3: Permisos al Abrir la App

Al abrir la app por primera vez, se solicitarán automáticamente:

1. **Permiso de notificaciones** → Presionar "Permitir"
2. **Permiso de alarmas exactas** → Presionar "Permitir" en configuración
3. **Desactivar optimización de batería** → Seleccionar "Permitir" o "No restringir"

### Paso 4: Configurar Notificaciones en la App

1. Ir a **Configuración** en la app
2. Habilitar **"Notificaciones"**
3. Configurar **frecuencia** (diaria/semanal/mensual)
4. Configurar **hora** de recordatorio
5. Guardar configuración

### Paso 5: Verificar en Logs

Usa logcat para ver si las alarmas se programan correctamente:

```bash
adb logcat | grep -i "DOC\|notification\|alarm"
```

Deberías ver:
- ✅ `Alarma DIARIA programada para las XX:XX`
- ✅ `Permiso de alarmas exactas concedido`
- ✅ `Optimización de batería desactivada`

### Paso 6: Probar en Diferentes Escenarios

#### Escenario 1: App Cerrada (Matada)
```bash
adb shell am force-stop com.example.doc
```
**Resultado esperado:** La notificación debe llegar a la hora programada aunque la app esté cerrada.

#### Escenario 2: Dispositivo en Doze Mode
```bash
# Simular Doze Mode
adb shell dumpsys deviceidle force-idle
```
**Resultado esperado:** La notificación debe llegar usando `setExactAndAllowWhileIdle`.

#### Escenario 3: Después de Reinicio
```bash
adb reboot
```
**Resultado esperado:** Las alarmas se deben reprogramar automáticamente al reiniciar.

## 🔧 Configuración Manual (Si los Permisos no se Solicitan)

Si por alguna razón los permisos no se solicitan automáticamente:

### 1. Permitir Alarmas Exactas
```
Configuración → Apps → DOC → Permisos Avanzados → Alarmas y recordatorios → Permitir
```

### 2. Desactivar Optimización de Batería
```
Configuración → Batería → Optimización de batería → Todas las apps → DOC → No optimizar
```

### 3. Permitir Notificaciones
```
Configuración → Notificaciones → DOC → Permitir notificaciones
```

### 4. Desactivar Restricciones de Segundo Plano
```
Configuración → Apps → DOC → Uso de batería → Sin restricciones
```

## 📊 Verificar Estado de Permisos

Para verificar que todos los permisos están concedidos:

```bash
# Ver permisos de la app
adb shell dumpsys package com.example.doc | grep permission

# Ver si puede programar alarmas exactas
adb shell dumpsys alarm | grep com.example.doc

# Ver optimización de batería
adb shell dumpsys deviceidle whitelist | grep com.example.doc
```

## 🐛 Solución de Problemas

### Problema: Las notificaciones llegan solo cuando abro la app

**Causa:** El sistema está matando la app en segundo plano.

**Solución:**
1. Desactivar optimización de batería
2. Configurar app en "Sin restricciones" de batería
3. Agregar a lista blanca de ahorro de batería

### Problema: Las notificaciones llegan a hora incorrecta

**Causa:** El sistema está retrasando las alarmas.

**Solución:**
1. Verificar que el permiso de alarmas exactas esté concedido
2. Desactivar modo "Ahorro de batería" extremo
3. Verificar que `canScheduleExactAlarms()` retorna `true`

### Problema: No llegan notificaciones después de reiniciar

**Causa:** El `BroadcastReceiver` no se está ejecutando al reiniciar.

**Solución:**
1. Verificar que el permiso `RECEIVE_BOOT_COMPLETED` está concedido
2. La app debe estar instalada en almacenamiento interno (no SD)
3. Verificar logs: `adb logcat | grep "Dispositivo reiniciado"`

## 📱 Diferencias entre Android 7 y Android 15

| Característica | Android 7 | Android 15 |
|----------------|-----------|------------|
| Alarmas exactas | Automático | Requiere permiso explícito |
| Doze Mode | Básico | Avanzado con restricciones |
| Optimización batería | Opcional | Agresiva por defecto |
| Full Screen Intent | Automático | Requiere permiso |
| BroadcastReceiver en boot | Funciona | Requiere `directBootAware` |
| WorkManager | Menos restricciones | Altamente restringido |

## ✨ Características Adicionales Implementadas

1. **Reprogramación automática** después de disparar una notificación
2. **Persistencia de configuración** en SharedPreferences
3. **Logs detallados** con emojis para debugging
4. **Soporte multi-frecuencia** (diaria, semanal, mensual)
5. **Notificaciones de ejercicio** con dos horarios (mañana y tarde)

## 📝 Notas Importantes

- ⚠️ **Google Play Store**: El uso de `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` puede ser rechazado si no se justifica. Para publicar, considera usar una estrategia alternativa o justificar su uso como app de salud/recordatorios médicos.

- ⚠️ **Android 14+**: El sistema puede solicitar al usuario que justifique por qué la app necesita Full Screen Intent. Asegúrate de explicar claramente en la UI.

- ✅ **Compatibilidad**: Los cambios son compatibles con Android 7 y superiores. En versiones antiguas, los permisos especiales se ignoran automáticamente.

## 🎯 Resultado Final

Con estas correcciones, las notificaciones deben funcionar correctamente en Android 15:

✅ Llegan a la hora exacta programada
✅ Funcionan con la app cerrada
✅ Sobreviven al reinicio del dispositivo
✅ Funcionan en Doze Mode
✅ Se muestran como heads-up notifications
✅ Vibración y sonido funcionan correctamente

---

**Autor:** GitHub Copilot
**Fecha:** 2026-01-06
**Versión Android mínima:** 24 (Android 7.0)
**Versión Android objetivo:** 35 (Android 15)

