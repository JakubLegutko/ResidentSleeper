package com.residentsleeper.ui.review

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.residentsleeper.data.local.AppDatabase
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

data class ReviewUiState(
    val selectedPeriod: ReviewPeriod = ReviewPeriod.DAILY,
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

    private fun loadReview(period: ReviewPeriod) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            when (period) {
                ReviewPeriod.DAILY -> {
                    val startToday = getStartOfDay(System.currentTimeMillis())
                    val endToday = getEndOfDay(System.currentTimeMillis())
                    val events = repository.getEventsInRangeSync(startToday, endToday)
                    val summary = StatisticsCalculator.calculateDailySummary(startToday, events)
                    _uiState.value = _uiState.value.copy(
                        dailySummary = summary,
                        aggregatedReview = null,
                        isLoading = false
                    )
                }
                ReviewPeriod.WEEKLY -> {
                    val summaries = loadPastDaysSummaries(7)
                    val aggregated = StatisticsCalculator.aggregateSummaries("Past 7 Days", summaries)
                    _uiState.value = _uiState.value.copy(
                        dailySummary = null,
                        aggregatedReview = aggregated,
                        isLoading = false
                    )
                }
                ReviewPeriod.MONTHLY -> {
                    val summaries = loadPastDaysSummaries(30)
                    val aggregated = StatisticsCalculator.aggregateSummaries("Past 30 Days", summaries)
                    _uiState.value = _uiState.value.copy(
                        dailySummary = null,
                        aggregatedReview = aggregated,
                        isLoading = false
                    )
                }
            }
        }
    }

    private suspend fun loadPastDaysSummaries(daysCount: Int): List<DailySummary> {
        val list = mutableListOf<DailySummary>()
        val cal = Calendar.getInstance()

        for (i in (daysCount - 1) downTo 0) {
            val targetCal = Calendar.getInstance().apply {
                timeInMillis = cal.timeInMillis
                add(Calendar.DAY_OF_YEAR, -i)
            }
            val start = getStartOfDay(targetCal.timeInMillis)
            val end = getEndOfDay(targetCal.timeInMillis)
            val events = repository.getEventsInRangeSync(start, end)
            list.add(StatisticsCalculator.calculateDailySummary(start, events))
        }
        return list
    }

    private fun getStartOfDay(timestamp: Long): Long {
        return Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private fun getEndOfDay(timestamp: Long): Long {
        return Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis
    }
}
