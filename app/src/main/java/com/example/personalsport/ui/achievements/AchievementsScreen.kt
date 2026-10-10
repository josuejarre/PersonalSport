package com.example.personalsport.ui.achievements

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsRun
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.personalsport.PersonalSportApp
import com.example.personalsport.gamification.GamificationManager
import com.example.personalsport.gamification.GamificationState
import com.example.personalsport.gamification.Trophy
import com.example.personalsport.gamification.TrophyCategory

/**
 * Pantalla de Gamificación, Logros y Trofeos Deportivos ([AchievementsScreen]).
 *
 * Muestra el progreso de recompensas y trofeos desbloqueables del atleta:
 * - Nivel deportivo actual, puntos XP e insignias.
 * - Trofeos clasificados por categorías (Carrera, Natación, Fuerza, Consistencia, Nutrición).
 * - Animaciones de trofeos bloqueados vs desbloqueados.
 *
 * **Manejo de Estado y Despacho de Hilos**:
 * - Recolecta el estado GamificationState de GamificationManager mediante StateFlow en el hilo principal de UI.
 *
 * @param gamificationManager Gestor de gamificación y persistencia de trofeos.
 * @param onOpenDrawer Callback para desplegar el menú de navegación lateral.
 * @param onOpenProfile Callback para abrir el perfil de usuario.
 * @param modifier Modificador de diseño Compose.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AchievementsScreen(
    gamificationManager: GamificationManager = GamificationManager.getInstance(LocalContext.current),
    onOpenDrawer: () -> Unit = {},
    onOpenProfile: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val state by gamificationManager.uiState.collectAsState()
    var selectedCategory by remember { mutableStateOf(TrophyCategory.ALL) }

    val filteredTrophies = remember(state.trophies, selectedCategory) {
        if (selectedCategory == TrophyCategory.ALL) {
            state.trophies
        } else {
            state.trophies.filter { it.category == selectedCategory }
        }
    }

    val unlockedCount = remember(state.trophies) { state.trophies.count { it.isUnlocked } }
    val totalTrophies = state.trophies.size

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.EmojiEvents,
                            contentDescription = null,
                            tint = Color(0xFFFFD700),
                            modifier = Modifier.size(28.dp).padding(end = 6.dp)
                        )
                        Column {
                            Text(
                                "Logros & Trofeos",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Gamificación & Nivel Olímpico",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Rounded.Menu, contentDescription = "Menú")
                    }
                },
                actions = {
                    IconButton(onClick = onOpenProfile) {
                        Icon(Icons.Rounded.AccountCircle, contentDescription = "Perfil")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header: Card de Nivel & Experiencia XP
            LevelProgressCard(state = state)

            // Resumen de Estadísticas Rápidas
            StatsOverviewBar(
                unlockedCount = unlockedCount,
                totalTrophies = totalTrophies,
                dietStreak = state.dietStreakDays,
                workoutStreak = state.workoutStreakDays
            )

            // Selector de Categoría (Filtros)
            TrophyCategoryFilterRow(
                selectedCategory = selectedCategory,
                onCategorySelected = { selectedCategory = it }
            )

            // Lista/Cuadrícula de Trofeos
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 150.dp),
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredTrophies, key = { it.id }) { trophy ->
                    TrophyCardItem(trophy = trophy)
                }
            }
        }
    }
}

@Composable
private fun LevelProgressCard(state: GamificationState) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 4.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.tertiaryContainer
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = state.levelTitle,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "XP Total: ${state.totalXp} pts",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }

                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFFFD700),
                        modifier = Modifier.size(52.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "${state.currentLevel}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Progreso de Nivel",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "${state.xpForCurrentLevel} / ${state.xpForNextLevel} XP",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    LinearProgressIndicator(
                        progress = { state.levelProgressPercent },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(12.dp)
                            .clip(RoundedCornerShape(6.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

@Composable
private fun StatsOverviewBar(
    unlockedCount: Int,
    totalTrophies: Int,
    dietStreak: Int,
    workoutStreak: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        StatPillItem(
            icon = Icons.Rounded.EmojiEvents,
            iconTint = Color(0xFFFFD700),
            title = "Trofeos",
            value = "$unlockedCount / $totalTrophies",
            modifier = Modifier.weight(1f)
        )
        StatPillItem(
            icon = Icons.Rounded.Restaurant,
            iconTint = MaterialTheme.colorScheme.secondary,
            title = "Racha Dieta",
            value = "$dietStreak Días",
            modifier = Modifier.weight(1f)
        )
        StatPillItem(
            icon = Icons.AutoMirrored.Rounded.DirectionsRun,
            iconTint = MaterialTheme.colorScheme.primary,
            title = "Racha Entrenos",
            value = "$workoutStreak Días",
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatPillItem(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(22.dp)
            )
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun TrophyCategoryFilterRow(
    selectedCategory: TrophyCategory,
    onCategorySelected: (TrophyCategory) -> Unit
) {
    ScrollableTabRow(
        selectedTabIndex = TrophyCategory.entries.indexOf(selectedCategory),
        edgePadding = 0.dp,
        divider = {}
    ) {
        TrophyCategory.entries.forEach { category ->
            val label = when (category) {
                TrophyCategory.ALL -> "Todos"
                TrophyCategory.WORKOUT -> "Entrenamientos"
                TrophyCategory.RUNNING_SWIMMING -> "Carrera & Natación"
                TrophyCategory.DIET -> "Dieta 30 Días"
                TrophyCategory.SLEEP -> "Sueño AI"
                TrophyCategory.CALISTHENICS -> "Calistenia"
            }
            FilterChip(
                selected = selectedCategory == category,
                onClick = { onCategorySelected(category) },
                label = { Text(label, style = MaterialTheme.typography.labelMedium) },
                modifier = Modifier.padding(end = 6.dp)
            )
        }
    }
}

@Composable
private fun TrophyCardItem(trophy: Trophy) {
    val isUnlocked = trophy.isUnlocked

    val iconVector = when (trophy.iconName) {
        "DirectionsRun" -> Icons.AutoMirrored.Rounded.DirectionsRun
        "Pool" -> Icons.Rounded.Pool
        "FitnessCenter" -> Icons.Rounded.FitnessCenter
        "Restaurant" -> Icons.Rounded.Restaurant
        "NightsStay" -> Icons.Rounded.NightsStay
        else -> Icons.Rounded.EmojiEvents
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        tonalElevation = if (isUnlocked) 3.dp else 1.dp,
        color = if (isUnlocked)
            MaterialTheme.colorScheme.surface
        else
            MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f),
        border = BorderStroke(
            width = 1.dp,
            color = if (isUnlocked) Color(0xFFFFD700) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = if (isUnlocked) Color(0xFFFFD700).copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = iconVector,
                        contentDescription = null,
                        tint = if (isUnlocked) Color(0xFFFFD700) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            Text(
                text = trophy.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = trophy.description,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.weight(1f))

            if (isUnlocked) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFFFD700).copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "🏆 ${trophy.unlockedAt ?: "Completado"}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFB8860B),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = "🔒 Bloqueado",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}
