package com.example.personalsport.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.personalsport.data.local.entity.BodyMetric
import kotlinx.coroutines.flow.Flow

/**
 * Objeto de Acceso a Datos (DAO) para la entidad [BodyMetric].
 * Proporciona métodos reactivos mediante [Flow] y operaciones asíncronas CRUD.
 */
@Dao
interface BodyMetricDao {
    /**
     * Obtiene el flujo reactivo de métricas corporales ordenadas de la más reciente a la más antigua.
     */
    @Query("SELECT * FROM body_metrics ORDER BY date DESC")
    fun getAllBodyMetrics(): Flow<List<BodyMetric>>

    /**
     * Busca una métrica por su marca de tiempo exacta.
     */
    @Query("SELECT * FROM body_metrics WHERE date = :date LIMIT 1")
    suspend fun getMetricByDate(date: Long): BodyMetric?

    /**
     * Inserta o reemplaza una métrica corporal en la base de datos.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBodyMetric(bodyMetric: BodyMetric)

    /**
     * Actualiza una métrica existente.
     */
    @Update
    suspend fun updateBodyMetric(bodyMetric: BodyMetric)

    /**
     * Elimina una métrica de la base de datos.
     */
    @Delete
    suspend fun deleteBodyMetric(bodyMetric: BodyMetric)
}
