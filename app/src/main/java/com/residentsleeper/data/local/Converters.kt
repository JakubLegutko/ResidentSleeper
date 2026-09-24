package com.residentsleeper.data.local

import androidx.room.TypeConverter
import com.residentsleeper.data.model.DiaperType
import com.residentsleeper.data.model.EventType
import com.residentsleeper.data.model.NursingType

class Converters {
    @TypeConverter
    fun fromEventType(value: EventType?): String? = value?.name

    @TypeConverter
    fun toEventType(value: String?): EventType? =
        value?.let { enumValueOf<EventType>(it) }

    @TypeConverter
    fun fromNursingType(value: NursingType?): String? = value?.name

    @TypeConverter
    fun toNursingType(value: String?): NursingType? =
        value?.let { enumValueOf<NursingType>(it) }

    @TypeConverter
    fun fromDiaperType(value: DiaperType?): String? = value?.name

    @TypeConverter
    fun toDiaperType(value: String?): DiaperType? =
        value?.let { enumValueOf<DiaperType>(it) }

    @TypeConverter
    fun fromGender(value: com.residentsleeper.data.model.Gender?): String? = value?.name

    @TypeConverter
    fun toGender(value: String?): com.residentsleeper.data.model.Gender? =
        value?.let { runCatching { enumValueOf<com.residentsleeper.data.model.Gender>(it) }.getOrDefault(com.residentsleeper.data.model.Gender.UNSPECIFIED) }
}
