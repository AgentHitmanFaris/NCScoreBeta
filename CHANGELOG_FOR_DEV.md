# Changelog for Developers

## [1.6.0] - 2026-05-20

### Added
- **Database Schema Sync**: Fully mapped models to `firestore-clean.json`. Added support for `artistIds`, `bpm`, `originalKey`, `isFeatured`, and `isComingSoon` in the `Song` model.
- **Arrangement Metadata**: Added `difficulty`, `arrangedBy`, and `downloadCount` to the `Arrangement` model (now fetched from nested sub-collections).
- **Haptic Feedback**: Integrated tactile responses for song clicks, favorite toggles, and authentication errors using `HapticUtils`.
- **Pull-to-Refresh**: Added `SwipeRefreshLayout` to the Home screen for manual data synchronization.
- **Shimmer Loading**: Implemented visual loading placeholders (`shimmer_item_song.xml`) to improve perceived performance.
- **List Animations**: Added "Fall-down" layout animations for a more fluid and professional entry of list items.

### Changed
- **Performance Optimization**: Migrated Firestore data fetching from real-time listeners and nested callbacks to **Parallel Kotlin Coroutines** (`async/await`) in `HomeFragment` and `SongDetailFragment`.
- **UI Enhancements**: Redesigned the Song Detail screen with a new horizontal metadata section for BPM, Key, and Difficulty.
- **App Version**: Incremented `versionCode` to 17 and `versionName` to "1.6.0" in `app/build.gradle.kts`.

## [1.5.8] - 2025-12-11

### Fixed
- **YouTube Error 152-4**: Replaced WebView-based YouTube embedding with `com.pierfrancescosoffritti.androidyoutubeplayer:core`. This uses a dedicated library for YouTube playback, which is more robust and should resolve persistent embedding errors.

### Changed
- **App Version**: Incremented `versionCode` to 15 and `versionName` to "1.5.8" in `app/build.gradle.kts`.

## [1.5.7] - 2025-12-11

### Documentation
- **WebView JavaScript**: Added an explicit comment in `SongDetailFragment.kt` to highlight that `webViewYoutube.settings.javaScriptEnabled` is set to `true`, addressing the user's inquiry about JavaScript enablement.

### Changed
- **App Version**: Incremented `versionCode` to 14 and `versionName` to "1.5.7" in `app/build.gradle.kts`.

## [1.5.6] - 2025-12-11

### Fixed
- **YouTube ID Extraction**: Added explicit safety stripping of query parameters (like `?si=...`) in `extractVideoId`. This ensures that if a user provides an existing embed link with parameters, they are cleanly removed before generating the new standardized `<iframe>` tag.

### Changed
- **App Version**: Incremented `versionCode` to 13 and `versionName` to "1.5.6" in `app/build.gradle.kts`.

## [1.5.5] - 2025-12-11

### Fixed
- **YouTube Embedding Logic**: Updated `SongDetailFragment.kt` to use a comprehensive `<iframe>` tag format with specific attributes (`referrerpolicy`, `allow` features, and `si` parameter) to improve compatibility and fix playback issues. The Video ID is now always extracted to reconstruct this tag dynamically.

### Changed
- **App Version**: Incremented `versionCode` to 12 and `versionName` to "1.5.5" in `app/build.gradle.kts`.

## [1.5.4] - 2025-12-11

### Fixed
- **YouTube Error 152-4**: Reverted the Base URL in `SongDetailFragment.kt` from `http://www.youtube.com` back to `https://www.youtube.com`. The previous change caused a protocol mismatch (HTTPS iframe in HTTP context), leading to Error 152-4 (Referer/Origin blocked).

### Changed
- **App Version**: Incremented `versionCode` to 11 and `versionName` to "1.5.4" in `app/build.gradle.kts`.

## [1.5.3] - 2025-12-11

### Documentation
- **Daily Checklist**: Updated `DAILY_CHECKUP.md` with today's tasks (YouTube fix, Splash Logo, Version bumps).

### Changed
- **App Version**: Incremented `versionCode` to 10 and `versionName` to "1.5.3" in `app/build.gradle.kts`.

## [1.5.2] - 2025-12-11

### Fixed
- **YouTube Player Base URL**: Updated `SongDetailFragment.kt` to use `http://www.youtube.com` (instead of `https`) as the base URL in `loadDataWithBaseURL`. This ensures strictly following the "fake http URL" fix recommendation for the YouTube player.

### Changed
- **App Version**: Incremented `versionCode` to 9 and `versionName` to "1.5.2" in `app/build.gradle.kts`.

## [1.5.1] - 2025-12-11

### Fixed
- **YouTube Player Crash in WebView**: Switched from `loadData()` to `loadDataWithBaseURL("https://www.youtube.com", ...)` in `SongDetailFragment.kt`. This fixes crashes and loading failures by tricking the YouTube player into thinking it's running on a real website (resolving JavaScript/DOM storage restrictions).

### Added
- **Splash Screen Logo**: Updated `activity_splash.xml` to use `ic_launcher` as the logo image within the `logoContainer`. This provides a consistent branding experience on startup.

### Changed
- **App Version**: Incremented `versionCode` to 8 and `versionName` to "1.5.1" in `app/build.gradle.kts`.
