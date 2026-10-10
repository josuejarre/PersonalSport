package com.example.personalsport.data.provider

import org.junit.Assert.*
import org.junit.Test

class NanoBananaAssetProviderTest {

    @Test
    fun testResolveCategory_mapsCorrectlyForMuscleGroupsAndCategories() {
        assertEquals("chest", NanoBananaAssetProvider.resolveCategory("Press de Pecho Inclinado Custom"))
        assertEquals("chest", NanoBananaAssetProvider.resolveCategory(targetMuscle = "Pectoral"))
        assertEquals("back", NanoBananaAssetProvider.resolveCategory("Remo Unilateral con Mancuerna"))
        assertEquals("back", NanoBananaAssetProvider.resolveCategory(targetMuscle = "Espalda / Dorsal"))
        assertEquals("legs", NanoBananaAssetProvider.resolveCategory("Sentadilla Búlgara Especial"))
        assertEquals("legs", NanoBananaAssetProvider.resolveCategory(targetMuscle = "Cuádriceps"))
        assertEquals("shoulders", NanoBananaAssetProvider.resolveCategory("Elevaciones Laterales Pesadas"))
        assertEquals("shoulders", NanoBananaAssetProvider.resolveCategory(targetMuscle = "Deltoides / Hombro"))
        assertEquals("arms", NanoBananaAssetProvider.resolveCategory("Curl Martillo Invertido"))
        assertEquals("arms", NanoBananaAssetProvider.resolveCategory(targetMuscle = "Bíceps & Tríceps"))
        assertEquals("abs", NanoBananaAssetProvider.resolveCategory("Plancha Isométrica Lateral"))
        assertEquals("abs", NanoBananaAssetProvider.resolveCategory(targetMuscle = "Abdominales / Core"))
        assertEquals("running", NanoBananaAssetProvider.resolveCategory("Trote Ligero y Sprints"))
        assertEquals("running", NanoBananaAssetProvider.resolveCategory(category = "Running"))
        assertEquals("swimming", NanoBananaAssetProvider.resolveCategory("Natación en Aguas Abiertas"))
        assertEquals("swimming", NanoBananaAssetProvider.resolveCategory(category = "Swimming"))
        assertEquals("default", NanoBananaAssetProvider.resolveCategory("Movimiento Extranormal Misterioso 99"))
        assertEquals("default", NanoBananaAssetProvider.resolveCategory(null, null, null))
    }

    @Test
    fun testGetAssetUrl_returnsCorrectFileUrl() {
        val chestUrl = NanoBananaAssetProvider.getAssetUrl("Aperturas de Pecho Custom", targetMuscle = "Pecho")
        assertEquals("file:///android_asset/nano_banana/nano_banana_chest.png", chestUrl)

        val backUrl = NanoBananaAssetProvider.getAssetUrl("Jalón Espalda Polea Ancha", targetMuscle = "Espalda")
        assertEquals("file:///android_asset/nano_banana/nano_banana_back.png", backUrl)

        val legsUrl = NanoBananaAssetProvider.getAssetUrl("Prensa 45 Invertida", targetMuscle = "Piernas")
        assertEquals("file:///android_asset/nano_banana/nano_banana_legs.png", legsUrl)

        val shouldersUrl = NanoBananaAssetProvider.getAssetUrl("Press Hombros Arnold Custom", targetMuscle = "Hombros")
        assertEquals("file:///android_asset/nano_banana/nano_banana_shoulders.png", shouldersUrl)

        val armsUrl = NanoBananaAssetProvider.getAssetUrl("Extensión de Tríceps Polea Alta", targetMuscle = "Brazos")
        assertEquals("file:///android_asset/nano_banana/nano_banana_arms.png", armsUrl)

        val absUrl = NanoBananaAssetProvider.getAssetUrl("Elevación de Piernas Colgado", targetMuscle = "Abdominales")
        assertEquals("file:///android_asset/nano_banana/nano_banana_abs.png", absUrl)

        val runningUrl = NanoBananaAssetProvider.getAssetUrl("Serie de Intervalos 400m Custom", category = "Running")
        assertEquals("file:///android_asset/nano_banana/nano_banana_running.png", runningUrl)

        val swimmingUrl = NanoBananaAssetProvider.getAssetUrl("Natación Crol Técnica", category = "Swimming")
        assertEquals("file:///android_asset/nano_banana/nano_banana_swimming.png", swimmingUrl)

        val defaultUrl = NanoBananaAssetProvider.getAssetUrl("Ejercicio Raro Desconocido ABC")
        assertEquals("file:///android_asset/nano_banana/nano_banana_default.png", defaultUrl)
    }

