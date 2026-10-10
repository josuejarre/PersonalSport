package com.example.personalsport.data.provider

import java.text.Normalizer

/**
 * Nano Banana Asset Provider for bundled anatomical line-art exercise image catalog.
 *
 * Provides instant offline access to bundled anatomical line-art images located in:
 * `app/src/main/assets/nano_banana/`
 *
 * Asset File URLs:
 * - `file:///android_asset/nano_banana/nano_banana_chest.png`
 * - `file:///android_asset/nano_banana/nano_banana_back.png`
 * - `file:///android_asset/nano_banana/nano_banana_legs.png`
 * - `file:///android_asset/nano_banana/nano_banana_shoulders.png`
 * - `file:///android_asset/nano_banana/nano_banana_arms.png`
 * - `file:///android_asset/nano_banana/nano_banana_abs.png`
 * - `file:///android_asset/nano_banana/nano_banana_running.png`
 * - `file:///android_asset/nano_banana/nano_banana_swimming.png`
 * - `file:///android_asset/nano_banana/nano_banana_default.png`
 */
object NanoBananaAssetProvider {

    const val ASSET_PREFIX = "file:///android_asset/nano_banana/"

    const val CATEGORY_CHEST = "chest"
    const val CATEGORY_BACK = "back"
    const val CATEGORY_LEGS = "legs"
    const val CATEGORY_SHOULDERS = "shoulders"
    const val CATEGORY_ARMS = "arms"
    const val CATEGORY_ABS = "abs"
    const val CATEGORY_RUNNING = "running"
    const val CATEGORY_SWIMMING = "swimming"
    const val CATEGORY_DEFAULT = "default"

    val CHEST_ASSET_URL = "${ASSET_PREFIX}nano_banana_chest.png"
    val BACK_ASSET_URL = "${ASSET_PREFIX}nano_banana_back.png"
    val LEGS_ASSET_URL = "${ASSET_PREFIX}nano_banana_legs.png"
    val SHOULDERS_ASSET_URL = "${ASSET_PREFIX}nano_banana_shoulders.png"
    val ARMS_ASSET_URL = "${ASSET_PREFIX}nano_banana_arms.png"
    val ABS_ASSET_URL = "${ASSET_PREFIX}nano_banana_abs.png"
    val RUNNING_ASSET_URL = "${ASSET_PREFIX}nano_banana_running.png"
    val SWIMMING_ASSET_URL = "${ASSET_PREFIX}nano_banana_swimming.png"
    val DEFAULT_ASSET_URL = "${ASSET_PREFIX}nano_banana_default.png"

