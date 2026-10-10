package com.example.personalsport.ui.health

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.DirectionsRun
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.health.connect.client.PermissionController
import com.example.personalsport.health.BrandDevice
import com.example.personalsport.health.DeviceCategory
import com.example.personalsport.health.MultiBrandSyncManager
import com.example.personalsport.health.MultiBrandSyncResult
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultiBrandSyncAssistantDialog(
    multiBrandSyncManager: MultiBrandSyncManager,
    initialBrand: BrandDevice = BrandDevice.HUAWEI_WATCH,
    onDismiss: () -> Unit,
    onSyncComplete: (MultiBrandSyncResult) -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var selectedCategory by remember { mutableStateOf(initialBrand.category) }
    var selectedBrand by remember { mutableStateOf(initialBrand) }

    var isAppInstalled by remember {
        mutableStateOf(multiBrandSyncManager.isCompanionAppInstalled(selectedBrand, context))
    }
    var hasPermissions by remember { mutableStateOf(false) }
    var recordCount by remember { mutableIntStateOf(0) }
    var isScanning by remember { mutableStateOf(false) }
    var isImportingDirect by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var lastSyncResult by remember { mutableStateOf<MultiBrandSyncResult?>(null) }

    // Health Connect permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract()
    ) { _ ->
        coroutineScope.launch {
            hasPermissions = multiBrandSyncManager.checkHealthConnectPermissions()
            if (hasPermissions) {
                statusMessage = "¡Permisos de Health Connect concedidos correctamente!"
                val records = multiBrandSyncManager.scanHealthConnectExerciseRecords()
                recordCount = records.size
            } else {
                statusMessage = "Algunos permisos aún están pendientes."
            }
        }
    }

    // Check permissions and app status when selected brand changes
    LaunchedEffect(selectedBrand) {
        isAppInstalled = multiBrandSyncManager.isCompanionAppInstalled(selectedBrand, context)
        hasPermissions = multiBrandSyncManager.checkHealthConnectPermissions()
        if (hasPermissions) {
            val records = multiBrandSyncManager.scanHealthConnectExerciseRecords()
            recordCount = records.size
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
                .clip(RoundedCornerShape(28.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (selectedCategory == DeviceCategory.WATCH) Icons.Rounded.Watch else Icons.Rounded.Scale,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Asistente Multimarca Sync",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Relojes & Básculas Inteligentes",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        },
                        actions = {
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.Rounded.Close, contentDescription = "Cerrar")
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        )
                    )
                }
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Category Tab Segment (Relojes vs Básculas)
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TabRow(
                            selectedTabIndex = if (selectedCategory == DeviceCategory.WATCH) 0 else 1,
                            containerColor = Color.Transparent,
                            divider = {}
                        ) {
                            Tab(
                                selected = selectedCategory == DeviceCategory.WATCH,
                                onClick = {
                                    selectedCategory = DeviceCategory.WATCH
                                    selectedBrand = MultiBrandSyncManager.ALL_WATCHES.first()
                                },
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(Icons.Rounded.Watch, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Text("Relojes", fontWeight = FontWeight.Bold)
                                    }
                                }
                            )
                            Tab(
                                selected = selectedCategory == DeviceCategory.SCALE,
                                onClick = {
                                    selectedCategory = DeviceCategory.SCALE
                                    selectedBrand = MultiBrandSyncManager.ALL_SCALES.first()
                                },
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(Icons.Rounded.Scale, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Text("Básculas", fontWeight = FontWeight.Bold)
                                    }
                                }
                            )
                        }
                    }

                    // Brand Selection Horizontal Filter Chips
                    val brandsForCategory = if (selectedCategory == DeviceCategory.WATCH) {
                        MultiBrandSyncManager.ALL_WATCHES
                    } else {
                        MultiBrandSyncManager.ALL_SCALES
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        brandsForCategory.forEach { brand ->
                            FilterChip(
                                selected = selectedBrand == brand,
                                onClick = { selectedBrand = brand },
                                label = { Text(brand.displayName, fontWeight = FontWeight.SemiBold) },
                                leadingIcon = {
                                    val isInst = multiBrandSyncManager.isCompanionAppInstalled(brand, context)
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(if (isInst) Color(0xFF2E7D32) else MaterialTheme.colorScheme.outline)
                                    )
                                },
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }

                    // Status Message Banner
                    statusMessage?.let { msg ->
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Rounded.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = msg,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    // Diagnostic Cards (Companion App & Health Connect)
                    Text(
                        text = "1. Estado de Conexión & Requisitos",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    // Companion App Status Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isAppInstalled)
                                MaterialTheme.colorScheme.surfaceContainer
                            else
                                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = if (isAppInstalled) Icons.Rounded.CheckCircle else Icons.Rounded.Warning,
                                contentDescription = null,
                                tint = if (isAppInstalled) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(28.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "App de la Marca (${selectedBrand.companionAppName})",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (isAppInstalled)
                                        "🟢 App detectada (${selectedBrand.packages.firstOrNull()})"
                                    else
                                        "🔴 App no detectada. Se recomienda tener ${selectedBrand.companionAppName} instalada para emparejar tu dispositivo.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Button(
                                onClick = {
                                    multiBrandSyncManager.openCompanionApp(selectedBrand, context)
                                    isAppInstalled = multiBrandSyncManager.isCompanionAppInstalled(selectedBrand, context)
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                ),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text("Abrir App de la Marca", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }

                    // Health Connect Status Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (hasPermissions)
                                MaterialTheme.colorScheme.surfaceContainer
                            else
                                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = if (hasPermissions) Icons.Rounded.CheckCircle else Icons.Rounded.Lock,
                                contentDescription = null,
                                tint = if (hasPermissions) Color(0xFF2E7D32) else MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(28.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Permisos Health Connect",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (hasPermissions)
                                        "🟢 Permisos activos (Ejercicios, Pasos, Frecuencia Cardíaca, Peso, Grasa)"
                                    else
                                        "🔴 Permisos pendientes en Health Connect.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Button(
                                onClick = {
                                    permissionLauncher.launch(MultiBrandSyncManager.REQUIRED_PERMISSIONS)
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondary
                                ),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    if (hasPermissions) "Revisar" else "Conceder",
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }

                    // Step-by-Step Connection Guide
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Rounded.FormatListNumbered,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "📋 Guía de Conexión: ${selectedBrand.displayName}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }

                            val steps = multiBrandSyncManager.getConnectionSteps(selectedBrand)
                            steps.forEach { stepText ->
                                Row(
                                    verticalAlignment = Alignment.Top,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Rounded.ArrowForward,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.tertiary,
                                        modifier = Modifier
                                            .size(16.dp)
                                            .padding(top = 2.dp)
                                    )
                                    Text(
                                        text = stepText,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                }
                            }
                        }
                    }

                    // Direct Import Section (Fallback for Watches & Scales)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.primary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Rounded.Sync,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Text(
                                    text = "⚡ Sincronización Directa de ${selectedBrand.displayName}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }

                            if (selectedCategory == DeviceCategory.SCALE) {
                                Text(
                                    text = "Importa al instante las mediciones de tu ${selectedBrand.displayName}: Peso, Porcentaje de Grasa, Masa Muscular, Grasa Visceral e IMC.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f)
                                )

                                Button(
                                    onClick = {
                                        coroutineScope.launch {
                                            isImportingDirect = true
                                            try {
                                                val result = multiBrandSyncManager.importDirectScaleData(selectedBrand)
                                                lastSyncResult = result
                                                statusMessage = result.message
                                                Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
                                                onSyncComplete(result)
                                            } catch (e: Exception) {
                                                statusMessage = "Error en importación: ${e.localizedMessage}"
                                            } finally {
                                                isImportingDirect = false
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = !isImportingDirect,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    if (isImportingDirect) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(18.dp),
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Sincronizando Báscula...")
                                    } else {
                                        Icon(Icons.Rounded.Download, contentDescription = null)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            "Sincronizar Datos Directos de ${selectedBrand.displayName}",
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }

                                // Preview extracted scale parameters card if available
                                lastSyncResult?.scaleDetails?.let { details ->
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(12.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                "Métricas Extraídas (${details.brand.displayName}):",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text("Peso: ${details.weightKg} kg", style = MaterialTheme.typography.bodySmall)
                                                Text("Grasa: ${details.bodyFatPercentage}%", style = MaterialTheme.typography.bodySmall)
                                                Text("Músculo: ${details.muscleMassPercentage}%", style = MaterialTheme.typography.bodySmall)
                                            }
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text("Grasa Visceral: Lvl ${details.visceralFatLevel}", style = MaterialTheme.typography.bodySmall)
                                                Text("IMC: ${details.bmi}", style = MaterialTheme.typography.bodySmall)
                                            }
                                        }
                                    }
                                }
                            } else {
                                // Smartwatch direct import
                                Text(
                                    text = "Sincroniza directamente tus entrenamientos de ${selectedBrand.displayName} (Carrera corta/larga, Natación y Musculación) en la base de datos.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f)
                                )

                                Button(
                                    onClick = {
                                        coroutineScope.launch {
                                            isImportingDirect = true
                                            try {
                                                val result = multiBrandSyncManager.importDirectWatchData(selectedBrand)
                                                lastSyncResult = result
                                                statusMessage = result.message
                                                Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
                                                onSyncComplete(result)
                                            } catch (e: Exception) {
                                                statusMessage = "Error en importación: ${e.localizedMessage}"
                                            } finally {
                                                isImportingDirect = false
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = !isImportingDirect,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    if (isImportingDirect) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(18.dp),
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Importando datos...")
                                    } else {
                                        Icon(Icons.Rounded.Download, contentDescription = null)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            "Sincronizar Datos Directos de ${selectedBrand.displayName}",
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }

                                HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))

                                Text(
                                    text = "Importar Actividad Específica:",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            coroutineScope.launch {
                                                val res = multiBrandSyncManager.importDirectWatchData(selectedBrand, "running_short")
                                                statusMessage = res.message
                                                onSyncComplete(res)
                                            }
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(4.dp)
                                    ) {
                                        Icon(Icons.AutoMirrored.Rounded.DirectionsRun, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Text("5K GPS", style = MaterialTheme.typography.labelSmall)
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            coroutineScope.launch {
                                                val res = multiBrandSyncManager.importDirectWatchData(selectedBrand, "open_water")
                                                statusMessage = res.message
                                                onSyncComplete(res)
                                            }
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(4.dp)
                                    ) {
                                        Icon(Icons.Rounded.Pool, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Text("Aguas Abiertas GPS", style = MaterialTheme.typography.labelSmall)
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            coroutineScope.launch {
                                                val res = multiBrandSyncManager.importDirectWatchData(selectedBrand, "swimming")
                                                statusMessage = res.message
                                                onSyncComplete(res)
                                            }
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(4.dp)
                                    ) {
                                        Icon(Icons.Rounded.Pool, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Text("Piscina", style = MaterialTheme.typography.labelSmall)
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            coroutineScope.launch {
                                                val res = multiBrandSyncManager.importDirectWatchData(selectedBrand, "strength")
                                                statusMessage = res.message
                                                onSyncComplete(res)
                                            }
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(4.dp)
                                    ) {
                                        Icon(Icons.Rounded.FitnessCenter, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Text("Gym", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                    }

                    // Bottom Action buttons (Scan Health Connect & Close)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                coroutineScope.launch {
                                    isScanning = true
                                    try {
                                        val res = multiBrandSyncManager.syncFromHealthConnect(selectedBrand)
                                        statusMessage = res.message
                                        onSyncComplete(res)
                                    } catch (e: Exception) {
                                        statusMessage = "Error al escanear: ${e.localizedMessage}"
                                    } finally {
                                        isScanning = false
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (isScanning) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Rounded.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Escanear Health Connect", style = MaterialTheme.typography.labelSmall)
                            }
                        }

                        Button(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Listo / Cerrar")
                        }
                    }
                }
            }
        }
    }
}
