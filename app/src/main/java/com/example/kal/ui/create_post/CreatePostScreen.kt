package com.example.kal.ui.create_post

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.kal.R // Make sure you have a default placeholder image

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePostScreen(
    navController: NavController,
    viewModel: CreatePostViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // --- Image Picker Launcher ---
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        viewModel.onImageSelected(uri)
    }

    // --- Navigation Logic ---
    val onNavigateToProfile: () -> Unit = {
        navController.navigate("profile") { // Assuming "profile" is your route
            popUpTo("profile") { inclusive = true }
            launchSingleTop = true
        }
    }

    LaunchedEffect(uiState.isPostSuccessful) {
        if (uiState.isPostSuccessful) {
            Toast.makeText(context, "Post published!", Toast.LENGTH_SHORT).show()
            onNavigateToProfile()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Promotion") },
                navigationIcon = {
                    IconButton(onClick = onNavigateToProfile) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back to Profile")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // --- 1. Image Upload Box ---
            ImageUploadBox(
                selectedImageUri = uiState.selectedImageUri,
                onClick = {
                    imagePickerLauncher.launch("image/*") // Launch image gallery
                }
            )
            Spacer(modifier = Modifier.height(24.dp))

            // --- 2. Text Fields ---
            OutlinedTextField(
                value = uiState.penName,
                onValueChange = { viewModel.onPenNameChange(it) },
                label = { Text("Author's Pen Name") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = uiState.bookTitle,
                onValueChange = { viewModel.onTitleChange(it) },
                label = { Text("Book Title") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = uiState.bookSynopsis,
                onValueChange = { viewModel.onSynopsisChange(it) },
                label = { Text("Book Synopsis") },
                modifier = Modifier.fillMaxWidth().height(150.dp), // Single big box
                maxLines = 7
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = uiState.purchaseLink,
                onValueChange = { viewModel.onLinkChange(it) },
                label = { Text("E-commerce Purchase Link (Optional)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = uiState.authorComment,
                onValueChange = { viewModel.onCommentChange(it) },
                label = { Text("Your Thoughts...") },
                placeholder = { Text("Share a personal message with your readers...") },
                modifier = Modifier.fillMaxWidth().height(120.dp),
                maxLines = 5
            )
            Spacer(modifier = Modifier.height(24.dp))

            // --- 3. Error and Publish Button ---
            uiState.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(8.dp))
            }

            Button(
                onClick = { viewModel.publishPost() },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                enabled = !uiState.isLoading
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Publish Post")
                }
            }
        }
    }
}

/**
 * A composable for the dashed "Upload Image" area.
 */
@Composable
fun ImageUploadBox(
    selectedImageUri: Uri?,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = 2.dp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (selectedImageUri != null) {
            // --- Show the selected image ---
            AsyncImage(
                model = selectedImageUri,
                contentDescription = "Selected book cover",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            // --- Show the placeholder ---
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_launcher_background), // TODO: Use a real "upload" icon
                    contentDescription = "Upload Icon",
                    modifier = Modifier.size(50.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Upload Cover Image",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}