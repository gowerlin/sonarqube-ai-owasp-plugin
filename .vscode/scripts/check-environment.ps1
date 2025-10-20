#!/usr/bin/env pwsh
#Requires -Version 7.0

<#
.SYNOPSIS
    檢查開發環境配置
.DESCRIPTION
    檢查 Maven, Java, PowerShell, SonarQube 等開發工具的安裝狀態和版本
.NOTES
    Author: SonarQube AI OWASP Plugin Team
    Version: 1.0.0
#>

$ErrorActionPreference = "Continue"

$WORKSPACE_DIR = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$SONARQUBE_HOME = "E:\sonarqube-community-25.10.0.114319"
$PLUGIN_DIR = Join-Path $SONARQUBE_HOME "extensions\plugins"

Write-Host ""
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "  開發環境檢查" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host ""

# 檢查 Maven
Write-Host "📦 Maven 版本：" -ForegroundColor Yellow
try {
    mvn --version
    Write-Host ""
} catch {
    Write-Host "   ❌ 未安裝或無法執行" -ForegroundColor Red
    Write-Host ""
}

# 檢查 Java
Write-Host "☕ Java 版本：" -ForegroundColor Yellow
try {
    java -version
    Write-Host ""
} catch {
    Write-Host "   ❌ 未安裝或無法執行" -ForegroundColor Red
    Write-Host ""
}

# 檢查 PowerShell
Write-Host "💻 PowerShell 版本：" -ForegroundColor Yellow
Write-Host "   $($PSVersionTable.PSVersion)" -ForegroundColor Cyan
Write-Host ""

# 檢查 SonarQube 安裝
Write-Host "🏠 SonarQube 本機安裝：" -ForegroundColor Yellow
if (Test-Path $SONARQUBE_HOME) {
    Write-Host "   安裝目錄: $SONARQUBE_HOME" -ForegroundColor Green

    # 嘗試取得版本
    $versionJar = Get-ChildItem -Path (Join-Path $SONARQUBE_HOME "lib") -Filter "sonar-application-*.jar" -ErrorAction SilentlyContinue |
        Select-Object -First 1

    if ($versionJar) {
        $version = $versionJar.BaseName -replace 'sonar-application-', ''
        Write-Host "   版本: $version" -ForegroundColor Cyan
    }

    # 檢查 SonarQube 是否運行
    $sonarProcess = Get-Process | Where-Object {
        $_.ProcessName -eq 'java' -and
        $_.CommandLine -like '*sonar*'
    } -ErrorAction SilentlyContinue

    if ($sonarProcess) {
        Write-Host "   狀態: ✅ 運行中 (PID: $($sonarProcess.Id))" -ForegroundColor Green
    } else {
        Write-Host "   狀態: ⚠️  未運行" -ForegroundColor Yellow
    }
} else {
    Write-Host "   ❌ 未找到 SonarQube 安裝" -ForegroundColor Red
    Write-Host "   預期位置: $SONARQUBE_HOME" -ForegroundColor Gray
}
Write-Host ""

# 檢查專案路徑
Write-Host "📂 專案路徑：" -ForegroundColor Yellow
Write-Host "   $WORKSPACE_DIR" -ForegroundColor Cyan
Write-Host ""

# 檢查插件目錄
Write-Host "🔌 SonarQube 插件目錄：" -ForegroundColor Yellow
if (Test-Path $PLUGIN_DIR) {
    Write-Host "   $PLUGIN_DIR" -ForegroundColor Green

    $plugins = Get-ChildItem -Path $PLUGIN_DIR -Filter "sonar-aiowasp-plugin-*.jar" -ErrorAction SilentlyContinue
    if ($plugins) {
        Write-Host "   已安裝插件:" -ForegroundColor Cyan
        foreach ($plugin in $plugins) {
            $sizeKB = ($plugin.Length / 1KB).ToString("F2")
            Write-Host "     - $($plugin.Name) ($sizeKB KB)" -ForegroundColor Gray
        }
    } else {
        Write-Host "   ⚠️  尚未部署插件" -ForegroundColor Yellow
    }
} else {
    Write-Host "   ❌ 目錄不存在: $PLUGIN_DIR" -ForegroundColor Red
}
Write-Host ""

Write-Host "============================================================" -ForegroundColor Green
Write-Host "  ✅ 環境檢查完成" -ForegroundColor Green
Write-Host "============================================================" -ForegroundColor Green
Write-Host ""
