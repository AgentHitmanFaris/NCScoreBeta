package com.noobcompany.nc_scorebeta

import com.google.firebase.firestore.PropertyName

/**
 * Data class that represents the structure of a musical arrangement document.
 *
 * This model is specifically designed to parse arrangement data fetched from the "arrangements"
 * sub-collection in Firestore. It provides the necessary links to download the sheet music
 * and categorizes the arrangement by type (e.g., specific instrument or ensemble).
 *
 * @property downloadLink The direct URL to the downloadable content (typically a PDF file).
 *                        It is mapped to the "downloadLink" field in the Firestore document.
 * @property type A string descriptor of the arrangement style (e.g., "Piano Solo", "Full Score", "Lead Sheet").
 */
data class Arrangement(
    @get:PropertyName("downloadLink") @set:PropertyName("downloadLink")
    var downloadLink: String = "",

    var type: String = "",
    
    var difficulty: String = "",
    
    var arrangedBy: String = "",
    
    var downloadCount: Int = 0
)
