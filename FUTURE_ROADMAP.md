# Future Roadmap: Interactive Learning Mode ("Simply Piano" Style)

This document outlines the technical plan to evolve NC Score from a sheet music viewer into an interactive learning platform.

## Core Requirement: Data Format Change
**Current State:** PDF Files (Static images).
**Required State:** Semantic Music Data (MusicXML, MIDI, or specific proprietary formats).
*   **Why:** To check if the user played the "correct" note, the app must strictly know what note is *supposed* to be played at a specific time. PDFs do not provide this data easily.
*   **Strategy:**
    1.  Start associating `.midi` or `.musicxml` files with every Song in Firestore.
    2.  Render the score dynamically using a library (like `osmd-android` or custom Canvas drawing) instead of just showing a PDF.

---

## Phase 1: MIDI Input Support (High Accuracy)
The most reliable way to detect notes is via a direct digital connection.

### 1. Hardware Connection
*   Support **USB OTG (On-The-Go)**: User plugs keyboard USB into phone.
*   Support **Bluetooth MIDI (BLE)**: Wireless connection for modern keyboards.

### 2. Technical Implementation (Android)
*   Use **Android MIDI API (`android.media.midi`)**.
*   **Logic:**
    *   App listens for `NOTE_ON` and `NOTE_OFF` events.
    *   App compares the incoming MIDI note number (e.g., 60 for Middle C) with the expected note from the song data.
    *   **Visual Feedback:**
        *   Green Highlight: Correct Note.
        *   Red Highlight: Wrong Note.
        *   Pause cursor: Wait for user to hit the right note.

---

## Phase 2: Acoustic Pitch Detection (Microphone)
For users with acoustic pianos or without MIDI cables.

### 1. Audio Engine
*   **Library:** Use **Oboe** (Google's High-Performance Audio) or **TarsosDSP** (Java-based, easier for prototyping).
*   **Algorithm:** Fast Fourier Transform (FFT) or YIN Algorithm for pitch detection.

### 2. Challenges & Solutions
*   **Latency:** Audio processing takes time. We must optimize the buffer size.
*   **Noise:** Background noise triggers false positives. Implementation needs a "Noise Gate" (minimum volume threshold).
*   **Polyphony:** Detecting multiple notes at once (chords) via microphone is extremely difficult mathematically. *Initial version should focus on single-note melody lines.*

---

## Phase 3: The "Gamification" Loop (The Simply Piano Experience)

### 1. Scrolling Score View
*   Instead of page flipping, the music should scroll horizontally or vertically as the song plays.
*   A "Cursor" line shows exactly where to play.

### 2. Feedback System
*   **Real-time:**
    *   Particles/Sparkles on correct hits.
    *   "Wait Mode": The backing track stops if you miss a note (optional setting).
*   **Post-Song:**
    *   Accuracy Score (e.g., "95% Perfect").
    *   Stars (1-3).
    *   Streak tracking (Days played in a row).

### 3. Backing Tracks
*   Play an MP3 accompaniment while the user plays the piano part.
*   Syncing the MP3 with the user's playing speed is complex.
    *   *MVP:* Constant tempo (User must keep up).
    *   *Advanced:* Time-stretching audio (Complex).

---

## Firestore Schema Updates Needed

### `songs` Collection
New fields required:
```json
{
  "midiUrl": "https://.../song.midi",
  "musicXmlUrl": "https://.../song.xml",
  "backingTrackUrl": "https://.../backing.mp3",
  "difficulty": "Easy" // or 1-5
}
```

### `user_progress` Collection (New)
Track user scores:
```json
{
  "userId": "uid123",
  "songId": "songABC",
  "highScore": 9500,
  "stars": 3,
  "lastPlayed": "timestamp"
}
```
