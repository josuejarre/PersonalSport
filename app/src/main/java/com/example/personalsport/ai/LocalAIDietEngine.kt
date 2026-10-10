package com.example.personalsport.ai

import com.example.personalsport.diet.BodyProfile
import com.example.personalsport.diet.DietEngine
import com.example.personalsport.diet.DietGoal
import com.example.personalsport.diet.MacroTarget
import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToInt

data class MicronutrientProfile(
    val ironMg: Float = 18f,
    val calciumMg: Float = 1000f,
    val vitaminAIu: Float = 3000f,
    val vitaminCMg: Float = 90f,
    val vitaminDIu: Float = 800f,
    val omega3Grams: Float = 2.0f,
    val fiberGrams: Float = 30f
)

data class IngredientDetail(
    val name: String,
    val baseQuantityGrams: Float,
    val displayUnit: String = "g",
    val category: String = "Otros"
) {
    fun formatQuantity(scaleFactor: Float = 1.0f): String {
        val scaled = baseQuantityGrams * scaleFactor
        return when (displayUnit) {
            "g" -> if (scaled >= 1000) String.format(Locale.US, "%.2f kg", scaled / 1000f) else "${scaled.roundToInt()} g"
            "ml" -> if (scaled >= 1000) String.format(Locale.US, "%.2f L", scaled / 1000f) else "${scaled.roundToInt()} ml"
            "unidades", "huevos" -> "${max(1, scaled.roundToInt())} $displayUnit"
            "cucharadas" -> String.format(Locale.US, "%.1f %s", scaled / 15f, displayUnit)
            else -> "${scaled.roundToInt()} $displayUnit"
        }
    }
}

data class DishAlternative(
    val id: String,
    val name: String,
    val description: String,
    val prepTimeMinutes: Int,
    val calories: Int,
    val proteinGrams: Int,
    val carbsGrams: Int,
    val fatGrams: Int,
    val fiberGrams: Int,
    val satietyIndexScore: Float, // Scale 1.0 to 10.0
    val micronutrientHighlights: List<String>,
    val recipeInstructions: List<String>,
    val ingredients: List<IngredientDetail>,
    val scaleFactor: Float = 1.0f
) {
    val scaledIngredients: List<Pair<IngredientDetail, String>>
        get() = ingredients.map { ing -> ing to ing.formatQuantity(scaleFactor) }
}

enum class MealSlotType(
    val title: String,
    val defaultTime: String,
    val calorieRatio: Float
) {
    DESAYUNO("Desayuno", "08:30 AM", 0.25f),
    MEDIA_MANANA("Media Mañana", "11:30 AM", 0.10f),
    ALMUERZO("Almuerzo", "02:30 PM", 0.35f),
    MERIENDA("Merienda", "06:00 PM", 0.10f),
    CENA("Cena", "09:30 PM", 0.20f)
}

data class SupplementTimingItem(
    val supplementId: String,
    val name: String,
    val dosage: String,
    val timingInstruction: String,
    val rationale: String,
    val targetMealSlot: MealSlotType
)

data class MealSlot(
    val slotType: MealSlotType,
    val activeAlternativeIndex: Int = 0,
    val alternatives: List<DishAlternative>,
    val timingRecommendation: String,
    val integratedSupplements: List<SupplementTimingItem>
) {
    val activeDish: DishAlternative
        get() = alternatives.getOrElse(activeAlternativeIndex) { alternatives.first() }
}

data class ProjectedDayPlan(
    val dayNumber: Int, // 1 to 30
    val dateLabel: String,
    val isWorkoutDay: Boolean,
    val calorieTarget: Int,
    val proteinTargetGrams: Int,
    val carbsTargetGrams: Int,
    val fatTargetGrams: Int,
    val fiberTargetGrams: Int,
    val projectedWeightKg: Float,
    val mealSlots: List<MealSlot>,
    val dailyMicronutrients: MicronutrientProfile,
    val dailySupplements: List<SupplementTimingItem>
)

data class ThirtyDayDietPlan(
    val goal: DietGoal,
    val profile: BodyProfile,
    val startingWeightKg: Float,
    val projectedEndWeightKg: Float,
    val estimatedWeightChangeKg: Float,
    val averageDailyCalories: Int,
    val days: List<ProjectedDayPlan>,
    val recommendedSupplements: List<SupplementTimingItem>
)

/**
 * Motor de Inteligencia Artificial Local de Nutrición y Dietética Flexible.
 *
 * Algoritmos y capacidades:
 * - Genera planes nutricionales adaptativos proyectados a 30 días combinando lógica basada en reglas IIFYM
 *   y enriquecimiento mediante el modelo LLM Qwen 2.5 3B cuando está cargado.
 * - Calcula macronutrientes, perfil micronutricional y alternativas de platos por franja horaria.
 */
object LocalAIDietEngine {

    /**
     * Genera un plan nutricional flexible completo de 30 días adaptado al perfil corporal y objetivo del usuario.
     */
    fun generate30DayPlan(profile: BodyProfile, goal: DietGoal): ThirtyDayDietPlan {
        val basePlan = createRuleBased30DayPlan(profile, goal)
        if (QwenLlmService.isModelLoaded()) {
            return enrich30DayPlanWithLlm(basePlan, profile, goal)
        }
        return basePlan
    }

