package com.residentsleeper.ui.dashboard

import com.google.common.truth.Truth.assertThat
import com.residentsleeper.data.model.BabyEvent
import com.residentsleeper.data.model.DiaperType
import com.residentsleeper.data.model.EventType
import com.residentsleeper.domain.FeedingState
import com.residentsleeper.domain.WakeWindowState
import org.junit.Test

class DashboardUiStateTest {

    private fun createBaseState(events: List<BabyEvent>): DashboardUiState {
        return DashboardUiState(
            selectedDayStart = 1000L,
            selectedDayEnd = 2000L,
            isToday = true,
            events = events,
            wakeWindowState = com.residentsleeper.domain.WakeWindowCalculator.computeState(com.residentsleeper.data.model.BabyProfile(), null, null),
            feedingState = com.residentsleeper.domain.FeedingPredictor.computeState(com.residentsleeper.data.model.BabyProfile(), null, null)
        )
    }

    @Test
    fun diaperCounts_emptyEvents_returnsZero() {
        val state = createBaseState(emptyList())
        assertThat(state.peeCount).isEqualTo(0)
        assertThat(state.pooCount).isEqualTo(0)
    }

    @Test
    fun diaperCounts_separatePeeAndPoo_countedAccurately() {
        val events = listOf(
            BabyEvent(id = 1, type = EventType.DIAPER, startTime = 1100L, diaperType = DiaperType.PEE),
            BabyEvent(id = 2, type = EventType.DIAPER, startTime = 1200L, diaperType = DiaperType.PEE),
            BabyEvent(id = 3, type = EventType.DIAPER, startTime = 1300L, diaperType = DiaperType.POO),
            BabyEvent(id = 4, type = EventType.SLEEP, startTime = 1400L, endTime = 1500L)
        )
        val state = createBaseState(events)
        assertThat(state.peeCount).isEqualTo(2)
        assertThat(state.pooCount).isEqualTo(1)
    }

    @Test
    fun diaperCounts_comboBoth_incrementsBothPeeAndPoo() {
        val events = listOf(
            BabyEvent(id = 1, type = EventType.DIAPER, startTime = 1100L, diaperType = DiaperType.PEE),
            BabyEvent(id = 2, type = EventType.DIAPER, startTime = 1200L, diaperType = DiaperType.BOTH)
        )
        val state = createBaseState(events)
        assertThat(state.peeCount).isEqualTo(2) // 1 pee + 1 both
        assertThat(state.pooCount).isEqualTo(1) // 1 both
    }

    @Test
    fun diaperCounts_removingEvent_decrementsCount() {
        val eventPee = BabyEvent(id = 1, type = EventType.DIAPER, startTime = 1100L, diaperType = DiaperType.PEE)
        val eventBoth = BabyEvent(id = 2, type = EventType.DIAPER, startTime = 1200L, diaperType = DiaperType.BOTH)
        val initialEvents = listOf(eventPee, eventBoth)

        val stateBefore = createBaseState(initialEvents)
        assertThat(stateBefore.peeCount).isEqualTo(2)
        assertThat(stateBefore.pooCount).isEqualTo(1)

        // Simulating removing the combo event
        val stateAfter = stateBefore.copy(events = initialEvents - eventBoth)
        assertThat(stateAfter.peeCount).isEqualTo(1)
        assertThat(stateAfter.pooCount).isEqualTo(0)

        // Simulating removing the pee event
        val stateEmpty = stateAfter.copy(events = emptyList())
        assertThat(stateEmpty.peeCount).isEqualTo(0)
        assertThat(stateEmpty.pooCount).isEqualTo(0)
    }

    @Test
    fun diaperCounts_legacyNullDiaperType_defaultsToPee() {
        val events = listOf(
            BabyEvent(id = 1, type = EventType.DIAPER, startTime = 1100L, diaperType = null)
        )
        val state = createBaseState(events)
        assertThat(state.peeCount).isEqualTo(1)
        assertThat(state.pooCount).isEqualTo(0)
    }
}
