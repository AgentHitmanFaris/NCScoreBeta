package com.noobcompany.nc_scorebeta

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import java.io.File

import com.google.firebase.firestore.FirebaseFirestore
import android.os.Build
import com.google.firebase.auth.FirebaseAuth

import android.app.AlertDialog
import android.widget.EditText
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Fragment that displays application settings and user profile options.
 *
 * It provides functionality for the user to log in or out, clear the app cache, view the "About" information,
 * report bugs (including sending encrypted logs), and check for application updates.
 */
class SettingsFragment : Fragment() {

    private lateinit var tvLogout: TextView
    private lateinit var dividerLogout: View

    /**
     * Inflates the layout for this fragment.
     *
     * @param inflater The LayoutInflater object that can be used to inflate any views in the fragment.
     * @param container If non-null, this is the parent view that the fragment's UI should be attached to.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state.
     * @return The View for the fragment's UI, or null.
     */
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    /**
     * Called immediately after [onCreateView] has returned.
     *
     * Initializes the UI components, displays the current app version, and sets up action listeners for the settings options.
     *
     * @param view The View returned by [onCreateView].
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        tvLogout = view.findViewById(R.id.tvLogout)
        dividerLogout = view.findViewById(R.id.dividerLogout)
        
        // Set dynamic version text
        try {
            val pInfo = requireContext().packageManager.getPackageInfo(requireContext().packageName, 0)
            view.findViewById<TextView>(R.id.tvVersion).text = "Version ${pInfo.versionName}"
        } catch (e: Exception) {
            view.findViewById<TextView>(R.id.tvVersion).text = "Version Unknown"
        }

        setupActions(view)
    }

    /**
     * Called when the fragment is resumed.
     *
     * Updates the state of the logout button (Log In vs Log Out) to reflect the current authentication status.
     */
    override fun onResume() {
        super.onResume()
        updateLogoutButton()
    }

