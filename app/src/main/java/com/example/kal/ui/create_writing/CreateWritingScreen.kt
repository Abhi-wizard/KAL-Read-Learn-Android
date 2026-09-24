package com.example.kal.ui.create_writing

import android.widget.Toast // <-- 1. ADD IMPORT
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect // <-- 2. ADD IMPORT
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext // <-- 3. ADD IMPORT
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateWritingScreen(
    viewModel: CreateWritingViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    // Observe the states from the ViewModel
    val title by viewModel.title.collectAsState()
    val content by viewModel.content.collectAsState()
    val genre by viewModel.genre.collectAsState() // <-- 4. UNCOMMENTED THESE
    val language by viewModel.language.collectAsState() // <-- 4. UNCOMMENTED THESE
    val uiState by viewModel.uiState.collectAsState()

    val context = LocalContext.current

    // --- 5. ADDED THIS LAUNCHEDEFFECT TO HANDLE POPUPS ---
    LaunchedEffect(Unit) {
        viewModel.publishEvent.collect { event ->
            when (event) {
                is PublishResult.Success -> {
                    Toast.makeText(context, "Successfully published!", Toast.LENGTH_SHORT).show()
                    onNavigateBack() // Go back after success
                }
                is PublishResult.Error -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    // --- END OF NEW CODE ---

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Write Story") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        },
        bottomBar = {
            BottomAppBar(
                modifier = Modifier.height(IntrinsicSize.Min)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    OutlinedButton(
                        onClick = { viewModel.onSaveDraft() },
                        enabled = uiState == CreateWritingUiState.Idle // Only enable when idle
                    ) {
                        if (uiState == CreateWritingUiState.SavingDraft) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        } else {
                            Text("Save Draft")
                        }
                    }

                    Button(
                        onClick = {
                            // TODO: Show a confirmation dialog here first
                            viewModel.onPublish()
                        },
                        enabled = uiState == CreateWritingUiState.Idle // Only enable when idle
                    ) {
                        if (uiState == CreateWritingUiState.Publishing) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        } else {
                            Text("Publish (50 Coins)")
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { viewModel.title.value = it },
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // TODO: Add ExposedDropdownMenuBox for Genre
            // For now, you can use temporary TextFields
            OutlinedTextField(
                value = genre,
                onValueChange = { viewModel.genre.value = it },
                label = { Text("Genre (e.g., Poetry)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = language,
                onValueChange = { viewModel.language.value = it },
                label = { Text("Language (e.g., en)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )


            OutlinedTextField(
                value = content,
                onValueChange = { viewModel.content.value = it },
                label = { Text("Start writing your story...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp)
            )
        }
    }
}