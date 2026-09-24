package com.example.kal.ui.auth

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kal.data.User // <-- 1. IMPORTED THE CORRECT USER CLASS
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
// 2. REMOVED the old, local User data class

/**
 * The ViewModel responsible for all authentication and user profile logic.
 */
class AuthViewModel : ViewModel() {

    // --- State Properties for UI ---
    val isLoading = mutableStateOf(false)
    val errorState = mutableStateOf<String?>(null)

    /**
     * Holds the fetched user profile data for display on the profile screen.
     * This now correctly uses the imported com.example.kal.data.User class.
     */
    val userProfile = mutableStateOf<User?>(null)

    // --- Firebase Instances ---
    private val auth: FirebaseAuth = Firebase.auth
    private val firestore = Firebase.firestore

    /**
     * Fetches the current user's profile from the 'users' collection in Firestore.
     */
    fun fetchUserProfile() {
        viewModelScope.launch {
            isLoading.value = true
            errorState.value = null
            userProfile.value = null // Reset previous profile data

            try {
                val currentUserId = auth.currentUser?.uid
                if (currentUserId == null) {
                    throw Exception("User is not logged in.")
                }

                val documentSnapshot = firestore.collection("users").document(currentUserId).get().await()

                if (documentSnapshot.exists()) {
                    // This now correctly converts to the imported User class
                    val profile = documentSnapshot.toObject(User::class.java)
                    userProfile.value = profile
                    Log.d("AuthViewModel", "Successfully fetched user profile.")
                } else {
                    errorState.value = "User profile not found."
                    Log.w("AuthViewModel", "User profile document does not exist for UID: $currentUserId")
                }

            } catch (e: Exception) {
                errorState.value = e.message ?: "Failed to fetch profile."
                Log.e("AuthViewModel", "Error fetching user profile", e)
            } finally {
                isLoading.value = false
            }
        }
    }


    /**
     * Creates a new user, sends a verification email, and saves their complete profile.
     * @param onComplete Callback to be invoked with success status and optional error.
     */
    fun signUpUser(
        name: String,
        email: String,
        pass: String,
        phoneNumber: String,
        dob: String,
        role: String,
        onComplete: (isSuccess: Boolean, error: String?) -> Unit
    ) {
        viewModelScope.launch {
            isLoading.value = true
            errorState.value = null // Clear old errors

            try {
                // 1. Create user in Firebase Authentication
                val authResult = auth.createUserWithEmailAndPassword(email.trim(), pass).await()
                val firebaseUser = authResult.user ?: throw Exception("User creation failed.")

                // 2. Send verification email
                firebaseUser.sendEmailVerification().await()
                Log.d("AuthViewModel", "Verification email sent successfully.")

                // 3. Create the full user profile object (using imported User class)
                val newUser = User(
                    uid = firebaseUser.uid,
                    name = name.trim(),
                    email = email.trim(),
                    phoneNumber = phoneNumber.trim(),
                    dob = dob.trim(),
                    totalPoints = 0,
                    isWritingUnlocked = false,
                    role = role
                )

                // 4. Save user profile to Firestore
                firestore.collection("users").document(firebaseUser.uid).set(newUser).await()
                Log.d("AuthViewModel", "User profile saved to Firestore.")

                onComplete(true, null) // Trigger navigation on complete success

            } catch (e: Exception) {
                val errorMessage = e.message ?: "An unknown error occurred."
                errorState.value = errorMessage // Set error state for UI to observe
                onComplete(false, errorMessage) // Report failure via callback
                Log.e("AuthViewModel", "Sign up failed", e)
            } finally {
                isLoading.value = false // Ensure loading is always stopped
            }
        }
    }

    /**
     * Creates a new AUTHOR, sends verification, and saves their profile.
     */
    fun signUpAuthor(
        penName: String,
        email: String,
        pass: String,
        bio: String,
        onComplete: (isSuccess: Boolean, error: String?) -> Unit
    ) {
        viewModelScope.launch {
            isLoading.value = true
            errorState.value = null // Clear old errors

            try {
                // 1. Create user in Firebase Authentication
                val authResult = auth.createUserWithEmailAndPassword(email.trim(), pass).await()
                val firebaseUser = authResult.user ?: throw Exception("User creation failed.")

                // 2. Send verification email
                firebaseUser.sendEmailVerification().await()
                Log.d("AuthViewModel", "Verification email sent successfully.")

                // 3. Create the full user profile object
                val newAuthor = User(
                    uid = firebaseUser.uid,
                    name = penName.trim(), // Using 'name' field for 'penName'
                    email = email.trim(),
                    role = "Author", // <-- Set role to Author
                    bio = bio.trim(), // <-- Save the bio
                    isWritingUnlocked = true, // <-- Authors can write by default
                    totalPoints = 0
                )

                // 4. Save user profile to Firestore
                firestore.collection("users").document(firebaseUser.uid).set(newAuthor).await()
                Log.d("AuthViewModel", "Author profile saved to Firestore.")

                onComplete(true, null) // Trigger navigation on complete success

            } catch (e: Exception) {
                val errorMessage = e.message ?: "An unknown error occurred."
                errorState.value = errorMessage
                onComplete(false, errorMessage)
                Log.e("AuthViewModel", "Author sign up failed", e)
            } finally {
                isLoading.value = false
            }
        }
    }

