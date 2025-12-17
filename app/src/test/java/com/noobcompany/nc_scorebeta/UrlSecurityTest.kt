package com.noobcompany.nc_scorebeta

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UrlSecurityTest {

    @Test
    fun `isSecureUrl accepts https`() {
        assertTrue(SecurityUtils.isSecureUrl("https://example.com/file.pdf"))
        assertTrue(SecurityUtils.isSecureUrl("https://drive.google.com/uc?id=123"))
    }

    @Test
    fun `isSecureUrl rejects http`() {
        assertFalse(SecurityUtils.isSecureUrl("http://example.com/file.pdf"))
    }

    @Test
    fun `isSecureUrl rejects private ips`() {
        assertFalse(SecurityUtils.isSecureUrl("https://192.168.1.1/config"))
        assertFalse(SecurityUtils.isSecureUrl("https://10.0.0.5/data"))
        assertFalse(SecurityUtils.isSecureUrl("https://172.16.0.1/admin"))
        assertFalse(SecurityUtils.isSecureUrl("https://127.0.0.1/local"))
        assertFalse(SecurityUtils.isSecureUrl("https://localhost/api"))
        assertFalse(SecurityUtils.isSecureUrl("https://169.254.1.1/meta"))
    }

    @Test
    fun `isSecureUrl rejects ambiguous ips`() {
        assertFalse(SecurityUtils.isSecureUrl("https://0127.0.0.1")) // Octal
        assertFalse(SecurityUtils.isSecureUrl("https://[::1]")) // IPv6 Loopback
        assertFalse(SecurityUtils.isSecureUrl("https://[fe80::1]")) // IPv6 Link Local
    }

    @Test
    fun `isSecureUrl rejects ftp`() {
        assertFalse(SecurityUtils.isSecureUrl("ftp://example.com/file.pdf"))
    }

    @Test
    fun `isSecureUrl rejects file`() {
        // While PdfViewerActivity handles local files via PDF_FILE intent extra,
        // any file URI passed to the network loader should be rejected to prevent
        // potential logic errors or weird state, as the network loader casts to HttpURLConnection.
        assertFalse(SecurityUtils.isSecureUrl("file:///etc/hosts"))
    }

    @Test
    fun `isSecureUrl rejects invalid urls`() {
        assertFalse(SecurityUtils.isSecureUrl("not a url"))
        assertFalse(SecurityUtils.isSecureUrl(""))
    }
}
