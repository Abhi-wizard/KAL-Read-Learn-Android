# 🔍 KAL — Source Code & Project Report Consistency Audit

This audit document performs an exhaustive cross-reference between the active KAL Android codebase and the official academic project report ([`KAL-Project-Report.pdf`](./KAL-Project-Report.pdf)).

---

## 1. Features Documented in PDF & Verified in Source Code

| Feature / Subsystem | Report Reference | Source Implementation Reference | Status |
| :--- | :--- | :--- | :--- |
| **Authentication & Roles** | Section 3.1, Figure 3 | `ui/auth/AuthViewModel.kt`, `SignupScreen.kt`, `AuthorSignupScreen.kt` | ✅ **VERIFIED** |
| **PDF Reading Engine** | Section 4.1, Figure 4 | `ui/reading/ReadingViewModel.kt`, `ReadingScreen.kt` | ✅ **VERIFIED** |
| **AI Quiz Generation Pipeline** | Section 4.1, 5.3, Figure 5 | `functions/src/index.ts` (`generateQuiz`), `ui/quiz/QuizViewModel.kt` | ✅ **VERIFIED** |
| **Timed Quiz & Scoring** | Section 5.1, Figure 6 | `ui/quiz/QuizScreen.kt`, `QuizResultScreen.kt` | ✅ **VERIFIED** |
| **15-Puzzle Loading Minigame** | Section 5.1, Figure 22 | `ui/quiz/components/NumberSlidePuzzle.kt`, `PuzzleRouter.kt` | ✅ **VERIFIED** |
| **Coin Wallet Economy** | Section 1.4, Figure 21 | `ui/wallet/WalletViewModel.kt`, `WalletScreen.kt` | ✅ **VERIFIED** |
| **Creative Writing Publishing** | Section 4.1, Figure 7 | `functions/src/index.ts` (`publishWriting`), `ui/create_writing/` | ✅ **VERIFIED** |
| **Community Literature Feed** | Section 5.1, Figure 19 | `ui/explore/ExploreViewModel.kt`, `ExploreScreen.kt` | ✅ **VERIFIED** |
| **Author Book Promotion Feed** | Section 5.1, Figure 20 | `ui/promotions_feed/`, `ui/create_post/CreatePostViewModel.kt` | ✅ **VERIFIED** |
| **Book Reviews & Ratings** | Section 5.1, Figure 14 | `ui/details/BookDetailsViewModel.kt`, `BookDetailsScreen.kt` | ✅ **VERIFIED** |
| **User Profile & History** | Section 5.1, Figure 23 | `ui/profile/ProfileViewModel.kt`, `ProfileScreen.kt` | ✅ **VERIFIED** |

---

## 2. Features Implemented in Source but Not Explicitly Detailed in Report

1. **Offline Draft Management with Room SQLite (`data/local/`)**:
   - The report mentions creating writings, but the actual codebase features a complete **Room Database architecture** (`AppDatabase`, `DraftDao`, `Draft`) allowing writers to autosave and store drafts locally before spending coins to publish online.
2. **Ktor Client Byte Stream Engine (`ui/reading/ReadingViewModel.kt`)**:
   - PDF download and memory-efficient byte streaming is implemented using `io.ktor:ktor-client-android:2.3.9` with local cache checks (`$bookId.pdf`).
3. **Pluggable Minigame Router Architecture (`ui/quiz/components/PuzzleRouter.kt`)**:
   - While the report showcases the 15-puzzle, the code features a modular `PuzzleRouter` with enum-based dispatching (`FunPuzzleType.NUMBER_SLIDE`, `COLOR_SEQUENCE`, `QUICK_TAP`) designed for extensible minigame variety.
4. **Google Mobile Ads (AdMob) Rewarded Integration (`ui/quiz/QuizViewModel.kt`)**:
   - Rewarded video ad gating before quiz access is implemented directly in `QuizViewModel.kt` using Google AdMob SDK `23.1.0`.

---

## 3. Report Discrepancies & Implementation Evolution

| Topic | Description in Project Report | Actual Source Implementation | Resolution / Status |
| :--- | :--- | :--- | :--- |
| **Quiz Generation Trigger** | Described generally as "middleware" | Implemented as Firebase Cloud Functions v2 triggered specifically on `quizRequests/{requestId}` document creation | Cloud Function event trigger is more robust and serverless than generic middleware. |
| **AI Model Version** | References Google Gemini API | Code explicitly uses `gemini-2.5-pro` with safety threshold configurations (`HarmCategory.BLOCK_NONE`) | Documented accurately as Gemini 2.5 Pro. |
| **Coin Wallet Naming** | Referred to as "Vault" in early report figures | Named "Wallet" (`WalletScreen.kt`, `WalletViewModel.kt`) in the application code and navigation bar | Both terms documented; UI route is `wallet`. |
| **Coin Balances Location** | Described in early drafts as separate subcollection | Refactored in source code to main `users/{uid}.totalPoints` with atomic Firestore increments | Eliminates nested query latency and maintains atomic consistency. |

---

## 4. Visual Assets Extraction Summary

- **Total Diagrams Extracted**: 8 technical figures (1 System Architecture, 1 Database Schema, 6 Process Flowcharts).
- **Total Screenshots Extracted**: 15 full application UI screens covering Authentication, Reading, Quizzing, Wallet, Author Promotion, and Profiles.
- **Extraction Fidelity**: All assets extracted directly from internal PDF XObjects at native resolution without lossy re-encoding.

---

## 5. Audit Conclusion

The active KAL codebase matches the core objectives, architecture, workflows, and UI screens documented in the project report. Enhancements made in the codebase (such as Room SQLite offline caching, Cloud Functions v2 serverless triggers, and Dagger Hilt dependency injection) reflect production-grade software engineering best practices that surpass typical academic prototypes.
