#!/bin/sh
set -e

env

if [ -z "$MINIO_ACCESS_KEY" ] || [ -z "$MINIO_SECRET_KEY" ]; then
  echo "Error: MINIO_ACCESS_KEY and MINIO_SECRET_KEY must be set"
  exit 1
fi

export AWS_ACCESS_KEY_ID="$MINIO_ACCESS_KEY"
export AWS_SECRET_ACCESS_KEY="$MINIO_SECRET_KEY"
export AWS_EC2_METADATA_DISABLED=true

echo "Downloading test files from MinIO bucket: $MINIO_BUCKET..."
aws --endpoint-url "$MINIO_URL" s3 cp "s3://$MINIO_BUCKET/" /app/ --recursive

if [ ! -f "/app/test.js" ]; then
  echo "Error: test.js not found in MinIO bucket"
  exit 1
fi

echo "Running k6 test: test.js"


K6_PROMETHEUS_RW_TREND_STATS="avg,p(90),p(95)" \
K6_PROMETHEUS_RW_SERVER_URL=$PROMETHEUS_ADDR \
k6 run -o experimental-prometheus-rw "/app/test.js"
