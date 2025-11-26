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

object UpdateManager {

    private const val GITHUB_OWNER = "AgentHitmanFaris"
    private const val GITHUB_REPO = "NCScoreBeta"
    // API endpoint for the latest release
    private const val LATEST_RELEASE_URL = "https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/releases/latest"

    fun checkForUpdates(context: Context) {
        Toast.makeText(context, "Checking for updates...", Toast.LENGTH_SHORT).show()
        FetchReleaseTask(context).execute()
    }

    private class FetchReleaseTask(val context: Context) : AsyncTask<Void, Void, String?>() {
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

                val currentVersion = "v${BuildConfig.VERSION_NAME}"
                
                // Simple string comparison. For robust semver, a library is better, 
                // but this works if we stick to consistent vX.X.X naming.
                if (isNewerVersion(tagName, currentVersion)) {
                    showUpdateDialog(context, tagName, downloadUrl)
                } else {
                    Toast.makeText(context, "You are on the latest version ($currentVersion).", Toast.LENGTH_SHORT).show()
                }

            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Error parsing update info.", Toast.LENGTH_SHORT).show()
            }
        }
        
        private fun isNewerVersion(serverTag: String, currentTag: String): Boolean {
            // Remove 'v' prefix
            val server = serverTag.replace("v", "").trim()
            val current = currentTag.replace("v", "").trim()
            
            return server != current
        }
    }

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

    private fun downloadAndInstall(context: Context, url: String) {
        DownloadTask(context).execute(url)
    }

    private class DownloadTask(val context: Context) : AsyncTask<String, Int, File?>() {
        private var progressDialog: ProgressDialog? = null

        override fun onPreExecute() {
            progressDialog = ProgressDialog(context)
            progressDialog?.setMessage("Downloading Update...")
            progressDialog?.isIndeterminate = false
            progressDialog?.max = 100
            progressDialog?.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
            progressDialog?.setCancelable(false)
            progressDialog?.show()
        }

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

        override fun onProgressUpdate(vararg values: Int?) {
            values[0]?.let { progressDialog?.progress = it }
        }

        override fun onPostExecute(file: File?) {
            progressDialog?.dismiss()
            if (file != null) {
                installApk(context, file)
            } else {
                Toast.makeText(context, "Download Failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

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