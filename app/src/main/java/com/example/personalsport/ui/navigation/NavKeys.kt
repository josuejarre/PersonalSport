package com.example.personalsport.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed interface NavKey {
    @Serializable
    data object Dashboard : NavKey
    @Serializable
    data object Planner : NavKey
    @Serializable
    data object Equipment : NavKey
    @Serializable
    data object Supplements : NavKey
    @Serializable
    data object PersonalTrainer : NavKey
    @Serializable
    data object ProgressPhotos : NavKey
    @Serializable
    data object ManualEntry : NavKey
    @Serializable
    data object Diet : NavKey
    @Serializable
    data object HealthSync : NavKey
    @Serializable
    data object DevicesManager : NavKey
    @Serializable
    data object Achievements : NavKey
}
