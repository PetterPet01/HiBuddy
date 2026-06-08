package com.example.hibuddy.ui.screens

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SwipeDecisionResolverTest {
    private val thresholds = SwipeDecisionThresholds(
        horizontalThreshold = 120f,
        verticalThreshold = 110f,
        flingVelocityThreshold = 850f,
    )

    @Test
    fun nearCenterLowVelocity_cancels() {
        val intent = resolveSwipeIntent(
            offset = Offset(55f, -45f),
            velocity = Offset(120f, -100f),
            thresholds = thresholds,
        )

        assertNull(intent)
    }

    @Test
    fun tinyMovementNoisyVelocity_cancels() {
        val intent = resolveSwipeIntent(
            offset = Offset(20f, -12f),
            velocity = Offset(650f, -120f),
            thresholds = thresholds,
        )

        assertNull(intent)
    }

    @Test
    fun beyondRightThreshold_likes() {
        val intent = resolveSwipeIntent(
            offset = Offset(124f, -8f),
            velocity = Offset.Zero,
            thresholds = thresholds,
        )

        assertEquals(SwipeIntent.Like, intent)
    }

    @Test
    fun beyondLeftThreshold_passes() {
        val intent = resolveSwipeIntent(
            offset = Offset(-126f, -4f),
            velocity = Offset.Zero,
            thresholds = thresholds,
        )

        assertEquals(SwipeIntent.Pass, intent)
    }

    @Test
    fun fastRightFlingWithEnoughMovement_likes() {
        val intent = resolveSwipeIntent(
            offset = Offset(48f, 0f),
            velocity = Offset(960f, 80f),
            thresholds = thresholds,
        )

        assertEquals(SwipeIntent.Like, intent)
    }

    @Test
    fun strongUpwardDrag_superLikes() {
        val intent = resolveSwipeIntent(
            offset = Offset(18f, -120f),
            velocity = Offset.Zero,
            thresholds = thresholds,
        )

        assertEquals(SwipeIntent.SuperLike, intent)
    }

    @Test
    fun intentionalUpperRightDrag_queues() {
        val intent = resolveSwipeIntent(
            offset = Offset(116f, -100f),
            velocity = Offset.Zero,
            thresholds = thresholds,
        )

        assertEquals(SwipeIntent.Queue, intent)
    }

    @Test
    fun weakUpperRightDiagonal_cancels() {
        val intent = resolveSwipeIntent(
            offset = Offset(72f, -66f),
            velocity = Offset(120f, -100f),
            thresholds = thresholds,
        )

        assertNull(intent)
    }
}
