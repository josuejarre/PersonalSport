package com.example.personalsport.ui.planner

import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import com.example.personalsport.ui.theme.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.window.core.layout.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.personalsport.PersonalSportApp
import com.example.personalsport.ai.AthleticsSeriesType
import com.example.personalsport.data.provider.ExerciseGifDatasetProvider
import com.example.personalsport.ui.components.ConfettiCelebration
import com.example.personalsport.util.AppCoilImageLoader

/**
 * Pantalla de Planificador Deportivo Macro de 3 Meses y Asistente IA ([PlannerScreen]).
 *
 * Visualiza la programación de entrenamiento estructurada a 90 Días (3 Meses):
 * - Vista detallada del plan trimestral dividido en meses, semanas y días.
 * - Asistente interactivo de configuración de rutinas ([AIRoutineWizardDialog]).
 * - Diálogo explicativo de ejercicios con animaciones GIF anatómicas de alta definición ([ExerciseDetailDialog]).
 * - Cuadro de mando semanal y seguimiento de completado de series y repeticiones.
 *
 * **Manejo de Estado y Despacho de Hilos**:
 * - Recolecta el estado reactivo [PlannerUiState] del [PlannerViewModel] en el hilo principal de UI.
 * - Las operaciones de cálculo de rutinas y Room DB se ejecutan en corrutinas en hilos secundarios.
 *
 * @param onNavigateToEquipment Callback para navegar al inventario de equipamiento.
 * @param onOpenDrawer Callback para desplegar el menú de navegación lateral.
 * @param onOpenProfile Callback para abrir el diálogo modal de perfil.
 * @param modifier Modificador de diseño Compose.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlannerScreen(
    onNavigateToEquipment: () -> Unit = {},
    onOpenDrawer: () -> Unit = {},
    onOpenProfile: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val app = PersonalSportApp.getInstance(context)
    val factory = remember {
        PlannerViewModelFactory(
            app.equipmentRepository,
            app.workoutLogRepository,
            app.progressPhotoRepository,
            app.completedExerciseRepository
        )
    }
    val viewModel: PlannerViewModel = viewModel(factory = factory)

    val state by viewModel.uiState.collectAsState()
    val adaptiveInfo = currentWindowAdaptiveInfo()
    val isExpandedWidth = adaptiveInfo.windowSizeClass.windowWidthSizeClass == WindowWidthSizeClass.EXPANDED

    val snackbarHostState = remember { SnackbarHostState() }
    var selectedExerciseForDetail by remember { mutableStateOf<PlannedExerciseStep?>(null) }

    LaunchedEffect(state.isSavedToLog) {
        if (state.isSavedToLog) {
            snackbarHostState.showSnackbar("Entrenamiento guardado en tu Registro de Entrenamientos!")
        }
    }

    if (state.isWizardOpen) {
        val wizardFactory = remember {
            AIRoutineWizardViewModelFactory(
                app.equipmentRepository,
                app.workoutLogRepository,
                app.progressPhotoRepository
            )
        }
        val wizardViewModel: AIRoutineWizardViewModel = viewModel(factory = wizardFactory)

        AIRoutineWizardDialog(
            viewModel = wizardViewModel,
            onDismiss = { viewModel.closeWizard() },
            onPlanGenerated = { generatedPlan ->
                viewModel.set3MonthPlan(generatedPlan)
            }
        )
    }

    selectedExerciseForDetail?.let { exercise ->
        val isCompleted = state.completedExerciseIds.contains(exercise.id) || exercise.isCompleted
        ExerciseDetailDialog(
            step = exercise.copy(isCompleted = isCompleted),
            onDismiss = { selectedExerciseForDetail = null },
            onToggleComplete = { stepToToggle ->
                viewModel.toggleExerciseCompletion(stepToToggle)
                selectedExerciseForDetail = stepToToggle.copy(isCompleted = !isCompleted)
            }
        )
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
                                "Planificador de Rutinas IA (3 Meses)",
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
                .perforatedMeshBackground()
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                // Prominent AI 3-Month Wizard Launch Banner
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
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
                                        .background(MaterialTheme.colorScheme.primary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Rounded.AutoAwesome,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "Asistente IA de Rutinas (3 Meses)",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        "Genera un plan de 90 días adaptado a tu modalidad, disciplinas, calistenia y nutrición con validación calórica.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                                    )
                                }
                            }

                            Button(
                                onClick = { viewModel.openWizard() },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            ) {
                                Icon(Icons.Rounded.RocketLaunch, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Lanzar Asistente IA de Rutinas (3 Meses)", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Weekly Training Dashboard (Weekday Selector Bar + Dedicated Day Window Card)
                item {
                    val currentWeekPlan = state.active3MonthPlan?.months
                        ?.flatMap { it.weeks }
                        ?.find { it.weekNumber == state.selectedWeekNumber }

                    WeeklyWorkoutDashboardView(
                        weekPlan = currentWeekPlan,
                        completedExerciseIds = state.completedExerciseIds,
                        onToggleExerciseComplete = { viewModel.toggleExerciseCompletion(it) },
                        onExerciseClick = { selectedExerciseForDetail = it },
                        onSaveDayToLog = { dayPlan -> viewModel.saveDayToLog(dayPlan) }
                    )
                }

                // Render Active 3-Month Plan View if generated
                state.active3MonthPlan?.let { plan ->
                    item {
                        ThreeMonthPlanView(
                            plan = plan,
                            selectedWeekNum = state.selectedWeekNumber,
                            onSelectWeek = { viewModel.selectWeekNumber(it) },
                            onSaveDayToLog = { viewModel.saveDayToLog(it) },
                            completedExerciseIds = state.completedExerciseIds,
                            onExerciseClick = { selectedExerciseForDetail = it },
                            onToggleComplete = { viewModel.toggleExerciseCompletion(it) }
                        )
                    }
                }

                // Quick Single Workout Generator Controls
                item {
                    Text(
                        if (state.active3MonthPlan != null) "Generador Rápido de Sesiones Individuales" else "Generador Rápido de Rutinas Individuales",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                item {
                    PlannerControlsCard(
                        state = state,
                        onSelectSport = { viewModel.selectSportType(it) },
                        onSelectAthleticsType = { viewModel.selectAthleticsType(it) },
                        onSelectDuration = { viewModel.selectDuration(it) },
                        onSelectIntensity = { viewModel.selectIntensity(it) },
                        onNavigateToEquipment = onNavigateToEquipment
                    )
                }

                item {
                    state.generatedRoutine?.let { routine ->
                        GeneratedRoutineView(
                            routine = routine,
                            isSaved = state.isSavedToLog,
                            completedExerciseIds = state.completedExerciseIds,
                            onSave = { viewModel.saveRoutineToLog(routine) },
                            onExerciseClick = { selectedExerciseForDetail = it },
                            onToggleComplete = { viewModel.toggleExerciseCompletion(it) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PlannerControlsCard(
    state: PlannerUiState,
    onSelectSport: (SportType) -> Unit,
    onSelectAthleticsType: (AthleticsSeriesType) -> Unit,
    onSelectDuration: (Int) -> Unit,
    onSelectIntensity: (String) -> Unit,
    onNavigateToEquipment: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
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
                Text(
                    "Configuración del Generador Rápido",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                TextButton(onClick = onNavigateToEquipment) {
                    Icon(Icons.Rounded.Build, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Catálogo Maquinaria")
                }
            }

            // Sport Type Selection Chips
            Text("Deporte / Disciplina", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(SportType.values()) { sport ->
                    val isSelected = sport == state.selectedSportType
                    FilterChip(
                        selected = isSelected,
                        onClick = { onSelectSport(sport) },
                        label = { Text(sport.displayName) },
                        leadingIcon = if (isSelected) {
                            { Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null
                    )
                }
            }

            // Athletics Series Type selector
            if (state.selectedSportType == SportType.ATHLETICS_SERIES) {
                Text("Tipo de Serie de Atletismo", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(AthleticsSeriesType.values()) { athleticsType ->
                        val isSelected = athleticsType == state.selectedAthleticsType
                        FilterChip(
                            selected = isSelected,
                            onClick = { onSelectAthleticsType(athleticsType) },
                            label = { Text(athleticsType.displayName) },
                            leadingIcon = if (isSelected) {
                                { Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null
                        )
                    }
                }
            }

            // Duration selector
            Text("Duración Objetivo (Minutos)", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(30, 45, 60, 90).forEach { dur ->
                    FilterChip(
                        selected = dur == state.targetDurationMinutes,
                        onClick = { onSelectDuration(dur) },
                        label = { Text("$dur min") }
                    )
                }
            }

            // Intensity selector
            Text("Intensidad del Entrenamiento", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Light", "Moderate", "High", "Extreme").forEach { intensity ->
                    FilterChip(
                        selected = intensity == state.selectedIntensity,
                        onClick = { onSelectIntensity(intensity) },
                        label = { Text(intensity) }
                    )
                }
            }
        }
    }
}

@Composable
fun GeneratedRoutineView(
    routine: GeneratedRoutine,
    isSaved: Boolean,
    completedExerciseIds: Set<String>,
    onSave: () -> Unit,
    onExerciseClick: (PlannedExerciseStep) -> Unit,
    onToggleComplete: (PlannedExerciseStep) -> Unit
) {
    val allSteps = routine.warmup + routine.mainSet + routine.cooldown
    val totalCount = allSteps.size
    val completedCount = allSteps.count { completedExerciseIds.contains(it.id) || it.isCompleted }
    val progressPercentage = if (totalCount > 0) (completedCount * 100) / totalCount else 0
    val isFullyCompleted = totalCount > 0 && completedCount == totalCount

    // Celebration Trigger State
    var celebratedRoutineKeys by remember { mutableStateOf(setOf<String>()) }
    var showCelebrationDialog by remember { mutableStateOf(false) }
    var isInitialized by remember { mutableStateOf(false) }

    val routineKey = routine.title

    LaunchedEffect(routineKey, completedCount, totalCount) {
        if (!isInitialized) {
            if (isFullyCompleted) {
                celebratedRoutineKeys = celebratedRoutineKeys + routineKey
            }
            isInitialized = true
        } else {
            if (isFullyCompleted && !celebratedRoutineKeys.contains(routineKey)) {
                celebratedRoutineKeys = celebratedRoutineKeys + routineKey
                showCelebrationDialog = true
            } else if (!isFullyCompleted && celebratedRoutineKeys.contains(routineKey)) {
                celebratedRoutineKeys = celebratedRoutineKeys - routineKey
            }
        }
    }

    if (showCelebrationDialog) {
        val estimatedCalories = routine.targetDurationMinutes * 8
        ConfettiCelebration(
            totalExercisesFinished = totalCount,
            totalEstimatedCaloriesBurned = estimatedCalories,
            onDismiss = { showCelebrationDialog = false }
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(routine.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(routine.notes, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                AssistChip(
                    onClick = {},
                    label = { Text("${routine.targetDurationMinutes} min · ${routine.intensity}") }
                )
            }

            // Routine completion progress bar
            if (totalCount > 0) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "$completedCount de $totalCount ejercicios completados ($progressPercentage%)",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (completedCount == totalCount) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (completedCount == totalCount) {
                            Text("✔ Completada", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
                        }
                    }

                    LinearProgressIndicator(
                        progress = { completedCount.toFloat() / totalCount },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = Color(0xFF4CAF50),
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }

            Button(
                onClick = onSave,
                enabled = !isSaved,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(if (isSaved) Icons.Rounded.Check else Icons.Rounded.BookmarkAdd, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isSaved) "Guardado en Registro" else "Guardar en Registro de Entrenamientos")
            }

            HorizontalDivider()

            // Warmup Section
            Text("1. Calentamiento", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            routine.warmup.forEach { step ->
                val isCompleted = completedExerciseIds.contains(step.id) || step.isCompleted
                StepItemCard(
                    step = step.copy(isCompleted = isCompleted),
                    onClick = { onExerciseClick(step.copy(isCompleted = isCompleted)) },
                    onToggleComplete = { onToggleComplete(step.copy(isCompleted = isCompleted)) }
                )
            }

            // Main Set Section
            Text("2. Bloque Principal", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            routine.mainSet.forEach { step ->
                val isCompleted = completedExerciseIds.contains(step.id) || step.isCompleted
                StepItemCard(
                    step = step.copy(isCompleted = isCompleted),
                    onClick = { onExerciseClick(step.copy(isCompleted = isCompleted)) },
                    onToggleComplete = { onToggleComplete(step.copy(isCompleted = isCompleted)) }
                )
            }

            // Cooldown Section
            Text("3. Enfriamiento & Recuperación", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            routine.cooldown.forEach { step ->
                val isCompleted = completedExerciseIds.contains(step.id) || step.isCompleted
                StepItemCard(
                    step = step.copy(isCompleted = isCompleted),
                    onClick = { onExerciseClick(step.copy(isCompleted = isCompleted)) },
                    onToggleComplete = { onToggleComplete(step.copy(isCompleted = isCompleted)) }
                )
            }
        }
    }
}

@Composable
fun StepItemCard(
    step: PlannedExerciseStep,
    onClick: () -> Unit = {},
    onToggleComplete: () -> Unit = {}
) {
    val gifUrl = remember(step.title, step.imageUrl) {
        if (!step.imageUrl.isNullOrBlank()) {
            step.imageUrl
        } else {
            ExerciseGifDatasetProvider.getGifUrlForExercise(step.title)
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (step.isCompleted) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        shape = RoundedCornerShape(12.dp),
        border = if (step.isCompleted) {
            BorderStroke(2.dp, Color(0xFF4CAF50))
        } else {
            null
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Interactive status / checkbox icon button
            IconButton(
                onClick = onToggleComplete,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(
                        if (step.isCompleted) Color(0xFF4CAF50)
                        else MaterialTheme.colorScheme.primaryContainer
                    )
            ) {
                Icon(
                    imageVector = if (step.isCompleted) Icons.Rounded.Check else Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    tint = if (step.isCompleted) Color.White else MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Exercise GIF Thumbnail Box (Clickable to open ExerciseDetailDialog)
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onClick() }
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF232B36),
                                Color(0xFF141920)
                            )
                        )
                    )
                    .border(
                        BorderStroke(1.dp, Color(0xFF3B4654)),
                        shape = RoundedCornerShape(10.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                val context = LocalContext.current
                val sharedImageLoader = AppCoilImageLoader.get(context)

                var currentGifData by remember(gifUrl) { mutableStateOf<Any>(gifUrl) }

                val imageRequest: ImageRequest = remember(currentGifData, context) {
                    ImageRequest.Builder(context)
                        .data(currentGifData)
                        .crossfade(true)
                        .build()
                }

                AsyncImage(
                    model = imageRequest,
                    imageLoader = sharedImageLoader,
                    contentDescription = step.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    onError = {
                        if (currentGifData != ExerciseGifDatasetProvider.FALLBACK_GIF_URL) {
                            currentGifData = ExerciseGifDatasetProvider.FALLBACK_GIF_URL
                        }
                    }
                )
            }

            // Exercise Title & Info (Clickable to open ExerciseDetailDialog)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onClick() }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = step.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = if (step.isCompleted) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurface
                    )

                    if (step.isCompleted) {
                        Surface(
                            color = Color(0xFF4CAF50).copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "✔",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF4CAF50),
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }

                Text(
                    text = step.description,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (step.equipmentName.isNotBlank() && step.equipmentName != "Peso Corporal") {
                    Text(
                        "Máquina: ${step.equipmentName}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    step.equipmentUsed?.let { eq ->
                        Text(
                            "Material: $eq",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Sets / Reps Badge (Clickable to open ExerciseDetailDialog)
            Badge(
                containerColor = if (step.isCompleted) Color(0xFF4CAF50).copy(alpha = 0.2f) else MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.clickable { onClick() }
            ) {
                Text(
                    step.durationOrReps,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (step.isCompleted) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }

            // Arrow button to open ExerciseDetailDialog directly
            IconButton(
                onClick = onClick,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.ChevronRight,
                    contentDescription = "Ver animación e instrucciones",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
