#!/bin/bash
# ============================================================
# 草花互动抽奖服务器 — Linux/Mac 启动脚本
# 用法:
#   ./start.sh        开发模式 (H2 内存)
#   ./start.sh prod   生产模式 (MySQL)
#
# 前置条件: Java 17+ 和 MySQL 已安装
# ============================================================

JAR="target/raffle-drawing-1.0.0.jar"

if [ ! -f "$JAR" ]; then
    echo "[错误] 找不到 $JAR，请先执行: mvn clean package -DskipTests"
    exit 1
fi

if [ "$1" = "prod" ]; then
    echo "============================================================"
    echo " 草花互动抽奖服务器 — 生产模式"
    echo "============================================================"
    echo " 数据库: MySQL"
    echo " 请确保已设置环境变量: DB_HOST DB_PORT DB_NAME DB_USER DB_PASS"
    echo "============================================================"
    java -jar "$JAR" --spring.profiles.active=prod --server.port="${SERVER_PORT:-8080}"
else
    echo "============================================================"
    echo " 草花互动抽奖服务器 — 开发模式"
    echo "============================================================"
    echo " 数据库: H2 内存 (重启清空)"
    echo " 管理员: 13800138000 / 管理员"
    echo " 地址:   http://localhost:8080"
    echo "============================================================"
    java -jar "$JAR" --spring.profiles.active=dev --server.port=8080
fi
