@echo off
setlocal EnableExtensions enabledelayedexpansion
cd /d "%~dp0"
title UNIMART + UNI-GRAM Presentation Launcher

echo ======================================================================
echo    UNIMART + UNI-GRAM COLLEGE DEMO LAUNCHER
echo    Chitkara University Student Marketplace & Campus Messenger
echo ======================================================================
echo.

:: 1. Verify JDK, Node.js and npm
echo [1/5] Checking Java, Node.js and npm...
where javac >nul 2>&1
if errorlevel 1 (
  echo [ERROR] javac was not found. Please install JDK 21+ and add javac to PATH.
  pause
  exit /b 1
)
where java >nul 2>&1
if errorlevel 1 (
  echo [ERROR] java was not found in PATH.
  pause
  exit /b 1
)
where node >nul 2>&1
if errorlevel 1 (
  echo [ERROR] Node.js is not found. Please install Node.js 18+ from https://nodejs.org/
  pause
  exit /b 1
)
where npm >nul 2>&1
if errorlevel 1 (
  echo [ERROR] npm was not found in PATH.
  pause
  exit /b 1
)
echo [PASS] JDK, Node.js and npm are available.

:: 2. Compile Java Backend
echo.
echo [2/5] Compiling Java backend classes...
if not exist "lib\gson-2.11.0.jar" (
  echo [ERROR] Missing Gson JAR in lib folder.
  pause
  exit /b 1
)
if not exist "lib\mysql-connector-j-8.4.0.jar" (
  echo [ERROR] Missing MySQL Connector JAR in lib folder.
  pause
  exit /b 1
)
if not exist "bin" mkdir "bin"

powershell -NoProfile -Command "$q=[char]34; $files=Get-ChildItem 'src/main/java' -Recurse -File -Filter '*.java' | ForEach-Object { [string]$q + ($_.FullName.Replace([char]92,[char]47)) + [string]$q }; [IO.File]::WriteAllLines('sources.txt',[string[]]@($files),[Text.Encoding]::ASCII)"
if errorlevel 1 (
  echo [ERROR] Could not generate sources.txt.
  pause
  exit /b 1
)

javac -encoding UTF-8 -cp "lib/*" -d "bin" "@sources.txt"
if errorlevel 1 (
  echo [ERROR] Java compilation failed.
  del "sources.txt" >nul 2>&1
  pause
  exit /b 1
)
del "sources.txt" >nul 2>&1
echo [PASS] Java backend compiled successfully.

:: 3. Free port 8080 if an old instance is running, then start server
echo.
echo [3/5] Starting Uni-Gram Java server on port 8080...
powershell -NoProfile -Command "try { $p = (Get-NetTCPConnection -LocalPort 8080 -ErrorAction SilentlyContinue).OwningProcess; if ($p) { Stop-Process -Id $p -Force -ErrorAction SilentlyContinue; Start-Sleep -Milliseconds 500 } } catch {}" >nul 2>&1

start "Uni-Gram Java Backend" cmd /k "java -cp ""bin;lib/*"" com.unigram.Main --server"

:: Health check poll
echo Verifying backend health at http://127.0.0.1:8080/api/health...
set BACKEND_UP=0
set MYSQL_CONNECTED=0
for /l %%i in (1, 1, 15) do (
  if !BACKEND_UP! equ 0 (
    for /f "usebackq delims=" %%s in (`powershell -NoProfile -Command "try { $r = Invoke-RestMethod -Uri 'http://127.0.0.1:8080/api/health' -TimeoutSec 1; if ($r.status -eq 'success') { if ($r.data.database -eq 'CONNECTED') { 'UP_CONNECTED' } else { 'UP_INMEMORY' } } else { 'DOWN' } } catch { 'DOWN' }"`) do (
      if "%%s"=="UP_CONNECTED" (
        set BACKEND_UP=1
        set MYSQL_CONNECTED=1
      ) else if "%%s"=="UP_INMEMORY" (
        set BACKEND_UP=1
        set MYSQL_CONNECTED=0
      ) else (
        timeout /t 1 /nobreak >nul
      )
    )
  )
)

if !BACKEND_UP! equ 0 (
  echo [ERROR] Java API did not respond on port 8080.
  echo Check the separate "Uni-Gram Java Backend" console window for details.
  pause
  exit /b 1
)

echo [PASS] Java backend is UP and responsive on port 8080.
if !MYSQL_CONNECTED! equ 1 (
  echo [PASS] MySQL Persistence: CONNECTED (Integrated Mode active).
) else (
  echo [WARNING] MySQL is NOT connected. Running in IN_MEMORY_PERSISTENCE mode (local demo fallback).
)

:: 4. Start Vite Frontend
echo.
echo [4/5] Starting UniMart React Frontend (Vite)...
cd /d "%~dp0unimart-frontend"
if not exist "node_modules" (
  echo Installing frontend dependencies (npm install)...
  call npm install
)

start "UniMart Frontend (Vite)" cmd /k "npm run dev"
cd /d "%~dp0"

:: 5. Wait for Frontend and Launch Browser
echo.
echo [5/5] Waiting for frontend at http://localhost:5173...
set FRONTEND_UP=0
for /l %%i in (1, 1, 15) do (
  if !FRONTEND_UP! equ 0 (
    powershell -NoProfile -Command "try { $r = Invoke-WebRequest -Uri 'http://localhost:5173' -UseBasicParsing -TimeoutSec 1; if ($r.StatusCode -eq 200) { exit 0 } else { exit 1 } } catch { exit 1 }"
    if not errorlevel 1 (
      set FRONTEND_UP=1
    ) else (
      timeout /t 1 /nobreak >nul
    )
  )
)

if !FRONTEND_UP! equ 0 (
  echo [ERROR] Frontend failed to respond on http://localhost:5173 within 15 seconds.
  echo Please check the "UniMart Frontend (Vite)" console window for details.
  pause
  exit /b 1
)
echo [PASS] Frontend is UP and accessible at http://localhost:5173.

:: Launch browser
start http://localhost:5173

echo.
echo ======================================================================
echo    UNIMART + UNI-GRAM READY FOR PROFESSOR PRESENTATION!
echo ======================================================================
echo.
echo  Frontend URL:     http://localhost:5173
echo  Backend Health:   http://127.0.0.1:8080/api/health
if !MYSQL_CONNECTED! equ 1 (
  echo  Persistence:      INTEGRATED DEMO (MySQL Connected)
) else (
  echo  Persistence:      IN-MEMORY DEMO (MySQL Disconnected)
)
echo.
echo  Demo Accounts for Presentation:
echo  ------------------------------------------------------------------
echo  [1] Student A (Seller): satkar0696.becse25@chitkara.edu.in / student123
echo  [2] Student B (Buyer):  priya.k@chitkara.edu.in           / student123
echo  [3] Campus Admin:       admin@chitkara.edu.in             / Unimart@123
echo  ------------------------------------------------------------------
echo.
echo  Press any key to close this launcher (services will keep running)...
pause >nul
