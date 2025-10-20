#!/usr/bin/env pwsh
#Requires -Version 7.0

<#
.SYNOPSIS
    在瀏覽器中開啟 SonarQube
.DESCRIPTION
    開啟 SonarQube Web 介面 (http://localhost:9000)
.NOTES
    Author: SonarQube AI OWASP Plugin Team
    Version: 1.0.0
#>

$ErrorActionPreference = "Stop"

$SONARQUBE_URL = "http://localhost:9000"

Write-Host "🌐 開啟 SonarQube 網頁介面..." -ForegroundColor Cyan
Write-Host "   URL: $SONARQUBE_URL" -ForegroundColor Gray

try {
    Start-Process $SONARQUBE_URL
    Write-Host "✅ 已在瀏覽器中開啟" -ForegroundColor Green
} catch {
    Write-Host "❌ 無法開啟瀏覽器: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host "   請手動訪問: $SONARQUBE_URL" -ForegroundColor Yellow
    exit 1
}
