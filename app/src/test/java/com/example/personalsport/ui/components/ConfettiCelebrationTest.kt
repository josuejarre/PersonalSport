package com.example.personalsport.ui.components

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class ConfettiCelebrationTest {

    @Test
    fun testConfettiColorsPaletteHasRequiredColors() {
        val expectedColors = listOf(
            Color(0xFFFF6500), // Electric Orange
            Color(0xFFFFD700), // Gold
            Color(0xFF4CAF50), // Green
            Color(0xFF00BCD4), // Cyan
            Color(0xFFFFFFFF), // White
            Color(0xFFE91E63)  // Magenta
        )

        assertEquals(6, ConfettiCelebrationColors.size)
        expectedColors.forEach { expected ->
            assertTrue("Palette should contain color $expected", ConfettiCelebrationColors.contains(expected))
        }
    }

    @Test
    fun testGenerateInitialParticlesCountAndProperties() {
        val count = 120
        val particles = ConfettiPhysicsUtils.generateInitialParticles(
            count = count,
            screenWidth = 1080f,
            screenHeight = 1920f,
            random = Random(1234)
        )

        assertEquals(count, particles.size)
        particles.forEachIndexed { index, particle ->
            assertEquals(index, particle.id)
            assertTrue("Alpha should be initial 1.0", particle.alpha == 1f)
            assertTrue("Width should be positive", particle.sizeWidth > 0f)
            assertTrue("Height should be positive", particle.sizeHeight > 0f)
            assertTrue("Particle color must belong to palette", ConfettiCelebrationColors.contains(particle.color))
        }
    }

    @Test
    fun testParticleUpdatePhysicsMovement() {
        val particle = ConfettiParticle(
            id = 1,
            x = 500f,
            y = 300f,
            vx = 10f,
            vy = -15f,
            rotation = 0f,
            vRot = 5f,
            sizeWidth = 10f,
            sizeHeight = 15f,
            color = ConfettiElectricOrange,
            shape = ConfettiShape.RECTANGLE,
            alpha = 1f
        )

        val initialX = particle.x
        val initialY = particle.y

        particle.update(
            screenWidth = 1080f,
            screenHeight = 1920f,
            gravity = 0.3f,
            drag = 0.98f,
            wind = 0.2f
        )

        assertTrue("X should move with velocity + wind", particle.x > initialX)
        // vy was -15, updated vy becomes (-15 + 0.3) * 0.98 = -14.406. y = 300 + (-14.406) = 285.594
        assertTrue("Y position should change based on vy", particle.y != initialY)
        assertEquals(5f, particle.rotation, 0.001f)
    }

    @Test
    fun testParticleLoopToTopWhenOffScreen() {
        val screenHeight = 1000f
        val screenWidth = 800f

        val particle = ConfettiParticle(
            id = 99,
            x = 400f,
            y = 1050f, // Off screen (> screenHeight + 40f)
            vx = 0f,
            vy = 10f,
            rotation = 0f,
            vRot = 2f,
            sizeWidth = 10f,
            sizeHeight = 10f,
            color = ConfettiGold,
            shape = ConfettiShape.CIRCLE,
            alpha = 1f
        )

        particle.update(
            screenWidth = screenWidth,
            screenHeight = screenHeight,
            gravity = 0.5f,
            drag = 1.0f,
            wind = 0f
        )

        assertEquals("Y position should loop back to -20f when falling off screen", -20f, particle.y, 0.001f)
    }

    @Test
    fun testWorkoutTableCompletionCondition() {
        // Condition: totalExercises > 0 AND completedCount == totalCount
        assertTrue(ConfettiPhysicsUtils.isWorkoutTableCompleted(completedCount = 5, totalCount = 5))
        assertFalse(ConfettiPhysicsUtils.isWorkoutTableCompleted(completedCount = 4, totalCount = 5))
        assertFalse(ConfettiPhysicsUtils.isWorkoutTableCompleted(completedCount = 0, totalCount = 0))
        assertFalse(ConfettiPhysicsUtils.isWorkoutTableCompleted(completedCount = 0, totalCount = 3))
    }

    @Test
    fun testConfettiShapesVariants() {
        val shapes = ConfettiShape.entries
        assertEquals(3, shapes.size)
        assertTrue(shapes.contains(ConfettiShape.RECTANGLE))
        assertTrue(shapes.contains(ConfettiShape.CIRCLE))
        assertTrue(shapes.contains(ConfettiShape.OVAL))
    }
}
