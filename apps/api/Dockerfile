FROM gradle:9.6.1-jdk21 AS builder
WORKDIR /app

COPY . .
RUN gradle clean bootJar --no-daemon

FROM eclipse-temurin:25-jre
WORKDIR /app

RUN groupadd -r app && useradd -r -g app app

COPY --from=builder /app/build/libs/*.jar app.jar
RUN chown app:app app.jar

USER app

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s --start-period=15s --retries=3 \
  CMD bash -c 'echo > /dev/tcp/127.0.0.1/8080' || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
