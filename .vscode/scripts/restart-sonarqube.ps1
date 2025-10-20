#!/usr/bin/env pwsh
#Requires -Version 7.0

<#
.SYNOPSIS
    重啟本機 SonarQube 服務
.DESCRIPTION
    停止並重新啟動安裝在本機的 SonarQube
.NOTES
    Author: SonarQube AI OWASP Plugin Team
    Version: 1.0.0
#>

$ErrorActionPreference = "Stop"

$SCRIPTS_DIR = $PSScriptRoot
$STOP_SCRIPT = Join-Path $SCRIPTS_DIR "stop-sonarqube.ps1"
$START_SCRIPT = Join-Path $SCRIPTS_DIR "start-sonarqube.ps1"

Write-Host ""
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "  重啟本機 SonarQube" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host ""

# 步驟 1: 停止 SonarQube
Write-Host "1️⃣ 停止 SonarQube..." -ForegroundColor Yellow
Write-Host "------------------------------------------------------------" -ForegroundColor Gray
if (Test-Path $STOP_SCRIPT) {
    & $STOP_SCRIPT
    if ($LASTEXITCODE -ne 0) {
        Write-Host "❌ 停止失敗，無法繼續重啟" -ForegroundColor Red
        exit 1
    }
} else {
    Write-Host "❌ 找不到停止腳本: $STOP_SCRIPT" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "⏳ 等待 3 秒..." -ForegroundColor Gray
Start-Sleep -Seconds 3
Write-Host ""

# 步驟 2: 啟動 SonarQube
Write-Host "2️⃣ 啟動 SonarQube..." -ForegroundColor Cyan
Write-Host "------------------------------------------------------------" -ForegroundColor Gray
if (Test-Path $START_SCRIPT) {
    & $START_SCRIPT
    if ($LASTEXITCODE -ne 0) {
        Write-Host "❌ 啟動失敗" -ForegroundColor Red
        exit 1
    }
} else {
    Write-Host "❌ 找不到啟動腳本: $START_SCRIPT" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "============================================================" -ForegroundColor Green
Write-Host "  ✅ SonarQube 重啟完成" -ForegroundColor Green
Write-Host "============================================================" -ForegroundColor Green
Write-Host ""
