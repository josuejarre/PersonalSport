package com.example.personalsport.util

import android.content.Context
import android.os.Build
import coil.ImageLoader
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.disk.DiskCache
import coil.memory.MemoryCache

/**
 * Singleton encargado de proporcionar e inicializar una instancia única y compartida de [ImageLoader]
 * optimizada para toda la aplicación PersonalSport.
 *
 * Configuración de rendimiento y optimización de memoria:
 * - **Caché en Memoria**: Asigna el 25% de la RAM disponible para la aplicación, evitando 'Out Of Memory' (OOM)
 *   y eliminando la sobrecarga del recolector de basura (GC thrashing) durante desplazamientos rápidos de listas.
 * - **Caché en Disco**: Reserva un máximo de 50 MB en el directorio de caché interno para imágenes y GIFs persistentes.
 * - **Aceleración por Hardware**: Activa el uso de Bitmaps gestionados por hardware (`allowHardware(true)`) para
 *   minimizar el consumo de memoria en la GPU/CPU y garantizar animaciones y renders fluidos a 120 Hz.
 * - **Decodificación GIF Compartida**: Utiliza un pool único de [ImageDecoderDecoder.Factory] en Android 9+ (API 28+)
 *   o [GifDecoder.Factory] en versiones anteriores para animaciones de ejercicios.
 */
object AppCoilImageLoader {

    @Volatile
    private var instance: ImageLoader? = null

    /**
     * Devuelve la instancia global de [ImageLoader], creándola de forma segura si aún no existe.
     *
     * @param context Contexto de la aplicación o del componente UI.
     * @return Instancia compartida optimizada de [ImageLoader].
     */
    fun get(context: Context): ImageLoader {
        return instance ?: synchronized(this) {
            instance ?: buildImageLoader(context.applicationContext).also { instance = it }
        }
    }

    private fun buildImageLoader(context: Context): ImageLoader {
        return ImageLoader.Builder(context)
            .memoryCache {
                MemoryCache.Builder(context)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("image_cache"))
                    .maxSizeBytes(50L * 1024L * 1024L) // 50 MB
                    .build()
            }
            .components {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .allowHardware(true)
            .build()
    }
}
