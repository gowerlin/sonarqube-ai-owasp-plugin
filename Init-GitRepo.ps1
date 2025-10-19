# Init-FullGitRepo.ps1
# 完整自動化 Git 專案初始化 + Forgejo Repo 建立工具

# === 設定參數 ===
param(
    [switch]$SkipPull
)
$forgejoUser = "gower"
$sshHost     = "sxnas"
$forgejoHost = "http://sxnas:3000"
$apiBaseUrl  = "$forgejoHost/api/v1"
$forgejoToken = $env:PowerShellDeployToken
$backupGitIgnorePath = "C:\Git\_templates\global-gitignore"

$repoName = Split-Path -Leaf (Get-Location)
# 將 repo 名轉為 URL slug（移除非 ASCII）
$repoName = ($repoName -replace "[^a-zA-Z0-9\-_.]", "-").Trim('-')
$remoteUrl = "${forgejoHost}/${forgejoUser}/${repoName}.git"
$repoApiUrl = "$apiBaseUrl/repos/$forgejoUser/$repoName"

Write-Host "`n🚀 [Forgejo Git 初始化工具]" -ForegroundColor Magenta
Write-Host "📂 專案目錄名稱: $repoName"
Write-Host "🔗 遠端 Git 位置: $remoteUrl"

# === 驗證 token ===
if (-not $forgejoToken) {
    Write-Host "❌ 未設定環境變數 PowerShellDeployToken" -ForegroundColor Red
    exit 1
} else {
    Write-Host "🔐 Token 已讀取 (長度 $($forgejoToken.Length))"
}

# === 驗證 .ssh/config ===
$sshConfigPath = "$env:USERPROFILE\.ssh\config"
if (-not (Test-Path $sshConfigPath)) {
    Write-Host "⚠️ 找不到 .ssh/config" -ForegroundColor Yellow
} else {
    $sshConfigContent = Get-Content $sshConfigPath
    if ($sshConfigContent -match "Host\s+$sshHost") {
        Write-Host "✅ 發現 SSH Host '$sshHost' 配置"
    } else {
        Write-Host "⚠️ 未發現 Host '$sshHost' 設定於 .ssh/config" -ForegroundColor Yellow
    }
}

# === git remote ===
Write-Host "🔎 現有 git remote 設定:"
try {
    git remote -v
} catch {
    Write-Host "⚠️ 未設定 git remote"
}

# === Forgejo repo 存在性 ===
$headers = @{ Authorization = "token $forgejoToken" }
try {
    Invoke-RestMethod -Uri $repoApiUrl -Headers $headers -Method Get -ErrorAction Stop | Out-Null
    Write-Host "✅ 遠端 repo 已存在"
} catch {
    Write-Host "➕ 啟動自動創建 repo..."
    $body = @{ name = $repoName; private = $true; auto_init = $false } | ConvertTo-Json -Depth 10
    try {
        Invoke-RestMethod -Uri "$apiBaseUrl/user/repos" -Headers $headers -Method Post -Body $body -ContentType "application/json"
        Write-Host "✅ 遠端 repo 創建成功"
    } catch {
        Write-Host "❌ API 創建失敗: $($_.Exception.Message)" -ForegroundColor Red
        if ($_.Exception.Response -ne $null) {
            $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
            $responseBody = $reader.ReadToEnd()
            Write-Host "💬 回應內容：" -ForegroundColor Yellow
            Write-Host $responseBody
        }
        exit 1
    }
}

# === Git 初始化 ===
if (-not (Test-Path ".git")) {
    git init -b main | Out-Null
    Write-Host "✅ 初始化 git repo（分支：main）"
}

# === .NET 變數當設定 ===
# === 自動產生 .gitattributes (統一換行符號為 LF) ===
if (-not (Test-Path ".gitattributes")) {
    @" 
* text=auto eol=lf
*.zip binary
*.exe binary
*.dll binary
*.pdf binary
*.png binary
*.jpg binary
*.jpeg binary
*.mp4 binary
*.mp3 binary
*.psd binary
*.xlsx binary
*.docx binary
*.pptx binary
"@ | Out-File .gitattributes -Encoding UTF8
    git add .gitattributes
    Write-Host "📄 產生 .gitattributes，統一使用 LF 換行格式"
}
$hasDotNet = Get-ChildItem -Recurse -Include *.csproj,*.sln,*.vbproj,*.fsproj | Where-Object { -not $_.PSIsContainer }
if (-not (Test-Path ".gitignore")) {
    $content = @"
# 自動產生 .gitignore
.vs/
.cr/
.sonar/
.sonarqube/
.sonarlint/
.scannerwork/
packages/
Thumbs.db
node_modules/
dist/
out/
.env
.DS_Store
*.log
*.bak
*.tmp
npm-debug.log
"@
    if ($hasDotNet) {
        $content += @"
bin/
obj/
*.user
*.suo
"@
    }
    $content | Out-File .gitignore -Encoding UTF8
    git add .gitignore
    Write-Host "📄 產生 .gitignore 含 .NET/CI/Sonar 忽略"
    if (-not (Test-Path $backupGitIgnorePath)) {
        New-Item -ItemType Directory -Path $backupGitIgnorePath | Out-Null
    }
    Copy-Item .gitignore -Destination "$backupGitIgnorePath\$repoName.gitignore" -Force
    Write-Host "📃 備份 .gitignore 至 $backupGitIgnorePath"

    # 強制排除已追蹤的忽略項目
    $excluded = @("node_modules", "packages", "Thumbs.db", "dist", "out", "bin", "obj", ".env", ".DS_Store", "*.log", "*.bak", "*.tmp", "npm-debug.log", "BiteGarden Report", "Sonar Report")
    foreach ($path in $excluded) {
        if (Test-Path $path) {
            $isTracked = git ls-files $path 2>$null
            if ($isTracked) {
                git rm -r --cached $path -q
                Write-Host "🧹 強制從 Git Index 移除已追蹤但應忽略項目: $path" -ForegroundColor Yellow
            } else {
                Write-Host "✅ $path 已正確排除"
            }
        }
    }
}

# === Git Commit / Remote ===
# 確保至少有一個檔案能 commit
if (-not (git rev-parse --quiet --verify HEAD)) {
    git add .
    Write-Host "📥 已自動加入現有檔案追蹤"
    $staged = git diff --cached --name-only
    if (-not $staged) {
        Write-Host "⚠️ 沒有任何檔案進入 commit 階段，請檢查 .gitignore 或目錄內容" -ForegroundColor Yellow
    } else {
        git commit -m "Initial commit" | Out-Null
        Write-Host "📆 Commit Initial"
    }
}
if (-not (git remote get-url origin 2>$null)) {
    git remote add origin $remoteUrl
    Write-Host "🔗 設定 remote origin"
}

# === Git Pull + Push ===
if (-not $SkipPull) {
    try {
        git pull origin main --allow-unrelated-histories
        Write-Host "↺ 拉取 remote main 分支"
    } catch {
        Write-Host "⚠️ 遠端無 main 分支，改用本地 main 分支初始化上傳" -ForegroundColor Yellow
    }
} else {
    Write-Host "⏩ 已跳過 git pull 步驟 (使用 -SkipPull)" -ForegroundColor Gray
}
$hasCommit = git rev-parse --quiet --verify HEAD
if ($hasCommit) {
    git push -u origin main
    Write-Host "🚀 push 完成！ http://$forgejoHost/$forgejoUser/$repoName"
} else {
    Write-Host "⚠️ 尚未建立有效的 commit，已略過 push" -ForegroundColor Yellow
}

