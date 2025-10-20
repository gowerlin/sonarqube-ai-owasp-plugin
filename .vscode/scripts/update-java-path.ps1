#!/usr/bin/env pwsh
#Requires -Version 7.0

# 更新 Java 環境變數至 Java 17

Write-Host "開始更新 Java 環境變數..." -ForegroundColor Cyan

# 設定 JAVA_HOME
$javaHome = "C:\Program Files\Java\jdk-17"
[Environment]::SetEnvironmentVariable('JAVA_HOME', $javaHome, 'Machine')
Write-Host "✅ JAVA_HOME 已設定為: $javaHome" -ForegroundColor Green

# 更新 PATH
$oldPath = [Environment]::GetEnvironmentVariable('PATH', 'Machine')
$java17BinPath = "C:\Program Files\Java\jdk-17\bin"

# 移除舊的 Java 11 路徑（如果存在）
$pathArray = $oldPath -split ';'
$cleanedPath = $pathArray | Where-Object { $_ -notlike "*Java\jdk-11*" -and $_ -notlike "*Java\microsoft_jdk*" }

# 檢查 Java 17 是否已在 PATH 中
if ($cleanedPath -notcontains $java17BinPath) {
    # 將 Java 17 加到最前面
    $newPath = @($java17BinPath) + $cleanedPath
    $finalPath = $newPath -join ';'
    [Environment]::SetEnvironmentVariable('PATH', $finalPath, 'Machine')
    Write-Host "✅ Java 17 bin 目錄已加入 PATH（最優先）" -ForegroundColor Green
} else {
    Write-Host "✅ Java 17 bin 目錄已在 PATH 中" -ForegroundColor Green
}

Write-Host ""
Write-Host "環境變數更新完成！" -ForegroundColor Green
Write-Host "⚠️  請重啟終端或 VSCode 以套用新的環境變數" -ForegroundColor Yellow
Write-Host ""
Write-Host "驗證命令:" -ForegroundColor Cyan
Write-Host '  java -version' -ForegroundColor White
Write-Host ""
