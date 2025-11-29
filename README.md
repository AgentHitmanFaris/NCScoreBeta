# NC Score Beta

**NC Score Beta** is a Kotlin-based Android application designed for viewing sheet music. It allows users to browse a library of songs, view details about artists, and access sheet music PDFs. The app integrates with Firebase for backend services such as authentication, database (Firestore), and logging.

## Features

*   **Sheet Music Viewer**: View PDF scores directly within the app. Supports standard URLs and Google Drive links.
*   **Library Management**: Browse a catalog of songs, filter by artist or title.
*   **Favorites**: Mark songs as favorites for quick access in your personal library.
*   **Artist Profiles**: View detailed biographies and song lists for individual artists.
*   **User Authentication**: Secure login and registration using Firebase Authentication.
*   **Premium Content**: Access control for premium scores based on user subscription status.
*   **Offline Support**: Caches data for offline browsing (using Firebase Persistence) and local storage for PDF files.
*   **Updates**: Built-in mechanism to check for and download app updates from GitHub.
*   **Bug Reporting**: Integrated system to send encrypted logs and device info for bug reports.

## Setup and Installation

### Prerequisites

*   Android Studio (latest version recommended)
*   Java Development Kit (JDK) 11 or higher
*   A Firebase project with:
    *   Authentication enabled (Email/Password)
    *   Firestore Database enabled

### Installation Steps

1.  **Clone the Repository**:
    ```bash
    git clone https://github.com/AgentHitmanFaris/NCScoreBeta.git
    cd NCScoreBeta
    ```

2.  **Configure Firebase**:
    *   Go to the Firebase Console and create a new project (or use an existing one).
    *   Add an Android app to your Firebase project.
    *   Download the `google-services.json` file.
    *   Place `google-services.json` in the `app/` directory of the project.

3.  **Build the Project**:
    *   Open the project in Android Studio.
    *   Let Gradle sync and download dependencies.
    *   Build the project: `Build > Make Project`.

4.  **Run the App**:
    *   Connect an Android device or start an emulator.
    *   Run the app: `Run > Run 'app'`.

## Usage

*   **Home**: The landing page showing new releases and trending scores.
*   **Browse**: Search for songs by title or artist. Use the search bar to filter results.
*   **Library**: View your favorite songs. You can filter your favorites using the search bar.
*   **Artists**: Browse a complete list of artists.
*   **Settings**: Manage your account, clear cache, check for updates, or report bugs.
*   **Viewing a Score**: Tap on any song card to open the sheet music details. Click "Open Score" to view the PDF.
*   **Offline Mode**: Enable "Offline Mode" in Settings to save viewed scores locally.

## Architecture

The app currently uses a standard Activity/Fragment pattern with direct Firestore integration.

*   **Activities**: `MainActivity` (navigation host), `LoginActivity` (auth), `PdfViewerActivity` (score view), `SplashActivity`.
*   **Fragments**: `HomeFragment`, `BrowseFragment`, `LibraryFragment`, `SettingsFragment`, `AllArtistsFragment`, `ArtistDetailFragment`, `SongDetailFragment`.
*   **Data Classes**: `Song`, `Artist`, `Arrangement`.
*   **Utilities**:
    *   `AppLogger`: Handles logging and bug report generation (with encryption).
    *   `UpdateManager`: Checks GitHub for app updates.
    *   `SongHandler`: Manages logic for opening scores (premium checks, offline handling).
    *   `FavoritesManager`: Manages local favorite song IDs.

**Note:** A refactor to MVVM is planned (see Roadmap).

## Future Roadmap & Todo List

We are actively working on transforming NC Score from a viewer into an interactive learning platform.

### Short-term Goals
- [ ] **Architecture Refactor**: Migrate from direct Activity logic to **MVVM** (ViewModel + Repository pattern) for improved stability and testability.
- [ ] **UI Modernization**: Begin gradual migration from XML Layouts to **Jetpack Compose**.
- [ ] **Gamification**: Add user XP, streaks, and "Days Practiced" tracking.

### Long-term Goals (Interactive Learning)
- [ ] **Data Migration**: Transition underlying data model from static PDFs to semantic formats (**MusicXML / MIDI**).
- [ ] **Phase 1 - MIDI Input**: Implement `android.media.midi` to detect notes played on connected digital pianos.
- [ ] **Phase 2 - Pitch Detection**: Prototype microphone-based note recognition (using libraries like TarsosDSP or Oboe) for acoustic instruments.
- [ ] **Phase 3 - Feedback Loop**: Build a scrolling score view that provides real-time feedback (Green/Red notes) based on user performance.

See `FUTURE_ROADMAP.md` for a deep dive into the technical strategy for these features.

## License

[License Information Here]
