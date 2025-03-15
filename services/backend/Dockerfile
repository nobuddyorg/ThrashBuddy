FROM gradle:8.13.0-jdk21 AS builder
WORKDIR /app

COPY . .
RUN gradle clean bootJar --no-daemon

FROM eclipse-temurin:21-jdk
WORKDIR /app

COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
