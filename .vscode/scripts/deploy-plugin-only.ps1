#!/usr/bin/env pwsh
#Requires -Version 7.0

<#
.SYNOPSIS
    僅部署插件（不建置）
.DESCRIPTION
    複製已建置的插件 JAR 檔案到 SonarQube 插件目錄
.NOTES
    Author: SonarQube AI OWASP Plugin Team
    Version: 1.0.0
#>

$ErrorActionPreference = "Stop"

$WORKSPACE_DIR = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$PLUGIN_TARGET = Join-Path $WORKSPACE_DIR "plugin-core\target"
$SONARQUBE_PLUGINS = "E:\sonarqube-community-25.10.0.114319\extensions\plugins"

Write-Host ""
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "  部署插件到 SonarQube" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host ""

# 搜尋插件 JAR 檔案
Write-Host "🔍 搜尋插件檔案..." -ForegroundColor Yellow
$pluginJar = Get-ChildItem -Path $PLUGIN_TARGET -Filter "sonar-aiowasp-plugin-*.jar" -ErrorAction SilentlyContinue |
    Sort-Object LastWriteTime -Descending |
    Select-Object -First 1

if (-not $pluginJar) {
    Write-Host "❌ 找不到插件 JAR 檔案" -ForegroundColor Red
    Write-Host "   搜尋路徑: $PLUGIN_TARGET" -ForegroundColor Yellow
    Write-Host "   請先執行建置任務：'🚀 Build & Deploy to SonarQube'" -ForegroundColor Yellow
    exit 1
}

Write-Host "✅ 找到插件: $($pluginJar.Name)" -ForegroundColor Green
Write-Host "   檔案大小: $(($pluginJar.Length / 1MB).ToString('F2')) MB" -ForegroundColor Gray
Write-Host "   建置時間: $($pluginJar.LastWriteTime)" -ForegroundColor Gray
Write-Host ""

# 確保目標目錄存在
if (-not (Test-Path $SONARQUBE_PLUGINS)) {
    Write-Host "📁 創建插件目錄..." -ForegroundColor Cyan
    New-Item -ItemType Directory -Path $SONARQUBE_PLUGINS -Force | Out-Null
}

# 複製插件
Write-Host "📥 複製插件至 SonarQube..." -ForegroundColor Cyan
try {
    Copy-Item -Path $pluginJar.FullName -Destination $SONARQUBE_PLUGINS -Force
    Write-Host "✅ 部署完成！" -ForegroundColor Green
    Write-Host "   部署位置: $SONARQUBE_PLUGINS\$($pluginJar.Name)" -ForegroundColor Cyan
} catch {
    Write-Host "❌ 部署失敗: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "⚠️  下一步：重啟 SonarQube 以載入插件" -ForegroundColor Yellow
Write-Host "   執行 VSCode Task: '🔄 Restart SonarQube'" -ForegroundColor Cyan
Write-Host ""
Write-Host "============================================================" -ForegroundColor Green
Write-Host ""
