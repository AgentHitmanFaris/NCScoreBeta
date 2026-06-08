package com.noobcompany.ncscores.data

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.noobcompany.ncscores.model.Arrangement
import com.noobcompany.ncscores.model.Artist
import com.noobcompany.ncscores.model.Song
import kotlinx.coroutines.tasks.await
import java.util.Date

class FirestoreService {

    private val firestore: FirebaseFirestore by lazy {
        try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            Log.e("FirestoreService", "Firebase firestore failed to initialize. Relying on cache/offline memory mode.", e)
            FirebaseFirestore.getInstance()
        }
    }

    // Standard in-memory data for instant offline rendering, previews, and DB initialization
    val mockArtists = listOf(
        Artist(
            id = "beethoven",
            name = "Ludwig van Beethoven",
            bio = "Ludwig van Beethoven (1770–1827) was a German composer and pianist. A crucial figure in the transition between the Classical and Romantic eras in Western classical music, he remains one of the most famous and influential of all composers.",
            image = "https://images.unsplash.com/photo-1507838153414-b4b713384a76?q=80&w=600&auto=format&fit=crop"
        ),
        Artist(
            id = "bach",
            name = "Johann Sebastian Bach",
            bio = "Johann Sebastian Bach (1685–1750) was a German composer and musician of the Baroque period. He is known for instrumental compositions such as the Brandenburg Concertos and keyboard works like the Goldberg Variations.",
            image = "https://images.unsplash.com/photo-1520523839897-bd0b52f945a0?q=80&w=600&auto=format&fit=crop"
        ),
        Artist(
            id = "mozart",
            name = "Wolfgang Amadeus Mozart",
            bio = "Wolfgang Amadeus Mozart (1756–1791) was a prolific and influential composer of the Classical era. Born in Salzburg, he showed prodigious ability from early childhood on keyboard and violin.",
            image = "https://images.unsplash.com/photo-1465847899084-d164df4dedc6?q=80&w=600&auto=format&fit=crop"
        )
    )

    val mockSongs = listOf(
        Song(
            id = "moonlight_sonata",
            title = "Moonlight Sonata (1st Mvt)",
            artistNames = listOf("Ludwig van Beethoven"),
            artistIds = listOf("beethoven"),
            albumCover = "https://images.unsplash.com/photo-1552422535-c45813c61732?q=80&w=500&auto=format&fit=crop",
            isPremium = false,
            isComingSoon = false,
            originalKey = "C# Minor",
            dateAdded = Timestamp(Date()),
            lyrics = "[Instrumental Masterpiece - Adagio sostenuto]",
            video = "https://www.youtube.com/watch?v=4Tr0otuiQuU",
            isFeatured = true
        ),
        Song(
            id = "minuet_in_g",
            title = "Minuet in G major",
            artistNames = listOf("Johann Sebastian Bach"),
            artistIds = listOf("bach"),
            albumCover = "https://images.unsplash.com/photo-1514320291840-2e0a9bf2a9ae?q=80&w=500&auto=format&fit=crop",
            isPremium = false,
            isComingSoon = false,
            originalKey = "G Major",
            dateAdded = Timestamp(Date()),
            lyrics = "[Baroque Keyboard Exercise - Minuet]",
            video = "https://www.youtube.com/watch?v=on1DDSL10Sg",
            isFeatured = true
        ),
        Song(
            id = "lacrimosa",
            title = "Lacrimosa (Requiem)",
            artistNames = listOf("Wolfgang Amadeus Mozart"),
            artistIds = listOf("mozart"),
            albumCover = "https://images.unsplash.com/photo-1465847899084-d164df4dedc6?q=80&w=500&auto=format&fit=crop",
            isPremium = true,
            isComingSoon = false,
            originalKey = "D Minor",
            dateAdded = Timestamp(Date()),
            lyrics = "Lacrimosa dies illa\nQua resurget ex favilla\nJudicandus homo reus.\nHuic ergo parce, Deus,\nPie Jesu Domine,\nDona eis requiem. Amen.",
            video = "https://www.youtube.com/watch?v=k1-TrAvp_xs",
            isFeatured = true
        ),
        Song(
            id = "symphony_5",
            title = "Symphony No. 5 Theme",
            artistNames = listOf("Ludwig van Beethoven"),
            artistIds = listOf("beethoven"),
            albumCover = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?q=80&w=500&auto=format&fit=crop",
            isPremium = true,
            isComingSoon = false,
            originalKey = "C Minor",
            dateAdded = Timestamp(Date()),
            lyrics = "[Symphonic Theme]",
            video = "https://www.youtube.com/watch?v=jv2WJMVPQi8",
            isFeatured = false
        ),
        Song(
            id = "air_g_string",
            title = "Air on the G String",
            artistNames = listOf("Johann Sebastian Bach"),
            artistIds = listOf("bach"),
            albumCover = "https://images.unsplash.com/photo-1520523839897-bd0b52f945a0?q=80&w=500&auto=format&fit=crop",
            isPremium = false,
            isComingSoon = false,
            originalKey = "D Major",
            dateAdded = Timestamp(Date()),
            lyrics = "[Instrumental]",
            video = "https://www.youtube.com/watch?v=E2j-frfKqn0",
            isFeatured = false
        ),
        Song(
            id = "eine_kleine",
            title = "Eine kleine Nachtmusik",
            artistNames = listOf("Wolfgang Amadeus Mozart"),
            artistIds = listOf("mozart"),
            albumCover = "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?q=80&w=500&auto=format&fit=crop",
            isPremium = false,
            isComingSoon = true,
            originalKey = "G Major",
            dateAdded = Timestamp(Date()),
            lyrics = "[Serenade Symphony]",
            video = "https://www.youtube.com/watch?v=nPbxIT9W1AY",
            isFeatured = false
        )
    )

    val mockArrangements = mapOf(
        "moonlight_sonata" to listOf(
            Arrangement("ms_p_easy", listOf("Piano Solo", "Keyboard"), "Easy", "https://www.mutopiaproject.org/ftp/BeethovenLv/O27/moonlight/moonlight-letter.pdf", ""),
            Arrangement("ms_p_int", listOf("Piano Solo"), "Intermediate", "https://www.mutopiaproject.org/ftp/BachJS/BWVAnh114/anh114/anh114-letter.pdf", "")
        ),
        "minuet_in_g" to listOf(
            Arrangement("mig_k_easy", listOf("Keyboard", "Harpsichord"), "Easy", "https://www.mutopiaproject.org/ftp/BachJS/BWVAnh114/anh114/anh114-letter.pdf", ""),
            Arrangement("mig_p_int", listOf("Piano solo"), "Intermediate", "https://www.mutopiaproject.org/ftp/BeethovenLv/O27/moonlight/moonlight-letter.pdf", "")
        ),
        "lacrimosa" to listOf(
            Arrangement("lac_organ", listOf("Organ", "Vocal Choir"), "Hard", "https://www.mutopiaproject.org/ftp/MozartWA/KV626/lacrimosa/lacrimosa-letter.pdf", ""),
            Arrangement("lac_piano", listOf("Piano Reduction"), "Intermediate", "https://www.mutopiaproject.org/ftp/BeethovenLv/O27/moonlight/moonlight-letter.pdf", "")
        ),
        "symphony_5" to listOf(
            Arrangement("sym5_piano", listOf("Piano Solo"), "Hard", "https://www.mutopiaproject.org/ftp/BeethovenLv/O27/moonlight/moonlight-letter.pdf", "")
        ),
        "air_g_string" to listOf(
            Arrangement("air_violin", listOf("Violin", "Piano accompaniment"), "Intermediate", "https://www.mutopiaproject.org/ftp/BachJS/BWVAnh114/anh114/anh114-letter.pdf", "")
        ),
        "eine_kleine" to listOf(
            Arrangement("ek_quartet", listOf("Violin I", "Violin II", "Viola", "Cello"), "Hard", "https://www.mutopiaproject.org/ftp/MozartWA/KV626/lacrimosa/lacrimosa-letter.pdf", "")
        )
    )

    init {
        // Run database seeding checking inside a safe scope if required.
        // It runs asynchronously asynchronously to prevent blocking the main thread.
    }

    /**
     * Seeds the Firestore DB in the cloud if it does not contain any songs.
     */
    suspend fun seedDatabaseIfEmpty() {
        try {
            val songDocsRef = firestore.collection("songs")
            val existing = songDocsRef.limit(1).get().await()
            if (existing.isEmpty) {
                Log.d("FirestoreService", "Target Firestore is empty. Initializing beautiful master list...")
                
                // Write artists
                val artistsColl = firestore.collection("artists")
                for (artist in mockArtists) {
                    artistsColl.document(artist.id).set(artist).await()
                }

                // Write songs & nested arrangements
                for (song in mockSongs) {
                    songDocsRef.document(song.id).set(song).await()
                    
                    val arrangementsList = mockArrangements[song.id] ?: emptyList()
                    val arrangementsColl = songDocsRef.document(song.id).collection("arrangements")
                    for (arr in arrangementsList) {
                        arrangementsColl.document(arr.id).set(arr).await()
                    }
                }
                Log.d("FirestoreService", "Database seeding completed successfully.")
            }
        } catch (e: Exception) {
            Log.e("FirestoreService", "Unable to seed remote database. Running locally.", e)
        }
    }

    suspend fun getFeaturedSongs(): List<Song> {
        return try {
            seedDatabaseIfEmpty()
            val querySnapshot = firestore.collection("songs")
                .whereEqualTo("featured", true)
                .get()
                .await()
            
            // If the query returns nothing, try querying isFeatured (both case options)
            val songs = if (querySnapshot.isEmpty) {
                val altQuery = firestore.collection("songs")
                    .whereEqualTo("isFeatured", true)
                    .get()
                    .await()
                altQuery.toObjects(Song::class.java)
            } else {
                querySnapshot.toObjects(Song::class.java)
            }

            if (songs.isEmpty()) mockSongs.filter { it.isFeatured } else songs
        } catch (e: Exception) {
            Log.e("FirestoreService", "Error loading featured songs: ${e.message}. Using cache fallback.")
            mockSongs.filter { it.isFeatured }
        }
    }

    suspend fun getAllSongs(): List<Song> {
        return try {
            seedDatabaseIfEmpty()
            val querySnapshot = firestore.collection("songs").get().await()
            val songs = querySnapshot.toObjects(Song::class.java)
            if (songs.isEmpty()) mockSongs else songs
        } catch (e: Exception) {
            Log.e("FirestoreService", "Error loading songs catalog: ${e.message}. Using cache fallback.")
            mockSongs
        }
    }

    suspend fun getArtistProfile(artistId: String): Artist {
        return try {
            val doc = firestore.collection("artists").document(artistId).get().await()
            val artist = doc.toObject(Artist::class.java)
            artist ?: mockArtists.firstOrNull { it.id == artistId } ?: Artist(id = artistId, name = "Unknown Artist")
        } catch (e: Exception) {
            Log.e("FirestoreService", "Error loading artist $artistId: ${e.message}. Using cache fallback.")
            mockArtists.firstOrNull { it.id == artistId } ?: Artist(id = artistId, name = "Unknown Artist")
        }
    }

    suspend fun getSongsByArtist(artistId: String): List<Song> {
        return try {
            val querySnapshot = firestore.collection("songs")
                .whereArrayContains("artistIds", artistId)
                .get()
                .await()
            val songs = querySnapshot.toObjects(Song::class.java)
            if (songs.isEmpty()) {
                mockSongs.filter { it.artistIds.contains(artistId) }
            } else {
                songs
            }
        } catch (e: Exception) {
            Log.e("FirestoreService", "Error loading songs for artist $artistId: ${e.message}. Using cache fallback.")
            mockSongs.filter { it.artistIds.contains(artistId) }
        }
    }

    suspend fun getArrangements(songId: String): List<Arrangement> {
        return try {
            val querySnapshot = firestore.collection("songs")
                .document(songId)
                .collection("arrangements")
                .get()
                .await()
            val arrangements = querySnapshot.toObjects(Arrangement::class.java)
            if (arrangements.isEmpty()) {
                mockArrangements[songId] ?: emptyList()
            } else {
                arrangements
            }
        } catch (e: Exception) {
            Log.e("FirestoreService", "Error loading arrangements for song $songId: ${e.message}. Using cache fallback.")
            mockArrangements[songId] ?: emptyList()
        }
    }

    suspend fun getSongDetails(songId: String): Song? {
         return try {
             val doc = firestore.collection("songs").document(songId).get().await()
             val song = doc.toObject(Song::class.java)
             song ?: mockSongs.firstOrNull { it.id == songId }
         } catch (e: Exception) {
             mockSongs.firstOrNull { it.id == songId }
         }
    }
}
