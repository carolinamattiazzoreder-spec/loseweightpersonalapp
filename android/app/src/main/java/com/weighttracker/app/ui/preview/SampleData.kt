package com.weighttracker.app.ui.preview

import com.weighttracker.app.data.AppState
import com.weighttracker.app.data.WeightEntry
import java.time.LocalDate

/** Example data used by @Preview functions and screenshot tests. */
object SampleData {
    val state = AppState(
        weights = listOf(
            WeightEntry(LocalDate.of(2025, 9, 1), 95.0, "Start"),
            WeightEntry(LocalDate.of(2025, 10, 6), 93.4),
            WeightEntry(LocalDate.of(2025, 11, 3), 91.8),
            WeightEntry(LocalDate.of(2025, 12, 1), 90.0),
            WeightEntry(LocalDate.of(2026, 1, 1), 89.0, "After holidays"),
            WeightEntry(LocalDate.of(2026, 3, 2), 88.1),
            WeightEntry(LocalDate.of(2026, 5, 8), 87.3),
            WeightEntry(LocalDate.of(2026, 5, 15), 86.7, "Morning weigh-in"),
        ),
        goalWeight = 65.0,
        heightCm = 165,
        lastSavedAt = "2026-05-15T13:32:27",
    )
}
