@echo off
chcp 65001 >nul
echo 正在启动宿舍管理系统后端...
echo 端口: 22223
echo.
cd /d "%~dp0\java_server\target"
java -jar java-server-0.0.1-SNAPSHOT.jar
pause
