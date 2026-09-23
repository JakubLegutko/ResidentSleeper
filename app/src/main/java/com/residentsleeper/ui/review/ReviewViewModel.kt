package com.residentsleeper.ui.review

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.residentsleeper.data.local.AppDatabase
import com.residentsleeper.data.model.BabyProfile
import com.residentsleeper.data.repository.BabyRepository
import com.residentsleeper.domain.AggregatedReview
import com.residentsleeper.domain.DailySummary
import com.residentsleeper.domain.StatisticsCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

enum class ReviewPeriod {
    DAILY,
    WEEKLY,
    MONTHLY
}

enum class ReviewMetric {
    SLEEP,
    WAKE_WINDOW,
    FEEDINGS,
    DIAPERS
}

data class ReviewUiState(
    val activeProfile: BabyProfile = BabyProfile(),
    val selectedPeriod: ReviewPeriod = ReviewPeriod.DAILY,
    val selectedMetric: ReviewMetric = ReviewMetric.SLEEP,
    val dailySummary: DailySummary? = null,
    val aggregatedReview: AggregatedReview? = null,
    val isLoading: Boolean = false
)

class ReviewViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: BabyRepository
    private val _uiState = MutableStateFlow(ReviewUiState())
    val uiState: StateFlow<ReviewUiState> = _uiState.asStateFlow()

    init {
        val db = AppDatabase.getDatabase(application)
        repository = BabyRepository(db.babyEventDao(), db.babyProfileDao())
        loadReview(ReviewPeriod.DAILY)
    }

    fun setPeriod(period: ReviewPeriod) {
        _uiState.value = _uiState.value.copy(selectedPeriod = period)
        loadReview(period)
    }

    fun selectMetric(metric: ReviewMetric) {
        _uiState.value = _uiState.value.copy(selectedMetric = metric)
    }

    private fun loadReview(period: ReviewPeriod) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val profile = repository.getActiveProfile()
            val startHour = profile.dayStartHour

            when (period) {
                ReviewPeriod.DAILY -> {
                    val startToday = getStartOfDay(System.currentTimeMillis(), startHour)
                    val endToday = getEndOfDay(System.currentTimeMillis(), startHour)
                    val events = repository.getEventsInRangeSync(profile.id, startToday, endToday)
                    val summary = StatisticsCalculator.calculateDailySummary(startToday, events)
                    _uiState.value = _uiState.value.copy(
                        activeProfile = profile,
                        dailySummary = summary,
                        aggregatedReview = null,
                        isLoading = false
                    )
                }
                ReviewPeriod.WEEKLY -> {
                    val summaries = loadPastDaysSummaries(profile.id, 7, startHour)
                    val aggregated = StatisticsCalculator.aggregateSummaries("Past 7 Days", summaries)
                    _uiState.value = _uiState.value.copy(
                        activeProfile = profile,
                        dailySummary = null,
                        aggregatedReview = aggregated,
                        isLoading = false
                    )
                }
                ReviewPeriod.MONTHLY -> {
                    val summaries = loadPastDaysSummaries(profile.id, 30, startHour)
                    val aggregated = StatisticsCalculator.aggregateSummaries("Past 30 Days", summaries)
                    _uiState.value = _uiState.value.copy(
                        activeProfile = profile,
                        dailySummary = null,
                        aggregatedReview = aggregated,
                        isLoading = false
                    )
                }
            }
        }
    }

    private suspend fun loadPastDaysSummaries(profileId: Long, daysCount: Int, startHour: Int = 7): List<DailySummary> {
        val list = mutableListOf<DailySummary>()
        val cal = Calendar.getInstance()

        for (i in (daysCount - 1) downTo 0) {
            val targetCal = Calendar.getInstance().apply {
                timeInMillis = cal.timeInMillis
                add(Calendar.DAY_OF_YEAR, -i)
            }
            val start = getStartOfDay(targetCal.timeInMillis, startHour)
            val end = getEndOfDay(targetCal.timeInMillis, startHour)
            val events = repository.getEventsInRangeSync(profileId, start, end)
            list.add(StatisticsCalculator.calculateDailySummary(start, events))
        }
        return list
    }

    private fun getStartOfDay(timestamp: Long, startHour: Int = 7): Long {
        return Calendar.getInstance().apply {
            timeInMillis = timestamp
            if (get(Calendar.HOUR_OF_DAY) < startHour) {
                add(Calendar.DAY_OF_YEAR, -1)
            }
            set(Calendar.HOUR_OF_DAY, startHour)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private fun getEndOfDay(timestamp: Long, startHour: Int = 7): Long {
        val s = getStartOfDay(timestamp, startHour)
        return s + (24 * 60 * 60 * 1000L) - 1L
    }
}
