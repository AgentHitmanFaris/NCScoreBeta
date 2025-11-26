@echo off
title Push to NC-Score GitHub

:: Change the directory to your project folder
cd "D:\Document\NCScoreBeta"

echo ---------------------------------------------------------
echo Preparing to push updates for N.C-Score
echo ---------------------------------------------------------

:: Step 1: Add all files
echo [STEP 1] Adding all new/changed files...
git add .
echo.

:: Step 2: Ask for a commit message
echo [STEP 2] Please enter your commit message:
set /p commit_message="Message: "

:: If the message is empty, set a default one (optional)
if "%commit_message%"=="" (
    set "commit_message=Quick update"
    echo No message entered. Using 'Quick update'.
)
echo.

:: Step 3: Commit the files
echo [STEP 3] Committing files with message: "%commit_message%"
git commit -m "%commit_message%"
echo.

:: Step 4: Push to GitHub
echo [STEP 4] Pushing changes to GitHub...
git push
echo.

echo ---------------------------------------------------------
echo All Done! Your changes are on GitHub.
echo ---------------------------------------------------------
echo.
pause