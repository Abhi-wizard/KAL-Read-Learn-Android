package com.example.kal.data

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

// --- NEW DATA CLASS (Updated from your old structure) ---
data class Review(
    var id: String = "", // Document ID
    val userId: String = "",
    val userEmail: String = "", // Store email as requested
    val rating: Float = 0f,
    val comment: String = "",
    @ServerTimestamp
    val timestamp: Date? = null
) {
    // Helper function to create a copy with a new ID
    fun withId(id: String): Review {
        this.id = id
        return this
    }

    // Overloaded copy function to handle FieldValue for server-side timestamp
    fun copy(timestamp: FieldValue): Map<String, Any> {
        return mapOf(
            "userId" to userId,
            "userEmail" to userEmail,
            "rating" to rating,
            "comment" to comment,
            "timestamp" to timestamp
        )
    }
}
