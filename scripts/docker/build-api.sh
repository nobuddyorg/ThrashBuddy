#!/bin/bash
# Description: Build the api Docker image.

set -eu

pushd "$(dirname "$0")"/../../ >/dev/null
pushd apps/api/ >/dev/null

echo "==========================================="
echo "Start building image for api:"
echo "==========================================="
docker build -t thrash-buddy/api .

popd >/dev/null
popd >/dev/null
