@echo off
title Collectes Reset Watcher
cd /d "%~dp0\.."
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0watch-collectes-reset.ps1"
pause
