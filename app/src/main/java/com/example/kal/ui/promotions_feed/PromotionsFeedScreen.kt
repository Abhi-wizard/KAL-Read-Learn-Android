package com.example.kal.ui.promotions_feed

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.kal.ui.promotions_feed.composables.PostCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PromotionsFeedScreen(
    viewModel: PromotionsFeedViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Promotions Feed", fontWeight = FontWeight.Bold) }
            )
        }
    ) { paddingValues ->
        when (val state = uiState) {
            is PromotionsUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is PromotionsUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                    Text(text = state.message, color = MaterialTheme.colorScheme.error)
                }
            }
            is PromotionsUiState.Success -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (state.posts.isEmpty()) {
                        item {
                            // --- FIX APPLIED HERE ---
                            // Wrap the Text in a Box/Column to resolve the ambiguous candidate error
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 64.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No new book promotions. Check back later!",
                                    // Set textAlign inside the Text scope
                                    textAlign = TextAlign.Center
                                )
                            }
                            // --- END OF FIX ---
                        }
                    } else {
                        items(state.posts, key = { it.id }) { post ->
                            PostCard(
                                post = post,
                                currentUserId = viewModel.currentUserId,
                                onLikeClick = viewModel::toggleLike
                            )
                        }
                    }
                }
            }
        }
    }
}

