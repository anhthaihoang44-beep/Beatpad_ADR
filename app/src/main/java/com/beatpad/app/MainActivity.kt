package com.beatpad.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.beatpad.app.ui.BeatPadApp
import com.beatpad.app.ui.pads.PadViewModel
import com.beatpad.app.ui.theme.BeatPadTheme

class MainActivity : ComponentActivity() {

    private val padViewModel: PadViewModel by viewModels {
        PadViewModel.Factory(application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            BeatPadTheme {
                BeatPadApp(padViewModel = padViewModel)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        padViewModel.pauseAudio()
    }

    override fun onResume() {
        super.onResume()
        padViewModel.resumeAudio()
    }
}
