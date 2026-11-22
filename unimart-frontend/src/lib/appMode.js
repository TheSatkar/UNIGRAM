/**
 * Application Demonstration Mode Management
 *
 * - INTEGRATED: Main college presentation mode (React + Java + MySQL).
 *   Shares the unified backend database. Strictly prohibits silent browser fallback.
 *   Throws clear understandable connection errors if the Java server is unreachable.
 *
 * - OFFLINE: Local browser test mode only when explicitly selected.
 *   Uses mock storage with a visible UI banner warning.
 */

const STORAGE_KEY = 'unimart_app_mode';

export const APP_MODES = {
  INTEGRATED: 'integrated',
  OFFLINE: 'offline',
};

export function getAppMode() {
  try {
    const saved = localStorage.getItem(STORAGE_KEY);
    if (saved === APP_MODES.OFFLINE) {
      return APP_MODES.OFFLINE;
    }
  } catch {
    // ignore
  }
  return APP_MODES.INTEGRATED; // Default for college presentations
}

export function setAppMode(mode) {
  try {
    if (mode === APP_MODES.OFFLINE) {
      localStorage.setItem(STORAGE_KEY, APP_MODES.OFFLINE);
    } else {
      localStorage.setItem(STORAGE_KEY, APP_MODES.INTEGRATED);
    }
    window.dispatchEvent(new CustomEvent('unimart:mode-change', { detail: { mode } }));
  } catch {
    // ignore
  }
}

export function isIntegratedMode() {
  return getAppMode() === APP_MODES.INTEGRATED;
}

export function isOfflineMode() {
  return getAppMode() === APP_MODES.OFFLINE;
}
