package com.noobcompany.nc_scorebeta

import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Singleton object that handles the logic for opening songs.
 *
 * It manages the flow of verifying premium access and fetching the appropriate PDF link.
 */
object SongHandler {

    /**
     * Handles a click event on a song.
     *
     * Opens the song detail view.
     *
     * @param context The application context.
     * @param song The song object that was clicked.
     */
    fun onSongClicked(context: Context, song: Song) {
        // NEW FLOW: Open Detail Fragment first
        if (context is MainActivity) {
            context.openSongDetail(song)
        } else {
            // Fallback for non-MainActivity contexts (shouldn't happen often in this app structure)
            Toast.makeText(context, "Opening Details...", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * Opens the score for the given song.
     *
     * If the song is premium, it checks for access. Otherwise, it fetches and opens the PDF.
     * This method is renamed from onSongClicked to openScore for direct access from Detail Page.
     *
     * @param context The application context.
     * @param song The song to open.
     */
    fun openScore(context: Context, song: Song) {
        if (song.isPremium) {
            checkPremiumAccess(context, song)
        } else {
            fetchAndOpenPdf(context, song)
        }
    }

    /**
     * Checks if the current user has access to premium content.
     *
     * Requires the user to be logged in. It verifies the user's existence in Firestore.
     *
     * @param context The application context.
     * @param song The premium song to access.
     */
    private fun checkPremiumAccess(context: Context, song: Song) {
        val user = FirebaseAuth.getInstance().currentUser

        if (user == null) {
            Toast.makeText(context, "Premium Score: Please Log In", Toast.LENGTH_SHORT).show()
            val intent = Intent(context, LoginActivity::class.java)
            context.startActivity(intent)
        } else {
            // Verify User in Firestore
            val db = FirebaseFirestore.getInstance()
            db.collection("users").document(user.uid).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        // Optional: Check for specific "isPremiumUser" field if needed
                        // val isPremium = document.getBoolean("isPremiumUser") ?: false
                        // if (isPremium) ... else ...

                        // For now, just being logged in and having a profile is enough
                        fetchPremiumPdf(context, song)
                    } else {
                        Toast.makeText(context, "User profile not found.", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(context, "Error verifying account.", Toast.LENGTH_SHORT).show()
                }
        }
    }

    /**
     * Fetches the PDF link for a premium song.
     *
     * Premium songs have their content stored in a nested "secure" subcollection.
     *
     * @param context The application context.
     * @param song The premium song.
     */
    @Suppress("UNCHECKED_CAST")
    private fun fetchPremiumPdf(context: Context, song: Song) {
        Toast.makeText(context, "Verifying Premium Access...", Toast.LENGTH_SHORT).show()

        // Step 1: Get the arrangement ID first
        FirebaseFirestore.getInstance()
            .collection("songs").document(song.id).collection("arrangements")
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    // For now, we take the first arrangement found
                    val arrangementDoc = documents.documents[0]
                    val arrangementId = arrangementDoc.id
                    
                    // Step 2: Fetch the secure link from the nested subcollection
                    // Path: songs/{songId}/arrangements/{arrId}/secure/content
                    fetchNestedSecureLink(context, song.id, arrangementId)
                } else {
                    Toast.makeText(context, "No arrangements found for this song.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(context, "Error finding arrangements.", Toast.LENGTH_SHORT).show()
            }
    }

    /**
     * Fetches the actual download link from the secure subcollection.
     *
     * @param context The application context.
     * @param songId The ID of the song.
     * @param arrangementId The ID of the arrangement.
     */
    private fun fetchNestedSecureLink(context: Context, songId: String, arrangementId: String) {
        FirebaseFirestore.getInstance()
            .collection("songs").document(songId)
            .collection("arrangements").document(arrangementId)
            .collection("secure").document("content")
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    // Debugging
                    android.util.Log.d("SongHandler", "Secure Doc Data: ${document.data}")

                    var pdfUrl = document.getString("downloadLink")

                    // Fallback checks
                    if (pdfUrl.isNullOrEmpty()) {
                         pdfUrl = document.getString("link")
                    }
                    if (pdfUrl.isNullOrEmpty()) {
                        pdfUrl = document.getString("url")
                    }

                    if (!pdfUrl.isNullOrEmpty()) {
                        openPdfViewer(context, pdfUrl!!)
                    } else {
                        Toast.makeText(context, "Error: Premium link empty in database.", Toast.LENGTH_LONG).show()
                    }
                } else {
                    Toast.makeText(context, "Premium content not found (404).", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                android.util.Log.e("SongHandler", "Access Denied", e)
                Toast.makeText(context, "Access Denied. Ensure you are logged in.", Toast.LENGTH_LONG).show()
            }
    }

    /**
     * Fetches and opens the PDF for a standard (non-premium) song.
     *
     * @param context The application context.
     * @param song The song to open.
     */
    private fun fetchAndOpenPdf(context: Context, song: Song) {
        Toast.makeText(context, "Opening ${song.title}...", Toast.LENGTH_SHORT).show()

        val prefs = context.getSharedPreferences("nc_prefs", Context.MODE_PRIVATE)
        val isOfflineEnabled = prefs.getBoolean("offline_mode", false)

        if (isOfflineEnabled) {
            // Try to open from local storage first
            val localFile = java.io.File(context.getExternalFilesDir("scores"), "${song.id}.pdf")
            if (localFile.exists()) {
                // Open Local
                android.util.Log.d("SongHandler", "Opening local file: ${localFile.absolutePath}")
                val intent = Intent(context, PdfViewerActivity::class.java)
                intent.putExtra("PDF_FILE", localFile.absolutePath) // Pass file path instead of URL
                context.startActivity(intent)
                return
            }
        }

        // If not offline or file doesn't exist, fetch URL
        FirebaseFirestore.getInstance()
            .collection("songs").document(song.id).collection("arrangements")
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val arrangement = documents.documents[0].toObject(Arrangement::class.java)
                    val pdfUrl = arrangement?.downloadLink ?: ""

                    if (pdfUrl.isNotEmpty()) {
                        if (isOfflineEnabled) {
                            downloadAndOpenPdf(context, song.id, pdfUrl)
                        } else {
                            openPdfViewer(context, pdfUrl)
                        }
                    } else {
                        Toast.makeText(context, "No PDF link found", Toast.LENGTH_LONG).show()
                    }
                } else {
                    Toast.makeText(context, "No arrangements found", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(context, "Error fetching PDF", Toast.LENGTH_SHORT).show()
            }
    }

    /**
     * Launches the [PdfViewerActivity] with the given URL.
     *
     * @param context The application context.
     * @param url The URL of the PDF file.
     */
    private fun openPdfViewer(context: Context, url: String) {
        val intent = Intent(context, PdfViewerActivity::class.java)
        intent.putExtra("PDF_URL", url)
        context.startActivity(intent)
    }

    /**
     * Downloads the PDF to local storage and then opens it.
     *
     * @param context The application context.
     * @param songId The ID of the song.
     * @param url The URL to download.
     */
    private fun downloadAndOpenPdf(context: Context, songId: String, url: String) {
        Toast.makeText(context, "Downloading for offline use...", Toast.LENGTH_SHORT).show()
        
        // Simple download using DownloadManager or Thread (Thread is easier for immediate open)
        // Ideally use DownloadManager, but for 'Open Now' we want a callback.
        // We'll use a simple thread to download to a temp file then move it.
        
        kotlin.concurrent.thread {
            try {
                val destFile = java.io.File(context.getExternalFilesDir("scores"), "$songId.pdf")
                val parent = destFile.parentFile
                if (parent != null && !parent.exists()) parent.mkdirs()

                val u = java.net.URL(url)
                val conn = u.openConnection()
                conn.connect()
                val input = java.io.BufferedInputStream(u.openStream())
                val output = java.io.FileOutputStream(destFile)

                val data = ByteArray(1024)
                var count: Int
                while (input.read(data).also { count = it } != -1) {
                    output.write(data, 0, count)
                }

                output.flush()
                output.close()
                input.close()

                // Open on Main Thread
                (context as? android.app.Activity)?.runOnUiThread {
                     val intent = Intent(context, PdfViewerActivity::class.java)
                     intent.putExtra("PDF_FILE", destFile.absolutePath)
                     context.startActivity(intent)
                }

            } catch (e: Exception) {
                android.util.Log.e("SongHandler", "Download Error", e)
                (context as? android.app.Activity)?.runOnUiThread {
                    Toast.makeText(context, "Download failed, streaming instead...", Toast.LENGTH_SHORT).show()
                    openPdfViewer(context, url)
                }
            }
        }
    }
}
