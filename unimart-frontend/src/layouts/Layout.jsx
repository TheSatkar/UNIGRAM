import React, { useState, useEffect } from "react";
import { Outlet } from "react-router-dom";
import { getAppMode, setAppMode, APP_MODES } from "../lib/appMode.js";

function Layout() {
  const [mode, setMode] = useState(getAppMode());

  useEffect(() => {
    const onModeChange = (e) => setMode(e.detail?.mode || getAppMode());
    window.addEventListener("unimart:mode-change", onModeChange);
    return () => window.removeEventListener("unimart:mode-change", onModeChange);
  }, []);

  const switchToIntegrated = () => {
    setAppMode(APP_MODES.INTEGRATED);
    window.location.reload();
  };

  return (
    <>
      {mode === APP_MODES.OFFLINE && (
        <div
          className="bg-warning text-dark px-3 py-2 text-center small fw-semibold d-flex align-items-center justify-content-center gap-2 border-bottom shadow-sm"
          style={{ zIndex: 1050, position: "relative" }}
        >
          <span className="material-symbols-outlined" style={{ fontSize: "18px" }}>
            warning
          </span>
          <span>
            <strong>OFFLINE DEMO MODE:</strong> Using temporary local browser storage. Changes will NOT sync to the shared Java + MySQL database.
          </span>
          <button
            type="button"
            className="btn btn-sm btn-dark py-0 px-2 ms-2 rounded-pill fw-bold"
            style={{ fontSize: "0.72rem" }}
            onClick={switchToIntegrated}
          >
            Switch to Integrated Demo
          </button>
        </div>
      )}
      <Outlet />
    </>
  );
}

export default Layout;
