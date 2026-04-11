@echo off
chcp 65001 >nul
echo ==========================================
echo 宿舍管理系统 - 分别打包前端和后端
echo ==========================================
echo.

:: 先清理
echo [1/4] 清理旧的构建文件...
if exist target rmdir /s /q target
if exist target-client rmdir /s /q target-client

:: 打包后端
echo.
echo [2/4] 打包后端 (Spring Boot)...
call mvnw.cmd -f pom-server.xml clean package -DskipTests
if errorlevel 1 (
    echo 后端打包失败！
    pause
    exit /b 1
)

:: 复制后端jar
echo.
echo [3/4] 复制后端JAR...
mkdir target 2>nul
copy target\dorm-server-0.0.1-SNAPSHOT.jar target\dorm-server.jar >nul
echo 后端JAR: target\dorm-server.jar

:: 打包前端
echo.
echo [4/4] 打包前端 (JavaFX)...
call mvnw.cmd -f pom-client.xml clean package -DskipTests
if errorlevel 1 (
    echo 前端打包失败！
    pause
    exit /b 1
)

:: 复制前端jar
copy target-client\dorm-client-0.0.1-SNAPSHOT.jar target\dorm-client.jar >nul 2>&1
if errorlevel 1 (
    copy target\dorm-client-0.0.1-SNAPSHOT.jar target\dorm-client.jar >nul 2>&1
)
echo 前端JAR: target\dorm-client.jar

echo.
echo ==========================================
echo 打包完成！
echo ==========================================
echo.
echo 文件位置:
echo   - 后端: target\dorm-server.jar
echo   - 前端: target\dorm-client.jar
echo.
echo 运行命令:
echo   - 后端: java -jar target\dorm-server.jar
echo   - 前端: java -jar target\dorm-client.jar
echo.
pause
