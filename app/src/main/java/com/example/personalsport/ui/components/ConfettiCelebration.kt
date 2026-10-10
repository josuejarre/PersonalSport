package com.example.personalsport.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.personalsport.ui.theme.PersonalSportTheme
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

// Color palette for confetti particles: Electric Orange, Gold, Green, Cyan, White, Magenta
val ConfettiElectricOrange = Color(0xFFFF6500)
val ConfettiGold = Color(0xFFFFD700)
val ConfettiGreen = Color(0xFF4CAF50)
val ConfettiCyan = Color(0xFF00BCD4)
val ConfettiWhite = Color(0xFFFFFFFF)
val ConfettiMagenta = Color(0xFFE91E63)

val ConfettiCelebrationColors = listOf(
    ConfettiElectricOrange,
    ConfettiGold,
    ConfettiGreen,
    ConfettiCyan,
    ConfettiWhite,
    ConfettiMagenta,
)

enum class ConfettiShape {
    RECTANGLE,
    CIRCLE,
    OVAL
}

data class ConfettiParticle(
    val id: Int,
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var rotation: Float,
    var vRot: Float,
    var sizeWidth: Float,
    var sizeHeight: Float,
    val color: Color,
    val shape: ConfettiShape = ConfettiShape.RECTANGLE,
    var alpha: Float = 1f
) {
    fun update(
        screenWidth: Float,
        screenHeight: Float,
        gravity: Float = 0.25f,
        drag: Float = 0.985f,
        wind: Float = 0.15f
    ) {
        x += vx + wind
        y += vy
        vy = (vy + gravity) * drag
        vx *= drag
        rotation = (rotation + vRot) % 360f

        // Loop particles to top if they fall off screen for continuous celebration animation
        if (screenHeight > 0f && (y > screenHeight + 40f)) {
            y = -20f
            x = (0f..screenWidth).random(Random(id + System.currentTimeMillis().toInt()))
            vy = (2f..8f).random(Random(id))
            vx = (-3f..3f).random(Random(id))
        }
    }
}

private fun ClosedRange<Float>.random(random: Random): Float =
    start + random.nextFloat() * (endInclusive - start)

object ConfettiPhysicsUtils {
    fun generateInitialParticles(
        count: Int = 120,
        screenWidth: Float = 1080f,
        screenHeight: Float = 1920f,
        random: Random = Random(42)
    ): List<ConfettiParticle> {
        val particles = ArrayList<ConfettiParticle>(count)
        val centerX = screenWidth / 2f
        val startY = screenHeight * 0.3f

        for (i in 0 until count) {
            val color = ConfettiCelebrationColors[i % ConfettiCelebrationColors.size]
            val angle = random.nextDouble(0.0, 2 * Math.PI)
            val speed = random.nextDouble(6.0, 26.0)
            val vx = (cos(angle) * speed).toFloat()
            val vy = (sin(angle) * speed - random.nextDouble(8.0, 20.0)).toFloat()

            val shape = when (i % 3) {
                0 -> ConfettiShape.RECTANGLE
                1 -> ConfettiShape.CIRCLE
                else -> ConfettiShape.OVAL
            }

            particles.add(
                ConfettiParticle(
                    id = i,
                    x = centerX + (random.nextFloat() - 0.5f) * 60f,
                    y = startY + (random.nextFloat() - 0.5f) * 30f,
                    vx = vx,
                    vy = vy,
                    rotation = random.nextFloat() * 360f,
                    vRot = (random.nextFloat() - 0.5f) * 14f,
                    sizeWidth = random.nextFloat() * 12f + 8f,
                    sizeHeight = random.nextFloat() * 16f + 10f,
                    color = color,
                    shape = shape,
                    alpha = 1f
                )
            )
        }
        return particles
    }

    fun updateParticlesList(
        particles: List<ConfettiParticle>,
        screenWidth: Float,
        screenHeight: Float,
        gravity: Float = 0.25f,
        drag: Float = 0.985f,
        wind: Float = 0.15f
    ) {
        particles.forEach { p ->
            p.update(screenWidth, screenHeight, gravity, drag, wind)
        }
    }

    fun isWorkoutTableCompleted(completedCount: Int, totalCount: Int): Boolean {
        return totalCount > 0 && completedCount == totalCount
    }
}

