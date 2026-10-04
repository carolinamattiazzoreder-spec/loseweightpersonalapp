package com.weighttracker.app.ui.preview

import com.weighttracker.app.data.AppState
import com.weighttracker.app.data.WeightEntry
import java.time.LocalDate

/** Example data (same story as the canvas design) for @Preview and screenshot tests. */
object SampleData {
    val today: LocalDate = LocalDate.of(2026, 10, 4)

    private val points = listOf(
        "2026-08-12" to 85.0, "2026-08-16" to 84.4, "2026-08-20" to 84.1, "2026-08-24" to 83.2,
        "2026-08-28" to 83.0, "2026-09-01" to 82.3, "2026-09-03" to 82.1, "2026-09-06" to 81.6,
        "2026-09-09" to 81.4, "2026-09-12" to 80.9, "2026-09-15" to 80.6, "2026-09-17" to 80.2,
        "2026-09-20" to 80.0, "2026-09-23" to 79.6, "2026-09-26" to 79.1, "2026-09-28" to 79.3,
        "2026-10-01" to 79.0, "2026-10-03" to 78.7, "2026-10-04" to 78.4,
    )
    private val notes = mapOf(
        "2026-09-26" to "Pesagem matinal",
        "2026-09-28" to "Depois do fim de semana",
        "2026-10-01" to "Após treino",
        "2026-10-04" to "Pesagem matinal",
    )

    val state = AppState(
        weights = points.map { (d, w) -> WeightEntry(LocalDate.parse(d), w, notes[d] ?: "") },
        goalWeight = 72.0,
        heightCm = 170,
        lastSavedAt = "2026-10-04T07:12:00",
    )
}
