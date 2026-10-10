package com.example.personalsport.audio

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/**
 * Entrenador de Audio por Voz mediante Sintetizador TextToSpeech ([AudioCoachManager]).
 *
 * Genera indicaciones por voz en tiempo real en español para transiciones de intervalos,
 * cuenta regresiva de descansos, cambio de ejercicios e incidencias en zonas de frecuencia cardíaca.
 *
 * **Gestión de Estado e Hilos**:
 * - Inicializa la API TTS nativa de Android de forma asíncrona.
 * - Expone los estados reactivos [isInitialized], [isSpeaking] y [lastAnnouncedText] mediante [StateFlow]
 *   para actualización fluida de la interfaz en el hilo principal de Compose UI.
 *
 * @param context Contexto de la aplicación Android.
 * @param locale Configuración regional de idioma en español (por defecto `es_ES`).
 */
class AudioCoachManager(
    context: Context,
    private val locale: Locale = Locale("es", "ES")
) : TextToSpeech.OnInitListener {

    private val appContext = context.applicationContext
    private var tts: TextToSpeech? = null

    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _lastAnnouncedText = MutableStateFlow("")
    val lastAnnouncedText: StateFlow<String> = _lastAnnouncedText.asStateFlow()

    var isMuted: Boolean = false
    var volume: Float = 1.0f
        set(value) {
            field = value.coerceIn(0.0f, 1.0f)
        }
    var speechRate: Float = 1.0f
        set(value) {
            field = value.coerceIn(0.5f, 2.0f)
            tts?.setSpeechRate(field)
        }

    init {
        try {
            tts = TextToSpeech(appContext, this)
        } catch (e: Exception) {
            Log.e(TAG, "Error al instanciar TextToSpeech: ${e.message}")
            _isInitialized.value = false
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(locale)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                val fallbackResult = tts?.setLanguage(Locale("es"))
                if (fallbackResult == TextToSpeech.LANG_MISSING_DATA || fallbackResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.w(TAG, "Idioma español no soportado en TTS")
                    _isInitialized.value = false
                    return
                }
            }
            
            tts?.setSpeechRate(speechRate)
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _isSpeaking.value = true
                }

                override fun onDone(utteranceId: String?) {
                    _isSpeaking.value = false
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    _isSpeaking.value = false
                }
            })

            _isInitialized.value = true
            Log.d(TAG, "AudioCoachManager TTS inicializado con éxito en idioma español")
        } else {
            Log.e(TAG, "Fallo al inicializar TextToSpeech. Estado: $status")
            _isInitialized.value = false
        }
    }

    /**
     * Anuncia el inicio de una serie de intervalos en la pista o piscina.
     *
     * @param seriesName Nombre del bloque o serie.
     * @param distanceMeters Distancia objetivo en metros (ej. 400m).
     * @param targetPaceMinPerKm Ritmo objetivo formateado ("3:50 /km").
     * @return Texto anunciado por voz en español.
     */
    fun announceIntervalSwitch(seriesName: String, distanceMeters: Int, targetPaceMinPerKm: String): String {
        val text = "¡Serie de ${distanceMeters}m iniciada para $seriesName! Ritmo objetivo $targetPaceMinPerKm min/km"
        speak(text)
        return text
    }

    /**
     * Anuncia la cuenta regresiva del temporizador de descanso.
     *
     * @param secondsRemaining Segundos restantes de descanso.
     * @return Texto anunciado.
     */
    fun announceRestCountdown(secondsRemaining: Int): String {
        val text = when (secondsRemaining) {
            30 -> "30 segundos de descanso restante. Prepárate."
            15 -> "15 segundos de descanso."
            10 -> "10 segundos de descanso restante"
            5 -> "5 segundos, 4, 3, 2, 1"
            0 -> "¡Descanso terminado! ¡A entrenar!"
            else -> "$secondsRemaining segundos de descanso restante"
        }
        speak(text)
        return text
    }

    /**
     * Anuncia la transición hacia el siguiente ejercicio de la rutina.
     *
     * @param nextExerciseName Nombre del ejercicio entrante.
     * @param repsOrDuration Repeticiones o tiempo asignado.
     * @return Texto anunciado.
     */
    fun announceExerciseTransition(nextExerciseName: String, repsOrDuration: String): String {
        val text = "Siguiente ejercicio: $nextExerciseName, $repsOrDuration"
        speak(text)
        return text
    }

    /**
     * Anuncia una alerta de zona de frecuencia cardíaca durante la sesión.
     *
     * @param currentBpm Frecuencia cardíaca actual en latidos por minuto.
     * @param zoneName Nombre de la zona de pulso ("Zona Anaeróbica", "Zona Láctica", etc.).
     * @param actionAdvice Indicación táctica de ajuste de ritmo.
     * @return Texto anunciado.
     */
    fun announceHeartRateZoneAlert(currentBpm: Int, zoneName: String, actionAdvice: String): String {
        val text = "Alerta de frecuencia cardíaca: $currentBpm BPM en $zoneName. $actionAdvice"
        speak(text)
        return text
    }

    /**
     * Sintetiza y reproduce el texto especificado en español mediante la API de voz.
     *
     * @param text Cadena de texto a reproducir.
     * @return `true` si el comando de voz se envió exitosamente al sintetizador.
     */
    fun speak(text: String): Boolean {
        _lastAnnouncedText.value = text
        if (isMuted) {
            Log.d(TAG, "TTS en silencio (Muted). Texto omitido: $text")
            return false
        }

        val ttsInstance = tts
        if (ttsInstance != null && _isInitialized.value) {
            val params = Bundle().apply {
                putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, volume)
            }
            val utteranceId = "audio_coach_${System.currentTimeMillis()}"
            val result = ttsInstance.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
            return result == TextToSpeech.SUCCESS
        } else {
            Log.w(TAG, "TTS no disponible o no inicializado. Anuncio simulado: $text")
            return false
        }
    }

    /**
     * Detiene inmediatamente la reproducción de voz actual.
     */
    fun stop() {
        try {
            tts?.stop()
            _isSpeaking.value = false
        } catch (e: Exception) {
            Log.e(TAG, "Error deteniendo TTS: ${e.message}")
        }
    }

    /**
     * Cierra el servicio TTS y libera todos los recursos del sistema.
     */
    fun shutdown() {
        try {
            tts?.stop()
            tts?.shutdown()
            tts = null
            _isInitialized.value = false
            _isSpeaking.value = false
        } catch (e: Exception) {
            Log.e(TAG, "Error cerrando TTS: ${e.message}")
        }
    }

    companion object {
        private const val TAG = "AudioCoachManager"

        @Volatile
        private var INSTANCE: AudioCoachManager? = null

        /**
         * Devuelve la instancia singleton de [AudioCoachManager].
         */
        fun getInstance(context: Context): AudioCoachManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AudioCoachManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