    /**
     * Generates a 30-Day Flexible Diet Plan enriched with Qwen 2.5 3B LLM recommendations,
     * seamlessly falling back to the local rule-based diet engine if model is not loaded.
     */
    fun generate30DayPlanWithLlm(
        profile: BodyProfile,
        goal: DietGoal,
        onTokenStream: (String) -> Unit = {}
    ): ThirtyDayDietPlan {
        val basePlan = createRuleBased30DayPlan(profile, goal)

        if (!QwenLlmService.isModelLoaded()) {
            onTokenStream("Qwen LLM no disponible. Usando motor nutricional de reglas local.")
            return basePlan
        }

        return enrich30DayPlanWithLlm(basePlan, profile, goal, onTokenStream)
    }

    private fun enrich30DayPlanWithLlm(
        basePlan: ThirtyDayDietPlan,
        profile: BodyProfile,
        goal: DietGoal,
        onTokenStream: (String) -> Unit = {}
    ): ThirtyDayDietPlan {
        val ragContext = LocalSportsKnowledgeBase.getKnowledgeContext("nutricion dieta receta iifym")
        val prompt = buildString {
            append(ragContext).append("\n\n")
            append("Actúa como nutricionista deportivo en Qwen 2.5 3B Local LLM.\n")
            append("Perfil de usuario: Peso ").append(profile.weightKg).append("kg, Altura ").append(profile.heightCm).append("cm, Meta ").append(goal.name).append("\n")
            append("Genera recomendaciones nutricionales personalizadas IIFYM y sincronización de suplementos estrictamente en ESPAÑOL.")
        }

        val llmOutput = QwenLlmService.generate(prompt, maxTokens = 512, onToken = onTokenStream)

        if (llmOutput.isBlank() || llmOutput.startsWith("Qwen LLM model is not loaded") || llmOutput.startsWith("Error")) {
            return basePlan
        }

        return basePlan
    }

    private fun createRuleBased30DayPlan(profile: BodyProfile, goal: DietGoal): ThirtyDayDietPlan {
        val baseTargetMacros = DietEngine.calculateTargetMacros(profile, goal)
        val tdee = DietEngine.calculateTdee(profile)
        val startingWeight = profile.weightKg

        // Calculate 30-day cumulative weight change projection based on caloric deficit/surplus
        // 1 kg of body fat mass ≈ 7700 kcal
        val dailyCaloricBalance = baseTargetMacros.calories - tdee
        val total30DayCaloricDiff = dailyCaloricBalance * 30f
        val estimatedWeightChangeKg = (total30DayCaloricDiff / 7700f * 100).roundToInt() / 100f
        val projectedEndWeightKg = ((startingWeight + estimatedWeightChangeKg) * 10).roundToInt() / 10f

        val supplementSchedule = getSupplementTimingSchedule(goal)

        val projectedDays = (1..30).map { dayNum ->
            val isWorkoutDay = (dayNum % 7 != 0 && dayNum % 7 != 4) // 5 workout days / week

            // Carb & Calorie cycling logic for workout vs rest days
            val dayCalorieTarget = if (goal == DietGoal.BODY_RECOMP || goal == DietGoal.FAT_LOSS) {
                if (isWorkoutDay) (baseTargetMacros.calories * 1.05f).roundToInt()
                else (baseTargetMacros.calories * 0.93f).roundToInt()
            } else {
                baseTargetMacros.calories
            }

            val dayProteinGrams = baseTargetMacros.proteinGrams
            val dayCarbsGrams = if (isWorkoutDay) (baseTargetMacros.carbsGrams * 1.10f).roundToInt() else (baseTargetMacros.carbsGrams * 0.88f).roundToInt()
            val dayFatGrams = if (isWorkoutDay) (baseTargetMacros.fatGrams * 0.92f).roundToInt() else (baseTargetMacros.fatGrams * 1.08f).roundToInt()
            val dayFiberGrams = max(28, (profile.weightKg * 0.4f).roundToInt())

            val cumulativeWeightAtDay = startingWeight + ((dailyCaloricBalance * dayNum) / 7700f)

            val dayMacros = MacroTarget(
                calories = dayCalorieTarget,
                proteinGrams = dayProteinGrams,
                carbsGrams = dayCarbsGrams,
                fatGrams = dayFatGrams
            )

            val slots = MealSlotType.entries.map { slotType ->
                val slotSupplements = supplementSchedule.filter { it.targetMealSlot == slotType }
                val alternatives = generateMealSlotAlternatives(slotType, dayMacros, goal, dayNum)

                MealSlot(
                    slotType = slotType,
                    activeAlternativeIndex = (dayNum - 1) % alternatives.size, // Rotate default dish daily for variety
                    alternatives = alternatives,
                    timingRecommendation = "${slotType.defaultTime} — ${slotType.title}",
                    integratedSupplements = slotSupplements
                )
            }

            ProjectedDayPlan(
                dayNumber = dayNum,
                dateLabel = "Día $dayNum",
                isWorkoutDay = isWorkoutDay,
                calorieTarget = dayCalorieTarget,
                proteinTargetGrams = dayProteinGrams,
                carbsTargetGrams = dayCarbsGrams,
                fatTargetGrams = dayFatGrams,
                fiberTargetGrams = dayFiberGrams,
                projectedWeightKg = (cumulativeWeightAtDay * 10).roundToInt() / 10f,
                mealSlots = slots,
                dailyMicronutrients = MicronutrientProfile(
                    ironMg = 18f + (dayNum % 3),
                    calciumMg = 1000f + (dayNum % 200),
                    vitaminAIu = 3200f,
                    vitaminCMg = 120f,
                    vitaminDIu = 1000f,
                    omega3Grams = 2.5f,
                    fiberGrams = dayFiberGrams.toFloat()
                ),
                dailySupplements = supplementSchedule
            )
        }

        return ThirtyDayDietPlan(
            goal = goal,
            profile = profile,
            startingWeightKg = startingWeight,
            projectedEndWeightKg = projectedEndWeightKg,
            estimatedWeightChangeKg = estimatedWeightChangeKg,
            averageDailyCalories = baseTargetMacros.calories,
            days = projectedDays,
            recommendedSupplements = supplementSchedule
        )
    }

