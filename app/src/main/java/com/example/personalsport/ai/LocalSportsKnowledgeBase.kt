package com.example.personalsport.ai

import com.example.personalsport.ui.equipment.BrandCatalog
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Ritmos de Entrenamiento VDOT según la Fórmula de Carrera de Jack Daniels ([VdotPaces]).
 */
data class VdotPaces(
    val vdotScore: Float,
    val easyPaceMinKm: String,
    val marathonPaceMinKm: String,
    val thresholdPaceMinKm: String,
    val intervalPaceMinKm: String,
    val repetitionPaceMinKm: String,
    val seriesAdviceEs: String
)

/**
 * Métricas de Velocidad Crítica de Natación CSS según la Metodología Swim Smooth ([CssPaceResult]).
 */
data class CssPaceResult(
    val cssPacePer100mSec: Float,
    val cssFormatted: String,
    val endurancePaceFormatted: String,
    val sprintPaceFormatted: String,
    val recommendedDrillsEs: List<String>
)

/**
 * Evaluación de Eficiencia Hidrodinámica SWOLF ([SwolfResult]).
 */
data class SwolfResult(
    val swolfScore: Int,
    val ratingEs: String,
    val recommendationEs: String
)

/**
 * Concepto de Arquetipo y Técnica Swim Smooth ([SwimSmoothConcept]).
 */
data class SwimSmoothConcept(
    val archetypeEs: String,
    val descriptionEs: String,
    val keyFlawsEs: List<String>,
    val correctiveDrillsEs: List<String>
)

/**
 * Grupos musculares principales en español ([MuscleGroupEs]).
 */
enum class MuscleGroupEs(val displayName: String) {
    PECHO("Pecho / Pectoral"),
    ESPALDA("Espalda / Dorsal"),
    HOMBROS("Hombros / Deltoides"),
    BICEPS("Bíceps Braquial"),
    TRICEPS("Tríceps Braquial"),
    CUADRICEPS("Cuádriceps"),
    ISQUIOTIBIALES("Isquiotibiales / Femoral"),
    GLUTEOS("Glúteos"),
    GEMELOS("Gemelos / Sóleo"),
    CORE("Core / Abdomen y Lumbar")
}

/**
 * Concepto de Ejercicio de Fuerza y Maquinaria Guiada ([StrengthExerciseConcept]).
 */
data class StrengthExerciseConcept(
    val nameEs: String,
    val primaryMuscle: MuscleGroupEs,
    val categoryEs: String,
    val descriptionEs: String,
    val rpeTarget: String = "RPE 7-9 (RIR 1-3)",
    val brandCompatibilities: List<String> = emptyList()
)

/**
 * Entrenamiento de Recomposición y Estética para Estrellas de Hollywood ([HollywoodTrainingConcept]).
 */
data class HollywoodTrainingConcept(
    val titleEs: String,
    val focusEs: String,
    val methodologyEs: String,
    val sampleProtocolEs: List<String>,
    val aestheticNotesEs: String
)

/**
 * Progresión de Calistenia y Peso Corporal ([CalisthenicsProgression]).
 */
data class CalisthenicsProgression(
    val exerciseNameEs: String,
    val levelEs: String,
    val descriptionEs: String,
    val progressionStepsEs: List<String>,
    val weightedOptionEs: String
)

/**
 * Receta Deportiva y Ficha Nutricional ([RecipeKnowledgeItem]).
 */
data class RecipeKnowledgeItem(
    val titleEs: String,
    val categoryEs: String,
    val calories: Int,
    val proteinGrams: Int,
    val carbsGrams: Int,
    val fatGrams: Int,
    val satietyIndexScore: Float,
    val ingredientsEs: List<String>,
    val instructionsEs: List<String>,
    val nutritionNotesEs: String
)

/**
 * Fundamentos de Nutrición Deportiva Avanzada ([SportsNutritionEssentialsItem]).
 */
data class SportsNutritionEssentialsItem(
    val bmrFormulaEs: String,
    val tdeeMultipliersEs: Map<String, Float>,
    val carbPeriodizationStrategyEs: String,
    val hydrationElectrolyteGuidanceEs: String,
    val micronutrientOptimizationEs: String
)

/**
 * Métricas biológicas de usuario para enriquecimiento de prompts IA ([BiologicalMetrics]).
 */
data class BiologicalMetrics(
    val age: Int = 28,
    val gender: String = "Masculino",
    val weightKg: Float = 70.0f,
    val bodyFatPercentage: Float = 15.0f,
    val visceralFat: Float = 5.0f,
    val boneMassKg: Float = 3.2f,
    val muscleMassKg: Float = 32.5f
)

/**
 * Categorías de Conocimiento Deportivo en el Motor RAG ([KnowledgeCategory]).
 */
enum class KnowledgeCategory(val displayNameEs: String) {
    RUNNING_DANIELS("Carrera y Atletismo (Daniels & Pfitzinger)"),
    SWIMMING_SWIM_SMOOTH("Natación (Swim Smooth CSS, SWOLF & Arquetipos)"),
    STRENGTH_NIPPARD_BEJARANO("Fuerza y Maquinaria (Starting Strength, Nippard & BejaranoFit)"),
    HOLLYWOOD_STARS("Entrenamiento Estético para Estrellas de Hollywood"),
    CALISTHENICS_GRAVITY("Calistenia y Peso Corporal (Overcoming Gravity)"),
    NUTRITION_FLEXIBLE("Nutrición Deportiva y Recetario (Dieta Flexible & IIFYM)"),
    MACRO_FRIENDLY_COOKBOOK("Recetario Macro Friendly (Alto Volumen & Baja Caloría)"),
    VEGAN_ATHLETE("Recetario Vegano para Atletas de Alto Rendimiento"),
    HIGH_PROTEIN_COOKBOOK("Recetario Alto en Proteína & Síntesis Muscular"),
    SPORTS_NUTRITION_ESSENTIALS("Essentials of Sports Nutrition & Bioenergética")
}

