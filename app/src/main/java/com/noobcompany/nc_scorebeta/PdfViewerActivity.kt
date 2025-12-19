package com.noobcompany.nc_scorebeta

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.github.barteksc.pdfviewer.PDFView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.regex.Pattern

/**
 * Activity for rendering PDF documents (Sheet Music).
 *
 * This activity acts as the core viewer for the application. It is capable of loading PDF files
 * from remote URLs (including handling Google Drive shared links) and from local file paths
 * (for offline viewing). It integrates the `AndroidPdfViewer` library for rendering.
 */
class PdfViewerActivity : AppCompatActivity() {

    /**
     * Called when the activity is first created.
     *
     * Logic flow:
     * 1. Check for a local file path in the intent extras ("PDF_FILE").
     *    - If present, load the file directly.
     * 2. If no local file, check for a remote URL in intent extras ("PDF_URL").
     *    - If present, attempt to convert the URL (e.g., if it's a Drive link).
     *    - Download/Stream the content in a background coroutine.
     *    - Render the stream into the PDFView on the main thread.
     *
     * @param savedInstanceState Bundle with saved state.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pdf_viewer)

        val rawPdfUrl = intent.getStringExtra("PDF_URL")
        val localFilePath = intent.getStringExtra("PDF_FILE") // NEW: Check for file path
        
        val pdfView = findViewById<PDFView>(R.id.pdfView)
        val progressBar = findViewById<View>(R.id.progressBar)
        
        // 1. LOCAL FILE HANDLING (OFFLINE MODE)
        if (!localFilePath.isNullOrEmpty()) {
            val file = java.io.File(localFilePath)
            if (file.exists()) {
                Log.d("PdfViewer", "Loading from file: $localFilePath")
                pdfView.fromFile(file)
                    .swipeHorizontal(true)
                    .enableSwipe(false)
                    .pageSnap(true)
                    .autoSpacing(true)
                    .pageFling(true)
                    .fitEachPage(true)
                    .enableDoubletap(true)
                    .onLoad { nbPages ->
                        progressBar.visibility = View.GONE
                        Toast.makeText(this, "Score Loaded (Offline)", Toast.LENGTH_SHORT).show()
                        setupTapNavigation(pdfView)
                    }
                    .onError { t ->
                        progressBar.visibility = View.GONE
                        Toast.makeText(this, "Error loading file: ${t.message}", Toast.LENGTH_LONG).show()
                    }
                    .load()
                return // Exit early, job done
            }
        }

        if (rawPdfUrl.isNullOrEmpty()) {
            Toast.makeText(this, "No PDF Link Found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // 1. CONVERT THE LINK AUTOMATICALLY
        // This fixes the "Preview" vs "Download" issue
        val directUrl = getDirectUrl(rawPdfUrl)
        Log.d("PdfViewer", "Original: $rawPdfUrl")
        Log.d("PdfViewer", "Direct: $directUrl")
        
        if (!SecurityUtils.isSecureUrl(directUrl)) {
            Toast.makeText(this, "Security Error: Only HTTPS URLs are allowed.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        progressBar.visibility = View.VISIBLE

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // ADDED SECURITY CHECK: DNS Resolution to prevent Rebinding
                if (!SecurityUtils.isSafeUrlWithDnsCheck(directUrl)) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@PdfViewerActivity, "Security Error: DNS check failed.", Toast.LENGTH_LONG).show()
                        progressBar.visibility = View.GONE
                        finish()
                    }
                    return@launch
                }

                // 2. Download from the NEW Direct URL
                val url = URL(directUrl)
                val urlConnection = url.openConnection() as HttpURLConnection

                // IMPORTANT: Handle Google Drive Redirects (302/303)
                urlConnection.instanceFollowRedirects = true

                if (urlConnection.responseCode == 200) {
                    val inputStream = BufferedInputStream(urlConnection.inputStream)

                    // Pre-read stream logic if needed or pass directly
                    // For PDFView, passing stream directly on Main thread is okay if the stream is ready?
                    // Actually pdfView.fromStream reads in background usually, but we need to pass the stream.
                    // Since we opened the stream in IO, we need to be careful.
                    // But wait, pdfView.fromStream(...).load() handles its own async loading usually?
                    // Let's check the library. AndroidPdfViewer usually takes a stream.
                    // It loads the PDF in background.
                    
                    withContext(Dispatchers.Main) {
                        pdfView.fromStream(inputStream)
                            .swipeHorizontal(true) // Horizontal Layout ensures centering
                            .enableSwipe(false)    // Disable Swipe Gesture (Tap only)
                            .pageSnap(true)
                            .autoSpacing(true)     // Re-enable for nice spacing in horizontal
                            .pageFling(true)
                            .fitEachPage(true)     // Force Fit Center
                            .enableDoubletap(true)
                            
                            // 3. LISTENERS: THIS TELLS YOU IF IT WORKED
                            .onLoad { nbPages ->
                                // Success! Hide loading bar
                                progressBar.visibility = View.GONE
                                Toast.makeText(this@PdfViewerActivity, "Score Loaded: $nbPages pages", Toast.LENGTH_SHORT).show()
                                setupTapNavigation(pdfView)
                            }
                            .onError { t ->
                                // Failure! Show error
                                progressBar.visibility = View.GONE
                                Toast.makeText(this@PdfViewerActivity, "Error parsing PDF: ${t.message}", Toast.LENGTH_LONG).show()
                                Log.e("PdfViewer", "PDF Load Error", t)
                            }
                            .load()
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@PdfViewerActivity, "Server Error: ${urlConnection.responseCode}", Toast.LENGTH_SHORT).show()
                        progressBar.visibility = View.GONE
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@PdfViewerActivity, "Connection Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    progressBar.visibility = View.GONE
                }
            }
        }
    }

    /**
     * Sets up invisible tap zones on the screen edges for page navigation.
     *
     * This replaces standard swipe gestures to prevent accidental page turns while playing.
     * - Tapping left moves to previous page.
     * - Tapping right moves to next page.
     *
     * @param pdfView The PDFView instance to control.
     */
    private fun setupTapNavigation(pdfView: PDFView) {
        findViewById<View>(R.id.viewTapLeft).setOnClickListener {
            val current = pdfView.currentPage
            if (current > 0) {
                pdfView.jumpTo(current - 1)
            }
        }

        findViewById<View>(R.id.viewTapRight).setOnClickListener {
            val current = pdfView.currentPage
            if (current < pdfView.pageCount - 1) {
                pdfView.jumpTo(current + 1)
            }
        }
    }

    /**
     * Utility to convert a cloud storage sharing link into a direct download link.
     *
     * Specifically handles Google Drive "view" links by extracting the file ID
     * and constructing a "uc?export=download" URL.
     *
     * @param url The input URL.
     * @return The direct download URL, or original if no conversion logic applies.
     */
    private fun getDirectUrl(url: String): String {
        // If it's a Google Drive link, we need to extract the ID and make it a download link
        if (url.contains("drive.google.com") || url.contains("docs.google.com")) {
            // Regex to find the ID between "/d/" and "/"
            val pattern = Pattern.compile("/d/([^/]+)")
            val matcher = pattern.matcher(url)

            if (matcher.find()) {
                val fileId = matcher.group(1)
                // Return the clean download format
                return "https://drive.google.com/uc?id=$fileId&export=download"
            }
        }
        // If it's not a Google Drive link (or we couldn't find ID), return original
        return url
    }

}
