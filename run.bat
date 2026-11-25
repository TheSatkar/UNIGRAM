@echo off
setlocal
cd /d "%~dp0"
title UNI-GRAM Campus Messaging System

if not exist ".env" (
  echo [!] Missing .env file in Final-java-project.
  echo     Copy .env.example to .env and configure your MySQL credentials first.
  pause
  exit /b 1
)

set "MYSQL_JAR=%USERPROFILE%\.m2\repository\com\mysql\mysql-connector-j\8.4.0\mysql-connector-j-8.4.0.jar"
set "PROTOBUF_JAR=%USERPROFILE%\.m2\repository\com\google\protobuf\protobuf-java\3.25.1\protobuf-java-3.25.1.jar"

where mvn >nul 2>&1
if not errorlevel 1 (
  call mvn -q compile exec:java -Dexec.mainClass=com.unigram.Main
  goto :done
)

if exist "%MYSQL_JAR%" (
  if not exist "target\classes" mkdir "target\classes"
  powershell -NoProfile -Command "$q=[char]34; $f=Get-ChildItem 'src\main\java' -Recurse -File -Filter '*.java' | ForEach-Object { $q + $_.FullName.Replace('\','/') + $q }; [IO.File]::WriteAllLines('sources.txt',[string[]]@($f),[Text.Encoding]::ASCII)"
  javac -encoding UTF-8 -d "target\classes" "@sources.txt"
  del "sources.txt" >nul 2>&1
  if errorlevel 1 (
    echo [ERROR] Compilation failed.
    pause
    exit /b 1
  )
  java -cp "target\classes;%MYSQL_JAR%;%PROTOBUF_JAR%" com.unigram.Main
) else (
  echo [ERROR] Maven was not found in PATH and MySQL Connector/J was not found in %USERPROFILE%\.m2.
  echo         Open this folder in IntelliJ IDEA as a Maven project and run com.unigram.Main.
  pause
  exit /b 1
)

:done
endlocal
