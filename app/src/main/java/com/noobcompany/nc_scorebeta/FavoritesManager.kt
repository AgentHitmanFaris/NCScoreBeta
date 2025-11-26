package com.noobcompany.nc_scorebeta

import android.content.Context
import android.content.SharedPreferences

object FavoritesManager {
    private const val PREF_NAME = "favorites_prefs"
    private const val KEY_FAVORITES = "favorite_song_ids"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun isFavorite(context: Context, songId: String): Boolean {
        val favorites = getFavorites(context)
        return favorites.contains(songId)
    }

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

    fun getFavorites(context: Context): Set<String> {
        return getPrefs(context).getStringSet(KEY_FAVORITES, emptySet()) ?: emptySet()
    }
}