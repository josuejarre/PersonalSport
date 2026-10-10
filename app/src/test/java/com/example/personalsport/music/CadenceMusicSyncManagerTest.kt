package com.example.personalsport.music

import android.content.Context
import android.content.ContextWrapper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

private class TestMusicContext : ContextWrapper(null) {
    override fun getApplicationContext(): Context = this
    override fun getSystemService(name: String): Any? = null
}

class CadenceMusicSyncManagerTest {

    private lateinit var testContext: Context
    private lateinit var syncManager: CadenceMusicSyncManager

    @Before
    fun setUp() {
        testContext = TestMusicContext()
        syncManager = CadenceMusicSyncManager(testContext)
    }

    @Test
    fun testInitialTrackState() {
        val state = syncManager.trackState.value
        assertNotNull(state)
        assertTrue(state.title.isNotBlank())
        assertTrue(state.targetCadenceSpm > 0)
    }

    @Test
    fun testSetTargetCadenceSpm() {
        syncManager.setTargetCadenceSpm(180)
        val state = syncManager.trackState.value
        assertEquals(180, state.targetCadenceSpm)
        assertTrue(state.syncStatusMessage.contains("180 SPM"))
    }

    @Test
    fun testTogglePlayPause() {
        val initialPlaying = syncManager.trackState.value.isPlaying
        syncManager.togglePlayPause()

        val newPlaying = syncManager.trackState.value.isPlaying
        assertEquals(!initialPlaying, newPlaying)
    }

    @Test
    fun testSelectPlatform() {
        syncManager.selectPlatform(MusicPlatform.YOUTUBE_MUSIC)
        val state = syncManager.trackState.value
        assertEquals("YouTube Music", state.platformName)
        assertTrue(state.syncStatusMessage.contains("YouTube Music"))
    }
}
