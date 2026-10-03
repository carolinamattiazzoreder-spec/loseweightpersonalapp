package com.weighttracker.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class StateJsonTest {

    /** Same shape as the Streamlit app's JSON export / weight_data.json. */
    private val streamlitBackup = """
        {
          "weights": [
            {"date": "2025-12-01", "weight": 90.0, "note": ""},
            {"date": "2025-09-01", "weight": 95, "note": "start"},
            {"date": "2025-12-01", "weight": "89.96", "note": " dup "},
            {"date": "not-a-date", "weight": 80.0, "note": ""},
            {"date": "2026-01-01", "weight": null, "note": ""}
          ],
          "goal_weight": 65.0,
          "height_cm": 165,
          "last_saved_at": "2026-05-15T13:32:27",
          "last_backup_at": "2026-05-15T13:28:23"
        }
    """.trimIndent()

    @Test
    fun decodesStreamlitBackup() {
        val state = StateJson.decode(streamlitBackup)
        assertEquals(
            listOf(
                WeightEntry(LocalDate.of(2025, 9, 1), 95.0, "start"),
                WeightEntry(LocalDate.of(2025, 12, 1), 90.0, "dup"),
            ),
            state.weights,
        )
        assertEquals(65.0, state.goalWeight!!, 0.0)
        assertEquals(165, state.heightCm)
        assertEquals("2026-05-15T13:32:27", state.lastSavedAt)
    }

    @Test
    fun missingAndNullFieldsUseDefaults() {
        val state = StateJson.decode("""{"goal_weight": null, "last_saved_at": null}""")
        assertEquals(emptyList<WeightEntry>(), state.weights)
        assertNull(state.goalWeight)
        assertEquals(AppState.DEFAULT_HEIGHT_CM, state.heightCm)
        assertNull(state.lastSavedAt)
    }

    @Test
    fun roundTrips() {
        val original = AppState(
            weights = listOf(WeightEntry(LocalDate.of(2026, 5, 8), 87.3, "after \"gym\", tired")),
            goalWeight = 65.0,
            heightCm = 165,
            lastSavedAt = "2026-05-15T13:32:27",
        )
        assertEquals(original, StateJson.decode(StateJson.encode(original)))
    }

    @Test
    fun csvEscapesNotes() {
        val state = AppState(weights = listOf(WeightEntry(LocalDate.of(2026, 5, 8), 87.3, "a, \"b\"")))
        assertEquals("date,weight,note\n2026-05-08,87.3,\"a, \"\"b\"\"\"\n", StateCsv.encode(state))
    }
}
