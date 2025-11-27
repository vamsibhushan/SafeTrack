@echo off
echo Starting clean and rebuild process...

echo Stopping Gradle daemon...
call gradlew --stop

echo Cleaning project...
call gradlew clean --no-daemon

echo Deleting build directories...
rmdir /s /q app\build 2>nul
rmdir /s /q build 2>nul
rmdir /s /q .gradle 2>nul

echo Creating required directories...
mkdir app\src\main\java 2>nul
mkdir app\src\debug\java 2>nul
mkdir app\src\debug\res 2>nul

echo Building project with stacktrace for detailed error reporting...
call gradlew build --no-daemon --stacktrace

echo Process completed.
pause 