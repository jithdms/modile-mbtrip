param(
    [string]$DeviceA = 'emulator-5554',
    [string]$DeviceB = 'emulator-5556',
    [Parameter(Mandatory=$true)][string]$EvidenceDirectory
)
$ErrorActionPreference = 'Stop'
$adb = Join-Path $env:LOCALAPPDATA 'Android\Sdk\platform-tools\adb.exe'
$repo = Split-Path $PSScriptRoot -Parent
$run = [guid]::NewGuid().ToString()
$remote = "/sdcard/Android/data/com.example.mbtrip/files/two-device-$run.json"
New-Item -ItemType Directory -Path $EvidenceDirectory -Force | Out-Null
$run | Set-Content -LiteralPath (Join-Path $EvidenceDirectory 'two-device-run.txt')
foreach ($serial in @($DeviceA,$DeviceB)) {
    $boot = & $adb -s $serial shell getprop sys.boot_completed
    if ($LASTEXITCODE -ne 0 -or $boot.Trim() -ne '1') { throw "$serial is not booted" }
    & $adb -s $serial install -r (Join-Path $repo 'app\build\outputs\apk\debug\app-debug.apk')
    if ($LASTEXITCODE -ne 0) { throw "App install failed on $serial" }
    & $adb -s $serial install -r (Join-Path $repo 'app\build\outputs\apk\androidTest\debug\app-debug-androidTest.apk')
    if ($LASTEXITCODE -ne 0) { throw "Test install failed on $serial" }
}
function Start-Role([string]$Serial,[string]$Role) {
    $arguments = @('-s',$Serial,'shell','am','instrument','-w','-r',
        '-e','class','com.example.mbtrip.TwoDeviceLiveInstrumentedTest',
        '-e','mbtripTwoDevice','true','-e','mbtripTwoDeviceRun',$run,
        '-e','mbtripTwoDeviceRole',$Role,'com.example.mbtrip.test/androidx.test.runner.AndroidJUnitRunner')
    Start-Process -FilePath $adb -ArgumentList $arguments -WindowStyle Hidden -PassThru `
        -RedirectStandardOutput (Join-Path $EvidenceDirectory "$Role-result.txt") `
        -RedirectStandardError (Join-Path $EvidenceDirectory "$Role-error.txt")
}
$owner = Start-Role $DeviceA 'owner'
$guest = Start-Role $DeviceB 'guest'
$handoff = Join-Path $EvidenceDirectory 'two-device-handoff.json'
$transferred = $false
for ($attempt=0; $attempt -lt 90; $attempt++) {
    & $adb -s $DeviceA pull $remote $handoff 2>$null | Out-Null
    if ($LASTEXITCODE -eq 0) {
        & $adb -s $DeviceB shell mkdir -p /sdcard/Android/data/com.example.mbtrip/files
        & $adb -s $DeviceB push $handoff $remote | Out-Null
        if ($LASTEXITCODE -ne 0) { throw 'Handoff transfer failed' }
        $transferred = $true
        Write-Output 'Invitation transferred between independent devices.'
        break
    }
    if ($owner.HasExited) { break }
    Start-Sleep -Seconds 1
}
if (-not $transferred) { Write-Warning 'No invitation handoff; waiting for test cleanup and results.' }
$deadline = [DateTime]::UtcNow.AddSeconds(210)
while ((-not $owner.HasExited -or -not $guest.HasExited) -and [DateTime]::UtcNow -lt $deadline) {
    Start-Sleep -Seconds 2
}
foreach ($pair in @(@($DeviceA,'owner'),@($DeviceB,'guest'))) {
    $serial=$pair[0]; $role=$pair[1]
    & $adb -s $serial logcat -d -s MBTripTwoDevice:I | Set-Content -LiteralPath (Join-Path $EvidenceDirectory "$role-cleanup.log")
    & $adb -s $serial pull "/sdcard/Android/data/com.example.mbtrip/files/two-device-$run-$role.png" (Join-Path $EvidenceDirectory "$role-screen.png") 2>$null | Out-Null
    $result=[System.IO.File]::ReadAllText((Join-Path $EvidenceDirectory "$role-result.txt"))
    $clean=[System.IO.File]::ReadAllText((Join-Path $EvidenceDirectory "$role-cleanup.log"))
    if ($result -notmatch 'OK \(1 test\)' -or $clean -notmatch "RESULT role=$role stage=passed cleanupErrors=\[\]") {
        throw "$role verification or cleanup failed; inspect its evidence files"
    }
    Write-Output "${role}: one test passed; temporary data cleanup succeeded."
}
Write-Output 'Two-device validation passed.'
