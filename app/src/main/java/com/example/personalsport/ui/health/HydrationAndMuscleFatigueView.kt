package com.example.personalsport.ui.health

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

/**
 * Nivel de intensidad del ejercicio para estimación de tasa de sudoración ([ExerciseIntensity]).
 *
 * @property label Etiqueta descriptiva en español.
 * @property sweatRateMlPerHour Tasa estimada de pérdida de sudor por hora en mililitros.
 */
enum class ExerciseIntensity(val label: String, val sweatRateMlPerHour: Int) {
    BAJA("Baja / Caminata", 300),
    MODERADA("Moderada / Trote", 600),
    ALTA("Alta / Series & HIIT", 1000),
    EXTREMA("Extrema / Maratón & Ciclismo", 1500)
}

/**
 * Estado de fatiga y recuperación de un grupo muscular ([MuscleGroupStatus]).
 *
 * @property id Identificador del músculo.
 * @property name Nombre legible en español.
 * @property viewArea Vista anatómica ("Frontal" o "Posterior").
 * @property recoveryPercent Porcentaje de recuperación (0 a 100%).
 * @property hoursRemaining Horas de descanso restantes.
 * @property primaryExercisesWorked Ejercicios principales causantes del estímulo.
 * @property stretchRecommendation Consejo de estiramiento y liberación miofascial.
 */
data class MuscleGroupStatus(
    val id: String,
    val name: String,
    val viewArea: String,
    val recoveryPercent: Int,
    val hoursRemaining: Int,
    val primaryExercisesWorked: String,
    val stretchRecommendation: String
) {
    val statusColor: Color
        get() = when {
            recoveryPercent >= 75 -> Color(0xFF2E7D32)
            recoveryPercent >= 40 -> Color(0xFFF57F17)
            else -> Color(0xFFC62828)
        }

    val statusText: String
        get() = when {
            recoveryPercent >= 75 -> "100% Regenerado (Óptimo)"
            recoveryPercent >= 40 -> "Fatiga Moderada"
            else -> "Alta Fatiga (Descanso)"
        }
}

/**
 * Vista de Calculadora de Hidratación, Tasa de Sudoración y Mapa de Calor Anatómico de Fatiga Muscular ([HydrationAndMuscleFatigueView]).
 *
 * **Manejo de Estado e Interfaz**:
 * - Mantiene estados locales reactivos para el registro de mililitros de agua y miligramos de sodio/electrolitos.
 * - Calcula dinámicamente la pérdida por sudor combinando la duración, temperatura ambiente y nivel de intensidad.
 * - Muestra una cuadrícula anatómica de 10 grupos musculares con indicadores de color de recuperación.
 *
 * @param modifier Modificador de diseño Compose.
 */
