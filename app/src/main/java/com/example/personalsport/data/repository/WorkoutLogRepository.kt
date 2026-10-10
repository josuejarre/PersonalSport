package com.example.personalsport.data.repository

import com.example.personalsport.data.local.dao.WorkoutLogDao
import com.example.personalsport.data.local.entity.WorkoutLog
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

/**
 * Repositorio de datos para la gestión y persistencia de registros de entrenamiento ([WorkoutLog]).
 *
 * Garantía de Hilos y Rendimiento:
 * - Todas las operaciones de lectura en flujo ([allWorkoutLogs]) se despachan en [ioDispatcher] mediante [flowOn].
 * - Todas las transacciones CRUD de escritura, actualización y eliminación se fuerzan explícitamente en [ioDispatcher]
 *   usando [withContext(ioDispatcher)] para prevenir bloqueos de renderizado en el Hilo Principal (Main Thread).
 *
 * @property workoutLogDao Objeto DAO de Room para registros de entrenamiento.
 * @property ioDispatcher Despachador de corrutinas para operaciones de I/O (por defecto [Dispatchers.IO]).
 */
class WorkoutLogRepository(
    private val workoutLogDao: WorkoutLogDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    /**
     * Flujo reactivo continuo de todos los registros de entrenamiento ordenados por fecha.
     */
    val allWorkoutLogs: Flow<List<WorkoutLog>> = workoutLogDao.getAllWorkoutLogs().flowOn(ioDispatcher)

    /**
     * Inserta un nuevo registro de entrenamiento en la base de datos de forma asíncrona.
     */
    suspend fun insert(workoutLog: WorkoutLog) = withContext(ioDispatcher) {
        workoutLogDao.insertWorkoutLog(workoutLog)
    }

    /**
     * Actualiza un registro de entrenamiento existente en la base de datos de forma asíncrona.
     */
    suspend fun update(workoutLog: WorkoutLog) = withContext(ioDispatcher) {
        workoutLogDao.updateWorkoutLog(workoutLog)
    }

    /**
     * Elimina un registro de entrenamiento de la base de datos de forma asíncrona.
     */
    suspend fun delete(workoutLog: WorkoutLog) = withContext(ioDispatcher) {
        workoutLogDao.deleteWorkoutLog(workoutLog)
    }
}
