package com.example.personalsport.diet

import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToInt

enum class DietGoal(
    val title: String,
    val description: String,
    val shortLabel: String
) {
    FAT_LOSS(
        title = "Pérdida de Grasa",
        description = "Déficit calórico moderado con alta proteína para preservar masa muscular y optimizar la quema de lípidos.",
        shortLabel = "Deficit - Grasa"
    ),
    MUSCLE_GAIN(
        title = "Ganancia Muscular",
        description = "Superávit calórico controlado con alto aporte de carbohidratos y proteínas para promover hipertrofia.",
        shortLabel = "Superávit - Músculo"
    ),
    BODY_RECOMP(
        title = "Recomposición Corporal (Ambas)",
        description = "Ajuste normocalórico o ciclado estratégico para quemar grasa corporal y ganar masa muscular simultáneamente.",
        shortLabel = "Recomposición"
    ),
    ATHLETIC_PERFORMANCE(
        title = "Rendimiento Atlético / Natación & Atletismo",
        description = "Carga estratégica de glucógeno y balance macronutricional para deportes de alta intensidad y resistencia.",
        shortLabel = "Rendimiento & Natación"
    )
}

enum class Gender(val label: String) {
    MALE("Masculino"),
    FEMALE("Femenino")
}

enum class ActivityLevel(
    val label: String,
    val description: String,
    val multiplier: Float
) {
    SEDENTARY("Sedentario", "Poco o ningún ejercicio físico diario", 1.2f),
    LIGHT("Actividad Ligera", "Entrenamiento 1-3 días por semana", 1.375f),
    MODERATE("Actividad Moderada", "Entrenamiento 3-5 días por semana", 1.55f),
    ACTIVE("Muy Activo", "Entrenamiento intenso 6-7 días por semana", 1.725f),
    EXTRA_ACTIVE("Atleta de Élite", "Entrenamientos dobles / alto volumen diario", 1.9f)
}

data class BodyProfile(
    val weightKg: Float = 75f,
    val heightCm: Float = 175f,
    val age: Int = 26,
    val gender: Gender = Gender.MALE,
    val bodyFatPercentage: Float? = 15f,
    val activityLevel: ActivityLevel = ActivityLevel.MODERATE,
    val workoutActiveCalories: Int = 350
)

data class MacroTarget(
    val calories: Int,
    val proteinGrams: Int,
    val carbsGrams: Int,
    val fatGrams: Int,
    val proteinCalories: Int = proteinGrams * 4,
    val carbsCalories: Int = carbsGrams * 4,
    val fatCalories: Int = fatGrams * 9,
    val proteinPercentage: Int = if (calories > 0) ((proteinGrams * 4f / calories) * 100).roundToInt() else 0,
    val carbsPercentage: Int = if (calories > 0) ((carbsGrams * 4f / calories) * 100).roundToInt() else 0,
    val fatPercentage: Int = if (calories > 0) ((fatGrams * 9f / calories) * 100).roundToInt() else 0
)

enum class MealType(val title: String, val recommendedCalorieRatio: Float) {
    DESAYUNO("Desayuno", 0.25f),
    ALMUERZO("Almuerzo / Media Mañana", 0.10f),
    COMIDA("Comida / Almuerzo Principal", 0.35f),
    MERIENDA("Merienda", 0.10f),
    CENA("Cena", 0.20f)
}

