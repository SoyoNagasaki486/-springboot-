@echo off
setlocal
cd /d "%~dp0"
title LabGuard Spring Boot 3 + Vue 3

where java >nul 2>nul
if errorlevel 1 goto JAVA_MISSING

set "DB_USER=root"
set "DB_PASSWORD=123456"
set "DB_URL=jdbc:mysql://localhost:3306/lab_safety?createDatabaseIfNotExist=true&useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false"

if exist "frontend\package.json" (
    where npm >nul 2>nul
    if not errorlevel 1 (
        echo [1/3] Building Vue 3 frontend...
        pushd frontend
        call npm run build
        if errorlevel 1 (
            popd
            goto BUILD_FAILED
        )
        popd
    )
)

echo [2/3] Building Spring Boot 3 backend...
call mvnw.cmd -q -DskipTests package
if errorlevel 1 goto BUILD_FAILED

echo [3/3] Starting LabGuard...
echo URL: http://localhost:8080/
echo Database: MySQL 5.7 / lab_safety
echo Press Ctrl+C to stop the application.
echo.
java -jar "target\lab-safety-2.0.0.jar"
goto END

:JAVA_MISSING
echo [ERROR] Java was not found. Install JDK 21 and add it to PATH.
pause
goto END

:BUILD_FAILED
echo [ERROR] Project build failed. Read the messages above.
pause

:END
endlocal
