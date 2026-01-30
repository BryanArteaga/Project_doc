# Script de Optimización y Build
# Fecha: 24/12/2025

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  SCRIPT DE OPTIMIZACIÓN - Doc App" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Verificar que estamos en el directorio correcto
if (-not (Test-Path ".\gradlew.bat")) {
    Write-Host "Error: No se encuentra gradlew.bat" -ForegroundColor Red
    Write-Host "Asegurate de ejecutar este script desde la raiz del proyecto" -ForegroundColor Yellow
    exit 1
}

# Función para mostrar tamaño de archivo
function Get-FileSize {
    param($FilePath)
    if (Test-Path $FilePath) {
        $size = (Get-Item $FilePath).Length
        return "{0:N2} MB" -f ($size / 1MB)
    }
    return "No encontrado"
}

# Menú principal
Write-Host "Selecciona una opción:" -ForegroundColor Yellow
Write-Host "1. Limpiar build anterior (Clean)" -ForegroundColor White
Write-Host "2. Compilar Debug (APK normal)" -ForegroundColor White
Write-Host "3. Compilar Release (APK optimizado)" -ForegroundColor White
Write-Host "4. Generar Bundle (AAB para Play Store)" -ForegroundColor White
Write-Host "5. Ver tamaños de archivos generados" -ForegroundColor White
Write-Host "6. TODO: Clean + Release + Bundle" -ForegroundColor Green
Write-Host "0. Salir" -ForegroundColor Red
Write-Host ""

$option = Read-Host "Opción"

switch ($option) {
    "1" {
        Write-Host "`nLimpiando build anterior..." -ForegroundColor Yellow
        .\gradlew.bat clean
        Write-Host "`n✓ Build limpiado" -ForegroundColor Green
    }

    "2" {
        Write-Host "`nCompilando Debug APK..." -ForegroundColor Yellow
        .\gradlew.bat assembleDebug

        $apkPath = "app\build\outputs\apk\debug\app-debug.apk"
        if (Test-Path $apkPath) {
            $size = Get-FileSize $apkPath
            Write-Host "`n✓ Debug APK generado exitosamente!" -ForegroundColor Green
            Write-Host "Tamaño: $size" -ForegroundColor Cyan
            Write-Host "Ubicación: $apkPath" -ForegroundColor Gray
        }
    }

    "3" {
        Write-Host "`nCompilando Release APK (optimizado)..." -ForegroundColor Yellow
        Write-Host "NOTA: Esto aplica minify + shrinkResources + ProGuard" -ForegroundColor Gray
        .\gradlew.bat assembleRelease

        $apkPath = "app\build\outputs\apk\release\app-release-unsigned.apk"
        if (Test-Path $apkPath) {
            $size = Get-FileSize $apkPath
            Write-Host "`n✓ Release APK generado exitosamente!" -ForegroundColor Green
            Write-Host "Tamaño: $size" -ForegroundColor Cyan
            Write-Host "Ubicación: $apkPath" -ForegroundColor Gray
            Write-Host "`nNOTA: APK sin firmar. Para firmar, configura keystore." -ForegroundColor Yellow
        }
    }

    "4" {
        Write-Host "`nGenerando App Bundle (AAB)..." -ForegroundColor Yellow
        Write-Host "NOTA: AAB con splits optimizados por dispositivo" -ForegroundColor Gray
        .\gradlew.bat bundleRelease

        $aabPath = "app\build\outputs\bundle\release\app-release.aab"
        if (Test-Path $aabPath) {
            $size = Get-FileSize $aabPath
            Write-Host "`n✓ App Bundle generado exitosamente!" -ForegroundColor Green
            Write-Host "Tamaño: $size" -ForegroundColor Cyan
            Write-Host "Ubicación: $aabPath" -ForegroundColor Gray
            Write-Host "`nEste archivo es para subir a Google Play Store" -ForegroundColor Yellow
        }
    }

    "5" {
        Write-Host "`nTamaños de archivos generados:" -ForegroundColor Yellow
        Write-Host "================================" -ForegroundColor Gray

        $debugApk = "app\build\outputs\apk\debug\app-debug.apk"
        $releaseApk = "app\build\outputs\apk\release\app-release-unsigned.apk"
        $aab = "app\build\outputs\bundle\release\app-release.aab"

        Write-Host "Debug APK:   " -NoNewline
        Write-Host (Get-FileSize $debugApk) -ForegroundColor Cyan

        Write-Host "Release APK: " -NoNewline
        Write-Host (Get-FileSize $releaseApk) -ForegroundColor Cyan

        Write-Host "App Bundle:  " -NoNewline
        Write-Host (Get-FileSize $aab) -ForegroundColor Cyan

        Write-Host "`nCarpeta drawable:" -ForegroundColor Yellow
        if (Test-Path "app\src\main\res\drawable") {
            $drawableSize = (Get-ChildItem "app\src\main\res\drawable" -Recurse | Measure-Object -Property Length -Sum).Sum
            Write-Host ("{0:N2} MB" -f ($drawableSize / 1MB)) -ForegroundColor Cyan
            $count = (Get-ChildItem "app\src\main\res\drawable" -File).Count
            Write-Host "Archivos: $count" -ForegroundColor Gray
        }
    }

    "6" {
        Write-Host "`nEjecutando proceso completo de optimización..." -ForegroundColor Green
        Write-Host "================================================" -ForegroundColor Gray

        Write-Host "`n[1/3] Limpiando build anterior..." -ForegroundColor Yellow
        .\gradlew.bat clean
        Write-Host "✓ Limpiado" -ForegroundColor Green

        Write-Host "`n[2/3] Compilando Release APK optimizado..." -ForegroundColor Yellow
        .\gradlew.bat assembleRelease
        $releaseApk = "app\build\outputs\apk\release\app-release-unsigned.apk"
        if (Test-Path $releaseApk) {
            Write-Host "✓ Release APK: $(Get-FileSize $releaseApk)" -ForegroundColor Green
        }

        Write-Host "`n[3/3] Generando App Bundle..." -ForegroundColor Yellow
        .\gradlew.bat bundleRelease
        $aab = "app\build\outputs\bundle\release\app-release.aab"
        if (Test-Path $aab) {
            Write-Host "✓ App Bundle: $(Get-FileSize $aab)" -ForegroundColor Green
        }

        Write-Host "`n========================================" -ForegroundColor Cyan
        Write-Host "  ✓ PROCESO COMPLETADO" -ForegroundColor Green
        Write-Host "========================================" -ForegroundColor Cyan
        Write-Host "`nArchivos generados:" -ForegroundColor Yellow
        Write-Host "- Release APK: $releaseApk" -ForegroundColor Gray
        Write-Host "- App Bundle:  $aab" -ForegroundColor Gray
    }

    "0" {
        Write-Host "`nSaliendo..." -ForegroundColor Gray
        exit 0
    }

    default {
        Write-Host "`nOpción inválida" -ForegroundColor Red
    }
}

Write-Host "`nPresiona cualquier tecla para continuar..." -ForegroundColor Gray
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")

