FROM gradle:9.8.2-jdk21 AS builder
WORKDIR /app

COPY . .
RUN gradle clean bootJar --no-daemon

# Alpine-based JRE: the Ubuntu-based eclipse-temurin:25-jre image carries a
# large set of OS packages (coreutils, glibc, pcre2, tar, dash, p11-kit, ...)
# with open CVEs and no published fixes, plus the Go-based "pebble" binary.
# None of that is needed to run the Spring Boot jar.
FROM eclipse-temurin:25-jre-alpine
WORKDIR /app

# Pick up Alpine security fixes that land before the next base image rebuild.
RUN apk upgrade --no-cache && \
    addgroup -S app && adduser -S -G app app

COPY --from=builder --chown=app:app /app/build/libs/*.jar app.jar

USER app

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s --start-period=15s --retries=3 \
  CMD nc -z 127.0.0.1 8080 || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
