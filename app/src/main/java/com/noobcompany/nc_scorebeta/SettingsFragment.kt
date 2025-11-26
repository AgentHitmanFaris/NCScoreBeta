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

class SettingsFragment : Fragment() {

    private lateinit var tvLogout: TextView
    private lateinit var dividerLogout: View

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

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

    override fun onResume() {
        super.onResume()
        updateLogoutButton()
    }

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

    private fun setupActions(view: View) {
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

    private fun reportBug() {
        Toast.makeText(context, "Sending Report...", Toast.LENGTH_SHORT).show()
        val db = FirebaseFirestore.getInstance()
        val user = FirebaseAuth.getInstance().currentUser
        
        // Generate encrypted logs (runs in background thread ideally, but for now UI thread is ok for beta)
        val encryptedLogs = AppLogger.getEncryptedSystemLogs()

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
        
        db.collection("bug_reports")
            .add(report)
            .addOnSuccessListener {
                Toast.makeText(context, "Report Sent! Thank you.", Toast.LENGTH_LONG).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Failed to send report: ${e.message}", Toast.LENGTH_SHORT).show()
                AppLogger.error("Settings", "Bug Report Failed", e)
            }
    }

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

    private fun formatSize(size: Long): String {
        val kb = size / 1024
        val mb = kb / 1024
        return if (mb > 0) "$mb MB" else "$kb KB"
    }
}