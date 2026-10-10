package com.example.personalsport.data.provider

import android.content.Context

/**
 * Data representation of an exercise entry in the Exercise GIF Dataset.
 */
data class ExerciseDatasetEntry(
    val id: String,
    val nameEn: String,
    val nameEs: String,
    val category: String, // "Gym", "Calisthenics", "Running", "Swimming"
    val targetMuscle: String, // "Chest", "Back", "Legs", "Shoulders", "Arms", "Abs", "Cardio", "Full Body"
    val targetMuscleEs: String, // "Pecho", "Espalda", "Piernas", "Hombros", "Brazos", "Abdominales", "Cardio", "Cuerpo Completo"
    val equipment: String, // "Barbell", "Dumbbell", "Cable", "Machine", "Bodyweight", "Track", "Pool"
    val equipmentEs: String, // "Barra", "Mancuerna", "Polea", "Máquina", "Peso Corporal", "Pista", "Piscina"
    val gifUrl: String,
    val mediaImageUrl: String,
    val instructionsEs: String,
    val instructionsEn: String,
    val aliases: List<String> = emptyList()
)

/**
 * Exercise GIF Dataset Provider mapped from `hasaneyldrm/exercises-dataset` GitHub repository.
 * Delegates dataset parsing and queries directly to `ExerciseJsonDatasetParser`.
 */
object ExerciseGifDatasetProvider {

    const val BASE_RAW_URL = ExerciseJsonDatasetParser.BASE_GITHUB_RAW_URL
    const val FALLBACK_GIF_URL = ExerciseJsonDatasetParser.FALLBACK_GIF_URL
    const val DEFAULT_ONLINE_GIF_URL = "${BASE_RAW_URL}videos/0001-barbell_bench_press.gif"

    /**
     * Resolves a relative GIF path (e.g. "videos/0001-2gPfomN.gif") or full URL into a valid loadable URL string.
     */
    fun resolveGifUrl(gifPath: String?): String {
        if (gifPath.isNullOrBlank()) return FALLBACK_GIF_URL
        if (gifPath.startsWith("http://") ||
            gifPath.startsWith("https://") ||
            gifPath.startsWith("file://") ||
            gifPath.startsWith("android.resource://")
        ) {
            return gifPath
        }
        val cleanPath = gifPath.trimStart('/')
        return "$BASE_RAW_URL$cleanPath"
    }

    /**
     * Returns the bundled offline fallback animated GIF URL.
     */
    fun getOfflineFallbackGifUrl(): String = FALLBACK_GIF_URL

    /**
     * Normalizes text string for matching.
     */
    fun normalizeText(input: String): String = ExerciseJsonDatasetParser.normalizeText(input)

    /**
     * Converts ExerciseJsonItem to ExerciseDatasetEntry.
     */
    fun toDatasetEntry(item: ExerciseJsonItem, context: Context? = null): ExerciseDatasetEntry {
        val fullGifUrl = if (item.gif_url.startsWith("file://")) item.gif_url else ExerciseJsonDatasetParser.getFullGifUrl(item.name, context)
        val instructionsEsText = ExerciseJsonDatasetParser.getInstructionsEsText(item.name, context)
        val instructionsEnText = item.instruction_steps.joinToString(" ")

        return ExerciseDatasetEntry(
            id = canonicalId(item.id, item.name),
            nameEn = item.name,
            nameEs = mapNameEs(item.name),
            category = mapCategory(item.category, item.name),
            targetMuscle = item.target.ifBlank { "Full Body" },
            targetMuscleEs = mapTargetMuscleEs(item.target.ifBlank { item.name }),
            equipment = item.equipment.ifBlank { "Bodyweight" },
            equipmentEs = mapEquipmentEs(item.equipment),
            gifUrl = fullGifUrl,
            mediaImageUrl = fullGifUrl,
            instructionsEs = instructionsEsText,
            instructionsEn = instructionsEnText.ifBlank { instructionsEsText },
            aliases = listOf(item.name.lowercase(), item.id, mapNameEs(item.name).lowercase())
        )
    }

