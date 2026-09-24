package com.example.kal.data

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * This class stores a user's progress for a single book.
 * It needs default values for Firestore to work.
 *
 * This document will be stored at:
 * /users/{userId}/readingProgress/{bookId}
 */
data class ReadingProgress(
    val bookId: String = "",
    val currentPage: Int = 0,
    val totalPages: Int = 0, // We store this here to easily calculate percentages
    val progress: Float = 0f, // Store the percentage

    @ServerTimestamp
    val lastReadAt: Date? = null
)