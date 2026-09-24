package com.example.kal.ui.profile

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.kal.data.User

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onLogout: () -> Unit,
    onNavigateToPrivacy: () -> Unit,
    onNavigateToCustomerCare: () -> Unit,
    onNavigateToLanguage: () -> Unit,
    onResumeReading: (String) -> Unit,
    // --- UPDATED SIGNATURES ---
    onNavigateToMyWritings: () -> Unit, // For regular users
    onNavigateToMyPosts: () -> Unit // <-- NEW: For authors to manage ads
) {
    val viewModel: ProfileScreenViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsState()
    val isUnlocking by viewModel.isUnlocking

    val context = LocalContext.current
    var showConfirmDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.unlockEvent.collect { event ->
            when (event) {
                is UnlockEvent.Success -> {
                    // Check the ViewModel's current UI state after successful unlock
                    val userRole = (viewModel.uiState.value as? ProfileUiState.Success)?.user?.role
                    if (userRole == "Author") {
                        onNavigateToMyPosts() // Redirect author to manage ads
                    } else {
                        onNavigateToMyWritings() // Redirect user to manage writings/drafts
                    }
                }
                is UnlockEvent.Error -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_LONG).show()
                }
                is UnlockEvent.ShowConfirmation -> {
                    showConfirmDialog = true
                }
            }
        }
    }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("Unlock Writing Area?") },
            text = { Text("This is a one-time cost of 30 coins to unlock the ability to write and save drafts.") },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmDialog = false
                        viewModel.confirmUnlock()
                    },
                    enabled = !isUnlocking
                ) {
                    if (isUnlocking) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    } else {
                        Text("Unlock for 30 Coins")
                    }
                }
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
                title = { Text("Profile", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { paddingValues ->
        when (val state = uiState) {
            is ProfileUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
            is ProfileUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                    Text(state.message, color = MaterialTheme.colorScheme.error)
                }
            }
            is ProfileUiState.Success -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        ProfileInfoCard(user = state.user)
                        Spacer(modifier = Modifier.height(24.dp))
                    }

                    if (state.recentBookTitle != null && state.recentBookProgress != null) {
                        item {
                            ReadingProgressSection(
                                bookTitle = state.recentBookTitle,
                                progress = state.recentBookProgress,
                                onResumeReading = { onResumeReading(state.user.recentlyReadBookId!!) }
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }

                    item {
                        SettingsSection(
                            onNavigateToPrivacy = onNavigateToPrivacy,
                            onNavigateToCustomerCare = onNavigateToCustomerCare,
                            onNavigateToLanguage = onNavigateToLanguage,
                            // --- NEW NAVIGATION LOGIC HERE ---
                            // This button press triggers the ViewModel's logic first.
                            onMyContentClicked = { viewModel.onMyWritingsClicked() },
                            isAuthor = state.user.role == "Author" // Pass role for button text
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                    }

                    item {
                        LogoutButton(onLogout = onLogout)
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }
        }
    }
}

// --- ProfileInfoCard and ProfileInfoRow (UNCHANGED) ---
@Composable
fun ProfileInfoCard(user: User) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            ProfileInfoRow(icon = Icons.Default.Person, label = "Name", value = user.name)
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            ProfileInfoRow(icon = Icons.Default.Email, label = "Email", value = user.email)
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            ProfileInfoRow(icon = Icons.Default.Cake, label = "Date of Birth", value = user.dob)
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            ProfileInfoRow(icon = Icons.Default.Phone, label = "Phone", value = user.phoneNumber)
        }
    }
}

@Composable
fun ProfileInfoRow(icon: ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = icon, contentDescription = label, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

// --- ReadingProgressSection (UNCHANGED) ---
@Composable
fun ReadingProgressSection(bookTitle: String, progress: Float, onResumeReading: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Recently Reading", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start), color = MaterialTheme.colorScheme.onBackground)
        Spacer(modifier = Modifier.height(16.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(bookTitle, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.background
                )
                Text(
                    text = "${(progress * 100).toInt()}% Completed",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(top = 4.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onResumeReading,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Resume Reading", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// --- SettingsSection (UPDATED) ---
@Composable
fun SettingsSection(
    onNavigateToPrivacy: () -> Unit,
    onNavigateToCustomerCare: () -> Unit,
    onNavigateToLanguage: () -> Unit,
    onMyContentClicked: () -> Unit, // Renamed generic callback
    isAuthor: Boolean // New parameter to change text
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Settings & Support", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start), color = MaterialTheme.colorScheme.onBackground)
        Spacer(modifier = Modifier.height(8.dp))

        SettingsItem(
            icon = if (isAuthor) Icons.Default.Campaign else Icons.Default.Edit, // Icon reflects role
            text = if (isAuthor) "My Promotions" else "My Writings", // Text reflects role
            onClick = onMyContentClicked // This triggers the VM check
        )

        SettingsItem(icon = Icons.Default.Policy, text = "Privacy Policy", onClick = onNavigateToPrivacy)
        SettingsItem(icon = Icons.AutoMirrored.Filled.HelpOutline, text = "Customer Care", onClick = onNavigateToCustomerCare)
        SettingsItem(icon = Icons.Default.Language, text = "Language", onClick = onNavigateToLanguage)
    }
}

// --- SettingsItem (UNCHANGED) ---
@Composable
fun SettingsItem(icon: ImageVector, text: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = text, tint = MaterialTheme.colorScheme.onBackground)
        Spacer(modifier = Modifier.width(16.dp))
        Text(text, fontSize = 16.sp, color = MaterialTheme.colorScheme.onBackground)
        Spacer(modifier = Modifier.weight(1f))
        Icon(imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// --- LogoutButton (UNCHANGED) ---
@Composable
fun LogoutButton(onLogout: () -> Unit) {
    OutlinedButton(
        onClick = onLogout,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
    ) {
        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Logout")
        Spacer(modifier = Modifier.width(8.dp))
        Text("Logout", fontWeight = FontWeight.Bold)
    }
}