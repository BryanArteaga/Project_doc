package com.example.doc.model

enum class Enfermedad(val displayName: String) {
    HIPERTENSION("Hipertensión"),
    DIABETES("Diabetes"),
    OBESIDAD("Obesidad"),
    COLESTEROL_ALTO("Colesterol alto"),
    DESCONOCIDA("Desconocida");

    companion object {
        fun fromString(name: String): Enfermedad {
            return values().find { it.displayName == name } ?: DESCONOCIDA
        }
    }
}