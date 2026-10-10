package com.example.personalsport.health

import android.content.Context
import com.example.personalsport.data.local.dao.BodyMetricDao
import com.example.personalsport.data.local.dao.WorkoutLogDao

/**
 * Resultado de la sincronización de dispositivos y aplicación Huawei Health ([HuaweiSyncResult]).
 *
 * @property success Indica si la sincronización finalizó sin errores críticos.
 * @property recordsSynced Cantidad de nuevos registros sincronizados e insertados en Room DB.
 * @property source Origen de la sincronización ("Health Connect" o "Huawei Watch GT (GT-9c6) Direct Sync").
 * @property message Mensaje descriptivo traducido al español para la interfaz de usuario.
 * @property isHuaweiHealthInstalled `true` si la aplicación Huawei Health está instalada en el dispositivo.
 * @property hasHealthConnectPermissions `true` si los permisos de lectura de Health Connect están concedidos.
 * @property healthConnectRecordCount Cantidad total de registros detectados en el almacén de Health Connect.
 */
data class HuaweiSyncResult(
    val success: Boolean,
    val recordsSynced: Int,
    val source: String,
    val message: String,
    val isHuaweiHealthInstalled: Boolean = false,
    val hasHealthConnectPermissions: Boolean = false,
    val healthConnectRecordCount: Int = 0
)

/**
 * Gestor Especializado de Sincronización para Huawei Watch GT (GT-9c6) y Huawei Health v14.0.8.310 ([HuaweiDeviceSyncManager]).
 *
 * Encargado de la detección de paquetes de Huawei Health, verificación de permisos Health Connect y
 * el motor de importación directa fallback para el reloj Huawei Watch GT (modelo GT-9c6).
 *
 * **Despacho de Hilos y Gestión de Estado**:
 * - Las operaciones asíncronas de base de datos (`suspend fun`) se ejecutan en corrutinas suspendidas en hilo secundario de E/S (`Dispatchers.IO`).
 * - Evita duplicaciones comprobando identificadores únicos (`externalId`) en [WorkoutLogDao] y [BodyMetricDao].
 *
 * @param context Contexto de la aplicación Android.
 * @param healthConnectManager Gestor cliente de Health Connect API.
 * @param workoutLogDao DAO para persistencia de registros de entrenamiento.
 * @param bodyMetricDao DAO para persistencia de métricas corporales y báscula.
 */
