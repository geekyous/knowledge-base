# 多阶段构建：先构建，再运行
# -------------------------------------------------------------------------
# 为什么用多阶段构建？
# 1. 构建阶段需要 JDK + Maven（~400MB），运行阶段只需要 JRE（~150MB）
# 2. 最终镜像不包含源码、编译工具，更小更安全
# 3. 每个阶段可以从不同的基础镜像开始
# -------------------------------------------------------------------------
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /build
COPY . .
# 使用 Alpine 包管理器安装 Maven（项目中没有 Maven Wrapper）
RUN apk add --no-cache maven && mvn package -DskipTests

# 运行阶段
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /build/target/*.jar app.jar

# 时区设置
RUN apk add --no-cache tzdata && \
    cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo "Asia/Shanghai" > /etc/timezone

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
