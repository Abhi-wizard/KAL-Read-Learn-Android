package com.example.kal.ui.explore



import android.annotation.SuppressLint

import android.widget.Toast

import androidx.compose.foundation.clickable

import androidx.compose.foundation.layout.Arrangement

import androidx.compose.foundation.layout.Box

import androidx.compose.foundation.layout.Column

import androidx.compose.foundation.layout.PaddingValues

import androidx.compose.foundation.layout.Row

import androidx.compose.foundation.layout.Spacer

import androidx.compose.foundation.layout.fillMaxSize

import androidx.compose.foundation.layout.fillMaxWidth

import androidx.compose.foundation.layout.height

import androidx.compose.foundation.layout.padding

import androidx.compose.foundation.layout.size

import androidx.compose.foundation.lazy.LazyColumn

import androidx.compose.foundation.lazy.LazyRow

import androidx.compose.foundation.lazy.items

import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.material.icons.Icons

import androidx.compose.material.icons.automirrored.filled.ArrowBack

import androidx.compose.material.icons.filled.Edit

import androidx.compose.material.icons.filled.Favorite

import androidx.compose.material.icons.outlined.FavoriteBorder

import androidx.compose.material3.AlertDialog

import androidx.compose.material3.Button

import androidx.compose.material3.Card

import androidx.compose.material3.CardDefaults

import androidx.compose.material3.CenterAlignedTopAppBar

import androidx.compose.material3.CircularProgressIndicator

import androidx.compose.material3.DropdownMenu

import androidx.compose.material3.DropdownMenuItem

import androidx.compose.material3.ExperimentalMaterial3Api

import androidx.compose.material3.FilterChip

import androidx.compose.material3.FloatingActionButton

import androidx.compose.material3.Icon

import androidx.compose.material3.IconButton

import androidx.compose.material3.MaterialTheme

import androidx.compose.material3.Scaffold

import androidx.compose.material3.SuggestionChip

import androidx.compose.material3.Text

import androidx.compose.material3.TextButton

import androidx.compose.material3.Tab

import androidx.compose.material3.TabRow // <-- ADD THIS IMPORT

import androidx.compose.runtime.Composable

import androidx.compose.runtime.LaunchedEffect

import androidx.compose.runtime.collectAsState

import androidx.compose.runtime.getValue

import androidx.compose.runtime.mutableStateOf

import androidx.compose.runtime.remember

import androidx.compose.runtime.setValue

import androidx.compose.ui.Alignment

import androidx.compose.ui.Modifier

import androidx.compose.ui.graphics.Color

import androidx.compose.ui.platform.LocalContext

import androidx.compose.ui.text.font.FontWeight

import androidx.compose.ui.text.style.TextAlign

import androidx.compose.ui.text.style.TextOverflow

import androidx.compose.ui.unit.dp

import androidx.hilt.navigation.compose.hiltViewModel

import com.example.kal.data.Writing

import com.example.kal.ui.promotions_feed.PromotionsFeedScreen // <-- ADD THIS IMPORT



// Define the two tabs

private enum class ExploreTab(val title: String) {

    Writings("Writings"),

    Promotions("Promotions")

}



@OptIn(ExperimentalMaterial3Api::class)

@Composable

