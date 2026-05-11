@echo off
java -jar "%~dp0CruiseMS.jar"
if errorlevel 1 (
    echo.
    echo ERROR: Application failed to start. See above for details.
    pause
)