/**
 * Base de Conocimiento RAG de Ciencias del Deporte y Nutrición en Español ([LocalSportsKnowledgeBase]).
 *
 * Enriquecida con principios extraídos de:
 * - Train Like Hollywood Stars
 * - The Macro Friendly Cookbook
 * - Vegan Cookbook for Athletes
 * - The High Protein Cookbook
 * - Swim Smooth
 * - Essentials of Sports Nutrition
 * - Jack Daniels Running Formula, Pfitzinger, Starting Strength, Jeff Nippard, Overcoming Gravity.
 */
object LocalSportsKnowledgeBase {

    const val SPANISH_SYSTEM_DIRECTIVE = """
DIRECTIVA BÁSICA OBLIGATORIA DE IDIOMA:
Todo el contenido generado, nombres de ejercicios, títulos de rutinas, nombres de máquinas, instrucciones de recetas, listas de ingredientes, consejos de descanso y explicaciones DEBEN redactarse Y mostrarse EXCLUSIVAMENTE EN ESPAÑOL, independientemente del idioma del libro o documento fuente. Está estrictamente prohibido emitir textos o términos técnicos en inglés si existe una traducción estándar en español de ciencias del deporte.
"""

    /**
     * Calcula los ritmos de entrenamiento VDOT según la Fórmula de Carrera de Jack Daniels.
     */
    fun calculateVdotPaces(vdotScore: Float): VdotPaces {
        val clampedVdot = vdotScore.coerceIn(30f, 85f)

        val eSec = (510f - (clampedVdot - 30f) * 5.2f).roundToInt().coerceAtLeast(180)
        val mSec = (420f - (clampedVdot - 30f) * 4.5f).roundToInt().coerceAtLeast(150)
        val tSec = (370f - (clampedVdot - 30f) * 4.1f).roundToInt().coerceAtLeast(135)
        val iSec = (330f - (clampedVdot - 30f) * 3.8f).roundToInt().coerceAtLeast(120)
        val rSec = (300f - (clampedVdot - 30f) * 3.5f).roundToInt().coerceAtLeast(105)

        return VdotPaces(
            vdotScore = clampedVdot,
            easyPaceMinKm = formatMinKm(eSec),
            marathonPaceMinKm = formatMinKm(mSec),
            thresholdPaceMinKm = formatMinKm(tSec),
            intervalPaceMinKm = formatMinKm(iSec),
            repetitionPaceMinKm = formatMinKm(rSec),
            seriesAdviceEs = "Fórmula de Jack Daniels: Realiza las Series de Atletismo (400m/1000m) a Ritmo I (${formatMinKm(iSec)}) o Ritmo R (${formatMinKm(rSec)}). Los rodajes de recuperación en Ritmo E (${formatMinKm(eSec)}) garantizan la regeneración mitocondrial sin fatiga acumulada."
        )
    }

    /**
     * Estima el valor VDOT en función del tiempo logrado en una prueba de 5000m.
     */
    fun estimateVdotFrom5kTime(time5kSeconds: Int): Float {
        val minutes = time5kSeconds / 60f
        return when {
            minutes <= 14f -> 75f
            minutes <= 16f -> 65f
            minutes <= 18f -> 57f
            minutes <= 20f -> 50f
            minutes <= 22f -> 44f
            minutes <= 25f -> 38f
            minutes <= 30f -> 32f
            else -> 30f
        }
    }

    /**
     * Calcula el ritmo de Velocidad Crítica de Natación (CSS) en segundos/100m (Swim Smooth).
     */
    fun calculateCssPace(time400Sec: Int, time200Sec: Int): CssPaceResult {
        val valid400 = time400Sec.coerceAtLeast(200)
        val valid200 = time200Sec.coerceIn(80, valid400 - 10)

        val cssSecPer100m = (400f - 200f) / (valid400 - valid200) * 100f
        val cssClamped = cssSecPer100m.coerceIn(60f, 240f)

        val endSec = (cssClamped + 2f).roundToInt()
        val sprSec = (cssClamped - 2f).roundToInt().coerceAtLeast(45)

        val drills = listOf(
            "Drill de Punto Muerto con Tubo Frontal (Alineación y agarre de codo alto)",
            "Remada de Tracción Superior e Inferior (Sensibilidad de agua en fase de agarre)",
            "Series CSS de Control de Ritmo (10 x 100m a Ritmo CSS estricto con 15\" de descanso)",
            "Series Largas de Capacidad Aeróbica (4 x 300m en Z1-Z2 con Pull-Buoy)"
        )

        return CssPaceResult(
            cssPacePer100mSec = cssClamped,
            cssFormatted = formatSwimPace(cssClamped.roundToInt()),
            endurancePaceFormatted = formatSwimPace(endSec),
            sprintPaceFormatted = formatSwimPace(sprSec),
            recommendedDrillsEs = drills
        )
    }

