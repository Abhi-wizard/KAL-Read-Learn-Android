// --- FILE: ui/wallet/WalletViewModel.kt ---
// (COPY AND PASTE THIS ENTIRE FILE)

package com.example.kal.ui.wallet

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WalletViewModel @Inject constructor() : ViewModel() {

    private val db = Firebase.firestore
    private val auth = Firebase.auth
    private val TAG = "WalletViewModel"

    // Private mutable state flow
    private val _coinBalance = MutableStateFlow(-1L) // Default to -1 (loading)
    // Public immutable state flow for the UI to observe
    val coinBalance: StateFlow<Long> = _coinBalance.asStateFlow()

    private var userListener: ListenerRegistration? = null

    init {
        // Start listening as soon as the ViewModel is created
        startListeningToUserWallet()
    }

    private fun startListeningToUserWallet() {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            // Handle error - user is not logged in
            Log.w(TAG, "User is not logged in, cannot listen to wallet.")
            _coinBalance.value = -1L // Use -1 or some error state
            return
        }

        // --- THIS IS THE FIX ---
        // We now listen to the MAIN user document, just like ProfileScreen
        val userDocRef = db.collection("users").document(uid)
        // --- END OF FIX ---

        Log.d(TAG, "Attaching listener to: ${userDocRef.path}")

        // Attach a real-time snapshot listener
        userListener = userDocRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                // Handle error
                Log.e(TAG, "Listen failed.", error)
                _coinBalance.value = -1L
                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists()) {
                // --- THIS IS THE FIX ---
                // We read the 'totalPoints' field, not 'balance'
                val balance = snapshot.getLong("totalPoints") ?: 0L
                // --- END OF FIX ---

                Log.d(TAG, "User document update received: $balance coins")
                viewModelScope.launch {
                    _coinBalance.emit(balance)
                }
            } else {
                // Document doesn't exist yet, so balance is 0
                Log.d(TAG, "User document doesn't exist. Setting balance to 0.")
                viewModelScope.launch {
                    _coinBalance.emit(0L)
                }
            }
        }
    }

    // This is crucial!
    // When the ViewModel is destroyed (e.g., user navigates away),
    // we must remove the listener to prevent memory leaks.
    override fun onCleared() {
        super.onCleared()
        userListener?.remove() // Renamed from vaultListener
        Log.d(TAG, "ViewModel cleared, user listener removed.")
    }
}