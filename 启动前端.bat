@echo off
chcp 65001 >nul
echo 正在启动宿舍管理系统前端...
echo.
cd /d "%~dp0\java_client\target"
java -jar dorm-client-0.0.1-SNAPSHOT.jar
pause
