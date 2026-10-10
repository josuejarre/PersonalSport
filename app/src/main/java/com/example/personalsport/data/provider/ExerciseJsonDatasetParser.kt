package com.example.personalsport.data.provider

import android.content.Context
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.io.InputStream
import java.text.Normalizer

data class ExerciseJsonItem(
    val id: String = "",
    val name: String = "",
    val gif_url: String = "",
    val instruction_steps: List<String> = emptyList(),
    val category: String = "",
    val target: String = "",
    val equipment: String = ""
)

object ExerciseJsonDatasetParser {

    const val BASE_GITHUB_RAW_URL = "https://raw.githubusercontent.com/hasaneyldrm/exercises-dataset/main/"
    const val FALLBACK_GIF_URL = "file:///android_asset/fallback_exercise.gif"

    private var cachedItems: List<ExerciseJsonItem>? = null

    /**
     * Map of common Spanish / English aliases to canonical exercise names in exercise.json.
     * All keys are normalized using normalizeText to guarantee consistent matching.
     */
    private val nameMappingRaw: Map<String, String> = mapOf(
        // Cuádriceps / Leg Extension
        "extension de cuadriceps" to "Leg Extension",
        "extensiones de cuadriceps" to "Leg Extension",
        "cuadriceps en maquina" to "Leg Extension",
        "extension de cuadriceps en maquina" to "Leg Extension",
        "cuadriceps" to "Leg Extension",
        "leg extension" to "Leg Extension",
        "lever leg extension" to "Leg Extension",

        // Prensa de Piernas / Leg Press
        "prensa de piernas" to "Leg Press",
        "prensa de piernas inclinada 45" to "Leg Press",
        "prensa 45" to "Leg Press",
        "prensa" to "Leg Press",
        "leg press" to "Leg Press",

        // Sentadilla / Squat
        "sentadilla" to "Barbell Squat",
        "sentadillas" to "Barbell Squat",
        "sentadilla con barra" to "Barbell Squat",
        "sentadillas con barra olimpica" to "Barbell Squat",
        "barbell squat" to "Barbell Squat",
        "squat" to "Barbell Squat",

        // Press de Banca / Bench Press
        "press de banca" to "Barbell Bench Press",
        "press de banca horizontal" to "Barbell Bench Press",
        "press de banca horizontal con barra olimpica" to "Barbell Bench Press",
        "press banca" to "Barbell Bench Press",
        "press plano" to "Barbell Bench Press",
        "barbell bench press" to "Barbell Bench Press",
        "bench press" to "Barbell Bench Press",

        // Press Inclinado / Incline Bench Press
        "press de banca inclinado" to "Incline Barbell Bench Press",
        "press inclinado con barra" to "Incline Barbell Bench Press",
        "press inclinado" to "Incline Barbell Bench Press",
        "incline barbell bench press" to "Incline Barbell Bench Press",
        "incline bench press" to "Incline Barbell Bench Press",
        "press inclinado con mancuernas" to "Incline Dumbbell Press",
        "incline dumbbell press" to "Incline Dumbbell Press",

        // Dominadas / Pull-up
        "dominadas" to "Pull-up",
        "dominada" to "Pull-up",
        "dominadas en barra" to "Pull-up",
        "dominadas con lastre strictly" to "Pull-up",
        "dominadas libres sin lastre" to "Pull-up",
        "dominadas libres estrictas" to "Pull-up",
        "pull-up" to "Pull-up",
        "pull up" to "Pull-up",
        "pullup" to "Pull-up",

        // Jalón al Pecho / Lat Pulldown
        "jalon al pecho" to "Lat Pulldown",
        "jalón al pecho" to "Lat Pulldown",
        "jalon al pecho en polea alta" to "Lat Pulldown",
        "jalon al pecho en polea alta lat machine" to "Lat Pulldown",
        "lat pulldown" to "Lat Pulldown",
        "cable lat pulldown" to "Lat Pulldown",

        // Remo con Barra / Row
        "remo con barra" to "Barbell Row",
        "remo con barra maquina" to "Barbell Row",
        "remo horizontal con barra" to "Barbell Row",
        "barbell row" to "Barbell Row",
        "remo con mancuerna" to "Dumbbell Row",
        "dumbbell row" to "Dumbbell Row",

        // Press Militar / Overhead Press
        "press militar" to "Overhead Press",
        "press militar con barra mancuernas" to "Overhead Press",
        "overhead press" to "Overhead Press",
        "shoulder press" to "Overhead Press",

        // Curl de Bíceps / Bicep Curl
        "curl de biceps" to "Dumbbell Curl",
        "curl de bíceps" to "Dumbbell Curl",
        "curl de biceps con barra mancuernas" to "Dumbbell Curl",
        "dumbbell curl" to "Dumbbell Curl",
        "bicep curl" to "Dumbbell Curl",
        "curl con barra" to "Barbell Curl",
        "barbell curl" to "Barbell Curl",

        // Extensiones de Tríceps / Pushdown
        "extensiones de triceps" to "Cable Triceps Pushdown",
        "extensiones de tríceps" to "Cable Triceps Pushdown",
        "extensiones de triceps en polea" to "Cable Triceps Pushdown",
        "cable triceps pushdown" to "Cable Triceps Pushdown",
        "triceps pushdown" to "Cable Triceps Pushdown",

        // Fondos en Paralelas / Dips
        "fondos en paralelas" to "Dips",
        "fondos triceps en paralelas" to "Dips",
        "fondos en banco paralelas" to "Dips",
        "fondos" to "Dips",
        "dips" to "Dips",
        "triceps dips" to "Dips",

        // Flexiones / Push-Up
        "flexiones de pecho" to "Push-Up",
        "flexiones" to "Push-Up",
        "push-up" to "Push-Up",
        "push up" to "Push-Up",

        // Peso Muerto / Deadlift
        "peso muerto" to "Deadlift",
        "deadlift" to "Deadlift",

        // Carrera / Running & Sprints
        "carrera" to "Running",
        "carrera continua" to "Running",
        "20 carrera continua" to "Running",
        "10 carrera continua suave" to "Running",
        "trote" to "Running",
        "trote y carrera de resistencia" to "Running",
        "running" to "Running",
        "sprint" to "Interval Sprints",
        "sprints" to "Interval Sprints",
        "series x 400m" to "Interval Sprints",
        "10 series x 400m" to "Interval Sprints",
        "8 x 60 m lisos" to "Interval Sprints",
        "sprints e intervalos de alta velocidad" to "Interval Sprints",
        "interval sprints" to "Interval Sprints",
        "fartlek" to "Fartlek Run",
        "fartlek run" to "Fartlek Run",

        // Natación / Swimming
        "natacion" to "Freestyle Swimming",
        "natación" to "Freestyle Swimming",
        "crol" to "Freestyle Swimming",
        "freestyle swimming" to "Freestyle Swimming",
        "swim" to "Freestyle Swimming",

        // Abdominales / Crunch & Sit-up
        "3/4 sit-up" to "3/4 Sit-up",
        "3/4 situp" to "3/4 Sit-up",
        "abdominales" to "3/4 Sit-up",
        "abdominales crunch" to "3/4 Sit-up",
        "abdominales crunch elevaciones de piernas" to "3/4 Sit-up",
        "crunch" to "3/4 Sit-up",
        "sit up" to "3/4 Sit-up",
        "sit-up" to "3/4 Sit-up",
        "situp" to "3/4 Sit-up"
    )

