package com.example.kal.data

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class User(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val phoneNumber: String = "",
    val dob: String = "",
    val profileImageUrl: String? = null,
    val totalPoints: Int = 0,
    val isWritingUnlocked: Boolean = false,
    val role: String = "User",

    // --- ADD THIS NEW LINE ---
    val recentlyReadBookId: String? = null, // For the profile "resume" button
    val bio: String = "", // For the author's profile

    @ServerTimestamp
    val createdAt: Date? = null
)