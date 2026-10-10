package com.example.personalsport.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workout_logs")
data class WorkoutLog(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val date: Long,
    val workoutName: String,
    val durationMinutes: Int,
    val notes: String? = null,
    val externalId: String? = null,
    val source: String = "manual",
    val routeGpsPoints: String? = null
)
