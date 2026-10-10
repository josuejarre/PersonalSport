package com.example.personalsport.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.personalsport.data.local.entity.ProgressPhoto
import kotlinx.coroutines.flow.Flow

/**
 * Objeto de Acceso a Datos (DAO) para la entidad [ProgressPhoto].
 * Permite gestionar la galería de fotos de evolución corporal.
 */
@Dao
interface ProgressPhotoDao {
    /**
     * Obtiene el flujo de todas las fotos de progreso ordenadas por fecha más reciente.
     */
    @Query("SELECT * FROM progress_photos ORDER BY date DESC")
    fun getAllProgressPhotos(): Flow<List<ProgressPhoto>>

    /**
     * Inserta o actualiza una foto de progreso.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProgressPhoto(progressPhoto: ProgressPhoto)

    /**
     * Actualiza una foto de progreso existente.
     */
    @Update
    suspend fun updateProgressPhoto(progressPhoto: ProgressPhoto)

    /**
     * Elimina una foto de progreso.
     */
    @Delete
    suspend fun deleteProgressPhoto(progressPhoto: ProgressPhoto)
}
