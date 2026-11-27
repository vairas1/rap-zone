# Rap Zone - Startup Script
# Run this in PowerShell as Administrator

Write-Host "===========================================" -ForegroundColor Cyan
Write-Host "    Rap Zone - Music Streaming Platform" -ForegroundColor Cyan
Write-Host "===========================================" -ForegroundColor Cyan
Write-Host ""

# Check prerequisites
Write-Host "Checking prerequisites..." -ForegroundColor Yellow

$errors = @()

# Check Node.js
try {
    $nodeVersion = node --version
    Write-Host "✓ Node.js: $nodeVersion" -ForegroundColor Green
} catch {
    $errors += "Node.js not found. Please install Node.js 18+"
}

# Check Java
try {
    $javaVersion = java -version 2>&1 | Select-Object -First 1
    Write-Host "✓ Java: $javaVersion" -ForegroundColor Green
} catch {
    $errors += "Java not found. Please install Java 17+"
}

# Check PostgreSQL (optional, we use SQLite by default)
try {
    $pgVersion = psql --version
    Write-Host "✓ PostgreSQL: $pgVersion" -ForegroundColor Green
} catch {
    Write-Host "⚠ PostgreSQL not found (using SQLite instead)" -ForegroundColor Yellow
}

if ($errors.Count -gt 0) {
    Write-Host "" -ForegroundColor Red
    Write-Host "ERRORS:" -ForegroundColor Red
    $errors | ForEach-Object { Write-Host "  - $_" -ForegroundColor Red }
    Write-Host ""
    exit 1
}

Write-Host ""
Write-Host "Starting services..." -ForegroundColor Yellow

# Start Backend
Write-Host ""
Write-Host "1. Starting Backend API (port 4000)..." -ForegroundColor Cyan
$backendPath = Join-Path $PSScriptRoot "server"
if (Test-Path $backendPath) {
    Set-Location $backendPath
    # Check if dist exists, if not build
    if (-not (Test-Path "dist")) {
        Write-Host "  Building TypeScript..." -ForegroundColor Yellow
        npm run build
    }
    Write-Host "  Starting server..." -ForegroundColor Yellow
    Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$backendPath'; node dist/main.js" -WindowStyle Normal
    Write-Host "  Backend started on http://localhost:4000" -ForegroundColor Green
} else {
    Write-Host "  Backend directory not found!" -ForegroundColor Red
}

# Wait for backend to start
Start-Sleep -Seconds 5

# Start Admin Panel
Write-Host ""
Write-Host "2. Starting Admin Panel (port 8080)..." -ForegroundColor Cyan
$adminPath = Join-Path $PSScriptRoot "admin"
if (Test-Path $adminPath) {
    Set-Location $adminPath
    # Check node_modules
    if (-not (Test-Path "node_modules")) {
        Write-Host "  Installing dependencies..." -ForegroundColor Yellow
        npm install
    }
    Write-Host "  Starting dev server..." -ForegroundColor Yellow
    Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$adminPath'; npm run dev" -WindowStyle Normal
    Write-Host "  Admin panel started on http://localhost:8080" -ForegroundColor Green
} else {
    Write-Host "  Admin directory not found!" -ForegroundColor Red
}

# Android instructions
Write-Host ""
Write-Host "3. Android App:" -ForegroundColor Cyan
Write-Host "   To build the Android app:" -ForegroundColor Yellow
Write-Host "   cd android" -ForegroundColor Gray
Write-Host "   ./gradlew assembleDebug" -ForegroundColor Gray
Write-Host "   APK will be at: android/app/build/outputs/apk/debug/app-debug.apk" -ForegroundColor Gray

Write-Host ""
Write-Host "===========================================" -ForegroundColor Cyan
Write-Host "    All services started!" -ForegroundColor Green
Write-Host "===========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Backend API:      http://localhost:4000" -ForegroundColor Cyan
Write-Host "Admin Panel:      http://localhost:8080" -ForegroundColor Cyan
Write-Host "API Health Check: http://localhost:4000/home" -ForegroundColor Cyan
Write-Host ""
Write-Host "Default Admin Login:" -ForegroundColor Yellow
Write-Host "  Email:    admin@rapzone.app" -ForegroundColor White
Write-Host "  Password: Admin@123" -ForegroundColor White
Write-Host ""
Write-Host "Press Ctrl+C in each window to stop services." -ForegroundColor Gray