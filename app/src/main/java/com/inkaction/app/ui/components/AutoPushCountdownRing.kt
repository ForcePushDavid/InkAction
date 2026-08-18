package com.inkaction.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inkaction.app.ui.theme.AccentCyan
import com.inkaction.app.ui.theme.BgSurface
import com.inkaction.app.ui.theme.BorderColor
import com.inkaction.app.ui.theme.TextSecondary
import com.inkaction.app.viewmodel.AutoPushUiState

@Composable
fun AutoPushCountdownPill(
    state: AutoPushUiState,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = state.progress,
        label = "autoPushProgress"
    )

    Row(
        modifier = modifier
            .background(BgSurface.copy(alpha = 0.9f), CircleShape)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier.size(18.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(18.dp)) {
                // Background circle
                drawCircle(
                    color = BorderColor,
                    style = Stroke(width = 2.5.dp.toPx())
                )
                // Progress arc
                if (state.isArmed) {
                    drawArc(
                        color = AccentCyan,
                        startAngle = -90f,
                        sweepAngle = animatedProgress * 360f,
                        useCenter = false,
                        style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            }
        }

        val labelText = when {
            state.isProcessing -> "Processing with Gemini..."
            state.isArmed -> "Auto-push in ${state.remainingSeconds}s"
            else -> "Auto-push on finish"
        }

        Text(
            text = labelText,
            fontSize = 12.sp,
            color = if (state.isArmed) AccentCyan else TextSecondary
        )
    }
}
