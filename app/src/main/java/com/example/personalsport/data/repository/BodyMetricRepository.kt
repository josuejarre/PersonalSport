package com.example.personalsport.data.repository

import com.example.personalsport.data.local.dao.BodyMetricDao
import com.example.personalsport.data.local.entity.BodyMetric
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

/**
 * Repositorio de datos para el seguimiento y persistencia de métricas corporales ([BodyMetric]).
 *
 * Garantía de Hilos y Rendimiento:
 * - Flujos despachados en [ioDispatcher] mediante [flowOn].
 * - Operaciones CRUD ejecutadas explícitamente en [ioDispatcher] mediante [withContext].
 *
 * @property bodyMetricDao Objeto DAO de Room para métricas corporales.
 * @property ioDispatcher Despachador de corrutinas para operaciones de I/O (por defecto [Dispatchers.IO]).
 */
class BodyMetricRepository(
    private val bodyMetricDao: BodyMetricDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    /**
     * Flujo reactivo de todas las métricas corporales registradas.
     */
    val allBodyMetrics: Flow<List<BodyMetric>> = bodyMetricDao.getAllBodyMetrics().flowOn(ioDispatcher)

    /**
     * Inserta una nueva métrica corporal en la base de datos de forma asíncrona.
     */
    suspend fun insert(bodyMetric: BodyMetric) = withContext(ioDispatcher) {
        bodyMetricDao.insertBodyMetric(bodyMetric)
    }

    /**
     * Actualiza una métrica corporal en la base de datos de forma asíncrona.
     */
    suspend fun update(bodyMetric: BodyMetric) = withContext(ioDispatcher) {
        bodyMetricDao.updateBodyMetric(bodyMetric)
    }

    /**
     * Elimina una métrica corporal de la base de datos de forma asíncrona.
     */
    suspend fun delete(bodyMetric: BodyMetric) = withContext(ioDispatcher) {
        bodyMetricDao.deleteBodyMetric(bodyMetric)
    }
}