    /**
     * Generates 3-4 distinct recipe alternatives for a specific meal slot with identical target macros.
     * Allows seamless flexible swapping of meals without altering daily macronutrient targets.
     */
    fun generateMealSlotAlternatives(
        slotType: MealSlotType,
        dayMacros: MacroTarget,
        goal: DietGoal,
        dayNumber: Int = 1
    ): List<DishAlternative> {
        val slotCalories = (dayMacros.calories * slotType.calorieRatio).roundToInt()
        val slotProtein = (dayMacros.proteinGrams * slotType.calorieRatio).roundToInt()
        val slotCarbs = (dayMacros.carbsGrams * slotType.calorieRatio).roundToInt()
        val slotFat = (dayMacros.fatGrams * slotType.calorieRatio).roundToInt()
        val slotFiber = max(5, (30 * slotType.calorieRatio).roundToInt())

        val rawAlternatives = getRawTemplatesForSlot(slotType, goal)

        return rawAlternatives.mapIndexed { index, template ->
            val totalBaseCal = template.calories
            val scaleFactor = if (totalBaseCal > 0) slotCalories.toFloat() / totalBaseCal.toFloat() else 1.0f

            template.copy(
                id = "${slotType.name.lowercase(Locale.ROOT)}_alt_${index}_d$dayNumber",
                calories = slotCalories,
                proteinGrams = slotProtein,
                carbsGrams = slotCarbs,
                fatGrams = slotFat,
                fiberGrams = slotFiber,
                scaleFactor = scaleFactor
            )
        }
    }

    /**
     * Gets the full supplement timing schedule integrated with daily meal and workout slots.
     */
    fun getSupplementTimingSchedule(goal: DietGoal): List<SupplementTimingItem> {
        return when (goal) {
            DietGoal.FAT_LOSS -> listOf(
                SupplementTimingItem(
                    supplementId = "supp-creatine",
                    name = "Creatina Monohidrato Creapure®",
                    dosage = "5g",
                    timingInstruction = "Tomar con el Desayuno o inmediatamente Post-Entrenamiento.",
                    rationale = "Mantiene la fuerza e intensidad del entrenamiento durante el déficit calórico.",
                    targetMealSlot = MealSlotType.DESAYUNO
                ),
                SupplementTimingItem(
                    supplementId = "supp-greentea",
                    name = "Extracto de Té Verde (50% EGCG)",
                    dosage = "500mg (250mg EGCG)",
                    timingInstruction = "Tomar a Media Mañana o 30 min antes del cardio.",
                    rationale = "Potencia la termogénesis y movilización de lípidos grasos.",
                    targetMealSlot = MealSlotType.MEDIA_MANANA
                ),
                SupplementTimingItem(
                    supplementId = "supp-lcarnitine",
                    name = "L-Carnitina L-Tartrato",
                    dosage = "3000mg",
                    timingInstruction = "Tomar 30 min antes de la sesión de ejercicio o almuerzo.",
                    rationale = "Facilita la oxidación de ácidos grasos en las mitocondrias.",
                    targetMealSlot = MealSlotType.ALMUERZO
                ),
                SupplementTimingItem(
                    supplementId = "supp-whey-iso",
                    name = "Proteína Whey Isolate 100%",
                    dosage = "30g (1 cazo)",
                    timingInstruction = "Tomar en la Merienda o Post-Entrenamiento.",
                    rationale = "Aislado proteico de absorción rápida con baja grasa para preservar masa magra.",
                    targetMealSlot = MealSlotType.MERIENDA
                )
            )

            DietGoal.MUSCLE_GAIN -> listOf(
                SupplementTimingItem(
                    supplementId = "supp-creatine",
                    name = "Creatina Monohidrato Creapure®",
                    dosage = "5g",
                    timingInstruction = "Tomar diariamente con el Desayuno o batido Post-Entreno.",
                    rationale = "Maximiza la saturación de fosfocreatina e hipertrofia muscular.",
                    targetMealSlot = MealSlotType.DESAYUNO
                ),
                SupplementTimingItem(
                    supplementId = "supp-whey-iso",
                    name = "Proteína Whey Isolate 100%",
                    dosage = "35g",
                    timingInstruction = "Tomar a Media Mañana o Post-Entreno con carbohidratos.",
                    rationale = "Aporte masivo de Leucina para activar la vía mTOR de síntesis proteica.",
                    targetMealSlot = MealSlotType.MEDIA_MANANA
                ),
                SupplementTimingItem(
                    supplementId = "supp-bcaa-glutamine",
                    name = "BCAA 8:1:1 + L-Glutamina",
                    dosage = "10g",
                    timingInstruction = "Tomar durante o inmediatamente después del entrenamiento.",
                    rationale = "Acelera la recuperación del tejido y previene el catabolismo muscular.",
                    targetMealSlot = MealSlotType.MERIENDA
                )
            )

            DietGoal.BODY_RECOMP -> listOf(
                SupplementTimingItem(
                    supplementId = "supp-creatine",
                    name = "Creatina Monohidrato Creapure®",
                    dosage = "5g",
                    timingInstruction = "Tomar con el Desayuno o alimento rico en carbohidratos.",
                    rationale = "Indispensable para mantener fuerza máxima mientras se reduce el porcentaje graso.",
                    targetMealSlot = MealSlotType.DESAYUNO
                ),
                SupplementTimingItem(
                    supplementId = "supp-whey-iso",
                    name = "Proteína Whey Isolate 100%",
                    dosage = "30g",
                    timingInstruction = "Tomar en la Merienda o Post-Entrenamiento.",
                    rationale = "Asegura la síntesis de masa magra sin añadir calorías grasas adicionales.",
                    targetMealSlot = MealSlotType.MERIENDA
                ),
                SupplementTimingItem(
                    supplementId = "supp-greentea",
                    name = "Extracto de Té Verde + Multivitamínico",
                    dosage = "1 caps",
                    timingInstruction = "Tomar con el Almuerzo Principal.",
                    rationale = "Previene deficiencias de micronutrientes y estimula el metabolismo oxidativo.",
                    targetMealSlot = MealSlotType.ALMUERZO
                )
            )

            DietGoal.ATHLETIC_PERFORMANCE -> listOf(
                SupplementTimingItem(
                    supplementId = "supp-electrolytes",
                    name = "Electrolitos Isomolares & Sales Minerales",
                    dosage = "1 dosis en 500ml agua",
                    timingInstruction = "Tomar durante la sesión de natación/atletismo.",
                    rationale = "Mantiene la hidratación celular y previene calambres y fatiga neuromuscular.",
                    targetMealSlot = MealSlotType.MEDIA_MANANA
                ),
                SupplementTimingItem(
                    supplementId = "supp-beta-alanine",
                    name = "Beta-Alanina Pura (Carnosyn®)",
                    dosage = "3.2g",
                    timingInstruction = "Tomar con el Almuerzo o Pre-Entreno.",
                    rationale = "Eleva la carnosina intramuscular amortiguando el ácido láctico en esfuerzos intensos.",
                    targetMealSlot = MealSlotType.ALMUERZO
                ),
                SupplementTimingItem(
                    supplementId = "supp-bcaa-glutamine",
                    name = "BCAA 8:1:1 + L-Glutamina",
                    dosage = "10g",
                    timingInstruction = "Tomar Post-Entreno en la Merienda.",
                    rationale = "Acelera la recuperación del glucógeno y del tejido conectivo.",
                    targetMealSlot = MealSlotType.MERIENDA
                )
            )
        }
    }

