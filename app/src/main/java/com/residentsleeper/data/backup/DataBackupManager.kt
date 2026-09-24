package com.residentsleeper.data.backup

import com.residentsleeper.data.model.BabyEvent
import com.residentsleeper.data.model.BabyProfile
import com.residentsleeper.data.model.DiaperType
import com.residentsleeper.data.model.EventType
import com.residentsleeper.data.model.Gender
import com.residentsleeper.data.model.NursingType
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

data class BackupData(
    val version: Int = 1,
    val exportTimestamp: Long = System.currentTimeMillis(),
    val profiles: List<BabyProfile>,
    val events: List<BabyEvent>
)

object DataBackupManager {

    /**
     * Serializes profiles and events to a structured JSON string.
     */
    fun exportToJson(profiles: List<BabyProfile>, events: List<BabyEvent>): String {
        val root = JSONObject()
        root.put("version", 1)
        root.put("exportTimestamp", System.currentTimeMillis())

        val profilesArray = JSONArray()
        for (p in profiles) {
            val pObj = JSONObject()
            pObj.put("id", p.id)
            pObj.put("name", p.name)
            pObj.put("birthTimestamp", p.birthTimestamp)
            pObj.put("isActive", p.isActive)
            pObj.put("customWakeWindowMinutes", p.customWakeWindowMinutes ?: JSONObject.NULL)
            pObj.put("feedingIntervalMinutes", p.feedingIntervalMinutes)
            pObj.put("selectedCalendarId", p.selectedCalendarId ?: JSONObject.NULL)
            pObj.put("notifyBeforeMinutes", p.notifyBeforeMinutes)
            pObj.put("enableCalendarSync", p.enableCalendarSync)
            pObj.put("enablePushNotifications", p.enablePushNotifications)
            pObj.put("dayStartHour", p.dayStartHour)
            pObj.put("maxRecommendedNightFeeds", p.maxRecommendedNightFeeds)
            pObj.put("notifyForOptionalNightFeeds", p.notifyForOptionalNightFeeds)
            pObj.put("gender", p.gender.name)
            pObj.put("use12HourFormat", p.use12HourFormat)
            profilesArray.put(pObj)
        }
        root.put("profiles", profilesArray)

        val eventsArray = JSONArray()
        for (e in events) {
            val eObj = JSONObject()
            eObj.put("id", e.id)
            eObj.put("babyProfileId", e.babyProfileId)
            eObj.put("type", e.type.name)
            eObj.put("startTime", e.startTime)
            eObj.put("endTime", e.endTime ?: JSONObject.NULL)
            eObj.put("nursingType", e.nursingType?.name ?: JSONObject.NULL)
            eObj.put("diaperType", e.diaperType?.name ?: JSONObject.NULL)
            eObj.put("amountMl", e.amountMl ?: JSONObject.NULL)
            eObj.put("note", e.note ?: JSONObject.NULL)
            eventsArray.put(eObj)
        }
        root.put("events", eventsArray)

        return root.toString(2)
    }

