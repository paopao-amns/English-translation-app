@echo off
setlocal
echo ============================================
echo  English Learner - Release APK Builder
echo ============================================
echo.

:: Check Java
java -version >nul 2>&1
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Java JDK 17 not found. Please install JDK 17.
    echo   Download: https://adoptium.net/download/
    pause
    exit /b 1
)

:: Check Android SDK
if "%ANDROID_HOME%"=="" (
    echo [WARN] ANDROID_HOME not set. Trying default locations...
    if exist "%LOCALAPPDATA%\Android\Sdk" (
        set ANDROID_HOME=%LOCALAPPDATA%\Android\Sdk
    ) else if exist "C:\Android\Sdk" (
        set ANDROID_HOME=C:\Android\Sdk
    ) else (
        echo [ERROR] Android SDK not found. Please install Android Studio or set ANDROID_HOME.
        pause
        exit /b 1
    )
)
echo [OK] ANDROID_HOME=%ANDROID_HOME%

:: Write local.properties
echo sdk.dir=%ANDROID_HOME:\=/%> local.properties
echo [OK] local.properties created.

:: Check/Generate keystore
if not exist "release.keystore" (
    echo.
    echo [INFO] No release.keystore found. Generating one...
    keytool -genkeypair ^
        -alias release ^
        -keyalg RSA ^
        -keysize 2048 ^
        -validity 36500 ^
        -keystore release.keystore ^
        -storepass android ^
        -keypass android ^
        -dname "CN=English Learner, OU=Dev, O=EngLearn, L=City, S=State, C=CN" ^
        -noprompt
    if %ERRORLEVEL% equ 0 (
        echo [OK] Keystore generated.
    ) else (
        echo [ERROR] Failed to generate keystore.
        pause
        exit /b 1
    )
) else (
    echo [OK] release.keystore found.
)

:: Check Gradle wrapper
if not exist "gradle\wrapper\gradle-wrapper.jar" (
    echo.
    echo [ERROR] gradle-wrapper.jar not found!
    echo   Please download Gradle 8.5 from https://gradle.org/releases/
    echo   and copy lib/gradle-wrapper-8.5.jar to gradle/wrapper/gradle-wrapper.jar
    pause
    exit /b 1
)

:: Build
echo.
echo [INFO] Building release APK...
call gradlew.bat assembleRelease
if %ERRORLEVEL% neq 0 (
    echo.
    echo [ERROR] Build failed.
    pause
    exit /b 1
)

echo.
echo ============================================
echo  BUILD SUCCESSFUL
echo  APK: app\build\outputs\apk\release\app-release.apk
echo ============================================
pause
endlocal
