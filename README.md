# NC Score Beta

**NC Score Beta** is a Kotlin-based Android application designed for viewing sheet music. It allows users to browse a library of songs, view details about artists, and access sheet music PDFs. The app integrates with Firebase for backend services such as authentication, database (Firestore), and logging.

## Recent Changes (v1.5.0)
*   **Enhanced UI**: New Netflix-style animated splash screen.
*   **Robust Media Player**: Fixed YouTube "Video Unavailable" (Error 4) issues by implementing smart video ID extraction and robust embed URL handling. Mapped Firestore `video` field for direct integration.
*   **Advanced Bug Reporting**: Users can now add comments to bug reports. Log encryption and compression are now handled in the background for smoother performance.
*   **Smart Updates**: Implemented Semantic Versioning (SemVer) logic to correctly identify and prompt for available updates.

## Features

*   **Sheet Music Viewer**: View PDF scores directly within the app. Supports standard URLs and Google Drive links.
*   **Library Management**: Browse a catalog of songs, filter by artist or title.
*   **Favorites**: Mark songs as favorites for quick access in your personal library.
*   **Artist Profiles**: View detailed biographies and song lists for individual artists.
*   **User Authentication**: Secure login and registration using Firebase Authentication.
*   **Premium Content**: Access control for premium scores based on user subscription status.
*   **Offline Support**: Caches data for offline browsing (using Firebase Persistence).
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
*   **Browse**: Search for songs by title or artist.
*   **Library**: View your favorite songs.
*   **Settings**: Manage your account, clear cache, check for updates, or report bugs.
*   **Viewing a Score**: Tap on any song card to open the sheet music. If it's a premium score, you may need to log in.

## Architecture

The app currently uses a standard Activity/Fragment pattern with direct Firestore integration. 

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
