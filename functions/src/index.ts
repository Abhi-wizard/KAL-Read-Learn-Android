// --- FILE: index.ts ---

import {onDocumentCreated} from "firebase-functions/v2/firestore";
import {onCall, HttpsError} from "firebase-functions/v2/https";
import * as admin from "firebase-admin";
import {
  GoogleGenerativeAI,
  GenerativeModel,
  HarmCategory,
  HarmBlockThreshold,
} from "@google/generative-ai";
import pdf from "pdf-parse";
import * as logger from "firebase-functions/logger";
import {FieldValue} from "firebase-admin/firestore";

// Initialize Firebase Admin
admin.initializeApp();
const db = admin.firestore();

/**
 * This function triggers when a new document is added to the `quizRequests` collection.
 * It generates a quiz using Gemini and updates the original request document.
 */
export const generateQuiz = onDocumentCreated(
  {
    document: "quizRequests/{requestId}",
    timeoutSeconds: 540,
    memory: "1GiB",
    region: "us-central1",
  },
  async (event) => {
    // 1. Get event data
    const snap = event.data;
    if (!snap) {
      logger.error("No data associated with the event.");
      return;
    }
    const requestId = event.params.requestId;
    const requestData = snap.data();
    const requestDocRef = db.doc(`quizRequests/${requestId}`);

    logger.info(`--- Processing quiz request: ${requestId} ---`);

    // --- Model Initialization ---
    let model: GenerativeModel;
    try {
      const geminiApiKey = process.env.GEMINI_API_KEY;
      if (!geminiApiKey) {
        throw new Error(
          "FATAL: GEMINI_API_KEY secret is not set in runtime environment."
        );
      }
      const genAI = new GoogleGenerativeAI(geminiApiKey);

      const safetySettings = [
        {
          category: HarmCategory.HARM_CATEGORY_HATE_SPEECH,
          threshold: HarmBlockThreshold.BLOCK_NONE,
        },
        {
          category: HarmCategory.HARM_CATEGORY_SEXUALLY_EXPLICIT,
          threshold: HarmBlockThreshold.BLOCK_NONE,
        },
        {
          category: HarmCategory.HARM_CATEGORY_HARASSMENT,
          threshold: HarmBlockThreshold.BLOCK_NONE,
        },
        {
          category: HarmCategory.HARM_CATEGORY_DANGEROUS_CONTENT,
          threshold: HarmBlockThreshold.BLOCK_NONE,
        },
      ];

      model = genAI.getGenerativeModel(
        {
          model: "gemini-2.5-pro",
          safetySettings: safetySettings,
        },
        {apiVersion: "v1beta"}
      );
    } catch (initError) {
      logger.error("Failed to initialize Gemini model:", initError);
      await requestDocRef.update({
        status: "error",
        errorMessage: "Internal server error: Could not initialize AI.",
        processedAt: FieldValue.serverTimestamp(),
      });
      return;
    }
    // --- END OF MODEL INIT ---

    try {
      // 3. Validate request data
      const {uid, bookId, endPage} = requestData as any;
      if (!uid || !bookId || !endPage) {
        throw new Error("Missing 'uid', 'bookId', or 'endPage' in request.");
      }

      // 4. Calculate the correct startPage
      const startPage = await getStartPage(uid, bookId);
      logger.info(`Calculated page range: ${startPage} to ${endPage}`);

      if (endPage < startPage) {
        logger.warn(
          `End page (${endPage}) is not after start page (${startPage}). Sending empty quiz.`
        );
        await requestDocRef.update({status: "complete", quiz: []});
        return;
      }

      // 5. Get PDF text
      const extractedText = await getPdfTextRange(bookId, startPage, endPage);
      if (extractedText.trim().length < 50) {
        throw new Error("Extracted text is too short to generate a quiz.");
      }
      logger.info(`Extracted ${extractedText.length} characters.`);

      // 6. Call Gemini API
      const prompt = getGeminiPrompt(extractedText.substring(0, 30000));
      logger.info("Calling Gemini AI...");

      const result = await model.generateContent(prompt);
      const response = result.response;
      let jsonString = response.text();
      logger.info("Received response from Gemini.");

      // 7. Clean and Parse JSON
      jsonString = jsonString
        .replace(/```json/g, "")
        .replace(/```/g, "")
        .trim();

      const quizData = JSON.parse(jsonString);

      if (!quizData.quiz) {
        throw new Error(
          "Gemini response was valid JSON, but missing 'quiz' key."
        );
      }
      logger.info(`Generated ${quizData.quiz.length} questions.`);

      // 8. Update Firestore with "complete" status (SUCCESS)
      await requestDocRef.update({
        status: "complete",
        quiz: quizData.quiz,
        processedAt: FieldValue.serverTimestamp(),
      });
      logger.info(`Successfully completed quiz request: ${requestId}`);
    } catch (error) {
      logger.error(`--- CRITICAL ERROR processing ${requestId}: ---`, error);
      let errorMessage = "An unknown server error occurred.";
      if (error instanceof Error) {
        errorMessage = error.message;
      }
      await requestDocRef.update({
        status: "error",
        errorMessage: errorMessage,
        processedAt: FieldValue.serverTimestamp(),
      });
    }
  }
);

