package com.example.doc.navigation

object Routes {
    const val Welcome = "welcome"
    const val Register = "register"
    const val Condition = "condition"
    const val Main = "main"
    const val RegistroSalud = "registro_salud"
    const val Ejercicios = "ejercicios"
    const val ExerciseDetails = "exercise_details"
    const val ExerciseTimer = "exercise_timer"
    const val Perfil = "perfil"

    fun conditionWithArgs(name: String, edad: Int, peso: Double, altura: Double, sexo: String) =
        "condition/$name/$edad/$peso/$altura/$sexo"

    fun exerciseDetails(ejercicioId: Int) = "exercise_details/$ejercicioId"

    fun exerciseTimer(ejercicioId: Int) = "exercise_timer/$ejercicioId"
}