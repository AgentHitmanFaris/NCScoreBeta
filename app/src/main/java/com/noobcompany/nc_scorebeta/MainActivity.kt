package com.noobcompany.nc_scorebeta

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.FirebaseFirestore

/**
 * The main container Activity for the NC Score application.
 *
 * This Activity is responsible for:
 * 1. Hosting the application's primary navigation structure via [BottomNavigationView].
 * 2. managing the lifecycle and transaction of the main content fragments.
 * 3. Initializing global configurations, such as Firebase Firestore offline persistence.
 */
class MainActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()

    /**
     * Called when the activity is starting.
     *
     * This method performs initial setup:
     * - Inflates the layout.
     * - Configures Firestore persistence.
     * - Sets up the bottom navigation listener.
     * - Loads the initial fragment (Home) if this is the first creation.
     *
     * @param savedInstanceState If the activity is being re-initialized after previously being shut down then this Bundle contains the data it most recently supplied in onSaveInstanceState(Bundle).
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
     * Enables offline persistence for the Firestore database instance.
     *
     * This setting allows the app to read and write to a local cache of the database,
     * ensuring functionality even when network connectivity is lost.
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
     * Initializes the bottom navigation view and defines the fragment switching logic.
     *
     * It assigns a listener to handle selection events, replacing the displayed fragment based on the menu item ID.
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
     * Replaces the currently active fragment in the `fragmentContainer` view.
     *
     * @param fragment The new [Fragment] to display.
     */
    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }

    /**
     * Public method to Programmatically switch the current tab to "Browse".
     *
     * This allows child fragments or other components to trigger a navigation change to the search/browse screen.
     */
    fun switchToBrowse() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNav.selectedItemId = R.id.nav_browse
    }
    
    /**
     * Navigates to the [SongDetailFragment] to display information about a specific song.
     *
     * This method handles the transaction to replace the current fragment with the detail fragment
     * and adds the transaction to the back stack for proper navigation history.
     *
     * @param song The [Song] object whose details are to be displayed.
     */
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