// --- HELPER FUNCTIONS for generateQuiz ---

async function getStartPage(uid: string, bookId: string): Promise<number> {
  let lastQuizPage = 0;
  let contentStartPage = 1;

  try {
    const progressDocRef = db
      .collection("users")
      .doc(uid)
      .collection("readingProgress")
      .doc(bookId);
    const progressDoc = await progressDocRef.get();
    if (progressDoc.exists) {
      lastQuizPage = progressDoc.data()?.lastQuizPage || 0;
    }
  } catch (e) {
    logger.warn(`No reading progress found for user ${uid}, book ${bookId}.`);
  }

  try {
    const bookDoc = await db.doc(`books/${bookId}`).get();
    if (bookDoc.exists) {
      contentStartPage = bookDoc.data()?.contentStartPage || 1;
    } else {
      throw new Error(`Book document ${bookId} not found.`);
    }
  } catch (e) {
    logger.error(`Failed to get book doc: ${bookId}`, e);
    throw e;
  }
  return Math.max(lastQuizPage, contentStartPage - 1) + 1;
}

async function getPdfTextRange(
  bookId: string,
  startPage: number,
  endPage: number
): Promise<string> {
  const bookDoc = await db.doc(`books/${bookId}`).get();
  const storagePath = bookDoc.data()?.storagePath;
  if (!storagePath) {
    throw new Error(`No 'storagePath' found on book document ${bookId}.`);
  }
  logger.info(`Downloading from storage: ${storagePath}`);

  const bucket = admin.storage().bucket();
  const [fileBuffer] = await bucket.file(storagePath).download();
  logger.info("File downloaded. Parsing PDF text...");

  type PageData = {
    pageIndex: number;
    getTextContent: () => Promise<TextContent>;
  };
  type TextContent = {
    items: TextItem[];
  };
  type TextItem = {
    str: string;
  };

  const options = {
    pagerender: (pageData: PageData) => {
      const pageNumber = pageData.pageIndex + 1;
      if (pageNumber >= startPage && pageNumber <= endPage) {
        return pageData.getTextContent().then((textContent: TextContent) => {
          return textContent.items.map((item: TextItem) => item.str).join(" ");
        });
      }
      return "";
    },
  };

  const pdfData = await pdf(fileBuffer, options);
  return pdfData.text;
}

function getGeminiPrompt(extractedText: string): string {
  return `
You are an expert quiz creator for an educational app.
Your task is to generate a JSON object containing a list of 5 multiple-choice questions.

RULES:
1.   Base the questions *ONLY* on the provided text. Do not use any external knowledge.
2.  The JSON output MUST have a single root key: "quiz".
3.  The "quiz" key must be an array of 5 "QuizQuestion" objects.
4.  Each "QuizQuestion" object MUST have these 4 keys:
    - "question": The string for the question text.
    - "type": The string "mcq".
    - "options": An array of 4 unique strings (the possible answers).
    - "correctAnswer": The string that exactly matches the correct option.
5.  If the text is too short or unclear, return an empty array: { "quiz": [] }
6.  Respond *ONLY* with the raw JSON object. Do not include "'''json", "'''", or any other text, warnings, or explanations.
7.  IMPORTANT: Generate a new, unique set of questions. Vary the question topics, styles, and difficulty. Do not generate the same questions you may have generated for this text in the past.

TEXT TO ANALYZE:
"${extractedText}"
`;
}

