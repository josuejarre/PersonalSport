package com.example.personalsport.data.provider

import android.content.Context

/**
 * Pro Anatomical Muscle Engagement Diagram Generator.
 *
 * Redirects diagram generation to RealisticExerciseIllustrationGenerator.
 */
object ProAnatomicalDiagramGenerator {

    const val BADGE_TEXT = RealisticExerciseIllustrationGenerator.BADGE_TEXT
    const val CACHE_PREFIX = RealisticExerciseIllustrationGenerator.CACHE_PREFIX

    /**
     * Checks whether an image URL string is a Realistic / Pro Anatomical Exercise Diagram.
     */
    fun isProAnatomicalDiagram(url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        return url.contains("pro_anatomical_", ignoreCase = true) ||
                RealisticExerciseIllustrationGenerator.isRealisticIllustration(url)
    }

    /**
     * Generates a realistic 3D-shaded athlete illustration for an exercise movement.
     *
     * @return Local file URL string starting with `file://`.
     */
    fun generateProAnatomicalDiagram(
        context: Context?,
        exerciseName: String,
        targetMuscle: String? = null
    ): String {
        return RealisticExerciseIllustrationGenerator.generateRealisticIllustration(
            context = context,
            exerciseName = exerciseName,
            targetMuscle = targetMuscle
        )
    }

    fun clearCache(context: Context?) {
        RealisticExerciseIllustrationGenerator.clearCache(context)
    }
}
