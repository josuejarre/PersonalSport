package com.example.personalsport.data.repository

import com.example.personalsport.data.local.dao.ProgressPhotoDao
import com.example.personalsport.data.local.entity.ProgressPhoto
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

/**
 * Repositorio de datos para la gestión y persistencia de fotos de progreso corporal ([ProgressPhoto]).
 *
 * Garantía de Hilos y Rendimiento:
 * - Flujos despachados en [ioDispatcher] mediante [flowOn].
 * - Transacciones CRUD ejecutadas de forma segura en [ioDispatcher] mediante [withContext].
 *
 * @property progressPhotoDao Objeto DAO de Room para fotos de progreso.
 * @property ioDispatcher Despachador de corrutinas para operaciones de I/O (por defecto [Dispatchers.IO]).
 */
class ProgressPhotoRepository(
    private val progressPhotoDao: ProgressPhotoDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    /**
     * Flujo reactivo de todas las fotos de progreso guardadas.
     */
    val allProgressPhotos: Flow<List<ProgressPhoto>> = progressPhotoDao.getAllProgressPhotos().flowOn(ioDispatcher)

    /**
     * Inserta una nueva foto de progreso en la base de datos de forma asíncrona.
     */
    suspend fun insert(progressPhoto: ProgressPhoto) = withContext(ioDispatcher) {
        progressPhotoDao.insertProgressPhoto(progressPhoto)
    }

    /**
     * Actualiza una foto de progreso en la base de datos de forma asíncrona.
     */
    suspend fun update(progressPhoto: ProgressPhoto) = withContext(ioDispatcher) {
        progressPhotoDao.updateProgressPhoto(progressPhoto)
    }

    /**
     * Elimina una foto de progreso de la base de datos de forma asíncrona.
     */
    suspend fun delete(progressPhoto: ProgressPhoto) = withContext(ioDispatcher) {
        progressPhotoDao.deleteProgressPhoto(progressPhoto)
    }
}
