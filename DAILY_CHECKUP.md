# Daily Checkup

This file is used to track daily progress and contributions.

## Date: 2025-12-12

### To-Do
- [x] Fix SSRF Vulnerability in `SecurityUtils`
- [x] Create test case for private IP blocking

### Notes
- Identified that `SecurityUtils.isSecureUrl` allowed connections to private IP addresses (e.g., `192.168.x.x`) and localhost.
- Enhanced `SecurityUtils.kt` to block private IP ranges (10.0.0.0/8, 172.16.0.0/12, 192.168.0.0/16, 127.0.0.0/8, 169.254.0.0/16).
- Verified the fix with a standalone Java reproduction script since Gradle tests are unavailable.
- Updated `UrlSecurityTest.kt` to include test cases for private IPs for future verification.

---

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
