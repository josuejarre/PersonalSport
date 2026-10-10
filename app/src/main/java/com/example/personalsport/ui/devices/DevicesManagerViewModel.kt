package com.example.personalsport.ui.devices

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.personalsport.ai.AutonomousAgentManager
import com.example.personalsport.health.BrandDevice
import com.example.personalsport.health.DeviceCategory
import com.example.personalsport.health.HuaweiDeviceSyncManager
import com.example.personalsport.health.MultiBrandSyncManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Immutable
data class LinkedDevice(
    val id: String,
    val name: String,
    val subtitle: String,
    val brandDevice: BrandDevice?,
    val categoryName: String,
    val metricsProvided: List<String>,
    val isPrimary: Boolean = false,
    val lastSyncTimestamp: Long = System.currentTimeMillis() - 300000L
)

@Immutable
data class DevicesManagerUiState(
    val linkedDevices: List<LinkedDevice> = emptyList(),
    val availableBrandsToPair: List<BrandDevice> = BrandDevice.entries,
    val isSyncingAll: Boolean = false,
    val syncingDeviceId: String? = null,
    val lastGlobalSyncTimestamp: Long = System.currentTimeMillis() - 300000L,
    val syncMessage: String? = null,
    val isPairingModalOpen: Boolean = false,
    val showHuaweiAssistant: Boolean = false,
    val showMultiBrandAssistant: Boolean = false,
    val selectedBrandForAssistant: BrandDevice = BrandDevice.HUAWEI_WATCH
)

