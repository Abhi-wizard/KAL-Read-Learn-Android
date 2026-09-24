package com.example.kal.ui.my_writings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kal.data.local.Draft
import com.example.kal.data.local.DraftDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// 1. Define the different states your UI can be in.
// This allows you to show a loading spinner, an empty message, or the list.
sealed class MyWritingsUiState {
    object Loading : MyWritingsUiState()
    data class Success(val drafts: List<Draft>) : MyWritingsUiState()
    object Empty : MyWritingsUiState()
}

@HiltViewModel
class MyWritingsViewModel @Inject constructor(
    private val draftDao: DraftDao // Hilt provides this automatically
) : ViewModel() {

    // 2. Create a private, editable state for the ViewModel
    private val _uiState = MutableStateFlow<MyWritingsUiState>(MyWritingsUiState.Loading)

    // 3. Create a public, read-only state for the UI to observe
    val uiState: StateFlow<MyWritingsUiState> = _uiState.asStateFlow()

    init {
        // 4. Start listening for changes in the database as soon as the VM is created
        fetchMyDrafts()
    }

    private fun fetchMyDrafts() {
        viewModelScope.launch {
            // draftDao.getMyDrafts() returns a Flow.
            // .collect will listen for any change in the 'drafts' table.
            draftDao.getMyDrafts().collect { drafts ->
                if (drafts.isEmpty()) {
                    // If the list is empty, tell the UI to show an "Empty" message
                    _uiState.value = MyWritingsUiState.Empty
                } else {
                    // If there are drafts, tell the UI to show the list
                    _uiState.value = MyWritingsUiState.Success(drafts)
                }
            }
        }
    }

    // 5. Create a function for the UI to call when a draft is deleted
    // (e.g., if you add a 'swipe-to-delete' feature)
    fun deleteDraft(draft: Draft) {
        viewModelScope.launch {
            draftDao.deleteDraft(draft)
            // We don't need to manually update the state here.
            // The .collect block in fetchMyDrafts() will automatically
            // get the new, smaller list and update the UI.
        }
    }
}