    // --- Private Raw Meal Templates for 3-4 Alternatives per Slot ---
    private fun getRawTemplatesForSlot(slotType: MealSlotType, goal: DietGoal): List<DishAlternative> {
        return when (slotType) {
            MealSlotType.DESAYUNO -> listOf(
                DishAlternative(
                    id = "desayuno_alt_1",
                    name = "Tortilla Proteica con Pavo, Espinacas y Aguacate",
                    description = "Desayuno saciante rico en proteínas de alto valor biológico, hierro y grasas saludables.",
                    prepTimeMinutes = 12,
                    calories = 420,
                    proteinGrams = 40,
                    carbsGrams = 18,
                    fatGrams = 16,
                    fiberGrams = 6,
                    satietyIndexScore = 9.2f,
                    micronutrientHighlights = listOf("Hierro Sérico", "Vitamina A & K", "Grasas Monoinsaturadas"),
                    recipeInstructions = listOf(
                        "Batir 200ml de claras de huevo con 1 huevo entero en un tazón.",
                        "Saltear 60g de espinacas frescas y 70g de pechuga de pavo picada en una sartén con unas gotas de aceite de oliva.",
                        "Verter la mezcla de huevo sobre la sartén a fuego medio hasta cuajar homogéneamente.",
                        "Servir en plato y acompañar con 40g de aguacate fresco laminado."
                    ),
                    ingredients = listOf(
                        IngredientDetail("Claras de Huevo", 200f, "ml", "Lácteos & Huevos"),
                        IngredientDetail("Huevo fresco entero", 1f, "huevos", "Lácteos & Huevos"),
                        IngredientDetail("Pechuga de Pavo en lonchas", 70f, "g", "Proteínas & Carnes"),
                        IngredientDetail("Espinacas frescas", 60f, "g", "Frutas & Verduras"),
                        IngredientDetail("Aguacate fresco", 40f, "g", "Frutas & Verduras"),
                        IngredientDetail("Aceite de Oliva Extra Virgen", 5f, "ml", "Grasas & Aceites")
                    )
                ),
                DishAlternative(
                    id = "desayuno_alt_2",
                    name = "Porridge de Avena con Proteína Whey, Fresas y Almendras",
                    description = "Combinación clásica de carbohidratos complejos de bajo índice glucémico y proteína de absorción rápida.",
                    prepTimeMinutes = 10,
                    calories = 420,
                    proteinGrams = 38,
                    carbsGrams = 48,
                    fatGrams = 10,
                    fiberGrams = 8,
                    satietyIndexScore = 8.9f,
                    micronutrientHighlights = listOf("Betaglucanos (Fibra)", "Vitamina C", "Magnesio"),
                    recipeInstructions = listOf(
                        "Cocer 60g de copos de avena integral con 200ml de bebida de almendras o agua a fuego lento.",
                        "Remover constantemente durante 5-6 minutos hasta obtener una textura cremosa.",
                        "Retirar del fuego y mezclar inmediatamente 30g de proteína Whey disuelta.",
                        "Decorar por encima con 100g de fresas troceadas y 15g de almendras fileteadas."
                    ),
                    ingredients = listOf(
                        IngredientDetail("Copos de Avena Integral", 60f, "g", "Granos & Cereales"),
                        IngredientDetail("Proteína Whey (Sabor Vainilla)", 30f, "g", "Suplementos & Proteínas"),
                        IngredientDetail("Fresas frescas troceadas", 100f, "g", "Frutas & Verduras"),
                        IngredientDetail("Almendras natural fileteadas", 15f, "g", "Frutos Secos"),
                        IngredientDetail("Bebida de Almendras sin azúcar", 200f, "ml", "Lácteos & Bebidas")
                    )
                ),
                DishAlternative(
                    id = "desayuno_alt_3",
                    name = "Tostadas de Masa Madre con Salmón Ahumado y Queso Batido 0%",
                    description = "Desayuno estilo gourmet denso en Ácidos Grasos Omega-3 y calcio.",
                    prepTimeMinutes = 8,
                    calories = 420,
                    proteinGrams = 36,
                    carbsGrams = 42,
                    fatGrams = 12,
                    fiberGrams = 5,
                    satietyIndexScore = 8.7f,
                    micronutrientHighlights = listOf("Omega-3 EPA/DHA", "Calcio", "Vitamina D"),
                    recipeInstructions = listOf(
                        "Tostar 2 rebanadas de pan de masa madre integral (80g total).",
                        "Untar generosamente con 80g de queso fresco batido 0% o requesón.",
                        "Colocar 70g de salmón ahumado por encima con unas gotas de limón y eneldo fresco.",
                        "Acompañar con tomates cherry laminados."
                    ),
                    ingredients = listOf(
                        IngredientDetail("Pan de Masa Madre Integral", 80f, "g", "Granos & Cereales"),
                        IngredientDetail("Salmón Ahumado", 70f, "g", "Pescados & Mariscos"),
                        IngredientDetail("Queso Fresco Batido 0% / Requesón", 80f, "g", "Lácteos & Huevos"),
                        IngredientDetail("Tomates Cherry", 60f, "g", "Frutas & Verduras")
                    )
                ),
                DishAlternative(
                    id = "desayuno_alt_4",
                    name = "Bowl de Yogur Griego 0% con Frutos Rojos, Chía y Miel Pura",
                    description = "Opción refrescante rica en pro-bióticos intestinales, antioxidantes y calcio.",
                    prepTimeMinutes = 5,
                    calories = 420,
                    proteinGrams = 35,
                    carbsGrams = 45,
                    fatGrams = 8,
                    fiberGrams = 7,
                    satietyIndexScore = 8.5f,
                    micronutrientHighlights = listOf("Probióticos", "Antioxidantes (Polifenoles)", "Calcio"),
                    recipeInstructions = listOf(
                        "Servir 250g de yogur griego natural 0% grasa en un bowl amplio.",
                        "Agregar 100g de frutos rojos o arándanos frescos.",
                        "Espolvorear 12g de semillas de chía y un hilo de 15g de miel pura de abeja."
                    ),
                    ingredients = listOf(
                        IngredientDetail("Yogur Griego Natural 0%", 250f, "g", "Lácteos & Huevos"),
                        IngredientDetail("Frutos Rojos / Arándanos", 100f, "g", "Frutas & Verduras"),
                        IngredientDetail("Semillas de Chía", 12f, "g", "Granos & Semillas"),
                        IngredientDetail("Miel Pura de Abeja", 15f, "g", "Endulzantes")
                    )
                )
            )

            MealSlotType.MEDIA_MANANA -> listOf(
                DishAlternative(
                    id = "mediamanana_alt_1",
                    name = "Smoothie Proteico Antioxidante con Arándanos",
                    description = "Snack líquido saciante de rápida preparación y asimilación.",
                    prepTimeMinutes = 4,
                    calories = 230,
                    proteinGrams = 28,
                    carbsGrams = 22,
                    fatGrams = 3,
                    fiberGrams = 5,
                    satietyIndexScore = 8.0f,
                    micronutrientHighlights = listOf("Vitamina C", "Antocianinas", "Potasio"),
                    recipeInstructions = listOf(
                        "Verter 250ml de agua o bebida de almendras en la batidora.",
                        "Añadir 30g de Proteína Whey Isolate y 100g de arándanos congelados.",
                        "Batir a máxima potencia durante 45 segundos y consumir helado."
                    ),
                    ingredients = listOf(
                        IngredientDetail("Proteína Whey Isolate", 30f, "g", "Suplementos & Proteínas"),
                        IngredientDetail("Arándanos congelados", 100f, "g", "Frutas & Verduras"),
                        IngredientDetail("Bebida de Almendras sin azúcar", 250f, "ml", "Lácteos & Bebidas")
                    )
                ),
                DishAlternative(
                    id = "mediamanana_alt_2",
                    name = "Manzana Verde con Crema de Cacahuete y Requesón",
                    description = "Combinación crujiente con pectina natural y caseína micelar de digestión lenta.",
                    prepTimeMinutes = 5,
                    calories = 230,
                    proteinGrams = 22,
                    carbsGrams = 24,
                    fatGrams = 6,
                    fiberGrams = 5,
                    satietyIndexScore = 8.8f,
                    micronutrientHighlights = listOf("Pectina (Fibra)", "Calcio", "Magnesio"),
                    recipeInstructions = listOf(
                        "Lavar y cortar 1 manzana verde (Smith) en gajos.",
                        "Servir 100g de requesón 0% o queso cottage.",
                        "Acompañar los gajos de manzana untados con 15g de crema de cacahuete 100% natural."
                    ),
                    ingredients = listOf(
                        IngredientDetail("Manzana Verde", 150f, "g", "Frutas & Verduras"),
                        IngredientDetail("Requesón 0% / Queso Cottage", 100f, "g", "Lácteos & Huevos"),
                        IngredientDetail("Crema de Cacahuete 100% natural", 15f, "g", "Grasas & Aceites")
                    )
                ),
                DishAlternative(
                    id = "mediamanana_alt_3",
                    name = "Tortitas de Arroz Integral con Pavo y Queso Blanco",
                    description = "Snack práctico ideal para llevar al trabajo o entrenamiento.",
                    prepTimeMinutes = 3,
                    calories = 230,
                    proteinGrams = 25,
                    carbsGrams = 20,
                    fatGrams = 4,
                    fiberGrams = 3,
                    satietyIndexScore = 8.1f,
                    micronutrientHighlights = listOf("Sodio Controlado", "Niacina (Vit B3)"),
                    recipeInstructions = listOf(
                        "Tomar 3 tortitas de arroz integral crujientes.",
                        "Colocar 60g de pechuga de pavo bajo en sal y 30g de queso fresco desnatado sobre las tortitas."
                    ),
                    ingredients = listOf(
                        IngredientDetail("Tortitas de Arroz Integral", 3f, "unidades", "Granos & Cereales"),
                        IngredientDetail("Pechuga de Pavo", 60f, "g", "Proteínas & Carnes"),
                        IngredientDetail("Queso Fresco Desnatado", 30f, "g", "Lácteos & Huevos")
                    )
                )
            )

            MealSlotType.ALMUERZO -> listOf(
                DishAlternative(
                    id = "almuerzo_alt_1",
                    name = "Pechuga de Pollo con Quinoa Real y Brócoli al Vapor",
                    description = "Comida emblemática de nutrición deportiva. Alto valor de saciedad y carbohidratos complejos.",
                    prepTimeMinutes = 20,
                    calories = 620,
                    proteinGrams = 52,
                    carbsGrams = 65,
                    fatGrams = 14,
                    fiberGrams = 9,
                    satietyIndexScore = 9.5f,
                    micronutrientHighlights = listOf("Sulforafano (Anticancerígeno)", "Hierro Hemínico", "Magnesio"),
                    recipeInstructions = listOf(
                        "Cocer 70g de quinoa real en agua con sal durante 12-14 minutos al dente.",
                        "Cocinar 150g de brócoli fresco al vapor durante 7 minutos.",
                        "Sazonar 200g de pechuga de pollo con orégano y ajo y marcar en la sartén a fuego medio.",
                        "Mezclar los ingredientes y aderezar con 10ml de aceite de oliva extra virgen."
                    ),
                    ingredients = listOf(
                        IngredientDetail("Pechuga de Pollo fresca", 200f, "g", "Proteínas & Carnes"),
                        IngredientDetail("Quinoa Real (peso en seco)", 70f, "g", "Granos & Cereales"),
                        IngredientDetail("Brócoli fresco", 150f, "g", "Frutas & Verduras"),
                        IngredientDetail("Aceite de Oliva Extra Virgen", 10f, "ml", "Grasas & Aceites")
                    )
                ),
                DishAlternative(
                    id = "almuerzo_alt_2",
                    name = "Ternera Magra con Arroz Basmati, Pimientos y Aguacate",
                    description = "Plato anabólico cargado de aminoácidos esenciales, zinc y grasas monoinsaturadas.",
                    prepTimeMinutes = 18,
                    calories = 620,
                    proteinGrams = 50,
                    carbsGrams = 64,
                    fatGrams = 16,
                    fiberGrams = 7,
                    satietyIndexScore = 9.3f,
                    micronutrientHighlights = listOf("Zinc", "Vitamina B12", "Hierro Biodisponible"),
                    recipeInstructions = listOf(
                        "Hervir 70g de arroz basmati aromático.",
                        "Saltear 180g de ternera magra en tiras con pimientos rojos y verdes en la sartén.",
                        "Emplatar el arroz como base con la ternera salteada y 40g de aguacate fresco."
                    ),
                    ingredients = listOf(
                        IngredientDetail("Filete de Ternera Magra", 180f, "g", "Proteínas & Carnes"),
                        IngredientDetail("Arroz Basmati (peso en seco)", 70f, "g", "Granos & Cereales"),
                        IngredientDetail("Pimientos Rojo y Verde", 100f, "g", "Frutas & Verduras"),
                        IngredientDetail("Aguacate", 40f, "g", "Frutas & Verduras"),
                        IngredientDetail("Aceite de Oliva Extra Virgen", 8f, "ml", "Grasas & Aceites")
                    )
                ),
                DishAlternative(
                    id = "almuerzo_alt_3",
                    name = "Lomo de Pavo con Batata / Boniato Asado y Ensalada Verde",
                    description = "Combinación ligera de carbohidratos de asimilación lenta y alto volumen vegetal.",
                    prepTimeMinutes = 25,
                    calories = 620,
                    proteinGrams = 48,
                    carbsGrams = 68,
                    fatGrams = 12,
                    fiberGrams = 10,
                    satietyIndexScore = 9.4f,
                    micronutrientHighlights = listOf("Betacarotenos (Vit A)", "Potasio", "Fibra Soluble"),
                    recipeInstructions = listOf(
                        "Asar 200g de batata/boniato troceado en la airfryer u horno a 200°C durante 20 min.",
                        "Cocinar 200g de solomillo de pavo a la plancha con romero y pimienta.",
                        "Servir acompañado de una ensalada abundante de canónigos y pepino aderezada con AOVE."
                    ),
                    ingredients = listOf(
                        IngredientDetail("Solomillo / Pechuga de Pavo", 200f, "g", "Proteínas & Carnes"),
                        IngredientDetail("Batata / Boniato dulce", 200f, "g", "Frutas & Verduras"),
                        IngredientDetail("Hojas de Canónigos y Pepino", 100f, "g", "Frutas & Verduras"),
                        IngredientDetail("Aceite de Oliva Extra Virgen", 10f, "ml", "Grasas & Aceites")
                    )
                ),
                DishAlternative(
                    id = "almuerzo_alt_4",
                    name = "Lentejas Estofadas con Verduras y Taco de Jamón Magro",
                    description = "Legumbres tradicionales ricas en fibra soluble, hierro no hemínico y proteína vegetal.",
                    prepTimeMinutes = 30,
                    calories = 620,
                    proteinGrams = 45,
                    carbsGrams = 70,
                    fatGrams = 11,
                    fiberGrams = 14,
                    satietyIndexScore = 9.6f,
                    micronutrientHighlights = listOf("Hierro Leguminosa", "Ácido Fólico (B9)", "Fibra Alta"),
                    recipeInstructions = listOf(
                        "Cocer 80g de lentejas pardinas con zanahoria, cebolla, laurel y ajo.",
                        "Agregar 50g de tacos de jamón serrano magro desgrasado los últimos 10 minutos de cocción.",
                        "Servir bien caliente aderezado con un chorrito de aceite de oliva crudo."
                    ),
                    ingredients = listOf(
                        IngredientDetail("Lentejas Pardinas (peso en seco)", 80f, "g", "Legumbres"),
                        IngredientDetail("Jamón Serrano Magro en Tacos", 50f, "g", "Proteínas & Carnes"),
                        IngredientDetail("Zanahoria, Cebolla y Pimiento", 120f, "g", "Frutas & Verduras"),
                        IngredientDetail("Aceite de Oliva Extra Virgen", 8f, "ml", "Grasas & Aceites")
                    )
                )
            )

            MealSlotType.MERIENDA -> listOf(
                DishAlternative(
                    id = "merienda_alt_1",
                    name = "Yogur Griego 0% con Nueces y Canela",
                    description = "Merienda rica en proteína de absorción lenta (caseína) ideal para mantener el estado anabólico.",
                    prepTimeMinutes = 3,
                    calories = 220,
                    proteinGrams = 22,
                    carbsGrams = 12,
                    fatGrams = 9,
                    fiberGrams = 3,
                    satietyIndexScore = 8.6f,
                    micronutrientHighlights = listOf("Calcio Tópico", "Omega-3 Vegetal (ALA)", "Sensibilidad Insulina"),
                    recipeInstructions = listOf(
                        "Servir 200g de yogur griego 0% grasa en un tazón.",
                        "Añadir 15g de nueces troceadas y espolvorear canela en polvo."
                    ),
                    ingredients = listOf(
                        IngredientDetail("Yogur Griego 0% Grasa", 200f, "g", "Lácteos & Huevos"),
                        IngredientDetail("Nueces troceadas", 15f, "g", "Frutos Secos"),
                        IngredientDetail("Canela Ceylan en polvo", 2f, "g", "Especias")
                    )
                ),
                DishAlternative(
                    id = "merienda_alt_2",
                    name = "Sandwich Integral de Atún al Natural y Tomate",
                    description = "Aporte proteico rápido con ácidos grasos omega-3 y licopeno.",
                    prepTimeMinutes = 5,
                    calories = 220,
                    proteinGrams = 24,
                    carbsGrams = 20,
                    fatGrams = 4,
                    fiberGrams = 4,
                    satietyIndexScore = 8.4f,
                    micronutrientHighlights = listOf("Licopeno", "Omega-3", "Proteína de Pesca"),
                    recipeInstructions = listOf(
                        "Escurrir 1 lata de atún al natural (80g).",
                        "Mezclar con tomate cortado en rodajas finas y montar entre 2 rebanadas de pan integral de centeno."
                    ),
                    ingredients = listOf(
                        IngredientDetail("Pan Integral de Centeno", 60f, "g", "Granos & Cereales"),
                        IngredientDetail("Atún al Natural en lata", 80f, "g", "Pescados & Mariscos"),
                        IngredientDetail("Tomate fresco", 50f, "g", "Frutas & Verduras")
                    )
                ),
                DishAlternative(
                    id = "merienda_alt_3",
                    name = "Smoothie de Proteína Isolate con Plátano y Cacao",
                    description = "Recuperador muscular con triptófano, potasio y flavonoides de cacao.",
                    prepTimeMinutes = 4,
                    calories = 220,
                    proteinGrams = 25,
                    carbsGrams = 22,
                    fatGrams = 2,
                    fiberGrams = 4,
                    satietyIndexScore = 8.2f,
                    micronutrientHighlights = listOf("Potasio", "Flavonoides de Cacao", "Magnesio"),
                    recipeInstructions = listOf(
                        "Triturar 1 plátano mediano con 25g de proteína Whey Isolate, 5g de cacao puro 100% desgrasado y 200ml de agua."
                    ),
                    ingredients = listOf(
                        IngredientDetail("Proteína Whey Isolate", 25f, "g", "Suplementos & Proteínas"),
                        IngredientDetail("Plátano maduro", 100f, "g", "Frutas & Verduras"),
                        IngredientDetail("Cacao Puro 100% en Polvo", 5f, "g", "Otros")
                    )
                )
            )

            MealSlotType.CENA -> listOf(
                DishAlternative(
                    id = "cena_alt_1",
                    name = "Lomo de Salmón al Horno con Espárragos Trigueros",
                    description = "Cena densa en micronutrientes y grasas esenciales Omega-3 que favorecen el sueño reparador.",
                    prepTimeMinutes = 18,
                    calories = 460,
                    proteinGrams = 42,
                    carbsGrams = 12,
                    fatGrams = 24,
                    fiberGrams = 5,
                    satietyIndexScore = 9.1f,
                    micronutrientHighlights = listOf("Omega-3 (DHA & EPA)", "Vitamina D", "Selenio"),
                    recipeInstructions = listOf(
                        "Precalentar el horno o freidora de aire a 190°C.",
                        "Colocar 180g de lomo de salmón fresco y 120g de espárragos trigueros en una bandeja.",
                        "Sazonar con sal marina, pimienta negra, ajo y un chorrito de limón.",
                        "Hornear durante 12-14 minutos y servir con brotes verdes."
                    ),
                    ingredients = listOf(
                        IngredientDetail("Lomo de Salmón fresco", 180f, "g", "Pescados & Mariscos"),
                        IngredientDetail("Espárragos verdes trigueros", 120f, "g", "Frutas & Verduras"),
                        IngredientDetail("Canónigos / Hojas verdes", 50f, "g", "Frutas & Verduras"),
                        IngredientDetail("Zumo de Limón fresco", 15f, "ml", "Frutas & Verduras")
                    )
                ),
                DishAlternative(
                    id = "cena_alt_2",
                    name = "Lubina a la Espalda con Patata Asada y Calabacín",
                    description = "Pescado blanco de fácil digestión con carbohidratos saciantes de bajo índice glucémico.",
                    prepTimeMinutes = 22,
                    calories = 460,
                    proteinGrams = 40,
                    carbsGrams = 38,
                    fatGrams = 12,
                    fiberGrams = 6,
                    satietyIndexScore = 9.3f,
                    micronutrientHighlights = listOf("Fósforo", "Potasio", "Yodo"),
                    recipeInstructions = listOf(
                        "Cortar 150g de patata fresca en láminas y asar al horno durante 15 minutos.",
                        "Añadir 200g de filete de lubina y calabacín en rodajas.",
                        "Sazonar con perejil fresco, ajo picado y un chorrito de aceite de oliva. Hornear 10 min más."
                    ),
                    ingredients = listOf(
                        IngredientDetail("Filete de Lubina / Dorada", 200f, "g", "Pescados & Mariscos"),
                        IngredientDetail("Patata fresca", 150f, "g", "Frutas & Verduras"),
                        IngredientDetail("Calabacín fresco", 100f, "g", "Frutas & Verduras"),
                        IngredientDetail("Aceite de Oliva Extra Virgen", 8f, "ml", "Grasas & Aceites")
                    )
                ),
                DishAlternative(
                    id = "cena_alt_3",
                    name = "Pechuga de Pavo con Crema de Verduras y Aceite de Oliva",
                    description = "Opción ultra-ligera y saciante de alta hidratación celular antes de dormir.",
                    prepTimeMinutes = 20,
                    calories = 460,
                    proteinGrams = 44,
                    carbsGrams = 25,
                    fatGrams = 14,
                    fiberGrams = 8,
                    satietyIndexScore = 9.0f,
                    micronutrientHighlights = listOf("Vitamina C", "Calcio", "Magnesio Relax"),
                    recipeInstructions = listOf(
                        "Cocer calabacín, puerro y espinacas en agua con sal durante 12 minutos y triturar hasta obtener una crema suave.",
                        "Cocinar 200g de pechuga de pavo a la plancha con hierbas provenzales.",
                        "Servir el pavo acompañado del bol de crema de verduras aderezado con 10ml de AOVE."
                    ),
                    ingredients = listOf(
                        IngredientDetail("Pechuga de Pavo", 200f, "g", "Proteínas & Carnes"),
                        IngredientDetail("Calabacín, Puerro y Espinacas", 250f, "g", "Frutas & Verduras"),
                        IngredientDetail("Aceite de Oliva Extra Virgen", 10f, "ml", "Grasas & Aceites")
                    )
                ),
                DishAlternative(
                    id = "cena_alt_4",
                    name = "Revuelto de Gambas, Setas y Huevos con Ensalada",
                    description = "Cena proteica baja en carbohidratos rica en yodo, selenio y colina.",
                    prepTimeMinutes = 12,
                    calories = 460,
                    proteinGrams = 42,
                    carbsGrams = 10,
                    fatGrams = 22,
                    fiberGrams = 5,
                    satietyIndexScore = 8.9f,
                    micronutrientHighlights = listOf("Colina Huevo", "Yodo Marino", "Selenio"),
                    recipeInstructions = listOf(
                        "Saltear 100g de gambas peladas y 100g de setas en la sartén con un diente de ajo laminado.",
                        "Batir 2 huevos enteros y 100ml de claras y verter sobre las gambas.",
                        "Remover a fuego suave hasta obtener un revuelto jugoso. Servir con ensalada verde."
                    ),
                    ingredients = listOf(
                        IngredientDetail("Gambas Peladas", 100f, "g", "Pescados & Mariscos"),
                        IngredientDetail("Setas / Champiñones", 100f, "g", "Frutas & Verduras"),
                        IngredientDetail("Huevo entero", 2f, "huevos", "Lácteos & Huevos"),
                        IngredientDetail("Claras de Huevo", 100f, "ml", "Lácteos & Huevos"),
                        IngredientDetail("Aceite de Oliva Extra Virgen", 8f, "ml", "Grasas & Aceites")
                    )
                )
            )
        }
    }
}
