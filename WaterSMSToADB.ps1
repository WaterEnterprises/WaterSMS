param(
  [switch]$NoBuild
)

$ErrorActionPreference = "Stop"
$PackageName = "jv.watersms.enterprises"
$LauncherActivity = "jv.watersms.enterprises/.MainActivity"
$ApkPath = Join-Path $PSScriptRoot "app\build\outputs\apk\debug\app-debug.apk"

if (-not $NoBuild) {
  Write-Host "[0/3] Stopping stale Gradle daemon..." -ForegroundColor Cyan
  & ./gradlew --stop 2>&1 | Out-Null

  Write-Host "[1/3] Building APK..." -ForegroundColor Cyan
  Push-Location $PSScriptRoot
  try {
    & ./gradlew assembleDebug --no-configuration-cache
    if ($LASTEXITCODE -ne 0) { throw "Build failed" }
  } finally { Pop-Location }
}

Write-Host "[2/3] Checking ADB device..." -ForegroundColor Cyan
$devices = & adb devices
if ($devices -match "[a-f0-9]+\s+device") {
  Write-Host "  Device found." -ForegroundColor Green
} else {
  Write-Host "  No device connected. Plug in your device and enable USB debugging." -ForegroundColor Red
  exit 1
}

Write-Host "[3/3] Installing and launching..." -ForegroundColor Cyan
Write-Host "  APK: $ApkPath" -ForegroundColor Gray

Write-Host "  Uninstalling previous version..." -ForegroundColor Yellow
$null = & adb uninstall $PackageName 2>&1

& adb install -r -d $ApkPath
if ($LASTEXITCODE -ne 0) { throw "Install failed" }

& adb shell am start -n $LauncherActivity -W --activity-clear-top 2>&1 | Out-Null
if ($LASTEXITCODE -eq 0) {
  Write-Host "  Done! App launched." -ForegroundColor Green
}
