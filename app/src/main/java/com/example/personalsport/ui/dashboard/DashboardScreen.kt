package com.example.personalsport.ui.dashboard

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsBike
import androidx.compose.material.icons.automirrored.rounded.DirectionsRun
import androidx.compose.material.icons.automirrored.rounded.Label
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.window.core.layout.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.personalsport.PersonalSportApp
import com.example.personalsport.ai.AIRecoveryCountdown
import com.example.personalsport.ai.AiProgressInsight
import com.example.personalsport.ai.LocalAIFitnessEngine
import com.example.personalsport.ai.LocalAISleepAnalyzer
import com.example.personalsport.ai.RecoveryCountdownResult
import com.example.personalsport.ai.SleepAnalysisResult
import com.example.personalsport.ai.SleepQualityBadge
import com.example.personalsport.data.local.entity.WorkoutLog
import com.example.personalsport.data.model.GpsRouteUtils
import com.example.personalsport.export.PerformanceReportExporter
import com.example.personalsport.health.BrandDevice
import com.example.personalsport.health.DeviceCategory
import com.example.personalsport.health.HealthSyncManager
import com.example.personalsport.health.HuaweiDeviceSyncManager
import com.example.personalsport.health.MultiBrandSyncManager
import com.example.personalsport.ui.components.GoogleMapRouteWindow
import com.example.personalsport.ui.components.RouteMapPreviewThumbnail
import com.example.personalsport.ui.health.HydrationAndMuscleFatigueView
import com.example.personalsport.ui.health.MultiBrandSyncAssistantDialog
import com.example.personalsport.ui.music.CadenceMusicSyncCard
import com.example.personalsport.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Pantalla Principal del Cuadro de Mando Deportivo ([DashboardScreen]).
 *
 * Muestra el resumen integral del estado físico del atleta:
 * - Métricas de composición corporal (peso, grasa %, IMC).
 * - Historial de entrenamientos sincronizados con rutas de mapa GPS (carreras, natación con SWOLF 36, musculación).
 * - Cuenta regresiva de recuperación neurológica e IA y análisis de sueño.
 * - Tarjeta de sincronización de cadencia musical BPM.
 * - Módulo de hidratación, sudoración y mapa de calor de fatiga muscular.
 * - Exportación de informes PDF profesionales de rendimiento.
 * - Diálogos de asistencia de sincronización multimarca (Huawei Watch GT-9c6, Amazfit, Xiaomi, Garmin, Fitbit, Básculas).
 *
 * **Manejo de Estado y Despacho de Hilos**:
 * - Utiliza DashboardViewModel recolectando DashboardUiState como StateFlow en el hilo de UI.
 * - Lanza tareas asíncronas de sincronización y exportación en corrutinas asociadas a [rememberCoroutineScope].
 *
 * @param healthSyncManager Gestor de sincronización de salud de Health Connect.
 * @param huaweiDeviceSyncManager Gestor de dispositivos Huawei Watch GT y Huawei Health v14.
 * @param multiBrandSyncManager Gestor multimarca para Amazfit, Xiaomi, Garmin, Fitbit y básculas.
 * @param onNavigateToPlanner Callback para navegar a la pantalla de planificador de rutinas.
 * @param onNavigateToEquipment Callback para navegar al catálogo de equipamiento y gimnasios.
 * @param onNavigateToLog Callback para abrir la pantalla de registro manual.
 * @param onNavigateToProgress Callback para navegar a la galería de fotos de progreso 3D.
 * @param onNavigateToSupplements Callback para abrir el centro de suplementación.
 * @param onNavigateToTrainers Callback para navegar a la sección de entrenadores personales IA.
 * @param onNavigateToDiet Callback para abrir el planificador nutricional.
 * @param onOpenDrawer Callback para desplegar el menú lateral.
 * @param onOpenProfile Callback para abrir la ventana modal del perfil de usuario.
 * @param modifier Modificador de diseño Compose.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    healthSyncManager: HealthSyncManager,
    huaweiDeviceSyncManager: HuaweiDeviceSyncManager = PersonalSportApp.getInstance(LocalContext.current).huaweiDeviceSyncManager,
    multiBrandSyncManager: MultiBrandSyncManager = PersonalSportApp.getInstance(LocalContext.current).multiBrandSyncManager,
    onNavigateToPlanner: () -> Unit = {},
    onNavigateToEquipment: () -> Unit = {},
    onNavigateToLog: () -> Unit = {},
    onNavigateToProgress: () -> Unit = {},
    onNavigateToSupplements: () -> Unit = {},
    onNavigateToTrainers: () -> Unit = {},
    onNavigateToDiet: () -> Unit = {},
    onOpenDrawer: () -> Unit = {},
    onOpenProfile: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val app = PersonalSportApp.getInstance(context)
    val factory = remember { DashboardViewModelFactory(app.bodyMetricRepository, app.workoutLogRepository) }
    val viewModel: DashboardViewModel = viewModel(factory = factory)
    
    val state by viewModel.uiState.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    var showMetricDialog by remember { mutableStateOf(false) }
    var showAssistantDialog by remember { mutableStateOf(false) }
    var selectedAssistantBrand by remember { mutableStateOf(BrandDevice.HUAWEI_WATCH) }

    val adaptiveInfo = currentWindowAdaptiveInfo()
    val isExpandedWidth = adaptiveInfo.windowSizeClass.windowWidthSizeClass == WindowWidthSizeClass.EXPANDED

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.syncMessage) {
        state.syncMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSyncMessage()
        }
    }

    fun handleWearableSync() {
        coroutineScope.launch {
            viewModel.setSyncing(true)
            try {
                val hasPerms = multiBrandSyncManager.checkHealthConnectPermissions()
                val records = multiBrandSyncManager.scanHealthConnectExerciseRecords()
                if (!hasPerms || records.isEmpty()) {
                    selectedAssistantBrand = BrandDevice.HUAWEI_WATCH
                    showAssistantDialog = true
                } else {
                    val result = multiBrandSyncManager.syncFromHealthConnect(BrandDevice.HUAWEI_WATCH)
                    viewModel.setSyncMessage(result.message)
                }
            } catch (e: Exception) {
                selectedAssistantBrand = BrandDevice.HUAWEI_WATCH
                showAssistantDialog = true
            } finally {
                viewModel.setSyncing(false)
            }
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.FitnessCenter,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .size(28.dp)
                                .padding(end = 6.dp)
                        )
                        Column {
                            Text(
                                "PersonalSport",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Dashboard & Health Metrics",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Rounded.Menu, contentDescription = "Menu")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val exporter = PerformanceReportExporter(context)
                        val pdf = exporter.generateReportPdf()
                        exporter.openPdfReport(pdf)
                    }) {
                        Icon(Icons.Rounded.PictureAsPdf, contentDescription = "Exportar Informe PDF")
                    }
                    IconButton(onClick = {
                        selectedAssistantBrand = BrandDevice.HUAWEI_WATCH
                        showAssistantDialog = true
                    }) {
                        Icon(Icons.Rounded.Watch, contentDescription = "Asistente Multimarca")
                    }
                    IconButton(onClick = { showMetricDialog = true }) {
                        Icon(Icons.Rounded.MonitorWeight, contentDescription = "Update Weight")
                    }
                    IconButton(onClick = onOpenProfile) {
                        Icon(Icons.Rounded.AccountCircle, contentDescription = "Perfil")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.primary,
                    actionIconContentColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant)
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .perforatedMeshBackground()
        ) {
            if (isExpandedWidth) {
                // Wide layout for tablets
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        MetricsSection(
                            state = state,
                            onUpdateClick = { showMetricDialog = true }
                        )
                        state.recoveryCountdown?.let { recovery ->
                            AIRecoveryCountdownCard(recoveryResult = recovery)
                        }
                        state.sleepAnalysis?.let { sleep ->
                            SleepAnalysisCard(sleepResult = sleep)
                        }
                        state.aiInsight?.let { insight ->
                            AiBodyCompositionInsightsCard(
                                insight = insight,
                                postWorkoutRecommendation = state.postWorkoutRecommendation,
                                latestWorkoutLog = state.latestWorkoutLog
                            )
                        }
                        QuickActionsRow(
                            onNavigateToPlanner = onNavigateToPlanner,
                            onNavigateToEquipment = onNavigateToEquipment,
                            onNavigateToProgress = onNavigateToProgress,
                            onNavigateToLog = onNavigateToLog,
                            onNavigateToSupplements = onNavigateToSupplements,
                            onNavigateToTrainers = onNavigateToTrainers,
                            onNavigateToDiet = onNavigateToDiet
                        )
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CadenceMusicSyncCard()
                        HydrationAndMuscleFatigueView()
                        RecentWorkoutsSection(recentWorkouts = state.recentWorkouts)
                    }
                }
            } else {
                // Phone / Compact layout
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(vertical = 16.dp)
                ) {
                    item {
                        MetricsSection(
                            state = state,
                            onUpdateClick = { showMetricDialog = true }
                        )
                    }

                    item {
                        state.recoveryCountdown?.let { recovery ->
                            AIRecoveryCountdownCard(recoveryResult = recovery)
                        }
                    }

                    item {
                        state.sleepAnalysis?.let { sleep ->
                            SleepAnalysisCard(sleepResult = sleep)
                        }
                    }

                    item {
                        CadenceMusicSyncCard()
                    }

                    item {
                        HydrationAndMuscleFatigueView()
                    }

                    item {
                        state.aiInsight?.let { insight ->
                            AiBodyCompositionInsightsCard(
                                insight = insight,
                                postWorkoutRecommendation = state.postWorkoutRecommendation,
                                latestWorkoutLog = state.latestWorkoutLog
                            )
                        }
                    }

                    item {
                        QuickActionsRow(
                            onNavigateToPlanner = onNavigateToPlanner,
                            onNavigateToEquipment = onNavigateToEquipment,
                            onNavigateToProgress = onNavigateToProgress,
                            onNavigateToLog = onNavigateToLog,
                            onNavigateToSupplements = onNavigateToSupplements,
                            onNavigateToTrainers = onNavigateToTrainers,
                            onNavigateToDiet = onNavigateToDiet
                        )
                    }

                    item {
                        Text(
                            text = "Recent Activity",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    if (state.recentWorkouts.isEmpty()) {
                        item {
                            EmptyWorkoutsCard(onNavigateToLog = onNavigateToLog)
                        }
                    } else {
                        items(state.recentWorkouts, key = { it.id }) { log ->
                            WorkoutLogCard(log = log)
                        }
                    }
                }
            }
        }
    }

    if (showMetricDialog) {
        UpdateMetricDialog(
            currentWeight = state.latestWeight,
            currentBodyFat = state.latestBodyFat,
            onDismiss = { showMetricDialog = false },
            onConfirm = { weight, bodyFat ->
                viewModel.addQuickBodyMetric(weight, bodyFat)
                showMetricDialog = false
            }
        )
    }

    if (showAssistantDialog) {
        MultiBrandSyncAssistantDialog(
            multiBrandSyncManager = multiBrandSyncManager,
            initialBrand = selectedAssistantBrand,
            onDismiss = { showAssistantDialog = false },
            onSyncComplete = { res ->
                viewModel.setSyncMessage(res.message)
            }
        )
    }
}

