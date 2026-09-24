package com.example.kal.ui.quiz.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Defines the enum for all available puzzle types.
 */
enum class FunPuzzleType {
    NUMBER_SLIDE,   // Your existing 15-puzzle
    COLOR_SEQUENCE, // Placeholder for Memory/Visual puzzle
    QUICK_TAP       // Placeholder for Dexterity/Reaction puzzle
}

/**
 * Main router composable that randomly selects and displays a puzzle type.
 */
@Composable
fun PuzzleRouter() {
    // 1. Randomly select one puzzle type once per composition of the QuizLoading state
    val randomPuzzleType = remember {
        FunPuzzleType.entries.toTypedArray().random()
    }

    // 2. Route to the specific puzzle component
    when (randomPuzzleType) {
        FunPuzzleType.NUMBER_SLIDE -> NumberSlidePuzzle() // Your fixed 15-puzzle
        FunPuzzleType.COLOR_SEQUENCE -> PlaceholderPuzzle(title = "Color Sequence (Memory)")
        FunPuzzleType.QUICK_TAP -> PlaceholderPuzzle(title = "Quick Tap (Dexterity)")
    }
}

// Placeholder for un-implemented puzzles (will be replaced later)
@Composable
private fun PlaceholderPuzzle(title: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator()
        Text(text = "Preparing quiz...", modifier = Modifier.padding(bottom = 8.dp))
        Text(text = "Loading Puzzle: $title", style = MaterialTheme.typography.titleMedium)
        Text(text = "The quiz will start as soon as your device gets the questions!")
    }
}