    /**
     * Calcula el índice de eficiencia de natación SWOLF (Brazadas + Tiempo en segundos).
     */
    fun calculateSwolfScore(strokesInPool: Int, timeInPoolSec: Int): SwolfResult {
        val score = strokesInPool + timeInPoolSec
        val (rating, rec) = when {
            score < 35 -> Pair("Eficacia Élite / Excelente", "Excelente deslizamiento y cadencia eficiente. Mantén la amplitud de brazada en series largas CSS.")
            score in 35..45 -> Pair("Buena Eficiencia", "Buen balance entre frecuencia y distancia por brazada. Trabaja la fase de empuje final.")
            score in 46..55 -> Pair("Eficiencia Media", "Ligera pérdida de tracción. Realiza ejercicios de codo alto y rotación de cadera (rollido).")
            else -> Pair("Necesita Mejora de Técnica", "Fricción alta en el agua. Incorpora drills de punto muerto y reduce la prisa de brazada.")
        }
        return SwolfResult(score, rating, rec)
    }

    /**
     * Devuelve los arquetipos de natación y corrección técnica de Swim Smooth.
     */
    fun getSwimSmoothArchetypesAndDrills(): List<SwimSmoothConcept> {
        return listOf(
            SwimSmoothConcept(
                archetypeEs = "Overglider (Súper Deslizador)",
                descriptionEs = "Pausa excesiva al frente en la fase de entrada, provocando pérdida de inercia y hundimiento de piernas.",
                keyFlawsEs = listOf("Pausa excesiva en extensión", "Pérdida de cadencia", "Caída de cadera y piernas"),
                correctiveDrillsEs = listOf("Drill de punto muerto corto con aletas", "Nado con metrónomo a 60-65 brazadas/min", "Eliminación de la pausa en la entrada")
            ),
            SwimSmoothConcept(
                archetypeEs = "Arnés / Bamboleador (Swinger / Arched)",
                descriptionEs = "Cruza la línea media al entrar la mano y serpentea el tronco en el agua con excesivo balanceo.",
                keyFlawsEs = listOf("Cruce de la línea central", "Falta de rotación axial homogénea", "Brazada en abanico"),
                correctiveDrillsEs = listOf("Nado con tubo frontal marcando carriles anchos", "Drill de tracción recta con palas pequeñas", "Patada lateral con rollo de 45°")
            ),
            SwimSmoothConcept(
                archetypeEs = "Smooth (Nadador Eficiente / Élite)",
                descriptionEs = "Tracción fluida, agarre de codo alto en fase subacuática, rollo ritmado y sincronización perfecta.",
                keyFlawsEs = emptyList(),
                correctiveDrillsEs = listOf("Series CSS de ritmo sostenido (10x100m)", "Piramidales Z2-Z3", "Entrenamiento en aguas abiertas con avistamiento")
            )
        )
    }

    /**
     * Devuelve el catálogo de entrenamientos y técnicas de Train Like Hollywood Stars.
     */
    fun getHollywoodStarsTrainings(): List<HollywoodTrainingConcept> {
        return listOf(
            HollywoodTrainingConcept(
                titleEs = "Transformación V-Taper de Alta Densidad (Superhéroe Hollywood)",
                focusEs = "Simetría V-Taper (Hombros anchos, dorsal amplio y cintura estrecha)",
                methodologyEs = "Superconjuntos antagonistas de alta densidad y descansos incompletos de 45 a 60 segundos.",
                sampleProtocolEs = listOf(
                    "Superconjunto A: Press de Hombros con Mancuernas (4x10) + Jalón al Pecho Agarre Ancho (4x10)",
                    "Superconjunto B: Elevaciones Laterales en Polea (4x15) + Remo Horizontal Gironda (4x12)",
                    "Finisher Estético: Paseo del Granjero con Mancuernas Pesadas (3 series de 40m)"
                ),
                aestheticNotesEs = "Diseñado para reconfigurar el perfil corporal antes de rodajes o competiciones, maximizando la congestión e hipertrofia del tren superior."
            ),
            HollywoodTrainingConcept(
                titleEs = "Periodización de Recomposición Rápida en 6 Semanas",
                focusEs = "Pérdida de grasa visceral preservando 100% de la masa muscular magra",
                methodologyEs = "Combina fuerza pesada (3-5 reps) al inicio del bloque con circuitos metabólicos HIIT con peso corporal.",
                sampleProtocolEs = listOf(
                    "Bloque 1 Fuerza: Peso Muerto Olímpico / Sentadilla Trasera (4x5 a RPE 8)",
                    "Bloque 2 HIIT Metabólico: 5 Rondas de (10 Kettler Swings + 10 Flexiones + 30s Esprint en Cinta)"
                ),
                aestheticNotesEs = "Crea un déficit calórico metabólico masivo post-entrenamiento (efecto EPOC)."
            )
        )
    }

