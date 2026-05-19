# Daily Checkup

This file is used to track daily progress and contributions.

## Date: 2026-05-20

### To-Do
- [x] Synchronize Song/Arrangement models with Firestore JSON mapping
- [x] Implement parallel data fetching with Kotlin Coroutines
- [x] Add Haptic Feedback across the app (Clicks, Errors, Favorites)
- [x] Implement Pull-to-Refresh on Home Screen
- [x] Add List Animations (Fall-down) and Shimmer placeholders
- [x] Redesign Song Detail UI with metadata (BPM, Key, Difficulty)
- [x] Fix Build/ Handshake issues in Gradle properties

### Notes
- Migrated `HomeFragment` and `SongDetailFragment` to Coroutines (`lifecycleScope` + `async/await`), significantly reducing screen load times.
- Resolved TLS handshake issues by forcing TLSv1.2 in `gradle.properties`.
- Synchronized models to match `firestore-clean.json`: difficulty is now handled at the arrangement sub-collection level.
- Added a custom `HapticUtils` to provide tactile feedback for a more "premium" feel.
- Cleaned up build configuration: synced root `build.gradle.kts` with `libs.versions.toml` versions.

## Date: 2025-12-11

### To-Do
- [x] Fix YouTube player crash in WebView
- [x] Update Splash Screen Logo
- [x] Bump App Version & Update Changelog
- [x] Fix YouTube Error 152-4
- [x] Fix YouTube URL regex invisible character bug
- [x] Improve YouTube WebView error handling

### Notes
- Replaced `loadData` mechanism with `loadDataWithBaseURL` using `http://www.youtube.com` as the base to resolve WebView crash/loading issues.
- Updated `activity_splash.xml` to display `ic_launcher` as the logo.
- Updated app version to 1.5.2 and documented changes in `CHANGELOG_FOR_DEV.md`.
- **Correction**: Reverted YouTube Base URL to `https` to fix Error 152-4.
- **Enhancement**: Implemented robust YouTube `<iframe>` construction with `si` parameter and strict `referrerpolicy` to ensure playback reliability.
- **Refinement**: Added safety check to `extractVideoId` to handle and strip existing query parameters from input URLs.
- **Clarification**: Added explicit comment in `SongDetailFragment.kt` confirming WebView JavaScript is enabled.
- **Major Fix**: Replaced WebView YouTube embedding with `androidyoutubeplayer` library to resolve persistent errors like 152-4.

---

## Date: 2025-12-08

### To-Do
- [x] Perform code cleanup
- [x] Check for unused imports
- [x] Verify project structure

### Notes
- Performed a general code cleanup pass.
- Verified that no unused imports are cluttering the main source files.
- Project structure remains consistent with Android best practices.

---

## Date: 2025-12-05

### To-Do
- [x] Review pending PRs
- [x] Check `FUTURE_ROADMAP.md` for next tasks
- [x] Verify unit tests status

### Notes
- Reviewed `FUTURE_ROADMAP.md`: Next major phase is "Data Format Change" (Transition from PDF to MusicXML/MIDI).
- Investigated `songs` collection schema requirements: Need to add `midiUrl` and `musicXmlUrl` fields.
- Unit testing environment is currently restricted (no local Android SDK), so manual verification is prioritized.
- Codebase health is stable.

---

## Date: 2024-05-22

### To-Do
- [x] Review code
- [x] Update documentation
- [ ] Check for new issues

### Notes
- Verified that all source files in `app/src/main/java/com/noobcompany/nc_scorebeta` have comprehensive KDoc documentation.
- Codebase appears healthy and well-documented.
