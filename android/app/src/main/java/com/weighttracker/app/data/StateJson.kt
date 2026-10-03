package com.weighttracker.app.data

import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.format.DateTimeParseException

/**
 * JSON format shared with the Streamlit app's "Download backup (JSON)" export,
 * so existing backups can be imported and exports can go back to the web app.
 */
object StateJson {

    fun encode(state: AppState): String {
        val weights = JSONArray()
        for (entry in state.weights) {
            weights.put(
                JSONObject()
                    .put("date", entry.date.toString())
                    .put("weight", entry.weight)
                    .put("note", entry.note)
            )
        }
        return JSONObject()
            .put("weights", weights)
            .put("goal_weight", state.goalWeight ?: JSONObject.NULL)
            .put("height_cm", state.heightCm)
            .put("last_saved_at", state.lastSavedAt ?: JSONObject.NULL)
            .toString(2)
    }

    /** Lenient decode, mirroring ensure_keys/normalize_weights in streamlit_app.py. */
    fun decode(text: String): AppState {
        val root = JSONObject(text)
        val entries = mutableListOf<WeightEntry>()
        val array = root.optJSONArray("weights")
        if (array != null) {
            for (i in 0 until array.length()) {
                val item = array.optJSONObject(i) ?: continue
                val date = parseDate(item.opt("date")) ?: continue
                val weight = parseDouble(item.opt("weight")) ?: continue
                entries += WeightEntry(date, weight, parseString(item.opt("note")))
            }
        }
        return AppState(
            weights = normalizeWeights(entries),
            goalWeight = parseDouble(root.opt("goal_weight"))?.let(::round1),
            heightCm = parseDouble(root.opt("height_cm"))?.toInt()?.takeIf { it > 0 }
                ?: AppState.DEFAULT_HEIGHT_CM,
            lastSavedAt = parseString(root.opt("last_saved_at")).ifBlank { null },
        )
    }

    private fun parseString(value: Any?): String = when (value) {
        is String -> value.trim()
        is Number -> value.toString()
        else -> ""
    }

    private fun parseDouble(value: Any?): Double? = when (value) {
        is Number -> value.toDouble()
        is String -> value.trim().toDoubleOrNull()
        else -> null
    }?.takeIf { it.isFinite() }

    private fun parseDate(value: Any?): LocalDate? {
        val text = (value as? String)?.trim() ?: return null
        return try {
            LocalDate.parse(text)
        } catch (e: DateTimeParseException) {
            null
        }
    }
}

object StateCsv {
    fun encode(state: AppState): String = buildString {
        append("date,weight,note\n")
        for (entry in state.weights) {
            append(entry.date).append(',').append(entry.weight).append(',')
                .append(escape(entry.note)).append('\n')
        }
    }

    private fun escape(value: String): String =
        if (value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }
}
