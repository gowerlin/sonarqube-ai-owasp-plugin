#!/usr/bin/env pwsh
#Requires -Version 7.0

<#
.SYNOPSIS
    執行 Maven 任務的通用腳本
.DESCRIPTION
    根據傳入參數執行不同的 Maven 命令
.PARAMETER TaskName
    任務名稱（用於顯示）
.PARAMETER MavenArgs
    Maven 命令參數
.PARAMETER Quiet
    是否使用安靜模式 (-q)
.NOTES
    Author: SonarQube AI OWASP Plugin Team
    Version: 1.0.0
.EXAMPLE
    .\run-maven-task.ps1 -TaskName "快速建置" -MavenArgs "clean package -DskipTests" -Quiet
#>

param(
    [Parameter(Mandatory = $true)]
    [string]$TaskName,

    [Parameter(Mandatory = $true)]
    [string]$MavenArgs,

    [switch]$Quiet
)

$ErrorActionPreference = "Stop"

Write-Host "📦 $TaskName..." -ForegroundColor Cyan

# 構建完整的 Maven 命令
$mavenCmd = "mvn $MavenArgs"
if ($Quiet) {
    $mavenCmd += " -q"
}

# 執行 Maven 命令
try {
    Invoke-Expression $mavenCmd

    if ($LASTEXITCODE -eq 0) {
        Write-Host "✅ $TaskName 成功" -ForegroundColor Green
        exit 0
    } else {
        Write-Host "❌ $TaskName 失敗 (退出碼: $LASTEXITCODE)" -ForegroundColor Red
        exit $LASTEXITCODE
    }
} catch {
    Write-Host "❌ 執行失敗: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}
