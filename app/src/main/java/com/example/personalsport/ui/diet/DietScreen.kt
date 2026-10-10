package com.example.personalsport.ui.diet

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsRun
import androidx.compose.material.icons.automirrored.rounded.DirectionsWalk
import androidx.compose.material.icons.automirrored.rounded.Launch
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.personalsport.ai.DishAlternative
import com.example.personalsport.ai.MealSlot
import com.example.personalsport.ai.MealSlotType
import com.example.personalsport.ai.ProjectedDayPlan
import com.example.personalsport.ai.SupplementTimingItem
import com.example.personalsport.ai.ThirtyDayDietPlan
import com.example.personalsport.diet.ActivityLevel
import com.example.personalsport.diet.DietGoal
import com.example.personalsport.diet.Gender
import com.example.personalsport.diet.GroceryCategory
import com.example.personalsport.diet.MacroTarget
import com.example.personalsport.ui.theme.*

/**
 * Pantalla de Nutrición Deportiva y Dieta Flexible Proyectada a 30 Días ([DietScreen]).
 *
 * Muestra el plan nutricional personalizado:
 * - Selección de metas nutricionales (Pérdida de Grasa, Ganancia Muscular, Recomposición, Rendimiento).
 * - Desglose de macronutrientes (proteínas, carbohidratos, grasas, fibra) y micronutrientes diarios.
 * - Ciclado de carbohidratos para días de entrenamiento vs descanso.
 * - Alternativas flexibles de platos por franja horaria (Desayuno, Media Mañana, Almuerzo, Merienda, Cena).
 * - Lista de la compra inteligente organizada por categorías.
 * - Sincronización de suplementos nutricionales.
 *
 * **Manejo de Estado y Despacho de Hilos**:
 * - Recolecta el estado [DietUiState] de [DietViewModel] con [collectAsStateWithLifecycle] en el hilo de UI.
 *
 * @param onNavigateToSupplements Callback para navegar al centro de suplementación.
 * @param onOpenDrawer Callback para desplegar el menú de navegación.
 * @param onOpenProfile Callback para abrir la ventana del perfil de usuario.
 * @param viewModel ViewModel del módulo nutricional.
 * @param modifier Modificador de diseño Compose.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DietScreen(
    onNavigateToSupplements: () -> Unit = {},
    onOpenDrawer: () -> Unit = {},
    onOpenProfile: () -> Unit = {},
    viewModel: DietViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Dietas & Nutrición",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Motor AI 1-Mes Dieta Flexible (IIFYM)",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(
                            imageVector = Icons.Rounded.Menu,
                            contentDescription = "Abrir Menú"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onOpenProfile) {
                        Icon(
                            imageVector = Icons.Rounded.AccountCircle,
                            contentDescription = "Perfil Atleta",
                            tint = MaterialTheme.colorScheme.primary
                        )
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
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { viewModel.setGroceryDialogVisible(true) },
                icon = {
                    Icon(
                        imageVector = Icons.Rounded.ShoppingCart,
                        contentDescription = null
                    )
                },
                text = { Text("Lista de la Compra", fontWeight = FontWeight.Bold) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp)
            )
        }
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
            contentPadding = PaddingValues(bottom = 88.dp, top = 8.dp)
        ) {
            // 1. Goal Selector Chips
            item {
                Text(
                    text = "OBJETIVO NUTRICIONAL & FLEXIBLE DIET",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(DietGoal.entries.toTypedArray()) { goal ->
                        val isSelected = uiState.goal == goal
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.selectGoal(goal) },
                            label = {
                                Text(
                                    text = goal.title,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            leadingIcon = {
                                val icon = when (goal) {
                                    DietGoal.FAT_LOSS -> Icons.Rounded.LocalFireDepartment
                                    DietGoal.MUSCLE_GAIN -> Icons.Rounded.FitnessCenter
                                    DietGoal.BODY_RECOMP -> Icons.Rounded.AutoAwesome
                                    DietGoal.ATHLETIC_PERFORMANCE -> Icons.AutoMirrored.Rounded.DirectionsRun
                                }
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }

                // Goal Explanation Card
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = uiState.goal.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // 2. 30-Day Projected Plan Timeline Selector
            item {
                uiState.thirtyDayPlan?.let { plan30 ->
                    ThirtyDayProjectionCard(
                        thirtyDayPlan = plan30,
                        selectedDayIndex = uiState.selectedDayIndex,
                        onSelectDay = { viewModel.selectDay(it) }
                    )
                }
            }

            // 3. Current Day Selected Plan Summary & Energy Expenditure
            item {
                uiState.currentDayPlan?.let { currentDay ->
                    CurrentDayPlanSummaryCard(
                        dayPlan = currentDay,
                        uiState = uiState,
                        onToggleCustomize = { viewModel.toggleCustomizingMetrics() }
                    )
                }
            }

            // 4. Customization Metrics Panel
            item {
                AnimatedVisibility(
                    visible = uiState.isCustomizingMetrics,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    MetricsCustomizerCard(
                        uiState = uiState,
                        onWeightChange = { viewModel.updateWeight(it) },
                        onHeightChange = { viewModel.updateHeight(it) },
                        onAgeChange = { viewModel.updateAge(it) },
                        onBodyFatChange = { viewModel.updateBodyFat(it) },
                        onActivityLevelChange = { viewModel.updateActivityLevel(it) },
                        onWorkoutCaloriesChange = { viewModel.updateWorkoutActiveCalories(it) },
                        onGenderChange = { viewModel.updateGender(it) }
                    )
                }
            }

            // 5. Macronutrient & Micronutrient Balances
            item {
                MacronutrientProgressCard(targetMacros = uiState.targetMacros)
            }

            // 6. Section Title: Daily Meal Slots with Alternative Choice Options
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "PLAN DE COMIDAS & 3-4 ALTERNATIVAS POR TOMA",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Platos intercambiables con idéntico objetivo de macros",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Badge(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                    ) {
                        Text(
                            text = "5 Tomas / Día",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            // 7. Interactive Meal Cards with Alternative Dialog Triggers
            uiState.currentDayPlan?.mealSlots?.let { slots ->
                items(slots, key = { it.slotType.name }) { slot ->
                    MealSlotCard(
                        mealSlot = slot,
                        onOpenAlternatives = { viewModel.openAlternativesDialog(slot.slotType) },
                        onOpenRecipeDetails = { viewModel.openRecipeDetails(it) }
                    )
                }
            }

            // 8. Supplement Timing & Cross-Tab Navigation Card
            item {
                uiState.thirtyDayPlan?.recommendedSupplements?.let { supplements ->
                    RecommendedSupplementsCard(
                        supplements = supplements,
                        onNavigateToSupplements = onNavigateToSupplements
                    )
                }
            }
        }
    }
}

    // Modal Dialog 1: Change Dish / Select Alternative Dialog
    uiState.selectedSlotForAlternatives?.let { slotType ->
        val currentSlot = uiState.currentDayPlan?.mealSlots?.find { it.slotType == slotType }
        if (currentSlot != null) {
            AlternativeDishesDialog(
                mealSlot = currentSlot,
                onDismiss = { viewModel.closeAlternativesDialog() },
                onSelectAlternative = { alternativeIndex ->
                    viewModel.selectAlternativeDish(slotType, alternativeIndex)
                    viewModel.closeAlternativesDialog()
                },
                onViewRecipe = { dish ->
                    viewModel.closeAlternativesDialog()
                    viewModel.openRecipeDetails(dish)
                }
            )
        }
    }

    // Modal Dialog 2: Recipe Step-by-Step Details & Micronutrients
    uiState.selectedDishForRecipeDetails?.let { dish ->
        RecipeDetailsDialog(
            dish = dish,
            onDismiss = { viewModel.closeRecipeDetails() }
        )
    }

    // Modal Dialog 3: Grocery List Dialog
    if (uiState.showGroceryDialog) {
        GroceryListDialog(
            categories = uiState.groceryList,
            onDismiss = { viewModel.setGroceryDialogVisible(false) },
            onCopy = {
                val fullText = buildString {
                    appendLine("🛒 LISTA DE LA COMPRA - PersonalSport")
                    appendLine("Objetivo: ${uiState.goal.title}\n")
                    uiState.groceryList.forEach { category ->
                        appendLine("📌 ${category.categoryName.uppercase()}:")
                        category.items.forEach { item ->
                            appendLine("  • $item")
                        }
                        appendLine()
                    }
                }

                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Lista de la Compra", fullText)
                clipboard.setPrimaryClip(clip)

                Toast.makeText(context, "Lista copiada al portapapeles", Toast.LENGTH_SHORT).show()
                viewModel.setGroceryDialogVisible(false)
            }
        )
    }
}

@Composable
fun ThirtyDayProjectionCard(
    thirtyDayPlan: ThirtyDayDietPlan,
    selectedDayIndex: Int,
    onSelectDay: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
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
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CalendarMonth,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Proyección Flexible 30 Días",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                val weightDiffStr = if (thirtyDayPlan.estimatedWeightChangeKg >= 0)
                    "+${thirtyDayPlan.estimatedWeightChangeKg} kg"
                else
                    "${thirtyDayPlan.estimatedWeightChangeKg} kg"

                Badge(containerColor = MaterialTheme.colorScheme.primaryContainer) {
                    Text(
                        text = "Meta 30d: $weightDiffStr",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Text(
                text = "Selecciona un día para visualizar los macronutrientes, ciclados de entreno y alternativas de platos:",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Horizontal Scrollable Day Selector (1 to 30)
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                itemsIndexed(thirtyDayPlan.days) { index, day ->
                    val isSelected = index == selectedDayIndex
                    Surface(
                        onClick = { onSelectDay(index) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                        border = if (!isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) else null,
                        modifier = Modifier.width(72.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = day.dateLabel,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (day.isWorkoutDay) "Entreno" else "Descanso",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${day.calorieTarget} kcal",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CurrentDayPlanSummaryCard(
    dayPlan: ProjectedDayPlan,
    uiState: DietUiState,
    onToggleCustomize: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
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
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "${dayPlan.dayNumber}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }

                    Column {
                        Text(
                            text = "Planificación para el ${dayPlan.dateLabel}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = if (dayPlan.isWorkoutDay) "⚡ Día con Sesión de Entrenamiento" else "💤 Día de Descanso & Recuperación",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }

                IconButton(onClick = onToggleCustomize) {
                    Icon(
                        imageVector = Icons.Rounded.Tune,
                        contentDescription = "Ajustar Métricas",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Objetivo Calórico",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                    Text(
                        text = "${dayPlan.calorieTarget} kcal",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Progreso Estimado de Peso",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                    Text(
                        text = "${dayPlan.projectedWeightKg} kg",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            // Micronutrients Quick Ribbon
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                MicronutrientChip("Fibra", "${dayPlan.fiberTargetGrams}g")
                MicronutrientChip("Hierro", "${dayPlan.dailyMicronutrients.ironMg.toInt()}mg")
                MicronutrientChip("Calcio", "${dayPlan.dailyMicronutrients.calciumMg.toInt()}mg")
                MicronutrientChip("Omega-3", "${dayPlan.dailyMicronutrients.omega3Grams}g")
            }
        }
    }
}

@Composable
fun MicronutrientChip(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun MetricsCustomizerCard(
    uiState: DietUiState,
    onWeightChange: (Float) -> Unit,
    onHeightChange: (Float) -> Unit,
    onAgeChange: (Int) -> Unit,
    onBodyFatChange: (Float?) -> Unit,
    onActivityLevelChange: (ActivityLevel) -> Unit,
    onWorkoutCaloriesChange: (Int) -> Unit,
    onGenderChange: (Gender) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Ajustar Métricas Corporales",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = uiState.profile.weightKg.toString(),
                    onValueChange = { text -> text.toFloatOrNull()?.let { onWeightChange(it) } },
                    label = { Text("Peso (kg)") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
                OutlinedTextField(
                    value = uiState.profile.heightCm.toString(),
                    onValueChange = { text -> text.toFloatOrNull()?.let { onHeightChange(it) } },
                    label = { Text("Altura (cm)") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = uiState.profile.bodyFatPercentage?.toString() ?: "",
                    onValueChange = { text ->
                        if (text.isEmpty()) onBodyFatChange(null)
                        else text.toFloatOrNull()?.let { onBodyFatChange(it) }
                    },
                    label = { Text("% Grasa Corporal") },
                    placeholder = { Text("Ej. 15.0") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
                OutlinedTextField(
                    value = uiState.profile.workoutActiveCalories.toString(),
                    onValueChange = { text -> text.toIntOrNull()?.let { onWorkoutCaloriesChange(it) } },
                    label = { Text("Kcal Entreno/Día") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }

            Text(
                text = "Nivel de Actividad Diaria",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                ActivityLevel.entries.forEach { level ->
                    val isSelected = uiState.profile.activityLevel == level
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surface
                            )
                            .clickable { onActivityLevelChange(level) }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = level.label,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = level.description,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        RadioButton(
                            selected = isSelected,
                            onClick = { onActivityLevelChange(level) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MacronutrientProgressCard(targetMacros: MacroTarget) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Distribución de Macronutrientes",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            MacroBarItem(
                name = "Proteínas",
                grams = targetMacros.proteinGrams,
                percentage = targetMacros.proteinPercentage,
                calories = targetMacros.proteinCalories,
                color = Color(0xFFE53935),
                icon = Icons.Rounded.Egg
            )

            MacroBarItem(
                name = "Carbohidratos",
                grams = targetMacros.carbsGrams,
                percentage = targetMacros.carbsPercentage,
                calories = targetMacros.carbsCalories,
                color = Color(0xFF1E88E5),
                icon = Icons.Rounded.Grain
            )

            MacroBarItem(
                name = "Grasas Saludables",
                grams = targetMacros.fatGrams,
                percentage = targetMacros.fatPercentage,
                calories = targetMacros.fatCalories,
                color = Color(0xFF4CAF50),
                icon = Icons.Rounded.Opacity
            )
        }
    }
}

@Composable
fun MacroBarItem(
    name: String,
    grams: Int,
    percentage: Int,
    calories: Int,
    color: Color,
    icon: ImageVector
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Text(
                text = "$grams g • $calories kcal ($percentage%)",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }

        LinearProgressIndicator(
            progress = { (percentage.coerceIn(0, 100) / 100f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp)),
            color = color,
            trackColor = color.copy(alpha = 0.2f)
        )
    }
}

@Composable
fun MealSlotCard(
    mealSlot: MealSlot,
    onOpenAlternatives: () -> Unit,
    onOpenRecipeDetails: (DishAlternative) -> Unit
) {
    val activeDish = mealSlot.activeDish

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Slot Badge + Timing
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = mealSlot.slotType.title,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    Text(
                        text = mealSlot.slotType.defaultTime,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = "${activeDish.calories} kcal",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Dish Title & Satiety Score
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = activeDish.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = activeDish.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Star,
                            contentDescription = null,
                            tint = Color(0xFFFFB300),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Saciación ${activeDish.satietyIndexScore}/10",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            // Macros & Fiber Pills
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MacroPill(label = "P", grams = activeDish.proteinGrams, color = Color(0xFFE53935))
                MacroPill(label = "C", grams = activeDish.carbsGrams, color = Color(0xFF1E88E5))
                MacroPill(label = "G", grams = activeDish.fatGrams, color = Color(0xFF4CAF50))
                MacroPill(label = "Fibra", grams = activeDish.fiberGrams, color = Color(0xFF8E24AA))
            }

            // Action Buttons: Swap Dish (3-4 Alternatives) & View Recipe
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onOpenAlternatives,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SwapHoriz,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Cambiar Plato (${mealSlot.alternatives.size} Alternativas)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                FilledTonalButton(
                    onClick = { onOpenRecipeDetails(activeDish) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MenuBook,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Ver Receta (${activeDish.prepTimeMinutes}m)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun RecommendedSupplementsCard(
    supplements: List<SupplementTimingItem>,
    onNavigateToSupplements: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.LocalPharmacy,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Text(
                        text = "Suplementación Recomendada & Horarios de Toma",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }

            Text(
                text = "Horario estratégico de tomas integrado con tu dieta y sesiones de entrenamiento:",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.85f)
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                supplements.forEach { supp ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.75f))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Schedule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = supp.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ) {
                                    Text(
                                        text = supp.dosage,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        softWrap = false,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = supp.timingInstruction,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = supp.rationale,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Button(
                onClick = onNavigateToSupplements,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Ver y Comprar en Suplementación",
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.Launch,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun AlternativeDishesDialog(
    mealSlot: MealSlot,
    onDismiss: () -> Unit,
    onSelectAlternative: (Int) -> Unit,
    onViewRecipe: (DishAlternative) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Rounded.SwapHoriz,
                contentDescription = null,
                modifier = Modifier.size(36.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Column {
                Text(
                    text = "Alternativas para ${mealSlot.slotType.title}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Todos los platos comparten los mismos macronutrientes para mantener tu IIFYM perfecto.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                itemsIndexed(mealSlot.alternatives) { index, dish ->
                    val isCurrent = index == mealSlot.activeAlternativeIndex

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .border(
                                width = if (isCurrent) 2.dp else 1.dp,
                                color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                shape = RoundedCornerShape(14.dp)
                            )
                            .clickable { onSelectAlternative(index) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isCurrent) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        )
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
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "Opción ${index + 1}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    if (isCurrent) {
                                        Badge(containerColor = MaterialTheme.colorScheme.primary) {
                                            Text("Activo", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimary)
                                        }
                                    }
                                }

                                Text(
                                    text = "${dish.calories} kcal",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Text(
                                text = dish.name,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                text = dish.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                MacroPill(label = "P", grams = dish.proteinGrams, color = Color(0xFFE53935))
                                MacroPill(label = "C", grams = dish.carbsGrams, color = Color(0xFF1E88E5))
                                MacroPill(label = "G", grams = dish.fatGrams, color = Color(0xFF4CAF50))
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(
                                    onClick = { onViewRecipe(dish) },
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Icon(Icons.Rounded.MenuBook, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Ver Receta")
                                }

                                Button(
                                    onClick = { onSelectAlternative(index) },
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                ) {
                                    Text(if (isCurrent) "Seleccionado" else "Activar Plato")
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar")
            }
        }
    )
}

@Composable
fun RecipeDetailsDialog(
    dish: DishAlternative,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Rounded.RestaurantMenu,
                contentDescription = null,
                modifier = Modifier.size(36.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Column {
                Text(
                    text = dish.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Tiempo de Prep: ${dish.prepTimeMinutes} min • Saciación: ${dish.satietyIndexScore}/10",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 440.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Micronutrient Highlights
                item {
                    Text(
                        text = "MICRONUTRIENTES DESTACADOS",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        dish.micronutrientHighlights.forEach { highlight ->
                            SuggestionChip(
                                onClick = {},
                                label = { Text(highlight, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                }

                // Ingredient List with Quantities in Grams/Ml
                item {
                    Text(
                        text = "INGREDIENTES EXACTOS (EN GRAMOS / ML)",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Column(
                        modifier = Modifier.padding(top = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        dish.scaledIngredients.forEach { (ing, formattedQty) ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "• ${ing.name}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = formattedQty,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                // Step by Step Instructions
                item {
                    Text(
                        text = "PASO A PASO DE PREPARACIÓN",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Column(
                        modifier = Modifier.padding(top = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        dish.recipeInstructions.forEachIndexed { index, step ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.size(22.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "${index + 1}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                                Text(
                                    text = step,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Entendido / Listo")
            }
        }
    )
}

@Composable
fun MacroPill(
    label: String,
    grams: Int,
    color: Color
) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = color.copy(alpha = 0.15f)
    ) {
        Text(
            text = "$label: ${grams}g",
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
fun GroceryListDialog(
    categories: List<GroceryCategory>,
    onDismiss: () -> Unit,
    onCopy: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Rounded.ShoppingCart,
                contentDescription = null,
                modifier = Modifier.size(36.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = "Lista de la Compra",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(categories) { category ->
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = category.categoryName.uppercase(),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        category.items.forEach { item ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.CheckCircleOutline,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                                Text(
                                    text = item,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onCopy) {
                Icon(
                    imageVector = Icons.Rounded.ContentCopy,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Copiar Lista")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar")
            }
        }
    )
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun DietScreenPreview() {
    PersonalSportTheme {
        DietScreen()
    }
}
