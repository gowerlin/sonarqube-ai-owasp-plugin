#!/usr/bin/env pwsh
#Requires -Version 7.0

<#
.SYNOPSIS
    修復測試程式碼中的 AiRequest.builder() API 不匹配問題
.DESCRIPTION
    將 AiRequest.builder().code(x) 改為 AiRequest.builder(x)
#>

$ErrorActionPreference = "Stop"

Write-Host "開始修復測試檔案中的 AiRequest.builder() API..." -ForegroundColor Cyan

# 測試檔案目錄
$testDir = "D:\ForgejoGit\Security_Plugin_for_SonarQube\ai-connector\src\test"

# 搜尋所有需要修復的 Java 測試檔案
$files = Get-ChildItem -Path $testDir -Filter "*.java" -Recurse | Where-Object {
    $content = Get-Content $_.FullName -Raw
    $content -match 'AiRequest\.builder\(\)\s+\.code\('
}

Write-Host "找到 $($files.Count) 個檔案需要修復" -ForegroundColor Yellow

foreach ($file in $files) {
    Write-Host "`n處理: $($file.Name)" -ForegroundColor Green

    $content = Get-Content $file.FullName -Raw

    # 正則表達式替換模式：
    # AiRequest.builder()
    #     .code("xxx")
    # 替換為:
    # AiRequest.builder("xxx")

    $pattern = '(AiRequest\.builder\(\))\s+\.code\(([^)]+)\)'
    $replacement = 'AiRequest.builder($2)'

    $newContent = $content -replace $pattern, $replacement

    if ($content -ne $newContent) {
        Set-Content -Path $file.FullName -Value $newContent -NoNewline
        Write-Host "  ✅ 已修復 $($file.Name)" -ForegroundColor Green
    } else {
        Write-Host "  ⚠️  無需變更 $($file.Name)" -ForegroundColor Yellow
    }
}

Write-Host "`n✅ 修復完成！" -ForegroundColor Green