    private fun canonicalId(jsonId: String, name: String): String {
        val norm = ExerciseJsonDatasetParser.normalizeText(name)
        return when {
            norm.contains("leg extension") -> "leg_extension"
            norm.contains("bench press") -> "barbell_bench_press"
            norm.contains("incline barbell") -> "incline_barbell_press"
            norm.contains("incline dumbbell") -> "incline_dumbbell_press"
            norm.contains("lat pulldown") -> "lat_pulldown"
            norm.contains("squat") -> "barbell_squat"
            norm.contains("pullup") || norm.contains("pull up") || norm.contains("pull-up") -> "pull_up"
            norm.contains("situp") || norm.contains("sit up") || norm.contains("sit-up") || norm.contains("crunch") || norm.contains("3/4") -> "abdominal_crunch"
            norm.contains("leg press") -> "leg_press"
            norm.contains("overhead press") || norm.contains("shoulder press") -> "overhead_press"
            norm.contains("dips") -> "triceps_dips"
            norm.contains("dumbbell curl") || norm.contains("bicep curl") -> "dumbbell_curl"
            norm.contains("deadlift") -> "deadlift"
            norm.contains("push up") || norm.contains("push-up") -> "push_up"
            norm.contains("sprint") -> "interval_sprints"
            norm.contains("running") -> "running_endurance"
            norm.contains("swimming") -> "freestyle_crol"
            else -> jsonId
        }
    }

    private fun mapCategory(cat: String, name: String): String {
        val norm = ExerciseJsonDatasetParser.normalizeText("$cat $name")
        return when {
            norm.contains("running") || norm.contains("sprint") || norm.contains("carrera") || norm.contains("trote") -> "Running"
            norm.contains("swim") || norm.contains("crol") || norm.contains("natacion") -> "Swimming"
            norm.contains("calisthenics") || norm.contains("pullup") || norm.contains("pull up") || norm.contains("dips") || norm.contains("pushup") || norm.contains("burpees") -> "Calisthenics"
            else -> "Gym"
        }
    }

    private fun mapTargetMuscleEs(target: String): String {
        val norm = ExerciseJsonDatasetParser.normalizeText(target)
        return when {
            norm.contains("chest") || norm.contains("pecho") || norm.contains("bench") -> "Pecho"
            norm.contains("back") || norm.contains("espalda") || norm.contains("lat") || norm.contains("pulldown") -> "Espalda"
            norm.contains("leg") || norm.contains("quad") || norm.contains("hamstring") || norm.contains("pierna") || norm.contains("squat") -> "Piernas"
            norm.contains("shoulder") || norm.contains("delt") || norm.contains("hombro") || norm.contains("overhead") -> "Hombros"
            norm.contains("arm") || norm.contains("bicep") || norm.contains("tricep") || norm.contains("brazo") || norm.contains("curl") -> "Brazos"
            norm.contains("abs") || norm.contains("abdominal") || norm.contains("core") || norm.contains("waist") || norm.contains("sit") || norm.contains("crunch") -> "Abdominales"
            norm.contains("cardio") || norm.contains("running") || norm.contains("swim") -> "Cardio"
            else -> "Cuerpo Completo"
        }
    }

    private fun mapEquipmentEs(equipment: String): String {
        val norm = ExerciseJsonDatasetParser.normalizeText(equipment)
        return when {
            norm.contains("barbell") || norm.contains("barra") -> "Barra"
            norm.contains("dumbbell") || norm.contains("mancuerna") -> "Mancuernas"
            norm.contains("cable") || norm.contains("polea") -> "Polea"
            norm.contains("machine") || norm.contains("maquina") -> "Máquina"
            norm.contains("bodyweight") || norm.contains("body weight") || norm.contains("peso corporal") -> "Peso Corporal"
            norm.contains("track") || norm.contains("pista") -> "Pista"
            norm.contains("pool") || norm.contains("piscina") -> "Piscina"
            else -> equipment.ifBlank { "Peso Corporal" }
        }
    }

