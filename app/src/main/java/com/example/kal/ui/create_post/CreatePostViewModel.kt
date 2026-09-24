package com.example.kal.ui.create_post

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kal.data.Post
import com.example.kal.data.User
import com.example.kal.data.repository.StorageRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class CreatePostViewModel @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val storageRepository: StorageRepository // Injected from Step 0
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreatePostUiState())
    val uiState = _uiState.asStateFlow()

    init {
        // Pre-fill the author's pen name
        fetchAuthorPenName()
    }

    // --- 1. Form Field Update Functions ---
    fun onPenNameChange(name: String) {
        _uiState.update { it.copy(penName = name, error = null) }
    }
    fun onTitleChange(title: String) {
        _uiState.update { it.copy(bookTitle = title, error = null) }
    }
    fun onSynopsisChange(synopsis: String) {
        _uiState.update { it.copy(bookSynopsis = synopsis, error = null) }
    }
    fun onLinkChange(link: String) {
        _uiState.update { it.copy(purchaseLink = link, error = null) }
    }
    fun onCommentChange(comment: String) {
        _uiState.update { it.copy(authorComment = comment, error = null) }
    }
    fun onImageSelected(uri: Uri?) {
        _uiState.update { it.copy(selectedImageUri = uri, error = null) }
    }

    // --- 2. Data Fetching ---
    private fun fetchAuthorPenName() {
        viewModelScope.launch {
            try {
                val userId = auth.currentUser?.uid ?: return@launch
                val userDoc = firestore.collection("users").document(userId).get().await()
                val penName = userDoc.toObject(User::class.java)?.name ?: ""
                _uiState.update { it.copy(penName = penName) }
            } catch (e: Exception) {
                // Can ignore, user can type manually
            }
        }
    }

    // --- 3. Main Publish Logic ---
    fun publishPost() {
        val state = _uiState.value
        // Validate inputs
        if (state.selectedImageUri == null) {
            _uiState.update { it.copy(error = "Please upload a book cover image.") }
            return
        }
        if (state.penName.isBlank() || state.bookTitle.isBlank() || state.bookSynopsis.isBlank() || state.authorComment.isBlank()) {
            _uiState.update { it.copy(error = "Please fill in all required fields.") }
            return
        }

        _uiState.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            try {
                val userId = auth.currentUser?.uid ?: throw Exception("No user logged in")

                // --- Step A: Upload Image ---
                val imageUrlResult = storageRepository.uploadImage(
                    imageUri = state.selectedImageUri,
                    folder = "post_covers"
                )
                val imageUrl = imageUrlResult.getOrThrow() // Get URL or throw exception

                // --- Step B: Get Author Profile Pic (Optional) ---
                val user = firestore.collection("users").document(userId).get().await()
                    .toObject(User::class.java)

                // --- Step C: Create Post Object ---
                val newPost = Post(
                    authorId = userId,
                    authorPenName = state.penName.trim(),
                    authorProfileImageUrl = user?.profileImageUrl, // Get from user doc
                    bookTitle = state.bookTitle.trim(),
                    bookSynopsis = state.bookSynopsis.trim(),
                    bookCoverUrl = imageUrl, // The URL from Firebase Storage
                    purchaseLink = state.purchaseLink.trim().takeIf { it.isNotBlank() },
                    authorComment = state.authorComment.trim()
                )

                // --- Step D: Save to Firestore ---
                firestore.collection("posts").add(newPost).await()

                _uiState.update { it.copy(isLoading = false, isPostSuccessful = true) }

            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Failed to publish post.") }
            }
        }
    }
}