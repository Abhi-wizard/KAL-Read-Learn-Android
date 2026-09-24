package com.example.kal.ui.quiz

import android.app.Activity
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.kal.data.QuizQuestion
import com.example.kal.ui.quiz.components.PuzzleRouter // <-- IMPORT THE NEW ROUTER

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizScreen(
    bookId: String,
    endPage: Int,
    onNavigateBack: () -> Unit,
    onNavigateToResult: (score: Int, total: Int, bookId: String) -> Unit,
    viewModel: QuizViewModel = hiltViewModel()
) {
    val activity = (LocalContext.current as? Activity)
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadRewardedAd()
    }

    val finishedState by viewModel.quizFinishedState.collectAsState()
    LaunchedEffect(finishedState) {
        finishedState?.let { (score, total) ->
            Log.d("QuizScreen", "Quiz finished state observed. Navigating to result.")
            onNavigateToResult(score, total, bookId)
        }
    }

    BackHandler(uiState is QuizUiState.Success) {
        Log.d("QuizScreen", "Back press disabled during quiz.")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val title = when (uiState) {
                        is QuizUiState.Success -> "Quiz Time!"
                        is QuizUiState.QuizLoading -> "Preparing Your Quiz..."
                        is QuizUiState.ReadyToShowAd -> "Unlock Quiz"
                        else -> "Loading..."
                    }
                    Text(title)
                },
                navigationIcon = {
                    if (uiState !is QuizUiState.QuizLoading && uiState !is QuizUiState.Success) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            when (val state = uiState) {
                is QuizUiState.Idle, is QuizUiState.AdLoading -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Loading Ad...")
                    }
                }

                is QuizUiState.AdFailedToLoad -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Failed to load ad. Please try again.")
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.loadRewardedAd() }) {
                            Text("Try Again")
                        }
                    }
                }

                is QuizUiState.ReadyToShowAd -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Watch a short ad to unlock your quiz.",
                            style = MaterialTheme.typography.headlineSmall,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(bottom = 24.dp)
                        )
                        if (activity != null) {
                            Button(
                                onClick = {
                                    viewModel.showRewardedAd(activity, bookId, endPage)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                            ) {
                                Text("Watch Ad to Start Quiz")
                            }
                        } else {
                            Text("Error: Cannot show ad.", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }

                // --- CALL THE NEW PUZZLE ROUTER HERE ---
                is QuizUiState.QuizLoading -> {
                    // This router handles the random selection of the puzzle type.
                    PuzzleRouter()
                }

                is QuizUiState.Error -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Error: ${state.message}", color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onNavigateBack) {
                            Text("Go Back")
                        }
                    }
                }

                // --- SUCCESS STATE (UNCHANGED) ---
                is QuizUiState.Success -> {
                    val currentQuestion by viewModel.currentQuestion.collectAsState()
                    val questionIndex by viewModel.currentQuestionIndex.collectAsState()
                    val totalQuestions by viewModel.totalQuestionCount.collectAsState()
                    val timeLeft by viewModel.timeLeft.collectAsState()
                    val selectedAnswer by viewModel.selectedAnswer.collectAsState()

                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (currentQuestion != null) {
                            TimerAndProgress(
                                timeLeft = timeLeft,
                                questionIndex = questionIndex,
                                totalQuestions = totalQuestions
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = currentQuestion!!.question,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(0.5f)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                currentQuestion!!.options.forEach { option ->
                                    OptionItem(
                                        text = option,
                                        isSelected = (option == selectedAnswer),
                                        onOptionSelected = { viewModel.onAnswerSelected(option) }
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = { viewModel.goToNextQuestion() },
                                enabled = selectedAnswer != null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                            ) {
                                Text(
                                    if (questionIndex + 1 < totalQuestions) "Next"
                                    else "Finish"
                                )
                            }
                        } else {
                            CircularProgressIndicator()
                        }
                    }
                }
            }
        }
    }
}

// --- NEW Composable for the timer and progress text (UNCHANGED) ---
@Composable
private fun TimerAndProgress(
    timeLeft: Int,
    questionIndex: Int,
    totalQuestions: Int
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LinearProgressIndicator(
            progress = { timeLeft / 45f },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Time Left: $timeLeft s",
            style = MaterialTheme.typography.titleMedium,
            color = if (timeLeft <= 10) MaterialTheme.colorScheme.error else LocalContentColor.current
        )
        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Question ${questionIndex + 1} of $totalQuestions",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

// --- NEW Composable for a single option (UNCHANGED) ---
@Composable
private fun OptionItem(
    text: String,
    isSelected: Boolean,
    onOptionSelected: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .selectable(
                selected = isSelected,
                onClick = onOptionSelected,
                role = Role.RadioButton
            )
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = null
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = text, style = MaterialTheme.typography.bodyLarge)
    }
}