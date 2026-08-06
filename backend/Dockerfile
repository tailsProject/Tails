# 멀티스테이지 빌드

# ---- 1단계: 빌드 ----
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# 의존성 파일 우선 복사
COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle ./
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon || true

COPY src src
RUN ./gradlew bootJar --no-daemon -x test

# ---- 2단계: 실행 ----
FROM eclipse-temurin:21-jre
WORKDIR /app

# 전용 사용자로 실행
RUN useradd --system --uid 1001 spring
COPY --from=build /app/build/libs/*.jar app.jar
RUN mkdir -p /app/uploads && chown -R spring:spring /app
USER spring

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
