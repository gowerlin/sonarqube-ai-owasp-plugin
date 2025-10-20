#!/usr/bin/env pwsh
#Requires -Version 7.0

<#
.SYNOPSIS
    產生測試涵蓋率報告
.DESCRIPTION
    使用 JaCoCo 產生測試涵蓋率報告並在瀏覽器中開啟
.NOTES
    Author: SonarQube AI OWASP Plugin Team
    Version: 1.0.0
#>

$ErrorActionPreference = "Stop"

$WORKSPACE_DIR = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$REPORT_PATH = Join-Path $WORKSPACE_DIR "plugin-core\target\site\jacoco\index.html"

Write-Host ""
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "  產生測試涵蓋率報告" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host ""

Write-Host "📊 產生 JaCoCo 涵蓋率報告..." -ForegroundColor Cyan

# 執行 JaCoCo 報告生成
try {
    mvn jacoco:report

    if ($LASTEXITCODE -ne 0) {
        Write-Host "❌ 報告產生失敗 (退出碼: $LASTEXITCODE)" -ForegroundColor Red
        exit $LASTEXITCODE
    }
} catch {
    Write-Host "❌ 執行失敗: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "✅ 報告產生完成" -ForegroundColor Green
Write-Host ""

# 檢查報告檔案是否存在
if (Test-Path $REPORT_PATH) {
    Write-Host "📂 報告位置: $REPORT_PATH" -ForegroundColor Cyan
    Write-Host "🌐 開啟報告..." -ForegroundColor Cyan

    try {
        Start-Process $REPORT_PATH
        Write-Host "✅ 已在瀏覽器中開啟報告" -ForegroundColor Green
    } catch {
        Write-Host "⚠️  無法自動開啟瀏覽器" -ForegroundColor Yellow
        Write-Host "   請手動開啟: $REPORT_PATH" -ForegroundColor Gray
    }
} else {
    Write-Host "⚠️  報告檔案不存在" -ForegroundColor Yellow
    Write-Host "   預期位置: $REPORT_PATH" -ForegroundColor Gray
    Write-Host "   請先執行測試：'🧪 Run All Tests'" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "============================================================" -ForegroundColor Green
Write-Host ""
