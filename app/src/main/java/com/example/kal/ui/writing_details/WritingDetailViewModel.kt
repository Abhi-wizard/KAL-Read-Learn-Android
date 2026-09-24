package com.example.kal.ui.writing_details

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kal.data.Writing // Make sure this import is correct
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// This sealed class is unchanged, it's already correct
sealed class WritingDetailUiState {
    object Loading : WritingDetailUiState()
    data class Success(val writing: Writing) : WritingDetailUiState()
    data class Error(val message: String) : WritingDetailUiState()
}

@HiltViewModel
class WritingDetailViewModel @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow<WritingDetailUiState>(WritingDetailUiState.Loading)
    val uiState: StateFlow<WritingDetailUiState> = _uiState.asStateFlow()

    val currentUserId: String? = auth.currentUser?.uid

    // This class property is still needed for toggleLike()
    private var writingId: String? = null

    init {
        // --- THIS IS THE FIX for the 'init' block ---
        // 1. Read the ID into a local, immutable 'val'
        val idFromHandle: String? = savedStateHandle["writingId"]

        // 2. Assign it to your class property
        this.writingId = idFromHandle

        // 3. Use the local 'val' for the check and function call.
        // The compiler can now safely "smart cast" it.
        if (idFromHandle.isNullOrEmpty()) {
            _uiState.value = WritingDetailUiState.Error("Story ID not found.")
        } else {
            // This is now safe and the error is gone
            fetchWritingWithRealtimeUpdates(idFromHandle)
        }
        // --- END OF FIX ---
    }

    /**
     * 6. This function now listens for REAL-TIME updates from Firestore.
     * (This function is unchanged)
     */
    private fun fetchWritingWithRealtimeUpdates(writingId: String) {
        _uiState.value = WritingDetailUiState.Loading

        firestore.collection("writings").document(writingId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    _uiState.value = WritingDetailUiState.Error(error.message ?: "Failed to load story.")
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    val writing = snapshot.toObject(Writing::class.java)
                    if (writing != null) {
                        _uiState.value = WritingDetailUiState.Success(writing)
                    } else {
                        _uiState.value = WritingDetailUiState.Error("Failed to parse story data.")
                    }
                } else {
                    _uiState.value = WritingDetailUiState.Error("Story not found.")
                }
            }
    }

    /**
     * 7. This is the NEW function to handle liking/unliking a post.
     */
    fun toggleLike() {
        // --- THIS IS THE FIX for 'toggleLike' ---
        // 1. Create a stable, local copy of the class property
        val localWritingId = writingId

        // 2. Use the local 'val' for your checks
        if (currentUserId == null) return // Not logged in
        if (localWritingId == null) return // No writing to like

        // Get the current state to perform an "optimistic update"
        val currentState = _uiState.value
        if (currentState is WritingDetailUiState.Success) {

            val currentWriting = currentState.writing
            val shouldLike = !currentWriting.likedBy.contains(currentUserId)

            // --- OPTIMISTIC UI UPDATE (Unchanged) ---
            val newLikedBy = if (shouldLike) {
                currentWriting.likedBy + currentUserId
            } else {
                currentWriting.likedBy - currentUserId
            }
            val newLikesCount = if (shouldLike) {
                currentWriting.likesCount + 1
            } else {
                (currentWriting.likesCount - 1).coerceAtLeast(0)
            }
            val newWriting = currentWriting.copy(
                likedBy = newLikedBy,
                likesCount = newLikesCount
            )
            _uiState.value = WritingDetailUiState.Success(newWriting)
            // --- END OF OPTIMISTIC UPDATE ---


            // --- DATABASE UPDATE ---
            // 3. Use the safe, local 'localWritingId' here.
            // This avoids the dangerous '!!' crash.
            val writingRef = firestore.collection("writings").document(localWritingId)

            firestore.runTransaction { transaction ->
                if (shouldLike) {
                    transaction.update(writingRef, "likedBy", FieldValue.arrayUnion(currentUserId))
                    transaction.update(writingRef, "likesCount", FieldValue.increment(1))
                } else {
                    transaction.update(writingRef, "likedBy", FieldValue.arrayRemove(currentUserId))
                    transaction.update(writingRef, "likesCount", FieldValue.increment(-1))
                }
                null
            }.addOnFailureListener {
                _uiState.value = currentState
            }
            // --- END OF DATABASE UPDATE ---
        }
    }
}