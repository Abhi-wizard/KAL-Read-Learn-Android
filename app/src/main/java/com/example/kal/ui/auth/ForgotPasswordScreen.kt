// FILE: com/example/kal/ui/auth/ForgotPasswordScreen.kt

package com.example.kal.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp


@Composable
fun ForgotPasswordScreen(
    authViewModel: AuthViewModel,
    onLinkSent: () -> Unit
) {
    // --- UPDATED: Create local state for the email field ---
    var email by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) } // For local validation

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Reset Password", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Enter your email and we'll send you a link to reset your password.",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = email, // <-- UPDATED
            onValueChange = { email = it }, // <-- UPDATED
            label = { Text("Email") },
            leadingIcon = { Icon(Icons.Default.Email, contentDescription = "Email") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = {
                // --- UPDATED: Validate locally first ---
                if (email.isBlank()) {
                    localError = "Please enter your email."
                } else {
                    localError = null // Clear local error
                    authViewModel.sendPasswordReset(email, onLinkSent) // <-- UPDATED
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Send Reset Link")
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (authViewModel.isLoading.value) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Show local validation error OR Firebase error
        val errorToShow = localError ?: authViewModel.errorState.value
        errorToShow?.let { err ->
            Text(text = err, color = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}