    /**
     * Signs in a user with their email/password AND fetches their role.
     * @param onNavigate Callback to be invoked with the user's role on success.
     */
    fun signInWithEmail(
        email: String,
        pass: String,
        onNavigate: (role: String) -> Unit // <-- UPDATED
    ) {
        viewModelScope.launch {
            if (email.isBlank() || pass.isBlank()) {
                errorState.value = "Email and password cannot be empty."
                return@launch
            }
            isLoading.value = true
            errorState.value = null

            try {
                // 1. Sign in the user
                val authResult = auth.signInWithEmailAndPassword(email.trim(), pass).await()
                val uid = authResult.user?.uid ?: throw Exception("Login failed, user not found.")

                // 2. Fetch the user's document from Firestore
                val userDoc = firestore.collection("users").document(uid).get().await()
                val user = userDoc.toObject(User::class.java)

                // 3. Find the role, defaulting to "User" if not found
                val userRole = user?.role ?: "User"

                // 4. Pass the role back to the UI for navigation
                onNavigate(userRole)

            } catch (e: Exception) {
                errorState.value = e.message ?: "Login failed."
                Log.e("AuthViewModel", "Sign in failed", e)
            } finally {
                isLoading.value = false
            }
        }
    }

    /**
     * Signs in a user with a Google credential and creates a profile if it doesn't exist.
     * @param onNavigate Callback to be invoked with the user's role on successful login.
     */
    fun signInWithGoogle(
        credential: AuthCredential,
        onNavigate: (role: String) -> Unit // <-- UPDATED
    ) {
        viewModelScope.launch {
            isLoading.value = true
            errorState.value = null

            try {
                val authResult = auth.signInWithCredential(credential).await()
                val firebaseUser = authResult.user ?: throw Exception("Google sign-in failed.")

                val userDocRef = firestore.collection("users").document(firebaseUser.uid)
                val document = userDocRef.get().await()

                val userRole: String
                if (!document.exists()) {
                    // This is a new user, create a profile
                    val newGoogleUser = User(
                        uid = firebaseUser.uid,
                        name = firebaseUser.displayName ?: "New User",
                        email = firebaseUser.email ?: "",
                        phoneNumber = firebaseUser.phoneNumber ?: "",
                        dob = "",
                        profileImageUrl = firebaseUser.photoUrl?.toString(), // <-- Correctly added
                        totalPoints = 0,
                        isWritingUnlocked = false,
                        role = "User" // Default role for Google Sign-In
                    )
                    userDocRef.set(newGoogleUser).await()
                    userRole = "User" // Set role for navigation
                    Log.d("AuthViewModel", "Created profile for Google user.")
                } else {
                    // This is an existing user, get their role
                    val user = document.toObject(User::class.java)
                    userRole = user?.role ?: "User" // Default to "User" if missing
                }

                // Pass role back to UI for navigation
                onNavigate(userRole)

            } catch (e: Exception) {
                errorState.value = e.message ?: "Google sign-in failed."
                Log.e("AuthViewModel", "Google sign-in failed", e)
            } finally {
                isLoading.value = false
            }
        }
    }

    /**
     * Sends a password reset link to the provided email.
     * @param onSuccess Callback to be invoked when the email is sent successfully.
     */
    fun sendPasswordReset(email: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            if (email.isBlank()) {
                errorState.value = "Please enter your email."
                return@launch
            }
            isLoading.value = true
            errorState.value = null

            try {
                auth.sendPasswordResetEmail(email.trim()).await()
                onSuccess()
            } catch (e: Exception) {
                errorState.value = e.message ?: "Failed to send reset email."
                Log.e("AuthViewModel", "Password reset failed", e)
            } finally {
                isLoading.value = false
            }
        }
    }

    /**
     * Checks if a user is currently logged in.
     * @return `true` if a user is signed in, `false` otherwise.
     */
    fun isUserLoggedIn(): Boolean {
        return auth.currentUser != null
    }

    /**
     * Checks if the currently signed-in user has verified their email.
     * @return `true` if verified, `false` otherwise.
     */
    fun isUserEmailVerified(): Boolean {
        return auth.currentUser?.isEmailVerified ?: false
    }

    /**
     * Re-sends the verification email to the current user.
     */
    fun sendVerificationEmail() {
        viewModelScope.launch {
            try {
                auth.currentUser?.sendEmailVerification()?.await()
                Log.d("AuthViewModel", "Verification email re-sent.")
            } catch (e: Exception) {
                errorState.value = e.message
                Log.e("AuthViewModel", "Failed to re-send verification email", e)
            }
        }
    }

    /**
     * Signs out the current user.
     */
    fun logout() {
        auth.signOut()
    }
}