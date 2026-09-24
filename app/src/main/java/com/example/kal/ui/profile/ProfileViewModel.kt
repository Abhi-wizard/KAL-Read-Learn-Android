package com.example.kal.ui.profile

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kal.data.Book
import com.example.kal.data.ReadingProgress
import com.example.kal.data.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source // <-- ADD IMPORT
import com.google.firebase.functions.FirebaseFunctions
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

// Represents the different states the UI can be in (Unchanged, correct)
sealed class ProfileUiState {
    object Loading : ProfileUiState()
    data class Success(
        val user: User,
        val recentBookTitle: String?,
        val recentBookProgress: Float?
    ) : ProfileUiState()
    data class Error(val message: String) : ProfileUiState()
}

// Represents the result of the unlock attempt (Unchanged, correct)
sealed class UnlockEvent {
    object ShowConfirmation : UnlockEvent()
    object Success : UnlockEvent()
    data class Error(val message: String) : UnlockEvent()
}


@HiltViewModel
class ProfileScreenViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val functions: FirebaseFunctions
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val uiState = _uiState.asStateFlow()

    val isUnlocking = mutableStateOf(false)
    private val _unlockEvent = MutableSharedFlow<UnlockEvent>()
    val unlockEvent = _unlockEvent.asSharedFlow()

    init {
        fetchUserProfile()
    }

    // --- UPDATED: Force fetch from server if needed ---
    fun fetchUserProfile(source: Source = Source.DEFAULT) {
        viewModelScope.launch {
            _uiState.value = ProfileUiState.Loading
            try {
                val userId = auth.currentUser?.uid ?: throw Exception("User not logged in")

                // 1. GET THE USER, explicitly specifying the source if required
                // Source.DEFAULT uses cache, Source.SERVER forces network read.
                val userDoc = firestore.collection("users").document(userId).get(source).await()
                val user = userDoc.toObject(User::class.java)
                    ?: throw Exception("User profile not found.")

                // 2. GET RECENT BOOK DATA (unchanged logic, removed for brevity)
                var recentBookTitle: String? = null
                var recentBookProgress: Float? = null

                if (user.recentlyReadBookId != null) {
                    try {
                        val bookDoc = firestore.collection("books").document(user.recentlyReadBookId).get().await()
                        recentBookTitle = bookDoc.toObject(Book::class.java)?.title

                        val progressDoc = firestore.collection("users").document(userId)
                            .collection("readingProgress").document(user.recentlyReadBookId).get().await()

                        val progress = progressDoc.toObject(ReadingProgress::class.java)

                        if (progress != null && progress.totalPages > 0) {
                            recentBookProgress = progress.currentPage.toFloat() / progress.totalPages.toFloat()
                        }
                    } catch (e: Exception) {
                        Log.w("ProfileVM", "Failed to fetch recent book details", e)
                    }
                }

                // 3. EMIT THE FINAL STATE
                _uiState.value = ProfileUiState.Success(
                    user = user,
                    recentBookTitle = recentBookTitle,
                    recentBookProgress = recentBookProgress
                )

            } catch (e: Exception) {
                _uiState.value = ProfileUiState.Error(e.message ?: "Failed to fetch profile.")
            }
        }
    }

    // --- onMyWritingsClicked (FIX: Use Source.SERVER when checking permissions) ---
    // Inside ProfileScreenViewModel.kt

// ... (fetchUserProfile remains the same) ...

    // --- FIX: The final, most aggressive fix for cache issues ---
    fun onMyWritingsClicked() {
        viewModelScope.launch {
            val userId = auth.currentUser?.uid
            if (userId == null) {
                _unlockEvent.emit(UnlockEvent.Error("User not logged in."))
                return@launch
            }

            try {
                // CRITICAL FIX: Use the Source.SERVER flag, but wrap it to isolate error
                val userDoc = firestore.collection("users").document(userId).get(com.google.firebase.firestore.Source.SERVER).await()
                val user = userDoc.toObject(User::class.java)
                    ?: throw Exception("User data not found.")

                if (user.isWritingUnlocked) {
                    // If it successfully reads TRUE, we navigate immediately.
                    _unlockEvent.emit(UnlockEvent.Success)
                } else {
                    // If it still reads FALSE, proceed with the payment flow.
                    val coinBalance = user.totalPoints

                    if (coinBalance < 30) {
                        _unlockEvent.emit(UnlockEvent.Error("You need 30 coins to unlock this feature."))
                    } else {
                        _unlockEvent.emit(UnlockEvent.ShowConfirmation)
                    }
                }
            } catch (e: Exception) {
                // This 'catch' is the key: it shows that the network fetch itself failed.
                // If the app is offline or the connection is bad, it will ask for coins.
                Log.e("ProfileVM", "Failed to fetch user status from network.", e)
                _unlockEvent.emit(UnlockEvent.Error("Connection error. Try again."))
            }
        }
    }
// ... (confirmUnlock remains the same) ...

    // --- confirmUnlock (FIX: Ensure profile UI is updated with fresh data) ---
    fun confirmUnlock() {
        viewModelScope.launch {
            isUnlocking.value = true
            try {
                functions.getHttpsCallable("unlockWritingFeature")
                    .call()
                    .await()

                _unlockEvent.emit(UnlockEvent.Success)

                // CRITICAL FIX: Force the profile UI to update by fetching from the network
                fetchUserProfile(source = Source.SERVER)

            } catch (e: Exception) {
                _unlockEvent.emit(UnlockEvent.Error(e.message ?: "Unlock failed."))
            } finally {
                isUnlocking.value = false
            }
        }
    }
}