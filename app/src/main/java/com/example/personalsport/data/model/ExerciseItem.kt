package com.example.personalsport.data.model

import java.util.UUID

data class ExerciseItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String = "",
    val category: String = "Entrenamiento",
    val targetMuscle: String = "Cuerpo Completo",
    val equipmentName: String = "Peso Corporal",
    val imageUrl: String? = null,
    val exerciseIllustration: String? = null,
    val instructions: String = "Ejecuta el movimiento con técnica controlada, manteniendo la postura biomecánica adecuada y la respiración fluida.",
    val durationOrReps: String = "3 series x 12 repeticiones",
    val isCompleted: Boolean = false
)
