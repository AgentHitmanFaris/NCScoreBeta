package com.noobcompany.nc_scorebeta

import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

object SongHandler {

    fun onSongClicked(context: Context, song: Song) {
        if (song.isPremium) {
            checkPremiumAccess(context, song)
        } else {
            fetchAndOpenPdf(context, song)
        }
    }

    private fun checkPremiumAccess(context: Context, song: Song) {
        val user = FirebaseAuth.getInstance().currentUser

        if (user == null) {
            Toast.makeText(context, "Premium Score: Please Log In", Toast.LENGTH_SHORT).show()
            val intent = Intent(context, LoginActivity::class.java)
            context.startActivity(intent)
        } else {
            // Verify User in Firestore
            val db = FirebaseFirestore.getInstance()
            db.collection("users").document(user.uid).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        // Optional: Check for specific "isPremiumUser" field if needed
                        // val isPremium = document.getBoolean("isPremiumUser") ?: false
                        // if (isPremium) ... else ...

                        // For now, just being logged in and having a profile is enough
                        fetchPremiumPdf(context, song)
                    } else {
                        Toast.makeText(context, "User profile not found.", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(context, "Error verifying account.", Toast.LENGTH_SHORT).show()
                }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun fetchPremiumPdf(context: Context, song: Song) {
        Toast.makeText(context, "Verifying Premium Access...", Toast.LENGTH_SHORT).show()

        // Step 1: Get the arrangement ID first
        FirebaseFirestore.getInstance()
            .collection("songs").document(song.id).collection("arrangements")
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    // For now, we take the first arrangement found
                    val arrangementDoc = documents.documents[0]
                    val arrangementId = arrangementDoc.id
                    
                    // Step 2: Fetch the secure link from the nested subcollection
                    // Path: songs/{songId}/arrangements/{arrId}/secure/content
                    fetchNestedSecureLink(context, song.id, arrangementId)
                } else {
                    Toast.makeText(context, "No arrangements found for this song.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(context, "Error finding arrangements.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun fetchNestedSecureLink(context: Context, songId: String, arrangementId: String) {
        FirebaseFirestore.getInstance()
            .collection("songs").document(songId)
            .collection("arrangements").document(arrangementId)
            .collection("secure").document("content")
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    // Debugging
                    android.util.Log.d("SongHandler", "Secure Doc Data: ${document.data}")

                    var pdfUrl = document.getString("downloadLink")

                    // Fallback checks
                    if (pdfUrl.isNullOrEmpty()) {
                         pdfUrl = document.getString("link")
                    }
                    if (pdfUrl.isNullOrEmpty()) {
                        pdfUrl = document.getString("url")
                    }

                    if (!pdfUrl.isNullOrEmpty()) {
                        openPdfViewer(context, pdfUrl!!)
                    } else {
                        Toast.makeText(context, "Error: Premium link empty in database.", Toast.LENGTH_LONG).show()
                    }
                } else {
                    Toast.makeText(context, "Premium content not found (404).", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                android.util.Log.e("SongHandler", "Access Denied", e)
                Toast.makeText(context, "Access Denied. Ensure you are logged in.", Toast.LENGTH_LONG).show()
            }
    }

    private fun fetchAndOpenPdf(context: Context, song: Song) {
        Toast.makeText(context, "Opening ${song.title}...", Toast.LENGTH_SHORT).show()

        FirebaseFirestore.getInstance()
            .collection("songs").document(song.id).collection("arrangements")
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val arrangement = documents.documents[0].toObject(Arrangement::class.java)
                    val pdfUrl = arrangement?.downloadLink ?: ""

                    if (pdfUrl.isNotEmpty()) {
                        openPdfViewer(context, pdfUrl)
                    } else {
                        Toast.makeText(context, "No PDF link found", Toast.LENGTH_LONG).show()
                    }
                } else {
                    Toast.makeText(context, "No arrangements found", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(context, "Error fetching PDF", Toast.LENGTH_SHORT).show()
            }
    }

    private fun openPdfViewer(context: Context, url: String) {
        val intent = Intent(context, PdfViewerActivity::class.java)
        intent.putExtra("PDF_URL", url)
        context.startActivity(intent)
    }
}