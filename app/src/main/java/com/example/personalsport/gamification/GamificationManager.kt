package com.example.personalsport.gamification

import android.content.Context
import android.content.SharedPreferences
import com.example.personalsport.data.local.entity.WorkoutLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class TrophyCategory {
    ALL, WORKOUT, RUNNING_SWIMMING, DIET, SLEEP, CALISTHENICS
}

data class Trophy(
    val id: String,
    val title: String,
    val description: String,
    val category: TrophyCategory,
    val iconName: String,
    val isUnlocked: Boolean = false,
    val unlockedAt: String? = null,
    val progress: Int = 0,
    val maxProgress: Int = 100
)

data class GamificationState(
    val totalXp: Int = 0,
    val currentLevel: Int = 1,
    val levelTitle: String = "Nivel 1 Novato",
    val xpForCurrentLevel: Int = 0,
    val xpForNextLevel: Int = 500,
    val levelProgressPercent: Float = 0f,
    val workoutStreakDays: Int = 0,
    val dietStreakDays: Int = 0,
    val totalWorkoutsCompleted: Int = 0,
    val trophies: List<Trophy> = emptyList(),
    val recentXpGains: List<String> = emptyList()
)

/**
 * Gestor de Gamificación, Niveles, Experiencia (XP) y Trofeos Olímpicos.
 * Administra la progresión desde el Nivel 1 (Novato) hasta el Nivel 50 (Leyenda Olímpica).
 */
class GamificationManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("gamification_prefs", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(GamificationState())
    val uiState: StateFlow<GamificationState> = _uiState.asStateFlow()

    init {
        loadState()
    }

    private fun loadState() {
        val totalXp = prefs.getInt(KEY_TOTAL_XP, 0)
        val dietStreak = prefs.getInt(KEY_DIET_STREAK, 0)
        val workoutStreak = prefs.getInt(KEY_WORKOUT_STREAK, 0)
        val completedWorkouts = prefs.getInt(KEY_WORKOUTS_COUNT, 0)
        val unlockedTrophiesSet = prefs.getStringSet(KEY_UNLOCKED_TROPHIES, emptySet()) ?: emptySet()

        val allTrophies = getDefaultTrophies().map { trophy ->
            if (unlockedTrophiesSet.contains(trophy.id)) {
                trophy.copy(
                    isUnlocked = true,
                    progress = trophy.maxProgress,
                    unlockedAt = prefs.getString("trophy_date_${trophy.id}", "Desbloqueado")
                )
            } else {
                trophy
            }
        }

        updateStateAndSave(
            newTotalXp = totalXp,
            dietStreak = dietStreak,
            workoutStreak = workoutStreak,
            completedWorkouts = completedWorkouts,
            trophies = allTrophies,
            recentGainText = null
        )
    }

    /**
     * Otorga XP por completar un entrenamiento.
     */
    fun awardWorkoutXp(durationMinutes: Int, calories: Int, activityType: String = "Entrenamiento"): Int {
        val basePoints = 100
        val durationBonus = (durationMinutes / 10) * 15
        val calorieBonus = (calories / 100) * 10
        val totalEarned = basePoints + durationBonus + calorieBonus

        val current = _uiState.value
        val newWorkoutsCount = current.totalWorkoutsCompleted + 1
        val newWorkoutStreak = current.workoutStreakDays + 1

        val reason = "+$totalEarned XP: $activityType (${durationMinutes} min, ${calories} kcal)"
        addXpInternal(totalEarned, reason, newWorkoutsCount, newWorkoutStreak, current.dietStreakDays)

        // Evaluar trofeos
        checkTrophyUnlocks(
            workoutType = activityType,
            durationMinutes = durationMinutes,
            calories = calories
        )

        return totalEarned
    }

    /**
     * Otorga XP por adherencia al plan de alimentación diario / racha.
     */
    fun awardDietAdherenceXp(dayStreak: Int): Int {
        val basePoints = 150
        val streakBonus = if (dayStreak >= 30) 1000 else dayStreak * 20
        val totalEarned = basePoints + streakBonus

        val current = _uiState.value
        val reason = "+$totalEarned XP: Cumplimiento de Dieta (Racha de $dayStreak días)"

        addXpInternal(totalEarned, reason, current.totalWorkoutsCompleted, current.workoutStreakDays, dayStreak)

        if (dayStreak >= 30) {
            unlockTrophy("trophy_30_days_diet")
        }

        return totalEarned
    }

    /**
     * Otorga XP por métricas de sueño y recuperación.
     */
    fun awardSleepRecoveryXp(sleepScore: Int, sleepHours: Double): Int {
        val earned = when {
            sleepScore >= 85 -> 200
            sleepScore >= 70 -> 120
            else -> 60
        }

        val current = _uiState.value
        val reason = "+$earned XP: Sueño Reparador ($sleepHours h - Score $sleepScore/100)"

        addXpInternal(earned, reason, current.totalWorkoutsCompleted, current.workoutStreakDays, current.dietStreakDays)

        if (sleepScore >= 85) {
            unlockTrophy("trophy_sleep_master")
        }

        return earned
    }

    /**
     * Otorga XP directa por cualquier logro o evento.
     */
    fun awardXp(amount: Int, reason: String) {
        val current = _uiState.value
        addXpInternal(amount, "+$amount XP: $reason", current.totalWorkoutsCompleted, current.workoutStreakDays, current.dietStreakDays)
    }

    private fun addXpInternal(
        earnedXp: Int,
        reasonText: String,
        workoutsCount: Int,
        workoutStreak: Int,
        dietStreak: Int
    ) {
        val current = _uiState.value
        val newTotalXp = current.totalXp + earnedXp

        val updatedRecentGains = (listOf(reasonText) + current.recentXpGains).take(10)

        updateStateAndSave(
            newTotalXp = newTotalXp,
            dietStreak = dietStreak,
            workoutStreak = workoutStreak,
            completedWorkouts = workoutsCount,
            trophies = current.trophies,
            recentGainText = reasonText,
            recentGainsList = updatedRecentGains
        )
    }

    /**
     * Evalúa el desbloqueo de trofeos específicos basados en registros y tiempos.
     */
    fun checkTrophyUnlocks(
        workoutType: String = "",
        durationMinutes: Int = 0,
        calories: Int = 0,
        swolfScore: Int = 99,
        best10kTimeMinutes: Double = 999.0,
        isCalisthenicsMaster: Boolean = false,
        totalTonnageKg: Double = 0.0
    ) {
        if (best10kTimeMinutes in 0.1..45.0) {
            unlockTrophy("trophy_10k_sub45")
        }

        if (swolfScore in 1..34) {
            unlockTrophy("trophy_swolf_sub35")
        }

        if (isCalisthenicsMaster || workoutType.contains("Calistenia", ignoreCase = true)) {
            unlockTrophy("trophy_calisthenics_master")
        }

        if (_uiState.value.totalWorkoutsCompleted >= 1) {
            unlockTrophy("trophy_first_workout")
        }

        if (totalTonnageKg >= 50000) {
            unlockTrophy("trophy_tonnage_50t")
        }
    }

    /**
     * Desbloquea un trofeo por su ID.
     */
    fun unlockTrophy(trophyId: String) {
        val current = _uiState.value
        val updatedTrophies = current.trophies.map { trophy ->
            if (trophy.id == trophyId && !trophy.isUnlocked) {
                val currentDateStr = SimpleDateFormat("dd MMM yyyy", Locale("es", "ES")).format(Date())
                prefs.edit().putString("trophy_date_${trophy.id}", currentDateStr).apply()
                trophy.copy(
                    isUnlocked = true,
                    progress = trophy.maxProgress,
                    unlockedAt = currentDateStr
                )
            } else {
                trophy
            }
        }

        val unlockedIds = updatedTrophies.filter { it.isUnlocked }.map { it.id }.toSet()
        prefs.edit().putStringSet(KEY_UNLOCKED_TROPHIES, unlockedIds).apply()

        updateStateAndSave(
            newTotalXp = current.totalXp,
            dietStreak = current.dietStreakDays,
            workoutStreak = current.workoutStreakDays,
            completedWorkouts = current.totalWorkoutsCompleted,
            trophies = updatedTrophies,
            recentGainText = null
        )
    }

    private fun updateStateAndSave(
        newTotalXp: Int,
        dietStreak: Int,
        workoutStreak: Int,
        completedWorkouts: Int,
        trophies: List<Trophy>,
        recentGainText: String?,
        recentGainsList: List<String>? = null
    ) {
        val levelInfo = calculateLevelFromXp(newTotalXp)
        val recentGains = recentGainsList ?: _uiState.value.recentXpGains

        _uiState.value = GamificationState(
            totalXp = newTotalXp,
            currentLevel = levelInfo.level,
            levelTitle = levelInfo.title,
            xpForCurrentLevel = levelInfo.xpForCurrentLevel,
            xpForNextLevel = levelInfo.xpForNextLevel,
            levelProgressPercent = levelInfo.progressPercent,
            workoutStreakDays = workoutStreak,
            dietStreakDays = dietStreak,
            totalWorkoutsCompleted = completedWorkouts,
            trophies = trophies,
            recentXpGains = recentGains
        )

        prefs.edit()
            .putInt(KEY_TOTAL_XP, newTotalXp)
            .putInt(KEY_DIET_STREAK, dietStreak)
            .putInt(KEY_WORKOUT_STREAK, workoutStreak)
            .putInt(KEY_WORKOUTS_COUNT, completedWorkouts)
            .apply()
    }

    /**
     * Calcula la curva de nivel (Nivel 1 Novato a Nivel 50 Leyenda Olímpica) a partir del XP total.
     */
    fun calculateLevelFromXp(totalXp: Int): LevelInfo {
        var currentLevel = 1
        var accumulatedXpForLevel = 0

        fun xpRequiredForLevel(level: Int): Int {
            return 300 + (level - 1) * 200
        }

        while (currentLevel < 50) {
            val req = xpRequiredForLevel(currentLevel)
            if (accumulatedXpForLevel + req <= totalXp) {
                accumulatedXpForLevel += req
                currentLevel++
            } else {
                break
            }
        }

        val xpInCurrentLevel = totalXp - accumulatedXpForLevel
        val xpNeededForNext = if (currentLevel >= 50) 1 else xpRequiredForLevel(currentLevel)
        val progressPercent = (xpInCurrentLevel.toFloat() / xpNeededForNext.toFloat()).coerceIn(0f, 1f)

        val title = getLevelTitle(currentLevel)

        return LevelInfo(
            level = currentLevel,
            title = "Nivel $currentLevel $title",
            xpForCurrentLevel = xpInCurrentLevel,
            xpForNextLevel = xpNeededForNext,
            progressPercent = progressPercent
        )
    }

    private fun getLevelTitle(level: Int): String {
        return when {
            level >= 50 -> "Leyenda Olímpica 🏆"
            level >= 45 -> "Titán Divino ⚡"
            level >= 40 -> "Maestro Olímpico 🎖️"
            level >= 35 -> "Campeón Mundial 🏅"
            level >= 30 -> "Titán Fitness 🏋️‍♂️"
            level >= 25 -> "Atleta de Élite 🥇"
            level >= 20 -> "Maratonista de Hierro 🏃"
            level >= 15 -> "Espartano del Gym ⚔️"
            level >= 10 -> "Guerrero del Gym 💪"
            level >= 5 -> "Atleta Promesa 🔥"
            else -> "Novato 🌱"
        }
    }

    private fun getDefaultTrophies(): List<Trophy> {
        return listOf(
            Trophy(
                id = "trophy_10k_sub45",
                title = "Primer 10K Sub-45",
                description = "Completa una carrera de 10 km en menos de 45 minutos.",
                category = TrophyCategory.RUNNING_SWIMMING,
                iconName = "DirectionsRun",
                maxProgress = 1
            ),
            Trophy(
                id = "trophy_swolf_sub35",
                title = "SWOLF Sub-35",
                description = "Alcanza una eficiencia de brazada SWOLF menor a 35 en piscina.",
                category = TrophyCategory.RUNNING_SWIMMING,
                iconName = "Pool",
                maxProgress = 1
            ),
            Trophy(
                id = "trophy_calisthenics_master",
                title = "Dominador de la Calistenia",
                description = "Completa una sesión avanzada de calistenia y peso corporal.",
                category = TrophyCategory.CALISTHENICS,
                iconName = "FitnessCenter",
                maxProgress = 1
            ),
            Trophy(
                id = "trophy_30_days_diet",
                title = "30 Días de Dieta Flexible",
                description = "Sigue tu plan de alimentación y suplementación durante 30 días consecutivos.",
                category = TrophyCategory.DIET,
                iconName = "Restaurant",
                maxProgress = 30
            ),
            Trophy(
                id = "trophy_first_workout",
                title = "Primera Victoria",
                description = "Completa tu primer entrenamiento registrado en la app.",
                category = TrophyCategory.WORKOUT,
                iconName = "EmojiEvents",
                maxProgress = 1
            ),
            Trophy(
                id = "trophy_sleep_master",
                title = "Maestro del Sueño",
                description = "Logra una puntuación de sueño superior a 85 en el analizador de IA.",
                category = TrophyCategory.SLEEP,
                iconName = "NightsStay",
                maxProgress = 1
            ),
            Trophy(
                id = "trophy_tonnage_50t",
                title = "50 Toneladas Acumuladas",
                description = "Levanta un tonelage total de 50,000 kg en ejercicios de fuerza.",
                category = TrophyCategory.WORKOUT,
                iconName = "FitnessCenter",
                maxProgress = 50000
            )
        )
    }

    data class LevelInfo(
        val level: Int,
        val title: String,
        val xpForCurrentLevel: Int,
        val xpForNextLevel: Int,
        val progressPercent: Float
    )

    companion object {
        private const val KEY_TOTAL_XP = "key_total_xp"
        private const val KEY_DIET_STREAK = "key_diet_streak"
        private const val KEY_WORKOUT_STREAK = "key_workout_streak"
        private const val KEY_WORKOUTS_COUNT = "key_workouts_count"
        private const val KEY_UNLOCKED_TROPHIES = "key_unlocked_trophies"

        @Volatile
        private var INSTANCE: GamificationManager? = null

        fun getInstance(context: Context): GamificationManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: GamificationManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
