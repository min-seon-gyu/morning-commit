# 1. Build Stage
FROM gradle:8.5-jdk21 AS builder
WORKDIR /app
COPY . .

RUN ./gradlew clean build -x test


FROM eclipse-temurin:21-jre
WORKDIR /app


COPY --from=builder /app/build/libs/*.jar app.jar


ENV TZ=Asia/Seoul

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]