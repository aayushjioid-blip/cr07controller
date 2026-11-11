# Install Cricket 07 HID Controller APK to connected OnePlus 13R
Write-Host "Checking for connected Android phone..." -ForegroundColor Cyan
$devices = & ".\platform-tools\adb.exe" devices

Write-Host $devices

if ($devices -match "device\b") {
    Write-Host "OnePlus detected! Installing app-release.apk..." -ForegroundColor Green
    & ".\platform-tools\adb.exe" install -r ".\compiled_apks\app-release.apk"
    Write-Host "`nInstallation successful! Launch 'Cricket 07 Controller' on your phone." -ForegroundColor Green
} else {
    Write-Host "`nNo authorized device detected yet." -ForegroundColor Yellow
    Write-Host "1. Plug in your OnePlus 13R via USB cable."
    Write-Host "2. Enable USB Debugging in Settings > Developer Options."
    Write-Host "3. Unlock your phone and tap 'Allow' when the prompt appears."
    Write-Host "4. Re-run this script: .\install_to_phone.ps1"
}
