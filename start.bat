@echo off
chcp 65001 >nul
REM ============================================================
REM 草花互动抽奖服务器 — Windows 启动脚本
REM 用法:
REM   开发模式: start.bat
REM   生产模式: start.bat prod
REM
REM 前置条件: Java 17+ 和 MySQL 已安装
REM ============================================================

set JAR=target\raffle-drawing-1.0.0.jar

if not exist "%JAR%" (
    echo [错误] 找不到 %JAR%，请先执行: mvn clean package -DskipTests
    pause
    exit /b 1
)

if "%1"=="prod" (
    echo ============================================================
    echo  草花互动抽奖服务器 — 生产模式
    echo ============================================================
    echo  数据库: MySQL
    echo  请确保已设置环境变量:
    echo    DB_HOST  (默认 localhost)
    echo    DB_PORT  (默认 3306)
    echo    DB_NAME  (默认 raffle)
    echo    DB_USER  (默认 root)
    echo    DB_PASS  (必填)
    echo ============================================================
    java -jar %JAR% --spring.profiles.active=prod --server.port=%SERVER_PORT:8080%
) else (
    echo ============================================================
    echo  草花互动抽奖服务器 — 开发模式
    echo ============================================================
    echo  数据库: H2 内存 (重启清空)
    echo  管理员: 13800138000 / 管理员
    echo  地址:   http://localhost:8080
    echo ============================================================
    java -jar %JAR% --spring.profiles.active=dev --server.port=8080
)

pause
