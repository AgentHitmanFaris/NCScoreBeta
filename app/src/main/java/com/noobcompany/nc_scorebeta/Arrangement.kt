package com.noobcompany.nc_scorebeta

import com.google.firebase.firestore.PropertyName

/**
 * Data class representing a musical arrangement.
 *
 * This class is used to map arrangement data from Firestore.
 *
 * @property downloadLink The URL to download the arrangement (e.g., a PDF file).
 *                        Mapped to the "downloadLink" field in Firestore.
 * @property type The type of the arrangement (e.g., "Piano Solo", "Ensemble").
 */
data class Arrangement(
    // The web app calls it 'downloadLink', so we must match that
    @get:PropertyName("downloadLink") @set:PropertyName("downloadLink")
    var downloadLink: String = "",

    var type: String = ""
)