// --- ⬇️ NEW FUNCTIONS ADDED BELOW ⬇️ ---

/**
 * 1. UNLOCK WRITING FEATURE (30 Coins)
 * FIX: Prevents repeated charging by checking isWritingUnlocked status.
 */
export const unlockWritingFeature = onCall(async (request) => {
  if (!request.auth) {
    throw new HttpsError("unauthenticated", "You must be logged in.");
  }
  const uid = request.auth.uid;
  const userRef = db.doc(`users/${uid}`);

  logger.info(`Processing 'unlockWritingFeature' for user: ${uid}`);

  try {
    return await db.runTransaction(async (transaction) => {
      const userDoc = await transaction.get(userRef);
      if (!userDoc.exists) {
        throw new HttpsError("not-found", "User document not found.");
      }

      const userData = userDoc.data();
      const isUnlocked = userData?.isWritingUnlocked || false; // <-- CRITICAL: Get status

      // --- FIX: CHECK IF ALREADY UNLOCKED ---
      if (isUnlocked === true) {
        // If already paid, do nothing but return success.
        return {status: "success", unlocked: true, message: "Feature already unlocked."};
      }
      // --- END FIX ---

      const coinBalance = userData?.totalPoints || 0;
      if (coinBalance < 30) {
        throw new HttpsError(
          "failed-precondition",
          "Not enough coins. You need 30."
        );
      }

      // Perform coin deduction and set the flag to true
      transaction.update(userRef, {
        totalPoints: FieldValue.increment(-30),
        isWritingUnlocked: true, // This flag is set ONCE
      });

      return {status: "success", unlocked: true};
    });
  } catch (error) {
    logger.error(`Error unlocking feature for ${uid}:`, error);
    throw error;
  }
});

/**
 * 2. PUBLISH WRITING (50 Coins)
 * (FIXED: Returns the transaction to ensure it completes)
 */
export const publishWriting = onCall(async (request) => {
  if (!request.auth) {
    throw new HttpsError("unauthenticated", "You must be logged in.");
  }
  const uid = request.auth.uid;

  const {title, content, genre, language} = request.data;
  if (!title || !content || !genre || !language) {
    throw new HttpsError("invalid-argument", "Missing required fields.");
  }

  logger.info(`Processing 'publishWriting' for user: ${uid}, title: ${title}`);

  const userRef = db.doc(`users/${uid}`);
  const newWritingRef = db.collection("writings").doc();

  const publishCost = 50; // Define the cost once

  try {
    return await db.runTransaction(async (transaction) => {
      const userDoc = await transaction.get(userRef);

      if (!userDoc.exists) {
        throw new HttpsError("not-found", "User not found.");
      }

      const userData = userDoc.data();

      // --- CRUCIAL FIX: Removed the redundant 30-coin isWritingUnlocked check.
      // We only check the 50-coin balance for publication fee.
      const coinBalance = userData?.totalPoints || 0;
      if (coinBalance < publishCost) {
        throw new HttpsError(
          "failed-precondition",
          `Not enough coins. You need ${publishCost} to publish.`
        );
      }

      const authorName = userData?.name || "Unknown Author";

      // Perform updates: Deduct 50 coins atomically
      transaction.update(userRef, {
        totalPoints: FieldValue.increment(-publishCost),
      });

      // Create the new story document
      const newStory = {
        uid: uid,
        authorName: authorName,
        title: title,
        content: content,
        genre: genre,
        language: language,
        status: "published",
        createdAt: FieldValue.serverTimestamp(),
        likeCount: 0,
      };
      transaction.set(newWritingRef, newStory);

      // Return success from within the transaction
      return {status: "success", writingId: newWritingRef.id};
    });
  } catch (error) {
    logger.error(`Error publishing story for ${uid}:`, error);
    throw error;
  }
});
