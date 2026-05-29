@echo off
echo ══════════════════════════════════════════════════════════
echo   Elite International School ERP — GitHub Upload Script
echo ══════════════════════════════════════════════════════════
echo.

:: Step 1: Initialize Git
echo [1/6] Initializing Git repository...
git init
echo.

:: Step 2: Add all files
echo [2/6] Staging all files...
git add .
echo.

:: Step 3: Commit
echo [3/6] Creating initial commit...
git commit -m "Initial commit - Final Project Submission"
echo.

:: Step 4: Rename branch to main
echo [4/6] Renaming branch to main...
git branch -M main
echo.

:: Step 5: Prompt for GitHub URL
echo [5/6] Enter your GitHub repository URL:
echo        (e.g. https://github.com/username/repo.git)
echo.
set /p REPO_URL="Paste URL here: "
echo.

:: Step 6: Add remote and push
echo [6/6] Pushing to GitHub...
git remote add origin %REPO_URL%
git push -u origin main
echo.

echo ══════════════════════════════════════════════════════════
echo   ✅ Upload complete! Your code is now on GitHub.
echo ══════════════════════════════════════════════════════════
echo.
pause
