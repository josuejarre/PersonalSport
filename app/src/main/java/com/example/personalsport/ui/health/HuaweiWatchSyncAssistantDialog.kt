package com.example.personalsport.ui.health

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
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
import com.example.personalsport.health.HuaweiDeviceSyncManager
import com.example.personalsport.health.HuaweiSyncResult
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HuaweiWatchSyncAssistantDialog(
    huaweiDeviceSyncManager: HuaweiDeviceSyncManager,
    onDismiss: () -> Unit,
    onSyncComplete: (HuaweiSyncResult) -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var isAppInstalled by remember { mutableStateOf(huaweiDeviceSyncManager.isHuaweiHealthInstalled(context)) }
    var hasPermissions by remember { mutableStateOf(false) }
    var recordCount by remember { mutableIntStateOf(0) }
    var isScanning by remember { mutableStateOf(false) }
    var isImportingDirect by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    // Health Connect permissions launcher contract
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = huaweiDeviceSyncManager.let {
            // Use contract from HealthConnectClient or PermissionController via Manager
            huaweiDeviceSyncManager.let { 
                PermissionController.createRequestPermissionResultContract()
            }
        }
    ) { grantedPermissions ->
        coroutineScope.launch {
            hasPermissions = huaweiDeviceSyncManager.checkHealthConnectPermissions()
            if (hasPermissions) {
                statusMessage = "¡Permisos de Health Connect concedidos correctamente!"
                val records = huaweiDeviceSyncManager.scanHealthConnectExerciseRecords()
                recordCount = records.size
            } else {
                statusMessage = "Algunos permisos aún están pendientes."
            }
        }
    }

    // Initial check
    LaunchedEffect(Unit) {
        isAppInstalled = huaweiDeviceSyncManager.isHuaweiHealthInstalled(context)
        hasPermissions = huaweiDeviceSyncManager.checkHealthConnectPermissions()
        if (hasPermissions) {
            val records = huaweiDeviceSyncManager.scanHealthConnectExerciseRecords()
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
                .fillMaxHeight(0.90f)
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
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Watch,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Asistente Huawei Watch GT",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Diagnóstico & Sincronización",
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
                    // Status banner if any
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

                    // 1. Diagnostic Status Cards (Huawei Health & Health Connect)
                    Text(
                        text = "1. Estado de Conexión & Requisitos",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    // Card A: Huawei Health App Status
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
                                    text = "Huawei Health App",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (isAppInstalled)
                                        "🟢 Aplicación Huawei Health detectada (com.huawei.health)"
                                    else
                                        "🔴 App no detectada. Se recomienda tener Huawei Health instalada para emparejar tu Huawei Watch GT.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Button(
                                onClick = {
                                    huaweiDeviceSyncManager.openHuaweiHealthApp(context)
                                    isAppInstalled = huaweiDeviceSyncManager.isHuaweiHealthInstalled(context)
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                ),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text("Abrir App", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }

                    // Card B: Health Connect Permissions Status
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
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
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
                                            "🔴 Permisos pendientes de Health Connect.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Button(
                                    onClick = {
                                        val reqs = HuaweiDeviceSyncManager.REQUIRED_PERMISSIONS
                                        permissionLauncher.launch(reqs)
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
                    }

                    // 2. Step-by-Step Connection Guide
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
                                    text = "📋 Guía de Navegación Huawei Health v14.0.8.310",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }

                            Text(
                                text = huaweiDeviceSyncManager.getHuaweiHealthV14NavigationGuide(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            huaweiDeviceSyncManager.getHuaweiHealthV14NavigationSteps().forEach { stepText ->
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

                    // 3. Automatic Scan & Direct Watch Data Import Section
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
                                    text = "⚡ Sincronización Directa Huawei Watch GT (GT-9c6)",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }

                            Text(
                                text = "Health Connect indica $recordCount registros de ejercicio. Si tus datos de Huawei Watch GT (GT-9c6) no se han transmitido por Health Connect en v14.0.8.310, utiliza la importación directa.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f)
                            )

                            // Main Direct Import Fallback Button for GT-9c6 v14
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        isImportingDirect = true
                                        try {
                                            val result = huaweiDeviceSyncManager.pairAndSyncHuaweiWatchGT9c6V14()
                                            statusMessage = result.message
                                            Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
                                            onSyncComplete(result)
                                        } catch (e: Exception) {
                                            statusMessage = "Error en importación GT-9c6: ${e.localizedMessage}"
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
                                    Text("Sincronizando GT-9c6 v14...")
                                } else {
                                    Icon(Icons.Rounded.Download, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        "Emparejar & Sincronizar Huawei Watch GT (GT-9c6) v14",
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))

                            Text(
                                text = "Importar Actividad Específica GT:",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )

                            // Quick Filter Chips for Specific Activities
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                ActivityChip(
                                    label = "🏃 5K (GPS)",
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        coroutineScope.launch {
                                            val res = huaweiDeviceSyncManager.importDirectHuaweiWatchData("running_short")
                                            statusMessage = res.message
                                            onSyncComplete(res)
                                        }
                                    }
                                )
                                ActivityChip(
                                    label = "🏃 10K (GPS)",
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        coroutineScope.launch {
                                            val res = huaweiDeviceSyncManager.importDirectHuaweiWatchData("running_long")
                                            statusMessage = res.message
                                            onSyncComplete(res)
                                        }
                                    }
                                )
                                ActivityChip(
                                    label = "🏊 Aguas Abiertas (GPS)",
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        coroutineScope.launch {
                                            val res = huaweiDeviceSyncManager.importDirectHuaweiWatchData("open_water")
                                            statusMessage = res.message
                                            onSyncComplete(res)
                                        }
                                    }
                                )
                                ActivityChip(
                                    label = "🏊 Piscina",
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        coroutineScope.launch {
                                            val res = huaweiDeviceSyncManager.importDirectHuaweiWatchData("swimming")
                                            statusMessage = res.message
                                            onSyncComplete(res)
                                        }
                                    }
                                )
                                ActivityChip(
                                    label = "🏋️ Gym",
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        coroutineScope.launch {
                                            val res = huaweiDeviceSyncManager.importDirectHuaweiWatchData("strength")
                                            statusMessage = res.message
                                            onSyncComplete(res)
                                        }
                                    }
                                )
                            }
                        }
                    }

                    // Action buttons (Scan & Close)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                coroutineScope.launch {
                                    isScanning = true
                                    try {
                                        val res = huaweiDeviceSyncManager.syncFromHealthConnect()
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

@Composable
private fun ActivityChip(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}
