package com.example.kal.ui.details

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock // <-- 1. NEW IMPORT
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.kal.data.Book
import com.example.kal.data.Review
import com.example.kal.ui.Screen // <-- Make sure this is imported
import com.example.kal.ui.navigation.AppBottomNavBar
import kotlinx.coroutines.launch // <-- 2. NEW IMPORT

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookDetailsScreen(
    navController: NavController,
    viewModel: BookDetailsViewModel = hiltViewModel(),
    onStartReading: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    // --- 3. NEW STATES FOR UNLOCK FLOW ---
    val isUnlocking by viewModel.isUnlocking.collectAsState()
    val unlockError by viewModel.unlockError.collectAsState()
    var showUnlockDialog by remember { mutableStateOf(false) }

    // --- 4. NEW SNACKBAR SETUP ---
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(unlockError) {
        unlockError?.let {
            scope.launch {
                snackbarHostState.showSnackbar(
                    message = it,
                    actionLabel = "OK"
                )
                viewModel.clearUnlockError() // Clear error after showing it
            }
        }
    }

    Scaffold(
        // --- 5. ADD SNACKBAR HOST ---
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Book Details") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        },
        bottomBar = {
            AppBottomNavBar(navController = navController)
        }
    ) { paddingValues ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues) // Apply padding from scaffold
        ) {
            when (val state = uiState) {
                is BookDetailsUiState.Loading -> {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        CircularProgressIndicator()
                    }
                }
                is BookDetailsUiState.Error -> {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Text("Error: ${state.message}", color = MaterialTheme.colorScheme.error)
                    }
                }
                is BookDetailsUiState.Success -> {

                    // --- 6. PASS NEW STATES/CALLBACKS DOWN ---
                    BookDetailsContent(
                        book = state.book,
                        reviews = state.allReviews,
                        userReview = state.userReview,
                        isUnlocked = state.isUnlocked, // <-- Pass lock state
                        isUnlocking = isUnlocking,   // <-- Pass loading state
                        onAddOrUpdateReview = { rating, comment ->
                            viewModel.addOrUpdateReview(rating, comment)
                        },
                        onStartReading = { onStartReading(state.book.id) },
                        onUnlockClick = {
                            showUnlockDialog = true // <-- Show the dialog
                        }
                    )

                    // --- 7. ADD THE CONFIRMATION DIALOG ---
                    if (showUnlockDialog) {
                        UnlockConfirmationDialog(
                            bookTitle = state.book.title,
                            price = state.book.price,
                            onConfirm = {
                                showUnlockDialog = false
                                viewModel.unlockBook() // <-- Call the ViewModel function
                            },
                            onDismiss = {
                                showUnlockDialog = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BookDetailsContent(
    book: Book,
    reviews: List<Review>,
    userReview: Review?,
    // --- 8. ADD NEW PARAMETERS ---
    isUnlocked: Boolean,
    isUnlocking: Boolean,
    onAddOrUpdateReview: (Float, String) -> Unit,
    onStartReading: () -> Unit,
    onUnlockClick: () -> Unit // <-- New callback
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp)
    ) {
        // --- Book Info Section ---
        item {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                AsyncImage(
                    model = book.coverImageUrl,
                    contentDescription = book.title,
                    modifier = Modifier
                        .width(200.dp)
                        .height(300.dp)
                        .clip(MaterialTheme.shapes.medium),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(book.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text("by ${book.author}", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    book.descrption,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                Spacer(modifier = Modifier.height(24.dp))

                // --- 9. *** KEY UI CHANGE: UNLOCK BUTTON *** ---
                // If the book is free (price 0) OR the user has unlocked it...
                if (book.price == 0 || isUnlocked) {
                    Button(
                        onClick = onStartReading,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Start Reading")
                    }
                } else {
                    // Otherwise, show the "Unlock" button
                    Button(
                        onClick = onUnlockClick,
                        enabled = !isUnlocking, // Disable button while loading
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isUnlocking) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Unlock"
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Unlock for ${book.price} Coins")
                        }
                    }
                }
                // --- END OF KEY UI CHANGE ---
            }
        }

        // --- Add/Edit Review Section (No changes needed) ---
        item {
            Divider(modifier = Modifier.padding(vertical = 24.dp))
            AddReviewSection(
                userReview = userReview,
                onAddReview = onAddOrUpdateReview
            )
            Spacer(modifier = Modifier.height(24.dp))
        }

        // --- Other Reviews Section (No changes needed) ---
        item {
            Text("Other Ratings & Comments", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (reviews.isEmpty()) {
            item {
                Text("Be the first to leave a review!", style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            items(reviews) { review ->
                ReviewItem(review)
            }
        }
    }
}

// --- ReviewItem Composable (No changes) ---
@Composable
fun ReviewItem(review: Review) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(review.userEmail, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
            Text("Rating: ${review.rating}/5.0", style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(review.comment, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

// --- AddReviewSection Composable (No changes) ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddReviewSection(
    userReview: Review?,
    onAddReview: (Float, String) -> Unit
) {
    var userRating by remember { mutableFloatStateOf(0f) }
    var userComment by remember { mutableStateOf("") }

    LaunchedEffect(userReview) {
        if (userReview != null) {
            userRating = userReview.rating
            userComment = userReview.comment
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        val title = if (userReview == null) "Leave a Review" else "Edit Your Review"
        Text(title, style = MaterialTheme.typography.titleMedium)

        if (userReview != null) {
            ReviewItem(review = userReview)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Update your review below:", style = MaterialTheme.typography.titleSmall)
        }

        Slider(
            value = userRating,
            onValueChange = { userRating = it },
            valueRange = 0f..5f,
            steps = 9
        )
        Text(String.format("Your Rating: %.1f", userRating), modifier = Modifier.align(Alignment.CenterHorizontally))

        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = userComment,
            onValueChange = { userComment = it },
            label = { Text("Write your comment") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = {
                onAddReview(userRating, userComment)
            },
            enabled = userComment.isNotBlank() && userRating > 0
        ) {
            val buttonText = if (userReview == null) "Submit Review" else "Update Review"
            Text(buttonText)
        }
    }
}

// --- 10. NEW DIALOG COMPOSABLE (ADD THIS TO YOUR FILE) ---
@Composable
fun UnlockConfirmationDialog(
    bookTitle: String,
    price: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Confirm Purchase") },
        text = {
            Text("Are you sure you want to spend $price coins to permanently unlock \"$bookTitle\"?")
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