    /**
     * Devuelve el catálogo de ejercicios de fuerza y máquinas guiadas.
     */
    fun getStrengthExercisesCatalog(): List<StrengthExerciseConcept> {
        val allBrands = BrandCatalog.supportedBrands

        return listOf(
            StrengthExerciseConcept(
                nameEs = "Press de Banca Horizontal con Barra Olímpica",
                primaryMuscle = MuscleGroupEs.PECHO,
                categoryEs = "Fuerza Básica / Barra Libre",
                descriptionEs = "Ejercicio básico multiarticular para pectoral mayor, deltoides anterior y tríceps. Enfócate en retracción escapular.",
                rpeTarget = "RPE 8 (RIR 2)",
                brandCompatibilities = listOf("Fit4Life", "Oss Fitness", "Salter", "Decathlon")
            ),
            StrengthExerciseConcept(
                nameEs = "Press de Pecho Guiado en Máquina",
                primaryMuscle = MuscleGroupEs.PECHO,
                categoryEs = "Máquinas / Hipertrofia Guiada",
                descriptionEs = "Aislamiento seguro del pectoral con máxima tensión constante sin fatiga de personal de apoyo.",
                rpeTarget = "RPE 8-9 (RIR 1-2)",
                brandCompatibilities = listOf("Technogym", "Life Fitness", "Matrix", "BH Fitness", "Bodytone", "Salter", "Etenon")
            ),
            StrengthExerciseConcept(
                nameEs = "Sentadilla con Barra Trasera Olímpica",
                primaryMuscle = MuscleGroupEs.CUADRICEPS,
                categoryEs = "Fuerza Básica / Barra Libre",
                descriptionEs = "Rey de los ejercicios de tren inferior. Rompe el paralelo de 90° con espalda neutra y empuje de talones.",
                rpeTarget = "RPE 8 (RIR 2)",
                brandCompatibilities = listOf("Fit4Life", "Oss Fitness", "Decathlon")
            ),
            StrengthExerciseConcept(
                nameEs = "Prensa de Piernas 45°",
                primaryMuscle = MuscleGroupEs.CUADRICEPS,
                categoryEs = "Máquinas / Hipertrofia Pierna",
                descriptionEs = "Carga masiva para cuádriceps y glúteos eliminando el estrés axial en la columna vertebral.",
                rpeTarget = "RPE 8-9 (RIR 1)",
                brandCompatibilities = listOf("Technogym", "Life Fitness", "Matrix", "Oss Fitness", "Bodytone", "Etenon")
            ),
            StrengthExerciseConcept(
                nameEs = "Extensión de Cuádriceps en Máquina",
                primaryMuscle = MuscleGroupEs.CUADRICEPS,
                categoryEs = "Máquinas / Aislamiento",
                descriptionEs = "Aislamiento directo del recto femoral con pausa isométrica de 1 segundo en máxima extensión.",
                rpeTarget = "RPE 9-10 (RIR 0-1)",
                brandCompatibilities = allBrands
            ),
            StrengthExerciseConcept(
                nameEs = "Remo Horizontal con Barra / Máquina de Remo",
                primaryMuscle = MuscleGroupEs.ESPALDA,
                categoryEs = "Fuerza Básica / Tracción",
                descriptionEs = "Desarrollo de grosor de espalda, dorsal ancho, romboides y trapecio medio.",
                rpeTarget = "RPE 8 (RIR 2)",
                brandCompatibilities = allBrands
            ),
            StrengthExerciseConcept(
                nameEs = "Jalón al Pecho en Polea Alta / Lat Machine",
                primaryMuscle = MuscleGroupEs.ESPALDA,
                categoryEs = "Poleas / Anchura de Espalda",
                descriptionEs = "Tracción vertical clave para anchura dorsal. Dirige la barra hacia la clavícula con el pecho alto.",
                rpeTarget = "RPE 8-9 (RIR 1-2)",
                brandCompatibilities = listOf("Technogym", "Life Fitness", "Matrix", "Bodytone", "Fit4Life", "Cecotec")
            ),
            StrengthExerciseConcept(
                nameEs = "Press Militar de Hombros",
                primaryMuscle = MuscleGroupEs.HOMBROS,
                categoryEs = "Fuerza Básica / Hombros",
                descriptionEs = "Empuje vertical para deltoides anterior y fuerza de core. Mantén el abdomen bloqueado.",
                rpeTarget = "RPE 8 (RIR 2)",
                brandCompatibilities = allBrands
            ),
            StrengthExerciseConcept(
                nameEs = "Cruces de Poleas para Pectoral / Dual Pulley",
                primaryMuscle = MuscleGroupEs.PECHO,
                categoryEs = "Poleas / Aislamiento",
                descriptionEs = "Tensión continua en todo el rango de movimiento enfatizando la línea media del pecho.",
                rpeTarget = "RPE 9-10 (RIR 0-1)",
                brandCompatibilities = listOf("Technogym", "Life Fitness", "Fit4Life", "Bodytone")
            )
        )
    }

