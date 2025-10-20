#!/usr/bin/env pwsh
#Requires -Version 7.0

<#
.SYNOPSIS
    快速建置並部署 SonarQube AI OWASP Plugin

.DESCRIPTION
    此腳本執行以下步驟：
    1. 使用本機 Maven 建置插件
    2. 複製插件 JAR 至 SonarQube 插件目錄
    3. 顯示部署結果和下一步操作建議

.PARAMETER SkipTests
    跳過單元測試（加快建置速度）

.PARAMETER CleanBuild
    執行 clean build（移除舊的建構產物）

.PARAMETER Deploy
    是否自動部署至 SonarQube

.EXAMPLE
    .\build-and-deploy.ps1
    完整建置並部署

.EXAMPLE
    .\build-and-deploy.ps1 -SkipTests
    跳過測試，快速建置並部署

.EXAMPLE
    .\build-and-deploy.ps1 -CleanBuild
    執行 clean build 後部署
#>

param(
    [switch]$SkipTests = $true,
    [switch]$CleanBuild = $true,
    [switch]$Deploy = $true
)

# ============================================================
# 配置參數
# ============================================================
$ErrorActionPreference = "Stop"
$SONARQUBE_PLUGINS_DIR = "E:\sonarqube-community-25.10.0.114319\extensions\plugins"
$PLUGIN_JAR_PATTERN = "sonar-aiowasp-plugin-*.jar"
$WORKSPACE_DIR = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$PLUGIN_TARGET_DIR = Join-Path $WORKSPACE_DIR "plugin-core\target"

# ============================================================
# 輔助函數
# ============================================================
function Write-Header {
    param([string]$Message)
    Write-Host ""
    Write-Host "============================================================" -ForegroundColor Cyan
    Write-Host "  $Message" -ForegroundColor Cyan
    Write-Host "============================================================" -ForegroundColor Cyan
    Write-Host ""
}

function Write-Success {
    param([string]$Message)
    Write-Host "✅ $Message" -ForegroundColor Green
}

function Write-Info {
    param([string]$Message)
    Write-Host "ℹ️  $Message" -ForegroundColor Blue
}

function Write-Warning {
    param([string]$Message)
    Write-Host "⚠️  $Message" -ForegroundColor Yellow
}

function Write-Error {
    param([string]$Message)
    Write-Host "❌ $Message" -ForegroundColor Red
}

function Write-Step {
    param([string]$Message)
    Write-Host "📦 $Message" -ForegroundColor Cyan
}

# ============================================================
# 主程序
# ============================================================

Write-Header "SonarQube AI OWASP Plugin - 快速建置與部署"

# 強制使用 Java 17（專案需求）
$env:JAVA_HOME = "C:\Program Files\Java\jdk-17"
$env:PATH = "C:\Program Files\Java\jdk-17\bin;" + $env:PATH

# 步驟 1: 檢查環境
Write-Step "步驟 1/4: 檢查環境..."

# 檢查 Maven
try {
    $mavenVersion = mvn --version | Select-Object -First 1
    Write-Success "Maven 已安裝: $mavenVersion"
} catch {
    Write-Error "Maven 未安裝或未設定環境變數，請先安裝 Maven 3.9+"
    Write-Info "安裝指引：https://maven.apache.org/install.html"
    exit 1
}

# 檢查 Java（專案需要 Java 17）
try {
    $javaVersion = java -version 2>&1 | Select-Object -First 1
    Write-Success "Java 版本: $javaVersion"

    if ($javaVersion -notlike "*17.*") {
        Write-Warning "專案需要 Java 17，當前使用: $javaVersion"
        Write-Info "已強制設定為使用: C:\Program Files\Java\jdk-17"
    }
} catch {
    Write-Error "Java 未安裝或未設定環境變數，請先安裝 JDK 17"
    Write-Info "下載位置：https://www.oracle.com/java/technologies/downloads/#java17"
    exit 1
}

# 檢查 SonarQube 插件目錄
if (-not (Test-Path $SONARQUBE_PLUGINS_DIR)) {
    Write-Warning "SonarQube 插件目錄不存在，將建立目錄"
    New-Item -ItemType Directory -Path $SONARQUBE_PLUGINS_DIR -Force | Out-Null
    Write-Success "已建立插件目錄: $SONARQUBE_PLUGINS_DIR"
} else {
    Write-Success "SonarQube 插件目錄存在: $SONARQUBE_PLUGINS_DIR"
}

# 步驟 2: Maven 建置
Write-Step "步驟 2/4: Maven 建置插件..."

$mavenArgs = @()
if ($CleanBuild) {
    $mavenArgs += "clean"
}
$mavenArgs += "package"
if ($SkipTests) {
    # 使用 maven.test.skip=true 來完全跳過測試編譯和執行
    $mavenArgs += "-Dmaven.test.skip=true"
}
$mavenArgs += "-q"  # Quiet mode

Write-Info "建置指令: mvn $($mavenArgs -join ' ')"
Write-Info "工作目錄: $WORKSPACE_DIR"
Write-Info "Java 版本: Java 17"
Write-Info "這可能需要幾分鐘時間（首次建置會下載依賴）..."

try {
    $buildStartTime = Get-Date

    # 使用本機 Maven 建置
    Push-Location $WORKSPACE_DIR
    & mvn $mavenArgs
    Pop-Location

    if ($LASTEXITCODE -ne 0) {
        throw "Maven 建置失敗，退出碼: $LASTEXITCODE"
    }

    $buildEndTime = Get-Date
    $buildDuration = ($buildEndTime - $buildStartTime).TotalSeconds

    Write-Success "建置完成！耗時：$([math]::Round($buildDuration, 2)) 秒"
} catch {
    Write-Error "Maven 建置失敗"
    Write-Host $_.Exception.Message -ForegroundColor Red
    Pop-Location
    exit 1
}

