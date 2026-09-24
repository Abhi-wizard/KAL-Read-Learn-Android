package com.example.kal.ui.quiz.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.random.Random

private const val GRID_SIZE = 4
private const val SHUFFLE_MOVES = 100 // Number of random moves to shuffle

@Composable
fun NumberSlidePuzzle() {
    // The solved state (0 represents the blank space)
    val solvedState = (1..15).toList() + 0
    // FIX: Use Random(System.currentTimeMillis()) for a unique shuffle seed
    val randomGenerator = remember { Random(System.currentTimeMillis()) }

    var puzzleState by remember { mutableStateOf(solvedState) }
    var isShuffling by remember { mutableStateOf(true) }

    // This effect runs once per composition to shuffle the puzzle
    LaunchedEffect(Unit) {
        puzzleState = shufflePuzzle(solvedState, randomGenerator)
        isShuffling = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // --- Instructions Text ---
        Text(
            text = "Preparing your quiz...",
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.padding(8.dp))
        Text(
            text = if (isShuffling) "Shuffling puzzle..." else "Solve this puzzle while you wait!",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.padding(16.dp))

        // --- The Puzzle Grid ---
        LazyVerticalGrid(
            columns = GridCells.Fixed(GRID_SIZE),
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f) // Makes the grid a square
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            itemsIndexed(puzzleState) { index, tileValue ->
                if (tileValue > 0) {
                    // This is a numbered tile
                    PuzzleTile(
                        number = tileValue,
                        onClick = {
                            if (!isShuffling) {
                                // Try to move the tile
                                puzzleState = moveTile(puzzleState, index)
                            }
                        }
                    )
                } else {
                    // This is the blank tile (0)
                    BlankTile()
                }
            }
        }

        // --- Win Condition ---
        if (puzzleState == solvedState && !isShuffling) {
            Spacer(modifier = Modifier.padding(16.dp))
            Text(
                text = "Great job!",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun PuzzleTile(number: Int, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .aspectRatio(1f)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$number",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun BlankTile() {
    Spacer(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(4.dp)
    )
}

// --- Puzzle Logic ---

/**
 * Attempts to move the tile at the given [clickedIndex].
 * Returns the new state if the move is valid, or the old state if not.
 */
private fun moveTile(currentState: List<Int>, clickedIndex: Int): List<Int> {
    val blankIndex = currentState.indexOf(0)
    if (blankIndex == -1) return currentState // Should never happen

    if (isAdjacent(clickedIndex, blankIndex)) {
        // Swap the clicked tile and the blank tile
        val newState = currentState.toMutableList()
        newState[blankIndex] = currentState[clickedIndex]
        newState[clickedIndex] = 0
        return newState
    }
    return currentState // Not a valid move
}

/**
 * Checks if two indices in the 4x4 grid are adjacent (not diagonally).
 */
private fun isAdjacent(index1: Int, index2: Int): Boolean {
    val row1 = index1 / GRID_SIZE
    val col1 = index1 % GRID_SIZE
    val row2 = index2 / GRID_SIZE
    val col2 = index2 % GRID_SIZE

    val rowDiff = kotlin.math.abs(row1 - row2)
    val colDiff = kotlin.math.abs(col1 - col2)

    return (rowDiff == 1 && colDiff == 0) || (rowDiff == 0 && colDiff == 1)
}

/**
 * Shuffles the puzzle by performing a number of random, valid moves.
 * This guarantees the puzzle is always solvable.
 */
private suspend fun shufflePuzzle(initialState: List<Int>, randomGenerator: Random): List<Int> {
    var state = initialState
    var blankIndex = state.indexOf(0)

    for (i in 0 until SHUFFLE_MOVES) {
        val possibleMoves = mutableListOf<Int>()

        // Check neighbors
        if (blankIndex - GRID_SIZE >= 0) possibleMoves.add(blankIndex - GRID_SIZE) // Up
        if (blankIndex + GRID_SIZE < 16) possibleMoves.add(blankIndex + GRID_SIZE) // Down
        if (blankIndex % GRID_SIZE > 0) possibleMoves.add(blankIndex - 1) // Left
        if (blankIndex % GRID_SIZE < 3) possibleMoves.add(blankIndex + 1) // Right

        // Pick a random valid move
        val tileToMoveIndex = possibleMoves.random(randomGenerator)

        // Perform the swap
        val newState = state.toMutableList()
        newState[blankIndex] = state[tileToMoveIndex]
        newState[tileToMoveIndex] = 0

        state = newState
        blankIndex = tileToMoveIndex
    }
    return state
}