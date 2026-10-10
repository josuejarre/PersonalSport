package com.example.personalsport.ui.planner

import android.os.Build
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.personalsport.data.model.GpsRouteUtils
import com.example.personalsport.data.provider.ExerciseGifDatasetProvider
import com.example.personalsport.data.provider.ExerciseJsonDatasetParser
import com.example.personalsport.data.provider.ExerciseWebScraperEngine
import com.example.personalsport.data.provider.NanoBananaAssetProvider
import com.example.personalsport.data.provider.NanoBananaExerciseImageGenerator
import com.example.personalsport.data.provider.RealisticExerciseIllustrationGenerator
import com.example.personalsport.ui.components.GoogleMapRouteWindow
import com.example.personalsport.util.AppCoilImageLoader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseDetailDialog(
    step: PlannedExerciseStep,
    onDismiss: () -> Unit,
    onToggleComplete: (PlannedExerciseStep) -> Unit
) {
    val context = LocalContext.current

    val datasetEntry = remember(step.title) {
        ExerciseGifDatasetProvider.findExercise(step.title)
    }

    val gifUrl = remember(step.title, step.imageUrl, step.targetMuscle, step.category, context) {
        if (!step.imageUrl.isNullOrBlank()) {
            if (!ExerciseJsonDatasetParser.isKnownExercise(step.title) &&
                (step.imageUrl == ExerciseJsonDatasetParser.FALLBACK_GIF_URL ||
                        step.imageUrl == ExerciseGifDatasetProvider.FALLBACK_GIF_URL)
            ) {
                RealisticExerciseIllustrationGenerator.generateRealisticIllustration(
                    context = context,
                    exerciseName = step.title,
                    targetMuscle = step.targetMuscle
                )
            } else {
                step.imageUrl
            }
        } else {
            ExerciseJsonDatasetParser.getFullGifUrl(
                query = step.title,
                context = context,
                categoryHint = step.category,
                targetMuscleHint = step.targetMuscle
            )
        }
    }

    val instructionSteps = remember(step.title) {
        ExerciseJsonDatasetParser.getInstructionStepsEs(step.title)
    }

    val targetMuscleText = datasetEntry?.targetMuscleEs
        ?: step.targetMuscle.ifBlank { "Cuerpo Completo" }

    val equipmentText = datasetEntry?.equipmentEs
        ?: step.equipmentName.ifBlank { "Peso Corporal" }

    val instructionsText = remember(step.title, datasetEntry, instructionSteps) {
        if (instructionSteps.isNotEmpty()) {
            instructionSteps.mapIndexed { idx, s -> "${idx + 1}. $s" }.joinToString("\n")
        } else {
            datasetEntry?.instructionsEs
                ?: step.instructions.ifBlank { "Ejecuta el ejercicio con técnica controlada, postura firme y respiración sostenida." }
        }
    }

    val isOpenWaterSwimStep = remember(step.title, step.category) {
        step.title.contains("aguas abiertas", ignoreCase = true) ||
                step.title.contains("open water", ignoreCase = true) ||
                step.category.contains("aguas abiertas", ignoreCase = true)
    }

    val isOutdoorRunStep = remember(step.title, step.category, step.equipmentName) {
        step.title.contains("carrera", ignoreCase = true) ||
                step.title.contains("run", ignoreCase = true) ||
                step.title.contains("atletismo", ignoreCase = true) ||
                step.title.contains("trote", ignoreCase = true) ||
                step.title.contains("sprint", ignoreCase = true) ||
                step.title.contains("rodaje", ignoreCase = true) ||
                step.title.contains("fartlek", ignoreCase = true) ||
                step.title.contains("intervalo", ignoreCase = true) ||
                step.category.contains("carrera", ignoreCase = true) ||
                step.category.contains("atletismo", ignoreCase = true) ||
                step.equipmentName.contains("pista", ignoreCase = true) ||
                step.equipmentName.contains("asfalto", ignoreCase = true)
    }

    val routeGpsJson = remember(step, isOpenWaterSwimStep, isOutdoorRunStep) {
        if (isOpenWaterSwimStep) {
            GpsRouteUtils.generateSampleOpenWaterSwimRouteJson()
        } else if (isOutdoorRunStep) {
            GpsRouteUtils.generateSampleRunRouteJson()
        } else null
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.90f)
                .padding(16.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Top Title & Close Button Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = step.title,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (datasetEntry != null && datasetEntry.nameEn != step.title) {
                            Text(
                                text = "Nombre en Inglés: ${datasetEntry.nameEn}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Cerrar",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Prominent Image / GIF Display
                val isRealisticImage = remember(gifUrl) {
                    RealisticExerciseIllustrationGenerator.isRealisticIllustration(gifUrl) ||
                            NanoBananaExerciseImageGenerator.isNanoBananaImage(gifUrl)
                }

                val isScrapedImage = remember(gifUrl) {
                    ExerciseWebScraperEngine.isScrapedImage(gifUrl)
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            if (isRealisticImage || isScrapedImage) {
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFFFFFFFF),
                                        Color(0xFFF8FAFC)
                                    )
                                )
                            } else {
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xFF1C2229),
                                        Color(0xFF0F1318)
                                    )
                                )
                            }
                        )
                        .border(
                            BorderStroke(
                                1.dp,
                                if (isRealisticImage || isScrapedImage) {
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color(0xFFFF6500),
                                            Color(0xFFFF3D00)
                                        )
                                    )
                                } else {
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color(0xFF3B4654),
                                            Color(0xFF181E24)
                                        )
                                    )
                                }
                            ),
                            shape = RoundedCornerShape(20.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    val sharedImageLoader = AppCoilImageLoader.get(context)

                    var currentGifData by remember(gifUrl) { mutableStateOf<Any>(gifUrl) }

                    val imageRequest: ImageRequest = remember(currentGifData, context) {
                        ImageRequest.Builder(context)
                            .data(currentGifData)
                            .crossfade(true)
                            .build()
                    }

                    var isLoading by remember(currentGifData) { mutableStateOf(true) }
                    var isError by remember(currentGifData) { mutableStateOf(false) }

                    if (isLoading) {
                        ShimmerLoadingBox()
                    }

                    if (isError) {
                        ExerciseIllustrationGraphic(
                            category = step.category,
                            targetMuscle = targetMuscleText,
                            equipmentName = equipmentText
                        )
                    }

                    AsyncImage(
                        model = imageRequest,
                        imageLoader = sharedImageLoader,
                        contentDescription = step.title,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        contentScale = ContentScale.Fit,
                        onLoading = {
                            isLoading = true
                            isError = false
                        },
                        onSuccess = {
                            isLoading = false
                            isError = false
                        },
                        onError = {
                            val realisticFallbackUrl = RealisticExerciseIllustrationGenerator.generateRealisticIllustration(
                                context = context,
                                exerciseName = step.title,
                                targetMuscle = step.targetMuscle
                            )
                            if (currentGifData != realisticFallbackUrl) {
                                currentGifData = realisticFallbackUrl
                            } else {
                                isLoading = false
                                isError = true
                            }
                        }
                    )

                    // Badge over the image: "🏋️ Ilustración Biomecánica Realista 3D", "🌐 Ilustración Específica" or "🎬 Animación en Vivo"
                    Surface(
                        color = Color(0xFF1C2229),
                        contentColor = Color.White,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(12.dp)
                            .border(
                                width = 1.dp,
                                color = if (isRealisticImage || isScrapedImage) Color(0xFFFF6500) else Color(0xFF3B4654),
                                shape = RoundedCornerShape(12.dp)
                            )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val badgeText = when {
                                isRealisticImage -> RealisticExerciseIllustrationGenerator.BADGE_TEXT
                                isScrapedImage -> "🌐 Ilustración Específica de ${step.title}"
                                else -> "🎬 Animación en Vivo"
                            }
                            Text(
                                text = badgeText,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    // Completion Badge Banner if finished
                    if (step.isCompleted) {
                        Surface(
                            color = Color(0xFF4CAF50),
                            contentColor = Color.White,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text("Completado", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                if (!routeGpsJson.isNullOrBlank()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Map,
                                    contentDescription = null,
                                    tint = Color(0xFFFF6500),
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Mapa de Ruta GPS & Métricas Integradas",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            GoogleMapRouteWindow(
                                routeGpsPoints = routeGpsJson,
                                durationMinutes = 35,
                                height = 190.dp,
                                showMetricsOverlay = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // Prominent Realistic 3D Athlete Illustration Card
                val realisticIllustrationUrl = remember(step.title, step.targetMuscle, context) {
                    RealisticExerciseIllustrationGenerator.generateRealisticIllustration(
                        context = context,
                        exerciseName = step.title,
                        targetMuscle = step.targetMuscle
                    )
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFFFF)),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, Color(0xFFFF6500))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            color = Color(0xFF1C2229),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFFFF6500))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.FitnessCenter,
                                    contentDescription = null,
                                    tint = Color(0xFFFF6500),
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = RealisticExerciseIllustrationGenerator.BADGE_TEXT,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFF6500)
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFFFFFFFF)),
                            contentAlignment = Alignment.Center
                        ) {
                            val sharedImageLoader = AppCoilImageLoader.get(context)

                            val realisticImageRequest = remember(realisticIllustrationUrl, context) {
                                ImageRequest.Builder(context)
                                    .data(realisticIllustrationUrl)
                                    .crossfade(true)
                                    .build()
                            }

                            AsyncImage(
                                model = realisticImageRequest,
                                imageLoader = sharedImageLoader,
                                contentDescription = "Ilustración Biomecánica Realista 3D",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                        }
                    }
                }

                // Target Muscle & Category Chips
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Target Muscle Badge
                    SuggestionChip(
                        onClick = {},
                        modifier = Modifier.weight(1f),
                        label = {
                            Text(
                                text = "Músculo: $targetMuscleText",
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        icon = {
                            Icon(
                                imageVector = Icons.Rounded.FitnessCenter,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    )

                    // Category Badge
                    SuggestionChip(
                        onClick = {},
                        modifier = Modifier.weight(1f),
                        label = {
                            Text(
                                text = datasetEntry?.category ?: step.category.ifBlank { "Gimnasio" },
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        icon = {
                            Icon(
                                imageVector = Icons.Rounded.Category,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    )
                }

                HorizontalDivider()

                // Step-by-Step Instructions & Execution Guidance
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Rounded.MenuBook,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                "Explicación e Instrucciones de Ejecución",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text(
                            text = instructionsText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        if (step.description.isNotBlank()) {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Detalle adicional: ${step.description}",
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(10.dp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Equipment / Machine Info Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = getEquipmentIcon(equipmentText),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Equipamiento / Máquina Utilizada",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = equipmentText,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Series / Reps Target Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Rounded.Repeat,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Meta de Series / Repeticiones",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = step.durationOrReps,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Completion Toggle Action Button ("Marcar como Completado")
                Button(
                    onClick = {
                        onToggleComplete(step)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = if (step.isCompleted) {
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50),
                            contentColor = Color.White
                        )
                    }
                ) {
                    Icon(
                        imageVector = if (step.isCompleted) Icons.Rounded.RemoveCircleOutline else Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (step.isCompleted) "Desmarcar Ejercicio" else "Marcar como Completado",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun ShimmerLoadingBox(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerTranslation"
    )

    val shimmerColors = listOf(
        Color(0xFF1C2229),
        Color(0xFF2E3846),
        Color(0xFF1C2229)
    )

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(translateAnim - 200f, translateAnim - 200f),
        end = Offset(translateAnim, translateAnim)
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(brush),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 3.dp,
                modifier = Modifier.size(32.dp)
            )
            Text(
                text = "Cargando ilustración 3D HD...",
                style = MaterialTheme.typography.labelSmall,
                color = Color.LightGray
            )
        }
    }
}

@Composable
fun ExerciseIllustrationGraphic(
    category: String,
    targetMuscle: String,
    equipmentName: String
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                    .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getCategoryIllustrationIcon(category, equipmentName),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(44.dp)
                )
            }

            Text(
                text = "Guía Biomecánica: $targetMuscle",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Demostración para $equipmentName",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

fun getEquipmentIcon(equipmentName: String): ImageVector {
    val lower = equipmentName.lowercase()
    return when {
        lower.contains("polea") || lower.contains("cable") || lower.contains("lat machine") -> Icons.Rounded.PrecisionManufacturing
        lower.contains("barra") || lower.contains("mancuerna") || lower.contains("peso") || lower.contains("disco") -> Icons.Rounded.FitnessCenter
        lower.contains("pista") || lower.contains("carrera") || lower.contains("trote") || lower.contains("asfalto") -> Icons.Rounded.DirectionsRun
        lower.contains("piscina") || lower.contains("agua") -> Icons.Rounded.Pool
        lower.contains("bicicleta") || lower.contains("rodillo") -> Icons.Rounded.DirectionsBike
        else -> Icons.Rounded.AccessibilityNew
    }
}

fun getCategoryIllustrationIcon(category: String, equipmentName: String): ImageVector {
    val lower = (category + " " + equipmentName).lowercase()
    return when {
        lower.contains("corredor") || lower.contains("carrera") || lower.contains("atletismo") || lower.contains("sprint") || lower.contains("running") -> Icons.Rounded.DirectionsRun
        lower.contains("natación") || lower.contains("piscina") || lower.contains("swimming") -> Icons.Rounded.Pool
        lower.contains("fuerza") || lower.contains("polea") || lower.contains("máquina") || lower.contains("gym") -> Icons.Rounded.FitnessCenter
        lower.contains("calistenia") || lower.contains("dominadas") || lower.contains("flexiones") -> Icons.Rounded.AccessibilityNew
        else -> Icons.Rounded.SelfImprovement
    }
}