    private fun mapNameEs(nameEn: String): String {
        val norm = ExerciseJsonDatasetParser.normalizeText(nameEn)
        return when {
            norm.contains("leg extension") -> "Extensión de Cuádriceps"
            norm.contains("bench press") -> "Press de Banca"
            norm.contains("incline barbell") -> "Press Inclinado con Barra"
            norm.contains("incline dumbbell") -> "Press Inclinado con Mancuernas"
            norm.contains("squat") -> "Sentadilla"
            norm.contains("pull up") || norm.contains("pull-up") -> "Dominadas"
            norm.contains("lat pulldown") -> "Jalón al Pecho"
            norm.contains("dips") -> "Fondos en Paralelas"
            norm.contains("leg press") -> "Prensa de Piernas"
            norm.contains("overhead press") -> "Press Militar"
            norm.contains("dumbbell curl") -> "Curl de Bíceps"
            norm.contains("deadlift") -> "Peso Muerto"
            norm.contains("push up") || norm.contains("push-up") -> "Flexiones de Pecho"
            norm.contains("sit up") || norm.contains("sit-up") || norm.contains("crunch") || norm.contains("3/4") -> "Abdominales Crunch"
            norm.contains("interval sprints") -> "Sprints e Intervalos de Alta Velocidad"
            norm.contains("running") -> "Trote y Carrera de Resistencia"
            norm.contains("swimming") -> "Estilo Libre / Crol"
            else -> nameEn
        }
    }

    /**
     * Retrieves full exercise entry details for a given exercise name or alias.
     */
    fun getExerciseDetails(
        exerciseName: String,
        context: Context? = null,
        categoryHint: String? = null,
        targetMuscleHint: String? = null
    ): ExerciseDatasetEntry? {
        return findExercise(exerciseName, context, categoryHint, targetMuscleHint)
    }

    /**
     * Finds matching exercise entry by delegating query to `ExerciseJsonDatasetParser`.
     */
    fun findExercise(
        exerciseName: String,
        context: Context? = null,
        categoryHint: String? = null,
        targetMuscleHint: String? = null
    ): ExerciseDatasetEntry? {
        val item = ExerciseJsonDatasetParser.findExercise(exerciseName, context, categoryHint, targetMuscleHint) ?: return null
        return toDatasetEntry(item, context)
    }

    /**
     * Gets direct GIF URL for a given exercise name or query.
     */
    fun getGifUrlForExercise(
        exerciseName: String,
        context: Context? = null,
        categoryHint: String? = null,
        targetMuscleHint: String? = null
    ): String {
        return ExerciseJsonDatasetParser.getFullGifUrl(exerciseName, context, categoryHint, targetMuscleHint)
    }

    /**
     * Gets descriptive media image URL for a given exercise name.
     */
    fun getMediaImageUrlForExercise(
        exerciseName: String,
        context: Context? = null,
        categoryHint: String? = null,
        targetMuscleHint: String? = null
    ): String {
        return ExerciseJsonDatasetParser.getFullGifUrl(exerciseName, context, categoryHint, targetMuscleHint)
    }

    /**
     * Retrieves all exercises matching a target muscle category.
     */
    fun getExercisesForTargetMuscle(targetMuscle: String, context: Context? = null): List<ExerciseDatasetEntry> {
        val all = getAllExercises(context)
        val norm = normalizeText(targetMuscle)
        return all.filter {
            normalizeText(it.targetMuscle).contains(norm) || normalizeText(it.targetMuscleEs).contains(norm)
        }
    }

    /**
     * Retrieves all exercises matching a primary category (Gym, Calisthenics, Running, Swimming).
     */
    fun getExercisesByCategory(category: String, context: Context? = null): List<ExerciseDatasetEntry> {
        val all = getAllExercises(context)
        val norm = normalizeText(category)
        return all.filter { normalizeText(it.category).contains(norm) }
    }

    /**
     * Searches exercise dataset with a query.
     */
    fun searchExercises(query: String, context: Context? = null): List<ExerciseDatasetEntry> {
        val all = getAllExercises(context)
        if (query.isBlank()) return all
        val norm = normalizeText(query)
        return all.filter {
            normalizeText(it.nameEn).contains(norm) ||
            normalizeText(it.nameEs).contains(norm) ||
            normalizeText(it.category).contains(norm) ||
            normalizeText(it.targetMuscleEs).contains(norm) ||
            it.aliases.any { alias -> normalizeText(alias).contains(norm) }
        }
    }

    /**
     * Returns total list of exercises in dataset.
     */
    fun getAllExercises(context: Context? = null): List<ExerciseDatasetEntry> {
        val jsonItems = ExerciseJsonDatasetParser.getAllExercises(context)
        return jsonItems.map { toDatasetEntry(it, context) }
    }
}
