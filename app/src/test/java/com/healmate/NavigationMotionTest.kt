package com.healmate

import com.healmate.ui.navigation.Screen
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
    fun sameTabOrNonTabScreensDoNotSlide() {
        assertEquals(0, Screen.tabDirection(Screen.Health, Screen.Health))
        assertEquals(0, Screen.tabDirection(Screen.Login, Screen.Dashboard))
    }
}
