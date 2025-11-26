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

class PdfViewerActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pdf_viewer)

        val rawPdfUrl = intent.getStringExtra("PDF_URL")

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

        val pdfView = findViewById<PDFView>(R.id.pdfView)
        val progressBar = findViewById<View>(R.id.progressBar)

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

    // --- HELPER FUNCTION: FIX GOOGLE DRIVE LINKS ---
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