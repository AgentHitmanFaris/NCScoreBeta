package com.noobcompany.nc_scorebeta

import java.net.URL

object SecurityUtils {
    /**
     * Validates that the URL uses a secure scheme (HTTPS).
     *
     * @param urlString The URL to check.
     * @return True if the URL is secure (HTTPS), False otherwise.
     */
    fun isSecureUrl(urlString: String): Boolean {
        return try {
            val url = URL(urlString)
            url.protocol.equals("https", ignoreCase = true)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Sanitizes a filename to ensure it only contains safe characters.
     * Prevents path traversal and file system issues.
     *
     * @param name The original filename.
     * @return A sanitized string safe for use as a filename.
     */
    fun sanitizeFilename(name: String): String {
        return name.replace(Regex("[^a-zA-Z0-9._-]"), "_")
    }
}