    /**
     * Updates the text and behavior of the logout/login button based on whether a user is currently signed in.
     *
     * If signed in, it configures the button to log out.
     * If signed out, it configures the button to navigate to the [LoginActivity].
     */
    private fun updateLogoutButton() {
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            tvLogout.visibility = View.VISIBLE
            dividerLogout.visibility = View.VISIBLE
            tvLogout.text = "Log Out (${user.email})"
            
            // Change click listener to Logout
            tvLogout.setOnClickListener {
                FirebaseAuth.getInstance().signOut()
                Toast.makeText(context, "Logged Out", Toast.LENGTH_SHORT).show()
                updateLogoutButton()
            }
            tvLogout.setCompoundDrawablesWithIntrinsicBounds(0, 0, android.R.drawable.ic_lock_power_off, 0)
            tvLogout.setTextColor(android.graphics.Color.parseColor("#FF5555"))
        } else {
            // Show "Log In" instead of hiding
            tvLogout.visibility = View.VISIBLE
            dividerLogout.visibility = View.VISIBLE
            tvLogout.text = "Log In / Register"
            
            // Change click listener to Login
            tvLogout.setOnClickListener {
                val intent = android.content.Intent(context, LoginActivity::class.java)
                startActivity(intent)
            }
            // Reset style for Login button
            tvLogout.setCompoundDrawablesWithIntrinsicBounds(0, 0, android.R.drawable.ic_menu_directions, 0)
            tvLogout.setTextColor(android.graphics.Color.WHITE)
        }
    }

    /**
     * Sets up click listeners for the various settings actions (Clear Cache, About, Report Bug, Check Update).
     *
     * @param view The root view of the fragment.
     */
    private fun setupActions(view: View) {
        // Offline Mode Switch
        val switchOffline = view.findViewById<com.google.android.material.switchmaterial.SwitchMaterial>(R.id.switchOffline)
        val prefs = requireContext().getSharedPreferences("nc_prefs", android.content.Context.MODE_PRIVATE)
        
        // Set initial state
        switchOffline.isChecked = prefs.getBoolean("offline_mode", false)

        switchOffline.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("offline_mode", isChecked).apply()
            val status = if (isChecked) "Enabled: Scores will be saved" else "Disabled: Scores will stream"
            Toast.makeText(context, status, Toast.LENGTH_SHORT).show()
        }

        view.findViewById<TextView>(R.id.btnCache).setOnClickListener {
            clearCache()
        }

        view.findViewById<TextView>(R.id.btnAbout).setOnClickListener {
            Toast.makeText(context, "Developed by NoobCompany", Toast.LENGTH_LONG).show()
        }
        
        view.findViewById<TextView>(R.id.btnReportBug).setOnClickListener {
            reportBug()
        }

        view.findViewById<TextView>(R.id.btnCheckUpdate).setOnClickListener {
            UpdateManager.checkForUpdates(requireContext())
        }
        
        // Initial listener setup is handled in updateLogoutButton
    }

    /**
     * Collects system logs and device information, encrypts the logs, and sends a bug report to the "bug_reports" collection in Firestore.
     */
    private fun reportBug() {
        val input = EditText(context)
        input.hint = "Describe the issue (optional)"
        
        AlertDialog.Builder(context)
            .setTitle("Report Bug")
            .setView(input)
            .setPositiveButton("Send") { _, _ ->
                val comment = input.text.toString()
                sendBugReport(comment)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    /**
     * Sends the bug report to Firestore.
     *
     * @param comment The user's comment describing the issue.
     */
    private fun sendBugReport(comment: String) {
        Toast.makeText(context, "Generating Report...", Toast.LENGTH_SHORT).show()
        
        lifecycleScope.launch {
            val user = FirebaseAuth.getInstance().currentUser
            
            // Generate encrypted logs in background
            val encryptedLogs = withContext(Dispatchers.IO) {
                AppLogger.getEncryptedSystemLogs(comment)
            }

            val report = hashMapOf(
                "userId" to (user?.uid ?: "anonymous"),
                "userEmail" to (user?.email ?: "anonymous"),
                "timestamp" to com.google.firebase.Timestamp.now(),
                "deviceModel" to Build.MODEL,
                "deviceManufacturer" to Build.MANUFACTURER,
                "androidVersion" to Build.VERSION.RELEASE,
                "sdkVersion" to Build.VERSION.SDK_INT,
                "encryptedLogs" to encryptedLogs // Sent as a single compressed string
            )
            
            FirebaseFirestore.getInstance().collection("bug_reports")
                .add(report)
                .addOnSuccessListener {
                    Toast.makeText(context, "Report Sent! Thank you.", Toast.LENGTH_LONG).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Failed to send report: ${e.message}", Toast.LENGTH_SHORT).show()
                    AppLogger.error("Settings", "Bug Report Failed", e)
                }
        }
    }

    /**
     * Clears the application's internal cache directory and displays a toast with the amount of space cleared.
     */
    private fun clearCache() {
        try {
            val cacheDir = context?.cacheDir
            val size = getDirSize(cacheDir)
            deleteDir(cacheDir)
            Toast.makeText(context, "Cache Cleared (${formatSize(size)})", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Error clearing cache", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Recursively deletes a directory and all of its contents.
     *
     * @param dir The directory (or file) to delete.
     * @return True if the deletion was successful, false otherwise.
     */
    private fun deleteDir(dir: File?): Boolean {
        if (dir != null && dir.isDirectory) {
            val children = dir.list()
            if (children != null) {
                for (i in children.indices) {
                    val success = deleteDir(File(dir, children[i]))
                    if (!success) {
                        return false
                    }
                }
            }
            return dir.delete()
        } else if (dir != null && dir.isFile) {
            return dir.delete()
        }
        return false
    }

    /**
     * Recursively calculates the total size of a directory and its contents.
     *
     * @param dir The directory (or file) to measure.
     * @return The total size in bytes.
     */
    private fun getDirSize(dir: File?): Long {
        var size: Long = 0
        if (dir != null && dir.isDirectory) {
            val children = dir.listFiles()
            if (children != null) {
                for (child in children) {
                    size += getDirSize(child)
                }
            }
        } else if (dir != null && dir.isFile) {
            size += dir.length()
        }
        return size
    }

    /**
     * Formats a byte size into a human-readable string (KB or MB).
     *
     * @param size The size in bytes.
     * @return A string representing the size in KB or MB.
     */
    private fun formatSize(size: Long): String {
        val kb = size / 1024
        val mb = kb / 1024
        return if (mb > 0) "$mb MB" else "$kb KB"
    }
}
