package com.noobcompany.nc_scorebeta

import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.os.AsyncTask
import android.os.Environment
import android.widget.Toast
import androidx.core.content.FileProvider
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Singleton manager for handling in-app updates via GitHub Releases.
 *
 * This utility facilitates checking a remote GitHub repository for new release tags,
 * comparing them against the installed version, and orchestrating the download and installation
 * of the APK file if a newer version is found.
 */
object UpdateManager {

    private const val GITHUB_OWNER = "AgentHitmanFaris"
    private const val GITHUB_REPO = "NCScoreBeta"
    // API endpoint for the latest release
    private const val LATEST_RELEASE_URL = "https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/releases/latest"

    /**
     * Triggers a check for application updates.
     *
     * This method displays a "Checking for updates..." toast and launches an asynchronous task
     * to fetch release metadata from GitHub.
     *
     * @param context The application Context, used for UI feedback.
     */
    fun checkForUpdates(context: Context) {
        Toast.makeText(context, "Checking for updates...", Toast.LENGTH_SHORT).show()
        FetchReleaseTask(context).execute()
    }

    /**
     * Background task to query the GitHub API for the latest release information.
     *
     * @property context The context used for displaying dialogs and toasts upon completion.
     */
    private class FetchReleaseTask(val context: Context) : AsyncTask<Void, Void, String?>() {
        /**
         * executes the network request to GitHub in a background thread.
         *
         * @param params (Unused)
         * @return The JSON response string from the API, or null if the request fails.
         */
        override fun doInBackground(vararg params: Void?): String? {
            return try {
                val url = URL(LATEST_RELEASE_URL)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connect()

                if (connection.responseCode == 200) {
                    connection.inputStream.bufferedReader().use { it.readText() }
                } else {
                    null
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }

        /**
         * Handles the API response on the UI thread.
         *
         * Parses the JSON to find the tag name (version) and download URL.
         * Compares the remote version with the local app version using semantic versioning rules.
         *
         * @param result The JSON string retrieved from GitHub.
         */
        override fun onPostExecute(result: String?) {
            if (result == null) {
                Toast.makeText(context, "Failed to check for updates.", Toast.LENGTH_SHORT).show()
                return
            }

            try {
                val json = JSONObject(result)
                val tagName = json.getString("tag_name") // e.g., "v1.2.0" or "1.2.0"
                val downloadUrl = json.getJSONArray("assets")
                    .getJSONObject(0)
                    .getString("browser_download_url")

                val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                val currentVersion = "v${pInfo.versionName}"
                
                // Compare versions using semantic versioning logic
                if (isNewerVersion(tagName, currentVersion)) {
                    showUpdateDialog(context, tagName, downloadUrl)
                } else {
                    Toast.makeText(context, "Current version installed ($currentVersion)", Toast.LENGTH_SHORT).show()
                }

            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Error parsing update info.", Toast.LENGTH_SHORT).show()
            }
        }
        
        /**
         * Semantic version comparison logic.
         *
         * @param serverTag The version tag from the server.
         * @param currentTag The locally installed version tag.
         * @return `true` if the server version is newer (higher).
         */
        private fun isNewerVersion(serverTag: String, currentTag: String): Boolean {
            val serverParts = serverTag.replace("v", "").trim().split(".")
            val currentParts = currentTag.replace("v", "").trim().split(".")

            val length = maxOf(serverParts.size, currentParts.size)
            
            for (i in 0 until length) {
                val serverVer = if (i < serverParts.size) serverParts[i].toIntOrNull() ?: 0 else 0
                val currentVer = if (i < currentParts.size) currentParts[i].toIntOrNull() ?: 0 else 0
                
                if (serverVer > currentVer) return true
                if (serverVer < currentVer) return false
            }
            
            // Versions are equal
            return false
        }
    }

    /**
     * Shows an AlertDialog prompting the user to update.
     *
     * @param context The Context.
     * @param newVersion The new version string.
     * @param downloadUrl The URL for the APK download.
     */
    private fun showUpdateDialog(context: Context, newVersion: String, downloadUrl: String) {
        AlertDialog.Builder(context)
            .setTitle("Update Available")
            .setMessage("A new version ($newVersion) is available. Would you like to download and install it?")
            .setPositiveButton("Update") { _, _ ->
                downloadAndInstall(context, downloadUrl)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    /**
     * Starts the APK download process.
     *
     * @param context The Context.
     * @param url The download URL.
     */
    private fun downloadAndInstall(context: Context, url: String) {
        DownloadTask(context).execute(url)
    }

    /**
     * Background task to download the APK file.
     *
     * Manages the download stream and updates a ProgressDialog.
     *
     * @property context The Context.
     */
    private class DownloadTask(val context: Context) : AsyncTask<String, Int, File?>() {
        private var progressDialog: ProgressDialog? = null

        /**
         * Sets up the progress dialog before download starts.
         */
        override fun onPreExecute() {
            progressDialog = ProgressDialog(context)
            progressDialog?.setMessage("Downloading Update...")
            progressDialog?.isIndeterminate = false
            progressDialog?.max = 100
            progressDialog?.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
            progressDialog?.setCancelable(false)
            progressDialog?.show()
        }

        /**
         * Downloads the file.
         *
         * @param params URL string.
         * @return The downloaded File on success, null on failure.
         */
        override fun doInBackground(vararg params: String?): File? {
            val downloadUrl = params[0] ?: return null
            return try {
                val url = URL(downloadUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.connect()

                val fileLength = connection.contentLength

                val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                val outputFile = File(storageDir, "update.apk")
                if (outputFile.exists()) outputFile.delete()

                val input = connection.inputStream
                val output = FileOutputStream(outputFile)

                val data = ByteArray(4096)
                var total: Long = 0
                var count: Int
                while (input.read(data).also { count = it } != -1) {
                    total += count.toLong()
                    if (fileLength > 0) {
                        publishProgress((total * 100 / fileLength).toInt())
                    }
                    output.write(data, 0, count)
                }

                output.flush()
                output.close()
                input.close()
                outputFile
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }

        /**
         * Updates progress dialog.
         *
         * @param values Progress percentage.
         */
        override fun onProgressUpdate(vararg values: Int?) {
            values[0]?.let { progressDialog?.progress = it }
        }

        /**
         * Called when download finishes. Initiates installation if successful.
         *
         * @param file The downloaded APK file.
         */
        override fun onPostExecute(file: File?) {
            progressDialog?.dismiss()
            if (file != null) {
                installApk(context, file)
            } else {
                Toast.makeText(context, "Download Failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Launches the system intent to install the APK.
     *
     * @param context The Context.
     * @param file The APK file.
     */
    private fun installApk(context: Context, file: File) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )

            val intent = Intent(Intent.ACTION_VIEW)
            intent.setDataAndType(uri, "application/vnd.android.package-archive")
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Install Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
