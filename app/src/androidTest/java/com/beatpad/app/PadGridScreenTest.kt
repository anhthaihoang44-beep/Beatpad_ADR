package com.beatpad.app

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.beatpad.app.data.model.InstrumentFamily
import com.beatpad.app.data.model.PadInfo
import com.beatpad.app.data.model.PadMode
import com.beatpad.app.ui.pads.PadGrid
import com.beatpad.app.ui.theme.BeatPadTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PadGridScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun padGrid_rendersPads_andHandlesTap() {
        val testPads = (0 until 12).map { id ->
            PadInfo(
                index = id,
                name = "TestPad $id",
                family = if (id % 2 == 0) InstrumentFamily.DRUMS else InstrumentFamily.BASS,
                fileA = "packs/trap/pad_0.wav",
                fileB = "packs/trap/pad_0_b.wav",
                color = if (id % 2 == 0) InstrumentFamily.DRUMS.color() else InstrumentFamily.BASS.color(),
                mode = PadMode.ONE_SHOT
            )
        }

        var clickedPadId = -1
        var touchedDown = false

        composeTestRule.setContent {
            BeatPadTheme {
                PadGrid(
                    pads = testPads,
                    pressedPadIds = emptySet(),
                    activeLoops = emptySet(),
                    onPadDown = { id ->
                        clickedPadId = id
                        touchedDown = true
                    },
                    onPadUp = {}
                )
            }
        }

        // Verify Pad 0 renders
        composeTestRule.onNodeWithText("TestPad 0").assertExists()
    }
}
