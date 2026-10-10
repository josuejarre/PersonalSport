package com.example.personalsport.data.repository

import com.example.personalsport.data.local.dao.CompletedExerciseDao
import com.example.personalsport.data.local.entity.CompletedExerciseRecord
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

/**
 * Repositorio de datos para el seguimiento del estado de ejecución de ejercicios ([CompletedExerciseRecord]).
 *
 * Garantía de Hilos y Rendimiento:
 * - Flujos despachados en [ioDispatcher] mediante [flowOn].
 * - Operaciones de actualización ejecutadas en [ioDispatcher] usando [withContext].
 *
 * @property dao Objeto DAO de Room para ejercicios completados.
 * @property ioDispatcher Despachador de corrutinas para operaciones de I/O (por defecto [Dispatchers.IO]).
 */
class CompletedExerciseRepository(
    private val dao: CompletedExerciseDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    /**
     * Flujo reactivo de todos los ejercicios marcados como completados.
     */
    val completedExercises: Flow<List<CompletedExerciseRecord>> = dao.getAllCompletedExercises().flowOn(ioDispatcher)

    /**
     * Alterna el estado de completado de un ejercicio en la base de datos de forma asíncrona.
     */
    suspend fun toggleExerciseCompletion(
        exerciseId: String,
        exerciseTitle: String,
        currentStatus: Boolean
    ) = withContext(ioDispatcher) {
        val newStatus = !currentStatus
        if (newStatus) {
            dao.insertOrUpdate(CompletedExerciseRecord(exerciseId, exerciseTitle, isCompleted = true))
        } else {
            dao.deleteByExerciseId(exerciseId)
        }
    }

    /**
     * Establece de forma explícita el estado de completado de un ejercicio.
     */
    suspend fun setExerciseCompleted(
        exerciseId: String,
        exerciseTitle: String,
        isCompleted: Boolean
    ) = withContext(ioDispatcher) {
        if (isCompleted) {
            dao.insertOrUpdate(CompletedExerciseRecord(exerciseId, exerciseTitle, isCompleted = true))
        } else {
            dao.deleteByExerciseId(exerciseId)
        }
    }
}
