# ---- Build Stage ----
FROM gradle:8.7-jdk17 AS build
WORKDIR /app

COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle* .

# 의존성 먼저 다운로드 (캐시 최적화)
RUN gradle dependencies --no-daemon || true

COPY src src
RUN gradle bootJar --no-daemon -x test

# ---- Run Stage ----
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
