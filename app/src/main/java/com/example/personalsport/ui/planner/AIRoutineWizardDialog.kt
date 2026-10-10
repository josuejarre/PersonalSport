package com.example.personalsport.ui.planner

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.DirectionsRun
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.personalsport.ai.AthleticsSeriesCategory
import com.example.personalsport.util.AppCoilImageLoader
import com.example.personalsport.ai.CardioDiscipline
import com.example.personalsport.ai.RoutineModality
import com.example.personalsport.ai.RunningSubType
import com.example.personalsport.ai.SwimDistanceSeriesCategory
import com.example.personalsport.ai.ThreeMonthPlan
import com.example.personalsport.diet.DietGoal
import com.example.personalsport.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AIRoutineWizardDialog(
    viewModel: AIRoutineWizardViewModel,
    onDismiss: () -> Unit,
    onPlanGenerated: (ThreeMonthPlan) -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.setBaselinePhotoUri(it.toString()) }
    }

    Dialog(
        onDismissRequest = { if (!state.isGenerating) onDismiss() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = !state.isGenerating,
            dismissOnClickOutside = !state.isGenerating
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.90f)
                .clip(RoundedCornerShape(24.dp)),
            color = DarkMetallicCard,
            tonalElevation = 8.dp
        ) {
            if (state.isGenerating) {
                GeneratingPlanLoadingScreen()
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp)
                ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(ElectricSportOrange.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                tint = ElectricSportOrange
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                "Asistente IA de Rutinas (3 Meses)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryWhite
                            )
                            Text(
                                "Generador de Plan Élite de 90 Días",
                                style = MaterialTheme.typography.bodySmall,
                                color = LightPlatinum
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Cerrar",
                            tint = LightPlatinum
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Progress Indicator
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Paso ${state.currentStep} de ${state.totalSteps}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = ElectricSportOrange
                        )
                        Text(
                            when (state.currentStep) {
                                1 -> "Modalidad Principal"
                                2 -> "Deportes & Series"
                                3 -> "Calistenia & Ejercicios"
                                else -> "Foto Inicial & Meta"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = LightPlatinum
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { state.currentStep.toFloat() / state.totalSteps.toFloat() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = ElectricSportOrange,
                        trackColor = DarkSteelAccent
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Middle Step Content (Scrollable, weighted to prevent truncation)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(scrollState)
                        .padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    when (state.currentStep) {
                        1 -> Step1ModalityView(
                            selectedModality = state.wizardConfig.modality,
                            onSelectModality = { viewModel.setModality(it) }
                        )
                        2 -> Step2DisciplinesView(
                            selectedDisciplines = state.wizardConfig.selectedDisciplines,
                            runningSubType = state.wizardConfig.runningSubType,
                            athleticsSeriesCategory = state.wizardConfig.athleticsSeriesCategory,
                            swimSeriesCategory = state.wizardConfig.swimSeriesCategory,
                            onToggleDiscipline = { viewModel.toggleDiscipline(it) },
                            onSelectRunningSubType = { viewModel.setRunningSubType(it) },
                            onSelectAthleticsCategory = { viewModel.setAthleticsSeriesCategory(it) },
                            onSelectSwimCategory = { viewModel.setSwimSeriesCategory(it) }
                        )
                        3 -> Step3CalisthenicsView(
                            includeCalisthenics = state.wizardConfig.includeCalisthenics,
                            selectedExercises = state.wizardConfig.selectedCalisthenicsExercises,
                            onToggleInclude = { viewModel.toggleCalisthenics(it) },
                            onToggleExercise = { viewModel.toggleCalisthenicsExercise(it) }
                        )
                        4 -> Step4PhotoAndGoalView(
                            baselinePhotoUri = state.wizardConfig.baselinePhotoUri,
                            goalType = state.wizardConfig.goalType,
                            onPickPhoto = { photoPickerLauncher.launch("image/*") },
                            onRemovePhoto = { viewModel.setBaselinePhotoUri(null) },
                            onSelectGoal = { viewModel.setGoalType(it) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                state.errorMessage?.let { err ->
                    Text(
                        text = err,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                // Footer Navigation Buttons (anchored at bottom)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { viewModel.previousStep() },
                        enabled = (state.currentStep > 1 && !state.isGenerating),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MetallicGray),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = TextPrimaryWhite,
                            disabledContentColor = PlatinumSteel
                        )
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Anterior", fontWeight = FontWeight.SemiBold)
                    }

                    if (state.currentStep < state.totalSteps) {
                        Button(
                            onClick = { viewModel.nextStep() },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ElectricSportOrange,
                                contentColor = PureWhite
                            )
                        ) {
                            Text("Siguiente", fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    } else {
                        Button(
                            onClick = {
                                viewModel.generatePlan { plan ->
                                    onPlanGenerated(plan)
                                    onDismiss()
                                }
                            },
                            enabled = !state.isGenerating,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ElectricSportOrange,
                                contentColor = PureWhite,
                                disabledContainerColor = DarkOrangeTint,
                                disabledContentColor = PlatinumLight
                            )
                        ) {
                            if (state.isGenerating) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = PureWhite,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Generando Plan 3 Meses...", fontWeight = FontWeight.Bold)
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.AutoAwesome,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Generar Plan 3 Meses", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
}

@Composable
fun GeneratingPlanLoadingScreen() {
    val infiniteTransition = rememberInfiniteTransition(label = "LoadingGlow")
    val alphaPulse by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "AlphaPulse"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(110.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(96.dp),
                color = ElectricSportOrange,
                strokeWidth = 6.dp,
                trackColor = DarkSteelAccent
            )
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(ElectricSportOrange.copy(alpha = 0.15f * alphaPulse)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = ElectricSportOrange
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = "Generando Plan de 3 Meses con IA Local...",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = TextPrimaryWhite,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "Procesamiento en segundo plano optimizado para Snapdragon 732G (4 hilos, 512 context, batch 128).\nManteniendo 120 Hz de fluidez en la interfaz.",
            style = MaterialTheme.typography.bodySmall,
            color = LightPlatinum,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        LinearProgressIndicator(
            modifier = Modifier
                .width(160.dp)
                .height(4.dp)
                .clip(CircleShape),
            color = ElectricSportOrange,
            trackColor = DarkSteelAccent
        )
    }
}

@Composable
fun Step1ModalityView(
    selectedModality: RoutineModality,
    onSelectModality: (RoutineModality) -> Unit
) {
    Text(
        "Paso 1: Modalidad Principal de Entrenamiento",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = TextPrimaryWhite
    )
    Text(
        "Elige el enfoque general que estructurará las 12 semanas de tu rutina:",
        style = MaterialTheme.typography.bodySmall,
        color = LightPlatinum
    )

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        RoutineModality.entries.forEach { modality ->
            val isSelected = modality == selectedModality
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectModality(modality) },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) DarkOrangeTint else DarkSteelAccent
                ),
                border = BorderStroke(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) ElectricSportOrange else MetallicBorder
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = isSelected,
                        onClick = { onSelectModality(modality) },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = ElectricSportOrange,
                            unselectedColor = LightPlatinum
                        )
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = modality.displayName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryWhite
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = modality.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = LightPlatinum
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Step2DisciplinesView(
    selectedDisciplines: Set<CardioDiscipline>,
    runningSubType: RunningSubType?,
    athleticsSeriesCategory: AthleticsSeriesCategory?,
    swimSeriesCategory: SwimDistanceSeriesCategory?,
    onToggleDiscipline: (CardioDiscipline) -> Unit,
    onSelectRunningSubType: (RunningSubType) -> Unit,
    onSelectAthleticsCategory: (AthleticsSeriesCategory) -> Unit,
    onSelectSwimCategory: (SwimDistanceSeriesCategory) -> Unit
) {
    Text(
        "Paso 2: Disciplinas Deportivas (Hasta 3 Combinadas)",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = TextPrimaryWhite
    )
    Text(
        "Selecciona los deportes que deseas combinar en tu plan semanal (ej. Musculación + Natación + Carrera):",
        style = MaterialTheme.typography.bodySmall,
        color = LightPlatinum
    )

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        CardioDiscipline.entries.forEach { discipline ->
            val isSelected = selectedDisciplines.contains(discipline)
            FilterChip(
                selected = isSelected,
                onClick = { onToggleDiscipline(discipline) },
                label = {
                    Text(
                        text = discipline.displayName,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 4.dp, horizontal = 2.dp)
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = when (discipline) {
                            CardioDiscipline.CARRERA -> Icons.AutoMirrored.Rounded.DirectionsRun
                            CardioDiscipline.NATACION -> Icons.Rounded.Pool
                            CardioDiscipline.SPINNING -> Icons.Rounded.PedalBike
                            CardioDiscipline.HIIT -> Icons.Rounded.FlashOn
                            CardioDiscipline.ZUMBA_BAILE -> Icons.Rounded.MusicNote
                            CardioDiscipline.MUSCULACION -> Icons.Rounded.FitnessCenter
                        },
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = DarkSteelAccent,
                    labelColor = LightPlatinum,
                    iconColor = LightPlatinum,
                    selectedContainerColor = ElectricSportOrange,
                    selectedLabelColor = PureWhite,
                    selectedLeadingIconColor = PureWhite
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = isSelected,
                    borderColor = MetallicBorder,
                    selectedBorderColor = ElectricSportOrange,
                    borderWidth = 1.dp,
                    selectedBorderWidth = 1.5.dp
                )
            )
        }
    }

    // Carrera Sub-types
    AnimatedVisibility(
        visible = selectedDisciplines.contains(CardioDiscipline.CARRERA),
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSteelAccent),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MetallicBorder)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "Modalidad Específica de Carrera / Running:",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = ElectricSportOrange
                )

                // Replaced rigid Row with Column fillMaxWidth chips for full text wrapping without vertical squeezing
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RunningSubType.entries.forEach { subType ->
                        val isSelected = runningSubType == subType
                        FilterChip(
                            selected = isSelected,
                            onClick = { onSelectRunningSubType(subType) },
                            label = {
                                Text(
                                    text = subType.displayName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.padding(vertical = 4.dp, horizontal = 2.dp)
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = DarkMetallicCard,
                                labelColor = LightPlatinum,
                                selectedContainerColor = ElectricSportOrange,
                                selectedLabelColor = PureWhite
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = MetallicBorder,
                                selectedBorderColor = ElectricSportOrange,
                                borderWidth = 1.dp,
                                selectedBorderWidth = 1.5.dp
                            )
                        )
                    }
                }

                if (runningSubType == RunningSubType.ATLETISMO) {
                    HorizontalDivider(color = MetallicBorder)
                    Text(
                        "Tipo de Series de Atletismo:",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryWhite
                    )
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AthleticsSeriesCategory.entries.forEach { seriesCat ->
                            val isSelected = athleticsSeriesCategory == seriesCat
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelectAthleticsCategory(seriesCat) },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) DarkOrangeTint else DarkMetallicCard
                                ),
                                border = BorderStroke(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) ElectricSportOrange else MetallicBorder
                                )
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp)
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { onSelectAthleticsCategory(seriesCat) },
                                        colors = RadioButtonDefaults.colors(
                                            selectedColor = ElectricSportOrange,
                                            unselectedColor = LightPlatinum
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = seriesCat.displayName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = TextPrimaryWhite
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = seriesCat.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = LightPlatinum
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Natación Sub-types
    AnimatedVisibility(
        visible = selectedDisciplines.contains(CardioDiscipline.NATACION),
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSteelAccent),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MetallicBorder)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "Categoría de Series de Natación:",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = ElectricSportOrange
                )
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SwimDistanceSeriesCategory.entries.forEach { swimCat ->
                        val isSelected = swimSeriesCategory == swimCat
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectSwimCategory(swimCat) },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) DarkOrangeTint else DarkMetallicCard
                            ),
                            border = BorderStroke(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) ElectricSportOrange else MetallicBorder
                            )
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp)
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { onSelectSwimCategory(swimCat) },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = ElectricSportOrange,
                                        unselectedColor = LightPlatinum
                                    )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = swimCat.displayName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimaryWhite
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = swimCat.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = LightPlatinum
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Step3CalisthenicsView(
    includeCalisthenics: Boolean,
    selectedExercises: Set<String>,
    onToggleInclude: (Boolean) -> Unit,
    onToggleExercise: (String) -> Unit
) {
    Text(
        "Paso 3: Calistenia & Ejercicios con Peso Corporal",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = TextPrimaryWhite
    )
    Text(
        "¿Deseas incluir bloques de calistenia, dominadas y trabajo funcional con peso corporal?",
        style = MaterialTheme.typography.bodySmall,
        color = LightPlatinum
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSteelAccent),
        border = BorderStroke(1.dp, MetallicBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.SportsGymnastics,
                    contentDescription = null,
                    tint = ElectricSportOrange
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Incluir Calistenia & Dominadas",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryWhite
                )
            }
            Switch(
                checked = includeCalisthenics,
                onCheckedChange = onToggleInclude,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = PureWhite,
                    checkedTrackColor = ElectricSportOrange,
                    uncheckedThumbColor = LightPlatinum,
                    uncheckedTrackColor = MetallicGray
                )
            )
        }
    }

    AnimatedVisibility(visible = includeCalisthenics) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                "Ejercicios específicos a incorporar:",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimaryWhite
            )
            val exercisesList = listOf("Dominadas", "Flexiones (Push-ups)", "Fondos en banco/paralelas", "Abdominales / Core", "Salto de Cuerda (Comba)")

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                exercisesList.forEach { ex ->
                    val isSelected = selectedExercises.contains(ex)
                    FilterChip(
                        selected = isSelected,
                        onClick = { onToggleExercise(ex) },
                        label = {
                            Text(
                                text = ex,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(vertical = 4.dp, horizontal = 2.dp)
                            )
                        },
                        leadingIcon = if (isSelected) {
                            {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = DarkSteelAccent,
                            labelColor = LightPlatinum,
                            iconColor = LightPlatinum,
                            selectedContainerColor = ElectricSportOrange,
                            selectedLabelColor = PureWhite,
                            selectedLeadingIconColor = PureWhite
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = MetallicBorder,
                            selectedBorderColor = ElectricSportOrange,
                            borderWidth = 1.dp,
                            selectedBorderWidth = 1.5.dp
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun Step4PhotoAndGoalView(
    baselinePhotoUri: String?,
    goalType: DietGoal,
    onPickPhoto: () -> Unit,
    onRemovePhoto: () -> Unit,
    onSelectGoal: (DietGoal) -> Unit
) {
    Text(
        "Paso 4: Captura de Foto Inicial & Meta a 3 Meses",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = TextPrimaryWhite
    )
    Text(
        "Registra tu foto inicial de punto de partida (en ropa deportiva o bañador) para habilitar la comparación automática a los 90 días:",
        style = MaterialTheme.typography.bodySmall,
        color = LightPlatinum
    )

    // Photo Box
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSteelAccent),
        border = BorderStroke(1.dp, MetallicBorder)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (baselinePhotoUri != null) {
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(2.dp, ElectricSportOrange, RoundedCornerShape(12.dp))
                ) {
                    AsyncImage(
                        model = baselinePhotoUri,
                        imageLoader = AppCoilImageLoader.get(LocalContext.current),
                        contentDescription = "Foto Inicial",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onPickPhoto,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, MetallicGray),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimaryWhite)
                    ) {
                        Text("Cambiar Foto")
                    }
                    TextButton(onClick = onRemovePhoto) {
                        Text("Eliminar", color = MaterialTheme.colorScheme.error)
                    }
                }
            } else {
                Icon(
                    imageVector = Icons.Rounded.AddAPhoto,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = ElectricSportOrange
                )
                Text(
                    "Subir Foto de Progreso Inicial",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryWhite
                )
                Text(
                    "Recomendado: Ropa deportiva / Bañador para visibilidad muscular",
                    style = MaterialTheme.typography.bodySmall,
                    color = LightPlatinum
                )
                Button(
                    onClick = onPickPhoto,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ElectricSportOrange,
                        contentColor = PureWhite
                    )
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CameraAlt,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Seleccionar / Tomar Foto", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(8.dp))

    Text(
        "Meta Principal de Nutrición & Rendimiento:",
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = TextPrimaryWhite
    )
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        DietGoal.entries.forEach { goal ->
            val isSelected = goal == goalType
            FilterChip(
                selected = isSelected,
                onClick = { onSelectGoal(goal) },
                label = {
                    Text(
                        text = goal.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.padding(vertical = 4.dp, horizontal = 2.dp)
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = DarkSteelAccent,
                    labelColor = LightPlatinum,
                    selectedContainerColor = ElectricSportOrange,
                    selectedLabelColor = PureWhite
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = isSelected,
                    borderColor = MetallicBorder,
                    selectedBorderColor = ElectricSportOrange,
                    borderWidth = 1.dp,
                    selectedBorderWidth = 1.5.dp
                )
            )
        }
    }
}
