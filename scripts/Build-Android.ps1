[CmdletBinding()]
param(
    [ValidateSet('Debug', 'Release')][string]$Configuration = 'Debug',
    [string]$HBuilderPath = 'D:\HBuilderX',
    [string]$AndroidSdk = $env:ANDROID_HOME,
    [string]$GradleUserHome = $env:GRADLE_USER_HOME,
    [switch]$SkipExport
)

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
if (-not $AndroidSdk) { $AndroidSdk = 'D:\androidSDK' }
if (-not $GradleUserHome) { $GradleUserHome = Join-Path $projectRoot 'unpackage/local-build/gradle-cache' }
$cliPath = Join-Path $HBuilderPath 'cli.exe'
if (-not (Test-Path -LiteralPath $cliPath)) { throw "HBuilderX CLI not found: $cliPath" }
if (-not (Test-Path -LiteralPath (Join-Path $AndroidSdk 'platforms/android-35/android.jar'))) {
    throw "Install Android SDK Platform 35 in $AndroidSdk first."
}

Push-Location $projectRoot
$previousJavaHome = $env:JAVA_HOME
try {
    if (-not $SkipExport) {
        $exportLog = & $cliPath publish app-android --project $projectRoot --type appResource 2>&1
        $exportLog | ForEach-Object { Write-Host $_ }
        if ($LASTEXITCODE -ne 0 -or ($exportLog -join "`n") -notmatch '导出 android 成功|export.*(success|succeed)') {
            throw 'HBuilderX resource export did not succeed. Open HBuilderX and retry.'
        }
    }
    python (Join-Path $PSScriptRoot 'prepare_android.py') --project $projectRoot --android-sdk $AndroidSdk
    if ($LASTEXITCODE -ne 0) { throw 'Preparing the local Android project failed.' }
    $env:JAVA_HOME = Join-Path $HBuilderPath 'plugins/amazon-corretto'
    if (-not (Test-Path -LiteralPath (Join-Path $env:JAVA_HOME 'bin/java.exe'))) {
        throw 'HBuilderX bundled JDK 17 was not found.'
    }
    $gradlePath = Join-Path $projectRoot 'unpackage/local-build/tools/gradle-8.14.3/bin/gradle.bat'
    $nativeProject = Join-Path $projectRoot 'unpackage/local-build/android'
    & $gradlePath --project-dir $nativeProject --gradle-user-home $GradleUserHome --console plain ":app:assemble$Configuration"
    if ($LASTEXITCODE -ne 0) { throw 'Local Gradle build failed.' }
    $variant = $Configuration.ToLowerInvariant()
    $apkName = if ($Configuration -eq 'Debug') { 'app-debug.apk' } else { 'app-release-unsigned.apk' }
    $apkPath = Join-Path $nativeProject "app/build/outputs/apk/$variant/$apkName"
    if (-not (Test-Path -LiteralPath $apkPath)) { throw "APK missing: $apkPath" }
    $outputDir = Join-Path $projectRoot 'unpackage/release/apk'
    New-Item -ItemType Directory -Force -Path $outputDir | Out-Null
    $suffix = if ($Configuration -eq 'Debug') { 'debug' } else { 'release-unsigned' }
    $exportedManifest = Get-Content -LiteralPath (Join-Path $projectRoot 'unpackage/resources/app-android/__UNI__7A2B275/www/manifest.json') -Raw | ConvertFrom-Json
    $outputPath = Join-Path $outputDir "DoneLog-$($exportedManifest.version.name)-local-$suffix.apk"
    Copy-Item -LiteralPath $apkPath -Destination $outputPath -Force
    Get-FileHash -LiteralPath $outputPath -Algorithm SHA256
    Write-Host "Local APK: $outputPath"
} finally {
    $env:JAVA_HOME = $previousJavaHome
    Pop-Location
}
