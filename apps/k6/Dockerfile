FROM golang:1.27.4@sha256:5e8afd493a7124a8586e134d9b532731161d4b9becf9dee5a5956b107426f93d AS builder
RUN go install go.k6.io/xk6/cmd/xk6@v1.4.6 && \
    xk6 build \
      --replace golang.org/x/net=golang.org/x/net@v0.62.0 \
      --replace golang.org/x/crypto=golang.org/x/crypto@v0.59.0

FROM alpine:3@sha256:f59abe656b692d901e184be3445e084786602108599d2a51d229c0f5a48c71bf
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
