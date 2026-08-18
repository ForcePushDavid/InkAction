# 🤖 InkAction AI Agent Architecture

InkAction processes raw handwritten stylus input through a coordinated multimodal agent pipeline powered by **Google Gemini (1.5 Flash / 2.0)**.

Instead of generic OCR that simply prints raw text, InkAction executes **4 specialized sub-agents** in a unified inference pass to turn visual scribbles into structured actions.

---

## 1. 📝 The Synthesizer Agent

### Purpose
Transforms messy handwritten ink, shorthand notes, and diagrams into clean, beautifully formatted Markdown with executive summaries, headers, and bullet points.

### Responsibilities
- Decipher cursive handwriting, abbreviations, and informal shorthand.
- Group fragmented thoughts into thematic sections.
- Create a concise 1-2 sentence executive summary for quick scanning.
- Highlight key terms in **bold** and code blocks where applicable.

### Prompt Directive
```markdown
Synthesizer Agent: Transcribe handwriting accurately, organize into a polished, 
structured markdown document with titles, key takeaways, and clean formatting.
```

---

## 2. ✅ The Action / Todo Agent

### Purpose
Detects and extracts actionable tasks, checklist items, commitments, and delegated action items buried within notes.

### Responsibilities
- Identify task indicators (e.g. `[ ]`, `-`, arrows `->`, exclamation marks `!`, or action verbs like "Call", "Email", "Review", "Buy", "Implement").
- Assign a priority rating:
  - `high`: Urgently marked items, exclamation points, same-day deadlines.
  - `medium`: Standard deliverables and follow-ups.
  - `low`: "Nice to have" or long-term ideas.
- Extract relative or absolute due dates (e.g., "Tomorrow", "By Friday", "2026-08-20").

### Output Schema
```json
{
  "id": "todo-1",
  "text": "Deploy InkAction PWA build to Galaxy Tab S9",
  "priority": "high",
  "dueDate": "2026-08-19",
  "completed": false
}
```

---

## 3. 📅 The Calendar / Schedule Agent

### Purpose
Detects meetings, appointments, calls, time-blocked work sessions, and scheduled deadlines.

### Responsibilities
- Parse temporal references (e.g., "Meeting at 2pm", "Sprint planning on Thursday 10:00", "Sync with team tomorrow").
- Format standard start/end timestamps.
- Generate direct **Google Calendar one-click templates** and standard **iCalendar (`.ics`)** downloads.
- Identify meeting platforms (e.g. Google Meet, Zoom, Physical room).

### Output Schema
```json
{
  "id": "event-1",
  "title": "InkAction Architecture Review",
  "date": "2026-08-19",
  "time": "14:00",
  "duration": "45m",
  "description": "Review multi-agent multimodal pipeline and Samsung S-Pen latency metrics.",
  "location": "Google Meet"
}
```

---

## 4. 🕸️ The Graph & Linking Agent

### Purpose
Extracts core entities, hashtag categories, and topic relationships to connect the note with the user's broader knowledge base.

### Responsibilities
- Identify primary concepts (e.g., `#SPen`, `#Architecture`, `#Sprint14`).
- Generate thematic summaries showing how this note fits into overarching projects.
- Facilitate cross-referencing across past handwritten sessions.

---

## 📦 Complete Multi-Agent Response Schema

```json
{
  "note": {
    "title": "Sprint Planning & S-Pen Architecture",
    "summary": "Synthesized handwritten diagram into specifications and sprint schedule.",
    "markdown": "### Discussion\n* Point 1\n* Point 2",
    "tags": ["Architecture", "S-Pen", "Gemini"]
  },
  "todos": [
    {
      "id": "todo-1",
      "text": "Review palm rejection benchmarks",
      "priority": "high",
      "dueDate": "Tomorrow",
      "completed": false
    }
  ],
  "events": [
    {
      "id": "event-1",
      "title": "Architecture Review",
      "date": "2026-08-19",
      "time": "14:00",
      "duration": "45m",
      "description": "Live demo of handwritten ink to calendar automation.",
      "location": "Workspace Hub"
    }
  ],
  "topics": [
    {
      "name": "S-Pen Ink Engine",
      "summary": "Catmull-Rom spline stroke smoothing and pressure-sensitive input."
    }
  ]
}
```
