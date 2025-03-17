FROM golang:1.24.1 AS builder
RUN go install go.k6.io/xk6/cmd/xk6@latest && \
    xk6 build --with github.com/grafana/xk6-output-influxdb

FROM alpine:latest
RUN apk add --no-cache ca-certificates
COPY --from=builder /go/k6 /usr/local/bin/k6

ENV K6_INFLUXDB_ORGANIZATION="cloud-thrash"
ENV K6_INFLUXDB_BUCKET="metrics"
ENV K6_INFLUXDB_ADDR="http://influxdb-influxdb2.default.svc.cluster.local:80"
ENV K6_INFLUXDB_TOKEN=""

WORKDIR /app
COPY example/test.js /app/test.js
ENTRYPOINT ["k6", "run", "/app/test.js", "-o", "xk6-influxdb"]
