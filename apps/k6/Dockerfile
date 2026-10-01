FROM golang:1.27.0@sha256:4013ae0f9e7994f8535c58c811f8f863fbed38b72e0d51e6592156f758d66146 AS builder
RUN go install go.k6.io/xk6/cmd/xk6@v1.4.6 && \
    xk6 build

FROM alpine:3@sha256:294b683cb724975bec92580e1e685676bd4b50bda910ddb8c51d4cabeaec77e6
# Upgrade base packages first so Alpine security fixes (e.g. openssl) that
# postdate the pinned base digest are picked up.
RUN apk upgrade --no-cache && \
    apk add --no-cache ca-certificates aws-cli dos2unix && \
    addgroup -S app && adduser -S -G app app
COPY --from=builder /go/k6 /usr/local/bin/k6

ARG PROMETHEUS_ADDR
ENV PROMETHEUS_ADDR=${PROMETHEUS_ADDR}
ENV MINIO_URL=""
ENV MINIO_ACCESS_KEY=""
ENV MINIO_SECRET_KEY=""
ENV MINIO_BUCKET=""

WORKDIR /app
RUN chown app:app /app

COPY --chown=app:app run-test.sh /app/run-test.sh
RUN dos2unix /app/run-test.sh && chmod +x /app/run-test.sh

USER app

ENTRYPOINT ["/bin/sh", "-c", "ls -a /app && /app/run-test.sh"]
