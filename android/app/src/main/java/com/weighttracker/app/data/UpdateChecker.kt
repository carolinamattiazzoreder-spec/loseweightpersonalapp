package com.weighttracker.app.data

import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** Result of asking GitHub for the newest published release. */
sealed class UpdateCheck {
    data class UpToDate(val installed: String) : UpdateCheck()
    data class Available(val version: String, val downloadUrl: String, val pageUrl: String, val notes: String) : UpdateCheck()
    data class Failed(val reason: String) : UpdateCheck()
}

/** Looks up the latest GitHub release of this app (the versioned ones, e.g. v1.5). */
object UpdateChecker {
    const val REPO = "carolinamattiazzoreder-spec/loseweightpersonalapp"
    const val LATEST_API = "https://api.github.com/repos/$REPO/releases/latest"
    const val RELEASES_PAGE = "https://github.com/$REPO/releases"

    /** "v1.10" is newer than "1.9"; a tag that is not a version number is never newer. */
    fun isNewer(latestTag: String, installed: String): Boolean {
        val latest = parseVersion(latestTag) ?: return false
        val current = parseVersion(installed) ?: return true
        for (i in 0 until maxOf(latest.size, current.size)) {
            val a = latest.getOrElse(i) { 0 }
            val b = current.getOrElse(i) { 0 }
            if (a != b) return a > b
        }
        return false
    }

    private fun parseVersion(text: String): List<Int>? {
        val core = text.trim().removePrefix("v").removePrefix("V").substringBefore('-').substringBefore('+')
        if (core.isEmpty()) return null
        return core.split('.').map { it.toIntOrNull() ?: return null }
    }

    /** Interprets GitHub's "latest release" JSON against the [installed] version. */
    fun interpret(json: String, installed: String): UpdateCheck {
        val release = JSONObject(json)
        val tag = release.optString("tag_name")
        if (!isNewer(tag, installed)) return UpdateCheck.UpToDate(installed)
        val pageUrl = (release.opt("html_url") as? String)?.ifBlank { null } ?: RELEASES_PAGE
        var downloadUrl: String? = null
        val assets = release.optJSONArray("assets")
        if (assets != null) {
            for (i in 0 until assets.length()) {
                val asset = assets.optJSONObject(i) ?: continue
                if (asset.optString("name").endsWith(".apk")) {
                    downloadUrl = asset.optString("browser_download_url").ifBlank { null }
                    break
                }
            }
        }
        val notes = (release.opt("body") as? String)?.trim().orEmpty()
        return UpdateCheck.Available(tag.removePrefix("v"), downloadUrl ?: pageUrl, pageUrl, notes)
    }

    /** Blocking network call; run it on Dispatchers.IO. */
    fun fetch(installed: String): UpdateCheck = try {
        val connection = (URL(LATEST_API).openConnection() as HttpURLConnection).apply {
            connectTimeout = 10_000
            readTimeout = 10_000
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", "WeightTracker-Android")
        }
        try {
            when (val code = connection.responseCode) {
                200 -> interpret(connection.inputStream.bufferedReader().readText(), installed)
                404 -> UpdateCheck.Failed("Nenhuma versão publicada foi encontrada. Se o repositório for privado, use a página de downloads.")
                else -> UpdateCheck.Failed("O GitHub respondeu com o código $code.")
            }
        } finally {
            connection.disconnect()
        }
    } catch (e: IOException) {
        UpdateCheck.Failed("Sem conexão com o GitHub. Verifique a internet e tente de novo.")
    } catch (e: Exception) {
        UpdateCheck.Failed("Resposta inesperada do GitHub.")
    }
}