@Composable
fun AiBodyCompositionInsightsCard(
    insight: AiProgressInsight,
    postWorkoutRecommendation: String? = null,
    latestWorkoutLog: WorkoutLog? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.tertiary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.AutoAwesome,
                        contentDescription = "AI Analytics",
                        tint = MaterialTheme.colorScheme.onTertiary
                    )
                }
                Text(
                    text = "AI Body Composition Insight",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }

            Text(
                text = insight.summary,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )

            // Badges for deltas if present
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                insight.fatChangeDeltaPct?.let { fatDelta ->
                    AssistChip(
                        onClick = {},
                        label = {
                            Text(
                                if (fatDelta <= 0) "Grasa: ${"%.1f".format(fatDelta)}%" else "Grasa: +${"%.1f".format(fatDelta)}%",
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        leadingIcon = {
                            Icon(
                                if (fatDelta <= 0) Icons.AutoMirrored.Rounded.TrendingDown else Icons.AutoMirrored.Rounded.TrendingUp,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    )
                }

                insight.weightChangeDeltaKg?.let { weightDelta ->
                    AssistChip(
                        onClick = {},
                        label = {
                            Text(
                                if (weightDelta <= 0) "Peso: ${"%.1f".format(weightDelta)} kg" else "Peso: +${"%.1f".format(weightDelta)} kg",
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        leadingIcon = {
                            Icon(
                                if (weightDelta <= 0) Icons.AutoMirrored.Rounded.TrendingDown else Icons.AutoMirrored.Rounded.TrendingUp,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    )
                }

                insight.currentBmi?.let { bmi ->
                    AssistChip(
                        onClick = {},
                        label = { Text("IMC: ${"%.1f".format(bmi)}", style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }

            Text(
                text = insight.recommendation,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.9f)
            )

            val recommendationText = postWorkoutRecommendation
                ?: LocalAIFitnessEngine.generatePostWorkoutRecommendation(latestWorkoutLog)

            HorizontalDivider(
                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.2f),
                thickness = 1.dp
            )
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.18f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.35f))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.tertiary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.FitnessCenter,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onTertiary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Text(
                            text = "Recomendación IA Post-Entrenamiento",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }

                    latestWorkoutLog?.let { workout ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Último entrenamiento: ${workout.workoutName} (${workout.durationMinutes} min)",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        }
                    }

                    Text(
                        text = recommendationText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        AssistChip(
                            onClick = {},
                            label = { Text("Nutrición", style = MaterialTheme.typography.labelSmall) },
                            leadingIcon = { Icon(Icons.Rounded.Restaurant, contentDescription = null, modifier = Modifier.size(12.dp)) }
                        )
                        AssistChip(
                            onClick = {},
                            label = { Text("Recuperación", style = MaterialTheme.typography.labelSmall) },
                            leadingIcon = { Icon(Icons.Rounded.WaterDrop, contentDescription = null, modifier = Modifier.size(12.dp)) }
                        )
                        AssistChip(
                            onClick = {},
                            label = { Text("Estiramientos", style = MaterialTheme.typography.labelSmall) },
                            leadingIcon = { Icon(Icons.Rounded.SelfImprovement, contentDescription = null, modifier = Modifier.size(12.dp)) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AiProgressInsightsCard(
    insight: AiProgressInsight,
    postWorkoutRecommendation: String? = null,
    latestWorkoutLog: WorkoutLog? = null
) {
    AiBodyCompositionInsightsCard(
        insight = insight,
        postWorkoutRecommendation = postWorkoutRecommendation,
        latestWorkoutLog = latestWorkoutLog
    )
}

private data class DashboardMetricData(
    val emoji: String,
    val label: String,
    val value: String
)

@Composable
fun MetricsSection(
    state: DashboardUiState,
    onUpdateClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkMetallicCard),
        border = BorderStroke(1.dp, ElectricSportOrange),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Métricas de Salud & Composición Corporal",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryWhite
                    )
                    Text(
                        text = "Sincronizadas con Wearables, Básculas & Sensores",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondaryPlatinum
                    )
                }
                TextButton(
                    onClick = onUpdateClick,
                    colors = ButtonDefaults.textButtonColors(contentColor = ElectricSportOrange)
                ) {
                    Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Actualizar", fontWeight = FontWeight.Bold)
                }
            }

            val exercisesVal = "${state.totalWorkoutsCount} (${state.totalDurationMinutes} min)"
            val pesoVal = if (state.latestWeight != null) "%.1f kg".format(state.latestWeight) else "-- kg"
            val grasaVal = if (state.latestBodyFat != null) "%.1f%%".format(state.latestBodyFat) else "-- %"
            val muscleVal = if (state.muscleMassKg != null) "%.1f kg".format(state.muscleMassKg) else "-- kg"
            val visceralVal = if (state.visceralFatLevel != null) "Nivel ${state.visceralFatLevel}" else "--"
            val boneVal = if (state.boneMassKg != null) "%.1f kg".format(state.boneMassKg) else "-- kg"
            val waterVal = if (state.bodyWaterPercentage != null) "%.1f%%".format(state.bodyWaterPercentage) else "-- %"
            val bmiVal = if (state.bmi != null) "%.1f".format(state.bmi) else "--"
            val restVal = state.sleepAnalysis?.let { "${it.sleepScore}%" }
                ?: state.sleepQuality?.split(" ")?.firstOrNull()?.takeIf { it.isNotEmpty() && it != "--" }
                ?: "--"

            val metrics = listOf(
                DashboardMetricData("🏋️", "Ejercicios", exercisesVal),
                DashboardMetricData("⚖️", "Peso", pesoVal),
                DashboardMetricData("📉", "Grasa Corp.", grasaVal),
                DashboardMetricData("💪", "M. Muscular", muscleVal),
                DashboardMetricData("🫀", "G. Visceral", visceralVal),
                DashboardMetricData("🦴", "M. Ósea", boneVal),
                DashboardMetricData("💧", "Agua Corp.", waterVal),
                DashboardMetricData("📊", "IMC", bmiVal),
                DashboardMetricData("😴", "Descanso", restVal)
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                metrics.chunked(2).forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowItems.forEach { item ->
                            HealthMetricRowItem(
                                emoji = item.emoji,
                                label = item.label,
                                value = item.value,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (rowItems.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HealthMetricRowItem(
    emoji: String,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = DarkSteelAccent,
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = emoji,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "$label: ",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondaryPlatinum,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                color = TextPrimaryWhite,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}

@Composable
fun QuickActionsRow(
    onNavigateToPlanner: () -> Unit,
    onNavigateToEquipment: () -> Unit,
    onNavigateToProgress: () -> Unit,
    onNavigateToLog: () -> Unit,
    onNavigateToSupplements: () -> Unit = {},
    onNavigateToTrainers: () -> Unit = {},
    onNavigateToDiet: () -> Unit = {}
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            OutlinedButton(
                onClick = onNavigateToPlanner,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
            ) {
                Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Planner", style = MaterialTheme.typography.labelSmall)
            }
            OutlinedButton(
                onClick = onNavigateToEquipment,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
            ) {
                Icon(Icons.Rounded.FitnessCenter, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Catalog", style = MaterialTheme.typography.labelSmall)
            }
            OutlinedButton(
                onClick = onNavigateToProgress,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
            ) {
                Icon(Icons.Rounded.PhotoCamera, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Photos", style = MaterialTheme.typography.labelSmall)
            }
            OutlinedButton(
                onClick = onNavigateToLog,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Log", style = MaterialTheme.typography.labelSmall)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Button(
                onClick = onNavigateToDiet,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
            ) {
                Icon(Icons.Rounded.Restaurant, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onTertiaryContainer)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Dietas", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onTertiaryContainer, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = onNavigateToSupplements,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Icon(Icons.Rounded.LocalPharmacy, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Suplementos", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = onNavigateToTrainers,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Icon(Icons.Rounded.PersonSearch, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Entrenadores", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSecondaryContainer, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun RecentWorkoutsSection(recentWorkouts: List<WorkoutLog>) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Recent Activity Logs",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        if (recentWorkouts.isEmpty()) {
            EmptyWorkoutsCard(onNavigateToLog = {})
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(recentWorkouts, key = { it.id }) { log ->
                    WorkoutLogCard(log = log)
                }
            }
        }
    }
}

@Composable
fun WorkoutLogCard(log: WorkoutLog) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy · HH:mm", Locale.getDefault()) }
    val dateStr = dateFormat.format(Date(log.date))

    val isOpenWaterSwim = log.workoutName.contains("aguas abiertas", ignoreCase = true) ||
            log.workoutName.contains("open water", ignoreCase = true) ||
            log.workoutName.contains("aguas_abiertas", ignoreCase = true)
    val isGpsRun = log.workoutName.contains("carrera", ignoreCase = true) ||
            log.workoutName.contains("run", ignoreCase = true) ||
            log.workoutName.contains("atletismo", ignoreCase = true) ||
            log.workoutName.contains("trote", ignoreCase = true) ||
            !log.routeGpsPoints.isNullOrBlank()

    val routeGpsJson = remember(log.routeGpsPoints, isOpenWaterSwim, isGpsRun) {
        if (!log.routeGpsPoints.isNullOrBlank()) {
            log.routeGpsPoints
        } else if (isOpenWaterSwim) {
            GpsRouteUtils.generateSampleOpenWaterSwimRouteJson()
        } else if (isGpsRun) {
            GpsRouteUtils.generateSampleRunRouteJson()
        } else null
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when {
                            log.workoutName.contains("run", ignoreCase = true) || log.workoutName.contains("corta", ignoreCase = true) || log.workoutName.contains("larga", ignoreCase = true) || log.workoutName.contains("carrera", ignoreCase = true) -> Icons.AutoMirrored.Rounded.DirectionsRun
                            log.workoutName.contains("swim", ignoreCase = true) || log.workoutName.contains("nataci", ignoreCase = true) -> Icons.Rounded.Pool
                            log.workoutName.contains("bike", ignoreCase = true) || log.workoutName.contains("cycl", ignoreCase = true) -> Icons.AutoMirrored.Rounded.DirectionsBike
                            else -> Icons.Rounded.FitnessCenter
                        },
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(log.workoutName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(dateStr, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (!log.notes.isNullOrBlank()) {
                        Text(log.notes, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f))
                    }
                }
            }

            if (!routeGpsJson.isNullOrBlank()) {
                GoogleMapRouteWindow(
                    routeGpsPoints = routeGpsJson,
                    durationMinutes = log.durationMinutes,
                    startTimestampMs = log.date,
                    height = 185.dp,
                    showMetricsOverlay = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isOpenWaterSwim) {
                    Surface(
                        color = Color(0xFF00695C).copy(alpha = 0.25f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFF004D40))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Pool,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = Color(0xFF80CBC4)
                            )
                            Text(
                                text = "Natación en Aguas Abiertas",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFE0F2F1)
                            )
                        }
                    }
                } else if (isGpsRun) {
                    Surface(
                        color = Color(0xFFE65100).copy(alpha = 0.25f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFFBF360C))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Map,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = Color(0xFFFFB74D)
                            )
                            Text(
                                text = "Carrera / Atletismo con Mapa GPS",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFFE0B2)
                            )
                        }
                    }
                } else if (!log.routeGpsPoints.isNullOrBlank()) {
                    Surface(
                        color = Color(0xFFE65100).copy(alpha = 0.25f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFFBF360C))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Map,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = Color(0xFFFFB74D)
                            )
                            Text(
                                text = "Ruta GPS",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFFE0B2)
                            )
                        }
                    }
                }

                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Timer,
                            contentDescription = "Duration",
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "${log.durationMinutes} min",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Label,
                            contentDescription = "Source",
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = log.source,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyWorkoutsCard(onNavigateToLog: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(Icons.Rounded.FitnessCenter, contentDescription = null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.outline)
            Text(
                text = "Sin actividades registradas. Sincroniza tus dispositivos o añade una sesión manual.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun UpdateMetricDialog(
    currentWeight: Float?,
    currentBodyFat: Float?,
    onDismiss: () -> Unit,
    onConfirm: (weight: Float, bodyFat: Float?) -> Unit
) {
    var weightInput by remember { mutableStateOf(currentWeight?.toString() ?: "") }
    var bodyFatInput by remember { mutableStateOf(currentBodyFat?.toString() ?: "") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Update Body Metrics") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = weightInput,
                    onValueChange = { weightInput = it },
                    label = { Text("Weight (kg)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = bodyFatInput,
                    onValueChange = { bodyFatInput = it },
                    label = { Text("Body Fat % (Optional)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                errorMsg?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val w = weightInput.toFloatOrNull()
                    val bf = bodyFatInput.toFloatOrNull()
                    if (w != null && w > 0) {
                        onConfirm(w, bf)
                    } else {
                        errorMsg = "Please enter a valid weight in kg"
                    }
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun SleepAnalysisCard(sleepResult: SleepAnalysisResult) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Rounded.Bedtime,
                            contentDescription = "Sleep Tracking",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Análisis de Sueño & Descanso IA",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Score IA: ${sleepResult.sleepScore}/100",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                val (badgeColor, textColor) = when (sleepResult.qualityBadge) {
                    SleepQualityBadge.EXCELENTE -> Color(0xFF2E7D32) to Color.White
                    SleepQualityBadge.BUENA -> Color(0xFF1976D2) to Color.White
                    SleepQualityBadge.REGULAR -> Color(0xFFF57C00) to Color.White
                    SleepQualityBadge.NECESITA_RECUPERACION -> Color(0xFFD32F2F) to Color.White
                }

                Surface(
                    color = badgeColor,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = sleepResult.qualityBadge.displayName,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val hoursInt = sleepResult.hoursSlept.toInt()
                val minutesInt = ((sleepResult.hoursSlept - hoursInt) * 60).toInt()
                val durationText = "${hoursInt}h ${minutesInt}m"

                StatMiniCard(
                    icon = Icons.Rounded.Schedule,
                    label = "Sueño Total",
                    value = durationText,
                    modifier = Modifier.weight(1f)
                )
                StatMiniCard(
                    icon = Icons.Rounded.Favorite,
                    label = "Pulsaciones Reposo",
                    value = "${sleepResult.restingHeartRateBpm} bpm",
                    modifier = Modifier.weight(1f)
                )
                StatMiniCard(
                    icon = Icons.Rounded.Bed,
                    label = "Profundo / REM",
                    value = "${sleepResult.deepSleepPercentage.toInt()}% / ${sleepResult.remSleepPercentage.toInt()}%",
                    modifier = Modifier.weight(1f)
                )
            }

            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Rounded.Restaurant,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = sleepResult.dietSupplementFeedback,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Recomendaciones IA de Descanso & Suplementación:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                sleepResult.recommendations.forEach { rec ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .size(16.dp)
                                .padding(top = 2.dp)
                        )
                        Text(
                            text = rec,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AIRecoveryCountdownCard(recoveryResult: RecoveryCountdownResult) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.secondary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Rounded.Timer,
                            contentDescription = "Recovery Timer",
                            tint = MaterialTheme.colorScheme.onSecondary
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Cuenta Atrás de Recuperación IA",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = "Recuperación Muscular & Fisiológica",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    color = MaterialTheme.colorScheme.secondary,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "${recoveryResult.muscleRecoveryPercentage}%",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondary,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = recoveryResult.formattedRemainingTime,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        color = if (recoveryResult.isFullyRecovered) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary
                    )

                    LinearProgressIndicator(
                        progress = { recoveryResult.muscleRecoveryPercentage / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = if (recoveryResult.isFullyRecovered) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.outlineVariant
                    )

                    Text(
                        text = recoveryResult.recommendationBadgeText,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Rounded.Watch,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(22.dp)
                    )
                    Column {
                        Text(
                            text = "Actividad No Registrada (Reloj / NEAT)",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = recoveryResult.neatAnalysisSummary,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.AutoMirrored.Rounded.DirectionsRun,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Próxima Ventana Óptima:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text = recoveryResult.recommendedTimeWindow,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Rounded.FitnessCenter,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Intensidad Sugerida:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text = recoveryResult.recommendedNextSessionIntensity,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
    }
}

@Composable
fun StatMiniCard(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
