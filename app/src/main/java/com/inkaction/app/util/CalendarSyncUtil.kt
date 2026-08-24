package com.inkaction.app.util

import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import com.inkaction.app.ai.EventDto
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object CalendarSyncUtil {

    /**
     * Launches Android System Calendar event creation intent
     */
    fun launchCalendarIntent(context: Context, event: EventDto) {
        val intent = Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.Events.TITLE, event.title)
            putExtra(CalendarContract.Events.DESCRIPTION, event.description)
            putExtra(CalendarContract.Events.EVENT_LOCATION, event.location)

            val startTimeMillis = parseDateTime(event.date, event.time)
            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startTimeMillis)
            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, startTimeMillis + (60 * 60 * 1000))
        }

        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            android.widget.Toast.makeText(context, "Google Calendar app is not installed or accessible.", android.widget.Toast.LENGTH_LONG).show()
        }
    }

    private fun parseDateTime(dateStr: String, timeStr: String): Long {
        val cal = Calendar.getInstance()
        try {
            val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val parsedDate = format.parse(dateStr)
            if (parsedDate != null) {
                cal.time = parsedDate
            }

            if (timeStr.isNotBlank() && timeStr.contains(":")) {
                val parts = timeStr.split(":")
                val hour = parts[0].trim().toIntOrNull() ?: 10
                val minute = parts[1].replace("[^0-9]".toRegex(), "").toIntOrNull() ?: 0
                cal.set(Calendar.HOUR_OF_DAY, hour)
                cal.set(Calendar.MINUTE, minute)
            }
        } catch (_: Exception) {
            // Default to current time + 1 day
            cal.add(Calendar.DAY_OF_YEAR, 1)
            cal.set(Calendar.HOUR_OF_DAY, 10)
            cal.set(Calendar.MINUTE, 0)
        }
        return cal.timeInMillis
    }

    /**
     * Directly inserts an event to the Google Calendar (requires WRITE_CALENDAR permission).
     */
    fun addEventToCalendar(context: Context, event: EventDto): Boolean {
        if (androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.WRITE_CALENDAR) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            return false
        }

        try {
            val startTimeMillis = parseDateTime(event.date, event.time)
            val values = android.content.ContentValues().apply {
                put(CalendarContract.Events.DTSTART, startTimeMillis)
                put(CalendarContract.Events.DTEND, startTimeMillis + (60 * 60 * 1000))
                put(CalendarContract.Events.TITLE, event.title)
                put(CalendarContract.Events.DESCRIPTION, event.description)
                put(CalendarContract.Events.EVENT_LOCATION, event.location)
                put(CalendarContract.Events.CALENDAR_ID, 1) // Default primary calendar
                put(CalendarContract.Events.EVENT_TIMEZONE, java.util.TimeZone.getDefault().id)
            }
            val uri = context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
            return uri != null
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }
}
