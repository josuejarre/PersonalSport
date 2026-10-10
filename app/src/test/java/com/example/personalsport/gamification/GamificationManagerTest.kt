package com.example.personalsport.gamification

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

private class FakeSharedPreferences : SharedPreferences {
    private val map = mutableMapOf<String, Any>()

    override fun getAll(): MutableMap<String, *> = map
    override fun getString(key: String?, defValue: String?): String? = map[key] as? String ?: defValue
    override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? = (map[key] as? Set<*>)?.map { it.toString() }?.toMutableSet() ?: defValues
    override fun getInt(key: String?, defValue: Int): Int = map[key] as? Int ?: defValue
    override fun getLong(key: String?, defValue: Long): Long = map[key] as? Long ?: defValue
    override fun getFloat(key: String?, defValue: Float): Float = map[key] as? Float ?: defValue
    override fun getBoolean(key: String?, defValue: Boolean): Boolean = map[key] as? Boolean ?: defValue
    override fun contains(key: String?): Boolean = map.containsKey(key)
    override fun edit(): SharedPreferences.Editor = FakeEditor(map)
    override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}
    override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}

    private class FakeEditor(private val map: MutableMap<String, Any>) : SharedPreferences.Editor {
        override fun putString(key: String?, value: String?): SharedPreferences.Editor {
            if (key != null && value != null) map[key] = value
            return this
        }
        override fun putStringSet(key: String?, values: MutableSet<String>?): SharedPreferences.Editor {
            if (key != null && values != null) map[key] = values
            return this
        }
        override fun putInt(key: String?, value: Int): SharedPreferences.Editor {
            if (key != null) map[key] = value
            return this
        }
        override fun putLong(key: String?, value: Long): SharedPreferences.Editor {
            if (key != null) map[key] = value
            return this
        }
        override fun putFloat(key: String?, value: Float): SharedPreferences.Editor {
            if (key != null) map[key] = value
            return this
        }
        override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor {
            if (key != null) map[key] = value
            return this
        }
        override fun remove(key: String?): SharedPreferences.Editor {
            map.remove(key)
            return this
        }
        override fun clear(): SharedPreferences.Editor {
            map.clear()
            return this
        }
        override fun commit(): Boolean = true
        override fun apply() {}
    }
}

private class TestGamificationContext : ContextWrapper(null) {
    private val prefs = FakeSharedPreferences()
    override fun getApplicationContext(): Context = this
    override fun getSharedPreferences(name: String?, mode: Int): SharedPreferences = prefs
}

class GamificationManagerTest {

    private lateinit var gamificationManager: GamificationManager

    @Before
    fun setUp() {
        val testContext = TestGamificationContext()
        gamificationManager = GamificationManager(testContext)
    }

    @Test
    fun testInitialState() {
        val state = gamificationManager.uiState.value
        assertNotNull(state)
        assertEquals(0, state.totalXp)
        assertEquals(1, state.currentLevel)
        assertTrue(state.levelTitle.contains("Novato"))
        assertTrue(state.trophies.isNotEmpty())
    }

    @Test
    fun testAwardWorkoutXp() {
        val earnedXp = gamificationManager.awardWorkoutXp(
            durationMinutes = 45,
            calories = 400,
            activityType = "Carrera de Intervalos"
        )

        assertTrue(earnedXp > 100)
        val state = gamificationManager.uiState.value
        assertEquals(earnedXp, state.totalXp)
        assertEquals(1, state.totalWorkoutsCompleted)
    }

    @Test
    fun testAwardDietAdherence30DaysUnlocksTrophy() {
        gamificationManager.awardDietAdherenceXp(30)
        val state = gamificationManager.uiState.value
        val dietTrophy = state.trophies.find { it.id == "trophy_30_days_diet" }

        assertNotNull(dietTrophy)
        assertTrue(dietTrophy!!.isUnlocked)
    }

    @Test
    fun testAwardSleepRecoveryUnlocksTrophy() {
        gamificationManager.awardSleepRecoveryXp(sleepScore = 88, sleepHours = 8.0)
        val state = gamificationManager.uiState.value
        val sleepTrophy = state.trophies.find { it.id == "trophy_sleep_master" }

        assertNotNull(sleepTrophy)
        assertTrue(sleepTrophy!!.isUnlocked)
    }

    @Test
    fun testTrophyUnlocksFor10kSub45AndSwolfSub35() {
        gamificationManager.checkTrophyUnlocks(
            swolfScore = 32,
            best10kTimeMinutes = 42.5,
            isCalisthenicsMaster = true
        )

        val state = gamificationManager.uiState.value
        val trophy10k = state.trophies.find { it.id == "trophy_10k_sub45" }
        val trophySwolf = state.trophies.find { it.id == "trophy_swolf_sub35" }
        val trophyCalisthenics = state.trophies.find { it.id == "trophy_calisthenics_master" }

        assertNotNull(trophy10k)
        assertTrue(trophy10k!!.isUnlocked)

        assertNotNull(trophySwolf)
        assertTrue(trophySwolf!!.isUnlocked)

        assertNotNull(trophyCalisthenics)
        assertTrue(trophyCalisthenics!!.isUnlocked)
    }

    @Test
    fun testCalculateLevelFromXp() {
        val levelInfo0 = gamificationManager.calculateLevelFromXp(0)
        assertEquals(1, levelInfo0.level)
        assertTrue(levelInfo0.title.contains("Novato"))

        // Earn large XP -> High level
        val levelInfo50000 = gamificationManager.calculateLevelFromXp(50000)
        assertTrue(levelInfo50000.level > 10)
        assertTrue(levelInfo50000.title.contains("Nivel"))
    }
}
