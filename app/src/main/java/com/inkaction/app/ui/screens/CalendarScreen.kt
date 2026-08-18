package com.inkaction.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Event
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.width
import com.inkaction.app.ai.EventDto
import com.inkaction.app.ui.theme.AccentBlue
import com.inkaction.app.ui.theme.AccentCyan
import com.inkaction.app.ui.theme.BgSurface
import com.inkaction.app.ui.theme.BgTertiary
import com.inkaction.app.ui.theme.TextMuted
import com.inkaction.app.ui.theme.TextPrimary
import com.inkaction.app.ui.theme.TextSecondary
import com.inkaction.app.util.CalendarSyncUtil

@Composable
fun CalendarScreen(
    events: List<EventDto>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    if (events.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(48.dp)
                )
                Text(
                    text = "No scheduled appointments",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
                Text(
                    text = "Meetings, dates, and reminders mentioned in your S-Pen handwriting are scheduled automatically.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
        return
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(events, key = { it.id }) { event ->
            EventCard(
                event = event,
                onAddToCalendar = {
                    CalendarSyncUtil.launchCalendarIntent(context, event)
                }
            )
        }
    }
}

@Composable
fun EventCard(
    event: EventDto,
    onAddToCalendar: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(BgSurface),
        verticalAlignment = Alignment.Top
    ) {
        // Vertical color stripe (Google Calendar style)
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(80.dp)
                .background(AccentBlue)
        )

        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Time / Date indicator
            Column(
                modifier = Modifier.width(55.dp),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = event.time.ifBlank { "All-day" },
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                if (event.duration.isNotBlank()) {
                    Text(
                        text = event.duration,
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                }
            }

            // Dot separator
            Box(
                modifier = Modifier
                    .padding(top = 6.dp)
                    .size(8.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(AccentBlue)
            )

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = event.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                
                if (event.date.isNotBlank()) {
                    Text(
                        text = event.date,
                        fontSize = 12.sp,
                        color = AccentCyan
                    )
                }

                if (event.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = event.description,
                        fontSize = 13.sp,
                        color = TextSecondary,
                        lineHeight = 16.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                androidx.compose.material3.TextButton(
                    onClick = onAddToCalendar,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 0.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Event,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = AccentBlue
                    )
                    Spacer(modifier = Modifier.size(6.dp))
                    Text(text = "Add to Google Calendar", fontSize = 12.sp, color = AccentBlue)
                }
            }
        }
    }
}
