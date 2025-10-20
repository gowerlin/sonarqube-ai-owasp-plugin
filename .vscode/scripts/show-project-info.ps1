#!/usr/bin/env pwsh
#Requires -Version 7.0

<#
.SYNOPSIS
    顯示專案資訊
.DESCRIPTION
    從 pom.xml 讀取並顯示專案的基本資訊
.NOTES
    Author: SonarQube AI OWASP Plugin Team
    Version: 1.0.0
#>

$ErrorActionPreference = "Stop"

$WORKSPACE_DIR = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$POM_PATH = Join-Path $WORKSPACE_DIR "pom.xml"

Write-Host ""
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "  專案資訊" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host ""

# 檢查 pom.xml 是否存在
if (-not (Test-Path $POM_PATH)) {
    Write-Host "❌ 找不到 pom.xml 檔案" -ForegroundColor Red
    Write-Host "   預期位置: $POM_PATH" -ForegroundColor Yellow
    exit 1
}

# 讀取 pom.xml
try {
    [xml]$pom = Get-Content $POM_PATH
    $project = $pom.project

    # 基本資訊
    Write-Host "📦 專案名稱: " -NoNewline -ForegroundColor Yellow
    Write-Host $project.name -ForegroundColor White

    Write-Host "🔖 版本號: " -NoNewline -ForegroundColor Yellow
    Write-Host $project.version -ForegroundColor White

    Write-Host "🏢 組織: " -NoNewline -ForegroundColor Yellow
    Write-Host $project.groupId -ForegroundColor White

    Write-Host "🆔 Artifact ID: " -NoNewline -ForegroundColor Yellow
    Write-Host $project.artifactId -ForegroundColor White

    if ($project.description) {
        Write-Host "📝 描述: " -NoNewline -ForegroundColor Yellow
        Write-Host $project.description -ForegroundColor White
    }

    Write-Host ""

    # 模組列表
    Write-Host "📚 模組列表:" -ForegroundColor Yellow
    $modules = Get-ChildItem -Path $WORKSPACE_DIR -Directory | Where-Object {
        Test-Path (Join-Path $_.FullName 'pom.xml')
    }

    if ($modules) {
        foreach ($module in $modules) {
            $modulePom = Join-Path $module.FullName 'pom.xml'
            try {
                [xml]$modulePomXml = Get-Content $modulePom
                $moduleName = $modulePomXml.project.artifactId
                Write-Host "   - $moduleName" -ForegroundColor Cyan
            } catch {
                Write-Host "   - $($module.Name)" -ForegroundColor Gray
            }
        }
    } else {
        Write-Host "   (無子模組)" -ForegroundColor Gray
    }

    Write-Host ""

    # 屬性資訊
    if ($project.properties) {
        Write-Host "🔧 重要屬性:" -ForegroundColor Yellow

        $properties = $project.properties
        $importantProps = @(
            @{ Name = "java.version"; Label = "Java 版本" },
            @{ Name = "maven.compiler.source"; Label = "編譯源版本" },
            @{ Name = "maven.compiler.target"; Label = "編譯目標版本" },
            @{ Name = "project.build.sourceEncoding"; Label = "編碼" },
            @{ Name = "sonar.api.version"; Label = "SonarQube API" }
        )

        foreach ($prop in $importantProps) {
            $value = $properties.($prop.Name)
            if ($value) {
                Write-Host "   $($prop.Label): $value" -ForegroundColor Gray
            }
        }
    }

} catch {
    Write-Host "❌ 讀取 pom.xml 失敗: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "============================================================" -ForegroundColor Green
Write-Host ""
