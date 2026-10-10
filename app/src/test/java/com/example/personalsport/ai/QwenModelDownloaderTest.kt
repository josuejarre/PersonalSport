package com.example.personalsport.ai

import org.junit.Assert.*
import org.junit.Test

class QwenModelDownloaderTest {

    @Test
    fun testInitialDownloadState() {
        val state = QwenModelDownloader.downloadState.value
        assertNotNull(state)
        assertTrue(
            state is QwenModelDownloadState.NotDownloaded ||
            state is QwenModelDownloadState.Ready ||
            state is QwenModelDownloadState.Error
        )
    }

    @Test
    fun testModelFilenameAndUrl() {
        assertEquals("qwen2.5-3b-instruct-q4_k_m.gguf", QwenModelDownloader.MODEL_FILENAME)
        assertTrue(QwenModelDownloader.HUGGING_FACE_URL.contains("Qwen/Qwen2.5-3B-Instruct-GGUF"))
        assertTrue(QwenModelDownloader.HUGGING_FACE_URL.endsWith(".gguf"))
    }
}
