FROM gradle:9.3.0-jdk21 AS builder
WORKDIR /app

COPY . .
RUN gradle clean bootJar --no-daemon

FROM eclipse-temurin:25-jdk
WORKDIR /app

COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
