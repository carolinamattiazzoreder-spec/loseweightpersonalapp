package com.weighttracker.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateCheckerTest {

    private val release = """
        {
          "tag_name": "v1.6",
          "html_url": "https://github.com/${UpdateChecker.REPO}/releases/tag/v1.6",
          "body": "WeightTracker 1.6 for Android.\n\nDownload **WeightTracker.apk** below.",
          "assets": [
            {"name": "notes.txt", "browser_download_url": "https://example.com/notes.txt"},
            {"name": "WeightTracker.apk", "browser_download_url": "https://github.com/${UpdateChecker.REPO}/releases/download/v1.6/WeightTracker.apk"}
          ]
        }
    """.trimIndent()

    @Test
    fun comparesVersionsNumerically() {
        assertTrue(UpdateChecker.isNewer("v1.10", "1.9"))
        assertTrue(UpdateChecker.isNewer("v2.0", "1.9.9"))
        assertTrue(UpdateChecker.isNewer("1.5.1", "1.5"))
        assertFalse(UpdateChecker.isNewer("v1.5", "1.5"))
        assertFalse(UpdateChecker.isNewer("v1.4", "1.5"))
        assertFalse(UpdateChecker.isNewer("apk-latest", "1.5"))
        assertTrue(UpdateChecker.isNewer("v1.5", "dev"))
    }

    @Test
    fun findsTheApkOfANewerRelease() {
        val result = UpdateChecker.interpret(release, "1.5") as UpdateCheck.Available
        assertEquals("1.6", result.version)
        assertEquals("https://github.com/${UpdateChecker.REPO}/releases/download/v1.6/WeightTracker.apk", result.downloadUrl)
        assertEquals("https://github.com/${UpdateChecker.REPO}/releases/tag/v1.6", result.pageUrl)
        assertTrue(result.notes.startsWith("WeightTracker 1.6"))
    }

    @Test
    fun sameOrOlderReleaseIsUpToDate() {
        assertEquals(UpdateCheck.UpToDate("1.6"), UpdateChecker.interpret(release, "1.6"))
        assertEquals(UpdateCheck.UpToDate("2.0"), UpdateChecker.interpret(release, "2.0"))
    }

    @Test
    fun fallsBackToThePageWithoutAnApkAndNullNotes() {
        val json = """{"tag_name": "v9.0", "html_url": "https://example.com/v9", "body": null, "assets": []}"""
        val result = UpdateChecker.interpret(json, "1.5") as UpdateCheck.Available
        assertEquals("https://example.com/v9", result.downloadUrl)
        assertEquals("", result.notes)
    }
}