data class Ingredient(
    val name: String,
    val baseQuantityGrams: Float,
    val displayUnit: String = "g",
    val category: String = "Otros"
) {
    fun formatQuantity(scaleFactor: Float): String {
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

data class Recipe(
    val title: String,
    val prepTimeMinutes: Int,
    val instructions: List<String>,
    val baseIngredients: List<Ingredient>
)

data class MealPlanItem(
    val id: String,
    val mealType: MealType,
    val name: String,
    val description: String,
    val calories: Int,
    val proteinGrams: Int,
    val carbsGrams: Int,
    val fatGrams: Int,
    val recipe: Recipe,
    val scaleFactor: Float = 1.0f
) {
    val scaledIngredients: List<Pair<Ingredient, String>>
        get() = recipe.baseIngredients.map { ingredient ->
            ingredient to ingredient.formatQuantity(scaleFactor)
        }
}

data class DailyMealPlan(
    val goal: DietGoal,
    val targetMacros: MacroTarget,
    val meals: List<MealPlanItem>
) {
    val actualCalories: Int = meals.sumOf { it.calories }
    val actualProteinGrams: Int = meals.sumOf { it.proteinGrams }
    val actualCarbsGrams: Int = meals.sumOf { it.carbsGrams }
    val actualFatGrams: Int = meals.sumOf { it.fatGrams }
}

data class GroceryCategory(
    val categoryName: String,
    val items: List<String>
)

object DietEngine {

    /**
     * Calculates Basal Metabolic Rate (BMR) in kcal/day.
     * Uses Katch-McArdle formula if body fat percentage is provided (>0),
     * otherwise falls back to Mifflin-St Jeor formula.
     */
    fun calculateBmr(profile: BodyProfile): Int {
        val bf = profile.bodyFatPercentage
        return if (bf != null && bf in 3.0f..60.0f) {
            // Katch-McArdle equation based on Lean Body Mass
            val leanMassKg = profile.weightKg * (1f - (bf / 100f))
            (370 + (21.6f * leanMassKg)).roundToInt()
        } else {
            // Mifflin-St Jeor equation
            val genderOffset = if (profile.gender == Gender.MALE) 5 else -161
            ((10f * profile.weightKg) + (6.25f * profile.heightCm) - (5f * profile.age) + genderOffset).roundToInt()
        }
    }

    /**
     * Calculates Total Daily Energy Expenditure (TDEE) in kcal/day.
     * Combines BMR multiplied by activity factor + extra calories burned during daily active workouts.
     */
    fun calculateTdee(profile: BodyProfile): Int {
        val bmr = calculateBmr(profile)
        val baseTdee = bmr * profile.activityLevel.multiplier
        return (baseTdee + profile.workoutActiveCalories).roundToInt()
    }

    /**
     * Calculates caloric target & macronutrient distribution according to user's fitness goal.
     */
    fun calculateTargetMacros(profile: BodyProfile, goal: DietGoal): MacroTarget {
        val tdee = calculateTdee(profile)
        val weight = profile.weightKg

        return when (goal) {
            DietGoal.FAT_LOSS -> {
                // Caloric deficit: ~20% below TDEE, minimum 1200 kcal
                val targetCalories = max(1200, (tdee * 0.80f).roundToInt())
                // Protein: High protein (2.3g per kg weight) to maintain muscle during deficit
                val proteinGrams = (weight * 2.3f).roundToInt().coerceAtLeast(100)
                val proteinCalories = proteinGrams * 4
                
                // Fat: ~25% of total calories
                val fatCalories = (targetCalories * 0.25f).roundToInt()
                val fatGrams = (fatCalories / 9f).roundToInt().coerceAtLeast(40)

                // Carbs: Remaining calories
                val remainingCalories = max(0, targetCalories - proteinCalories - (fatGrams * 9))
                val carbsGrams = (remainingCalories / 4f).roundToInt()

                MacroTarget(
                    calories = targetCalories,
                    proteinGrams = proteinGrams,
                    carbsGrams = carbsGrams,
                    fatGrams = fatGrams
                )
            }

            DietGoal.MUSCLE_GAIN -> {
                // Caloric surplus: +15% above TDEE
                val targetCalories = (tdee * 1.15f).roundToInt()
                // Protein: High (2.0g per kg body weight)
                val proteinGrams = (weight * 2.0f).roundToInt().coerceAtLeast(120)
                val proteinCalories = proteinGrams * 4

                // Fat: Moderate (~20% of calories)
                val fatCalories = (targetCalories * 0.20f).roundToInt()
                val fatGrams = (fatCalories / 9f).roundToInt().coerceAtLeast(50)

                // Carbs: High (~55% of calories) for training energy & glycogen
                val remainingCalories = max(0, targetCalories - proteinCalories - (fatGrams * 9))
                val carbsGrams = (remainingCalories / 4f).roundToInt()

                MacroTarget(
                    calories = targetCalories,
                    proteinGrams = proteinGrams,
                    carbsGrams = carbsGrams,
                    fatGrams = fatGrams
                )
            }

            DietGoal.BODY_RECOMP -> {
                // Recomposition: Near maintenance calories (~95% TDEE with slight deficit)
                val targetCalories = max(1300, (tdee * 0.95f).roundToInt())
                // High protein (2.2g per kg) to support muscle building while in slight deficit
                val proteinGrams = (weight * 2.2f).roundToInt().coerceAtLeast(110)
                val proteinCalories = proteinGrams * 4

                // Fat: ~25% of calories
                val fatCalories = (targetCalories * 0.25f).roundToInt()
                val fatGrams = (fatCalories / 9f).roundToInt().coerceAtLeast(45)

                // Carbs: Remaining calories
                val remainingCalories = max(0, targetCalories - proteinCalories - (fatGrams * 9))
                val carbsGrams = (remainingCalories / 4f).roundToInt()

                MacroTarget(
                    calories = targetCalories,
                    proteinGrams = proteinGrams,
                    carbsGrams = carbsGrams,
                    fatGrams = fatGrams
                )
            }

            DietGoal.ATHLETIC_PERFORMANCE -> {
                // Performance focus: Surplus / match high energy expenditure (+5% for endurance buffer)
                val targetCalories = (tdee * 1.05f).roundToInt()
                
                // Carbs: High performance carb loading (~55% - 60% of total calories or 5.5g/kg)
                val carbsGrams = max((weight * 5.5f).roundToInt(), ((targetCalories * 0.58f) / 4f).roundToInt())
                val carbsCalories = carbsGrams * 4

                // Protein: 1.8g per kg for endurance muscle recovery
                val proteinGrams = (weight * 1.8f).roundToInt().coerceAtLeast(110)
                val proteinCalories = proteinGrams * 4

                // Fat: Balance remaining calories (~20%)
                val remainingCalories = max(0, targetCalories - carbsCalories - proteinCalories)
                val fatGrams = (remainingCalories / 9f).roundToInt().coerceAtLeast(45)

                MacroTarget(
                    calories = targetCalories,
                    proteinGrams = proteinGrams,
                    carbsGrams = carbsGrams,
                    fatGrams = fatGrams
                )
            }
        }
    }

    /**
     * Generates a complete daily meal plan with recipes, ingredient quantities, and total macros tailored to goal.
     */
    fun generateDailyMealPlan(profile: BodyProfile, goal: DietGoal): DailyMealPlan {
        val targetMacros = calculateTargetMacros(profile, goal)
        val rawTemplates = getMealTemplatesForGoal(goal)

        val totalBaseCalories = rawTemplates.sumOf { it.calories }
        val scaleFactor = if (totalBaseCalories > 0) targetMacros.calories.toFloat() / totalBaseCalories.toFloat() else 1.0f

        val scaledMeals = rawTemplates.map { template ->
            val scaledCal = (template.calories * scaleFactor).roundToInt()
            val scaledProt = (template.proteinGrams * scaleFactor).roundToInt()
            val scaledCarbs = (template.carbsGrams * scaleFactor).roundToInt()
            val scaledFat = (template.fatGrams * scaleFactor).roundToInt()

            template.copy(
                calories = scaledCal,
                proteinGrams = scaledProt,
                carbsGrams = scaledCarbs,
                fatGrams = scaledFat,
                scaleFactor = scaleFactor
            )
        }

        return DailyMealPlan(
            goal = goal,
            targetMacros = targetMacros,
            meals = scaledMeals
        )
    }

    /**
     * Consolidates ingredient lists across all meals into a clean, categorized grocery list.
     */
    fun generateGroceryList(mealPlan: DailyMealPlan): List<GroceryCategory> {
        val categoryMap = mutableMapOf<String, MutableList<String>>()

        mealPlan.meals.forEach { meal ->
            meal.recipe.baseIngredients.forEach { ingredient ->
                val qtyStr = ingredient.formatQuantity(meal.scaleFactor)
                val category = ingredient.category
                val itemText = "${ingredient.name} ($qtyStr)"

                val list = categoryMap.getOrPut(category) { mutableListOf() }
                list.add(itemText)
            }
        }

        return categoryMap.map { (cat, items) ->
            GroceryCategory(categoryName = cat, items = items.distinct())
        }.sortedBy { it.categoryName }
    }

    // Private helpers for meal templates per goal
    private fun getMealTemplatesForGoal(goal: DietGoal): List<MealPlanItem> {
        return when (goal) {
            DietGoal.BODY_RECOMP -> getMealTemplatesForGoal(DietGoal.FAT_LOSS)
            DietGoal.FAT_LOSS -> listOf(
                MealPlanItem(
                    id = "desayuno_fatloss",
                    mealType = MealType.DESAYUNO,
                    name = "Tortilla Proteica con Espinacas y Pavo",
                    description = "Tortilla alta en proteínas y baja en carbohidratos con claras, pavo y aguacate.",
                    calories = 420,
                    proteinGrams = 42,
                    carbsGrams = 18,
                    fatGrams = 16,
                    recipe = Recipe(
                        title = "Tortilla Proteica Fitness",
                        prepTimeMinutes = 12,
                        instructions = listOf(
                            "Batir las claras de huevo con sal y pimienta al gusto.",
                            "Saltear las espinacas frescas y la pechuga de pavo picada en una sartén con unas gotas de aceite de oliva.",
                            "Verter las claras sobre la sartén y cocinar a fuego medio hasta cuajar.",
                            "Servir acompañado de rodajas de aguacate fresco."
                        ),
                        baseIngredients = listOf(
                            Ingredient("Claras de Huevo", 200f, "ml", "Lácteos & Huevos"),
                            Ingredient("Huevo entero", 1f, "huevos", "Lácteos & Huevos"),
                            Ingredient("Pechuga de Pavo en lonchas", 80f, "g", "Proteínas & Carnes"),
                            Ingredient("Espinacas frescas", 60f, "g", "Frutas & Verduras"),
                            Ingredient("Aguacate", 40f, "g", "Frutas & Verduras"),
                            Ingredient("Aceite de Oliva Extra Virgen", 5f, "ml", "Grasas & Aceites")
                        )
                    )
                ),
                MealPlanItem(
                    id = "almuerzo_fatloss",
                    mealType = MealType.ALMUERZO,
                    name = "Batido Proteico Reductor con Berries",
                    description = "Snack saciante rico en antioxidantes y proteínas de rápida absorción.",
                    calories = 220,
                    proteinGrams = 28,
                    carbsGrams = 18,
                    fatGrams = 3,
                    recipe = Recipe(
                        title = "Smoothie Proteico Antioxidante",
                        prepTimeMinutes = 5,
                        instructions = listOf(
                            "Añadir la proteína Whey, frutos rojos congelados y la bebida de almendras a la batidora.",
                            "Triturar durante 45 segundos hasta obtener una consistencia cremosa y homogénea.",
                            "Consumir inmediatamente bien frío."
                        ),
                        baseIngredients = listOf(
                            Ingredient("Proteína Aislada de Suero (Whey Isolate)", 30f, "g", "Suplementos & Proteínas"),
                            Ingredient("Frutos Rojos / Arándanos", 100f, "g", "Frutas & Verduras"),
                            Ingredient("Bebida de Almendras sin azúcar", 250f, "ml", "Lácteos & Bebidas")
                        )
                    )
                ),
                MealPlanItem(
                    id = "comida_fatloss",
                    mealType = MealType.COMIDA,
                    name = "Pechuga de Pollo a la Plancha con Quinoa y Brócoli",
                    description = "Comida equilibrada con carbohidratos de bajo índice glucémico y vegetal saciante.",
                    calories = 580,
                    proteinGrams = 52,
                    carbsGrams = 48,
                    fatGrams = 14,
                    recipe = Recipe(
                        title = "Pollo Dorado con Quinoa Real y Brócoli al Vapor",
                        prepTimeMinutes = 20,
                        instructions = listOf(
                            "Cocer la quinoa en agua hirviendo con una pizca de sal durante 12-15 minutos.",
                            "Cocer el brócoli al vapor durante 7 minutos para mantener sus nutrientes.",
                            "Sazonar la pechuga de pollo con orégano y ajo en polvo y marcar a la plancha.",
                            "Emplatar la quinoa como base, la pechuga cortada en tiras y el brócoli aderezado con aceite de oliva."
                        ),
                        baseIngredients = listOf(
                            Ingredient("Pechuga de Pollo", 200f, "g", "Proteínas & Carnes"),
                            Ingredient("Quinoa (peso en seco)", 60f, "g", "Granos & Cereales"),
                            Ingredient("Brócoli fresco", 150f, "g", "Frutas & Verduras"),
                            Ingredient("Aceite de Oliva Extra Virgen", 10f, "ml", "Grasas & Aceites")
                        )
                    )
                ),
                MealPlanItem(
                    id = "merienda_fatloss",
                    mealType = MealType.MERIENDA,
                    name = "Yogur Griego 0% con Nueces y Canela",
                    description = "Aporte proteico de digestión lenta (caseína) con grasas saludables.",
                    calories = 210,
                    proteinGrams = 20,
                    carbsGrams = 10,
                    fatGrams = 9,
                    recipe = Recipe(
                        title = "Bol de Yogur Griego y Frutos Secos",
                        prepTimeMinutes = 3,
                        instructions = listOf(
                            "Servir el yogur griego 0% en un tazón.",
                            "Trocear las nueces y espolvorear sobre el yogur.",
                            "Añadir una pizca de canela en polvo para potenciar el sabor y la sensibilidad a la insulina."
                        ),
                        baseIngredients = listOf(
                            Ingredient("Yogur Griego Natural 0% Grasa", 200f, "g", "Lácteos & Huevos"),
                            Ingredient("Nueces peladas", 15f, "g", "Frutos Secos"),
                            Ingredient("Canela en polvo", 2f, "g", "Especias")
                        )
                    )
                ),
                MealPlanItem(
                    id = "cena_fatloss",
                    mealType = MealType.CENA,
                    name = "Lomo de Salmón al Horno con Espárragos y Ensalada Verde",
                    description = "Cena ligera rica en Ácidos Grasos Omega-3 y micronutrientes.",
                    calories = 450,
                    proteinGrams = 40,
                    carbsGrams = 12,
                    fatGrams = 22,
                    recipe = Recipe(
                        title = "Salmón Grillado con Espárragos Trigueros",
                        prepTimeMinutes = 18,
                        instructions = listOf(
                            "Precalentar el horno o la freidora de aire a 190°C.",
                            "Colocar el lomo de salmón y los espárragos en una bandeja con sal marina, pimienta y limón.",
                            "Hornear durante 12-14 minutos.",
                            "Servir con una ensalada de canónigos y pepino aderezada con limón."
                        ),
                        baseIngredients = listOf(
                            Ingredient("Lomo de Salmón fresco", 180f, "g", "Pescados & Mariscos"),
                            Ingredient("Espárragos verdes trigueros", 120f, "g", "Frutas & Verduras"),
                            Ingredient("Canónigos / Hojas verdes", 50f, "g", "Frutas & Verduras"),
                            Ingredient("Zumo de Limón fresco", 15f, "ml", "Frutas & Verduras")
                        )
                    )
                )
            )

            DietGoal.MUSCLE_GAIN -> listOf(
                MealPlanItem(
                    id = "desayuno_musclegain",
                    mealType = MealType.DESAYUNO,
                    name = "Porridge de Avena Anabólico con Platano y Mantequilla de Cacahuete",
                    description = "Desayuno hipercalórico denso en carbohidratos complejas y energía sostenida.",
                    calories = 680,
                    proteinGrams = 38,
                    carbsGrams = 85,
                    fatGrams = 20,
                    recipe = Recipe(
                        title = "Bowl de Avena con Proteína y Crema de Cacahuete",
                        prepTimeMinutes = 10,
                        instructions = listOf(
                            "Cocer los copos de avena en la leche entera o de avena a fuego medio hasta espesar.",
                            "Retirar del fuego y mezclar enérgicamente con la proteína en polvo.",
                            "Decorar con el plátano cortado en rodajas y la cucharada de mantequilla de cacahuete."
                        ),
                        baseIngredients = listOf(
                            Ingredient("Copos de Avena integral", 90f, "g", "Granos & Cereales"),
                            Ingredient("Leche Entera / Proteica", 250f, "ml", "Lácteos & Bebidas"),
                            Ingredient("Proteína Whey (Sabor Vainilla o Chocolate)", 30f, "g", "Suplementos & Proteínas"),
                            Ingredient("Plátano maduro", 120f, "g", "Frutas & Verduras"),
                            Ingredient("Mantequilla de Cacahuete 100% natural", 25f, "g", "Grasas & Aceites")
                        )
                    )
                ),
                MealPlanItem(
                    id = "almuerzo_musclegain",
                    mealType = MealType.ALMUERZO,
                    name = "Bocadillo de Ternasco / Ternera con Queso y Tomate",
                    description = "Snack energético entre horas rico en aminoácidos esenciales.",
                    calories = 420,
                    proteinGrams = 32,
                    carbsGrams = 45,
                    fatGrams = 12,
                    recipe = Recipe(
                        title = "Bocadillo de Ternera Magra y Queso Havarti",
                        prepTimeMinutes = 8,
                        instructions = listOf(
                            "Tostar el pan integral de centeno o masa madre.",
                            "Cocinar los filetes de ternera a la plancha a fuego fuerte.",
                            "Montar el bocadillo con el queso derretido, tomate en rodajas y filetes de ternera."
                        ),
                        baseIngredients = listOf(
                            Ingredient("Pan Integral / Masa Madre", 80f, "g", "Granos & Cereales"),
                            Ingredient("Filete de Ternera Magra", 110f, "g", "Proteínas & Carnes"),
                            Ingredient("Queso Havarti / Mozzarella", 30f, "g", "Lácteos & Huevos"),
                            Ingredient("Tomate natural", 50f, "g", "Frutas & Verduras")
                        )
                    )
                ),
                MealPlanItem(
                    id = "comida_musclegain",
                    mealType = MealType.COMIDA,
                    name = "Arroz con Ternera Magra, Aguacate y Huevo Frito",
                    description = "Comida hipertrófica clásica con carbohidratos de rápida asimilación y grasas monoinsaturadas.",
                    calories = 850,
                    proteinGrams = 55,
                    carbsGrams = 95,
                    fatGrams = 26,
                    recipe = Recipe(
                        title = "Arroz de Atleta con Ternera y Huevo",
                        prepTimeMinutes = 22,
                        instructions = listOf(
                            "Cocer el arroz jazmín o basmati en agua con ajo.",
                            "Cocinar la carne picada de ternera magra en la sartén sazonada con pimentón y sal.",
                            "Cocinar un huevo a la plancha o a la copa con aceite de oliva.",
                            "Servir en un bol grande mezclando el arroz, la ternera, el huevo y el aguacate troceado."
                        ),
                        baseIngredients = listOf(
                            Ingredient("Arroz Jazmín o Basmati (peso en seco)", 110f, "g", "Granos & Cereales"),
                            Ingredient("Carne Picada de Ternera Magra (90/10)", 180f, "g", "Proteínas & Carnes"),
                            Ingredient("Huevo fresco", 1f, "huevos", "Lácteos & Huevos"),
                            Ingredient("Aguacate", 60f, "g", "Frutas & Verduras"),
                            Ingredient("Aceite de Oliva Extra Virgen", 10f, "ml", "Grasas & Aceites")
                        )
                    )
                ),
                MealPlanItem(
                    id = "merienda_musclegain",
                    mealType = MealType.MERIENDA,
                    name = "Batido Mass Gainer Casero (Avena, Plátano, Miel y Whey)",
                    description = "Superávit calórico líquido ideal pre o post entrenamiento.",
                    calories = 480,
                    proteinGrams = 36,
                    carbsGrams = 65,
                    fatGrams = 8,
                    recipe = Recipe(
                        title = "Smoothie Hipercalórico de Asimilación Rápida",
                        prepTimeMinutes = 4,
                        instructions = listOf(
                            "Triturar la avena molida con la leche en la batidora.",
                            "Agregar el plátano, la miel pura y la proteína en polvo.",
                            "Mezclar bien durante 60 segundos y beber inmediatamente."
                        ),
                        baseIngredients = listOf(
                            Ingredient("Harina de Avena molida", 60f, "g", "Granos & Cereales"),
                            Ingredient("Proteína Whey Isolate", 35f, "g", "Suplementos & Proteínas"),
                            Ingredient("Plátano", 100f, "g", "Frutas & Verduras"),
                            Ingredient("Miel Pura de Abeja", 20f, "g", "Endulzantes"),
                            Ingredient("Leche Semidesnatada", 250f, "ml", "Lácteos & Bebidas")
                        )
                    )
                ),
                MealPlanItem(
                    id = "cena_musclegain",
                    mealType = MealType.CENA,
                    name = "Muslos de Pollo al Horno con Patatas y Ensalada Mixta",
                    description = "Cena completa para promover la síntesis proteica nocturna.",
                    calories = 650,
                    proteinGrams = 48,
                    carbsGrams = 60,
                    fatGrams = 22,
                    recipe = Recipe(
                        title = "Muslos de Pollo Asados con Patatas Asadas",
                        prepTimeMinutes = 35,
                        instructions = listOf(
                            "Cortar las patatas en gajos y aderezar con romero, ajo, pimienta y aceite de oliva.",
                            "Hornear el pollo y las patatas a 200°C durante 30-35 minutos hasta dorar.",
                            "Acompañar con una ensalada de tomate y maíz."
                        ),
                        baseIngredients = listOf(
                            Ingredient("Muslos / Pechuga de Pollo", 220f, "g", "Proteínas & Carnes"),
                            Ingredient("Patatas / Papa fresca", 250f, "g", "Frutas & Verduras"),
                            Ingredient("Maíz dulce en conserva", 50f, "g", "Frutas & Verduras"),
                            Ingredient("Aceite de Oliva Extra Virgen", 12f, "ml", "Grasas & Aceites")
                        )
                    )
                )
            )

            DietGoal.ATHLETIC_PERFORMANCE -> listOf(
                MealPlanItem(
                    id = "desayuno_endurance",
                    mealType = MealType.DESAYUNO,
                    name = "Tortitas Energéticas de Avena, Claras, Miel y Fruta Fresca",
                    description = "Combustible de alta densidad de carbohidratos para nadadores y corredores de fondo.",
                    calories = 550,
                    proteinGrams = 32,
                    carbsGrams = 88,
                    fatGrams = 8,
                    recipe = Recipe(
                        title = "Tortitas de Avena y Miel para Atletas",
                        prepTimeMinutes = 15,
                        instructions = listOf(
                            "Batir la harina de avena, claras de huevo, un chorrito de leche y una pizca de levadura.",
                            "Vertir porciones en la sartén antiadherente bien caliente.",
                            "Dar la vuelta cuando salgan burbujas y cocinar por ambos lados.",
                            "Servir cubierto de miel pura y plátano o fresas laminadas."
                        ),
                        baseIngredients = listOf(
                            Ingredient("Harina de Avena Integral", 90f, "g", "Granos & Cereales"),
                            Ingredient("Claras de Huevo", 180f, "ml", "Lácteos & Huevos"),
                            Ingredient("Miel Pura de Abeja", 25f, "g", "Endulzantes"),
                            Ingredient("Fresas o Plátano", 100f, "g", "Frutas & Verduras")
                        )
                    )
                ),
                MealPlanItem(
                    id = "almuerzo_endurance",
                    mealType = MealType.ALMUERZO,
                    name = "Tostadas de Pan de Masa Madre con Pavo y Dátiles",
                    description = "Carga rápida de carbohidratos de absorción media-rápida previa al entrenamiento de natación/atletismo.",
                    calories = 310,
                    proteinGrams = 22,
                    carbsGrams = 50,
                    fatGrams = 4,
                    recipe = Recipe(
                        title = "Tostadas de Masa Madre y Dátiles Deshuesados",
                        prepTimeMinutes = 5,
                        instructions = listOf(
                            "Tostar las rebanadas gruesas de pan de masa madre.",
                            "Colocar las lonchas de pechuga de pavo y acompañar con dátiles deshuesados."
                        ),
                        baseIngredients = listOf(
                            Ingredient("Pan de Masa Madre", 80f, "g", "Granos & Cereales"),
                            Ingredient("Pechuga de Pavo en lonchas", 70f, "g", "Proteínas & Carnes"),
                            Ingredient("Dátiles deshuesados", 30f, "g", "Frutas & Verduras")
                        )
                    )
                ),
                MealPlanItem(
                    id = "comida_endurance",
                    mealType = MealType.COMIDA,
                    name = "Pasta Integral con Atún, Tomate Natural y Aceitunas",
                    description = "Plato estrella para recarga muscular de glucógeno y recuperación de electrolitos.",
                    calories = 780,
                    proteinGrams = 48,
                    carbsGrams = 110,
                    fatGrams = 16,
                    recipe = Recipe(
                        title = "Pasta de Alto Rendimiento para Natación & Atletismo",
                        prepTimeMinutes = 18,
                        instructions = listOf(
                            "Hervir la pasta integral en abundante agua con sal al dente (10 min).",
                            "En una sartén, saltear el atún en al natural con salsa de tomate casera y orégano.",
                            "Mezclar la pasta con la salsa, añadir aceitunas picadas y servir caliente."
                        ),
                        baseIngredients = listOf(
                            Ingredient("Pasta Integral / Macarrones (peso en seco)", 130f, "g", "Granos & Cereales"),
                            Ingredient("Atún al natural en lata", 150f, "g", "Pescados & Mariscos"),
                            Ingredient("Salsa de Tomate Natural casera", 120f, "g", "Frutas & Verduras"),
                            Ingredient("Aceitunas negras / verdes", 25f, "g", "Grasas & Aceites"),
                            Ingredient("Aceite de Oliva Extra Virgen", 8f, "ml", "Grasas & Aceites")
                        )
                    )
                ),
                MealPlanItem(
                    id = "merienda_endurance",
                    mealType = MealType.MERIENDA,
                    name = "Batido Isotónico Post-Entreno con Ciclodextrina y Whey",
                    description = "Recuperador muscular y de glucógeno ultra-rápido para evitar la fatiga central.",
                    calories = 360,
                    proteinGrams = 30,
                    carbsGrams = 55,
                    fatGrams = 2,
                    recipe = Recipe(
                        title = "Recovery Shake para Deporte de Resistencia",
                        prepTimeMinutes = 3,
                        instructions = listOf(
                            "Mezclar en el shaker 300ml de agua fría o agua de coco.",
                            "Añadir la proteína Whey, la amilopectina/ciclodextrina y una pizca de sal marina.",
                            "Agitar vigorosamente durante 20 segundos y tomar justo al finalizar el entreno."
                        ),
                        baseIngredients = listOf(
                            Ingredient("Ciclodextrina / Amilopectina", 40f, "g", "Suplementos & Carbohidratos"),
                            Ingredient("Proteína Whey Isolate", 30f, "g", "Suplementos & Proteínas"),
                            Ingredient("Agua de Coco o Agua con Electrolitos", 350f, "ml", "Lácteos & Bebidas")
                        )
                    )
                ),
                MealPlanItem(
                    id = "cena_endurance",
                    mealType = MealType.CENA,
                    name = "Lenguado / Lubina al Horno con Boniato / Camote Asado",
                    description = "Cena de fácil digestión para un descanso reparador y repleción completa de glucógeno.",
                    calories = 520,
                    proteinGrams = 42,
                    carbsGrams = 62,
                    fatGrams = 12,
                    recipe = Recipe(
                        title = "Pescado Blanco con Boniato Asado al Limón",
                        prepTimeMinutes = 25,
                        instructions = listOf(
                            "Asar el boniato en láminas delgadas al horno a 200°C durante 20 minutos.",
                            "Añadir el filete de pescado blanco salpimentado con perejil fresco y limón los últimos 10 minutos.",
                            "Servir acompañado de espárragos o ensalada suave."
                        ),
                        baseIngredients = listOf(
                            Ingredient("Filete de Lubina / Lenguado / Merluza", 200f, "g", "Pescados & Mariscos"),
                            Ingredient("Boniato / Camote dulce", 220f, "g", "Frutas & Verduras"),
                            Ingredient("Aceite de Oliva Extra Virgen", 8f, "ml", "Grasas & Aceites"),
                            Ingredient("Limón y Perejil fresco", 20f, "g", "Frutas & Verduras")
                        )
                    )
                )
            )
        }
    }
}
