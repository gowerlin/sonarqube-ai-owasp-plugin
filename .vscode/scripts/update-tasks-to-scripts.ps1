#!/usr/bin/env pwsh
#Requires -Version 7.0

<#
.SYNOPSIS
    將 tasks.json 中的內嵌命令改為調用 .ps1 腳本
.DESCRIPTION
    批量更新 tasks.json，將所有內嵌的 PowerShell 命令改為調用獨立的 .ps1 腳本
#>

$ErrorActionPreference = "Stop"

$tasksJsonPath = Join-Path (Split-Path $PSScriptRoot -Parent) "tasks.json"
$content = Get-Content $tasksJsonPath -Raw

Write-Host "開始更新 tasks.json..." -ForegroundColor Cyan

# 定義替換規則
$replacements = @(
    @{
        Label = "🏗️ Full Build (With Tests)"
        Old = '"args": \[\s*"-Command",\s*"Write-Host ''📦 完整建置（含測試）\.\.\.'' -ForegroundColor Cyan; mvn clean package; if \(\$LASTEXITCODE -eq 0\) \{ Write-Host ''✅ 建置成功'' -ForegroundColor Green \} else \{ Write-Host ''❌ 建置失敗'' -ForegroundColor Red; exit \$LASTEXITCODE \}"\s*\]'
        New = '"args": [
                "-File",
                "${workspaceFolder}/.vscode/scripts/run-maven-task.ps1",
                "-TaskName",
                "完整建置（含測試）",
                "-MavenArgs",
                "clean package"
            ]'
    },
    @{
        Label = "⚡ Incremental Build"
        Old = '"args": \[\s*"-Command",\s*"Write-Host ''⚡ 增量建置（不清理）\.\.\.'' -ForegroundColor Cyan; mvn package -DskipTests -q; if \(\$LASTEXITCODE -eq 0\) \{ Write-Host ''✅ 建置成功'' -ForegroundColor Green \} else \{ Write-Host ''❌ 建置失敗'' -ForegroundColor Red; exit \$LASTEXITCODE \}"\s*\]'
        New = '"args": [
                "-File",
                "${workspaceFolder}/.vscode/scripts/run-maven-task.ps1",
                "-TaskName",
                "增量建置（不清理）",
                "-MavenArgs",
                "package -DskipTests",
                "-Quiet"
            ]'
    },
    @{
        Label = "📦 Deploy Plugin Only"
        Old = '"-Command",[^]]*"\$pluginJar[^]]*exit 1[^]]*"'
        New = '"-File",
                "${workspaceFolder}/.vscode/scripts/deploy-plugin-only.ps1"'
    }
)

# 執行替換
foreach ($rule in $replacements) {
    Write-Host "  更新: $($rule.Label)" -ForegroundColor Yellow
    $content = $content -replace $rule.Old, $rule.New
}

# 寫回檔案
Set-Content -Path $tasksJsonPath -Value $content -NoNewline

Write-Host "✅ 更新完成！" -ForegroundColor Green