    /**
     * Parses a JSON string into a BackupData object.
     */
    fun importFromJson(jsonString: String): BackupData {
        val root = JSONObject(jsonString)
        val version = root.optInt("version", 1)
        val exportTimestamp = root.optLong("exportTimestamp", System.currentTimeMillis())

        val profilesList = mutableListOf<BabyProfile>()
        val profilesArray = root.optJSONArray("profiles")
        if (profilesArray != null) {
            for (i in 0 until profilesArray.length()) {
                val pObj = profilesArray.getJSONObject(i)
                val profile = BabyProfile(
                    id = pObj.optLong("id", 0),
                    name = pObj.optString("name", "Baby"),
                    birthTimestamp = pObj.optLong("birthTimestamp", System.currentTimeMillis()),
                    isActive = pObj.optBoolean("isActive", false),
                    customWakeWindowMinutes = if (pObj.isNull("customWakeWindowMinutes")) null else pObj.optInt("customWakeWindowMinutes"),
                    feedingIntervalMinutes = pObj.optInt("feedingIntervalMinutes", 150),
                    selectedCalendarId = if (pObj.isNull("selectedCalendarId")) null else pObj.optLong("selectedCalendarId"),
                    notifyBeforeMinutes = pObj.optInt("notifyBeforeMinutes", 10),
                    enableCalendarSync = pObj.optBoolean("enableCalendarSync", false),
                    enablePushNotifications = pObj.optBoolean("enablePushNotifications", true),
                    dayStartHour = pObj.optInt("dayStartHour", 7),
                    maxRecommendedNightFeeds = pObj.optInt("maxRecommendedNightFeeds", 1),
                    notifyForOptionalNightFeeds = pObj.optBoolean("notifyForOptionalNightFeeds", false),
                    gender = if (!pObj.isNull("gender")) runCatching { Gender.valueOf(pObj.getString("gender")) }.getOrDefault(Gender.UNSPECIFIED) else Gender.UNSPECIFIED,
                    use12HourFormat = pObj.optBoolean("use12HourFormat", false)
                )
                profilesList.add(profile)
            }
        }

        val eventsList = mutableListOf<BabyEvent>()
        val eventsArray = root.optJSONArray("events")
        if (eventsArray != null) {
            for (i in 0 until eventsArray.length()) {
                val eObj = eventsArray.getJSONObject(i)
                val typeStr = eObj.getString("type")
                val eventType = EventType.valueOf(typeStr)

                val nursingType = if (!eObj.isNull("nursingType")) {
                    NursingType.valueOf(eObj.getString("nursingType"))
                } else null

                val diaperType = if (!eObj.isNull("diaperType")) {
                    DiaperType.valueOf(eObj.getString("diaperType"))
                } else null

                val event = BabyEvent(
                    id = eObj.optLong("id", 0),
                    babyProfileId = eObj.optLong("babyProfileId", 1),
                    type = eventType,
                    startTime = eObj.getLong("startTime"),
                    endTime = if (eObj.isNull("endTime")) null else eObj.getLong("endTime"),
                    nursingType = nursingType,
                    diaperType = diaperType,
                    amountMl = if (eObj.isNull("amountMl")) null else eObj.getInt("amountMl"),
                    note = if (eObj.isNull("note")) null else eObj.getString("note")
                )
                eventsList.add(event)
            }
        }

        return BackupData(
            version = version,
            exportTimestamp = exportTimestamp,
            profiles = profilesList,
            events = eventsList
        )
    }

    /**
     * Exports profiles and events into a tabular CSV string.
     */
    fun exportToCsv(profiles: List<BabyProfile>, events: List<BabyEvent>): String {
        val profileMap = profiles.associateBy { it.id }
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

        val sb = java.lang.StringBuilder()
        sb.append("ProfileId,BabyName,EventType,StartTime,EndTime,DurationMinutes,NursingType,AmountMl,DiaperType,Notes\n")

        for (e in events) {
            val babyName = profileMap[e.babyProfileId]?.name ?: "Profile ${e.babyProfileId}"
            val startStr = dateFormat.format(e.startTime)
            val endStr = e.endTime?.let { dateFormat.format(it) } ?: ""
            val durationMinutes = e.endTime?.let {
                TimeUnit.MILLISECONDS.toMinutes(maxOf(0L, it - e.startTime))
            } ?: ""
            val nursingType = e.nursingType?.name ?: ""
            val amountMl = e.amountMl?.toString() ?: ""
            val diaperType = e.diaperType?.name ?: ""
            val notes = (e.note ?: "").replace("\"", "\"\"")

            sb.append("${e.babyProfileId},\"$babyName\",${e.type.name},\"$startStr\",\"$endStr\",$durationMinutes,$nursingType,$amountMl,$diaperType,\"$notes\"\n")
        }

        return sb.toString()
    }
}
