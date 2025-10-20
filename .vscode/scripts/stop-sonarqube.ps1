#!/usr/bin/env pwsh
#Requires -Version 7.0

<#
.SYNOPSIS
    停止本機 SonarQube 服務
.DESCRIPTION
    終止運行中的 SonarQube Java 進程
.NOTES
    Author: SonarQube AI OWASP Plugin Team
    Version: 1.0.0
#>

$ErrorActionPreference = "Stop"

Write-Host ""
Write-Host "============================================================" -ForegroundColor Yellow
Write-Host "  停止本機 SonarQube" -ForegroundColor Yellow
Write-Host "============================================================" -ForegroundColor Yellow
Write-Host ""

# 查找 SonarQube 進程
Write-Host "🔍 搜尋 SonarQube 進程..." -ForegroundColor Cyan
$sonarProcesses = Get-Process | Where-Object {
    $_.ProcessName -eq 'java' -and
    $_.CommandLine -like '*sonar*'
} -ErrorAction SilentlyContinue

if (-not $sonarProcesses) {
    Write-Host "⚠️  未找到運行中的 SonarQube 進程" -ForegroundColor Yellow
    Write-Host "   SonarQube 可能已經停止或未啟動" -ForegroundColor Gray
    exit 0
}

# 顯示找到的進程
Write-Host "📋 找到 $($sonarProcesses.Count) 個 SonarQube 相關進程：" -ForegroundColor Cyan
foreach ($proc in $sonarProcesses) {
    Write-Host "   - 進程 ID: $($proc.Id) | 啟動時間: $($proc.StartTime)" -ForegroundColor Gray
}
Write-Host ""

# 終止進程
Write-Host "🛑 停止 SonarQube..." -ForegroundColor Yellow
try {
    $sonarProcesses | Stop-Process -Force
    Start-Sleep -Seconds 2

    # 驗證進程是否已停止
    $remainingProcesses = Get-Process | Where-Object {
        $_.ProcessName -eq 'java' -and
        $_.CommandLine -like '*sonar*'
    } -ErrorAction SilentlyContinue

    if ($remainingProcesses) {
        Write-Host "⚠️  部分進程仍在運行，嘗試再次終止..." -ForegroundColor Yellow
        $remainingProcesses | Stop-Process -Force
        Start-Sleep -Seconds 1
    }

    Write-Host "✅ SonarQube 已成功停止" -ForegroundColor Green
    Write-Host "   已終止 $($sonarProcesses.Count) 個進程" -ForegroundColor Gray

} catch {
    Write-Host "❌ 停止失敗: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host "   您可能需要管理員權限或手動終止進程" -ForegroundColor Yellow
    exit 1
}

Write-Host ""
Write-Host "============================================================" -ForegroundColor Green
Write-Host ""
