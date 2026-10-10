package com.example.personalsport.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.personalsport.data.local.entity.Equipment
import kotlinx.coroutines.flow.Flow

/**
 * Objeto de Acceso a Datos (DAO) para la entidad [Equipment].
 * Gestiona el inventario de equipamiento disponible.
 */
@Dao
interface EquipmentDao {
    /**
     * Obtiene el flujo del equipamiento disponible.
     */
    @Query("SELECT * FROM equipment")
    fun getAllEquipment(): Flow<List<Equipment>>

    /**
     * Inserta un elemento de equipamiento.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEquipment(equipment: Equipment)

    /**
     * Actualiza un elemento de equipamiento.
     */
    @Update
    suspend fun updateEquipment(equipment: Equipment)

    /**
     * Elimina un elemento de equipamiento.
     */
    @Delete
    suspend fun deleteEquipment(equipment: Equipment)
}
