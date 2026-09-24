package com.example.kal.ui.quiz

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kal.data.QuizQuestion
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

// --- UI State Machine (No changes) ---
sealed class QuizUiState {
    object Idle : QuizUiState()
    object AdLoading : QuizUiState()
    object AdFailedToLoad : QuizUiState()
    object ReadyToShowAd : QuizUiState()
    object QuizLoading : QuizUiState()
    data class Success(val questions: List<QuizQuestion>) : QuizUiState()
    data class Error(val message: String) : QuizUiState()
}

@HiltViewModel
class QuizViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val TAG = "QuizViewModel"

    // --- Ad and General UI State ---
    private val _uiState = MutableStateFlow<QuizUiState>(QuizUiState.Idle)
    val uiState = _uiState.asStateFlow()

    private var rewardedAd: RewardedAd? = null
    private val adUnitId = "ca-app-pub-3940256099942544/5224354917" // Test Ad ID
    private var quizRequestListener: ListenerRegistration? = null

    // --- NEW QUIZ STATE VARIABLES ---
    private val _totalQuestionCount = MutableStateFlow(0)
    val totalQuestionCount = _totalQuestionCount.asStateFlow()

    private val _currentQuestionIndex = MutableStateFlow(0)
    val currentQuestionIndex = _currentQuestionIndex.asStateFlow()

    private val _currentQuestion = MutableStateFlow<QuizQuestion?>(null)
    val currentQuestion = _currentQuestion.asStateFlow()

    private val _timeLeft = MutableStateFlow(45)
    val timeLeft = _timeLeft.asStateFlow()

    private val _selectedAnswer = MutableStateFlow<String?>(null)
    val selectedAnswer = _selectedAnswer.asStateFlow()

    // This state triggers navigation to the result screen
    // Pair<Score, Total>
    private val _quizFinishedState = MutableStateFlow<Pair<Int, Int>?>(null)
    val quizFinishedState = _quizFinishedState.asStateFlow()

    // --- Private properties to manage the quiz ---
    private var allQuestions: List<QuizQuestion> = emptyList()
    private val userAnswers = mutableMapOf<Int, String>() // Map<QuestionIndex, SelectedAnswer>
    private var timerJob: Job? = null
    private var currentBookId: String = ""
    private var currentEndPage: Int = 0

    // --- Ad Functions (No changes) ---
    fun loadRewardedAd() {
        if (rewardedAd != null || _uiState.value == QuizUiState.AdLoading) return
        _uiState.value = QuizUiState.AdLoading
        Log.d(TAG, "Loading rewarded ad...")
        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(context, adUnitId, adRequest, object : RewardedAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                Log.e(TAG, "Ad failed to load: ${adError.message}")
                rewardedAd = null
                _uiState.value = QuizUiState.AdFailedToLoad
            }

            override fun onAdLoaded(ad: RewardedAd) {
                Log.d(TAG, "Ad was loaded successfully.")
                rewardedAd = ad
                _uiState.value = QuizUiState.ReadyToShowAd
            }
        })
    }

    fun showRewardedAd(activity: Activity, bookId: String, endPage: Int) {
        if (rewardedAd != null) {
            rewardedAd?.show(activity) { rewardItem ->
                Log.d(TAG, "User earned reward. Starting quiz generation.")
                // Call the updated startQuizGeneration
                startQuizGeneration(bookId, endPage)
            }
            rewardedAd = null
            loadRewardedAd()
        } else {
            Log.e(TAG, "Ad was not ready to show.")
            _uiState.value = QuizUiState.AdFailedToLoad
            loadRewardedAd()
        }
    }

    // --- Quiz Generation (UPDATED) ---
    private fun startQuizGeneration(bookId: String, endPage: Int) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            _uiState.value = QuizUiState.Error("User not logged in.")
            return
        }

        // --- Store parameters for later ---
        this.currentBookId = bookId
        this.currentEndPage = endPage
        // ---

        _uiState.value = QuizUiState.QuizLoading
        Log.d(TAG, "Placing quiz request in Firestore...")

        viewModelScope.launch {
            try {
                val quizRequestRef = db.collection("quizRequests").document()
                val requestData = hashMapOf(
                    "uid" to userId, "bookId" to bookId, "endPage" to endPage,
                    "status" to "pending", "createdAt" to FieldValue.serverTimestamp()
                )

                quizRequestRef.set(requestData).await()
                Log.d(TAG, "Quiz request sent (ID: ${quizRequestRef.id}). Attaching listener.")

                quizRequestListener = quizRequestRef.addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(TAG, "Listener error", error)
                        _uiState.value = QuizUiState.Error(error.message ?: "Listener failed")
                        quizRequestListener?.remove()
                        return@addSnapshotListener
                    }

                    if (snapshot != null && snapshot.exists()) {
                        val status = snapshot.getString("status")
                        Log.d(TAG, "Received update: status is '$status'")

                        when (status) {
                            "pending" -> { /* Server is still working */ }
                            "complete" -> {
                                val questions = parseQuestions(snapshot.get("quiz"))
                                if (questions.isEmpty()) {
                                    Log.e(TAG, "Quiz is complete but no questions found.")
                                    _uiState.value = QuizUiState.Error("Failed to parse quiz questions.")
                                } else {
                                    Log.d(TAG, "Successfully parsed ${questions.size} questions.")

                                    // --- NEW QUIZ START LOGIC ---
                                    allQuestions = questions
                                    _totalQuestionCount.value = questions.size
                                    _currentQuestionIndex.value = 0
                                    _currentQuestion.value = questions[0]
                                    userAnswers.clear() // Clear any previous answers

                                    // Set Success state to tell UI to show the quiz
                                    _uiState.value = QuizUiState.Success(questions)

                                    startTimer() // Start timer for the first question
                                    // ---
                                }
                                quizRequestListener?.remove()
                            }
                            "error" -> {
                                val message = snapshot.getString("errorMessage")
                                _uiState.value = QuizUiState.Error(message ?: "Unknown server error")
                                quizRequestListener?.remove()
                            }
                        }
                    } else {
                        Log.w(TAG, "Quiz request document deleted or does not exist.")
                        _uiState.value = QuizUiState.Error("Quiz request not found.")
                        quizRequestListener?.remove()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error writing quiz request", e)
                _uiState.value = QuizUiState.Error(e.message ?: "Failed to start quiz")
            }
        }
    }

    // --- NEW TIMER AND NAVIGATION FUNCTIONS ---

    private fun startTimer() {
        timerJob?.cancel() // Cancel any old timer
        _timeLeft.value = 45 // Reset to 45 seconds
        timerJob = viewModelScope.launch {
            while (_timeLeft.value > 0) {
                delay(1000L)
                _timeLeft.value--
            }
            // Time's up!
            Log.d(TAG, "Time is up for question ${_currentQuestionIndex.value}")
            goToNextQuestion()
        }
    }

    // Called by the UI when a radio button is clicked
    fun onAnswerSelected(answer: String) {
        _selectedAnswer.value = answer
    }

    // Called by the UI when the "Next" button is clicked or timer hits 0
    fun goToNextQuestion() {
        timerJob?.cancel() // Stop the timer

        // Record the answer (or "No Answer" if null/timeout)
        val answer = _selectedAnswer.value ?: "No Answer"
        userAnswers[_currentQuestionIndex.value] = answer

        val nextIndex = _currentQuestionIndex.value + 1

        if (nextIndex < allQuestions.size) {
            // --- More questions left ---
            Log.d(TAG, "Moving to question $nextIndex")
            _currentQuestionIndex.value = nextIndex
            _currentQuestion.value = allQuestions[nextIndex]
            _selectedAnswer.value = null // Clear selection for new question
            startTimer() // Start timer for the new question
        } else {
            // --- This was the last question ---
            Log.d(TAG, "Last question answered. Finishing quiz.")
            finishQuiz()
        }
    }

    private fun finishQuiz() {
        // Calculate the score
        var score = 0
        for ((index, selectedAnswer) in userAnswers) {
            if (allQuestions[index].correctAnswer == selectedAnswer) {
                score++
            }
        }

        Log.d(TAG, "Quiz finished. Score: $score / ${allQuestions.size}")

        // Save the result (this function is corrected below)
        saveQuizResult(currentBookId, currentEndPage, score, allQuestions.size)

        // Set the finished state to trigger navigation in the UI
        _quizFinishedState.value = Pair(score, allQuestions.size)
    }

    // --- Helper Function (No changes) ---
    @Suppress("UNCHECKED_CAST")
    private fun parseQuestions(data: Any?): List<QuizQuestion> {
        return try {
            if (data !is List<*>) return emptyList()
            data.mapNotNull { item ->
                if (item !is Map<*, *>) return@mapNotNull null
                val question = item["question"] as? String
                val type = item["type"] as? String
                val options = item["options"] as? List<String>
                val correctAnswer = item["correctAnswer"] as? String
                if (question != null && type != null && options != null && correctAnswer != null) {
                    QuizQuestion(question, type, options, correctAnswer)
                } else {
                    Log.w(TAG, "Skipping invalid question object: $item")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse questions: $e")
            emptyList()
        }
    }

    // --- FIX: Save Quiz Result Function ---
    fun saveQuizResult(bookId: String, endPage: Int, score: Int, total: Int) {
        val userId = auth.currentUser?.uid ?: return
        val coinsEarned = score * 10
        Log.d(TAG, "Saving quiz result. Book: $bookId, Score: $score/$total, Coins: $coinsEarned")

        // --- 1. Reference the main user document ---
        val userDocRef = db.collection("users").document(userId)

        // --- 2. Reference the reading progress subcollection document ---
        val progressDocRef = userDocRef.collection("readingProgress").document(bookId)

        viewModelScope.launch {
            try {
                db.runBatch { batch ->

                    // FIX: Update the main 'totalPoints' field directly
                    if (coinsEarned > 0) {
                        batch.update(
                            userDocRef,
                            "totalPoints",
                            FieldValue.increment(coinsEarned.toLong())
                        )
                    }

                    // Update reading progress (unchanged and correct)
                    val progressUpdate = hashMapOf(
                        "lastQuizPage" to endPage,
                        "lastQuizScore" to "$score/$total",
                        "lastQuizTakenAt" to FieldValue.serverTimestamp()
                    )
                    batch.set(progressDocRef, progressUpdate, SetOptions.merge())
                }.await()
                Log.d(TAG, "Successfully saved quiz progress and updated totalPoints.")

                // --- Manual fix instruction reminder (not in code) ---
                // REMINDER: Manually delete the '/vault/main' subcollection in Firestore

            } catch (e: Exception) {
                Log.w(TAG, "Error saving quiz/wallet data", e)
            }
        }
    }

    // --- Clean up (UPDATED) ---
    override fun onCleared() {
        super.onCleared()
        quizRequestListener?.remove() // Remove listener
        timerJob?.cancel() // --- Cancel timer ---
        Log.d(TAG, "ViewModel cleared, listener and timer cancelled.")
    }
}