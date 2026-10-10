package com.example.personalsport.ui.planner

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import com.example.personalsport.data.provider.ExerciseGifDatasetProvider
import com.example.personalsport.ui.components.ConfettiCelebration
import com.example.personalsport.util.AppCoilImageLoader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyWorkoutCard(
    dayName: String,
    title: String,
    isRestDay: Boolean = false,
    estimatedCaloriesBurned: Int = 0,
    exercises: List<PlannedExerciseStep>,
    completedExerciseIds: Set<String> = emptySet(),
    onExerciseClick: (PlannedExerciseStep) -> Unit,
    onToggleComplete: (PlannedExerciseStep) -> Unit,
    onSaveToLog: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(true) }

    val totalCount = exercises.size
    val completedCount = exercises.count { completedExerciseIds.contains(it.id) || it.isCompleted }
    val progressPercentage = if (totalCount > 0) (completedCount * 100) / totalCount else 0
    val isFullyCompleted = totalCount > 0 && completedCount == totalCount

    // Celebration Trigger State
    var celebratedKeys by remember { mutableStateOf(setOf<String>()) }
    var showCelebrationDialog by remember { mutableStateOf(false) }
    var isInitialized by remember { mutableStateOf(false) }

    val cardKey = "${dayName}_${title}"

    LaunchedEffect(cardKey, completedCount, totalCount) {
        if (!isInitialized) {
            if (isFullyCompleted) {
                celebratedKeys = celebratedKeys + cardKey
            }
            isInitialized = true
        } else {
            if (isFullyCompleted && !celebratedKeys.contains(cardKey)) {
                celebratedKeys = celebratedKeys + cardKey
                showCelebrationDialog = true
            } else if (!isFullyCompleted && celebratedKeys.contains(cardKey)) {
                celebratedKeys = celebratedKeys - cardKey
            }
        }
    }

    if (showCelebrationDialog) {
        ConfettiCelebration(
            totalExercisesFinished = totalCount,
            totalEstimatedCaloriesBurned = estimatedCaloriesBurned,
            onDismiss = { showCelebrationDialog = false }
        )
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isRestDay) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            else if (isFullyCompleted) MaterialTheme.colorScheme.surfaceContainerHigh
            else MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(20.dp),
        border = if (isFullyCompleted) BorderStroke(2.dp, Color(0xFF4CAF50)) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(
                                if (isRestDay) MaterialTheme.colorScheme.secondaryContainer
                                else if (isFullyCompleted) Color(0xFF4CAF50).copy(alpha = 0.2f)
                                else MaterialTheme.colorScheme.primaryContainer
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when {
                                isRestDay -> Icons.Rounded.Bed
                                isFullyCompleted -> Icons.Rounded.CheckCircle
                                else -> Icons.Rounded.FitnessCenter
                            },
                            contentDescription = null,
                            tint = when {
                                isRestDay -> MaterialTheme.colorScheme.onSecondaryContainer
                                isFullyCompleted -> Color(0xFF4CAF50)
                                else -> MaterialTheme.colorScheme.primary
                            },
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "$dayName • $title",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (estimatedCaloriesBurned > 0) {
                            Text(
                                text = "Gasto estimado: ~$estimatedCaloriesBurned kcal",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                if (!isRestDay && totalCount > 0) {
                    IconButton(onClick = { expanded = !expanded }) {
                        Icon(
                            imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                            contentDescription = "Expandir"
                        )
                    }
                }
            }

            // Daily Routine Progress Bar Section
            if (!isRestDay && totalCount > 0) {
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
                            color = if (isFullyCompleted) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (isFullyCompleted) {
                            Surface(
                                color = Color(0xFF4CAF50).copy(alpha = 0.15f),
                                contentColor = Color(0xFF4CAF50),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(12.dp))
                                    Text("✔ Día Completado", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    LinearProgressIndicator(
                        progress = { if (totalCount > 0) completedCount.toFloat() / totalCount else 0f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = Color(0xFF4CAF50),
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }

            // Exercise Cards List
            if (!isRestDay) {
                AnimatedVisibility(visible = expanded) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        exercises.forEach { step ->
                            val isCompleted = completedExerciseIds.contains(step.id) || step.isCompleted
                            val stepWithStatus = step.copy(isCompleted = isCompleted)

                            DailyExerciseItemCard(
                                step = stepWithStatus,
                                onClick = { onExerciseClick(stepWithStatus) },
                                onToggleComplete = { onToggleComplete(stepWithStatus) }
                            )
                        }

                        onSaveToLog?.let { saveAction ->
                            Button(
                                onClick = saveAction,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            ) {
                                Icon(Icons.Rounded.BookmarkAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Guardar $dayName en Registro de Entrenamientos", style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }
            } else {
                Text(
                    text = "Día de descanso activo y recuperación muscular para regenerar fibras.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun DailyExerciseItemCard(
    step: PlannedExerciseStep,
    onClick: () -> Unit,
    onToggleComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val gifUrl = remember(step.title, step.imageUrl) {
        if (!step.imageUrl.isNullOrBlank()) {
            step.imageUrl
        } else {
            ExerciseGifDatasetProvider.getGifUrlForExercise(step.title)
        }
    }

    val datasetEntry = remember(step.title) {
        ExerciseGifDatasetProvider.findExercise(step.title)
    }

    Card(
        modifier = modifier
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
            // Checkbox Icon Button for direct toggle
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
                    contentDescription = if (step.isCompleted) "Completado" else "Iniciar",
                    tint = if (step.isCompleted) Color.White else MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Exercise GIF / Media Thumbnail Container (Clicking opens ExerciseDetailDialog)
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

            // Exercise Title & Info (Clicking opens ExerciseDetailDialog)
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
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = if (step.isCompleted) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f, fill = false)
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

                val subInfo = datasetEntry?.targetMuscleEs ?: step.targetMuscle
                Text(
                    text = "Músculo: $subInfo",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                val eqName = datasetEntry?.equipmentEs ?: step.equipmentName
                if (eqName.isNotBlank() && eqName != "Peso Corporal") {
                    Text(
                        text = "Máquina: $eqName",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Set/Rep Target Badge (Clicking opens ExerciseDetailDialog)
            Badge(
                containerColor = if (step.isCompleted) {
                    Color(0xFF4CAF50).copy(alpha = 0.2f)
                } else {
                    MaterialTheme.colorScheme.secondaryContainer
                },
                modifier = Modifier.clickable { onClick() }
            ) {
                Text(
                    text = step.durationOrReps,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (step.isCompleted) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                    fontWeight = FontWeight.SemiBold
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