fun ExploreScreen(

    viewModel: ExploreViewModel = hiltViewModel(),

    onNavigateToDetail: (writingId: String) -> Unit,

    onNavigateToEditor: () -> Unit,

    onNavigateBack: () -> Unit

) {

    val uiState by viewModel.uiState.collectAsState()

    val isUnlocking by viewModel.isUnlocking

    val context = LocalContext.current

    var showConfirmDialog by remember { mutableStateOf(false) }

    var selectedTab by remember { mutableStateOf(ExploreTab.Writings) } // <-- NEW STATE



    LaunchedEffect(Unit) {

        viewModel.unlockEvent.collect { event ->

            when (event) {

                is UnlockEvent.Success -> { onNavigateToEditor() }

                is UnlockEvent.Error -> { Toast.makeText(context, event.message, Toast.LENGTH_LONG).show() }

                is UnlockEvent.ShowConfirmation -> { showConfirmDialog = true }

            }

        }

    }



    if (showConfirmDialog) {

// ... AlertDialog logic (unchanged) ...

        AlertDialog(

            onDismissRequest = { showConfirmDialog = false },

            title = { Text("Unlock Writing Area?") },

            text = { Text("This is a one-time cost of 30 coins to unlock the ability to write and save drafts.") },

            confirmButton = {

                Button(

                    onClick = {

                        showConfirmDialog = false

                        viewModel.confirmUnlock()

                    }

                ) { Text("Unlock for 30 Coins") }

            },

            dismissButton = {

                TextButton(onClick = { showConfirmDialog = false }) {

                    Text("Cancel")

                }

            }

        )

    }



    Scaffold(

        topBar = {

            CenterAlignedTopAppBar(

                title = { Text("Explore", fontWeight = FontWeight.Bold) },

                navigationIcon = {

                    IconButton(onClick = onNavigateBack) {

                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")

                    }

                }

            )

        },

        floatingActionButton = {

// Only show FAB on the Writings tab

            if (selectedTab == ExploreTab.Writings) {

                FloatingActionButton(

                    onClick = {

                        if (!isUnlocking) {

                            viewModel.onStartWritingClicked()

                        }

                    },

                    containerColor = MaterialTheme.colorScheme.primary,

                    contentColor = MaterialTheme.colorScheme.onPrimary

                ) {

                    if (isUnlocking) {

                        CircularProgressIndicator(

                            modifier = Modifier.size(24.dp),

                            color = MaterialTheme.colorScheme.onPrimary

                        )

                    } else {

                        Icon(Icons.Default.Edit, "Start Writing")

                    }

                }

            }

        }

    ) { paddingValues ->

        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {



// --- 1. TAB ROW ---

            TabRow(selectedTabIndex = ExploreTab.entries.indexOf(selectedTab)) {

                ExploreTab.entries.forEach { tab ->

                    Tab(

                        selected = tab == selectedTab,

                        onClick = { selectedTab = tab },

                        text = { Text(tab.title) }

                    )

                }

            }



// --- 2. CONDITIONAL CONTENT ---

            Box(modifier = Modifier.fillMaxSize()) {

                when (selectedTab) {

                    ExploreTab.Writings -> {

                        val showSortMenu = false

                        WritingsView(

                            uiState = uiState,

                            viewModel = viewModel,

                            onNavigateToDetail = onNavigateToDetail,

                            onSortMenuStateChange = { var showSortMenu = it },

                            showSortMenu = showSortMenu

                        )

                    }

                    ExploreTab.Promotions -> {

// This is where the new promotions feed goes!

// Assuming you already created PromotionsFeedScreen.kt

                        PromotionsFeedScreen()

                    }

                }

            }

        }

    }

}





// --- EXTRACTED ORIGINAL CONTENT INTO A NEW COMPOSABLE ---

@Composable

