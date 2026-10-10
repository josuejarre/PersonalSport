package com.example.personalsport.ui.navigation

enum class TopLevelRoute(
    val title: String,
    val subtitle: String,
    val navKey: NavKey
) {
    DASHBOARD("Dashboard (Inicio)", "Visión general & Métricas de salud", NavKey.Dashboard),
    PLANNER("Planner (Planificador)", "Planificador de Entrenamientos", NavKey.Planner),
    EQUIPMENT("Equipment Manager", "Catálogo de Máquinas & Equipos", NavKey.Equipment),
    MANUAL_ENTRY("Manual Entry", "Registro Manual de Sesiones", NavKey.ManualEntry),
    PROGRESS("Progress Photos", "Fotos de Progreso & Comparativas", NavKey.ProgressPhotos),
    DIET("Dietas & Nutrición", "Planes de Alimentación & Dietas", NavKey.Diet),
    SUPPLEMENTS("Suplementos", "Catálogo & Suplementación AI", NavKey.Supplements),
    TRAINERS("Entrenadores Personales", "Asesoría & Entrenadores", NavKey.PersonalTrainer),
    DEVICES_MANAGER("Dispositivos", "Relojes, Básculas & Monitores", NavKey.DevicesManager),
    ACHIEVEMENTS("Logros & Trofeos", "Nivel, XP & Trofeos Olímpicos", NavKey.Achievements)
}
