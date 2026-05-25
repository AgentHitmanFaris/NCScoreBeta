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
     * This method fetches all available arrangements and allows selection if multiple exist.
     *
     * @param context The application [Context].
     * @param song The [Song] object to be opened.
     */
    fun openScore(context: Context, song: Song) {
        Toast.makeText(context, "Fetching arrangements...", Toast.LENGTH_SHORT).show()

        FirebaseFirestore.getInstance()
            .collection("songs").document(song.id).collection("arrangements")
            .get()
            .addOnSuccessListener { documents ->
                val arrangements = documents.toObjects(Arrangement::class.java)
                if (arrangements.isEmpty()) {
                    Toast.makeText(context, "No arrangements found for this song.", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                if (arrangements.size == 1) {
                    handleArrangementSelection(context, song, arrangements[0])
                } else {
                    showArrangementSelectionDialog(context, song, arrangements)
                }
            }
            .addOnFailureListener {
                Toast.makeText(context, "Error fetching score data.", Toast.LENGTH_SHORT).show()
            }
    }

    /**
     * Displays a dialog for the user to select from multiple song arrangements (e.g., different keys).
     */
    private fun showArrangementSelectionDialog(context: Context, song: Song, arrangements: List<Arrangement>) {
        val options = arrangements.map { arr ->
            val type = arr.type.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            val keyStr = if (arr.key.isNotBlank()) " [Key: ${arr.key}]" else ""
            val diff = if (arr.difficulty.isNotBlank()) " (${arr.difficulty})" else ""
            "$type$keyStr$diff"
        }.toTypedArray()

        androidx.appcompat.app.AlertDialog.Builder(context)
            .setTitle("Select Arrangement")
            .setItems(options) { _, which ->
                handleArrangementSelection(context, song, arrangements[which])
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    /**
     * Directs the flow based on whether the selected arrangement is premium or standard.
     */
    private fun handleArrangementSelection(context: Context, song: Song, arrangement: Arrangement) {
        if (song.isPremium == true) {
            checkPremiumAccess(context, song, arrangement)
        } else {
            openStandardArrangement(context, song, arrangement)
        }
    }

    /**
     * Internal helper to verify if the current user is authorized to view premium content.
     *
     * Requirement: Login Only.
     */
    private fun checkPremiumAccess(context: Context, song: Song, arrangement: Arrangement) {
        val user = FirebaseAuth.getInstance().currentUser

        if (user == null) {
            Toast.makeText(context, "Premium Score: Please Log In", Toast.LENGTH_SHORT).show()
            val intent = Intent(context, LoginActivity::class.java)
            context.startActivity(intent)
        } else {
            // Requirement is for login only - if user is authenticated, allow access.
            fetchPremiumPdf(context, song, arrangement)
        }
    }

    /**
     * Fetches the secure download link for a verified premium arrangement.
     */
    private fun fetchPremiumPdf(context: Context, song: Song, arrangement: Arrangement) {
        Toast.makeText(context, "Verifying Premium Access...", Toast.LENGTH_SHORT).show()
        // Path: songs/{songId}/arrangements/{arrId}/secure/content
        fetchNestedSecureLink(context, song.id, arrangement.id)
    }

    /**
     * Retrieves the actual PDF URL from the nested 'secure' document in Firestore.
     */
    private fun fetchNestedSecureLink(context: Context, songId: String, arrangementId: String) {
        FirebaseFirestore.getInstance()
            .collection("songs").document(songId)
            .collection("arrangements").document(arrangementId)
            .collection("secure").document("content")
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    var pdfUrl = document.getString("downloadLink")
                    // Fallback checks
                    if (pdfUrl.isNullOrEmpty()) pdfUrl = document.getString("link")
                    if (pdfUrl.isNullOrEmpty()) pdfUrl = document.getString("url")

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
     * Handles opening a standard (public) arrangement, with offline support.
     */
    private fun openStandardArrangement(context: Context, song: Song, arrangement: Arrangement) {
        Toast.makeText(context, "Opening ${song.title}...", Toast.LENGTH_SHORT).show()

        val prefs = context.getSharedPreferences("nc_prefs", Context.MODE_PRIVATE)
        val isOfflineEnabled = prefs.getBoolean("offline_mode", false)

        if (isOfflineEnabled) {
            val safeSongId = SecurityUtils.sanitizeFilename(song.id)
            val safeArrId = SecurityUtils.sanitizeFilename(arrangement.id)
            
            // Primary check: Arrangement-specific file
            val localFile = java.io.File(context.getExternalFilesDir("scores"), "${safeSongId}_${safeArrId}.pdf")
            // Legacy check: General song file (backwards compatibility)
            val legacyFile = java.io.File(context.getExternalFilesDir("scores"), "$safeSongId.pdf")
            
            val fileToOpen = if (localFile.exists()) localFile else if (legacyFile.exists()) legacyFile else null

            if (fileToOpen != null) {
                android.util.Log.d("SongHandler", "Opening local file: ${fileToOpen.absolutePath}")
                val intent = Intent(context, PdfViewerActivity::class.java)
                intent.putExtra("PDF_FILE", fileToOpen.absolutePath)
                context.startActivity(intent)
                return
            }
        }

        val pdfUrl = arrangement.downloadLink
        if (pdfUrl.isNotEmpty()) {
            if (isOfflineEnabled) {
                downloadAndOpenPdf(context, song.id, arrangement.id, pdfUrl)
            } else {
                openPdfViewer(context, pdfUrl)
            }
        } else {
            Toast.makeText(context, "No PDF link found for this arrangement.", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Helper to launch the [PdfViewerActivity] with a remote URL.
     */
    private fun openPdfViewer(context: Context, url: String) {
        val intent = Intent(context, PdfViewerActivity::class.java)
        intent.putExtra("PDF_URL", url)
        context.startActivity(intent)
    }

    /**
     * Downloads the PDF file to local storage and then opens it in the viewer.
     */
    private fun downloadAndOpenPdf(context: Context, songId: String, arrangementId: String, url: String) {
        if (!SecurityUtils.isSecureUrl(url)) {
            Toast.makeText(context, "Security Error: Insecure URL rejected.", Toast.LENGTH_SHORT).show()
            return
        }

        Toast.makeText(context, "Downloading for offline use...", Toast.LENGTH_SHORT).show()
        val contextRef = java.lang.ref.WeakReference(context)

        kotlin.concurrent.thread {
            try {
                val ctxForPath = contextRef.get() ?: return@thread
                val safeSongId = SecurityUtils.sanitizeFilename(songId)
                val safeArrId = SecurityUtils.sanitizeFilename(arrangementId)
                
                val destFile = java.io.File(ctxForPath.getExternalFilesDir("scores"), "${safeSongId}_${safeArrId}.pdf")

                val parent = destFile.parentFile
                if (parent != null && !parent.exists()) parent.mkdirs()

                val conn = SecurityUtils.openSafeConnection(url)
                val input = java.io.BufferedInputStream(conn.inputStream)
                val output = java.io.FileOutputStream(destFile)

                val data = ByteArray(1024)
                var count: Int
                while (input.read(data).also { count = it } != -1) {
                    output.write(data, 0, count)
                }

                output.flush()
                output.close()
                input.close()

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