    /**
     * Devuelve las progresiones completas de calistenia y peso corporal.
     */
    fun getCalisthenicsProgressions(): List<CalisthenicsProgression> {
        return listOf(
            CalisthenicsProgression(
                exerciseNameEs = "Dominadas (Pull-Ups)",
                levelEs = "Progresión Completa (Básica a Avanzada)",
                descriptionEs = "Patrón fundamental de tracción corporal superior según Overcoming Gravity.",
                progressionStepsEs = listOf(
                    "Nivel 1: Remo Australiano en Barra Baja (3x12)",
                    "Nivel 2: Dominadas Escapulares & Isométricas (3x10s arriba)",
                    "Nivel 3: Dominadas Negativas Lentas (4x5 reps bajando en 5s)",
                    "Nivel 4: Dominadas Libres Estrictas Pronas (4x10-15)",
                    "Nivel 5: Dominadas Lastradas con Disco / Cinturón (+10kg a +25kg)",
                    "Nivel 6: Muscle-Up en Barra / Anillas"
                ),
                weightedOptionEs = "Agrega lastre con cinturón o chaleco lastrado a partir de poder realizar 12 repeticiones limpias sin balanceo."
            ),
            CalisthenicsProgression(
                exerciseNameEs = "Flexiones de Pecho (Push-Ups)",
                levelEs = "Progresión Completa",
                descriptionEs = "Patrón fundamental de empuje horizontal para pectoral, deltoides anterior y tríceps.",
                progressionStepsEs = listOf(
                    "Nivel 1: Flexiones Inclinadas sobre Banco (3x15)",
                    "Nivel 2: Flexiones Estándar en Suelo con alineación estricta (4x15)",
                    "Nivel 3: Flexiones Declinadas con Pies Elevados (4x12)",
                    "Nivel 4: Flexiones en Diamante / Arqueras (4x10)",
                    "Nivel 5: Flexiones Explosivas con Palmada / Pseudo Planche Push-Ups"
                ),
                weightedOptionEs = "Coloca chaleco o disco en la espalda alta para sobrecarga progresiva."
            ),
            CalisthenicsProgression(
                exerciseNameEs = "Fondos en Paralelas (Dips)",
                levelEs = "Progresión Intermedia / Avanzada",
                descriptionEs = "Empuje vertical potente enfocado en pectoral inferior y tríceps.",
                progressionStepsEs = listOf(
                    "Nivel 1: Fondos en Banco con Pies Apoyados (3x15)",
                    "Nivel 2: Fondos asistidos con banda elástica (3x10)",
                    "Nivel 3: Fondos Libres en Paralelas a 90° (4x12)",
                    "Nivel 4: Fondos Lastrados (+10kg a +20kg)"
                ),
                weightedOptionEs = "Utiliza cinturón de lastre para incremento progresivo de carga."
            )
        )
    }

    /**
     * Recetario General de la Base de Conocimiento.
     */
    fun getCookbookRecipesCatalog(): List<RecipeKnowledgeItem> {
        return getHighProteinRecipesCatalog() + getMacroFriendlyRecipesCatalog() + getVeganAthleteRecipesCatalog()
    }

    /**
     * Recetario de "The Macro Friendly Cookbook" (Alto volumen / baja densidad calórica).
     */
    fun getMacroFriendlyRecipesCatalog(): List<RecipeKnowledgeItem> {
        return listOf(
            RecipeKnowledgeItem(
                titleEs = "Pizza Proteica Volumétrica de Calabacín y Avena",
                categoryEs = "Recetas Macro Friendly",
                calories = 380,
                proteinGrams = 42,
                carbsGrams = 32,
                fatGrams = 9,
                satietyIndexScore = 9.6f,
                ingredientsEs = listOf("200g Calabacín rallado", "40g Harina de avena integral", "150g Claras de huevo", "60g Queso mozzarella light 10%", "50g Tomate tamizado sin azúcar", "30g Pechuga de pavo picada"),
                instructionsEs = listOf("Mezclar calabacín, avena y claras.", "Extender la masa en bandeja de horno a 200°C durante 15m.", "Añadir tomate, mozzarella y pavo.", "Gratinar 5 minutos."),
                nutritionNotesEs = "Estrategia del libro The Macro Friendly Cookbook: Alto volumen de fibra y agua para maximizar saciedad gastando pocas calorías."
            ),
            RecipeKnowledgeItem(
                titleEs = "Helado Proteico Volumétrico de Fresas y Goma Xanthan",
                categoryEs = "Postres Macro Friendly",
                calories = 190,
                proteinGrams = 30,
                carbsGrams = 15,
                fatGrams = 1.5f.toInt(),
                satietyIndexScore = 9.4f,
                ingredientsEs = listOf("250g Fresas congeladas", "30g Proteína de suero (Whey)", "100ml Leche de almendras sin azúcar", "2g Goma xanthan (Espesante)", "Edulcorante líquido"),
                instructionsEs = listOf("Procesar fresas y leche en batidora de vaso.", "Añadir la proteína y la goma xanthan.", "Batir a máxima velocidad 3 minutos hasta triplicar su volumen."),
                nutritionNotesEs = "Atrapa aire generando una textura de mousse helada gigante ultra-saciante."
            )
        )
    }

