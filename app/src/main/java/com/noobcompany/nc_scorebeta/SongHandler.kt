package com.noobcompany.nc_scorebeta

import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Singleton object that serves as the central manager for handling song interactions.
 *
 * This utility class encapsulates the logic for opening song details, verifying premium access permissions,
 * and managing the retrieval of sheet music (PDFs) from either local storage (offline mode) or remote Firestore URLs.
 */
object SongHandler {

    /**
     * Handles the primary user interaction when a song item is clicked in the UI.
     *
     * It directs the application to open the detailed view of the selected song.
     * If the context is [MainActivity], it delegates the navigation to it; otherwise, it shows a toast.
     *
     * @param context The [Context] in which the click occurred, typically the [MainActivity].
     * @param song The [Song] data object representing the clicked item.
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
     * Initiates the process of opening the sheet music (PDF) for a specific song.
     *
     * This method contains the business logic for:
     * 1. Checking if the song requires premium access.
     * 2. If premium, verifying the user's subscription or login status.
     * 3. If standard (or verified premium), fetching the PDF URL or local file path.
     *
     * @param context The application [Context].
     * @param song The [Song] object to be opened.
     */
    fun openScore(context: Context, song: Song) {
        if (song.isPremium) {
            checkPremiumAccess(context, song)
        } else {
            fetchAndOpenPdf(context, song)
        }
    }

    /**
     * Internal helper to verify if the current user is authorized to view premium content.
     *
     * Checks if a user is logged in via Firebase Auth and if their profile exists in the 'users' collection.
     * If unauthorized, it redirects the user to the [LoginActivity].
     *
     * @param context The application [Context].
     * @param song The premium [Song] attempting to be accessed.
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
     * Fetches the secure download link for a verified premium song.
     *
     * Premium songs have their content stored in a nested "secure" subcollection to prevent public access.
     * This method first looks up the arrangement ID and then queries the secure subcollection.
     *
     * @param context The application [Context].
     * @param song The premium [Song] object.
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
     * Retrieves the actual PDF URL from the nested 'secure' document in Firestore.
     *
     * @param context The application [Context].
     * @param songId The unique ID of the song.
     * @param arrangementId The unique ID of the arrangement.
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
     * Fetches and opens the PDF for a standard (public) song.
     *
     * This method respects the "Offline Mode" preference. If enabled, it attempts to load the file
     * from local storage. If not found or offline mode is disabled, it fetches the URL from the 'arrangements' subcollection.
     *
     * @param context The application [Context].
     * @param song The [Song] object.
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
     * Helper to launch the [PdfViewerActivity] with a remote URL.
     *
     * @param context The application [Context].
     * @param url The web URL of the PDF file.
     */
    private fun openPdfViewer(context: Context, url: String) {
        val intent = Intent(context, PdfViewerActivity::class.java)
        intent.putExtra("PDF_URL", url)
        context.startActivity(intent)
    }

    /**
     * Downloads the PDF file to local storage and then opens it in the viewer.
     *
     * This ensures the file is available for future offline access.
     *
     * @param context The application [Context].
     * @param songId The ID of the song (used as the filename).
     * @param url The URL of the PDF to download.
     */
    private fun downloadAndOpenPdf(context: Context, songId: String, url: String) {
        Toast.makeText(context, "Downloading for offline use...", Toast.LENGTH_SHORT).show()
        
        // Simple download using DownloadManager or Thread (Thread is easier for immediate open)
        // Ideally use DownloadManager, but for 'Open Now' we want a callback.
        // We'll use a simple thread to download to a temp file then move it.
        val contextRef = java.lang.ref.WeakReference(context)

        kotlin.concurrent.thread {
            try {
                // Ensure context is still valid for file path access, or use application context if possible
                // However, getExternalFilesDir depends on Context.
                // We grab it early if possible or safely unwrap.
                val ctxForPath = contextRef.get() ?: return@thread
                val destFile = java.io.File(ctxForPath.getExternalFilesDir("scores"), "$songId.pdf")

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
                val ctx = contextRef.get()
                if (ctx != null) {
                    (ctx as? android.app.Activity)?.runOnUiThread {
                         val intent = Intent(ctx, PdfViewerActivity::class.java)
                         intent.putExtra("PDF_FILE", destFile.absolutePath)
                         ctx.startActivity(intent)
                    }
                }

            } catch (e: Exception) {
                android.util.Log.e("SongHandler", "Download Error", e)
                val ctx = contextRef.get()
                if (ctx != null) {
                    (ctx as? android.app.Activity)?.runOnUiThread {
                        Toast.makeText(ctx, "Download failed, streaming instead...", Toast.LENGTH_SHORT).show()
                        openPdfViewer(ctx, url)
                    }
                }
            }
        }
    }
}
