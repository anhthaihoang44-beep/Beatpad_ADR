package com.beatpad.app.ui.learn

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beatpad.app.data.model.HitRating
import com.beatpad.app.ui.theme.AmberAccent

@Composable
fun NextPadRingIndicator(
    modifier: Modifier = Modifier,
    ringColor: Color = Color.White
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ringPulse")
    val ringScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ringScale"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .scale(ringScale)
            .border(
                width = 3.dp,
                color = ringColor,
                shape = RoundedCornerShape(16.dp)
            )
    )
}

@Composable
fun UpcomingHitsIndicator(
    hitsRemainingOnPad: Int,
    modifier: Modifier = Modifier
) {
    if (hitsRemainingOnPad <= 0) return

    Row(
        modifier = modifier.padding(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val count = hitsRemainingOnPad.coerceAtMost(4)
        for (i in 0 until count) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(Color.White)
            )
            if (i < count - 1) {
                Spacer(modifier = Modifier.width(3.dp))
            }
        }
    }
}

@Composable
fun HitRatingPopup(
    rating: HitRating?,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = rating != null,
        enter = scaleIn() + fadeIn(),
        exit = scaleOut() + fadeOut(),
        modifier = modifier
    ) {
        if (rating == null) return@AnimatedVisibility

        val (text, color) = when (rating) {
            HitRating.PERFECT -> "PERFECT!" to Color(0xFF00F5D4)
            HitRating.GOOD -> "GOOD" to AmberAccent
            HitRating.MISS -> "MISS" to Color(0xFFFF3366)
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF15171C).copy(alpha = 0.9f))
                .border(1.5.dp, color, RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = color,
                fontSize = 13.sp,
                fontWeight = FontWeight.Black
            )
        }
    }
}
