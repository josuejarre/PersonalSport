package com.example.personalsport.ai

import android.content.Context
import android.content.ContextWrapper
import com.example.personalsport.health.TestContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class TestContextWithFilesDir(private val tempDir: File) : ContextWrapperOverride() {
    override fun getFilesDir(): File {
        val models = File(tempDir, "files")
        if (!models.exists()) models.mkdirs()
        return models
    }
    override fun getApplicationContext(): Context = this
}

open class ContextWrapperOverride : ContextWrapper(TestContext())

@OptIn(ExperimentalCoroutinesApi::class)
class AutoModelManagerTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var testContext: Context

    @Before
    fun setUp() {
        testContext = TestContextWithFilesDir(tempFolder.root)
    }

    @Test
    fun testInitialStatusAndConstants() {
        val status = AutoModelManager.status.value
        assertNotNull(status)
        assertEquals("qwen2.5-3b-instruct-q4_k_m.gguf", AutoModelManager.MODEL_FILENAME)
        assertEquals("Qwen2.5-3B-Instruct.Q4_K_M.gguf", AutoModelManager.ALT_MODEL_FILENAME)
        assertTrue(AutoModelManager.HUGGING_FACE_URL.contains("Qwen/Qwen2.5-3B-Instruct-GGUF"))
    }

    @Test
    fun testGetModelFile() {
        val file = AutoModelManager.getModelFile(testContext)
        assertNotNull(file)
        assertTrue(file.absolutePath.contains("models"))
        assertTrue(file.name == AutoModelManager.MODEL_FILENAME || file.name == AutoModelManager.ALT_MODEL_FILENAME)
    }

    @Test
    fun testAutoDownloadWithExistingFile() = runTest {
        val modelsDir = File(testContext.filesDir, "models")
        modelsDir.mkdirs()
        val dummyModel = File(modelsDir, AutoModelManager.MODEL_FILENAME)

        // Write > 1MB dummy content
        val dummyData = ByteArray(1_050_000)
        dummyModel.writeBytes(dummyData)

        val job = AutoModelManager.startAutoDownloadAndInit(
            context = testContext,
            scope = this
        )
        job.join()

        val currentStatus = AutoModelManager.status.value
        assertTrue(currentStatus is AutoModelStatus.Ready)
        if (currentStatus is AutoModelStatus.Ready) {
            assertEquals(dummyModel.name, currentStatus.modelFile.name)
        }
    }

    @Test
    fun testAutoDownloadWithMissingFileFailure() = runTest {
        // Pass a mock client that returns 404
        val mockClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(404)
                    .message("Not Found")
                    .body("Model not found".toResponseBody("text/plain".toMediaTypeOrNull()))
                    .build()
            }
            .build()

        val job = AutoModelManager.startAutoDownloadAndInit(
            context = testContext,
            scope = this,
            customHttpClient = mockClient
        )
        job.join()

        val currentStatus = AutoModelManager.status.value
        assertTrue(currentStatus is AutoModelStatus.Error)
        if (currentStatus is AutoModelStatus.Error) {
            assertTrue(currentStatus.message.contains("404"))
        }
    }

    @Test
    fun testIsModelReadyInitialState() {
        val isReady = AutoModelManager.isModelReady(testContext)
        assertFalse(isReady)
    }
}
