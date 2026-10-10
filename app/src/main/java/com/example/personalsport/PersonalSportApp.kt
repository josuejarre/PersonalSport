package com.example.personalsport

import android.content.Context
import com.example.personalsport.ai.AutonomousAgentManager
import com.example.personalsport.data.local.AppDatabase
import com.example.personalsport.data.repository.BodyMetricRepository
import com.example.personalsport.data.repository.CompletedExerciseRepository
import com.example.personalsport.data.repository.EquipmentRepository
import com.example.personalsport.data.repository.ProgressPhotoRepository
import com.example.personalsport.data.repository.UserProfileRepository
import com.example.personalsport.data.repository.WorkoutLogRepository
import com.example.personalsport.audio.AudioCoachManager
import com.example.personalsport.export.PerformanceReportExporter
import com.example.personalsport.gamification.GamificationManager
import com.example.personalsport.health.HealthConnectManager
import com.example.personalsport.health.HealthSyncManager
import com.example.personalsport.health.HuaweiDeviceSyncManager
import com.example.personalsport.health.MultiBrandSyncManager
import com.example.personalsport.music.CadenceMusicSyncManager

import coil.Coil
import com.example.personalsport.util.AppCoilImageLoader

/**
 * Clase principal de la Aplicación PersonalSport.
 * Gestiona el ciclo de vida de los componentes Singleton, repositorios de datos,
 * bases de datos Room, sincronizadores de salud y la configuración global de Coil.
 */
class PersonalSportApp private constructor(context: Context) {
    private val appContext = context.applicationContext

    init {
        // Inicializa y registra la instancia Singleton de Coil ImageLoader en toda la app
        Coil.setImageLoader(AppCoilImageLoader.get(appContext))
    }

    val database by lazy { AppDatabase.getDatabase(appContext) }

    val bodyMetricRepository by lazy { BodyMetricRepository(database.bodyMetricDao()) }
    val equipmentRepository by lazy { EquipmentRepository(database.equipmentDao()) }
    val progressPhotoRepository by lazy { ProgressPhotoRepository(database.progressPhotoDao()) }
    val workoutLogRepository by lazy { WorkoutLogRepository(database.workoutLogDao()) }
    val completedExerciseRepository by lazy { CompletedExerciseRepository(database.completedExerciseDao()) }
    val userProfileRepository by lazy { UserProfileRepository(appContext) }

    val healthConnectManager by lazy { HealthConnectManager(appContext) }
    val healthSyncManager by lazy { HealthSyncManager(healthConnectManager, database.workoutLogDao(), database.bodyMetricDao()) }
    val huaweiDeviceSyncManager by lazy { HuaweiDeviceSyncManager(appContext, healthConnectManager, database.workoutLogDao(), database.bodyMetricDao()) }
    val multiBrandSyncManager by lazy { MultiBrandSyncManager(appContext, healthConnectManager, database.workoutLogDao(), database.bodyMetricDao()) }
    val audioCoachManager by lazy { AudioCoachManager(appContext) }
    val gamificationManager by lazy { GamificationManager(appContext) }
    val performanceReportExporter by lazy { PerformanceReportExporter(appContext) }
    val cadenceMusicSyncManager by lazy { CadenceMusicSyncManager(appContext) }

    val autonomousAgentManager by lazy {
        AutonomousAgentManager(
            context = appContext,
            bodyMetricDao = database.bodyMetricDao(),
            workoutLogDao = database.workoutLogDao(),
            userProfileRepository = userProfileRepository
        )
    }

    companion object {
        @Volatile
        private var INSTANCE: PersonalSportApp? = null

        fun getInstance(context: Context): PersonalSportApp {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: PersonalSportApp(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
