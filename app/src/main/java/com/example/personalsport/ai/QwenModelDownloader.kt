package com.example.personalsport.ai

import android.content.Context
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

/**
 * Estados detallados del gestor explícito de descargas del modelo Qwen LLM.
 */
sealed class QwenModelDownloadState {
    object NotDownloaded : QwenModelDownloadState()
    data class Downloading(
        val progressPercent: Int,
        val bytesDownloaded: Long,
        val totalBytes: Long
    ) : QwenModelDownloadState()
    data class Ready(val file: File) : QwenModelDownloadState()
    data class Error(val message: String) : QwenModelDownloadState()
}

/**
 * Servicio encargado de la descarga bajo demanda del archivo GGUF del modelo Qwen LLM de IA local.
 *
 * Protección contra fugas y manejo de Corrutinas:
 * - Aplica comprobación de cancelación activa por bucle mediante [ensureActive] y [yield].
 */
object QwenModelDownloader {
    private const val TAG = "QwenModelDownloader"
    const val MODEL_FILENAME = "qwen2.5-3b-instruct-q4_k_m.gguf"
    const val HUGGING_FACE_URL = "https://huggingface.co/Qwen/Qwen2.5-3B-Instruct-GGUF/resolve/main/qwen2.5-3b-instruct-q4_k_m.gguf"

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

    private val _downloadState = MutableStateFlow<QwenModelDownloadState>(QwenModelDownloadState.NotDownloaded)
    val downloadState: StateFlow<QwenModelDownloadState> = _downloadState.asStateFlow()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    fun getModelFile(context: Context): File {
        val modelsDir = File(context.filesDir, "models")
        if (!modelsDir.exists()) {
            modelsDir.mkdirs()
        }
        return File(modelsDir, MODEL_FILENAME)
    }

    fun checkModelStatus(context: Context): QwenModelDownloadState {
        val file = getModelFile(context)
        val currentState = _downloadState.value

        if (currentState is QwenModelDownloadState.Downloading) {
            return currentState
        }

        return if (file.exists() && file.length() > 1_000_000L) {
            val readyState = QwenModelDownloadState.Ready(file)
            _downloadState.value = readyState
            if (!QwenLlmService.isModelLoaded()) {
                QwenLlmService.initModel(context, file.name)
            }
            readyState
        } else {
            val notDownloaded = QwenModelDownloadState.NotDownloaded
            _downloadState.value = notDownloaded
            notDownloaded
        }
    }

    /**
     * Inicia la descarga activa del archivo GGUF desde la red con protección contra fugas de memoria.
     */
    fun startDownload(context: Context, scope: CoroutineScope) {
        if (_downloadState.value is QwenModelDownloadState.Downloading) {
            logW("La descarga ya está en progreso.")
            return
        }

        scope.launch(Dispatchers.IO) {
            val targetFile = getModelFile(context)
            try {
                _downloadState.value = QwenModelDownloadState.Downloading(0, 0L, 1_900_000_000L)

                val request = Request.Builder()
                    .url(HUGGING_FACE_URL)
                    .addHeader("User-Agent", "PersonalSport-Android")
                    .build()

                val response = okHttpClient.newCall(request).execute()
                if (!response.isSuccessful) {
                    val errMsg = "HTTP Error ${response.code}: ${response.message}"
                    _downloadState.value = QwenModelDownloadState.Error(errMsg)
                    return@launch
                }

                val body = response.body
                if (body == null) {
                    _downloadState.value = QwenModelDownloadState.Error("El cuerpo de la respuesta es nulo")
                    return@launch
                }

                val contentLength = body.contentLength().coerceAtLeast(1_900_000_000L)
                val tempFile = File(targetFile.parentFile, "${MODEL_FILENAME}.tmp")

                body.byteStream().use { inputStream ->
                    FileOutputStream(tempFile).use { outputStream ->
                        val buffer = ByteArray(64 * 1024)
                        var bytesRead: Int
                        var totalBytesRead = 0L
                        var lastProgressPercent = -1

                        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                            currentCoroutineContext().ensureActive()
                            outputStream.write(buffer, 0, bytesRead)
                            totalBytesRead += bytesRead

                            val percent = if (contentLength > 0) {
                                ((totalBytesRead * 100) / contentLength).toInt().coerceIn(0, 99)
                            } else 0

                            if (percent != lastProgressPercent) {
                                lastProgressPercent = percent
                                _downloadState.value = QwenModelDownloadState.Downloading(
                                    progressPercent = percent,
                                    bytesDownloaded = totalBytesRead,
                                    totalBytes = contentLength
                                )
                                yield()
                            }
                        }
                    }
                }

                if (tempFile.exists()) {
                    if (targetFile.exists()) {
                        targetFile.delete()
                    }
                    tempFile.renameTo(targetFile)
                }

                val readyState = QwenModelDownloadState.Ready(targetFile)
                _downloadState.value = readyState

                withContext(Dispatchers.Main) {
                    QwenLlmService.initModel(context, targetFile.name)
                }
                logI("¡Descarga del modelo Qwen completada con éxito!")

            } catch (e: Exception) {
                logE("Error al descargar el modelo", e)
                _downloadState.value = QwenModelDownloadState.Error(e.localizedMessage ?: "Error de red")
            }
        }
    }

    /**
     * Permite importar un modelo local guardado en el dispositivo.
     */
    fun setLocalModelFromUri(context: Context, uri: Uri, scope: CoroutineScope) {
        scope.launch(Dispatchers.IO) {
            try {
                _downloadState.value = QwenModelDownloadState.Downloading(10, 0L, 0L)
                val targetFile = getModelFile(context)

                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(targetFile).use { output ->
                        val buffer = ByteArray(64 * 1024)
                        var read: Int
                        var total = 0L
                        while (input.read(buffer).also { read = it } != -1) {
                            currentCoroutineContext().ensureActive()
                            output.write(buffer, 0, read)
                            total += read
                        }
                    }
                }

                val readyState = QwenModelDownloadState.Ready(targetFile)
                _downloadState.value = readyState

                withContext(Dispatchers.Main) {
                    QwenLlmService.initModel(context, targetFile.name)
                }
                logI("¡Modelo local copiado e inicializado correctamente!")
            } catch (e: Exception) {
                logE("Error al copiar modelo GGUF local", e)
                _downloadState.value = QwenModelDownloadState.Error("Error copiando archivo GGUF local: ${e.message}")
            }
        }
    }

    fun deleteModel(context: Context) {
        val file = getModelFile(context)
        if (file.exists()) {
            file.delete()
        }
        QwenLlmService.releaseModel()
        _downloadState.value = QwenModelDownloadState.NotDownloaded
    }
}