    @Test
    fun testGetAssetUrlForCategory() {
        assertEquals("file:///android_asset/nano_banana/nano_banana_chest.png", NanoBananaAssetProvider.getAssetUrlForCategory("chest"))
        assertEquals("file:///android_asset/nano_banana/nano_banana_swimming.png", NanoBananaAssetProvider.getAssetUrlForCategory("swimming"))
        assertEquals("file:///android_asset/nano_banana/nano_banana_running.png", NanoBananaAssetProvider.getAssetUrlForCategory("running"))
        assertEquals("file:///android_asset/nano_banana/nano_banana_default.png", NanoBananaAssetProvider.getAssetUrlForCategory("desconocido"))
    }

    @Test
    fun testIsNanoBananaAssetUrl() {
        val assetUrl = "file:///android_asset/nano_banana/nano_banana_chest.png"
        val externalUrl = "https://raw.githubusercontent.com/hasaneyldrm/exercises-dataset/main/videos/0001-barbell_bench_press.gif"

        assertTrue("Should return true for Nano Banana asset URL", NanoBananaAssetProvider.isNanoBananaAssetUrl(assetUrl))
        assertFalse("Should return false for external GIF URL", NanoBananaAssetProvider.isNanoBananaAssetUrl(externalUrl))
        assertFalse("Should return false for null URL", NanoBananaAssetProvider.isNanoBananaAssetUrl(null))
        assertFalse("Should return false for empty URL", NanoBananaAssetProvider.isNanoBananaAssetUrl(""))
    }

    @Test
    fun testIntegrationWithExerciseJsonDatasetParser_customExercise() {
        val unknownChestExercise = "Aperturas Pliométricas Desconocidas XYZ 99"
        val chestGifUrl = ExerciseJsonDatasetParser.getFullGifUrl(unknownChestExercise, targetMuscleHint = "Pecho")
        assertTrue("Custom exercise should resolve to dynamic diagram file URL, found: $chestGifUrl", chestGifUrl.contains("pro_anatomical_") || chestGifUrl.contains("scraped_exercises") || chestGifUrl.contains("exercise_"))

        val unknownRunExercise = "Actividad Atlética Especial Inexistente XYZ 99"
        val runGifUrl = ExerciseJsonDatasetParser.getFullGifUrl(unknownRunExercise, categoryHint = "Running")
        assertTrue("Custom exercise should resolve to dynamic diagram file URL, found: $runGifUrl", runGifUrl.contains("pro_anatomical_") || runGifUrl.contains("scraped_exercises") || runGifUrl.contains("exercise_"))

        val unknownSwimExercise = "Estilo Mariposa Especial Inexistente XYZ 99"
        val swimGifUrl = ExerciseJsonDatasetParser.getFullGifUrl(unknownSwimExercise, categoryHint = "Swimming")
        assertTrue("Custom exercise should resolve to dynamic diagram file URL, found: $swimGifUrl", swimGifUrl.contains("pro_anatomical_") || swimGifUrl.contains("scraped_exercises") || swimGifUrl.contains("exercise_"))

        val unknownGeneralExercise = "Movimiento Misterioso 12345 ABC"
        val generalGifUrl = ExerciseJsonDatasetParser.getFullGifUrl(unknownGeneralExercise)
        assertTrue("Custom exercise should resolve to dynamic diagram file URL, found: $generalGifUrl", generalGifUrl.contains("pro_anatomical_") || generalGifUrl.contains("scraped_exercises") || generalGifUrl.contains("exercise_"))
    }

    @Test
    fun testIntegrationWithExerciseGifDatasetProvider_customExercise() {
        val unknownLegsExercise = "Zancadas Búlgaras Especiales Inexistentes XYZ 99"
        val legGifUrl = ExerciseGifDatasetProvider.getGifUrlForExercise(unknownLegsExercise, targetMuscleHint = "Cuádriceps")
        assertTrue("Custom exercise should resolve to dynamic diagram file URL, found: $legGifUrl", legGifUrl.contains("pro_anatomical_") || legGifUrl.contains("scraped_exercises") || legGifUrl.contains("exercise_"))
    }
}
