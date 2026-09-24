package com.example.kal.ui.quiz

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizResultScreen(
    score: Int,
    total: Int,
    onNavigateBackToReading: () -> Unit,
    bookId: String
) {
    // This calculation matches the screenshot (3 * 10 = 30)
    val coinsEarned = score * 10

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Quiz Complete!") })
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            // --- Icon or Animation ---
            // TODO: Add a nice "congratulations" animation or icon here
            // e.g., Icon(Icons.Default.CheckCircle, ...)

            Spacer(modifier = Modifier.height(32.dp))

            // --- Score Text ---
            Text(
                text = "Your Score",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )
            Text(
                text = "$score / $total",
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(32.dp))

            // --- Reward Text (Updated to match your screenshot) ---
            Text(
                text = "Congratulations!",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center
            )
            Text(
                // This now matches your screenshot
                text = "You've earned $coinsEarned KAL coins!",
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.weight(1f)) // Pushes button to bottom

            // --- Done Button ---
            Button(
                onClick = onNavigateBackToReading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text("Continue Reading")
            }
        }
    }
}
