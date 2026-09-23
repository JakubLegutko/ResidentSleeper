# ResidentSleeper 👶💤

ResidentSleeper is a native Android application built with **Jetpack Compose**, **Room SQLite**, and **Material 3**, specifically designed to track a newborn's biological rhythms (Eat – Activity – Sleep) and provide parents with actionable predictability.

---

## 🌟 Key Features

1. **24-Hour Donut / Ring Clock Chart**:
   - Hollow circular 24h clock track with customizable thickness.
   - Dynamic arcs displaying **Sleep periods** (deep indigo).
   - Ticks and labels for major hours (00, 06, 12, 18).
   - Point-in-time badges for **Nursing** (bottle vs breast side) and **Diaper changes** (pee vs poo).
   - Real-time **Clock Needle** indicating the exact current time of day.
   - Hollow center displaying live state: Sleeping vs Awake duration, wake window countdown, and next feed countdown.

2. **Fast 4-Action Logging Buttons**:
   - **Sleep Button**: Toggle between Start Sleep and Wake Up. Tap for instant "Right Now", or long-press to open the quick offset / custom time picker.
   - **Nursing Button**: Toggle Start / End Nursing with side selection (Left / Right / Both Breasts) or Bottle selection (with amount in ml).
   - **Pee Diaper Button**: One-shot instant logging. Long-press for time adjustment.
   - **Poo Diaper Button**: One-shot instant logging. Long-press for time adjustment.

3. **Predictive Wake Window Engine**:
   - Automatically adapts wake window duration based on baby's age in weeks (derived from birth date):
     - **0–4 weeks**: 50 minutes (45–60 min range)
     - **5–8 weeks**: 65 minutes (60–75 min range)
     - **9–12 weeks**: 80 minutes (75–90 min range)
     - **13–16 weeks**: 100 minutes (90–120 min range)
   - Supports manual override in Settings.
   - Senses when wake window is ending and triggers soothing reminder before overtiredness occurs.

4. **Approximated Feeding Predictor**:
   - Calculates next feeding approximation measured from the *start* of the previous feeding (standard pediatric practice).
   - Live timer displaying time elapsed since last feed and countdown to next feed.

5. **Alerts & Google Calendar Sync**:
   - **Push Notifications**: Exact system alarms via `AlarmManager.setExactAndAllowWhileIdle` firing **10 minutes prior** to wake window end and upcoming feeding.
   - **Device Reboot Resilience**: `BootReceiver` automatically restores alarms upon phone reboot.
   - **Google Calendar Sync**: Native integration via Android `CalendarContract`. Select your synced Google account in Settings to insert events with 10-minute alert reminders.

6. **Review & Analytics Screen**:
   - **Daily Review**: Total sleep hours (daytime naps vs night sleep), nap count, average wake window, feeding count, bottle ml total, and diaper counts.
   - **Weekly Average**: 7-day rolling daily averages with visual sleep trend bar chart.
   - **Monthly Average**: 30-day aggregated trends.

---

## 🛠️ Tech Stack & Architecture

- **Language**: Kotlin 2.0
- **UI Toolkit**: Jetpack Compose (Material 3)
- **Local Database**: Room SQLite (`androidx.room`) with Flow / Coroutines
- **Architecture**: MVVM (Model-View-ViewModel) + Clean Architecture
- **Alarms**: Android `AlarmManager` with exact alarms and notification channels
- **Calendar**: Android `CalendarContract` Provider
- **Unit Testing**: JUnit 4, Google Truth, Kotlinx Coroutines Test

---

## 📂 Project Structure

```
ResidentSleeper/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/residentsleeper/
│   │   │   │   ├── calendar/CalendarSyncManager.kt
│   │   │   │   ├── data/
│   │   │   │   │   ├── local/ (AppDatabase, BabyEventDao, Converters)
│   │   │   │   │   ├── model/ (BabyEvent, BabyProfile, Enums)
│   │   │   │   │   └── repository/ (BabyRepository)
│   │   │   │   ├── domain/
│   │   │   │   │   ├── WakeWindowCalculator.kt
│   │   │   │   │   ├── FeedingPredictor.kt
│   │   │   │   │   └── StatisticsCalculator.kt
│   │   │   │   ├── notifications/ (BabyAlarmScheduler, AlarmReceiver, BootReceiver)
│   │   │   │   ├── ui/
│   │   │   │   │   ├── chart/ (Clock24HourChart, ClockChartMath)
│   │   │   │   │   ├── components/ (TimeAdjustDialog, NursingDetailsDialog)
│   │   │   │   │   ├── dashboard/ (DashboardScreen, DashboardViewModel)
│   │   │   │   │   ├── review/ (ReviewScreen, ReviewViewModel)
│   │   │   │   │   ├── settings/ (SettingsScreen, SettingsViewModel)
│   │   │   │   │   └── theme/ (Color, Theme)
│   │   │   │   ├── MainActivity.kt
│   │   │   │   └── ResidentSleeperApp.kt
│   │   │   ├── res/ (values/strings.xml, values-pl/strings.xml, themes.xml, xml/)
│   │   │   └── AndroidManifest.xml
│   │   └── test/java/com/residentsleeper/
│   │       ├── domain/ (WakeWindowCalculatorTest, FeedingPredictorTest, StatisticsCalculatorTest)
│   │       └── ui/chart/ (Clock24HourChartMathTest)
│   └── build.gradle.kts
├── gradle/
│   ├── libs.versions.toml
│   └── wrapper/
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

---

## 🧪 Testing

The domain calculators, angle math, and aggregation logic are covered by unit tests:

- `WakeWindowCalculatorTest`: Tests age-based wake window rules (0-4w, 5-8w, etc.), custom overrides, active sleep vs wake states, and 10-minute alert trigger calculations.
- `FeedingPredictorTest`: Tests feed interval countdowns from feed start time, bottle amount retention, and 10-minute warning alert calculation.
- `Clock24HourChartMathTest`: Tests angle conversion (midnight at top -90°, 06:00 at 0°, 12:00 at 90°, 18:00 at 180°) and interval arc splitting across midnight.
- `StatisticsCalculatorTest`: Tests day sleep vs night sleep breakdown (19:00 - 07:00), average wake window between naps, diaper tallies, and weekly/monthly aggregation.

To run the unit tests:
```bash
./gradlew test
```
