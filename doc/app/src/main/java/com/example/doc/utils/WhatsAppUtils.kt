package com.example.doc.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.doc.model.Usuario
import timber.log.Timber

/**
 * 📲 Abrir WhatsApp del doctor para preguntas sobre registros
 * Solo aparece si el registro está en ALERTA (amarillo/naranja/rojo)
 */
fun abrirWhatsAppDoctor(
    context: Context?,
    usuario: Usuario?,
    mensaje: String,
    detalles: String = ""
) {
    if (context == null) {
        Timber.w("⚠️ Context es null, no se puede abrir WhatsApp")
        return
    }
    
    // 🔧 Obtener número del doctor (actualmente configurado como fallback)
    val numeroDoctorWhatsApp = "+593967313638"  // Número del doctor
    // TODO: Obtener desde endpoint /api/doctor/whatsapp o de usuario.doctor.whatsapp (más seguro)
    
    if (numeroDoctorWhatsApp.isEmpty()) {
        Timber.w("⚠️ Número de doctor no disponible")
        return
    }
    
    try {
        // Limpiar número: solo dígitos y +
        val numeroLimpio = numeroDoctorWhatsApp.replace(Regex("[^0-9+]"), "")
        
        // Construir mensaje con datos de salud si están disponibles
        val mensajeCompleto = if (detalles.isNotEmpty()) {
            "$mensaje\n\nDatos del registro:\n$detalles"
        } else {
            mensaje
        }
        
        // Crear intent para WhatsApp
        val url = "https://wa.me/$numeroLimpio?text=${Uri.encode(mensajeCompleto)}"
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = Uri.parse(url)
        
        context.startActivity(intent)
        Timber.d("✅ Abriendo WhatsApp: $mensajeCompleto")
    } catch (e: Exception) {
        Timber.e(e, "❌ Error al abrir WhatsApp")
        // Fallback: Si WhatsApp no está instalado, mostrar error al usuario
        // Podrías agregar un Toast aquí
    }
}

/**
 * Verificar si WhatsApp está instalado
 */
fun esWhatsAppInstalado(context: Context): Boolean {
    return try {
        context.packageManager.getPackageInfo("com.whatsapp", 0)
        true
    } catch (e: Exception) {
        false
    }
}