    private val nameMapping: Map<String, String> by lazy {
        nameMappingRaw.mapKeys { normalizeText(it.key) }
    }

    /**
     * Fallback embedded items list used when JSON loading stream is unavailable.
     */
    private val fallbackEmbeddedItems = listOf(
        ExerciseJsonItem(
            id = "0040_squat",
            name = "Barbell Squat",
            gif_url = "videos/0040-barbell_squat.gif",
            category = "Legs",
            target = "Quads",
            equipment = "Barbell",
            instruction_steps = listOf(
                "Coloca la barra sobre los trapecios y retírala del soporte.",
                "Baja doblando rodillas y cadera hasta romper el paralelo de 90 grados.",
                "Empuja firmemente a través de los talones para volver a la posición de pie."
            )
        ),
        ExerciseJsonItem(
            id = "0045_leg_extension",
            name = "Leg Extension",
            gif_url = "videos/0045-leg_extension.gif",
            category = "Legs",
            target = "Quads",
            equipment = "Machine",
            instruction_steps = listOf(
                "Ajusta el rodillo de la máquina sobre los tobillos.",
                "Extiende las piernas elevando la carga hasta bloquear suavemente arriba.",
                "Aprieta los cuadríceps y desciende lentamente."
            )
        ),
        ExerciseJsonItem(
            id = "0001_bench",
            name = "Barbell Bench Press",
            gif_url = "videos/0001-barbell_bench_press.gif",
            category = "Chest",
            target = "Chest",
            equipment = "Barbell",
            instruction_steps = listOf(
                "Recuéstate en el banco plano apoyando los pies firmemente en el suelo.",
                "Saca la barra del soporte con un agarre ligeramente superior al ancho de los hombros.",
                "Baja la barra de forma controlada hacia la parte media del pecho.",
                "Empuja la barra verticalmente hasta extender los brazos con fuerza."
            )
        ),
        ExerciseJsonItem(
            id = "0002_incline_bench",
            name = "Incline Barbell Bench Press",
            gif_url = "videos/0002-incline_barbell_press.gif",
            category = "Chest",
            target = "Upper Chest",
            equipment = "Barbell",
            instruction_steps = listOf(
                "Ajusta el banco inclinado a un ángulo de 30 a 45 grados.",
                "Saca la barra del soporte manteniéndola sobre el pecho superior.",
                "Baja la barra lentamente hacia el esternón superior.",
                "Empuja verticalmente hacia arriba manteniendo las escápulas retraídas."
            )
        ),
        ExerciseJsonItem(
            id = "0024_pull_up",
            name = "Pull-up",
            gif_url = "videos/0024-pull_up.gif",
            category = "Calisthenics",
            target = "Back",
            equipment = "Bodyweight",
            instruction_steps = listOf(
                "Sujeta la barra de dominadas con agarre prono más ancho que los hombros.",
                "Tira del cuerpo hacia arriba activando la espalda hasta que la barbilla supere la barra.",
                "Baja lentamente con control completo hasta colgar de nuevo con brazos extendidos."
            )
        ),
        ExerciseJsonItem(
            id = "0020_lat_pulldown",
            name = "Lat Pulldown",
            gif_url = "videos/0020-lat_pulldown.gif",
            category = "Back",
            target = "Back",
            equipment = "Cable",
            instruction_steps = listOf(
                "Sujeta la barra de la polea alta con un agarre ancho.",
                "Siéntate ajustando los rodillos sobre los muslos y saca el pecho.",
                "Tira de la barra hacia la parte superior del pecho llevando los codos hacia abajo y atrás.",
                "Regresa la barra de forma pausada hasta la extensión completa de brazos."
            )
        ),
        ExerciseJsonItem(
            id = "0075_dips",
            name = "Dips",
            gif_url = "videos/0075-triceps_dips.gif",
            category = "Calisthenics",
            target = "Triceps & Chest",
            equipment = "Bodyweight",
            instruction_steps = listOf(
                "Suspéndete en las barras paralelas con los brazos extendidos.",
                "Baja doblando los codos a 90 grados manteniéndote erguido.",
                "Empuja hacia arriba con fuerza de los tríceps hasta bloquear."
            )
        ),
        ExerciseJsonItem(
            id = "0001",
            name = "3/4 Sit-up",
            gif_url = "videos/0001-2gPfomN.gif",
            category = "Abs",
            target = "Abs",
            equipment = "Bodyweight",
            instruction_steps = listOf(
                "Recuéstate boca arriba con las rodillas flexionadas y los pies firmes en el suelo.",
                "Coloca las manos detrás de la cabeza con los codos señalando hacia afuera.",
                "Contrae los abdominales y eleva el torso hacia las rodillas aproximadamente a un ángulo de 45 grados.",
                "Pausa un momento en la parte superior y baja lentamente a la posición inicial."
            )
        )
    )

