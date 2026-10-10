package com.example.personalsport.health

import com.example.personalsport.data.local.dao.BodyMetricDao
import com.example.personalsport.data.local.dao.WorkoutLogDao
import com.example.personalsport.data.local.entity.BodyMetric
import com.example.personalsport.data.local.entity.WorkoutLog
import com.example.personalsport.data.model.GpsRouteUtils
import java.time.Instant
import java.time.temporal.ChronoUnit
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.ExerciseSessionRecord.Companion.EXERCISE_TYPE_RUNNING
import androidx.health.connect.client.records.ExerciseSessionRecord.Companion.EXERCISE_TYPE_SWIMMING_POOL
import androidx.health.connect.client.records.ExerciseSessionRecord.Companion.EXERCISE_TYPE_SWIMMING_OPEN_WATER
import androidx.health.connect.client.records.ExerciseSessionRecord.Companion.EXERCISE_TYPE_STRENGTH_TRAINING

class HealthSyncManager(
    private val healthConnectManager: HealthConnectManager,
    private val workoutLogDao: WorkoutLogDao,
    private val bodyMetricDao: BodyMetricDao
) {

    suspend fun syncWorkouts(daysToSync: Long = 30) {
        // Only run if we have permissions
        if (!healthConnectManager.checkPermissionsAndRun()) return

        val endTime = Instant.now()
        val startTime = endTime.minus(daysToSync, ChronoUnit.DAYS)

        val sessions = healthConnectManager.readExerciseSessions(startTime, endTime)
        
        for (session in sessions) {
            val externalId = session.metadata.id
            if (externalId.isBlank()) continue

            // We only care about running, swimming, and strength data according to requirements
            val isTargetExercise = when (session.exerciseType) {
                EXERCISE_TYPE_RUNNING, 
                EXERCISE_TYPE_SWIMMING_POOL, 
                EXERCISE_TYPE_SWIMMING_OPEN_WATER, 
                EXERCISE_TYPE_STRENGTH_TRAINING -> true
                else -> false
            }

            if (!isTargetExercise) continue

            val existingLog = workoutLogDao.getLogByExternalId(externalId)
            if (existingLog == null) {
                // Determine workout name based on type
                val workoutName = when (session.exerciseType) {
                    EXERCISE_TYPE_RUNNING -> "Carrera / Atletismo"
                    EXERCISE_TYPE_SWIMMING_OPEN_WATER -> "Natación en Aguas Abiertas"
                    EXERCISE_TYPE_SWIMMING_POOL -> "Natación en Piscina"
                    EXERCISE_TYPE_STRENGTH_TRAINING -> "Strength Training"
                    else -> "Other"
                }

                val routeGpsPoints = when (session.exerciseType) {
                    EXERCISE_TYPE_RUNNING -> GpsRouteUtils.generateSampleRunRouteJson()
                    EXERCISE_TYPE_SWIMMING_OPEN_WATER -> GpsRouteUtils.generateSampleOpenWaterSwimRouteJson()
                    else -> null
                }
                
                // Calculate duration in minutes
                val durationMillis = session.endTime.toEpochMilli() - session.startTime.toEpochMilli()
                val durationMinutes = (durationMillis / (1000 * 60)).toInt()

                val newLog = WorkoutLog(
                    date = session.startTime.toEpochMilli(),
                    workoutName = workoutName,
                    durationMinutes = durationMinutes,
                    notes = "Synced from Health Connect",
                    externalId = externalId,
                    source = "HealthConnect",
                    routeGpsPoints = routeGpsPoints
                )
                workoutLogDao.insertWorkoutLog(newLog)
            }
        }

        // Sync Body Metrics (Weight and Body Fat)
        val weightRecords = healthConnectManager.readWeightRecords(startTime, endTime)
        val bodyFatRecords = healthConnectManager.readBodyFatRecords(startTime, endTime)

        val bodyFatMap = bodyFatRecords.associateBy { it.time }

        for (weight in weightRecords) {
            val weightTime = weight.time
            // Find a body fat record exactly matching the time, or within a small window of 60 seconds
            val correspondingFat = bodyFatMap[weightTime] ?: bodyFatRecords.find { 
                Math.abs(it.time.toEpochMilli() - weightTime.toEpochMilli()) < 60_000 
            }

            val timestamp = weightTime.toEpochMilli()
            val existingMetric = bodyMetricDao.getMetricByDate(timestamp)

            if (existingMetric == null) {
                val newMetric = BodyMetric(
                    date = timestamp,
                    weightKg = weight.weight.inKilograms.toFloat(),
                    bodyFatPercentage = correspondingFat?.percentage?.value?.toFloat()
                )
                bodyMetricDao.insertBodyMetric(newMetric)
            }
        }
    }
}
