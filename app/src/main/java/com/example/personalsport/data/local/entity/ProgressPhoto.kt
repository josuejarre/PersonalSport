package com.example.personalsport.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "progress_photos")
data class ProgressPhoto(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val date: Long,
    val photoUri: String,
    val notes: String = ""
)
