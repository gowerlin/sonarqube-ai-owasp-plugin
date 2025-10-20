#!/usr/bin/env pwsh
#Requires -Version 7.0

$ErrorActionPreference = "Stop"

# 設定 Java 17 環境
$env:JAVA_HOME = "C:\Program Files\Java\jdk-17"
$env:PATH = "C:\Program Files\Java\jdk-17\bin;" + $env:PATH

Write-Host "Java version:"
java -version

Write-Host "`nStarting Maven test..."
Set-Location "D:\ForgejoGit\Security_Plugin_for_SonarQube"

mvn clean test -q