@Composable
fun HydrationAndMuscleFatigueView(
    modifier: Modifier = Modifier
) {
    var waterLoggedMl by remember { mutableIntStateOf(1750) }
    var electrolytesLoggedMg by remember { mutableIntStateOf(600) }

    var durationMins by remember { mutableIntStateOf(60) }
    var ambientTempC by remember { mutableIntStateOf(26) }
    var intensity by remember { mutableStateOf(ExerciseIntensity.ALTA) }

    // Cálculo dinámico de sudoración y electrolitos
    val sweatLossMl = remember(durationMins, ambientTempC, intensity) {
        val baseRate = intensity.sweatRateMlPerHour
        val tempFactor = if (ambientTempC > 22) 1.0 + (ambientTempC - 22) * 0.03 else 1.0
        ((baseRate * (durationMins / 60.0)) * tempFactor).toInt()
    }

    val targetWaterMl = 2500 + sweatLossMl
    val targetSodiumMg = 1000 + (sweatLossMl * 0.5).toInt()
    val waterProgress = (waterLoggedMl.toFloat() / targetWaterMl.toFloat()).coerceIn(0f, 1f)

    var selectedMuscleGroup by remember { mutableStateOf<MuscleGroupStatus?>(null) }

    val muscleGroups = remember {
        listOf(
            MuscleGroupStatus("chest", "Pecho / Pectorales", "Frontal", 30, 28, "Press Banca, Flexiones", "Estiramiento en marco de puerta"),
            MuscleGroupStatus("quads", "Cuádriceps", "Frontal", 45, 18, "Sentadilla, Zancadas", "Estiramiento de cuádriceps de pie"),
            MuscleGroupStatus("abs", "Abdomen / Core", "Frontal", 90, 0, "Planchas, Crunches", "Postura de Cobra"),
            MuscleGroupStatus("shoulders", "Hombros / Deltoides", "Frontal", 85, 0, "Press Militar, Elevaciones", "Estiramiento de hombro cruzado"),
            MuscleGroupStatus("biceps", "Bíceps", "Frontal", 25, 34, "Curl de Bíceps, Dominadas Supinas", "Extensión de brazo contra pared"),
            MuscleGroupStatus("lats", "Dorsales / Espalda Alta", "Posterior", 35, 24, "Dominadas, Remo con Barra", "Estiramiento de gato-vaca"),
            MuscleGroupStatus("hamstrings", "Isquiotibiales", "Posterior", 60, 12, "Peso Muerto, Femoral Tumbado", "Inclinación hacia adelante de pie"),
            MuscleGroupStatus("glutes", "Glúteos", "Posterior", 80, 0, "Hip Thrust, Zancadas", "Estiramiento de piramidal / Figura 4"),
            MuscleGroupStatus("triceps", "Tríceps", "Posterior", 95, 0, "Fondos, Press Francés", "Estiramiento por encima de la cabeza"),
            MuscleGroupStatus("calves", "Gemelos / Pantorrillas", "Posterior", 100, 0, "Elevación de Talones", "Estiramiento en escalón")
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Sección 1: Rastreador de Hidratación & Electrolitos
        HydrationTrackerCard(
            waterLoggedMl = waterLoggedMl,
            targetWaterMl = targetWaterMl,
            sweatLossMl = sweatLossMl,
            electrolytesLoggedMg = electrolytesLoggedMg,
            targetSodiumMg = targetSodiumMg,
            waterProgress = waterProgress,
            durationMins = durationMins,
            ambientTempC = ambientTempC,
            intensity = intensity,
            onAddWater = { ml -> waterLoggedMl += ml },
            onAddElectrolytes = { mg -> electrolytesLoggedMg += mg },
            onUpdateParams = { d, t, i ->
                durationMins = d
                ambientTempC = t
                intensity = i
            }
        )

        // Sección 2: Mapa de Calor Anatómico de Fatiga Muscular
        MuscleFatigueHeatmapCard(
            muscleGroups = muscleGroups,
            onSelectMuscle = { selectedMuscleGroup = it }
        )
    }

    selectedMuscleGroup?.let { muscle ->
        MuscleDetailDialog(
            muscleGroup = muscle,
            onDismiss = { selectedMuscleGroup = null }
        )
    }
}

@Composable
private fun HydrationTrackerCard(
    waterLoggedMl: Int,
    targetWaterMl: Int,
    sweatLossMl: Int,
    electrolytesLoggedMg: Int,
    targetSodiumMg: Int,
    waterProgress: Float,
    durationMins: Int,
    ambientTempC: Int,
    intensity: ExerciseIntensity,
    onAddWater: (Int) -> Unit,
    onAddElectrolytes: (Int) -> Unit,
    onUpdateParams: (Int, Int, ExerciseIntensity) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 3.dp,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF0288D1).copy(alpha = 0.15f),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.WaterDrop,
                                contentDescription = null,
                                tint = Color(0xFF0288D1),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Column {
                        Text(
                            text = "Calculadora de Hidratación & Electrolitos",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Pérdida por sudor: $sweatLossMl ml ($durationMins min a $ambientTempC°C)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF0288D1).copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "${(waterProgress * 100).toInt()}%",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0288D1),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            // Barra de Progreso de Agua
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Agua: $waterLoggedMl / $targetWaterMl ml",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Electrolitos: $electrolytesLoggedMg / $targetSodiumMg mg Na+",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFE65100),
                        fontWeight = FontWeight.Bold
                    )
                }

                LinearProgressIndicator(
                    progress = { waterProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp)),
                    color = Color(0xFF0288D1),
                    trackColor = Color(0xFFB3E5FC)
                )
            }

            // Botones de Registro Rápido
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { onAddWater(250) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("+250 ml")
                }
                OutlinedButton(
                    onClick = { onAddWater(500) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("+500 ml")
                }
                Button(
                    onClick = { onAddElectrolytes(300) },
                    modifier = Modifier.weight(1.2f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100))
                ) {
                    Text("+300mg Na+")
                }
            }
        }
    }
}

@Composable
private fun MuscleFatigueHeatmapCard(
    muscleGroups: List<MuscleGroupStatus>,
    onSelectMuscle: (MuscleGroupStatus) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 3.dp,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AccessibilityNew,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(
                            text = "Mapa de Calor Anatómico & Fatiga",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Estado de recuperación muscular en tiempo real",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Leyenda de Colores
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                HeatmapLegendItem(color = Color(0xFFC62828), label = "Alta Fatiga")
                HeatmapLegendItem(color = Color(0xFFF57F17), label = "Moderada")
                HeatmapLegendItem(color = Color(0xFF2E7D32), label = "100% Regenerado")
            }

            // Cuadrícula Anatómica de Músculos
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(muscleGroups, key = { it.id }) { muscle ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = muscle.statusColor.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, muscle.statusColor),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onSelectMuscle(muscle) }
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = muscle.name,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${muscle.recoveryPercent}% • ${muscle.hoursRemaining}h rest.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Surface(
                                shape = CircleShape,
                                color = muscle.statusColor,
                                modifier = Modifier.size(12.dp)
                            ) {}
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HeatmapLegendItem(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = color,
            modifier = Modifier.size(10.dp)
        ) {}
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun MuscleDetailDialog(
    muscleGroup: MuscleGroupStatus,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = muscleGroup.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Vista ${muscleGroup.viewArea}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = "Cerrar")
                    }
                }

                HorizontalDivider()

                // Estado de Recuperación
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = muscleGroup.statusColor.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, muscleGroup.statusColor)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = muscleGroup.statusText,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = muscleGroup.statusColor
                        )
                        Text(
                            text = "${muscleGroup.recoveryPercent}%",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = muscleGroup.statusColor
                        )
                    }
                }

                Text(
                    text = "Ejercicios Causantes: ${muscleGroup.primaryExercisesWorked}",
                    style = MaterialTheme.typography.bodySmall
                )

                Text(
                    text = "Recomendación de Estiramiento & Recuperación:\n${muscleGroup.stretchRecommendation}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Entendido")
                }
            }
        }
    }
}
