package com.noobcompany.nc_scorebeta

import android.app.Activity
import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.AsyncTask
import android.os.Build
import android.os.Environment
import android.widget.Toast
import androidx.core.content.FileProvider
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Singleton object that manages application updates.
 *
 * It checks GitHub for the latest release, downloads the APK, and initiates the installation.
 */
object UpdateManager {

    private const val GITHUB_OWNER = "AgentHitmanFaris"
    private const val GITHUB_REPO = "NCScoreBeta"
    // API endpoint for the latest release
    private const val LATEST_RELEASE_URL = "https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/releases/latest"

    /**
     * Checks for the latest updates from the GitHub repository.
     *
     * Displays a toast indicating the check is in progress and executes the [FetchReleaseTask].
     *
     * @param context The application context.
     */
    fun checkForUpdates(context: Context) {
        Toast.makeText(context, "Checking for updates...", Toast.LENGTH_SHORT).show()
        FetchReleaseTask(context).execute()
    }

    /**
     * AsyncTask to fetch release information from GitHub API.
     *
     * @param context The application context.
     */
    private class FetchReleaseTask(val context: Context) : AsyncTask<Void, Void, String?>() {
        /**
         * Performs the network request in the background.
         *
         * @param params Void parameters.
         * @return The JSON response string or null if the request failed.
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
         * Processes the result on the main thread.
         *
         * Parses the JSON, compares versions, and prompts the user if an update is available.
         *
         * @param result The JSON response string.
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
         * Compares the server tag version with the current version using Semantic Versioning rules.
         *
         * @param serverTag The version tag from the server (e.g., "v1.2.0").
         * @param currentTag The current app version tag (e.g., "v1.1.0").
         * @return True if the server version is strictly greater than the current version.
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
     * Shows a dialog prompting the user to update.
     *
     * @param context The application context.
     * @param newVersion The version string of the new update.
     * @param downloadUrl The URL to download the APK.
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
     * Initiates the download and installation process.
     *
     * @param context The application context.
     * @param url The download URL.
     */
    private fun downloadAndInstall(context: Context, url: String) {
        DownloadTask(context).execute(url)
    }

    /**
     * AsyncTask to download the APK file.
     *
     * Shows a progress dialog during download.
     *
     * @param context The application context.
     */
    private class DownloadTask(val context: Context) : AsyncTask<String, Int, File?>() {
        private var progressDialog: ProgressDialog? = null

        /**
         * Sets up and shows the progress dialog before downloading starts.
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
         * Downloads the file in the background.
         *
         * @param params The URL string.
         * @return The downloaded File object, or null if failed.
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
         * Updates the progress dialog.
         *
         * @param values The progress percentage.
         */
        override fun onProgressUpdate(vararg values: Int?) {
            values[0]?.let { progressDialog?.progress = it }
        }

        /**
         * Handles the result of the download.
         *
         * Dismisses the dialog and starts the installation if successful.
         *
         * @param file The downloaded file.
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
     * Triggers the installation intent for the downloaded APK.
     *
     * @param context The application context.
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
