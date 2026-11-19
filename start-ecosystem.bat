@echo off
title MAX UNIVERSAL AI ECOSYSTEM
cd /d "%~dp0"
echo =======================================================
echo   STARTING MAX UNIVERSAL PERSONAL AI ECOSYSTEM
echo =======================================================
start "MAX Central Backend Server" cmd /k "cd backend && node src/server.js"
timeout /t 2 /nobreak >nul
start "MAX Desktop Workstation" cmd /k "cd desktop && node main.js"
echo.
echo Ecosystem online:
echo - Central Backend: http://localhost:8085
echo - Desktop Interface: http://localhost:8086
echo.
pause
