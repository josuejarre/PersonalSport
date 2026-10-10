package com.example.personalsport.ai

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

/**
 * Estados del proceso de descarga e inicialización automática del modelo LLM de IA local Qwen.
 */
sealed class AutoModelStatus {
    object Idle : AutoModelStatus()
    object Checking : AutoModelStatus()
    data class Downloading(val progressPercent: Int, val bytesDownloaded: Long, val totalBytes: Long) : AutoModelStatus()
    data class Ready(val modelFile: File) : AutoModelStatus()
    data class Error(val message: String) : AutoModelStatus()
}

/**
 * Gestor Singleton encargado de la verificación, descarga en segundo plano e inicialización transparente
 * del modelo de lenguaje en formato GGUF para la IA local de PersonalSport.
 *
 * Consideraciones de Concurrencia Estructurada y Cancelación:
 * - Cancela activamente la transferencia si el subproceso o CoroutineScope es cancelado mediante [ensureActive] y [yield].
 * - Previene fugas de memoria y uso innecesario de datos móviles/batería al cerrar o pausar la app.
 */
object AutoModelManager {
    private const val TAG = "AutoModelManager"
    const val MODEL_FILENAME = "qwen2.5-3b-instruct-q4_k_m.gguf"
    const val ALT_MODEL_FILENAME = "Qwen2.5-3B-Instruct.Q4_K_M.gguf"
    const val HUGGING_FACE_URL = "https://huggingface.co/Qwen/Qwen2.5-3B-Instruct-GGUF/resolve/main/qwen2.5-3b-instruct-q4_k_m.gguf"

    private val _status = MutableStateFlow<AutoModelStatus>(AutoModelStatus.Idle)
    val status: StateFlow<AutoModelStatus> = _status.asStateFlow()

    private var currentJob: Job? = null

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    private fun logI(msg: String) {
        try { Log.i(TAG, msg) } catch (_: Throwable) { println("[$TAG] $msg") }
    }
    private fun logW(msg: String) {
        try { Log.w(TAG, msg) } catch (_: Throwable) { println("[$TAG] $msg") }
    }
    private fun logE(msg: String, tr: Throwable? = null) {
        try {
            if (tr != null) Log.e(TAG, msg, tr) else Log.e(TAG, msg)
        } catch (_: Throwable) {
            println("[$TAG] $msg ${tr?.message ?: ""}")
        }
    }

    /**
     * Obtiene el archivo del modelo en el almacenamiento local de la app.
     */
    fun getModelFile(context: Context): File {
        val modelsDir = File(context.filesDir, "models")
        if (!modelsDir.exists()) {
            modelsDir.mkdirs()
        }
        val primaryFile = File(modelsDir, MODEL_FILENAME)
        if (primaryFile.exists() && primaryFile.length() > 100L) {
            return primaryFile
        }
        val altFile = File(modelsDir, ALT_MODEL_FILENAME)
        if (altFile.exists() && altFile.length() > 100L) {
            return altFile
        }
        return primaryFile
    }

    /**
     * Inicia la descarga transparente en segundo plano del modelo de IA si no existe de forma local.
     * Implementa la verificación de cancelación activa mediante [ensureActive] y [yield].
     */
    fun startAutoDownloadAndInit(
        context: Context,
        scope: CoroutineScope = CoroutineScope(Dispatchers.IO),
        customHttpClient: OkHttpClient? = null
    ): Job {
        _status.value = AutoModelStatus.Checking
        val job = scope.launch {
            try {
                val modelFile = getModelFile(context)
                if (modelFile.exists() && modelFile.length() > 1_000_000L) {
                    _status.value = AutoModelStatus.Ready(modelFile)
                    if (!QwenLlmService.isModelLoaded()) {
                        QwenLlmService.initModel(context, modelFile.name)
                    }
                    logI("El archivo del modelo ya existe. QwenLlmService inicializado.")
                    return@launch
                }

                _status.value = AutoModelStatus.Downloading(0, 0L, 1_900_000_000L)

                val client = customHttpClient ?: okHttpClient
                val request = Request.Builder()
                    .url(HUGGING_FACE_URL)
                    .addHeader("User-Agent", "PersonalSport-Android")
                    .build()

                val response = client.newCall(request).execute()
                if (!response.isSuccessful) {
                    val errMsg = "HTTP ${response.code}: ${response.message}"
                    _status.value = AutoModelStatus.Error(errMsg)
                    logW("Error HTTP en descarga silenciosa: $errMsg")
                    return@launch
                }

                val body = response.body
                if (body == null) {
                    _status.value = AutoModelStatus.Error("El cuerpo de la respuesta es nulo")
                    logW("Cuerpo de respuesta nulo")
                    return@launch
                }

                val contentLength = body.contentLength().coerceAtLeast(1_900_000_000L)
                val tempFile = File(modelFile.parentFile, "${MODEL_FILENAME}.tmp")

                body.byteStream().use { input ->
                    FileOutputStream(tempFile).use { output ->
                        val buffer = ByteArray(64 * 1024)
                        var read: Int
                        var totalRead = 0L
                        var lastPct = -1

                        while (input.read(buffer).also { read = it } != -1) {
                            currentCoroutineContext().ensureActive()
                            output.write(buffer, 0, read)
                            totalRead += read

                            val pct = if (contentLength > 0) {
                                ((totalRead * 100) / contentLength).toInt().coerceIn(0, 99)
                            } else 0

                            if (pct != lastPct) {
                                lastPct = pct
                                _status.value = AutoModelStatus.Downloading(
                                    progressPercent = pct,
                                    bytesDownloaded = totalRead,
                                    totalBytes = contentLength
                                )
                                yield() // Oportunidad de suspensión y cancelación limpia
                            }
                        }
                    }
                }

                if (tempFile.exists()) {
                    if (modelFile.exists()) {
                        modelFile.delete()
                    }
                    tempFile.renameTo(modelFile)
                }

                _status.value = AutoModelStatus.Ready(modelFile)

                QwenLlmService.initModel(context, modelFile.name)
                logI("Descarga e inicialización silenciosa completada con éxito.")

            } catch (e: Exception) {
                logE("Descarga silenciosa fallida en segundo plano", e)
                _status.value = AutoModelStatus.Error(e.localizedMessage ?: "Error de red durante la descarga")
            }
        }
        currentJob = job
        return job
    }

    /**
     * Comprueba si el modelo está descargado y cargado en memoria listo para inferencia.
     */
    fun isModelReady(context: Context): Boolean {
        val file = getModelFile(context)
        return file.exists() && file.length() > 1_000_000L && QwenLlmService.isModelLoaded()
    }
}
