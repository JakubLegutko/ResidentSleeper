package com.residentsleeper.data.model

enum class EventType {
    SLEEP,
    NURSING,
    DIAPER
}

enum class NursingType {
    LEFT_BREAST,
    RIGHT_BREAST,
    BOTH_BREASTS,
    BOTTLE
}

enum class DiaperType {
    PEE,
    POO,
    BOTH
}

enum class Gender {
    UNSPECIFIED,
    BOY,
    GIRL;

    val emote: String
        get() = when (this) {
            BOY -> "\uD83D\uDC66"        // 👦
            GIRL -> "\uD83D\uDC67"       // 👧
            UNSPECIFIED -> "\uD83D\uDC76" // 👶
        }
}
