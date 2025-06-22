#!/bin/bash
# Description: Build the ui Docker image.

set -e

pushd "$(dirname "$0")"/../../ >/dev/null
. ./scripts/setup/get-config.sh
pushd apps/ui/ >/dev/null

echo "==========================================="
echo "Start building image for ui:"
echo "==========================================="
docker build -t $APP_NAME/ui .

popd >/dev/null
popd >/dev/null
