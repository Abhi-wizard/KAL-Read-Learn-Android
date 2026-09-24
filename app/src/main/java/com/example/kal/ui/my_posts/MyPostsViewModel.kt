package com.example.kal.ui.my_posts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kal.data.Post
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

// Define the UI states for this screen
sealed class MyPostsUiState {
    object Loading : MyPostsUiState()
    data class Success(val posts: List<Post>) : MyPostsUiState()
    data class Error(val message: String) : MyPostsUiState()
}

@HiltViewModel
class MyPostsViewModel @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _uiState = MutableStateFlow<MyPostsUiState>(MyPostsUiState.Loading)
    val uiState = _uiState.asStateFlow()

    init {
        fetchMyPosts()
    }

    private fun fetchMyPosts() {
        viewModelScope.launch {
            _uiState.value = MyPostsUiState.Loading
            val userId = auth.currentUser?.uid
            if (userId == null) {
                _uiState.value = MyPostsUiState.Error("Author not logged in.")
                return@launch
            }

            try {
                // --- KEY QUERY: Filter posts by the current author's ID ---
                val snapshot = firestore.collection("posts")
                    .whereEqualTo("authorId", userId)
                    .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .get().await()

                val posts = snapshot.toObjects(Post::class.java)

                _uiState.value = MyPostsUiState.Success(posts)
            } catch (e: Exception) {
                _uiState.value = MyPostsUiState.Error(e.message ?: "Failed to load your promotions.")
            }
        }
    }

    // TODO: Add deletePost(postId: String) function later
}