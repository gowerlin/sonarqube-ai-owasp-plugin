#!/usr/bin/env pwsh
#Requires -Version 7.0

<#
.SYNOPSIS
    執行指定的測試類別
.DESCRIPTION
    互動式選擇並執行特定的 JUnit 測試類別
.NOTES
    Author: SonarQube AI OWASP Plugin Team
    Version: 1.0.0
#>

$ErrorActionPreference = "Stop"

Write-Host ""
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "  執行特定測試類別" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host ""

# 提示輸入測試類別名稱
$testClass = Read-Host "請輸入測試類別名稱（例如：RuleEngineTest）"

if ([string]::IsNullOrWhiteSpace($testClass)) {
    Write-Host "❌ 未指定測試類別" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "🧪 執行測試: $testClass" -ForegroundColor Cyan
Write-Host ""

# 執行測試
try {
    mvn test -Dtest=$testClass

    if ($LASTEXITCODE -eq 0) {
        Write-Host ""
        Write-Host "✅ 測試通過" -ForegroundColor Green
        exit 0
    } else {
        Write-Host ""
        Write-Host "❌ 測試失敗 (退出碼: $LASTEXITCODE)" -ForegroundColor Red
        exit $LASTEXITCODE
    }
} catch {
    Write-Host ""
    Write-Host "❌ 執行失敗: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}
