package com.noobcompany.nc_scorebeta

import android.content.Context
import android.content.SharedPreferences

/**
 * Singleton object to manage user's favorite songs.
 *
 * Stores a set of favorite song IDs in SharedPreferences.
 */
object FavoritesManager {
    private const val PREF_NAME = "favorites_prefs"
    private const val KEY_FAVORITES = "favorite_song_ids"

    /**
     * Helper method to get the SharedPreferences instance.
     *
     * @param context The application context.
     * @return The SharedPreferences instance.
     */
    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Checks if a song is marked as a favorite.
     *
     * @param context The application context.
     * @param songId The ID of the song to check.
     * @return True if the song is a favorite, false otherwise.
     */
    fun isFavorite(context: Context, songId: String): Boolean {
        val favorites = getFavorites(context)
        return favorites.contains(songId)
    }

    /**
     * Toggles the favorite status of a song.
     *
     * If the song is currently a favorite, it is removed. If it is not, it is added.
     *
     * @param context The application context.
     * @param songId The ID of the song to toggle.
     * @return True if the song is now a favorite, false if it was removed.
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
     * Retrieves the set of all favorite song IDs.
     *
     * @param context The application context.
     * @return A set of strings representing the IDs of favorite songs.
     */
    fun getFavorites(context: Context): Set<String> {
        return getPrefs(context).getStringSet(KEY_FAVORITES, emptySet()) ?: emptySet()
    }
}
