package com.example.kal.ui.main

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kal.data.Book
import com.example.kal.data.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.FlowPreview // <-- Import needed for debounce
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

// Separate class to manage the list of all books fetched once
data class HomeData(
    val allBooks: List<Book> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@OptIn(FlowPreview::class)
class HomeViewModel : ViewModel() {

    private val db: FirebaseFirestore = Firebase.firestore
    private val auth: FirebaseAuth = Firebase.auth

    // --- Core Data & State ---
    private val _userRole = MutableStateFlow<String?>("User") // Initial assumption
    val userRole = _userRole.asStateFlow()

    private val _searchQuery = MutableStateFlow("") // State for user input
    val searchQuery = _searchQuery.asStateFlow()

    private val _homeData = MutableStateFlow(HomeData(isLoading = true))

    // --- COMBINED & FILTERED STATE (Final output for UI) ---
    // This flow combines the raw book list with the search query to produce the final filtered list.
    val booksByGenre = _homeData.combine(_searchQuery.debounce(300)) { data, query ->
        if (data.isLoading) return@combine emptyMap() // Don't filter if loading

        val filteredList = if (query.isBlank()) {
            data.allBooks
        } else {
            data.allBooks.filter { book ->
                book.title.contains(query, ignoreCase = true) ||
                        book.author.contains(query, ignoreCase = true)
            }
        }
        filteredList.groupBy { it.genre }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyMap()
    )

    val isLoadingFlow = _homeData.asStateFlow()

    init {
        fetchUserRole()
        fetchBooks()
    }

    // --- User Role Fetching ---
    private fun fetchUserRole() {
        viewModelScope.launch {
            try {
                val uid = auth.currentUser?.uid ?: return@launch
                val userDoc = db.collection("users").document(uid).get().await()
                val user = userDoc.toObject(User::class.java)
                _userRole.value = user?.role ?: "User"
            } catch (e: Exception) {
                _userRole.value = "User"
            }
        }
    }

    // --- Book Fetching (Fetches all books once) ---
    private fun fetchBooks() {
        _homeData.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            try {
                // Fetch all books for local filtering/searching
                val snapshot = db.collection("books").get().await()
                val books = snapshot.toObjects(Book::class.java)

                _homeData.update {
                    it.copy(
                        allBooks = books,
                        isLoading = false,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _homeData.update {
                    it.copy(
                        isLoading = false,
                        error = "Failed to fetch books: ${e.message}"
                    )
                }
            }
        }
    }

    // --- Function to handle search input from the UI ---
    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }
}