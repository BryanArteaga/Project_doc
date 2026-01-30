package com.example.doc.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "consejos")
data class ConsejoEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val enfermedad: String,
    val consejo: String
)