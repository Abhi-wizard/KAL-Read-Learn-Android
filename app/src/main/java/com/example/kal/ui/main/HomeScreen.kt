package com.example.kal.ui.main

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import coil.compose.AsyncImage
import com.example.kal.R
import com.example.kal.data.Book
import com.example.kal.ui.Screen


@Composable
fun HomeScreen(onBookClick: (String) -> Unit, navController: NavHostController) {

    val viewModel: HomeViewModel = hiltViewModel()
    val userRole by viewModel.userRole.collectAsState()

    // --- FIX 1: Collect the HomeData flow for loading and error states ---
    val homeDataState by viewModel.isLoadingFlow.collectAsState()

    // Extract state properties directly from the collected flow value
    val isLoading = homeDataState.isLoading
    val error = homeDataState.error

    val searchQuery by viewModel.searchQuery.collectAsState()
    val booksByGenre by viewModel.booksByGenre.collectAsState()

    // --- 2. ADD THE "BOUNCE" LOGIC (Unchanged) ---
    LaunchedEffect(userRole) {
        if (userRole == "Author") {
            navController.navigate(Screen.CreatePost.route) {
                popUpTo(Screen.Home.route) { inclusive = true }
            }
        }
    }

    // --- 3. WRAP THE UI IN THE ROLE CHECK ---
    if (userRole != "Author") {
        Scaffold(
            topBar = {
                KalTopAppBar()
            },
            floatingActionButton = {
                // FAB section remains empty
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // --- 4. WORKING SEARCH BAR ---
                WorkingSearchBar(
                    query = searchQuery,
                    onQueryChange = viewModel::onSearchQueryChange,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))

                // --- 5. LOADING/ERROR/CONTENT ---
                when {
                    isLoading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }

                    error != null -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Error: $error")
                        }
                    }

                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(items = booksByGenre.entries.toList()) { entry ->
                                val genre = entry.key
                                val books = entry.value
                                GenreCarousel(
                                    genre = genre,
                                    books = books,
                                    onBookClick = onBookClick,
                                    onSeeAllClick = { /* TODO: Navigate to Genre List */ }
                                )
                            }
                        }
                    }
                }
            }
        }
    } else {
        // --- 6. SHOW LOADER WHILE REDIRECTING AUTHOR ---
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    }
}

// --- MODIFIED: KalTopAppBar now includes "KAL" name (Unchanged) ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KalTopAppBar() {
    CenterAlignedTopAppBar(
        title = {
            Text(
                "KAL",
                fontWeight = FontWeight.ExtraBold,
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary
            )
        },
    )
}

// --- NEW: Working Search Bar Composable (Unchanged) ---
@Composable
fun WorkingSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        label = { Text("Search Books or Authors") },
        leadingIcon = {
            Icon(Icons.Default.Search, contentDescription = "Search Icon")
        },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.fillMaxWidth()
    )
}

// --- GenreCarousel (UNCHANGED) ---
@Composable
fun GenreCarousel(
    genre: String,
    books: List<Book>,
    onBookClick: (String) -> Unit,
    onSeeAllClick: (String) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = genre, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.weight(1f))
            TextButton(onClick = { onSeeAllClick(genre) }) {
                Text("See All")
            }
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(books) { book ->
                BookCard(book = book, onBookClick = onBookClick)
            }
        }
    }
}

// --- BookCard (UNCHANGED) ---
@Composable
fun BookCard(book: Book, onBookClick: (String) -> Unit) {
    Card(
        modifier = Modifier
            .width(140.dp)
            .clickable { onBookClick(book.id) },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column {
            AsyncImage(
                model = book.coverImageUrl,
                contentDescription = book.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                contentScale = ContentScale.Crop,
                error = painterResource(id = R.drawable.ic_launcher_background)
            )
            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = book.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = book.author,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}