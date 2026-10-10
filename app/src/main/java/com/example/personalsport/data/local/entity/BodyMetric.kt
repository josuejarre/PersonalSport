package com.example.personalsport.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "body_metrics")
data class BodyMetric(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val date: Long,
    val weightKg: Float,
    val bodyFatPercentage: Float? = null
)
