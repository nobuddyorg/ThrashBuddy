#!/bin/bash

set -e

pushd $(dirname $0)/../../ > /dev/null
pushd services/backend/ > /dev/null

echo "==========================================="
echo "Start building image for backend:"
echo "==========================================="
docker build -t cloud-thrash/backend .

popd > /dev/null
popd > /dev/null