private fun WritingsView(

    uiState: ExploreUiState,

    viewModel: ExploreViewModel,

    onNavigateToDetail: (writingId: String) -> Unit,

    onSortMenuStateChange: (Boolean) -> Unit,

    showSortMenu: Boolean

) {

    Box(modifier = Modifier.fillMaxSize()) {

        when (val state = uiState) {

            is ExploreUiState.Loading -> {

                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))

            }

            is ExploreUiState.Error -> {

                Text(

                    text = state.message,

                    color = MaterialTheme.colorScheme.error,

                    modifier = Modifier.align(Alignment.Center).padding(16.dp),

                    textAlign = TextAlign.Center

                )

            }

            is ExploreUiState.Success -> {

                LazyColumn(

                    modifier = Modifier.fillMaxSize(),

                    contentPadding = PaddingValues(vertical = 8.dp)

                ) {

// --- GENRE FILTER CHIPS ---

                    item {

                        LazyRow(

                            contentPadding = PaddingValues(horizontal = 16.dp),

                            horizontalArrangement = Arrangement.spacedBy(8.dp)

                        ) {

                            items(state.genres) { genre ->

                                FilterChip(

                                    selected = genre == state.selectedGenre,

                                    onClick = { viewModel.onGenreSelected(genre) },

                                    label = { Text(genre) }

                                )

                            }

                        }

                    }



// --- "SORT BY" HEADER ---

                    item {

                        Row(

                            modifier = Modifier

                                .fillMaxWidth()

                                .padding(horizontal = 16.dp, vertical = 16.dp),

                            verticalAlignment = Alignment.CenterVertically

                        ) {

                            Text(

                                "Stories",

                                style = MaterialTheme.typography.titleMedium,

                                fontWeight = FontWeight.Bold

                            )

                            Spacer(modifier = Modifier.weight(1f))

                            Box {

                                TextButton(onClick = { onSortMenuStateChange(true) }) {

                                    Text(state.selectedSort)

                                }

                                DropdownMenu(

                                    expanded = showSortMenu,

                                    onDismissRequest = { onSortMenuStateChange(false) }

                                ) {

                                    DropdownMenuItem(

                                        text = { Text("Popular") },

                                        onClick = {

                                            viewModel.onSortSelected("Popular")

                                            onSortMenuStateChange(false)

                                        }

                                    )

                                    DropdownMenuItem(

                                        text = { Text("Newest") },

                                        onClick = {

                                            viewModel.onSortSelected("Newest")

                                            onSortMenuStateChange(false)

                                        }

                                    )

                                }

                            }

                        }

                    }



// --- WRITINGS LIST ---

                    if (state.writings.isEmpty()) {

                        item {

                            Text(

                                text = "No stories published yet.\nBe the first!",

                                style = MaterialTheme.typography.bodyLarge,

                                color = MaterialTheme.colorScheme.onSurfaceVariant,

                                modifier = Modifier

                                    .fillMaxWidth()

                                    .padding(top = 100.dp),

                                textAlign = TextAlign.Center

                            )

                        }

                    } else {

                        items(state.writings, key = { it.id }) { writing ->

                            WritingCard(

                                writing = writing,

                                currentUserId = viewModel.currentUserId,

                                onCardClick = { onNavigateToDetail(writing.id) },

                                onLikeClick = { viewModel.toggleLike(writing.id) },

                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)

                            )

                        }

                    }

                }

            }

        }

    }

}





// --- WritingCard is UNCHANGED ---

@Composable

fun WritingCard(

    writing: Writing,

    currentUserId: String?,

    onCardClick: () -> Unit,

    onLikeClick: () -> Unit,

    modifier: Modifier = Modifier

) {

    Card(

        modifier = modifier

            .fillMaxWidth()

            .clickable(onClick = onCardClick),

        shape = RoundedCornerShape(16.dp),

        colors = CardDefaults.cardColors(

            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)

        )

    ) {

        Column(

            modifier = Modifier.padding(16.dp),

            verticalArrangement = Arrangement.spacedBy(12.dp)

        ) {

            Text(

                text = writing.title,

                style = MaterialTheme.typography.titleLarge,

                fontWeight = FontWeight.Bold

            )

            Text(

                text = writing.authorName,

                style = MaterialTheme.typography.bodyMedium,

                color = MaterialTheme.colorScheme.onSurfaceVariant

            )

            Text(

                text = writing.content,

                style = MaterialTheme.typography.bodyMedium,

                maxLines = 3,

                overflow = TextOverflow.Ellipsis

            )



// --- LIKE BUTTON LOGIC ---

            Row(

                modifier = Modifier.fillMaxWidth(),

                verticalAlignment = Alignment.CenterVertically

            ) {

                SuggestionChip(

                    onClick = { /* Not clickable */ },

                    label = { Text(writing.genre) }

                )

                Spacer(modifier = Modifier.weight(1f))



                val isLiked = writing.likedBy.contains(currentUserId)



                IconButton(onClick = onLikeClick) {

                    Icon(

                        imageVector = if (isLiked) {

                            Icons.Filled.Favorite

                        } else {

                            Icons.Outlined.FavoriteBorder

                        },

                        contentDescription = "Like",

                        tint = if (isLiked) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant,

                        modifier = Modifier.size(18.dp)

                    )

                }



                Text(

                    text = formatLikeCount(writing.likesCount),

                    style = MaterialTheme.typography.bodyMedium,

                    fontWeight = FontWeight.Bold,

                    color = MaterialTheme.colorScheme.onSurface

                )

            }

        }

    }

}



// This function is unchanged and correct

@SuppressLint("DefaultLocale")

private fun formatLikeCount(count: Long): String {

    return when {

        count < 1000 -> count.toString()

        count < 1000000 -> String.format("%.1fK", count / 1000.0)

        else -> String.format("%.1fM", count / 1000000.0)

    }

}

