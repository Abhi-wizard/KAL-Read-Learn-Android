// FILE: com/example/kal/ui/reading/ReadingScreen.kt
package com.example.kal.ui.reading

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.core.graphics.createBitmap

// Timer import removed


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadingScreen(
    bookId: String,
    onNavigateBack: () -> Unit,
    onNavigateToQuiz: (bookId: String, endPage: Int) -> Unit, // <-- ADDED
    viewModel: ReadingViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState() // <-- FIXED LINE
    val context = LocalContext.current

    LaunchedEffect(key1 = bookId) {
        viewModel.loadBook(bookId, context)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(uiState.bookTitle, maxLines = 1)
                        if (uiState.authorName.isNotEmpty()) {
                            Text(
                                uiState.authorName,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator()
            } else if (uiState.error != null) {
                Text(text = uiState.error!!, color = MaterialTheme.colorScheme.error)
            } else if (uiState.pdfUri != null) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    PdfPageRenderer(
                        uri = uiState.pdfUri!!,
                        pageToShow = uiState.currentPage,
                        modifier = Modifier.weight(1f),
                        onPageCount = {
                            if (uiState.totalPages == 0) {
                                viewModel.onPdfLoaded(it)
                            }
                        }
                    )

                    // Pass the navigation lambda to the controls
                    ReadingControls(
                        uiState = uiState,
                        viewModel = viewModel,
                        onStartQuizClick = {
                            // Pass the current page as the endPage for the quiz
                            onNavigateToQuiz(bookId, uiState.currentPage)
                        }
                    )
                }
            }
        }
    }
}

// ... (PdfPageRenderer composable remains exactly the same as you provided) ...
@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun PdfPageRenderer(
    uri: Uri,
    pageToShow: Int,
    modifier: Modifier = Modifier,
    onPageCount: (Int) -> Unit
) {
    val context = LocalContext.current
    var bitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    var pdfRenderer by remember { mutableStateOf<PdfRenderer?>(null) }
    var pfd by remember { mutableStateOf<ParcelFileDescriptor?>(null) }


    LaunchedEffect(key1 = uri) {
        try {
            pdfRenderer?.close()
            pfd?.close()

            val fileDescriptor = context.contentResolver.openFileDescriptor(uri, "r")
            if (fileDescriptor == null) {
                error = "Failed to open PDF"
                return@LaunchedEffect
            }
            pfd = fileDescriptor
            pdfRenderer = PdfRenderer(fileDescriptor).also {
                onPageCount(it.pageCount)
            }
        } catch (e: Exception) {
            error = e.message ?: "Error opening PDF"
        }
    }

    // ... (This composable is complex and correct, no changes needed) ...

    BoxWithConstraints(
        modifier = modifier.fillMaxWidth().background(Color.LightGray),
        contentAlignment = Alignment.Center
    ) {
        val availableWidth = constraints.maxWidth
        LaunchedEffect(key1 = pdfRenderer, key2 = pageToShow, key3 = availableWidth) {
            val renderer = pdfRenderer ?: return@LaunchedEffect
            if (pageToShow < 0 || pageToShow >= renderer.pageCount || availableWidth <= 0) {
                return@LaunchedEffect
            }
            error = null
            try {
                renderer.openPage(pageToShow).use { page ->
                    val newHeight = (availableWidth.toFloat() * (page.height.toFloat() / page.width.toFloat())).toInt()
                    val pageBitmap = createBitmap(availableWidth, newHeight)
                    pageBitmap.eraseColor(android.graphics.Color.WHITE)
                    val matrix = android.graphics.Matrix()
                    matrix.postScale(
                        availableWidth.toFloat() / page.width,
                        newHeight.toFloat() / page.height
                    )
                    page.render(
                        pageBitmap,
                        null,
                        matrix,
                        PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY
                    )
                    bitmap = pageBitmap.asImageBitmap()
                }
            } catch (e: Exception) {
                error = e.message ?: "Error rendering page"
            }
        }

        if (bitmap != null) {
            Image(
                bitmap = bitmap!!,
                contentDescription = "PDF Page ${pageToShow + 1}",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        } else if (error != null) {
            Text(text = error!!, color = MaterialTheme.colorScheme.error)
        } else {
            CircularProgressIndicator()
        }
    }
}


/**
 * ReadingControls UPDATED
 * - Timer UI is REMOVED
 * - "Next" button logic is simplified
 * - 'onStartQuizClick' lambda is added
 */
@Composable
private fun ReadingControls(
    uiState: ReadingUiState,
    viewModel: ReadingViewModel,
    onStartQuizClick: () -> Unit // <-- ADDED
) {
    // TimerText logic REMOVED

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Timer and Page Count
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Timer Text REMOVED

                if (uiState.totalPages > 0) {
                    Text(
                        text = "Page ${uiState.currentPage + 1} of ${uiState.totalPages}",
                        style = MaterialTheme.typography.bodyMedium,
                        // Make page count take up the full width
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Navigation Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { viewModel.goToPreviousPage() },
                    enabled = uiState.currentPage > 0
                ) {
                    Text("Previous")
                }

                AnimatedVisibility(
                    visible = uiState.showQuizButton,
                    enter = scaleIn(),
                    exit = scaleOut()
                ) {
                    Button(
                        onClick = onStartQuizClick, // <-- UPDATED
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Text("Start Quiz")
                    }
                }

                Button(
                    onClick = { viewModel.goToNextPage() },
                    // "isPageTurnUnlocked" logic REMOVED
                    enabled = uiState.currentPage < uiState.totalPages - 1
                ) {
                    Text("Next")
                }
            }
        }
    }
}