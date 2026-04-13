@echo off
chcp 65001 >nul
echo ==========================================
echo    宿舍管理系统 - 打包脚本
echo ==========================================

:: 打包后端
echo.
echo [1/2] 正在打包后端 (Spring Boot)...
cd /d "%~dp0\java_server"
call mvnw.cmd clean package -DskipTests
if %errorlevel% neq 0 (
    echo 后端打包失败！
    pause
    exit /b 1
)

:: 打包前端
echo.
echo [2/2] 正在打包前端 (JavaFX)...
cd /d "%~dp0\java_client"
call mvnw.cmd clean package -DskipTests
if %errorlevel% neq 0 (
    echo 前端打包失败！
    pause
    exit /b 1
)

echo.
echo ==========================================
echo    打包完成！
echo ==========================================
echo 后端JAR: java_server\target\java-server-0.0.1-SNAPSHOT.jar
echo 前端JAR: java_client\target\dorm-client-0.0.1-SNAPSHOT.jar
echo.
pause
