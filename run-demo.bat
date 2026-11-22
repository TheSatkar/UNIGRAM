@echo off
setlocal enabledelayedexpansion
title UNIMART + UNI-GRAM College Presentation Launcher

echo ======================================================================
echo    UNIMART + UNI-GRAM COLLEGE PROJECT PRESENTATION LAUNCHER
echo    Chitkara University Student Marketplace & Campus Messenger
echo ======================================================================
echo.

:: Check for JDK
set "JAVAC_EXE=javac"
where javac >nul 2>&1
if errorlevel 1 (
    if exist "C:\Program Files\Java\jdk-26.0.1\bin\javac.exe" (
        set "JAVAC_EXE=C:\Program Files\Java\jdk-26.0.1\bin\javac.exe"
        set "JAVA_EXE=C:\Program Files\Java\jdk-26.0.1\bin\java.exe"
    ) else (
        echo [ERROR] Java Development Kit (JDK) not found in PATH.
        echo Please ensure JDK 21+ is installed.
        pause
        exit /b 1
    )
) else (
    set "JAVA_EXE=java"
)

:: 1. Compile Java Backend
echo [1/3] Compiling Java backend classes...
if not exist "bin" mkdir "bin"

powershell -NoProfile -Command "$src = (Get-ChildItem -Path 'src/main/java' -Recurse -Filter '*.java') | ForEach-Object { '\"' + $_.FullName.Replace('\', '/') + '\"' }; [System.IO.File]::WriteAllLines('sources.txt', $src)"

"%JAVAC_EXE%" -encoding UTF-8 -d bin -cp "lib/*" "@sources.txt"
if errorlevel 1 (
    echo [ERROR] Java compilation failed.
    pause
    exit /b 1
)
echo [PASS] Backend compiled successfully.
del sources.txt >nul 2>&1

:: 2. Launch Java Backend Server
echo.
echo [2/3] Starting Uni-Gram Java HTTP Server (Option 15: http://127.0.0.1:8080)...
start "Uni-Gram Java Backend (Port 8080)" cmd /k ""%JAVA_EXE%" -cp "bin;lib/*" com.unigram.Main"

:: Wait 2 seconds for backend to bind port
timeout /t 2 /nobreak >nul

:: 3. Launch Vite Frontend
echo.
echo [3/3] Starting UniMart React 19 Frontend...
cd unimart-frontend
start "UniMart Frontend (Vite)" cmd /k "npm run dev"
cd ..

echo.
echo ======================================================================
echo    DEMO READY FOR PROFESSOR PRESENTATION!
echo ======================================================================
echo.
echo  Marketplace Frontend:  http://localhost:5173
echo  Backend Health Check:  http://127.0.0.1:8080/api/health
echo.
echo  Demo Presentation Accounts:
echo  -------------------------------------------------------------
echo  [1] Admin:     admin@chitkara.edu.in             / Unimart@123
echo  [2] Student A: satkar0696.becse25@chitkara.edu.in / student123
echo  [3] Student B: priya.k@chitkara.edu.in           / student123
echo  -------------------------------------------------------------
echo.
echo  Presentation Flow:
echo  1. Open http://localhost:5173 in browser.
echo  2. Click 'Student A' quick-fill to log in as Satkar Singh.
echo  3. List an item for sale (e.g. Textbook) -> submitted as Pending.
echo  4. In private/incognito window, log in as Campus Admin -> Approve.
echo  5. In another window, log in as Student B (Priya) -> Add to Wishlist & Message Satkar.
echo  6. Both students chat live via Uni-Gram!
echo ======================================================================
echo.
echo Press any key to exit this launcher window (services will stay running)...
pause >nul
