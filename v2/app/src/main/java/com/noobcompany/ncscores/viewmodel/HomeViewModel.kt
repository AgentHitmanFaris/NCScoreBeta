package com.noobcompany.ncscores.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.noobcompany.ncscores.data.FirestoreService
import com.noobcompany.ncscores.model.Resource
import com.noobcompany.ncscores.model.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

object RecentlyViewedTracker {
    private val _recentlyViewedIds = MutableStateFlow<List<String>>(listOf("moonlight_sonata", "minuet_in_g"))
    val recentlyViewedIds: StateFlow<List<String>> = _recentlyViewedIds.asStateFlow()

    fun addSong(songId: String) {
        val current = _recentlyViewedIds.value.toMutableList()
        current.remove(songId)
        current.add(0, songId) // Insert at the top
        _recentlyViewedIds.value = current.take(5) // Limit to top 5
    }
}

class HomeViewModel(
    private val firestoreService: FirestoreService = FirestoreService()
) : ViewModel() {

    private val _featuredSongsState = MutableStateFlow<Resource<List<Song>>>(Resource.Loading)
    val featuredSongsState: StateFlow<Resource<List<Song>>> = _featuredSongsState.asStateFlow()

    private val _recentSongsState = MutableStateFlow<Resource<List<Song>>>(Resource.Loading)
    val recentSongsState: StateFlow<Resource<List<Song>>> = _recentSongsState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _featuredSongsState.value = Resource.Loading
            _recentSongsState.value = Resource.Loading
            try {
                // Fetch featured songs from Firestore
                val featuredList = firestoreService.getFeaturedSongs()
                _featuredSongsState.value = Resource.Success(featuredList)

                // Fetch recent songs matching local state ids
                val allSongs = firestoreService.getAllSongs()
                RecentlyViewedTracker.recentlyViewedIds.collect { viewedIds ->
                    val matching = viewedIds.mapNotNull { id ->
                        allSongs.firstOrNull { it.id == id }
                    }
                    _recentSongsState.value = Resource.Success(matching)
                }
            } catch (e: Exception) {
                _featuredSongsState.value = Resource.Error(e)
                _recentSongsState.value = Resource.Error(e)
            }
        }
    }

    fun registerSongInteracted(songId: String) {
        RecentlyViewedTracker.addSong(songId)
    }
}
