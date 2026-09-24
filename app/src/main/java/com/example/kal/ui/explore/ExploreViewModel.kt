package com.example.kal.ui.explore

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kal.data.User
import com.example.kal.data.Writing
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.functions.FirebaseFunctions
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject

// --- CONSOLIDATED UI STATE CLASSES (Unchanged, correct) ---

sealed class ExploreUiState {
    object Loading : ExploreUiState()
    data class Success(
        val writings: List<Writing>,
        val genres: List<String>,
        val selectedGenre: String,
        val selectedSort: String
    ) : ExploreUiState()
    data class Error(val message: String) : ExploreUiState()
}

sealed class UnlockEvent {
    object ShowConfirmation : UnlockEvent()
    object Success : UnlockEvent()
    data class Error(val message: String) : UnlockEvent()
}

// --- NEW INTERNAL STATE TO MANAGE FILTERS (For robust filtering) ---
data class FilterState(
    val genre: String = "All",
    val sort: String = "Popular"
)

// --- CONSOLIDATED VIEWMODEL ---

@HiltViewModel
class ExploreViewModel @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val functions: FirebaseFunctions
) : ViewModel() {

    // --- FIX 1: Corrected the full list of genres ---
    private val allGenres = listOf("All", "Short story", "Poetry", "Novels", "Essays")

    // Internal state that holds the current filter parameters
    private val _filterState = MutableStateFlow(FilterState())

    // UI State combines data and filter parameters for the screen
    private val _uiState = MutableStateFlow<ExploreUiState>(ExploreUiState.Loading)
    val uiState = _uiState.asStateFlow()

    private val _unlockEvent = MutableSharedFlow<UnlockEvent>()
    val unlockEvent = _unlockEvent.asSharedFlow()

    val isUnlocking = mutableStateOf(false)
    val currentUserId: String? = auth.currentUser?.uid

    private var snapshotListener: com.google.firebase.firestore.ListenerRegistration? = null

    init {
        fetchWritingsWithRealtimeUpdates()
    }

    override fun onCleared() {
        super.onCleared()
        snapshotListener?.remove()
    }

    // --- REAL-TIME LISTENER FUNCTION (Handles filtering and reading data) ---
    private fun fetchWritingsWithRealtimeUpdates() {
        viewModelScope.launch {
            _filterState
                .collect { filter ->
                    _uiState.value = ExploreUiState.Loading

                    var query: Query = firestore.collection("writings")
                        .whereEqualTo("status", "published")

                    if (filter.genre != "All") {
                        query = query.whereEqualTo("genre", filter.genre)
                    }

                    snapshotListener?.remove()

                    snapshotListener = query.limit(50).addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            _uiState.value = ExploreUiState.Error(error.message ?: "Failed to load writings.")
                            return@addSnapshotListener
                        }

                        if (snapshot != null) {
                            var writings = snapshot.toObjects(Writing::class.java)

                            writings = if (filter.sort == "Popular") {
                                writings.sortedByDescending { it.likesCount }
                            } else {
                                writings.sortedByDescending { it.createdAt ?: Date(0) }
                            }

                            _uiState.value = ExploreUiState.Success(
                                writings = writings,
                                genres = allGenres,
                                selectedGenre = filter.genre,
                                selectedSort = filter.sort
                            )
                        }
                    }
                }
        }
    }

    fun onGenreSelected(genre: String) {
        _filterState.update { it.copy(genre = genre) }
    }

    fun onSortSelected(sort: String) {
        _filterState.update { it.copy(sort = sort) }
    }

    // --- toggleLike FUNCTION (Unchanged, correct) ---
    fun toggleLike(writingId: String) {
        if (currentUserId == null) return

        val currentState = _uiState.value
        if (currentState is ExploreUiState.Success) {
            val writingToUpdate = currentState.writings.find { it.id == writingId } ?: return

            val shouldLike = !writingToUpdate.likedBy.contains(currentUserId)

            // OPTIMISTIC UI UPDATE
            val newLikedBy = if (shouldLike) writingToUpdate.likedBy + currentUserId else writingToUpdate.likedBy - currentUserId
            val newLikesCount = if (shouldLike) writingToUpdate.likesCount + 1 else (writingToUpdate.likesCount - 1).coerceAtLeast(0)

            val updatedWriting = writingToUpdate.copy(
                likedBy = newLikedBy,
                likesCount = newLikesCount
            )
            val newWritingsList = currentState.writings.map { if (it.id == writingId) updatedWriting else it }

            _uiState.value = currentState.copy(writings = newWritingsList)

            // DATABASE UPDATE
            viewModelScope.launch {
                val writingRef = firestore.collection("writings").document(writingId)

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
                    Log.e("ExploreVM", "Like transaction failed, reverting UI.", it)
                    onSortSelected(_filterState.value.sort) // Revert by forcing a refresh
                }
            }
        }
    }

    // --- FIX FOR 30 COINS RE-PROMPT (PERMANENT ACCESS LOGIC) ---
    // Inside ExploreViewModel.kt

// ... (other functions) ...

    // --- FIX FOR 30 COINS RE-PROMPT (PERMANENT ACCESS LOGIC) ---
    fun onStartWritingClicked() {
        viewModelScope.launch {
            val uid = auth.currentUser?.uid
            if (uid == null) {
                _unlockEvent.emit(UnlockEvent.Error("You must be logged in to write."))
                return@launch
            }

            try {
                isUnlocking.value = true

                // --- FIX: FORCE FRESH READ OF USER DOCUMENT ---
                val userDoc = firestore.collection("users").document(uid).get(com.google.firebase.firestore.Source.SERVER).await()
                val user = userDoc.toObject(User::class.java)

                if (user == null) {
                    throw Exception("User document not found.")
                }

                // 1. PERMANENT ACCESS CHECK (30 coins)
                if (user.isWritingUnlocked) {
                    _unlockEvent.emit(UnlockEvent.Success)
                } else {
                    // 2. ONE-TIME PAYMENT LOGIC
                    val coinBalance = user.totalPoints

                    if (coinBalance < 30) {
                        _unlockEvent.emit(UnlockEvent.Error("You need 30 coins to unlock this feature."))
                    } else {
                        _unlockEvent.emit(UnlockEvent.ShowConfirmation)
                    }
                }
            } catch (e: Exception) {
                Log.e("ExploreVM", "Failed to fetch unlock status from network.", e)
                _unlockEvent.emit(UnlockEvent.Error("Connection error or failed to load status."))
            } finally {
                isUnlocking.value = false
            }
        }
    }
    // ... (confirmUnlock remains the same) ...
    fun confirmUnlock() {
        viewModelScope.launch {
            isUnlocking.value = true
            try {
                // Call the Cloud Function to deduct 30 coins and set isWritingUnlocked: true
                functions.getHttpsCallable("unlockWritingFeature")
                    .call()
                    .await()

                _unlockEvent.emit(UnlockEvent.Success)

                // Force a refresh so the Explore screen knows the user is now unlocked
                onSortSelected(_filterState.value.sort)

            } catch (e: Exception) {
                _unlockEvent.emit(UnlockEvent.Error(e.message ?: "Unlock failed."))
            } finally {
                isUnlocking.value = false
            }
        }
    }
}

