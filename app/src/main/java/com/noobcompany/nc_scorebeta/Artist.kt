package com.noobcompany.nc_scorebeta

data class Artist(
    val id: String = "",
    val name: String = "",
    val bio: String = "",
    val imageUrl: String = "", // Primary
    val image: String = ""     // Fallback if field is named "image"
) {
    fun getSafeImage(): String {
        return if (imageUrl.isNotEmpty()) imageUrl else image
    }
}