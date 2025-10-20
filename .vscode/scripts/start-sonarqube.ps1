#!/usr/bin/env pwsh
#Requires -Version 7.0

<#
.SYNOPSIS
    啟動本機 SonarQube 服務
.DESCRIPTION
    啟動安裝在本機的 SonarQube Community Edition
.NOTES
    Author: SonarQube AI OWASP Plugin Team
    Version: 1.0.0
#>

$ErrorActionPreference = "Stop"

# SonarQube 安裝路徑
$SONARQUBE_HOME = "E:\sonarqube-community-25.10.0.114319"
$START_SCRIPT = Join-Path $SONARQUBE_HOME "bin\windows-x86-64\StartSonar.bat"

Write-Host ""
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "  啟動本機 SonarQube" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host ""

# 檢查 SonarQube 是否已經在運行
Write-Host "🔍 檢查 SonarQube 狀態..." -ForegroundColor Yellow
$existingProcess = Get-Process | Where-Object {
    $_.ProcessName -eq 'java' -and
    $_.CommandLine -like '*sonar*'
} -ErrorAction SilentlyContinue

if ($existingProcess) {
    Write-Host "⚠️  SonarQube 已經在運行中" -ForegroundColor Yellow
    Write-Host "   進程 ID: $($existingProcess.Id)" -ForegroundColor Gray
    Write-Host "   如需重啟，請先執行 'Stop SonarQube' 任務" -ForegroundColor Gray
    exit 0
}

# 檢查啟動腳本是否存在
if (-not (Test-Path $START_SCRIPT)) {
    Write-Host "❌ 找不到 SonarQube 啟動腳本" -ForegroundColor Red
    Write-Host "   預期位置: $START_SCRIPT" -ForegroundColor Yellow
    Write-Host "   請確認 SonarQube 已正確安裝在: $SONARQUBE_HOME" -ForegroundColor Yellow
    exit 1
}

# 啟動 SonarQube
Write-Host "🚀 啟動 SonarQube..." -ForegroundColor Cyan
Write-Host "   執行: StartSonar.bat" -ForegroundColor Gray

try {
    $workingDir = Split-Path $START_SCRIPT -Parent
    Start-Process -FilePath $START_SCRIPT -WorkingDirectory $workingDir -WindowStyle Normal

    Start-Sleep -Seconds 2

    # 驗證進程是否啟動
    $newProcess = Get-Process | Where-Object {
        $_.ProcessName -eq 'java' -and
        $_.CommandLine -like '*sonar*'
    } -ErrorAction SilentlyContinue

    if ($newProcess) {
        Write-Host "✅ SonarQube 正在啟動" -ForegroundColor Green
        Write-Host "   進程 ID: $($newProcess.Id)" -ForegroundColor Gray
    } else {
        Write-Host "⚠️  進程啟動確認中..." -ForegroundColor Yellow
    }

} catch {
    Write-Host "❌ 啟動失敗: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "📋 訪問資訊：" -ForegroundColor Yellow
Write-Host "   URL: http://localhost:9000" -ForegroundColor Cyan
Write-Host "   預設帳號: admin / admin" -ForegroundColor Gray
Write-Host "   啟動時間: 約 30-60 秒" -ForegroundColor Gray
Write-Host "   日誌位置: $SONARQUBE_HOME\logs\" -ForegroundColor Gray
Write-Host ""
Write-Host "============================================================" -ForegroundColor Green
Write-Host ""
