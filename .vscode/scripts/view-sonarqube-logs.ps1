#!/usr/bin/env pwsh
#Requires -Version 7.0

<#
.SYNOPSIS
    查看本機 SonarQube 日誌
.DESCRIPTION
    即時監看 SonarQube 的 sonar.log 日誌檔案
.NOTES
    Author: SonarQube AI OWASP Plugin Team
    Version: 1.0.0
#>

$ErrorActionPreference = "Stop"

# SonarQube 日誌目錄
$SONARQUBE_HOME = "E:\sonarqube-community-25.10.0.114319"
$LOG_DIR = Join-Path $SONARQUBE_HOME "logs"
$MAIN_LOG = Join-Path $LOG_DIR "sonar.log"

Write-Host ""
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "  查看本機 SonarQube 日誌" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host ""

# 檢查日誌目錄是否存在
if (-not (Test-Path $LOG_DIR)) {
    Write-Host "❌ 找不到日誌目錄" -ForegroundColor Red
    Write-Host "   預期位置: $LOG_DIR" -ForegroundColor Yellow
    Write-Host "   請確認 SonarQube 已正確安裝在: $SONARQUBE_HOME" -ForegroundColor Yellow
    exit 1
}

# 檢查主日誌檔案是否存在
if (-not (Test-Path $MAIN_LOG)) {
    Write-Host "⚠️  主日誌檔案尚未建立" -ForegroundColor Yellow
    Write-Host "   預期位置: $MAIN_LOG" -ForegroundColor Gray
    Write-Host ""
    Write-Host "📂 可用的日誌檔案：" -ForegroundColor Cyan
    $logFiles = Get-ChildItem -Path $LOG_DIR -Filter "*.log" -ErrorAction SilentlyContinue
    if ($logFiles) {
        foreach ($file in $logFiles) {
            Write-Host "   - $($file.Name) ($(($file.Length / 1KB).ToString('F2')) KB)" -ForegroundColor Gray
        }
        Write-Host ""
        Write-Host "💡 提示：請先啟動 SonarQube 以產生日誌" -ForegroundColor Yellow
    } else {
        Write-Host "   (無日誌檔案)" -ForegroundColor Gray
    }
    exit 0
}

# 顯示日誌資訊
$logInfo = Get-Item $MAIN_LOG
Write-Host "📋 日誌檔案資訊：" -ForegroundColor Cyan
Write-Host "   檔案: $($logInfo.Name)" -ForegroundColor Gray
Write-Host "   大小: $(($logInfo.Length / 1KB).ToString('F2')) KB" -ForegroundColor Gray
Write-Host "   最後修改: $($logInfo.LastWriteTime)" -ForegroundColor Gray
Write-Host ""

Write-Host "🔄 開始監看日誌（按 Ctrl+C 停止）..." -ForegroundColor Yellow
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host ""

try {
    # 即時監看日誌（顯示最後 50 行並持續追蹤新內容）
    Get-Content -Path $MAIN_LOG -Wait -Tail 50
} catch {
    Write-Host ""
    Write-Host "⚠️  監看已停止: $($_.Exception.Message)" -ForegroundColor Yellow
    exit 0
}
