package com.noobcompany.nc_scorebeta

import com.google.firebase.firestore.PropertyName

data class Arrangement(
    // The web app calls it 'downloadLink', so we must match that
    @get:PropertyName("downloadLink") @set:PropertyName("downloadLink")
    var downloadLink: String = "",

    var type: String = ""
)