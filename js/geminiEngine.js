/**
 * InkAction Gemini Multimodal AI Engine
 * Communicates with Google Gemini API with multi-agent prompts for
 * handwriting transcription, note synthesis, actionable tasks, and calendar events.
 */
export class GeminiEngine {
  constructor(apiKey = '', model = 'gemini-1.5-flash') {
    this.apiKey = apiKey;
    this.model = model;
    this.onStatusUpdate = () => {};
  }

  setApiKey(key) {
    this.apiKey = key;
  }

  setModel(model) {
    this.model = model;
  }

  setStatusCallback(callback) {
    this.onStatusUpdate = callback;
  }

  /**
   * Process canvas image through the AI Agent pipeline
   * @param {string} base64Image - Base64 encoded PNG of the handwritten canvas
   * @returns {Promise<Object>} Structured action hub data
   */
  async processHandwrittenNotes(base64Image) {
    if (!this.apiKey || this.apiKey.trim() === '') {
      this.onStatusUpdate({ step: 'demo', message: 'No Gemini API key set - running in Instant Smart Demo Mode' });
      return this.runMockPipeline();
    }

    this.onStatusUpdate({ step: 'uploading', message: 'Pushing ink snapshot to Google Gemini...' });

    const systemPrompt = `
You are InkAction, an elite multimodal AI assistant for digital stylus notes (Galaxy Tab S9, S26 Ultra S-Pen).
Analyze the provided handwritten notes, drawings, diagrams, checklists, or scribbles.

You must act as 4 specialized sub-agents in a single pipeline:
1. Synthesizer Agent: Transcribe handwriting accurately, organize into a polished, structured markdown document with titles, key takeaways, and clean formatting.
2. Todo Agent: Extract all actionable tasks, checklist items, commitments, or follow-ups. Assign priority (high/medium/low) and infer deadlines or due dates if mentioned.
3. Calendar/Schedule Agent: Extract any meetings, calls, appointments, time-blocked events, dates, or time references. Provide start/end dates/times in YYYY-MM-DD HH:MM format if mentioned.
4. Graph Linker Agent: Identify 2-5 core concepts, topics, and hashtag categories to interlink this note with other workspace thoughts.

You MUST respond ONLY with a valid JSON object strictly matching this schema:
{
  "note": {
    "title": "Clear concise title of the note",
    "summary": "1-2 sentence executive summary of the note contents",
    "markdown": "Clean, beautifully formatted markdown with headings, bullets, and bold terms",
    "tags": ["tag1", "tag2", "tag3"]
  },
  "todos": [
    {
      "id": "todo-1",
      "text": "Specific actionable task",
      "priority": "high" | "medium" | "low",
      "dueDate": "YYYY-MM-DD or timeframe like 'Tomorrow', 'This Friday', 'None'",
      "completed": false
    }
  ],
  "events": [
    {
      "id": "event-1",
      "title": "Meeting or Event Title",
      "date": "YYYY-MM-DD",
      "time": "HH:MM",
      "duration": "1h",
      "description": "Brief context or agenda",
      "location": "Online / Room / etc"
    }
  ],
  "topics": [
    {
      "name": "Topic or Project Name",
      "summary": "How this relates to broader projects or concepts"
    }
  ]
}
Do not wrap in backticks or markdown code blocks, just pure JSON.
`;

    try {
      this.onStatusUpdate({ step: 'synthesizing', message: 'Synthesizer Agent reading handwriting...' });

      const endpoint = `https://generativelanguage.googleapis.com/v1beta/models/${this.model}:generateContent?key=${this.apiKey.trim()}`;
      
      const payload = {
        contents: [
          {
            parts: [
              { text: systemPrompt },
              {
                inline_data: {
                  mime_type: "image/png",
                  data: base64Image
                }
              }
            ]
          }
        ],
        generationConfig: {
          temperature: 0.2,
          responseMimeType: "application/json"
        }
      };

      const response = await fetch(endpoint, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
      });

      if (!response.ok) {
        const errorData = await response.json().catch(() => ({}));
        throw new Error(errorData.error?.message || `Gemini API returned status ${response.status}`);
      }

      this.onStatusUpdate({ step: 'extracting', message: 'Extracting Actions, Tasks & Calendar Events...' });

      const data = await response.json();
      const rawText = data.candidates?.[0]?.content?.parts?.[0]?.text;

      if (!rawText) {
        throw new Error('Gemini returned an empty response. Please try again.');
      }

      // Parse JSON safely
      let cleanedJson = rawText.trim();
      if (cleanedJson.startsWith('```json')) {
        cleanedJson = cleanedJson.replace(/^```json/, '').replace(/```$/, '').trim();
      } else if (cleanedJson.startsWith('```')) {
        cleanedJson = cleanedJson.replace(/^```/, '').replace(/```$/, '').trim();
      }

      const parsed = JSON.parse(cleanedJson);
      this.onStatusUpdate({ step: 'done', message: 'All actions synthesized & linked!' });
      return this.sanitizeOutput(parsed);

    } catch (err) {
      console.error('Gemini Engine Error:', err);
      this.onStatusUpdate({ step: 'error', message: `API Error: ${err.message}. Falling back to Smart Demo.` });
      // Return enhanced smart fallback
      return this.runMockPipeline();
    }
  }

  sanitizeOutput(parsed) {
    return {
      timestamp: new Date().toISOString(),
      note: {
        title: parsed.note?.title || 'Handwritten Notes Synthesis',
        summary: parsed.note?.summary || 'Handwritten notes processed by InkAction.',
        markdown: parsed.note?.markdown || 'No transcription content parsed.',
        tags: Array.isArray(parsed.note?.tags) ? parsed.note.tags : ['Note', 'S-Pen']
      },
      todos: Array.isArray(parsed.todos) ? parsed.todos.map((t, idx) => ({
        id: t.id || `todo-${Date.now()}-${idx}`,
        text: t.text || 'Action item',
        priority: ['high', 'medium', 'low'].includes(t.priority?.toLowerCase()) ? t.priority.toLowerCase() : 'medium',
        dueDate: t.dueDate || 'Soon',
        completed: false
      })) : [],
      events: Array.isArray(parsed.events) ? parsed.events.map((e, idx) => ({
        id: e.id || `event-${Date.now()}-${idx}`,
        title: e.title || 'Scheduled Item',
        date: e.date || new Date().toISOString().split('T')[0],
        time: e.time || '10:00 AM',
        duration: e.duration || '45m',
        description: e.description || '',
        location: e.location || ''
      })) : [],
      topics: Array.isArray(parsed.topics) ? parsed.topics : []
    };
  }

  /**
   * Generates a realistic multimodal sample response when no API key is supplied
   */
  async runMockPipeline() {
    await new Promise(r => setTimeout(r, 600));
    this.onStatusUpdate({ step: 'synthesizing', message: 'Synthesizer Agent organizing handwriting...' });
    await new Promise(r => setTimeout(r, 500));
    this.onStatusUpdate({ step: 'extracting', message: 'Todo & Calendar Agents parsing action items...' });
    await new Promise(r => setTimeout(r, 400));
    this.onStatusUpdate({ step: 'done', message: 'InkAction Pipeline Complete!' });

    const today = new Date();
    const tomorrow = new Date(today);
    tomorrow.setDate(today.getDate() + 1);
    const nextWeek = new Date(today);
    nextWeek.setDate(today.getDate() + 7);

    const formatDate = (d) => d.toISOString().split('T')[0];

    return {
      timestamp: new Date().toISOString(),
      note: {
        title: "Product Architecture & Sprint Planning Notes",
        summary: "Synthesized handwritten diagram and meeting points into structured specs, deliverables, and upcoming sprint schedule.",
        markdown: `### Key Discussion Points\n* **InkAction Engine**: Finalized auto-debounce inactivity threshold (5-8s) for S-Pen handwriting capture.\n* **Multimodal Agents**: Deployed Synthesizer, Task Extractor, and Calendar Scheduler.\n* **Galaxy S26 & Tab S9 Support**: High-DPI canvas rendering with palm rejection enabled.\n\n### Architectural Decisions\n1. Zero-dependency ES Modules for instant client performance.\n2. LocalStorage persistence for complete offline privacy.\n3. Direct Google Gemini GenAI integration with structured JSON schemas.`,
        tags: ["Architecture", "S-Pen", "Gemini", "Productivity", "Sprint-14"]
      },
      todos: [
        {
          id: `todo-${Date.now()}-1`,
          text: "Deploy InkAction PWA build to Galaxy Tab S9 & test palm rejection",
          priority: "high",
          dueDate: formatDate(tomorrow),
          completed: false
        },
        {
          id: `todo-${Date.now()}-2`,
          text: "Connect Google Gemini API Key in Settings modal",
          priority: "high",
          dueDate: "Today",
          completed: false
        },
        {
          id: `todo-${Date.now()}-3`,
          text: "Review bi-directional linking between canvas strokes and todo items",
          priority: "medium",
          dueDate: formatDate(nextWeek),
          completed: false
        }
      ],
      events: [
        {
          id: `event-${Date.now()}-1`,
          title: "InkAction Architecture Review",
          date: formatDate(tomorrow),
          time: "14:00",
          duration: "45m",
          description: "Review multi-agent multimodal pipeline and Samsung S-Pen latency metrics.",
          location: "Google Meet"
        },
        {
          id: `event-${Date.now()}-2`,
          title: "Sprint Retrospective & Demo",
          date: formatDate(nextWeek),
          time: "16:30",
          duration: "1h",
          description: "Live demo of handwritten ink to calendar/todo automation.",
          location: "Workspace Hub"
        }
      ],
      topics: [
        {
          name: "S-Pen Ink Engine",
          summary: "Catmull-Rom spline stroke smoothing and pressure-sensitive input."
        },
        {
          name: "Gemini Agent Pipeline",
          summary: "Automated handwriting transcription, task extraction, and calendar scheduling."
        }
      ]
    };
  }
}
