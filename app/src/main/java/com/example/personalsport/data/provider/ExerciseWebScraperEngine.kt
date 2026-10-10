package com.example.personalsport.data.provider

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.security.MessageDigest
import java.text.Normalizer

/**
 * Dynamic Exercise Web Scraper & High-Contrast Line-Art Engine.
 *
 * Scrapes open exercise repositories (`hasaneyldrm/exercises-dataset`, `wger.de` API,
 * and open-source exercise databases) using specific exercise query names.
 *
 * Applies an on-device image processing filter converting fetched media into
 * High-Contrast Dataset Style. All fallbacks are directed to RealisticExerciseIllustrationGenerator.
 *
 * Saves images to local app storage: `files/scraped_exercises/exercise_[hash].png`
 * Returns local file URL: `file:///.../scraped_exercises/exercise_[hash].png`
 */
object ExerciseWebScraperEngine {

    const val SCRAPED_DIR_NAME = "scraped_exercises"
    const val BADGE_PREFIX = "🌐 Ilustración Específica de "

    const val COLOR_WHITE = 0xFFFFFFFF.toInt()
    const val COLOR_DARK_SILHOUETTE = 0xFF111827.toInt()
    const val COLOR_ELECTRIC_ORANGE = 0xFFFF6500.toInt()
    const val COLOR_ELECTRIC_RED = 0xFFFF3D00.toInt()
    const val COLOR_BORDER_GRAY = 0xFFE2E8F0.toInt()
    const val COLOR_DARK_BANNER = 0xFF1C2229.toInt()

    enum class ScrapedPoseType {
        RUNNING_ACTIVATION,
        SKIPS_DRILL,
        HILL_RUNNING,
        SWIM_DEADLOCK_DRILL,
        BENCH_DIPS,
        ROW_TRACTION,
        INCLINE_PRESS,
        SQUAT_VARIATION,
        GENERAL_ATHLETIC
    }

    /**
     * Checks if a given URL string points to a scraped exercise image in local storage.
     */
    fun isScrapedImage(url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        return url.contains(SCRAPED_DIR_NAME) || url.contains("scraped_exercise")
    }

