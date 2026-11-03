# ==============================================================================
# Vaulta Live Runner (Like "flutter run")
# ==============================================================================
# Usage:
#   .\run.ps1             -> Builds, installs, launches app & streams live logs
#   .\run.ps1 -NoBuild    -> Skips build; installs latest APK, launches & streams logs
#   .\run.ps1 -LogsOnly   -> Just streams live app logs from your phone
# ==============================================================================

param (
    [switch]$NoBuild,
    [switch]$LogsOnly
)

$ErrorActionPreference = "Stop"

# Configure environment
$env:JAVA_HOME = "C:\Program Files\Microsoft\jdk-17.0.21.21-hotspot"
$env:Path = "$env:JAVA_HOME\bin;C:\Users\Shrs\AppData\Local\Android\Sdk\platform-tools;$env:Path"

# Check connected device
$deviceLines = & adb devices | Out-String
$match = [regex]::Match($deviceLines, "(\S+)\s+device\r?\n")
if (-not $match.Success) {
    Write-Host ""
    Write-Host "No Android device or emulator detected!" -ForegroundColor Red
    Write-Host "Please connect your phone via USB with USB Debugging enabled, or start an emulator." -ForegroundColor Yellow
    exit 1
}

$deviceName = $match.Groups[1].Value
Write-Host ""
Write-Host "Target Device: $deviceName" -ForegroundColor Green

if ($LogsOnly) {
    Write-Host "Streaming live app logs from $deviceName (Press Ctrl+C to stop)..." -ForegroundColor Yellow
    Write-Host ""
    & adb logcat -c
    & adb logcat -v color -s Vaulta:V VaultaViewer:V VaultaBrowser:V VaultaVault:V VaultaCrash:V AndroidRuntime:E System.err:W *:F
    exit 0
}

# 1. Build APK
if (-not $NoBuild) {
    Write-Host "[1/3] Building Vaulta debug APK..." -ForegroundColor Cyan
    & .\gradlew assembleDebug
    if ($LASTEXITCODE -ne 0) {
        Write-Host "Build failed!" -ForegroundColor Red
        exit $LASTEXITCODE
    }
    Write-Host "Build succeeded!" -ForegroundColor Green
} else {
    Write-Host "[1/3] Skipping build (-NoBuild requested)..." -ForegroundColor DarkGray
}

# 2. Install APK
$apk = "app\build\outputs\apk\debug\app-debug.apk"
if (-not (Test-Path $apk)) {
    Write-Host "APK not found at $apk. Please run without -NoBuild first." -ForegroundColor Red
    exit 1
}

Write-Host "[2/3] Installing APK onto $deviceName..." -ForegroundColor Cyan
& adb install -r $apk
if ($LASTEXITCODE -ne 0) {
    Write-Host "Installation failed!" -ForegroundColor Red
    exit $LASTEXITCODE
}
Write-Host "Installation complete!" -ForegroundColor Green

# 3. Launch App
Write-Host "[3/3] Launching Vaulta on $deviceName..." -ForegroundColor Cyan
& adb shell am start -n com.vaulta.shrs/.MainActivity | Out-Null
Write-Host "App launched!" -ForegroundColor Green

# 4. Stream Logs
Write-Host ""
Write-Host "========================================================" -ForegroundColor DarkGray
Write-Host " REAL-TIME APP LOGS (Press Ctrl+C to exit)" -ForegroundColor Yellow
Write-Host " Showing Vaulta logs, viewer/crypto operations, and errors" -ForegroundColor DarkGray
Write-Host "========================================================" -ForegroundColor DarkGray
Write-Host ""

& adb logcat -c
& adb logcat -v color -s Vaulta:V VaultaViewer:V VaultaBrowser:V VaultaVault:V VaultaCrash:V AndroidRuntime:E System.err:W *:F