    /**
     * Helper to test if a string contains a word with proper boundaries.
     */
    private fun containsWord(text: String, word: String): Boolean {
        if (word.isBlank() || text.isBlank()) return false
        val escaped = Regex.escape(word)
        return Regex("\\b$escaped\\b").containsMatchIn(text)
    }

    /**
     * Loads the dataset from assets or classpath resources.
     */
    fun loadDataset(context: Context? = null): List<ExerciseJsonItem> {
        cachedItems?.let { return it }

        var jsonString: String? = null

        if (context != null) {
            try {
                jsonString = context.assets.open("exercise.json").bufferedReader().use { it.readText() }
            } catch (e: Exception) {
                // Ignore and proceed to fallback streams
            }
        }

        if (jsonString.isNullOrBlank()) {
            val resourcePaths = listOf(
                "assets/exercise.json",
                "exercise.json",
                "/assets/exercise.json",
                "/exercise.json"
            )

            for (path in resourcePaths) {
                try {
                    val stream: InputStream? = javaClass.classLoader?.getResourceAsStream(path)
                        ?: javaClass.getResourceAsStream(path)
                    if (stream != null) {
                        jsonString = stream.bufferedReader().use { it.readText() }
                        if (!jsonString.isNullOrBlank()) break
                    }
                } catch (e: Exception) {
                    // Try next path
                }
            }
        }

        val parsed = if (!jsonString.isNullOrBlank()) {
            parseJson(jsonString)
        } else emptyList()

        val finalItems = if (parsed.isNotEmpty()) parsed else fallbackEmbeddedItems
        cachedItems = finalItems
        return finalItems
    }

