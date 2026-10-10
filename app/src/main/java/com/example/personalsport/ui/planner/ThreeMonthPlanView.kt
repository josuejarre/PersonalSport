package com.example.personalsport.ui.planner

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.personalsport.ai.DayPlan
import com.example.personalsport.ai.ThreeMonthPlan
import com.example.personalsport.ai.WorkoutBlock
import com.example.personalsport.util.AppCoilImageLoader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThreeMonthPlanView(
    plan: ThreeMonthPlan,
    selectedWeekNum: Int,
    onSelectWeek: (Int) -> Unit,
    onSaveDayToLog: (DayPlan) -> Unit,
    completedExerciseIds: Set<String> = emptySet(),
    onExerciseClick: (PlannedExerciseStep) -> Unit = {},
    onToggleComplete: (PlannedExerciseStep) -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Find current week plan
    val currentWeek = plan.months.flatMap { it.weeks }.find { it.weekNumber == selectedWeekNum }
        ?: plan.months.first().weeks.first()

    val currentMonth = plan.months.find { m -> m.weeks.any { w -> w.weekNumber == selectedWeekNum } }
        ?: plan.months.first()

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Main Plan Header Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            plan.title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Plan Estructurado de 90 Días (12 Semanas)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Badge(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ) {
                        Text(
                            "12 Semanas",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                HorizontalDivider()

                // Caloric Validation Status Badge
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Verified,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                plan.caloricValidation.statusTitle,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                plan.caloricValidation.statusMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                "Consejo Nutricional: ${plan.caloricValidation.recommendation}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // 3-Month Photo Milestone Checkpoint Status
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (plan.baselinePhotoUri != null) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                            ) {
                                AsyncImage(
                                    model = plan.baselinePhotoUri,
                                    imageLoader = AppCoilImageLoader.get(LocalContext.current),
                                    contentDescription = "Baseline Photo",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.secondaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Rounded.CameraAlt, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Meta a 3 Meses & Checkpoint de Fotos",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                if (plan.baselinePhotoUri != null)
                                    "Foto inicial registrada. Próximo test de foto comparativa al finalizar la Semana 12."
                                else
                                    "Sin foto inicial. Registra fotos en Progreso para habilitar el comparador a los 90 días.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Month Indicator Tabs
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    currentMonth.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    currentMonth.focusDescription,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                HorizontalDivider()

                // Week Selector Chips (1-12)
                Text("Selecciona la Semana (1 a 12):", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(12) { index ->
                        val wNum = index + 1
                        val isSelected = wNum == selectedWeekNum
                        FilterChip(
                            selected = isSelected,
                            onClick = { onSelectWeek(wNum) },
                            label = { Text("Semana $wNum") },
                            leadingIcon = if (isSelected) {
                                { Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null
                        )
                    }
                }
            }
        }

        // Current Week Workouts Breakdown
        Text(
            "${currentWeek.title} • Rutina Diaria",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 4.dp)
        )

        currentWeek.days.forEach { dayPlan ->
            DailyWorkoutCard(
                dayName = dayPlan.dayName,
                title = dayPlan.title,
                isRestDay = dayPlan.isRestDay,
                estimatedCaloriesBurned = dayPlan.estimatedCaloriesBurned,
                exercises = dayPlan.blocks.flatMap { it.exercises },
                completedExerciseIds = completedExerciseIds,
                onExerciseClick = onExerciseClick,
                onToggleComplete = onToggleComplete,
                onSaveToLog = { onSaveDayToLog(dayPlan) }
            )
        }
    }
}
