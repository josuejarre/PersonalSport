package com.example.personalsport.ui.manualentry

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import com.example.personalsport.ui.theme.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.personalsport.PersonalSportApp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualEntryScreen(
    onSaveComplete: () -> Unit = {},
    onOpenDrawer: () -> Unit = {},
    onOpenProfile: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val app = PersonalSportApp.getInstance(context)
    val factory = remember { ManualEntryViewModelFactory(app.workoutLogRepository, app.bodyMetricRepository) }
    val viewModel: ManualEntryViewModel = viewModel(factory = factory)

    val saveSuccess by viewModel.saveSuccess.collectAsState()

    var title by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Running") }
    var durationMinutes by remember { mutableFloatStateOf(30f) }
    var distanceKmText by remember { mutableStateOf("") }
    var caloriesText by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    var updateBodyMetrics by remember { mutableStateOf(false) }
    var weightText by remember { mutableStateOf("") }
    var bodyFatText by remember { mutableStateOf("") }

    var errorMsg by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    val categories = listOf("Carrera con GPS", "Natación en Aguas Abiertas", "Running", "Swimming", "Strength Training", "Cycling", "HIIT", "Yoga", "Walking", "Other")

    LaunchedEffect(saveSuccess) {
        if (saveSuccess) {
            snackbarHostState.showSnackbar("Workout logged successfully!")
            viewModel.resetSaveSuccess()
            // Reset form fields
            title = ""
            notes = ""
            distanceKmText = ""
            caloriesText = ""
            weightText = ""
            bodyFatText = ""
            updateBodyMetrics = false
            onSaveComplete()
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
                                "Log Workout Session",
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
                .perforatedMeshBackground(),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                    modifier = Modifier
                        .widthIn(max = 700.dp)
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Text("Workout Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                                // Quick Activity Category Chips
                                Text("Activity Category:", style = MaterialTheme.typography.labelLarge)
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(categories) { cat ->
                                        FilterChip(
                                            selected = selectedCategory == cat,
                                            onClick = {
                                                selectedCategory = cat
                                                if (title.isBlank()) {
                                                    title = "$cat Session"
                                                }
                                            },
                                            label = { Text(cat) },
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = when {
                                                        cat.contains("Carrera", ignoreCase = true) || cat.contains("Running", ignoreCase = true) -> Icons.Rounded.DirectionsRun
                                                        cat.contains("Natación", ignoreCase = true) || cat.contains("Swimming", ignoreCase = true) -> Icons.Rounded.Pool
                                                        cat.contains("Cycling", ignoreCase = true) -> Icons.Rounded.DirectionsBike
                                                        cat.contains("Strength", ignoreCase = true) -> Icons.Rounded.FitnessCenter
                                                        else -> Icons.Rounded.Sports
                                                    },
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        )
                                    }
                                }

                                // Workout Title
                                OutlinedTextField(
                                    value = title,
                                    onValueChange = { title = it },
                                    label = { Text("Workout Title / Name *") },
                                    placeholder = { Text("e.g. Morning 5k Run, Leg Day") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                )

                                // Duration Slider & Text
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Duration:", style = MaterialTheme.typography.labelLarge)
                                        Text(
                                            "${durationMinutes.toInt()} minutes",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Slider(
                                        value = durationMinutes,
                                        onValueChange = { durationMinutes = it },
                                        valueRange = 5f..180f,
                                        steps = 35,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }

                                // Distance & Calories Row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    OutlinedTextField(
                                        value = distanceKmText,
                                        onValueChange = { distanceKmText = it },
                                        label = { Text("Distance (km)") },
                                        placeholder = { Text("e.g. 5.2") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        singleLine = true,
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    OutlinedTextField(
                                        value = caloriesText,
                                        onValueChange = { caloriesText = it },
                                        label = { Text("Calories (kcal)") },
                                        placeholder = { Text("e.g. 350") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                }

                                // Notes
                                OutlinedTextField(
                                    value = notes,
                                    onValueChange = { notes = it },
                                    label = { Text("Notes / Journal (Optional)") },
                                    placeholder = { Text("How did the session feel? RPE 8/10, sunny weather...") },
                                    modifier = Modifier.fillMaxWidth(),
                                    minLines = 3,
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }
                        }
                    }

                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Checkbox(
                                        checked = updateBodyMetrics,
                                        onCheckedChange = { updateBodyMetrics = it }
                                    )
                                    Text(
                                        "Log Body Metrics along with this workout",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                if (updateBodyMetrics) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = weightText,
                                            onValueChange = { weightText = it },
                                            label = { Text("Weight (kg)") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                            singleLine = true,
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        OutlinedTextField(
                                            value = bodyFatText,
                                            onValueChange = { bodyFatText = it },
                                            label = { Text("Body Fat %") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                            singleLine = true,
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    errorMsg?.let { err ->
                        item {
                            Text(err, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                        }
                    }

                    item {
                        Button(
                            onClick = {
                                if (title.isBlank()) {
                                    errorMsg = "Please enter a workout title"
                                    return@Button
                                }
                                errorMsg = null
                                viewModel.addWorkoutLog(
                                    title = title,
                                    category = selectedCategory,
                                    durationMinutes = durationMinutes.toInt(),
                                    distanceKm = distanceKmText.toFloatOrNull(),
                                    calories = caloriesText.toIntOrNull(),
                                    notes = notes,
                                    updateMetrics = updateBodyMetrics,
                                    weightKg = weightText.toFloatOrNull(),
                                    bodyFatPct = bodyFatText.toFloatOrNull()
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Rounded.Save, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Save Workout Log", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