    /**
     * Normalizes input text for case-insensitive and accent-insensitive matching.
     */
    fun normalize(input: String?): String {
        if (input.isNullOrBlank()) return ""
        val temp = Normalizer.normalize(input.lowercase().trim(), Normalizer.Form.NFD)
        return temp.replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
            .replace(Regex("[^a-z0-9\\s]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun containsWord(text: String, word: String): Boolean {
        if (word.isBlank() || text.isBlank()) return false
        val escaped = Regex.escape(word)
        return Regex("\\b$escaped\\b").containsMatchIn(text)
    }

    /**
     * Determines the Nano Banana asset category key based on exercise name, category, or target muscle.
     */
    fun resolveCategory(
        exerciseName: String? = null,
        category: String? = null,
        targetMuscle: String? = null
    ): String {
        val combined = normalize("${exerciseName.orEmpty()} ${category.orEmpty()} ${targetMuscle.orEmpty()}")
        if (combined.isBlank()) return CATEGORY_DEFAULT

        return when {
            // Swimming / Open Water
            combined.contains("swim") || combined.contains("natacion") || combined.contains("crol") ||
                    combined.contains("piscina") || combined.contains("water") || combined.contains("aguas abiertas") ||
                    combined.contains("mariposa") -> CATEGORY_SWIMMING

            // Running / Sprints
            combined.contains("run") || combined.contains("carrera") || combined.contains("trote") ||
                    combined.contains("sprint") || combined.contains("atletismo") || combined.contains("fartlek") ||
                    combined.contains("rodaje") || combined.contains("maraton") || combined.contains("intervalo") ||
                    combined.contains("pista") -> CATEGORY_RUNNING

            // Abs / Core
            combined.contains("abs") || combined.contains("abdo") || combined.contains("core") ||
                    combined.contains("crunch") || combined.contains("situp") || combined.contains("sit up") ||
                    combined.contains("plancha") || combined.contains("oblicuo") -> CATEGORY_ABS

            // Shoulders / Hombros
            combined.contains("shoulder") || combined.contains("hombro") || combined.contains("delt") ||
                    combined.contains("deltoid") || combined.contains("deltoides") || combined.contains("militar") ||
                    combined.contains("overhead") || combined.contains("laterales") || containsWord(combined, "lateral") -> CATEGORY_SHOULDERS

            // Arms / Brazos
            combined.contains("arm") || combined.contains("brazo") || combined.contains("bicep") ||
                    combined.contains("tricep") || combined.contains("curl") || combined.contains("dip") ||
                    combined.contains("fondo") || combined.contains("pushdown") -> CATEGORY_ARMS

            // Chest / Pecho
            combined.contains("chest") || combined.contains("pecho") || combined.contains("pectoral") ||
                    combined.contains("bench") || combined.contains("banca") || combined.contains("pushup") ||
                    combined.contains("push up") || combined.contains("flexion") -> CATEGORY_CHEST

            // Back / Espalda
            combined.contains("back") || combined.contains("espalda") || combined.contains("dorsal") ||
                    combined.contains("pulldown") || combined.contains("row") || combined.contains("remo") ||
                    combined.contains("pullup") || combined.contains("pull up") || combined.contains("dominada") ||
                    combined.contains("jalon") || combined.contains("trapecio") || combined.contains("lats") ||
                    containsWord(combined, "lat") -> CATEGORY_BACK

            // Legs / Pierna
            combined.contains("leg") || combined.contains("pierna") || combined.contains("quad") ||
                    combined.contains("cuadricep") || combined.contains("squat") || combined.contains("sentadilla") ||
                    combined.contains("prensa") || combined.contains("lunge") || combined.contains("zancada") ||
                    combined.contains("glute") || combined.contains("isquio") || combined.contains("femoral") ||
                    combined.contains("hamstring") || combined.contains("calf") || combined.contains("gemelo") ||
                    combined.contains("extension") -> CATEGORY_LEGS

            else -> CATEGORY_DEFAULT
        }
    }

    /**
     * Returns the bundled asset file URL for the given exercise parameters.
     * E.g. `file:///android_asset/nano_banana/nano_banana_chest.png`
     */
    fun getAssetUrl(
        exerciseName: String? = null,
        category: String? = null,
        targetMuscle: String? = null
    ): String {
        val categoryKey = resolveCategory(exerciseName, category, targetMuscle)
        return "${ASSET_PREFIX}nano_banana_${categoryKey}.png"
    }

    /**
     * Returns the bundled asset file URL for a direct category key string.
     */
    fun getAssetUrlForCategory(categoryKey: String?): String {
        val resolved = resolveCategory(category = categoryKey)
        return "${ASSET_PREFIX}nano_banana_${resolved}.png"
    }

    /**
     * Returns the bundled asset file URL for a missing or custom exercise.
     */
    fun getAssetUrlForExercise(
        exerciseName: String,
        category: String? = null,
        targetMuscle: String? = null
    ): String {
        return getAssetUrl(exerciseName, category, targetMuscle)
    }

    /**
     * Checks whether a given image URL string references a Nano Banana asset or IA generated image.
     */
    fun isNanoBananaAssetUrl(url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        return url.contains("nano_banana", ignoreCase = true)
    }
}
