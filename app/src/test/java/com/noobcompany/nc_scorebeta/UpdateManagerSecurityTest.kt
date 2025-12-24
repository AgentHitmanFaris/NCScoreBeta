package com.noobcompany.nc_scorebeta

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.URL

class UpdateManagerSecurityTest {

    // Since I cannot mock UpdateManager's internal async tasks easily without Robolectric,
    // I will verify the logic that was injected into UpdateManager by running it here.

    // Logic 1: LATEST_RELEASE_URL Validation
    @Test
    fun `LATEST_RELEASE_URL is secure`() {
        val url = "https://api.github.com/repos/AgentHitmanFaris/NCScoreBeta/releases/latest"
        assertTrue(SecurityUtils.isSecureUrl(url))
    }

    // Logic 2: Download URL Validation (Simulated)
    @Test
    fun `Download URL validation logic`() {
        // Safe URL
        val safeUrl = "https://objects.githubusercontent.com/github-production-release-asset-2e65be/..."
        assertTrue(SecurityUtils.isSecureUrl(safeUrl))

        // Unsafe URLs
        val unsafeHttp = "http://malicious.com/update.apk"
        assertFalse(SecurityUtils.isSecureUrl(unsafeHttp))

        val unsafeIp = "https://192.168.1.1/update.apk"
        assertFalse(SecurityUtils.isSecureUrl(unsafeIp))
    }

    // Logic 3: Version Comparison Logic (Extracted from UpdateManager for verification)
    @Test
    fun `Version comparison works correctly`() {
        // Now calling the actual public method on UpdateManager
        assertTrue(UpdateManager.isNewerVersion("v1.2.0", "v1.1.0"))
        assertTrue(UpdateManager.isNewerVersion("1.2.0", "1.1.9"))
        assertTrue(UpdateManager.isNewerVersion("2.0.0", "1.9.9"))

        assertFalse(UpdateManager.isNewerVersion("v1.0.0", "v1.0.0"))
        assertFalse(UpdateManager.isNewerVersion("v0.9.0", "v1.0.0"))

        // Edge cases
        assertTrue(UpdateManager.isNewerVersion("v1.0.1", "v1.0")) // More specific
        assertFalse(UpdateManager.isNewerVersion("v1.0", "v1.0.1"))
    }
}
