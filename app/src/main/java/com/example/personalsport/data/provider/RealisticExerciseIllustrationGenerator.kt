package com.example.personalsport.data.provider

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.text.Normalizer
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * Detailed Anatomical Human Muscle Vector Path & 3D Grayscale Shading Generator.
 *
 * Renders realistic human anatomical muscle vector paths (`Path`) with 3D grayscale shading
 * and electric red/orange active target muscle highlights with muscle fiber striations:
 * - Anatomical Vector Path Engine: Detailed Head/Neck profile, Torso/Chest (Pectoral, Lats, Abdominals),
 *   Muscular Arms (Deltoid, Biceps, Triceps, Forearms), and Muscular Legs (Quads, Hamstrings, Glutes, Calves).
 * - 3D Grayscale Shading & Fiber Striations: Linear and Radial Gradients for 3D body volume, with inner
 *   muscle fiber striation paths (#FF3D00 / #FF6500 / #FF8A65) on target muscle groups.
 * - Exercise Postures:
 *     - Dominadas / Pull-Ups: Athlete hanging from bar pulling up with flexed lats & biceps.
 *     - Salto de Cuerda / Comba: Mid-jump posture with jump rope arc and highlighted calves & quads.
 *     - Sentadillas / Squats: Deep 90° squat posture with barbell and highlighted quads & glutes.
 *     - Press de Banca: Lying on bench pressing barbell with highlighted pectorals & triceps.
 *     - Abdominales / Crunches: Flexed core crunch position with highlighted red 6-pack abdominal pack.
 *     - Carrera / Sprints: Sprint stride phase with highlighted quads, hamstrings, and calves.
 *     - Natación: Streamlined swim stroke with highlighted back, shoulders, and arms.
 * - Local Cache Storage: Saves high-res PNG to local cache (`cache/realistic_exercise_[hash].png`).
 */
object RealisticExerciseIllustrationGenerator {

    const val BADGE_TEXT = "🏋️ Ilustración Biomecánica Realista 3D"
    const val CACHE_PREFIX = "realistic_exercise_"

    // Color definitions
    const val COLOR_BACKGROUND_WHITE = 0xFFFFFFFF.toInt()
    const val COLOR_PRIMARY_HIGHLIGHT_RED = 0xFFFF3D00.toInt()   // #FF3D00 Rich Red
    const val COLOR_PRIMARY_HIGHLIGHT_ORANGE = 0xFFFF6500.toInt()// #FF6500 Rich Orange
    const val COLOR_STRIATION_HIGHLIGHT = 0xFFFF8A65.toInt()     // Electric Fiber Light
    const val COLOR_BODY_LIGHT_GRAY = 0xFFE2E8F0.toInt()         // 3D Grayscale Highlight
    const val COLOR_BODY_MID_GRAY = 0xFF94A3B8.toInt()           // 3D Grayscale Midtone
    const val COLOR_BODY_DARK_GRAY = 0xFF475569.toInt()          // 3D Grayscale Shadow
    const val COLOR_BODY_SHADOW_DEEP = 0xFF1E293B.toInt()        // Deep shadow contour
    const val COLOR_BORDER_SLATE = 0xFFCBD5E1.toInt()
    const val COLOR_BADGE_DARK = 0xFF1C2229.toInt()

    enum class PoseType {
        PULL_UP,
        SQUAT,
        BENCH_PRESS,
        JUMP_ROPE,
        RUNNING,
        SWIMMING,
        DEADLIFT,
        SHOULDER_PRESS,
        BICEP_CURL,
        ABS_CRUNCH,
        GENERAL_ATHLETIC
    }

    enum class MuscleGroup {
        CHEST,
        BACK,
        LEGS,
        SHOULDERS,
        ARMS,
        ABS,
        GLUTES,
        CALVES,
        CARDIO,
        FULL_BODY
    }

    /**
     * Checks whether an image URL string points to a Realistic Exercise Illustration.
     */
    fun isRealisticIllustration(url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        return url.contains(CACHE_PREFIX, ignoreCase = true)
    }

    /**
     * Normalizes text string for safe cache filenames and matching.
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
     * Generates an MD5 hash for cached file naming.
     */
    fun getHash(exerciseName: String, targetMuscle: String? = null): String {
        val normName = normalize(exerciseName)
        val normMuscle = normalize(targetMuscle.orEmpty())
        val input = "realistic_exercise_${normName}_$normMuscle"
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
     * Determines athletic pose type from exercise name and target muscle hint.
     */
    fun determinePose(exerciseName: String, targetMuscle: String? = null): PoseType {
        val combined = normalize("$exerciseName ${targetMuscle.orEmpty()}")
        return when {
            combined.contains("cuerda") || combined.contains("jump rope") || combined.contains("comba") || combined.contains("salto de cuerda") -> PoseType.JUMP_ROPE
            combined.contains("pull") || combined.contains("dominada") || combined.contains("jalon") || combined.contains("pulldown") || combined.contains("row") || combined.contains("remo") -> PoseType.PULL_UP
            combined.contains("squat") || combined.contains("sentadilla") || combined.contains("prensa") || combined.contains("zancada") || combined.contains("lunge") -> PoseType.SQUAT
            combined.contains("bench") || combined.contains("banca") || (combined.contains("press") && (combined.contains("pecho") || combined.contains("chest") || combined.contains("plano"))) || combined.contains("pushup") || combined.contains("flexion") -> PoseType.BENCH_PRESS
            combined.contains("swim") || combined.contains("natacion") || combined.contains("crol") || combined.contains("piscina") || combined.contains("agua") -> PoseType.SWIMMING
            combined.contains("abs") || combined.contains("crunch") || combined.contains("situp") || combined.contains("abdo") || combined.contains("plancha") -> PoseType.ABS_CRUNCH
            combined.contains("carrera") || combined.contains("trote") || combined.contains("sprint") || combined.contains("atletismo") || combined.contains("fartlek") || combined.contains("rodaje") || combined.contains("running") || combined.contains(Regex("\\brun\\b")) -> PoseType.RUNNING
            combined.contains("deadlift") || combined.contains("muerto") || combined.contains("clean") || combined.contains("snatch") -> PoseType.DEADLIFT
            combined.contains("militar") || combined.contains("overhead") || combined.contains("shoulder") || combined.contains("hombro") -> PoseType.SHOULDER_PRESS
            combined.contains("curl") || combined.contains("bicep") -> PoseType.BICEP_CURL
            else -> PoseType.GENERAL_ATHLETIC
        }
    }

    /**
     * Determines primary target muscle group.
     */
    fun determineMuscleGroup(exerciseName: String, targetMuscle: String? = null): MuscleGroup {
        val combined = normalize("$exerciseName ${targetMuscle.orEmpty()}")
        return when {
            combined.contains("pecho") || combined.contains("chest") || combined.contains("pectoral") || combined.contains("bench") -> MuscleGroup.CHEST
            combined.contains("espalda") || combined.contains("back") || combined.contains("dorsal") || combined.contains("lat") || combined.contains("row") || combined.contains("pull") -> MuscleGroup.BACK
            combined.contains("pierna") || combined.contains("leg") || combined.contains("quad") || combined.contains("cuadricep") || combined.contains("squat") -> MuscleGroup.LEGS
            combined.contains("pantorrilla") || combined.contains("calf") || combined.contains("calves") || combined.contains("gemelo") -> MuscleGroup.CALVES
            combined.contains("hombro") || combined.contains("shoulder") || combined.contains("delt") || combined.contains("overhead") || combined.contains("militar") -> MuscleGroup.SHOULDERS
            combined.contains("brazo") || combined.contains("arm") || combined.contains("bicep") || combined.contains("tricep") || combined.contains("curl") || combined.contains("dip") -> MuscleGroup.ARMS
            combined.contains("abdo") || combined.contains("abs") || combined.contains("core") || combined.contains("crunch") || combined.contains("situp") || combined.contains("plancha") -> MuscleGroup.ABS
            combined.contains("glute") || combined.contains("isquio") || combined.contains("femoral") || combined.contains("hamstring") -> MuscleGroup.GLUTES
            combined.contains("cardio") || combined.contains("carrera") || combined.contains("swim") || combined.contains("natacion") || combined.contains("comba") || combined.contains("cuerda") || combined.contains("running") || combined.contains(Regex("\\brun\\b")) -> MuscleGroup.CARDIO
            else -> MuscleGroup.FULL_BODY
        }
    }

    /**
     * Generates a realistic 3D/grayscale shaded athlete illustration for an exercise and saves to cache.
     */
    fun generateRealisticIllustration(
        context: Context?,
        exerciseName: String,
        targetMuscle: String? = null
    ): String {
        val cleanName = exerciseName.ifBlank { "Ejercicio" }
        val hash = getHash(cleanName, targetMuscle)
        val fileName = "${CACHE_PREFIX}${hash}.png"

        val cacheDir = if (context != null) {
            context.cacheDir
        } else {
            File("/data/user/0/com.example.personalsport/cache")
        }

        val cacheFile = File(cacheDir, fileName)

        // Return existing cached file if non-empty
        if (cacheFile.exists() && cacheFile.length() > 0) {
            return "file://${cacheFile.absolutePath}"
        }

        return try {
            if (!cacheDir.exists()) {
                cacheDir.mkdirs()
            }

            val width = 800
            val height = 800
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            renderRealisticAthleteIllustration(canvas, width, height, cleanName, targetMuscle)

            FileOutputStream(cacheFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }

            "file://${cacheFile.absolutePath}"
        } catch (e: Exception) {
            "file:///data/user/0/com.example.personalsport/cache/$fileName"
        }
    }

    private fun renderRealisticAthleteIllustration(
        canvas: Canvas,
        w: Int,
        h: Int,
        exerciseName: String,
        targetMuscle: String?
    ) {
        val pose = determinePose(exerciseName, targetMuscle)
        val muscleGroup = determineMuscleGroup(exerciseName, targetMuscle)

        // 1. Clean White Background (#FFFFFF)
        val bgPaint = Paint().apply {
            color = COLOR_BACKGROUND_WHITE
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), bgPaint)

        // Outer subtle border
        val borderPaint = Paint().apply {
            color = COLOR_BORDER_SLATE
            style = Paint.Style.STROKE
            strokeWidth = 4f
            isAntiAlias = true
        }
        val borderRect = RectF(16f, 16f, w - 16f, h - 16f)
        canvas.drawRoundRect(borderRect, 28f, 28f, borderPaint)

        // 2. Top Header Title & Target Muscle Subtitle
        val titlePaint = Paint().apply {
            color = COLOR_BODY_SHADOW_DEEP
            textSize = 28f
            isFakeBoldText = true
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        val displayTitle = if (exerciseName.length > 32) exerciseName.take(30) + "..." else exerciseName
        canvas.drawText(displayTitle.uppercase(), w / 2f, 58f, titlePaint)

        val muscleNameText = targetMuscle?.ifBlank { null } ?: when (muscleGroup) {
            MuscleGroup.CHEST -> "Pectorales / Pecho"
            MuscleGroup.BACK -> "Dorsal Ancho / Espalda"
            MuscleGroup.LEGS -> "Cuádriceps / Piernas"
            MuscleGroup.CALVES -> "Pantorrillas / Gemelos"
            MuscleGroup.SHOULDERS -> "Deltoides / Hombros"
            MuscleGroup.ARMS -> "Bícep / Trícep / Brazos"
            MuscleGroup.ABS -> "Recto Abdominal / Core"
            MuscleGroup.GLUTES -> "Glúteos / Femoral"
            MuscleGroup.CARDIO -> "Capacidad Cardiovascular"
            MuscleGroup.FULL_BODY -> "Cuerpo Completo"
        }

        val subtitlePaint = Paint().apply {
            color = COLOR_PRIMARY_HIGHLIGHT_ORANGE
            textSize = 20f
            isFakeBoldText = true
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("🎯 MÚSCULO OBJETIVO: $muscleNameText", w / 2f, 92f, subtitlePaint)

        // 3. Render Anatomical 3D Vector Shaded Athlete Body Pose
        draw3DShadedAthletePose(canvas, pose, muscleGroup, w, h)

        // 4. Render Bottom Badge Banner
        val badgeBgPaint = Paint().apply {
            color = COLOR_BADGE_DARK
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        val badgeBorderPaint = Paint().apply {
            color = COLOR_PRIMARY_HIGHLIGHT_ORANGE
            style = Paint.Style.STROKE
            strokeWidth = 2f
            isAntiAlias = true
        }

        val badgeTextPaint = Paint().apply {
            color = COLOR_BACKGROUND_WHITE
            textSize = 20f
            isFakeBoldText = true
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }

        val badgeRect = RectF(80f, h - 80f, w - 80f, h - 28f)
        canvas.drawRoundRect(badgeRect, 22f, 22f, badgeBgPaint)
        canvas.drawRoundRect(badgeRect, 22f, 22f, badgeBorderPaint)
        canvas.drawText(BADGE_TEXT, w / 2f, h - 46f, badgeTextPaint)
    }

    // =========================================================================
    // ANATOMICAL SHADING & VECTOR PATH ENGINE
    // =========================================================================

    private fun create3DGradientShader(
        x1: Float, y1: Float, x2: Float, y2: Float,
        isHighlighted: Boolean
    ): Shader {
        return if (isHighlighted) {
            LinearGradient(
                x1, y1, x2, y2,
                intArrayOf(COLOR_PRIMARY_HIGHLIGHT_ORANGE, COLOR_PRIMARY_HIGHLIGHT_RED, 0xFFD81B60.toInt()),
                floatArrayOf(0.0f, 0.65f, 1.0f),
                Shader.TileMode.CLAMP
            )
        } else {
            LinearGradient(
                x1, y1, x2, y2,
                intArrayOf(COLOR_BODY_LIGHT_GRAY, COLOR_BODY_MID_GRAY, COLOR_BODY_DARK_GRAY),
                floatArrayOf(0.0f, 0.5f, 1.0f),
                Shader.TileMode.CLAMP
            )
        }
    }

    private fun getContourStrokePaint(isHighlighted: Boolean, width: Float = 3f): Paint {
        return Paint().apply {
            color = if (isHighlighted) COLOR_PRIMARY_HIGHLIGHT_RED else COLOR_BODY_SHADOW_DEEP
            style = Paint.Style.STROKE
            strokeWidth = width
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            isAntiAlias = true
        }
    }

    private fun getStriationPaint(isHighlighted: Boolean): Paint {
        return Paint().apply {
            color = if (isHighlighted) COLOR_STRIATION_HIGHLIGHT else 0x40FFFFFF
            style = Paint.Style.FILL_AND_STROKE
            strokeWidth = 1.8f
            strokeCap = Paint.Cap.ROUND
            isAntiAlias = true
        }
    }

    /**
     * Renders detailed Anatomical Head & Neck vector path with facial profile, jawline, chin, and neck muscles.
     */
    private fun drawAnatomicalHeadAndNeck(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        headRadius: Float = 32f,
        facingAngle: Float = 0f, // 0 = front/quarter, 90 = right profile
        isFacingRight: Boolean = true
    ) {
        val headPath = Path()
        val dir = if (isFacingRight) 1f else -1f

        if (facingAngle > 45f) {
            // Profile view with facial jawline, nose bump, chin contour, occipital curve
            headPath.moveTo(cx - 10f * dir, cy - headRadius * 1.1f)
            // Cranial forehead
            headPath.cubicTo(
                cx + headRadius * 0.5f * dir, cy - headRadius * 1.2f,
                cx + headRadius * 0.9f * dir, cy - headRadius * 0.5f,
                cx + headRadius * 0.8f * dir, cy - headRadius * 0.2f
            )
            // Nose bump
            headPath.lineTo(cx + headRadius * 1.05f * dir, cy - headRadius * 0.05f)
            headPath.lineTo(cx + headRadius * 0.75f * dir, cy + headRadius * 0.1f)
            // Upper lip & Mouth
            headPath.lineTo(cx + headRadius * 0.8f * dir, cy + headRadius * 0.25f)
            // Chin
            headPath.cubicTo(
                cx + headRadius * 0.85f * dir, cy + headRadius * 0.5f,
                cx + headRadius * 0.6f * dir, cy + headRadius * 0.65f,
                cx + headRadius * 0.3f * dir, cy + headRadius * 0.55f
            )
            // Jawline & Ear bump
            headPath.lineTo(cx - headRadius * 0.2f * dir, cy + headRadius * 0.35f)
            headPath.lineTo(cx - headRadius * 0.1f * dir, cy + headRadius * 0.05f) // Ear
            // Back of skull & Neck (Sternocleidomastoid) insertion
            headPath.cubicTo(
                cx - headRadius * 0.8f * dir, cy + headRadius * 0.2f,
                cx - headRadius * 1.0f * dir, cy - headRadius * 0.6f,
                cx - 10f * dir, cy - headRadius * 1.1f
            )
            headPath.close()
        } else {
            // Frontal / Quarter anatomical head
            headPath.addCircle(cx, cy - 4f, headRadius, Path.Direction.CW)
            // Jawline chin addition
            val jawPath = Path().apply {
                moveTo(cx - headRadius * 0.7f, cy)
                cubicTo(
                    cx - headRadius * 0.6f, cy + headRadius * 0.8f,
                    cx - headRadius * 0.2f, cy + headRadius * 1.05f,
                    cx, cy + headRadius * 1.1f
                )
                cubicTo(
                    cx + headRadius * 0.2f, cy + headRadius * 1.05f,
                    cx + headRadius * 0.6f, cy + headRadius * 0.8f,
                    cx + headRadius * 0.7f, cy
                )
                close()
            }
            headPath.op(jawPath, Path.Op.UNION)
        }

        // 3D Grayscale Shading
        val fillPaint = Paint().apply {
            shader = RadialGradient(
                cx - headRadius * 0.3f, cy - headRadius * 0.4f, headRadius * 1.8f,
                intArrayOf(COLOR_BODY_LIGHT_GRAY, COLOR_BODY_MID_GRAY, COLOR_BODY_DARK_GRAY),
                floatArrayOf(0.0f, 0.55f, 1.0f),
                Shader.TileMode.CLAMP
            )
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        canvas.drawPath(headPath, fillPaint)
        canvas.drawPath(headPath, getContourStrokePaint(false, 3f))

        // Neck Muscle Contours (Sternocleidomastoid & Trapezius neck bands)
        val neckPath = Path().apply {
            moveTo(cx - headRadius * 0.4f, cy + headRadius * 0.6f)
            cubicTo(
                cx - headRadius * 0.3f, cy + headRadius * 1.1f,
                cx - headRadius * 0.5f, cy + headRadius * 1.4f,
                cx - headRadius * 0.8f, cy + headRadius * 1.6f
            )
            lineTo(cx + headRadius * 0.8f, cy + headRadius * 1.6f)
            cubicTo(
                cx + headRadius * 0.5f, cy + headRadius * 1.4f,
                cx + headRadius * 0.3f, cy + headRadius * 1.1f,
                cx + headRadius * 0.4f, cy + headRadius * 0.6f
            )
            close()
        }

        val neckShader = LinearGradient(
            cx - headRadius, cy + headRadius * 0.6f, cx + headRadius, cy + headRadius * 1.6f,
            intArrayOf(COLOR_BODY_LIGHT_GRAY, COLOR_BODY_MID_GRAY, COLOR_BODY_DARK_GRAY),
            floatArrayOf(0.0f, 0.5f, 1.0f),
            Shader.TileMode.CLAMP
        )
        val neckPaint = Paint().apply {
            shader = neckShader
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawPath(neckPath, neckPaint)
        canvas.drawPath(neckPath, getContourStrokePaint(false, 2.5f))
    }

    /**
     * Renders detailed Anatomical Torso & Chest vector path with 3D shaded Pectoral, Lats (V-taper),
     * and Abdominal muscle contours (6-pack blocks, serratus, obliques) with muscle fiber striations.
     */
    private fun drawAnatomicalTorso(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        w: Float = 140f,
        h: Float = 180f,
        targetMuscle: MuscleGroup,
        isBackView: Boolean = false
    ) {
        val halfW = w / 2f
        val topY = cy - h / 2f
        val botY = cy + h / 2f

        // 1. Latissimus Dorsi (Lats) V-Taper Wings
        val isLatsActive = targetMuscle == MuscleGroup.BACK || targetMuscle == MuscleGroup.FULL_BODY
        val leftLatPath = Path().apply {
            moveTo(cx - halfW * 0.5f, topY + 20f)
            cubicTo(
                cx - halfW * 1.25f, topY + 30f,
                cx - halfW * 1.35f, topY + h * 0.45f,
                cx - halfW * 0.65f, botY - 20f
            )
            lineTo(cx - halfW * 0.4f, topY + h * 0.4f)
            close()
        }

        val rightLatPath = Path().apply {
            moveTo(cx + halfW * 0.5f, topY + 20f)
            cubicTo(
                cx + halfW * 1.25f, topY + 30f,
                cx + halfW * 1.35f, topY + h * 0.45f,
                cx + halfW * 0.65f, botY - 20f
            )
            lineTo(cx + halfW * 0.4f, topY + h * 0.4f)
            close()
        }

        val latShaderLeft = create3DGradientShader(cx - halfW * 1.3f, topY, cx - halfW * 0.4f, botY, isLatsActive)
        val latShaderRight = create3DGradientShader(cx + halfW * 0.4f, topY, cx + halfW * 1.3f, botY, isLatsActive)

        val latPaintLeft = Paint().apply { shader = latShaderLeft; style = Paint.Style.FILL; isAntiAlias = true }
        val latPaintRight = Paint().apply { shader = latShaderRight; style = Paint.Style.FILL; isAntiAlias = true }

        canvas.drawPath(leftLatPath, latPaintLeft)
        canvas.drawPath(leftLatPath, getContourStrokePaint(isLatsActive))
        canvas.drawPath(rightLatPath, latPaintRight)
        canvas.drawPath(rightLatPath, getContourStrokePaint(isLatsActive))

        // Lat Muscle Fiber Striations (Diagonal sweeping fibers running toward armpits)
        val latStriationPaint = getStriationPaint(isLatsActive)
        for (i in 1..5) {
            val offset = i * (h * 0.08f)
            canvas.drawLine(cx - halfW * 0.5f, topY + 30f + offset, cx - halfW * 1.15f, topY + 20f + offset * 0.7f, latStriationPaint)
            canvas.drawLine(cx + halfW * 0.5f, topY + 30f + offset, cx + halfW * 1.15f, topY + 20f + offset * 0.7f, latStriationPaint)
        }

        // 2. Central Torso Frame / Ribcage & Spine
        val torsoFrame = Path().apply {
            moveTo(cx - halfW * 0.7f, topY)
            lineTo(cx + halfW * 0.7f, topY)
            cubicTo(
                cx + halfW * 0.85f, topY + h * 0.3f,
                cx + halfW * 0.75f, topY + h * 0.7f,
                cx + halfW * 0.55f, botY
            )
            lineTo(cx - halfW * 0.55f, botY)
            cubicTo(
                cx - halfW * 0.75f, topY + h * 0.7f,
                cx - halfW * 0.85f, topY + h * 0.3f,
                cx - halfW * 0.7f, topY
            )
            close()
        }

        val frameShader = create3DGradientShader(cx - halfW, topY, cx + halfW, botY, false)
        val framePaint = Paint().apply { shader = frameShader; style = Paint.Style.FILL; isAntiAlias = true }
        canvas.drawPath(torsoFrame, framePaint)
        canvas.drawPath(torsoFrame, getContourStrokePaint(false))

        if (!isBackView) {
            // 3. Pectoral Muscles (Pectoralis Major Left & Right Fan Shapes)
            val isChestActive = targetMuscle == MuscleGroup.CHEST || targetMuscle == MuscleGroup.FULL_BODY
            val leftPecPath = Path().apply {
                moveTo(cx - 4f, topY + 15f)
                lineTo(cx - halfW * 0.75f, topY + 10f)
                cubicTo(
                    cx - halfW * 0.95f, topY + h * 0.2f,
                    cx - halfW * 0.85f, topY + h * 0.35f,
                    cx - halfW * 0.45f, topY + h * 0.38f
                )
                cubicTo(
                    cx - halfW * 0.25f, topY + h * 0.38f,
                    cx - 4f, topY + h * 0.32f,
                    cx - 4f, topY + 15f
                )
                close()
            }

            val rightPecPath = Path().apply {
                moveTo(cx + 4f, topY + 15f)
                lineTo(cx + halfW * 0.75f, topY + 10f)
                cubicTo(
                    cx + halfW * 0.95f, topY + h * 0.2f,
                    cx + halfW * 0.85f, topY + h * 0.35f,
                    cx + halfW * 0.45f, topY + h * 0.38f
                )
                cubicTo(
                    cx + halfW * 0.25f, topY + h * 0.38f,
                    cx + 4f, topY + h * 0.32f,
                    cx + 4f, topY + 15f
                )
                close()
            }

            val pecShaderLeft = create3DGradientShader(cx - halfW * 0.8f, topY + 10f, cx, topY + h * 0.38f, isChestActive)
            val pecShaderRight = create3DGradientShader(cx, topY + 10f, cx + halfW * 0.8f, topY + h * 0.38f, isChestActive)

            val pecPaintLeft = Paint().apply { shader = pecShaderLeft; style = Paint.Style.FILL; isAntiAlias = true }
            val pecPaintRight = Paint().apply { shader = pecShaderRight; style = Paint.Style.FILL; isAntiAlias = true }

            canvas.drawPath(leftPecPath, pecPaintLeft)
            canvas.drawPath(leftPecPath, getContourStrokePaint(isChestActive))
            canvas.drawPath(rightPecPath, pecPaintRight)
            canvas.drawPath(rightPecPath, getContourStrokePaint(isChestActive))

            // Pectoral Muscle Fiber Striations (Radiating from sternum toward armpits)
            val pecStriationPaint = getStriationPaint(isChestActive)
            for (i in 1..4) {
                val frac = i / 5f
                val startY = topY + 15f + frac * (h * 0.3f)
                canvas.drawLine(cx - 6f, startY, cx - halfW * 0.75f, topY + 15f + frac * (h * 0.15f), pecStriationPaint)
                canvas.drawLine(cx + 6f, startY, cx + halfW * 0.75f, topY + 15f + frac * (h * 0.15f), pecStriationPaint)
            }

            // 4. Abdominal Muscle Pack (Rectus Abdominis 6-pack / 8-pack blocks with Linea Alba) - Matching Image 1!
            val isAbsActive = targetMuscle == MuscleGroup.ABS || targetMuscle == MuscleGroup.FULL_BODY
            val absStartY = topY + h * 0.4f
            val absHeight = h * 0.55f
            val absWidth = halfW * 0.8f

            val absShader = create3DGradientShader(cx - absWidth / 2f, absStartY, cx + absWidth / 2f, absStartY + absHeight, isAbsActive)
            val absPaint = Paint().apply { shader = absShader; style = Paint.Style.FILL; isAntiAlias = true }
            val absStroke = getContourStrokePaint(isAbsActive, 2.5f)

            val rows = 4 // 8-pack blocks
            val blockH = absHeight / rows - 4f
            val blockW = absWidth / 2f - 4f

            for (r in 0 until rows) {
                val blockY = absStartY + r * (blockH + 4f)

                // Left block
                val leftRect = RectF(cx - 3f - blockW, blockY, cx - 3f, blockY + blockH)
                canvas.drawRoundRect(leftRect, 8f, 8f, absPaint)
                canvas.drawRoundRect(leftRect, 8f, 8f, absStroke)

                // Right block
                val rightRect = RectF(cx + 3f, blockY, cx + 3f + blockW, blockY + blockH)
                canvas.drawRoundRect(rightRect, 8f, 8f, absPaint)
                canvas.drawRoundRect(rightRect, 8f, 8f, absStroke)

                // Muscle fiber striation lines across abdominal blocks
                val absStriationPaint = getStriationPaint(isAbsActive)
                canvas.drawLine(cx - absWidth / 2f + 6f, blockY + blockH * 0.5f, cx - 8f, blockY + blockH * 0.5f, absStriationPaint)
                canvas.drawLine(cx + 8f, blockY + blockH * 0.5f, cx + absWidth / 2f - 6f, blockY + blockH * 0.5f, absStriationPaint)
            }

            // Linea Alba central line
            val albaLine = getContourStrokePaint(isAbsActive, 3f)
            canvas.drawLine(cx, absStartY - 5f, cx, absStartY + absHeight + 5f, albaLine)
        }
    }

    /**
     * Renders detailed Anatomical Arm vector path (Deltoid, Biceps, Triceps, Forearm) with muscle volume curves & striations.
     */
    private fun drawAnatomicalArm(
        canvas: Canvas,
        x1: Float, y1: Float,     // Shoulder joint
        x2: Float, y2: Float,     // Elbow joint
        x3: Float, y3: Float,     // Wrist joint
        isHighlighted: Boolean,
        muscleFocus: MuscleGroup = MuscleGroup.ARMS
    ) {
        // Deltoid (Shoulder Cap)
        val deltRadius = 24f
        val deltPath = Path().apply {
            addCircle(x1, y1, deltRadius, Path.Direction.CW)
        }
        val isShoulderActive = isHighlighted || muscleFocus == MuscleGroup.SHOULDERS
        val deltShader = create3DGradientShader(x1 - deltRadius, y1 - deltRadius, x1 + deltRadius, y1 + deltRadius, isShoulderActive)
        val deltPaint = Paint().apply { shader = deltShader; style = Paint.Style.FILL; isAntiAlias = true }

        canvas.drawPath(deltPath, deltPaint)
        canvas.drawPath(deltPath, getContourStrokePaint(isShoulderActive, 3f))

        // Deltoid Muscle Striations
        val deltStriationPaint = getStriationPaint(isShoulderActive)
        for (a in -45..45 step 22) {
            val rad = Math.toRadians(a.toDouble())
            val sx = x1 + (deltRadius * 0.3f * sin(rad)).toFloat()
            val sy = y1 + (deltRadius * 0.3f * cos(rad)).toFloat()
            val ex = x1 + (deltRadius * 0.85f * sin(rad)).toFloat()
            val ey = y1 + (deltRadius * 0.85f * cos(rad)).toFloat()
            canvas.drawLine(sx, sy, ex, ey, deltStriationPaint)
        }

        // Upper Arm (Biceps / Triceps muscle belly volume curve)
        val isArmActive = isHighlighted || muscleFocus == MuscleGroup.ARMS
        val armShader = create3DGradientShader(x1, y1, x2, y2, isArmActive)

        // Biceps & Triceps muscular contour path
        val dx1 = x2 - x1
        val dy1 = y2 - y1
        val len1 = Math.hypot(dx1.toDouble(), dy1.toDouble()).toFloat().coerceAtLeast(1f)
        val nx1 = -dy1 / len1 * 18f // Normal vector for muscle width bulge
        val ny1 = dx1 / len1 * 18f

        val upperArmPath = Path().apply {
            moveTo(x1 + nx1 * 0.8f, y1 + ny1 * 0.8f)
            // Biceps curve bulge
            quadTo(
                (x1 + x2) / 2f + nx1 * 1.4f, (y1 + y2) / 2f + ny1 * 1.4f,
                x2 + nx1 * 0.6f, y2 + ny1 * 0.6f
            )
            lineTo(x2 - nx1 * 0.6f, y2 - ny1 * 0.6f)
            // Triceps curve bulge
            quadTo(
                (x1 + x2) / 2f - nx1 * 1.4f, (y1 + y2) / 2f - ny1 * 1.4f,
                x1 - nx1 * 0.8f, y1 - ny1 * 0.8f
            )
            close()
        }

        val upperArmPaint = Paint().apply { shader = armShader; style = Paint.Style.FILL; isAntiAlias = true }
        canvas.drawPath(upperArmPath, upperArmPaint)
        canvas.drawPath(upperArmPath, getContourStrokePaint(isArmActive, 3f))

        // Longitudinal Biceps/Triceps Striations
        val armStriationPaint = getStriationPaint(isArmActive)
        for (f in floatArrayOf(-0.4f, 0.0f, 0.4f)) {
            canvas.drawLine(
                x1 + dx1 * 0.2f + nx1 * f, y1 + dy1 * 0.2f + ny1 * f,
                x1 + dx1 * 0.8f + nx1 * f, y1 + dy1 * 0.8f + ny1 * f,
                armStriationPaint
            )
        }

        // Elbow Joint
        val elbowPaint = getContourStrokePaint(isArmActive, 4f)
        canvas.drawCircle(x2, y2, 6f, elbowPaint)

        // Forearm (Brachioradialis muscular teardrop volume tapering to wrist)
        val dx2 = x3 - x2
        val dy2 = y3 - y2
        val len2 = Math.hypot(dx2.toDouble(), dy2.toDouble()).toFloat().coerceAtLeast(1f)
        val nx2 = -dy2 / len2 * 14f
        val ny2 = dx2 / len2 * 14f

        val forearmPath = Path().apply {
            moveTo(x2 + nx2 * 0.8f, y2 + ny2 * 0.8f)
            quadTo(
                x2 + dx2 * 0.35f + nx2 * 1.3f, y2 + dy2 * 0.35f + ny2 * 1.3f,
                x3 + nx2 * 0.4f, y3 + ny2 * 0.4f
            )
            lineTo(x3 - nx2 * 0.4f, y3 - ny2 * 0.4f)
            quadTo(
                x2 + dx2 * 0.35f - nx2 * 1.1f, y2 + dy2 * 0.35f - ny2 * 1.1f,
                x2 - nx2 * 0.8f, y2 - ny2 * 0.8f
            )
            close()
        }

        val forearmShader = create3DGradientShader(x2, y2, x3, y3, isArmActive)
        val forearmPaint = Paint().apply { shader = forearmShader; style = Paint.Style.FILL; isAntiAlias = true }

        canvas.drawPath(forearmPath, forearmPaint)
        canvas.drawPath(forearmPath, getContourStrokePaint(isArmActive, 3f))

        // Hand & Wrist contour
        val handPaint = Paint().apply {
            color = if (isArmActive) COLOR_PRIMARY_HIGHLIGHT_RED else COLOR_BODY_MID_GRAY
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawCircle(x3, y3, 8f, handPaint)
    }

    /**
     * Renders detailed Anatomical Leg vector path (Glutes, Quads, Hamstrings, Knees, Calves, Achilles, Feet).
     */
    private fun drawAnatomicalLeg(
        canvas: Canvas,
        x1: Float, y1: Float,     // Hip joint
        x2: Float, y2: Float,     // Knee joint
        x3: Float, y3: Float,     // Ankle joint
        isHighlighted: Boolean,
        muscleFocus: MuscleGroup = MuscleGroup.LEGS
    ) {
        val isLegActive = isHighlighted || muscleFocus == MuscleGroup.LEGS || muscleFocus == MuscleGroup.FULL_BODY
        val isCalfActive = isHighlighted || muscleFocus == MuscleGroup.CALVES || muscleFocus == MuscleGroup.CARDIO
        val isGluteActive = isHighlighted || muscleFocus == MuscleGroup.GLUTES

        // 1. Gluteus Maximus / Hip Muscle Contour
        val gluteRadius = 26f
        val glutePath = Path().apply {
            addCircle(x1, y1, gluteRadius, Path.Direction.CW)
        }
        val gluteShader = create3DGradientShader(x1 - gluteRadius, y1 - gluteRadius, x1 + gluteRadius, y1 + gluteRadius, isGluteActive || isLegActive)
        val glutePaint = Paint().apply { shader = gluteShader; style = Paint.Style.FILL; isAntiAlias = true }
        canvas.drawPath(glutePath, glutePaint)
        canvas.drawPath(glutePath, getContourStrokePaint(isGluteActive || isLegActive, 3f))

        // 2. Thigh / Cuádriceps & Hamstrings (Rectus Femoris, Vastus Lateralis & Medialis teardrop)
        val dx1 = x2 - x1
        val dy1 = y2 - y1
        val len1 = Math.hypot(dx1.toDouble(), dy1.toDouble()).toFloat().coerceAtLeast(1f)
        val nx1 = -dy1 / len1 * 26f // Thigh muscle width
        val ny1 = dx1 / len1 * 26f

        val thighPath = Path().apply {
            moveTo(x1 + nx1 * 0.8f, y1 + ny1 * 0.8f)
            // Vastus Lateralis outer quad muscular sweep
            quadTo(
                (x1 + x2) / 2f + nx1 * 1.35f, (y1 + y2) / 2f + ny1 * 1.35f,
                x2 + nx1 * 0.7f, y2 + ny1 * 0.7f
            )
            // Vastus Medialis teardrop above knee
            lineTo(x2 + nx1 * 0.4f, y2 + ny1 * 0.4f)
            lineTo(x2 - nx1 * 0.4f, y2 - ny1 * 0.4f)
            // Hamstrings posterior sweep
            quadTo(
                (x1 + x2) / 2f - nx1 * 1.2f, (y1 + y2) / 2f - ny1 * 1.2f,
                x1 - nx1 * 0.8f, y1 - ny1 * 0.8f
            )
            close()
        }

        val thighShader = create3DGradientShader(x1, y1, x2, y2, isLegActive)
        val thighPaint = Paint().apply { shader = thighShader; style = Paint.Style.FILL; isAntiAlias = true }

        canvas.drawPath(thighPath, thighPaint)
        canvas.drawPath(thighPath, getContourStrokePaint(isLegActive, 3f))

        // Quads Muscle Fiber Striations (Sweeping vertical lines)
        val quadStriationPaint = getStriationPaint(isLegActive)
        for (f in floatArrayOf(-0.4f, -0.1f, 0.2f, 0.5f)) {
            canvas.drawLine(
                x1 + dx1 * 0.15f + nx1 * f, y1 + dy1 * 0.15f + ny1 * f,
                x1 + dx1 * 0.85f + nx1 * (f * 0.7f), y1 + dy1 * 0.85f + ny1 * (f * 0.7f),
                quadStriationPaint
            )
        }

        // Patella Knee Cap
        val kneePaint = getContourStrokePaint(isLegActive, 4f)
        canvas.drawCircle(x2, y2, 8f, kneePaint)

        // 3. Calves / Lower Leg (Gastrocnemius Medial & Lateral heads tapering to Achilles)
        val dx2 = x3 - x2
        val dy2 = y3 - y2
        val len2 = Math.hypot(dx2.toDouble(), dy2.toDouble()).toFloat().coerceAtLeast(1f)
        val nx2 = -dy2 / len2 * 20f
        val ny2 = dx2 / len2 * 20f

        val calfPath = Path().apply {
            moveTo(x2 + nx2 * 0.6f, y2 + ny2 * 0.6f)
            // Gastrocnemius bulbous muscular head
            quadTo(
                x2 + dx2 * 0.35f + nx2 * 1.4f, y2 + dy2 * 0.35f + ny2 * 1.4f,
                x2 + dx2 * 0.65f + nx2 * 0.5f, y2 + dy2 * 0.65f + ny2 * 0.5f
            )
            // Achilles tendon narrowing to ankle
            lineTo(x3 + nx2 * 0.3f, y3 + ny2 * 0.3f)
            lineTo(x3 - nx2 * 0.3f, y3 - ny2 * 0.3f)
            quadTo(
                x2 + dx2 * 0.35f - nx2 * 1.2f, y2 + dy2 * 0.35f - ny2 * 1.2f,
                x2 - nx2 * 0.6f, y2 - ny2 * 0.6f
            )
            close()
        }

        val calfShader = create3DGradientShader(x2, y2, x3, y3, isCalfActive)
        val calfPaint = Paint().apply { shader = calfShader; style = Paint.Style.FILL; isAntiAlias = true }

        canvas.drawPath(calfPath, calfPaint)
        canvas.drawPath(calfPath, getContourStrokePaint(isCalfActive, 3f))

        // Calf Gastrocnemius Striation Lines
        val calfStriationPaint = getStriationPaint(isCalfActive)
        canvas.drawLine(
            x2 + dx2 * 0.2f + nx2 * 0.3f, y2 + dy2 * 0.2f + ny2 * 0.3f,
            x2 + dx2 * 0.55f + nx2 * 0.2f, y2 + dy2 * 0.55f + ny2 * 0.2f,
            calfStriationPaint
        )
        canvas.drawLine(
            x2 + dx2 * 0.2f - nx2 * 0.3f, y2 + dy2 * 0.2f - ny2 * 0.3f,
            x2 + dx2 * 0.55f - nx2 * 0.2f, y2 + dy2 * 0.55f - ny2 * 0.2f,
            calfStriationPaint
        )

        // Foot / Ankle Joint
        val footPaint = Paint().apply {
            color = if (isCalfActive) COLOR_PRIMARY_HIGHLIGHT_RED else COLOR_BODY_MID_GRAY
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawCircle(x3, y3, 9f, footPaint)
    }

    // =========================================================================
    // SPECIFIC EXERCISE POSTURES RENDERER
    // =========================================================================

    private fun draw3DShadedAthletePose(
        canvas: Canvas,
        pose: PoseType,
        targetMuscle: MuscleGroup,
        w: Int,
        h: Int
    ) {
        val cx = w / 2f
        val cy = h / 2f + 10f

        when (pose) {
            PoseType.PULL_UP -> {
                // Dominadas / Pull-Ups: Athlete hanging from bar pulling up with arms flexed and highlighted lats & biceps
                val barPaint = Paint().apply {
                    shader = LinearGradient(cx - 220f, 0f, cx + 220f, 0f, 0xFF64748B.toInt(), 0xFF0F172A.toInt(), Shader.TileMode.CLAMP)
                    style = Paint.Style.FILL
                    isAntiAlias = true
                }
                // Metallic Pull-Up Bar
                canvas.drawRoundRect(RectF(cx - 230f, cy - 250f, cx + 230f, cy - 232f), 8f, 8f, barPaint)

                // Head between hands looking slightly up
                drawAnatomicalHeadAndNeck(canvas, cx, cy - 190f, 30f, 0f)

                // Torso with wide flexed Lats & Back V-Taper
                drawAnatomicalTorso(canvas, cx, cy - 80f, 150f, 170f, targetMuscle, isBackView = true)

                // Arms flexed pulling up to bar (Shoulder -> Elbow flared down -> Wrist gripping bar)
                drawAnatomicalArm(canvas, cx - 65f, cy - 150f, cx - 110f, cy - 180f, cx - 150f, cy - 241f, targetMuscle == MuscleGroup.ARMS || targetMuscle == MuscleGroup.BACK, targetMuscle)
                drawAnatomicalArm(canvas, cx + 65f, cy - 150f, cx + 110f, cy - 180f, cx + 150f, cy - 241f, targetMuscle == MuscleGroup.ARMS || targetMuscle == MuscleGroup.BACK, targetMuscle)

                // Legs hanging down with flexed knees
                drawAnatomicalLeg(canvas, cx - 35f, cy + 5f, cx - 30f, cy + 110f, cx - 25f, cy + 200f, targetMuscle == MuscleGroup.LEGS, targetMuscle)
                drawAnatomicalLeg(canvas, cx + 35f, cy + 5f, cx + 30f, cy + 110f, cx + 25f, cy + 200f, targetMuscle == MuscleGroup.LEGS, targetMuscle)
            }

            PoseType.JUMP_ROPE -> {
                // Salto de Cuerda / Comba: Athlete in mid-jump with jump rope arc and highlighted calves/quads
                val groundLine = Paint().apply {
                    color = COLOR_BORDER_SLATE
                    style = Paint.Style.STROKE
                    strokeWidth = 3f
                    isAntiAlias = true
                }
                canvas.drawLine(cx - 200f, cy + 185f, cx + 200f, cy + 185f, groundLine)

                // Head
                drawAnatomicalHeadAndNeck(canvas, cx, cy - 165f, 30f, 0f)

                // Torso erect
                drawAnatomicalTorso(canvas, cx, cy - 65f, 130f, 160f, targetMuscle)

                // Arms at sides holding handles
                drawAnatomicalArm(canvas, cx - 55f, cy - 120f, cx - 90f, cy - 60f, cx - 100f, cy - 10f, targetMuscle == MuscleGroup.ARMS, targetMuscle)
                drawAnatomicalArm(canvas, cx + 55f, cy - 120f, cx + 90f, cy - 60f, cx + 100f, cy - 10f, targetMuscle == MuscleGroup.ARMS, targetMuscle)

                // Mid-air Legs (Knees slightly flexed, ankle extended)
                drawAnatomicalLeg(canvas, cx - 28f, cy + 15f, cx - 25f, cy + 95f, cx - 18f, cy + 165f, targetMuscle == MuscleGroup.LEGS || targetMuscle == MuscleGroup.CALVES || targetMuscle == MuscleGroup.CARDIO, targetMuscle)
                drawAnatomicalLeg(canvas, cx + 28f, cy + 15f, cx + 25f, cy + 95f, cx + 18f, cy + 165f, targetMuscle == MuscleGroup.LEGS || targetMuscle == MuscleGroup.CALVES || targetMuscle == MuscleGroup.CARDIO, targetMuscle)

                // Curved Jump Rope Arc (Overhead and passing under hovering feet)
                val ropePath = Path().apply {
                    moveTo(cx - 100f, cy - 10f)
                    cubicTo(cx - 220f, cy - 240f, cx + 220f, cy - 240f, cx + 100f, cy - 10f)
                    cubicTo(cx + 200f, cy + 220f, cx - 200f, cy + 220f, cx - 100f, cy - 10f)
                }
                val ropePaint = Paint().apply {
                    color = COLOR_PRIMARY_HIGHLIGHT_ORANGE
                    style = Paint.Style.STROKE
                    strokeWidth = 5f
                    isAntiAlias = true
                }
                canvas.drawPath(ropePath, ropePaint)
            }

            PoseType.SQUAT -> {
                // Sentadillas / Squats: Athlete in deep squat posture (90° knee flex) with highlighted quads/glutes
                val barPaint = Paint().apply {
                    shader = LinearGradient(cx - 180f, 0f, cx + 180f, 0f, 0xFF475569.toInt(), 0xFF0F172A.toInt(), Shader.TileMode.CLAMP)
                    style = Paint.Style.FILL
                    isAntiAlias = true
                }
                // Barbell resting across shoulders
                canvas.drawRoundRect(RectF(cx - 200f, cy - 105f, cx + 200f, cy - 88f), 8f, 8f, barPaint)
                canvas.drawCircle(cx - 200f, cy - 96f, 26f, barPaint)
                canvas.drawCircle(cx + 200f, cy - 96f, 26f, barPaint)

                // Head angled forward
                drawAnatomicalHeadAndNeck(canvas, cx - 25f, cy - 130f, 28f, 60f, isFacingRight = false)

                // Torso leaning forward ~45°
                drawAnatomicalTorso(canvas, cx - 10f, cy - 50f, 135f, 150f, targetMuscle)

                // Arms gripping bar
                drawAnatomicalArm(canvas, cx - 50f, cy - 95f, cx - 75f, cy - 55f, cx - 55f, cy - 90f, false, targetMuscle)
                drawAnatomicalArm(canvas, cx + 30f, cy - 95f, cx + 55f, cy - 55f, cx + 35f, cy - 90f, false, targetMuscle)

                // Deep 90° Squat Thighs & Calves (Hip -> Knee horizontal -> Ankle vertical to floor)
                drawAnatomicalLeg(canvas, cx + 25f, cy - 10f, cx - 85f, cy - 5f, cx - 75f, cy + 130f, targetMuscle == MuscleGroup.LEGS || targetMuscle == MuscleGroup.GLUTES || targetMuscle == MuscleGroup.FULL_BODY, targetMuscle)
                drawAnatomicalLeg(canvas, cx + 50f, cy - 10f, cx - 60f, cy - 5f, cx - 50f, cy + 130f, targetMuscle == MuscleGroup.LEGS || targetMuscle == MuscleGroup.GLUTES || targetMuscle == MuscleGroup.FULL_BODY, targetMuscle)
            }

            PoseType.BENCH_PRESS -> {
                // Press de Banca: Athlete lying on bench pressing barbell with highlighted pectorals/triceps
                val benchPaint = Paint().apply {
                    color = 0xFF334155.toInt()
                    style = Paint.Style.FILL
                    isAntiAlias = true
                }
                // Bench frame & padding
                canvas.drawRoundRect(RectF(cx - 210f, cy + 10f, cx + 210f, cy + 40f), 10f, 10f, benchPaint)
                canvas.drawRect(cx - 170f, cy + 40f, cx - 145f, cy + 160f, benchPaint)
                canvas.drawRect(cx + 145f, cy + 40f, cx + 170f, cy + 160f, benchPaint)

                // Head lying on bench
                drawAnatomicalHeadAndNeck(canvas, cx - 150f, cy - 10f, 26f, 75f, isFacingRight = true)

                // Horizontal Torso & Pectorals
                drawAnatomicalTorso(canvas, cx - 30f, cy - 5f, 130f, 160f, targetMuscle)

                // Legs / Feet on floor
                drawAnatomicalLeg(canvas, cx + 50f, cy + 5f, cx + 110f, cy + 60f, cx + 120f, cy + 145f, targetMuscle == MuscleGroup.LEGS, targetMuscle)

                // Arms pressing barbell vertically overhead
                drawAnatomicalArm(canvas, cx - 60f, cy - 20f, cx - 70f, cy - 85f, cx - 60f, cy - 135f, targetMuscle == MuscleGroup.CHEST || targetMuscle == MuscleGroup.ARMS || targetMuscle == MuscleGroup.SHOULDERS, targetMuscle)
                drawAnatomicalArm(canvas, cx, cy - 20f, cx + 10f, cy - 85f, cx + 20f, cy - 135f, targetMuscle == MuscleGroup.CHEST || targetMuscle == MuscleGroup.ARMS || targetMuscle == MuscleGroup.SHOULDERS, targetMuscle)

                // Barbell overhead
                val barPaint = Paint().apply {
                    shader = LinearGradient(cx - 200f, 0f, cx + 200f, 0f, 0xFF475569.toInt(), 0xFF0F172A.toInt(), Shader.TileMode.CLAMP)
                    style = Paint.Style.FILL
                    isAntiAlias = true
                }
                canvas.drawRoundRect(RectF(cx - 200f, cy - 150f, cx + 200f, cy - 134f), 6f, 6f, barPaint)
                canvas.drawCircle(cx - 200f, cy - 142f, 24f, barPaint)
                canvas.drawCircle(cx + 200f, cy - 142f, 24f, barPaint)
            }

            PoseType.ABS_CRUNCH -> {
                // Abdominales / Crunches: Athlete lying with flexed core (matching Image 1!) with highlighted red abdominal pack
                val matPaint = Paint().apply {
                    color = 0xFF475569.toInt()
                    style = Paint.Style.FILL
                    isAntiAlias = true
                }
                canvas.drawRoundRect(RectF(cx - 220f, cy + 80f, cx + 220f, cy + 100f), 8f, 8f, matPaint)

                // Head curled up
                drawAnatomicalHeadAndNeck(canvas, cx - 110f, cy - 45f, 28f, 45f, isFacingRight = true)

                // Curled Torso with highlighted 6-pack / 8-pack abdominal pack matching Image 1!
                drawAnatomicalTorso(canvas, cx - 10f, cy + 10f, 140f, 150f, MuscleGroup.ABS)

                // Hands behind head/neck
                drawAnatomicalArm(canvas, cx - 40f, cy - 15f, cx - 80f, cy - 35f, cx - 100f, cy - 40f, false, targetMuscle)

                // Legs bent 90° with feet flat on ground
                drawAnatomicalLeg(canvas, cx + 60f, cy + 30f, cx + 120f, cy - 10f, cx + 140f, cy + 80f, targetMuscle == MuscleGroup.LEGS, targetMuscle)
            }

            PoseType.RUNNING -> {
                // Carrera / Sprints: Athlete in sprint stride with highlighted legs
                val speedLine = Paint().apply {
                    color = COLOR_BORDER_SLATE
                    style = Paint.Style.STROKE
                    strokeWidth = 3f
                    isAntiAlias = true
                }
                canvas.drawLine(cx - 200f, cy + 170f, cx + 200f, cy + 170f, speedLine)

                // Head looking forward
                drawAnatomicalHeadAndNeck(canvas, cx + 30f, cy - 145f, 28f, 80f, isFacingRight = true)

                // Torso leaning forward
                drawAnatomicalTorso(canvas, cx + 10f, cy - 50f, 130f, 150f, targetMuscle)

                // Arms in sprint counter-drive
                drawAnatomicalArm(canvas, cx - 10f, cy - 100f, cx - 60f, cy - 65f, cx - 80f, cy - 30f, targetMuscle == MuscleGroup.ARMS, targetMuscle)
                drawAnatomicalArm(canvas, cx + 35f, cy - 100f, cx + 80f, cy - 80f, cx + 110f, cy - 110f, targetMuscle == MuscleGroup.ARMS, targetMuscle)

                // High Knee Lead Drive (Quads / Hamstrings)
                drawAnatomicalLeg(canvas, cx + 20f, cy + 15f, cx + 105f, cy + 10f, cx + 90f, cy + 115f, targetMuscle == MuscleGroup.LEGS || targetMuscle == MuscleGroup.CARDIO || targetMuscle == MuscleGroup.FULL_BODY, targetMuscle)

                // Trailing Leg extended back pushing off
                drawAnatomicalLeg(canvas, cx - 15f, cy + 15f, cx - 85f, cy + 65f, cx - 140f, cy + 155f, targetMuscle == MuscleGroup.LEGS || targetMuscle == MuscleGroup.GLUTES || targetMuscle == MuscleGroup.CALVES, targetMuscle)
            }

            PoseType.SWIMMING -> {
                // Natación: Athlete in swim stroke with highlighted back/lats/arms
                val waterPath = Path().apply {
                    moveTo(cx - 220f, cy + 30f)
                    quadTo(cx - 110f, cy + 10f, cx, cy + 30f)
                    quadTo(cx + 110f, cy + 50f, cx + 220f, cy + 30f)
                }
                val waterPaint = Paint().apply {
                    color = 0xFF38BDF8.toInt()
                    style = Paint.Style.STROKE
                    strokeWidth = 4f
                    isAntiAlias = true
                }
                canvas.drawPath(waterPath, waterPaint)

                // Head turned breathing
                drawAnatomicalHeadAndNeck(canvas, cx - 120f, cy - 25f, 26f, 80f, isFacingRight = false)

                // Lead Arm reaching far forward
                drawAnatomicalArm(canvas, cx - 80f, cy - 20f, cx - 150f, cy - 25f, cx - 215f, cy - 30f, targetMuscle == MuscleGroup.ARMS || targetMuscle == MuscleGroup.BACK, targetMuscle)

                // Streamlined Torso
                drawAnatomicalTorso(canvas, cx - 30f, cy - 10f, 130f, 150f, targetMuscle, isBackView = true)

                // Trailing Arm along hip
                drawAnatomicalArm(canvas, cx - 40f, cy - 15f, cx + 10f, cy, cx + 45f, cy + 15f, targetMuscle == MuscleGroup.ARMS, targetMuscle)

                // Flutter Kick Legs
                drawAnatomicalLeg(canvas, cx + 45f, cy - 10f, cx + 120f, cy - 20f, cx + 185f, cy - 25f, targetMuscle == MuscleGroup.LEGS || targetMuscle == MuscleGroup.CARDIO, targetMuscle)
                drawAnatomicalLeg(canvas, cx + 45f, cy - 10f, cx + 120f, cy + 5f, cx + 195f, cy + 15f, targetMuscle == MuscleGroup.LEGS || targetMuscle == MuscleGroup.CARDIO, targetMuscle)
            }

            PoseType.DEADLIFT -> {
                // Deadlift: Athlete bent at hips holding barbell near shins
                val barPaint = Paint().apply {
                    shader = LinearGradient(cx - 180f, 0f, cx + 180f, 0f, 0xFF475569.toInt(), 0xFF0F172A.toInt(), Shader.TileMode.CLAMP)
                    style = Paint.Style.FILL
                    isAntiAlias = true
                }
                canvas.drawRoundRect(RectF(cx - 180f, cy + 45f, cx + 180f, cy + 60f), 6f, 6f, barPaint)
                canvas.drawCircle(cx - 180f, cy + 52f, 26f, barPaint)
                canvas.drawCircle(cx + 180f, cy + 52f, 26f, barPaint)

                // Head neutral spine
                drawAnatomicalHeadAndNeck(canvas, cx - 70f, cy - 110f, 26f, 60f, isFacingRight = true)

                // Back / Spine angled forward
                drawAnatomicalTorso(canvas, cx - 15f, cy - 45f, 135f, 150f, targetMuscle, isBackView = true)

                // Arms straight holding barbell
                drawAnatomicalArm(canvas, cx - 40f, cy - 65f, cx - 25f, cy - 5f, cx - 15f, cy + 45f, targetMuscle == MuscleGroup.ARMS, targetMuscle)

                // Loaded Legs & Glutes
                drawAnatomicalLeg(canvas, cx + 25f, cy - 15f, cx + 10f, cy + 45f, cx, cy + 155f, targetMuscle == MuscleGroup.LEGS || targetMuscle == MuscleGroup.GLUTES || targetMuscle == MuscleGroup.FULL_BODY, targetMuscle)
            }

            PoseType.SHOULDER_PRESS, PoseType.BICEP_CURL, PoseType.GENERAL_ATHLETIC -> {
                // Standing Anatomical Athletic Pose
                drawAnatomicalHeadAndNeck(canvas, cx, cy - 160f, 32f, 0f)

                // Front Torso (Pectorals, Lats, Abdominals)
                drawAnatomicalTorso(canvas, cx, cy - 60f, 140f, 170f, targetMuscle)

                // Muscular Arms
                drawAnatomicalArm(canvas, cx - 60f, cy - 120f, cx - 80f, cy - 40f, cx - 90f, cy + 30f, targetMuscle == MuscleGroup.ARMS || targetMuscle == MuscleGroup.SHOULDERS, targetMuscle)
                drawAnatomicalArm(canvas, cx + 60f, cy - 120f, cx + 80f, cy - 40f, cx + 90f, cy + 30f, targetMuscle == MuscleGroup.ARMS || targetMuscle == MuscleGroup.SHOULDERS, targetMuscle)

                // Muscular Legs
                drawAnatomicalLeg(canvas, cx - 32f, cy + 25f, cx - 30f, cy + 110f, cx - 25f, cy + 180f, targetMuscle == MuscleGroup.LEGS || targetMuscle == MuscleGroup.CALVES || targetMuscle == MuscleGroup.FULL_BODY, targetMuscle)
                drawAnatomicalLeg(canvas, cx + 32f, cy + 25f, cx + 30f, cy + 110f, cx + 25f, cy + 180f, targetMuscle == MuscleGroup.LEGS || targetMuscle == MuscleGroup.CALVES || targetMuscle == MuscleGroup.FULL_BODY, targetMuscle)
            }
        }
    }

    /**
     * Clears cached realistic exercise illustrations.
     */
    fun clearCache(context: Context?) {
        if (context == null) return
        try {
            val cacheDir = context.cacheDir
            if (cacheDir.exists()) {
                cacheDir.listFiles()?.forEach { file ->
                    if (file.name.startsWith(CACHE_PREFIX)) {
                        file.delete()
                    }
                }
            }
        } catch (e: Exception) {
            // Ignore
        }
    }
}
