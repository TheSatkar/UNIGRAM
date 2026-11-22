@echo off
setlocal enabledelayedexpansion
title UNIMART + UNI-GRAM Presentation Launcher

echo ======================================================================
echo    UNIMART + UNI-GRAM COLLEGE PROJECT PRESENTATION LAUNCHER
echo    Chitkara University Student Marketplace & Campus Messenger
echo ======================================================================
echo.

:: 1. Verify JDK
set "JAVAC_EXE=javac"
set "JAVA_EXE=java"
where javac >nul 2>&1
if errorlevel 1 (
    if exist "C:\Program Files\Java\jdk-26.0.1\bin\javac.exe" (
        set "JAVAC_EXE=C:\Program Files\Java\jdk-26.0.1\bin\javac.exe"
        set "JAVA_EXE=C:\Program Files\Java\jdk-26.0.1\bin\java.exe"
    ) else if exist "C:\Program Files\Java\latest\bin\javac.exe" (
        set "JAVAC_EXE=C:\Program Files\Java\latest\bin\javac.exe"
        set "JAVA_EXE=C:\Program Files\Java\latest\bin\java.exe"
    ) else (
        echo [ERROR] Java Development Kit (JDK 21+) was not found.
        echo Please ensure JDK is installed and javac is in your PATH.
        pause
        exit /b 1
    )
)

:: 2. Verify Node.js & npm
where node >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Node.js is not found in PATH.
    echo Please install Node.js 18+ from https://nodejs.org/ to run the frontend.
    pause
    exit /b 1
)
where npm >nul 2>&1
if errorlevel 1 (
    echo [ERROR] npm is not found in PATH.
    pause
    exit /b 1
)

:: 3. Compile Java Backend
echo [1/4] Compiling Java backend classes...
if not exist "bin" mkdir "bin"

powershell -NoProfile -Command "$src = (Get-ChildItem -Path 'src/main/java' -Recurse -Filter '*.java') | ForEach-Object { '\"' + $_.FullName.Replace('\', '/') + '\"' }; [System.IO.File]::WriteAllLines('sources.txt', $src)"

"%JAVAC_EXE%" -encoding UTF-8 -d bin -cp "lib/*" "@sources.txt"
if errorlevel 1 (
    echo [ERROR] Java compilation failed.
    del sources.txt >nul 2>&1
    pause
    exit /b 1
)
del sources.txt >nul 2>&1
echo [PASS] Backend compiled successfully.

:: 4. Launch Java Backend Server
echo.
echo [2/4] Starting Uni-Gram Java HTTP API Server on port 8080...
start "Uni-Gram Java Backend (Port 8080)" cmd /k ""%JAVA_EXE%" -cp "bin;lib/*" com.unigram.Main --server"

:: 5. Health Check for Backend
echo [3/4] Verifying backend health at http://127.0.0.1:8080/api/health...
set BACKEND_UP=0
for /l %%i in (1, 1, 15) do (
    if !BACKEND_UP! equ 0 (
        powershell -NoProfile -Command "try { $r = Invoke-WebRequest -Uri 'http://127.0.0.1:8080/api/health' -UseBasicParsing -TimeoutSec 1; if ($r.StatusCode -eq 200) { exit 0 } else { exit 1 } } catch { exit 1 }"
        if not errorlevel 1 (
            set BACKEND_UP=1
        ) else (
            timeout /t 1 /nobreak >nul
        )
    )
)

if !BACKEND_UP! equ 0 (
    echo [ERROR] Backend failed to bind port 8080 within 15 seconds.
    echo Please check the "Uni-Gram Java Backend" console window for details.
    pause
    exit /b 1
)
echo [PASS] Java backend is UP and responsive on port 8080.

:: 6. Launch Vite Frontend
echo.
echo [4/4] Starting UniMart React 19 Frontend...
cd unimart-frontend
if not exist "node_modules" (
    echo Installing frontend dependencies (npm install)...
    call npm install
)
start "UniMart Frontend (Vite)" cmd /k "npm run dev"
cd ..

:: 7. Wait for Frontend
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

:: 8. Open in browser
start http://localhost:5173

echo.
echo ======================================================================
echo    UNIMART + UNI-GRAM READY FOR PROFESSOR PRESENTATION!
echo ======================================================================
echo.
echo  Frontend URL:     http://localhost:5173
echo  Backend Health:   http://127.0.0.1:8080/api/health
echo.
echo  Demo Accounts for Presentation:
echo  ------------------------------------------------------------------
echo  [1] Student A (Seller): satkar0696.becse25@chitkara.edu.in / student123
echo  [2] Student B (Buyer):  priya.k@chitkara.edu.in           / student123
echo  [3] Campus Admin:       admin@chitkara.edu.in             / Unimart@123
echo  ------------------------------------------------------------------
echo.
echo  Workflow to Demonstrate to Professors:
echo  1. Log in as Student A (click 'Student A' quick-fill on login page).
echo  2. Click 'Sell an Item', list a textbook -> Status: Pending Approval.
echo  3. Open incognito/private window, log in as Campus Admin -> Approve it.
echo  4. Back in regular window (or 2nd browser), log in as Student B (Priya).
echo  5. Save textbook to Wishlist and click 'Message Seller' to chat with Satkar.
echo  6. Live student messaging operates via Uni-Gram Java + MySQL!
echo ======================================================================
echo.
echo Press any key to exit this launcher (services will keep running)...
pause >nul
