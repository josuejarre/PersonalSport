package com.example.personalsport.ui.health

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import com.example.personalsport.ui.components.RouteMapPreviewThumbnail
import com.example.personalsport.ui.theme.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsBike
import androidx.compose.material.icons.automirrored.rounded.DirectionsRun
import androidx.compose.material.icons.automirrored.rounded.Label
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.personalsport.PersonalSportApp
import com.example.personalsport.data.local.entity.WorkoutLog
import com.example.personalsport.health.BrandDevice
import com.example.personalsport.health.MultiBrandSyncManager
import com.example.personalsport.health.MultiBrandSyncResult
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthSyncScreen(
    onOpenDrawer: () -> Unit = {},
    onOpenProfile: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val app = PersonalSportApp.getInstance(context)
    val multiBrandSyncManager = remember { app.multiBrandSyncManager }

    val coroutineScope = rememberCoroutineScope()
    val workoutLogDao = remember { app.database.workoutLogDao() }
    val workoutLogsFlow = remember { workoutLogDao.getAllWorkoutLogs() }
    val workoutLogs by workoutLogsFlow.collectAsState(initial = emptyList())

    var showAssistantDialog by remember { mutableStateOf(false) }
    var selectedBrand by remember { mutableStateOf(BrandDevice.HUAWEI_WATCH) }
    var isSyncing by remember { mutableStateOf(false) }
    var syncMessage by remember { mutableStateOf<String?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(syncMessage) {
        syncMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            syncMessage = null
        }
    }

    fun handleSyncWearableData() {
        coroutineScope.launch {
            isSyncing = true
            try {
                val hasPerms = multiBrandSyncManager.checkHealthConnectPermissions()
                val records = multiBrandSyncManager.scanHealthConnectExerciseRecords()
                
                if (!hasPerms || records.isEmpty()) {
                    showAssistantDialog = true
                } else {
                    val result = multiBrandSyncManager.syncFromHealthConnect(selectedBrand)
                    syncMessage = result.message
                }
            } catch (e: Exception) {
                syncMessage = "Error al sincronizar: ${e.localizedMessage}"
                showAssistantDialog = true
            } finally {
                isSyncing = false
            }
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Watch,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                "Multi-Brand Sync System",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Smartwatches & Smart Scales via Health Connect",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Rounded.Menu, contentDescription = "Menú")
                    }
                },
                actions = {
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
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
            // Main Sync Banner & Assistant Launch Button
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.secondary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Rounded.Sync, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondary)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Multi-Brand Wearable & Scale Sync",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    "Sincroniza Huawei GT, Amazfit (Zepp), Xiaomi, Garmin, Fitbit, Tanita y Honor",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { handleSyncWearableData() },
                                enabled = !isSyncing,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                if (isSyncing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        color = MaterialTheme.colorScheme.onSecondary,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(Icons.Rounded.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Sync Wearable Data", fontWeight = FontWeight.Bold)
                                }
                            }

                            OutlinedButton(
                                onClick = { showAssistantDialog = true },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Rounded.Build, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Asistente Multimarca")
                            }
                        }
                    }
                }
            }

            // Direct Scale Import Section (Fallback)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Rounded.Scale,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                "Sincronizar Básculas Inteligentes",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }

                        Text(
                            "Importa Peso, Porcentaje de Grasa, Masa Muscular, Grasa Visceral e IMC desde Xiaomi, Huawei, Honor o Tanita Smart Scales.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.9f)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            QuickImportCard(
                                title = "Xiaomi Scale",
                                subtitle = "72.5kg · 16.2% Grasa",
                                icon = Icons.Rounded.Scale,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    coroutineScope.launch {
                                        val res = multiBrandSyncManager.importDirectScaleData(BrandDevice.XIAOMI_SCALE)
                                        syncMessage = res.message
                                        Toast.makeText(context, res.message, Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                            QuickImportCard(
                                title = "Huawei Scale",
                                subtitle = "74.8kg · 15.8% Grasa",
                                icon = Icons.Rounded.Scale,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    coroutineScope.launch {
                                        val res = multiBrandSyncManager.importDirectScaleData(BrandDevice.HUAWEI_SCALE)
                                        syncMessage = res.message
                                        Toast.makeText(context, res.message, Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            QuickImportCard(
                                title = "Honor Scale 2",
                                subtitle = "73.2kg · 16.5% Grasa",
                                icon = Icons.Rounded.Scale,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    coroutineScope.launch {
                                        val res = multiBrandSyncManager.importDirectScaleData(BrandDevice.HONOR_SCALE)
                                        syncMessage = res.message
                                        Toast.makeText(context, res.message, Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                            QuickImportCard(
                                title = "Tanita Scale",
                                subtitle = "75.0kg · 14.9% Grasa",
                                icon = Icons.Rounded.Scale,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    coroutineScope.launch {
                                        val res = multiBrandSyncManager.importDirectScaleData(BrandDevice.TANITA_SCALE)
                                        syncMessage = res.message
                                        Toast.makeText(context, res.message, Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // Direct Watch Data Import Section (Fallback)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Rounded.DownloadForOffline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                "Importación Directa Relojes",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }

                        Text(
                            "Importa actividades (Carrera corta/larga, Natación y Musculación) desde Huawei Watch GT, Amazfit, Xiaomi, Garmin o Fitbit.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            QuickImportCard(
                                title = "Huawei GT",
                                subtitle = "Carrera, Natación, Gym",
                                icon = Icons.AutoMirrored.Rounded.DirectionsRun,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    coroutineScope.launch {
                                        val res = multiBrandSyncManager.importDirectWatchData(BrandDevice.HUAWEI_WATCH)
                                        syncMessage = res.message
                                    }
                                }
                            )
                            QuickImportCard(
                                title = "Amazfit Zepp",
                                subtitle = "Carrera, Natación, Gym",
                                icon = Icons.AutoMirrored.Rounded.DirectionsRun,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    coroutineScope.launch {
                                        val res = multiBrandSyncManager.importDirectWatchData(BrandDevice.AMAZFIT)
                                        syncMessage = res.message
                                    }
                                }
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            QuickImportCard(
                                title = "Garmin Connect",
                                subtitle = "Carrera, Natación, Gym",
                                icon = Icons.AutoMirrored.Rounded.DirectionsBike,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    coroutineScope.launch {
                                        val res = multiBrandSyncManager.importDirectWatchData(BrandDevice.GARMIN)
                                        syncMessage = res.message
                                    }
                                }
                            )
                            QuickImportCard(
                                title = "Fitbit App",
                                subtitle = "Carrera, Natación, Gym",
                                icon = Icons.Rounded.FitnessCenter,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    coroutineScope.launch {
                                        val res = multiBrandSyncManager.importDirectWatchData(BrandDevice.FITBIT)
                                        syncMessage = res.message
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // Synced Logs Header
            item {
                Text(
                    text = "Historial de Actividades Sincronizadas",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            val syncedLogs = workoutLogs.filter { log ->
                log.source.contains("Huawei", ignoreCase = true) ||
                        log.source.contains("Amazfit", ignoreCase = true) ||
                        log.source.contains("Zepp", ignoreCase = true) ||
                        log.source.contains("Xiaomi", ignoreCase = true) ||
                        log.source.contains("Garmin", ignoreCase = true) ||
                        log.source.contains("Fitbit", ignoreCase = true) ||
                        log.source.contains("HealthConnect", ignoreCase = true) ||
                        log.source.contains("Connect", ignoreCase = true)
            }

            if (syncedLogs.isEmpty()) {
                item {
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
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Rounded.Watch, contentDescription = null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.outline)
                            Text("No hay actividades registradas aún", style = MaterialTheme.typography.bodyLarge)
                            Text("Pulsa 'Sync Wearable Data' o 'Importar Directo' para registrar entrenamientos.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                        }
                    }
                }
            } else {
                items(syncedLogs, key = { it.id }) { log ->
                    SyncedLogCard(log = log)
                }
            }
        }
    }
}

    if (showAssistantDialog) {
        MultiBrandSyncAssistantDialog(
            multiBrandSyncManager = multiBrandSyncManager,
            initialBrand = selectedBrand,
            onDismiss = { showAssistantDialog = false },
            onSyncComplete = { res: MultiBrandSyncResult ->
                syncMessage = res.message
            }
        )
    }
}

@Composable
private fun QuickImportCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedCard(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Column {
                Text(title, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun SyncedLogCard(log: WorkoutLog) {
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy · HH:mm", Locale.getDefault()) }
    val dateStr = dateFormat.format(Date(log.date))

    val isOpenWaterSwim = log.workoutName.contains("aguas abiertas", ignoreCase = true) ||
            log.workoutName.contains("open water", ignoreCase = true) ||
            log.workoutName.contains("aguas_abiertas", ignoreCase = true)
    val isGpsRun = (log.workoutName.contains("carrera", ignoreCase = true) ||
            log.workoutName.contains("run", ignoreCase = true) ||
            log.workoutName.contains("atletismo", ignoreCase = true)) && !log.routeGpsPoints.isNullOrBlank()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (!log.routeGpsPoints.isNullOrBlank()) {
                    RouteMapPreviewThumbnail(
                        gpsPointsJson = log.routeGpsPoints,
                        width = 72.dp,
                        height = 56.dp
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when {
                                log.workoutName.contains("carrera", ignoreCase = true) || log.workoutName.contains("run", ignoreCase = true) -> Icons.AutoMirrored.Rounded.DirectionsRun
                                log.workoutName.contains("nataci", ignoreCase = true) || log.workoutName.contains("swim", ignoreCase = true) -> Icons.Rounded.Pool
                                log.workoutName.contains("bici", ignoreCase = true) || log.workoutName.contains("bike", ignoreCase = true) -> Icons.AutoMirrored.Rounded.DirectionsBike
                                else -> Icons.Rounded.FitnessCenter
                            },
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(log.workoutName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(dateStr, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    log.notes?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
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
