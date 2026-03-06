package com.example.doc.network

import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonDeserializationContext
import java.lang.reflect.Type

/**
 * Deserializer defensivo para Long timestamps (Android 7 compatible)
 * Maneja tanto números como strings sin crashes
 */
class LongTimestampDeserializer : JsonDeserializer<Long> {
    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): Long {
        return when {
            json == null -> System.currentTimeMillis()
            json.isJsonNull -> System.currentTimeMillis()
            json.isJsonPrimitive -> {
                val prim = json.asJsonPrimitive
                when {
                    prim.isNumber -> prim.asLong
                    prim.isString -> {
                        // Intentar parsear como timestamp
                        try {
                            prim.asString.toLong()
                        } catch (e: Exception) {
                            // Si falla, usar ahora
                            System.currentTimeMillis()
                        }
                    }
                    else -> System.currentTimeMillis()
                }
            }
            else -> System.currentTimeMillis()
        }
    }
}
