package com.example.kal.ui.auth

import android.util.Patterns
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * A new screen for authors to register.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthorSignupScreen(
    authViewModel: AuthViewModel = hiltViewModel(),
    onSignupSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    val scrollState = rememberScrollState()

    // --- Local State for UI ---
    var penName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }

    // --- Validation Function ---
    fun validateInputs(): Boolean {
        val n = penName.trim()
        val e = email.trim()
        val b = bio.trim()

        if (n.isEmpty() || e.isEmpty() || password.isEmpty() || confirmPassword.isEmpty() || b.isEmpty()) {
            localError = "Please fill in all fields."
            return false
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(e).matches()) {
            localError = "Please enter a valid email address."
            return false
        }
        if (password.length < 6) {
            localError = "Password must be at least 6 characters long."
            return false
        }
        if (password != confirmPassword) {
            localError = "Passwords do not match."
            return false
        }
        if (b.length < 50) {
            localError = "Your bio must be at least 50 characters long."
            return false
        }
        localError = null // Clear error if all checks pass
        return true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(text = "Author Registration", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = penName,
            onValueChange = { penName = it },
            label = { Text("Pen Name") },
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = "Pen Name") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            leadingIcon = { Icon(Icons.Default.Email, contentDescription = "Email") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = { Text("Confirm Password") },
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Confirm") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = bio,
            onValueChange = { bio = it },
            label = { Text("Author Bio (min. 50 chars)") },
            leadingIcon = { Icon(Icons.Default.RateReview, contentDescription = "Bio") },
            modifier = Modifier.fillMaxWidth().height(120.dp),
            maxLines = 5
        )
        Spacer(modifier = Modifier.height(24.dp))

        if (authViewModel.isLoading.value) {
            CircularProgressIndicator()
        } else {
            Button(
                onClick = {
                    if (validateInputs()) {
                        authViewModel.signUpAuthor( // <-- Using the new ViewModel function
                            penName = penName,
                            email = email,
                            pass = password,
                            bio = bio,
                            onComplete = { isSuccess, error ->
                                if (isSuccess) {
                                    onSignupSuccess()
                                } else {
                                    localError = error ?: "An unknown error occurred."
                                }
                            }
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text("Create Author Account")
            }
        }

        // Show validation or Firebase errors
        val errorToShow = localError ?: authViewModel.errorState.value
        errorToShow?.let { err ->
            Text(
                text = err,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Already have an account?")
            TextButton(onClick = onNavigateToLogin) {
                Text("Login")
            }
        }
    }
}