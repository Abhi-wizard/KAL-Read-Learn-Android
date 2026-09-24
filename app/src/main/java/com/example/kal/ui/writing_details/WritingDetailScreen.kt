package com.example.kal.ui.writing_details

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite // <-- 1. ADD THIS IMPORT
import androidx.compose.material.icons.outlined.FavoriteBorder // <-- 2. ADD THIS IMPORT
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color // <-- 3. ADD THIS IMPORT
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WritingDetailScreen(
    viewModel: WritingDetailViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    // This title logic is correct and will update when data loads
    val title = (uiState as? WritingDetailUiState.Success)?.writing?.title ?: ""

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                is WritingDetailUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is WritingDetailUiState.Error -> {
                    Text(
                        text = state.message,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp)
                    )
                }
                is WritingDetailUiState.Success -> {
                    // --- 4. GET THE DATA NEEDED FOR THE LIKE BUTTON ---
                    val writing = state.writing
                    val currentUserId = viewModel.currentUserId // Get from ViewModel
                    val isLiked = writing.likedBy.contains(currentUserId) // Check if liked

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        item {
                            Text(
                                text = "by ${writing.authorName}",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )

                            // --- 5. ADD THE LIKE BUTTON AND COUNT ---
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                // The Like Button
                                IconButton(onClick = {
                                    // Call the ViewModel function!
                                    viewModel.toggleLike()
                                }) {
                                    Icon(
                                        imageVector = if (isLiked) {
                                            Icons.Filled.Favorite
                                        } else {
                                            Icons.Outlined.FavoriteBorder
                                        },
                                        contentDescription = "Like",
                                        tint = if (isLiked) Color.Red else Color.Gray
                                    )
                                }
                                // The Like Count Text
                                Text(
                                    text = "${writing.likesCount} likes",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.Gray
                                )
                            }
                            // --- END OF LIKE BUTTON SECTION ---

                            Spacer(modifier = Modifier.height(24.dp))
                        }

                        item {
                            Text(
                                text = writing.content,
                                style = MaterialTheme.typography.bodyLarge,
                                fontSize = 18.sp,
                                lineHeight = 28.sp
                            )
                        }
                    }
                }
            }
        }
    }
}