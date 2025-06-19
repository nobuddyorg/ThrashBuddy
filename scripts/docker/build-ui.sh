#!/bin/bash
# Description: Build the ui Docker image.

set -e

pushd "$(dirname "$0")"/../../ >/dev/null
pushd apps/ui/ >/dev/null

echo "==========================================="
echo "Start building image for ui:"
echo "==========================================="
docker build -t thrash-buddy/ui .

popd >/dev/null
popd >/dev/null
