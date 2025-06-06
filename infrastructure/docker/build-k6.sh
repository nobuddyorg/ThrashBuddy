#!/bin/bash

set -e

pushd $(dirname $0) > /dev/null
../helm/check-env.sh
. ../helm/.env

pushd ../../ > /dev/null
pushd services/k6 > /dev/null

echo "==========================================="
echo "Start building image for k6 test:"
echo "==========================================="
docker build -t thrash-buddy/k6-test .

popd > /dev/null
popd > /dev/null
