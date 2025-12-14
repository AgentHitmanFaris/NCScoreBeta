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
}
