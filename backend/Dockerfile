########## 构建阶段 ##########
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B clean package -DskipTests

########## 运行阶段 ##########
FROM eclipse-temurin:17-jre AS runtime
RUN apt-get update && apt-get install -y --no-install-recommends curl tzdata \
    && rm -rf /var/lib/apt/lists/* \
    && useradd -r -u 1001 -m -d /app appuser \
    && mkdir -p /app && chown appuser:appuser /app
ENV TZ=Asia/Shanghai
WORKDIR /app
COPY --from=build /build/target/eldercare-platform.jar /app/app.jar
USER appuser
EXPOSE 8080
HEALTHCHECK --interval=15s --timeout=5s --start-period=90s --retries=10 \
  CMD curl -fsS http://localhost:8080/actuator/health | grep -q '"status":"UP"' || exit 1
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
