package com.residentsleeper.domain

import android.content.Context
import com.residentsleeper.R

data class FeedingScheduleBracket(
    val minAgeDays: Int,
    val maxAgeDays: Int,
    val ageBracketLabel: String,
    val ageBracketLabelRes: Int,
    val minIntervalMinutes: Int,
    val maxIntervalMinutes: Int,
    val defaultIntervalMinutes: Int,
    val recommendedFeedsPerDay: String,
    val typicalFormulaPortionMl: String,
    val dominantMechanism: String,
    val dominantMechanismRes: Int
) {
    fun getLocalizedAgeBracket(context: Context): String {
        return if (ageBracketLabelRes != 0) context.getString(ageBracketLabelRes) else ageBracketLabel
    }

    fun getLocalizedMechanism(context: Context): String {
        return if (dominantMechanismRes != 0) context.getString(dominantMechanismRes) else dominantMechanism
    }
}

data class FeedingIntervalCalculationResult(
    val recommendedIntervalMinutes: Int,
    val ageBracket: FeedingScheduleBracket,
    val ageDays: Int,
    val ageWeeks: Int,
    val observedMedianMinutes: Int?,
    val sampleCount: Int,
    val usedHistoricalData: Boolean
)

object FeedingScheduleDatabase {

    val brackets: List<FeedingScheduleBracket> = listOf(
        // 1.–3. doba: 8–12+ karmień, odstęp co 1.5–3 h, porcja 5–25 ml
        FeedingScheduleBracket(
            minAgeDays = 0,
            maxAgeDays = 3,
            ageBracketLabel = "1.–3. doba",
            ageBracketLabelRes = R.string.feeding_bracket_1_3_days,
            minIntervalMinutes = 90,
            maxIntervalMinutes = 180,
            defaultIntervalMinutes = 120, // 2.0 h
            recommendedFeedsPerDay = "8–12+",
            typicalFormulaPortionMl = "5–25 ml",
            dominantMechanism = "Siara, minimalna pojemność żołądka",
            dominantMechanismRes = R.string.feeding_mech_1_3_days
        ),
        // 4.–14. doba: 8–12 karmień, odstęp co 2–3 h, porcja 30–80 ml
        FeedingScheduleBracket(
            minAgeDays = 4,
            maxAgeDays = 14,
            ageBracketLabel = "4.–14. doba",
            ageBracketLabelRes = R.string.feeding_bracket_4_14_days,
            minIntervalMinutes = 120,
            maxIntervalMinutes = 180,
            defaultIntervalMinutes = 150, // 2.5 h
            recommendedFeedsPerDay = "8–12",
            typicalFormulaPortionMl = "30–80 ml",
            dominantMechanism = "Dynamiczny wzrost laktacji i żołądka",
            dominantMechanismRes = R.string.feeding_mech_4_14_days
        ),
        // 2.–8. tydzień: 7–10 karmień, odstęp co 2.5–3.5 h, porcja 90–120 ml
        FeedingScheduleBracket(
            minAgeDays = 15,
            maxAgeDays = 56,
            ageBracketLabel = "2.–8. tydzień",
            ageBracketLabelRes = R.string.feeding_bracket_2_8_weeks,
            minIntervalMinutes = 150,
            maxIntervalMinutes = 210,
            defaultIntervalMinutes = 180, // 3.0 h
            recommendedFeedsPerDay = "7–10",
            typicalFormulaPortionMl = "90–120 ml",
            dominantMechanism = "Stabilizacja laktacji, skoki rozwojowe",
            dominantMechanismRes = R.string.feeding_mech_2_8_weeks
        ),
        // 2.–4. miesiąc: 6–8 karmień, odstęp co 3–3.5 h, porcja 120–150 ml
        FeedingScheduleBracket(
            minAgeDays = 57,
            maxAgeDays = 120,
            ageBracketLabel = "2.–4. miesiąc",
            ageBracketLabelRes = R.string.feeding_bracket_2_4_months,
            minIntervalMinutes = 180,
            maxIntervalMinutes = 210,
            defaultIntervalMinutes = 195, // 3h 15m
            recommendedFeedsPerDay = "6–8",
            typicalFormulaPortionMl = "120–150 ml",
            dominantMechanism = "Wydłużenie przerw nocnych",
            dominantMechanismRes = R.string.feeding_mech_2_4_months
        ),
        // 5.–6. miesiąc: 5–6 karmień, odstęp co 3.5–4 h, porcja 150–180 ml
        FeedingScheduleBracket(
            minAgeDays = 121,
            maxAgeDays = 182,
            ageBracketLabel = "5.–6. miesiąc",
            ageBracketLabelRes = R.string.feeding_bracket_5_6_months,
            minIntervalMinutes = 210,
            maxIntervalMinutes = 240,
            defaultIntervalMinutes = 225, // 3h 45m
            recommendedFeedsPerDay = "5–6",
            typicalFormulaPortionMl = "150–180 ml",
            dominantMechanism = "Wyższa gęstość kaloryczna porcji",
            dominantMechanismRes = R.string.feeding_mech_5_6_months
        ),
        // 7.–9. miesiąc: 4–5 karmień (w tym stałe), odstęp co 3.5–4 h, porcja 180–210 ml
        FeedingScheduleBracket(
            minAgeDays = 183,
            maxAgeDays = 273,
            ageBracketLabel = "7.–9. miesiąc",
            ageBracketLabelRes = R.string.feeding_bracket_7_9_months,
            minIntervalMinutes = 210,
            maxIntervalMinutes = 240,
            defaultIntervalMinutes = 225, // 3h 45m
            recommendedFeedsPerDay = "4–5",
            typicalFormulaPortionMl = "180–210 ml",
            dominantMechanism = "Wprowadzanie pokarmów uzupełniających",
            dominantMechanismRes = R.string.feeding_mech_7_9_months
        ),
        // 10.–12. miesiąc: 3–4 karmień (w tym stałe), odstęp co 4 h, porcja 210–240 ml
        FeedingScheduleBracket(
            minAgeDays = 274,
            maxAgeDays = 365,
            ageBracketLabel = "10.–12. miesiąc",
            ageBracketLabelRes = R.string.feeding_bracket_10_12_months,
            minIntervalMinutes = 210,
            maxIntervalMinutes = 270,
            defaultIntervalMinutes = 240, // 4.0 h
            recommendedFeedsPerDay = "3–4",
            typicalFormulaPortionMl = "210–240 ml",
            dominantMechanism = "Posiłki stałe stanowią główne źródło energii",
            dominantMechanismRes = R.string.feeding_mech_10_12_months
        ),
        // > 12 miesięcy: 3–4 posiłki, odstęp co 4–5 h
        FeedingScheduleBracket(
            minAgeDays = 366,
            maxAgeDays = Int.MAX_VALUE,
            ageBracketLabel = "> 12 miesięcy",
            ageBracketLabelRes = R.string.feeding_bracket_12_plus_months,
            minIntervalMinutes = 240,
            maxIntervalMinutes = 300,
            defaultIntervalMinutes = 240, // 4.0 h
            recommendedFeedsPerDay = "3–4",
            typicalFormulaPortionMl = "210–240 ml",
            dominantMechanism = "Ustalony rytm posiłków stałych",
            dominantMechanismRes = R.string.feeding_mech_12_plus_months
        )
    )

    fun getBracketForAge(ageDays: Int): FeedingScheduleBracket {
        val safeAge = maxOf(0, ageDays)
        return brackets.firstOrNull { safeAge in it.minAgeDays..it.maxAgeDays }
            ?: brackets.last()
    }
}
