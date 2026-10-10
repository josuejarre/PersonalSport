package com.example.personalsport.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.personalsport.data.local.entity.WorkoutLog
import kotlinx.coroutines.flow.Flow

/**
 * Objeto de Acceso a Datos (DAO) para la entidad [WorkoutLog].
 * Gestiona lecturas en flujo y escrituras asíncronas para entrenamientos.
 */
@Dao
interface WorkoutLogDao {
    /**
     * Obtiene el flujo ordenado de registros de entrenamiento por fecha descendente.
     */
    @Query("SELECT * FROM workout_logs ORDER BY date DESC")
    fun getAllWorkoutLogs(): Flow<List<WorkoutLog>>

    /**
     * Busca un entrenamiento por su identificador externo de dispositivo/sincronización.
     */
    @Query("SELECT * FROM workout_logs WHERE externalId = :externalId LIMIT 1")
    suspend fun getLogByExternalId(externalId: String): WorkoutLog?

    /**
     * Inserta o actualiza un registro de entrenamiento en la base de datos.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkoutLog(workoutLog: WorkoutLog)

    /**
     * Actualiza un entrenamiento existente.
     */
    @Update
    suspend fun updateWorkoutLog(workoutLog: WorkoutLog)

    /**
     * Elimina un registro de entrenamiento.
     */
    @Delete
    suspend fun deleteWorkoutLog(workoutLog: WorkoutLog)
}
