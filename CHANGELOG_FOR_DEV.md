# Changelog for Developers

## [1.5.1] - 2025-12-11

### Fixed
- **YouTube Player Crash in WebView**: Switched from `loadData()` to `loadDataWithBaseURL("https://www.youtube.com", ...)` in `SongDetailFragment.kt`. This fixes crashes and loading failures by tricking the YouTube player into thinking it's running on a real website (resolving JavaScript/DOM storage restrictions).

### Added
- **Splash Screen Logo**: Updated `activity_splash.xml` to use `ic_launcher` as the logo image within the `logoContainer`. This provides a consistent branding experience on startup.

### Changed
- **App Version**: Incremented `versionCode` to 8 and `versionName` to "1.5.1" in `app/build.gradle.kts`.
