package com.residentsleeper.data.backup

import com.google.common.truth.Truth.assertThat
import com.residentsleeper.data.model.BabyEvent
import com.residentsleeper.data.model.BabyProfile
import com.residentsleeper.data.model.DiaperType
import com.residentsleeper.data.model.EventType
import com.residentsleeper.data.model.NursingType
import org.junit.Test

class DataBackupManagerTest {

    @Test
    fun exportAndImportJson_preservesAllProfilesAndEventsLosslessly() {
        val profile1 = BabyProfile(
            id = 1,
            name = "Emma",
            birthTimestamp = 1700000000000L,
            isActive = true,
            customWakeWindowMinutes = 60,
            feedingIntervalMinutes = 150,
            customFeedingIntervalMinutes = 120,
            selectedCalendarId = 12L,
            notifyBeforeMinutes = 10,
            enableCalendarSync = true,
            enablePushNotifications = true,
            use12HourFormat = true
        )

        val profile2 = BabyProfile(
            id = 2,
            name = "Lucas",
            birthTimestamp = 1710000000000L,
            isActive = false,
            customWakeWindowMinutes = null,
            feedingIntervalMinutes = 180,
            customFeedingIntervalMinutes = null,
            selectedCalendarId = null,
            notifyBeforeMinutes = 10,
            enableCalendarSync = false,
            enablePushNotifications = true
        )

        val event1 = BabyEvent(
            id = 101,
            babyProfileId = 1,
            type = EventType.SLEEP,
            startTime = 1720000000000L,
            endTime = 1720003600000L,
            note = "Morning nap"
        )

        val event2 = BabyEvent(
            id = 102,
            babyProfileId = 1,
            type = EventType.NURSING,
            startTime = 1720004000000L,
            endTime = 1720005200000L,
            nursingType = NursingType.BOTTLE,
            amountMl = 90
        )

        val event3 = BabyEvent(
            id = 103,
            babyProfileId = 2,
            type = EventType.DIAPER,
            startTime = 1720006000000L,
            diaperType = DiaperType.BOTH
        )

        val profiles = listOf(profile1, profile2)
        val events = listOf(event1, event2, event3)

        // 1. Export to JSON
        val jsonString = DataBackupManager.exportToJson(profiles, events)
        assertThat(jsonString).contains("\"name\": \"Emma\"")
        assertThat(jsonString).contains("\"name\": \"Lucas\"")
        assertThat(jsonString).contains("\"type\": \"SLEEP\"")
        assertThat(jsonString).contains("\"amountMl\": 90")

        // 2. Import from JSON
        val imported = DataBackupManager.importFromJson(jsonString)
        assertThat(imported.version).isEqualTo(1)
        assertThat(imported.profiles).hasSize(2)
        assertThat(imported.events).hasSize(3)

        val importedP1 = imported.profiles.first { it.id == 1L }
        assertThat(importedP1.name).isEqualTo("Emma")
        assertThat(importedP1.customWakeWindowMinutes).isEqualTo(60)
        assertThat(importedP1.customFeedingIntervalMinutes).isEqualTo(120)
        assertThat(importedP1.isActive).isTrue()
        assertThat(importedP1.use12HourFormat).isTrue()

        val importedP2 = imported.profiles.first { it.id == 2L }
        assertThat(importedP2.name).isEqualTo("Lucas")
        assertThat(importedP2.customWakeWindowMinutes).isNull()
        assertThat(importedP2.customFeedingIntervalMinutes).isNull()
        assertThat(importedP2.isActive).isFalse()
        assertThat(importedP2.use12HourFormat).isFalse()

        val importedE1 = imported.events.first { it.id == 101L }
        assertThat(importedE1.babyProfileId).isEqualTo(1L)
        assertThat(importedE1.type).isEqualTo(EventType.SLEEP)
        assertThat(importedE1.note).isEqualTo("Morning nap")

        val importedE2 = imported.events.first { it.id == 102L }
        assertThat(importedE2.nursingType).isEqualTo(NursingType.BOTTLE)
        assertThat(importedE2.amountMl).isEqualTo(90)

        val importedE3 = imported.events.first { it.id == 103L }
        assertThat(importedE3.babyProfileId).isEqualTo(2L)
        assertThat(importedE3.diaperType).isEqualTo(DiaperType.BOTH)
    }

    @Test
    fun exportToCsv_generatesValidHeadersAndColumns() {
        val profile = BabyProfile(id = 1, name = "Emma")
        val event = BabyEvent(
            id = 10,
            babyProfileId = 1,
            type = EventType.SLEEP,
            startTime = 1720000000000L,
            endTime = 1720003600000L,
            note = "Restful sleep"
        )

        val csv = DataBackupManager.exportToCsv(listOf(profile), listOf(event))
        val lines = csv.trim().split("\n")

        assertThat(lines.size).isAtLeast(2)
        assertThat(lines[0]).isEqualTo("ProfileId,BabyName,EventType,StartTime,EndTime,DurationMinutes,NursingType,AmountMl,DiaperType,Notes")
        assertThat(lines[1]).contains("1,\"Emma\",SLEEP")
        assertThat(lines[1]).contains("60") // 3600 seconds = 60 min
        assertThat(lines[1]).contains("\"Restful sleep\"")
    }
}
