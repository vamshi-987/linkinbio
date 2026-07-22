/**
 * The four palettes a creator can pick for their public page. Shared by the settings preview and
 * the page itself so what the owner picks is exactly what a visitor sees — the ids match the
 * `default|dark|pastel|neon` values the API accepts.
 */
export const themes = {
  default: {
    label: 'Default',
    bg: '#22262b',
    text: '#ffffff',
    sub: '#aeb4bd',
    btn: '#33405a',
    btnText: '#eef0f3',
    border: 'rgba(255, 255, 255, 0.10)',
    accent: '#5b8def',
    accentText: '#ffffff',
  },
  dark: {
    label: 'Dark',
    bg: '#0f1114',
    text: '#ffffff',
    sub: '#9aa0ab',
    btn: '#242830',
    btnText: '#eef0f3',
    border: 'rgba(255, 255, 255, 0.08)',
    accent: '#e8eaee',
    accentText: '#0f1114',
  },
  pastel: {
    label: 'Pastel',
    bg: '#fdf2f6',
    text: '#2d2a32',
    sub: '#6f6a76',
    btn: '#f4c9db',
    btnText: '#3a2b33',
    border: 'rgba(45, 42, 50, 0.10)',
    accent: '#c2557f',
    accentText: '#ffffff',
  },
  neon: {
    label: 'Neon',
    bg: '#05060a',
    text: '#39ff14',
    sub: '#7bffb0',
    btn: '#0d1f16',
    btnText: '#39ff14',
    border: 'rgba(57, 255, 20, 0.30)',
    accent: '#39ff14',
    accentText: '#05060a',
  },
};

export const DEFAULT_THEME = 'default';

/** Unknown or missing ids fall back rather than rendering an unstyled page. */
export const themeFor = (id) => themes[id] || themes[DEFAULT_THEME];

export const themeList = Object.entries(themes).map(([id, palette]) => ({ id, ...palette }));

/** Palette as CSS custom properties, applied inline to whichever element scopes the theme. */
export const themeVars = (t) => ({
  '--pp-bg': t.bg,
  '--pp-text': t.text,
  '--pp-sub': t.sub,
  '--pp-btn': t.btn,
  '--pp-btn-text': t.btnText,
  '--pp-border': t.border,
  '--pp-accent': t.accent,
  '--pp-accent-text': t.accentText,
});
