# Changelog for Developers

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
