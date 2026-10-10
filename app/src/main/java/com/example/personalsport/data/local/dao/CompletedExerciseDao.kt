package com.example.personalsport.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.personalsport.data.local.entity.CompletedExerciseRecord
import kotlinx.coroutines.flow.Flow

/**
 * Objeto de Acceso a Datos (DAO) para la entidad [CompletedExerciseRecord].
 * Gestiona el seguimiento de ejercicios completados individualmente.
 */
@Dao
interface CompletedExerciseDao {
    /**
     * Obtiene el flujo de todos los ejercicios completados.
     */
    @Query("SELECT * FROM completed_exercises WHERE isCompleted = 1")
    fun getAllCompletedExercises(): Flow<List<CompletedExerciseRecord>>

    /**
     * Inserta o actualiza el registro de ejecución de un ejercicio.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(record: CompletedExerciseRecord)

    /**
     * Elimina el registro de un ejercicio por su identificador.
     */
    @Query("DELETE FROM completed_exercises WHERE exerciseId = :exerciseId")
    suspend fun deleteByExerciseId(exerciseId: String)

    /**
     * Comprueba si un ejercicio ha sido marcado como completado.
     */
    @Query("SELECT EXISTS(SELECT 1 FROM completed_exercises WHERE exerciseId = :exerciseId AND isCompleted = 1)")
    suspend fun isExerciseCompleted(exerciseId: String): Boolean
}
