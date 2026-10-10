package com.example.personalsport.data.repository

import com.example.personalsport.data.local.dao.EquipmentDao
import com.example.personalsport.data.local.entity.Equipment
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

/**
 * Repositorio de datos para el inventario de equipamiento y material deportivo ([Equipment]).
 *
 * Garantía de Hilos y Rendimiento:
 * - Flujos despachados en [ioDispatcher] mediante [flowOn].
 * - Operaciones CRUD ejecutadas en [ioDispatcher] usando [withContext].
 *
 * @property equipmentDao Objeto DAO de Room para equipamiento.
 * @property ioDispatcher Despachador de corrutinas para operaciones de I/O (por defecto [Dispatchers.IO]).
 */
class EquipmentRepository(
    private val equipmentDao: EquipmentDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    /**
     * Flujo reactivo del catálogo de equipamiento disponible.
     */
    val allEquipment: Flow<List<Equipment>> = equipmentDao.getAllEquipment().flowOn(ioDispatcher)

    /**
     * Inserta un nuevo objeto de equipamiento en la base de datos de forma asíncrona.
     */
    suspend fun insert(equipment: Equipment) = withContext(ioDispatcher) {
        equipmentDao.insertEquipment(equipment)
    }

    /**
     * Actualiza un objeto de equipamiento en la base de datos de forma asíncrona.
     */
    suspend fun update(equipment: Equipment) = withContext(ioDispatcher) {
        equipmentDao.updateEquipment(equipment)
    }

    /**
     * Elimina un objeto de equipamiento de la base de datos de forma asíncrona.
     */
    suspend fun delete(equipment: Equipment) = withContext(ioDispatcher) {
        equipmentDao.deleteEquipment(equipment)
    }
}
