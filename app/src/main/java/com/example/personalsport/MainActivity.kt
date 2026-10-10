package com.example.personalsport

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.personalsport.ai.AutoModelManager
import com.example.personalsport.ui.navigation.AppNavigationSuite
import com.example.personalsport.ui.theme.PersonalSportTheme

import com.example.personalsport.util.AppCoilImageLoader

/**
 * Actividad principal de la aplicación PersonalSport.
 * Punto de entrada para la interfaz gráfica basada en Jetpack Compose,
 * inicio de descarga automática del modelo de IA y soporte Edge-to-Edge.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inicializa el cargador de imágenes optimizado de la app
        AppCoilImageLoader.get(applicationContext)

        AutoModelManager.startAutoDownloadAndInit(applicationContext)

        val app = PersonalSportApp.getInstance(applicationContext)
        val healthSyncManager = app.healthSyncManager

        enableEdgeToEdge()
        setContent {
            PersonalSportTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigationSuite(healthSyncManager = healthSyncManager)
                }
            }
        }
    }
}
