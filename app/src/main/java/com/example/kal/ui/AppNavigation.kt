package com.example.kal.ui

import android.widget.Toast
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.kal.ui.auth.AuthViewModel
import com.example.kal.ui.auth.AuthorSignupScreen
import com.example.kal.ui.auth.ForgotPasswordScreen
import com.example.kal.ui.auth.LoginScreen
import com.example.kal.ui.auth.SignupScreen
import com.example.kal.ui.auth.VerifyEmailScreen
import com.example.kal.ui.details.BookDetailsScreen
import com.example.kal.ui.main.HomeScreen
import com.example.kal.ui.profile.ProfileScreen
import com.example.kal.ui.quiz.QuizResultScreen
import com.example.kal.ui.quiz.QuizScreen
import com.example.kal.ui.reading.ReadingScreen
import com.example.kal.ui.wallet.WalletScreen
// --- 1. Import new screens ---
import com.example.kal.ui.explore.ExploreScreen
import com.example.kal.ui.my_writings.MyWritingsScreen
import com.example.kal.ui.create_writing.CreateWritingScreen
import com.example.kal.ui.writing_details.WritingDetailScreen
// --- 2. IMPORT THE NEW CREATE POST SCREEN ---
import com.example.kal.ui.create_post.CreatePostScreen
// --- 3. IMPORT THE NEW MY POSTS SCREEN ---
import com.example.kal.ui.my_posts.MyPostsScreen


// --- Placeholders (unchanged) ---
@Composable fun PrivacyPolicyScreen() { Text("Privacy Policy") }
@Composable fun CustomerCareScreen() { Text("Customer Care") }
@Composable fun LanguageSettingsScreen() { Text("Language Settings") }
// ---------------------------------------------------

// --- 4. Screen Routes (UNCHANGED, CORRECT) ---
sealed class Screen(val route: String, val icon: ImageVector? = null, val title: String? = null) {
    object Login : Screen("login")
    object Signup : Screen("signup")
    object VerifyEmail : Screen("verify_email")
    object ForgotPassword : Screen("forgot_password")
    object AuthorSignup : Screen("author_signup")

    // Bottom Bar Screens
    object Home : Screen("home", Icons.Default.Home, "Home")
    object Explore : Screen("explore", Icons.Default.Explore, "Explore")
    object Wallet : Screen("wallet", Icons.Default.AccountBalanceWallet, "Wallet")
    object Profile : Screen("profile", Icons.Default.Person, "Profile")

    // Other Screens
    object BookDetails : Screen("book_details/{bookId}") {
        fun createRoute(bookId: String) = "book_details/$bookId"
    }
    object Reading : Screen("reader/{bookId}") {
        fun createRoute(bookId: String) = "reader/$bookId"
    }
    object Quiz : Screen("quiz/{bookId}/{endPage}") {
        fun createRoute(bookId: String, endPage: Int) = "quiz/$bookId/$endPage"
    }
    object QuizResult : Screen("quiz_result/{score}/{total}/{bookId}") {
        fun createRoute(score: Int, total: Int, bookId: String) = "quiz_result/$score/$total/$bookId"
    }
    object PrivacyPolicy : Screen("privacy_policy")
    object CustomerCare : Screen("customer_care")
    object LanguageSettings : Screen("language_settings")
    object MyWritings : Screen("my_writings")
    object CreateWriting : Screen("create_writing/{draftId}") {
        fun createRoute(draftId: Int) = "create_writing/$draftId"
    }
    object WritingDetail : Screen("writing_detail/{writingId}") {
        fun createRoute(writingId: String) = "writing_detail/$writingId"
    }

    object CreatePost : Screen("create_post")

    // --- ADD THE NEW MY POSTS ROUTE HERE ---
    object MyPosts : Screen("my_posts")
}

