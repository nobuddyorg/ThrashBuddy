#!/bin/bash
# Description: Build the Docker image for the k6 test.

set -e

pushd "$(dirname "$0")"/../../ >/dev/null
. ./scripts/setup/get-config.sh
pushd apps/k6 >/dev/null

echo "==========================================="
echo "Start building image for k6 test:"
echo "==========================================="
docker build --build-arg K6_INFLUXDB_ADDR="$INFLUXDB_ADDR" --build-arg K6_INFLUXDB_ORGANIZATION="$APP_NAME" -t $APP_NAME/k6-test .

popd >/dev/null
popd >/dev/null
