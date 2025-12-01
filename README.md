# NC Score Beta

**NC Score Beta** is a Kotlin-based Android application designed for musicians to view, manage, and interact with sheet music. It leverages Firebase for backend services (Authentication, Firestore, Storage) and provides a seamless experience for browsing artist catalogs, managing personal libraries, and viewing PDF scores.

## Table of Contents
1. [Features](#features)
2. [Tech Stack](#tech-stack)
3. [Project Structure](#project-structure)
4. [Prerequisites](#prerequisites)
5. [Setup & Installation](#setup--installation)
6. [Usage Guide](#usage-guide)
7. [Architecture](#architecture)
8. [Roadmap](#roadmap)
9. [License](#license)

## Features

*   **Sheet Music Viewer**: High-performance PDF rendering for scores using `AndroidPdfViewer`. Supports standard URLs and automatic conversion of Google Drive links.
*   **Cloud Library**: Browse a vast catalog of songs, filtered by artists or titles.
*   **Personal Favorites**: Save songs to your local library for quick access.
*   **Artist Profiles**: Detailed artist pages with biographies and discographies.
*   **User Authentication**: Secure sign-up and login via Firebase Authentication.
*   **Offline Mode**: Cache scores locally to view them without an internet connection.
*   **Premium Access**: Support for premium content validation (subscription logic).
*   **Integrated Media**: View lyrics and watch embedded YouTube videos directly within the song details.
*   **Bug Reporting**: Built-in tool to generate and send encrypted system logs for debugging.
*   **Auto-Updates**: Checks the GitHub repository for new releases and supports in-app updates.

## Tech Stack

*   **Language**: Kotlin
*   **Platform**: Android (Min SDK 24)
*   **Backend**: Firebase (Authentication, Firestore Database)
*   **Libraries**:
    *   **Glide**: Image loading and caching.
    *   **AndroidPdfViewer** (`com.github.barteksc:android-pdf-viewer`): PDF rendering.
    *   **Coroutines**: Asynchronous programming.
    *   **AndroidX/Jetpack**: Core Android components (AppCompat, RecyclerView, Fragment, ConstraintLayout).

## Project Structure

The source code is located in `app/src/main/java/com/noobcompany/nc_scorebeta/`.

### Key Directories & Files

*   **Activities** (`*Activity.kt`):
    *   `MainActivity.kt`: The central hub hosting the bottom navigation and fragments.
    *   `LoginActivity.kt`: Handles user authentication.
    *   `PdfViewerActivity.kt`: specialized activity for rendering PDF scores.
    *   `SplashActivity.kt`: Launch screen with branding animation.
*   **Fragments** (`*Fragment.kt`):
    *   `HomeFragment.kt`: Landing page with trending songs and new releases.
    *   `BrowseFragment.kt`: Searchable grid of all available songs.
    *   `LibraryFragment.kt`: User's favorite songs.
    *   `SongDetailFragment.kt`: Song metadata, lyrics, and media.
    *   `SettingsFragment.kt`: User preferences and app management.
*   **Data Models**:
    *   `Song.kt`: Represents a music score entity.
    *   `Artist.kt`: Represents a composer or performer.
    *   `Arrangement.kt`: Detailed metadata for specific score versions.
*   **Utilities**:
    *   `SongHandler.kt`: Centralized logic for opening scores (handling premiums, offline files, etc.).
    *   `AppLogger.kt`: Custom logging utility with encryption for secure bug reporting.
    *   `UpdateManager.kt`: Auto-updater logic fetching releases from GitHub.
    *   `FavoritesManager.kt`: Local storage management for user favorites.

## Prerequisites

Before you begin, ensure you have the following:

*   **Android Studio**: Hedgehog (2023.1.1) or newer recommended.
*   **JDK**: Java Development Kit 11 or higher.
*   **Firebase Account**: You will need to set up a Firebase project.

## Setup & Installation

1.  **Clone the Repository**
    ```bash
    git clone https://github.com/AgentHitmanFaris/NCScoreBeta.git
    cd NCScoreBeta
    ```

2.  **Firebase Configuration**
    *   Go to the [Firebase Console](https://console.firebase.google.com/).
    *   Create a new project.
    *   Add an Android App with the package name `com.noobcompany.nc_scorebeta`.
    *   Download the `google-services.json` file.
    *   **Important**: Place the `google-services.json` file inside the `app/` directory of your project.
    *   Enable **Authentication** (Email/Password provider).
    *   Enable **Firestore Database**.

3.  **Build the Project**
    *   Open the project in Android Studio.
    *   Sync Gradle files (`File > Sync Project with Gradle Files`).
    *   Build the project (`Build > Make Project`).

4.  **Run the App**
    *   Connect an Android device (via USB) or create an Android Virtual Device (AVD).
    *   Click `Run > Run 'app'`.

## Usage Guide

*   **Navigation**: Use the bottom navigation bar to switch between Home, Browse, Artists, Library, and Settings.
*   **Viewing Scores**: Tap on any song card. In the details page, click "Open Score".
*   **Offline Mode**: Go to **Settings** and toggle "Offline Mode". When enabled, viewed scores are saved locally for future access without internet.
*   **Favorites**: Tap the "Star" icon on any song card to add it to your Library.
*   **Reporting Bugs**: In **Settings**, click "Report Bug". You can add a comment, and the app will generate an encrypted log file to send to the developers.

## Architecture

The application follows a standard **Single-Activity Architecture** (mostly) where `MainActivity` hosts multiple Fragments.

*   **UI Layer**: Fragments handle UI logic and user interaction.
*   **Data Layer**: Direct integration with Firebase Firestore within Fragments and Adapter callbacks. *Note: A migration to the Repository Pattern is planned.*
*   **Navigation**: Manual Fragment transactions are currently used.

## Roadmap

We have an ambitious roadmap to transform NC Score from a passive viewer into an interactive learning tool.

*   **Short-term**: MVVM Refactor, Jetpack Compose migration.
*   **Long-term**: MIDI Input support, Pitch Detection, and Gamification.

See [FUTURE_ROADMAP.md](FUTURE_ROADMAP.md) for detailed technical plans.

## License

[License Information]
<!-- Updated by Jules -->
