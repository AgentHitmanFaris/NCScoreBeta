package com.noobcompany.ncscores.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.noobcompany.ncscores.data.FirestoreService
import com.noobcompany.ncscores.model.Arrangement
import com.noobcompany.ncscores.model.Resource
import com.noobcompany.ncscores.model.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CatalogFilters(
    val query: String,
    val key: String?,
    val difficulty: String?,
    val instrument: String?
)

class SongsViewModel(
    private val firestoreService: FirestoreService = FirestoreService()
) : ViewModel() {

    private val _songsCatalogState = MutableStateFlow<Resource<List<Song>>>(Resource.Loading)
    val songsCatalogState: StateFlow<Resource<List<Song>>> = _songsCatalogState.asStateFlow()

    // Filter states
    val searchQuery = MutableStateFlow("")
    val selectedKey = MutableStateFlow<String?>(null)
    val selectedDifficulty = MutableStateFlow<String?>(null)
    val selectedInstrument = MutableStateFlow<String?>(null)

    // Cached map of arrangements to song ID to avoid redundant firestore loading during text search
    private val _arrangementsCache = MutableStateFlow<Map<String, List<Arrangement>>>(emptyMap())

    init {
        loadCatalog()
    }

    fun loadCatalog() {
        viewModelScope.launch {
            _songsCatalogState.value = Resource.Loading
            try {
                val songs = firestoreService.getAllSongs()
                
                // Warm up cache of arrangements for precise filtering
                val tempCache = mutableMapOf<String, List<Arrangement>>()
                for (song in songs) {
                    val arrs = firestoreService.getArrangements(song.id)
                    tempCache[song.id] = arrs
                }
                _arrangementsCache.value = tempCache
                _songsCatalogState.value = Resource.Success(songs)
            } catch (e: Exception) {
                _songsCatalogState.value = Resource.Error(e)
            }
        }
    }

    // Helper flow combining simple filters
    private val _filtersFlow = combine(
        searchQuery,
        selectedKey,
        selectedDifficulty,
        selectedInstrument
    ) { query, key, diff, instr ->
        CatalogFilters(query, key, diff, instr)
    }

    // Reactive filtered song stream built by combining flows cleanly without signatures collision
    val filteredSongs: StateFlow<Resource<List<Song>>> = combine(
        _songsCatalogState,
        _filtersFlow,
        _arrangementsCache
    ) { catalog, filters, arrCache ->
        if (catalog !is Resource.Success) {
            catalog
        } else {
            val filtered = catalog.data.filter { song ->
                // 1. Text Query Filter (Matches title or artist names)
                val matchesQuery = filters.query.isEmpty() ||
                        song.title.contains(filters.query, ignoreCase = true) ||
                        song.artistNames.any { it.contains(filters.query, ignoreCase = true) }

                // 2. Key Filter
                val matchesKey = filters.key == null || song.originalKey.equals(filters.key, ignoreCase = true)

                // Gather arrangements for this song to test arrangement-based filters
                val arrangements = arrCache[song.id] ?: emptyList()

                // 3. Difficulty Filter
                val matchesDifficulty = filters.difficulty == null || arrangements.any { arr ->
                    arr.difficulty.equals(filters.difficulty, ignoreCase = true)
                }

                // 4. Instrument Filter
                val matchesInstrument = filters.instrument == null || arrangements.any { arr ->
                    arr.instruments.any { inst -> inst.contains(filters.instrument, ignoreCase = true) }
                }

                matchesQuery && matchesKey && matchesDifficulty && matchesInstrument
            }
            Resource.Success(filtered)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = Resource.Loading
    )

    fun setSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun selectKey(key: String?) {
        selectedKey.value = key
    }

    fun selectDifficulty(diff: String?) {
        selectedDifficulty.value = diff
    }

    fun selectInstrument(instr: String?) {
        selectedInstrument.value = instr
    }

    fun resetFilters() {
        searchQuery.value = ""
        selectedKey.value = null
        selectedDifficulty.value = null
        selectedInstrument.value = null
    }
}
