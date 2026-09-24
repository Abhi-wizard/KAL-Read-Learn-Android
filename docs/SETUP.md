# 🛠️ KAL — Setup & Developer Guide

This guide walks you through setting up, configuring, building, and running the **KAL — Read • Learn • Earn** Android project and its associated Firebase Cloud Functions backend.

---

## 📋 Prerequisites

Before you begin, ensure you have the following installed:

- **Java Development Kit (JDK)**: JDK 17 (Eclipse Adoptium OpenJDK 17 recommended)
- **Android Studio**: Android Studio Hedgehog (2023.1.1) or newer
- **Android SDK**: SDK Platform 34 (Android 14) with build-tools 34.0.0
- **Node.js**: Node.js 18 or 20+ (for Firebase Cloud Functions)
- **Firebase CLI**: `npm install -g firebase-tools`

---

## 🚀 1. Repository Setup

### Clone the Repository
```bash
git clone https://github.com/YOUR_USERNAME/KAL-Read-Learn-Android.git
cd KAL-Read-Learn-Android
```

### Configure Android SDK (`local.properties`)
Create a `local.properties` file in the project root based on [`local.properties.example`](file:///e:/kal/local.properties.example):

**Windows:**
```properties
sdk.dir=C\:\\Users\\YOUR_USERNAME\\AppData\\Local\\Android\\Sdk
```

**macOS / Linux:**
```properties
sdk.dir=/Users/YOUR_USERNAME/Library/Android/sdk
```

---

## 🔥 2. Firebase Backend Configuration

KAL uses Firebase for Authentication, Cloud Firestore, Cloud Storage, and Cloud Functions.

### Step 1: Create a Firebase Project
1. Go to the [Firebase Console](https://console.firebase.google.com/).
2. Create a new Firebase project named `kal-read-earn` (or your preferred name).
3. Add an **Android Application** to your project:
   - **Package name**: `com.example.kal`
   - **App nickname**: `KAL`
   - **SHA-1 fingerprint**: (Add your debug SHA-1 from `./gradlew signingReport` for Google Sign-In)

### Step 2: Add `google-services.json`
1. Download the generated `google-services.json` from the Firebase Console.
2. Place it into the `app/` directory:
   ```
   KAL-Read-Learn-Android/
   └── app/
       └── google-services.json
   ```
   *(A reference template is available in [`app/google-services.json.example`](file:///e:/kal/app/google-services.json.example))*

### Step 3: Enable Firebase Services
In the Firebase Console, enable:
- **Authentication**: Email/Password and Google Sign-In providers.
- **Cloud Firestore**: Create a database in Production or Test mode.
- **Cloud Storage**: Create default storage bucket for PDFs and cover images.
- **Cloud Functions**: Ensure your Firebase project is on the Blaze (pay-as-you-go) plan to allow external API calls to Google Gemini.

---

## 🧠 3. Cloud Functions & Gemini AI Setup

KAL's AI-driven quiz generation runs on Firebase Cloud Functions powered by Google Gemini 2.5 Pro.

### Step 1: Obtain a Gemini API Key
1. Visit [Google AI Studio](https://aistudio.google.com/).
2. Generate an API Key.

### Step 2: Configure Environment Variables
Inside the `functions/` directory, create a `.env` file based on [`functions/.env.example`](file:///e:/kal/functions/.env.example):
```env
GEMINI_API_KEY=your_actual_gemini_api_key_here
```

### Step 3: Install Dependencies & Deploy
```bash
# Login to Firebase
firebase login

# Select your project
firebase use --add

# Install function dependencies
cd functions
npm install

# Build and deploy Cloud Functions
npm run deploy
```

---

## 📱 4. Build and Run the Android Application

### Build via Command Line
```powershell
# Set Java 17 environment if not globally set
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot"

# Compile Kotlin sources
.\gradlew.bat compileDebugKotlin

# Assemble Debug APK
.\gradlew.bat assembleDebug
```
The compiled APK will be generated at: `app/build/outputs/apk/debug/app-debug.apk`.

### Run from Android Studio
1. Launch **Android Studio**.
2. Select **Open** and choose the `KAL-Read-Learn-Android` root directory.
3. Wait for Gradle sync to complete.
4. Select an emulator or connected physical device (API level 26+).
5. Click **Run 'app'** (`Shift + F10`).

---

## 🧪 5. Testing AdMob Ads

KAL includes test AdMob configurations for Rewarded Ads:
- **Application ID** (in `AndroidManifest.xml`): `ca-app-pub-8991017264402019~3392206865`
- **Rewarded Ad Unit ID** (in `QuizViewModel.kt`): Google standard test ad unit `ca-app-pub-3940256099942544/5224354917`

During development and testing, standard Google test ad responses will be served automatically.

---

## ❓ 6. Troubleshooting

| Issue | Cause | Solution |
| :--- | :--- | :--- |
| `JAVA_HOME is set to an invalid directory` | JDK 17 environment variable path mismatch | Set `JAVA_HOME` pointing to your installed JDK 17 root directory before invoking `./gradlew`. |
| `File google-services.json is missing` | Missing client Firebase config | Place the downloaded `google-services.json` from Firebase Console in the `app/` folder. |
| `FATAL: GEMINI_API_KEY secret is not set` | Missing Cloud Function API key | Create `functions/.env` containing `GEMINI_API_KEY=...` and redeploy functions via `firebase deploy --only functions`. |
| `Room schema export warning` | Room compiler warning | Room database runs correctly with entity schema version 1. |