@Composable
fun ConfettiCelebration(
    totalExercisesFinished: Int,
    totalEstimatedCaloriesBurned: Int,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val particles = remember { mutableStateListOf<ConfettiParticle>() }
    var frameTick by remember { mutableLongStateOf(0L) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.70f)),
            contentAlignment = Alignment.Center
        ) {
            // Full Screen Confetti Canvas
            Canvas(
                modifier = Modifier.fillMaxSize()
            ) {
                val canvasWidth = size.width
                val canvasHeight = size.height

                if (particles.isEmpty() && canvasWidth > 0f && canvasHeight > 0f) {
                    particles.addAll(
                        ConfettiPhysicsUtils.generateInitialParticles(
                            count = 130,
                            screenWidth = canvasWidth,
                            screenHeight = canvasHeight
                        )
                    )
                }

                particles.forEach { particle ->
                    drawConfettiParticle(particle)
                }
            }

            // LaunchedEffect physics frame loop
            LaunchedEffect(particles.isNotEmpty()) {
                if (particles.isNotEmpty()) {
                    while (true) {
                        withFrameNanos {
                            ConfettiPhysicsUtils.updateParticlesList(
                                particles = particles,
                                screenWidth = 1080f,
                                screenHeight = 1920f
                            )
                            frameTick++
                        }
                    }
                }
            }

            // Congratulatory Material 3 Dialog Card
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.90f)
                    .padding(16.dp),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                ),
                border = BorderStroke(2.dp, ConfettiElectricOrange),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Top Celebration Icon Badge
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        ConfettiElectricOrange.copy(alpha = 0.3f),
                                        ConfettiGold.copy(alpha = 0.15f)
                                    )
                                )
                            )
                            .border(2.dp, ConfettiGold, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.EmojiEvents,
                            contentDescription = "Troféo de Enhorabuena",
                            tint = ConfettiGold,
                            modifier = Modifier.size(40.dp)
                        )
                    }

                    // Title
                    Text(
                        text = "🎉 ¡Enhorabuena! ¡Tabla de Entrenamiento Completada!",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Subtitle
                    Text(
                        text = "Has cumplido el 100% de los ejercicios de hoy. ¡Excelente trabajo de disciplina y superación!",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    // Stats Summary Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Total Exercises Finished
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.CheckCircle,
                                        contentDescription = null,
                                        tint = ConfettiGreen,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = totalExercisesFinished.toString(),
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Text(
                                    text = "Ejercicios",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            VerticalDivider(
                                modifier = Modifier.height(36.dp),
                                color = MaterialTheme.colorScheme.outlineVariant
                            )

                            // Calories Burned
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.LocalFireDepartment,
                                        contentDescription = null,
                                        tint = ConfettiElectricOrange,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "~$totalEstimatedCaloriesBurned",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Text(
                                    text = "kcal Estimadas",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Close Button
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ConfettiElectricOrange,
                            contentColor = Color.White
                        )
                    ) {
                        Text(
                            text = "¡Seguir con el Día!",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

private fun DrawScope.drawConfettiParticle(p: ConfettiParticle) {
    if (p.alpha <= 0f) return
    val drawColor = p.color.copy(alpha = p.alpha)

    rotate(degrees = p.rotation, pivot = Offset(p.x, p.y)) {
        when (p.shape) {
            ConfettiShape.RECTANGLE -> {
                drawRect(
                    color = drawColor,
                    topLeft = Offset(p.x - p.sizeWidth / 2f, p.y - p.sizeHeight / 2f),
                    size = Size(p.sizeWidth, p.sizeHeight)
                )
            }
            ConfettiShape.CIRCLE -> {
                drawCircle(
                    color = drawColor,
                    center = Offset(p.x, p.y),
                    radius = p.sizeWidth / 2f
                )
            }
            ConfettiShape.OVAL -> {
                drawOval(
                    color = drawColor,
                    topLeft = Offset(p.x - p.sizeWidth / 2f, p.y - p.sizeHeight / 2f),
                    size = Size(p.sizeWidth, p.sizeHeight)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ConfettiCelebrationPreview() {
    PersonalSportTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            ConfettiCelebration(
                totalExercisesFinished = 6,
                totalEstimatedCaloriesBurned = 380,
                onDismiss = {}
            )
        }
    }
}