// --- List for Bottom Bar items (unchanged) ---
val bottomNavItems = listOf(
    Screen.Home,
    Screen.Explore,
    Screen.Wallet,
    Screen.Profile
)


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = viewModel()
    val context = LocalContext.current

    // Error toast LaunchedEffect is unchanged
    val error = authViewModel.errorState.value
    LaunchedEffect(error) {
        error?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            authViewModel.errorState.value = null
        }
    }

    // --- Start Destination Logic (unchanged) ---
    val startDestination =
        if (authViewModel.isUserLoggedIn() && authViewModel.isUserEmailVerified()) {
            Screen.Home.route
        } else if (authViewModel.isUserLoggedIn()) {
            Screen.VerifyEmail.route
        } else {
            Screen.Login.route
        }

    // --- Determine if Bottom Bar should be shown (unchanged) ---
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomBar = currentRoute in bottomNavItems.map { it.route }

    // --- Central Scaffold (unchanged) ---
    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { screen ->
                        NavigationBarItem(
                            icon = { Icon(screen.icon!!, contentDescription = screen.title) },
                            label = { Text(screen.title!!) },
                            selected = currentRoute == screen.route,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(Screen.Home.route) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->

        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)
        ) {

            // --- Auth Flow (Login is FIXED) ---
            composable(Screen.Login.route) {
                val onLoginSuccess = { role: String ->
                    if (!authViewModel.isUserEmailVerified()) {
                        navController.navigate(Screen.VerifyEmail.route) {
                            popUpTo(navController.graph.startDestinationId) { inclusive = true }
                        }
                    }
                    else if (role == "Author") {
                        navController.navigate(Screen.CreatePost.route) {
                            popUpTo(navController.graph.startDestinationId) { inclusive = true }
                        }
                    }
                    else {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(navController.graph.startDestinationId) { inclusive = true }
                        }
                    }
                }

                LoginScreen(
                    authViewModel = authViewModel,
                    onNavigateToSignup = { navController.navigate(Screen.Signup.route) },
                    onNavigateToForgotPassword = { navController.navigate(Screen.ForgotPassword.route) },
                    onLoginSuccess = onLoginSuccess
                )
            }

            composable(Screen.Signup.route) {
                SignupScreen(
                    authViewModel = authViewModel,
                    onSignupSuccess = {
                        Toast.makeText(
                            context,
                            "Signup successful! Please check your email.",
                            Toast.LENGTH_LONG
                        ).show()
                        navController.navigate(Screen.VerifyEmail.route) {
                            popUpTo(navController.graph.startDestinationId) {
                                inclusive = true
                            }
                        }
                    },
                    onNavigateToLogin = { navController.popBackStack() },
                    onNavigateToAuthorSignup = {
                        navController.navigate(Screen.AuthorSignup.route)
                    }
                )
            }

            composable(Screen.VerifyEmail.route) {
                VerifyEmailScreen(
                    onResendEmailClick = {
                        authViewModel.sendVerificationEmail()
                        Toast.makeText(context, "Verification email sent.", Toast.LENGTH_SHORT)
                            .show()
                    },
                    onBackToLoginClick = {
                        authViewModel.logout()
                        navController.navigate(Screen.Login.route) {
                            popUpTo(navController.graph.startDestinationId) {
                                inclusive = true
                            }
                        }
                    }
                )
            }

            composable(Screen.ForgotPassword.route) {
                ForgotPasswordScreen(
                    authViewModel = authViewModel,
                    onLinkSent = {
                        Toast.makeText(
                            context,
                            "Password reset link sent to your email.",
                            Toast.LENGTH_LONG
                        ).show()
                        navController.popBackStack()
                    }
                )
            }

            composable(Screen.AuthorSignup.route) {
                AuthorSignupScreen(
                    onSignupSuccess = {
                        Toast.makeText(context, "Signup successful! Please check your email.", Toast.LENGTH_LONG).show()
                        navController.navigate(Screen.VerifyEmail.route) { popUpTo(navController.graph.startDestinationId) { inclusive = true } }
                    },
                    onNavigateToLogin = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    }
                )
            }

            // --- Main App Flow (Tabs) (unchanged) ---
            composable(Screen.Home.route) {
                HomeScreen(
                    onBookClick = { bookId ->
                        navController.navigate(Screen.BookDetails.createRoute(bookId))
                    },
                    navController = navController
                )
            }

            composable(Screen.Explore.route) {
                ExploreScreen(
                    onNavigateToDetail = { writingId ->
                        navController.navigate(Screen.WritingDetail.createRoute(writingId))
                    },
                    onNavigateToEditor = {
                        // TODO: Add 30-coin unlock logic here
                        navController.navigate(Screen.CreateWriting.createRoute(0)) // 0 = new draft
                    },
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // --- 5. CORRECTED PROFILE COMPOSABLE ---
            composable(Screen.Profile.route) {
                ProfileScreen(
                    onLogout = {
                        authViewModel.logout()
                        navController.navigate(Screen.Login.route) {
                            popUpTo(navController.graph.id) { inclusive = true }
                        }
                    },
                    onNavigateToPrivacy = { navController.navigate(Screen.PrivacyPolicy.route) },
                    onNavigateToCustomerCare = { navController.navigate(Screen.CustomerCare.route) },
                    onNavigateToLanguage = { navController.navigate(Screen.LanguageSettings.route) },
                    onResumeReading = { bookId ->
                        navController.navigate(Screen.Reading.createRoute(bookId))
                    },
                    // FIX 1: Add new MyPosts route
                    onNavigateToMyWritings = { navController.navigate(Screen.MyWritings.route) },
                    onNavigateToMyPosts = { navController.navigate(Screen.MyPosts.route) }
                )
            }

            composable(Screen.Wallet.route) {
                WalletScreen(navController = navController)
            }


            // --- Reading & Quiz Flow (unchanged) ---
            composable(
                route = Screen.BookDetails.route,
                arguments = listOf(navArgument("bookId") { type = NavType.StringType })
            ) {
                BookDetailsScreen(
                    navController = navController,
                    onStartReading = { bookId ->
                        navController.navigate(Screen.Reading.createRoute(bookId))
                    }
                )
            }
            composable(
                route = Screen.Reading.route,
                arguments = listOf(navArgument("bookId") { type = NavType.StringType })
            ) { backStackEntry ->
                val bookId = backStackEntry.arguments?.getString("bookId")
                requireNotNull(bookId) { "Book ID is required." }

                ReadingScreen(
                    bookId = bookId,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToQuiz = { bId, endPage ->
                        navController.navigate(Screen.Quiz.createRoute(bId, endPage))
                    }
                )
            }
            composable(
                route = Screen.Quiz.route,
                arguments = listOf(
                    navArgument("bookId") { type = NavType.StringType },
                    navArgument("endPage") { type = NavType.IntType }
                )
            ) { backStackEntry ->
                val bookId = backStackEntry.arguments?.getString("bookId")
                val endPage = backStackEntry.arguments?.getInt("endPage")
                requireNotNull(bookId)
                requireNotNull(endPage)

                QuizScreen(
                    bookId = bookId,
                    endPage = endPage,
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onNavigateToResult = { score, total, resultBookId ->
                        navController.navigate(
                            Screen.QuizResult.createRoute(
                                score,
                                total,
                                resultBookId
                            )
                        ) {
                            popUpTo(Screen.Quiz.route) { inclusive = true }
                        }
                    }
                )
            }
            composable(
                route = Screen.QuizResult.route,
                arguments = listOf(
                    navArgument("score") { type = NavType.IntType },
                    navArgument("total") { type = NavType.IntType },
                    navArgument("bookId") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val score = backStackEntry.arguments?.getInt("score") ?: 0
                val total = backStackEntry.arguments?.getInt("total") ?: 0
                val bookId = backStackEntry.arguments?.getString("bookId")!!

                QuizResultScreen(
                    score = score,
                    total = total,
                    bookId = bookId,
                    onNavigateBackToReading = {
                        navController.popBackStack()
                    }
                )
            }

            // --- Settings (unchanged) ---
            composable(Screen.PrivacyPolicy.route) { PrivacyPolicyScreen() }
            composable(Screen.CustomerCare.route) { CustomerCareScreen() }
            composable(Screen.LanguageSettings.route) { LanguageSettingsScreen() }

            // --- Writings Flow (unchanged) ---
            composable(Screen.MyWritings.route) {
                MyWritingsScreen(
                    onNavigateToEditor = { draftId ->
                        navController.navigate(Screen.CreateWriting.createRoute(draftId ?: 0))
                    },
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(
                route = Screen.CreateWriting.route,
                arguments = listOf(navArgument("draftId") {
                    type = NavType.IntType
                    defaultValue = 0 // Default to 0 for a new draft
                })
            ) {
                CreateWritingScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(
                route = Screen.WritingDetail.route,
                arguments = listOf(navArgument("writingId") { type = NavType.StringType })
            ) {
                WritingDetailScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // --- Create Post Flow (unchanged) ---
            composable(Screen.CreatePost.route) {
                CreatePostScreen(
                    navController = navController
                )
            }

            // --- FIX 2: ADD THE MISSING MY POSTS COMPOSABLE ---
            composable(Screen.MyPosts.route) {
                MyPostsScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            // --- END OF FIX ---
        }
    }
}

