package com.example.kal.ui.details

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kal.data.Book
import com.example.kal.data.Review
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

// --- 1. UPDATED UI STATE ---
sealed class BookDetailsUiState {
    object Loading : BookDetailsUiState()
    data class Success(
        val book: Book,
        val allReviews: List<Review>, // All reviews for the list
        val userReview: Review?,      // The logged-in user's specific review
        val isUnlocked: Boolean      // <-- NEW: Added this
    ) : BookDetailsUiState()
    data class Error(val message: String) : BookDetailsUiState()
}

@HiltViewModel
class BookDetailsViewModel @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val functions: FirebaseFunctions, // <-- 2. INJECTED FUNCTIONS
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow<BookDetailsUiState>(BookDetailsUiState.Loading)
    val uiState = _uiState.asStateFlow()

    // --- 3. NEW STATES FOR UNLOCK PROCESS ---
    private val _isUnlocking = MutableStateFlow(false)
    val isUnlocking = _isUnlocking.asStateFlow()

    private val _unlockError = MutableStateFlow<String?>(null)
    val unlockError = _unlockError.asStateFlow()
    // ----------------------------------------

    private val bookId: String = savedStateHandle.get<String>("bookId")!!
    private val bookRef = firestore.collection("books").document(bookId)
    private val reviewsRef = bookRef.collection("reviews")
    private val currentUserId = auth.currentUser?.uid

    init {
        fetchBookDetailsAndReviews()
    }

    // --- 4. UPDATED FETCH FUNCTION ---
    private fun fetchBookDetailsAndReviews() {
        if (currentUserId == null) {
            _uiState.value = BookDetailsUiState.Error("You must be logged in to see details.")
            return
        }

        viewModelScope.launch {
            _uiState.value = BookDetailsUiState.Loading
            try {
                // 1. Fetch the main book document (No change)
                val bookDocument = bookRef.get().await()
                val book = bookDocument.toObject(Book::class.java)?.withId(bookDocument.id)
                    ?: throw IllegalStateException("Book not found")

                // 2. Fetch all reviews (No change)
                val allReviewsSnapshot = reviewsRef
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .get()
                    .await()
                val allReviews = allReviewsSnapshot.documents.mapNotNull {
                    it.toObject(Review::class.java)?.withId(it.id)
                }

                // 3. Fetch the current user's specific review (No change)
                var userReview: Review? = null
                val userReviewSnapshot = reviewsRef.document(currentUserId).get().await()
                if (userReviewSnapshot.exists()) {
                    userReview = userReviewSnapshot.toObject(Review::class.java)?.withId(userReviewSnapshot.id)
                }
                val otherReviews = allReviews.filter { it.userId != currentUserId }

                // 4. *** NEW UNLOCK LOGIC ***
                val unlockedBookRef = firestore.collection("users").document(currentUserId)
                    .collection("unlockedBooks").document(bookId)
                val unlockedBookDoc = unlockedBookRef.get().await()

                // A book is "unlocked" if its price is 0 OR if the user's unlock document exists.
                val isBookUnlocked = book.price == 0 || unlockedBookDoc.exists()
                // --- End of new logic ---

                // 5. Update UI State with the new isUnlocked value
                _uiState.value = BookDetailsUiState.Success(
                    book = book,
                    allReviews = otherReviews,
                    userReview = userReview,
                    isUnlocked = isBookUnlocked // <-- Pass the new state
                )

            } catch (e: Exception) {
                Log.e("BookDetailsVM", "Error fetching details", e)
                _uiState.value = BookDetailsUiState.Error(e.message ?: "An unknown error occurred")
            }
        }
    }

    // --- 6. NEW FUNCTION TO CALL CLOUD FUNCTION ---
    fun unlockBook() {
        // Get the current book from the UI state
        val currentBook = (_uiState.value as? BookDetailsUiState.Success)?.book ?: return

        viewModelScope.launch {
            _isUnlocking.value = true
            _unlockError.value = null
            try {
                // This is the name of your Cloud Function
                val unlockBookFunction = functions.getHttpsCallable("unlockBook")

                // This is the data we send to the function (the bookId)
                val data = hashMapOf("bookId" to currentBook.id)

                // Call the function and wait for the result
                unlockBookFunction.call(data).await()

                // If the call succeeds, update the UI state to show "Start Reading"
                _uiState.update {
                    if (it is BookDetailsUiState.Success) {
                        it.copy(isUnlocked = true)
                    } else {
                        it
                    }
                }

            } catch (e: Exception) {
                // This catches errors from the Cloud Function (e.g., "Not enough coins!")
                if (e is FirebaseFunctionsException) {
                    _unlockError.value = e.message ?: "An unknown error occurred."
                    Log.e("BookDetailsVM", "Cloud Function Error: ${e.code} - ${e.message}")
                } else {
                    _unlockError.value = e.message ?: "A local error occurred."
                    Log.e("BookDetailsVM", "Unlock Error: ${e.message}")
                }
            } finally {
                _isUnlocking.value = false
            }
        }
    }

    // --- 7. NEW HELPER TO CLEAR ERROR FROM SNACKBAR ---
    fun clearUnlockError() {
        _unlockError.value = null
    }

    // --- YOUR EXISTING REVIEW FUNCTION (NO CHANGES) ---
    fun addOrUpdateReview(rating: Float, comment: String) {
        val currentUser = auth.currentUser
        if (currentUser?.uid == null || currentUser.email == null) {
            _uiState.value = BookDetailsUiState.Error("You must be logged in to leave a review.")
            return
        }

        val review = Review(
            userId = currentUser.uid,
            userEmail = currentUser.email!!,
            rating = rating,
            comment = comment
        )

        viewModelScope.launch {
            try {
                val reviewWithTimestamp = review.copy(timestamp = FieldValue.serverTimestamp())
                reviewsRef.document(currentUser.uid).set(reviewWithTimestamp).await()
                fetchBookDetailsAndReviews()
            } catch (e: Exception) {
                Log.e("BookDetailsVM", "Error adding review", e)
            }
        }
    }
}

