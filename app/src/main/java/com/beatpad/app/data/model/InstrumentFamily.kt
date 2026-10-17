package com.beatpad.app.data.model

import androidx.compose.ui.graphics.Color
import com.beatpad.app.ui.theme.FamilyBass
import com.beatpad.app.ui.theme.FamilyChords
import com.beatpad.app.ui.theme.FamilyDrums
import com.beatpad.app.ui.theme.FamilyFxVocal
import com.beatpad.app.ui.theme.FamilySynth
import kotlinx.serialization.Serializable

@Serializable
enum class InstrumentFamily {
    DRUMS,
    BASS,
    SYNTH,
    CHORDS,
    FX_VOCAL,
    FX,
    VOCAL;

    fun color(): Color = when (this) {
        DRUMS -> FamilyDrums
        BASS -> FamilyBass
        SYNTH -> FamilySynth
        CHORDS -> FamilyChords
        FX_VOCAL, FX, VOCAL -> FamilyFxVocal
    }
}
