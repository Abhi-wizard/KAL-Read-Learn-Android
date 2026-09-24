package com.example.kal.data

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * This data class represents a Writing document in your Firestore database.
 * The field names here MUST EXACTLY match the field names in Firestore.
 */
data class Writing(
    @DocumentId // This annotation tells Firestore to map the document's ID to this field
    val id: String = "",
    val uid: String = "",
    val authorName: String = "",
    val title: String = "",
    val content: String = "",
    val genre: String = "",
    val language: String = "",
    val status: String = "published",

    // --- Fields for the Like System ---

    // 1. This name must match the field name in your ViewModel functions
    val likesCount: Long = 0,

    // 2. This is the new field you were missing.
    // It stores a list of user UIDs who have liked the post.
    val likedBy: List<String> = emptyList(),

    @ServerTimestamp // This tells Firestore to automatically set the creation time
    val createdAt: Date? = null
)