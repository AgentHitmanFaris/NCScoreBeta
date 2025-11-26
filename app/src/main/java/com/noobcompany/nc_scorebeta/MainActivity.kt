package com.noobcompany.nc_scorebeta

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setupOfflinePersistence()
        setupNavigation()

        // Load HomeFragment by default
        if (savedInstanceState == null) {
            loadFragment(HomeFragment())
        }
    }

    private fun setupOfflinePersistence() {
        try {
            val settings = FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .build()
            db.firestoreSettings = settings
        } catch (e: Exception) {
            Log.w("MainActivity", "Persistence already enabled")
        }
    }

    private fun setupNavigation() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNav.setOnItemSelectedListener { item ->
            val fragment: Fragment = when (item.itemId) {
                R.id.nav_home -> HomeFragment()
                R.id.nav_browse -> BrowseFragment()
                R.id.nav_artists -> AllArtistsFragment()
                R.id.nav_library -> LibraryFragment()
                R.id.nav_settings -> SettingsFragment()
                else -> HomeFragment()
            }
            loadFragment(fragment)
            true
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }

    // Helper method to switch to Browse from Home
    fun switchToBrowse() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNav.selectedItemId = R.id.nav_browse
    }
}