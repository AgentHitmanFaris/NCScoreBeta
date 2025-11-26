package com.noobcompany.nc_scorebeta

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName

data class Song(
    @DocumentId
    val id: String = "",

    var title: String = "",

    @get:PropertyName("artistNames") @set:PropertyName("artistNames")
    var artistNames: List<String> = emptyList(),

    var albumCover: String = "",

    @field:JvmField
    var isPremium: Boolean = false,

    // ADDED: This lets us sort by date!
    var createdAt: Timestamp? = null
) {
    fun getFormattedArtist(): String {
        return if (artistNames.isNotEmpty()) {
            artistNames.joinToString(", ")
        } else {
            "Unknown Artist"
        }
    }
}