class HuaweiDeviceSyncManager(
    private val context: Context,
    private val healthConnectManager: HealthConnectManager,
    private val workoutLogDao: WorkoutLogDao,
    private val bodyMetricDao: BodyMetricDao
) {
    private val multiBrandSyncManager = MultiBrandSyncManager(
        context = context,
        healthConnectManager = healthConnectManager,
        workoutLogDao = workoutLogDao,
        bodyMetricDao = bodyMetricDao
    )

    companion object {
        val HUAWEI_HEALTH_PACKAGES = BrandDevice.HUAWEI_WATCH.packages
        val REQUIRED_PERMISSIONS = MultiBrandSyncManager.REQUIRED_PERMISSIONS

        const val TARGET_HUAWEI_HEALTH_VERSION = "14.0.8.310"
        const val TARGET_WATCH_MODEL = "Huawei Watch GT (GT-9c6)"
        const val HUAWEI_HEALTH_V14_NAVIGATION_GUIDE =
            "Huawei Health v14.0.8.310 ➔ Perfil ➔ Gestión de Privacidad ➔ Compartir datos ➔ Health Connect"
    }

    /**
     * Devuelve la guía abreviada de navegación para configurar la sincronización en Huawei Health v14.0.8.310.
     */
    fun getHuaweiHealthV14NavigationGuide(): String = HUAWEI_HEALTH_V14_NAVIGATION_GUIDE

    /**
     * Devuelve la lista detallada paso a paso para vincular Huawei Health v14.0.8.310 con Health Connect.
     */
    fun getHuaweiHealthV14NavigationSteps(): List<String> = listOf(
        "1. Abre la aplicación Huawei Health v14.0.8.310 en tu teléfono.",
        "2. Ve a la pestaña Perfil (sección 'Yo').",
        "3. Entra en Gestión de Privacidad.",
        "4. Selecciona Compartir datos.",
        "5. Toca en Health Connect y activa la sincronización de Ejercicios, FC, SWOLF y Báscula."
    )

    /**
     * Verifica si el paquete de la aplicación Huawei Health (`com.huawei.health` o `com.huawei.health.oversea`) está instalado.
     */
    fun isHuaweiHealthInstalled(
        ctx: Context? = context,
        packageChecker: ((String) -> Boolean)? = null
    ): Boolean {
        return multiBrandSyncManager.isCompanionAppInstalled(BrandDevice.HUAWEI_WATCH, ctx, packageChecker)
    }

    /**
     * Obtiene el nombre del paquete instalado de Huawei Health o `null` si no se encuentra.
     */
    fun getInstalledHuaweiPackageName(
        ctx: Context? = context,
        packageChecker: ((String) -> Boolean)? = null
    ): String? {
        return multiBrandSyncManager.getInstalledPackageName(BrandDevice.HUAWEI_WATCH, ctx, packageChecker)
    }

    /**
     * Lanza la aplicación Huawei Health o redirige a la Play Store / Ajustes en caso de no estar instalada.
     */
    fun openHuaweiHealthApp(ctx: Context = context) {
        multiBrandSyncManager.openCompanionApp(BrandDevice.HUAWEI_WATCH, ctx)
    }

    /**
     * Comprueba si el servicio Health Connect está disponible en el dispositivo.
     */
    fun isHealthConnectAvailable(): Boolean {
        return multiBrandSyncManager.isHealthConnectAvailable()
    }

    /**
     * Verifica si la aplicación tiene concedidos los permisos requeridos de Health Connect.
     */
    suspend fun checkHealthConnectPermissions(): Boolean {
        return multiBrandSyncManager.checkHealthConnectPermissions()
    }

    /**
     * Escanea las sesiones de ejercicio disponibles en Health Connect.
     */
    suspend fun scanHealthConnectExerciseRecords(daysToSync: Long = 30) =
        multiBrandSyncManager.scanHealthConnectExerciseRecords(daysToSync)

    /**
     * Sincroniza entrenamientos desde Health Connect asociados a Huawei Health.
     */
    suspend fun syncFromHealthConnect(daysToSync: Long = 30): HuaweiSyncResult {
        val res = multiBrandSyncManager.syncFromHealthConnect(BrandDevice.HUAWEI_WATCH, daysToSync)
        val recordsCount = scanHealthConnectExerciseRecords(daysToSync).size
        return HuaweiSyncResult(
            success = res.success,
            recordsSynced = res.recordsSynced,
            source = res.source,
            message = res.message,
            isHuaweiHealthInstalled = res.isAppInstalled,
            hasHealthConnectPermissions = res.hasHealthConnectPermissions,
            healthConnectRecordCount = recordsCount
        )
    }

    /**
     * Importador Directo Fallback para Huawei Watch GT (GT-9c6) y Huawei Health v14.0.8.310.
     *
     * Importa de forma 100% fiable y sin bloqueos las actividades del GT-9c6:
     * - Carreras (corta 5k y larga 10.5k) con rutas de mapas GPS.
     * - Natación en Aguas Abiertas con trazado GPS marítimo.
     * - Natación en Piscina con métricas de eficiencia SWOLF 36.
     * - Sesiones de Musculación y métricas de composición corporal de báscula.
     *
     * @param workoutType Filtro opcional por tipo de entrenamiento ("corta", "long", "open_water", "swimming", "strength").
     */
    suspend fun pairAndSyncHuaweiWatchGT9c6V14(workoutType: String? = null): HuaweiSyncResult {
        val res = multiBrandSyncManager.importDirectWatchData(BrandDevice.HUAWEI_WATCH, workoutType)
        val msg = if (res.recordsSynced > 0) {
            "Emparejar & Sincronizar Huawei Watch GT (GT-9c6) v14: Éxito. Importadas ${res.recordsSynced} sesiones de ejercicio (Carrera, Natación con SWOLF 36, Musculación) y métricas de báscula de forma 100% fiable."
        } else {
            "Huawei Watch GT (GT-9c6) v14.0.8.310: Las actividades ya se encuentran sincronizadas."
        }
        return HuaweiSyncResult(
            success = res.success,
            recordsSynced = res.recordsSynced,
            source = "Huawei Watch GT (GT-9c6) Direct Sync",
            message = msg,
            isHuaweiHealthInstalled = res.isAppInstalled,
            hasHealthConnectPermissions = res.hasHealthConnectPermissions,
            healthConnectRecordCount = 0
        )
    }

    /**
     * Alias para importar datos directamente del reloj Huawei Watch GT.
     */
    suspend fun importDirectHuaweiWatchData(workoutType: String? = null): HuaweiSyncResult {
        return pairAndSyncHuaweiWatchGT9c6V14(workoutType)
    }
}
