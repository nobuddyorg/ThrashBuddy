#!/bin/bash
# Description: Build the api Docker image.

set -e

pushd "$(dirname "$0")"/../../ >/dev/null
. ./scripts/setup/get-config.sh
pushd apps/api/ >/dev/null

echo "==========================================="
echo "Start building image for api:"
echo "==========================================="
docker build -t $APP_NAME/api .

popd >/dev/null
popd >/dev/null