    /**
     * Parses json string into ExerciseJsonItem list.
     */
    fun parseJson(jsonString: String): List<ExerciseJsonItem> {
        return try {
            val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
            val listType = Types.newParameterizedType(List::class.java, ExerciseJsonItem::class.java)
            val adapter = moshi.adapter<List<ExerciseJsonItem>>(listType)
            adapter.fromJson(jsonString) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Normalizes input text for case-insensitive and accent-insensitive matching.
     */
    fun normalizeText(input: String): String {
        val temp = Normalizer.normalize(input.lowercase().trim(), Normalizer.Form.NFD)
        return temp.replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
            .replace(Regex("[^a-z0-9\\s]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    /**
     * Finds matching ExerciseJsonItem by Spanish or English name or alias,
     * falling back intelligently to category / muscle group without defaulting to 3/4 Sit-up.
     */
    fun findExercise(
        query: String,
        context: Context? = null,
        categoryHint: String? = null,
        targetMuscleHint: String? = null
    ): ExerciseJsonItem? {
        val items = loadDataset(context)
        if (items.isEmpty()) return null
        if (query.isBlank()) return getFallbackExerciseForCategoryOrQuery("", categoryHint, targetMuscleHint, items)

        val normQuery = normalizeText(query)

        // 1. Direct exact match on item name
        items.firstOrNull { normalizeText(it.name) == normQuery }?.let { return it }

        // 2. Exact match in nameMapping table
        val exactCanonical = nameMapping[normQuery]
        if (exactCanonical != null) {
            items.firstOrNull { normalizeText(it.name) == normalizeText(exactCanonical) }?.let { return it }
        }

        // 3. Substring match in nameMapping table (checking longest keys first, with word boundaries)
        val sortedMappingEntries = nameMapping.entries.sortedByDescending { it.key.length }
        val aliasMatch = sortedMappingEntries.firstOrNull { entry ->
            containsWord(normQuery, entry.key) || normQuery == entry.key
        }?.value
        if (aliasMatch != null) {
            items.firstOrNull { normalizeText(it.name) == normalizeText(aliasMatch) }?.let { return it }
        }

        // 4. Substring match on exercise item names in dataset
        items.firstOrNull {
            val normName = normalizeText(it.name)
            containsWord(normQuery, normName) || (normQuery.length >= 4 && containsWord(normName, normQuery))
        }?.let { return it }

        // 5. Biomechanical keyword rules
        val keywordMatch = matchByBiomechanicalKeywords(normQuery, items)
        if (keywordMatch != null) {
            return keywordMatch
        }

        // 6. Realistic 3D Athlete Illustration fallback for unknown/custom exercises
        val fallbackItem = getFallbackExerciseForCategoryOrQuery(normQuery, categoryHint, targetMuscleHint, items)
        val target = targetMuscleHint ?: fallbackItem.target
        val realisticUrl = RealisticExerciseIllustrationGenerator.generateRealisticIllustration(
            context = context,
            exerciseName = query,
            targetMuscle = target
        )
        return fallbackItem.copy(gif_url = realisticUrl)
    }

    /**
     * Checks whether an exercise query matches a known entry in exercise.json dataset or aliases.
     */
    fun isKnownExercise(query: String, context: Context? = null): Boolean {
        if (query.isBlank()) return false
        val items = loadDataset(context)
        val normQuery = normalizeText(query)
        if (items.any { normalizeText(it.name) == normQuery }) return true
        if (nameMapping.containsKey(normQuery)) return true
        if (nameMapping.entries.any { containsWord(normQuery, it.key) || normQuery == it.key }) return true
        if (items.any { containsWord(normQuery, normalizeText(it.name)) || (normQuery.length >= 4 && containsWord(normalizeText(it.name), normQuery)) }) return true
        return matchByBiomechanicalKeywords(normQuery, items) != null
    }

    private fun matchByBiomechanicalKeywords(normQuery: String, items: List<ExerciseJsonItem>): ExerciseJsonItem? {
        return when {
            // Incline bench variants
            (normQuery.contains("incline") || normQuery.contains("inclinado")) &&
                (normQuery.contains("bench") || normQuery.contains("banca") || containsWord(normQuery, "press")) &&
                normQuery.contains("dumbbell") ->
                items.firstOrNull { normalizeText(it.name) == "incline dumbbell press" }

            (normQuery.contains("incline") || normQuery.contains("inclinado")) &&
                (normQuery.contains("bench") || normQuery.contains("banca") || containsWord(normQuery, "press")) ->
                items.firstOrNull { normalizeText(it.name) == "incline barbell bench press" }

            // Bench press variants
            normQuery.contains("bench") || normQuery.contains("banca") ->
                items.firstOrNull { normalizeText(it.name) == "barbell bench press" }

            // Cuádriceps / Leg Extension
            normQuery.contains("cuadricep") || containsWord(normQuery, "quad") || containsWord(normQuery, "quads") ||
                (normQuery.contains("extension") && (normQuery.contains("leg") || normQuery.contains("pierna") || normQuery.contains("maquina"))) ->
                items.firstOrNull { normalizeText(it.name) == "leg extension" }

            // Leg Press
            normQuery.contains("prensa") || normQuery.contains("leg press") ->
                items.firstOrNull { normalizeText(it.name) == "leg press" }

            // Squat
            normQuery.contains("squat") || normQuery.contains("sentadilla") ->
                items.firstOrNull { normalizeText(it.name) == "barbell squat" }

            // Pull-up / Dominada
            normQuery.contains("pullup") || normQuery.contains("pull up") || normQuery.contains("pull-up") || normQuery.contains("dominada") ->
                items.firstOrNull { normalizeText(it.name) == "pull-up" }

            // Lat Pulldown / Jalón
            normQuery.contains("pulldown") || normQuery.contains("jalon") ->
                items.firstOrNull { normalizeText(it.name) == "lat pulldown" }

            // Row / Remo
            containsWord(normQuery, "remo") || containsWord(normQuery, "row") ->
                items.firstOrNull { normalizeText(it.name) == "barbell row" }

            // Dips / Fondos
            containsWord(normQuery, "dip") || containsWord(normQuery, "dips") || containsWord(normQuery, "fondo") || containsWord(normQuery, "fondos") ->
                items.firstOrNull { normalizeText(it.name) == "dips" }

            // Press Militar / Overhead
            normQuery.contains("militar") || normQuery.contains("overhead") || (containsWord(normQuery, "press") && normQuery.contains("hombro")) ->
                items.firstOrNull { normalizeText(it.name) == "overhead press" }

            // Curl / Bíceps
            normQuery.contains("curl") || normQuery.contains("bicep") ->
                items.firstOrNull { normalizeText(it.name) == "dumbbell curl" }

            // Tríceps
            normQuery.contains("tricep") || normQuery.contains("pushdown") ->
                items.firstOrNull { normalizeText(it.name) == "cable triceps pushdown" }

            // Carrera / Running / Sprint / Fartlek
            normQuery.contains("sprint") || normQuery.contains("series x") || normQuery.contains("400m") || normQuery.contains("100m") || normQuery.contains("200m") ->
                items.firstOrNull { normalizeText(it.name) == "interval sprints" }

            normQuery.contains("fartlek") ->
                items.firstOrNull { normalizeText(it.name) == "fartlek run" } ?: items.firstOrNull { normalizeText(it.name) == "running" }

            normQuery.contains("carrera") || normQuery.contains("running") || normQuery.contains("trote") ->
                items.firstOrNull { normalizeText(it.name) == "running" }

            // Natación / Swimming / Crol
            normQuery.contains("natacion") || normQuery.contains("crol") || normQuery.contains("swimming") || normQuery.contains("swim") ->
                items.firstOrNull { normalizeText(it.name) == "freestyle swimming" }

            // Abs / Sit-up / Crunch
            normQuery.contains("situp") || normQuery.contains("sit up") || normQuery.contains("sit-up") || normQuery.contains("crunch") || normQuery.contains("abdominal") ->
                items.firstOrNull { normalizeText(it.name) == "3/4 sit-up" }

            else -> null
        }
    }

    /**
     * Returns category/muscle-based fallback exercise when query is not directly recognized.
     * NEVER defaults to 3/4 Sit-up unless query specifically indicates abdominal/core exercise.
     */
    fun getFallbackExerciseForCategoryOrQuery(
        normQuery: String,
        categoryHint: String? = null,
        targetMuscleHint: String? = null,
        items: List<ExerciseJsonItem>
    ): ExerciseJsonItem {
        val combinedContext = normalizeText("${categoryHint.orEmpty()} ${targetMuscleHint.orEmpty()} $normQuery")

        return when {
            // Pierna / Leg -> Fallback to Leg Extension or Squat
            combinedContext.contains("pierna") || combinedContext.contains("leg") ||
                combinedContext.contains("cuadricep") || combinedContext.contains("quad") ||
                combinedContext.contains("isquiotibial") || combinedContext.contains("femoral") ||
                combinedContext.contains("glute") || combinedContext.contains("calf") ||
                combinedContext.contains("gemelo") -> {
                items.firstOrNull { normalizeText(it.name) == "leg extension" }
                    ?: items.firstOrNull { normalizeText(it.name) == "barbell squat" }
                    ?: items.firstOrNull { it.category.equals("Legs", ignoreCase = true) }
            }

            // Pecho / Chest -> Fallback to Bench Press
            combinedContext.contains("pecho") || combinedContext.contains("chest") ||
                combinedContext.contains("pectoral") -> {
                items.firstOrNull { normalizeText(it.name) == "barbell bench press" }
                    ?: items.firstOrNull { it.category.equals("Chest", ignoreCase = true) }
            }

            // Espalda / Back -> Fallback to Lat Pulldown or Pull-up
            combinedContext.contains("espalda") || combinedContext.contains("back") ||
                combinedContext.contains("dorsal") || combinedContext.contains("lat") ||
                combinedContext.contains("trapecio") -> {
                items.firstOrNull { normalizeText(it.name) == "lat pulldown" }
                    ?: items.firstOrNull { normalizeText(it.name) == "pull-up" }
                    ?: items.firstOrNull { it.category.equals("Back", ignoreCase = true) }
            }

            // Hombro / Shoulder -> Fallback to Overhead Press
            combinedContext.contains("hombro") || combinedContext.contains("shoulder") ||
                combinedContext.contains("deltoid") || combinedContext.contains("militar") -> {
                items.firstOrNull { normalizeText(it.name) == "overhead press" }
                    ?: items.firstOrNull { it.category.equals("Shoulders", ignoreCase = true) }
            }

            // Brazos / Arms -> Fallback to Dumbbell Curl or Triceps Pushdown
            combinedContext.contains("brazo") || combinedContext.contains("arm") ||
                combinedContext.contains("bicep") || combinedContext.contains("tricep") ||
                combinedContext.contains("curl") -> {
                items.firstOrNull { normalizeText(it.name) == "dumbbell curl" }
                    ?: items.firstOrNull { normalizeText(it.name) == "cable triceps pushdown" }
                    ?: items.firstOrNull { it.category.equals("Arms", ignoreCase = true) }
            }

            // Cardio / Carrera / Natación -> Fallback to Running or Swim
            combinedContext.contains("cardio") || combinedContext.contains("carrera") ||
                combinedContext.contains("running") || combinedContext.contains("sprint") ||
                combinedContext.contains("atletismo") -> {
                items.firstOrNull { normalizeText(it.name) == "running" }
                    ?: items.firstOrNull { it.category.equals("Running", ignoreCase = true) }
            }

            combinedContext.contains("natacion") || combinedContext.contains("swim") ||
                combinedContext.contains("crol") || combinedContext.contains("piscina") -> {
                items.firstOrNull { normalizeText(it.name) == "freestyle swimming" }
                    ?: items.firstOrNull { it.category.equals("Swimming", ignoreCase = true) }
            }

            // Abdominales / Core -> Fallback to 3/4 Sit-up
            combinedContext.contains("abdominal") || combinedContext.contains("abs") ||
                combinedContext.contains("core") || combinedContext.contains("crunch") ||
                combinedContext.contains("situp") || combinedContext.contains("sit up") -> {
                items.firstOrNull { normalizeText(it.name) == "3/4 sit-up" }
                    ?: items.firstOrNull { it.category.equals("Abs", ignoreCase = true) }
            }

            // General Fallback: NEVER default to 3/4 Sit-up! Choose Squat or Bench Press or first non-situp item
            else -> {
                items.firstOrNull { normalizeText(it.name) == "barbell squat" }
                    ?: items.firstOrNull { normalizeText(it.name) == "barbell bench press" }
                    ?: items.firstOrNull { normalizeText(it.name) != "3 4 sit up" && normalizeText(it.name) != "3/4 sit-up" }
                    ?: items.first()
            }
        } ?: items.first()
    }

    /**
     * Returns GitHub Raw URL for relative gif_url in exercise.json,
     * or dynamic ExerciseWebScraperEngine media URL for unknown / custom exercises.
     */
    fun getFullGifUrl(
        query: String,
        context: Context? = null,
        categoryHint: String? = null,
        targetMuscleHint: String? = null
    ): String {
        if (query.isNotBlank() && !isKnownExercise(query, context)) {
            return RealisticExerciseIllustrationGenerator.generateRealisticIllustration(
                context = context,
                exerciseName = query,
                targetMuscle = targetMuscleHint
            )
        }
        val item = findExercise(query, context, categoryHint, targetMuscleHint)
        val relativeUrl = item?.gif_url ?: ""
        if (relativeUrl.isBlank()) {
            return RealisticExerciseIllustrationGenerator.generateRealisticIllustration(
                context = context,
                exerciseName = query,
                targetMuscle = targetMuscleHint
            )
        }
        if (relativeUrl.startsWith("http://") || relativeUrl.startsWith("https://") || relativeUrl.startsWith("file://")) {
            return relativeUrl
        }
        val cleanPath = relativeUrl.trimStart('/')
        return "$BASE_GITHUB_RAW_URL$cleanPath"
    }

    /**
     * Returns instruction steps translated to Spanish.
     */
    fun getInstructionStepsEs(
        query: String,
        context: Context? = null,
        categoryHint: String? = null,
        targetMuscleHint: String? = null
    ): List<String> {
        val item = findExercise(query, context, categoryHint, targetMuscleHint) ?: return emptyList()
        return item.instruction_steps.map { translateStepToEs(it) }
    }

    /**
     * Helper to ensure step text is translated to Spanish.
     */
    fun translateStepToEs(step: String): String {
        if (step.contains(" de ") || step.contains(" el ") || step.contains(" la ") || step.contains(" los ") || step.contains(" las ") || step.contains(" con ") || step.contains(" para ")) {
            return step
        }
        return step
            .replace("Lie flat on your back", "Túmbate sobre tu espalda")
            .replace("with your knees bent", "con las rodillas flexionadas")
            .replace("and feet flat on the ground", "y los pies apoyados en el suelo")
            .replace("Place your hands behind your head", "Coloca las manos detrás de la cabeza")
            .replace("Engaging your abs", "Contrayendo tus abdominales")
            .replace("slowly lift your upper body", "eleva lentamente la parte superior del cuerpo")
            .replace("Pause for a moment at the top", "Pausa un momento arriba")
            .replace("Repeat for the desired number of repetitions", "Repite el número deseado de repeticiones")
            .replace("Grasp bar with a wide grip", "Sujeta la barra con un agarre ancho")
            .replace("Pull down smoothly to upper chest", "Tira suavemente hacia la parte superior del pecho")
            .replace("Bar rested across upper back", "Barra apoyada en la parte superior de la espalda")
            .replace("Squat down breaking parallel", "Baja en sentadilla rompiendo el paralelo")
            .replace("Overhand grip wider than shoulders", "Agarre prono más ancho que los hombros")
            .replace("Pull up until chin clears the bar", "Tira hacia arriba hasta que la barbilla supere la barra")
    }

    /**
     * Returns concatenated Spanish instructions.
     */
    fun getInstructionsEsText(
        query: String,
        context: Context? = null,
        categoryHint: String? = null,
        targetMuscleHint: String? = null
    ): String {
        val steps = getInstructionStepsEs(query, context, categoryHint, targetMuscleHint)
        if (steps.isEmpty()) return "Ejecuta el ejercicio con técnica controlada, postura firme y respiración sostenida."
        return steps.mapIndexed { idx, s -> "${idx + 1}. $s" }.joinToString("\n")
    }

    fun getAllExercises(context: Context? = null): List<ExerciseJsonItem> {
        return loadDataset(context)
    }

    fun clearCache() {
        cachedItems = null
    }
}
