package com.healmate

import com.healmate.ui.navigation.Screen
import com.healmate.ui.theme.Motion
import org.junit.Assert.assertEquals
import org.junit.Test

class NavigationMotionTest {

    @Test
    fun tabsFollowBottomBarOrder() {
        assertEquals(
            listOf(Screen.Dashboard, Screen.Medications, Screen.Appointments, Screen.Health, Screen.Profile),
            Screen.tabs
        )
    }

    @Test
    fun movingRightSlidesForwardAndLeftSlidesBack() {
        assertEquals(1, Screen.tabDirection(Screen.Dashboard, Screen.Health))
        assertEquals(-1, Screen.tabDirection(Screen.Profile, Screen.Medications))
    }

    @Test
    fun staggerDelayGrowsPerItemAndIsCapped() {
        assertEquals(0, Motion.staggerDelay(0))
        assertEquals(Motion.STAGGER_STEP * 3, Motion.staggerDelay(3))
        // Listas largas no hacen esperar: a partir del tope todos entran juntos.
        assertEquals(Motion.staggerDelay(Motion.STAGGER_MAX_STEPS), Motion.staggerDelay(50))
        assertEquals(0, Motion.staggerDelay(-2))
    }

    @Test
    fun sameTabOrNonTabScreensDoNotSlide() {
        assertEquals(0, Screen.tabDirection(Screen.Health, Screen.Health))
        assertEquals(0, Screen.tabDirection(Screen.Login, Screen.Dashboard))
    }
}
