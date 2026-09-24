package com.example.kal.ui.promotions_feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kal.data.Post
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class PromotionsFeedViewModel @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _uiState = MutableStateFlow<PromotionsUiState>(PromotionsUiState.Loading)
    val uiState = _uiState.asStateFlow()

    val currentUserId: String? = auth.currentUser?.uid

    init {
        fetchPromotionsFeed()
    }

    private fun fetchPromotionsFeed() {
        viewModelScope.launch {
            _uiState.value = PromotionsUiState.Loading
            try {
                // Fetch all posts, ordered by creation time
                val snapshot = firestore.collection("posts")
                    .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .limit(50)
                    .get().await()

                val posts = snapshot.toObjects(Post::class.java)

                _uiState.value = PromotionsUiState.Success(posts)
            } catch (e: Exception) {
                _uiState.value = PromotionsUiState.Error(e.message ?: "Failed to load promotions.")
            }
        }
    }

    // --- Like Toggle Logic (Reused from Writings) ---
    fun toggleLike(postId: String) {
        val uid = currentUserId ?: return

        // Perform optimistic update on the UI
        _uiState.update { currentState ->
            if (currentState is PromotionsUiState.Success) {
                val currentPost = currentState.posts.find { it.id == postId } ?: return@update currentState
                val isLiked = currentPost.likedBy.contains(uid)

                val newLikedBy = if (isLiked) currentPost.likedBy - uid else currentPost.likedBy + uid
                val newLikesCount = if (isLiked) currentPost.likesCount - 1 else currentPost.likesCount + 1

                val updatedPost = currentPost.copy(
                    likedBy = newLikedBy,
                    likesCount = newLikesCount.coerceAtLeast(0)
                )

                val newPostsList = currentState.posts.map { if (it.id == postId) updatedPost else it }
                currentState.copy(posts = newPostsList)
            } else {
                currentState
            }
        }

        // Update Firestore in the background
        viewModelScope.launch {
            val postRef = firestore.collection("posts").document(postId)
            val isCurrentlyLiked = _uiState.value.let {
                if (it is PromotionsUiState.Success) it.posts.find { p -> p.id == postId }?.likedBy?.contains(uid) == true else false
            }

            firestore.runTransaction { transaction ->
                if (isCurrentlyLiked) {
                    transaction.update(postRef, "likedBy", FieldValue.arrayRemove(uid))
                    transaction.update(postRef, "likesCount", FieldValue.increment(-1))
                } else {
                    transaction.update(postRef, "likedBy", FieldValue.arrayUnion(uid))
                    transaction.update(postRef, "likesCount", FieldValue.increment(1))
                }
                null
            }.addOnFailureListener {
                // On failure, refresh the feed to revert the optimistic UI
                fetchPromotionsFeed()
            }
        }
    }
}