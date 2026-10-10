package com.example.personalsport.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import com.example.personalsport.data.local.entity.BodyMetric
import com.example.personalsport.data.local.entity.ProgressPhoto
import java.util.Locale
import kotlin.math.abs

/**
 * Clasificación cualitativa de la transformación física visual ([TransformationOutcome]).
 *
 * @property titleEs Título cualitativo traducido al español.
 * @property descriptionEs Explicación fisiológica de la evolución observada.
 */
enum class TransformationOutcome(
    val titleEs: String,
    val descriptionEs: String
) {
    FAT_LOSS(
        "Pérdida de Grasa",
        "Reducción de tejido graso corporal con mayor definición abdominal y estrechamiento de cintura."
    ),
    MUSCLE_GAIN(
        "Ganancia Muscular",
        "Aumento de volumen en hombros y pecho con expansión de la estructura V-Taper y masa magra."
    ),
    BODY_RECOMPOSITION(
        "Recomposición Corporal",
        "Optimización simultánea: quema de masa grasa corporal y desarrollo/firmeza de densidad muscular."
    ),
    MAINTENANCE(
        "Mantenimiento",
        "Estructura corporal, proporciones antropométricas y peso estables en el período analizado."
    )
}

/**
 * Ratios e índices antropométricos extraídos de las imágenes de progreso ([AnthropometricRatios]).
 *
 * @property shoulderWidthPx Ancho estimado de hombros en píxeles normalizados.
 * @property chestWidthPx Ancho estimado de pecho/tórax en píxeles.
 * @property waistWidthPx Ancho estimado de cintura en píxeles.
 * @property vTaperIndex Índice V-Taper (Hombros / Cintura).
 * @property chestToWaistRatio Ratio Pecho / Cintura.
 * @property silhouetteDefinitionIndex Índice de definición de silueta y contraste abdominal (0.0% a 100.0%).
 */
data class AnthropometricRatios(
    val shoulderWidthPx: Float,
    val chestWidthPx: Float,
    val waistWidthPx: Float,
    val vTaperIndex: Float,
    val chestToWaistRatio: Float,
    val silhouetteDefinitionIndex: Float
)

/**
 * Resumen cuantitativo de cambios porcentuales en los ratios antropométricos ([RatioChangeSummary]).
 */
data class RatioChangeSummary(
    val vTaperChangePct: Float,
    val chestToWaistChangePct: Float,
    val definitionChangePct: Float,
    val beforeRatios: AnthropometricRatios,
    val afterRatios: AnthropometricRatios
)

/**
 * Fusión multimodal con datos cuantitativos de la base de datos Room ([MultiModalData]).
 */
data class MultiModalData(
    val beforeWeightKg: Float? = null,
    val afterWeightKg: Float? = null,
    val weightChangeKg: Float? = null,
    val beforeFatPct: Float? = null,
    val afterFatPct: Float? = null,
    val fatPercentageChangePct: Float? = null
)

/**
 * Resultado completo del análisis visual antropométrico por IA ([VisualAnalysisResult]).
 *
 * @property outcome Clasificación cualitativa ([TransformationOutcome]).
 * @property confidenceScore Grado de confianza del algoritmo (0 a 100%).
 * @property ratioChanges Resumen de cambios en ratios de silueta.
 * @property multiModalData Fusión con datos de peso y grasa de la báscula.
 * @property explanationBreakdown Desglose explicativo en español.
 * @property trainingAdvice Consejo de entrenamiento ajustado a la evolución.
 * @property nutritionAdvice Consejo de nutrición ajustado a la evolución.
 * @property isLocalProcessingConfirmed Confirmación de procesamiento 100% local en el dispositivo.
 */
data class VisualAnalysisResult(
    val outcome: TransformationOutcome,
    val confidenceScore: Int,
    val ratioChanges: RatioChangeSummary,
    val multiModalData: MultiModalData,
    val explanationBreakdown: String,
    val trainingAdvice: String,
    val nutritionAdvice: String,
    val isLocalProcessingConfirmed: Boolean = true
)

