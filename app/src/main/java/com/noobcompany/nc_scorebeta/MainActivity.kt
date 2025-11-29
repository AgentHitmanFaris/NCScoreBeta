package com.noobcompany.nc_scorebeta

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.FirebaseFirestore

/**
 * The Main Activity of the application.
 *
 * It serves as the entry point after the splash screen and hosts the main navigation fragments
 * (Home, Browse, Library, etc.) via a BottomNavigationView.
 * It also configures global Firebase settings like offline persistence.
 */
class MainActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()

    /**
     * Called when the activity is first created.
     *
     * Initializes offline persistence, sets up bottom navigation, and loads the default fragment.
     *
     * @param savedInstanceState If non-null, this activity is being re-constructed from a previous saved state.
     */
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

    /**
     * Enables offline persistence for Firestore.
     *
     * This allows the app to work (to some extent) without an internet connection by caching data locally.
     */
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

    /**
     * Sets up the bottom navigation view and its item selection listener.
     */
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

    /**
     * Replaces the current fragment in the container with the specified fragment.
     *
     * @param fragment The fragment to display.
     */
    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }

    /**
     * Helper method to programmatically switch to the Browse tab.
     *
     * This is useful when triggering navigation from within other fragments (e.g., "See All" buttons).
     */
    fun switchToBrowse() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNav.selectedItemId = R.id.nav_browse
    }
    
    fun openSongDetail(song: Song) {
        val fragment = SongDetailFragment()
        val args = Bundle()
        args.putString("SONG_ID", song.id)
        fragment.arguments = args
        
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .addToBackStack(null)
            .commit()
    }
}
