// FILE: com/example/kal/ui/auth/SignupScreen.kt

package com.example.kal.ui.auth

import android.util.Patterns
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignupScreen(
    authViewModel: AuthViewModel,
    onSignupSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit,
    onNavigateToAuthorSignup: () -> Unit // <-- Add this to your NavHost
) {
    val scrollState = rememberScrollState()

    // --- State for Role Selection ---
    var selectedRole by remember { mutableStateOf("") } // Start empty
    val roles = listOf("User", "Author")
    var isDropdownExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(text = "Create Your Account", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(24.dp))

        // --- 1. NEW ROLE SPINNER ---
        ExposedDropdownMenuBox(
            expanded = isDropdownExpanded,
            onExpandedChange = { isDropdownExpanded = !isDropdownExpanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = selectedRole,
                onValueChange = {}, // Read-only
                label = { Text("Select Your Role") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isDropdownExpanded) },
                readOnly = true,
                modifier = Modifier
                    .menuAnchor() // This is important for the dropdown to work
                    .fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = isDropdownExpanded,
                onDismissRequest = { isDropdownExpanded = false }
            ) {
                roles.forEach { role ->
                    DropdownMenuItem(
                        text = { Text(role) },
                        onClick = {
                            selectedRole = role
                            isDropdownExpanded = false
                        }
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))

        // --- 2. CONDITIONAL UI ---
        when (selectedRole) {
            "User" -> {
                // Show the original form
                UserSignupForm(
                    authViewModel = authViewModel,
                    onSignupSuccess = onSignupSuccess
                )
            }
            "Author" -> {
                // Show the new author prompt
                AuthorSignupPrompt(
                    onNavigateToAuthorSignup = onNavigateToAuthorSignup
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f)) // Pushes login to bottom

        // --- Login Navigation (Always visible) ---
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Already have an account?")
            TextButton(onClick = onNavigateToLogin) {
                Text("Login")
            }
        }
    }
}

/**
 * This composable contains your ORIGINAL signup form for a "User".
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserSignupForm(
    authViewModel: AuthViewModel,
    onSignupSuccess: () -> Unit
) {
    // --- All of your original local state is now inside here ---
    var name by remember { mutableStateOf("") }
    var dob by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }

    // --- Date Picker Dialog State ---
    val showDatePicker = remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()

    if (showDatePicker.value) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker.value = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val selectedDate = datePickerState.selectedDateMillis?.let {
                            val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                            formatter.format(Date(it))
                        }
                        if (selectedDate != null) {
                            dob = selectedDate
                        }
                        showDatePicker.value = false
                    }
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker.value = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // --- Your original validation function ---
    fun validateInputs(): Boolean {
        val n = name.trim()
        val d = dob.trim()
        val p = phone.trim()
        val e = email.trim()

        if (n.isEmpty() || d.isEmpty() || p.isEmpty() || e.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            localError = "Please fill in all fields."
            return false
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(e).matches()) {
            localError = "Please enter a valid email address."
            return false
        }
        if (p.length < 10) {
            localError = "Please enter a valid 10-digit phone number."
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
        localError = null
        return true
    }

    // --- Your original form UI ---
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp) // Replaces Spacers
    ) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Full Name") },
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = "Name") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = dob,
            onValueChange = { },
            label = { Text("Date of Birth") },
            leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = "DOB") },
            trailingIcon = {
                IconButton(onClick = { showDatePicker.value = true }) {
                    Icon(Icons.Default.CalendarToday, contentDescription = "Pick date")
                }
            },
            readOnly = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            leadingIcon = { Icon(Icons.Default.Email, contentDescription = "Email") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = phone,
            onValueChange = { phone = it },
            label = { Text("Phone Number") },
            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = "Phone") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = { Text("Confirm Password") },
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Confirm") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp)) // Small spacer before button

        if (authViewModel.isLoading.value) {
            CircularProgressIndicator()
        } else {
            Button(
                onClick = {
                    if (validateInputs()) {
                        authViewModel.signUpUser(
                            name = name,
                            email = email,
                            pass = password,
                            phoneNumber = phone,
                            dob = dob,
                            role = "User", // <-- 3. IMPORTANT: Pass the role
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
                Text("Create Account")
            }
        }

        val errorToShow = localError ?: authViewModel.errorState.value
        errorToShow?.let { err ->
            Text(
                text = err,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

/**
 * This is the new, simple prompt shown for "Author" selection.
 */
@Composable
fun AuthorSignupPrompt(onNavigateToAuthorSignup: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Welcome, Author!",
            style = MaterialTheme.typography.titleLarge
        )
        Text(
            text = "The author registration process is separate. Please continue to our author portal.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
        Button(onClick = onNavigateToAuthorSignup) {
            Text("Continue to Author Registration")
        }
    }
}