package com.noobcompany.ncscores.model

import com.google.firebase.Timestamp

data class Song(
    val id: String = "",
    val title: String = "",
    val artistNames: List<String> = emptyList(),
    val artistIds: List<String> = emptyList(),
    val albumCover: String = "",
    val isPremium: Boolean = false,
    val isComingSoon: Boolean = false,
    val originalKey: String = "",
    val dateAdded: Timestamp? = null,
    val lyrics: String = "",
    val video: String = "", // YouTube link
    val isFeatured: Boolean = false
) {
    // Empty constructor for Firestore deserialization fallback
    constructor() : this(
        id = "",
        title = "",
        artistNames = emptyList(),
        artistIds = emptyList(),
        albumCover = "",
        isPremium = false,
        isComingSoon = false,
        originalKey = "",
        dateAdded = null,
        lyrics = "",
        video = "",
        isFeatured = false
    )
}

data class Arrangement(
    val id: String = "",
    val instruments: List<String> = emptyList(),
    val difficulty: String = "",
    val downloadLink: String = "", // Firebase Storage PDF URL
    val midiUrl: String = ""
) {
    // Empty constructor for Firestore
    constructor() : this(
        id = "",
        instruments = emptyList(),
        difficulty = "",
        downloadLink = "",
        midiUrl = ""
    )
}

data class Artist(
    val id: String = "",
    val name: String = "",
    val bio: String = "",
    val image: String = ""
) {
    // Empty constructor for Firestore
    constructor() : this(
        id = "",
        name = "",
        bio = "",
        image = ""
    )
}
