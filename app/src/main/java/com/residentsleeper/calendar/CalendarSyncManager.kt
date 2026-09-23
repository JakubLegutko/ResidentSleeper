package com.residentsleeper.calendar

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import java.util.TimeZone

data class DeviceCalendar(
    val id: Long,
    val displayName: String,
    val accountName: String,
    val isPrimary: Boolean
)

object CalendarSyncManager {

    fun hasCalendarPermission(context: Context): Boolean {
        val readPerm = ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_CALENDAR)
        val writePerm = ContextCompat.checkSelfPermission(context, android.Manifest.permission.WRITE_CALENDAR)
        return readPerm == PackageManager.PERMISSION_GRANTED && writePerm == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Lists all calendars available on the device (synced Google Calendars, local, etc.).
     */
    fun getAvailableCalendars(context: Context): List<DeviceCalendar> {
        if (!hasCalendarPermission(context)) return emptyList()

        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.ACCOUNT_NAME,
            CalendarContract.Calendars.IS_PRIMARY
        )

        val calendars = mutableListOf<DeviceCalendar>()
        val cursor: Cursor? = context.contentResolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            projection,
            null,
            null,
            null
        )

        cursor?.use {
            val idIndex = it.getColumnIndexOrThrow(CalendarContract.Calendars._ID)
            val nameIndex = it.getColumnIndexOrThrow(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME)
            val accountIndex = it.getColumnIndexOrThrow(CalendarContract.Calendars.ACCOUNT_NAME)
            val primaryIndex = it.getColumnIndex(CalendarContract.Calendars.IS_PRIMARY)

            while (it.moveToNext()) {
                val id = it.getLong(idIndex)
                val name = it.getString(nameIndex) ?: "Unnamed"
                val account = it.getString(accountIndex) ?: "Local"
                val isPrimary = if (primaryIndex != -1) it.getInt(primaryIndex) == 1 else false
                calendars.add(DeviceCalendar(id, name, account, isPrimary))
            }
        }

        return calendars
    }

    /**
     * Adds an event with an exact alert reminder (e.g. 10 minutes prior) to the chosen calendar.
     * Returns the created event ID, or null if failed.
     */
    fun createCalendarEventWithReminder(
        context: Context,
        calendarId: Long,
        title: String,
        description: String,
        startTimeMillis: Long,
        endTimeMillis: Long,
        reminderMinutes: Int = 10
    ): Long? {
        if (!hasCalendarPermission(context)) return null

        try {
            val values = ContentValues().apply {
                put(CalendarContract.Events.CALENDAR_ID, calendarId)
                put(CalendarContract.Events.TITLE, title)
                put(CalendarContract.Events.DESCRIPTION, description)
                put(CalendarContract.Events.DTSTART, startTimeMillis)
                put(CalendarContract.Events.DTEND, maxOf(startTimeMillis + 60_000, endTimeMillis))
                put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
            }

            val uri = context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
            val eventId = uri?.lastPathSegment?.toLongOrNull() ?: return null

            // Add the alert reminder
            val reminderValues = ContentValues().apply {
                put(CalendarContract.Reminders.EVENT_ID, eventId)
                put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT)
                put(CalendarContract.Reminders.MINUTES, reminderMinutes)
            }
            context.contentResolver.insert(CalendarContract.Reminders.CONTENT_URI, reminderValues)

            return eventId
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    /**
     * Deletes a calendar event by ID.
     */
    fun deleteCalendarEvent(context: Context, eventId: Long) {
        if (!hasCalendarPermission(context)) return
        try {
            val deleteUri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId)
            context.contentResolver.delete(deleteUri, null, null)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
