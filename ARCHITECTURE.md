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

## 3. Room Database & Local Persistence

- **`notes` Table**: Stores transcribed markdown, executive summaries, and topic tags.
- **`todos` Table**: Stores extracted checklist tasks with completion toggles, priority levels (`HIGH`, `MEDIUM`, `LOW`), and foreign key linking back to `notes.id`.
- **`SharedPreferences`**: Stores API keys, chosen Gemini models, and debounce timing.

---

## 4. Android Calendar Integration

- When the Calendar Agent identifies an appointment, InkAction uses `CalendarSyncUtil` to trigger `Intent.ACTION_INSERT` with `CalendarContract.Events.CONTENT_URI`.
- Pre-fills event title, start/end timestamps, location, and description directly in Google Calendar or Samsung Calendar.