    /**
     * Recetario de "Vegan Cookbook for Athletes" (Proteína vegetal completa & biodisponibilidad).
     */
    fun getVeganAthleteRecipesCatalog(): List<RecipeKnowledgeItem> {
        return listOf(
            RecipeKnowledgeItem(
                titleEs = "Bowl de Tempeh Salteado, Edamame y Arroz Salvaje",
                categoryEs = "Nutrición Vegana de Rendimiento",
                calories = 540,
                proteinGrams = 38,
                carbsGrams = 58,
                fatGrams = 16,
                satietyIndexScore = 9.3f,
                ingredientsEs = listOf("150g Tempeh orgánico de soja", "100g Edamame cocido", "80g Arroz salvaje y quinoa (peso cocido)", "100g Brócoli al vapor", "15ml Salsa de soja baja en sodio", "10g Levadura nutricional"),
                instructionsEs = listOf("Dorar el tempeh en dados en sartén con salsa de soja.", "Cocer el edamame y arroz salvaje.", "Montar el bowl y espolvorear levadura nutricional rica en vitamina B12."),
                nutritionNotesEs = "Combina fuentes completas de lisina y metionina para alcanzar el perfil de aminoácidos del suero lácteo en atletas veganos."
            ),
            RecipeKnowledgeItem(
                titleEs = "Hamburguesa Proteica de Lentejas, Seitán y Semillas de Cáñamo",
                categoryEs = "Cenas Veganas Proteicas",
                calories = 460,
                proteinGrams = 40,
                carbsGrams = 42,
                fatGrams = 12,
                satietyIndexScore = 9.1f,
                ingredientsEs = listOf("120g Lentejas cocidas", "80g Seitán picado", "20g Semillas de cáñamo peladas", "30g Copos de avena integral", "Ajo, comino y pimentón ahumado"),
                instructionsEs = listOf("Triturar las lentejas con el seitán y las especias.", "Añadir avena y semillas de cáñamo hasta compactar.", "Formar hamburguesas y dorar a la plancha."),
                nutritionNotesEs = "Aporte masivo de hierro no hemo acompañado de vitamina C para optimizar su absorción."
            )
        )
    }

    /**
     * Recetario de "The High Protein Cookbook" (30g-50g Proteína por plato & activación mTOR).
     */
    fun getHighProteinRecipesCatalog(): List<RecipeKnowledgeItem> {
        return listOf(
            RecipeKnowledgeItem(
                titleEs = "Tortilla Proteica con Pavo, Espinacas y Aguacate",
                categoryEs = "Desayunos proteicos",
                calories = 420,
                proteinGrams = 40,
                carbsGrams = 18,
                fatGrams = 16,
                satietyIndexScore = 9.2f,
                ingredientsEs = listOf("200ml Claras de huevo", "1 Huevo entero", "70g Pechuga de pavo", "60g Espinacas frescas", "40g Aguacate", "5ml Aceite de oliva"),
                instructionsEs = listOf("Batir claras y huevo.", "Saltear espinacas y pavo.", "Cuajar la tortilla en sartén antiadherente.", "Servir con aguacate fresco."),
                nutritionNotesEs = "Rico en leucina para activar la síntesis proteica matutina e hierro."
            ),
            RecipeKnowledgeItem(
                titleEs = "Pechuga de Pollo con Quinoa Real y Brócoli al Vapor",
                categoryEs = "Almuerzos de rendimiento",
                calories = 620,
                proteinGrams = 52,
                carbsGrams = 65,
                fatGrams = 14,
                satietyIndexScore = 9.5f,
                ingredientsEs = listOf("200g Pechuga de pollo", "70g Quinoa real (peso seco)", "150g Brócoli fresco", "10ml Aceite de oliva extra virgen"),
                instructionsEs = listOf("Cocer quinoa 12 minutos.", "Cocinar brócoli al vapor.", "Marcar pollo a la plancha con especias.", "Aliñar con AOVE."),
                nutritionNotesEs = "Carbohidratos de bajo índice glucémico y alto volumen saciante."
            ),
            RecipeKnowledgeItem(
                titleEs = "Lomo de Salmón al Horno con Espárragos Trigueros",
                categoryEs = "Cenas de recuperación",
                calories = 460,
                proteinGrams = 42,
                carbsGrams = 12,
                fatGrams = 24,
                satietyIndexScore = 9.1f,
                ingredientsEs = listOf("180g Lomo de salmón fresco", "120g Espárragos trigueros", "50g Canónigos", "Zumo de limón y ajo"),
                instructionsEs = listOf("Hornear salmón y espárragos a 190°C durante 12-14 minutos.", "Sazonar con sal marina, pimienta y limón."),
                nutritionNotesEs = "Aporte masivo de ácidos grasos Omega-3 (EPA/DHA) que favorecen la recuperación nocturna."
            ),
            RecipeKnowledgeItem(
                titleEs = "Mousse Proteica de Cacao y Caseína Micelar",
                categoryEs = "Postres fitness",
                calories = 210,
                proteinGrams = 32,
                carbsGrams = 10,
                fatGrams = 3,
                satietyIndexScore = 8.8f,
                ingredientsEs = listOf("30g Caseína micelar sabor chocolate", "150g Queso fresco batido 0%", "10g Cacao puro en polvo desgrasado", "Edulcorante al gusto"),
                instructionsEs = listOf("Mezclar la caseína con el queso batido y cacao.", "Batir enérgicamente con varilla hasta obtener textura Mousse.", "Refrigerar 20 minutos."),
                nutritionNotesEs = "Proteína de digestión ultra-lenta para prevenir el catabolismo durante el sueño."
            )
        )
    }

