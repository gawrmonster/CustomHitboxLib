@echo off
cd /d "%~dp0"
echo Building CustomHitboxLib...
call gradlew.bat build --no-daemon
if %ERRORLEVEL% neq 0 (
    echo Build failed!
    pause
    exit /b %ERRORLEVEL%
)
echo.
echo Copying jar to mods folder...
copy /Y "build\libs\customhitboxlib-1.0.0.jar" "C:\Users\TGDD\AppData\Roaming\.minecraft\versions\TestingPack\mods\customhitboxlib-1.0.0.jar"
echo Done!
pause
