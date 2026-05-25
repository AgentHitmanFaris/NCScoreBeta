package com.noobcompany.nc_scorebeta

import org.junit.Assert.assertEquals
import org.junit.Test

class FilenameSanitizationTest {

    @Test
    fun `sanitizeFilename allows alphanumeric characters`() {
        assertEquals("Song123", SecurityUtils.sanitizeFilename("Song123"))
    }

    @Test
    fun `sanitizeFilename allows dots, underscores, and hyphens`() {
        assertEquals("song_version-1.0.pdf", SecurityUtils.sanitizeFilename("song_version-1.0.pdf"))
    }

    @Test
    fun `sanitizeFilename replaces spaces with underscore`() {
        assertEquals("My_Song_Title", SecurityUtils.sanitizeFilename("My Song Title"))
    }

    @Test
    fun `sanitizeFilename replaces slashes with underscore`() {
        assertEquals(".._.._secrets", SecurityUtils.sanitizeFilename("../../secrets"))
    }

    @Test
    fun `sanitizeFilename replaces special characters`() {
        assertEquals("Cool_Song_", SecurityUtils.sanitizeFilename("Cool\$Song!"))
    }
}