    /**
     * Devuelve las directrices de "Essentials of Sports Nutrition".
     */
    fun getSportsNutritionEssentials(): SportsNutritionEssentialsItem {
        return SportsNutritionEssentialsItem(
            bmrFormulaEs = "Ecuación de Katch-McArdle (basada en masa magra): BMR = 370 + (21.6 * MasaMagraKg). Más exacta que Harris-Benedict para atletas.",
            tdeeMultipliersEs = mapOf(
                "Sedentario" to 1.2f,
                "Ligero (1-3 días/semana)" to 1.375f,
                "Moderado (3-5 días/semana)" to 1.55f,
                "Intenso (6-7 días/semana)" to 1.725f,
                "Élite / Doble sesión" to 1.9f
            ),
            carbPeriodizationStrategyEs = "Periodización de Carbohidratos (Carb Cycling): Días de fuerza alta/HIIT (4-6g carb/kg) vs Días de descanso o cardio suave (1.5-2.5g carb/kg).",
            hydrationElectrolyteGuidanceEs = "Pérdida hídrica: Reponer 1.25 a 1.5 Litros de agua por cada kg de peso perdido durante el entrenamiento, con 500-700mg de Sodio por Litro.",
            micronutrientOptimizationEs = "Atención prioritaria a Magnesio (350-400mg/día), Vitamina D3 (2000-4000 UI/día) y Zinc para el entorno hormonal y función muscular."
        )
    }

    /**
     * Genera el bloque de contexto con la ficha biológica del usuario para inyectar en las consultas de Qwen IA.
     */
    fun buildBiologicalPromptContext(
        age: Int,
        gender: String,
        weightKg: Float,
        bodyFatPercentage: Float,
        visceralFat: Float,
        boneMassKg: Float,
        muscleMassKg: Float
    ): String {
        val leanMassKg = (weightKg * (1f - (bodyFatPercentage / 100f))).coerceAtLeast(20f)
        val estimatedBmr = (370f + (21.6f * leanMassKg)).roundToInt()

        val proteinLowerGrams = (weightKg * 1.8f).roundToInt()
        val proteinUpperGrams = (weightKg * 2.4f).roundToInt()

        return buildString {
            append("=== PARÁMETROS BIOLÓGICOS DEL ATLETA (TRANSFERENCIA QWEN IA) ===\n")
            append("• Edad: ").append(age).append(" años\n")
            append("• Sexo / Género: ").append(gender).append("\n")
            append("• Peso Total: ").append(String.format(Locale.US, "%.1f", weightKg)).append(" kg\n")
            append("• % Grasa Corporal: ").append(String.format(Locale.US, "%.1f", bodyFatPercentage)).append(" %\n")
            append("• Grasa Visceral: Nivel ").append(String.format(Locale.US, "%.1f", visceralFat)).append("\n")
            append("• Masa Ósea: ").append(String.format(Locale.US, "%.1f", boneMassKg)).append(" kg\n")
            append("• Masa Muscular: ").append(String.format(Locale.US, "%.1f", muscleMassKg)).append(" kg\n")
            append("• Masa Magra Estimada: ").append(String.format(Locale.US, "%.1f", leanMassKg)).append(" kg\n")
            append("• Tasa Metabólica Basal BMR (Katch-McArdle): ~").append(estimatedBmr).append(" kcal/día\n")
            append("• Rango Óptimo Proteico Diario: ").append(proteinLowerGrams).append("g - ").append(proteinUpperGrams).append("g de Proteína\n")
            append("• Directiva Fisiológica: Optimizar recomposición corporal, preservar masa muscular magra y mantener grasa visceral en niveles saludables (<= 9).\n")
        }
    }

