package com.example.personalsport.ui.planner

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.personalsport.ai.AIRoutineWizardState
import com.example.personalsport.ai.DayPlan
import com.example.personalsport.ui.components.ConfettiCelebration
import com.example.personalsport.ui.music.CadenceMusicSyncCard
import com.example.personalsport.ai.LocalAIRoutineGenerator
import com.example.personalsport.ai.SwimDistanceSeriesCategory
import com.example.personalsport.util.AppCoilImageLoader
import com.example.personalsport.ai.WeekPlan
import com.example.personalsport.data.provider.ExerciseGifDatasetProvider
import com.example.personalsport.ui.theme.*
import kotlinx.coroutines.runBlocking

data class DisciplineBadgeInfo(
    val emoji: String,
    val label: String,
    val icon: ImageVector,
    val containerColor: Color,
    val contentColor: Color
)

fun getDisciplineBadgesForDay(dayPlan: DayPlan): List<DisciplineBadgeInfo> {
    if (dayPlan.isRestDay) {
        return listOf(
            DisciplineBadgeInfo(
                emoji = "🧘‍♂️",
                label = "Descanso Activo / Recuperación",
                icon = Icons.Rounded.SelfImprovement,
                containerColor = Color(0xFFFFF3E0),
                contentColor = Color(0xFFE65100)
            )
        )
    }

    val badges = mutableListOf<DisciplineBadgeInfo>()
    val allCategories = dayPlan.blocks.map { it.category.lowercase() }
    val allTitles = dayPlan.blocks.map { it.title.lowercase() }
    val allExercises = dayPlan.blocks.flatMap { it.exercises }
    val allEquipment = allExercises.mapNotNull { it.equipmentUsed?.lowercase() } +
            allExercises.mapNotNull { it.equipmentName.lowercase() } +
            allExercises.map { it.title.lowercase() }

    // 1. Pista de Atletismo (for track/interval running days)
    val hasTrackRunning = allCategories.any { it.contains("carrera") || it.contains("atletismo") || it.contains("fartlek") || (it.contains("aeróbico") && !it.contains("natación")) } ||
            allTitles.any { it.contains("carrera") || it.contains("atletismo") || it.contains("fartlek") || it.contains("series de atletismo") || it.contains("series x") || it.contains("pista") } ||
            allEquipment.any { it.contains("pista") || it.contains("atletismo") }

    if (hasTrackRunning) {
        badges.add(
            DisciplineBadgeInfo(
                emoji = "🏃‍♂️",
                label = "Pista de Atletismo",
                icon = Icons.Rounded.DirectionsRun,
                containerColor = ElectricSportOrange.copy(alpha = 0.15f),
                contentColor = Color(0xFFD84315)
            )
        )
    }

    // 2. Piscina (for swimming days)
    val hasSwimming = allCategories.any { it.contains("natación") || it.contains("piscina") } ||
            allTitles.any { it.contains("natación") } ||
            allEquipment.any { it.contains("piscina") || it.contains("pull-buoy") || it.contains("tabla") }

    if (hasSwimming) {
        badges.add(
            DisciplineBadgeInfo(
                emoji = "🏊‍♂️",
                label = "Piscina",
                icon = Icons.Rounded.Pool,
                containerColor = Color(0xFFE0F7FA),
                contentColor = Color(0xFF00838F)
            )
        )
    }

    // 3. Gimnasio / Musculación (for strength training days)
    val hasGymStrength = allCategories.any { it.contains("gimnasio") || it.contains("musculación") || it.contains("fuerza") || it.contains("calistenia") } ||
            allTitles.any { it.contains("gimnasio") || it.contains("fuerza") || it.contains("calistenia") || it.contains("musculación") } ||
            !dayPlan.muscleFocus.isNullOrBlank()

    if (hasGymStrength) {
        badges.add(
            DisciplineBadgeInfo(
                emoji = "🏋️‍♂️",
                label = "Gimnasio / Musculación",
                icon = Icons.Rounded.FitnessCenter,
                containerColor = Color(0xFFEDE7F6),
                contentColor = Color(0xFF5E35B1)
            )
        )
    }

    // 4. Spinning / Bici
    val hasSpinning = allCategories.any { it.contains("spinning") || it.contains("ciclismo") || it.contains("bici") } ||
            allTitles.any { it.contains("spinning") || it.contains("ciclismo") } ||
            allEquipment.any { it.contains("spinning") || it.contains("bicicleta") }

    if (hasSpinning) {
        badges.add(
            DisciplineBadgeInfo(
                emoji = "🚴‍♂️",
                label = "Spinning / Bici",
                icon = Icons.Rounded.DirectionsBike,
                containerColor = Color(0xFFE8F5E9),
                contentColor = Color(0xFF2E7D32)
            )
        )
    }

    // Fallback if no specific discipline matched and not rest day
    if (badges.isEmpty()) {
        badges.add(
            DisciplineBadgeInfo(
                emoji = "🏋️‍♂️",
                label = "Gimnasio / Musculación",
                icon = Icons.Rounded.FitnessCenter,
                containerColor = Color(0xFFEDE7F6),
                contentColor = Color(0xFF5E35B1)
            )
        )
    }

    return badges
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DisciplineAndMuscleBadges(
    dayPlan: DayPlan,
    modifier: Modifier = Modifier
) {
    val badges = remember(dayPlan) { getDisciplineBadgesForDay(dayPlan) }
    val muscleFocus = dayPlan.muscleFocus

    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Discipline Badges
        badges.forEach { badge ->
            Surface(
                color = badge.containerColor,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, badge.contentColor.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(text = badge.emoji, style = MaterialTheme.typography.bodyMedium)
                    Icon(
                        imageVector = badge.icon,
                        contentDescription = null,
                        tint = badge.contentColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = badge.label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = badge.contentColor
                    )
                }
            }
        }

        // Muscle Focus Badge (if not rest day or if muscle focus available)
        if (!dayPlan.isRestDay && !muscleFocus.isNullOrBlank()) {
            val labelText = if (muscleFocus.startsWith("Foco Muscular:", ignoreCase = true)) {
                muscleFocus
            } else {
                "Foco Muscular: $muscleFocus"
            }
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(text = "💪", style = MaterialTheme.typography.bodyMedium)
                    Icon(
                        imageVector = Icons.Rounded.FitnessCenter,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = labelText,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}

@Composable
fun WeeklyWorkoutDashboardView(
    modifier: Modifier = Modifier,
    weekPlan: WeekPlan? = null,
    selectedDay: String? = null,
    onDaySelected: (String) -> Unit = {},
    completedExerciseIds: Set<String> = emptySet(),
    onToggleExerciseComplete: (PlannedExerciseStep) -> Unit = {},
    onExerciseClick: (PlannedExerciseStep) -> Unit = {},
    onSaveDayToLog: ((DayPlan) -> Unit)? = null
) {
    var internalSelectedDay by remember { mutableStateOf(selectedDay ?: "Lunes") }
    val activeDay = selectedDay ?: internalSelectedDay

    val daysList = remember(weekPlan) {
        weekPlan?.days ?: getFallbackWeekDays()
    }

    val currentDayPlan = daysList.find { it.dayName.equals(activeDay, ignoreCase = true) }
        ?: daysList.first()

    val focusTitle = getFocusTitleForDay(currentDayPlan)
    val allExercises = currentDayPlan.blocks.flatMap { it.exercises }
    val totalCount = allExercises.size
    val completedCount = allExercises.count { completedExerciseIds.contains(it.id) || it.isCompleted }
    val progressPercentage = if (totalCount > 0) (completedCount * 100) / totalCount else 0
    val totalDurationMins = remember(allExercises) {
        calculateTotalDurationMinutes(allExercises)
    }

    // Celebration Trigger State
    var celebratedDays by remember { mutableStateOf(setOf<String>()) }
    var showCelebrationDialog by remember { mutableStateOf(false) }
    var isInitialized by remember { mutableStateOf(false) }

    val isFullyCompleted = totalCount > 0 && completedCount == totalCount
    val activeDayKey = "${currentDayPlan.dayName}_${currentDayPlan.title}"

    LaunchedEffect(activeDayKey, completedCount, totalCount) {
        if (!isInitialized) {
            if (isFullyCompleted) {
                celebratedDays = celebratedDays + activeDayKey
            }
            isInitialized = true
        } else {
            if (isFullyCompleted && !celebratedDays.contains(activeDayKey)) {
                celebratedDays = celebratedDays + activeDayKey
                showCelebrationDialog = true
            } else if (!isFullyCompleted && celebratedDays.contains(activeDayKey)) {
                celebratedDays = celebratedDays - activeDayKey
            }
        }
    }

    if (showCelebrationDialog) {
        ConfettiCelebration(
            totalExercisesFinished = totalCount,
            totalEstimatedCaloriesBurned = currentDayPlan.estimatedCaloriesBurned,
            onDismiss = { showCelebrationDialog = false }
        )
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Dashboard Main Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(ElectricSportOrange.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CalendarMonth,
                            contentDescription = null,
                            tint = ElectricSportOrange,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Dashboard de Entrenamiento Semanal",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Selecciona un día para acceder al desglose de sesión",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // 1. Weekday Selector Bar (Lunes, Martes, Miércoles, Jueves, Viernes, Sábado, Domingo)
            WeekdaySelectorBar(
                selectedDay = activeDay,
                daysList = daysList,
                onSelectDay = { day ->
                    internalSelectedDay = day
                    onDaySelected(day)
                }
            )

            // 2. Dedicated Day Window Card
            DayWindowCard(
                dayPlan = currentDayPlan,
                focusTitle = focusTitle,
                estimatedCalories = currentDayPlan.estimatedCaloriesBurned,
                totalDurationMinutes = totalDurationMins,
                totalExercises = totalCount,
                completedExercises = completedCount,
                progressPercentage = progressPercentage,
                allExercises = allExercises,
                completedExerciseIds = completedExerciseIds,
                onToggleComplete = onToggleExerciseComplete,
                onExerciseClick = onExerciseClick,
                onSaveDayToLog = onSaveDayToLog
            )

            // 3. Multi-Platform Cadence BPM Music Sync Bar
            CadenceMusicSyncCard()
        }
    }
}

@Composable
fun WeekdaySelectorBar(
    selectedDay: String,
    daysList: List<DayPlan> = emptyList(),
    onSelectDay: (String) -> Unit
) {
    val weekdays = listOf(
        "Lunes" to "LUN",
        "Martes" to "MAR",
        "Miércoles" to "MIÉ",
        "Jueves" to "JUE",
        "Viernes" to "VIE",
        "Sábado" to "SÁB",
        "Domingo" to "DOM"
    )

    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(weekdays) { (fullDay, shortDay) ->
            val isSelected = fullDay.equals(selectedDay, ignoreCase = true)
            val dayPlan = daysList.find { it.dayName.equals(fullDay, ignoreCase = true) }
            val badges = if (dayPlan != null) getDisciplineBadgesForDay(dayPlan) else emptyList()
            val emojis = badges.joinToString(" ") { it.emoji }

            Surface(
                onClick = { onSelectDay(fullDay) },
                shape = RoundedCornerShape(14.dp),
                color = if (isSelected) ElectricSportOrange else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                contentColor = if (isSelected) PureWhite else MaterialTheme.colorScheme.onSurfaceVariant,
                border = if (isSelected) BorderStroke(1.5.dp, ElectricSportOrange) else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = shortDay,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = fullDay,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                    if (emojis.isNotBlank()) {
                        Text(
                            text = emojis,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DayWindowCard(
    dayPlan: DayPlan,
    focusTitle: String,
    estimatedCalories: Int,
    totalDurationMinutes: Int,
    totalExercises: Int,
    completedExercises: Int,
    progressPercentage: Int,
    allExercises: List<PlannedExerciseStep>,
    completedExerciseIds: Set<String>,
    onToggleComplete: (PlannedExerciseStep) -> Unit,
    onExerciseClick: (PlannedExerciseStep) -> Unit,
    onSaveDayToLog: ((DayPlan) -> Unit)?
) {
    val isRestDay = dayPlan.isRestDay
    val isFullyCompleted = totalExercises > 0 && completedExercises == totalExercises

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isRestDay) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = if (isFullyCompleted) 2.dp else 1.dp,
            color = if (isFullyCompleted) Color(0xFF4CAF50) else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Day Focus Title Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = focusTitle,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (isRestDay) "Día de descanso activo y recuperación" else "Plan de entrenamiento diario",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (isFullyCompleted) {
                    Surface(
                        color = Color(0xFF4CAF50).copy(alpha = 0.15f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Rounded.Check, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(16.dp))
                            Text("✔ Completado", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
                        }
                    }
                }
            }

            // 1 & 2. Discipline Badges & Muscle Focus Badge
            DisciplineAndMuscleBadges(dayPlan = dayPlan)

            // Day Summary Header (Calories, Duration, Completion Progress Bar)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Rounded.LocalFireDepartment, contentDescription = null, tint = ElectricSportOrange, modifier = Modifier.size(18.dp))
                                Text("~$estimatedCalories kcal", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Rounded.Timer, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                Text("$totalDurationMinutes mins", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                            }
                        }

                        if (!isRestDay && totalExercises > 0) {
                            Text(
                                "$completedExercises de $totalExercises ($progressPercentage%)",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isFullyCompleted) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    if (!isRestDay && totalExercises > 0) {
                        LinearProgressIndicator(
                            progress = { completedExercises.toFloat() / totalExercises.toFloat() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = Color(0xFF4CAF50),
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }
            }

            HorizontalDivider()

            // List of Planned Exercises specifically for that day
            if (!isRestDay && allExercises.isNotEmpty()) {
                Text(
                    "Ejercicios Planificados",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    allExercises.forEach { step ->
                        val isCompleted = completedExerciseIds.contains(step.id) || step.isCompleted
                        val stepWithStatus = step.copy(isCompleted = isCompleted)

                        DashboardExerciseRow(
                            step = stepWithStatus,
                            onToggleComplete = { onToggleComplete(stepWithStatus) },
                            onClick = { onExerciseClick(stepWithStatus) }
                        )
                    }
                }

                onSaveDayToLog?.let { saveAction ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Button(
                        onClick = { saveAction(dayPlan) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    ) {
                        Icon(Icons.Rounded.BookmarkAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Guardar ${dayPlan.dayName} en Registro de Entrenamientos", style = MaterialTheme.typography.labelLarge)
                    }
                }
            } else if (isRestDay) {
                Row(
                    modifier = Modifier.padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Rounded.Bed, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                    Text(
                        "Día de descanso muscular activo, caminata ligera y movilidad articular.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun DashboardExerciseRow(
    step: PlannedExerciseStep,
    onToggleComplete: () -> Unit,
    onClick: () -> Unit
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
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (step.isCompleted) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            } else {
                MaterialTheme.colorScheme.surfaceContainerHigh
            }
        ),
        border = if (step.isCompleted) BorderStroke(1.5.dp, Color(0xFF4CAF50)) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Individual completion checkbox icon button
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
                    contentDescription = if (step.isCompleted) "Completado" else "Marcar completado",
                    tint = if (step.isCompleted) Color.White else MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Animated GIF Thumbnail Box (Clickable to open ExerciseDetailDialog)
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

            // Exercise Info: Title, Description, Equipment/Machine used (Clickable to open ExerciseDetailDialog)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onClick() }
            ) {
                Text(
                    text = step.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (step.isCompleted) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = step.description,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                val equipmentDisplay = when {
                    step.equipmentName.isNotBlank() && step.equipmentName != "Peso Corporal" -> step.equipmentName
                    !step.equipmentUsed.isNullOrBlank() -> step.equipmentUsed
                    else -> null
                }

                equipmentDisplay?.let { eq ->
                    Text(
                        text = "Máquina/Equipo: $eq",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Sets / Reps / Distances Badge (Clickable to open ExerciseDetailDialog)
            Badge(
                containerColor = if (step.isCompleted) Color(0xFF4CAF50).copy(alpha = 0.2f) else MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.clickable { onClick() }
            ) {
                Text(
                    text = step.durationOrReps,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (step.isCompleted) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                    fontWeight = FontWeight.Bold
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

private fun getFocusTitleForDay(dayPlan: DayPlan): String {
    if (dayPlan.isRestDay) {
        return "Domingo: Descanso Total & Recuperación Activa"
    }
    val title = dayPlan.title
    if (title.startsWith(dayPlan.dayName, ignoreCase = true)) {
        return title
    }
    return "${dayPlan.dayName}: $title"
}

private fun calculateTotalDurationMinutes(exercises: List<PlannedExerciseStep>): Int {
    if (exercises.isEmpty()) return 0
    var totalMins = 0
    for (ex in exercises) {
        val str = ex.durationOrReps
        val minsMatch = Regex("(\\d+)\\s*mins?").find(str)
        if (minsMatch != null) {
            totalMins += minsMatch.groupValues[1].toIntOrNull() ?: 10
        } else {
            totalMins += 10
        }
    }
    return if (totalMins > 0) totalMins else 45
}

private fun getFallbackWeekDays(): List<DayPlan> {
    val defaultWizardState = AIRoutineWizardState(
        swimSeriesCategory = SwimDistanceSeriesCategory.COMBINATION_SERIES
    )
    val plan = runBlocking { LocalAIRoutineGenerator.generate3MonthPlan(defaultWizardState) }
    return plan.months.first().weeks.first().days
}

@Preview
@Composable
fun WeeklyWorkoutDashboardViewPreview() {
    PersonalSportTheme {
        WeeklyWorkoutDashboardView()
    }
}