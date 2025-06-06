#!/bin/bash

pushd $(dirname $0) > /dev/null

set -e

./dependencies.sh

echo "Validating Helm chart..."

KUBE_SCORE_BINARY="./.deps/kube-score"
KUBE_CONFORM_BINARY="./.deps/kubeconform"

helm lint .
helm template . | $KUBE_CONFORM_BINARY -strict -verbose
helm template . | $KUBE_SCORE_BINARY score -
helm install thrash-buddy --dry-run --debug .
echo "Helm chart is valid."

echo "Unit testing Helm chart..."
helm plugin install https://github.com/quintush/helm-unittest || true
helm plugin update unittest
helm unittest . --strict
echo "Helm chart unit tests passed."

popd > /dev/null