    /**
     * Construye el contexto de conocimiento estructurado en español para consultas RAG del LLM,
     * incorporando la ficha biológica del atleta cuando está disponible.
     */
    fun getKnowledgeContext(query: String, userMetrics: BiologicalMetrics? = null): String {
        val q = query.lowercase(Locale.ROOT)
        val sb = StringBuilder()
        sb.append(SPANISH_SYSTEM_DIRECTIVE).append("\n\n")

        if (userMetrics != null) {
            sb.append(buildBiologicalPromptContext(
                age = userMetrics.age,
                gender = userMetrics.gender,
                weightKg = userMetrics.weightKg,
                bodyFatPercentage = userMetrics.bodyFatPercentage,
                visceralFat = userMetrics.visceralFat,
                boneMassKg = userMetrics.boneMassKg,
                muscleMassKg = userMetrics.muscleMassKg
            )).append("\n\n")
        }

        when {
            q.contains("carrera") || q.contains("atletismo") || q.contains("vdot") || q.contains("daniels") || q.contains("pfitzinger") -> {
                val paces = calculateVdotPaces(48f)
                sb.append("=== BASE DE CONOCIMIENTO: CARRERA Y ATLETISMO (DANIELS & PFITZINGER) ===\n")
                sb.append("Fórmulas VDOT y Ritmos de Entrenamiento (VDOT 48 de referencia):\n")
                sb.append("- Ritmo E (Rodaje Suave): ").append(paces.easyPaceMinKm).append("\n")
                sb.append("- Ritmo M (Maratón): ").append(paces.marathonPaceMinKm).append("\n")
                sb.append("- Ritmo T (Umbral Lactato): ").append(paces.thresholdPaceMinKm).append("\n")
                sb.append("- Ritmo I (Intervalos VO2máx): ").append(paces.intervalPaceMinKm).append("\n")
                sb.append("- Ritmo R (Series Cortas): ").append(paces.repetitionPaceMinKm).append("\n")
                sb.append("Principios clave: Series de Atletismo en Pista de 400m, Fartlek estructurado y Cuestas para potencia.\n")
            }

            q.contains("natacion") || q.contains("swim") || q.contains("css") || q.contains("swolf") -> {
                val css = calculateCssPace(420, 200)
                sb.append("=== BASE DE CONOCIMIENTO: NATACIÓN (SWIM SMOOTH) ===\n")
                sb.append("Fórmula CSS (Critical Swim Speed): ").append(css.cssFormatted).append("\n")
                sb.append("Ritmos de Serie CSS: Endurance (").append(css.endurancePaceFormatted).append(") | Sprint (").append(css.sprintPaceFormatted).append(")\n")
                sb.append("Arquetipos Swim Smooth: Overglider, Bamboleador/Arnés, Smooth.\n")
                sb.append("Progreso de Drills: ").append(css.recommendedDrillsEs.joinToString("; ")).append("\n")
            }

            q.contains("fuerza") || q.contains("gimnasio") || q.contains("ppl") || q.contains("nippard") || q.contains("starting strength") -> {
                sb.append("=== BASE DE CONOCIMIENTO: FUERZA Y MAQUINARIA (NIPPARD, BEJARANOFIT & STARTING STRENGTH) ===\n")
                sb.append("Principios: Sobrecarga progresiva, volumen de 10-20 series/músculo/semana, RPE 7-9, splits Push/Pull/Legs (PPL).\n")
                val concepts = getStrengthExercisesCatalog()
                concepts.forEach {
                    sb.append("• ").append(it.nameEs).append(" [").append(it.primaryMuscle.displayName).append("]: ").append(it.descriptionEs).append("\n")
                }
            }

            q.contains("hollywood") || q.contains("estrellas") || q.contains("camara") || q.contains("estetica") -> {
                sb.append("=== BASE DE CONOCIMIENTO: TRAIN LIKE HOLLYWOOD STARS ===\n")
                val hollywood = getHollywoodStarsTrainings()
                hollywood.forEach {
                    sb.append("• ").append(it.titleEs).append(" | Enfoque: ").append(it.focusEs).append(" | ").append(it.aestheticNotesEs).append("\n")
                }
            }

            q.contains("calistenia") || q.contains("overcoming gravity") || q.contains("dominadas") -> {
                sb.append("=== BASE DE CONOCIMIENTO: CALISTENIA (OVERCOMING GRAVITY) ===\n")
                val progressions = getCalisthenicsProgressions()
                progressions.forEach {
                    sb.append("• ").append(it.exerciseNameEs).append(": ").append(it.progressionStepsEs.joinToString(" ➔ ")).append("\n")
                }
            }

            q.contains("macro") || q.contains("volumen") || q.contains("saciedad") -> {
                sb.append("=== BASE DE CONOCIMIENTO: THE MACRO FRIENDLY COOKBOOK ===\n")
                val macroRecipes = getMacroFriendlyRecipesCatalog()
                macroRecipes.forEach {
                    sb.append("• ").append(it.titleEs).append(": ").append(it.calories).append(" kcal | Proteína ").append(it.proteinGrams).append("g | ").append(it.nutritionNotesEs).append("\n")
                }
            }

            q.contains("vegan") || q.contains("vegano") || q.contains("planta") -> {
                sb.append("=== BASE DE CONOCIMIENTO: VEGAN COOKBOOK FOR ATHLETES ===\n")
                val veganRecipes = getVeganAthleteRecipesCatalog()
                veganRecipes.forEach {
                    sb.append("• ").append(it.titleEs).append(": ").append(it.calories).append(" kcal | Proteína ").append(it.proteinGrams).append("g | ").append(it.nutritionNotesEs).append("\n")
                }
            }

            q.contains("nutricion") || q.contains("dieta") || q.contains("receta") || q.contains("iifym") || q.contains("proteina") -> {
                sb.append("=== BASE DE CONOCIMIENTO: NUTRICIÓN DEPORTIVA Y RECETARIO (HIGH PROTEIN & ESSENTIALS OF SPORTS NUTRITION) ===\n")
                val essentials = getSportsNutritionEssentials()
                sb.append("• BMR: ").append(essentials.bmrFormulaEs).append("\n")
                sb.append("• Carbohidratos: ").append(essentials.carbPeriodizationStrategyEs).append("\n")
                val recipes = getHighProteinRecipesCatalog()
                recipes.forEach {
                    sb.append("• ").append(it.titleEs).append(" (").append(it.categoryEs).append("): ").append(it.calories).append(" kcal | P:").append(it.proteinGrams).append("g C:").append(it.carbsGrams).append("g G:").append(it.fatGrams).append("g\n")
                }
            }

            else -> {
                sb.append("=== COMPREHENSIVE SPANISH SPORTS & NUTRITION KNOWLEDGE BASE ===\n")
                sb.append("Integrando principios de Train Like Hollywood Stars, The Macro Friendly Cookbook, Vegan Cookbook for Athletes, The High Protein Cookbook, Swim Smooth, Essentials of Sports Nutrition, Jack Daniels, Pfitzinger, Starting Strength, Jeff Nippard, BejaranoFit y Overcoming Gravity.\n")
                sb.append("Todos los componentes, ejercicios, máquinas y nutrición están formateados estrictamente en Español.\n")
            }
        }

        return sb.toString()
    }

    private fun formatMinKm(totalSec: Int): String {
        val m = totalSec / 60
        val s = totalSec % 60
        return String.format(Locale.US, "%d:%02d /km", m, s)
    }

    private fun formatSwimPace(totalSec: Int): String {
        val m = totalSec / 60
        val s = totalSec % 60
        return String.format(Locale.US, "%d:%02d /100m", m, s)
    }
}
