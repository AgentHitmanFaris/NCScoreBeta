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
 * It serves as the primary container for the application's user interface after the splash screen.
 * It hosts the main navigation fragments (Home, Browse, Library, Artists, Settings) via a [BottomNavigationView].
 * It also configures global Firebase settings, such as offline persistence.
 */
class MainActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()

    /**
     * Called when the activity is first created.
     *
     * It initializes offline persistence for Firestore, sets up the bottom navigation listeners,
     * and loads the default [HomeFragment] if the activity is not being restored from a saved state.
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
     * This setting allows the app to cache data locally, enabling it to function partially without an internet connection.
     * It handles the potential exception if persistence is already enabled.
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
     * Sets up the bottom navigation view and defines the behavior for item selection.
     *
     * It maps menu items to their corresponding fragments and loads them into the fragment container.
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
     * Replaces the currently displayed fragment in the `fragmentContainer` with the specified fragment.
     *
     * @param fragment The [Fragment] to display.
     */
    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }

    /**
     * Programmatically switches the selected tab to the Browse tab.
     *
     * This method allows other parts of the application (e.g., fragments) to trigger navigation changes.
     */
    fun switchToBrowse() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNav.selectedItemId = R.id.nav_browse
    }
    
    /**
     * Opens the song detail fragment for the given song.
     *
     * @param song The [Song] object to display details for.
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
