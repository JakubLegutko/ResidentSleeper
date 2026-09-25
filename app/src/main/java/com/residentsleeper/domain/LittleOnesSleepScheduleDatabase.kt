package com.residentsleeper.domain

import android.content.Context
import com.residentsleeper.R

enum class SleepCategory {
    MORNING_NAP,
    MIDDAY_NAP,
    BRIDGE_CATNAP,
    BEDTIME
}

data class AgeSleepSchedule(
    val ageBracketLabel: String,
    val minAgeWeeks: Int,
    val maxAgeWeeks: Int,
    val totalSleepTargetHours: Float,
    val totalDaySleepTargetHours: Float,
    val totalNightSleepTargetHours: Float,
    val targetNapsCount: Int,
    // Wake windows in minutes by period of day
    val morningWakeWindowMin: Int,
    val middayWakeWindowMin: Int,
    val afternoonWakeWindowMin: Int,
    val preBedtimeWakeWindowMin: Int,
    // Recommended nap durations
    val morningNapDurationMin: Int,
    val middayNapDurationMin: Int,
    val catnapDurationMin: Int,
    val bedtimeStartHour: Int = 18,
    val bedtimeStartMinute: Int = 30,
    val bedtimeEndHour: Int = 19,
    val bedtimeEndMinute: Int = 30,
    val triviaTips: List<String>,
    val ageBracketLabelRes: Int = 0,
    val minWakeWindowMin: Int = 35,
    val maxWakeWindowMin: Int = 60,
    val developmentalMechanismRes: Int = 0
) {
    fun getLocalizedAgeBracket(context: Context): String {
        return if (ageBracketLabelRes != 0) context.getString(ageBracketLabelRes) else ageBracketLabel
    }

    fun getLocalizedMechanism(context: Context): String {
        return if (developmentalMechanismRes != 0) context.getString(developmentalMechanismRes) else ""
    }
}

object LittleOnesSleepScheduleDatabase {

