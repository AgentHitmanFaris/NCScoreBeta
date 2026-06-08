package com.noobcompany.ncscores.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.noobcompany.ncscores.data.FirestoreService
import com.noobcompany.ncscores.model.Artist
import com.noobcompany.ncscores.model.Resource
import com.noobcompany.ncscores.model.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ArtistViewModel(
    private val firestoreService: FirestoreService = FirestoreService()
) : ViewModel() {

    private val _artistProfileState = MutableStateFlow<Resource<Artist>>(Resource.Loading)
    val artistProfileState: StateFlow<Resource<Artist>> = _artistProfileState.asStateFlow()

    private val _artistSongsState = MutableStateFlow<Resource<List<Song>>>(Resource.Loading)
    val artistSongsState: StateFlow<Resource<List<Song>>> = _artistSongsState.asStateFlow()

    fun loadArtistDetails(artistId: String) {
        viewModelScope.launch {
            _artistProfileState.value = Resource.Loading
            _artistSongsState.value = Resource.Loading
            try {
                // Fetch profile bio/image
                val profile = firestoreService.getArtistProfile(artistId)
                _artistProfileState.value = Resource.Success(profile)

                // Fetch matching song catalog
                val songs = firestoreService.getSongsByArtist(artistId)
                _artistSongsState.value = Resource.Success(songs)
            } catch (e: Exception) {
                _artistProfileState.value = Resource.Error(e)
                _artistSongsState.value = Resource.Error(e)
            }
        }
    }
}