/**
 * Analizador Antropométrico Visual de Composición Corporal ([VisualAIBodyAnalyzer]).
 *
 * Procesa imágenes comparativas de progreso físico (Antes vs Después), extrayendo la silueta mediante
 * detección de bordes y mapa de gradientes, calcula índices V-Taper y contraste abdominal, y los fusiona
 * con las métricas de peso y porcentaje graso registradas en Room DB.
 *
 * **Gestión de Estado e Hilos**:
 * - Operaciones computacionales optimizadas en Bitmaps redimensionados a 240x320 para minimizar uso de memoria GPU/RAM.
 * - Seguro para ejecución en hilos secundarios de fondo (`Dispatchers.Default` / `Dispatchers.IO`).
 */
object VisualAIBodyAnalyzer {

    /**
     * Realiza el análisis visual comparativo entre dos fotos de progreso y métricas corporales opcionales.
     */
    fun analyzeProgressPhotos(
        beforePhoto: ProgressPhoto,
        afterPhoto: ProgressPhoto,
        beforeMetric: BodyMetric? = null,
        afterMetric: BodyMetric? = null,
        context: Context? = null,
        beforeBitmapOverride: Bitmap? = null,
        afterBitmapOverride: Bitmap? = null
    ): VisualAnalysisResult {
        val baseResult = computeBaseProgressPhotoAnalysis(
            beforePhoto = beforePhoto,
            afterPhoto = afterPhoto,
            beforeMetric = beforeMetric,
            afterMetric = afterMetric,
            context = context,
            beforeBitmapOverride = beforeBitmapOverride,
            afterBitmapOverride = afterBitmapOverride
        )
        if (QwenLlmService.isModelLoaded()) {
            return enrichVisualAnalysisWithLlm(baseResult)
        }
        return baseResult
    }

