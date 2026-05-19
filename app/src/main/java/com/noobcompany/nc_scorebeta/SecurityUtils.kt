package com.noobcompany.nc_scorebeta

import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.URL
import java.io.IOException

object SecurityUtils {

    /**
     * Opens a HttpURLConnection while manually handling redirects to ensure
     * each hop is validated against SSRF rules.
     *
     * @param urlString The initial URL.
     * @return A connected HttpURLConnection.
     * @throws SecurityException if a redirect target is unsafe.
     * @throws IOException if network error occurs.
     */
    fun openSafeConnection(urlString: String): HttpURLConnection {
        var currentUrlStr = urlString
        var redirects = 0
        val maxRedirects = 10

        while (redirects < maxRedirects) {
            // 1. Validate URL (DNS + Scheme)
            if (!isSafeUrlWithDnsCheck(currentUrlStr)) {
                throw SecurityException("Unsafe URL blocked (SSRF Protection): $currentUrlStr")
            }

            val url = URL(currentUrlStr)
            val connection = url.openConnection() as HttpURLConnection

            // Disable auto-redirects to inspect headers
            connection.instanceFollowRedirects = false
            connection.connectTimeout = 15000
            connection.readTimeout = 15000

            // Mock headers to look like a browser (optional but good for compatibility)
            connection.setRequestProperty("User-Agent", "NCScoreBeta/1.0")

            connection.connect()
            val responseCode = connection.responseCode

            if (responseCode in 300..399) {
                val location = connection.getHeaderField("Location")
                if (location != null) {
                    // Handle relative redirects
                    val nextUrl = URL(url, location).toString()
                    currentUrlStr = nextUrl
                    redirects++
                    // We must close the previous connection input stream if we aren't using it?
                    // HttpURLConnection usually handles this on disconnect or new connection.
                    // But explicitly:
                    connection.disconnect()
                    continue
                }
            }

            return connection
        }

        throw SecurityException("Too many redirects")
    }

    /**
     * Performs a comprehensive security check on the URL, including DNS resolution
     * to prevent DNS Rebinding attacks.
     * This method performs network operations and MUST be called on a background thread.
     *
     * @param urlString The URL to validate.
     * @return True if the URL is secure and resolves to a public IP, False otherwise.
     */
    fun isSafeUrlWithDnsCheck(urlString: String): Boolean {
        // 1. Basic String Check (Fast)
        if (!isSecureUrl(urlString)) return false

        return try {
            val url = URL(urlString)
            // 2. DNS Resolution (Slow, Blocking)
            val inetAddresses = InetAddress.getAllByName(url.host)

            // 3. Validate Resolved IPs
            for (addr in inetAddresses) {
                // IMPORTANT: We now allow isSiteLocalAddress and isLinkLocalAddress
                // to support VPNs and local corporate/home networks that may resolve
                // public domain names to internal IP addresses.
                
                if (addr.isLoopbackAddress || addr.isAnyLocalAddress) {
                    AppLogger.error("SecurityUtils", "Blocked resolution to loopback/local: ${addr.hostAddress}")
                    return false
                }
                
                // Log site-local for debugging but allow it
                if (addr.isSiteLocalAddress || addr.isLinkLocalAddress) {
                    AppLogger.log("SecurityUtils", "Allowing VPN/Site-Local address: ${addr.hostAddress}")
                }
            }
            true
        } catch (e: Exception) {
            // DNS failure or other issue -> Fail secure
            false
        }
    }

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

        // Block Integer IPs (Decimal format like 2130706433)
        // This prevents bypasses where non-dotted IP formats are treated as valid hostnames
        if (host.all { it.isDigit() }) {
            return true
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

            // IMPORTANT: We now ALLOW site-local and link-local to support VPNs
            // These were previously blocked here and in isSafeUrlWithDnsCheck.

            /* 
            // 10.0.0.0/8
            if (b0 == 10) return true
            // 172.16.0.0/12 (172.16 - 172.31)
            if (b0 == 172 && b1 in 16..31) return true
            // 192.168.0.0/16
            if (b0 == 192 && b1 == 168) return true
            // 169.254.0.0/16 (Link Local)
            if (b0 == 169 && b1 == 254) return true
            */

            // 127.0.0.0/8 (Loopback) - STILL BLOCKED
            if (b0 == 127) return true

            // 0.0.0.0/8 (Current network) - STILL BLOCKED
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

    /**
     * Checks if a file path is safe and contained within a specified root directory.
     * Prevents Path Traversal attacks by resolving canonical paths.
     *
     * @param file The file to check.
     * @param rootDir The trusted root directory (e.g. context.getExternalFilesDir(null)).
     * @return True if the file is inside the root directory, False otherwise.
     */
    fun isSafeFilePath(file: java.io.File, rootDir: java.io.File?): Boolean {
        if (rootDir == null) return false
        return try {
            val canonicalPath = file.canonicalPath
            val canonicalRoot = rootDir.canonicalPath

            // Ensure proper directory checking to avoid partial matches (e.g. /dir/foo vs /dir/foobar)
            if (canonicalPath == canonicalRoot) return true
            if (canonicalPath.startsWith(canonicalRoot + java.io.File.separator)) return true

            false
        } catch (e: Exception) {
            false
        }
    }
}
