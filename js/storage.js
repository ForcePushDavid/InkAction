/**
 * InkAction Storage Service
 * Manages local persistence for API Keys, user preferences,
 * auto-debounce timings, and historical handwritten note sessions.
 */
const STORAGE_KEYS = {
  API_KEY: 'inkaction_gemini_api_key',
  MODEL: 'inkaction_gemini_model',
  DEBOUNCE_MS: 'inkaction_debounce_ms',
  THEME: 'inkaction_theme',
  PEN_COLOR: 'inkaction_pen_color',
  PEN_SIZE: 'inkaction_pen_size',
  SESSIONS: 'inkaction_sessions_history'
};

export class StorageService {
  static getApiKey() {
    return localStorage.getItem(STORAGE_KEYS.API_KEY) || '';
  }

  static setApiKey(key) {
    localStorage.setItem(STORAGE_KEYS.API_KEY, key.trim());
  }

  static getModel() {
    return localStorage.getItem(STORAGE_KEYS.MODEL) || 'gemini-1.5-flash';
  }

  static setModel(model) {
    localStorage.setItem(STORAGE_KEYS.MODEL, model);
  }

  static getDebounceMs() {
    const val = localStorage.getItem(STORAGE_KEYS.DEBOUNCE_MS);
    return val ? parseInt(val, 10) : 6000;
  }

  static setDebounceMs(ms) {
    localStorage.setItem(STORAGE_KEYS.DEBOUNCE_MS, ms.toString());
  }

  static getTheme() {
    return localStorage.getItem(STORAGE_KEYS.THEME) || 'dark';
  }

  static setTheme(theme) {
    localStorage.setItem(STORAGE_KEYS.THEME, theme);
  }

  static getPenColor() {
    return localStorage.getItem(STORAGE_KEYS.PEN_COLOR) || '#f0f6fc';
  }

  static setPenColor(color) {
    localStorage.setItem(STORAGE_KEYS.PEN_COLOR, color);
  }

  static getPenSize() {
    const val = localStorage.getItem(STORAGE_KEYS.PEN_SIZE);
    return val ? parseInt(val, 10) : 3;
  }

  static setPenSize(size) {
    localStorage.setItem(STORAGE_KEYS.PEN_SIZE, size.toString());
  }

  static getSessions() {
    try {
      const data = localStorage.getItem(STORAGE_KEYS.SESSIONS);
      return data ? JSON.parse(data) : [];
    } catch {
      return [];
    }
  }

  static saveSession(session) {
    try {
      const sessions = this.getSessions();
      sessions.unshift(session);
      // Keep last 30 sessions in storage
      const trimmed = sessions.slice(0, 30);
      localStorage.setItem(STORAGE_KEYS.SESSIONS, JSON.stringify(trimmed));
    } catch (e) {
      console.warn('Failed to save session to localStorage:', e);
    }
  }

  static clearAll() {
    localStorage.clear();
  }
}
