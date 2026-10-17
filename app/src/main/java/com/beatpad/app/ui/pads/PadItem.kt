package com.beatpad.app.ui.pads

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beatpad.app.data.model.PackSide
import com.beatpad.app.data.model.PadInfo
import com.beatpad.app.data.model.PadMode
import com.beatpad.app.ui.theme.PadActiveOutline

@Composable
fun PadItem(
    pad: PadInfo,
    currentSide: PackSide = PackSide.SIDE_A,
    isPressed: Boolean,
    isActiveLoop: Boolean,
    isPendingLoop: Boolean = false,
    onDown: () -> Unit,
    onUp: () -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    var isCurrentlyDown by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isPressed || isCurrentlyDown) 0.96f else 1.0f,
        animationSpec = tween(durationMillis = 50),
        label = "padScale"
    )

    // Pulsing transition for pending quantized loops
    val infiniteTransition = rememberInfiniteTransition(label = "pendingPulse")
    val pendingAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 400),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    val baseColor = pad.color
    val displayColor = if (isPressed || isCurrentlyDown) {
        baseColor.copy(
            red = (baseColor.red * 1.25f).coerceAtMost(1.0f),
            green = (baseColor.green * 1.25f).coerceAtMost(1.0f),
            blue = (baseColor.blue * 1.25f).coerceAtMost(1.0f),
            alpha = 1.0f
        )
    } else {
        baseColor.copy(alpha = 0.88f)
    }

    Box(
        modifier = modifier
            .aspectRatio(1.0f)
            .scale(scale)
            .shadow(
                elevation = if (isPressed || isCurrentlyDown) 14.dp else 4.dp,
                shape = RoundedCornerShape(16.dp),
                spotColor = if (isPressed || isCurrentlyDown) baseColor else Color.Transparent
            )
            .clip(RoundedCornerShape(16.dp))
            .background(displayColor)
            .then(
                if (isActiveLoop) {
                    Modifier.border(3.dp, PadActiveOutline, RoundedCornerShape(16.dp))
                } else if (isPendingLoop) {
                    Modifier.border(3.dp, PadActiveOutline.copy(alpha = pendingAlpha), RoundedCornerShape(16.dp))
                } else if (isPressed || isCurrentlyDown) {
                    Modifier.border(2.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                } else {
                    Modifier.border(1.dp, Color.Black.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
                }
            )
            .pointerInput(pad.index) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val hasDown = event.changes.any { it.pressed }
                        if (hasDown && !isCurrentlyDown) {
                            isCurrentlyDown = true
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            onDown()
                        } else if (!hasDown && isCurrentlyDown) {
                            isCurrentlyDown = false
                            onUp()
                        }
                    }
                }
            }
            .padding(10.dp)
    ) {
        // Top indicators: Mode badge + Family dot (and Side B badge if active)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.7f))
                )
                if (currentSide == PackSide.SIDE_B) {
                    Text(
                        text = "B",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            if (pad.mode == PadMode.LOOP) {
                Icon(
                    imageVector = Icons.Default.AllInclusive,
                    contentDescription = "Loop",
                    tint = if (isActiveLoop) {
                        PadActiveOutline
                    } else if (isPendingLoop) {
                        PadActiveOutline.copy(alpha = pendingAlpha)
                    } else {
                        Color.White.copy(alpha = 0.6f)
                    },
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        // Bottom label: Pad Name & Instrument Family
        Column(
            modifier = Modifier.align(Alignment.BottomStart)
        ) {
            Text(
                text = pad.name,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = pad.family.name,
                color = Color.White.copy(alpha = 0.75f),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }
    }
}
