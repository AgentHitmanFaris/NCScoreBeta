package com.noobcompany.nc_scorebeta

import java.net.URL

object SecurityUtils {
    /**
     * Validates that the URL uses a secure scheme (HTTPS) and does not point to
     * private, loopback, or link-local addresses (SSRF protection).
     *
     * @param urlString The URL to check.
     * @return True if the URL is secure (HTTPS) and public, False otherwise.
     */
    fun isSecureUrl(urlString: String): Boolean {
        return try {
            val url = URL(urlString)

            // 1. Check Protocol (Must be HTTPS)
            if (!url.protocol.equals("https", ignoreCase = true)) {
                return false
            }

            // 2. Check Host (Must not be private/local)
            val host = url.host
            if (isPrivateHost(host)) {
                return false
            }

            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Checks if a host string corresponds to a private, loopback, or link-local address.
     *
     * Note: This method attempts to detect IP addresses by string pattern to avoid
     * DNS lookups on the main thread.
     *
     * @param host The hostname or IP address string.
     * @return True if the host is identified as private/local, False otherwise.
     */
    private fun isPrivateHost(host: String): Boolean {
        // Block "localhost" explicitly
        if (host.equals("localhost", ignoreCase = true)) {
            return true
        }

        // Block IPv6 Loopback / Link-Local roughly
        // If it contains a colon, it's likely IPv6.
        // We will be conservative and block raw IPv6 addresses as the app is unlikely to need them directly.
        // Or at least check for [::1] or fe80:
        if (host.contains(":")) {
             // Basic IPv6 Loopback
             if (host == "[::1]" || host == "::1") return true
             // Link local fe80::
             if (host.startsWith("fe80", ignoreCase = true) || host.startsWith("[fe80", ignoreCase = true)) return true
        }

        // Simple Regex to check if it looks like an IPv4 address (Decimal or Octal potentially)
        // Format: d.d.d.d
        // Note: Java's URL parser generally handles dotted quad.
        // If an attacker sends "0127.0.0.1", it is treated as octal by some libraries, but Java URL?
        // Let's stick to parsing segments as integers.
        val ipv4Pattern = Regex("^(\\d{1,4})\\.(\\d{1,4})\\.(\\d{1,4})\\.(\\d{1,4})$")
        val match = ipv4Pattern.find(host)

        if (match != null) {
            val parts = match.destructured.toList()

            // Convert to Int. If any part starts with '0' and length > 1, it might be octal.
            // But standard integer parsing handles this if we are careful, or we can just treat it as decimal
            // and check ranges.
            // Wait, "012" in decimal is 12. "012" in octal is 10.
            // If the system resolves 012.0.0.1 as 10.0.0.1, we must detect that.
            // The safest bet for this regex-based validation is to reject potential octal ambiguity.
            // If any part starts with '0' and is not just "0", reject it?

            if (parts.any { it.length > 1 && it.startsWith("0") }) {
                // Potential Octal encoding - risky. Reject.
                return true
            }

            val intParts = parts.map { it.toIntOrNull() ?: return false } // if too big for Int, not valid IP anyway

            // Validate each part is a byte
            if (intParts.any { it < 0 || it > 255 }) return false

            val (b0, b1, b2, b3) = intParts

            // 10.0.0.0/8
            if (b0 == 10) return true

            // 172.16.0.0/12 (172.16 - 172.31)
            if (b0 == 172 && b1 in 16..31) return true

            // 192.168.0.0/16
            if (b0 == 192 && b1 == 168) return true

            // 127.0.0.0/8 (Loopback)
            if (b0 == 127) return true

            // 169.254.0.0/16 (Link Local)
            if (b0 == 169 && b1 == 254) return true

            // 0.0.0.0/8 (Current network)
            if (b0 == 0) return true
        }

        return false
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
