package com.example.personalsport.audio

import android.content.Context
import android.content.ContextWrapper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

private class TestAudioContext : ContextWrapper(null) {
    override fun getApplicationContext(): Context = this
}

class AudioCoachManagerTest {

    private lateinit var testContext: Context
    private lateinit var audioCoachManager: AudioCoachManager

    @Before
    fun setUp() {
        testContext = TestAudioContext()
        audioCoachManager = AudioCoachManager(testContext)
    }

    @Test
    fun testAnnounceIntervalSwitchText() {
        val announcement = audioCoachManager.announceIntervalSwitch(
            seriesName = "Series de Atletismo 400m",
            distanceMeters = 400,
            targetPaceMinPerKm = "3:50"
        )
        assertNotNull(announcement)
        assertTrue(announcement.contains("400m"))
        assertTrue(announcement.contains("Series de Atletismo 400m"))
        assertTrue(announcement.contains("3:50 min/km"))
        assertEquals(announcement, audioCoachManager.lastAnnouncedText.value)
    }

    @Test
    fun testAnnounceRestCountdownTexts() {
        val c30 = audioCoachManager.announceRestCountdown(30)
        assertTrue(c30.contains("30 segundos"))

        val c10 = audioCoachManager.announceRestCountdown(10)
        assertTrue(c10.contains("10 segundos"))

        val c0 = audioCoachManager.announceRestCountdown(0)
        assertTrue(c0.contains("¡Descanso terminado!"))
    }

    @Test
    fun testAnnounceExerciseTransitionText() {
        val announcement = audioCoachManager.announceExerciseTransition(
            nextExerciseName = "Press de Banca",
            repsOrDuration = "10 repeticiones"
        )
        assertTrue(announcement.contains("Press de Banca"))
        assertTrue(announcement.contains("10 repeticiones"))
    }

    @Test
    fun testAnnounceHeartRateZoneAlertText() {
        val announcement = audioCoachManager.announceHeartRateZoneAlert(
            currentBpm = 175,
            zoneName = "Zona Anaeróbica",
            actionAdvice = "Reduce un poco el ritmo"
        )
        assertTrue(announcement.contains("175 BPM"))
        assertTrue(announcement.contains("Zona Anaeróbica"))
        assertTrue(announcement.contains("Reduce un poco el ritmo"))
    }

    @Test
    fun testMuteAndVolumeSettings() {
        audioCoachManager.isMuted = true
        assertTrue(audioCoachManager.isMuted)

        val resultWhenMuted = audioCoachManager.speak("Prueba de volumen")
        assertFalse(resultWhenMuted)

        audioCoachManager.volume = 0.8f
        assertEquals(0.8f, audioCoachManager.volume, 0.01f)

        audioCoachManager.speechRate = 1.2f
        assertEquals(1.2f, audioCoachManager.speechRate, 0.01f)
    }
}
