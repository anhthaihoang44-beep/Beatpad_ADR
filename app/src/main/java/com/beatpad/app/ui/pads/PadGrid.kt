package com.beatpad.app.ui.pads

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.beatpad.app.data.model.PackSide
import com.beatpad.app.data.model.PadInfo
import com.beatpad.app.util.AppConfig

@Composable
fun PadGrid(
    pads: List<PadInfo>,
    currentSide: PackSide,
    pressedPadIds: Set<Int>,
    activeLoops: Set<Int>,
    pendingLoops: Set<Int>,
    onPadDown: (Int) -> Unit,
    onPadUp: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val numCols = AppConfig.GRID_COLUMNS
    val numRows = AppConfig.GRID_ROWS

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        for (r in 0 until numRows) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.0f),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                for (c in 0 until numCols) {
                    val index = r * numCols + c
                    val pad = pads.getOrNull(index)
                    if (pad != null) {
                        PadItem(
                            pad = pad,
                            currentSide = currentSide,
                            isPressed = pressedPadIds.contains(index),
                            isActiveLoop = activeLoops.contains(index),
                            isPendingLoop = pendingLoops.contains(index),
                            onDown = { onPadDown(index) },
                            onUp = { onPadUp(index) },
                            modifier = Modifier.weight(1.0f)
                        )
                    } else {
                        Box(modifier = Modifier.weight(1.0f))
                    }
                }
            }
        }
    }
}
