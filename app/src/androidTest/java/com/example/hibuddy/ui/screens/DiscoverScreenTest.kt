package com.example.hibuddy.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertTrue

class DiscoverScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun statChip_displaysCorrectly() {
        composeTestRule.setContent {
            StatChip(icon = "✅", value = "10", label = "Projects")
        }

        composeTestRule.onNodeWithText("✅").assertIsDisplayed()
        composeTestRule.onNodeWithText("10").assertIsDisplayed()
        composeTestRule.onNodeWithText("Projects").assertIsDisplayed()
    }

    @Test
    fun metaTag_displaysCorrectly() {
        composeTestRule.setContent {
            MetaTag(icon = "🏠", label = "Remote")
        }

        composeTestRule.onNodeWithText("🏠").assertIsDisplayed()
        composeTestRule.onNodeWithText("Remote").assertIsDisplayed()
    }

    @Test
    fun discoverHeader_peopleMode_displaysCorrectly() {
        var createProjectClicked = false
        var toggleModeClicked = false

        composeTestRule.setContent {
            DiscoverHeader(
                isPeopleMode = true,
                onToggle = { toggleModeClicked = true },
                onCreateProject = { createProjectClicked = true }
            )
        }

        composeTestRule.onNodeWithText("Create Project").assertIsDisplayed()
        composeTestRule.onNodeWithText("👑 Owner Mode").assertIsDisplayed()

        composeTestRule.onNodeWithText("Create Project").performClick()
        assertTrue(createProjectClicked)

        composeTestRule.onNodeWithText("👑 Owner Mode").performClick()
        assertTrue(toggleModeClicked)
    }

    @Test
    fun discoverHeader_contributorMode_displaysCorrectly() {
        composeTestRule.setContent {
            DiscoverHeader(
                isPeopleMode = false,
                onToggle = {},
                onCreateProject = {}
            )
        }

        composeTestRule.onNodeWithText("Create Project").assertDoesNotExist()
        composeTestRule.onNodeWithText("🛠️ Contributor Mode").assertIsDisplayed()
    }

    @Test
    fun emptyStackView_peopleMode_displaysCorrectly() {
        var createProjectClicked = false

        composeTestRule.setContent {
            EmptyStackView(
                isPeopleMode = true,
                onCreateProject = { createProjectClicked = true }
            )
        }

        composeTestRule.onNodeWithText("🎉").assertIsDisplayed()
        composeTestRule.onNodeWithText("You've seen everyone!").assertIsDisplayed()
        composeTestRule.onNodeWithText("Create a New Project").assertIsDisplayed()

        composeTestRule.onNodeWithText("Create a New Project").performClick()
        assertTrue(createProjectClicked)
    }

    @Test
    fun emptyStackView_contributorMode_displaysCorrectly() {
        composeTestRule.setContent {
            EmptyStackView(
                isPeopleMode = false,
                onCreateProject = {}
            )
        }

        composeTestRule.onNodeWithText("🚀").assertIsDisplayed()
        composeTestRule.onNodeWithText("You've seen everyone!").assertIsDisplayed()
        composeTestRule.onNodeWithText("Create a New Project").assertDoesNotExist()
    }
}