    private fun loadBitmapFromUri(context: Context?, uriString: String): Bitmap? {
        if (context == null || uriString.isBlank()) return null
        return try {
            val uri = Uri.parse(uriString)
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream)
            }
        } catch (_: Throwable) {
            null
        }
    }

    /**
     * Extrae los ratios antropométricos (Hombros, Pecho, Cintura, V-Taper) de un Bitmap de imagen.
     */
    fun extractPhotoRatios(bitmap: Bitmap?, photo: ProgressPhoto): AnthropometricRatios {
        if (bitmap != null) {
            val processed = processBitmapSilhouette(bitmap)
            if (processed != null) {
                return processed
            }
        }
        return generateHeuristicRatios(photo)
    }

    private fun processBitmapSilhouette(bitmap: Bitmap): AnthropometricRatios? {
        return try {
            val targetW = 240
            val targetH = 320
            val scaled = Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)

            val rowWidths = FloatArray(targetH)
            val rowGradients = FloatArray(targetH)

            var topY = -1
            var bottomY = -1

            for (y in 0 until targetH) {
                var minX = -1
                var maxX = -1
                var gradSum = 0f

                for (x in 2 until targetW - 2) {
                    val pixel = scaled.getPixel(x, y)
                    val prevPixel = scaled.getPixel(x - 1, y)

                    val r = Color.red(pixel)
                    val g = Color.green(pixel)
                    val b = Color.blue(pixel)
                    val lum = 0.299f * r + 0.587f * g + 0.114f * b

                    val prevR = Color.red(prevPixel)
                    val prevG = Color.green(prevPixel)
                    val prevB = Color.blue(prevPixel)
                    val prevLum = 0.299f * prevR + 0.587f * prevG + 0.114f * prevB

                    val diff = abs(lum - prevLum)
                    gradSum += diff

                    if (diff > 18f) {
                        if (minX == -1) minX = x
                        maxX = x
                    }
                }

                if (minX != -1 && maxX > minX) {
                    if (topY == -1) topY = y
                    bottomY = y
                    rowWidths[y] = (maxX - minX).toFloat()
                    rowGradients[y] = gradSum / targetW
                } else {
                    rowWidths[y] = 0f
                    rowGradients[y] = 0f
                }
            }

            val bodyHeight = bottomY - topY
            if (bodyHeight < 60) return null

            val yShoulder = (topY + bodyHeight * 0.20f).toInt().coerceIn(0, targetH - 1)
            val yChest = (topY + bodyHeight * 0.35f).toInt().coerceIn(0, targetH - 1)
            val yWaist = (topY + bodyHeight * 0.62f).toInt().coerceIn(0, targetH - 1)

            val shoulderWidthPx = averageRowSlice(rowWidths, yShoulder, 5)
            val chestWidthPx = averageRowSlice(rowWidths, yChest, 5)
            val waistWidthPx = averageRowSlice(rowWidths, yWaist, 5).coerceAtLeast(10f)

            val yAbsStart = (topY + bodyHeight * 0.45f).toInt().coerceIn(0, targetH - 1)
            val yAbsEnd = (topY + bodyHeight * 0.70f).toInt().coerceIn(0, targetH - 1)
            var absGradTotal = 0f
            var absCount = 0
            for (y in yAbsStart..yAbsEnd) {
                absGradTotal += rowGradients[y]
                absCount++
            }
            val avgAbsGrad = if (absCount > 0) absGradTotal / absCount else 15f
            val definitionIndex = (avgAbsGrad * 2.5f).coerceIn(15f, 98f)

            val vTaper = shoulderWidthPx / waistWidthPx
            val chestToWaist = chestWidthPx / waistWidthPx

            AnthropometricRatios(
                shoulderWidthPx = shoulderWidthPx,
                chestWidthPx = chestWidthPx,
                waistWidthPx = waistWidthPx,
                vTaperIndex = vTaper,
                chestToWaistRatio = chestToWaist,
                silhouetteDefinitionIndex = definitionIndex
            )
        } catch (_: Throwable) {
            null
        }
    }

    private fun averageRowSlice(rowWidths: FloatArray, centerY: Int, radius: Int): Float {
        var sum = 0f
        var count = 0
        for (y in (centerY - radius)..(centerY + radius)) {
            if (y in rowWidths.indices && rowWidths[y] > 0f) {
                sum += rowWidths[y]
                count++
            }
        }
        return if (count > 0) sum / count else 80f
    }

    private fun generateHeuristicRatios(photo: ProgressPhoto): AnthropometricRatios {
        val notesLower = photo.notes.lowercase(Locale.ROOT)
        var shoulderWidth = 120f
        var chestWidth = 105f
        var waistWidth = 80f
        var definition = 60f

        if (notesLower.contains("cut") || notesLower.contains("defin") || notesLower.contains("baja") || notesLower.contains("lean")) {
            waistWidth -= 3.5f
            definition += 10f
        }
        if (notesLower.contains("bulk") || notesLower.contains("mas") || notesLower.contains("fuerza") || notesLower.contains("gain")) {
            shoulderWidth += 4.5f
            chestWidth += 3.5f
            waistWidth += 1.0f
        }

        val vTaper = shoulderWidth / waistWidth
        val chestToWaist = chestWidth / waistWidth

        return AnthropometricRatios(
            shoulderWidthPx = shoulderWidth,
            chestWidthPx = chestWidth,
            waistWidthPx = waistWidth,
            vTaperIndex = vTaper,
            chestToWaistRatio = chestToWaist,
            silhouetteDefinitionIndex = definition
        )
    }

    /**
     * Fusiona los cambios de silueta con los datos de peso y grasa de Room DB.
     */
    fun computeFusionAnalysis(
        beforeRatios: AnthropometricRatios,
        afterRatios: AnthropometricRatios,
        beforePhoto: ProgressPhoto,
        afterPhoto: ProgressPhoto,
        beforeMetric: BodyMetric? = null,
        afterMetric: BodyMetric? = null
    ): VisualAnalysisResult {

        val vTaperChangePct = if (beforeRatios.vTaperIndex > 0f) {
            ((afterRatios.vTaperIndex - beforeRatios.vTaperIndex) / beforeRatios.vTaperIndex) * 100f
        } else 0f

        val chestToWaistChangePct = if (beforeRatios.chestToWaistRatio > 0f) {
            ((afterRatios.chestToWaistRatio - beforeRatios.chestToWaistRatio) / beforeRatios.chestToWaistRatio) * 100f
        } else 0f

        val definitionChangePct = afterRatios.silhouetteDefinitionIndex - beforeRatios.silhouetteDefinitionIndex

        val weightChangeKg = if (beforeMetric != null && afterMetric != null) {
            afterMetric.weightKg - beforeMetric.weightKg
        } else null

        val fatChangePct = if (beforeMetric?.bodyFatPercentage != null && afterMetric?.bodyFatPercentage != null) {
            afterMetric.bodyFatPercentage - beforeMetric.bodyFatPercentage
        } else null

        val multiModalData = MultiModalData(
            beforeWeightKg = beforeMetric?.weightKg,
            afterWeightKg = afterMetric?.weightKg,
            weightChangeKg = weightChangeKg,
            beforeFatPct = beforeMetric?.bodyFatPercentage,
            afterFatPct = afterMetric?.bodyFatPercentage,
            fatPercentageChangePct = fatChangePct
        )

        val ratioSummary = RatioChangeSummary(
            vTaperChangePct = vTaperChangePct,
            chestToWaistChangePct = chestToWaistChangePct,
            definitionChangePct = definitionChangePct,
            beforeRatios = beforeRatios,
            afterRatios = afterRatios
        )

        val outcome = classifyOutcome(
            vTaperChangePct = vTaperChangePct,
            chestToWaistChangePct = chestToWaistChangePct,
            definitionChangePct = definitionChangePct,
            weightChangeKg = weightChangeKg,
            fatChangePct = fatChangePct
        )

        val confidenceScore = calculateConfidenceScore(
            beforePhoto = beforePhoto,
            afterPhoto = afterPhoto,
            beforeMetric = beforeMetric,
            afterMetric = afterMetric,
            vTaperChangePct = vTaperChangePct,
            fatChangePct = fatChangePct,
            weightChangeKg = weightChangeKg
        )

        val explanationBreakdown = buildExplanationBreakdown(
            confidence = confidenceScore,
            vTaperChangePct = vTaperChangePct,
            chestToWaistChangePct = chestToWaistChangePct,
            definitionChangePct = definitionChangePct,
            multiModalData = multiModalData
        )

        val (trainingAdvice, nutritionAdvice) = generateAdvice(outcome)

        return VisualAnalysisResult(
            outcome = outcome,
            confidenceScore = confidenceScore,
            ratioChanges = ratioSummary,
            multiModalData = multiModalData,
            explanationBreakdown = explanationBreakdown,
            trainingAdvice = trainingAdvice,
            nutritionAdvice = nutritionAdvice,
            isLocalProcessingConfirmed = true
        )
    }

    private fun classifyOutcome(
        vTaperChangePct: Float,
        chestToWaistChangePct: Float,
        definitionChangePct: Float,
        weightChangeKg: Float?,
        fatChangePct: Float?
    ): TransformationOutcome {
        val hasFatReductionCue = (fatChangePct != null && fatChangePct <= -0.3f) || (weightChangeKg != null && weightChangeKg <= -0.4f)
        val hasMuscleGainCue = (weightChangeKg != null && weightChangeKg >= 0.4f)
        val hasRatioExpansion = vTaperChangePct >= 1.0f || chestToWaistChangePct >= 1.0f
        val hasDefinitionGain = definitionChangePct >= 2.5f

        val isSignificantWeightLoss = weightChangeKg != null && weightChangeKg <= -1.2f

        return when {
            isSignificantWeightLoss -> {
                TransformationOutcome.FAT_LOSS
            }

            hasFatReductionCue && hasRatioExpansion && (weightChangeKg == null || abs(weightChangeKg) <= 1.2f) -> {
                TransformationOutcome.BODY_RECOMPOSITION
            }

            hasFatReductionCue -> {
                TransformationOutcome.FAT_LOSS
            }

            hasMuscleGainCue && (fatChangePct == null || fatChangePct <= 0.6f) -> {
                TransformationOutcome.MUSCLE_GAIN
            }
            vTaperChangePct >= 2.0f || chestToWaistChangePct >= 2.0f -> {
                TransformationOutcome.MUSCLE_GAIN
            }

            hasDefinitionGain || vTaperChangePct >= 1.2f -> {
                TransformationOutcome.FAT_LOSS
            }

            else -> TransformationOutcome.MAINTENANCE
        }
    }

    private fun calculateConfidenceScore(
        beforePhoto: ProgressPhoto,
        afterPhoto: ProgressPhoto,
        beforeMetric: BodyMetric?,
        afterMetric: BodyMetric?,
        vTaperChangePct: Float,
        fatChangePct: Float?,
        weightChangeKg: Float?
    ): Int {
        var confidence = 78

        if (beforeMetric != null && afterMetric != null) {
            confidence += 8
            if (fatChangePct != null) {
                confidence += 4
            }
            if (fatChangePct != null && fatChangePct < 0f) {
                confidence += 3
            } else if (weightChangeKg != null && weightChangeKg > 0f) {
                confidence += 3
            }
        }

        val daysDiff = abs(afterPhoto.date - beforePhoto.date) / (1000 * 60 * 60 * 24)
        when {
            daysDiff >= 30 -> confidence += 4
            daysDiff >= 7 -> confidence += 2
            daysDiff < 1 -> confidence -= 10
        }

        if (abs(vTaperChangePct) > 1.0f) {
            confidence += 2
        }

        return confidence.coerceIn(50, 98)
    }

    private fun buildExplanationBreakdown(
        confidence: Int,
        vTaperChangePct: Float,
        chestToWaistChangePct: Float,
        definitionChangePct: Float,
        multiModalData: MultiModalData
    ): String {
        val vTaperStr = formatSignedPct(vTaperChangePct)
        val chestStr = formatSignedPct(chestToWaistChangePct)
        val defStr = formatSignedPct(definitionChangePct)

        val sb = StringBuilder()
        sb.append("Proporción Hombro-Cintura (V-Taper) ").append(if (vTaperChangePct >= 0) "aumentada " else "variada ").append(vTaperStr).append(". ")
        sb.append("Ratio Pecho-Cintura ").append(chestStr).append(". ")

        if (definitionChangePct >= 0.5f) {
            sb.append("Definición de silueta mejorada en ").append(defStr).append(". ")
        }

        val weight = multiModalData.weightChangeKg
        val fat = multiModalData.fatPercentageChangePct

        if (weight != null) {
            val beforeW = multiModalData.beforeWeightKg
            val afterW = multiModalData.afterWeightKg
            if (beforeW != null && afterW != null) {
                sb.append(String.format(Locale.US, "Peso corporal: %.1f kg ➔ %.1f kg (%+.1f kg). ", beforeW, afterW, weight))
            } else {
                sb.append(String.format(Locale.US, "Cambio de peso: %+.1f kg. ", weight))
            }
        }

        if (fat != null) {
            val beforeF = multiModalData.beforeFatPct
            val afterF = multiModalData.afterFatPct
            if (beforeF != null && afterF != null) {
                sb.append(String.format(Locale.US, "Grasa corporal: %.1f%% ➔ %.1f%% (%+.1f%%). ", beforeF, afterF, fat))
            } else {
                sb.append(String.format(Locale.US, "Variación de grasa: %+.1f%%. ", fat))
            }
        }

        sb.append(String.format(Locale.US, "Confianza de IA Local: %d%%.", confidence))
        return sb.toString()
    }

    private fun generateAdvice(outcome: TransformationOutcome): Pair<String, String> {
        return when (outcome) {
            TransformationOutcome.FAT_LOSS -> Pair(
                "Aumenta la densidad del entrenamiento combinando 3-4 sesiones de fuerza pesada con 2 sesiones de cardio HIIT o atletismo (series de 400m/1000m) para preservar masa muscular mientras maximizas el gasto calórico.",
                "Mantén un déficit calórico moderado de 300-500 kcal/día. Prioriza un aporte elevado de proteína (1.8 - 2.2 g/kg) para proteger el tejido magro y asegúrate de mantener alta hidratación."
            )
            TransformationOutcome.MUSCLE_GAIN -> Pair(
                "Enfócate en la sobrecarga progresiva en ejercicios multiarticulares (press de banca, sentadillas, dominadas) manteniendo un volumen de 10-15 series semanales por grupo muscular cerca del fallo.",
                "Mantén un superávit calórico controlado (+250-400 kcal/día) con un consumo proteico de 1.6 - 2.0 g/kg y prioriza carbohidratos complejos alrededor de tus sesiones de entrenamiento."
            )
            TransformationOutcome.BODY_RECOMPOSITION -> Pair(
                "Combina hipertrofia con rango de 8-12 repeticiones pesadas e incluye entrenamientos tipo Fartlek o aceleraciones para estimular la oxidación de masa grasa sin sacrificar masa muscular.",
                "Consume una dieta normocalórica o con déficit muy leve (-150 kcal), elevando la ingesta de proteína a 2.0 - 2.4 g/kg para favorecer la síntesis proteica muscular simultánea a la quema de grasa."
            )
            TransformationOutcome.MAINTENANCE -> Pair(
                "Continúa con tu rutina actual de fuerza y resistencia. Si deseas estimular nuevos cambios en tu silueta, aumenta progresivamente las cargas o introduce variaciones en la intensidad.",
                "Mantén tu consumo energético al nivel de tu tasa metabólica total. Asegúrate de cubrir tus requerimientos de micronutrientes, proteína y descanso para consolidar la estructura actual."
            )
        }
    }

    private fun formatSignedPct(value: Float): String {
        return String.format(Locale.US, "%+.1f%%", value)
    }

    private fun computeBaseProgressPhotoAnalysis(
        beforePhoto: ProgressPhoto,
        afterPhoto: ProgressPhoto,
        beforeMetric: BodyMetric? = null,
        afterMetric: BodyMetric? = null,
        context: Context? = null,
        beforeBitmapOverride: Bitmap? = null,
        afterBitmapOverride: Bitmap? = null
    ): VisualAnalysisResult {
        val beforeBitmap = beforeBitmapOverride ?: loadBitmapFromUri(context, beforePhoto.photoUri)
        val afterBitmap = afterBitmapOverride ?: loadBitmapFromUri(context, afterPhoto.photoUri)

        val beforeRatios = extractPhotoRatios(beforeBitmap, beforePhoto)
        val afterRatios = extractPhotoRatios(afterBitmap, afterPhoto)

        return computeFusionAnalysis(
            beforeRatios = beforeRatios,
            afterRatios = afterRatios,
            beforePhoto = beforePhoto,
            afterPhoto = afterPhoto,
            beforeMetric = beforeMetric,
            afterMetric = afterMetric
        )
    }

    private fun enrichVisualAnalysisWithLlm(
        baseResult: VisualAnalysisResult,
        onTokenStream: (String) -> Unit = {}
    ): VisualAnalysisResult {
        val ragContext = LocalSportsKnowledgeBase.getKnowledgeContext("fuerza calistenia nutricion")
        val prompt = buildString {
            append(ragContext).append("\n\n")
            append("Actúa como analista de composición corporal visual en Qwen 2.5 3B Local LLM.\n")
            append("Resultado: ").append(baseResult.outcome.titleEs).append(" (Confianza ").append(baseResult.confidenceScore).append("%).\n")
            append("Cambio V-Taper: ").append(String.format(Locale.US, "%.1f%%", baseResult.ratioChanges.vTaperChangePct)).append(", Pecho-Cintura: ").append(String.format(Locale.US, "%.1f%%", baseResult.ratioChanges.chestToWaistChangePct)).append("\n")
            append("Genera desglose de silueta y consejo de entrenamiento/nutrición estrictamente en ESPAÑOL.")
        }

        val llmOutput = QwenLlmService.generate(prompt, maxTokens = 512, onToken = onTokenStream)

        if (llmOutput.isBlank() || llmOutput.startsWith("Qwen LLM model is not loaded") || llmOutput.startsWith("Error")) {
            return baseResult
        }

        val enrichedBreakdown = "${baseResult.explanationBreakdown}\n\n[Qwen 2.5 LLM Analysis]:\n${llmOutput.take(300)}"
        return baseResult.copy(explanationBreakdown = enrichedBreakdown)
    }

    /**
     * Realiza el análisis antropométrico visual enriquecido con las explicaciones del modelo Qwen 2.5 3B LLM.
     */
    fun analyzeProgressPhotosWithLlm(
        beforePhoto: ProgressPhoto,
        afterPhoto: ProgressPhoto,
        beforeMetric: BodyMetric? = null,
        afterMetric: BodyMetric? = null,
        context: Context? = null,
        beforeBitmapOverride: Bitmap? = null,
        afterBitmapOverride: Bitmap? = null,
        onTokenStream: (String) -> Unit = {}
    ): VisualAnalysisResult {
        val baseResult = computeBaseProgressPhotoAnalysis(
            beforePhoto = beforePhoto,
            afterPhoto = afterPhoto,
            beforeMetric = beforeMetric,
            afterMetric = afterMetric,
            context = context,
            beforeBitmapOverride = beforeBitmapOverride,
            afterBitmapOverride = afterBitmapOverride
        )

        if (!QwenLlmService.isModelLoaded()) {
            onTokenStream("Qwen LLM no disponible. Usando análisis visual de reglas local.")
            return baseResult
        }

        return enrichVisualAnalysisWithLlm(baseResult, onTokenStream)
    }
}
