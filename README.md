# NC Score Beta

**NC Score Beta** is a Kotlin-based Android application designed for viewing sheet music. It allows users to browse a library of songs, view details about artists, and access sheet music PDFs. The app integrates with Firebase for backend services such as authentication, database (Firestore), and logging.

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

The app follows a standard Android MVVM-like architecture (though simplified in some areas) using:
*   **Activities & Fragments**: For UI navigation and display.
*   **RecyclerView & Adapters**: For efficient list rendering (Songs, Artists).
*   **Firebase Firestore**: As the primary data source.
*   **Coroutines**: For background tasks like downloading PDFs.

## Future Roadmap

See `FUTURE_ROADMAP.md` for details on upcoming features, including interactive learning modes and MIDI support.

## License

[License Information Here]