class DevicesManagerViewModel(
    private val multiBrandSyncManager: MultiBrandSyncManager? = null,
    private val huaweiDeviceSyncManager: HuaweiDeviceSyncManager? = null,
    private val autonomousAgentManager: AutonomousAgentManager? = null,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    private val _uiState = MutableStateFlow(DevicesManagerUiState())
    val uiState: StateFlow<DevicesManagerUiState> = _uiState.asStateFlow()

    private val defaultLinkedDevices = listOf(
        LinkedDevice(
            id = "huawei_gt9c6",
            name = "Huawei Watch GT (GT-9c6)",
            subtitle = "Huawei Health v14.0.8.310 · Sincronización Directa & Health Connect",
            brandDevice = BrandDevice.HUAWEI_WATCH,
            categoryName = "Reloj Inteligente",
            metricsProvided = listOf("Carrera 5K/10K", "Natación (SWOLF 36)", "Gym (FC Máx 164)", "Sueño & SpO2"),
            isPrimary = true
        ),
        LinkedDevice(
            id = "tanita_health",
            name = "Báscula Tanita InnerScan Dual",
            subtitle = "Tanita Health Planet · Bioimpedancia Médica de Precisión",
            brandDevice = BrandDevice.TANITA_SCALE,
            categoryName = "Báscula Inteligente",
            metricsProvided = listOf("Peso (75.0 kg)", "% Grasa (14.9%)", "Masa Ósea", "Grasa Visceral Lvl 3"),
            isPrimary = false
        )
    )

    init {
        _uiState.update { currentState ->
            currentState.copy(
                linkedDevices = defaultLinkedDevices,
                availableBrandsToPair = BrandDevice.entries.filter { brand ->
                    defaultLinkedDevices.none { it.brandDevice == brand }
                }
            )
        }
    }

    fun openPairingModal() {
        _uiState.update { it.copy(isPairingModalOpen = true) }
    }

    fun closePairingModal() {
        _uiState.update { it.copy(isPairingModalOpen = false) }
    }

    fun linkNewDevice(brand: BrandDevice) {
        val existing = _uiState.value.linkedDevices.find { it.brandDevice == brand }
        if (existing != null) {
            _uiState.update {
                it.copy(
                    isPairingModalOpen = false,
                    syncMessage = "El dispositivo ${brand.displayName} ya está en tus dispositivos vinculados."
                )
            }
            return
        }

        val categoryName = when (brand.category) {
            DeviceCategory.WATCH -> "Reloj Inteligente"
            DeviceCategory.SCALE -> "Báscula Inteligente"
        }

        val metrics = when (brand) {
            BrandDevice.HUAWEI_WATCH -> listOf("Carrera 5K/10K", "Natación (SWOLF 36)", "Gym (FC Máx 164)", "Sueño & SpO2")
            BrandDevice.AMAZFIT -> listOf("Pasos & Distancia", "FC Continua", "Calorías Activas", "Sueño")
            BrandDevice.XIAOMI_WATCH -> listOf("Pasos Diarios", "Entrenamientos", "Estrés & SpO2", "FC Promedio")
            BrandDevice.GARMIN -> listOf("Carrera/Ciclismo", "VO2Max & Carga", "Body Battery", "Sueño Profundo")
            BrandDevice.FITBIT -> listOf("Entrenamientos", "Minutos en Zona", "FC Reposo", "Registro Peso")
            BrandDevice.XIAOMI_SCALE -> listOf("Peso (72.5 kg)", "% Grasa (16.2%)", "Masa Muscular", "IMC (22.8)")
            BrandDevice.HUAWEI_SCALE -> listOf("Peso (74.8 kg)", "% Grasa (15.8%)", "Masa Muscular", "Grasa Visceral Lvl 5")
            BrandDevice.HONOR_SCALE -> listOf("Peso (73.2 kg)", "% Grasa (16.5%)", "Masa Muscular", "Agua Corporal")
            BrandDevice.TANITA_SCALE -> listOf("Peso (75.0 kg)", "% Grasa (14.9%)", "Masa Ósea", "Grasa Visceral Lvl 3")
        }

        val newDevice = LinkedDevice(
            id = "${brand.id}_${System.currentTimeMillis()}",
            name = brand.displayName,
            subtitle = "${brand.companionAppName} · Conexión Directa & Health Connect",
            brandDevice = brand,
            categoryName = categoryName,
            metricsProvided = metrics,
            isPrimary = false,
            lastSyncTimestamp = System.currentTimeMillis()
        )

        _uiState.update { currentState ->
            val updatedList = currentState.linkedDevices + newDevice
            currentState.copy(
                linkedDevices = updatedList,
                availableBrandsToPair = BrandDevice.entries.filter { b ->
                    updatedList.none { it.brandDevice == b }
                },
                isPairingModalOpen = false,
                syncMessage = "¡Dispositivo ${brand.displayName} vinculado con éxito!"
            )
        }
    }

    fun unlinkDevice(deviceId: String) {
        val deviceToUnlink = _uiState.value.linkedDevices.find { it.id == deviceId }
        val deviceName = deviceToUnlink?.name ?: "Dispositivo"

        _uiState.update { currentState ->
            val updatedList = currentState.linkedDevices.filter { it.id != deviceId }
            currentState.copy(
                linkedDevices = updatedList,
                availableBrandsToPair = BrandDevice.entries.filter { b ->
                    updatedList.none { it.brandDevice == b }
                },
                syncMessage = "Dispositivo $deviceName desvinculado."
            )
        }
    }

    fun syncAllDevices() {
        viewModelScope.launch(ioDispatcher) {
            _uiState.update { it.copy(isSyncingAll = true) }
            try {
                huaweiDeviceSyncManager?.pairAndSyncHuaweiWatchGT9c6V14()

                val scaleDevices = _uiState.value.linkedDevices.mapNotNull { it.brandDevice }.filter {
                    it.category == DeviceCategory.SCALE
                }
                for (scale in scaleDevices) {
                    multiBrandSyncManager?.importDirectScaleData(scale)
                }

                val agentRes = autonomousAgentManager?.onDeviceSyncTriggered()
                val now = System.currentTimeMillis()

                _uiState.update { currentState ->
                    val updatedLinked = currentState.linkedDevices.map { dev ->
                        dev.copy(lastSyncTimestamp = now)
                    }
                    val agentMsg = agentRes?.message ?: ""
                    val msg = "¡Sincronización Completada! Todos los dispositivos vinculados fueron actualizados.\n$agentMsg".trim()
                    currentState.copy(
                        linkedDevices = updatedLinked,
                        lastGlobalSyncTimestamp = now,
                        isSyncingAll = false,
                        syncMessage = msg
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSyncingAll = false,
                        syncMessage = "Error en la sincronización: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    fun syncSingleDevice(device: LinkedDevice) {
        viewModelScope.launch(ioDispatcher) {
            _uiState.update { it.copy(syncingDeviceId = device.id) }
            val now = System.currentTimeMillis()
            var resultMessage = "Métricas de ${device.name} actualizadas correctamente."

            try {
                if (device.brandDevice == BrandDevice.HUAWEI_WATCH && huaweiDeviceSyncManager != null) {
                    val res = huaweiDeviceSyncManager.pairAndSyncHuaweiWatchGT9c6V14()
                    resultMessage = res.message
                } else if (device.brandDevice != null && multiBrandSyncManager != null) {
                    if (device.brandDevice.category == DeviceCategory.SCALE) {
                        val res = multiBrandSyncManager.importDirectScaleData(device.brandDevice)
                        resultMessage = res.message
                    } else {
                        val res = multiBrandSyncManager.importDirectWatchData(device.brandDevice)
                        resultMessage = res.message
                    }
                }
                autonomousAgentManager?.onDeviceSyncTriggered()
            } catch (e: Exception) {
                resultMessage = "Error al sincronizar ${device.name}: ${e.localizedMessage}"
            } finally {
                _uiState.update { currentState ->
                    val updatedList = currentState.linkedDevices.map { d ->
                        if (d.id == device.id) d.copy(lastSyncTimestamp = now) else d
                    }
                    currentState.copy(
                        linkedDevices = updatedList,
                        syncingDeviceId = null,
                        syncMessage = resultMessage
                    )
                }
            }
        }
    }

    fun openHuaweiAssistant() {
        _uiState.update { it.copy(showHuaweiAssistant = true) }
    }

    fun closeHuaweiAssistant() {
        _uiState.update { it.copy(showHuaweiAssistant = false) }
    }

    fun openMultiBrandAssistant(brand: BrandDevice) {
        _uiState.update {
            it.copy(
                selectedBrandForAssistant = brand,
                showMultiBrandAssistant = true
            )
        }
    }

    fun closeMultiBrandAssistant() {
        _uiState.update { it.copy(showMultiBrandAssistant = false) }
    }

    fun clearSyncMessage() {
        _uiState.update { it.copy(syncMessage = null) }
    }

    class Factory(
        private val multiBrandSyncManager: MultiBrandSyncManager,
        private val huaweiDeviceSyncManager: HuaweiDeviceSyncManager,
        private val autonomousAgentManager: AutonomousAgentManager,
        private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(DevicesManagerViewModel::class.java)) {
                return DevicesManagerViewModel(
                    multiBrandSyncManager,
                    huaweiDeviceSyncManager,
                    autonomousAgentManager,
                    ioDispatcher
                ) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
