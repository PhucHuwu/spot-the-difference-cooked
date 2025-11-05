# Script to test login functionality
Write-Host "=== Testing Login Flow ===" -ForegroundColor Green

# Kill any existing Java processes
Write-Host "`nKilling existing Java processes..." -ForegroundColor Yellow
Get-Process -Name "java" -ErrorAction SilentlyContinue | Stop-Process -Force

Start-Sleep -Seconds 2

# Start server in background
Write-Host "`nStarting server..." -ForegroundColor Cyan
$serverJob = Start-Job -ScriptBlock {
    Set-Location "D:\spot-the-difference-cooked\server"
    mvn exec:java -Dexec.mainClass="com.ltm.game.server.ServerApp"
}

# Wait for server to start
Write-Host "Waiting for server to start..." -ForegroundColor Yellow
Start-Sleep -Seconds 5

# Start client
Write-Host "`nStarting client..." -ForegroundColor Cyan
Set-Location "D:\spot-the-difference-cooked\client"
mvn javafx:run

# Cleanup
Write-Host "`nStopping server..." -ForegroundColor Yellow
Stop-Job $serverJob
Remove-Job $serverJob
Get-Process -Name "java" -ErrorAction SilentlyContinue | Stop-Process -Force

Write-Host "`nTest complete!" -ForegroundColor Green


