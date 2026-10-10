package com.example.personalsport.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.personalsport.health.HealthSyncManager
import com.example.personalsport.ui.achievements.AchievementsScreen
import com.example.personalsport.ui.dashboard.DashboardScreen
import com.example.personalsport.ui.devices.DevicesManagerScreen
import com.example.personalsport.ui.diet.DietScreen
import com.example.personalsport.ui.equipment.EquipmentScreen
import com.example.personalsport.ui.health.HealthSyncScreen
import com.example.personalsport.ui.manualentry.ManualEntryScreen
import com.example.personalsport.ui.planner.PlannerScreen
import com.example.personalsport.ui.progress.ProgressPhotoScreen
import com.example.personalsport.ui.supplement.SupplementHubScreen
import com.example.personalsport.ui.trainer.PersonalTrainerScreen

/**
 * Grafo de Navegación Principal de la aplicación PersonalSport ([NavGraph]).
 *
 * Administra el enrutamiento y la renderización reactiva de las pantallas basada en la clave activa [NavKey].
 *
 * **Manejo de Estado y Despacho de Hilos**:
 * - La navegación se gestiona mediante eventos de estado inmutables en el hilo principal de Compose UI (Dispatchers.Main).
 * - Se integra con [HealthSyncManager] para pasar referencias a las pantallas que requieren sincronización de dispositivos.
 *
 * @param healthSyncManager Gestor de sincronización de datos de salud y wearables.
 * @param currentKey Clave de navegación activa en el estado global [NavKey].
 * @param onNavigate Callback para solicitar un cambio de pantalla.
 * @param onOpenDrawer Callback para desplegar el menú de navegación lateral.
 * @param onOpenProfile Callback para abrir el diálogo del perfil de usuario.
 * @param modifier Modificador de diseño Compose.
 */
@Composable
fun NavGraph(
    healthSyncManager: HealthSyncManager,
    currentKey: NavKey = NavKey.Dashboard,
    onNavigate: (NavKey) -> Unit = {},
    onOpenDrawer: () -> Unit = {},
    onOpenProfile: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    when (currentKey) {
        NavKey.Dashboard -> DashboardScreen(
            healthSyncManager = healthSyncManager,
            onNavigateToPlanner = { onNavigate(NavKey.Planner) },
            onNavigateToEquipment = { onNavigate(NavKey.Equipment) },
            onNavigateToLog = { onNavigate(NavKey.ManualEntry) },
            onNavigateToProgress = { onNavigate(NavKey.ProgressPhotos) },
            onNavigateToSupplements = { onNavigate(NavKey.Supplements) },
            onNavigateToTrainers = { onNavigate(NavKey.PersonalTrainer) },
            onNavigateToDiet = { onNavigate(NavKey.Diet) },
            onOpenDrawer = onOpenDrawer,
            onOpenProfile = onOpenProfile,
            modifier = modifier
        )
        NavKey.Planner -> PlannerScreen(
            onNavigateToEquipment = { onNavigate(NavKey.Equipment) },
            onOpenDrawer = onOpenDrawer,
            onOpenProfile = onOpenProfile,
            modifier = modifier
        )
        NavKey.Equipment -> EquipmentScreen(
            onOpenDrawer = onOpenDrawer,
            onOpenProfile = onOpenProfile,
            modifier = modifier
        )
        NavKey.Supplements -> SupplementHubScreen(
            onOpenDrawer = onOpenDrawer,
            onOpenProfile = onOpenProfile,
            modifier = modifier
        )
        NavKey.PersonalTrainer -> PersonalTrainerScreen(
            onOpenDrawer = onOpenDrawer,
            onOpenProfile = onOpenProfile,
            modifier = modifier
        )
        NavKey.ProgressPhotos -> ProgressPhotoScreen(
            onOpenDrawer = onOpenDrawer,
            onOpenProfile = onOpenProfile,
            modifier = modifier
        )
        NavKey.ManualEntry -> ManualEntryScreen(
            onSaveComplete = { onNavigate(NavKey.Dashboard) },
            onOpenDrawer = onOpenDrawer,
            onOpenProfile = onOpenProfile,
            modifier = modifier
        )
        NavKey.Diet -> DietScreen(
            onOpenDrawer = onOpenDrawer,
            onOpenProfile = onOpenProfile,
            modifier = modifier
        )
        NavKey.HealthSync -> HealthSyncScreen(
            onOpenDrawer = onOpenDrawer,
            onOpenProfile = onOpenProfile,
            modifier = modifier
        )
        NavKey.DevicesManager -> DevicesManagerScreen(
            onOpenDrawer = onOpenDrawer,
            onOpenProfile = onOpenProfile,
            modifier = modifier
        )
        NavKey.Achievements -> AchievementsScreen(
            onOpenDrawer = onOpenDrawer,
            onOpenProfile = onOpenProfile,
            modifier = modifier
        )
    }
}
