# 🏛️ InkAction Android Architecture

Technical specifications for the **InkAction** native Android application (Kotlin, Jetpack Compose, S-Pen SDK, Room DB, Google Generative AI).

---

## 1. Hardware S-Pen & MotionEvent Processing

```
MotionEvent (Stylus / Finger)
   │
   ├──> Tool Type Detection (`event.getToolType(0)`)
   │      ├── TOOL_TYPE_STYLUS / TOOL_TYPE_ERASER:
   │      │     └── Activate Palm Rejection & Process Pressure
   │      └── TOOL_TYPE_FINGER:
   │            └── If S-Pen Active -> Ignore (Palm Rejection)
   │
   └──> Historical Event Batching (`event.getHistoricalX/Y/Pressure`)
          └──> Quadratic Bézier Path Construction ──> Hardware Canvas
```

### Palm Rejection Heuristics
- When `TOOL_TYPE_STYLUS` is detected, `hasActiveStylus` flag is armed.
- Subsequent `TOOL_TYPE_FINGER` events are intercepted and dropped to prevent accidental wrist/palm marks on Galaxy Tab S9 & S26 Ultra.
- S-Pen hardware barrel button press (`MotionEvent.BUTTON_STYLUS_PRIMARY`) automatically engages eraser mode without lifting the pen.

---

## 2. Inactivity Debounce StateFlow

- Implemented in `InkActionViewModel` using Kotlin Coroutines:
  - On `ACTION_DOWN` (`onStrokeStarted`), any pending countdown job is canceled.
  - On `ACTION_UP` (`onStrokeFinished`), a countdown job is launched with 60 update steps over the debounce duration (default: 6,000 ms).
  - The UI updates an SVG/Canvas circular countdown ring in real time.
  - When the countdown finishes, `createOcrBitmap()` captures high-contrast black strokes on pure white background and streams to Google Gemini.

---

## 3. Storage Architecture & Local Persistence

- **Atomic JSON Storage**:
  - `saved_notes.json`: Transcribed Markdown notes, executive summaries, folders, and tags.
  - `strokes_<noteId>.json`: Raw vector drawing strokes (`InkStroke`, `InkPoint` coordinate arrays) saved per note to preserve full editing fidelity.
  - `saved_todos.json`: Extracted checklist tasks with completion toggles, priority levels (`HIGH`, `MEDIUM`, `LOW`), due dates, and links back to `noteId`.
  - `saved_folders.json`: Custom folder categorization with custom color badges.
  - Thread-safe serialization managed via Kotlin Coroutine `Mutex` with atomic file swapping (`.tmp` write then atomic file rename).
- **`SharedPreferences`**: Stores application preferences (Gemini API keys, selected model, language, theme mode, stroke width, sync passphrase, and cloud sync toggle).

---

## 4. Android Calendar Integration

- When the Calendar Agent identifies an appointment, InkAction uses `CalendarSyncUtil` to trigger `Intent.ACTION_INSERT` with `CalendarContract.Events.CONTENT_URI`.
- Pre-fills event title, start/end timestamps, location, and description directly in Google Calendar or Samsung Calendar without requiring intrusive calendar read/write background permissions.

---

## 5. Zero-Knowledge Cloud Sync & E2EE Cryptography

InkAction features an enterprise-grade Zero-Knowledge End-to-End Encrypted (E2EE) synchronization engine designed for multi-device setups (e.g. Galaxy S26 Ultra phone and Galaxy Tab S9 tablet).

```
Device A (S26 Ultra)                                  Device B (Tab S9)
┌─────────────────────────┐                           ┌─────────────────────────┐
│ Local Notes + Strokes   │                           │ Local Notes + Strokes   │
└────────────┬────────────┘                           └────────────▲────────────┘
             │                                                     │
    1. GZIP Compression                                   5. AES-256-GCM Decrypt
             │                                            6. GZIP Decompression
    2. AES-256-GCM Encrypt                                         │
       (Key = PBKDF2 100k)                                4. Pull Ciphertext
             │                                                     │
    3. Push Ciphertext                                             │
             └──────────────────►  Firebase  ◄─────────────────────┘
                                  Firestore
                            (users/{uid}/notes)
                         * Stores ONLY Ciphertext *
```

### Cryptographic Security Model
- **Key Derivation (PBKDF2)**: 256-bit AES keys derived via `PBKDF2WithHmacSHA256` using 100,000 iterations and a fixed application domain salt (`InkAction_E2EE_Vault_Salt_v1`).
- **Cipher**: `AES/GCM/NoPadding` (256-bit key, 12-byte random IV per note, 128-bit authentication tag).
- **Zero-Knowledge Guarantee**: The encryption key is derived exclusively from the user's secret **Sync Passphrase**, which is stored strictly in device local storage and never transmitted over the network. The Firebase backend stores only random-looking Base64 ciphertext and IVs. Neither Firebase, Google, nor database administrators can access note contents.
- **API Key Isolation**: Google Gemini API keys are hardware Keystore-bound and never synchronized to the cloud.
- **Strict Offline Privacy Mode**: A single master toggle disables all cloud calls and network handlers, ensuring 100% local operation when desired.

### GZIP Stream Compression & Firestore 1 MB Protection
- **The Problem**: Drawing extensive notes with Samsung S-Pen creates tens of thousands of coordinate points (`x`, `y`, `pressure`, `timestamp`). Serialized raw JSON can exceed Firestore's hard 1 MiB (`1,048,576` bytes) document limit.
- **GZIP Compression**: Before AES-256-GCM encryption, the raw stroke JSON is passed through a `GZIPOutputStream`. Because coordinate JSON contains repetitive structural keys, GZIP achieves a **90–95% compression ratio** (shrinking ~1.5 MB of strokes to ~80 KB).
- **Auto-Chunking Safeguard**: If an exceptionally large canvas exceeds 600 KB even after GZIP compression, `CloudSyncManager` automatically splits the ciphertext into 500 KB slices stored in a `stroke_chunks` Firestore subcollection (`notes/{noteId}/stroke_chunks/{index}`). On pull, chunks are reassembled seamlessly before decryption.

---

## 6. Responsive Tablet & Stylus UX

- **Adaptive Navigation Rail**: On wide screens (>600 dp) such as Galaxy Tab S9 and foldables, navigation shifts to a compact 76 dp vertical rail, leaving >90% of screen width dedicated to drawing.
- **Dual-Pane Reference Panel**: Optional landscape mode featuring a 70% canvas / 30% reference panel for cross-referencing past notes or checking task lists while sketching.
- **Interactive Pen Thickness UX**: In-toolbar color bullets cycle stroke thickness (`2.5f`, `4.0f`, `6.5f`, `10.0f`) on repeated tap with animated spring scaling and haptic feedback.
