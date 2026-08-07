# ============================================================
# 草花互动抽奖服务器 — Docker 镜像
# 构建: docker build -t raffle-server .
# ============================================================
FROM eclipse-temurin:17-jre-alpine

# 设置时区
RUN apk add --no-cache tzdata curl && \
    cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo "Asia/Shanghai" > /etc/timezone

WORKDIR /app

# 复制 JAR
COPY target/raffle-drawing-1.0.0.jar app.jar

# 创建日志目录
RUN mkdir -p logs

# 健康检查
HEALTHCHECK --interval=30s --timeout=5s --retries=3 \
    CMD curl -f http://localhost:8080/ || exit 1

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
