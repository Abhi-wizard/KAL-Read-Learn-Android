package com.example.kal.ui.my_posts



import androidx.compose.foundation.layout.*

import androidx.compose.foundation.lazy.LazyColumn

import androidx.compose.foundation.lazy.items

import androidx.compose.material.icons.Icons

import androidx.compose.material.icons.automirrored.filled.ArrowBack

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

import com.example.kal.data.Post

import com.example.kal.ui.promotions_feed.composables.PostCard // Reusing the PostCard UI



@OptIn(ExperimentalMaterial3Api::class)

@Composable

fun MyPostsScreen(

    viewModel: MyPostsViewModel = hiltViewModel(),

    onNavigateBack: () -> Unit

) {

    val uiState by viewModel.uiState.collectAsState()



    Scaffold(

        topBar = {

            TopAppBar(

                title = { Text("My Promotions") },

                navigationIcon = {

                    IconButton(onClick = onNavigateBack) {

                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")

                    }

                }

            )

        }

    ) { paddingValues ->

        when (val state = uiState) {

            is MyPostsUiState.Loading -> {

                Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {

                    CircularProgressIndicator()

                }

            }

            is MyPostsUiState.Error -> {

                Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {

                    Text(text = state.message, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp))

                }

            }

            is MyPostsUiState.Success -> {

                LazyColumn(

                    modifier = Modifier

                        .fillMaxSize()

                        .padding(paddingValues),

                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),

                    verticalArrangement = Arrangement.spacedBy(16.dp)

                ) {

                    if (state.posts.isEmpty()) {

                        item {

                            Box(

                                modifier = Modifier.fillMaxWidth().padding(top = 64.dp),

                                contentAlignment = Alignment.Center

                            ) {

                                Text(

                                    // FIX APPLIED HERE: Explicitly using the 'text' parameter

                                    text = "You haven't published any ads yet.",

                                    textAlign = TextAlign.Center, // Use TextAlign

                                    style = MaterialTheme.typography.titleMedium

                                )

                            }

                        }

                    } else {

                        items(state.posts, key = { it.id }) { post ->

                            MyPostManagementCard(

                                post = post,

                                // TODO: Implement onDelete and onEdit

                                onDelete = { /* viewModel.deletePost(post.id) */ },

                                onEdit = { /* Navigate to CreatePostScreen with post data */ }

                            )

                        }

                    }

                }

            }

        }

    }



    // --- Helper composable for management features (unchanged) ---

    @Composable

    fun MyPostManagementCard(

        post: Post,

        onDelete: () -> Unit,

        onEdit: () -> Unit

    ) {

        Column {

            // Display the read-only post card first (from the PromotionsFeed files)

            PostCard(

                post = post,

                currentUserId = null, // Likes don't matter on the management view

                onLikeClick = { /* No-op */ }

            )



            // Add a row of buttons for management actions

            Row(

                modifier = Modifier

                    .fillMaxWidth()

                    .padding(horizontal = 16.dp, vertical = 8.dp),

                horizontalArrangement = Arrangement.End

            ) {

                TextButton(onClick = onEdit) {

                    Text("Edit", color = MaterialTheme.colorScheme.secondary)

                }

                Spacer(modifier = Modifier.width(8.dp))

                TextButton(onClick = onDelete) {

                    Text("Delete", color = MaterialTheme.colorScheme.error)

                }

            }

            HorizontalDivider()

        }

    }

}



@Composable

fun MyPostManagementCard(post: Post, onDelete: () -> Unit, onEdit: () -> Unit) {

    TODO("Not yet implemented")

}