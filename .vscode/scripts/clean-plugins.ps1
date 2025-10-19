#!/usr/bin/env pwsh
#Requires -Version 7.0

<#
.SYNOPSIS
    清理 SonarQube 插件目錄中的舊版本插件

.DESCRIPTION
    此腳本會：
    1. 掃描 SonarQube 插件目錄
    2. 找出所有 AI OWASP Plugin 相關檔案
    3. 可選擇保留最新版本或全部刪除
    4. 備份檔案至 backup 目錄

.PARAMETER RemoveAll
    移除所有版本（包含最新版）

.PARAMETER KeepLatest
    只保留最新版本，移除其他舊版本（預設）

.EXAMPLE
    .\clean-plugins.ps1
    保留最新版本，移除舊版本

.EXAMPLE
    .\clean-plugins.ps1 -RemoveAll
    移除所有版本
#>

param(
    [switch]$RemoveAll = $false,
    [switch]$KeepLatest = $true
)

# ============================================================
# 配置參數
# ============================================================
$ErrorActionPreference = "Stop"
$SONARQUBE_PLUGINS_DIR = "E:\sonarqube-community-25.10.0.114319\extensions\plugins"
$PLUGIN_JAR_PATTERN = "sonar-aiowasp-plugin-*.jar"
$BACKUP_DIR = Join-Path $SONARQUBE_PLUGINS_DIR "backup"

# ============================================================
# 主程序
# ============================================================

Write-Host ""
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "  清理 SonarQube AI OWASP Plugin" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host ""

# 檢查目錄
if (-not (Test-Path $SONARQUBE_PLUGINS_DIR)) {
    Write-Host "❌ SonarQube 插件目錄不存在: $SONARQUBE_PLUGINS_DIR" -ForegroundColor Red
    exit 1
}

# 查找插件
$plugins = Get-ChildItem -Path $SONARQUBE_PLUGINS_DIR -Filter $PLUGIN_JAR_PATTERN -ErrorAction SilentlyContinue

if ($plugins.Count -eq 0) {
    Write-Host "ℹ️  沒有找到 AI OWASP Plugin" -ForegroundColor Yellow
    Write-Host "   搜尋路徑: $SONARQUBE_PLUGINS_DIR" -ForegroundColor Gray
    exit 0
}

Write-Host "📦 找到 $($plugins.Count) 個插件檔案：" -ForegroundColor Cyan
Write-Host ""
$plugins | ForEach-Object {
    $size = [math]::Round($_.Length / 1MB, 2)
    $time = $_.LastWriteTime.ToString("yyyy-MM-dd HH:mm:ss")
    Write-Host "  - $($_.Name)" -ForegroundColor White
    Write-Host "    大小：$size MB | 修改時間：$time" -ForegroundColor Gray
}
Write-Host ""

# 建立備份目錄
if (-not (Test-Path $BACKUP_DIR)) {
    New-Item -ItemType Directory -Path $BACKUP_DIR -Force | Out-Null
    Write-Host "✅ 已建立備份目錄: $BACKUP_DIR" -ForegroundColor Green
}

# 決定要移除的檔案
if ($RemoveAll) {
    $filesToRemove = $plugins
    Write-Host "⚠️  模式：移除所有版本" -ForegroundColor Yellow
} elseif ($KeepLatest) {
    # 按修改時間排序，保留最新的
    $sortedPlugins = $plugins | Sort-Object LastWriteTime -Descending
    $latestPlugin = $sortedPlugins | Select-Object -First 1
    $filesToRemove = $sortedPlugins | Select-Object -Skip 1

    Write-Host "✅ 保留最新版本：$($latestPlugin.Name)" -ForegroundColor Green
    Write-Host "   修改時間：$($latestPlugin.LastWriteTime)" -ForegroundColor Gray
    Write-Host ""

    if ($filesToRemove.Count -eq 0) {
        Write-Host "ℹ️  沒有需要移除的舊版本" -ForegroundColor Yellow
        exit 0
    }
}

# 確認移除
Write-Host "🗑️  即將移除以下檔案：" -ForegroundColor Yellow
$filesToRemove | ForEach-Object {
    Write-Host "  - $($_.Name)" -ForegroundColor Red
}
Write-Host ""

if ($Host.UI.RawUI) {
    $response = Read-Host "確認移除？(y/N)"
    if ($response -ne 'y' -and $response -ne 'Y') {
        Write-Host "❌ 操作已取消" -ForegroundColor Yellow
        exit 0
    }
}

# 執行備份與移除
Write-Host ""
Write-Host "📦 執行備份與移除..." -ForegroundColor Cyan

$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$filesToRemove | ForEach-Object {
    try {
        # 備份
        $backupFile = Join-Path $BACKUP_DIR "$($_.Name).$timestamp.bak"
        Copy-Item -Path $_.FullName -Destination $backupFile -Force
        Write-Host "  ✅ 已備份: $($_.Name) → $backupFile" -ForegroundColor Green

        # 移除
        Remove-Item -Path $_.FullName -Force
        Write-Host "  🗑️  已移除: $($_.Name)" -ForegroundColor Yellow
    } catch {
        Write-Host "  ❌ 處理失敗: $($_.Name)" -ForegroundColor Red
        Write-Host "     錯誤：$($_.Exception.Message)" -ForegroundColor Red
    }
}

Write-Host ""
Write-Host "============================================================" -ForegroundColor Green
Write-Host "  清理完成" -ForegroundColor Green
Write-Host "============================================================" -ForegroundColor Green
Write-Host ""

# 顯示當前狀態
$remainingPlugins = Get-ChildItem -Path $SONARQUBE_PLUGINS_DIR -Filter $PLUGIN_JAR_PATTERN -ErrorAction SilentlyContinue
if ($remainingPlugins) {
    Write-Host "📦 當前插件目錄狀態：" -ForegroundColor Cyan
    $remainingPlugins | ForEach-Object {
        Write-Host "  - $($_.Name)" -ForegroundColor White
    }
} else {
    Write-Host "ℹ️  插件目錄已清空" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "💡 備份檔案位於: $BACKUP_DIR" -ForegroundColor Gray
Write-Host ""
