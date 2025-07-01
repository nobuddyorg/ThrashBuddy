FROM golang:1.24.4 AS builder
RUN go install go.k6.io/xk6/cmd/xk6@latest && \
    xk6 build --with github.com/grafana/xk6-output-influxdb

FROM alpine:3
RUN apk add --no-cache ca-certificates aws-cli dos2unix
COPY --from=builder /go/k6 /usr/local/bin/k6

ARG K6_INFLUXDB_ADDR
ARG K6_INFLUXDB_ORGANIZATION

ENV K6_INFLUXDB_ADDR=${K6_INFLUXDB_ADDR}
ENV K6_INFLUXDB_ORGANIZATION=${K6_INFLUXDB_ORGANIZATION}
ENV K6_INFLUXDB_BUCKET="metrics"
ENV K6_INFLUXDB_TOKEN=""

ENV MINIO_URL=""
ENV MINIO_ACCESS_KEY=""
ENV MINIO_SECRET_KEY=""
ENV MINIO_BUCKET=""

WORKDIR /app

COPY ./telegraf.conf /etc/telegraf/telegraf.conf
RUN wget -q https://dl.influxdata.com/telegraf/releases/telegraf-1.34.0_linux_amd64.tar.gz && \
    tar xf telegraf-1.34.0_linux_amd64.tar.gz && \
    cp telegraf-1.34.0/usr/bin/telegraf /usr/bin/ && \
    rm -rf telegraf-1.34.0_linux_amd64.tar.gz telegraf-1.34.0

COPY run-test.sh /app/run-test.sh
RUN dos2unix run-test.sh && chmod +x /app/run-test.sh

ENTRYPOINT ["/bin/sh", "-c", "mkdir -p /var/log/telegraf && (telegraf --config /etc/telegraf/telegraf.conf &) && ls -a /app && /app/run-test.sh"]
