package com.noobcompany.nc_scorebeta

import android.content.Context
import android.content.SharedPreferences

/**
 * Singleton manager for handling user favorites.
 *
 * This object abstracts the `SharedPreferences` interaction required to persist the user's
 * list of favorite songs locally on the device. It supports adding, removing, and querying
 * the favorite status of songs by their unique ID.
 */
object FavoritesManager {
    private const val PREF_NAME = "favorites_prefs"
    private const val KEY_FAVORITES = "favorite_song_ids"

    /**
     * Internal helper to access the application's shared preferences for favorites.
     *
     * @param context The Android Context required to open SharedPreferences.
     * @return The `SharedPreferences` instance.
     */
    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Determines if a song is currently in the user's favorites list.
     *
     * @param context The application Context.
     * @param songId The unique ID of the song to check.
     * @return `true` if the song ID exists in the stored set; `false` otherwise.
     */
    fun isFavorite(context: Context, songId: String): Boolean {
        val favorites = getFavorites(context)
        return favorites.contains(songId)
    }

    /**
     * Toggles the favorite state of a song.
     *
     * If the song is already favorited, it is removed. If not, it is added.
     * The changes are immediately committed to storage.
     *
     * @param context The application Context.
     * @param songId The unique ID of the song.
     * @return `true` if the song resulted in being a favorite (added); `false` if it was removed.
     */
    fun toggleFavorite(context: Context, songId: String): Boolean {
        val prefs = getPrefs(context)
        val favorites = getFavorites(context).toMutableSet()
        val isNowFavorite: Boolean

        if (favorites.contains(songId)) {
            favorites.remove(songId)
            isNowFavorite = false
        } else {
            favorites.add(songId)
            isNowFavorite = true
        }

        prefs.edit().putStringSet(KEY_FAVORITES, favorites).apply()
        return isNowFavorite
    }

    /**
     * Retrieves the complete set of favorite song IDs.
     *
     * @param context The application Context.
     * @return An immutable [Set] of song IDs. Returns an empty set if no favorites are stored.
     */
    fun getFavorites(context: Context): Set<String> {
        return getPrefs(context).getStringSet(KEY_FAVORITES, emptySet()) ?: emptySet()
    }
}
