package com.example.kal.ui.reading

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.request.get
import io.ktor.client.statement.readBytes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.File
import java.util.Date
import kotlin.math.max

// Data class to hold the UI state
data class ReadingUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val bookTitle: String = "",
    val authorName: String = "",
    val pdfUri: Uri? = null,
    val totalPages: Int = 0, // Total pages in the PDF
    val currentPage: Int = 0, // Current 0-indexed page number in the PDF
    val pagesReadCount: Int = 0, // Total pages "visited" (currentPage + 1)
    val showQuizButton: Boolean = false,
    val contentStartPage: Int = 0, // 0-indexed page where actual content begins
    val lastQuizPage: Int = 0 // 0-indexed last page where a quiz was taken/passed
)

class ReadingViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ReadingUiState())
    val uiState = _uiState.asStateFlow()

    private val db = Firebase.firestore
    private val auth = FirebaseAuth.getInstance()
    private val httpClient = HttpClient(Android)
    private var bookId: String? = null
    // No need for separate totalPageCount variable, use uiState.totalPages

    fun loadBook(bookId: String, context: Context) {
        Log.d("ReadingViewModel", ">>> Starting loadBook for bookId: $bookId")
        this.bookId = bookId
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Log.e("ReadingViewModel", "!!! User not logged in, cannot load book.")
            _uiState.update { it.copy(isLoading = false, error = "User not logged in") }
            return
        }
        Log.d("ReadingViewModel", "User ID: $userId")

        _uiState.update { ReadingUiState() } // Reset state for new book

        viewModelScope.launch {
            try {
                Log.d("ReadingViewModel", "Fetching Firestore documents...")
                val bookDocRef = db.collection("books").document(bookId).get().await()
                val progressDocRef = db.collection("users").document(userId)
                    .collection("readingProgress").document(bookId).get().await()
                Log.d("ReadingViewModel", "Firestore documents fetched.")

                val pdfUrl = bookDocRef.getString("pdfUrl")
                if (pdfUrl == null) {
                    Log.e("ReadingViewModel", "!!! PDF URL is null in Firestore.")
                    throw IllegalStateException("PDF URL not found.")
                }
                Log.d("ReadingViewModel", "PDF URL: $pdfUrl")

                val title = bookDocRef.getString("title") ?: "Unknown Title"
                val author = bookDocRef.getString("author") ?: "Unknown Author"

                // --- Content Start Page Logic ---
                // Firestore stores 1-based, convert to 0-based index
                val contentStartPageOneBased = bookDocRef.getLong("contentStartPage")?.toInt() ?: 1
                val contentStartPageZeroBased = max(0, contentStartPageOneBased - 1)
                // ---

                // --- Last Quiz Page Logic ---
                // Use 0-based index. Default to 0 if not set.
                val lastQuizPageZeroBased = progressDocRef.getLong("lastQuizPage")?.toInt() ?: 0
                // ---

                var initialPage = 0 // Default to first page (index 0)
                if (progressDocRef.exists()) {
                    // Get last read page (0-based)
                    initialPage = progressDocRef.getLong("lastPageRead")?.toInt() ?: 0
                    // Ensure initialPage isn't before the last quiz page (important for previous button logic)
                    initialPage = max(initialPage, lastQuizPageZeroBased)
                }
                // Ensure initial page isn't before content start page (unless it's the very first time)
                if (progressDocRef.exists()){
                    initialPage = max(initialPage, contentStartPageZeroBased)
                }


                _uiState.update {
                    it.copy(
                        bookTitle = title,
                        authorName = author,
                        currentPage = initialPage,
                        contentStartPage = contentStartPageZeroBased, // Store 0-based index
                        lastQuizPage = lastQuizPageZeroBased      // Store 0-based index
                    )
                }

                Log.d("ReadingViewModel", "Starting PDF download...")
                val file = downloadPdf(pdfUrl, bookId, context)
                Log.d("ReadingViewModel", "PDF download complete. File path: ${file.absolutePath}")

                _uiState.update {
                    it.copy(
                        pdfUri = Uri.fromFile(file),
                        isLoading = false // Loading finished AFTER setting URI
                    )
                }
                // Initial check for quiz button visibility after PDF URI is set
                checkQuizButtonVisibility(initialPage)

                Log.d("ReadingViewModel", "<<< loadBook finished successfully.")

            } catch (e: Exception) {
                Log.e("ReadingViewModel", "!!! Error during loadBook: ${e.message}", e)
                _uiState.update { it.copy(isLoading = false, error = "Failed to load book: ${e.message}") }
            }
        }
    }

    private suspend fun downloadPdf(url: String, bookId: String, context: Context): File {
        val cacheDir = context.cacheDir
        val file = File(cacheDir, "$bookId.pdf")
        // Check if file exists and has content, otherwise download
        if (!file.exists() || file.length() == 0L) {
            try {
                Log.d("ReadingViewModel", "Downloading PDF from $url to ${file.absolutePath}")
                val bytes = httpClient.get(url).readBytes()
                if (bytes.isEmpty()) {
                    throw Exception("Downloaded PDF file is empty")
                }
                file.writeBytes(bytes)
                Log.d("ReadingViewModel", "PDF downloaded successfully (${bytes.size} bytes)")
            } catch (e: Exception) {
                Log.e("ReadingViewModel", "!!! Error downloading PDF: ${e.message}", e)
                // Attempt to delete corrupted file if download failed
                if (file.exists()) {
                    file.delete()
                }
                throw e // Re-throw exception to be caught in loadBook
            }
        } else {
            Log.d("ReadingViewModel", "PDF already cached: ${file.absolutePath}")
        }
        return file
    }

    // Called when PdfRenderer provides the total page count
    fun onPdfLoaded(pageCount: Int) {
        if (pageCount <= 0) {
            Log.e("ReadingViewModel", "!!! Invalid page count received: $pageCount")
            _uiState.update { it.copy(error = "Invalid PDF: Zero pages found.") }
            return
        }
        val pagesRead = _uiState.value.currentPage + 1 // Simple count of visited pages
        _uiState.update {
            it.copy(
                totalPages = pageCount,
                pagesReadCount = pagesRead // Update initial pagesReadCount
            )
        }
        // Re-check quiz button visibility now that we have totalPages
        checkQuizButtonVisibility(_uiState.value.currentPage)
        updateProgressInFirestore() // Save initial progress
    }

    fun goToNextPage() {
        if (_uiState.value.totalPages == 0) return // Not loaded yet
        if (_uiState.value.currentPage < _uiState.value.totalPages - 1) {
            val newPage = _uiState.value.currentPage + 1
            updateCurrentPage(newPage)
        }
    }

    fun goToPreviousPage() {
        if (_uiState.value.totalPages == 0) return // Not loaded yet
        // --- FIX: Prevent going back past the last quiz page OR the content start page ---
        val lowerBound = max(_uiState.value.lastQuizPage, _uiState.value.contentStartPage)
        if (_uiState.value.currentPage > lowerBound) {
            val newPage = _uiState.value.currentPage - 1
            updateCurrentPage(newPage)
        } else {
            Log.d("ReadingViewModel", "Cannot go to previous page. Current: ${_uiState.value.currentPage}, Lower Bound: $lowerBound")
        }
    }

    // Central function to update page and related states
    private fun updateCurrentPage(newPage: Int) {
        if (_uiState.value.totalPages == 0) return // Ensure PDF is loaded

        // Clamp newPage to valid range
        val clampedNewPage = newPage.coerceIn(0, _uiState.value.totalPages - 1)

        val pagesRead = clampedNewPage + 1 // Total pages visited (simple count)

        _uiState.update {
            it.copy(
                currentPage = clampedNewPage,
                pagesReadCount = pagesRead
            )
        }
        checkQuizButtonVisibility(clampedNewPage) // Check if quiz button should show
        updateProgressInFirestore() // Save progress
    }

    // --- NEW: Central logic for quiz button visibility ---
    private fun checkQuizButtonVisibility(currentPage: Int) {
        val state = _uiState.value
        // Calculate pages read *within the actual content* since the last quiz
        val contentPagesReadSinceQuiz = max(0, currentPage - max(state.lastQuizPage, state.contentStartPage -1)) // Adjusted lower bound
        val showQuiz = contentPagesReadSinceQuiz >= 10 && currentPage < (state.totalPages -1) // Don't show on last page

        Log.d("ReadingViewModel", "Quiz Check: Current=$currentPage, LastQuiz=${state.lastQuizPage}, Start=${state.contentStartPage}, ContentRead=$contentPagesReadSinceQuiz, Show=$showQuiz")


        if (showQuiz != state.showQuizButton) {
            _uiState.update { it.copy(showQuizButton = showQuiz) }
        }
    }


    private fun updateProgressInFirestore() {
        val userId = auth.currentUser?.uid ?: return
        val bookId = this.bookId ?: return
        val state = _uiState.value // Get current state

        if (state.totalPages == 0) {
            Log.w("ReadingViewModel", "Skipping Firestore update: totalPages is 0.")
            return // Don't save progress if PDF metadata isn't loaded yet
        }

        // Calculate progress percentage based on total pages visited vs total PDF pages
        val progressPercent = if (state.totalPages > 0) {
            (state.pagesReadCount.toDouble() / state.totalPages * 100).coerceIn(0.0, 100.0)
        } else {
            0.0
        }

        val progressData = hashMapOf(
            "lastPageRead" to state.currentPage, // Save 0-based index
            "pagesReadCount" to state.pagesReadCount,
            "lastReadAt" to Date(),
            "progressPercent" to progressPercent
            // DO NOT update 'lastQuizPage' here. That's updated separately after a quiz.
        )

        Log.d("ReadingViewModel", "Updating Firestore progress: $progressData")

        db.collection("users").document(userId)
            .collection("readingProgress").document(bookId)
            .set(progressData, SetOptions.merge()) // Use merge to avoid overwriting lastQuizPage
            .addOnSuccessListener { Log.d("ReadingViewModel", "Firestore progress updated successfully.") }
            .addOnFailureListener { e ->
                Log.w("ReadingViewModel", "Failed to update progress in Firestore: ${e.message}")
            }
    }

    override fun onCleared() {
        super.onCleared()
        httpClient.close()
        Log.d("ReadingViewModel", "ViewModel cleared, HttpClient closed.")
    }
}

