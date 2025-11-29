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
 * Activity for viewing PDF files (sheet music).
 *
 * This activity handles the retrieval of a PDF URL from the intent, converting it to a direct download link if necessary
 * (e.g., for Google Drive links), downloading the PDF, and displaying it using the [PDFView] library.
 */
class PdfViewerActivity : AppCompatActivity() {

    /**
     * Called when the activity is first created.
     *
     * It retrieves the "PDF_URL" extra from the intent. If present, it initiates the download and display process.
     * It also sets up the UI components and handles potential errors during the process.
     *
     * @param savedInstanceState If non-null, this activity is being re-constructed from a previous saved state.
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
        
        progressBar.visibility = View.VISIBLE

        lifecycleScope.launch(Dispatchers.IO) {
            try {
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
     * Sets up tap navigation zones for changing pages within the PDF view.
     *
     * Configures click listeners on invisible overlay views:
     * - Tapping the left side navigates to the previous page.
     * - Tapping the right side navigates to the next page.
     *
     * @param pdfView The [PDFView] instance to control.
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
     * Converts a raw URL into a direct download link.
     *
     * This method is specifically designed to handle Google Drive sharing links. It extracts the file ID
     * and constructs a URL that triggers a direct download (`export=download`).
     *
     * @param url The original URL string.
     * @return The converted direct download URL, or the original URL if no conversion was applicable.
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
