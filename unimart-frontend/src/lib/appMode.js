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
  return APP_MODES.INTEGRATED; // Integrated mode is permanent
}

export function setAppMode(mode) {
  // Mode toggle is disabled - always stays INTEGRATED
}

export function isIntegratedMode() {
  return true;
}

export function isOfflineMode() {
  return false;
}
