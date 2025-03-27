#!/bin/bash

set -e

pushd $(dirname $0)/../../ > /dev/null
pushd services/frontend/ > /dev/null

echo "==========================================="
echo "Start building image for frontend:"
echo "==========================================="
docker build -t cloud-thrash/frontend .

popd > /dev/null
popd > /dev/null