    val schedules: List<AgeSleepSchedule> = listOf(
        AgeSleepSchedule(
            ageBracketLabel = "0–4 Weeks (Newborn)",
            ageBracketLabelRes = R.string.age_bracket_0_4,
            minAgeWeeks = 0,
            maxAgeWeeks = 4,
            totalSleepTargetHours = 16.5f,
            totalDaySleepTargetHours = 7.0f,
            totalNightSleepTargetHours = 9.5f,
            targetNapsCount = 5,
            morningWakeWindowMin = 50,
            middayWakeWindowMin = 60,
            afternoonWakeWindowMin = 60,
            preBedtimeWakeWindowMin = 60,
            morningNapDurationMin = 60,
            middayNapDurationMin = 120,
            catnapDurationMin = 45,
            bedtimeStartHour = 19,
            bedtimeStartMinute = 0,
            bedtimeEndHour = 20,
            bedtimeEndMinute = 30,
            minWakeWindowMin = 35,
            maxWakeWindowMin = 60,
            developmentalMechanismRes = R.string.sleep_mech_0_4_weeks,
            triviaTips = listOf(
                "Newborns do not yet have a circadian rhythm. Exposing baby to natural daylight during awake times helps calibrate their developing internal body clock.",
                "At 1 month old, wake windows include feeding, changing, interaction, and soothing. Sticking close to 50–60 minutes prevents rapid overtiredness.",
                "Limiting individual daytime naps to 2–2.5 hours helps protect important nighttime sleep stretches."
            )
        ),
        AgeSleepSchedule(
            ageBracketLabel = "5–8 Weeks (~2 Months)",
            ageBracketLabelRes = R.string.age_bracket_5_8,
            minAgeWeeks = 5,
            maxAgeWeeks = 8,
            totalSleepTargetHours = 15.5f,
            totalDaySleepTargetHours = 4.5f,
            totalNightSleepTargetHours = 11.0f,
            targetNapsCount = 4,
            morningWakeWindowMin = 65,
            middayWakeWindowMin = 80,
            afternoonWakeWindowMin = 80,
            preBedtimeWakeWindowMin = 90,
            morningNapDurationMin = 45,
            middayNapDurationMin = 120,
            catnapDurationMin = 35,
            bedtimeStartHour = 18,
            bedtimeStartMinute = 45,
            bedtimeEndHour = 19,
            bedtimeEndMinute = 30,
            minWakeWindowMin = 60,
            maxWakeWindowMin = 90,
            developmentalMechanismRes = R.string.sleep_mech_5_8_weeks,
            triviaTips = listOf(
                "Between 6 and 8 weeks, infant circadian rhythm begins to emerge. Anchoring a consistent morning wake time (around 7:00 AM) sets up a smoother daily rhythm.",
                "At 2 months, the first stretch of night sleep naturally begins to lengthen, especially with a solid 2-hour midday nap in place.",
                "Wake windows that are too long cause cortisol spikes. If baby is fussy or resisting sleep, tighten the wake window by 15 minutes."
            )
        ),
        AgeSleepSchedule(
            ageBracketLabel = "9–12 Weeks (~3 Months)",
            ageBracketLabelRes = R.string.age_bracket_9_12,
            minAgeWeeks = 9,
            maxAgeWeeks = 12,
            totalSleepTargetHours = 15.5f,
            totalDaySleepTargetHours = 3.5f,
            totalNightSleepTargetHours = 12.0f,
            targetNapsCount = 3,
            morningWakeWindowMin = 80,
            middayWakeWindowMin = 100,
            afternoonWakeWindowMin = 105,
            preBedtimeWakeWindowMin = 120,
            morningNapDurationMin = 40,
            middayNapDurationMin = 120,
            catnapDurationMin = 35,
            bedtimeStartHour = 18,
            bedtimeStartMinute = 30,
            bedtimeEndHour = 19,
            bedtimeEndMinute = 15,
            minWakeWindowMin = 75,
            maxWakeWindowMin = 120,
            developmentalMechanismRes = R.string.sleep_mech_9_12_weeks,
            triviaTips = listOf(
                "By 12 weeks, babies consolidate sleep into a short morning nap, a long 2-hour lunchtime sleep, and a short afternoon catnap.",
                "Consistency in your 3-month daytime structure lays the foundation for navigating the upcoming 4-month sleep regression smoothly.",
                "A calming 20-minute wind-down routine in dim lighting signals melatonin release, helping your baby settle without overtiredness."
            )
        ),
        AgeSleepSchedule(
            ageBracketLabel = "13–16 Weeks (~4 Months)",
            ageBracketLabelRes = R.string.age_bracket_13_16,
            minAgeWeeks = 13,
            maxAgeWeeks = 16,
            totalSleepTargetHours = 14.5f,
            totalDaySleepTargetHours = 3.5f,
            totalNightSleepTargetHours = 11.0f,
            targetNapsCount = 3,
            morningWakeWindowMin = 105,
            middayWakeWindowMin = 120,
            afternoonWakeWindowMin = 120,
            preBedtimeWakeWindowMin = 135,
            morningNapDurationMin = 40,
            middayNapDurationMin = 120,
            catnapDurationMin = 30,
            bedtimeStartHour = 18,
            bedtimeStartMinute = 30,
            bedtimeEndHour = 19,
            bedtimeEndMinute = 30,
            minWakeWindowMin = 75,
            maxWakeWindowMin = 140,
            developmentalMechanismRes = R.string.sleep_mech_13_16_weeks,
            triviaTips = listOf(
                "The 4-Month Regression is a permanent biological milestone: infant sleep cycles mature into 4 adult-like stages every 45–50 minutes.",
                "Because babies surface between sleep cycles around 4 months, practicing putting baby down drowsy but awake builds crucial independent settling skills.",
                "Maintaining an age-appropriate 2-hour wake window prevents overtiredness, which is the #1 cause of frequent night wakes."
            )
        ),
        AgeSleepSchedule(
            ageBracketLabel = "17–21 Weeks (~5 Months)",
            ageBracketLabelRes = R.string.age_bracket_17_21,
            minAgeWeeks = 17,
            maxAgeWeeks = 21,
            totalSleepTargetHours = 14.5f,
            totalDaySleepTargetHours = 3.25f,
            totalNightSleepTargetHours = 11.25f,
            targetNapsCount = 3,
            morningWakeWindowMin = 120,
            middayWakeWindowMin = 135,
            afternoonWakeWindowMin = 135,
            preBedtimeWakeWindowMin = 145,
            morningNapDurationMin = 45,
            middayNapDurationMin = 120,
            catnapDurationMin = 25,
            bedtimeStartHour = 18,
            bedtimeStartMinute = 30,
            bedtimeEndHour = 19,
            bedtimeEndMinute = 15,
            minWakeWindowMin = 90,
            maxWakeWindowMin = 150,
            developmentalMechanismRes = R.string.sleep_mech_17_21_weeks,
            triviaTips = listOf(
                "Five months often brings real predictability: a 45m morning nap, a 2h restorative lunchtime sleep, and a short 20–30m bridge catnap.",
                "Ensure the late afternoon catnap ends no later than 5:00 PM to protect sleep pressure for a 7:00 PM bedtime.",
                "Self-settling skills practiced during daytime naps carry over directly to reducing midnight wakeups."
            )
        ),
        AgeSleepSchedule(
            ageBracketLabel = "22–25 Weeks (~6 Months)",
            ageBracketLabelRes = R.string.age_bracket_22_25,
            minAgeWeeks = 22,
            maxAgeWeeks = 25,
            totalSleepTargetHours = 14.25f,
            totalDaySleepTargetHours = 2.75f,
            totalNightSleepTargetHours = 11.5f,
            targetNapsCount = 3,
            morningWakeWindowMin = 135,
            middayWakeWindowMin = 150,
            afternoonWakeWindowMin = 165,
            preBedtimeWakeWindowMin = 160,
            morningNapDurationMin = 35,
            middayNapDurationMin = 110,
            catnapDurationMin = 15,
            bedtimeStartHour = 18,
            bedtimeStartMinute = 30,
            bedtimeEndHour = 19,
            bedtimeEndMinute = 0,
            minWakeWindowMin = 120,
            maxWakeWindowMin = 165,
            developmentalMechanismRes = R.string.sleep_mech_22_25_weeks,
            triviaTips = listOf(
                "At 6 months, the 3-to-2 nap transition begins. The late afternoon nap shrinks to a 10–15 minute bridge catnap.",
                "If your 6-month-old skips or refuses the 3rd nap, pull bedtime forward to 6:30 PM to avoid an overtired bedtime meltdown.",
                "Introducing solid foods and learning to roll can temporarily cause nighttime restlessness; keep the bedtime routine steady."
            )
        ),
        AgeSleepSchedule(
            ageBracketLabel = "26–30 Weeks (~7 Months)",
            ageBracketLabelRes = R.string.age_bracket_26_30,
            minAgeWeeks = 26,
            maxAgeWeeks = 30,
            totalSleepTargetHours = 14.0f,
            totalDaySleepTargetHours = 2.5f,
            totalNightSleepTargetHours = 11.5f,
            targetNapsCount = 2,
            morningWakeWindowMin = 140,
            middayWakeWindowMin = 165,
            afternoonWakeWindowMin = 180,
            preBedtimeWakeWindowMin = 195,
            morningNapDurationMin = 35,
            middayNapDurationMin = 110,
            catnapDurationMin = 15,
            bedtimeStartHour = 18,
            bedtimeStartMinute = 30,
            bedtimeEndHour = 19,
            bedtimeEndMinute = 0,
            minWakeWindowMin = 135,
            maxWakeWindowMin = 210,
            developmentalMechanismRes = R.string.sleep_mech_26_30_weeks,
            triviaTips = listOf(
                "Fighting the 3rd nap is the primary sign baby is ready for a 2-nap routine (morning nap ~9:30 AM, lunchtime nap ~12:30 PM).",
                "Wake windows naturally lengthen to 2.5–3 hours, with the shortest window in the morning and longest before bed.",
                "If morning wake is early (before 6:00 AM), check if bedtime was too late; overtiredness causes early morning waking."
            )
        ),
        AgeSleepSchedule(
            ageBracketLabel = "31–34 Weeks (~8 Months)",
            ageBracketLabelRes = R.string.age_bracket_31_34,
            minAgeWeeks = 31,
            maxAgeWeeks = 34,
            totalSleepTargetHours = 14.0f,
            totalDaySleepTargetHours = 2.5f,
            totalNightSleepTargetHours = 11.5f,
            targetNapsCount = 2,
            morningWakeWindowMin = 150,
            middayWakeWindowMin = 180,
            afternoonWakeWindowMin = 195,
            preBedtimeWakeWindowMin = 210,
            morningNapDurationMin = 30,
            middayNapDurationMin = 110,
            catnapDurationMin = 0,
            bedtimeStartHour = 18,
            bedtimeStartMinute = 30,
            bedtimeEndHour = 19,
            bedtimeEndMinute = 0,
            minWakeWindowMin = 150,
            maxWakeWindowMin = 210,
            developmentalMechanismRes = R.string.sleep_mech_31_34_weeks,
            triviaTips = listOf(
                "The 8-Month Sleep Regression is driven by major physical leaps: crawling, pulling to stand, and emerging separation anxiety.",
                "At 8 months, 2 naps totaling ~2.5 hours of day sleep provides the sweet spot for 11–12 hours of consolidated night sleep.",
                "Keep bedtime between 6:30 and 7:00 PM. A consistent sequence of bath, feeding, and lullaby anchors baby through separation anxiety."
            )
        ),
        AgeSleepSchedule(
            ageBracketLabel = "35–38 Weeks (~9 Months)",
            ageBracketLabelRes = R.string.age_bracket_35_38,
            minAgeWeeks = 35,
            maxAgeWeeks = 38,
            totalSleepTargetHours = 13.75f,
            totalDaySleepTargetHours = 2.25f,
            totalNightSleepTargetHours = 11.5f,
            targetNapsCount = 2,
            morningWakeWindowMin = 155,
            middayWakeWindowMin = 180,
            afternoonWakeWindowMin = 200,
            preBedtimeWakeWindowMin = 210,
            morningNapDurationMin = 30,
            middayNapDurationMin = 105,
            catnapDurationMin = 0,
            bedtimeStartHour = 18,
            bedtimeStartMinute = 45,
            bedtimeEndHour = 19,
            bedtimeEndMinute = 0,
            minWakeWindowMin = 150,
            maxWakeWindowMin = 220,
            developmentalMechanismRes = R.string.sleep_mech_35_38_weeks,
            triviaTips = listOf(
                "At 9 months, overtiredness often disguises itself as hyperactivity, standing in the cot, or loud babbling rather than yawning!",
                "Most 9-month-olds thrive on: 7:00 AM wake, 9:30 AM nap 1 (30m), 12:30 PM nap 2 (1.5–2h), 6:45 PM bedtime.",
                "Avoid letting the afternoon nap run past 2:30–2:45 PM so that sleep pressure rebuilds adequately for bedtime."
            )
        ),
        AgeSleepSchedule(
            ageBracketLabel = "39–43 Weeks (~10 Months)",
            ageBracketLabelRes = R.string.age_bracket_39_43,
            minAgeWeeks = 39,
            maxAgeWeeks = 43,
            totalSleepTargetHours = 13.75f,
            totalDaySleepTargetHours = 2.25f,
            totalNightSleepTargetHours = 11.5f,
            targetNapsCount = 2,
            morningWakeWindowMin = 165,
            middayWakeWindowMin = 195,
            afternoonWakeWindowMin = 210,
            preBedtimeWakeWindowMin = 225,
            morningNapDurationMin = 30,
            middayNapDurationMin = 95,
            catnapDurationMin = 0,
            bedtimeStartHour = 18,
            bedtimeStartMinute = 45,
            bedtimeEndHour = 19,
            bedtimeEndMinute = 15,
            minWakeWindowMin = 165,
            maxWakeWindowMin = 240,
            developmentalMechanismRes = R.string.sleep_mech_39_43_weeks,
            triviaTips = listOf(
                "At 10 months, if your baby resists nap 1, cap it to 20–30 minutes to preserve their sleep pressure for the longer midday nap.",
                "Separation awareness peaks at 10 months. Spending 5 minutes playing peek-a-boo in the nursery builds confidence before naptime.",
                "Do not drop to 1 nap yet! 10-month-olds who drop to 1 nap quickly accumulate severe sleep debt resulting in split nights."
            )
        ),
        AgeSleepSchedule(
            ageBracketLabel = "44–47 Weeks (~11 Months)",
            ageBracketLabelRes = R.string.age_bracket_44_47,
            minAgeWeeks = 44,
            maxAgeWeeks = 47,
            totalSleepTargetHours = 13.5f,
            totalDaySleepTargetHours = 2.0f,
            totalNightSleepTargetHours = 11.5f,
            targetNapsCount = 2,
            morningWakeWindowMin = 180,
            middayWakeWindowMin = 205,
            afternoonWakeWindowMin = 215,
            preBedtimeWakeWindowMin = 240,
            morningNapDurationMin = 30,
            middayNapDurationMin = 90,
            catnapDurationMin = 0,
            bedtimeStartHour = 18,
            bedtimeStartMinute = 45,
            bedtimeEndHour = 19,
            bedtimeEndMinute = 15,
            minWakeWindowMin = 180,
            maxWakeWindowMin = 240,
            developmentalMechanismRes = R.string.sleep_mech_44_47_weeks,
            triviaTips = listOf(
                "Beware the 11-Month False 1-Nap Trap! Babies often test boundaries by resisting nap 2, but genuine 1-nap readiness rarely occurs before 14–15 months.",
                "Wake windows now comfortably reach 3.5 to 4 hours before bedtime.",
                "Balancing daytime sleep to around 2 hours total is key to preventing 5:00 AM early morning rising."
            )
        ),
        AgeSleepSchedule(
            ageBracketLabel = "48+ Weeks (12+ Months)",
            ageBracketLabelRes = R.string.age_bracket_48_plus,
            minAgeWeeks = 48,
            maxAgeWeeks = 150,
            totalSleepTargetHours = 13.25f,
            totalDaySleepTargetHours = 1.75f,
            totalNightSleepTargetHours = 11.5f,
            targetNapsCount = 1,
            morningWakeWindowMin = 210,
            middayWakeWindowMin = 240,
            afternoonWakeWindowMin = 250,
            preBedtimeWakeWindowMin = 270,
            morningNapDurationMin = 30,
            middayNapDurationMin = 90,
            catnapDurationMin = 0,
            bedtimeStartHour = 19,
            bedtimeStartMinute = 0,
            bedtimeEndHour = 19,
            bedtimeEndMinute = 30,
            minWakeWindowMin = 210,
            maxWakeWindowMin = 300,
            developmentalMechanismRes = R.string.sleep_mech_48_plus_weeks,
            triviaTips = listOf(
                "The 2-to-1 nap transition happens between 12 and 18 months, most commonly around 14–15 months when baby handles a 5-hour morning window.",
                "When transitioning to 1 nap, schedule the single nap in the middle of the day (~12:00–12:30 PM) for 2 to 2.5 hours.",
                "During nap transitions, bring bedtime 30–45 minutes earlier to avoid overtiredness until the new schedule consolidates."
            )
        )
    )

    fun getScheduleForAge(ageInWeeks: Int): AgeSleepSchedule {
        val safeAge = maxOf(0, ageInWeeks)
        return schedules.find { safeAge in it.minAgeWeeks..it.maxAgeWeeks }
            ?: schedules.last()
    }

    fun getRandomTriviaForAge(ageInWeeks: Int): String {
        val sched = getScheduleForAge(ageInWeeks)
        return sched.triviaTips.random()
    }
}
