package com.example.kal.ui.create_writing

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kal.data.local.Draft
import com.example.kal.data.local.DraftDao
import com.google.firebase.functions.FirebaseFunctions
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

// This sealed class could be expanded to show more states (e.g., "Published!")
sealed class CreateWritingUiState {
    object Idle : CreateWritingUiState()
    object SavingDraft : CreateWritingUiState()
    object Publishing : CreateWritingUiState()
}

// This sealed class is for sending one-time events to the UI (like popups)
sealed class PublishResult {
    object Success : PublishResult()
    data class Error(val message: String) : PublishResult()
}

@HiltViewModel
class CreateWritingViewModel @Inject constructor(
    private val draftDao: DraftDao,
    private val functions: FirebaseFunctions, // For calling the Cloud Function
    savedStateHandle: SavedStateHandle // Used to get navigation arguments
) : ViewModel() {

    // --- State for the UI ---
    val title = MutableStateFlow("")
    val content = MutableStateFlow("")
    val genre = MutableStateFlow("")
    val language = MutableStateFlow("")

    private val _uiState = MutableStateFlow<CreateWritingUiState>(CreateWritingUiState.Idle)
    val uiState = _uiState.asStateFlow()

    // --- New flow for sending one-time events (popups) to the UI ---
    private val _publishEvent = MutableSharedFlow<PublishResult>()
    val publishEvent = _publishEvent.asSharedFlow()

    // --- State for the draft ID ---
    private val draftId: Int = savedStateHandle["draftId"] ?: 0 // Get ID from nav
    private var currentDraftId: Int = draftId // To keep track for saving

    init {
        if (draftId != 0) {
            // This is an existing draft, load it
            loadDraft(draftId)
        } else {
            // This is a new draft
        }
    }

    private fun loadDraft(id: Int) {
        viewModelScope.launch {
            // Use the new DAO function. .firstOrNull() gets the draft one time.
            val draft = draftDao.getDraftById(id).firstOrNull()
            if (draft != null) {
                // Populate the UI with the loaded draft data
                title.value = draft.title
                content.value = draft.content
                genre.value = draft.genre
                language.value = draft.language
            }
        }
    }

    fun onSaveDraft() {
        _uiState.value = CreateWritingUiState.SavingDraft
        viewModelScope.launch {
            val draft = Draft(
                id = currentDraftId, // If 0, Room inserts. If > 0, Room replaces.
                title = title.value,
                content = content.value,
                genre = genre.value,
                language = language.value
            )
            // Save the draft and get its new ID
            val newId = draftDao.saveDraft(draft)
            currentDraftId = newId.toInt() // Update the ID for future saves

            _uiState.value = CreateWritingUiState.Idle
        }
    }

    fun onPublish() {
        // --- 1. PRE-FLIGHT CHECKS ---
        // (You should show these errors to the user in a popup)
        if (title.value.isBlank()) {
            viewModelScope.launch { _publishEvent.emit(PublishResult.Error("Title cannot be empty.")) }
            return
        }
        if (content.value.isBlank()) {
            viewModelScope.launch { _publishEvent.emit(PublishResult.Error("Content cannot be empty.")) }
            return
        }
        if (genre.value.isBlank() || language.value.isBlank()) {
            viewModelScope.launch { _publishEvent.emit(PublishResult.Error("Please select a genre and language.")) }
            return
        }

        _uiState.value = CreateWritingUiState.Publishing

        // --- 2. PREPARE DATA FOR CLOUD FUNCTION ---
        viewModelScope.launch {
            val data = hashMapOf(
                "title" to title.value,
                "content" to content.value,
                "genre" to genre.value,
                "language" to language.value
            )

            try {
                // --- 3. CALL CLOUD FUNCTION ---
                // This calls the 'publishWriting' function you deployed
                functions.getHttpsCallable("publishWriting")
                    .call(data)
                    .await()

                // --- 4. HANDLE SUCCESS ---
                _uiState.value = CreateWritingUiState.Idle

                // On success, delete the local draft
                if (currentDraftId != 0) {
                    val draftToDelete = Draft(
                        id = currentDraftId,
                        title = title.value,
                        content = content.value,
                        genre = genre.value,
                        language = language.value
                    )
                    draftDao.deleteDraft(draftToDelete)
                }

                // Send a success event to the UI (to show "Published!" popup and navigate)
                _publishEvent.emit(PublishResult.Success)

            } catch (e: Exception) {
                // --- 5. HANDLE FAILURE ---
                _uiState.value = CreateWritingUiState.Idle

                // Send an error event to the UI (e.g., "Not enough coins")
                _publishEvent.emit(PublishResult.Error(e.message ?: "Failed to publish"))
            }
        }
    }
}