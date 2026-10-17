package com.beatpad.app.ui.learn

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beatpad.app.data.model.PadInfo
import com.beatpad.app.ui.theme.TextPrimary

@Composable
fun LessonPadCell(
    pad: PadInfo,
    isLit: Boolean,
    isUsed: Boolean,
    isNextTarget: Boolean,
    upcomingHits: Int,
    onPadDown: () -> Unit,
    onPadUp: () -> Unit,
    modifier: Modifier = Modifier
) {
    val alpha = if (isUsed) 1.0f else 0.25f
    val scale by animateFloatAsState(targetValue = if (isLit) 0.96f else 1.0f, label = "padScale")

    Box(
        modifier = modifier
            .aspectRatio(1.0f)
            .scale(scale)
            .alpha(alpha)
            .clip(RoundedCornerShape(16.dp))
            .background(if (isLit) pad.color else pad.color.copy(alpha = 0.35f))
            .border(
                width = if (isLit) 2.dp else 1.dp,
                color = if (isLit) Color.White else pad.color.copy(alpha = 0.6f),
                shape = RoundedCornerShape(16.dp)
            )
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        onPadDown()
                        tryAwaitRelease()
                        onPadUp()
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // Next Pad Pulsing Ring Indicator
        if (isNextTarget) {
            NextPadRingIndicator(ringColor = Color.White)
        }

        // Upcoming Hits White Dots
        if (upcomingHits > 0) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.TopEnd
            ) {
                UpcomingHitsIndicator(hitsRemainingOnPad = upcomingHits)
            }
        }

        // Pad Name
        Text(
            text = pad.name,
            color = if (isLit) Color.White else TextPrimary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(4.dp)
        )
    }
}
