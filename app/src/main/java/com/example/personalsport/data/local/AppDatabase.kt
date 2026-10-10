package com.example.personalsport.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.personalsport.data.local.dao.BodyMetricDao
import com.example.personalsport.data.local.dao.CompletedExerciseDao
import com.example.personalsport.data.local.dao.EquipmentDao
import com.example.personalsport.data.local.dao.ProgressPhotoDao
import com.example.personalsport.data.local.dao.WorkoutLogDao
import com.example.personalsport.data.local.entity.BodyMetric
import com.example.personalsport.data.local.entity.CompletedExerciseRecord
import com.example.personalsport.data.local.entity.Equipment
import com.example.personalsport.data.local.entity.ProgressPhoto
import com.example.personalsport.data.local.entity.WorkoutLog

/**
 * Base de datos principal de Room de la aplicación PersonalSport.
 *
 * Contiene las entidades locales persistentes:
 * - [Equipment]: Inventario de equipamiento y pesas.
 * - [WorkoutLog]: Historial de sesiones de entrenamiento y mapas GPS.
 * - [BodyMetric]: Registro de peso, porcentaje de grasa y métricas corporales.
 * - [ProgressPhoto]: Galería de fotos de evolución corporal.
 * - [CompletedExerciseRecord]: Registro individual de ejercicios completados.
 *
 * Arquitectura de Rendimiento:
 * - Implementación Singleton con un único pool de conexiones SQLite optimizado.
 * - Migraciones seguras para prevenir pérdida de datos durante actualizaciones de esquema.
 */
@Database(
    entities = [
        Equipment::class,
        WorkoutLog::class,
        BodyMetric::class,
        ProgressPhoto::class,
        CompletedExerciseRecord::class
    ],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun equipmentDao(): EquipmentDao
    abstract fun workoutLogDao(): WorkoutLogDao
    abstract fun bodyMetricDao(): BodyMetricDao
    abstract fun progressPhotoDao(): ProgressPhotoDao
    abstract fun completedExerciseDao(): CompletedExerciseDao

    companion object {
        /**
         * Migración de esquema versión 4 a 5: Añade soporte para rutas y trazados GPS.
         */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE workout_logs ADD COLUMN routeGpsPoints TEXT DEFAULT NULL")
            }
        }

        @Volatile
        private var Instance: AppDatabase? = null

        /**
         * Obtiene la instancia única Singleton de [AppDatabase].
         *
         * @param context Contexto de la aplicación.
         * @return Instancia única de la base de datos Room.
         */
        fun getDatabase(context: Context): AppDatabase {
            return Instance ?: synchronized(this) {
                Room.databaseBuilder(context, AppDatabase::class.java, "personal_sport_db")
                    .addMigrations(MIGRATION_4_5)
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { Instance = it }
            }
        }
    }
}