# 步驟 3: 查找插件 JAR 檔案
Write-Step "步驟 3/4: 查找插件 JAR 檔案..."

$pluginJar = Get-ChildItem -Path $PLUGIN_TARGET_DIR -Filter $PLUGIN_JAR_PATTERN -ErrorAction SilentlyContinue |
             Select-Object -First 1

if (-not $pluginJar) {
    Write-Error "找不到插件 JAR 檔案"
    Write-Host "  搜尋路徑: $PLUGIN_TARGET_DIR" -ForegroundColor Yellow
    Write-Host "  搜尋模式: $PLUGIN_JAR_PATTERN" -ForegroundColor Yellow
    exit 1
}

Write-Success "找到插件 JAR: $($pluginJar.Name)"
Write-Info "檔案大小: $([math]::Round($pluginJar.Length / 1MB, 2)) MB"
Write-Info "建置時間: $($pluginJar.LastWriteTime)"

# 步驟 4: 部署至 SonarQube
if ($Deploy) {
    Write-Step "步驟 4/4: 部署至 SonarQube..."

    try {
        # 備份舊版本（如果存在）
        $existingPlugin = Get-ChildItem -Path $SONARQUBE_PLUGINS_DIR -Filter $PLUGIN_JAR_PATTERN -ErrorAction SilentlyContinue
        if ($existingPlugin) {
            $backupPath = Join-Path $SONARQUBE_PLUGINS_DIR "backup"
            if (-not (Test-Path $backupPath)) {
                New-Item -ItemType Directory -Path $backupPath -Force | Out-Null
            }
            $backupFile = Join-Path $backupPath "$($existingPlugin.Name).$(Get-Date -Format 'yyyyMMdd-HHmmss').bak"
            Move-Item -Path $existingPlugin.FullName -Destination $backupFile -Force
            Write-Info "已備份舊版本至: $backupFile"
        }

        # 複製新插件
        Copy-Item -Path $pluginJar.FullName -Destination $SONARQUBE_PLUGINS_DIR -Force
        Write-Success "插件已部署至: $SONARQUBE_PLUGINS_DIR"

        # 驗證部署
        $deployedPlugin = Get-ChildItem -Path $SONARQUBE_PLUGINS_DIR -Filter $PLUGIN_JAR_PATTERN | Select-Object -First 1
        if ($deployedPlugin) {
            Write-Success "部署驗證成功"
            Write-Info "部署檔案: $($deployedPlugin.FullName)"
        }

    } catch {
        Write-Error "部署失敗"
        Write-Host $_.Exception.Message -ForegroundColor Red
        exit 1
    }
} else {
    Write-Warning "跳過部署步驟（Deploy = false）"
}

# ============================================================
# 完成訊息
# ============================================================
Write-Header "部署完成"

Write-Success "SonarQube AI OWASP Plugin 已成功建置並部署！"
Write-Host ""

Write-Host "📋 下一步操作：" -ForegroundColor Yellow
Write-Host ""
Write-Host "  1. 重啟本機 SonarQube 以載入新插件" -ForegroundColor White
Write-Host "     執行 VSCode Task: '🔄 Restart SonarQube'" -ForegroundColor Cyan
Write-Host "     或手動執行: E:\sonarqube-community-25.10.0.114319\bin\windows-x86-64\StartSonar.bat" -ForegroundColor Gray
Write-Host ""
Write-Host "  2. 訪問 SonarQube: http://localhost:9000" -ForegroundColor White
Write-Host "     預設帳號：admin / admin" -ForegroundColor Gray
Write-Host ""
Write-Host "  3. 驗證插件載入" -ForegroundColor White
Write-Host "     前往：Administration → Marketplace → Installed" -ForegroundColor Gray
Write-Host "     查找：AI OWASP Security" -ForegroundColor Gray
Write-Host ""
Write-Host "  4. 配置 AI API 金鑰" -ForegroundColor White
Write-Host "     前往：Administration → Configuration → AI Configuration" -ForegroundColor Gray
Write-Host ""

Write-Host "============================================================" -ForegroundColor Green
Write-Host "  建置與部署腳本執行完成" -ForegroundColor Green
Write-Host "============================================================" -ForegroundColor Green
Write-Host ""

# 詢問是否重啟 SonarQube（僅在互動模式下）
if ($Host.UI.RawUI) {
    $response = Read-Host "是否立即重啟本機 SonarQube？(y/N)"
    if ($response -eq 'y' -or $response -eq 'Y') {
        Write-Info "停止 SonarQube..."
        $sonarProc = Get-Process | Where-Object { $_.ProcessName -eq 'java' -and $_.CommandLine -like '*sonar*' }
        if ($sonarProc) {
            $sonarProc | Stop-Process -Force
            Write-Info "已停止 SonarQube 進程"
            Start-Sleep -Seconds 3
        }

        Write-Info "啟動 SonarQube..."
        $sonarPath = 'E:/sonarqube-community-25.10.0.114319/bin/windows-x86-64/StartSonar.bat'
        if (Test-Path $sonarPath) {
            Start-Process -FilePath $sonarPath -WorkingDirectory 'E:/sonarqube-community-25.10.0.114319/bin/windows-x86-64'
            Write-Success "SonarQube 正在啟動，請稍候 30-60 秒後訪問 http://localhost:9000"
        } else {
            Write-Error "找不到 SonarQube 啟動腳本: $sonarPath"
        }
    }
}
