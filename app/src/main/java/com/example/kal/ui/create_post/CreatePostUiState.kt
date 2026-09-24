package com.example.kal.ui.create_post

import android.net.Uri

/**
 * Holds all the data for the CreatePostScreen UI.
 */
data class CreatePostUiState(
    // Form fields
    val penName: String = "",
    val bookTitle: String = "",
    val bookSynopsis: String = "",
    val purchaseLink: String = "",
    val authorComment: String = "",

    // URI of the image the user picked from their phone
    val selectedImageUri: Uri? = null,

    // State properties
    val isLoading: Boolean = false,
    val isPostSuccessful: Boolean = false,
    val error: String? = null
)