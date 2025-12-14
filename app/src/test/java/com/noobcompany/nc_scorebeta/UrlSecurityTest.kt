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
        assertFalse(SecurityUtils.isSecureUrl("http://192.168.1.1/config"))
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
