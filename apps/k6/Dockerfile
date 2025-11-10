FROM golang:1.25.4 AS builder
RUN go install go.k6.io/xk6/cmd/xk6@latest && \
    xk6 build

FROM alpine:3
RUN apk add --no-cache ca-certificates aws-cli dos2unix
COPY --from=builder /go/k6 /usr/local/bin/k6

ARG PROMETHEUS_ADDR
ENV PROMETHEUS_ADDR=${PROMETHEUS_ADDR}
ENV MINIO_URL=""
ENV MINIO_ACCESS_KEY=""
ENV MINIO_SECRET_KEY=""
ENV MINIO_BUCKET=""

WORKDIR /app

COPY run-test.sh /app/run-test.sh
RUN dos2unix run-test.sh && chmod +x /app/run-test.sh

ENTRYPOINT ["/bin/sh", "-c", "ls -a /app && /app/run-test.sh"]