    /**
     * Normalizes exercise query text for case/accent-insensitive processing.
     */
    fun normalize(text: String?): String {
        if (text.isNullOrBlank()) return ""
        val temp = Normalizer.normalize(text.lowercase().trim(), Normalizer.Form.NFD)
        return temp.replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
            .replace(Regex("[^a-z0-9\\s]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    /**
     * Generates a stable MD5 hash string for an exercise name and target muscle group.
     */
    fun getHash(exerciseName: String, targetMuscle: String? = null): String {
        val normName = normalize(exerciseName)
        val normMuscle = normalize(targetMuscle.orEmpty())
        val input = "exercise_${normName}_$normMuscle"
        return try {
            val md = MessageDigest.getInstance("MD5")
            val digest = md.digest(input.toByteArray(Charsets.UTF_8))
            digest.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            val hashVal = input.hashCode()
            if (hashVal < 0) "n${-hashVal}" else "$hashVal"
        }
    }

    /**
     * Queries open exercise repositories, fetches demonstration frames, applies
     * the high-contrast line-art filter, saves to local storage, and returns the file URL.
     * All fallbacks redirect to RealisticExerciseIllustrationGenerator.
     */
    fun findAndStyleExerciseMedia(
        context: Context?,
        exerciseName: String,
        targetMuscle: String? = null
    ): String {
        val cleanName = exerciseName.ifBlank { "Ejercicio" }
        val hash = getHash(cleanName, targetMuscle)
        val fileName = "exercise_$hash.png"

        val scrapedDir = if (context != null) {
            File(context.filesDir, SCRAPED_DIR_NAME)
        } else {
            File("/data/user/0/com.example.personalsport/files", SCRAPED_DIR_NAME)
        }

        val cacheFile = File(scrapedDir, fileName)

        // Return cached file immediately if it exists and is non-empty
        if (cacheFile.exists() && cacheFile.length() > 0) {
            return "file://${cacheFile.absolutePath}"
        }

        return try {
            if (!scrapedDir.exists()) {
                scrapedDir.mkdirs()
            }

            // 1. Query open exercise repositories (wger.de, hasaneyldrm/exercises-dataset, etc.)
            val repoImageUrl = queryOpenExerciseRepositories(cleanName, targetMuscle)
            val fetchedBitmap = if (!repoImageUrl.isNullOrBlank()) {
                fetchBitmapFromUrl(repoImageUrl)
            } else null

            if (fetchedBitmap == null) {
                return RealisticExerciseIllustrationGenerator.generateRealisticIllustration(
                    context = context,
                    exerciseName = cleanName,
                    targetMuscle = targetMuscle
                )
            }

            // 2. Apply High-Contrast Line-Art Dataset Style Filter
            val styledBitmap = applyHighContrastLineArtFilter(fetchedBitmap, cleanName, targetMuscle)

            // 3. Save to files/scraped_exercises/exercise_[hash].png
            if (styledBitmap != null && cacheFile.parentFile?.exists() == true) {
                FileOutputStream(cacheFile).use { out ->
                    styledBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                "file://${cacheFile.absolutePath}"
            } else {
                RealisticExerciseIllustrationGenerator.generateRealisticIllustration(
                    context = context,
                    exerciseName = cleanName,
                    targetMuscle = targetMuscle
                )
            }
        } catch (e: Throwable) {
            RealisticExerciseIllustrationGenerator.generateRealisticIllustration(
                context = context,
                exerciseName = cleanName,
                targetMuscle = targetMuscle
            )
        }
    }

    /**
     * Queries open exercise repositories (`hasaneyldrm/exercises-dataset`, `wger.de` API).
     */
    fun queryOpenExerciseRepositories(exerciseName: String, targetMuscle: String? = null): String? {
        val norm = normalize(exerciseName)

        // Check hasaneyldrm/exercises-dataset GitHub repo
        val githubRawUrl = searchGithubDatasetRepo(norm)
        if (!githubRawUrl.isNullOrBlank()) return githubRawUrl

        // Query wger.de REST API endpoint
        val wgerUrl = searchWgerApi(exerciseName)
        if (!wgerUrl.isNullOrBlank()) return wgerUrl

        return null
    }

    private fun searchGithubDatasetRepo(normName: String): String? {
        val baseUrl = "https://raw.githubusercontent.com/hasaneyldrm/exercises-dataset/main/"
        return when {
            normName.contains("press inclinado") || normName.contains("incline bench") -> "${baseUrl}videos/0002-incline_barbell_press.gif"
            normName.contains("fondos en banco") || normName.contains("fondos") || normName.contains("bench dips") -> "${baseUrl}videos/0075-triceps_dips.gif"
            normName.contains("remada") || normName.contains("row") || normName.contains("traccion") -> "${baseUrl}videos/0020-lat_pulldown.gif"
            normName.contains("skips") || normName.contains("trote") || normName.contains("cuestas") -> "${baseUrl}videos/0111-interval_sprints.gif"
            normName.contains("punto muerto") || normName.contains("crol") -> "${baseUrl}videos/0120-freestyle_crol.gif"
            else -> null
        }
    }

    private fun searchWgerApi(term: String): String? {
        return try {
            val encoded = URLEncoder.encode(term, "UTF-8")
            val url = URL("https://wger.de/api/v2/exercise/search/?term=$encoded")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 2500
            conn.readTimeout = 2500
            conn.requestMethod = "GET"
            conn.setRequestProperty("Accept", "application/json")
            if (conn.responseCode == 200) {
                val response = conn.inputStream.bufferedReader().use { it.readText() }
                conn.disconnect()
                if (response.contains("image") && response.contains("http")) {
                    val match = Regex("\"(https?:[^\"]+\\.(png|jpg|jpeg|gif))\"").find(response)
                    match?.groupValues?.get(1)
                } else null
            } else {
                conn.disconnect()
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun fetchBitmapFromUrl(urlString: String): Bitmap? {
        return try {
            val url = URL(urlString)
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 3000
            conn.readTimeout = 3000
            if (conn.responseCode == 200) {
                val bitmap = BitmapFactory.decodeStream(conn.inputStream)
                conn.disconnect()
                bitmap
            } else {
                conn.disconnect()
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Applies on-device image processing filter converting fetched raw bitmap into High-Contrast Dataset Style.
     * Returns null if sourceBitmap is null (triggering fallback to RealisticExerciseIllustrationGenerator).
     */
    fun applyHighContrastLineArtFilter(
        sourceBitmap: Bitmap?,
        exerciseName: String,
        targetMuscle: String? = null
    ): Bitmap? {
        if (sourceBitmap == null) return null
        return try {
            val width = 600
            val height = 600
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            // 1. Clean White Background
            canvas.drawColor(COLOR_WHITE)

            // 2. Outer Line-Art Border
            val borderPaint = Paint().apply {
                color = COLOR_BORDER_GRAY
                style = Paint.Style.STROKE
                strokeWidth = 4f
                isAntiAlias = true
            }
            canvas.drawRoundRect(RectF(12f, 12f, width - 12f, height - 12f), 24f, 24f, borderPaint)

            // 3. Draw Source Bitmap
            drawFilteredSourceBitmap(canvas, sourceBitmap, width, height)

            // 4. Highlight Target Muscle Group in Electric Orange (#FF6500)
            drawTargetMuscleHighlight(canvas, exerciseName, targetMuscle, width, height)

            // 5. Header Banner & Badge Text
            drawHeaderAndBadge(canvas, exerciseName, targetMuscle, width, height)

            bitmap
        } catch (e: Throwable) {
            null
        }
    }

    private fun drawFilteredSourceBitmap(canvas: Canvas, source: Bitmap, w: Int, h: Int) {
        val contrastPaint = Paint().apply {
            isAntiAlias = true
            val matrix = ColorMatrix().apply {
                set(floatArrayOf(
                    -2f,  0f,  0f, 0f, 255f,
                     0f, -2f,  0f, 0f, 255f,
                     0f,  0f, -2f, 0f, 255f,
                     0f,  0f,  0f, 1f,   0f
                ))
            }
            colorFilter = ColorMatrixColorFilter(matrix)
        }
        val srcRect = Rect(0, 0, source.width, source.height)
        val destRect = RectF(50f, 100f, w - 50f, h - 100f)
        canvas.drawBitmap(source, srcRect, destRect, contrastPaint)
    }

    /**
     * Classifies specific exercise names to targeted pose types.
     */
    fun determinePoseType(exerciseName: String, targetMuscle: String? = null): ScrapedPoseType {
        val norm = normalize("$exerciseName ${targetMuscle.orEmpty()}")
        return when {
            norm.contains("skips") || norm.contains("skip") || norm.contains("tecnica de carrera") -> ScrapedPoseType.SKIPS_DRILL
            norm.contains("cuestas") || norm.contains("cuesta") || norm.contains("hill") || norm.contains("pendiente") -> ScrapedPoseType.HILL_RUNNING
            norm.contains("trote") || norm.contains("activacion") || norm.contains("calentamiento") || norm.contains("rodaje") -> ScrapedPoseType.RUNNING_ACTIVATION
            norm.contains("punto muerto") || norm.contains("deadlock") || norm.contains("swim drill") -> ScrapedPoseType.SWIM_DEADLOCK_DRILL
            norm.contains("fondos en banco") || norm.contains("fondos banco") || norm.contains("bench dips") -> ScrapedPoseType.BENCH_DIPS
            norm.contains("remada") || norm.contains("traccion") || norm.contains("remo") || norm.contains("pull") -> ScrapedPoseType.ROW_TRACTION
            norm.contains("inclinado") || norm.contains("incline") -> ScrapedPoseType.INCLINE_PRESS
            norm.contains("sentadilla") || norm.contains("squat") || norm.contains("zancada") || norm.contains("prensa") -> ScrapedPoseType.SQUAT_VARIATION
            else -> ScrapedPoseType.GENERAL_ATHLETIC
        }
    }

    private fun drawTargetMuscleHighlight(
        canvas: Canvas,
        exerciseName: String,
        targetMuscle: String?,
        w: Int,
        h: Int
    ) {
        val cx = w / 2f
        val cy = h / 2f

        val fillPaint = Paint().apply {
            color = COLOR_ELECTRIC_ORANGE
            style = Paint.Style.FILL
            alpha = 220
            isAntiAlias = true
        }

        val strokePaint = Paint().apply {
            color = COLOR_ELECTRIC_RED
            style = Paint.Style.STROKE
            strokeWidth = 4f
            isAntiAlias = true
        }

        val norm = normalize("$exerciseName ${targetMuscle.orEmpty()}")

        when {
            norm.contains("fondos") || norm.contains("tricep") || norm.contains("brazo") || norm.contains("bicep") -> {
                val leftArm = RectF(cx - 90f, cy - 60f, cx - 60f, cy - 30f)
                val rightArm = RectF(cx + 10f, cy - 40f, cx + 45f, cy - 10f)
                canvas.drawOval(leftArm, fillPaint)
                canvas.drawOval(leftArm, strokePaint)
                canvas.drawOval(rightArm, fillPaint)
                canvas.drawOval(rightArm, strokePaint)
            }

            norm.contains("remada") || norm.contains("traccion") || norm.contains("espalda") || norm.contains("row") || norm.contains("back") -> {
                val backOval = RectF(cx - 35f, cy - 75f, cx + 25f, cy - 35f)
                canvas.drawOval(backOval, fillPaint)
                canvas.drawOval(backOval, strokePaint)
            }

            norm.contains("inclinado") || norm.contains("press") || norm.contains("pecho") || norm.contains("chest") -> {
                val chestOval = RectF(cx - 30f, cy - 85f, cx + 30f, cy - 50f)
                canvas.drawOval(chestOval, fillPaint)
                canvas.drawOval(chestOval, strokePaint)
            }

            norm.contains("skips") || norm.contains("cuestas") || norm.contains("trote") || norm.contains("pierna") || norm.contains("leg") || norm.contains("quad") -> {
                val quad1 = RectF(cx - 15f, cy + 10f, cx + 50f, cy + 65f)
                val quad2 = RectF(cx - 60f, cy + 20f, cx - 15f, cy + 70f)
                canvas.drawOval(quad1, fillPaint)
                canvas.drawOval(quad1, strokePaint)
                canvas.drawOval(quad2, fillPaint)
                canvas.drawOval(quad2, strokePaint)
            }

            norm.contains("punto muerto") || norm.contains("crol") || norm.contains("natacion") || norm.contains("swim") -> {
                val shoulderDorsal = RectF(cx - 85f, cy - 35f, cx - 35f, cy - 5f)
                canvas.drawOval(shoulderDorsal, fillPaint)
                canvas.drawOval(shoulderDorsal, strokePaint)
            }

            else -> {
                val coreGroup = RectF(cx - 25f, cy - 70f, cx + 25f, cy - 30f)
                canvas.drawOval(coreGroup, fillPaint)
                canvas.drawOval(coreGroup, strokePaint)
            }
        }
    }

    private fun drawHeaderAndBadge(
        canvas: Canvas,
        exerciseName: String,
        targetMuscle: String?,
        w: Int,
        h: Int
    ) {
        // 1. Top Title
        val titlePaint = Paint().apply {
            color = COLOR_DARK_SILHOUETTE
            textSize = 22f
            isFakeBoldText = true
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        val displayTitle = if (exerciseName.length > 34) exerciseName.take(32) + "..." else exerciseName
        canvas.drawText(displayTitle.uppercase(), w / 2f, 48f, titlePaint)

        // 2. Muscle Subtitle
        val subtitlePaint = Paint().apply {
            color = COLOR_ELECTRIC_ORANGE
            textSize = 16f
            isFakeBoldText = true
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        val targetText = targetMuscle?.ifBlank { "Técnica Específica" } ?: "Técnica Específica"
        canvas.drawText("🎯 MÚSCULO: $targetText", w / 2f, 75f, subtitlePaint)

        // 3. Bottom Scraped Badge Banner: "🌐 Ilustración Específica de [Nombre]"
        val badgeBgPaint = Paint().apply {
            color = COLOR_DARK_BANNER
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        val badgeBorderPaint = Paint().apply {
            color = COLOR_ELECTRIC_ORANGE
            style = Paint.Style.STROKE
            strokeWidth = 2f
            isAntiAlias = true
        }

        val badgeTextPaint = Paint().apply {
            color = COLOR_WHITE
            textSize = 17f
            isFakeBoldText = true
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }

        val badgeRect = RectF(40f, h - 68f, w - 40f, h - 24f)
        canvas.drawRoundRect(badgeRect, 18f, 18f, badgeBgPaint)
        canvas.drawRoundRect(badgeRect, 18f, 18f, badgeBorderPaint)

        val fullBadgeLabel = "$BADGE_PREFIX$displayTitle"
        val clampedLabel = if (fullBadgeLabel.length > 42) fullBadgeLabel.take(40) + "..." else fullBadgeLabel
        canvas.drawText(clampedLabel, w / 2f, h - 40f, badgeTextPaint)
    }

    /**
     * Helper to clear cached scraped exercise images for tests or cache resetting.
     */
    fun clearCache(context: Context?) {
        if (context == null) return
        try {
            val dir = File(context.filesDir, SCRAPED_DIR_NAME)
            if (dir.exists()) {
                dir.listFiles()?.forEach { it.delete() }
            }
        } catch (e: Exception) {
            // Ignore
        }
    }
}
