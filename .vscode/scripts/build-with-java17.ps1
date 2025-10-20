#!/usr/bin/env pwsh
#Requires -Version 7.0

<#
.SYNOPSIS
    使用 Java 17 建置 SonarQube AI OWASP Plugin

.DESCRIPTION
    此腳本強制使用 Java 17 進行建置，無論系統預設 Java 版本為何
#>

param(
    [switch]$SkipTests = $true,
    [switch]$CleanBuild = $true
)

$ErrorActionPreference = "Stop"

Write-Host ""
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "  使用 Java 17 建置 SonarQube AI OWASP Plugin" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host ""

# 設定 Java 17 環境
$env:JAVA_HOME = "C:\Program Files\Java\jdk-17"
$env:PATH = "C:\Program Files\Java\jdk-17\bin;" + $env:PATH

# 驗證 Java 版本
Write-Host "📦 步驟 1/3: 驗證 Java 版本..." -ForegroundColor Cyan
try {
    $javaVersion = java -version 2>&1 | Select-Object -First 1
    Write-Host "✅ Java 版本: $javaVersion" -ForegroundColor Green

    if ($javaVersion -notlike "*17.*") {
        throw "Java 版本不是 17，請檢查環境設定"
    }
} catch {
    Write-Host "❌ Java 17 驗證失敗" -ForegroundColor Red
    Write-Host $_.Exception.Message -ForegroundColor Red
    exit 1
}

# 驗證 Maven
Write-Host ""
Write-Host "📦 步驟 2/3: 驗證 Maven..." -ForegroundColor Cyan
try {
    $mavenVersion = mvn --version | Select-Object -First 1
    Write-Host "✅ Maven 版本: $mavenVersion" -ForegroundColor Green
} catch {
    Write-Host "❌ Maven 未安裝或未設定環境變數" -ForegroundColor Red
    exit 1
}

# 建置專案
Write-Host ""
Write-Host "📦 步驟 3/3: 建置專案..." -ForegroundColor Cyan

# $PSScriptRoot = .vscode\scripts, 需要往上兩層到專案根目錄
$workspaceDir = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
Push-Location $workspaceDir

$mavenArgs = @()
if ($CleanBuild) {
    $mavenArgs += "clean"
}
$mavenArgs += "package"
if ($SkipTests) {
    # 使用 maven.test.skip=true 來完全跳過測試編譯和執行
    $mavenArgs += "-Dmaven.test.skip=true"
}
$mavenArgs += "-q"

Write-Host "ℹ️  建置指令: mvn $($mavenArgs -join ' ')" -ForegroundColor Blue
Write-Host "ℹ️  工作目錄: $workspaceDir" -ForegroundColor Blue
Write-Host ""

try {
    $buildStartTime = Get-Date

    & mvn $mavenArgs

    if ($LASTEXITCODE -ne 0) {
        throw "Maven 建置失敗，退出碼: $LASTEXITCODE"
    }

    $buildEndTime = Get-Date
    $buildDuration = ($buildEndTime - $buildStartTime).TotalSeconds

    Write-Host ""
    Write-Host "✅ 建置成功！耗時：$([math]::Round($buildDuration, 2)) 秒" -ForegroundColor Green
    Write-Host ""
} catch {
    Write-Host ""
    Write-Host "❌ Maven 建置失敗" -ForegroundColor Red
    Write-Host $_.Exception.Message -ForegroundColor Red
    Pop-Location
    exit 1
} finally {
    Pop-Location
}

Write-Host "============================================================" -ForegroundColor Green
Write-Host "  建置完成" -ForegroundColor Green
Write-Host "============================================================" -ForegroundColor Green
Write-Host ""
