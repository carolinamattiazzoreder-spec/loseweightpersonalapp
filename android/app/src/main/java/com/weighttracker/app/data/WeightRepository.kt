package com.weighttracker.app.data

import android.content.Context
import android.util.AtomicFile
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

/** Stores the app state as JSON in the app's private storage. */
class WeightRepository(context: Context) {

    private val dir = context.filesDir
    private val file = AtomicFile(File(dir, FILE_NAME))

    fun load(): AppState {
        val text = try {
            file.readFully().decodeToString()
        } catch (e: FileNotFoundException) {
            return AppState()
        }
        return try {
            StateJson.decode(text)
        } catch (e: Exception) {
            // Keep the unreadable file aside instead of overwriting it on the next save.
            File(dir, "weight_data.corrupt-${System.currentTimeMillis()}.json").writeText(text)
            AppState()
        }
    }

    fun save(state: AppState): AppState {
        val stamped = state.copy(
            weights = normalizeWeights(state.weights),
            lastSavedAt = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS).toString(),
        )
        val out = file.startWrite()
        try {
            out.write(StateJson.encode(stamped).encodeToByteArray())
            file.finishWrite(out)
        } catch (e: IOException) {
            file.failWrite(out)
            throw e
        }
        return stamped
    }

    companion object {
        const val FILE_NAME = "weight_data.json"
    }
}
