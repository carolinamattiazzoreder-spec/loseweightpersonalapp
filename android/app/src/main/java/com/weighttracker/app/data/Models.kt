package com.weighttracker.app.data

import java.time.LocalDate

data class WeightEntry(
    val date: LocalDate,
    val weight: Double,
    val note: String = "",
)

data class AppState(
    val weights: List<WeightEntry> = emptyList(),
    val goalWeight: Double? = null,
    val heightCm: Int = DEFAULT_HEIGHT_CM,
    val lastSavedAt: String? = null,
) {
    companion object {
        const val DEFAULT_HEIGHT_CM = 170
    }
}

fun round1(value: Double): Double = Math.round(value * 10.0) / 10.0

fun round2(value: Double): Double = Math.round(value * 100.0) / 100.0

/** Deduplicate by date (last write wins), round, sort ascending. */
fun normalizeWeights(entries: List<WeightEntry>): List<WeightEntry> {
    val byDate = LinkedHashMap<LocalDate, WeightEntry>()
    for (entry in entries) {
        if (!entry.weight.isFinite()) continue
        byDate[entry.date] = entry.copy(weight = round1(entry.weight), note = entry.note.trim())
    }
    return byDate.values.sortedBy { it.date }
}
