package com.example.personalsport.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "completed_exercises")
data class CompletedExerciseRecord(
    @PrimaryKey
    val exerciseId: String,
    val exerciseTitle: String,
    val isCompleted: Boolean = true,
    val timestamp: Long = System.currentTimeMillis()
)
