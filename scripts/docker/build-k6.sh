#!/bin/bash
# Description: Build the Docker image for the k6 test.

set -eu

pushd "$(dirname "$0")" >/dev/null
../helm/check-env.sh
. ../../configs/.env

pushd ../../ >/dev/null
pushd apps/k6 >/dev/null

echo "==========================================="
echo "Start building image for k6 test:"
echo "==========================================="
docker build -t thrash-buddy/k6-test .

popd >/dev/null
popd >/dev/null
