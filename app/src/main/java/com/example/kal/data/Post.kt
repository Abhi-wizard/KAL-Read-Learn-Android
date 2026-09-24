package com.example.kal.data

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * Represents a promotional post (book advertisement) by an author.
 * This will be stored in the root "posts" collection.
 *
 * All fields have default values to make it compatible with Firestore's
 * .toObject() deserialization.
 */
data class Post(
    @DocumentId
    val id: String = "",

    // --- Author Info ---
    // (Copied from the User document at the time of posting)
    val authorId: String = "",
    val authorPenName: String = "",
    val authorProfileImageUrl: String? = null, // Optional

    // --- Book Info ---
    // (This is all entered by the author in the CreatePostScreen form)
    val bookTitle: String = "",
    val bookSynopsis: String = "",
    val bookCoverUrl: String = "", // The URL from StorageRepository
    val purchaseLink: String? = null, // Optional: The external e-commerce link

    // --- Author's Personal Message ---
    val authorComment: String = "",

    // --- Like System ---
    val likesCount: Long = 0,
    val likedBy: List<String> = emptyList(),

    // --- Timestamp ---
    @ServerTimestamp
    val createdAt: Date? = null
)