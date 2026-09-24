package com.example.kal.ui.my_writings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.kal.data.local.Draft

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyWritingsScreen(
    viewModel: MyWritingsViewModel = hiltViewModel(),
    onNavigateToEditor: (draftId: Int?) -> Unit, // Pass ID to edit, null for new
    onNavigateBack: () -> Unit
) {
    // Observe the UI state from the ViewModel
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Writings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { onNavigateToEditor(null) }) { // null = new draft
                Icon(Icons.Default.Add, "New Draft")
            }
        }
    ) { paddingValues ->
        // Handle the different UI states
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            when (val state = uiState) {
                is MyWritingsUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is MyWritingsUiState.Empty -> {
                    Text(
                        text = "You have no drafts.\nTap the + button to start writing.",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                is MyWritingsUiState.Success -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(state.drafts) { draft ->
                            DraftCard(
                                draft = draft,
                                onClick = {
                                    // Pass the specific ID to edit this draft
                                    onNavigateToEditor(draft.id)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

// A simple card composable to show draft info
@Composable
fun DraftCard(draft: Draft, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = draft.title.ifEmpty { "(Untitled Draft)" },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = draft.content.ifEmpty { "(No content)" },
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}